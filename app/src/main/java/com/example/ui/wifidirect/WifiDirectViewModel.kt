package com.example.ui.wifidirect

import android.app.Application
import android.net.wifi.p2p.WifiP2pDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.WifiDirectLogManager
import com.example.model.WifiDirectBatchSummary
import com.example.model.WifiDirectRunResult
import com.example.network.NetworkMeter
import com.example.wifidirect.TransferExecutionResult
import com.example.wifidirect.WifiDirectManager
import com.example.wifidirect.WifiDirectState
import com.example.wifidirect.WifiDirectTransferEngine
import com.example.wifidirect.WifiDirectTransferProgress
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

enum class P2pRole {
    SENDER,
    RECEIVER
}

data class P2pPayloadPreset(
    val id: String,
    val label: String,
    val sizeBytes: Long,
    val description: String
)

val P2P_PAYLOAD_PRESETS = listOf(
    P2pPayloadPreset("10mb", "10 MB", 10_000_000L, "Fast local transfer test"),
    P2pPayloadPreset("50mb", "50 MB", 50_000_000L, "Medium payload benchmark"),
    P2pPayloadPreset("100mb", "100 MB", 100_000_000L, "Sustained high-throughput test")
)

val P2P_CONDITION_PRESETS = listOf(
    "Direct Line-of-Sight (1m)",
    "Line-of-Sight (3m)",
    "Line-of-Sight (5m)",
    "Wall Obstacle",
    "Different Rooms",
    "Device in Pocket"
)

data class WifiDirectUiState(
    // P2P Manager state
    val p2pState: WifiDirectState = WifiDirectState(),

    // Configuration
    val selectedRole: P2pRole = P2pRole.SENDER,
    val selectedPayloadPresetId: String = "10mb",
    val conditionNote: String = "Direct Line-of-Sight (1m)",
    val batchRunsPlannedCount: Int = 5,

    // Execution & Progress
    val isTransferRunning: Boolean = false,
    val currentRunNumber: Int = 0,
    val totalRunsPlanned: Int = 5,
    val batchCountdownSeconds: Int = 0,
    val transferProgress: WifiDirectTransferProgress? = null,
    val activeBatchRuns: List<WifiDirectRunResult> = emptyList(),
    val latestBatchSummary: WifiDirectBatchSummary? = null,
    val activeBatchId: String? = null,
    val canResumeBatch: Boolean = false,

    // Device and Environment Telemetry
    val isCharging: Boolean = false,
    val chargingWarning: String? = null,
    val isConnectedToWifiRouter: Boolean = false,
    val batteryPercentage: Int = 0,
    val deviceModel: String = "",

    // Logs & Status
    val errorMessage: String? = null,
    val recentRuns: List<WifiDirectRunResult> = emptyList()
)

class WifiDirectViewModel(application: Application) : AndroidViewModel(application) {

    private val networkMeter = NetworkMeter(application)
    private val logManager = WifiDirectLogManager(application)
    private val transferEngine = WifiDirectTransferEngine()
    val p2pManager = WifiDirectManager(application, viewModelScope)

    private val _uiState = MutableStateFlow(WifiDirectUiState())
    val uiState: StateFlow<WifiDirectUiState> = _uiState.asStateFlow()

