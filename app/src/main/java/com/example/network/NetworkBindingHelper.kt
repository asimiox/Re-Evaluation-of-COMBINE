package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Dns
import okhttp3.OkHttpClient
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Manages binding HTTP traffic directly to specific physical networks
 * (e.g. forcing Cellular on the Helper, or WiFi on the Coordinator)
 * so that HTTP byte-range requests never leak onto the Wi-Fi Direct P2P link.
 */
class NetworkBindingHelper(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var cellularCallback: ConnectivityManager.NetworkCallback? = null

    private val _cellularNetwork = MutableStateFlow<Network?>(null)
    val cellularNetwork: StateFlow<Network?> = _cellularNetwork.asStateFlow()

    private val _wifiNetwork = MutableStateFlow<Network?>(null)
    val wifiNetwork: StateFlow<Network?> = _wifiNetwork.asStateFlow()

    init {
        findCurrentNetworks()
    }

    fun findCurrentNetworks() {
        for (network in connectivityManager.allNetworks) {
            val caps = connectivityManager.getNetworkCapabilities(network) ?: continue
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                _wifiNetwork.value = network
            }
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                _cellularNetwork.value = network
            }
        }
    }

    /**
     * Actively requests and keeps the Cellular data radio alive, even if Wi-Fi Direct is active.
     */
    fun acquireCellularNetwork() {
        findCurrentNetworks()
        if (cellularCallback != null) return

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _cellularNetwork.value = network
            }

            override fun onLost(network: Network) {
                if (_cellularNetwork.value == network) {
                    _cellularNetwork.value = null
                }
            }
        }

        try {
            connectivityManager.requestNetwork(request, callback)
            cellularCallback = callback
        } catch (_: Exception) {}
    }

    fun releaseCellularNetwork() {
        cellularCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (_: Exception) {}
            cellularCallback = null
        }
    }

    /**
     * Creates an OkHttpClient strictly bound to a target network (Cellular or WiFi).
     */
    fun createBoundClient(targetNetwork: Network?): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)

        if (targetNetwork != null) {
            builder.socketFactory(targetNetwork.socketFactory)
            builder.dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> {
                    return try {
                        targetNetwork.getAllByName(hostname).toList()
                    } catch (_: Exception) {
                        Dns.SYSTEM.lookup(hostname)
                    }
                }
            })
        }

        return builder.build()
    }

    fun getActiveNetworkLabel(targetNetwork: Network?): String {
        if (targetNetwork == null) {
            val active = connectivityManager.activeNetwork ?: return "Default"
            val caps = connectivityManager.getNetworkCapabilities(active) ?: return "Default"
            return when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                else -> "Default"
            }
        }
        val caps = connectivityManager.getNetworkCapabilities(targetNetwork) ?: return "Bound"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            else -> "Bound Network"
        }
    }
}
