package com.example.ui.cooperative

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CooperativeBatchSummary
import com.example.model.CooperativeRunResult
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CooperativeScreen(viewModel: CooperativeViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshDeviceInfo()
        viewModel.refreshLogs()
    }

    Scaffold(
        containerColor = CanvasBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CombineLogoC(size = 32.dp)
                        Column {
                            Text(
                                text = "COOPERATIVE DOWNLOAD",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.2.sp,
                                    color = NavyDominant
                                )
                            )
                            Text(
                                text = "Step 3 • Dual-Phone Multi-Link Pooling",
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
                        modifier = Modifier.testTag("refresh_coop_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NavySecondary)
                    }
                    IconButton(
                        onClick = { viewModel.exportCsv() },
                        modifier = Modifier.testTag("export_coop_csv_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export CSV", tint = NavySecondary)
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
                // BENTO TILE 1: HERO DUAL-LINK & P2P STATUS
                // ==========================================
                BentoCoopHeroCard(uiState = uiState)

                // Charging Warning Banner
                if (uiState.isCharging || uiState.chargingWarning != null) {
                    BentoCoopChargingWarningBanner()
                }

                // General Error Banner
                if (uiState.errorMessage != null) {
                    BentoCoopErrorBanner(message = uiState.errorMessage ?: "")
                }

                // ==========================================
                // BENTO TILE 2: MODE & ROLE SELECTOR
                // ==========================================
                BentoCoopModeRoleCard(
                    uiState = uiState,
                    onSelectMode = { viewModel.selectMode(it) },
                    onSelectRole = { viewModel.selectRole(it) }
                )

                // ==========================================
                // BENTO TILE 3: PAYLOAD & CHUNK SIZE
                // ==========================================
                BentoCoopPayloadCard(
                    uiState = uiState,
                    onSelectPreset = { viewModel.selectPreset(it) },
                    onUrlChanged = { viewModel.onUrlChanged(it) },
                    onSelectChunkSize = { viewModel.selectChunkSize(it) },
                    onConditionChanged = { viewModel.setConditionNote(it) }
                )

                // ==========================================
                // BENTO TILE 4: BENCHMARK CONTROLLER (10% CTA)
                // ==========================================
                BentoCoopControllerCard(
                    uiState = uiState,
                    onBatchCountChanged = { viewModel.setBatchRunsCount(it) },
                    onStartBenchmark = { viewModel.startBenchmark(resume = false) },
                    onResumeBenchmark = { viewModel.startBenchmark(resume = true) },
                    onResetBatch = { viewModel.resetBatchSession() }
                )

                // ==========================================
                // BENTO TILE 5: LIVE PROGRESS & DUAL-LINK TELEMETRY
                // ==========================================
                if (uiState.isRunning) {
                    BentoCoopLiveProgressCard(
                        uiState = uiState,
                        onCancel = { viewModel.cancelBenchmark() }
                    )
                }

                // ==========================================
                // BENTO TILE 6: BATCH SUMMARY & SPEEDUP
                // ==========================================
                if (uiState.latestBatchSummary != null) {
                    BentoCoopBatchSummaryCard(summary = uiState.latestBatchSummary!!)
                }

                // ==========================================
                // BENTO TILE 7: COOPERATIVE LOGS HISTORY
                // ==========================================
                BentoCoopLogsHistoryCard(
                    recentRuns = uiState.recentRuns,
                    onExportCsv = { viewModel.exportCsv() },
                    onClearLogs = { viewModel.clearLogs() }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ============================================================================
// BENTO TILES
// ============================================================================

@Composable
private fun BentoCoopHeroCard(uiState: CooperativeUiState) {
    val p2p = uiState.p2pState

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
                    CombineLogoC(
                        size = 46.dp,
                        containerColor = NavyDark,
                        accentColor = AccentGold
                    )
                    Column {
                        Text(
                            text = if (uiState.mode == CoopMode.SOLO) "Solo Download Mode" else "Cooperative Pooled Mode",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextNavy)
                        )
                        Text(
                            text = if (uiState.mode == CoopMode.SOLO) "Single Device • Range Engine" else "Dual-Interface • WiFi + Cellular P2P",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (uiState.mode == CoopMode.SOLO) NavySecondary else AccentGold
                            )
                        )
                    }
                }

                // Wi-Fi Direct Status Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (p2p.isConnected) AlertSuccessContainer else NavyLightContainer)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (p2p.isConnected) Color(0xFF10B981) else Color(0xFF94A3B8))
                        )
                        Text(
                            text = if (p2p.isConnected) "P2P Paired" else "P2P Disconnected",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (p2p.isConnected) AlertSuccessText else NavyDominant
                            )
                        )
                    }
                }
            }

            HorizontalDivider(color = BentoCardBorder)

            // Multi-Interface Telemetry Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Coordinator Network
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = NavyDominant, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Coordinator: ${uiState.coordinatorNetwork}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextNavy)
                    )
                }

                // Helper Cellular
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Smartphone, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Helper: ${if (p2p.isConnected) "Cellular Active" else "Ready on Step 2"}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = if (p2p.isConnected) Color(0xFF047857) else TextMuted)
                    )
                }
            }
        }
    }
}

