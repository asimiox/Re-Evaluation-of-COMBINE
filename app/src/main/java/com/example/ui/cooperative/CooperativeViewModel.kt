package com.example.ui.cooperative

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cooperative.CooperativeEngine
import com.example.cooperative.CooperativeProgress
import com.example.data.CooperativeLogManager
import com.example.model.CooperativeBatchSummary
import com.example.model.CooperativeRunResult
import com.example.network.NetworkBindingHelper
import com.example.network.NetworkMeter
import com.example.wifidirect.WifiDirectManager
import com.example.wifidirect.WifiDirectState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt

enum class CoopMode {
    SOLO,
    POOLED
}

enum class CoopRole {
    COORDINATOR,
    HELPER
}

data class CoopUrlPreset(
    val id: String,
    val label: String,
    val sizeLabel: String,
    val url: String,
    val description: String
)

val COOP_URL_PRESETS = listOf(
    CoopUrlPreset(
        id = "hetzner_100mb",
        label = "Hetzner 100MB",
        sizeLabel = "100 MB",
        url = "https://fsn1-speed.hetzner.com/100MB.bin",
        description = "Official Falkenstein DC Mirror (Range Supported)"
    ),
    CoopUrlPreset(
        id = "tele2_100mb",
        label = "Tele2 100MB",
        sizeLabel = "100 MB",
        url = "http://speedtest.tele2.net/100MB.zip",
        description = "Tele2 Global High-Speed Mirror (Range Supported)"
    ),
    CoopUrlPreset(
        id = "hetzner_nbg",
        label = "Hetzner NBG 100MB",
        sizeLabel = "100 MB",
        url = "https://nbg1-speed.hetzner.com/100MB.bin",
        description = "Hetzner Nuremberg Mirror (Range Supported)"
    ),
    CoopUrlPreset(
        id = "hetzner_ash",
        label = "Hetzner US 100MB",
        sizeLabel = "100 MB",
        url = "https://ash-speed.hetzner.com/100MB.bin",
        description = "Hetzner Ashburn DC Mirror (Range Supported)"
    ),
    CoopUrlPreset(
        id = "hetzner_1gb",
        label = "Hetzner 1GB",
        sizeLabel = "1 GB",
        url = "https://fsn1-speed.hetzner.com/1GB.bin",
        description = "Large sustained benchmark mirror (1024 MB)"
    )
)

data class ChunkSizePreset(
    val label: String,
    val bytes: Long
)

val CHUNK_SIZE_PRESETS = listOf(
    ChunkSizePreset("1 MB", 1_048_576L),
    ChunkSizePreset("2 MB", 2_097_152L),
    ChunkSizePreset("4 MB", 4_194_304L)
)

data class CooperativeUiState(
    val mode: CoopMode = CoopMode.POOLED,
    val role: CoopRole = CoopRole.COORDINATOR,
    val fileUrl: String = "https://fsn1-speed.hetzner.com/100MB.bin",
    val selectedPresetId: String? = "hetzner_100mb",
    val selectedChunkSizeBytes: Long = 2_097_152L, // Default 2 MB
    val conditionNote: String = "WiFi Coordinator + Cellular Helper",
    val batchRunsPlannedCount: Int = 5,

    // Execution state
    val isRunning: Boolean = false,
    val currentRunNumber: Int = 0,
    val totalRunsPlanned: Int = 5,
    val batchCountdownSeconds: Int = 0,
    val liveProgress: CooperativeProgress? = null,
    val helperStatusText: String = "",
    val activeBatchRuns: List<CooperativeRunResult> = emptyList(),
    val latestBatchSummary: CooperativeBatchSummary? = null,
    val activeBatchId: String? = null,
    val canResumeBatch: Boolean = false,

    // Device telemetry
    val isCharging: Boolean = false,
    val chargingWarning: String? = null,
    val batteryPercentage: Int = 0,
    val coordinatorNetwork: String = "",
    val helperCellularNetwork: String = "",

    // Wi-Fi Direct connection state from manager
    val p2pState: WifiDirectState = WifiDirectState(),

    // Logs and error
    val lastSoloMeanThroughputMbps: Double? = null,
    val errorMessage: String? = null,
    val recentRuns: List<CooperativeRunResult> = emptyList()
)

class CooperativeViewModel(application: Application) : AndroidViewModel(application) {

