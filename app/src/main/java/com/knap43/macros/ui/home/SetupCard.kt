package com.knap43.macros.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.knap43.macros.engine.Engine
import com.knap43.macros.engine.Prompts
import com.knap43.macros.engine.Shell
import com.knap43.macros.ui.panel

private data class Step(
    val icon: ImageVector,
    val title: String,
    val detail: String,
    val button: String,
    val act: () -> Unit,
)

/**
 * Everything Android needs granted before a macro can fire unattended, listed only while missing.
 * Re-checked whenever the app comes back to the front, since most fixes happen in Settings.
 */
@Composable
fun SetupCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++; Engine.sync(context) }
    val shizuku by Shell.changes.collectAsState()

    val location = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tick++ }
    val background = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // Some skins refuse the in-place prompt; the app's own settings page always works.
        if (!granted) context.startActivity(appSettings(context))
        tick++
    }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    val steps = remember(tick, shizuku) {
        buildList {
            if (!granted(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
                add(Step(Icons.Outlined.LocationOn, "Location access", "Android reveals the WiFi name only to apps with precise location.", "Allow") {
                    location.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                })
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                !granted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            ) {
                add(Step(Icons.Outlined.MyLocation, "Allow all the time", "So the name can still be read when the app isn't open.", "Allow") {
                    background.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                })
            }
            if (!locationOn(context)) {
                add(Step(Icons.Outlined.LocationOff, "Location is off", "With it off, every WiFi network looks nameless.", "Turn on") {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                })
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                shizukuStep(context)?.let { add(it) }
                if (!Prompts.canPost(context)) {
                    add(Step(Icons.Outlined.NotificationsOff, "Notifications", "Needed for the one-tap prompt when Shizuku isn't running.", "Allow") {
                        notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    })
                }
            }
            if (!unrestricted(context)) {
                add(Step(Icons.Outlined.BatteryAlert, "Battery restrictions", "Some phones freeze background apps and swallow the WiFi wake-up.", "Allow") {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")),
                    )
                })
            }
        }
    }

    if (steps.isEmpty()) return
    Column(modifier.panel().padding(vertical = 8.dp)) {
        Text(
            "Before it can run on its own",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        )
        steps.forEach { step ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            ) {
                Icon(step.icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(step.title, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        step.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = step.act) { Text(step.button) }
            }
        }
    }
}

private fun shizukuStep(context: Context): Step? = when (Shell.status(context)) {
    Shell.Status.READY -> null
    Shell.Status.NOT_INSTALLED -> Step(
        Icons.Outlined.Terminal, "Shizuku (optional)",
        "Lets Bluetooth switch silently. Without it you'll get a prompt to finish with one tap.", "Get",
    ) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RikkaApps/Shizuku/releases")))
    }
    Shell.Status.NOT_RUNNING -> Step(
        Icons.Outlined.Terminal, "Shizuku isn't running",
        "Start it in Shizuku over wireless debugging; it stops at every reboot.", "Open",
    ) {
        context.packageManager.getLaunchIntentForPackage(Shell.SHIZUKU_PACKAGE)?.let { context.startActivity(it) }
    }
    Shell.Status.NO_PERMISSION -> Step(
        Icons.Outlined.Terminal, "Shizuku access", "Shizuku is running but hasn't let this app in yet.", "Grant",
    ) { Shell.requestPermission() }
}

private fun granted(context: Context, permission: String) =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun locationOn(context: Context): Boolean {
    val manager = context.getSystemService(LocationManager::class.java) ?: return false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) return manager.isLocationEnabled
    @Suppress("DEPRECATION")
    return Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE, 0) != 0
}

private fun unrestricted(context: Context) =
    context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) ?: true

private fun appSettings(context: Context) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