    private var transferJob: Job? = null
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
        transferEngine.cancelActiveTransfer()
        p2pManager.unregister()
        p2pManager.disconnect()
    }

    fun selectRole(role: P2pRole) {
        _uiState.update { it.copy(selectedRole = role) }
    }

    fun selectPayloadPreset(preset: P2pPayloadPreset) {
        _uiState.update { it.copy(selectedPayloadPresetId = preset.id) }
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
        val onRouter = networkMeter.isWiFiActive()
        val model = networkMeter.getDeviceDescription()

        _uiState.update {
            it.copy(
                batteryPercentage = battery,
                isCharging = charging,
                chargingWarning = if (charging) "Device is charging — battery depletion data for this run will not be reliable." else null,
                isConnectedToWifiRouter = onRouter,
                deviceModel = model
            )
        }
    }

    fun refreshLogs() {
        val runs = logManager.readAllRuns()
        _uiState.update { it.copy(recentRuns = runs) }
    }

    // --- Wi-Fi Direct Controls ---
    fun startPeerDiscovery() {
        p2pManager.startDiscovery()
    }

    fun stopPeerDiscovery() {
        p2pManager.stopDiscovery()
    }

    fun connectToPeer(device: WifiP2pDevice) {
        p2pManager.connect(device)
    }

    fun disconnectPeer() {
        transferEngine.cancelActiveTransfer()
        transferJob?.cancel()
        transferJob = null
        p2pManager.disconnect()
        _uiState.update {
            it.copy(
                isTransferRunning = false,
                transferProgress = null
            )
        }
    }

    // --- Benchmark Transfer Execution ---
    fun startTransferBenchmark(resume: Boolean = false) {
        val p2p = _uiState.value.p2pState
        if (!p2p.isConnected) {
            _uiState.update { it.copy(errorMessage = "Please connect to a peer first via Wi-Fi Direct.") }
            return
        }

        val totalRuns = _uiState.value.batchRunsPlannedCount.coerceIn(1, 20)
        val selectedPreset = P2P_PAYLOAD_PRESETS.firstOrNull { it.id == _uiState.value.selectedPayloadPresetId }
            ?: P2P_PAYLOAD_PRESETS.first()
        val payloadBytes = selectedPreset.sizeBytes
        val roleStr = if (_uiState.value.selectedRole == P2pRole.SENDER) "sender" else "receiver"
        val condition = _uiState.value.conditionNote.trim()

        // Batch ID consistency: preserve exact same batchId across entire batch and retries
        val batchId = if (resume && !currentBatchId.isNullOrBlank()) {
            currentBatchId!!
        } else {
            val newId = "p2p_batch_${System.currentTimeMillis()}"
            currentBatchId = newId
            newId
        }

        val completedRuns = if (resume) {
            _uiState.value.activeBatchRuns.toMutableList()
        } else {
            mutableListOf()
        }

        val startRunIndex = if (resume) completedRuns.size + 1 else 1

        transferJob?.cancel()
        refreshDeviceInfo()

        _uiState.update {
            it.copy(
                isTransferRunning = true,
                activeBatchId = batchId,
                canResumeBatch = false,
                currentRunNumber = startRunIndex,
                totalRunsPlanned = totalRuns,
                batchCountdownSeconds = 0,
                transferProgress = null,
                activeBatchRuns = completedRuns.toList(),
                latestBatchSummary = null,
                errorMessage = null
            )
        }

        transferJob = viewModelScope.launch {
            // Track retry count per planned run number for strict retry labeling
            var currentAttemptIndex = startRunIndex
            var retryCountForThisRun = 0

            while (currentAttemptIndex <= totalRuns && isActive) {
                _uiState.update {
                    it.copy(
                        currentRunNumber = currentAttemptIndex,
                        transferProgress = null,
                        batchCountdownSeconds = 0
                    )
                }

                // Requirement 6: Strict Run Labeling (No duplicate run numbers without "(retry)")
                val runLabel = if (retryCountForThisRun == 0) {
                    "$currentAttemptIndex of $totalRuns"
                } else if (retryCountForThisRun == 1) {
                    "$currentAttemptIndex of $totalRuns (retry)"
                } else {
                    "$currentAttemptIndex of $totalRuns (retry $retryCountForThisRun)"
                }

                val startTimeWall = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val batteryBefore = networkMeter.getBatteryPercentage()
                val isChargingAtStart = networkMeter.isDeviceCharging()
                val onRouterAtStart = networkMeter.isWiFiActive()

                val latestP2p = _uiState.value.p2pState

                val result = transferEngine.executeTransfer(
                    isGroupOwner = latestP2p.isGroupOwner,
                    groupOwnerAddress = latestP2p.groupOwnerAddress,
                    isSender = _uiState.value.selectedRole == P2pRole.SENDER,
                    payloadSizeBytes = payloadBytes
                ) { progress ->
                    _uiState.update { it.copy(transferProgress = progress) }
                }

                val batteryAfter = networkMeter.getBatteryPercentage()

                result.fold(
                    onSuccess = { execResult ->
                        val logEntry = WifiDirectRunResult(
                            timestamp = startTimeWall,
                            role = roleStr,
                            deviceModel = _uiState.value.deviceModel,
                            peerDeviceName = latestP2p.connectedPeerName,
                            payloadSizeBytes = execResult.payloadSizeBytes,
                            discoveryTimeMs = latestP2p.discoveryTimeMs,
                            connectionTimeMs = latestP2p.connectionTimeMs,
                            transferTimeSeconds = execResult.transferTimeSeconds,
                            localThroughputMbps = execResult.localThroughputMbps,
                            bytesReceivedOk = execResult.bytesReceivedOk,
                            connectedToWifiRouter = onRouterAtStart,
                            p2pFrequencyMhz = latestP2p.p2pFrequencyMhz,
                            batteryBefore = batteryBefore,
                            batteryAfter = batteryAfter,
                            isCharging = isChargingAtStart,
                            testConditionNote = condition,
                            batchId = batchId,
                            runNumberInBatch = runLabel,
                            status = execResult.status
                        )
                        logManager.appendRun(logEntry)

                        if (execResult.bytesReceivedOk) {
                            completedRuns.add(logEntry)
                            retryCountForThisRun = 0
                            currentAttemptIndex++
                        } else {
                            // Run failed validation (size or CRC mismatch): record as FAILED and increment retry
                            retryCountForThisRun++
                        }

                        _uiState.update {
                            it.copy(
                                activeBatchRuns = completedRuns.toList(),
                                recentRuns = logManager.readAllRuns()
                            )
                        }
                    },
                    onFailure = { err ->
                        // Log failed socket run as its own row marked FAILED
                        val failedEntry = WifiDirectRunResult(
                            timestamp = startTimeWall,
                            role = roleStr,
                            deviceModel = _uiState.value.deviceModel,
                            peerDeviceName = latestP2p.connectedPeerName,
                            payloadSizeBytes = payloadBytes,
                            discoveryTimeMs = latestP2p.discoveryTimeMs,
                            connectionTimeMs = latestP2p.connectionTimeMs,
                            transferTimeSeconds = 0.0,
                            localThroughputMbps = 0.0,
                            bytesReceivedOk = false,
                            connectedToWifiRouter = onRouterAtStart,
                            p2pFrequencyMhz = latestP2p.p2pFrequencyMhz,
                            batteryBefore = batteryBefore,
                            batteryAfter = batteryAfter,
                            isCharging = isChargingAtStart,
                            testConditionNote = "$condition [FAILED: ${err.message?.take(40)}]",
                            batchId = batchId,
                            runNumberInBatch = "$runLabel (FAILED)",
                            status = "FAILED: ${err.message?.take(50)}"
                        )
                        logManager.appendRun(failedEntry)
                        retryCountForThisRun++

                        _uiState.update {
                            it.copy(
                                errorMessage = "Run failed: ${err.message}",
                                recentRuns = logManager.readAllRuns()
                            )
                        }
                    }
                )

                // 3-second gap between runs if more runs remaining
                if (currentAttemptIndex <= totalRuns && isActive) {
                    for (sec in 3 downTo 1) {
                        _uiState.update { it.copy(batchCountdownSeconds = sec) }
                        delay(1000L)
                    }
                    _uiState.update { it.copy(batchCountdownSeconds = 0) }
                }
            }

            val summary = computeSummary(batchId, totalRuns, completedRuns, roleStr, p2p.connectedPeerName, payloadBytes, condition)
            val allDone = completedRuns.size >= totalRuns

            _uiState.update {
                it.copy(
                    isTransferRunning = false,
                    transferProgress = null,
                    batchCountdownSeconds = 0,
                    canResumeBatch = !allDone && completedRuns.isNotEmpty(),
                    latestBatchSummary = summary,
                    recentRuns = logManager.readAllRuns()
                )
            }
            refreshDeviceInfo()
        }
    }

    fun cancelTransferBenchmark() {
        transferEngine.cancelActiveTransfer()
        transferJob?.cancel()
        transferJob = null

        val completed = _uiState.value.activeBatchRuns
        val planned = _uiState.value.totalRunsPlanned
        val batchId = currentBatchId ?: "p2p_batch_cancelled"
        val p2p = _uiState.value.p2pState

        val selectedPreset = P2P_PAYLOAD_PRESETS.firstOrNull { it.id == _uiState.value.selectedPayloadPresetId }
            ?: P2P_PAYLOAD_PRESETS.first()
        val roleStr = if (_uiState.value.selectedRole == P2pRole.SENDER) "sender" else "receiver"

        val summary = if (completed.isNotEmpty()) {
            computeSummary(batchId, planned, completed, roleStr, p2p.connectedPeerName, selectedPreset.sizeBytes, _uiState.value.conditionNote)
        } else null

        _uiState.update {
            it.copy(
                isTransferRunning = false,
                batchCountdownSeconds = 0,
                transferProgress = null,
                canResumeBatch = completed.size < planned,
                latestBatchSummary = summary,
                errorMessage = if (completed.isEmpty()) {
                    "P2P benchmark cancelled. No runs completed."
                } else {
                    "P2P benchmark paused at ${completed.size} of $planned runs. All runs logged with batch_id: $batchId."
                }
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
        runs: List<WifiDirectRunResult>,
        role: String,
        peerName: String,
        payloadBytes: Long,
        condition: String
    ): WifiDirectBatchSummary? {
        if (runs.isEmpty()) return null

        val validRuns = runs.filter { it.localThroughputMbps > 0.0 && it.bytesReceivedOk }
        val count = runs.size
        val avgMbps = if (validRuns.isNotEmpty()) validRuns.map { it.localThroughputMbps }.average() else 0.0
        val minMbps = if (validRuns.isNotEmpty()) validRuns.minOf { it.localThroughputMbps } else 0.0
        val maxMbps = if (validRuns.isNotEmpty()) validRuns.maxOf { it.localThroughputMbps } else 0.0

        val variance = if (validRuns.size > 1) {
            validRuns.map { (it.localThroughputMbps - avgMbps).pow(2) }.average()
        } else 0.0
        val stdDev = sqrt(variance)

        return WifiDirectBatchSummary(
            batchId = batchId,
            totalRunsPlanned = totalPlanned,
            completedRunsCount = count,
            avgThroughputMbps = avgMbps,
            minThroughputMbps = minMbps,
            maxThroughputMbps = maxMbps,
            stdDevThroughputMbps = stdDev,
            role = role,
            peerDeviceName = peerName,
            payloadSizeMb = payloadBytes / (1024.0 * 1024.0),
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
