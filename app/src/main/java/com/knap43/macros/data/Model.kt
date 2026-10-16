package com.knap43.macros.data

import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A macro: one trigger, then its actions in order.
 *
 * New trigger or action kinds are added as further subclasses below; every `when` over them is
 * exhaustive, so the compiler points at each place (description, editor, engine) that needs a branch.
 */
@Serializable
data class Macro(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val enabled: Boolean = true,
    val trigger: Trigger,
    val actions: List<Action>,
    val lastRun: Long? = null,
)

@Serializable
sealed interface Trigger {
    /** Fires once each time the phone joins one of these networks. */
    @Serializable
    @SerialName("wifi_connected")
    data class WifiConnected(val ssids: List<String> = emptyList()) : Trigger
}

@Serializable
sealed interface Action {
    @Serializable
    @SerialName("bluetooth")
    data class SetBluetooth(val on: Boolean = false) : Action
}

/** One line in the activity log. */
@Serializable
data class LogEntry(
    val time: Long,
    val macro: String,
    val message: String,
    val ok: Boolean = true,
)

fun Trigger.describe(): String = when (this) {
    is Trigger.WifiConnected -> when (ssids.size) {
        0 -> "When WiFi connects to — no network chosen"
        1 -> "When WiFi connects to ${ssids[0]}"
        else -> "When WiFi connects to ${ssids.dropLast(1).joinToString()} or ${ssids.last()}"
    }
}

fun Action.describe(): String = when (this) {
    is Action.SetBluetooth -> if (on) "Turn Bluetooth on" else "Turn Bluetooth off"
}

/** The kinds the editor can add; one entry per Action subclass. */
enum class ActionKind(val label: String, val create: () -> Action) {
    BLUETOOTH("Bluetooth", { Action.SetBluetooth(on = false) }),
}
