package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.TestLogManager
import com.example.download.DownloadEngine
import com.example.download.DownloadProgress
import com.example.model.BatchSummary
import com.example.model.TestRunResult
import com.example.network.NetworkMeter
import com.example.network.SignalQuality
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

data class PresetFile(
    val id: String,
    val label: String,
    val sizeLabel: String,
    val url: String,
    val description: String
)

val HETZNER_PRESET_FILES = listOf(
    PresetFile(
        id = "small_10mb",
        label = "Small (10MB)",
        sizeLabel = "10 MB",
        url = "https://speed.cloudflare.com/__down?bytes=10000000",
        description = "Cloudflare Global Anycast (Fast & 100% reliable for batch runs)"
    ),
    PresetFile(
        id = "med_25mb",
        label = "Medium (25MB)",
        sizeLabel = "25 MB",
        url = "https://speed.cloudflare.com/__down?bytes=25000000",
        description = "Cloudflare Anycast 25MB test file for mobile broadband"
    ),
    PresetFile(
        id = "large_100mb",
        label = "Large (100MB)",
        sizeLabel = "100 MB",
        url = "https://speed.cloudflare.com/__down?bytes=100000000",
        description = "Cloudflare Anycast 100MB high-throughput test"
    ),
    PresetFile(
        id = "hetzner_100mb",
        label = "Hetzner (100MB)",
        sizeLabel = "100 MB",
        url = "https://fsn1-speed.hetzner.com/100MB.bin",
        description = "Hetzner Falkenstein official datacenter mirror"
    )
)

val CONDITION_NOTE_PRESETS = listOf(
    "Indoor",
    "Outdoor",
    "Near router",
    "Far from router",
    "Moving / Vehicle",
    "Basement"
)

data class BaselineUiState(
    val fileUrl: String = "https://speed.cloudflare.com/__down?bytes=10000000",
    val selectedPresetId: String? = "small_10mb",
    val conditionNote: String = "Indoor",
    val batchRunsPlannedCount: Int = 10,

    // Single Test State
    val isSingleDownloading: Boolean = false,
    val singleProgress: DownloadProgress? = null,
    val latestSingleResult: TestRunResult? = null,

    // Batch Test State
    val isBatchRunning: Boolean = false,
    val currentBatchRunNumber: Int = 0,
    val totalBatchRunsPlanned: Int = 10,
    val batchCountdownSeconds: Int = 0,
    val currentBatchDownloadProgress: DownloadProgress? = null,
    val activeBatchRuns: List<TestRunResult> = emptyList(),
    val latestBatchSummary: BatchSummary? = null,

    // Logs & Device Metrics
    val errorMessage: String? = null,
    val recentRuns: List<TestRunResult> = emptyList(),
    val liveNetworkType: String = "",
    val liveSignalStrength: String = "",
    val signalQuality: SignalQuality = SignalQuality.UNKNOWN,
    val carrierName: String? = null,
    val isWiFiActive: Boolean = false,
    val isCellularActive: Boolean = false,
    val liveBatteryPercentage: Int = 0,
    val deviceDescription: String = "",
    val isEmulator: Boolean = false
) {
    val isAnyTestRunning: Boolean
        get() = isSingleDownloading || isBatchRunning
}

class BaselineViewModel(application: Application) : AndroidViewModel(application) {

    private val networkMeter = NetworkMeter(application)
    private val logManager = TestLogManager(application)
    private val downloadEngine = DownloadEngine(networkMeter, logManager)

    private val _uiState = MutableStateFlow(BaselineUiState())
    val uiState: StateFlow<BaselineUiState> = _uiState.asStateFlow()

    private var singleJob: Job? = null
    private var batchJob: Job? = null

    init {
        refreshDeviceInfo()
        refreshLogs()
    }

    fun onUrlChanged(newUrl: String) {
        val matchingPreset = HETZNER_PRESET_FILES.firstOrNull { it.url.equals(newUrl.trim(), ignoreCase = true) }
        _uiState.update { it.copy(
            fileUrl = newUrl,
            selectedPresetId = matchingPreset?.id,
            errorMessage = null
        ) }
    }

    fun selectPresetFile(preset: PresetFile) {
        _uiState.update { it.copy(
            fileUrl = preset.url,
            selectedPresetId = preset.id,
            errorMessage = null
        ) }
    }