@Composable
private fun BentoCoopChargingWarningBanner() {
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
            Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = AlertWarningGold, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Charger Plugged In",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = AlertWarningText)
                )
                Text(
                    text = "Device is charging — battery depletion data for this run will not be reliable.",
                    style = MaterialTheme.typography.bodySmall.copy(color = AlertWarningText)
                )
            }
        }
    }
}

@Composable
private fun BentoCoopErrorBanner(message: String) {
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
            Icon(Icons.Default.Warning, contentDescription = null, tint = AlertErrorText, modifier = Modifier.size(20.dp))
            Text(text = message, style = MaterialTheme.typography.bodySmall.copy(color = AlertErrorText, fontWeight = FontWeight.SemiBold))
        }
    }
}

/**
 * Bento Tile 2: Mode & Role Selector
 */
@Composable
private fun BentoCoopModeRoleCard(
    uiState: CooperativeUiState,
    onSelectMode: (CoopMode) -> Unit,
    onSelectRole: (CoopRole) -> Unit
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
            Text(
                text = "Download Mode",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextNavy)
            )

            // Mode Toggle Row (Solo vs Pooled)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val isSolo = uiState.mode == CoopMode.SOLO
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSolo) AccentGoldLight else BentoCardSurfaceSubtle)
                        .border(if (isSolo) 1.5.dp else 1.dp, if (isSolo) AccentGold else BentoCardBorder, RoundedCornerShape(14.dp))
                        .clickable { onSelectMode(CoopMode.SOLO) }
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = if (isSolo) NavyDominant else TextMuted, modifier = Modifier.size(16.dp))
                            Text("1. Solo Mode", fontWeight = FontWeight.Bold, color = TextNavy)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Coordinator alone (Baseline for speedup)", fontSize = 10.sp, color = TextMuted)
                    }
                }

                val isPooled = uiState.mode == CoopMode.POOLED
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isPooled) AccentGoldLight else BentoCardSurfaceSubtle)
                        .border(if (isPooled) 1.5.dp else 1.dp, if (isPooled) AccentGold else BentoCardBorder, RoundedCornerShape(14.dp))
                        .clickable { onSelectMode(CoopMode.POOLED) }
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Hub, contentDescription = null, tint = if (isPooled) NavyDominant else TextMuted, modifier = Modifier.size(16.dp))
                            Text("2. Pooled Mode", fontWeight = FontWeight.Bold, color = TextNavy)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Cooperative WiFi + Cellular over P2P", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }

            // Role Toggle (When Pooled is active)
            if (uiState.mode == CoopMode.POOLED) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "THIS PHONE'S ROLE IN POOL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextMuted
                        )
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val isCoord = uiState.role == CoopRole.COORDINATOR
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCoord) NavyLightContainer else BentoCardSurfaceSubtle)
                                .border(if (isCoord) 1.5.dp else 1.dp, if (isCoord) NavyDominant else BentoCardBorder, RoundedCornerShape(12.dp))
                                .clickable { onSelectRole(CoopRole.COORDINATOR) }
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("Phone A (Coordinator)", fontWeight = FontWeight.Bold, color = TextNavy)
                                Text("Uses WiFi • Controls work queue", fontSize = 10.sp, color = TextMuted)
                            }
                        }

                        val isHelper = uiState.role == CoopRole.HELPER
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isHelper) NavyLightContainer else BentoCardSurfaceSubtle)
                                .border(if (isHelper) 1.5.dp else 1.dp, if (isHelper) NavyDominant else BentoCardBorder, RoundedCornerShape(12.dp))
                                .clickable { onSelectRole(CoopRole.HELPER) }
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("Phone B (Helper)", fontWeight = FontWeight.Bold, color = TextNavy)
                                Text("Uses Cellular • Sends via Wi-Fi Direct", fontSize = 10.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bento Tile 3: Payload & Chunk Size Configuration
 */
@Composable
private fun BentoCoopPayloadCard(
    uiState: CooperativeUiState,
    onSelectPreset: (CoopUrlPreset) -> Unit,
    onUrlChanged: (String) -> Unit,
    onSelectChunkSize: (Long) -> Unit,
    onConditionChanged: (String) -> Unit
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
            Text(
                text = "Payload & HTTP Byte-Ranges",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextNavy)
            )

            // Preset Grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                COOP_URL_PRESETS.forEach { preset ->
                    val isSelected = uiState.selectedPresetId == preset.id
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AccentGoldLight else BentoCardSurfaceSubtle)
                            .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) AccentGold else BentoCardBorder, RoundedCornerShape(12.dp))
                            .clickable { onSelectPreset(preset) }
                            .padding(12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(preset.label, fontWeight = FontWeight.Bold, color = TextNavy)
                                Text(preset.description, fontSize = 10.sp, color = TextMuted)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) AccentGold else BentoCardBorder)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = preset.sizeLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) OnAccentGold else TextNavy
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Chunk Size Selector (1 MB, 2 MB, 4 MB)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "BYTE-RANGE CHUNK SIZE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextMuted
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CHUNK_SIZE_PRESETS.forEach { chunk ->
                        val isSelected = uiState.selectedChunkSizeBytes == chunk.bytes
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectChunkSize(chunk.bytes) },
                            label = { Text(chunk.label) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = NavyDominant)
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

            // Custom URL Accordion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCustomUrl = !showCustomUrl },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Custom Range-Enabled URL...", style = MaterialTheme.typography.labelSmall.copy(color = NavySecondary, fontWeight = FontWeight.Bold))
                Icon(imageVector = if (showCustomUrl) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null, tint = NavySecondary)
            }

            if (showCustomUrl) {
                OutlinedTextField(
                    value = uiState.fileUrl,
                    onValueChange = onUrlChanged,
                    label = { Text("Direct File URL (Accept-Ranges required)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
 * Bento Tile 4: Benchmark Controller
 */
@Composable
private fun BentoCoopControllerCard(
    uiState: CooperativeUiState,
    onBatchCountChanged: (Int) -> Unit,
    onStartBenchmark: () -> Unit,
    onResumeBenchmark: () -> Unit,
    onResetBatch: () -> Unit
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
            // Incomplete Batch Resumption Card
            if (uiState.canResumeBatch && uiState.activeBatchRuns.isNotEmpty() && uiState.activeBatchRuns.size < uiState.totalRunsPlanned) {
                val done = uiState.activeBatchRuns.size
                val total = uiState.totalRunsPlanned
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
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = OnAccentGold)
                        )
                        Text(
                            text = "Resume remaining runs with the same batch_id, or discard to start fresh.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextNavy)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onResumeBenchmark,
                                modifier = Modifier.weight(1f).height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = OnAccentGold),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resume (Run ${done + 1})", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onResetBatch,
                                modifier = Modifier.height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("New Batch", color = NavyDominant)
                            }
                        }
                    }
                }
            }

            // Batch Run Count Selector
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
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, color = NavyDominant)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 5, 10).forEach { count ->
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
                text = "Runs with 3s gap. Verifies SHA-256 integrity and appends results to cooperative_logs.csv.",
                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
            )

            // Primary 10% Gold CTA Button
            Button(
                onClick = onStartBenchmark,
                enabled = !uiState.isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_coop_benchmark_btn"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGold,
                    contentColor = OnAccentGold,
                    disabledContainerColor = BentoCardBorder,
                    disabledContentColor = TextMuted
                )
            ) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = OnAccentGold)
                Spacer(modifier = Modifier.width(10.dp))
                val label = when {
                    uiState.mode == CoopMode.SOLO -> "START SOLO BENCHMARK (${uiState.batchRunsPlannedCount} RUNS)"
                    uiState.role == CoopRole.HELPER -> "START HELPER WORKER (CELLULAR)"
                    else -> "START POOLED BENCHMARK (${uiState.batchRunsPlannedCount} RUNS)"
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }
    }
}

