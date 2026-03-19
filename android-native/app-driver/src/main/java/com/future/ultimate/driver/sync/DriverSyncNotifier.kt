package com.future.ultimate.driver.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.future.ultimate.core.common.repository.DriverMileageSyncState

object DriverSyncNotifier {
    private const val ChannelId = "driver-mileage-sync"
    private const val ChannelName = "Synchronizacja kierowcy"
    private const val NotificationId = 4101

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

        runCatching {
            ensureChannel(context)
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.notify(
                NotificationId,
                Notification.Builder(context, ChannelId)
                    .setSmallIcon(android.R.drawable.stat_notify_sync)
                    .setContentTitle(payload.first)
                    .setContentText(payload.second)
                    .setStyle(Notification.BigTextStyle().bigText(payload.second))
                    .setAutoCancel(true)
                    .build(),
            )
        }
    }
}
