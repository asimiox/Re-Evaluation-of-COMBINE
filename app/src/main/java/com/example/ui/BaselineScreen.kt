package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.download.DownloadProgress
import com.example.model.BatchSummary
import com.example.model.TestRunResult
import com.example.network.SignalQuality
import com.example.ui.components.CombineLogoC
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGoldBorder
import com.example.ui.theme.AccentGoldLight
import com.example.ui.theme.AlertErrorBorder
import com.example.ui.theme.AlertErrorContainer
import com.example.ui.theme.AlertErrorText
import com.example.ui.theme.AlertSuccessContainer
import com.example.ui.theme.AlertSuccessText
import com.example.ui.theme.AlertWarningBorder
import com.example.ui.theme.AlertWarningContainer
import com.example.ui.theme.AlertWarningGold
import com.example.ui.theme.AlertWarningText
import com.example.ui.theme.BentoCardBorder
import com.example.ui.theme.BentoCardSurface
import com.example.ui.theme.BentoCardSurfaceSubtle
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyDominant
import com.example.ui.theme.NavyLightContainer
import com.example.ui.theme.NavySecondary
import com.example.ui.theme.OnAccentGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextNavy
import java.util.Locale

/**
 * Bento Grid UI for COMBINE with the 60-30-10 design system:
 * - 60% Dominant: Slate canvas + Crisp white bento cards
 * - 30% Secondary: Midnight Navy for structure, telemetry labels, and typography
 * - 10% Accent: Warm Gold for CTAs, active highlights, and "C" logo accent
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaselineScreen(viewModel: BaselineViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
    ) { perms ->
        hasPhonePermission = perms[Manifest.permission.READ_PHONE_STATE] == true ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
        viewModel.refreshDeviceInfo()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshDeviceInfo()
        viewModel.refreshLogs()
    }

    var selectedTestTab by rememberSaveable { mutableIntStateOf(0) } // 0: Batch, 1: Single

    Scaffold(
        containerColor = CanvasBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Letter "C" Brand Monogram
                        CombineLogoC(size = 32.dp)
                        Column {
                            Text(
                                text = "COMBINE",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.5.sp,
                                    color = NavyDominant
                                )
                            )
                            Text(
                                text = "Network Baseline Suite",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshDeviceInfo() },
                        modifier = Modifier.testTag("refresh_device_state_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Telemetry",
                            tint = NavySecondary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.exportCsv() },
                        modifier = Modifier.testTag("top_bar_export_csv")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export CSV",
                            tint = NavySecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BentoCardSurface,
                    titleContentColor = NavyDominant
                )
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
                    .widthIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ==========================================
                // BENTO TILE 1: HERO DEVICE & STATUS BANNER
                // ==========================================
                BentoHeroDeviceCard(
                    uiState = uiState,
                    hasPermission = hasPhonePermission,
                    onRequestPermission = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_PHONE_STATE,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                    }
                )

                // ==========================================
                // BENTO ROW 2: TELEMETRY SPLIT (2 COLUMNS)
                // ==========================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Bento: Network Radio
                    BentoNetworkTile(
                        modifier = Modifier.weight(1f),
                        uiState = uiState
                    )

                    // Right Bento: Power & Charging
                    BentoBatteryTile(
                        modifier = Modifier.weight(1f),
                        uiState = uiState
                    )
                }

                // ==========================================
                // BENTO TILE 3: CONDITIONAL SAFEGUARDS
                // ==========================================
                if (uiState.isCharging || uiState.chargingWarning != null) {
                    BentoChargingWarningCard()
                }

                if (uiState.isWiFiActive) {
                    BentoWiFiNoticeCard()
                }

                if (uiState.errorMessage != null) {
                    BentoErrorCard(message = uiState.errorMessage ?: "")
                }

                // ==========================================
                // BENTO TILE 4: LIVE BENCHMARK (ACTIVE STATUS)
                // ==========================================
                if (uiState.isBatchRunning || uiState.isSingleDownloading) {
                    BentoLiveBenchmarkCard(
                        uiState = uiState,
                        onCancel = {
                            if (uiState.isBatchRunning) viewModel.cancelBatchTest()
                            else viewModel.cancelSingleTest()
                        }
                    )
                }

                // ==========================================
                // BENTO TILE 5: TEST CONFIGURATION (PAYLOADS)
                // ==========================================
                BentoPayloadConfigCard(
                    uiState = uiState,
                    onSelectPreset = { viewModel.selectPresetFile(it) },
                    onUrlChanged = { viewModel.onUrlChanged(it) },
                    onConditionNoteChanged = { viewModel.setConditionNote(it) }
                )

                // ==========================================
                // BENTO TILE 6: BENCHMARK CONTROLLER (10% CTA)
                // ==========================================
                BentoRunControllerCard(
                    uiState = uiState,
                    selectedTab = selectedTestTab,
                    onTabSelected = { selectedTestTab = it },
                    onBatchCountChanged = { viewModel.setBatchRunsCount(it) },
                    onStartBatch = { viewModel.startBatchTest(resume = false) },
                    onResumeBatch = { viewModel.startBatchTest(resume = true) },
                    onResetBatch = { viewModel.resetBatchSession() },
                    onStartSingle = { viewModel.startSingleTest() }
                )

                // ==========================================
                // BENTO TILE 7: BATCH SUMMARY (AFTER TEST)
                // ==========================================
                if (uiState.latestBatchSummary != null) {
                    BentoBatchSummaryCard(summary = uiState.latestBatchSummary!!)
                }

                // ==========================================
                // BENTO TILE 8: LOGS & HISTORY
                // ==========================================
                BentoLogsHistoryCard(
                    recentRuns = uiState.recentRuns,
                    onExportCsv = { viewModel.exportCsv() },
                    onClearLogs = { viewModel.clearLogHistory() }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ============================================================================
// BENTO COMPONENTS (Guided Bento Grid Architecture)
// ============================================================================

/**
 * Bento Tile 1: Top Hero Brand & Device Banner
 */
