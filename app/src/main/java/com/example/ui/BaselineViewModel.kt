package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.TestLogManager
import com.example.download.DownloadEngine
import com.example.download.DownloadProgress
import com.example.model.TestRunResult
import com.example.network.NetworkMeter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BaselineUiState(
    val fileUrl: String = "https://www.mediafire.com/file/q16udyq3g97g208/GGC.apk/file",
    val isDownloading: Boolean = false,
    val progress: DownloadProgress? = null,
    val latestResult: TestRunResult? = null,
    val errorMessage: String? = null,
    val recentRuns: List<TestRunResult> = emptyList(),
    val liveNetworkType: String = "",
    val liveSignalStrength: String = "",
    val liveBatteryPercentage: Int = 0
)

class BaselineViewModel(application: Application) : AndroidViewModel(application) {

    private val networkMeter = NetworkMeter(application)
    private val logManager = TestLogManager(application)
    private val downloadEngine = DownloadEngine(networkMeter, logManager)

    private val _uiState = MutableStateFlow(BaselineUiState())
    val uiState: StateFlow<BaselineUiState> = _uiState.asStateFlow()

    private var downloadJob: Job? = null

    init {
        refreshDeviceInfo()
        refreshLogs()
    }

    fun onUrlChanged(newUrl: String) {
        _uiState.value = _uiState.value.copy(fileUrl = newUrl, errorMessage = null)
    }

    fun selectBenchmarkUrl(url: String) {
        _uiState.value = _uiState.value.copy(fileUrl = url, errorMessage = null)
    }

    fun refreshDeviceInfo() {
        _uiState.value = _uiState.value.copy(
            liveNetworkType = networkMeter.getNetworkType(),
            liveSignalStrength = networkMeter.getSignalStrength(),
            liveBatteryPercentage = networkMeter.getBatteryPercentage()
        )
    }

    fun refreshLogs() {
        val runs = logManager.readAllTestRuns()
        _uiState.value = _uiState.value.copy(recentRuns = runs)
    }

    fun startDownload() {
        val currentUrl = _uiState.value.fileUrl.trim()
        if (currentUrl.isBlank() || (!currentUrl.startsWith("http://") && !currentUrl.startsWith("https://"))) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please enter a valid HTTP or HTTPS URL."
            )
            return
        }

        downloadJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isDownloading = true,
            progress = null,
            latestResult = null,
            errorMessage = null
        )

        downloadJob = viewModelScope.launch {
            val result = downloadEngine.runBenchmark(currentUrl) { progressUpdate ->
                _uiState.value = _uiState.value.copy(progress = progressUpdate)
            }

            result.fold(
                onSuccess = { testResult ->
                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        progress = null,
                        latestResult = testResult,
                        errorMessage = null
                    )
                    refreshLogs()
                    refreshDeviceInfo()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        progress = null,
                        errorMessage = error.localizedMessage ?: "Download failed"
                    )
                    refreshDeviceInfo()
                }
            )
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _uiState.value = _uiState.value.copy(
            isDownloading = false,
            errorMessage = "Download was cancelled."
        )
        refreshDeviceInfo()
    }

    fun exportCsv() {
        logManager.shareCsv()
    }

    fun clearLogHistory() {
        logManager.clearLogs()
        refreshLogs()
    }
}
