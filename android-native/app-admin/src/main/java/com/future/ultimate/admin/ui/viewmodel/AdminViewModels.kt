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
import com.future.ultimate.core.common.repository.ClothesOrderItemListItem
import com.future.ultimate.core.common.repository.ClothesOrderListItem
import com.future.ultimate.core.common.repository.ClothesSizeListItem
import com.future.ultimate.core.common.repository.EmailTemplateData
import com.future.ultimate.core.common.repository.SmtpSettingsData
import com.future.ultimate.core.common.ui.CarsUiState
import com.future.ultimate.core.common.ui.ClothesOrdersUiState
import com.future.ultimate.core.common.ui.ClothesReportsUiState
import com.future.ultimate.core.common.ui.ClothesSizesUiState
import com.future.ultimate.core.common.ui.ContactsUiState
import com.future.ultimate.core.common.ui.CarsServiceFilter
import com.future.ultimate.core.common.ui.PayrollUiState
import com.future.ultimate.core.common.ui.PlantsUiState
import com.future.ultimate.core.common.ui.ReportsUiState
import com.future.ultimate.core.common.ui.SettingsUiState
import com.future.ultimate.core.common.ui.SmtpUiState
import com.future.ultimate.core.common.ui.TableUiState
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
    private var workerDriverSuggestions: List<String> = emptyList()
    private var contactDriverSuggestions: List<String> = emptyList()

    init {
        repository.observeCars().onEach { items ->
            val currentState = _uiState.value
            _uiState.value = currentState.copy(
                items = items,
                mileageDrafts = items.associate { car -> car.id to (currentState.mileageDrafts[car.id] ?: car.mileage.toString()) },
                driverDrafts = items.associate { car -> car.id to (currentState.driverDrafts[car.id] ?: car.driver) },
            )
        }.launchIn(viewModelScope)
        repository.observeWorkers().onEach { workers ->
            workerDriverSuggestions = workers.map { "${it.name} ${it.surname}".trim() }.filter { it.isNotBlank() }
            refreshDriverSuggestions()
        }.launchIn(viewModelScope)
        repository.observeContacts().onEach { contacts ->
            contactDriverSuggestions = contacts.map { "${it.name} ${it.surname}".trim() }.filter { it.isNotBlank() }
            refreshDriverSuggestions()
        }.launchIn(viewModelScope)
    }

    private fun refreshDriverSuggestions() {
        _uiState.value = _uiState.value.copy(
            driverSuggestions = (workerDriverSuggestions + contactDriverSuggestions).distinct().sorted(),
        )
    }

    fun updateQuery(value: String) { _uiState.value = _uiState.value.copy(query = value, actionMessage = null) }
    fun cycleServiceFilter() {
        val nextFilter = when (_uiState.value.serviceFilter) {
            CarsServiceFilter.All -> CarsServiceFilter.DueSoon
            CarsServiceFilter.DueSoon -> CarsServiceFilter.Urgent
            CarsServiceFilter.Urgent -> CarsServiceFilter.All
        }
        _uiState.value = _uiState.value.copy(serviceFilter = nextFilter, actionMessage = null)
    }
    fun updateEditor(draft: CarDraft) { _uiState.value = _uiState.value.copy(editor = draft, actionMessage = null) }
    fun applyEditorDriverSuggestion(driver: String) { _uiState.value = _uiState.value.copy(editor = _uiState.value.editor.copy(driver = driver), actionMessage = null) }

    fun updateMileageDraft(id: Long, value: String) {
        _uiState.value = _uiState.value.copy(mileageDrafts = _uiState.value.mileageDrafts + (id to value), actionMessage = null)
    }

    fun updateDriverDraft(id: Long, value: String) {
        _uiState.value = _uiState.value.copy(driverDrafts = _uiState.value.driverDrafts + (id to value), actionMessage = null)
    }

    fun applyDriverDraftSuggestion(id: Long, driver: String) {
        _uiState.value = _uiState.value.copy(driverDrafts = _uiState.value.driverDrafts + (id to driver), actionMessage = null)
    }

    fun save() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isSaving = true, actionMessage = null)
        repository.saveCar(_uiState.value.editor)
        _uiState.value = _uiState.value.copy(isSaving = false, editor = CarDraft(), actionMessage = "Samochód zapisany")
    }

    fun saveMileage(id: Long) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(actionInFlightId = id, actionMessage = null)
        val mileage = _uiState.value.mileageDrafts[id]?.toIntOrNull() ?: 0
        repository.updateCarMileage(id, mileage)
        _uiState.value = _uiState.value.copy(actionInFlightId = null, actionMessage = "Przebieg zapisany")
    }

    fun saveDriver(id: Long) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(actionInFlightId = id, actionMessage = null)
        val driver = _uiState.value.driverDrafts[id].orEmpty()
        repository.updateCarDriver(id, driver)
        _uiState.value = _uiState.value.copy(
            actionInFlightId = null,
            actionMessage = if (driver.isBlank()) "Kierowca usunięty, konto kierowcy wyczyszczone" else "Kierowca zapisany",
        )
    }

    fun confirmService(id: Long) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(actionInFlightId = id, actionMessage = null)
        repository.confirmCarService(id)
        _uiState.value = _uiState.value.copy(actionInFlightId = null, actionMessage = "Serwis potwierdzony")
    }

    fun deleteCar(id: Long) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(actionInFlightId = id, actionMessage = null)
        repository.deleteCar(id)
        _uiState.value = _uiState.value.copy(actionInFlightId = null, actionMessage = "Samochód usunięty")
    }

    fun resetDriverCredentials(id: Long) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(actionInFlightId = id, actionMessage = null)
        val credentials = repository.resetCarDriverCredentials(id)
        _uiState.value = _uiState.value.copy(
            actionInFlightId = null,
            actionMessage = if (credentials.login.isBlank()) {
                "Brak kierowcy — konto kierowcy usunięte"
            } else {
                "Nowe dane kierowcy: ${credentials.login} / ${credentials.password}"
            },
        )
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

