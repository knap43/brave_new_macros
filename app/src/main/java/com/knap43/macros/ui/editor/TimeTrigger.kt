package com.knap43.macros.ui.editor

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.knap43.macros.data.Trigger
import com.knap43.macros.data.describeDays
import com.knap43.macros.data.formatTime
import com.knap43.macros.engine.Alarms
import com.knap43.macros.ui.formatWhen
import com.knap43.macros.ui.panel
import com.knap43.macros.ui.theme.ProseFamily
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** The time, set by tapping it, the days it repeats on, and when that next lands. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimeTrigger(time: Trigger.Time, onChange: (Trigger.Time) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    val locale = Locale.getDefault()

    Column(Modifier.panel().padding(16.dp)) {
        Text(
            formatTime(time.localTime),
            style = MaterialTheme.typography.displayMedium.copy(fontFamily = ProseFamily),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .clickable { picking = true }
                .padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DayOfWeek.entries.forEach { day ->
                val on = day.value in time.days
                FilterChip(
                    selected = on,
                    onClick = { onChange(time.copy(days = if (on) time.days - day.value else time.days + day.value)) },
                    label = { Text(day.getDisplayName(TextStyle.SHORT_STANDALONE, locale)) },
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        val next = Alarms.next(time)
        Text(
            if (next == null) "Pick at least one day."
            else "${describeDays(time.days)} · next ${formatWhen(next.toInstant().toEpochMilli())}",
            style = MaterialTheme.typography.bodySmall,
            color = if (next == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (picking) {
        val state = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = DateFormat.is24HourFormat(LocalContext.current),
        )
        AlertDialog(
            onDismissRequest = { picking = false },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    onChange(time.copy(hour = state.hour, minute = state.minute))
                    picking = false
                }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
        )
    }
}
