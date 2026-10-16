package com.knap43.macros.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import androidx.core.content.IntentCompat
import com.knap43.macros.data.MacroStore
import com.knap43.macros.data.Trigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/** Woken by the system's network request when a WiFi network comes up. */
class NetworkReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val network = IntentCompat.getParcelableExtra(intent, ConnectivityManager.EXTRA_NETWORK, Network::class.java)
            ?: return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Engine.onWifiAvailable(context.applicationContext, network)
            } finally {
                pending.finish()
            }
        }
    }
}

/** Woken by a time macro's alarm: runs it, then sets its next occurrence. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(Alarms.EXTRA_MACRO) ?: return
        val app = context.applicationContext
        val pending = goAsync()
        receiverScope.launch {
            try {
                val macro = MacroStore.get(app).get(id)
                if (macro != null && macro.enabled && macro.trigger is Trigger.Time) Engine.run(app, macro)
            } finally {
                Alarms.scheduleAll(app)
                pending.finish()
            }
        }
    }
}

/**
 * Network requests and alarms are forgotten on reboot and on update, and alarms go stale when
 * the clock, time zone or exact-alarm access changes; re-arm everything.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Engine.sync(context.applicationContext)
    }
}
