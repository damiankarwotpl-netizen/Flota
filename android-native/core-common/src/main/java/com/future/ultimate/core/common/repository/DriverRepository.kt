package com.future.ultimate.core.common.repository

import com.future.ultimate.core.common.model.VehicleReportDraft

data class DriverSession(
    val login: String = "",
    val password: String = "",
    val driverName: String = "",
    val registration: String = "",
)

interface DriverRepository {
    suspend fun login(login: String, password: String): DriverSession
    suspend fun changePassword(login: String, password: String)
    suspend fun saveMileage(login: String, registration: String, mileage: Int)
    suspend fun saveVehicleReportDraft(draft: VehicleReportDraft)
}
