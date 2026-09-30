package com.example.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.model.TestRunResult
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages persisting benchmark runs to combine_logs.csv and
 * sharing the CSV via Android FileProvider.
 */
class TestLogManager(private val context: Context) {

    private val csvFileName = "combine_logs.csv"
    private val csvHeader = "timestamp,file_url,file_size_bytes,network_type,signal_strength,total_time_seconds,throughput_kbps,battery_before,battery_after"

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

    /**
     * Appends a new test run to the CSV file.
     */
    @Synchronized
    fun appendTestRun(result: TestRunResult) {
        ensureHeaderExists()
        val line = buildString {
            append(escapeCsv(result.timestamp)).append(",")
            append(escapeCsv(result.fileUrl)).append(",")
            append(result.fileSizeBytes).append(",")
            append(escapeCsv(result.networkType)).append(",")
            append(escapeCsv(result.signalStrength)).append(",")
            append(String.format(Locale.US, "%.3f", result.totalTimeSeconds)).append(",")
            append(String.format(Locale.US, "%.2f", result.throughputKbps)).append(",")
            append(result.batteryBefore).append(",")
            append(result.batteryAfter).append("\n")
        }

        FileWriter(logFile, true).use { writer ->
            writer.write(line)
        }
    }

    /**
     * Reads all recorded test runs from the CSV.
     */
    @Synchronized
    fun readAllTestRuns(): List<TestRunResult> {
        val file = logFile
        if (!file.exists()) return emptyList()

        val results = mutableListOf<TestRunResult>()
        val lines = file.readLines()
        if (lines.size <= 1) return emptyList()

        for (line in lines.drop(1)) {
            if (line.isBlank()) continue
            val tokens = parseCsvLine(line)
            if (tokens.size >= 9) {
                try {
                    results.add(
                        TestRunResult(
                            timestamp = tokens[0],
                            fileUrl = tokens[1],
                            fileSizeBytes = tokens[2].toLongOrNull() ?: 0L,
                            networkType = tokens[3],
                            signalStrength = tokens[4],
                            totalTimeSeconds = tokens[5].toDoubleOrNull() ?: 0.0,
                            throughputKbps = tokens[6].toDoubleOrNull() ?: 0.0,
                            batteryBefore = tokens[7].toIntOrNull() ?: 0,
                            batteryAfter = tokens[8].toIntOrNull() ?: 0
                        )
                    )
                } catch (_: Exception) {
                    // Skip malformed lines
                }
            }
        }
        return results
    }

    /**
     * Shares the CSV file via system sharesheet.
     */
    fun shareCsv() {
        val file = logFile
        if (!file.exists() || file.length() == 0L) {
            ensureHeaderExists()
        }

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "COMBINE Benchmark Log - ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Export / Share COMBINE Log CSV").apply {
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
