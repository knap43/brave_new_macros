package com.knap43.macros.data

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
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

    /** Fires at [hour]:[minute] on each of [days] (ISO numbering: 1 = Monday … 7 = Sunday). */
    @Serializable
    @SerialName("time")
    data class Time(
        val hour: Int = 8,
        val minute: Int = 0,
        val days: Set<Int> = ALL_DAYS,
    ) : Trigger {
        val localTime: LocalTime get() = LocalTime.of(hour, minute)

        companion object {
            val ALL_DAYS = (1..7).toSet()
            val WEEKDAYS = (1..5).toSet()
            val WEEKEND = setOf(6, 7)
        }
    }
}

/** The kinds the editor offers, with the draft each starts a new macro from. */
enum class TriggerKind(val label: String, val defaultName: String, val defaultActions: List<Action>) {
    WIFI("WiFi", "Home WiFi", listOf(Action.SetBluetooth(on = false))),
    TIME("Time", "Bluetooth on", listOf(Action.SetBluetooth(on = true))),
    ;

    companion object {
        fun of(trigger: Trigger) = when (trigger) {
            is Trigger.WifiConnected -> WIFI
            is Trigger.Time -> TIME
        }
    }
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
    is Trigger.Time -> "${describeDays(days)} at ${formatTime(localTime)}"
}

fun formatTime(time: LocalTime): String = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

fun describeDays(days: Set<Int>): String = when (days) {
    Trigger.Time.ALL_DAYS -> "Every day"
    Trigger.Time.WEEKDAYS -> "Weekdays"
    Trigger.Time.WEEKEND -> "Weekends"
    emptySet<Int>() -> "Never"
    else -> days.sorted().joinToString { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}

fun Action.describe(): String = when (this) {
    is Action.SetBluetooth -> if (on) "Turn Bluetooth on" else "Turn Bluetooth off"
}

/** The kinds the editor can add; one entry per Action subclass. */
enum class ActionKind(val label: String, val create: () -> Action) {
    BLUETOOTH("Bluetooth", { Action.SetBluetooth(on = false) }),
}
