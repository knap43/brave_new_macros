package com.knap43.macros.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.knap43.macros.ui.theme.ProseFamily
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

val PanelShape = RoundedCornerShape(18.dp)

/** The rounded, slightly lifted block every list item and card sits on. */
@Composable
fun Modifier.panel(): Modifier = this
    .fillMaxWidth()
    .padding(horizontal = 12.dp, vertical = 4.dp)
    .clip(PanelShape)
    .background(MaterialTheme.colorScheme.surfaceContainerLow)

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = ProseFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** "today at 18:42", "yesterday at 08:10", or a short date and time. */
fun formatWhen(millis: Long): String {
    val zone = ZoneId.systemDefault()
    val time = Instant.ofEpochMilli(millis).atZone(zone)
    val clock = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    val today = LocalDate.now(zone)
    return when (time.toLocalDate()) {
        today -> "today at $clock"
        today.minusDays(1) -> "yesterday at $clock"
        else -> time.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))
    }
}