@Composable
private fun BentoHeroDeviceCard(
    uiState: BaselineUiState,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BentoCardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BentoCardBorder, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Modern Letter "C" Emblem
                    CombineLogoC(
                        size = 46.dp,
                        containerColor = NavyDark,
                        accentColor = AccentGold
                    )

                    Column {
                        Text(
                            text = if (uiState.isEmulator) "Cloud Datacenter Preview" else uiState.deviceDescription,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextNavy
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (uiState.isEmulator) "Google Cloud Virtual Host" else "📱 Physical Hardware Verified",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (uiState.isEmulator) AlertWarningGold else AlertSuccessText,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                // Live Radio Status Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (uiState.isWiFiActive) NavyLightContainer else AccentGoldLight)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (uiState.signalQuality) {
                                        SignalQuality.STRONG -> Color(0xFF10B981)
                                        SignalQuality.MEDIUM -> AccentGold
                                        SignalQuality.WEAK -> Color(0xFFEF4444)
                                        SignalQuality.UNKNOWN -> NavySecondary
                                    }
                                )
                        )
                        Text(
                            text = uiState.liveNetworkType.ifBlank { "Radio Idle" },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isWiFiActive) NavyDominant else OnAccentGold
                            )
                        )
                    }
                }
            }

            if (!hasPermission && !uiState.isEmulator) {
                HorizontalDivider(color = BentoCardBorder)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = NavySecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Grant Phone State to read LTE/5G carrier dBm",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                        )
                    }
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NavyDominant,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Allow", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

/**
 * Bento Tile 2A: Network Telemetry Card
 */
