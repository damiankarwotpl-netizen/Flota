package com.future.ultimate.core.database.repository

import com.future.ultimate.core.common.repository.DriverMileageSyncState
import com.future.ultimate.core.database.dao.AppDao
import com.future.ultimate.core.database.entity.SettingEntity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first

internal object DriverMileageSyncCoordinator {
    private const val PendingPrefix = "driver_mileage_sync_pending_"
    private const val LoginPrefix = "driver_mileage_sync_login_"
    private const val DriverNamePrefix = "driver_mileage_sync_driver_"
    private const val StatusPrefix = "driver_mileage_sync_status_"
    private const val AttemptPrefix = "driver_mileage_sync_attempt_"
    private const val SyncedPrefix = "driver_mileage_sync_at_"
    private const val ErrorPrefix = "driver_mileage_sync_error_"
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    suspend fun queueMileage(
        dao: AppDao,
        registration: String,
        mileage: Int,
        login: String = "",
        driverName: String = "",
    ) {
        val normalized = registration.trim().uppercase()
        require(normalized.isNotBlank()) { "Brak rejestracji do synchronizacji przebiegu" }
        val now = nowText()
        dao.upsertSetting(SettingEntity(key = pendingKey(normalized), valText = "${mileage.coerceAtLeast(0)}|$now"))
        dao.upsertSetting(SettingEntity(key = loginKey(normalized), valText = login.trim()))
        dao.upsertSetting(SettingEntity(key = driverNameKey(normalized), valText = driverName.trim()))
        dao.upsertSetting(SettingEntity(key = statusKey(normalized), valText = "Oczekuje na synchronizację"))
        dao.upsertSetting(SettingEntity(key = attemptKey(normalized), valText = now))
        dao.upsertSetting(SettingEntity(key = errorKey(normalized), valText = ""))
    }

    suspend fun flushPending(dao: AppDao, registration: String? = null): DriverMileageSyncState {
        val settings = dao.firstSnapshot()
        val targetRegistration = registration?.trim()?.uppercase().orEmpty()
        val pendingEntries = settings
            .filterKeys { key -> key.startsWith(PendingPrefix) }
            .filterKeys { key -> targetRegistration.isBlank() || key == pendingKey(targetRegistration) }

        pendingEntries.forEach { (key, payload) ->
            val reg = key.removePrefix(PendingPrefix)
            val pending = parsePendingPayload(payload)
            if (pending == null) {
                clearPending(dao, reg)
                return@forEach
            }

            val attemptAt = nowText()
            dao.upsertSetting(SettingEntity(key = attemptKey(reg), valText = attemptAt))
            val car = dao.getCarByRegistration(reg)
            val driverAccount = dao.getDriverAccountByRegistration(reg)
            val queuedLogin = settings[loginKey(reg)].orEmpty()
            val queuedDriverName = settings[driverNameKey(reg)].orEmpty()

            try {
                if (car != null) {
                    dao.updateMileageByRegistration(reg, pending.mileage)
                }
                syncRemoteMileage(
                    dao = dao,
                    registration = reg,
                    mileage = pending.mileage,
                    queuedAt = pending.queuedAt.ifBlank { attemptAt },
                    login = driverAccount?.login.orEmpty().ifBlank { queuedLogin },
                    driverName = driverAccount?.driverName.orEmpty().ifBlank { queuedDriverName },
                )
                dao.upsertSetting(SettingEntity(key = "driver_last_mileage_$reg", valText = pending.mileage.toString()))
                dao.upsertSetting(SettingEntity(key = syncedKey(reg), valText = attemptAt))
                dao.upsertSetting(
                    SettingEntity(
                        key = statusKey(reg),
                        valText = if (car == null) {
                            "Zsynchronizowano zdalnie (brak lokalnego auta)"
                        } else {
                            "Zsynchronizowano"
                        },
                    ),
                )
                dao.upsertSetting(SettingEntity(key = errorKey(reg), valText = ""))
                dao.upsertSetting(SettingEntity(key = pendingKey(reg), valText = ""))
            } catch (error: Exception) {
                dao.upsertSetting(SettingEntity(key = statusKey(reg), valText = "Retry po błędzie synchronizacji"))
                dao.upsertSetting(
                    SettingEntity(
                        key = errorKey(reg),
                        valText = error.message ?: "Nieznany błąd synchronizacji przebiegu",
                    ),
                )
            }
        }

        return buildState(settings = dao.firstSnapshot(), registration = registration)
    }

    fun buildState(settings: Map<String, String>, registration: String?): DriverMileageSyncState {
        val reg = registration?.trim()?.uppercase().orEmpty()
        if (reg.isBlank()) return DriverMileageSyncState()
        val pending = parsePendingPayload(settings[pendingKey(reg)].orEmpty())
        return DriverMileageSyncState(
            registration = reg,
            pendingCount = if (pending != null) 1 else 0,
            queuedMileage = pending?.mileage,
            lastAttemptAt = settings[attemptKey(reg)].orEmpty(),
            lastSyncedAt = settings[syncedKey(reg)].orEmpty(),
            status = settings[statusKey(reg)].orEmpty().ifBlank {
                if (pending != null) "Oczekuje na synchronizację" else "Brak aktywnej kolejki"
            },
            error = settings[errorKey(reg)].orEmpty(),
        )
    }

    private suspend fun clearPending(dao: AppDao, registration: String) {
        dao.upsertSetting(SettingEntity(key = pendingKey(registration), valText = ""))
        dao.upsertSetting(SettingEntity(key = loginKey(registration), valText = ""))
        dao.upsertSetting(SettingEntity(key = driverNameKey(registration), valText = ""))
        dao.upsertSetting(SettingEntity(key = statusKey(registration), valText = "Brak aktywnej kolejki"))
        dao.upsertSetting(SettingEntity(key = errorKey(registration), valText = ""))
    }

    private fun parsePendingPayload(payload: String): PendingMileagePayload? {
        val trimmed = payload.trim()
        if (trimmed.isBlank()) return null
        val parts = trimmed.split("|", limit = 2)
        val mileage = parts.firstOrNull()?.toIntOrNull() ?: return null
        val queuedAt = parts.getOrElse(1) { "" }
        return PendingMileagePayload(mileage = mileage, queuedAt = queuedAt)
    }

    private fun pendingKey(registration: String): String = "$PendingPrefix$registration"
    private fun loginKey(registration: String): String = "$LoginPrefix$registration"
    private fun driverNameKey(registration: String): String = "$DriverNamePrefix$registration"
    private fun statusKey(registration: String): String = "$StatusPrefix$registration"
    private fun attemptKey(registration: String): String = "$AttemptPrefix$registration"
    private fun syncedKey(registration: String): String = "$SyncedPrefix$registration"
    private fun errorKey(registration: String): String = "$ErrorPrefix$registration"
    private fun nowText(): String = LocalDateTime.now().format(timestampFormatter)

    private suspend fun syncRemoteMileage(
        dao: AppDao,
        registration: String,
        mileage: Int,
        queuedAt: String,
        login: String,
        driverName: String,
    ) = DriverRemoteSyncGateway.syncMileage(
        dao = dao,
        registration = registration,
        mileage = mileage,
        queuedAt = queuedAt,
        login = login,
        driverName = driverName,
    )

    private data class PendingMileagePayload(
        val mileage: Int,
        val queuedAt: String,
    )
}

private suspend fun AppDao.firstSnapshot(): Map<String, String> =
    observeSettings().first().associateBy({ it.key }, { it.valText })
