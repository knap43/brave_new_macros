package com.knap43.macros.ui.editor

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.knap43.macros.data.Action
import com.knap43.macros.data.ActionKind
import com.knap43.macros.data.Macro
import com.knap43.macros.data.MacroStore
import com.knap43.macros.data.Trigger
import com.knap43.macros.engine.Engine
import com.knap43.macros.engine.Wifi
import com.knap43.macros.ui.SectionHeader
import com.knap43.macros.ui.panel
import com.knap43.macros.ui.theme.ProseFamily
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Edits a macro, or drafts a new one; [macroId] null means new. Nothing is stored until Save. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    store: MacroStore,
    appScope: CoroutineScope,
    macroId: String?,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // The first macro most people want is the one this app was made for, so a new draft starts as it.
    val original = remember(macroId) {
        macroId?.let(store::get) ?: Macro(
            name = "Home WiFi",
            trigger = Trigger.WifiConnected(listOfNotNull(Wifi.currentSsid(context))),
            actions = listOf(Action.SetBluetooth(on = false)),
        )
    }
    var name by remember(original) { mutableStateOf(original.name) }
    val ssids = remember(original) { (original.trigger as Trigger.WifiConnected).ssids.toMutableStateList() }
    val actions = remember(original) { original.actions.toMutableStateList() }

    fun draft() = original.copy(
        name = name.trim(),
        trigger = Trigger.WifiConnected(ssids.toList()),
        actions = actions.toList(),
    )
    val dirty = draft() != original.copy(name = original.name.trim()) || macroId == null
    val valid = name.isNotBlank() && ssids.isNotEmpty() && actions.isNotEmpty()

    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var nearbyOpen by remember { mutableStateOf(false) }
    var typeOpen by remember { mutableStateOf(false) }

    val leave: () -> Unit = {
        if (dirty && valid && macroId != null) {
            confirmDiscard = true
        } else {
            onClose()
        }
    }
    BackHandler(onBack = leave)

    fun save() {
        val macro = draft()
        appScope.launch {
            store.save(macro)
            Engine.sync(context, seedCurrent = true)
        }
        onClose()
    }

    fun addSsid(ssid: String) {
        val clean = ssid.trim()
        if (clean.isNotEmpty() && clean !in ssids) ssids.add(clean)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = leave) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {},
                actions = {
                    IconButton(
                        enabled = valid,
                        onClick = {
                            val macro = draft()
                            scope.launch { snackbar.showSnackbar("Running “${macro.name}”…") }
                            appScope.launch {
                                Engine.run(context, macro)
                                val result = store.log.value.firstOrNull()?.message ?: "Done"
                                scope.launch { snackbar.showSnackbar(result) }
                            }
                        },
                    ) { Icon(Icons.Outlined.PlayArrow, contentDescription = "Test run") }
                    if (macroId != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete")
                        }
                    }
                    TextButton(enabled = valid && dirty, onClick = ::save) { Text("Save") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = ProseFamily,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                decorationBox = { inner ->
                    if (name.isEmpty()) {
                        Text(
                            "Name",
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = ProseFamily),
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    inner()
                },
            )

            SectionHeader("When")
            Column(Modifier.panel().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Wifi, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("WiFi connects to", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.size(12.dp))
                if (ssids.isEmpty()) {
                    Text(
                        "No network chosen yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ssids.forEach { ssid ->
                            InputChip(
                                selected = true,
                                onClick = { ssids.remove(ssid) },
                                label = { Text(ssid) },
                                trailingIcon = { Icon(Icons.Outlined.Close, "Remove $ssid", Modifier.size(18.dp)) },
                            )
                        }
                    }
                }
                Spacer(Modifier.size(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {
                            val current = Wifi.currentSsid(context)
                            if (current != null) addSsid(current)
                            else scope.launch {
                                snackbar.showSnackbar("Can't read the network name — connect to WiFi and allow location access")
                            }
                        },
                        label = { Text("Current network") },
                        leadingIcon = { Icon(Icons.Outlined.Wifi, null, Modifier.size(AssistChipDefaults.IconSize)) },
                    )
                    AssistChip(
                        onClick = { nearbyOpen = true },
                        label = { Text("Nearby") },
                        leadingIcon = { Icon(Icons.Outlined.Radar, null, Modifier.size(AssistChipDefaults.IconSize)) },
                    )
                    AssistChip(
                        onClick = { typeOpen = true },
                        label = { Text("Type a name") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, null, Modifier.size(AssistChipDefaults.IconSize)) },
                    )
                }
            }

            SectionHeader("Then")
            actions.forEachIndexed { index, action ->
                ActionRow(
                    action = action,
                    onChange = { actions[index] = it },
                    onRemove = { actions.removeAt(index) },
                )
            }
            AddAction(onAdd = { actions.add(it) })
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && actions.any { it is Action.SetBluetooth }) {
                Text(
                    "Android 13 and later let apps switch Bluetooth only through Shizuku. " +
                        "When it isn't running, you'll get a notification to finish the job in one tap.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        }
    }

    if (nearbyOpen) {
        NearbyDialog(
            networks = remember { Wifi.nearby(context) },
            chosen = ssids,
            onPick = { addSsid(it); nearbyOpen = false },
            onDismiss = { nearbyOpen = false },
        )
    }
    if (typeOpen) {
        TypeDialog(onDone = { addSsid(it); typeOpen = false }, onDismiss = { typeOpen = false })
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard changes?") },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; onClose() }) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false; save() }) { Text("Save") } },
        )
    }
    if (confirmDelete && macroId != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete “${original.name}”?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    appScope.launch {
                        store.delete(macroId)
                        Engine.sync(context)
                    }
                    onClose()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionRow(action: Action, onChange: (Action) -> Unit, onRemove: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.panel().padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
    ) {
        when (action) {
            is Action.SetBluetooth -> {
                Icon(Icons.Outlined.Bluetooth, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("Bluetooth", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                SingleChoiceSegmentedButtonRow {
                    listOf(false to "Off", true to "On").forEachIndexed { i, (on, label) ->
                        SegmentedButton(
                            selected = action.on == on,
                            onClick = { onChange(action.copy(on = on)) },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                        ) { Text(label) }
                    }
                }
            }
        }
        IconButton(onClick = onRemove) { Icon(Icons.Outlined.Close, contentDescription = "Remove action") }
    }
}

@Composable
private fun AddAction(onAdd: (Action) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        TextButton(onClick = { open = true }) {
            Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add action")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ActionKind.entries.forEach { kind ->
                DropdownMenuItem(text = { Text(kind.label) }, onClick = { open = false; onAdd(kind.create()) })
            }
        }
    }
}

@Composable
private fun NearbyDialog(networks: List<String>, chosen: List<String>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nearby networks") },
        text = {
            if (networks.isEmpty()) {
                Text(
                    "Nothing in the last scan. Location access must be allowed, and WiFi on.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(networks) { ssid ->
                        val taken = ssid in chosen
                        Text(
                            ssid,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (taken) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !taken) { onPick(ssid) }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun TypeDialog(onDone: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Network name") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text("Exactly as it appears, case included") },
            )
        },
        confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = { onDone(text) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
