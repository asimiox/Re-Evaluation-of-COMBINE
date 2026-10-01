package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.download.DownloadProgress
import com.example.model.BatchSummary
import com.example.model.TestRunResult
import com.example.network.SignalQuality
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaselineScreen(viewModel: BaselineViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasPhonePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPhonePermission = permissions[Manifest.permission.READ_PHONE_STATE] == true
        viewModel.refreshDeviceInfo()
    }

    LaunchedEffect(Unit) {
        if (!hasPhonePermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    // Active tab in Run Test section: 0 for Batch, 1 for Single
    var selectedTestTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "COMBINE",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Step 1: Baseline Downloader & Meter",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshDeviceInfo() },
                        modifier = Modifier.testTag("refresh_device_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh network and device info"
                        )
                    }
                    IconButton(
                        onClick = { viewModel.exportCsv() },
                        modifier = Modifier.testTag("export_csv_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export or Share CSV log"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.exportCsv() },
                icon = { Icon(Icons.Default.Share, contentDescription = null) },
                text = { Text("Export CSV") },
                modifier = Modifier.testTag("fab_export_csv")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SECTION 1: DEVICE STATE
                DeviceStateSection(
                    uiState = uiState,
                    hasPhonePermission = hasPhonePermission,
                    onRequestPermissions = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_PHONE_STATE,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                    }
                )

                // Charging Warning Banner
                if (uiState.isCharging || uiState.chargingWarning != null) {
                    ChargingWarningBanner()
                }

                // Non-blocking WiFi safeguard reminder
                if (uiState.isWiFiActive) {
                    WiFiSafeguardBanner()
                }

                // Error Banner if present
                if (uiState.errorMessage != null) {
                    ErrorBanner(
                        message = uiState.errorMessage ?: "",
                        onDismiss = { /* auto-dismissed on new actions */ }
                    )
                }

                // SECTION 2: TEST SETUP
                TestSetupSection(
                    uiState = uiState,
                    onSelectPreset = { viewModel.selectPresetFile(it) },
                    onUrlChanged = { viewModel.onUrlChanged(it) },
                    onConditionNoteChanged = { viewModel.setConditionNote(it) }
                )

                // SECTION 3: RUN TEST (Batch vs Single)
                RunTestSection(
                    uiState = uiState,
                    selectedTab = selectedTestTab,
                    onTabSelected = { selectedTestTab = it },
                    onBatchRunsCountChanged = { viewModel.setBatchRunsCount(it) },
                    onStartBatch = { viewModel.startBatchTest(resume = false) },
                    onResumeBatch = { viewModel.startBatchTest(resume = true) },
                    onResetBatch = { viewModel.resetBatchSession() },
                    onCancelBatch = { viewModel.cancelBatchTest() },
                    onStartSingle = { viewModel.startSingleTest() },
                    onCancelSingle = { viewModel.cancelSingleTest() }
                )

                // SECTION 4: BATCH SUMMARY (if available)
                if (uiState.latestBatchSummary != null) {
                    BatchSummarySection(summary = uiState.latestBatchSummary!!)
                }

                // Single Test Result Card (if completed single test)
                if (uiState.latestSingleResult != null && !uiState.isSingleDownloading) {
                    SingleTestResultCard(result = uiState.latestSingleResult!!)
                }

                // SECTION 5: LOGS & HISTORY
                LogsHistorySection(
                    recentRuns = uiState.recentRuns,
                    onExportCsv = { viewModel.exportCsv() },
                    onClearLogs = { viewModel.clearLogHistory() }
                )

                Spacer(modifier = Modifier.height(72.dp)) // Extra padding for FAB
            }
        }
    }
}

// ==========================================
// SECTION 1: DEVICE STATE
// ==========================================

