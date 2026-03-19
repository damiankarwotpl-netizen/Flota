package com.future.ultimate.core.database.repository

import com.future.ultimate.core.common.repository.DriverMileageSyncState
import com.future.ultimate.core.database.dao.AppDao
import com.future.ultimate.core.database.entity.SettingEntity
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal object DriverMileageSyncCoordinator {
    private const val DefaultDriverRemoteApiUrl =
        "https://script.google.com/macros/s/AKfycbxFQLZU-sg8Gg58J2dE-Bbt2jTyXrdcd1DOUM78vcqFLa789gpeOC9S4MyjGHpQ12_l/exec"
    private const val PendingPrefix = "driver_mileage_sync_pending_"
    private const val StatusPrefix = "driver_mileage_sync_status_"
    private const val AttemptPrefix = "driver_mileage_sync_attempt_"
    private const val SyncedPrefix = "driver_mileage_sync_at_"
    private const val ErrorPrefix = "driver_mileage_sync_error_"
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    suspend fun queueMileage(dao: AppDao, registration: String, mileage: Int) {
        val normalized = registration.trim().uppercase()
        require(normalized.isNotBlank()) { "Brak rejestracji do synchronizacji przebiegu" }
        val now = nowText()
        dao.upsertSetting(SettingEntity(key = pendingKey(normalized), valText = "${mileage.coerceAtLeast(0)}|$now"))
        dao.upsertSetting(SettingEntity(key = statusKey(normalized), valText = "Oczekuje na synchronizację"))
        dao.upsertSetting(SettingEntity(key = attemptKey(normalized), valText = now))
        dao.upsertSetting(SettingEntity(key = errorKey(normalized), valText = ""))
    }

    suspend fun flushPending(dao: AppDao, registration: String? = null): DriverMileageSyncState {
        val settings = dao.observeSettings().firstSnapshot()
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
            if (car == null) {
                dao.upsertSetting(SettingEntity(key = statusKey(reg), valText = "Retry: brak auta do synchronizacji"))
                dao.upsertSetting(SettingEntity(key = errorKey(reg), valText = "Nie znaleziono auta o rejestracji $reg"))
                return@forEach
            }
            val driverAccount = dao.getDriverAccountByRegistration(reg)

            runCatching {
                dao.updateMileageByRegistration(reg, pending.mileage)
                syncRemoteMileage(
                    dao = dao,
                    registration = reg,
                    mileage = pending.mileage,
                    queuedAt = pending.queuedAt.ifBlank { attemptAt },
                    login = driverAccount?.login.orEmpty(),
                    driverName = driverAccount?.driverName.orEmpty(),
                )
                dao.upsertSetting(SettingEntity(key = "driver_last_mileage_$reg", valText = pending.mileage.toString()))
                dao.upsertSetting(SettingEntity(key = syncedKey(reg), valText = attemptAt))
                dao.upsertSetting(SettingEntity(key = statusKey(reg), valText = "Zsynchronizowano"))
                dao.upsertSetting(SettingEntity(key = errorKey(reg), valText = ""))
                dao.upsertSetting(SettingEntity(key = pendingKey(reg), valText = ""))
            }.onFailure { error ->
                dao.upsertSetting(SettingEntity(key = statusKey(reg), valText = "Retry po błędzie synchronizacji"))
                dao.upsertSetting(
                    SettingEntity(
                        key = errorKey(reg),
                        valText = error.message ?: "Nieznany błąd synchronizacji przebiegu",
                    ),
                )
            }
        }

        return buildState(settings = dao.observeSettings().firstSnapshot(), registration = registration)
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
    ) {
        val endpoint = loadRemoteEndpoint(dao)
        require(endpoint.isNotBlank()) { "Brak endpointu zdalnej synchronizacji przebiegu" }

        val payload = JSONObject().apply {
            put("action", "mileage_update")
            put("registration", registration)
            put("plate", registration)
            put("mileage", mileage.coerceAtLeast(0))
            put("timestamp", queuedAt.ifBlank { nowText() })
            if (login.isNotBlank()) put("login", login)
            if (driverName.isNotBlank()) put("name", driverName)
        }

        val (statusCode, responseBody) = postJson(endpoint, payload)
        require(statusCode in 200..299) { "HTTP $statusCode" }
        validateRemoteResponse(responseBody)
    }

    private suspend fun loadRemoteEndpoint(dao: AppDao): String =
        dao.getSetting("driver_remote_api_url")?.valText.orEmpty().ifBlank { DefaultDriverRemoteApiUrl }

    private suspend fun postJson(url: String, payload: JSONObject): Pair<Int, String> = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 10000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }

        try {
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
            }
            val responseCode = connection.responseCode
            val responseBody = runCatching {
                val source = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                source?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            }.getOrDefault("")
            responseCode to responseBody
        } finally {
            connection.disconnect()
        }
    }

    private fun validateRemoteResponse(body: String) {
        if (body.isBlank()) return
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return
        val status = json.optString("status").trim().lowercase()
        val okStatuses = setOf("", "ok", "queued", "saved", "created", "success")
        require(status in okStatuses) {
            json.optString("message")
                .takeIf { it.isNotBlank() }
                ?: json.optString("error").takeIf { it.isNotBlank() }
                ?: "Zdalny endpoint odrzucił aktualizację przebiegu"
        }
    }

    private data class PendingMileagePayload(
        val mileage: Int,
        val queuedAt: String,
    )
}

private suspend fun AppDao.firstSnapshot(): Map<String, String> =
    observeSettings().first().associateBy({ it.key }, { it.valText })
