package com.example.wifidirect

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.zip.CRC32
import kotlin.coroutines.coroutineContext

data class WifiDirectTransferProgress(
    val bytesTransferred: Long,
    val totalBytesExpected: Long,
    val elapsedSeconds: Double,
    val currentSpeedMbps: Double
)

data class TransferExecutionResult(
    val payloadSizeBytes: Long,
    val bytesTransferred: Long,
    val transferTimeSeconds: Double,
    val localThroughputMbps: Double,
    val bytesReceivedOk: Boolean,
    val status: String
)

class WifiDirectTransferEngine {

    companion object {
        const val P2P_PORT = 8988
        const val SOCKET_TIMEOUT_MS = 25000 // 25s socket read timeout
        const val OP_CLIENT_TO_COORDINATOR = 1
        const val OP_COORDINATOR_TO_CLIENT = 2
        const val CHUNK_SIZE = 64 * 1024 // 64 KB streaming buffer
    }

    @Volatile
    private var activeServerSocket: ServerSocket? = null

    @Volatile
    private var activeSocket: Socket? = null

    @Volatile
    private var isCancelled = false

    fun cancelActiveTransfer() {
        isCancelled = true
        try {
            activeSocket?.close()
        } catch (_: Exception) {}
        try {
            activeServerSocket?.close()
        } catch (_: Exception) {}
    }

    /**
     * Executes local P2P transfer session between Coordinator and Client.
     *
     * @param isGroupOwner Whether this phone is the Wi-Fi Direct Coordinator (Group Owner)
     * @param groupOwnerAddress The IP of the Coordinator (typically 192.168.49.1)
     * @param isSender Whether this phone is selected as the "Sender" (true) or "Receiver" (false)
     * @param payloadSizeBytes Desired payload size (e.g. 10 MB, 50 MB, 100 MB)
     * @param onProgress Callback invoked with real-time transfer progress
     */
    suspend fun executeTransfer(
        isGroupOwner: Boolean,
        groupOwnerAddress: String,
        isSender: Boolean,
        payloadSizeBytes: Long,
        onProgress: (WifiDirectTransferProgress) -> Unit
    ): Result<TransferExecutionResult> = withContext(Dispatchers.IO) {
        isCancelled = false
        val startClock = SystemClock.elapsedRealtime()

        try {
            if (isGroupOwner) {
                // Coordinator always opens ServerSocket and listens for Client connection
                executeAsCoordinator(
                    isSender = isSender,
                    payloadSizeBytes = payloadSizeBytes,
                    startClock = startClock,
                    onProgress = onProgress
                )
            } else {
                // Client always connects to Coordinator's known IP address
                executeAsClient(
                    coordinatorIp = groupOwnerAddress,
                    isSender = isSender,
                    payloadSizeBytes = payloadSizeBytes,
                    startClock = startClock,
                    onProgress = onProgress
                )
            }
        } catch (e: Exception) {
            val endClock = SystemClock.elapsedRealtime()
            val elapsedSec = maxOf((endClock - startClock) / 1000.0, 0.001)
            val errorMsg = if (isCancelled) "Transfer cancelled by user." else (e.localizedMessage ?: "Socket transfer error")

            Result.failure(Exception(errorMsg))
        } finally {
            closeAllSockets()
        }
    }

