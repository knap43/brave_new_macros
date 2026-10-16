package com.knap43.macros.engine

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.knap43.macros.data.MacroStore
import com.knap43.macros.data.Trigger
import java.time.ZonedDateTime

/**
 * One alarm per enabled time macro, always set for its next occurrence; after it fires,
 * [AlarmReceiver] runs the macro and sets the one after.
 */
object Alarms {
    private const val PREFS = "alarms"
    private const val KEY_SCHEDULED = "scheduled"
    const val EXTRA_MACRO = "macro"

    /** The next moment [trigger] fires strictly after [from], or null if it has no days. */
    fun next(trigger: Trigger.Time, from: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime? {
        if (trigger.days.isEmpty()) return null
        for (offset in 0L..7L) {
            val date = from.toLocalDate().plusDays(offset)
            if (date.dayOfWeek.value !in trigger.days) continue
            val at = date.atTime(trigger.localTime).atZone(from.zone)
            if (at.isAfter(from)) return at
        }
        return null
    }

    /** Sets alarms for every enabled time macro and cancels the rest, including deleted ones. */
    @SuppressLint("ScheduleExactAlarm") // Checked in exact(), and a refusal falls back below.
    fun scheduleAll(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val wanted = MacroStore.get(context).macros.value
            .filter { it.enabled && it.trigger is Trigger.Time }
            .associate { it.id to it.trigger as Trigger.Time }

        val previous = prefs.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty()
        (previous - wanted.keys).forEach { alarms.cancel(pendingIntent(context, it)) }

        wanted.forEach { (id, trigger) ->
            val at = next(trigger)?.toInstant()?.toEpochMilli() ?: return@forEach
            val intent = pendingIntent(context, id)
            val exactSet = exact(alarms) && runCatching {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            }.isSuccess
            // Without exact-alarm access Android may run it some minutes late, but it still runs.
            if (!exactSet) alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        }
        prefs.edit().putStringSet(KEY_SCHEDULED, wanted.keys).apply()
    }

    private fun exact(alarms: AlarmManager) =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    /** Distinct per macro (by data URI), so each macro's alarm replaces only its own. */
    private fun pendingIntent(context: Context, macroId: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, AlarmReceiver::class.java)
            .setData(Uri.parse("macro://$macroId"))
            .putExtra(EXTRA_MACRO, macroId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
