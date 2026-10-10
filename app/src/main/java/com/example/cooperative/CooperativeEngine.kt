package com.example.cooperative

import android.content.Context
import android.os.SystemClock
import com.example.data.CooperativeLogManager
import com.example.model.CooperativeRunResult
import com.example.network.NetworkBindingHelper
import com.example.network.NetworkMeter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.InetSocketAddress
import java.net.ProtocolException
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

data class CooperativeProgress(
    val bytesTotalExpected: Long,
    val bytesFromCoordinator: Long,
    val bytesFromHelper: Long,
    val chunksTotal: Int,
    val chunksFromCoordinator: Int,
    val chunksFromHelper: Int,
    val elapsedSeconds: Double,
    val currentThroughputMbps: Double,
    val statusText: String
)

data class ChunkWork(
    val id: Int,
    val startByte: Long,
    val endByte: Long,
    var retries: Int = 0
) {
    val length: Long get() = (endByte - startByte) + 1
}

data class HelperChunkPayload(
    val chunkId: Int,
    val startByte: Long,
    val endByte: Long,
    val data: ByteArray
)

class CooperativeEngine(
    private val context: Context,
    private val networkMeter: NetworkMeter,
    private val bindingHelper: NetworkBindingHelper,
    private val logManager: CooperativeLogManager
) {

    companion object {
        const val COOP_PORT = 8990
        const val SOCKET_TIMEOUT_MS = 30000

        // P2P Protocol OP codes
        const val OP_HELPER_HELLO = 101
        const val OP_COORDINATOR_START = 102
        const val OP_ASSIGN_CHUNK = 103
        const val OP_CHUNK_PAYLOAD = 104
        const val OP_CHUNK_FAILED = 105
        const val OP_REQUEST_CHUNK = 106
        const val OP_ALL_CHUNKS_COMPLETE = 107
        const val OP_HELPER_FINAL_STATS = 108

        // Cache of Solo run SHA-256 hashes per URL
        val soloHashes = ConcurrentHashMap<String, String>()
    }

    @Volatile
    private var activeServerSocket: ServerSocket? = null

    @Volatile
    private var activeSocket: Socket? = null

    @Volatile
    private var isCancelled = false

    fun cancelOperation() {
        isCancelled = true
        try {
            activeSocket?.close()
        } catch (_: Exception) {}
        try {
            activeServerSocket?.close()
        } catch (_: Exception) {}
    }

    /**
     * Checks if server supports Accept-Ranges and returns file size.
     * Probes directly via a 1-byte GET Range request (Range: bytes=0-0) to avoid servers
     * (like Hetzner) that drop or reject HTTP HEAD requests.
     */
    suspend fun probeUrl(fileUrl: String, client: OkHttpClient): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val rangeProbe = Request.Builder()
                .url(fileUrl)
                .header("Range", "bytes=0-0")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) COMBINE-Benchmark/2.0")
                .header("Accept", "*/*")
                .header("Connection", "close")
                .build()

            val probeResp = client.newCall(rangeProbe).execute()
            if (probeResp.code != 206) {
                val code = probeResp.code
                probeResp.close()
                return@withContext Result.failure(
                    Exception("Server returned HTTP $code. Server does not support HTTP byte-range requests (HTTP 206 Partial Content required).")
                )
            }

            val contentRange = probeResp.header("Content-Range")
            probeResp.close()

            var contentLength = -1L
            if (contentRange != null && contentRange.contains("/")) {
                val totalStr = contentRange.substringAfter("/").trim()
                contentLength = totalStr.toLongOrNull() ?: -1L
            }

            if (contentLength <= 0L) {
                return@withContext Result.failure(Exception("Could not determine total file size from server response (Content-Range: $contentRange)."))
            }

            Result.success(contentLength)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun connectWithRetry(
        targetIp: String,
        port: Int,
        maxAttempts: Int = 30,
        delayMs: Long = 1000L
    ): Socket = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        for (attempt in 1..maxAttempts) {
            if (isCancelled) throw CancellationException("Cancelled")
            val socket = Socket()
            try {
                socket.tcpNoDelay = true
                socket.soTimeout = SOCKET_TIMEOUT_MS
                socket.connect(InetSocketAddress(targetIp, port), 4000)
                activeSocket = socket
                return@withContext socket
            } catch (e: Exception) {
                lastException = e
                try { socket.close() } catch (_: Exception) {}
                delay(delayMs)
            }
        }
        throw lastException ?: IOException("Failed to connect to $targetIp:$port after $maxAttempts attempts")
    }

    private suspend fun downloadChunk(
        client: OkHttpClient,
        fileUrl: String,
        startByte: Long,
        endByte: Long,
        chunkId: Int,
        maxRetries: Int = 3
    ): ByteArray {
        var lastError: Exception? = null
        val expectedSize = (endByte - startByte + 1).toInt()
        for (attempt in 1..maxRetries) {
            if (isCancelled) throw CancellationException("Cancelled")
            var response: Response? = null
            try {
                val req = Request.Builder()
                    .url(fileUrl)
                    .header("Range", "bytes=$startByte-$endByte")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) COMBINE-Benchmark/2.0")
                    .header("Accept", "*/*")
                    .header("Connection", "keep-alive")
                    .build()

                response = client.newCall(req).execute()
                if (!response.isSuccessful && response.code != 206) {
                    val code = response.code
                    response.close()
                    throw IOException("HTTP $code on chunk $chunkId")
                }
                val body = response.body ?: throw IOException("Empty body on chunk $chunkId")
                val bytes = body.bytes()
                body.close()

                if (bytes.size != expectedSize) {
                    throw ProtocolException("Incomplete chunk $chunkId: got ${bytes.size} bytes, expected $expectedSize")
                }
                return bytes
            } catch (e: Exception) {
                lastError = e
                try {
                    response?.close()
                } catch (_: Exception) {}
                if (isCancelled) throw e
                if (attempt < maxRetries) {
                    delay(200L * attempt)
                }
            }
        }
        throw lastError ?: IOException("Failed to download chunk $chunkId after $maxRetries retries")
    }

    // =========================================================================
    // 1. SOLO DOWNLOAD EXECUTION
    // =========================================================================
    suspend fun executeSoloDownload(
        fileUrl: String,
        chunkSizeBytes: Long,
        conditionNote: String,
        batchId: String,
        runLabel: String,
        onProgress: (CooperativeProgress) -> Unit
    ): Result<CooperativeRunResult> = withContext(Dispatchers.IO) {
        isCancelled = false
        val startTimeWall = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val startClock = SystemClock.elapsedRealtime()

        val batteryBefore = networkMeter.getBatteryPercentage()
        val isChargingAtStart = networkMeter.isDeviceCharging()
        val coordinatorNetType = networkMeter.getNetworkType()
        val coordinatorDevice = networkMeter.getDeviceDescription()

        // Coordinator uses WiFi if available with internet, otherwise default active internet network
        val wifiNet = bindingHelper.wifiNetwork.value
        val internetNet = if (wifiNet != null && bindingHelper.hasInternet(wifiNet)) wifiNet else null
        val client = bindingHelper.createBoundClient(internetNet)

        val probeResult = probeUrl(fileUrl, client)
        if (probeResult.isFailure) {
            return@withContext Result.failure(probeResult.exceptionOrNull() ?: Exception("Probe failed"))
        }
        val fileSize = probeResult.getOrThrow()

        val chunks = mutableListOf<ChunkWork>()
        var offset = 0L
        var chunkId = 0
        while (offset < fileSize) {
            val end = minOf(offset + chunkSizeBytes - 1, fileSize - 1)
            chunks.add(ChunkWork(chunkId++, offset, end))
            offset = end + 1
        }
        val totalChunks = chunks.size

        val tempFile = File(context.cacheDir, "solo_assembled_${System.currentTimeMillis()}.bin")
        val raf = RandomAccessFile(tempFile, "rw")
        raf.setLength(fileSize)

        val bytesDownloaded = AtomicLong(0L)
        val chunksDone = AtomicInteger(0)
        var lastEmit = startClock

        try {
            for (chunk in chunks) {
                if (isCancelled) throw CancellationException("Solo download cancelled.")

                val bytes = downloadChunk(client, fileUrl, chunk.startByte, chunk.endByte, chunk.id, maxRetries = 3)

                raf.seek(chunk.startByte)
                raf.write(bytes)

                val downloaded = bytesDownloaded.addAndGet(bytes.size.toLong())
                val cDone = chunksDone.incrementAndGet()

                val now = SystemClock.elapsedRealtime()
                if (now - lastEmit >= 100 || cDone == totalChunks) {
                    val elapsedSec = maxOf((now - startClock) / 1000.0, 0.001)
                    val speedMbps = (downloaded * 8.0) / (elapsedSec * 1e6)
                    onProgress(
                        CooperativeProgress(
                            bytesTotalExpected = fileSize,
                            bytesFromCoordinator = downloaded,
                            bytesFromHelper = 0L,
                            chunksTotal = totalChunks,
                            chunksFromCoordinator = cDone,
                            chunksFromHelper = 0,
                            elapsedSeconds = elapsedSec,
                            currentThroughputMbps = speedMbps,
                            statusText = "Downloading chunk $cDone of $totalChunks (Solo)..."
                        )
                    )
                    lastEmit = now
                }
            }

            val endClock = SystemClock.elapsedRealtime()
            val totalTimeSec = maxOf((endClock - startClock) / 1000.0, 0.001)
            val effectiveMbps = (fileSize * 8.0) / (totalTimeSec * 1e6)
            val batteryAfter = networkMeter.getBatteryPercentage()

            // Compute and store SHA-256 for reference
            raf.close()
            val soloSha256 = computeSha256(tempFile)
            soloHashes[fileUrl] = soloSha256
            tempFile.delete()

            val runResult = CooperativeRunResult(
                timestamp = startTimeWall,
                mode = "Solo",
                fileUrl = fileUrl,
                fileSizeBytes = fileSize,
                chunkSizeBytes = chunkSizeBytes,
                coordinatorNetwork = coordinatorNetType,
                helperNetwork = "N/A",
                coordinatorDevice = coordinatorDevice,
                helperDevice = "N/A",
                totalTimeSeconds = totalTimeSec,
                effectiveThroughputMbps = effectiveMbps,
                bytesFromCoordinatorLink = fileSize,
                bytesFromHelperLink = 0L,
                chunksTotal = totalChunks,
                chunksFromCoordinator = totalChunks,
                chunksFromHelper = 0,
                chunkRetries = 0,
                localTransferTotalSeconds = 0.0,
                localThroughputMbps = 0.0,
                hashMatch = "N/A (Solo Reference: ${soloSha256.take(8)})",
                batteryBeforeCoordinator = batteryBefore,
                batteryAfterCoordinator = batteryAfter,
                batteryBeforeHelper = 0,
                batteryAfterHelper = 0,
                isChargingCoordinator = isChargingAtStart,
                isChargingHelper = false,
                testConditionNote = conditionNote,
                batchId = batchId,
                runNumberInBatch = runLabel,
                status = "OK"
            )

            logManager.appendRun(runResult)
            Result.success(runResult)
        } catch (e: Exception) {
            raf.close()
            tempFile.delete()
            Result.failure(e)
        }
    }

    // =========================================================================
    // 2. POOLED DOWNLOAD: COORDINATOR LOGIC
    // =========================================================================
    suspend fun executeCoordinatorPooled(
        fileUrl: String,
        chunkSizeBytes: Long,
        conditionNote: String,
        batchId: String,
        runLabel: String,
        isGroupOwner: Boolean,
        groupOwnerAddress: String,
        onProgress: (CooperativeProgress) -> Unit
    ): Result<CooperativeRunResult> = withContext(Dispatchers.IO) {
        isCancelled = false
        val startTimeWall = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val startClock = SystemClock.elapsedRealtime()

        val batteryBefore = networkMeter.getBatteryPercentage()
        val isChargingAtStart = networkMeter.isDeviceCharging()
        val coordinatorNetType = networkMeter.getNetworkType()
        val coordinatorDevice = networkMeter.getDeviceDescription()

        // Coordinator HTTP client bound to WiFi (if internet present) or default internet
        val wifiNet = bindingHelper.wifiNetwork.value
        val internetNet = if (wifiNet != null && bindingHelper.hasInternet(wifiNet)) wifiNet else null
        val client = bindingHelper.createBoundClient(internetNet)

        val probeResult = probeUrl(fileUrl, client)
        if (probeResult.isFailure) {
            return@withContext Result.failure(probeResult.exceptionOrNull() ?: Exception("Probe failed"))
        }
        val fileSize = probeResult.getOrThrow()

        // Partition chunks into work queue
        val workQueue = ConcurrentLinkedQueue<ChunkWork>()
        var offset = 0L
        var chunkId = 0
        while (offset < fileSize) {
            val end = minOf(offset + chunkSizeBytes - 1, fileSize - 1)
            workQueue.add(ChunkWork(chunkId++, offset, end))
            offset = end + 1
        }
        val totalChunks = chunkId

        val tempFile = File(context.cacheDir, "pooled_assembled_${System.currentTimeMillis()}.bin")
        val raf = RandomAccessFile(tempFile, "rw")
        raf.setLength(fileSize)
        val fileMutex = Mutex()

        // Symmetrical P2P socket establishment: Group Owner listens; Client connects to GO
        val socket: Socket = if (isGroupOwner) {
            onProgress(
                CooperativeProgress(
                    bytesTotalExpected = fileSize,
                    bytesFromCoordinator = 0L,
                    bytesFromHelper = 0L,
                    chunksTotal = totalChunks,
                    chunksFromCoordinator = 0,
                    chunksFromHelper = 0,
                    elapsedSeconds = 0.0,
                    currentThroughputMbps = 0.0,
                    statusText = "Waiting for Helper phone to connect on port $COOP_PORT..."
                )
            )
            val serverSocket = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(COOP_PORT))
                soTimeout = 45000
            }
            activeServerSocket = serverSocket
            try {
                serverSocket.accept()
            } catch (e: Exception) {
                raf.close()
                tempFile.delete()
                serverSocket.close()
                return@withContext Result.failure(Exception("Timeout waiting for Helper phone to connect: ${e.message}"))
            } finally {
                serverSocket.close()
            }
        } else {
            val targetIp = groupOwnerAddress.ifBlank { "192.168.49.1" }
            onProgress(
                CooperativeProgress(
                    bytesTotalExpected = fileSize,
                    bytesFromCoordinator = 0L,
                    bytesFromHelper = 0L,
                    chunksTotal = totalChunks,
                    chunksFromCoordinator = 0,
                    chunksFromHelper = 0,
                    elapsedSeconds = 0.0,
                    currentThroughputMbps = 0.0,
                    statusText = "Connecting to Helper at $targetIp:$COOP_PORT..."
                )
            )
            try {
                connectWithRetry(targetIp, COOP_PORT, maxAttempts = 30)
            } catch (e: Exception) {
                raf.close()
                tempFile.delete()
                return@withContext Result.failure(Exception("Could not connect to Helper ($targetIp:$COOP_PORT): ${e.message}"))
            }
        }
        activeSocket = socket
        socket.soTimeout = SOCKET_TIMEOUT_MS
        socket.tcpNoDelay = true

        val dis = DataInputStream(BufferedInputStream(socket.getInputStream()))
        val dos = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))

        // Read Helper Hello Handshake
        val helloOp = dis.readInt()
        if (helloOp != OP_HELPER_HELLO) {
            closeAll()
            raf.close()
            tempFile.delete()
            return@withContext Result.failure(Exception("Invalid handshake from Helper."))
        }
        val helperBatteryBefore = dis.readInt()
        val helperChargingBefore = dis.readBoolean()
        val helperDevice = dis.readUTF()
        val helperNetwork = dis.readUTF()

        // Tell Helper to start pooled session
        dos.writeInt(OP_COORDINATOR_START)
        dos.writeUTF(fileUrl)
        dos.writeLong(fileSize)
        dos.writeLong(chunkSizeBytes)
        dos.flush()

        val bytesFromCoordinator = AtomicLong(0L)
        val bytesFromHelper = AtomicLong(0L)
        val chunksFromCoordinator = AtomicInteger(0)
        val chunksFromHelper = AtomicInteger(0)
        val chunkRetries = AtomicInteger(0)
        val localTransferNanos = AtomicLong(0L)

        val isFinished = AtomicBoolean(false)
        var helperBatteryAfter = helperBatteryBefore
        var helperChargingAfter = helperChargingBefore

        try {
            coroutineScope {
                // Pipeline 1: Coordinator local HTTP downloader (WiFi)
                val coordJob: Job = launch(Dispatchers.IO) {
                    while (!isFinished.get() && !isCancelled && isActive) {
                        val chunk = workQueue.poll() ?: break

                        try {
                            val bytes = downloadChunk(client, fileUrl, chunk.startByte, chunk.endByte, chunk.id, maxRetries = 3)

                            fileMutex.withLock {
                                raf.seek(chunk.startByte)
                                raf.write(bytes)
                            }

                            bytesFromCoordinator.addAndGet(bytes.size.toLong())
                            chunksFromCoordinator.incrementAndGet()
                        } catch (e: Exception) {
                            chunk.retries++
                            chunkRetries.incrementAndGet()
                            if (chunk.retries <= 3) {
                                workQueue.add(chunk) // Put back for retry
                            } else {
                                throw Exception("Chunk ${chunk.id} failed after 3 retries: ${e.message}")
                            }
                        }
                    }
                }

                // Pipeline 2: Helper Dispatcher & P2P Stream Receiver (Cellular -> Wi-Fi Direct)
                val helperJob: Job = launch(Dispatchers.IO) {
                    while (!isFinished.get() && !isCancelled && isActive) {
                        // Read message from Helper
                        val op = try {
                            dis.readInt()
                        } catch (e: Exception) {
                            if (workQueue.isEmpty() && coordJob.isCompleted) break
                            throw e
                        }

                        when (op) {
                            OP_REQUEST_CHUNK -> {
                                val chunk = workQueue.poll()
                                if (chunk != null) {
                                    dos.writeInt(OP_ASSIGN_CHUNK)
                                    dos.writeInt(chunk.id)
                                    dos.writeLong(chunk.startByte)
                                    dos.writeLong(chunk.endByte)
                                    dos.flush()
                                } else {
                                    // No more chunks, notify Helper
                                    dos.writeInt(OP_ALL_CHUNKS_COMPLETE)
                                    dos.flush()
                                }
                            }

                            OP_CHUNK_PAYLOAD -> {
                                val p2pTransferStart = SystemClock.elapsedRealtimeNanos()
                                val cId = dis.readInt()
                                val startByte = dis.readLong()
                                val endByte = dis.readLong()
                                val dataLength = dis.readInt()

                                val chunkBuffer = ByteArray(dataLength)
                                dis.readFully(chunkBuffer)

                                val p2pTransferEnd = SystemClock.elapsedRealtimeNanos()
                                localTransferNanos.addAndGet(p2pTransferEnd - p2pTransferStart)

                                fileMutex.withLock {
                                    raf.seek(startByte)
                                    raf.write(chunkBuffer)
                                }

                                bytesFromHelper.addAndGet(dataLength.toLong())
                                chunksFromHelper.incrementAndGet()

                                // Immediately ask if another chunk is needed
                                val nextChunk = workQueue.poll()
                                if (nextChunk != null) {
                                    dos.writeInt(OP_ASSIGN_CHUNK)
                                    dos.writeInt(nextChunk.id)
                                    dos.writeLong(nextChunk.startByte)
                                    dos.writeLong(nextChunk.endByte)
                                    dos.flush()
                                } else {
                                    dos.writeInt(OP_ALL_CHUNKS_COMPLETE)
                                    dos.flush()
                                }
                            }

                            OP_CHUNK_FAILED -> {
                                val failedId = dis.readInt()
                                val startByte = dis.readLong()
                                val endByte = dis.readLong()
                                val retryCount = dis.readInt()

                                chunkRetries.incrementAndGet()
                                if (retryCount <= 3) {
                                    workQueue.add(ChunkWork(failedId, startByte, endByte, retryCount))
                                }

                                // Send next available chunk if any
                                val nextChunk = workQueue.poll()
                                if (nextChunk != null) {
                                    dos.writeInt(OP_ASSIGN_CHUNK)
                                    dos.writeInt(nextChunk.id)
                                    dos.writeLong(nextChunk.startByte)
                                    dos.writeLong(nextChunk.endByte)
                                    dos.flush()
                                } else {
                                    dos.writeInt(OP_ALL_CHUNKS_COMPLETE)
                                    dos.flush()
                                }
                            }

                            OP_HELPER_FINAL_STATS -> {
                                helperBatteryAfter = dis.readInt()
                                helperChargingAfter = dis.readBoolean()
                                break
                            }
                        }

                        // Check if all chunks completed
                        val totalWritten = bytesFromCoordinator.get() + bytesFromHelper.get()
                        if (totalWritten >= fileSize) {
                            isFinished.set(true)
                            dos.writeInt(OP_ALL_CHUNKS_COMPLETE)
                            dos.flush()
                        }
                    }
                }

                // UI Progress Emitter
                launch(Dispatchers.IO) {
                    var lastEmit = startClock
                    while (!isFinished.get() && !isCancelled && isActive) {
                        val now = SystemClock.elapsedRealtime()
                        val elapsedSec = maxOf((now - startClock) / 1000.0, 0.001)
                        val totalBytes = bytesFromCoordinator.get() + bytesFromHelper.get()
                        val speedMbps = (totalBytes * 8.0) / (elapsedSec * 1e6)

                        onProgress(
                            CooperativeProgress(
                                bytesTotalExpected = fileSize,
                                bytesFromCoordinator = bytesFromCoordinator.get(),
                                bytesFromHelper = bytesFromHelper.get(),
                                chunksTotal = totalChunks,
                                chunksFromCoordinator = chunksFromCoordinator.get(),
                                chunksFromHelper = chunksFromHelper.get(),
                                elapsedSeconds = elapsedSec,
                                currentThroughputMbps = speedMbps,
                                statusText = "Pooled Download: ${chunksFromCoordinator.get() + chunksFromHelper.get()} of $totalChunks chunks (${String.format(Locale.US, "%.1f", speedMbps)} Mbps)..."
                            )
                        )
                        kotlinx.coroutines.delay(100)
                    }
                }

                coordJob.join()
                helperJob.join()
            }

            val endClock = SystemClock.elapsedRealtime()
            val totalTimeSec = maxOf((endClock - startClock) / 1000.0, 0.001)
            val effectiveMbps = (fileSize * 8.0) / (totalTimeSec * 1e6)
            val batteryAfter = networkMeter.getBatteryPercentage()

            val localTransferSec = maxOf(localTransferNanos.get() / 1e9, 0.001)
            val localMbps = (bytesFromHelper.get() * 8.0) / (localTransferSec * 1e6)

            // Compute SHA-256 and check match against Solo
            raf.close()
            val pooledSha256 = computeSha256(tempFile)
            tempFile.delete()

            val soloHash = soloHashes[fileUrl]
            val hashMatchResult = when {
                soloHash == null -> "No Solo Run Yet (${pooledSha256.take(8)})"
                soloHash.equals(pooledSha256, ignoreCase = true) -> "Match"
                else -> "Mismatch"
            }

            val runResult = CooperativeRunResult(
                timestamp = startTimeWall,
                mode = "Pooled",
                fileUrl = fileUrl,
                fileSizeBytes = fileSize,
                chunkSizeBytes = chunkSizeBytes,
                coordinatorNetwork = coordinatorNetType,
                helperNetwork = helperNetwork,
                coordinatorDevice = coordinatorDevice,
                helperDevice = helperDevice,
                totalTimeSeconds = totalTimeSec,
                effectiveThroughputMbps = effectiveMbps,
                bytesFromCoordinatorLink = bytesFromCoordinator.get(),
                bytesFromHelperLink = bytesFromHelper.get(),
                chunksTotal = totalChunks,
                chunksFromCoordinator = chunksFromCoordinator.get(),
                chunksFromHelper = chunksFromHelper.get(),
                chunkRetries = chunkRetries.get(),
                localTransferTotalSeconds = localTransferSec,
                localThroughputMbps = localMbps,
                hashMatch = hashMatchResult,
                batteryBeforeCoordinator = batteryBefore,
                batteryAfterCoordinator = batteryAfter,
                batteryBeforeHelper = helperBatteryBefore,
                batteryAfterHelper = helperBatteryAfter,
                isChargingCoordinator = isChargingAtStart,
                isChargingHelper = helperChargingBefore,
                testConditionNote = conditionNote,
                batchId = batchId,
                runNumberInBatch = runLabel,
                status = "OK"
            )

            logManager.appendRun(runResult)
            Result.success(runResult)
        } catch (e: Exception) {
            raf.close()
            tempFile.delete()
            Result.failure(e)
        } finally {
            closeAll()
        }
    }

    // =========================================================================
    // 3. POOLED DOWNLOAD: HELPER WORKER (Runs on Phone B)
    // =========================================================================
    suspend fun executeHelperWorker(
        isGroupOwner: Boolean,
        groupOwnerAddress: String,
        onProgress: (String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        isCancelled = false

        // Helper requests and holds active cellular network
        bindingHelper.acquireCellularNetwork()
        val cellularNet = bindingHelper.cellularNetwork.value
        val client = bindingHelper.createBoundClient(cellularNet)
        val cellularLabel = bindingHelper.getActiveNetworkLabel(cellularNet)

        // Symmetrical P2P socket establishment: Group Owner listens; Client connects to GO
        val socket: Socket = if (isGroupOwner) {
            onProgress("Helper is Group Owner: waiting on port $COOP_PORT for Coordinator...")
            val serverSocket = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(COOP_PORT))
                soTimeout = 45000
            }
            activeServerSocket = serverSocket
            try {
                serverSocket.accept()
            } catch (e: Exception) {
                serverSocket.close()
                bindingHelper.releaseCellularNetwork()
                return@withContext Result.failure(Exception("Timeout waiting for Coordinator to connect: ${e.message}"))
            } finally {
                serverSocket.close()
            }
        } else {
            val targetIp = groupOwnerAddress.ifBlank { "192.168.49.1" }
            onProgress("Connecting to Coordinator ($targetIp:$COOP_PORT)...")
            try {
                connectWithRetry(targetIp, COOP_PORT, maxAttempts = 30)
            } catch (e: Exception) {
                bindingHelper.releaseCellularNetwork()
                return@withContext Result.failure(Exception("Could not connect to Coordinator ($targetIp:$COOP_PORT): ${e.message}"))
            }
        }
        activeSocket = socket
        socket.tcpNoDelay = true
        socket.soTimeout = SOCKET_TIMEOUT_MS

        val dis = DataInputStream(BufferedInputStream(socket.getInputStream()))
        val dos = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))

        val batteryBefore = networkMeter.getBatteryPercentage()
        val isChargingBefore = networkMeter.isDeviceCharging()
        val helperDevice = networkMeter.getDeviceDescription()

        // Send Hello Handshake
        dos.writeInt(OP_HELPER_HELLO)
        dos.writeInt(batteryBefore)
        dos.writeBoolean(isChargingBefore)
        dos.writeUTF(helperDevice)
        dos.writeUTF(cellularLabel)
        dos.flush()

        onProgress("Connected! Waiting for session parameters from Coordinator...")

        // Read session parameters from Coordinator
        val startOp = dis.readInt()
        if (startOp != OP_COORDINATOR_START) {
            socket.close()
            bindingHelper.releaseCellularNetwork()
            return@withContext Result.failure(Exception("Expected OP_COORDINATOR_START."))
        }
        val fileUrl = dis.readUTF()
        val fileSize = dis.readLong()
        val chunkSizeBytes = dis.readLong()

        onProgress("Pooled session started: $fileUrl (${fileSize / (1024 * 1024)} MB). Requesting chunks...")

        // Pipelined Queue:
        // Helper downloads chunk through cellular, puts into channel, while P2P sender pumps it over socket!
        val payloadChannel = Channel<HelperChunkPayload>(capacity = 4)
        val isSessionActive = AtomicBoolean(true)

        try {
            coroutineScope {
                // Task A: Socket Writer & Dispatcher
                launch(Dispatchers.IO) {
                    // Send initial chunk request to Coordinator
                    dos.writeInt(OP_REQUEST_CHUNK)
                    dos.flush()

                    for (payload in payloadChannel) {
                        if (!isSessionActive.get() || isCancelled) break

                        dos.writeInt(OP_CHUNK_PAYLOAD)
                        dos.writeInt(payload.chunkId)
                        dos.writeLong(payload.startByte)
                        dos.writeLong(payload.endByte)
                        dos.writeInt(payload.data.size)
                        dos.write(payload.data)
                        dos.flush()
                    }
                }

                // Task B: Receiver & Cellular Downloader
                launch(Dispatchers.IO) {
                    while (isSessionActive.get() && !isCancelled && isActive) {
                        val op = try {
                            dis.readInt()
                        } catch (e: Exception) {
                            break
                        }

                        when (op) {
                            OP_ASSIGN_CHUNK -> {
                                val chunkId = dis.readInt()
                                val startByte = dis.readLong()
                                val endByte = dis.readLong()

                                onProgress("Downloading chunk $chunkId (${startByte}-${endByte}) via Cellular...")

                                try {
                                    val bytes = downloadChunk(client, fileUrl, startByte, endByte, chunkId, maxRetries = 3)

                                    // Pipeline: send to P2P socket channel
                                    payloadChannel.send(HelperChunkPayload(chunkId, startByte, endByte, bytes))
                                } catch (e: Exception) {
                                    // Report failure to Coordinator so it re-enqueues
                                    dos.writeInt(OP_CHUNK_FAILED)
                                    dos.writeInt(chunkId)
                                    dos.writeLong(startByte)
                                    dos.writeLong(endByte)
                                    dos.writeInt(1)
                                    dos.flush()
                                }
                            }

                            OP_ALL_CHUNKS_COMPLETE -> {
                                onProgress("All chunks completed by group! Finalizing...")
                                isSessionActive.set(false)
                                payloadChannel.close()

                                val batteryAfter = networkMeter.getBatteryPercentage()
                                val isChargingAfter = networkMeter.isDeviceCharging()

                                dos.writeInt(OP_HELPER_FINAL_STATS)
                                dos.writeInt(batteryAfter)
                                dos.writeBoolean(isChargingAfter)
                                dos.flush()
                                break
                            }
                        }
                    }
                }
            }

            onProgress("Pooled benchmark finished successfully!")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            isSessionActive.set(false)
            payloadChannel.close()
            closeAll()
            bindingHelper.releaseCellularNetwork()
        }
    }

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { stream ->
            val buffer = ByteArray(64 * 1024)
            var read: Int
            while (stream.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun closeAll() {
        try {
            activeSocket?.close()
        } catch (_: Exception) {}
        activeSocket = null

        try {
            activeServerSocket?.close()
        } catch (_: Exception) {}
        activeServerSocket = null
    }
}
