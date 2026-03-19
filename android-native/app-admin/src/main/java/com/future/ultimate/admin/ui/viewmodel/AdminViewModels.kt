package com.future.ultimate.admin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.future.ultimate.core.common.model.CarDraft
import com.future.ultimate.core.common.model.ClothesOrderDraft
import com.future.ultimate.core.common.model.ClothesOrderItemDraft
import com.future.ultimate.core.common.model.ClothesSizeDraft
import com.future.ultimate.core.common.model.ContactDraft
import com.future.ultimate.core.common.model.PlantDraft
import com.future.ultimate.core.common.model.VehicleReportDraft
import com.future.ultimate.core.common.model.WorkerDraft
import com.future.ultimate.core.common.repository.AdminRepository
import com.future.ultimate.core.common.repository.EmailTemplateData
import com.future.ultimate.core.common.repository.SmtpSettingsData
import com.future.ultimate.core.common.ui.CarsUiState
import com.future.ultimate.core.common.ui.ClothesOrdersUiState
import com.future.ultimate.core.common.ui.ClothesReportsUiState
import com.future.ultimate.core.common.ui.ClothesSizesUiState
import com.future.ultimate.core.common.ui.ContactsUiState
import com.future.ultimate.core.common.ui.PayrollUiState
import com.future.ultimate.core.common.ui.PlantsUiState
import com.future.ultimate.core.common.ui.ReportsUiState
import com.future.ultimate.core.common.ui.SettingsUiState
import com.future.ultimate.core.common.ui.SmtpUiState
import com.future.ultimate.core.common.ui.TemplateUiState
import com.future.ultimate.core.common.ui.VehicleReportUiState
import com.future.ultimate.core.common.ui.WorkersUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.LocalDate

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
    fun exportPdf() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isSaving = true, exportMessage = null)
        runCatching {
            repository.saveVehicleReportDraft(_uiState.value.draft)
            repository.exportVehicleReportPdf(_uiState.value.draft)
        }.onSuccess { path ->
            _uiState.value = _uiState.value.copy(isSaving = false, exportMessage = "PDF zapisany: $path")
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                exportMessage = error.message ?: "Nie udało się wygenerować PDF",
            )
        }
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

class ClothesSizesViewModel(private val repository: AdminRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ClothesSizesUiState())
    val uiState: StateFlow<ClothesSizesUiState> = _uiState.asStateFlow()

    init { repository.observeClothesSizes().onEach { _uiState.value = _uiState.value.copy(items = it) }.launchIn(viewModelScope) }

    fun updateQuery(value: String) { _uiState.value = _uiState.value.copy(query = value) }
    fun updateEditor(draft: ClothesSizeDraft) { _uiState.value = _uiState.value.copy(editor = draft) }

    fun save() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isSaving = true)
        repository.saveClothesSize(_uiState.value.editor)
        _uiState.value = _uiState.value.copy(isSaving = false, editor = ClothesSizeDraft())
    }

    fun delete(id: Long) = viewModelScope.launch {
        repository.deleteClothesSize(id)
    }
}


