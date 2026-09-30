package com.example.model

/**
 * Data model representing a single baseline download test run.
 */
data class TestRunResult(
    val timestamp: String,
    val fileUrl: String,
    val fileSizeBytes: Long,
    val networkType: String,
    val signalStrength: String,
    val totalTimeSeconds: Double,
    val throughputKbps: Double,
    val batteryBefore: Int,
    val batteryAfter: Int
) {
    val throughputMbps: Double
        get() = throughputKbps / 1000.0

    val fileSizeMb: Double
        get() = fileSizeBytes / (1024.0 * 1024.0)
}
