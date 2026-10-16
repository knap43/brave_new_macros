package com.knap43.macros.engine

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.knap43.macros.R

/** The fallback when an action can't be done silently: a notification that takes you one tap away. */
object Prompts {
    private const val CHANNEL = "prompts"
    private const val BLUETOOTH_ID = 1

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL, "Action prompts", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Asks you to finish an action Android won't let the app do on its own"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED)

    /** Returns false if notifications are off, so the caller can log that nothing reached you. */
    @SuppressLint("MissingPermission") // Checked in canPost.
    fun bluetooth(context: Context, on: Boolean, macro: String): Boolean {
        if (!canPost(context)) return false
        val open = PendingIntent.getActivity(
            context,
            BLUETOOTH_ID,
            Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_bolt)
            .setColor(ContextCompat.getColor(context, R.color.accent))
            .setContentTitle(if (on) "Turn Bluetooth on" else "Turn Bluetooth off")
            .setContentText("$macro · tap to open Bluetooth settings")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setTimeoutAfter(10 * 60 * 1000L)
            .build()
        NotificationManagerCompat.from(context).notify(BLUETOOTH_ID, notification)
        return true
    }

    fun dismissBluetooth(context: Context) = NotificationManagerCompat.from(context).cancel(BLUETOOTH_ID)
}
