package com.future.ultimate.core.database.repository

import com.future.ultimate.core.common.model.VehicleReportDraft
import com.future.ultimate.core.common.repository.DriverRepository
import com.future.ultimate.core.common.repository.DriverSession

class StubDriverRepository : DriverRepository {
    override suspend fun login(login: String, password: String): DriverSession = DriverSession(
        login = login,
        password = password,
        driverName = login,
        registration = "REGISTRATION",
    )

    override suspend fun changePassword(login: String, password: String) = Unit

    override suspend fun saveMileage(login: String, registration: String, mileage: Int) = Unit

    override suspend fun saveVehicleReportDraft(draft: VehicleReportDraft) = Unit
}