class PayrollViewModel(private val repository: AdminRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(PayrollUiState())
    val uiState: StateFlow<PayrollUiState> = _uiState.asStateFlow()

    init {
        repository.observeContacts().onEach { items ->
            _uiState.value = _uiState.value.copy(totalRecipients = items.size)
        }.launchIn(viewModelScope)
    }

    fun toggleAutoSend() {
        _uiState.value = _uiState.value.copy(autoSend = !_uiState.value.autoSend, actionMessage = null)
    }

    fun attachContactsCsv() = viewModelScope.launch {
        val path = repository.exportContactsCsv()
        addAttachment(path, "Dołączono CSV kontaktów")
    }

    fun attachSessionReportsCsv() = viewModelScope.launch {
        val path = repository.exportSessionReportsCsv()
        addAttachment(path, "Dołączono CSV raportów")
    }

    fun clearAttachments() {
        _uiState.value = _uiState.value.copy(
            attachmentPaths = emptyList(),
            attachmentCount = 0,
            actionMessage = "Załączniki wyczyszczone",
            progressLabel = "Gotowy",
            isMailingRunning = false,
        )
    }

    fun sendSingle() {
        val latest = _uiState.value.attachmentPaths.lastOrNull()
        _uiState.value = _uiState.value.copy(
            actionMessage = if (latest == null) {
                "Najpierw dołącz lokalny eksport jako załącznik"
            } else {
                "Pakiet gotowy do pojedynczej wysyłki SMTP: ${latest.substringAfterLast('/')}"
            },
            progressLabel = if (latest == null) "Brak załączników" else "Pakiet pojedynczej wysyłki przygotowany",
        )
    }

    fun startMassMailing() {
        val hasRecipients = _uiState.value.totalRecipients > 0
        val hasAttachments = _uiState.value.attachmentPaths.isNotEmpty()
        _uiState.value = _uiState.value.copy(
            isMailingRunning = hasRecipients && hasAttachments,
            progressLabel = when {
                !hasRecipients -> "Brak odbiorców w kontaktach"
                !hasAttachments -> "Brak załączników do masowej wysyłki"
                else -> "Kolejka przygotowana dla ${_uiState.value.totalRecipients} odbiorców"
            },
            actionMessage = when {
                !hasRecipients -> "Dodaj kontakty przed uruchomieniem wysyłki"
                !hasAttachments -> "Dołącz co najmniej jeden lokalny eksport"
                else -> "Przygotowano lokalny pakiet pod przyszły SMTP pipeline"
            },
        )
    }

    fun togglePauseMailing() {
        val running = _uiState.value.isMailingRunning
        _uiState.value = _uiState.value.copy(
            isMailingRunning = !running && _uiState.value.attachmentPaths.isNotEmpty() && _uiState.value.totalRecipients > 0,
            progressLabel = if (running) "Wysyłka wstrzymana" else "Wysyłka wznowiona",
            actionMessage = if (running) "Kolejka została wstrzymana" else "Kolejka została wznowiona",
        )
    }

    private fun addAttachment(path: String, label: String) {
        val updated = (_uiState.value.attachmentPaths + path).distinct()
        _uiState.value = _uiState.value.copy(
            attachmentPaths = updated,
            attachmentCount = updated.size,
            actionMessage = "$label: ${path.substringAfterLast('/')}",
            progressLabel = "Załączniki gotowe: ${updated.size}",
        )
    }
}