@Composable
private fun BentoNetworkTile(
    modifier: Modifier = Modifier,
    uiState: BaselineUiState
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BentoCardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BentoCardBorder, RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE RADIO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextMuted
                    )
                )
                Icon(
                    imageVector = if (uiState.isWiFiActive) Icons.Default.Wifi else Icons.Default.NetworkCheck,
                    contentDescription = null,
                    tint = NavySecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = uiState.liveNetworkType.ifBlank { "Detecting..." },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = TextNavy
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = uiState.liveSignalStrength.ifBlank { "N/A" },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = when (uiState.signalQuality) {
                            SignalQuality.STRONG -> Color(0xFF047857)
                            SignalQuality.MEDIUM -> AlertWarningGold
                            SignalQuality.WEAK -> Color(0xFFB91C1C)
                            SignalQuality.UNKNOWN -> TextMuted
                        }
                    )
                )
                if (uiState.carrierName != null) {
                    Text(
                        text = "• ${uiState.carrierName}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Bento Tile 2B: Battery & Power Card
 */
@Composable
private fun BentoBatteryTile(
    modifier: Modifier = Modifier,
    uiState: BaselineUiState
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BentoCardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BentoCardBorder, RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "POWER STATE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextMuted
                    )
                )
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = if (uiState.isCharging) AlertWarningGold else NavySecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = "${uiState.liveBatteryPercentage}%",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = TextNavy
                )
            )

            // Dynamic Charging Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (uiState.isCharging) AlertWarningContainer else AlertSuccessContainer)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (uiState.isCharging) "⚡ Charging" else "🔋 On Battery",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.isCharging) AlertWarningText else AlertSuccessText
                    )
                )
            }
        }
    }
}

/**
 * Bento Tile 3A: Charging Warning Banner (60-30-10 Warning Styling)
 */
@Composable
private fun BentoChargingWarningCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AlertWarningContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AlertWarningBorder, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(AlertWarningBorder),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = AlertWarningGold,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Charger Plugged In",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = AlertWarningText
                    )
                )
                Text(
                    text = "Device is charging — battery depletion data for this run will not be reliable.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = AlertWarningText.copy(alpha = 0.9f)
                    )
                )
            }
        }
    }
}

/**
 * Bento Tile 3B: Wi-Fi Notice Banner
 */
@Composable
private fun BentoWiFiNoticeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyLightContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BentoCardBorder, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = null,
                tint = NavySecondary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Currently on Wi-Fi — disable Wi-Fi in quick settings if you want to benchmark 4G/5G cellular.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = NavyDominant,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

/**
 * Bento Tile 3C: Error Card
 */
@Composable
private fun BentoErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AlertErrorContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AlertErrorBorder, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = AlertErrorText,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = AlertErrorText,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

/**
 * Bento Tile 4: Test Payload & Conditions Bento Card
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BentoPayloadConfigCard(
    uiState: BaselineUiState,
    onSelectPreset: (PresetFile) -> Unit,
    onUrlChanged: (String) -> Unit,
    onConditionNoteChanged: (String) -> Unit
) {
    var showCustomUrl by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BentoCardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BentoCardBorder, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = NavyDominant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Benchmark Payloads",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextNavy
                        )
                    )
                }
                Text(
                    text = "Cloudflare Edge",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = AccentGold
                    )
                )
            }

            // Bento 2x2 Grid for Preset Files
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HETZNER_PRESET_FILES.take(2).forEach { preset ->
                        BentoPresetTile(
                            modifier = Modifier.weight(1f),
                            preset = preset,
                            isSelected = uiState.selectedPresetId == preset.id,
                            onClick = { onSelectPreset(preset) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HETZNER_PRESET_FILES.drop(2).take(2).forEach { preset ->
                        BentoPresetTile(
                            modifier = Modifier.weight(1f),
                            preset = preset,
                            isSelected = uiState.selectedPresetId == preset.id,
                            onClick = { onSelectPreset(preset) }
                        )
                    }
                }
            }

            // Condition Tag Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "ENVIRONMENT TAG",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextMuted
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CONDITION_NOTE_PRESETS.forEach { tag ->
                        val isSelected = uiState.conditionNote.equals(tag, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onConditionNoteChanged(tag) },
                            label = { Text(tag) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = NavyDominant
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentGoldLight,
                                selectedLabelColor = NavyDominant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = BentoCardBorder,
                                selectedBorderColor = AccentGoldBorder
                            )
                        )
                    }
                }
            }

            // Custom Direct Link Dropdown (for advanced users)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCustomUrl = !showCustomUrl },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Custom Direct Link...",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = NavySecondary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Icon(
                    imageVector = if (showCustomUrl) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = NavySecondary
                )
            }

            if (showCustomUrl) {
                OutlinedTextField(
                    value = uiState.fileUrl,
                    onValueChange = onUrlChanged,
                    label = { Text("Direct Download URL") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("file_url_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGold,
                        unfocusedBorderColor = BentoCardBorder
                    )
                )
            }
        }
    }
}

/**
 * Individual Preset Tile in the Bento Grid
 */