@Composable
private fun DeviceStateSection(
    uiState: BaselineUiState,
    hasPhonePermission: Boolean,
    onRequestPermissions: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Device State",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (uiState.deviceDescription.isNotBlank()) {
                    Text(
                        text = if (uiState.isEmulator) "☁️ Cloud Emulator" else "📱 Physical Device",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.isEmulator) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Network Type with distinctive WiFi vs Cellular styling
                val isCellular = uiState.isCellularActive
                val networkIcon = if (uiState.isWiFiActive) Icons.Default.Wifi else Icons.Default.SignalCellularAlt
                val networkColor = if (uiState.isWiFiActive) MaterialTheme.colorScheme.primary else Color(0xFF2E7D32)

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = networkIcon,
                            contentDescription = null,
                            tint = networkColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Network",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = uiState.liveNetworkType.ifBlank { "Detecting..." },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = networkColor
                    )
                    if (isCellular && !uiState.carrierName.isNullOrBlank()) {
                        Text(
                            text = uiState.carrierName ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Signal Strength with color coding
                val signalColor = when (uiState.signalQuality) {
                    SignalQuality.STRONG -> Color(0xFF2E7D32) // Green (> -70 dBm)
                    SignalQuality.MEDIUM -> Color(0xFFF57F17) // Amber (-70 to -90 dBm)
                    SignalQuality.WEAK -> Color(0xFFD32F2F)   // Red (< -90 dBm)
                    SignalQuality.UNKNOWN -> MaterialTheme.colorScheme.onSurface
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = signalColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Signal (dBm)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = uiState.liveSignalStrength.ifBlank { "N/A" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = signalColor
                    )
                    Text(
                        text = when (uiState.signalQuality) {
                            SignalQuality.STRONG -> "Strong (> -70)"
                            SignalQuality.MEDIUM -> "Medium (-70 to -90)"
                            SignalQuality.WEAK -> "Weak (< -90)"
                            SignalQuality.UNKNOWN -> ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = signalColor
                    )
                }

                // Battery
                val batteryIconTint = if (uiState.isCharging) Color(0xFFE65100) else MaterialTheme.colorScheme.primary
                Column(modifier = Modifier.weight(0.9f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = batteryIconTint,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (uiState.isCharging) "Battery (⚡)" else "Battery",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (uiState.isCharging) "${uiState.liveBatteryPercentage}% (Charging)" else "${uiState.liveBatteryPercentage}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.isCharging) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (uiState.isEmulator) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ℹ️ Cloud Preview: Speeds (~70 Mbps) & -50 dBm signal reflect Google Cloud Datacenter. Install APK on phone to test local WiFi & phone battery.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (!hasPhonePermission) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Phone State Permission",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Required for accurate LTE/5G technology detection and carrier cellular dBm.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onRequestPermissions,
                    modifier = Modifier.testTag("enable_phone_permission_button")
                ) {
                    Text("Enable")
                }
            }
        }
    }
}