class TableViewModel(private val repository: AdminRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(TableUiState())
    val uiState: StateFlow<TableUiState> = _uiState.asStateFlow()

    init {
        repository.observeContacts().onEach { items ->
            _uiState.value = _uiState.value.copy(items = items)
        }.launchIn(viewModelScope)
    }

    fun updateQuery(value: String) {
        _uiState.value = _uiState.value.copy(query = value, exportMessage = null)
    }

    fun exportCsv() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isExporting = true, exportMessage = null)
        val path = repository.exportContactsCsv()
        _uiState.value = _uiState.value.copy(isExporting = false, exportMessage = "CSV kontaktów zapisany: $path")
    }

    fun exportRow(item: com.future.ultimate.core.common.repository.ContactListItem) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isExporting = true, exportMessage = null)
        val path = repository.exportContactRowXlsx(item.name, item.surname)
        _uiState.value = _uiState.value.copy(
            isExporting = false,
            exportMessage = if (path.isBlank()) "Nie znaleziono rekordu do eksportu" else "XLSX kontaktu zapisany: $path",
        )
    }
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

    fun edit(item: ClothesSizeListItem) {
        _uiState.value = _uiState.value.copy(
            editor = ClothesSizeDraft(
                id = item.id,
                name = item.name,
                surname = item.surname,
                plant = item.plant,
                shirt = item.shirt,
                hoodie = item.hoodie,
                pants = item.pants,
                jacket = item.jacket,
                shoes = item.shoes,
            ),
        )
    }

    fun clearEditor() {
        _uiState.value = _uiState.value.copy(editor = ClothesSizeDraft())
    }

    fun delete(id: Long) = viewModelScope.launch {
        repository.deleteClothesSize(id)
        if (_uiState.value.editor.id == id) {
            _uiState.value = _uiState.value.copy(editor = ClothesSizeDraft())
        }
    }
}