@Composable
private fun BentoPresetTile(
    modifier: Modifier = Modifier,
    preset: PresetFile,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) AccentGold else BentoCardBorder
    val bgColor = if (isSelected) AccentGoldLight else BentoCardSurfaceSubtle

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = preset.sizeLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) NavyDominant else TextNavy
                    )
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(AccentGold),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
            Text(
                text = preset.label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextNavy
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = preset.description,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = TextMuted
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Bento Tile 5: Benchmark Controller & 10% Gold Accent CTA
 */
@Composable
private fun BentoRunControllerCard(
    uiState: BaselineUiState,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onBatchCountChanged: (Int) -> Unit,
    onStartBatch: () -> Unit,
    onResumeBatch: () -> Unit,
    onResetBatch: () -> Unit,
    onStartSingle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BentoCardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BentoCardBorder, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Mode Tabs (Batch vs Single)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = BentoCardSurfaceSubtle,
                contentColor = NavyDominant,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = AccentGold,
                        height = 3.dp
                    )
                },
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { if (!uiState.isAnyTestRunning) onTabSelected(0) },
                    enabled = !uiState.isAnyTestRunning,
                    text = {
                        Text(
                            text = "Batch Benchmark",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 0) NavyDominant else TextMuted
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { if (!uiState.isAnyTestRunning) onTabSelected(1) },
                    enabled = !uiState.isAnyTestRunning,
                    text = {
                        Text(
                            text = "Single Test",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 1) NavyDominant else TextMuted
                        )
                    }
                )
            }

            if (selectedTab == 0) {
                // Incomplete Batch Resumption Card
                if (uiState.canResumeBatch && uiState.activeBatchRuns.isNotEmpty() && uiState.activeBatchRuns.size < uiState.totalBatchRunsPlanned) {
                    val done = uiState.activeBatchRuns.size
                    val total = uiState.totalBatchRunsPlanned
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = AccentGoldLight)
                    ) {
                        Column(
                            modifier = Modifier
                                .border(1.dp, AccentGoldBorder, RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Incomplete Batch ($done of $total completed)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = OnAccentGold
                                )
                            )
                            Text(
                                text = "Resume remaining runs with the same batch_id, or discard to start a new batch.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextNavy)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onResumeBatch,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("resume_batch_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentGold,
                                        contentColor = OnAccentGold
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Resume (Run ${done + 1})", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = onResetBatch,
                                    modifier = Modifier
                                        .height(44.dp)
                                        .testTag("reset_batch_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("New Batch", color = NavyDominant)
                                }
                            }
                        }
                    }
                }

                // Batch Runs Count Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REPEAT ITERATIONS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextMuted
                        )
                    )
                    Text(
                        text = "${uiState.batchRunsPlannedCount} runs",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = NavyDominant
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(3, 5, 10, 20, 30).forEach { count ->
                        val isSelected = count == uiState.batchRunsPlannedCount
                        FilterChip(
                            selected = isSelected,
                            onClick = { onBatchCountChanged(count) },
                            label = { Text("$count") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyDominant,
                                selectedLabelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = BentoCardBorder,
                                selectedBorderColor = NavyDominant
                            )
                        )
                    }
                }

                Text(
                    text = "Runs sequentially with 3-second recovery interval. Real-time metrics appended to CSV.",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                )

                // 10% Accent CTA Button: START BATCH
                Button(
                    onClick = onStartBatch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_batch_button"),
                    enabled = !uiState.isAnyTestRunning,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGold,
                        contentColor = OnAccentGold,
                        disabledContainerColor = BentoCardBorder,
                        disabledContentColor = TextMuted
                    )
                ) {
                    CombineLogoC(
                        size = 24.dp,
                        containerColor = NavyDark,
                        accentColor = AccentGold,
                        showBorder = false
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "START BATCH (${uiState.batchRunsPlannedCount} RUNS)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            } else {
                // Single Test Mode
                Text(
                    text = "Runs a single immediate benchmark download to measure instantaneous peak throughput and battery.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )

                Button(
                    onClick = onStartSingle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_single_download_button"),
                    enabled = !uiState.isAnyTestRunning,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGold,
                        contentColor = OnAccentGold,
                        disabledContainerColor = BentoCardBorder,
                        disabledContentColor = TextMuted
                    )
                ) {
                    Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = OnAccentGold)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "START SINGLE TEST",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Bento Tile 6: Real-Time On-The-Fly Benchmark Loading Card
 */
@Composable
private fun BentoLiveBenchmarkCard(
    uiState: BaselineUiState,
    onCancel: () -> Unit
) {
    val progress = if (uiState.isBatchRunning) uiState.currentBatchDownloadProgress else uiState.singleProgress
    val isBatch = uiState.isBatchRunning
    val currentRun = uiState.currentBatchRunNumber
    val totalRuns = uiState.totalBatchRunsPlanned
    val countdown = uiState.batchCountdownSeconds

    // On-the-fly status calculation based on real-time stream state
    val onTheFlyStatus = when {
        countdown > 0 -> "Run $currentRun completed. Cooling down (${countdown}s) before next run..."
        progress == null -> "Resolving Anycast Edge route & initializing socket..."
        progress.bytesDownloaded == 0L -> "Handshaking TLS 1.3 & opening HTTP/2 stream..."
        progress.totalBytesExpected > 0 && progress.bytesDownloaded < progress.totalBytesExpected * 0.35 ->
            "Receiving initial payload chunks from edge mirror..."
        progress.totalBytesExpected > 0 && progress.bytesDownloaded < progress.totalBytesExpected * 0.85 ->
            "Streaming high-throughput binary payload..."
        else -> "Finalizing stream bytes & calculating telemetry..."
    }

    val currentSpeedMbps = (progress?.currentSpeedKbps ?: 0.0) / 1000.0

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BentoCardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, AccentGold.copy(alpha = borderAlpha), RoundedCornerShape(20.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.5.dp,
                        color = AccentGold
                    )
                    Text(
                        text = if (isBatch) "BATCH IN PROGRESS • RUN $currentRun OF $totalRuns" else "SINGLE BENCHMARK ACTIVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = NavyDominant,
                            letterSpacing = 1.sp
                        )
                    )
                }

                if (countdown > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentGoldLight)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Cooldown ${countdown}s",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = OnAccentGold
                            )
                        )
                    }
                }
            }

            // Big Live Speedometer Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "LIVE THROUGHPUT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format(Locale.US, "%.1f", currentSpeedMbps),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = NavyDominant
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mbps",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AccentGold
                            ),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                if (progress != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        val downloadedMb = progress.bytesDownloaded / (1024.0 * 1024.0)
                        val totalMb = if (progress.totalBytesExpected > 0) progress.totalBytesExpected / (1024.0 * 1024.0) else null
                        Text(
                            text = if (totalMb != null) String.format(Locale.US, "%.1f / %.1f MB", downloadedMb, totalMb)
                            else String.format(Locale.US, "%.1f MB", downloadedMb),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextNavy)
                        )
                        Text(
                            text = String.format(Locale.US, "Elapsed: %.1fs", progress.elapsedSeconds),
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                        )
                    }
                }
            }

            // Animated Live Progress Bar
            if (progress != null && progress.totalBytesExpected > 0) {
                val fraction = (progress.bytesDownloaded.toFloat() / progress.totalBytesExpected.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = AccentGold,
                    trackColor = BentoCardSurfaceSubtle
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = AccentGold,
                    trackColor = BentoCardSurfaceSubtle
                )
            }

            // On-The-Fly Status Ticker Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NavyLightContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = NavyDominant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = onTheFlyStatus,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = NavyDominant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Cancel Action
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertErrorText),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(AlertErrorBorder))
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cancel Benchmark", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Bento Tile 7: Batch Analytics & Summary Card
 */
