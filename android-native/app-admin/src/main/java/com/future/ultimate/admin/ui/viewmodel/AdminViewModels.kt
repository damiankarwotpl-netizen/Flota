package com.future.ultimate.admin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.future.ultimate.core.common.model.CarDraft
import com.future.ultimate.core.common.model.ContactDraft
import com.future.ultimate.core.common.model.VehicleReportDraft
import com.future.ultimate.core.common.repository.AdminRepository
import com.future.ultimate.core.common.ui.CarsUiState
import com.future.ultimate.core.common.ui.ContactsUiState
import com.future.ultimate.core.common.ui.PayrollUiState
import com.future.ultimate.core.common.ui.VehicleReportUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val repository: AdminRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    init {
        repository.observeContacts().onEach { items ->
            _uiState.value = _uiState.value.copy(items = items)
        }.launchIn(viewModelScope)
    }

    fun updateQuery(value: String) {
        _uiState.value = _uiState.value.copy(query = value)
    }

    fun updateEditor(draft: ContactDraft) {
        _uiState.value = _uiState.value.copy(editor = draft)
    }

    fun save() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            repository.saveContact(_uiState.value.editor)
            _uiState.value = _uiState.value.copy(isSaving = false, editor = ContactDraft())
        }
    }
}

class CarsViewModel(
    private val repository: AdminRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CarsUiState())
    val uiState: StateFlow<CarsUiState> = _uiState.asStateFlow()

    init {
        repository.observeCars().onEach { items ->
            _uiState.value = _uiState.value.copy(items = items)
        }.launchIn(viewModelScope)
    }

    fun updateQuery(value: String) {
        _uiState.value = _uiState.value.copy(query = value)
    }

    fun updateEditor(draft: CarDraft) {
        _uiState.value = _uiState.value.copy(editor = draft)
    }

    fun save() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            repository.saveCar(_uiState.value.editor)
            _uiState.value = _uiState.value.copy(isSaving = false, editor = CarDraft())
        }
    }
}

class VehicleReportViewModel(
    private val repository: AdminRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(VehicleReportUiState())
    val uiState: StateFlow<VehicleReportUiState> = _uiState.asStateFlow()

    fun updateDraft(draft: VehicleReportDraft) {
        _uiState.value = _uiState.value.copy(draft = draft)
    }

    fun saveDraft() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            repository.saveVehicleReportDraft(_uiState.value.draft)
            _uiState.value = _uiState.value.copy(isSaving = false, exportMessage = "Szkic raportu zapisany")
        }
    }
}

class PayrollViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PayrollUiState())
    val uiState: StateFlow<PayrollUiState> = _uiState.asStateFlow()

    fun toggleAutoSend() {
        _uiState.value = _uiState.value.copy(autoSend = !_uiState.value.autoSend)
    }
}

class AdminViewModelFactory(
    private val repository: AdminRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(ContactsViewModel::class.java) -> ContactsViewModel(repository) as T
        modelClass.isAssignableFrom(CarsViewModel::class.java) -> CarsViewModel(repository) as T
        modelClass.isAssignableFrom(VehicleReportViewModel::class.java) -> VehicleReportViewModel(repository) as T
        modelClass.isAssignableFrom(PayrollViewModel::class.java) -> PayrollViewModel() as T
        else -> error("Unsupported modelClass: ${modelClass.name}")
    }
}