/**
 * Bento Tile 5: Live Dual-Link Progress Card
 */
@Composable
private fun BentoCoopLiveProgressCard(
    uiState: CooperativeUiState,
    onCancel: () -> Unit
) {
    val progress = uiState.liveProgress
    val currentRun = uiState.currentRunNumber
    val totalRuns = uiState.totalRunsPlanned
    val countdown = uiState.batchCountdownSeconds
    val currentSpeedMbps = progress?.currentThroughputMbps ?: 0.0

    val infiniteTransition = rememberInfiniteTransition(label = "coop_pulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.5.dp, color = AccentGold)
                    Text(
                        text = if (uiState.role == CoopRole.HELPER) "HELPER WORKER STREAMING" else "RUN $currentRun OF $totalRuns ACTIVE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = NavyDominant, letterSpacing = 1.sp)
                    )
                }

                if (countdown > 0) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(AccentGoldLight).padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(text = "Cooldown ${countdown}s", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = OnAccentGold))
                    }
                }
            }

            // Live Throughput Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "EFFECTIVE THROUGHPUT",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format(Locale.US, "%.1f", currentSpeedMbps),
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 42.sp, fontWeight = FontWeight.Black, color = NavyDominant)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mbps",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = AccentGold),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                if (progress != null) {
                    val downloadedMb = (progress.bytesFromCoordinator + progress.bytesFromHelper) / (1024.0 * 1024.0)
                    val totalMb = progress.bytesTotalExpected / (1024.0 * 1024.0)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format(Locale.US, "%.1f / %.1f MB", downloadedMb, totalMb),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextNavy)
                        )
                        Text(
                            text = String.format(Locale.US, "Elapsed: %.1fs", progress.elapsedSeconds),
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                        )
                    }
                }
            }

            // Dual Link Progress Bars (Coordinator vs Helper)
            if (progress != null && uiState.mode == CoopMode.POOLED) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Coordinator (WiFi): ${(progress.bytesFromCoordinator / (1024.0 * 1024.0)).toInt()} MB (${progress.chunksFromCoordinator} chunks)", fontSize = 11.sp, color = NavyDominant, fontWeight = FontWeight.Bold)
                        Text("Helper (Cellular): ${(progress.bytesFromHelper / (1024.0 * 1024.0)).toInt()} MB (${progress.chunksFromHelper} chunks)", fontSize = 11.sp, color = AccentGold, fontWeight = FontWeight.Bold)
                    }

                    val total = progress.bytesTotalExpected
                    val fractionCoord = (progress.bytesFromCoordinator.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    val fractionHelper = (progress.bytesFromHelper.toFloat() / total.toFloat()).coerceIn(0f, 1f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(BentoCardSurfaceSubtle)
                    ) {
                        if (fractionCoord > 0f) {
                            Box(modifier = Modifier.fillMaxWidth(fractionCoord).height(10.dp).background(NavyDominant))
                        }
                        if (fractionHelper > 0f) {
                            Box(modifier = Modifier.fillMaxWidth(fractionHelper).height(10.dp).background(AccentGold))
                        }
                    }
                }
            } else if (progress != null) {
                // Solo progress bar
                val fraction = (progress.bytesFromCoordinator.toFloat() / progress.bytesTotalExpected.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = AccentGold,
                    trackColor = BentoCardSurfaceSubtle
                )
            }

            // Status Ticker
            val statusMessage = progress?.statusText ?: uiState.helperStatusText.ifBlank { "Initializing benchmark..." }
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(NavyLightContainer).padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = NavyDominant, modifier = Modifier.size(16.dp))
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, color = NavyDominant),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Cancel Button
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertErrorText)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cancel Benchmark", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Bento Tile 6: Batch Summary & Speedup Card
 */
