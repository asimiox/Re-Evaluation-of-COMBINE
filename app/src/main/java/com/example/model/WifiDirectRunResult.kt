package com.example.model

/**
 * Data model representing a local Wi-Fi Direct peer-to-peer benchmark run.
 */
data class WifiDirectRunResult(
    val timestamp: String,
    val role: String, // "sender" or "receiver"
    val deviceModel: String,
    val peerDeviceName: String,
    val payloadSizeBytes: Long,
    val discoveryTimeMs: Long,
    val connectionTimeMs: Long,
    val transferTimeSeconds: Double,
    val localThroughputMbps: Double,
    val bytesReceivedOk: Boolean,
    val connectedToWifiRouter: Boolean,
    val p2pFrequencyMhz: String = "",
    val batteryBefore: Int,
    val batteryAfter: Int,
    val isCharging: Boolean = false,
    val testConditionNote: String = "",
    val batchId: String = "",
    val runNumberInBatch: String = "",
    val status: String = "OK" // "OK" or "FAILED: <reason>"
) {
    val payloadSizeMb: Double
        get() = payloadSizeBytes / (1024.0 * 1024.0)

    val isSuccessful: Boolean
        get() = status.startsWith("OK", ignoreCase = true) && bytesReceivedOk
}

/**
 * Summary statistics for a Wi-Fi Direct batch benchmark session.
 */
data class WifiDirectBatchSummary(
    val batchId: String,
    val totalRunsPlanned: Int,
    val completedRunsCount: Int,
    val avgThroughputMbps: Double,
    val minThroughputMbps: Double,
    val maxThroughputMbps: Double,
    val stdDevThroughputMbps: Double,
    val role: String,
    val peerDeviceName: String,
    val payloadSizeMb: Double,
    val testConditionNote: String,
    val runs: List<WifiDirectRunResult>
)
