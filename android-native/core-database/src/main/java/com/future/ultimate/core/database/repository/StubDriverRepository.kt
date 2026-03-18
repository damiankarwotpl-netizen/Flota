package com.future.ultimate.core.database.repository

import com.future.ultimate.core.common.model.VehicleReportDraft
import com.future.ultimate.core.common.repository.DriverRepository
import com.future.ultimate.core.common.repository.DriverSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class StubDriverRepository : DriverRepository {
    private val session = MutableStateFlow<DriverSession?>(null)

    override fun observeSession(): Flow<DriverSession?> = session.asStateFlow()

    override suspend fun login(login: String, password: String): DriverSession = DriverSession(
        login = login,
        password = password,
        driverName = login,
        registration = "REGISTRATION",
        changePasswordRequired = true,
    ).also { session.value = it }

    override suspend fun changePassword(login: String, password: String) {
        val current = session.value ?: return
        session.value = current.copy(password = password, changePasswordRequired = false)
    }

    override suspend fun saveMileage(login: String, registration: String, mileage: Int) = Unit

    override suspend fun saveVehicleReportDraft(draft: VehicleReportDraft) = Unit
}
