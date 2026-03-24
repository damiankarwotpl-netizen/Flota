package com.future.ultimate.core.database.repository

import com.future.ultimate.core.common.model.VehicleReportDraft
import com.future.ultimate.core.common.repository.DriverMileageSyncState
import com.future.ultimate.core.common.repository.DriverRemoteEndpointSettings
import com.future.ultimate.core.common.repository.DriverRepository
import com.future.ultimate.core.common.repository.DriverSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class StubDriverRepository : DriverRepository {
    private val session = MutableStateFlow<DriverSession?>(null)
    private val remoteEndpointSettings = MutableStateFlow(DriverRemoteEndpointSettings())

    override fun observeSession(): Flow<DriverSession?> = session.asStateFlow()
    override fun observeMileageSyncState(): Flow<DriverMileageSyncState> = MutableStateFlow(DriverMileageSyncState()).asStateFlow()
    override fun observeRemoteEndpointSettings(): Flow<DriverRemoteEndpointSettings> = remoteEndpointSettings.asStateFlow()

    override suspend fun login(login: String, password: String): DriverSession = DriverSession(
        login = login,
        password = password,
        driverName = login,
        registration = "REGISTRATION",
        availableRegistrations = listOf("REGISTRATION"),
        changePasswordRequired = true,
    ).also { session.value = it }

    override suspend fun logout() {
        session.value = null
    }

    override suspend fun changePassword(login: String, password: String) {
        val current = session.value ?: return
        session.value = current.copy(password = password, changePasswordRequired = false)
    }

    override suspend fun selectRegistration(registration: String) {
        val current = session.value ?: return
        if (current.availableRegistrations.any { it.equals(registration.trim(), ignoreCase = true) }) {
            session.value = current.copy(registration = registration.trim().uppercase())
        }
    }

    override suspend fun saveMileage(login: String, registration: String, mileage: Int) = Unit
    override suspend fun flushPendingMileageSync(): DriverMileageSyncState = DriverMileageSyncState(
        registration = registrationFallback(session.value?.registration),
        status = "Stub sync completed",
    )

    override suspend fun saveRemoteEndpointSettings(settings: DriverRemoteEndpointSettings) {
        remoteEndpointSettings.value = settings
    }

    override suspend fun validateRemoteEndpointSettings(settings: DriverRemoteEndpointSettings): String =
        settings.apiUrl.trim().ifBlank { "Adres API jest wymagany" }

    override suspend fun saveVehicleReportDraft(draft: VehicleReportDraft) = Unit

    override suspend fun exportVehicleReportPdf(draft: VehicleReportDraft): String = "/tmp/vehicle_report.pdf"

    private fun registrationFallback(value: String?): String = value.orEmpty().ifBlank { "REGISTRATION" }
}
