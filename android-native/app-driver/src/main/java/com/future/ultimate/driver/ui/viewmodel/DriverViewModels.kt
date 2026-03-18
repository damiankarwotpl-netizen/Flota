package com.future.ultimate.driver.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.future.ultimate.core.common.model.VehicleReportDraft
import com.future.ultimate.core.common.repository.DriverRepository
import com.future.ultimate.core.common.ui.DriverLoginUiState
import com.future.ultimate.core.common.ui.DriverMileageUiState
import com.future.ultimate.core.common.ui.DriverVehicleReportUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DriverLoginViewModel(
    private val repository: DriverRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DriverLoginUiState())
    val uiState: StateFlow<DriverLoginUiState> = _uiState.asStateFlow()

    fun updateLogin(value: String) { _uiState.value = _uiState.value.copy(login = value) }
    fun updatePassword(value: String) { _uiState.value = _uiState.value.copy(password = value) }

    fun login(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.login(_uiState.value.login, _uiState.value.password)
            _uiState.value = _uiState.value.copy(isLoading = false)
            onSuccess()
        }
    }
}

class DriverMileageViewModel(
    private val repository: DriverRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DriverMileageUiState())
    val uiState: StateFlow<DriverMileageUiState> = _uiState.asStateFlow()

    fun setRegistration(value: String) { _uiState.value = _uiState.value.copy(registration = value) }
    fun updateMileage(value: String) { _uiState.value = _uiState.value.copy(mileage = value) }

    fun save(login: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            repository.saveMileage(login, _uiState.value.registration, _uiState.value.mileage.toIntOrNull() ?: 0)
            _uiState.value = _uiState.value.copy(isSaving = false, status = "Zapisano")
        }
    }
}

class DriverVehicleReportViewModel(
    private val repository: DriverRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DriverVehicleReportUiState())
    val uiState: StateFlow<DriverVehicleReportUiState> = _uiState.asStateFlow()

    fun updateDraft(draft: VehicleReportDraft) {
        _uiState.value = _uiState.value.copy(draft = draft)
    }

    fun save() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            repository.saveVehicleReportDraft(_uiState.value.draft)
            _uiState.value = _uiState.value.copy(isSaving = false, message = "Szkic raportu zapisany")
        }
    }
}

class DriverViewModelFactory(
    private val repository: DriverRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(DriverLoginViewModel::class.java) -> DriverLoginViewModel(repository) as T
        modelClass.isAssignableFrom(DriverMileageViewModel::class.java) -> DriverMileageViewModel(repository) as T
        modelClass.isAssignableFrom(DriverVehicleReportViewModel::class.java) -> DriverVehicleReportViewModel(repository) as T
        else -> error("Unsupported modelClass: ${modelClass.name}")
    }
}
