package com.example.network

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.telephony.CellSignalStrength
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

enum class SignalQuality {
    STRONG,   // > -70 dBm
    MEDIUM,   // -70 to -90 dBm
    WEAK,     // < -90 dBm
    UNKNOWN
}

/**
 * Helper utility to probe network type, signal strength (RSSI/dBm),
 * carrier name, and battery percentage for baseline benchmark runs.
 */
class NetworkMeter(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val batteryManager =
        context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.PRODUCT.contains("sdk_gphone"))
    }

    fun getDeviceDescription(): String {
        return if (isEmulator()) {
            "Cloud Virtual Emulator (Google Datacenter)"
        } else {
            "${Build.MANUFACTURER} ${Build.MODEL}".trim()
        }
    }

    /**
     * Determines current active network type: "WiFi", "5G", "4G (LTE)", "3G", or "Cellular".
     */
    fun getNetworkType(): String {
        val cm = connectivityManager ?: return "Unknown"
        val activeNetwork = cm.activeNetwork ?: return "No Connection"
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return "No Connection"

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> getCellularNetworkType()
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Other"
        }
    }

    /**
     * Returns true if currently connected to WiFi.
     */
    fun isWiFiActive(): Boolean {
        val cm = connectivityManager ?: return false
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Returns true if currently connected to Cellular.
     */
    fun isCellularActive(): Boolean {
        val cm = connectivityManager ?: return false
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    /**
     * Carrier operator name if cellular is available (e.g., "T-Mobile", "Verizon", "Jio").
     */
    fun getCarrierName(): String? {
        val tm = telephonyManager ?: return null
        val netName = tm.networkOperatorName
        if (!netName.isNullOrBlank()) return netName
        val simName = tm.simOperatorName
        if (!simName.isNullOrBlank()) return simName
        return null
    }

    private fun getCellularNetworkType(): String {
        val tm = telephonyManager ?: return "Cellular"

        val hasPhoneStatePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPhoneStatePermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return "Cellular"
        }

        return try {
            @SuppressLint("MissingPermission")
            val networkType = tm.dataNetworkType
            when (networkType) {
                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                TelephonyManager.NETWORK_TYPE_LTE -> "4G (LTE)"
                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
                else -> "Cellular"
            }
        } catch (_: Exception) {
            "Cellular"
        }
    }

    /**
     * Obtains signal strength (in dBm or RSSI where available).
     */
    @SuppressLint("MissingPermission")
    fun getSignalStrength(): String {
        val cm = connectivityManager ?: return "N/A"
        val activeNetwork = cm.activeNetwork ?: return "No Signal"
        val caps = cm.getNetworkCapabilities(activeNetwork)

        // 1. If WiFi is connected
        if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            val rssi = wifiManager?.connectionInfo?.rssi
            if (rssi != null && rssi != -127 && rssi != 0) {
                return "$rssi dBm"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && caps.signalStrength != NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED) {
                return "${caps.signalStrength} dBm"
            }
            return "WiFi Connected"
        }

        // 2. If Cellular is connected
        if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val ss = telephonyManager?.signalStrength
                    val cellSignals = ss?.cellSignalStrengths
                    val primarySignal = cellSignals?.firstOrNull()
                    if (primarySignal != null && primarySignal.dbm != 0 && primarySignal.dbm != Int.MAX_VALUE) {
                        return "${primarySignal.dbm} dBm"
                    }
                    if (primarySignal != null) {
                        return "Level ${primarySignal.level}/4"
                    }
                } catch (_: Exception) {
                    // Fallback
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && caps.signalStrength != NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED) {
                return "${caps.signalStrength} dBm"
            }

            return "Cellular Active"
        }

        return "N/A"
    }

    /**
     * Extracts numerical dBm if present (e.g. "-65 dBm" -> -65.0)
     */
    fun extractDbmValue(signalStr: String): Double? {
        val match = Regex("(-?\\d+)\\s*dBm", RegexOption.IGNORE_CASE).find(signalStr)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }

    /**
     * Classifies signal strength into Strong (> -70 dBm), Medium (-70 to -90 dBm), Weak (< -90 dBm)
     */
    fun evaluateSignalQuality(signalStr: String): SignalQuality {
        val dbm = extractDbmValue(signalStr) ?: return SignalQuality.UNKNOWN
        return when {
            dbm > -70.0 -> SignalQuality.STRONG   // e.g. -50 dBm, -65 dBm
            dbm >= -90.0 -> SignalQuality.MEDIUM  // -70 dBm to -90 dBm
            else -> SignalQuality.WEAK            // -91 dBm to -120 dBm
        }
    }

    /**
     * Returns current battery percentage (0-100).
     */
    fun getBatteryPercentage(): Int {
        try {
            val capacity = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            if (capacity in 0..100) {
                return capacity
            }
        } catch (_: Exception) {
            // Ignore
        }

        return try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, intentFilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                ((level.toFloat() / scale.toFloat()) * 100).toInt()
            } else {
                0
            }
        } catch (_: Exception) {
            0
        }
    }
}
