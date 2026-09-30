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
    val batteryAfter: Int,
    val testConditionNote: String = "",
    val batchId: String = "",
    val runNumberInBatch: String = ""
) {
    val throughputMbps: Double
        get() = throughputKbps / 1000.0

    val fileSizeMb: Double
        get() = fileSizeBytes / (1024.0 * 1024.0)

    val isPartOfBatch: Boolean
        get() = batchId.isNotBlank()
}

/**
 * Aggregated summary statistics for a completed or in-progress batch test.
 */
data class BatchSummary(
    val batchId: String,
    val totalRunsPlanned: Int,
    val completedRunsCount: Int,
    val avgThroughputKbps: Double,
    val minThroughputKbps: Double,
    val maxThroughputKbps: Double,
    val stdDevThroughputKbps: Double,
    val avgSignalDbm: Double?,
    val signalSummary: String,
    val testConditionNote: String,
    val runs: List<TestRunResult>
) {
    val avgThroughputMbps: Double
        get() = avgThroughputKbps / 1000.0

    val minThroughputMbps: Double
        get() = minThroughputKbps / 1000.0

    val maxThroughputMbps: Double
        get() = maxThroughputKbps / 1000.0

    val stdDevThroughputMbps: Double
        get() = stdDevThroughputKbps / 1000.0
}