class ClothesOrdersViewModel(private val repository: AdminRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ClothesOrdersUiState())
    val uiState: StateFlow<ClothesOrdersUiState> = _uiState.asStateFlow()
    private var orderItemsJob: Job? = null

    init { repository.observeClothesOrders().onEach { _uiState.value = _uiState.value.copy(items = it) }.launchIn(viewModelScope) }

    fun updateEditor(draft: ClothesOrderDraft) { _uiState.value = _uiState.value.copy(editor = draft, actionMessage = null) }
    fun updateItemEditor(draft: ClothesOrderItemDraft) { _uiState.value = _uiState.value.copy(itemEditor = draft, actionMessage = null) }

    fun save() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isSaving = true)
        repository.saveClothesOrder(_uiState.value.editor)
        _uiState.value = _uiState.value.copy(isSaving = false, editor = ClothesOrderDraft(), actionMessage = "Zamówienie zapisane")
    }

    fun toggleOrderSelection(orderId: Long) {
        val current = _uiState.value.selectedOrderId
        if (current == orderId) {
            orderItemsJob?.cancel()
            _uiState.value = _uiState.value.copy(
                selectedOrderId = null,
                selectedOrderItems = emptyList(),
                itemEditor = ClothesOrderItemDraft(),
            )
            return
        }
        orderItemsJob?.cancel()
        _uiState.value = _uiState.value.copy(
            selectedOrderId = orderId,
            selectedOrderItems = emptyList(),
            itemEditor = ClothesOrderItemDraft(),
        )
        orderItemsJob = repository.observeClothesOrderItems(orderId).onEach { items ->
            _uiState.value = _uiState.value.copy(selectedOrderItems = items)
        }.launchIn(viewModelScope)
    }

    fun saveItem() = viewModelScope.launch {
        val selectedOrderId = _uiState.value.selectedOrderId ?: return@launch
        val draft = _uiState.value.itemEditor
        if (draft.name.isBlank() || draft.surname.isBlank() || draft.item.isBlank()) return@launch
        _uiState.value = _uiState.value.copy(isSavingItem = true, actionMessage = null)
        repository.saveClothesOrderItem(selectedOrderId, draft)
        _uiState.value = _uiState.value.copy(isSavingItem = false, itemEditor = ClothesOrderItemDraft(), actionMessage = "Pozycja dodana")
    }

    fun deleteItem(id: Long) = viewModelScope.launch {
        repository.deleteClothesOrderItem(id)
        _uiState.value = _uiState.value.copy(actionMessage = "Pozycja usunięta")
    }

    fun markOrdered(orderId: Long) = viewModelScope.launch {
        repository.markClothesOrderOrdered(orderId)
        _uiState.value = _uiState.value.copy(actionMessage = "Status zmieniony na Zamówione")
    }

    fun issueItem(id: Long) = viewModelScope.launch {
        repository.issueClothesOrderItem(id)
        _uiState.value = _uiState.value.copy(actionMessage = "Pozycja wydana")
    }

    fun issueAll(orderId: Long) = viewModelScope.launch {
        repository.issueAllClothesOrderItems(orderId)
        _uiState.value = _uiState.value.copy(actionMessage = "Wydano wszystkie dostępne pozycje")
    }

    fun exportOrderCsv(orderId: Long) = viewModelScope.launch {
        val path = repository.exportClothesOrderCsv(orderId)
        _uiState.value = _uiState.value.copy(actionMessage = if (path.isBlank()) "Nie znaleziono zamówienia" else "CSV zamówienia: $path")
    }

    fun exportOrderXlsx(orderId: Long) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isExportingXlsx = true, actionMessage = null)
        val export = repository.exportClothesOrderXlsx(orderId)
        _uiState.value = _uiState.value.copy(
            isExportingXlsx = false,
            actionMessage = if (export.supplierPath.isBlank() || export.issuePath.isBlank()) {
                "Nie znaleziono pozycji do eksportu XLSX"
            } else {
                """XLSX zapisane:
1) Hurtownia: ${export.supplierPath}
2) Wydanie: ${export.issuePath}"""
            },
        )
    }
}

class ClothesReportsViewModel(private val repository: AdminRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ClothesReportsUiState(year = LocalDate.now().year.toString()))
    val uiState: StateFlow<ClothesReportsUiState> = _uiState.asStateFlow()

    init {
        repository.observeClothesHistory().onEach { history ->
            _uiState.value = _uiState.value.copy(
                history = history,
                yearlySummary = buildYearlySummary(history, _uiState.value.year),
            )
        }.launchIn(viewModelScope)
    }

    fun updateYear(value: String) {
        _uiState.value = _uiState.value.copy(
            year = value,
            yearlySummary = buildYearlySummary(_uiState.value.history, value),
        )
    }

    fun exportCsv() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isExporting = true, exportMessage = null)
        val path = repository.exportClothesHistoryCsv()
        _uiState.value = _uiState.value.copy(isExporting = false, exportMessage = "CSV zapisane: $path")
    }

    private fun buildYearlySummary(history: List<com.future.ultimate.core.common.repository.ClothesHistoryListItem>, year: String): List<String> {
        val normalizedYear = year.trim()
        if (normalizedYear.isBlank()) return emptyList()
        return history
            .filter { it.date.startsWith(normalizedYear) }
            .groupBy { Triple(it.workerId, it.name.trim(), it.surname.trim()) }
            .map { (worker, entries) ->
                val workerLabel = "${worker.second} ${worker.third}".trim().ifBlank { "Pracownik #${worker.first}" }
                val lastIssueDate = entries.maxOfOrNull { it.date }.orEmpty()
                "$workerLabel • ${entries.size} wydań • ostatnie: $lastIssueDate"
            }
            .sortedByDescending { summary ->
                summary.substringAfter("• ").substringBefore(" wydań").toIntOrNull() ?: 0
            }
    }
}