    fun setConditionNote(note: String) {
        _uiState.update { it.copy(conditionNote = note) }
    }

    fun setBatchRunsCount(count: Int) {
        val clamped = count.coerceIn(1, 30)
        _uiState.update { it.copy(batchRunsPlannedCount = clamped) }
    }

    fun refreshDeviceInfo() {
        val netType = networkMeter.getNetworkType()
        val signal = networkMeter.getSignalStrength()
        val quality = networkMeter.evaluateSignalQuality(signal)
        val carrier = networkMeter.getCarrierName()
        val isWifi = networkMeter.isWiFiActive()
        val isCellular = networkMeter.isCellularActive()
        val battery = networkMeter.getBatteryPercentage()
        val desc = networkMeter.getDeviceDescription()
        val emulator = networkMeter.isEmulator()

        _uiState.update { it.copy(
            liveNetworkType = netType,
            liveSignalStrength = signal,
            signalQuality = quality,
            carrierName = carrier,
            isWiFiActive = isWifi,
            isCellularActive = isCellular,
            liveBatteryPercentage = battery,
            deviceDescription = desc,
            isEmulator = emulator
        ) }
    }

    fun refreshLogs() {
        val runs = logManager.readAllTestRuns()
        _uiState.update { it.copy(recentRuns = runs) }
    }

