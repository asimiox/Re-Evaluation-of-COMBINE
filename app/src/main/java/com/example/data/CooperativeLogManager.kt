package com.example.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.model.CooperativeRunResult
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages persisting cooperative download benchmark runs to cooperative_logs.csv
 * and sharing the CSV via Android FileProvider.
 *
 * Schema:
 * timestamp,mode,file_url,file_size_bytes,chunk_size_bytes,coordinator_network,helper_network,coordinator_device,helper_device,total_time_seconds,effective_throughput_mbps,bytes_from_coordinator_link,bytes_from_helper_link,chunks_total,chunks_from_coordinator,chunks_from_helper,chunk_retries,local_transfer_total_seconds,local_throughput_mbps,hash_match,battery_before_coordinator,battery_after_coordinator,battery_before_helper,battery_after_helper,is_charging_coordinator,is_charging_helper,test_condition_note,batch_id,run_number_in_batch,status
 */
class CooperativeLogManager(private val context: Context) {

    private val csvFileName = "cooperative_logs.csv"
    val csvHeader = "timestamp,mode,file_url,file_size_bytes,chunk_size_bytes,coordinator_network,helper_network,coordinator_device,helper_device,total_time_seconds,effective_throughput_mbps,bytes_from_coordinator_link,bytes_from_helper_link,chunks_total,chunks_from_coordinator,chunks_from_helper,chunk_retries,local_transfer_total_seconds,local_throughput_mbps,hash_match,battery_before_coordinator,battery_after_coordinator,battery_before_helper,battery_after_helper,is_charging_coordinator,is_charging_helper,test_condition_note,batch_id,run_number_in_batch,status"

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
    fun appendRun(result: CooperativeRunResult) {
        ensureHeaderExists()
        val line = buildString {
            append(escapeCsv(result.timestamp)).append(",")
            append(escapeCsv(result.mode)).append(",")
            append(escapeCsv(result.fileUrl)).append(",")
            append(result.fileSizeBytes).append(",")
            append(result.chunkSizeBytes).append(",")
            append(escapeCsv(result.coordinatorNetwork)).append(",")
            append(escapeCsv(result.helperNetwork)).append(",")
            append(escapeCsv(result.coordinatorDevice)).append(",")
            append(escapeCsv(result.helperDevice)).append(",")
            append(String.format(Locale.US, "%.3f", result.totalTimeSeconds)).append(",")
            append(String.format(Locale.US, "%.2f", result.effectiveThroughputMbps)).append(",")
            append(result.bytesFromCoordinatorLink).append(",")
            append(result.bytesFromHelperLink).append(",")
            append(result.chunksTotal).append(",")
            append(result.chunksFromCoordinator).append(",")
            append(result.chunksFromHelper).append(",")
            append(result.chunkRetries).append(",")
            append(String.format(Locale.US, "%.3f", result.localTransferTotalSeconds)).append(",")
            append(String.format(Locale.US, "%.2f", result.localThroughputMbps)).append(",")
            append(escapeCsv(result.hashMatch)).append(",")
            append(result.batteryBeforeCoordinator).append(",")
            append(result.batteryAfterCoordinator).append(",")
            append(result.batteryBeforeHelper).append(",")
            append(result.batteryAfterHelper).append(",")
            append(result.isChargingCoordinator).append(",")
            append(result.isChargingHelper).append(",")
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
    fun readAllRuns(): List<CooperativeRunResult> {
        val file = logFile
        if (!file.exists()) return emptyList()

        val results = mutableListOf<CooperativeRunResult>()
        val lines = file.readLines()
        if (lines.size <= 1) return emptyList()

        for (line in lines.drop(1)) {
            if (line.isBlank()) continue
            val tokens = parseCsvLine(line)
            if (tokens.size >= 30) {
                try {
                    results.add(
                        CooperativeRunResult(
                            timestamp = tokens[0],
                            mode = tokens[1],
                            fileUrl = tokens[2],
                            fileSizeBytes = tokens[3].toLongOrNull() ?: 0L,
                            chunkSizeBytes = tokens[4].toLongOrNull() ?: 0L,
                            coordinatorNetwork = tokens[5],
                            helperNetwork = tokens[6],
                            coordinatorDevice = tokens[7],
                            helperDevice = tokens[8],
                            totalTimeSeconds = tokens[9].toDoubleOrNull() ?: 0.0,
                            effectiveThroughputMbps = tokens[10].toDoubleOrNull() ?: 0.0,
                            bytesFromCoordinatorLink = tokens[11].toLongOrNull() ?: 0L,
                            bytesFromHelperLink = tokens[12].toLongOrNull() ?: 0L,
                            chunksTotal = tokens[13].toIntOrNull() ?: 0,
                            chunksFromCoordinator = tokens[14].toIntOrNull() ?: 0,
                            chunksFromHelper = tokens[15].toIntOrNull() ?: 0,
                            chunkRetries = tokens[16].toIntOrNull() ?: 0,
                            localTransferTotalSeconds = tokens[17].toDoubleOrNull() ?: 0.0,
                            localThroughputMbps = tokens[18].toDoubleOrNull() ?: 0.0,
                            hashMatch = tokens[19],
                            batteryBeforeCoordinator = tokens[20].toIntOrNull() ?: 0,
                            batteryAfterCoordinator = tokens[21].toIntOrNull() ?: 0,
                            batteryBeforeHelper = tokens[22].toIntOrNull() ?: 0,
                            batteryAfterHelper = tokens[23].toIntOrNull() ?: 0,
                            isChargingCoordinator = tokens[24].equals("true", ignoreCase = true),
                            isChargingHelper = tokens[25].equals("true", ignoreCase = true),
                            testConditionNote = tokens[26],
                            batchId = tokens[27],
                            runNumberInBatch = tokens[28],
                            status = tokens[29]
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
                "COMBINE Cooperative Log - ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}"
            )
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Export / Share Cooperative Log CSV").apply {
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