class SmtpViewModel(private val repository: AdminRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SmtpUiState())
    val uiState: StateFlow<SmtpUiState> = _uiState.asStateFlow()

    init {
        repository.observeSmtpSettings().onEach { settings ->
            _uiState.value = _uiState.value.copy(settings = settings)
        }.launchIn(viewModelScope)
    }

    fun updateSettings(update: SmtpSettingsData) {
        _uiState.value = _uiState.value.copy(settings = update, message = null)
    }

    fun save() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isSaving = true, message = null)
        repository.saveSmtpSettings(_uiState.value.settings)
        _uiState.value = _uiState.value.copy(isSaving = false, message = "Ustawienia SMTP zapisane")
    }

    fun validate() {
        val current = _uiState.value.settings
        val message = when {
            current.host.isBlank() || current.user.isBlank() || current.password.isBlank() -> "Uzupełnij host, login i hasło"
            current.port.toIntOrNull() == null -> "Port SMTP musi być liczbą"
            else -> "Konfiguracja wygląda poprawnie. Realny test połączenia wróci w etapie SMTP pipeline."
        }
        _uiState.value = _uiState.value.copy(message = message)
    }
}

class TemplateViewModel(private val repository: AdminRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(
        TemplateUiState(
            previewDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
        ),
    )
    val uiState: StateFlow<TemplateUiState> = _uiState.asStateFlow()

    init {
        repository.observeEmailTemplate().onEach { template ->
            _uiState.value = _uiState.value.copy(
                template = template,
                subjectPreview = renderPreview(template.subject, _uiState.value.previewName, _uiState.value.previewDate),
                bodyPreview = renderPreview(template.body, _uiState.value.previewName, _uiState.value.previewDate),
            )
        }.launchIn(viewModelScope)
    }

    fun updateTemplate(update: EmailTemplateData) {
        _uiState.value = _uiState.value.copy(
            template = update,
            subjectPreview = renderPreview(update.subject, _uiState.value.previewName, _uiState.value.previewDate),
            bodyPreview = renderPreview(update.body, _uiState.value.previewName, _uiState.value.previewDate),
            message = null,
        )
    }

    fun updatePreviewName(value: String) {
        _uiState.value = _uiState.value.copy(
            previewName = value,
            subjectPreview = renderPreview(_uiState.value.template.subject, value, _uiState.value.previewDate),
            bodyPreview = renderPreview(_uiState.value.template.body, value, _uiState.value.previewDate),
        )
    }

    fun updatePreviewDate(value: String) {
        _uiState.value = _uiState.value.copy(
            previewDate = value,
            subjectPreview = renderPreview(_uiState.value.template.subject, _uiState.value.previewName, value),
            bodyPreview = renderPreview(_uiState.value.template.body, _uiState.value.previewName, value),
        )
    }

    fun save() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isSaving = true, message = null)
        repository.saveEmailTemplate(_uiState.value.template)
        _uiState.value = _uiState.value.copy(isSaving = false, message = "Szablon email zapisany")
    }

    private fun renderPreview(template: String, name: String, date: String): String =
        template
            .replace("{Imię}", name.ifBlank { "Jan" })
            .replace("{Data}", date.ifBlank { LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) })
}

class ReportsViewModel(private val repository: AdminRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        repository.observeSessionReports().onEach { items ->
            _uiState.value = _uiState.value.copy(items = items)
        }.launchIn(viewModelScope)
    }

    fun exportCsv() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isExporting = true, exportMessage = null)
        val path = repository.exportSessionReportsCsv()
        _uiState.value = _uiState.value.copy(isExporting = false, exportMessage = "CSV raportów zapisane: $path")
    }
}

class SettingsViewModel(private val repository: AdminRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        repository.observeDashboardStats().onEach { stats ->
            _uiState.value = _uiState.value.copy(stats = stats)
        }.launchIn(viewModelScope)
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
        modelClass.isAssignableFrom(ClothesSizesViewModel::class.java) -> ClothesSizesViewModel(repository) as T
        modelClass.isAssignableFrom(ClothesOrdersViewModel::class.java) -> ClothesOrdersViewModel(repository) as T
        modelClass.isAssignableFrom(ClothesReportsViewModel::class.java) -> ClothesReportsViewModel(repository) as T
        modelClass.isAssignableFrom(SmtpViewModel::class.java) -> SmtpViewModel(repository) as T
        modelClass.isAssignableFrom(TemplateViewModel::class.java) -> TemplateViewModel(repository) as T
        modelClass.isAssignableFrom(ReportsViewModel::class.java) -> ReportsViewModel(repository) as T
        modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(repository) as T
        else -> error("Unsupported modelClass: ${modelClass.name}")
    }
}
