package com.example.download

import android.os.SystemClock
import com.example.data.TestLogManager
import com.example.model.TestRunResult
import com.example.network.NetworkMeter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytesExpected: Long, // -1 if unknown / chunked
    val elapsedSeconds: Double,
    val currentSpeedKbps: Double
)

class DownloadEngine(
    private val networkMeter: NetworkMeter,
    private val logManager: TestLogManager
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun runBenchmark(
        fileUrl: String,
        onProgress: (DownloadProgress) -> Unit
    ): Result<TestRunResult> = withContext(Dispatchers.IO) {
        try {
            // Snapshot initial device metrics
            val networkTypeAtStart = networkMeter.getNetworkType()
            val signalStrengthAtStart = networkMeter.getSignalStrength()
            val batteryBefore = networkMeter.getBatteryPercentage()

            val request = Request.Builder()
                .url(fileUrl)
                .header("User-Agent", "COMBINE-Benchmark/1.0 (Android Academic Research)")
                .build()

            val startTimeWall = System.currentTimeMillis()
            val startClock = SystemClock.elapsedRealtime()

            val call = client.newCall(request)
            val response = call.execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("HTTP ${response.code}: ${response.message}")
                )
            }

            val body = response.body
                ?: return@withContext Result.failure(Exception("Response body is empty"))

            val contentLength = body.contentLength()
            var totalBytesRead = 0L
            val buffer = ByteArray(64 * 1024) // 64 KB buffer

            val inputStream: InputStream = body.byteStream()
            var lastProgressEmit = startClock

            inputStream.use { stream ->
                while (true) {
                    coroutineContext.ensureActive()
                    val read = stream.read(buffer)
                    if (read == -1) break
                    totalBytesRead += read

                    val now = SystemClock.elapsedRealtime()
                    // Throttle UI progress updates to every 100ms
                    if (now - lastProgressEmit >= 100) {
                        val elapsedSec = (now - startClock) / 1000.0
                        val currentSpeed = if (elapsedSec > 0) {
                            ((totalBytesRead * 8.0) / 1000.0) / elapsedSec
                        } else 0.0

                        onProgress(
                            DownloadProgress(
                                bytesDownloaded = totalBytesRead,
                                totalBytesExpected = contentLength,
                                elapsedSeconds = elapsedSec,
                                currentSpeedKbps = currentSpeed
                            )
                        )
                        lastProgressEmit = now
                    }
                }
            }

            val endClock = SystemClock.elapsedRealtime()
            val totalTimeSeconds = maxOf((endClock - startClock) / 1000.0, 0.001)
            val batteryAfter = networkMeter.getBatteryPercentage()

            // Throughput in Kbps = (bytes * 8 bits / 1000) / totalTimeSeconds
            val throughputKbps = ((totalBytesRead * 8.0) / 1000.0) / totalTimeSeconds

            val timestampStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(startTimeWall))

            val testResult = TestRunResult(
                timestamp = timestampStr,
                fileUrl = fileUrl,
                fileSizeBytes = totalBytesRead,
                networkType = networkTypeAtStart,
                signalStrength = signalStrengthAtStart,
                totalTimeSeconds = totalTimeSeconds,
                throughputKbps = throughputKbps,
                batteryBefore = batteryBefore,
                batteryAfter = batteryAfter
            )

            // Persist to CSV
            logManager.appendTestRun(testResult)

            Result.success(testResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
