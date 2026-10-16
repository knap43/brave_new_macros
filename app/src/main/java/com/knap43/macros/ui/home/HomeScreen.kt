package com.knap43.macros.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.knap43.macros.data.Macro
import com.knap43.macros.data.MacroStore
import com.knap43.macros.data.describe
import com.knap43.macros.engine.Engine
import com.knap43.macros.ui.EmptyState
import com.knap43.macros.ui.formatWhen
import com.knap43.macros.ui.panel
import com.knap43.macros.ui.theme.AppearanceDialog
import com.knap43.macros.ui.theme.ProseFamily
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    store: MacroStore,
    appScope: CoroutineScope,
    onOpen: (String) -> Unit,
    onNew: () -> Unit,
    onActivity: () -> Unit,
) {
    val context = LocalContext.current
    val macros by store.macros.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var appearanceOpen by remember { mutableStateOf(false) }
    if (appearanceOpen) AppearanceDialog(onDismiss = { appearanceOpen = false })

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text("Macros", fontFamily = ProseFamily) },
                actions = {
                    IconButton(onClick = onActivity) {
                        Icon(Icons.Outlined.History, contentDescription = "Activity")
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Appearance") },
                                leadingIcon = { Icon(Icons.Outlined.Palette, null) },
                                onClick = { menuOpen = false; appearanceOpen = true },
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNew,
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text("New macro") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (macros.isNotEmpty()) item(key = "setup") { SetupCard() }
            items(macros, key = { it.id }) { macro ->
                MacroRow(
                    macro = macro,
                    onClick = { onOpen(macro.id) },
                    onToggle = { on ->
                        appScope.launch {
                            store.setEnabled(macro.id, on)
                            Engine.sync(context, seedCurrent = on)
                        }
                    },
                )
            }
            if (macros.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        "No macros yet.\nTeach the phone one small habit.",
                        modifier = Modifier.padding(top = 120.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroRow(macro: Macro, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val dim = if (macro.enabled) 1f else 0.55f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .panel()
            .clickable(onClick = onClick)
            .padding(start = 18.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                macro.name,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = ProseFamily, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = dim),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                macro.trigger.describe(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = dim),
            )
            Text(
                macro.actions.joinToString(", then ") { it.describe() }.ifEmpty { "No actions" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = dim),
            )
            macro.lastRun?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Last ran ${formatWhen(it)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = macro.enabled, onCheckedChange = onToggle)
    }
}