class ClothesOrdersViewModel(private val repository: AdminRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ClothesOrdersUiState())
    val uiState: StateFlow<ClothesOrdersUiState> = _uiState.asStateFlow()
    private var orderItemsJob: Job? = null

    init {
        repository.observeClothesOrders().onEach { _uiState.value = _uiState.value.copy(items = it) }.launchIn(viewModelScope)
        repository.observeClothesOrderWorkers().onEach { workers ->
            _uiState.value = _uiState.value.copy(availableWorkers = workers)
        }.launchIn(viewModelScope)
    }

    fun updateEditor(draft: ClothesOrderDraft) { _uiState.value = _uiState.value.copy(editor = draft, actionMessage = null) }
    fun updateItemEditor(draft: ClothesOrderItemDraft) { _uiState.value = _uiState.value.copy(itemEditor = draft, actionMessage = null) }
    fun updateWorkerQuery(value: String) { _uiState.value = _uiState.value.copy(workerQuery = value, actionMessage = null) }
    fun updateStarterQuantities(
        shirtQty: String = _uiState.value.shirtQty,
        hoodieQty: String = _uiState.value.hoodieQty,
        pantsQty: String = _uiState.value.pantsQty,
        jacketQty: String = _uiState.value.jacketQty,
        shoesQty: String = _uiState.value.shoesQty,
    ) {
        _uiState.value = _uiState.value.copy(
            shirtQty = shirtQty,
            hoodieQty = hoodieQty,
            pantsQty = pantsQty,
            jacketQty = jacketQty,
            shoesQty = shoesQty,
            actionMessage = null,
        )
    }

    fun toggleWorkerSelection(workerId: Long) {
        val selected = _uiState.value.selectedWorkerIds
        _uiState.value = _uiState.value.copy(
            selectedWorkerIds = if (workerId in selected) selected - workerId else selected + workerId,
            actionMessage = null,
        )
    }

    fun selectWorkers(workerIds: Set<Long>) {
        if (workerIds.isEmpty()) return
        _uiState.value = _uiState.value.copy(
            selectedWorkerIds = _uiState.value.selectedWorkerIds + workerIds,
            actionMessage = null,
        )
    }

    fun unselectWorkers(workerIds: Set<Long>) {
        if (workerIds.isEmpty()) return
        _uiState.value = _uiState.value.copy(
            selectedWorkerIds = _uiState.value.selectedWorkerIds - workerIds,
            actionMessage = null,
        )
    }

    fun clearStarterSelection() {
        _uiState.value = _uiState.value.copy(
            workerQuery = "",
            selectedWorkerIds = emptySet(),
            shirtQty = "1",
            hoodieQty = "1",
            pantsQty = "1",
            jacketQty = "1",
            shoesQty = "1",
            actionMessage = null,
        )
    }

    fun save() = viewModelScope.launch {
        val isEditing = _uiState.value.editor.id != null
        _uiState.value = _uiState.value.copy(isSaving = true)
        repository.saveClothesOrder(_uiState.value.editor)
        _uiState.value = _uiState.value.copy(
            isSaving = false,
            editor = ClothesOrderDraft(),
            actionMessage = if (isEditing) "Zamówienie zaktualizowane" else "Zamówienie zapisane",
        )
    }

    fun createStarterOrder() = viewModelScope.launch {
        val state = _uiState.value
        if (state.selectedWorkerIds.isEmpty()) {
            _uiState.value = state.copy(actionMessage = "Wybierz co najmniej jednego pracownika do zestawu startowego")
            return@launch
        }
        val quantities = listOf(
            state.shirtQty.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            state.hoodieQty.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            state.pantsQty.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            state.jacketQty.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            state.shoesQty.toIntOrNull()?.coerceAtLeast(0) ?: 0,
        )
        if (quantities.all { it == 0 }) {
            _uiState.value = state.copy(actionMessage = "Ustaw ilość większą od zera dla przynajmniej jednej pozycji")
            return@launch
        }
        _uiState.value = state.copy(isCreatingStarterOrder = true, actionMessage = null)
        val orderId = repository.createClothesOrderStarter(
            draft = state.editor,
            workerIds = state.selectedWorkerIds,
            shirtQty = quantities[0],
            hoodieQty = quantities[1],
            pantsQty = quantities[2],
            jacketQty = quantities[3],
            shoesQty = quantities[4],
        )
        if (orderId == null) {
            _uiState.value = _uiState.value.copy(
                isCreatingStarterOrder = false,
                actionMessage = "Nie udało się zbudować zamówienia — sprawdź pracowników i ilości",
            )
            return@launch
        }

        _uiState.value = _uiState.value.copy(
            isCreatingStarterOrder = false,
            editor = ClothesOrderDraft(),
            workerQuery = "",
            selectedWorkerIds = emptySet(),
            shirtQty = "1",
            hoodieQty = "1",
            pantsQty = "1",
            jacketQty = "1",
            shoesQty = "1",
            actionMessage = "Utworzono zamówienie startowe #$orderId",
            selectedOrderId = orderId,
            selectedOrderItems = emptyList(),
            selectedOrderSummary = emptyList(),
            itemEditor = ClothesOrderItemDraft(),
        )
        orderItemsJob?.cancel()
        orderItemsJob = repository.observeClothesOrderItems(orderId).onEach { items ->
            _uiState.value = _uiState.value.copy(
                selectedOrderItems = items,
                selectedOrderSummary = buildOrderSummary(items),
            )
        }.launchIn(viewModelScope)
    }

    fun toggleOrderSelection(orderId: Long) {
        val current = _uiState.value.selectedOrderId
        if (current == orderId) {
            orderItemsJob?.cancel()
            _uiState.value = _uiState.value.copy(
                selectedOrderId = null,
                selectedOrderItems = emptyList(),
                selectedOrderSummary = emptyList(),
                showOnlyPendingItems = false,
                itemEditor = ClothesOrderItemDraft(),
            )
            return
        }
        orderItemsJob?.cancel()
        _uiState.value = _uiState.value.copy(
            selectedOrderId = orderId,
            selectedOrderItems = emptyList(),
            selectedOrderSummary = emptyList(),
            showOnlyPendingItems = false,
            itemEditor = ClothesOrderItemDraft(),
        )
        orderItemsJob = repository.observeClothesOrderItems(orderId).onEach { items ->
            _uiState.value = _uiState.value.copy(
                selectedOrderItems = items,
                selectedOrderSummary = buildOrderSummary(items),
            )
        }.launchIn(viewModelScope)
    }

    fun saveItem() = viewModelScope.launch {
        val selectedOrderId = _uiState.value.selectedOrderId ?: return@launch
        val draft = _uiState.value.itemEditor
        if (draft.name.isBlank() || draft.surname.isBlank() || draft.item.isBlank()) return@launch
        val isEditing = draft.id != null
        _uiState.value = _uiState.value.copy(isSavingItem = true, actionMessage = null)
        repository.saveClothesOrderItem(selectedOrderId, draft)
        _uiState.value = _uiState.value.copy(
            isSavingItem = false,
            itemEditor = ClothesOrderItemDraft(),
            actionMessage = if (isEditing) "Pozycja zaktualizowana" else "Pozycja dodana",
        )
    }

    fun deleteItem(id: Long) = viewModelScope.launch {
        repository.deleteClothesOrderItem(id)
        _uiState.value = _uiState.value.copy(
            itemEditor = if (_uiState.value.itemEditor.id == id) ClothesOrderItemDraft() else _uiState.value.itemEditor,
            actionMessage = "Pozycja usunięta",
        )
    }

    fun editOrder(item: ClothesOrderListItem) {
        _uiState.value = _uiState.value.copy(
            editor = ClothesOrderDraft(
                id = item.id,
                date = item.date,
                plant = item.plant,
                status = item.status,
                orderDesc = item.orderDesc,
            ),
            actionMessage = null,
        )
    }

    fun clearOrderEditor() {
        _uiState.value = _uiState.value.copy(editor = ClothesOrderDraft(), actionMessage = null)
    }

    fun editItem(item: ClothesOrderItemListItem) {
        _uiState.value = _uiState.value.copy(
            itemEditor = ClothesOrderItemDraft(
                id = item.id,
                name = item.name,
                surname = item.surname,
                item = item.item,
                size = item.size,
                qty = item.qty.toString(),
            ),
            actionMessage = null,
        )
    }

    fun clearItemEditor() {
        _uiState.value = _uiState.value.copy(itemEditor = ClothesOrderItemDraft(), actionMessage = null)
    }

    fun togglePendingItemsFilter() {
        _uiState.value = _uiState.value.copy(
            showOnlyPendingItems = !_uiState.value.showOnlyPendingItems,
            actionMessage = null,
        )
    }

    fun deleteOrder(orderId: Long) = viewModelScope.launch {
        repository.deleteClothesOrder(orderId)
        _uiState.value = _uiState.value.copy(
            selectedOrderId = if (_uiState.value.selectedOrderId == orderId) null else _uiState.value.selectedOrderId,
            selectedOrderItems = if (_uiState.value.selectedOrderId == orderId) emptyList() else _uiState.value.selectedOrderItems,
            selectedOrderSummary = if (_uiState.value.selectedOrderId == orderId) emptyList() else _uiState.value.selectedOrderSummary,
            showOnlyPendingItems = if (_uiState.value.selectedOrderId == orderId) false else _uiState.value.showOnlyPendingItems,
            editor = if (_uiState.value.editor.id == orderId) ClothesOrderDraft() else _uiState.value.editor,
            itemEditor = ClothesOrderItemDraft(),
            actionMessage = "Zamówienie usunięte",
        )
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

    fun exportOrderPdf(orderId: Long) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isExportingPdf = true, actionMessage = null)
        val path = repository.exportClothesOrderPdf(orderId)
        _uiState.value = _uiState.value.copy(
            isExportingPdf = false,
            actionMessage = if (path.isBlank()) "Nie znaleziono pozycji do eksportu PDF" else "PDF zamówienia: $path",
        )
    }

    fun exportIssuePdf(orderId: Long) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isExportingIssuePdf = true, actionMessage = null)
        val path = repository.exportClothesIssuePdf(orderId)
        _uiState.value = _uiState.value.copy(
            isExportingIssuePdf = false,
            actionMessage = if (path.isBlank()) "Nie znaleziono pozycji do raportu wydania PDF" else "PDF wydania: $path",
        )
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

    private fun buildOrderSummary(items: List<ClothesOrderItemListItem>): List<String> =
        items.groupBy { it.item.trim() to it.size.trim().ifBlank { "-" } }
            .toList()
            .sortedWith(compareBy({ it.first.first.lowercase() }, { it.first.second.lowercase() }))
            .map { (key, groupedItems) ->
                "${key.first} • rozmiar: ${key.second} • suma: ${groupedItems.sumOf { it.qty }}"
            }
}

