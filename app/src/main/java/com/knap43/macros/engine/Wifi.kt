package com.knap43.macros.engine

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager

object Wifi {
    private const val UNKNOWN = "<unknown ssid>"

    /**
     * The name of the network the phone is on, or null when there is none or Android hides it
     * (no location permission, location turned off, or no background access while in the background).
     */
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION") // Still the only way to read the SSID outside a NetworkCallback.
    fun currentSsid(context: Context): String? {
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java) ?: return null
        val raw = wifi.connectionInfo?.ssid ?: return null
        if (raw.isBlank() || raw == UNKNOWN) return null
        return raw.removeSurrounding("\"")
    }

    /** Networks from the system's most recent scan, strongest first. Needs location, like the SSID. */
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    fun nearby(context: Context): List<String> {
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java) ?: return emptyList()
        return runCatching {
            wifi.scanResults
                .sortedByDescending { it.level }
                .map { it.SSID.removeSurrounding("\"") }
                .filter { it.isNotBlank() && it != UNKNOWN }
                .distinct()
        }.getOrDefault(emptyList())
    }

    /** The WiFi network currently up, if any. */
    fun activeNetwork(context: Context): Network? {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
        @Suppress("DEPRECATION")
        return cm.allNetworks.firstOrNull { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }
    }
}
