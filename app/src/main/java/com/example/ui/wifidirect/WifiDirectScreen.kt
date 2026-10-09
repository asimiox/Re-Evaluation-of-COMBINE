package com.example.ui.wifidirect

import android.Manifest
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDevice
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.WifiDirectBatchSummary
import com.example.model.WifiDirectRunResult
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
fun WifiDirectScreen(viewModel: WifiDirectViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Check permissions
    fun checkHasPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val nearbyDevices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        return fineLocation && nearbyDevices
    }

    var hasPermissions by remember { mutableStateOf(checkHasPermissions()) }
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasPermissions = checkHasPermissions()
        if (hasPermissions) {
            viewModel.p2pManager.register()
        }
    }

    LaunchedEffect(Unit) {
        hasPermissions = checkHasPermissions()
        viewModel.refreshDeviceInfo()
        viewModel.refreshLogs()
    }

    // Permission Explanation Dialog before requesting
    if (showPermissionRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionRationaleDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = NavyDominant)
                    Text("Wi-Fi Direct Permissions", fontWeight = FontWeight.Bold, color = TextNavy)
                }
            },
            text = {
                Text(
                    text = "Wi-Fi Direct requires Nearby Devices and Location permissions to discover and connect to your nearby test phone directly without an internet router or cellular data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextNavy
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationaleDialog = false
                        val permissionsToRequest = mutableListOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
                        }
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDominant, contentColor = Color.White)
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationaleDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = BentoCardSurface
        )
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
                                text = "WI-FI DIRECT P2P",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.2.sp,
                                    color = NavyDominant
                                )
                            )
                            Text(
                                text = "Local Peer-to-Peer Throughput",
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
                        modifier = Modifier.testTag("refresh_p2p_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NavySecondary)
                    }
                    IconButton(
                        onClick = { viewModel.exportCsv() },
                        modifier = Modifier.testTag("export_p2p_csv_btn")
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
                // Permission Warning Banner if missing
                if (!hasPermissions) {
                    P2pPermissionRequiredBanner(onRequestPermission = { showPermissionRationaleDialog = true })
                }

                // ==========================================
                // BENTO TILE 1: P2P STATUS & ROLE HERO CARD
                // ==========================================
                BentoP2pHeroCard(
                    uiState = uiState,
                    onDisconnect = { viewModel.disconnectPeer() }
                )

                // ==========================================
                // BENTO TILE 2: HARDWARE & ENVIRONMENT TELEMETRY
                // ==========================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BentoP2pFrequencyTile(
                        modifier = Modifier.weight(1f),
                        uiState = uiState
                    )
                    BentoP2pBatteryTile(
                        modifier = Modifier.weight(1f),
                        uiState = uiState
                    )
                }

                // Charging Warning Banner
                if (uiState.isCharging || uiState.chargingWarning != null) {
                    BentoP2pChargingWarningBanner()
                }

                // General Error Banner
                if (uiState.errorMessage != null) {
                    BentoP2pErrorBanner(message = uiState.errorMessage ?: "")
                }

                // ==========================================
                // BENTO TILE 3: DISCOVERY & PEER PAIRING
                // ==========================================
                BentoPeerDiscoveryCard(
                    uiState = uiState,
                    hasPermission = hasPermissions,
                    onStartDiscovery = {
                        if (!hasPermissions) {
                            showPermissionRationaleDialog = true
                        } else {
                            viewModel.startPeerDiscovery()
                        }
                    },
                    onStopDiscovery = { viewModel.stopPeerDiscovery() },
                    onConnectPeer = { device -> viewModel.connectToPeer(device) }
                )

                // ==========================================
                // BENTO TILE 4: TEST CONFIGURATION (ROLE & PAYLOAD)
                // ==========================================
                BentoP2pConfigCard(
                    uiState = uiState,
                    onSelectRole = { viewModel.selectRole(it) },
                    onSelectPayload = { viewModel.selectPayloadPreset(it) },
                    onConditionChanged = { viewModel.setConditionNote(it) }
                )

                // ==========================================
                // BENTO TILE 5: BENCHMARK CONTROLLER (10% CTA)
                // ==========================================
                BentoP2pControllerCard(
                    uiState = uiState,
                    onBatchCountChanged = { viewModel.setBatchRunsCount(it) },
                    onStartBenchmark = { viewModel.startTransferBenchmark(resume = false) },
                    onResumeBenchmark = { viewModel.startTransferBenchmark(resume = true) },
                    onResetBatch = { viewModel.resetBatchSession() }
                )

                // ==========================================
                // BENTO TILE 6: LIVE PROGRESS & REAL-TIME STATUS
                // ==========================================
                if (uiState.isTransferRunning) {
                    BentoP2pLiveTransferCard(
                        uiState = uiState,
                        onCancel = { viewModel.cancelTransferBenchmark() }
                    )
                }

                // ==========================================
                // BENTO TILE 7: BATCH SUMMARY
                // ==========================================
                if (uiState.latestBatchSummary != null) {
                    BentoP2pBatchSummaryCard(summary = uiState.latestBatchSummary!!)
                }

                // ==========================================
                // BENTO TILE 8: P2P LOGS & CSV EXPORT
                // ==========================================
                BentoP2pLogsHistoryCard(
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
// BENTO TILES FOR WI-FI DIRECT
// ============================================================================

@Composable
private fun P2pPermissionRequiredBanner(onRequestPermission: () -> Unit) {
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
            Icon(Icons.Default.Security, contentDescription = null, tint = AlertWarningGold)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Wi-Fi Direct Permission Needed",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = AlertWarningText)
                )
                Text(
                    text = "Nearby Devices permission is required to discover and connect to other phones.",
                    style = MaterialTheme.typography.bodySmall.copy(color = AlertWarningText)
                )
            }
            Button(
                onClick = onRequestPermission,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyDominant, contentColor = Color.White)
            ) {
                Text("Grant", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
private fun BentoP2pHeroCard(
    uiState: WifiDirectUiState,
    onDisconnect: () -> Unit
) {
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
                            text = if (p2p.thisDeviceName.isNotBlank()) p2p.thisDeviceName else uiState.deviceModel,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextNavy),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (p2p.isP2pEnabled) "Wi-Fi Direct Active" else "Wi-Fi Direct Disabled",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (p2p.isP2pEnabled) AlertSuccessText else AlertWarningGold
                            )
                        )
                    }
                }

                // Status Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (p2p.isConnected) AlertSuccessContainer else NavyLightContainer)
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
                                .background(if (p2p.isConnected) Color(0xFF10B981) else Color(0xFF94A3B8))
                        )
                        Text(
                            text = if (p2p.isConnected) p2p.roleLabel else "Disconnected",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (p2p.isConnected) AlertSuccessText else NavyDominant
                            )
                        )
                    }
                }
            }

            // Connection Details
            if (p2p.isConnected) {
                HorizontalDivider(color = BentoCardBorder)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Connected Peer: ${p2p.connectedPeerName.ifBlank { "Remote Device" }}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextNavy)
                        )
                        Text(
                            text = "Coordinator IP: ${p2p.groupOwnerAddress} • Role: ${if (p2p.isGroupOwner) "Coordinator" else "Client"}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            if (p2p.discoveryTimeMs > 0L) {
                                Text(
                                    text = "Discovery: ${p2p.discoveryTimeMs}ms",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = NavySecondary, fontWeight = FontWeight.SemiBold)
                                )
                            }
                            if (p2p.connectionTimeMs > 0L) {
                                Text(
                                    text = "Connection: ${p2p.connectionTimeMs}ms",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = NavySecondary, fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier.height(34.dp).testTag("disconnect_p2p_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertErrorText)
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Disconnect", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
private fun BentoP2pFrequencyTile(
    modifier: Modifier = Modifier,
    uiState: WifiDirectUiState
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
                    text = "P2P RADIO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextMuted
                    )
                )
                Icon(Icons.Default.WifiTethering, contentDescription = null, tint = NavySecondary, modifier = Modifier.size(16.dp))
            }

            Text(
                text = if (uiState.p2pState.p2pFrequencyMhz.isNotBlank()) uiState.p2pState.p2pFrequencyMhz else "Auto Channel",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = TextNavy)
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Router, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                Text(
                    text = if (uiState.isConnectedToWifiRouter) "Router: Connected" else "Router: Offline",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (uiState.isConnectedToWifiRouter) AlertWarningGold else AlertSuccessText,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
private fun BentoP2pBatteryTile(
    modifier: Modifier = Modifier,
    uiState: WifiDirectUiState
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
                    text = "BATTERY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextMuted
                    )
                )
                Icon(
                    Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = if (uiState.isCharging) AlertWarningGold else NavySecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = "${uiState.batteryPercentage}%",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = TextNavy)
            )

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