class ClothesReportsViewModel(private val repository: AdminRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ClothesReportsUiState(year = LocalDate.now().year.toString()))
    val uiState: StateFlow<ClothesReportsUiState> = _uiState.asStateFlow()

    init {
        repository.observeClothesHistory().onEach { history ->
            _uiState.value = _uiState.value.copy(
                history = history,
                yearlySummary = buildYearlySummary(history, _uiState.value.year, _uiState.value.workerQuery),
            )
        }.launchIn(viewModelScope)
    }

    fun updateYear(value: String) {
        _uiState.value = _uiState.value.copy(
            year = value,
            yearlySummary = buildYearlySummary(_uiState.value.history, value, _uiState.value.workerQuery),
        )
    }

    fun updateWorkerQuery(value: String) {
        _uiState.value = _uiState.value.copy(
            workerQuery = value,
            yearlySummary = buildYearlySummary(_uiState.value.history, _uiState.value.year, value),
        )
    }

    fun exportCsv() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isExporting = true, exportMessage = null)
        val path = repository.exportClothesHistoryCsv()
        _uiState.value = _uiState.value.copy(isExporting = false, exportMessage = "CSV zapisane: $path")
    }

    private fun buildYearlySummary(
        history: List<com.future.ultimate.core.common.repository.ClothesHistoryListItem>,
        year: String,
        workerQuery: String,
    ): List<String> {
        val normalizedYear = year.trim()
        if (normalizedYear.isBlank()) return emptyList()
        val normalizedQuery = workerQuery.trim().lowercase()
        return history
            .filter { it.date.startsWith(normalizedYear) }
            .filter { item ->
                normalizedQuery.isBlank() || listOf(item.name, item.surname, item.item, item.size).joinToString(" ").lowercase().contains(normalizedQuery)
            }
            .groupBy { Triple(it.workerId, it.name.trim(), it.surname.trim()) }
            .map { (worker, entries) ->
                val workerLabel = "${worker.second} ${worker.third}".trim().ifBlank { "Pracownik #${worker.first}" }
                val lastIssueDate = entries.maxOfOrNull { it.date }.orEmpty()
                val groupedItems = entries.groupBy { entry ->
                    when {
                        entry.item.contains("koszul", ignoreCase = true) -> "Koszulka"
                        entry.item.contains("bluz", ignoreCase = true) || entry.item.contains("hoodie", ignoreCase = true) -> "Bluza"
                        entry.item.contains("spodni", ignoreCase = true) || entry.item.contains("pants", ignoreCase = true) -> "Spodnie"
                        entry.item.contains("kurt", ignoreCase = true) || entry.item.contains("jacket", ignoreCase = true) -> "Kurtka"
                        entry.item.contains("but", ignoreCase = true) || entry.item.contains("shoe", ignoreCase = true) -> "Buty"
                        else -> "Inne"
                    }
                }.mapValues { (_, categoryEntries) -> categoryEntries.size }
                val categorySummary = listOf("Koszulka", "Bluza", "Spodnie", "Kurtka", "Buty", "Inne")
                    .mapNotNull { label ->
                        val count = groupedItems[label] ?: 0
                        if (count <= 0) null else "$label: $count"
                    }
                    .joinToString(" • ")
                "$workerLabel\n$categorySummary\nŁącznie: ${entries.size} • ostatnie wydanie: $lastIssueDate"
            }
            .sortedByDescending { summary ->
                summary.substringAfter("Łącznie: ").substringBefore(" •").toIntOrNull() ?: 0
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

    fun exportDatabaseSnapshot() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isExportingDatabase = true, actionMessage = null)
        val path = repository.exportDatabaseSnapshot()
        _uiState.value = _uiState.value.copy(
            isExportingDatabase = false,
            actionMessage = if (path.isBlank()) "Nie udało się wyeksportować bazy" else "Snapshot bazy zapisany: $path",
        )
    }
}

class AdminViewModelFactory(private val repository: AdminRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(ContactsViewModel::class.java) -> ContactsViewModel(repository) as T
        modelClass.isAssignableFrom(CarsViewModel::class.java) -> CarsViewModel(repository) as T
        modelClass.isAssignableFrom(VehicleReportViewModel::class.java) -> VehicleReportViewModel(repository) as T
        modelClass.isAssignableFrom(PayrollViewModel::class.java) -> PayrollViewModel(repository) as T
        modelClass.isAssignableFrom(TableViewModel::class.java) -> TableViewModel(repository) as T
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