@Composable
private fun BentoBatchSummaryCard(summary: BatchSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BentoCardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BentoCardBorder, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Dataset,
                        contentDescription = null,
                        tint = NavyDominant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Batch Analytics",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextNavy
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentGoldLight)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${summary.completedRunsCount} of ${summary.totalRunsPlanned} Runs Done",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = OnAccentGold
                        )
                    )
                }
            }

            // Metric Bento Tiles Row (Mean, Min/Max, StdDev)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mean Speed
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoCardSurfaceSubtle)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "MEAN SPEED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f", summary.avgThroughputMbps),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = AccentGold
                            )
                        )
                        Text("Mbps", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                    }
                }

                // Min / Max
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoCardSurfaceSubtle)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "MIN / MAX",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f - %.1f", summary.minThroughputMbps, summary.maxThroughputMbps),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextNavy
                            )
                        )
                        Text("Mbps Range", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                    }
                }

                // Jitter / StdDev
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoCardSurfaceSubtle)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "JITTER / STD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                        )
                        Text(
                            text = String.format(Locale.US, "±%.1f", summary.stdDevThroughputMbps),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextNavy
                            )
                        )
                        Text("Mbps StdDev", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                    }
                }
            }
        }
    }
}

/**
 * Bento Tile 8: Log History & CSV Export Bento Card
 */