@Composable
private fun BentoP2pChargingWarningBanner() {
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
private fun BentoP2pErrorBanner(message: String) {
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
 * Bento Tile 3: Peer Discovery & Pairing Card
 */
@Composable
private fun BentoPeerDiscoveryCard(
    uiState: WifiDirectUiState,
    hasPermission: Boolean,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onConnectPeer: (WifiP2pDevice) -> Unit
) {
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Devices, contentDescription = null, tint = NavyDominant, modifier = Modifier.size(20.dp))
                    Text(
                        text = "Peer Discovery",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextNavy)
                    )
                }

                if (p2p.isDiscovering) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentGoldLight)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Scanning (${p2p.discoveryTimeRemainingSec}s)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = OnAccentGold)
                        )
                    }
                }
            }

            // Status Line
            Text(
                text = p2p.discoveryStatusMessage,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = if (p2p.isDiscovering) NavyDominant else TextMuted
                )
            )

            // Discovery Action Button
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!p2p.isDiscovering) {
                    Button(
                        onClick = onStartDiscovery,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("discover_peers_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDominant, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.WifiTethering, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Discover Peers", fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onStopDiscovery,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("stop_discovery_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertErrorText)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop Discovery", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Discovered Peers List
            if (p2p.peers.isNotEmpty()) {
                HorizontalDivider(color = BentoCardBorder)
                Text(
                    text = "DISCOVERED PEERS (${p2p.peers.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextMuted
                    )
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    p2p.peers.forEach { peer ->
                        BentoPeerDeviceItem(
                            device = peer,
                            isConnecting = p2p.isConnecting,
                            onConnect = { onConnectPeer(peer) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BentoPeerDeviceItem(
    device: WifiP2pDevice,
    isConnecting: Boolean,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BentoCardSurfaceSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BentoCardBorder, RoundedCornerShape(14.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.deviceName.ifBlank { "Unknown Device" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextNavy),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${device.deviceAddress} • ${formatP2pStatus(device.status)}",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                )
            }

            Button(
                onClick = onConnect,
                enabled = !isConnecting && device.status == WifiP2pDevice.AVAILABLE,
                modifier = Modifier.height(38.dp).testTag("connect_peer_btn_${device.deviceAddress}"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = OnAccentGold)
            ) {
                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Connect", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

/**
 * Bento Tile 4: Role & Payload Configuration
 */
@Composable
private fun BentoP2pConfigCard(
    uiState: WifiDirectUiState,
    onSelectRole: (P2pRole) -> Unit,
    onSelectPayload: (P2pPayloadPreset) -> Unit,
    onConditionChanged: (String) -> Unit
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
                text = "Transfer Configuration",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextNavy)
            )

            // Role Selection (Sender vs Receiver)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "DEVICE TEST ROLE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextMuted
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Sender Card
                    val isSender = uiState.selectedRole == P2pRole.SENDER
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSender) AccentGoldLight else BentoCardSurfaceSubtle)
                            .border(if (isSender) 1.5.dp else 1.dp, if (isSender) AccentGold else BentoCardBorder, RoundedCornerShape(14.dp))
                            .clickable { onSelectRole(P2pRole.SENDER) }
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.CallMade, contentDescription = null, tint = if (isSender) NavyDominant else TextMuted, modifier = Modifier.size(16.dp))
                                Text("Send", fontWeight = FontWeight.Bold, color = TextNavy)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Generates & streams payload", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TextMuted))
                        }
                    }

                    // Receiver Card
                    val isReceiver = uiState.selectedRole == P2pRole.RECEIVER
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isReceiver) AccentGoldLight else BentoCardSurfaceSubtle)
                            .border(if (isReceiver) 1.5.dp else 1.dp, if (isReceiver) AccentGold else BentoCardBorder, RoundedCornerShape(14.dp))
                            .clickable { onSelectRole(P2pRole.RECEIVER) }
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.CallReceived, contentDescription = null, tint = if (isReceiver) NavyDominant else TextMuted, modifier = Modifier.size(16.dp))
                                Text("Receive", fontWeight = FontWeight.Bold, color = TextNavy)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Receives & verifies CRC32", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TextMuted))
                        }
                    }
                }
            }

            // Payload Preset Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "PAYLOAD SIZE (IN-MEMORY)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextMuted
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    P2P_PAYLOAD_PRESETS.forEach { preset ->
                        val isSelected = uiState.selectedPayloadPresetId == preset.id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) AccentGoldLight else BentoCardSurfaceSubtle)
                                .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) AccentGold else BentoCardBorder, RoundedCornerShape(12.dp))
                                .clickable { onSelectPayload(preset) }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = preset.label, fontWeight = FontWeight.Bold, color = TextNavy)
                                Text(text = preset.description, fontSize = 9.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            // Test Condition Note Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "TEST CONDITION TAG",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextMuted
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    P2P_CONDITION_PRESETS.forEach { tag ->
                        val isSelected = uiState.conditionNote.equals(tag, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onConditionChanged(tag) },
                            label = { Text(tag) },
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
        }
    }
}

