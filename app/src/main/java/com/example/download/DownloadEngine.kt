package com.example.download

import android.os.SystemClock
import com.example.data.TestLogManager
import com.example.model.TestRunResult
import com.example.network.NetworkMeter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.net.InetAddress
import java.net.UnknownHostException
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

private class RobustDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        try {
            val addresses = Dns.SYSTEM.lookup(hostname)
            if (addresses.isNotEmpty()) {
                return addresses
            }
        } catch (_: Exception) {
            // Fall through to fallback
        }

        // Hardcoded Anycast IPs if carrier/local DNS lookup fails
        return when {
            hostname.equals("speed.cloudflare.com", ignoreCase = true) -> {
                listOf(
                    InetAddress.getByName("104.16.123.96"),
                    InetAddress.getByName("104.16.124.96")
                )
            }
            hostname.contains("hetzner", ignoreCase = true) -> {
                listOf(InetAddress.getByName("78.46.170.2"))
            }
            else -> throw UnknownHostException("Unable to resolve host \"$hostname\": check your device internet connection.")
        }
    }
}

class DownloadEngine(
    private val networkMeter: NetworkMeter,
    private val logManager: TestLogManager
) {
    private val client = OkHttpClient.Builder()
        .dns(RobustDns())
        .retryOnConnectionFailure(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun runBenchmark(
        fileUrl: String,
        testConditionNote: String = "",
        batchId: String = "",
        runNumberInBatch: String = "",
        onProgress: (DownloadProgress) -> Unit
    ): Result<TestRunResult> = withContext(Dispatchers.IO) {
        try {
            // Resolve direct download URL if this is a MediaFire share page
            val directUrl = resolveDirectUrlIfNeeded(fileUrl.trim())

            if (!directUrl.startsWith("http://", ignoreCase = true) && !directUrl.startsWith("https://", ignoreCase = true)) {
                return@withContext Result.failure(
                    Exception("Invalid URL scheme: must start with http:// or https:// ($directUrl)")
                )
            }

            // Snapshot initial device metrics
            val networkTypeAtStart = networkMeter.getNetworkType()
            val signalStrengthAtStart = networkMeter.getSignalStrength()
            val batteryBefore = networkMeter.getBatteryPercentage()

            val request = Request.Builder()
                .url(directUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) COMBINE-Benchmark/1.0")
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

            val contentType = response.header("Content-Type").orEmpty().lowercase()
            val body = response.body
                ?: return@withContext Result.failure(Exception("Response body is empty"))

            val contentLength = body.contentLength()

            // Guard against accidentally benchmarking an HTML web page
            if (contentType.contains("text/html") && contentLength in 0..1000000) {
                body.close()
                return@withContext Result.failure(
                    Exception("The URL returned an HTML web page instead of the actual file. Please verify it is a direct download link.")
                )
            }

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
                batteryAfter = batteryAfter,
                testConditionNote = testConditionNote,
                batchId = batchId,
                runNumberInBatch = runNumberInBatch
            )

            // Persist to CSV
            logManager.appendTestRun(testResult)

            Result.success(testResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun resolveDirectUrlIfNeeded(url: String): String {
        var cleanUrl = url.trim()

        // Remap discontinued speed.hetzner.de URLs to working endpoints
        if (cleanUrl.contains("speed.hetzner.de/10MB.bin", ignoreCase = true)) {
            return "https://speed.cloudflare.com/__down?bytes=10000000"
        }
        if (cleanUrl.contains("speed.hetzner.de/100MB.bin", ignoreCase = true)) {
            return "https://fsn1-speed.hetzner.com/100MB.bin"
        }
        if (cleanUrl.contains("speed.hetzner.de/1GB.bin", ignoreCase = true)) {
            return "https://fsn1-speed.hetzner.com/1GB.bin"
        }

        if (!cleanUrl.contains("mediafire.com/file/", ignoreCase = true)) {
            return normalizeUrl(cleanUrl)
        }

        return try {
            val pageRequest = Request.Builder()
                .url(cleanUrl)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
                )
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            client.newCall(pageRequest).execute().use { response ->
                if (!response.isSuccessful) return cleanUrl
                val html = response.body?.string().orEmpty()

                // 1. Direct mediafire download subdomain (https://downloadXXXX.mediafire.com/...)
                val regexDirect = Regex(
                    """href=["'](https?://download[^"']*mediafire\.com/[^"']+)["']""",
                    RegexOption.IGNORE_CASE
                )
                val matchDirect = regexDirect.find(html)
                if (matchDirect != null) {
                    return normalizeUrl(matchDirect.groupValues[1])
                }

                // 2. Download button anchor: id="downloadButton"
                val regexBtn1 = Regex(
                    """id=["']downloadButton["'][^>]*href=["']([^"']+)["']""",
                    RegexOption.IGNORE_CASE
                )
                val matchBtn1 = regexBtn1.find(html)
                if (matchBtn1 != null) {
                    val raw = matchBtn1.groupValues[1].trim()
                    if (raw.isNotBlank() && raw != "#") {
                        return normalizeUrl(raw)
                    }
                }

                val regexBtn2 = Regex(
                    """href=["']([^"']+)["'][^>]*id=["']downloadButton["']""",
                    RegexOption.IGNORE_CASE
                )
                val matchBtn2 = regexBtn2.find(html)
                if (matchBtn2 != null) {
                    val raw = matchBtn2.groupValues[1].trim()
                    if (raw.isNotBlank() && raw != "#") {
                        return normalizeUrl(raw)
                    }
                }

                // 3. Aria-label anchor
                val regexAria = Regex(
                    """aria-label=["']Download file["'][^>]*href=["']([^"']+)["']""",
                    RegexOption.IGNORE_CASE
                )
                val matchAria = regexAria.find(html)
                if (matchAria != null) {
                    val raw = matchAria.groupValues[1].trim()
                    if (raw.isNotBlank() && raw != "#") {
                        return normalizeUrl(raw)
                    }
                }

                cleanUrl
            }
        } catch (_: Exception) {
            cleanUrl
        }
    }

    private fun normalizeUrl(url: String): String {
        var u = url.trim()
        if (u.startsWith("//")) {
            u = "https:$u"
        } else if (u.startsWith("/")) {
            u = "https://www.mediafire.com$u"
        }
        return u
    }
}
