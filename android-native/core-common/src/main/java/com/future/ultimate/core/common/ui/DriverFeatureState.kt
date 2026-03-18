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
    val mileage: String = "",
    val status: String? = null,
    val isSaving: Boolean = false,
)

data class DriverVehicleReportUiState(
    val draft: VehicleReportDraft = VehicleReportDraft(),
    val isSaving: Boolean = false,
    val message: String? = null,
)