@Composable
private fun BentoCoopBatchSummaryCard(summary: CooperativeBatchSummary) {
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = NavyDominant, modifier = Modifier.size(20.dp))
                    Text(
                        text = "${summary.mode} Summary",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextNavy)
                    )
                }

                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(AccentGoldLight).padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${summary.completedRunsCount} of ${summary.totalRunsPlanned} Runs Done",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = OnAccentGold)
                    )
                }
            }

            // Speedup vs Solo Banner (if Pooled and Solo exists)
            if (summary.mode.equals("Pooled", ignoreCase = true)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AlertSuccessContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("COOPERATIVE SPEEDUP VS SOLO", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AlertSuccessText))
                            val speedupStr = if (summary.speedupFactor != null) {
                                String.format(Locale.US, "%.2fx faster", summary.speedupFactor)
                            } else {
                                "Run Solo first to compute speedup"
                            }
                            Text(speedupStr, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = AlertSuccessText))
                        }
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White).padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "SHA-256: ${summary.hashMatchSummary}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = NavyDominant)
                            )
                        }
                    }
                }
            }

            // Metric Tiles
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Mean Speed
                Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BentoCardSurfaceSubtle)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("MEAN SPEED", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted))
                        Text(String.format(Locale.US, "%.1f", summary.avgThroughputMbps), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = AccentGold))
                        Text("Mbps", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                    }
                }

                // Min / Max
                Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BentoCardSurfaceSubtle)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("MIN / MAX", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted))
                        Text(String.format(Locale.US, "%.1f - %.1f", summary.minThroughputMbps, summary.maxThroughputMbps), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextNavy))
                        Text("Mbps Range", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                    }
                }

                // Jitter
                Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BentoCardSurfaceSubtle)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("JITTER / STD", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted))
                        Text(String.format(Locale.US, "±%.1f", summary.stdDevThroughputMbps), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextNavy))
                        Text("Mbps StdDev", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                    }
                }
            }
        }
    }
}

