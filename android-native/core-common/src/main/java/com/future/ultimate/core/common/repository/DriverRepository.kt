package com.future.ultimate.core.common.repository

import com.future.ultimate.core.common.model.VehicleReportDraft
import kotlinx.coroutines.flow.Flow

data class DriverSession(
    val login: String = "",
    val password: String = "",
    val driverName: String = "",
    val registration: String = "",
    val changePasswordRequired: Boolean = false,
)

interface DriverRepository {
    fun observeSession(): Flow<DriverSession?>
    suspend fun login(login: String, password: String): DriverSession
    suspend fun logout()
    suspend fun changePassword(login: String, password: String)
    suspend fun saveMileage(login: String, registration: String, mileage: Int)
    suspend fun saveVehicleReportDraft(draft: VehicleReportDraft)
    suspend fun exportVehicleReportPdf(draft: VehicleReportDraft): String
}