    // --- Single Test Execution ---
    fun startSingleTest() {
        val currentUrl = _uiState.value.fileUrl.trim()
        if (currentUrl.isBlank() || (!currentUrl.startsWith("http://") && !currentUrl.startsWith("https://"))) {
            _uiState.update { it.copy(errorMessage = "Please select a preset or enter a valid HTTP/HTTPS URL.") }
            return
        }

        singleJob?.cancel()
        _uiState.update { it.copy(
            isSingleDownloading = true,
            singleProgress = null,
            latestSingleResult = null,
            errorMessage = null
        ) }

        singleJob = viewModelScope.launch {
            val result = downloadEngine.runBenchmark(
                fileUrl = currentUrl,
                testConditionNote = _uiState.value.conditionNote.trim(),
                batchId = "",
                runNumberInBatch = ""
            ) { progressUpdate ->
                _uiState.update { it.copy(singleProgress = progressUpdate) }
            }

            result.fold(
                onSuccess = { testResult ->
                    _uiState.update { it.copy(
                        isSingleDownloading = false,
                        singleProgress = null,
                        latestSingleResult = testResult,
                        errorMessage = null
                    ) }
                    refreshLogs()
                    refreshDeviceInfo()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(
                        isSingleDownloading = false,
                        singleProgress = null,
                        errorMessage = error.localizedMessage ?: "Download test failed."
                    ) }
                    refreshDeviceInfo()
                }
            )
        }
    }

    fun cancelSingleTest() {
        singleJob?.cancel()
        singleJob = null
        _uiState.update { it.copy(
            isSingleDownloading = false,
            singleProgress = null,
            errorMessage = "Single download test was cancelled."
        ) }
        refreshDeviceInfo()
    }

    // --- Batch Test Execution ---
    fun startBatchTest() {
        val currentUrl = _uiState.value.fileUrl.trim()
        if (currentUrl.isBlank() || (!currentUrl.startsWith("http://") && !currentUrl.startsWith("https://"))) {
            _uiState.update { it.copy(errorMessage = "Please select a preset or enter a valid HTTP/HTTPS URL.") }
            return
        }

        val totalRuns = _uiState.value.batchRunsPlannedCount.coerceIn(1, 30)
        val condition = _uiState.value.conditionNote.trim()
        val batchId = "batch_${System.currentTimeMillis()}"

        batchJob?.cancel()
        _uiState.update { it.copy(
            isBatchRunning = true,
            currentBatchRunNumber = 0,
            totalBatchRunsPlanned = totalRuns,
            batchCountdownSeconds = 0,
            currentBatchDownloadProgress = null,
            activeBatchRuns = emptyList(),
            latestBatchSummary = null,
            errorMessage = null
        ) }

        batchJob = viewModelScope.launch {
            val completedRuns = mutableListOf<TestRunResult>()

            for (runIndex in 1..totalRuns) {
                if (!isActive) break

                _uiState.update { it.copy(
                    currentBatchRunNumber = runIndex,
                    currentBatchDownloadProgress = null,
                    batchCountdownSeconds = 0
                ) }

                val runLabel = "$runIndex of $totalRuns"
                val result = downloadEngine.runBenchmark(
                    fileUrl = currentUrl,
                    testConditionNote = condition,
                    batchId = batchId,
                    runNumberInBatch = runLabel
                ) { progress ->
                    _uiState.update { it.copy(currentBatchDownloadProgress = progress) }
                }

                result.fold(
                    onSuccess = { runResult ->
                        completedRuns.add(runResult)
                        _uiState.update { it.copy(
                            activeBatchRuns = completedRuns.toList(),
                            recentRuns = logManager.readAllTestRuns()
                        ) }
                    },
                    onFailure = { err ->
                        // Record error message but proceed with countdown if not cancelled
                        _uiState.update { it.copy(
                            errorMessage = "Run $runIndex encountered error: ${err.message}"
                        ) }
                    }
                )

                // 3-second gap between runs if more runs remaining
                if (runIndex < totalRuns && isActive) {
                    for (sec in 3 downTo 1) {
                        _uiState.update { it.copy(batchCountdownSeconds = sec) }
                        delay(1000L)
                    }
                    _uiState.update { it.copy(batchCountdownSeconds = 0) }
                }
            }

            val summary = computeBatchSummary(batchId, totalRuns, completedRuns, condition)
            _uiState.update { it.copy(
                isBatchRunning = false,
                currentBatchDownloadProgress = null,
                batchCountdownSeconds = 0,
                latestBatchSummary = summary,
                recentRuns = logManager.readAllTestRuns()
            ) }
            refreshDeviceInfo()
        }
    }

    fun cancelBatchTest() {
        batchJob?.cancel()
        batchJob = null

        val completed = _uiState.value.activeBatchRuns
        val planned = _uiState.value.totalBatchRunsPlanned
        val summary = if (completed.isNotEmpty()) {
            computeBatchSummary("batch_partial", planned, completed, _uiState.value.conditionNote)
        } else null

        _uiState.update { it.copy(
            isBatchRunning = false,
            batchCountdownSeconds = 0,
            currentBatchDownloadProgress = null,
            latestBatchSummary = summary,
            errorMessage = if (completed.isEmpty()) {
                "Batch test cancelled before completing any runs."
            } else {
                "Batch cancelled. ${completed.size} completed runs were logged."
            }
        ) }
        refreshLogs()
        refreshDeviceInfo()
    }

    private fun computeBatchSummary(
        batchId: String,
        totalPlanned: Int,
        runs: List<TestRunResult>,
        conditionNote: String
    ): BatchSummary? {
        if (runs.isEmpty()) return null

        val count = runs.size
        val avgThroughput = runs.map { it.throughputKbps }.average()
        val minThroughput = runs.minOf { it.throughputKbps }
        val maxThroughput = runs.maxOf { it.throughputKbps }

        val variance = if (count > 1) {
            runs.map { (it.throughputKbps - avgThroughput).pow(2) }.average()
        } else 0.0
        val stdDev = sqrt(variance)

        val dbmList = runs.mapNotNull { networkMeter.extractDbmValue(it.signalStrength) }
        val avgDbm = if (dbmList.isNotEmpty()) dbmList.average() else null
        val sigSummary = if (avgDbm != null) {
            String.format(Locale.US, "%.1f dBm (avg of %d runs)", avgDbm, dbmList.size)
        } else {
            runs.firstOrNull()?.signalStrength ?: "N/A"
        }

        return BatchSummary(
            batchId = batchId,
            totalRunsPlanned = totalPlanned,
            completedRunsCount = count,
            avgThroughputKbps = avgThroughput,
            minThroughputKbps = minThroughput,
            maxThroughputKbps = maxThroughput,
            stdDevThroughputKbps = stdDev,
            avgSignalDbm = avgDbm,
            signalSummary = sigSummary,
            testConditionNote = conditionNote,
            runs = runs
        )
    }

    fun exportCsv() {
        logManager.shareCsv()
    }

    fun clearLogHistory() {
        logManager.clearLogs()
        _uiState.update { it.copy(
            recentRuns = emptyList(),
            latestSingleResult = null,
            latestBatchSummary = null,
            activeBatchRuns = emptyList()
        ) }
    }
}
