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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DriverLoginViewModel(
    private val repository: DriverRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DriverLoginUiState())
    val uiState: StateFlow<DriverLoginUiState> = _uiState.asStateFlow()

    fun updateLogin(value: String) { _uiState.value = _uiState.value.copy(login = value) }
    fun updatePassword(value: String) { _uiState.value = _uiState.value.copy(password = value) }

    fun login(onSuccess: (requiresPasswordChange: Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val session = repository.login(_uiState.value.login, _uiState.value.password)
                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess(session.changePasswordRequired)
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message ?: "Nie udało się zalogować",
                )
            }
        }
    }
}

class DriverMileageViewModel(
    private val repository: DriverRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DriverMileageUiState())
    val uiState: StateFlow<DriverMileageUiState> = _uiState.asStateFlow()

    init {
        repository.observeSession().onEach { session ->
            if (session != null) {
                _uiState.value = _uiState.value.copy(
                    registration = session.registration,
                    driverName = session.driverName,
                )
            }
        }.launchIn(viewModelScope)
        repository.observeMileageSyncState().onEach { syncState ->
            _uiState.value = _uiState.value.copy(
                pendingSyncCount = syncState.pendingCount,
                queuedMileage = syncState.queuedMileage?.toString().orEmpty(),
                lastAttemptAt = syncState.lastAttemptAt,
                lastSyncedAt = syncState.lastSyncedAt,
                syncStatus = syncState.status,
                syncError = syncState.error,
            )
        }.launchIn(viewModelScope)
    }

    fun setRegistration(value: String) { _uiState.value = _uiState.value.copy(registration = value) }
    fun updateMileage(value: String) { _uiState.value = _uiState.value.copy(mileage = value) }

    fun save() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, status = null)
            try {
                repository.saveMileage(
                    login = "",
                    registration = _uiState.value.registration,
                    mileage = _uiState.value.mileage.toIntOrNull() ?: 0,
                )
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    mileage = "",
                    status = "Przebieg zapisany lokalnie i przekazany do kolejki synchronizacji",
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    status = error.message ?: "Nie udało się zapisać przebiegu",
                )
            }
        }
    }

    fun flushSyncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, status = "Trwa ręczna synchronizacja przebiegu...")
            try {
                val state = repository.flushPendingMileageSync()
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    status = if (state.pendingCount > 0) {
                        "Kolejka nadal oczekuje: ${state.status}"
                    } else {
                        "Synchronizacja zakończona: ${state.status}"
                    },
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    status = error.message ?: "Ręczna synchronizacja nie powiodła się",
                )
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = DriverMileageUiState()
            onSuccess()
        }
    }
}

class DriverVehicleReportViewModel(
    private val repository: DriverRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DriverVehicleReportUiState())
    val uiState: StateFlow<DriverVehicleReportUiState> = _uiState.asStateFlow()

    init {
        repository.observeSession().onEach { session ->
            if (session != null) {
                _uiState.value = _uiState.value.copy(
                    driverName = session.driverName,
                    draft = _uiState.value.draft.copy(rej = session.registration),
                )
            }
        }.launchIn(viewModelScope)
    }

    fun updateDraft(draft: VehicleReportDraft) {
        _uiState.value = _uiState.value.copy(draft = draft, message = null)
    }

    fun save() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, message = null)
            try {
                repository.saveVehicleReportDraft(_uiState.value.draft)
                val path = repository.exportVehicleReportPdf(_uiState.value.draft)
                _uiState.value = _uiState.value.copy(isSaving = false, message = "PDF zapisany: $path")
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    message = error.message ?: "Nie udało się zapisać raportu",
                )
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = DriverVehicleReportUiState()
            onSuccess()
        }
    }
}

class DriverChangePasswordViewModel(
    private val repository: DriverRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DriverLoginUiState())
    val uiState: StateFlow<DriverLoginUiState> = _uiState.asStateFlow()

    init {
        repository.observeSession().onEach { session ->
            if (session != null) {
                _uiState.value = _uiState.value.copy(login = session.login)
            }
        }.launchIn(viewModelScope)
    }

    fun updatePassword(value: String) { _uiState.value = _uiState.value.copy(password = value) }

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                repository.changePassword(_uiState.value.login, _uiState.value.password)
                _uiState.value = _uiState.value.copy(isLoading = false, password = "")
                onSuccess()
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message ?: "Nie udało się zmienić hasła",
                )
            }
        }
    }
}

class DriverViewModelFactory(
    private val repository: DriverRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(DriverLoginViewModel::class.java) -> DriverLoginViewModel(repository) as T
        modelClass.isAssignableFrom(DriverMileageViewModel::class.java) -> DriverMileageViewModel(repository) as T
        modelClass.isAssignableFrom(DriverVehicleReportViewModel::class.java) -> DriverVehicleReportViewModel(repository) as T
        modelClass.isAssignableFrom(DriverChangePasswordViewModel::class.java) -> DriverChangePasswordViewModel(repository) as T
        else -> error("Unsupported modelClass: ${modelClass.name}")
    }
}
