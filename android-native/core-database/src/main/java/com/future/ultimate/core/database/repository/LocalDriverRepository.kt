package com.future.ultimate.core.database.repository

import android.content.Context
import com.future.ultimate.core.common.model.VehicleReportDraft
import com.future.ultimate.core.common.pdf.VehicleReportPdfExporter
import com.future.ultimate.core.common.repository.DriverRepository
import com.future.ultimate.core.common.repository.DriverSession
import com.future.ultimate.core.database.dao.AppDao
import com.future.ultimate.core.database.entity.SettingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalDriverRepository(
    private val dao: AppDao,
    private val context: Context,
) : DriverRepository {
    private val session = MutableStateFlow<DriverSession?>(null)

    override fun observeSession(): Flow<DriverSession?> = session.asStateFlow()

    override suspend fun login(login: String, password: String): DriverSession {
        val account = dao.getDriverAccount(login.trim(), password)
            ?: throw IllegalArgumentException("Błędny login lub hasło")

        return DriverSession(
            login = account.login,
            password = account.password,
            driverName = account.driverName,
            registration = account.registration,
            changePasswordRequired = account.changePassword == 1,
        ).also { session.value = it }
    }

    override suspend fun logout() {
        session.value = null
    }

    override suspend fun changePassword(login: String, password: String) {
        val current = session.value ?: throw IllegalStateException("Brak aktywnej sesji kierowcy")
        if (!current.login.equals(login.trim(), ignoreCase = true)) {
            throw IllegalArgumentException("Sesja nie pasuje do wskazanego loginu")
        }
        val newPassword = password.trim()
        if (newPassword.isBlank()) throw IllegalArgumentException("Hasło nie może być puste")

        dao.updateDriverPassword(current.registration, newPassword, 0)
        session.value = current.copy(password = newPassword, changePasswordRequired = false)
    }

    override suspend fun saveMileage(login: String, registration: String, mileage: Int) {
        val current = session.value ?: throw IllegalStateException("Brak aktywnej sesji kierowcy")
        val targetRegistration = registration.trim().ifBlank { current.registration }.uppercase()
        dao.updateMileageByRegistration(targetRegistration, mileage.coerceAtLeast(0))
        dao.upsertSetting(SettingEntity(key = "driver_last_mileage_${targetRegistration}", valText = mileage.coerceAtLeast(0).toString()))
    }

    override suspend fun saveVehicleReportDraft(draft: VehicleReportDraft) {
        val current = session.value ?: throw IllegalStateException("Brak aktywnej sesji kierowcy")
        val registration = draft.rej.ifBlank { current.registration }.uppercase()
        dao.upsertSetting(SettingEntity(key = "driver_vehicle_report_registration", valText = registration))
        dao.upsertSetting(SettingEntity(key = "driver_vehicle_report_payload", valText = draft.copy(rej = registration).toString()))
    }

    override suspend fun exportVehicleReportPdf(draft: VehicleReportDraft): String =
        VehicleReportPdfExporter.export(context, draft, ownerTag = "driver")
}
