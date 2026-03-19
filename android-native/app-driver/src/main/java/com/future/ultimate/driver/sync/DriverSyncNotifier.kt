package com.future.ultimate.driver.sync

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.future.ultimate.core.common.repository.DriverMileageSyncState

object DriverSyncNotifier {
    private const val ChannelId = "driver-mileage-sync"
    private const val ChannelName = "Synchronizacja kierowcy"
    private const val NotificationId = 4101
    private const val PreferenceName = "driver_sync_notifications"
    private const val LastPayloadKey = "last_payload"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(ChannelId) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                ChannelId,
                ChannelName,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Status synchronizacji przebiegu kierowcy"
            },
        )
    }

    fun notifySyncState(context: Context, state: DriverMileageSyncState) {
        if (state.registration.isBlank()) return
        val payload = when {
            state.pendingCount > 0 -> {
                "Synchronizacja przebiegu oczekuje" to buildString {
                    append(state.status)
                    state.error.takeIf { it.isNotBlank() }?.let { append(" • $it") }
                }
            }

            state.error.isNotBlank() -> {
                "Błąd synchronizacji przebiegu" to "${state.status} • ${state.error}"
            }

            state.status.equals("Zsynchronizowano", ignoreCase = true) -> {
                "Przebieg zsynchronizowany" to buildString {
                    append("Auto: ${state.registration}")
                    state.lastSyncedAt.takeIf { it.isNotBlank() }?.let { append(" • $it") }
                }
            }

            else -> null
        } ?: return

        postNotification(context, payload.first, payload.second)
    }

    fun notifyWorkerFailure(context: Context, message: String) {
        postNotification(
            context = context,
            title = "Błąd pracy synchronizacji",
            body = message,
        )
    }

    private fun postNotification(context: Context, title: String, body: String) {
        if (!canPostNotifications(context)) return
        val payload = "$title|$body"
        if (payload == lastPayload(context)) return

        runCatching {
            ensureChannel(context)
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.notify(
                NotificationId,
                Notification.Builder(context, ChannelId)
                    .setSmallIcon(android.R.drawable.stat_notify_sync)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(Notification.BigTextStyle().bigText(body))
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(true)
                    .build(),
            )
            rememberPayload(context, payload)
        }
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        return manager.areNotificationsEnabled()
    }

    private fun lastPayload(context: Context): String =
        context.getSharedPreferences(PreferenceName, Context.MODE_PRIVATE).getString(LastPayloadKey, "").orEmpty()

    private fun rememberPayload(context: Context, payload: String) {
        context.getSharedPreferences(PreferenceName, Context.MODE_PRIVATE)
            .edit()
            .putString(LastPayloadKey, payload)
            .apply()
    }
}