@Composable
private fun WiFiSafeguardBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Currently on WiFi — this won't test cellular performance.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ChargingWarningBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.BatteryChargingFull,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Device is charging — battery depletion data for this run will not be reliable.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ==========================================
// SECTION 2: TEST SETUP
// ==========================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TestSetupSection(
    uiState: BaselineUiState,
    onSelectPreset: (PresetFile) -> Unit,
    onUrlChanged: (String) -> Unit,
    onConditionNoteChanged: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Test Setup",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Preset Speed-Test Files (Hetzner)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Reliable Speed-Test Files (Direct CDN & Mirrors):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HETZNER_PRESET_FILES.forEach { preset ->
                        val isSelected = uiState.selectedPresetId == preset.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectPreset(preset) },
                            label = { Text(preset.label) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                val currentPreset = HETZNER_PRESET_FILES.firstOrNull { it.id == uiState.selectedPresetId }
                if (currentPreset != null) {
                    Text(
                        text = "ℹ️ ${currentPreset.description}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Manual URL option (collapsible/editable)
            OutlinedTextField(
                value = uiState.fileUrl,
                onValueChange = onUrlChanged,
                label = { Text("Benchmark File URL") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("file_url_input"),
                trailingIcon = {
                    if (uiState.fileUrl.isNotBlank()) {
                        IconButton(onClick = { onUrlChanged("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear URL")
                        }
                    }
                }
            )

            HorizontalDivider()

            // Manual Condition Tagging
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Test Condition Note (CSV Tag):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CONDITION_NOTE_PRESETS.forEach { presetTag ->
                        val isSelected = uiState.conditionNote.equals(presetTag, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onConditionNoteChanged(presetTag) },
                            label = { Text(presetTag, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.conditionNote,
                    onValueChange = onConditionNoteChanged,
                    label = { Text("Custom Condition Note (e.g. 'Cellular, indoors, weak signal')") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("condition_note_input")
                )
            }
        }
    }
}

// ==========================================
// SECTION 3: RUN TEST (Batch vs Single)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RunTestSection(
    uiState: BaselineUiState,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onBatchRunsCountChanged: (Int) -> Unit,
    onStartBatch: () -> Unit,
    onResumeBatch: () -> Unit,
    onResetBatch: () -> Unit,
    onCancelBatch: () -> Unit,
    onStartSingle: () -> Unit,
    onCancelSingle: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Run Benchmark",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Tabs for Batch vs Single Test
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { if (!uiState.isAnyTestRunning) onTabSelected(0) },
                    enabled = !uiState.isAnyTestRunning,
                    text = { Text("Batch Test (Repeated)") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { if (!uiState.isAnyTestRunning) onTabSelected(1) },
                    enabled = !uiState.isAnyTestRunning,
                    text = { Text("Single Test") }
                )
            }

            if (selectedTab == 0) {
                // BATCH TEST CONTROLS
                if (uiState.isBatchRunning) {
                    BatchProgressCard(
                        runNumber = uiState.currentBatchRunNumber,
                        totalRuns = uiState.totalBatchRunsPlanned,
                        progress = uiState.currentBatchDownloadProgress,
                        countdownSeconds = uiState.batchCountdownSeconds,
                        onCancel = onCancelBatch
                    )
                } else {
                    BatchSetupControls(
                        plannedCount = uiState.batchRunsPlannedCount,
                        canResume = uiState.canResumeBatch,
                        completedCount = uiState.activeBatchRuns.size,
                        onCountChanged = onBatchRunsCountChanged,
                        onStartBatch = onStartBatch,
                        onResumeBatch = onResumeBatch,
                        onResetBatch = onResetBatch,
                        isEnabled = !uiState.isAnyTestRunning
                    )
                }
            } else {
                // SINGLE TEST CONTROLS
                if (uiState.isSingleDownloading) {
                    SingleProgressCard(
                        progress = uiState.singleProgress,
                        onCancel = onCancelSingle
                    )
                } else {
                    Button(
                        onClick = onStartSingle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_single_download_button"),
                        enabled = !uiState.isAnyTestRunning
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Single Download Test")
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchSetupControls(
    plannedCount: Int,
    canResume: Boolean,
    completedCount: Int,
    onCountChanged: (Int) -> Unit,
    onStartBatch: () -> Unit,
    onResumeBatch: () -> Unit,
    onResetBatch: () -> Unit,
    isEnabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (canResume && completedCount > 0 && completedCount < plannedCount) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Incomplete Batch ($completedCount of $plannedCount runs done)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Resume remaining runs with the same batch_id, or discard to start a new batch.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onResumeBatch,
                            modifier = Modifier.weight(1f).testTag("resume_batch_button"),
                            enabled = isEnabled
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume (Run ${completedCount + 1})")
                        }
                        OutlinedButton(
                            onClick = onResetBatch,
                            modifier = Modifier.testTag("reset_batch_button"),
                            enabled = isEnabled
                        ) {
                            Text("New Batch")
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Number of Repeated Runs:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$plannedCount runs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Quick Preset Chips for batch run counts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(3, 5, 10, 20, 30).forEach { count ->
                val isSelected = count == plannedCount
                FilterChip(
                    selected = isSelected,
                    onClick = { onCountChanged(count) },
                    label = { Text("$count") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }

        Text(
            text = "Back-to-back downloads with a 3s gap. Each run is appended to the CSV.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = onStartBatch,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("start_batch_button"),
            enabled = isEnabled
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Batch ($plannedCount Runs)", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BatchProgressCard(
    runNumber: Int,
    totalRuns: Int,
    progress: DownloadProgress?,
    countdownSeconds: Int,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Batch in Progress: Run $runNumber of $totalRuns",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }

            // Overall Batch Progress
            val batchFraction = (runNumber.toFloat() / maxOf(totalRuns, 1))
            LinearProgressIndicator(
                progress = { batchFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )

            // 3-second gap countdown notice
            if (countdownSeconds > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "⏳ Next run starting in $countdownSeconds second(s)...",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            } else if (progress != null) {
                // Live run download metrics
                val progressFraction = if (progress.totalBytesExpected > 0) {
                    (progress.bytesDownloaded.toFloat() / progress.totalBytesExpected.toFloat()).coerceIn(0f, 1f)
                } else null

                if (progressFraction != null) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val downloadedMb = progress.bytesDownloaded / (1024.0 * 1024.0)
                    val totalMb = if (progress.totalBytesExpected > 0) {
                        progress.totalBytesExpected / (1024.0 * 1024.0)
                    } else null

                    Text(
                        text = if (totalMb != null) {
                            String.format(Locale.US, "%.1f / %.1f MB", downloadedMb, totalMb)
                        } else {
                            String.format(Locale.US, "%.1f MB", downloadedMb)
                        },
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = String.format(Locale.US, "%.1f Mbps", progress.currentSpeedKbps / 1000.0),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cancel_batch_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cancel Batch (Keep Completed Runs)")
            }
        }
    }
}

@Composable
private fun SingleProgressCard(
    progress: DownloadProgress?,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Downloading Test File...",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }

            if (progress != null) {
                val progressFraction = if (progress.totalBytesExpected > 0) {
                    (progress.bytesDownloaded.toFloat() / progress.totalBytesExpected.toFloat()).coerceIn(0f, 1f)
                } else null

                if (progressFraction != null) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val downloadedMb = progress.bytesDownloaded / (1024.0 * 1024.0)
                    Text(
                        text = String.format(Locale.US, "%.2f MB", downloadedMb),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f Mbps", progress.currentSpeedKbps / 1000.0),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cancel Download")
            }
        }
    }
}

// ==========================================
// SECTION 4: BATCH SUMMARY & INLINE BAR CHART
// ==========================================

@Composable
private fun BatchSummarySection(summary: BatchSummary) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Batch Test Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${summary.completedRunsCount}/${summary.totalRunsPlanned} Runs",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Summary Statistics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetricItem(
                    label = "Average",
                    value = String.format(Locale.US, "%.2f Mbps", summary.avgThroughputMbps),
                    color = MaterialTheme.colorScheme.primary
                )
                SummaryMetricItem(
                    label = "Min",
                    value = String.format(Locale.US, "%.2f Mbps", summary.minThroughputMbps),
                    color = MaterialTheme.colorScheme.outline
                )
                SummaryMetricItem(
                    label = "Max",
                    value = String.format(Locale.US, "%.2f Mbps", summary.maxThroughputMbps),
                    color = Color(0xFF2E7D32)
                )
                SummaryMetricItem(
                    label = "Std Dev (±)",
                    value = String.format(Locale.US, "%.2f Mbps", summary.stdDevThroughputMbps),
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Avg Signal Strength",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = summary.signalSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (summary.testConditionNote.isNotBlank()) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Condition Note",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = summary.testConditionNote,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Inline Bar Chart showing throughput per run
            Text(
                text = "Throughput per Run (Mbps):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )

            BatchThroughputBarChart(
                runs = summary.runs,
                avgThroughputKbps = summary.avgThroughputKbps
            )
        }
    }
}

/**
 * Lightweight, zero-dependency inline bar chart rendered directly using Compose Canvas.
 */
@Composable
private fun BatchThroughputBarChart(
    runs: List<TestRunResult>,
    avgThroughputKbps: Double
) {
    if (runs.isEmpty()) return

    val maxThroughputKbps = maxOf(runs.maxOf { it.throughputKbps }, 1.0)
    val barColor = MaterialTheme.colorScheme.primary
    val avgLineColor = MaterialTheme.colorScheme.tertiary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(10.dp, 3.dp).background(avgLineColor))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = String.format(Locale.US, "Avg (%.1f Mbps)", avgThroughputKbps / 1000.0),
                style = MaterialTheme.typography.labelSmall,
                color = avgLineColor,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Canvas Bars
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartWidth = size.width
                val chartHeight = size.height - 24f // leave room for labels
                val count = runs.size
                val barSpacing = 8.dp.toPx()
                val totalSpacing = barSpacing * (count + 1)
                val barWidth = ((chartWidth - totalSpacing) / count).coerceIn(12f, 40f)

                // Draw Average dashed line
                val avgY = chartHeight - ((avgThroughputKbps / maxThroughputKbps).toFloat() * chartHeight)
                drawLine(
                    color = avgLineColor,
                    start = Offset(0f, avgY),
                    end = Offset(chartWidth, avgY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                // Draw each bar
                runs.forEachIndexed { index, run ->
                    val x = barSpacing + index * (barWidth + barSpacing)
                    val fraction = (run.throughputKbps / maxThroughputKbps).toFloat().coerceIn(0.02f, 1f)
                    val barHeight = fraction * chartHeight
                    val y = chartHeight - barHeight

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }

            // Labels under bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                runs.forEachIndexed { index, run ->
                    Text(
                        text = "R${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricItem(
    label: String,
    value: String,
    color: Color
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun SingleTestResultCard(result: TestRunResult) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Single Test Result",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Throughput", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = String.format(Locale.US, "%.2f Mbps", result.throughputMbps),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Duration / Size", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = String.format(Locale.US, "%.2fs / %.1f MB", result.totalTimeSeconds, result.fileSizeMb),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ==========================================
// SECTION 5: LOGS & HISTORY
// ==========================================

@Composable
private fun LogsHistorySection(
    recentRuns: List<TestRunResult>,
    onExportCsv: () -> Unit,
    onClearLogs: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Benchmark Logs (${recentRuns.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "Schema includes: timestamp, file_url, size, network, signal_strength, time, throughput, battery, condition_note, batch_id, run_number.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExportCsv,
                    modifier = Modifier.weight(1f).testTag("export_csv_section_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export / Share CSV")
                }
                OutlinedButton(
                    onClick = onClearLogs,
                    modifier = Modifier.testTag("clear_logs_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear")
                }
            }

            if (recentRuns.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Recent Test Runs:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                recentRuns.takeLast(8).reversed().forEach { run ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${run.networkType} (${run.signalStrength})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f Mbps", run.throughputMbps),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${run.timestamp} | ${String.format(Locale.US, "%.1fs", run.totalTimeSeconds)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (run.runNumberInBatch.isNotBlank()) {
                                    Text(
                                        text = "Batch ${run.runNumberInBatch}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            if (run.testConditionNote.isNotBlank()) {
                                Text(
                                    text = "Tag: ${run.testConditionNote}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss error")
            }
        }
    }
}