/**
 * Bento Tile 7: Log History Card
 */
@Composable
private fun BentoCoopLogsHistoryCard(
    recentRuns: List<CooperativeRunResult>,
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.History, contentDescription = null, tint = NavyDominant, modifier = Modifier.size(20.dp))
                    Text("Cooperative Log History", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextNavy))
                }

                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(NavyLightContainer).padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("${recentRuns.size} Records", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = NavyDominant))
                }
            }

            // Export Actions
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onExportCsv,
                    modifier = Modifier.weight(1f).height(46.dp).testTag("export_coop_csv_section_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDominant, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export / Share CSV", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onClearLogs,
                    modifier = Modifier.height(46.dp).testTag("clear_coop_logs_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertErrorText)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
                }
            }

            // Runs List
            if (recentRuns.isNotEmpty()) {
                HorizontalDivider(color = BentoCardBorder)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentRuns.takeLast(10).reversed().forEach { run ->
                        BentoCoopRunItem(run = run)
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No cooperative runs logged yet. Start a benchmark above.", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                }
            }
        }
    }
}

@Composable
private fun BentoCoopRunItem(run: CooperativeRunResult) {
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (run.mode.equals("Pooled", true)) AccentGoldLight else NavyLightContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = run.mode.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = if (run.mode.equals("Pooled", true)) OnAccentGold else NavyDominant)
                        )
                    }
                    Text(
                        text = "${String.format(Locale.US, "%.1f MB", run.fileSizeMb)} • ${String.format(Locale.US, "%.1fs", run.totalTimeSeconds)}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                    )
                }

                // Speed Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (run.isSuccessful) AccentGoldLight else AlertErrorContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (run.isSuccessful) String.format(Locale.US, "%.1f Mbps", run.effectiveThroughputMbps) else "FAILED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (run.isSuccessful) OnAccentGold else AlertErrorText
                        )
                    )
                }
            }

            // Dual Link details
            if (run.mode.equals("Pooled", true)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("WiFi: ${run.bytesFromCoordinatorLink / (1024 * 1024)}MB (${run.chunksFromCoordinator} ch)", fontSize = 11.sp, color = NavyDominant, fontWeight = FontWeight.SemiBold)
                    Text("Cellular: ${run.bytesFromHelperLink / (1024 * 1024)}MB (${run.chunksFromHelper} ch)", fontSize = 11.sp, color = AccentGold, fontWeight = FontWeight.SemiBold)
                    Text("SHA: ${run.hashMatch}", fontSize = 11.sp, color = if (run.hashMatch == "Match") Color(0xFF047857) else TextMuted, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${run.timestamp} • Run ${run.runNumberInBatch}",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                )
                if (run.chunkRetries > 0) {
                    Text("Retries: ${run.chunkRetries}", fontSize = 10.sp, color = AlertWarningText)
                }
            }
        }
    }
}