    private suspend fun executeAsCoordinator(
        isSender: Boolean,
        payloadSizeBytes: Long,
        startClock: Long,
        onProgress: (WifiDirectTransferProgress) -> Unit
    ): Result<TransferExecutionResult> {
        val serverSocket = ServerSocket().apply {
            reuseAddress = true
            bind(InetSocketAddress(P2P_PORT))
            soTimeout = SOCKET_TIMEOUT_MS
        }
        activeServerSocket = serverSocket

        val socket = try {
            serverSocket.accept()
        } catch (e: IOException) {
            if (isCancelled) throw Exception("Transfer cancelled while waiting for peer.")
            throw Exception("Timeout waiting for peer connection. Make sure other phone is on Receive/Send.")
        }
        activeSocket = socket
        socket.soTimeout = SOCKET_TIMEOUT_MS
        socket.tcpNoDelay = true

        val dis = DataInputStream(BufferedInputStream(socket.getInputStream()))
        val dos = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))

        return if (isSender) {
            // Coordinator is SENDER: Send OP_COORDINATOR_TO_CLIENT header and stream payload to client
            dos.writeInt(OP_COORDINATOR_TO_CLIENT)
            dos.writeLong(payloadSizeBytes)
            dos.flush()

            streamPayloadOut(dos, payloadSizeBytes, startClock, onProgress)
        } else {
            // Coordinator is RECEIVER: Read header from client
            val op = dis.readInt()
            val expectedSize = dis.readLong()

            streamPayloadIn(dis, expectedSize, startClock, onProgress)
        }
    }

    private suspend fun executeAsClient(
        coordinatorIp: String,
        isSender: Boolean,
        payloadSizeBytes: Long,
        startClock: Long,
        onProgress: (WifiDirectTransferProgress) -> Unit
    ): Result<TransferExecutionResult> {
        if (coordinatorIp.isBlank()) {
            throw Exception("Coordinator IP address is unknown. Reconnect Wi-Fi Direct.")
        }

        val socket = Socket()
        activeSocket = socket
        socket.tcpNoDelay = true

        try {
            socket.connect(InetSocketAddress(coordinatorIp, P2P_PORT), 15000)
        } catch (e: Exception) {
            if (isCancelled) throw Exception("Transfer cancelled by user.")
            throw Exception("Could not connect to Coordinator ($coordinatorIp:$P2P_PORT). Ensure Coordinator is ready.")
        }
        socket.soTimeout = SOCKET_TIMEOUT_MS

        val dis = DataInputStream(BufferedInputStream(socket.getInputStream()))
        val dos = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))

        return if (isSender) {
            // Client is SENDER: Write OP_CLIENT_TO_COORDINATOR header and stream payload
            dos.writeInt(OP_CLIENT_TO_COORDINATOR)
            dos.writeLong(payloadSizeBytes)
            dos.flush()

            streamPayloadOut(dos, payloadSizeBytes, startClock, onProgress)
        } else {
            // Client is RECEIVER: Read OP_COORDINATOR_TO_CLIENT header and read stream
            val op = dis.readInt()
            val expectedSize = dis.readLong()

            streamPayloadIn(dis, expectedSize, startClock, onProgress)
        }
    }

    /**
     * Streams generated in-memory bytes to peer (zero storage writes, zero internet).
     */
    private suspend fun streamPayloadOut(
        dos: DataOutputStream,
        totalBytesToSend: Long,
        startClock: Long,
        onProgress: (WifiDirectTransferProgress) -> Unit
    ): Result<TransferExecutionResult> {
        val buffer = ByteArray(CHUNK_SIZE)
        // Fill pattern bytes for non-zero in-memory payload
        for (i in buffer.indices) {
            buffer[i] = (i % 256).toByte()
        }

        val crc = CRC32()
        var bytesSent = 0L
        var lastEmit = startClock

        while (bytesSent < totalBytesToSend) {
            if (isCancelled || !coroutineContext.isActive) {
                throw Exception("Transfer cancelled by user.")
            }

            val remaining = totalBytesToSend - bytesSent
            val chunkSize = minOf(CHUNK_SIZE.toLong(), remaining).toInt()

            dos.write(buffer, 0, chunkSize)
            crc.update(buffer, 0, chunkSize)
            bytesSent += chunkSize

            val now = SystemClock.elapsedRealtime()
            if (now - lastEmit >= 100 || bytesSent == totalBytesToSend) {
                val elapsedSec = maxOf((now - startClock) / 1000.0, 0.001)
                val currentSpeedMbps = (bytesSent * 8.0) / (elapsedSec * 1_000_000.0)
                onProgress(
                    WifiDirectTransferProgress(
                        bytesTransferred = bytesSent,
                        totalBytesExpected = totalBytesToSend,
                        elapsedSeconds = elapsedSec,
                        currentSpeedMbps = currentSpeedMbps
                    )
                )
                lastEmit = now
            }
        }
        dos.flush()

        // Write CRC32 checksum at the end
        dos.writeLong(crc.value)
        dos.flush()

        val endClock = SystemClock.elapsedRealtime()
        val totalSec = maxOf((endClock - startClock) / 1000.0, 0.001)
        val throughputMbps = (bytesSent * 8.0) / (totalSec * 1_000_000.0)

        return Result.success(
            TransferExecutionResult(
                payloadSizeBytes = totalBytesToSend,
                bytesTransferred = bytesSent,
                transferTimeSeconds = totalSec,
                localThroughputMbps = throughputMbps,
                bytesReceivedOk = true,
                status = "OK"
            )
        )
    }

    /**
     * Reads in-memory bytes from peer and validates byte count and CRC32.
     */
    private suspend fun streamPayloadIn(
        dis: DataInputStream,
        expectedBytes: Long,
        startClock: Long,
        onProgress: (WifiDirectTransferProgress) -> Unit
    ): Result<TransferExecutionResult> {
        val buffer = ByteArray(CHUNK_SIZE)
        val crc = CRC32()
        var bytesReadTotal = 0L
        var lastEmit = startClock

        while (bytesReadTotal < expectedBytes) {
            if (isCancelled || !coroutineContext.isActive) {
                throw Exception("Transfer cancelled by user.")
            }

            val remaining = expectedBytes - bytesReadTotal
            val toRead = minOf(CHUNK_SIZE.toLong(), remaining).toInt()
            val read = dis.read(buffer, 0, toRead)
            if (read == -1) break

            crc.update(buffer, 0, read)
            bytesReadTotal += read

            val now = SystemClock.elapsedRealtime()
            if (now - lastEmit >= 100 || bytesReadTotal == expectedBytes) {
                val elapsedSec = maxOf((now - startClock) / 1000.0, 0.001)
                val currentSpeedMbps = (bytesReadTotal * 8.0) / (elapsedSec * 1_000_000.0)
                onProgress(
                    WifiDirectTransferProgress(
                        bytesTransferred = bytesReadTotal,
                        totalBytesExpected = expectedBytes,
                        elapsedSeconds = elapsedSec,
                        currentSpeedMbps = currentSpeedMbps
                    )
                )
                lastEmit = now
            }
        }

        // Read peer CRC32 checksum
        val peerCrc = try {
            dis.readLong()
        } catch (_: Exception) {
            -1L
        }

        val endClock = SystemClock.elapsedRealtime()
        val totalSec = maxOf((endClock - startClock) / 1000.0, 0.001)
        val throughputMbps = (bytesReadTotal * 8.0) / (totalSec * 1_000_000.0)

        val sizeMatches = bytesReadTotal == expectedBytes
        val crcMatches = peerCrc != -1L && peerCrc == crc.value
        val isOk = sizeMatches && crcMatches

        val statusMsg = when {
            isOk -> "OK"
            !sizeMatches -> "FAILED: Size mismatch ($bytesReadTotal / $expectedBytes bytes)"
            else -> "FAILED: CRC checksum mismatch"
        }

        return Result.success(
            TransferExecutionResult(
                payloadSizeBytes = expectedBytes,
                bytesTransferred = bytesReadTotal,
                transferTimeSeconds = totalSec,
                localThroughputMbps = throughputMbps,
                bytesReceivedOk = isOk,
                status = statusMsg
            )
        )
    }

    private fun closeAllSockets() {
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
