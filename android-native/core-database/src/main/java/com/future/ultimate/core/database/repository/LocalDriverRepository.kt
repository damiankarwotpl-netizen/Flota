package com.future.ultimate.core.database.repository

import android.content.Context
import com.future.ultimate.core.common.model.VehicleReportDraft
import com.future.ultimate.core.common.pdf.VehicleReportPdfExporter
import com.future.ultimate.core.common.repository.DriverMileageSyncState
import com.future.ultimate.core.common.repository.DriverRepository
import com.future.ultimate.core.common.repository.DriverSession
import com.future.ultimate.core.database.dao.AppDao
import com.future.ultimate.core.database.entity.DriverAccountEntity
import com.future.ultimate.core.database.entity.SettingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    override suspend fun login(login: String, password: String): DriverSession {
        val account = dao.getDriverAccount(login.trim(), password)
            ?: throw IllegalArgumentException("Błędny login lub hasło")

        return DriverSession(
            login = account.login,
            password = account.password,
            driverName = account.driverName,
            registration = account.registration,
            changePasswordRequired = account.changePassword == 1,
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

        val account = DriverAccountEntity(
            registration = current.registration.trim().uppercase(),
            login = current.login,
            password = newPassword,
            driverName = current.driverName,
            changePassword = 0,
        )
        DriverRemoteSyncGateway.syncDriverUpsert(
            dao = dao,
            account = account,
            action = "reset_driver",
            successStatus = "Hasło kierowcy zsynchronizowane zdalnie",
        )
        dao.upsertDriverAccount(account)
        session.value = current.copy(password = newPassword, changePasswordRequired = false)
        persistSessionRegistration(account.registration)
    }

    override suspend fun saveMileage(login: String, registration: String, mileage: Int) {
        val current = session.value ?: throw IllegalStateException("Brak aktywnej sesji kierowcy")
        val targetRegistration = registration.trim().ifBlank { current.registration }.uppercase()
        DriverMileageSyncCoordinator.queueMileage(dao, targetRegistration, mileage.coerceAtLeast(0))
        DriverMileageSyncCoordinator.flushPending(dao, targetRegistration)
    }

    override suspend fun flushPendingMileageSync(): DriverMileageSyncState =
        DriverMileageSyncCoordinator.flushPending(dao, session.value?.registration)

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
        DriverSession(
            login = account.login,
            password = account.password,
            driverName = account.driverName,
            registration = account.registration,
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
}
