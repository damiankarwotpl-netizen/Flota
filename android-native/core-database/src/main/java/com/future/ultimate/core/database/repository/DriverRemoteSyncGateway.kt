package com.future.ultimate.core.database.repository

import com.future.ultimate.core.database.dao.AppDao
import com.future.ultimate.core.database.entity.DriverAccountEntity
import com.future.ultimate.core.database.entity.SettingEntity
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal object DriverRemoteSyncGateway {
    const val EndpointSettingKey = "driver_remote_api_url"
    const val DefaultDriverRemoteApiUrl =
        "https://script.google.com/macros/s/AKfycbxFQLZU-sg8Gg58J2dE-Bbt2jTyXrdcd1DOUM78vcqFLa789gpeOC9S4MyjGHpQ12_l/exec"

    private val okStatuses = setOf("", "ok", "queued", "saved", "created", "success")
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    suspend fun syncDriverUpsert(
        dao: AppDao,
        account: DriverAccountEntity,
        action: String,
        successStatus: String = "Zdalne konto kierowcy zsynchronizowane",
    ) {
        val payload = JSONObject().apply {
            put("action", action)
            put("login", account.login)
            put("password", account.password)
            put("name", account.driverName)
            put("registration", account.registration)
        }
        postDriverPayload(dao, account.registration, payload, successStatus)
        syncDriverAssignment(dao, account)
    }

    suspend fun syncDriverAssignment(dao: AppDao, account: DriverAccountEntity) {
        val payload = JSONObject().apply {
            put("action", "sync_driver_assignment")
            put("login", account.login)
            put("name", account.driverName)
            put("registration", account.registration)
        }
        postDriverPayload(dao, account.registration, payload, successStatus = "Zdalne przypisanie kierowcy zsynchronizowane")
    }

    suspend fun syncDriverDeletion(dao: AppDao, registration: String) {
        val normalizedRegistration = registration.trim().uppercase()
        if (normalizedRegistration.isBlank()) return
        val payload = JSONObject().apply {
            put("action", "delete_driver")
            put("registration", normalizedRegistration)
        }
        postDriverPayload(dao, normalizedRegistration, payload, successStatus = "Zdalne konto kierowcy usunięte")
    }

    suspend fun syncMileage(
        dao: AppDao,
        registration: String,
        mileage: Int,
        queuedAt: String,
        login: String,
        driverName: String,
    ) {
        val payload = JSONObject().apply {
            put("action", "mileage_update")
            put("registration", registration)
            put("plate", registration)
            put("mileage", mileage.coerceAtLeast(0))
            put("timestamp", queuedAt.ifBlank { nowText() })
            if (login.isNotBlank()) put("login", login)
            if (driverName.isNotBlank()) put("name", driverName)
        }

        val (statusCode, responseBody) = postPayload(dao, payload)
        require(statusCode in 200..299) { "HTTP $statusCode" }
        validateResponse(responseBody, fallbackMessage = "Zdalny endpoint odrzucił aktualizację przebiegu")
    }

    suspend fun loadEndpoint(dao: AppDao): String =
        dao.getSetting(EndpointSettingKey)?.valText.orEmpty().ifBlank { DefaultDriverRemoteApiUrl }

    suspend fun saveEndpoint(dao: AppDao, endpoint: String) {
        dao.upsertSetting(SettingEntity(key = EndpointSettingKey, valText = endpoint.trim()))
    }

    suspend fun validateEndpoint(dao: AppDao, endpointOverride: String = ""): String {
        val endpoint = endpointOverride.trim().ifBlank { loadEndpoint(dao) }
        require(endpoint.isNotBlank()) { "Brak endpointu zdalnej synchronizacji kierowców" }
        val (statusCode, responseBody) = postPayloadToUrl(
            endpoint = endpoint,
            payload = JSONObject().apply { put("action", "get_logs") },
        )
        require(statusCode in 200..299) { "HTTP $statusCode" }
        validateProbeResponse(responseBody)
        return "Zdalny endpoint kierowców odpowiada prawidłowo"
    }

    private suspend fun postDriverPayload(
        dao: AppDao,
        registration: String,
        payload: JSONObject,
        successStatus: String,
    ) {
        val normalizedRegistration = registration.trim().uppercase()
        if (normalizedRegistration.isBlank()) return
        val endpoint = loadEndpoint(dao)
        if (endpoint.isBlank()) {
            markDriverRemoteSync(normalizedRegistration, dao, status = "Zdalny sync kierowców wyłączony", error = "")
            return
        }

        runCatching {
            val (statusCode, responseBody) = postPayload(dao, payload)
            require(statusCode in 200..299) { "HTTP $statusCode" }
            validateResponse(responseBody, fallbackMessage = "Zdalny endpoint odrzucił synchronizację kierowcy")
        }.onSuccess {
            markDriverRemoteSync(normalizedRegistration, dao, status = successStatus, error = "")
        }.onFailure { error ->
            markDriverRemoteSync(
                normalizedRegistration,
                dao,
                status = "Błąd zdalnej synchronizacji kierowcy",
                error = error.message.orEmpty().ifBlank { "Nieznany błąd zdalnego syncu" },
            )
            throw error
        }
    }

    private suspend fun postPayload(dao: AppDao, payload: JSONObject): Pair<Int, String> {
        val endpoint = loadEndpoint(dao)
        require(endpoint.isNotBlank()) { "Brak endpointu zdalnej synchronizacji kierowcy" }
        return postPayloadToUrl(endpoint = endpoint, payload = payload)
    }

    private suspend fun postPayloadToUrl(endpoint: String, payload: JSONObject): Pair<Int, String> = withContext(Dispatchers.IO) {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
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

    private suspend fun markDriverRemoteSync(
        registration: String,
        dao: AppDao,
        status: String,
        error: String,
    ) {
        val normalizedRegistration = registration.trim().uppercase()
        val timestamp = nowText()
        dao.upsertSetting(SettingEntity(key = "driver_remote_sync_at_$normalizedRegistration", valText = timestamp))
        dao.upsertSetting(SettingEntity(key = "driver_remote_sync_status_$normalizedRegistration", valText = status))
        dao.upsertSetting(SettingEntity(key = "driver_remote_sync_error_$normalizedRegistration", valText = error))
    }

    private fun validateResponse(body: String, fallbackMessage: String) {
        if (body.isBlank()) return
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return
        val status = json.optString("status").trim().lowercase()
        require(status in okStatuses) {
            json.optString("message")
                .takeIf { it.isNotBlank() }
                ?: json.optString("error").takeIf { it.isNotBlank() }
                ?: fallbackMessage
        }
    }

    private fun validateProbeResponse(body: String) {
        if (body.isBlank()) return
        val trimmed = body.trim()
        if (trimmed.startsWith("[")) return
        val json = runCatching { JSONObject(trimmed) }.getOrNull()
            ?: throw IllegalArgumentException("Endpoint zwrócił nieoczekiwaną odpowiedź")
        val status = json.optString("status").trim().lowercase()
        require(status !in setOf("error", "fail", "failed")) {
            json.optString("message")
                .takeIf { it.isNotBlank() }
                ?: json.optString("error").takeIf { it.isNotBlank() }
                ?: "Endpoint zgłosił błąd walidacji"
        }
    }

    private fun nowText(): String = LocalDateTime.now().format(timestampFormatter)
}
