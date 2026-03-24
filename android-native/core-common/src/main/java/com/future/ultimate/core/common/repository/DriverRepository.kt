package com.future.ultimate.core.common.repository

import com.future.ultimate.core.common.model.VehicleReportDraft
import kotlinx.coroutines.flow.Flow

data class DriverSession(
    val login: String = "",
    val password: String = "",
    val driverName: String = "",
    val registration: String = "",
    val availableRegistrations: List<String> = emptyList(),
    val changePasswordRequired: Boolean = false,
)

data class DriverMileageSyncState(
    val registration: String = "",
    val pendingCount: Int = 0,
    val queuedMileage: Int? = null,
    val lastAttemptAt: String = "",
    val lastSyncedAt: String = "",
    val status: String = "Brak danych",
    val error: String = "",
)

data class DriverRemoteEndpointSettings(
    val apiUrl: String = "",
)

interface DriverRepository {
    fun observeSession(): Flow<DriverSession?>
    fun observeMileageSyncState(): Flow<DriverMileageSyncState>
    fun observeRemoteEndpointSettings(): Flow<DriverRemoteEndpointSettings>
    suspend fun login(login: String, password: String): DriverSession
    suspend fun logout()
    suspend fun changePassword(login: String, password: String)
    suspend fun selectRegistration(registration: String)
    suspend fun saveMileage(login: String, registration: String, mileage: Int)
    suspend fun flushPendingMileageSync(): DriverMileageSyncState
    suspend fun saveRemoteEndpointSettings(settings: DriverRemoteEndpointSettings)
    suspend fun validateRemoteEndpointSettings(settings: DriverRemoteEndpointSettings): String
    suspend fun saveVehicleReportDraft(draft: VehicleReportDraft)
    suspend fun exportVehicleReportPdf(draft: VehicleReportDraft): String
}
