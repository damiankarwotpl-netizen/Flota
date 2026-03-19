package com.future.ultimate.core.common.ui

import com.future.ultimate.core.common.model.VehicleReportDraft

data class DriverLoginUiState(
    val login: String = "",
    val password: String = "",
    val error: String? = null,
    val isLoading: Boolean = false,
)

data class DriverMileageUiState(
    val registration: String = "",
    val driverName: String = "",
    val mileage: String = "",
    val status: String? = null,
    val isSaving: Boolean = false,
    val syncStatus: String = "Brak danych o synchronizacji",
    val pendingSyncCount: Int = 0,
    val queuedMileage: String = "",
    val lastAttemptAt: String = "",
    val lastSyncedAt: String = "",
    val syncError: String = "",
)

data class DriverVehicleReportUiState(
    val draft: VehicleReportDraft = VehicleReportDraft(),
    val driverName: String = "",
    val isSaving: Boolean = false,
    val message: String? = null,
)
