package com.knap43.macros.engine

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import com.knap43.macros.data.Action

/** What came of one action, phrased for the activity log. */
data class Outcome(val message: String, val ok: Boolean)

object Actions {
    suspend fun perform(context: Context, action: Action, macro: String): Outcome = when (action) {
        is Action.SetBluetooth -> bluetooth(context, action.on, macro)
    }

    /** Directly where Android still allows it, then through Shizuku, then by asking you. */
    @SuppressLint("MissingPermission")
    private suspend fun bluetooth(context: Context, on: Boolean, macro: String): Outcome {
        val word = if (on) "on" else "off"
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            ?: return Outcome("This phone has no Bluetooth", ok = false)
        if (runCatching { adapter.isEnabled }.getOrNull() == on) return Outcome("Bluetooth was already $word", ok = true)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION")
            val done = runCatching { if (on) adapter.enable() else adapter.disable() }.getOrDefault(false)
            if (done) return Outcome("Turned Bluetooth $word", ok = true)
        }

        var shizukuNote = "Shizuku isn't running"
        if (Shell.awaitReady()) {
            val verb = if (on) "enable" else "disable"
            var result = Shell.run("cmd", "bluetooth_manager", verb)
            if (result.code != 0) result = Shell.run("svc", "bluetooth", verb)
            if (result.code == 0) {
                Prompts.dismissBluetooth(context)
                return Outcome("Turned Bluetooth $word via Shizuku", ok = true)
            }
            shizukuNote = "Shizuku failed (${result.error.ifBlank { "exit ${result.code}" }})"
        }

        return if (Prompts.bluetooth(context, on, macro)) {
            Outcome("$shizukuNote — asked you to turn Bluetooth $word", ok = true)
        } else {
            Outcome("$shizukuNote, and notifications are off — Bluetooth left ${if (on) "off" else "on"}", ok = false)
        }
    }
}
