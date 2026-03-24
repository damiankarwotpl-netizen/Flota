package com.future.ultimate.core.database.repository

import android.content.Context
import com.future.ultimate.core.common.model.VehicleReportDraft
import com.future.ultimate.core.common.pdf.VehicleReportPdfExporter
import com.future.ultimate.core.common.repository.DriverMileageSyncState
import com.future.ultimate.core.common.repository.DriverRemoteEndpointSettings
import com.future.ultimate.core.common.repository.DriverRepository
import com.future.ultimate.core.common.repository.DriverSession
import com.future.ultimate.core.database.dao.AppDao
import com.future.ultimate.core.database.entity.DriverAccountEntity
import com.future.ultimate.core.database.entity.SettingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class LocalDriverRepository(
    private val dao: AppDao,
    private val context: Context,
) : DriverRepository {
    private companion object {
        const val SessionRegistrationKey = "driver_session_registration"
    }

    private val session = MutableStateFlow<DriverSession?>(restorePersistedSession())

    override fun observeSession(): Flow<DriverSession?> = session.asStateFlow()
    override fun observeMileageSyncState(): Flow<DriverMileageSyncState> =
        session.combine(dao.observeSettings()) { currentSession, settings ->
            DriverMileageSyncCoordinator.buildState(
                settings = settings.associateBy({ it.key }, { it.valText }),
                registration = currentSession?.registration,
            )
        }

    override fun observeRemoteEndpointSettings(): Flow<DriverRemoteEndpointSettings> =
        dao.observeSettings().map { settings ->
            DriverRemoteEndpointSettings(
                apiUrl = settings
                    .firstOrNull { it.key == DriverRemoteSyncGateway.EndpointSettingKey }
                    ?.valText
                    .orEmpty()
                    .ifBlank { DriverRemoteSyncGateway.DefaultDriverRemoteApiUrl },
            )
        }

    override suspend fun login(login: String, password: String): DriverSession {
        val normalizedLogin = login.trim()
        val normalizedPassword = password.trim()

        val remoteAccount = runCatching {
            DriverRemoteSyncGateway.loginDriver(
                dao = dao,
                login = normalizedLogin,
                password = normalizedPassword,
            )
        }.getOrNull()

        val primaryAccount = when {
            remoteAccount != null -> {
                if (remoteAccount.registration.isNotBlank()) {
                    dao.upsertDriverAccount(remoteAccount)
                }
                remoteAccount
            }

            else -> dao.getDriverAccount(normalizedLogin, normalizedPassword)
                ?: throw IllegalArgumentException("Błędny login lub hasło")
        }
        val matchingAccounts = dao.getDriverAccountsByLogin(primaryAccount.login)
            .filter { it.password == primaryAccount.password }
            .ifEmpty { listOf(primaryAccount) }
        val availableRegistrations = matchingAccounts
            .map { it.registration.trim().uppercase() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        val persistedRegistration = dao.getSetting(SessionRegistrationKey)?.valText.orEmpty().trim().uppercase()
        val activeRegistration = availableRegistrations.firstOrNull { it == persistedRegistration }
            ?: availableRegistrations.firstOrNull()
            ?: primaryAccount.registration.trim().uppercase()
        val activeAccount = matchingAccounts.firstOrNull { it.registration.equals(activeRegistration, ignoreCase = true) }
            ?: primaryAccount

        return DriverSession(
            login = activeAccount.login,
            password = activeAccount.password,
            driverName = activeAccount.driverName,
            registration = activeRegistration,
            availableRegistrations = availableRegistrations.ifEmpty { listOf(activeRegistration).filter { it.isNotBlank() } },
            changePasswordRequired = matchingAccounts.any { it.changePassword == 1 },
        ).also {
            session.value = it
            persistSessionRegistration(it.registration)
        }
    }

    override suspend fun logout() {
        session.value = null
        persistSessionRegistration("")
    }

    override suspend fun changePassword(login: String, password: String) {
        val current = session.value ?: throw IllegalStateException("Brak aktywnej sesji kierowcy")
        if (!current.login.equals(login.trim(), ignoreCase = true)) {
            throw IllegalArgumentException("Sesja nie pasuje do wskazanego loginu")
        }
        val newPassword = password.trim()
        if (newPassword.isBlank()) throw IllegalArgumentException("Hasło nie może być puste")

        val registrations = dao.getDriverAccountsByLogin(current.login)
            .map { it.registration.trim().uppercase() }
            .filter { it.isNotBlank() }
            .distinct()
            .ifEmpty { listOf(current.registration.trim().uppercase()) }
        registrations.forEach { registration ->
            val existingAccount = dao.getDriverAccountByRegistration(registration)
            val account = DriverAccountEntity(
                registration = registration,
                login = current.login,
                password = newPassword,
                driverName = existingAccount?.driverName ?: current.driverName,
                changePassword = 0,
                licenseType = existingAccount?.licenseType ?: "PL",
                licenseValidUntil = existingAccount?.licenseValidUntil.orEmpty(),
            )
            DriverRemoteSyncGateway.syncDriverUpsert(
                dao = dao,
                account = account,
                action = "reset_driver",
                successStatus = "Hasło kierowcy zsynchronizowane zdalnie",
            )
            dao.upsertDriverAccount(account)
        }
        session.value = current.copy(password = newPassword, changePasswordRequired = false)
        persistSessionRegistration(current.registration)
    }

    override suspend fun selectRegistration(registration: String) {
        val current = session.value ?: throw IllegalStateException("Brak aktywnej sesji kierowcy")
        val normalizedRegistration = registration.trim().uppercase()
        require(normalizedRegistration.isNotBlank()) { "Wybierz rejestrację" }
        val allowedRegistrations = current.availableRegistrations.map { it.trim().uppercase() }
        require(allowedRegistrations.contains(normalizedRegistration)) {
            "Nie możesz wybrać rejestracji spoza przypisanych pojazdów"
        }
        session.value = current.copy(registration = normalizedRegistration)
        persistSessionRegistration(normalizedRegistration)
    }

    override suspend fun saveMileage(login: String, registration: String, mileage: Int) {
        val current = session.value ?: throw IllegalStateException("Brak aktywnej sesji kierowcy")
        val assignedRegistration = requireActiveAssignment(current)
        val targetRegistration = registration.trim().ifBlank { assignedRegistration }.uppercase()
        require(targetRegistration == assignedRegistration) {
            "Nie możesz wysłać przebiegu dla innego samochodu niż aktualnie przypisany"
        }
        val normalizedMileage = mileage.coerceAtLeast(0)
        val localMileage = dao.getCarByRegistration(targetRegistration)?.mileage ?: 0
        val syncedMileage = dao.getSetting("driver_last_mileage_$targetRegistration")?.valText?.toIntOrNull() ?: 0
        val highestKnownMileage = maxOf(localMileage, syncedMileage)
        require(normalizedMileage >= highestKnownMileage) {
            "Przebieg mniejszy niż ostatni, sprawdź wprowadzone dane"
        }
        DriverMileageSyncCoordinator.queueMileage(
            dao = dao,
            registration = targetRegistration,
            mileage = normalizedMileage,
            login = current.login,
            driverName = current.driverName,
        )
        DriverMileageSyncCoordinator.flushPending(dao, targetRegistration)
    }

    override suspend fun flushPendingMileageSync(): DriverMileageSyncState =
        DriverMileageSyncCoordinator.flushPending(dao, session.value?.registration)

    override suspend fun saveRemoteEndpointSettings(settings: DriverRemoteEndpointSettings) {
        DriverRemoteSyncGateway.saveEndpoint(dao, settings.apiUrl)
    }

    override suspend fun validateRemoteEndpointSettings(settings: DriverRemoteEndpointSettings): String =
        DriverRemoteSyncGateway.validateEndpoint(dao, settings.apiUrl)

    override suspend fun saveVehicleReportDraft(draft: VehicleReportDraft) {
        val current = session.value ?: throw IllegalStateException("Brak aktywnej sesji kierowcy")
        val registration = draft.rej.ifBlank { current.registration }.uppercase()
        dao.upsertSetting(SettingEntity(key = "driver_vehicle_report_registration", valText = registration))
        dao.upsertSetting(SettingEntity(key = "driver_vehicle_report_payload", valText = draft.copy(rej = registration).toString()))
    }

    override suspend fun exportVehicleReportPdf(draft: VehicleReportDraft): String =
        VehicleReportPdfExporter.export(context, draft, ownerTag = "driver")

    private fun restorePersistedSession(): DriverSession? = runBlocking {
        val registration = dao.getSetting(SessionRegistrationKey)?.valText.orEmpty().trim().uppercase()
        if (registration.isBlank()) return@runBlocking null
        val account = dao.getDriverAccountByRegistration(registration) ?: run {
            dao.upsertSetting(SettingEntity(key = SessionRegistrationKey, valText = ""))
            return@runBlocking null
        }
        val registrations = dao.getDriverAccountsByLogin(account.login)
            .filter { it.password == account.password }
            .map { it.registration.trim().uppercase() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        DriverSession(
            login = account.login,
            password = account.password,
            driverName = account.driverName,
            registration = account.registration,
            availableRegistrations = registrations.ifEmpty { listOf(account.registration.trim().uppercase()) },
            changePasswordRequired = account.changePassword == 1,
        )
    }

    private suspend fun persistSessionRegistration(registration: String) {
        dao.upsertSetting(
            SettingEntity(
                key = SessionRegistrationKey,
                valText = registration.trim().uppercase(),
            ),
        )
    }

    private suspend fun requireActiveAssignment(current: DriverSession): String {
        val sessionRegistration = current.registration.trim().uppercase()
        val allowedRegistrations = current.availableRegistrations.map { it.trim().uppercase() }.filter { it.isNotBlank() }
        if (sessionRegistration.isBlank()) {
            invalidateSession()
            throw IllegalStateException("Nie masz już przypisanego samochodu. Zaloguj się ponownie po nowym przypisaniu.")
        }
        if (allowedRegistrations.isNotEmpty() && !allowedRegistrations.contains(sessionRegistration)) {
            invalidateSession()
            throw IllegalStateException("Wybrana rejestracja nie jest już przypisana. Zaloguj się ponownie.")
        }

        val remoteLookup = runCatching { DriverRemoteSyncGateway.findDriverAccount(dao, current.login) }
        remoteLookup.getOrNull()?.let { remoteAccount ->
            val remoteRegistration = remoteAccount.registration.trim().uppercase()
            if (remoteRegistration.isBlank()) {
                invalidateSession()
                throw IllegalStateException("Przypisanie samochodu zostało usunięte. Zaloguj się ponownie po nowym przypisaniu.")
            }
            if (remoteRegistration != sessionRegistration && allowedRegistrations.isNotEmpty()) {
                return sessionRegistration
            }
            return remoteRegistration
        }

        if (remoteLookup.isSuccess) {
            invalidateSession()
            throw IllegalStateException("Nie masz już przypisanego samochodu. Zaloguj się ponownie po nowym przypisaniu.")
        }

        return sessionRegistration
    }

    private suspend fun invalidateSession() {
        session.value = null
        persistSessionRegistration("")
    }
}