/**
 * Bento Tile 5: Benchmark Controller & 10% CTA
 */
@Composable
private fun BentoP2pControllerCard(
    uiState: WifiDirectUiState,
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
                            text = "Incomplete P2P Batch ($done of $total completed)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = OnAccentGold)
                        )
                        Text(
                            text = "Resume remaining runs with the same batch_id, or discard to start fresh.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextNavy)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onResumeBenchmark,
                                modifier = Modifier.weight(1f).height(44.dp).testTag("resume_p2p_batch_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = OnAccentGold),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resume (Run ${done + 1})", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onResetBatch,
                                modifier = Modifier.height(44.dp).testTag("reset_p2p_batch_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("New Batch", color = NavyDominant)
                            }
                        }
                    }
                }
            }

            // Batch Iteration Count Pills
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
                listOf(3, 5, 10, 20).forEach { count ->
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
                text = "Sequentially executes local TCP transfers with 3s recovery interval. Appends results to wifi_direct_logs.csv.",
                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
            )

            // Primary 10% Gold CTA Button
            val isConnected = uiState.p2pState.isConnected
            Button(
                onClick = onStartBenchmark,
                enabled = isConnected && !uiState.isTransferRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_p2p_benchmark_btn"),
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
                val roleTitle = if (uiState.selectedRole == P2pRole.SENDER) "SENDER" else "RECEIVER"
                Text(
                    text = if (isConnected) "START $roleTitle BENCHMARK (${uiState.batchRunsPlannedCount} RUNS)" else "CONNECT TO PEER FIRST",
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
 * Bento Tile 6: Live Transfer Progress & Speedometer Card
 */
@Composable
private fun BentoP2pLiveTransferCard(
    uiState: WifiDirectUiState,
    onCancel: () -> Unit
) {
    val progress = uiState.transferProgress
    val currentRun = uiState.currentRunNumber
    val totalRuns = uiState.totalRunsPlanned
    val countdown = uiState.batchCountdownSeconds
    val currentSpeedMbps = progress?.currentSpeedMbps ?: 0.0

    val onTheFlyStatus = when {
        countdown > 0 -> "Run $currentRun complete. Cooling down (${countdown}s) before next run..."
        progress == null -> "Establishing TCP socket on port 8988..."
        progress.bytesTransferred == 0L -> "Handshaking role headers & zero-copy stream..."
        progress.totalBytesExpected > 0 && progress.bytesTransferred < progress.totalBytesExpected * 0.4 ->
            "Streaming local in-memory chunks (zero storage writes)..."
        progress.totalBytesExpected > 0 && progress.bytesTransferred < progress.totalBytesExpected * 0.95 ->
            "Pumping maximum bandwidth payload stream..."
        else -> "Computing CRC32 integrity checksum & final throughput..."
    }

    val infiniteTransition = rememberInfiniteTransition(label = "p2p_pulse")
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
                        text = "P2P RUN $currentRun OF $totalRuns ACTIVE",
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

            // Live Throughput Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "LOCAL THROUGHPUT",
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
                    Column(horizontalAlignment = Alignment.End) {
                        val transferredMb = progress.bytesTransferred / (1024.0 * 1024.0)
                        val totalMb = progress.totalBytesExpected / (1024.0 * 1024.0)
                        Text(
                            text = String.format(Locale.US, "%.1f / %.1f MB", transferredMb, totalMb),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextNavy)
                        )
                        Text(
                            text = String.format(Locale.US, "Elapsed: %.1fs", progress.elapsedSeconds),
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                        )
                    }
                }
            }

            // Progress Bar
            if (progress != null && progress.totalBytesExpected > 0) {
                val fraction = (progress.bytesTransferred.toFloat() / progress.totalBytesExpected.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = AccentGold,
                    trackColor = BentoCardSurfaceSubtle
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = AccentGold,
                    trackColor = BentoCardSurfaceSubtle
                )
            }

            // Status Ticker
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(NavyLightContainer).padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = NavyDominant, modifier = Modifier.size(16.dp))
                    Text(
                        text = onTheFlyStatus,
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
 * Bento Tile 7: Batch Summary Card
 */
@Composable
private fun BentoP2pBatchSummaryCard(summary: WifiDirectBatchSummary) {
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
                        text = "P2P Batch Summary",
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

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Mean Speed
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoCardSurfaceSubtle)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("MEAN SPEED", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted))
                        Text(
                            text = String.format(Locale.US, "%.1f", summary.avgThroughputMbps),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = AccentGold)
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
                        Text("MIN / MAX", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted))
                        Text(
                            text = String.format(Locale.US, "%.1f - %.1f", summary.minThroughputMbps, summary.maxThroughputMbps),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextNavy)
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
                        Text("JITTER / STD", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted))
                        Text(
                            text = String.format(Locale.US, "±%.1f", summary.stdDevThroughputMbps),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextNavy)
                        )
                        Text("Mbps StdDev", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                    }
                }
            }
        }
    }
}

