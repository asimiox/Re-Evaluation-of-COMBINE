package com.example.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.model.WifiDirectRunResult
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages persisting Wi-Fi Direct peer-to-peer benchmark runs to wifi_direct_logs.csv
 * and sharing the CSV via Android FileProvider.
 *
 * Schema:
 * timestamp,role,device_model,peer_device_name,payload_size_bytes,discovery_time_ms,connection_time_ms,transfer_time_seconds,local_throughput_mbps,bytes_received_ok,connected_to_wifi_router,p2p_frequency_mhz,battery_before,battery_after,is_charging,test_condition_note,batch_id,run_number_in_batch,status
 */
class WifiDirectLogManager(private val context: Context) {

    private val csvFileName = "wifi_direct_logs.csv"
    val csvHeader = "timestamp,role,device_model,peer_device_name,payload_size_bytes,discovery_time_ms,connection_time_ms,transfer_time_seconds,local_throughput_mbps,bytes_received_ok,connected_to_wifi_router,p2p_frequency_mhz,battery_before,battery_after,is_charging,test_condition_note,batch_id,run_number_in_batch,status"

    private val logFile: File
        get() {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            return File(dir, csvFileName)
        }

    init {
        ensureHeaderExists()
    }

    private fun ensureHeaderExists() {
        val file = logFile
        if (!file.exists() || file.length() == 0L) {
            file.parentFile?.mkdirs()
            file.writeText("$csvHeader\n")
        }
    }

    @Synchronized
    fun appendRun(result: WifiDirectRunResult) {
        ensureHeaderExists()
        val line = buildString {
            append(escapeCsv(result.timestamp)).append(",")
            append(escapeCsv(result.role)).append(",")
            append(escapeCsv(result.deviceModel)).append(",")
            append(escapeCsv(result.peerDeviceName)).append(",")
            append(result.payloadSizeBytes).append(",")
            append(result.discoveryTimeMs).append(",")
            append(result.connectionTimeMs).append(",")
            append(String.format(Locale.US, "%.3f", result.transferTimeSeconds)).append(",")
            append(String.format(Locale.US, "%.2f", result.localThroughputMbps)).append(",")
            append(result.bytesReceivedOk).append(",")
            append(result.connectedToWifiRouter).append(",")
            append(escapeCsv(result.p2pFrequencyMhz)).append(",")
            append(result.batteryBefore).append(",")
            append(result.batteryAfter).append(",")
            append(result.isCharging).append(",")
            append(escapeCsv(result.testConditionNote)).append(",")
            append(escapeCsv(result.batchId)).append(",")
            append(escapeCsv(result.runNumberInBatch)).append(",")
            append(escapeCsv(result.status)).append("\n")
        }

        FileWriter(logFile, true).use { writer ->
            writer.write(line)
        }
    }

    @Synchronized
    fun readAllRuns(): List<WifiDirectRunResult> {
        val file = logFile
        if (!file.exists()) return emptyList()

        val results = mutableListOf<WifiDirectRunResult>()
        val lines = file.readLines()
        if (lines.size <= 1) return emptyList()

        for (line in lines.drop(1)) {
            if (line.isBlank()) continue
            val tokens = parseCsvLine(line)
            if (tokens.size >= 19) {
                try {
                    results.add(
                        WifiDirectRunResult(
                            timestamp = tokens[0],
                            role = tokens[1],
                            deviceModel = tokens[2],
                            peerDeviceName = tokens[3],
                            payloadSizeBytes = tokens[4].toLongOrNull() ?: 0L,
                            discoveryTimeMs = tokens[5].toLongOrNull() ?: 0L,
                            connectionTimeMs = tokens[6].toLongOrNull() ?: 0L,
                            transferTimeSeconds = tokens[7].toDoubleOrNull() ?: 0.0,
                            localThroughputMbps = tokens[8].toDoubleOrNull() ?: 0.0,
                            bytesReceivedOk = tokens[9].equals("true", ignoreCase = true),
                            connectedToWifiRouter = tokens[10].equals("true", ignoreCase = true),
                            p2pFrequencyMhz = tokens[11],
                            batteryBefore = tokens[12].toIntOrNull() ?: 0,
                            batteryAfter = tokens[13].toIntOrNull() ?: 0,
                            isCharging = tokens[14].equals("true", ignoreCase = true),
                            testConditionNote = tokens[15],
                            batchId = tokens[16],
                            runNumberInBatch = tokens[17],
                            status = tokens[18]
                        )
                    )
                } catch (_: Exception) {
                    // Skip malformed entries
                }
            }
        }
        return results
    }

    fun shareCsv() {
        val file = logFile
        if (!file.exists() || file.length() == 0L) {
            ensureHeaderExists()
        }

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(
                Intent.EXTRA_SUBJECT,
                "COMBINE Wi-Fi Direct Log - ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}"
            )
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Export / Share Wi-Fi Direct Log CSV").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun getLogFilePath(): String = logFile.absolutePath

    fun getLogFileSize(): Long = if (logFile.exists()) logFile.length() else 0L

    @Synchronized
    fun clearLogs() {
        if (logFile.exists()) {
            logFile.delete()
        }
        ensureHeaderExists()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when {
                ch == '\"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current = StringBuilder()
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString().trim())
        return result
    }
}
