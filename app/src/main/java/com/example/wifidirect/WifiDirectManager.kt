package com.example.wifidirect

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.WpsInfo
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WifiDirectState(
    val isP2pEnabled: Boolean = false,
    val isDiscovering: Boolean = false,
    val discoveryTimeRemainingSec: Int = 0,
    val discoveryStatusMessage: String = "Ready to discover peers.",
    val thisDeviceName: String = "",
    val thisDeviceStatus: String = "Unknown",
    val peers: List<WifiP2pDevice> = emptyList(),
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val isGroupOwner: Boolean = false, // true = Coordinator, false = Client
    val groupOwnerAddress: String = "",
    val connectedPeerName: String = "",
    val p2pFrequencyMhz: String = "",
    val discoveryTimeMs: Long = 0L,
    val connectionTimeMs: Long = 0L,
    val connectionStatusMessage: String = "Disconnected."
) {
    val roleLabel: String
        get() = if (isConnected) {
            if (isGroupOwner) "Coordinator (Group Owner)" else "Client"
        } else "None"
}

class WifiDirectManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val p2pManager: WifiP2pManager? =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private val channel: WifiP2pManager.Channel? =
        p2pManager?.initialize(context, Looper.getMainLooper(), null)

    private val _state = MutableStateFlow(WifiDirectState())
    val state: StateFlow<WifiDirectState> = _state.asStateFlow()

    private var discoveryTimeoutJob: Job? = null
    private var discoveryStartTime: Long = 0L
    private var connectionStartTime: Long = 0L
    private var isReceiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val p2pState = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    val enabled = p2pState == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                    _state.update {
                        it.copy(
                            isP2pEnabled = enabled,
                            discoveryStatusMessage = if (enabled) "Wi-Fi Direct is ready." else "Wi-Fi is turned off. Please turn on Wi-Fi."
                        )
                    }
                }

                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    p2pManager?.requestPeers(channel) { peerList ->
                        val list = peerList?.deviceList?.toList() ?: emptyList()
                        val previousPeersCount = _state.value.peers.size
                        val wasDiscovering = _state.value.isDiscovering

                        val measuredDiscoveryTime = if (wasDiscovering && list.isNotEmpty() && _state.value.discoveryTimeMs == 0L && discoveryStartTime > 0L) {
                            System.currentTimeMillis() - discoveryStartTime
                        } else {
                            _state.value.discoveryTimeMs
                        }

                        _state.update { current ->
                            current.copy(
                                peers = list,
                                discoveryTimeMs = measuredDiscoveryTime,
                                discoveryStatusMessage = when {
                                    list.isNotEmpty() -> "Found ${list.size} peer(s). Tap one to connect."
                                    wasDiscovering -> "Searching for nearby peers..."
                                    else -> current.discoveryStatusMessage
                                }
                            )
                        }
                    }
                }

                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO, NetworkInfo::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
                    }

                    if (networkInfo?.isConnected == true) {
                        p2pManager?.requestConnectionInfo(channel) { info ->
                            handleConnectionEstablished(info)
                        }
                        p2pManager?.requestGroupInfo(channel) { group ->
                            handleGroupInfo(group)
                        }
                    } else {
                        _state.update {
                            it.copy(
                                isConnected = false,
                                isConnecting = false,
                                isGroupOwner = false,
                                groupOwnerAddress = "",
                                connectedPeerName = "",
                                p2pFrequencyMhz = "",
                                connectionStatusMessage = "Disconnected."
                            )
                        }
                    }
                }

                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    val thisDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE, WifiP2pDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                    }
                    if (thisDevice != null) {
                        val statusStr = formatDeviceStatus(thisDevice.status)
                        _state.update {
                            it.copy(
                                thisDeviceName = thisDevice.deviceName.ifBlank { thisDevice.deviceAddress },
                                thisDeviceStatus = statusStr
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleConnectionEstablished(info: WifiP2pInfo?) {
        if (info == null || !info.groupFormed) return

        val duration = if (connectionStartTime > 0L) {
            System.currentTimeMillis() - connectionStartTime
        } else {
            _state.value.connectionTimeMs
        }

        val goAddress = info.groupOwnerAddress?.hostAddress ?: ""
        val isGo = info.isGroupOwner
        val roleStr = if (isGo) "Connected as Coordinator" else "Connected as Client"

        _state.update {
            it.copy(
                isConnected = true,
                isConnecting = false,
                isGroupOwner = isGo,
                groupOwnerAddress = goAddress,
                connectionTimeMs = duration,
                connectionStatusMessage = "$roleStr (Group Owner IP: $goAddress)"
            )
        }
    }

    private fun handleGroupInfo(group: WifiP2pGroup?) {
        if (group == null) return

        val freqStr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val freq = group.frequency
            if (freq > 0) "$freq MHz" else ""
        } else ""

        val peerName = if (_state.value.isGroupOwner) {
            // We are Coordinator: find first client
            group.clientList?.firstOrNull()?.deviceName ?: "Client Device"
        } else {
            // We are Client: peer is Group Owner
            group.owner?.deviceName ?: "Coordinator"
        }

        _state.update {
            it.copy(
                connectedPeerName = peerName,
                p2pFrequencyMhz = freqStr
            )
        }
    }

    fun register() {
        if (isReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        try {
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            isReceiverRegistered = true
        } catch (_: Exception) {
            try {
                context.registerReceiver(receiver, filter)
                isReceiverRegistered = true
            } catch (_: Exception) {}
        }
    }

    fun unregister() {
        stopDiscovery()
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
            isReceiverRegistered = false
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (p2pManager == null || channel == null) {
            _state.update { it.copy(discoveryStatusMessage = "Wi-Fi Direct is not supported on this device.") }
            return
        }

        discoveryStartTime = System.currentTimeMillis()
        discoveryTimeoutJob?.cancel()

        _state.update {
            it.copy(
                isDiscovering = true,
                discoveryTimeMs = 0L,
                discoveryTimeRemainingSec = 30,
                discoveryStatusMessage = "Discovering peers..."
            )
        }

        p2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _state.update { it.copy(discoveryStatusMessage = "Discovering peers...") }
            }

            override fun onFailure(reasonCode: Int) {
                val errorMsg = formatErrorReason(reasonCode)
                _state.update {
                    it.copy(
                        isDiscovering = false,
                        discoveryStatusMessage = "Discovery failed: $errorMsg"
                    )
                }
            }
        })

        // 30-second timeout with visible countdown
        discoveryTimeoutJob = scope.launch {
            for (sec in 30 downTo 1) {
                _state.update { it.copy(discoveryTimeRemainingSec = sec) }
                delay(1000L)
            }
            if (_state.value.isDiscovering) {
                if (_state.value.peers.isEmpty()) {
                    _state.update {
                        it.copy(
                            isDiscovering = false,
                            discoveryTimeRemainingSec = 0,
                            discoveryStatusMessage = "No devices found. Make sure Wi-Fi is on and the other phone has this screen open."
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isDiscovering = false,
                            discoveryTimeRemainingSec = 0,
                            discoveryStatusMessage = "Discovery completed. ${_state.value.peers.size} peer(s) found."
                        )
                    }
                }
                stopPeerDiscoveryInternal()
            }
        }
    }

    fun stopDiscovery() {
        discoveryTimeoutJob?.cancel()
        _state.update {
            it.copy(
                isDiscovering = false,
                discoveryTimeRemainingSec = 0
            )
        }
        stopPeerDiscoveryInternal()
    }

    private fun stopPeerDiscoveryInternal() {
        p2pManager?.stopPeerDiscovery(channel, null)
    }

    @SuppressLint("MissingPermission")
    fun connect(device: WifiP2pDevice) {
        if (p2pManager == null || channel == null) return

        connectionStartTime = System.currentTimeMillis()
        _state.update {
            it.copy(
                isConnecting = true,
                connectionTimeMs = 0L,
                connectionStatusMessage = "Connecting to ${device.deviceName}..."
            )
        }

        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }

        p2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _state.update {
                    it.copy(connectionStatusMessage = "Connection invitation sent. Accepting on peer...")
                }
            }

            override fun onFailure(reasonCode: Int) {
                val errorMsg = formatErrorReason(reasonCode)
                _state.update {
                    it.copy(
                        isConnecting = false,
                        connectionStatusMessage = "Connection failed: $errorMsg"
                    )
                }
            }
        })
    }

    fun disconnect() {
        discoveryTimeoutJob?.cancel()
        p2pManager?.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _state.update {
                    it.copy(
                        isConnected = false,
                        isConnecting = false,
                        isGroupOwner = false,
                        groupOwnerAddress = "",
                        connectedPeerName = "",
                        p2pFrequencyMhz = "",
                        connectionStatusMessage = "Disconnected."
                    )
                }
            }

            override fun onFailure(reasonCode: Int) {
                // Force state reset even if framework already removed group
                _state.update {
                    it.copy(
                        isConnected = false,
                        isConnecting = false,
                        isGroupOwner = false,
                        groupOwnerAddress = "",
                        connectedPeerName = "",
                        p2pFrequencyMhz = "",
                        connectionStatusMessage = "Disconnected."
                    )
                }
            }
        })
    }

    private fun formatDeviceStatus(status: Int): String {
        return when (status) {
            WifiP2pDevice.AVAILABLE -> "Available"
            WifiP2pDevice.INVITED -> "Invited"
            WifiP2pDevice.CONNECTED -> "Connected"
            WifiP2pDevice.FAILED -> "Failed"
            WifiP2pDevice.UNAVAILABLE -> "Unavailable"
            else -> "Unknown"
        }
    }

    private fun formatErrorReason(reasonCode: Int): String {
        return when (reasonCode) {
            WifiP2pManager.P2P_UNSUPPORTED -> "Wi-Fi Direct is not supported on this device."
            WifiP2pManager.ERROR -> "Internal system error. Try toggling Wi-Fi off and on."
            WifiP2pManager.BUSY -> "Framework is busy with another operation. Please wait a moment."
            else -> "Error code $reasonCode"
        }
    }
}
