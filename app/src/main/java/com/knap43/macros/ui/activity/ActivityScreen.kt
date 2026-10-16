package com.knap43.macros.ui.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.knap43.macros.data.MacroStore
import com.knap43.macros.ui.EmptyState
import com.knap43.macros.ui.formatWhen
import com.knap43.macros.ui.panel
import com.knap43.macros.ui.theme.ProseFamily
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** What ran, when, and how — the first place to look when a macro seems not to fire. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(store: MacroStore, appScope: CoroutineScope, onClose: () -> Unit) {
    val log by store.log.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Activity", fontFamily = ProseFamily) },
                actions = {
                    if (log.isNotEmpty()) {
                        TextButton(onClick = { appScope.launch { store.clearLog() } }) { Text("Clear") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (log.isEmpty()) {
            EmptyState("Nothing has run yet.", Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(log) { entry ->
                Row(Modifier.panel().padding(16.dp)) {
                    Icon(
                        if (entry.ok) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                        contentDescription = if (entry.ok) "Succeeded" else "Failed",
                        tint = if (entry.ok) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(entry.message, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${entry.macro} · ${formatWhen(entry.time)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }
    }
}
