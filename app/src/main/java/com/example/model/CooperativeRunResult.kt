package com.example.model

/**
 * Data model representing a cooperative download benchmark run.
 * Schema maps 1:1 to cooperative_logs.csv.
 */
data class CooperativeRunResult(
    val timestamp: String,
    val mode: String, // "Solo" or "Pooled"
    val fileUrl: String,
    val fileSizeBytes: Long,
    val chunkSizeBytes: Long,
    val coordinatorNetwork: String, // "WiFi" or "Cellular"
    val helperNetwork: String, // "Cellular" or "WiFi" or "N/A"
    val coordinatorDevice: String,
    val helperDevice: String,
    val totalTimeSeconds: Double,
    val effectiveThroughputMbps: Double,
    val bytesFromCoordinatorLink: Long,
    val bytesFromHelperLink: Long,
    val chunksTotal: Int,
    val chunksFromCoordinator: Int,
    val chunksFromHelper: Int,
    val chunkRetries: Int,
    val localTransferTotalSeconds: Double,
    val localThroughputMbps: Double,
    val hashMatch: String, // "Match", "Mismatch", "N/A (Solo)", or hash preview
    val batteryBeforeCoordinator: Int,
    val batteryAfterCoordinator: Int,
    val batteryBeforeHelper: Int,
    val batteryAfterHelper: Int,
    val isChargingCoordinator: Boolean,
    val isChargingHelper: Boolean,
    val testConditionNote: String,
    val batchId: String,
    val runNumberInBatch: String,
    val status: String // "OK" or "FAILED: <reason>"
) {
    val fileSizeMb: Double
        get() = fileSizeBytes / (1024.0 * 1024.0)

    val isSuccessful: Boolean
        get() = status.startsWith("OK", ignoreCase = true)
}

/**
 * Statistical summary for a Cooperative batch benchmark session.
 */
data class CooperativeBatchSummary(
    val batchId: String,
    val mode: String,
    val totalRunsPlanned: Int,
    val completedRunsCount: Int,
    val avgThroughputMbps: Double,
    val minThroughputMbps: Double,
    val maxThroughputMbps: Double,
    val stdDevThroughputMbps: Double,
    val speedupFactor: Double?, // e.g., 1.45 means 1.45x faster than last solo run
    val lastSoloMeanThroughputMbps: Double?,
    val hashMatchSummary: String,
    val testConditionNote: String,
    val runs: List<CooperativeRunResult>
)