/**
 * Bento Tile 8: Log History & Export Card
 */
@Composable
private fun BentoP2pLogsHistoryCard(
    recentRuns: List<WifiDirectRunResult>,
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
                    Text(
                        text = "Wi-Fi Direct Log History",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextNavy)
                    )
                }

                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(NavyLightContainer).padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${recentRuns.size} Records",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = NavyDominant)
                    )
                }
            }

            // Export Actions
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onExportCsv,
                    modifier = Modifier.weight(1f).height(46.dp).testTag("export_p2p_csv_section_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDominant, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export / Share CSV", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onClearLogs,
                    modifier = Modifier.height(46.dp).testTag("clear_p2p_logs_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertErrorText)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
                }
            }

            // Recent Runs List
            if (recentRuns.isNotEmpty()) {
                HorizontalDivider(color = BentoCardBorder)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentRuns.takeLast(10).reversed().forEach { run ->
                        BentoP2pRunItem(run = run)
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No Wi-Fi Direct runs logged yet. Connect to a peer and start above.", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                }
            }
        }
    }
}

@Composable
private fun BentoP2pRunItem(run: WifiDirectRunResult) {
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
                    Text(
                        text = if (run.role.equals("sender", ignoreCase = true)) "📤 SENDER" else "📥 RECEIVER",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = NavyDominant)
                    )
                    Text(
                        text = "• ${run.peerDeviceName.ifBlank { "Peer" }}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                        text = if (run.isSuccessful) String.format(Locale.US, "%.1f Mbps", run.localThroughputMbps) else "FAILED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (run.isSuccessful) OnAccentGold else AlertErrorText
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sizeMb = run.payloadSizeBytes / (1024.0 * 1024.0)
                Text(
                    text = "${run.timestamp} • ${String.format(Locale.US, "%.1f MB in %.2fs", sizeMb, run.transferTimeSeconds)}",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                )

                if (run.runNumberInBatch.isNotBlank()) {
                    Text(
                        text = "Run ${run.runNumberInBatch}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = NavySecondary)
                    )
                }
            }

            // Discovery and Connection Timings
            if (run.discoveryTimeMs > 0L || run.connectionTimeMs > 0L) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (run.discoveryTimeMs > 0L) {
                        Text(
                            text = "Discovery: ${run.discoveryTimeMs}ms",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = NavySecondary, fontWeight = FontWeight.Medium)
                        )
                    }
                    if (run.connectionTimeMs > 0L) {
                        Text(
                            text = "• Connection: ${run.connectionTimeMs}ms",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = NavySecondary, fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }

            // Condition Tag & Charging Status
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

private fun formatP2pStatus(status: Int): String {
    return when (status) {
        WifiP2pDevice.AVAILABLE -> "Available"
        WifiP2pDevice.INVITED -> "Invited"
        WifiP2pDevice.CONNECTED -> "Connected"
        WifiP2pDevice.FAILED -> "Failed"
        WifiP2pDevice.UNAVAILABLE -> "Unavailable"
        else -> "Unknown"
    }
}