@Composable
private fun BentoLogsHistoryCard(
    recentRuns: List<TestRunResult>,
    onExportCsv: () -> Unit,
    onClearLogs: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BentoCardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BentoCardBorder, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = NavyDominant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Benchmark History",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextNavy
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NavyLightContainer)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${recentRuns.size} Records",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = NavyDominant
                        )
                    )
                }
            }

            // Export Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExportCsv,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("export_csv_section_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NavyDominant,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export / Share CSV", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onClearLogs,
                    modifier = Modifier
                        .height(46.dp)
                        .testTag("clear_logs_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertErrorText)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
                }
            }

            // Recent Runs List Items
            if (recentRuns.isNotEmpty()) {
                HorizontalDivider(color = BentoCardBorder)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentRuns.takeLast(10).reversed().forEach { run ->
                        BentoRunHistoryItem(run = run)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No runs logged yet. Start a benchmark above.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }
            }
        }
    }
}

/**
 * Individual Run Item inside the History Bento Card
 */
@Composable
private fun BentoRunHistoryItem(run: TestRunResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BentoCardSurfaceSubtle)
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, BentoCardBorder, RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = run.networkType,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextNavy)
                    )
                    Text(
                        text = "(${run.signalStrength})",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                    )
                }

                // Speed Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentGoldLight)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f Mbps", run.throughputMbps),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = OnAccentGold
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${run.timestamp} • ${String.format(Locale.US, "%.1fs", run.totalTimeSeconds)}",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                )

                if (run.runNumberInBatch.isNotBlank()) {
                    Text(
                        text = "Run ${run.runNumberInBatch}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = NavySecondary
                        )
                    )
                }
            }

            // Bottom Badges Row: Condition Tag & Charging Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (run.testConditionNote.isNotBlank()) {
                    Text(
                        text = "Tag: ${run.testConditionNote}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (run.isCharging) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AlertWarningContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "⚡ Plugged In (Charging)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AlertWarningText
                            )
                        )
                    }
                }
            }
        }
    }
}
