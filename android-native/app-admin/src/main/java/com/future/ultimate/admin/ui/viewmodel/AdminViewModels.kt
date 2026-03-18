package com.future.ultimate.admin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.future.ultimate.core.common.model.CarDraft
import com.future.ultimate.core.common.model.ContactDraft
import com.future.ultimate.core.common.model.PlantDraft
import com.future.ultimate.core.common.model.VehicleReportDraft
import com.future.ultimate.core.common.model.WorkerDraft
import com.future.ultimate.core.common.repository.AdminRepository
import com.future.ultimate.core.common.ui.CarsUiState
import com.future.ultimate.core.common.ui.ContactsUiState
import com.future.ultimate.core.common.ui.PayrollUiState
import com.future.ultimate.core.common.ui.PlantsUiState
import com.future.ultimate.core.common.ui.VehicleReportUiState
import com.future.ultimate.core.common.ui.WorkersUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ContactsViewModel(private val repository: AdminRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    init { repository.observeContacts().onEach { _uiState.value = _uiState.value.copy(items = it) }.launchIn(viewModelScope) }
    fun updateQuery(value: String) { _uiState.value = _uiState.value.copy(query = value) }
    fun updateEditor(draft: ContactDraft) { _uiState.value = _uiState.value.copy(editor = draft) }
    fun save() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isSaving = true)
        repository.saveContact(_uiState.value.editor)
        _uiState.value = _uiState.value.copy(isSaving = false, editor = ContactDraft())
    }
}

class CarsViewModel(private val repository: AdminRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CarsUiState())
    val uiState: StateFlow<CarsUiState> = _uiState.asStateFlow()

    init {
        repository.observeCars().onEach { items ->
            val currentState = _uiState.value
            _uiState.value = currentState.copy(
                items = items,
                mileageDrafts = items.associate { car -> car.id to (currentState.mileageDrafts[car.id] ?: car.mileage.toString()) },
                driverDrafts = items.associate { car -> car.id to (currentState.driverDrafts[car.id] ?: car.driver) },
            )
        }.launchIn(viewModelScope)
    }

    fun updateQuery(value: String) { _uiState.value = _uiState.value.copy(query = value) }
    fun updateEditor(draft: CarDraft) { _uiState.value = _uiState.value.copy(editor = draft) }

    fun updateMileageDraft(id: Long, value: String) {
        _uiState.value = _uiState.value.copy(mileageDrafts = _uiState.value.mileageDrafts + (id to value))
    }

    fun updateDriverDraft(id: Long, value: String) {
        _uiState.value = _uiState.value.copy(driverDrafts = _uiState.value.driverDrafts + (id to value))
    }

    fun save() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isSaving = true)
        repository.saveCar(_uiState.value.editor)
        _uiState.value = _uiState.value.copy(isSaving = false, editor = CarDraft())
    }

    fun saveMileage(id: Long) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(actionInFlightId = id)
        val mileage = _uiState.value.mileageDrafts[id]?.toIntOrNull() ?: 0
        repository.updateCarMileage(id, mileage)
        _uiState.value = _uiState.value.copy(actionInFlightId = null)
    }

    fun saveDriver(id: Long) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(actionInFlightId = id)
        repository.updateCarDriver(id, _uiState.value.driverDrafts[id].orEmpty())
        _uiState.value = _uiState.value.copy(actionInFlightId = null)
    }

    fun confirmService(id: Long) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(actionInFlightId = id)
        repository.confirmCarService(id)
        _uiState.value = _uiState.value.copy(actionInFlightId = null)
    }

    fun deleteCar(id: Long) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(actionInFlightId = id)
        repository.deleteCar(id)
        _uiState.value = _uiState.value.copy(actionInFlightId = null)
    }
}

class VehicleReportViewModel(private val repository: AdminRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(VehicleReportUiState())
    val uiState: StateFlow<VehicleReportUiState> = _uiState.asStateFlow()
    fun updateDraft(draft: VehicleReportDraft) { _uiState.value = _uiState.value.copy(draft = draft) }
    fun saveDraft() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isSaving = true)
        repository.saveVehicleReportDraft(_uiState.value.draft)
        _uiState.value = _uiState.value.copy(isSaving = false, exportMessage = "Szkic raportu zapisany")
    }
}

class PayrollViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PayrollUiState())
    val uiState: StateFlow<PayrollUiState> = _uiState.asStateFlow()
    fun toggleAutoSend() { _uiState.value = _uiState.value.copy(autoSend = !_uiState.value.autoSend) }
}

class WorkersViewModel(private val repository: AdminRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkersUiState())
    val uiState: StateFlow<WorkersUiState> = _uiState.asStateFlow()

    init { repository.observeWorkers().onEach { _uiState.value = _uiState.value.copy(items = it) }.launchIn(viewModelScope) }
    fun updateQuery(value: String) { _uiState.value = _uiState.value.copy(query = value) }
    fun updateEditor(draft: WorkerDraft) { _uiState.value = _uiState.value.copy(editor = draft) }
    fun save() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isSaving = true)
        repository.saveWorker(_uiState.value.editor)
        _uiState.value = _uiState.value.copy(isSaving = false, editor = WorkerDraft())
    }
}

class PlantsViewModel(private val repository: AdminRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(PlantsUiState())
    val uiState: StateFlow<PlantsUiState> = _uiState.asStateFlow()

    init { repository.observePlants().onEach { _uiState.value = _uiState.value.copy(items = it) }.launchIn(viewModelScope) }
    fun updateQuery(value: String) { _uiState.value = _uiState.value.copy(query = value) }
    fun updateEditor(draft: PlantDraft) { _uiState.value = _uiState.value.copy(editor = draft) }
    fun save() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isSaving = true)
        repository.savePlant(_uiState.value.editor)
        _uiState.value = _uiState.value.copy(isSaving = false, editor = PlantDraft())
    }
}

class AdminViewModelFactory(private val repository: AdminRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(ContactsViewModel::class.java) -> ContactsViewModel(repository) as T
        modelClass.isAssignableFrom(CarsViewModel::class.java) -> CarsViewModel(repository) as T
        modelClass.isAssignableFrom(VehicleReportViewModel::class.java) -> VehicleReportViewModel(repository) as T
        modelClass.isAssignableFrom(PayrollViewModel::class.java) -> PayrollViewModel() as T
        modelClass.isAssignableFrom(WorkersViewModel::class.java) -> WorkersViewModel(repository) as T
        modelClass.isAssignableFrom(PlantsViewModel::class.java) -> PlantsViewModel(repository) as T
        else -> error("Unsupported modelClass: ${modelClass.name}")
    }
}
