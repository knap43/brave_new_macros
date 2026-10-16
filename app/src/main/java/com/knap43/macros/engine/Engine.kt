package com.knap43.macros.engine

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.knap43.macros.data.Macro
import com.knap43.macros.data.MacroStore
import com.knap43.macros.data.Trigger
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Arms the system to wake the app when a trigger could fire, and runs the macros that match.
 *
 * WiFi is watched with a PendingIntent network request rather than a running service: Android
 * keeps it while the app sleeps and delivers it to [NetworkReceiver], so there's no persistent
 * notification and no battery cost between connections.
 */
object Engine {
    private const val PREFS = "engine"
    private const val KEY_HANDLED = "handled_network"
    private const val REQUEST = 1
    private val lock = Mutex()

    /**
     * Arms or disarms the WiFi watch to match the enabled macros.
     *
     * [seedCurrent] marks the network the phone is already on as seen, so saving or enabling a
     * macro while at home doesn't fire it there and then: it waits for the next real connection.
     */
    fun sync(context: Context, seedCurrent: Boolean = false) {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        val intent = pendingIntent(context)
        val watchWifi = MacroStore.get(context).macros.value.any { it.enabled && it.trigger is Trigger.WifiConnected }
        if (seedCurrent) Wifi.activeNetwork(context)?.let { markHandled(context, it) }
        if (watchWifi) {
            // Registering the same PendingIntent again replaces the old request rather than adding one.
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            runCatching { cm.registerNetworkCallback(request, intent) }
        } else {
            runCatching { cm.unregisterNetworkCallback(intent) }
        }
    }

    /** Called by [NetworkReceiver] each time a WiFi network comes up. */
    suspend fun onWifiAvailable(context: Context, network: Network) = lock.withLock {
        // Each connection gets a fresh handle; the same one arriving twice is one connection.
        if (prefs(context).getLong(KEY_HANDLED, 0L) == network.networkHandle) return@withLock
        markHandled(context, network)

        val store = MacroStore.get(context)
        val candidates = store.macros.value.filter { it.enabled && it.trigger is Trigger.WifiConnected }
        if (candidates.isEmpty()) return@withLock

        // The network can come up a beat before WifiInfo carries its name.
        var ssid: String? = null
        for (attempt in 0 until 6) {
            ssid = Wifi.currentSsid(context)
            if (ssid != null) break
            delay(500)
        }
        if (ssid == null) {
            store.log(
                "WiFi",
                "Connected, but Android hid the network name — check location access is “Allow all the time”",
                ok = false,
            )
            return@withLock
        }

        candidates
            .filter { (it.trigger as Trigger.WifiConnected).ssids.contains(ssid) }
            .forEach { run(context, it) }
    }

    /** Runs a macro's actions in order and records each result. Also used by “Test run”. */
    suspend fun run(context: Context, macro: Macro) {
        val store = MacroStore.get(context)
        macro.actions.forEach { action ->
            val outcome = Actions.perform(context, action, macro.name)
            store.log(macro.name, outcome.message, outcome.ok)
        }
        store.markRun(macro.id, System.currentTimeMillis())
    }

    private fun markHandled(context: Context, network: Network) =
        prefs(context).edit().putLong(KEY_HANDLED, network.networkHandle).commit()

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST,
        Intent(context, NetworkReceiver::class.java),
        // Mutable so the system can attach which network came up.
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