    private val networkMeter = NetworkMeter(application)
    val bindingHelper = NetworkBindingHelper(application)
    val logManager = CooperativeLogManager(application)
    val engine = CooperativeEngine(application, networkMeter, bindingHelper, logManager)
    val p2pManager = WifiDirectManager(application, viewModelScope)

    private val _uiState = MutableStateFlow(CooperativeUiState())
    val uiState: StateFlow<CooperativeUiState> = _uiState.asStateFlow()

    private var benchmarkJob: Job? = null
    private var currentBatchId: String? = null

    init {
        p2pManager.register()
        refreshDeviceInfo()
        refreshLogs()

        viewModelScope.launch {
            p2pManager.state.collect { p2p ->
                _uiState.update { it.copy(p2pState = p2p) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        engine.cancelOperation()
        p2pManager.unregister()
        bindingHelper.releaseCellularNetwork()
    }

    fun selectMode(mode: CoopMode) {
        _uiState.update { it.copy(mode = mode, errorMessage = null) }
    }

    fun selectRole(role: CoopRole) {
        _uiState.update { it.copy(role = role, errorMessage = null) }
    }

    fun selectPreset(preset: CoopUrlPreset) {
        _uiState.update {
            it.copy(
                fileUrl = preset.url,
                selectedPresetId = preset.id,
                errorMessage = null
            )
        }
    }

    fun onUrlChanged(newUrl: String) {
        val match = COOP_URL_PRESETS.firstOrNull { it.url.equals(newUrl.trim(), ignoreCase = true) }
        _uiState.update {
            it.copy(
                fileUrl = newUrl,
                selectedPresetId = match?.id,
                errorMessage = null
            )
        }
    }

    fun selectChunkSize(bytes: Long) {
        _uiState.update { it.copy(selectedChunkSizeBytes = bytes) }
    }

    fun setConditionNote(note: String) {
        _uiState.update { it.copy(conditionNote = note) }
    }

    fun setBatchRunsCount(count: Int) {
        val clamped = count.coerceIn(1, 20)
        _uiState.update { it.copy(batchRunsPlannedCount = clamped) }
    }

    fun refreshDeviceInfo() {
        val battery = networkMeter.getBatteryPercentage()
        val charging = networkMeter.isDeviceCharging()
        val coordNet = networkMeter.getNetworkType()
        val cellularNet = bindingHelper.getActiveNetworkLabel(bindingHelper.cellularNetwork.value)

        _uiState.update {
            it.copy(
                batteryPercentage = battery,
                isCharging = charging,
                chargingWarning = if (charging) "Device is charging — battery depletion data for this run will not be reliable." else null,
                coordinatorNetwork = coordNet,
                helperCellularNetwork = cellularNet
            )
        }
    }

    fun refreshLogs() {
        val runs = logManager.readAllRuns()
        val soloRuns = runs.filter { it.mode.equals("Solo", ignoreCase = true) && it.isSuccessful }
        val lastSoloMean = if (soloRuns.isNotEmpty()) soloRuns.map { it.effectiveThroughputMbps }.average() else null

        _uiState.update {
            it.copy(
                recentRuns = runs,
                lastSoloMeanThroughputMbps = lastSoloMean
            )
        }
    }

    // --- Cooperative Benchmark Execution ---
    fun startBenchmark(resume: Boolean = false) {
        val state = _uiState.value
        val url = state.fileUrl.trim()
        if (url.isBlank() || (!url.startsWith("http://", true) && !url.startsWith("https://", true))) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid HTTP/HTTPS direct URL.") }
            return
        }

        if (state.mode == CoopMode.POOLED) {
            if (state.role == CoopRole.HELPER) {
                startHelperWorker()
                return
            } else {
                // Coordinator: ensure Wi-Fi Direct peer is connected
                if (!state.p2pState.isConnected) {
                    _uiState.update { it.copy(errorMessage = "Wi-Fi Direct is not connected. Connect to Helper phone first.") }
                    return
                }
            }
        }

        // Coordinator execution (Solo or Pooled Coordinator)
        val totalRuns = state.batchRunsPlannedCount.coerceIn(1, 20)
        val chunkSize = state.selectedChunkSizeBytes
        val condition = state.conditionNote.trim()

        val batchId = if (resume && !currentBatchId.isNullOrBlank()) {
            currentBatchId!!
        } else {
            val prefix = if (state.mode == CoopMode.SOLO) "solo_batch" else "coop_batch"
            val newId = "${prefix}_${System.currentTimeMillis()}"
            currentBatchId = newId
            newId
        }

        val completedRuns = if (resume) {
            state.activeBatchRuns.toMutableList()
        } else {
            mutableListOf()
        }

        val startRunIndex = if (resume) completedRuns.size + 1 else 1

        benchmarkJob?.cancel()
        refreshDeviceInfo()

        _uiState.update {
            it.copy(
                isRunning = true,
                activeBatchId = batchId,
                canResumeBatch = false,
                currentRunNumber = startRunIndex,
                totalRunsPlanned = totalRuns,
                batchCountdownSeconds = 0,
                liveProgress = null,
                activeBatchRuns = completedRuns.toList(),
                latestBatchSummary = null,
                errorMessage = null
            )
        }

        benchmarkJob = viewModelScope.launch {
            var currentAttemptIndex = startRunIndex
            var retryCountForThisRun = 0

            while (currentAttemptIndex <= totalRuns && isActive) {
                _uiState.update {
                    it.copy(
                        currentRunNumber = currentAttemptIndex,
                        liveProgress = null,
                        batchCountdownSeconds = 0
                    )
                }

                // Strict run labeling without duplication
                val runLabel = if (retryCountForThisRun == 0) {
                    "$currentAttemptIndex of $totalRuns"
                } else if (retryCountForThisRun == 1) {
                    "$currentAttemptIndex of $totalRuns (retry)"
                } else {
                    "$currentAttemptIndex of $totalRuns (retry $retryCountForThisRun)"
                }

                val result = if (state.mode == CoopMode.SOLO) {
                    engine.executeSoloDownload(
                        fileUrl = url,
                        chunkSizeBytes = chunkSize,
                        conditionNote = condition,
                        batchId = batchId,
                        runLabel = runLabel
                    ) { prog ->
                        _uiState.update { it.copy(liveProgress = prog) }
                    }
                } else {
                    engine.executeCoordinatorPooled(
                        fileUrl = url,
                        chunkSizeBytes = chunkSize,
                        conditionNote = condition,
                        batchId = batchId,
                        runLabel = runLabel,
                        isGroupOwner = state.p2pState.isGroupOwner,
                        groupOwnerAddress = state.p2pState.groupOwnerAddress
                    ) { prog ->
                        _uiState.update { it.copy(liveProgress = prog) }
                    }
                }

                result.fold(
                    onSuccess = { runResult ->
                        completedRuns.add(runResult)
                        retryCountForThisRun = 0
                        currentAttemptIndex++
                        _uiState.update {
                            it.copy(
                                activeBatchRuns = completedRuns.toList(),
                                recentRuns = logManager.readAllRuns()
                            )
                        }
                    },
                    onFailure = { err ->
                        retryCountForThisRun++
                        _uiState.update {
                            it.copy(
                                errorMessage = "Run $currentAttemptIndex encountered error: ${err.message}",
                                recentRuns = logManager.readAllRuns()
                            )
                        }
                    }
                )

                if (retryCountForThisRun >= 2) {
                    // Abort loop on repeated failures to prevent infinite retry loop
                    break
                }

                // 3-second gap between runs if more remain
                if (currentAttemptIndex <= totalRuns && isActive) {
                    for (sec in 3 downTo 1) {
                        _uiState.update { it.copy(batchCountdownSeconds = sec) }
                        delay(1000L)
                    }
                    _uiState.update { it.copy(batchCountdownSeconds = 0) }
                }
            }

            val summary = computeSummary(batchId, totalRuns, completedRuns, state.mode.name, condition)
            val allDone = completedRuns.size >= totalRuns

            _uiState.update {
                it.copy(
                    isRunning = false,
                    liveProgress = null,
                    batchCountdownSeconds = 0,
                    canResumeBatch = !allDone && completedRuns.isNotEmpty(),
                    latestBatchSummary = summary,
                    recentRuns = logManager.readAllRuns()
                )
            }
            refreshDeviceInfo()
            refreshLogs()
        }
    }

    private fun startHelperWorker() {
        val p2p = _uiState.value.p2pState
        val isGo = p2p.isGroupOwner
        val goAddress = p2p.groupOwnerAddress.ifBlank { "192.168.49.1" }
        benchmarkJob?.cancel()

        _uiState.update {
            it.copy(
                isRunning = true,
                helperStatusText = "Helper Worker starting...",
                errorMessage = null
            )
        }

        benchmarkJob = viewModelScope.launch {
            val result = engine.executeHelperWorker(
                isGroupOwner = isGo,
                groupOwnerAddress = goAddress
            ) { msg ->
                _uiState.update { it.copy(helperStatusText = msg) }
            }

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isRunning = false,
                            helperStatusText = "Helper finished pooled session successfully."
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isRunning = false,
                            helperStatusText = "Helper stopped: ${err.message}",
                            errorMessage = err.message
                        )
                    }
                }
            )
            refreshDeviceInfo()
        }
    }

    fun cancelBenchmark() {
        engine.cancelOperation()
        benchmarkJob?.cancel()
        benchmarkJob = null

        val completed = _uiState.value.activeBatchRuns
        val planned = _uiState.value.totalRunsPlanned
        val batchId = currentBatchId ?: "cancelled_batch"

        val summary = if (completed.isNotEmpty()) {
            computeSummary(batchId, planned, completed, _uiState.value.mode.name, _uiState.value.conditionNote)
        } else null

        _uiState.update {
            it.copy(
                isRunning = false,
                batchCountdownSeconds = 0,
                liveProgress = null,
                canResumeBatch = completed.size < planned,
                latestBatchSummary = summary,
                helperStatusText = "Operation cancelled.",
                errorMessage = if (completed.isEmpty()) "Benchmark cancelled before completing any runs."
                else "Benchmark paused at ${completed.size} of $planned runs."
            )
        }
        refreshLogs()
        refreshDeviceInfo()
    }

    fun resetBatchSession() {
        currentBatchId = null
        _uiState.update {
            it.copy(
                activeBatchId = null,
                canResumeBatch = false,
                activeBatchRuns = emptyList(),
                latestBatchSummary = null,
                errorMessage = null
            )
        }
    }

    private fun computeSummary(
        batchId: String,
        totalPlanned: Int,
        runs: List<CooperativeRunResult>,
        mode: String,
        condition: String
    ): CooperativeBatchSummary? {
        if (runs.isEmpty()) return null

        val validRuns = runs.filter { it.effectiveThroughputMbps > 0.0 && it.isSuccessful }
        val count = runs.size
        val avgMbps = if (validRuns.isNotEmpty()) validRuns.map { it.effectiveThroughputMbps }.average() else 0.0
        val minMbps = if (validRuns.isNotEmpty()) validRuns.minOf { it.effectiveThroughputMbps } else 0.0
        val maxMbps = if (validRuns.isNotEmpty()) validRuns.maxOf { it.effectiveThroughputMbps } else 0.0

        val variance = if (validRuns.size > 1) {
            validRuns.map { (it.effectiveThroughputMbps - avgMbps).pow(2) }.average()
        } else 0.0
        val stdDev = sqrt(variance)

        val lastSolo = _uiState.value.lastSoloMeanThroughputMbps
        val speedup = if (lastSolo != null && lastSolo > 0.0 && avgMbps > 0.0) {
            avgMbps / lastSolo
        } else null

        val hashSummary = runs.lastOrNull()?.hashMatch ?: "N/A"

        return CooperativeBatchSummary(
            batchId = batchId,
            mode = mode,
            totalRunsPlanned = totalPlanned,
            completedRunsCount = count,
            avgThroughputMbps = avgMbps,
            minThroughputMbps = minMbps,
            maxThroughputMbps = maxMbps,
            stdDevThroughputMbps = stdDev,
            speedupFactor = speedup,
            lastSoloMeanThroughputMbps = lastSolo,
            hashMatchSummary = hashSummary,
            testConditionNote = condition,
            runs = runs
        )
    }

    fun exportCsv() {
        logManager.shareCsv()
    }

    fun clearLogs() {
        logManager.clearLogs()
        currentBatchId = null
        _uiState.update {
            it.copy(
                recentRuns = emptyList(),
                latestBatchSummary = null,
                activeBatchRuns = emptyList(),
                activeBatchId = null,
                canResumeBatch = false
            )
        }
    }
}
