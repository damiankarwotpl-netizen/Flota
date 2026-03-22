package com.future.ultimate.admin.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.future.ultimate.admin.payroll.DelimitedExcelParser
import com.future.ultimate.admin.payroll.ExportService
import com.future.ultimate.admin.payroll.PayslipFilter
import com.future.ultimate.admin.payroll.PayslipGenerator
import com.future.ultimate.admin.payroll.PayslipMapper
import com.future.ultimate.admin.payroll.PayslipModule
import com.future.ultimate.core.common.model.CarDraft
import com.future.ultimate.core.common.model.ClothesOrderDraft
import com.future.ultimate.core.common.model.ClothesOrderItemDraft
import com.future.ultimate.core.common.model.ClothesSizeDraft
import com.future.ultimate.core.common.model.ContactDraft
import com.future.ultimate.core.common.model.PlantDraft
import com.future.ultimate.core.common.model.VehicleReportDraft
import com.future.ultimate.core.common.model.WorkerDraft
import com.future.ultimate.core.common.patch.PatchLoader
import com.future.ultimate.core.common.repository.AdminRepository
import com.future.ultimate.core.common.repository.ClothesOrderItemListItem
import com.future.ultimate.core.common.repository.ClothesOrderImportRow
import com.future.ultimate.core.common.repository.ClothesOrderListItem
import com.future.ultimate.core.common.repository.ClothesSizeListItem
import com.future.ultimate.core.common.repository.CarListItem
import com.future.ultimate.core.common.repository.EmailTemplateData
import com.future.ultimate.core.common.repository.ContactListItem
import com.future.ultimate.core.common.repository.MailApprovalRequest
import com.future.ultimate.core.common.repository.PayrollWorkbookRow
import com.future.ultimate.core.common.repository.PayrollPreviewRow
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.time.LocalDateTime
import androidx.documentfile.provider.DocumentFile
import java.io.File

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
            driverSuggestions = (workerDriverSuggestions + contactDriverSuggestions)
                .groupBy { it.trim().lowercase() }
                .mapNotNull { (_, values) -> values.firstOrNull()?.trim()?.takeIf { it.isNotBlank() } }
                .sorted(),
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
    fun editCar(car: CarListItem) {
        _uiState.value = _uiState.value.copy(
            editor = CarDraft(
                id = car.id,
                name = car.name,
                registration = car.registration,
                driver = car.driver,
                serviceInterval = car.serviceInterval.toString(),
            ),
            actionMessage = null,
        )
    }
    fun clearEditor() {
        _uiState.value = _uiState.value.copy(editor = CarDraft(), actionMessage = null)
    }
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
        val draft = _uiState.value.editor
        if (draft.name.isBlank()) {
            _uiState.value = _uiState.value.copy(actionMessage = "Nazwa samochodu jest wymagana")
            return@launch
        }
        if (draft.registration.isBlank()) {
            _uiState.value = _uiState.value.copy(actionMessage = "Rejestracja samochodu jest wymagana")
            return@launch
        }
        if (draft.serviceInterval.toIntOrNull()?.let { it > 0 } != true) {
            _uiState.value = _uiState.value.copy(actionMessage = "Interwał serwisowy musi być dodatnią liczbą")
            return@launch
        }
        val isEditing = draft.id != null
        _uiState.value = _uiState.value.copy(isSaving = true, actionMessage = null)
        repository.saveCar(draft)
        _uiState.value = _uiState.value.copy(
            isSaving = false,
            editor = CarDraft(),
            actionMessage = if (isEditing) "Samochód zaktualizowany" else "Samochód zapisany",
        )
    }

    fun saveMileage(id: Long) = viewModelScope.launch {
        val mileageText = _uiState.value.mileageDrafts[id]
        val mileage = mileageText?.toIntOrNull()
        if (mileage == null || mileage < 0) {
            _uiState.value = _uiState.value.copy(actionMessage = "Przebieg musi być liczbą dodatnią lub zerem")
            return@launch
        }
        _uiState.value = _uiState.value.copy(actionInFlightId = id, actionMessage = null)
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

    fun retryRemoteDriverSync(id: Long) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(actionInFlightId = id, actionMessage = null)
        repository.retryCarDriverRemoteSync(id)
        _uiState.value = _uiState.value.copy(actionInFlightId = null, actionMessage = "Ponowiono zdalną synchronizację kierowcy")
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
    private val _uiState = MutableStateFlow(PayrollUiState(operatorLabel = PatchLoader.fallbackUserLabel()))
    val uiState: StateFlow<PayrollUiState> = _uiState.asStateFlow()
    private var contactsCache: List<ContactListItem> = emptyList()
    private var templateCache: EmailTemplateData = EmailTemplateData()
    private var mailingJob: Job? = null
    private var pendingApprovalDecision: CompletableDeferred<Boolean>? = null
    private val payslipModule = PayslipModule(
        excelParser = DelimitedExcelParser(),
        mapper = PayslipMapper(),
        filter = PayslipFilter(),
        generator = PayslipGenerator(),
        exportService = object : ExportService {
            override suspend fun export(rows: List<PayrollWorkbookRow>): String = repository.exportPayrollWorkbookCsv(rows)
        },
    )
    @Volatile
    private var mailingPaused: Boolean = false

    init {
        repository.observeContacts().onEach { items ->
            contactsCache = items
            val knownKeys = items.map(::selectionKey).toSet()
            _uiState.value = _uiState.value.copy(
                contacts = items,
                filteredRecipients = filterRecipients(items, _uiState.value.recipientQuery),
                totalRecipients = items.size,
                selectedRecipientKeys = _uiState.value.selectedRecipientKeys.intersect(knownKeys),
            )
        }.launchIn(viewModelScope)
        repository.observeEmailTemplate().onEach { template ->
            templateCache = template
            val state = _uiState.value
            _uiState.value = state.copy(
                specialSubject = state.specialSubject.ifBlank { template.subject },
                specialBody = state.specialBody.ifBlank { template.body },
            )
        }.launchIn(viewModelScope)
    }

    fun toggleAutoSend() {
        _uiState.value = _uiState.value.copy(autoSend = !_uiState.value.autoSend, actionMessage = null)
    }

    fun updateGrossAmount(value: String) {
        _uiState.value = _uiState.value.copy(grossAmount = value, actionMessage = null)
    }

    fun updateBonusAmount(value: String) {
        _uiState.value = _uiState.value.copy(bonusAmount = value, actionMessage = null)
    }

    fun updateDeductionsAmount(value: String) {
        _uiState.value = _uiState.value.copy(deductionsAmount = value, actionMessage = null)
    }

    fun updateTaxPercent(value: String) {
        _uiState.value = _uiState.value.copy(taxPercent = value, actionMessage = null)
    }

    fun updateWorkbookImportText(value: String) {
        _uiState.value = _uiState.value.copy(workbookImportText = value, actionMessage = null)
    }

    fun updateExportFolderUri(uri: Uri?) {
        _uiState.value = _uiState.value.copy(
            exportFolderUri = uri?.toString().orEmpty(),
            actionMessage = if (uri == null) "Wybór folderu anulowany" else "Wybrano folder eksportu",
        )
    }

    fun stageWorkbookImport() {
        val payslipData = payslipModule.loadFromDelimitedText(_uiState.value.workbookImportText)
        applyPayslipData(payslipData)
    }

    fun loadWorkbookFromText(rawText: String) {
        _uiState.value = _uiState.value.copy(workbookImportText = rawText)
        stageWorkbookImport()
    }

    fun loadWorkbookFromFile(
        fileName: String?,
        mimeType: String?,
        bytes: ByteArray,
    ) {
        val payslipData = payslipModule.loadFromBytes(
            fileName = fileName,
            mimeType = mimeType,
            bytes = bytes,
        )
        _uiState.value = _uiState.value.copy(workbookImportText = fileName ?: "plik")
        applyPayslipData(payslipData)
    }

    fun clearWorkbookImport() {
        _uiState.value = _uiState.value.copy(
            workbookImportText = "",
            stagedWorkbookRows = emptyList(),
            previewHeaders = emptyList(),
            previewRows = emptyList(),
            selectedPreviewRowIndexes = emptySet(),
            selectedPreviewColumnIndexes = emptySet(),
            actionMessage = "Staging workbooka wyczyszczony",
            progressLabel = "Gotowy",
        )
    }

    fun togglePreviewRowSelection(index: Int) {
        val updated = _uiState.value.selectedPreviewRowIndexes.let { current ->
            if (index in current) current - index else current + index
        }
        _uiState.value = _uiState.value.copy(selectedPreviewRowIndexes = updated, actionMessage = null)
    }

    fun clearPreviewSelection() {
        _uiState.value = _uiState.value.copy(selectedPreviewRowIndexes = emptySet(), actionMessage = "Wyczyszczono wybór tabeli")
    }

    fun togglePreviewColumnSelection(index: Int) {
        val updated = _uiState.value.selectedPreviewColumnIndexes.let { current ->
            if (index in current) current - index else current + index
        }
        _uiState.value = _uiState.value.copy(selectedPreviewColumnIndexes = updated, actionMessage = null)
    }

    fun selectAllPreviewColumns() {
        val all = (0 until maxOf(_uiState.value.previewHeaders.size, _uiState.value.previewRows.maxOfOrNull { it.cells.size } ?: 0)).toSet()
        _uiState.value = _uiState.value.copy(selectedPreviewColumnIndexes = all, actionMessage = "Zaznaczono wszystkie kolumny")
    }

    fun prepareCashReportSelection() {
        val previewRows = _uiState.value.previewRows
        val previewHeaders = _uiState.value.previewHeaders
        val fallbackMaxColumns = previewRows.maxOfOrNull { it.cells.size } ?: 0
        val defaultColumns = if (previewHeaders.isNotEmpty()) {
            previewHeaders.mapIndexedNotNull { index, header ->
                val normalized = header.trim().lowercase()
                when {
                    normalized.contains("imi") -> index
                    normalized.contains("nazw") -> index
                    else -> null
                }
            }.toSet()
        } else {
            emptySet()
        }
        _uiState.value = _uiState.value.copy(
            selectedPreviewRowIndexes = previewRows.map { it.index }.toSet(),
            selectedPreviewColumnIndexes = defaultColumns.ifEmpty { (0 until fallbackMaxColumns.coerceAtMost(3)).toSet() },
            actionMessage = null,
        )
    }

    fun clearActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }

    fun exportSinglePreviewRow(index: Int) = viewModelScope.launch {
        val row = _uiState.value.previewRows.firstOrNull { it.index == index } ?: return@launch
        val selectedColumns = _uiState.value.selectedPreviewColumnIndexes
        if (selectedColumns.isEmpty()) {
            _uiState.value = _uiState.value.copy(actionMessage = "Wybierz co najmniej jedną kolumnę do eksportu")
            return@launch
        }
        val filteredHeaders = _uiState.value.previewHeaders.filterIndexed { columnIndex, _ -> columnIndex in selectedColumns }
        val filteredRow = row.cells.filterIndexed { columnIndex, _ -> columnIndex in selectedColumns }.map(::normalizePayrollCell)
        val path = repository.exportPayrollRowsXlsx(
            headers = filteredHeaders,
            rows = listOf(filteredRow),
            filePrefix = "PPI",
            nameHint = row.name,
            surnameHint = row.surname,
        )
        addAttachment(path, "Wyeksportowano pojedynczy wiersz")
    }

    fun sendSinglePreviewRowMail(index: Int) = viewModelScope.launch {
        val row = _uiState.value.previewRows.firstOrNull { it.index == index } ?: return@launch
        val selectedColumns = _uiState.value.selectedPreviewColumnIndexes
        if (selectedColumns.isEmpty()) {
            _uiState.value = _uiState.value.copy(actionMessage = "Wybierz co najmniej jedną kolumnę do wysyłki")
            return@launch
        }

        val recipient = contactsCache.firstOrNull {
            it.name.trim().equals(row.name.trim(), ignoreCase = true) &&
                it.surname.trim().equals(row.surname.trim(), ignoreCase = true)
        }
        if (recipient == null) {
            _uiState.value = _uiState.value.copy(
                actionMessage = "Nie znaleziono kontaktu dla ${row.name} ${row.surname}".trim(),
            )
            return@launch
        }
        if (recipient.email.isBlank()) {
            _uiState.value = _uiState.value.copy(
                actionMessage = "Kontakt ${row.name} ${row.surname} nie ma adresu email".trim(),
            )
            return@launch
        }

        val filteredHeaders = selectedHeaders(selectedColumns)
        val filteredRow = row.cells.filterIndexed { columnIndex, _ -> columnIndex in selectedColumns }.map(::normalizePayrollCell)
        val attachmentPath = repository.exportPayrollRowsXlsx(
            headers = filteredHeaders,
            rows = listOf(filteredRow),
            filePrefix = "PPI",
            nameHint = row.name,
            surnameHint = row.surname,
        )
        if (attachmentPath.isBlank()) {
            _uiState.value = _uiState.value.copy(actionMessage = "Nie udało się wygenerować załącznika XLSX")
            return@launch
        }

        runCatching {
            repository.sendSpecialMailing(
                recipients = listOf(recipient),
                attachmentPaths = listOf(attachmentPath),
                subject = templateCache.subject,
                body = templateCache.body,
            )
        }.onSuccess { result ->
            val actionMessage = when {
                result.ok > 0 -> "Wysłano email do ${recipient.email}"
                result.skip > 0 -> result.details.lineSequence().firstOrNull { it.isNotBlank() } ?: "Wysyłka została pominięta"
                else -> result.details.lineSequence().firstOrNull { it.isNotBlank() } ?: "Nie udało się wysłać emaila"
            }
            _uiState.value = _uiState.value.copy(actionMessage = actionMessage)
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(
                actionMessage = error.message ?: "Nie udało się wysłać emaila dla wybranego wiersza",
            )
        }
    }

    fun exportSinglePreviewRowToFolder(context: Context, index: Int) = viewModelScope.launch {
        val folderUri = _uiState.value.exportFolderUri.takeIf { it.isNotBlank() }?.let(Uri::parse)
        if (folderUri == null) {
            _uiState.value = _uiState.value.copy(actionMessage = "Najpierw wybierz folder eksportu")
            return@launch
        }
        val row = _uiState.value.previewRows.firstOrNull { it.index == index } ?: return@launch
        val selectedColumns = _uiState.value.selectedPreviewColumnIndexes
        if (selectedColumns.isEmpty()) {
            _uiState.value = _uiState.value.copy(actionMessage = "Wybierz co najmniej jedną kolumnę do eksportu")
            return@launch
        }
        val filteredHeaders = selectedHeaders(selectedColumns)
        val filteredRow = row.cells.filterIndexed { columnIndex, _ -> columnIndex in selectedColumns }.map(::normalizePayrollCell)
        val tempPath = repository.exportPayrollRowsXlsx(
            headers = filteredHeaders,
            rows = listOf(filteredRow),
            filePrefix = "PPI",
            nameHint = row.name,
            surnameHint = row.surname,
        )
        if (tempPath.isBlank()) {
            _uiState.value = _uiState.value.copy(actionMessage = "Nie udało się wygenerować pliku XLSX")
            return@launch
        }
        val fileName = exportFileName(row.name, row.surname)
        val saved = copyFileToDocumentTree(context, File(tempPath), folderUri, fileName)
        _uiState.value = _uiState.value.copy(
            actionMessage = if (saved) "Wyeksportowano: $fileName" else "Nie udało się zapisać pliku w wybranym folderze",
        )
    }

    fun exportSelectedPreviewRows() = viewModelScope.launch {
        val selected = _uiState.value.previewRows.filter { it.index in _uiState.value.selectedPreviewRowIndexes }
        if (selected.isEmpty()) {
            _uiState.value = _uiState.value.copy(actionMessage = "Wybierz co najmniej jeden wiersz do eksportu tabeli")
            return@launch
        }
        val selectedColumns = _uiState.value.selectedPreviewColumnIndexes
        if (selectedColumns.isEmpty()) {
            _uiState.value = _uiState.value.copy(actionMessage = "Wybierz co najmniej jedną kolumnę do eksportu")
            return@launch
        }
        val filteredHeaders = _uiState.value.previewHeaders.filterIndexed { columnIndex, _ -> columnIndex in selectedColumns }
        val path = repository.exportPayrollRowsXlsx(
            headers = filteredHeaders,
            rows = selected.map { row ->
                row.cells.filterIndexed { columnIndex, _ -> columnIndex in selectedColumns }.map(::normalizePayrollCell)
            },
            filePrefix = "PPI_TABELA",
            nameHint = "zaznaczone",
            surnameHint = selected.size.toString(),
        )
        addAttachment(path, "Wyeksportowano zaznaczoną tabelę")
    }

    fun generateCashReportToFolder(context: Context) = viewModelScope.launch {
        val folderUri = _uiState.value.exportFolderUri.takeIf { it.isNotBlank() }?.let(Uri::parse)
        if (folderUri == null) {
            _uiState.value = _uiState.value.copy(actionMessage = "Najpierw wybierz folder eksportu")
            return@launch
        }
        val selectedRows = _uiState.value.previewRows.filter { it.index in _uiState.value.selectedPreviewRowIndexes }
        if (selectedRows.isEmpty()) {
            _uiState.value = _uiState.value.copy(actionMessage = "Wybierz co najmniej jeden wiersz do raportu gotówki")
            return@launch
        }
        val selectedColumns = _uiState.value.selectedPreviewColumnIndexes
        if (selectedColumns.isEmpty()) {
            _uiState.value = _uiState.value.copy(actionMessage = "Wybierz co najmniej jedną kolumnę do raportu gotówki")
            return@launch
        }

        val filteredHeaders = selectedHeaders(selectedColumns)
        val filteredRows = selectedRows.map { row ->
            row.cells.filterIndexed { columnIndex, _ -> columnIndex in selectedColumns }.map(::normalizePayrollCell)
        }
        val totalAmount = calculateCashReportTotal(filteredHeaders, filteredRows)
        val tempPath = repository.exportPayrollCashReportXlsx(
            headers = filteredHeaders,
            rows = filteredRows,
            totalAmount = totalAmount,
        )
        if (tempPath.isBlank()) {
            _uiState.value = _uiState.value.copy(actionMessage = "Nie udało się wygenerować raportu gotówki")
            return@launch
        }

        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
        val fileName = "raport_gotowki_$stamp.xlsx"
        val saved = copyFileToDocumentTree(context, File(tempPath), folderUri, fileName)
        _uiState.value = _uiState.value.copy(
            actionMessage = if (saved) "Wygenerowano raport gotówki: $fileName" else "Nie udało się zapisać raportu gotówki",
        )
    }

    private fun selectedHeaders(selectedColumns: Set<Int>): List<String> {
        val headers = _uiState.value.previewHeaders
        if (headers.isEmpty()) {
            val maxColumns = _uiState.value.previewRows.maxOfOrNull { it.cells.size } ?: 0
            return (0 until maxColumns).filter { it in selectedColumns }.map { "kolumna_${it + 1}" }
        }
        return headers.filterIndexed { columnIndex, _ -> columnIndex in selectedColumns }
    }

    private fun normalizePayrollCell(value: String): String = value.trim().ifBlank { "0" }

    private fun calculateCashReportTotal(headers: List<String>, rows: List<List<String>>): String {
        val amountIndex = headers.indexOfFirst { header ->
            val normalized = header.trim().lowercase()
            normalized.contains("suma") || normalized.contains("netto") || normalized.contains("amount")
        }
        if (amountIndex < 0) return "0"
        val total = rows.sumOf { row ->
            row.getOrNull(amountIndex)
                ?.replace(" ", "")
                ?.replace(",", ".")
                ?.toDoubleOrNull()
                ?: 0.0
        }
        return if (total % 1.0 == 0.0) total.toInt().toString() else "%.2f".format(total)
    }

    private fun applyPayslipData(payslipData: com.future.ultimate.admin.payroll.PayslipData) {
        val rows = PayslipGenerator().toWorkbookRows(payslipData.rows)
        val previewRows = payslipData.rows.mapIndexed { index, row ->
            PayrollPreviewRow(
                index = index,
                cells = row.raw.map(::normalizePayrollCell),
                name = row.name,
                surname = row.surname,
            )
        }
        if (rows.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                stagedWorkbookRows = emptyList(),
                previewHeaders = emptyList(),
                previewRows = emptyList(),
                selectedPreviewRowIndexes = emptySet(),
                selectedPreviewColumnIndexes = emptySet(),
                actionMessage = "Nie udało się sparsować żadnych wierszy importu",
                progressLabel = "Brak danych do stagingu",
            )
            return
        }
        val columnCount = maxOf(
            payslipData.headers.size,
            previewRows.maxOfOrNull { it.cells.size } ?: 0,
        )
        _uiState.value = _uiState.value.copy(
            stagedWorkbookRows = rows,
            previewHeaders = payslipData.headers,
            previewRows = previewRows,
            selectedPreviewRowIndexes = emptySet(),
            selectedPreviewColumnIndexes = (0 until columnCount).toSet(),
            actionMessage = "Zaimportowano lokalnie ${rows.size} wierszy workbooka do stagingu",
            progressLabel = "Workbook staged: ${rows.size} wierszy",
        )
    }

    private fun exportFileName(name: String, surname: String): String {
        val safeName = name.trim().ifBlank { "rekord" }.replace("\\s+".toRegex(), "_")
        val safeSurname = surname.trim().ifBlank { "rekord" }.replace("\\s+".toRegex(), "_")
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
        return "${safeName}_${safeSurname}_$stamp.xlsx"
    }

    private fun copyFileToDocumentTree(context: Context, source: File, folderUri: Uri, fileName: String): Boolean {
        val pickedDir = DocumentFile.fromTreeUri(context, folderUri) ?: return false
        val created = pickedDir.createFile(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            fileName.removeSuffix(".xlsx"),
        ) ?: return false
        context.contentResolver.openOutputStream(created.uri)?.use { output ->
            source.inputStream().use { input -> input.copyTo(output) }
            return true
        }
        return false
    }

    fun attachStagedWorkbookCsv() = viewModelScope.launch {
        val rows = _uiState.value.stagedWorkbookRows
        if (rows.isEmpty()) {
            _uiState.value = _uiState.value.copy(actionMessage = PatchLoader.fallbackImportMessage("Płace"))
            return@launch
        }
        val path = payslipModule.export(
            rows = rows.map {
                com.future.ultimate.admin.payroll.PayslipRow(
                    raw = listOf(it.amount),
                    name = it.name,
                    surname = it.surname,
                    pesel = null,
                    email = it.email,
                )
            },
            workplace = rows.firstOrNull()?.workplace.orEmpty(),
        )
        addAttachment(path, "Dołączono staging workbooka")
    }

    fun calculatePayroll() {
        val gross = _uiState.value.grossAmount.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val bonus = _uiState.value.bonusAmount.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val deductions = _uiState.value.deductionsAmount.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val taxPercent = (_uiState.value.taxPercent.toDoubleOrNull() ?: 12.0).coerceAtLeast(0.0)
        val taxableBase = (gross + bonus - deductions).coerceAtLeast(0.0)
        val taxAmount = taxableBase * (taxPercent / 100.0)
        val employerCost = gross * 0.2048
        val netAmount = (taxableBase - taxAmount).coerceAtLeast(0.0)
        _uiState.value = _uiState.value.copy(
            employerCostAmount = formatMoney(employerCost),
            netAmount = formatMoney(netAmount),
            calculationSummary = "Brutto ${formatMoney(gross)} + premia ${formatMoney(bonus)} - potrącenia ${formatMoney(deductions)} - podatek ${formatMoney(taxAmount)}",
            actionMessage = "Kalkulacja płac wykonana lokalnie",
            progressLabel = "Kalkulacja gotowa",
        )
    }

    fun attachContactsCsv() = viewModelScope.launch {
        val path = repository.exportContactsCsv()
        addAttachment(path, "Dołączono CSV kontaktów")
    }

    fun attachSessionReportsCsv() = viewModelScope.launch {
        val path = repository.exportSessionReportsCsv()
        addAttachment(path, "Dołączono CSV raportów")
    }

    fun attachPayrollPackage() = viewModelScope.launch {
        if (contactsCache.isEmpty()) {
            _uiState.value = _uiState.value.copy(actionMessage = "Brak kontaktów do przygotowania paczki płac")
            return@launch
        }
        val path = repository.exportPayrollPackage(contactsCache)
        addAttachment(path, "Dołączono paczkę płac")
    }

    fun clearAttachments() {
        if (mailingJob?.isActive == true) {
            _uiState.value = _uiState.value.copy(actionMessage = "Nie można czyścić załączników podczas aktywnej wysyłki")
            return
        }
        _uiState.value = _uiState.value.copy(
            attachmentPaths = emptyList(),
            attachmentCount = 0,
            actionMessage = "Załączniki wyczyszczone",
            progressLabel = "Gotowy",
            isMailingRunning = false,
            isMailingPaused = false,
        )
    }

    fun sendSingle() {
        val attachments = _uiState.value.attachmentPaths
        if (attachments.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                actionMessage = "Najpierw dołącz lokalny eksport jako załącznik",
                progressLabel = "Brak załączników",
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMailingRunning = true, actionMessage = null, progressLabel = "Wysyłanie podglądu...")
            runCatching {
                repository.sendSinglePreviewMail(attachments)
            }.onSuccess { mailbox ->
                _uiState.value = _uiState.value.copy(
                    isMailingRunning = false,
                    actionMessage = "Wysłano testowy podgląd na skrzynkę SMTP: $mailbox",
                    progressLabel = "Podgląd wysłany",
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isMailingRunning = false,
                    actionMessage = error.message ?: "Nie udało się wysłać podglądu",
                    progressLabel = "Błąd wysyłki podglądu",
                )
            }
        }
    }

    fun startMassMailing() {
        if (mailingJob?.isActive == true) {
            _uiState.value = _uiState.value.copy(actionMessage = "Wysyłka już trwa")
            return
        }
        val hasRecipients = _uiState.value.totalRecipients > 0
        val hasAttachments = _uiState.value.attachmentPaths.isNotEmpty()
        if (!hasRecipients || !hasAttachments) {
            _uiState.value = _uiState.value.copy(
                isMailingRunning = false,
                progressLabel = when {
                    !hasRecipients -> "Brak odbiorców w kontaktach"
                    else -> "Brak załączników do masowej wysyłki"
                },
                actionMessage = when {
                    !hasRecipients -> "Dodaj kontakty przed uruchomieniem wysyłki"
                    else -> "Dołącz co najmniej jeden lokalny eksport"
                },
            )
            return
        }
        mailingPaused = false
        mailingJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isMailingRunning = true,
                isMailingPaused = false,
                isCancellingMailing = false,
                actionMessage = null,
                progressLabel = "Trwa realna wysyłka do ${_uiState.value.totalRecipients} kontaktów...",
            )
            runCatching {
                repository.sendMassMailing(
                    attachmentPaths = _uiState.value.attachmentPaths,
                    autoMode = _uiState.value.autoSend,
                    onProgress = ::handleMailProgress,
                    awaitResume = ::awaitMailResume,
                    awaitApproval = ::awaitMailApproval,
                )
            }.onSuccess { result ->
                _uiState.value = _uiState.value.copy(
                    isMailingRunning = false,
                    isMailingPaused = false,
                    isCancellingMailing = false,
                    isAwaitingMailApproval = false,
                    pendingApprovalRecipientName = "",
                    pendingApprovalRecipientEmail = "",
                    progressLabel = "Sesja zakończona: OK ${result.ok} / Błędy ${result.fail} / Skip ${result.skip}",
                    actionMessage = if (result.details.isBlank()) {
                        "Brak szczegółów sesji"
                    } else {
                        result.details.lineSequence().take(4).joinToString("\n")
                    },
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isMailingRunning = false,
                    isMailingPaused = false,
                    isCancellingMailing = false,
                    isAwaitingMailApproval = false,
                    pendingApprovalRecipientName = "",
                    pendingApprovalRecipientEmail = "",
                    progressLabel = if (error is CancellationException) "Masowa wysyłka anulowana" else "Masowa wysyłka przerwana",
                    actionMessage = if (error is CancellationException) {
                        "Operator anulował masową wysyłkę"
                    } else {
                        error.message ?: "Nie udało się uruchomić masowej wysyłki"
                    },
                )
            }
            mailingJob = null
            mailingPaused = false
            pendingApprovalDecision = null
        }
    }

    fun togglePauseMailing() {
        val running = mailingJob?.isActive == true
        if (!running) {
            _uiState.value = _uiState.value.copy(
                isMailingRunning = false,
                isMailingPaused = false,
                progressLabel = "Brak aktywnej wysyłki",
                actionMessage = "Najpierw uruchom wysyłkę",
            )
            return
        }
        mailingPaused = !mailingPaused
        _uiState.value = _uiState.value.copy(
            isMailingRunning = true,
            isMailingPaused = mailingPaused,
            isCancellingMailing = false,
            progressLabel = if (mailingPaused) "Wysyłka wstrzymana" else "Wysyłka wznowiona — oczekiwanie na kolejny element",
            actionMessage = if (mailingPaused) "Kolejka została wstrzymana" else "Kolejka została wznowiona",
        )
    }

    fun cancelMailing() {
        val activeJob = mailingJob
        if (activeJob?.isActive != true) {
            _uiState.value = _uiState.value.copy(
                isCancellingMailing = false,
                actionMessage = "Brak aktywnej wysyłki do anulowania",
            )
            return
        }
        mailingPaused = false
        pendingApprovalDecision?.cancel()
        _uiState.value = _uiState.value.copy(
            isCancellingMailing = true,
            isAwaitingMailApproval = false,
            pendingApprovalRecipientName = "",
            pendingApprovalRecipientEmail = "",
            actionMessage = "Trwa anulowanie aktywnej wysyłki...",
            progressLabel = "Anulowanie kolejki",
        )
        activeJob.cancel(CancellationException("Wysyłka anulowana przez operatora"))
    }

    fun updateRecipientQuery(value: String) {
        _uiState.value = _uiState.value.copy(
            recipientQuery = value,
            filteredRecipients = filterRecipients(contactsCache, value),
            actionMessage = null,
        )
    }

    fun toggleSpecialRecipient(item: ContactListItem) {
        val key = selectionKey(item)
        val updated = _uiState.value.selectedRecipientKeys.let { current ->
            if (key in current) current - key else current + key
        }
        _uiState.value = _uiState.value.copy(selectedRecipientKeys = updated, actionMessage = null)
    }

    fun isRecipientSelected(item: ContactListItem): Boolean = selectionKey(item) in _uiState.value.selectedRecipientKeys

    fun selectVisibleRecipients() {
        val visibleKeys = _uiState.value.filteredRecipients.map(::selectionKey)
        if (visibleKeys.isEmpty()) {
            _uiState.value = _uiState.value.copy(actionMessage = "Brak widocznych odbiorców do zaznaczenia")
            return
        }
        _uiState.value = _uiState.value.copy(
            selectedRecipientKeys = _uiState.value.selectedRecipientKeys + visibleKeys,
            actionMessage = "Zaznaczono ${visibleKeys.size} odbiorców",
        )
    }

    fun clearSpecialRecipients() {
        _uiState.value = _uiState.value.copy(selectedRecipientKeys = emptySet(), actionMessage = "Wyczyszczono listę odbiorców")
    }

    fun updateSpecialSubject(value: String) {
        _uiState.value = _uiState.value.copy(specialSubject = value, actionMessage = null)
    }

    fun updateSpecialBody(value: String) {
        _uiState.value = _uiState.value.copy(specialBody = value, actionMessage = null)
    }

    fun loadTemplateIntoSpecial() {
        _uiState.value = _uiState.value.copy(
            specialSubject = templateCache.subject,
            specialBody = templateCache.body,
            actionMessage = "Załadowano zapisany szablon do wysyłki specjalnej",
        )
    }

    fun sendSpecial() {
        if (mailingJob?.isActive == true) {
            _uiState.value = _uiState.value.copy(actionMessage = "Poczekaj na zakończenie bieżącej wysyłki")
            return
        }
        val recipients = contactsCache.filter { selectionKey(it) in _uiState.value.selectedRecipientKeys }
        if (recipients.isEmpty()) {
            _uiState.value = _uiState.value.copy(actionMessage = "Zaznacz co najmniej jednego odbiorcę")
            return
        }
        if (_uiState.value.specialSubject.isBlank()) {
            _uiState.value = _uiState.value.copy(actionMessage = "Uzupełnij temat wiadomości specjalnej")
            return
        }
        if (_uiState.value.specialBody.isBlank()) {
            _uiState.value = _uiState.value.copy(actionMessage = "Uzupełnij treść wiadomości specjalnej")
            return
        }
        mailingPaused = false
        mailingJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isMailingRunning = true,
                isMailingPaused = false,
                isCancellingMailing = false,
                actionMessage = null,
                progressLabel = "Specjalna wysyłka do ${recipients.size} odbiorców...",
            )
            runCatching {
                repository.sendSpecialMailing(
                    recipients = recipients,
                    attachmentPaths = _uiState.value.attachmentPaths,
                    subject = _uiState.value.specialSubject,
                    body = _uiState.value.specialBody,
                    onProgress = ::handleMailProgress,
                    awaitResume = ::awaitMailResume,
                )
            }.onSuccess { result ->
                _uiState.value = _uiState.value.copy(
                    isMailingRunning = false,
                    isMailingPaused = false,
                    isCancellingMailing = false,
                    isAwaitingMailApproval = false,
                    pendingApprovalRecipientName = "",
                    pendingApprovalRecipientEmail = "",
                    progressLabel = "Specjalna sesja zakończona: OK ${result.ok} / Błędy ${result.fail} / Skip ${result.skip}",
                    actionMessage = if (result.details.isBlank()) {
                        "Brak szczegółów sesji"
                    } else {
                        result.details.lineSequence().take(4).joinToString("\n")
                    },
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isMailingRunning = false,
                    isMailingPaused = false,
                    isCancellingMailing = false,
                    isAwaitingMailApproval = false,
                    pendingApprovalRecipientName = "",
                    pendingApprovalRecipientEmail = "",
                    progressLabel = if (error is CancellationException) "Specjalna wysyłka anulowana" else "Specjalna wysyłka przerwana",
                    actionMessage = if (error is CancellationException) {
                        "Operator anulował wysyłkę specjalną"
                    } else {
                        error.message ?: "Nie udało się uruchomić wysyłki specjalnej"
                    },
                )
            }
            mailingJob = null
            mailingPaused = false
            pendingApprovalDecision = null
        }
    }

    fun resolvePendingApproval(approved: Boolean) {
        val deferred = pendingApprovalDecision ?: return
        if (deferred.isActive) {
            deferred.complete(approved)
        }
        _uiState.value = _uiState.value.copy(
            isAwaitingMailApproval = false,
            pendingApprovalRecipientName = "",
            pendingApprovalRecipientEmail = "",
            actionMessage = if (approved) "Operator zatwierdził wysyłkę" else "Operator pominął odbiorcę",
            progressLabel = if (approved) "Wysyłanie zatwierdzone — trwa dalej" else "Odbiorca pominięty przez operatora",
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

    private suspend fun handleMailProgress(progress: com.future.ultimate.core.common.repository.MailDispatchProgress) {
        val prefix = if (_uiState.value.isMailingPaused) "Wstrzymano po" else "Przetworzono"
        _uiState.value = _uiState.value.copy(
            progressLabel = "$prefix ${progress.processed}/${progress.total} • OK ${progress.ok} / Błędy ${progress.fail} / Skip ${progress.skip}",
            actionMessage = if (progress.currentRecipient.isBlank()) null else "Ostatni odbiorca: ${progress.currentRecipient}",
        )
    }

    private suspend fun awaitMailResume() {
        while (mailingPaused) {
            delay(200)
        }
    }

    private suspend fun awaitMailApproval(request: MailApprovalRequest): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        pendingApprovalDecision = deferred
        _uiState.value = _uiState.value.copy(
            isAwaitingMailApproval = true,
            pendingApprovalRecipientName = request.recipientName,
            pendingApprovalRecipientEmail = request.recipientEmail,
            actionMessage = "Oczekiwanie na decyzję operatora",
            progressLabel = "Weryfikacja odbiorcy przed wysyłką",
        )
        return try {
            deferred.await()
        } finally {
            if (pendingApprovalDecision === deferred) {
                pendingApprovalDecision = null
            }
        }
    }

    private fun filterRecipients(items: List<ContactListItem>, query: String): List<ContactListItem> {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isBlank()) return items
        return items.filter { item ->
            listOf(item.name, item.surname, item.email, item.workplace, item.phone)
                .joinToString(" ")
                .lowercase()
                .contains(normalizedQuery)
        }
    }

    private fun selectionKey(item: ContactListItem): String =
        "${item.name.trim().lowercase()}|${item.surname.trim().lowercase()}|${item.email.trim().lowercase()}"

    private fun formatMoney(value: Double): String = String.format(java.util.Locale.US, "%.2f", value)

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

    fun toggleSelection(item: com.future.ultimate.core.common.repository.ContactListItem) {
        val key = item.selectionKey()
        val current = _uiState.value.selectedContactKeys
        _uiState.value = _uiState.value.copy(
            selectedContactKeys = if (key in current) current - key else current + key,
            exportMessage = null,
        )
    }

    fun selectVisible(items: List<com.future.ultimate.core.common.repository.ContactListItem>) {
        if (items.isEmpty()) return
        _uiState.value = _uiState.value.copy(
            selectedContactKeys = _uiState.value.selectedContactKeys + items.map { it.selectionKey() },
            exportMessage = null,
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedContactKeys = emptySet(), exportMessage = null)
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

    fun exportPackage() = viewModelScope.launch {
        val selected = _uiState.value.items.filter { it.selectionKey() in _uiState.value.selectedContactKeys }
        if (selected.isEmpty()) {
            _uiState.value = _uiState.value.copy(exportMessage = "Najpierw wybierz co najmniej jeden rekord do paczki")
            return@launch
        }
        _uiState.value = _uiState.value.copy(isExporting = true, exportMessage = null)
        val path = repository.exportPayrollPackage(selected)
        _uiState.value = _uiState.value.copy(
            isExporting = false,
            exportMessage = if (path.isBlank()) "Nie udało się przygotować paczki eksportowej" else "Paczka płac zapisana: $path",
        )
    }

    private fun com.future.ultimate.core.common.repository.ContactListItem.selectionKey(): String =
        "${name.trim().lowercase()}|${surname.trim().lowercase()}"
}

class WorkersViewModel(private val repository: AdminRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkersUiState())
    val uiState: StateFlow<WorkersUiState> = _uiState.asStateFlow()

    init { repository.observeWorkers().onEach { _uiState.value = _uiState.value.copy(items = it) }.launchIn(viewModelScope) }
    fun updateQuery(value: String) { _uiState.value = _uiState.value.copy(query = value) }
    fun updateEditor(draft: WorkerDraft) { _uiState.value = _uiState.value.copy(editor = draft) }
    fun edit(worker: com.future.ultimate.core.common.repository.WorkerListItem) {
        _uiState.value = _uiState.value.copy(
            editor = WorkerDraft(
                id = worker.id,
                name = worker.name,
                surname = worker.surname,
                plant = worker.plant,
                phone = worker.phone,
                position = worker.position,
                hireDate = worker.hireDate,
            ),
        )
    }
    fun clearEditor() { _uiState.value = _uiState.value.copy(editor = WorkerDraft()) }
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
    fun edit(plant: com.future.ultimate.core.common.repository.PlantListItem) {
        _uiState.value = _uiState.value.copy(
            editor = PlantDraft(
                id = plant.id,
                name = plant.name,
                city = plant.city,
                address = plant.address,
                contactPhone = plant.contactPhone,
                notes = plant.notes,
            ),
        )
    }
    fun clearEditor() { _uiState.value = _uiState.value.copy(editor = PlantDraft()) }
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
    fun updateImportText(value: String) { _uiState.value = _uiState.value.copy(importText = value, actionMessage = null) }
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
            showOnlyPendingItems = false,
            importText = "",
            importPreview = emptyList(),
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

    fun stageImportRows() {
        val rows = parseImportRows(_uiState.value.importText)
        _uiState.value = _uiState.value.copy(
            importPreview = rows,
            actionMessage = if (rows.isEmpty()) "Nie udało się sparsować żadnych pozycji importu" else "Przygotowano ${rows.size} pozycji do importu",
        )
    }

    fun clearImport() {
        _uiState.value = _uiState.value.copy(importText = "", importPreview = emptyList(), actionMessage = null)
    }

    fun applyImportedRows() = viewModelScope.launch {
        val orderId = _uiState.value.selectedOrderId ?: return@launch
        val rows = _uiState.value.importPreview.ifEmpty { parseImportRows(_uiState.value.importText) }
        if (rows.isEmpty()) {
            _uiState.value = _uiState.value.copy(actionMessage = "Brak pozycji do importu")
            return@launch
        }
        val importedCount = repository.importClothesOrderItems(orderId, rows)
        _uiState.value = _uiState.value.copy(
            importText = "",
            importPreview = emptyList(),
            actionMessage = "Zaimportowano $importedCount pozycji do zamówienia",
        )
    }

    fun togglePendingItemsFilter() {
        _uiState.value = _uiState.value.copy(
            showOnlyPendingItems = !_uiState.value.showOnlyPendingItems,
            actionMessage = null,
        )
    }

    fun deleteOrder(orderId: Long) = viewModelScope.launch {
        if (_uiState.value.selectedOrderId == orderId) {
            orderItemsJob?.cancel()
        }
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
        val order = _uiState.value.items.find { it.id == orderId }
        if (order == null) return@launch
        if (_uiState.value.selectedOrderId == orderId && _uiState.value.selectedOrderItems.isEmpty()) {
            _uiState.value = _uiState.value.copy(actionMessage = "Dodaj co najmniej jedną pozycję przed oznaczeniem zamówienia jako Zamówione")
            return@launch
        }
        if (!canMarkClothesOrderOrdered(order.status)) {
            _uiState.value = _uiState.value.copy(actionMessage = "Nie można cofnąć zamówienia wydanego lub częściowo wydanego do statusu Zamówione")
            return@launch
        }
        repository.markClothesOrderOrdered(orderId)
        _uiState.value = _uiState.value.copy(actionMessage = "Status zmieniony na Zamówione")
    }

    fun issueItem(id: Long) = viewModelScope.launch {
        val orderId = _uiState.value.selectedOrderId ?: return@launch
        val order = _uiState.value.items.find { it.id == orderId } ?: return@launch
        val item = _uiState.value.selectedOrderItems.find { it.id == id }
        if (!canIssueClothesOrder(order.status)) {
            _uiState.value = _uiState.value.copy(actionMessage = "Najpierw oznacz zamówienie jako Zamówione")
            return@launch
        }
        if (item?.issued == true) {
            _uiState.value = _uiState.value.copy(actionMessage = "Ta pozycja została już wydana")
            return@launch
        }
        repository.issueClothesOrderItem(id)
        _uiState.value = _uiState.value.copy(actionMessage = "Pozycja wydana")
    }

    fun issueAll(orderId: Long) = viewModelScope.launch {
        val order = _uiState.value.items.find { it.id == orderId }
        if (order == null) return@launch
        if (!canIssueClothesOrder(order.status)) {
            _uiState.value = _uiState.value.copy(actionMessage = "Najpierw oznacz zamówienie jako Zamówione")
            return@launch
        }
        if (_uiState.value.selectedOrderId == orderId && _uiState.value.selectedOrderItems.none { !it.issued }) {
            _uiState.value = _uiState.value.copy(actionMessage = "Brak niewydanych pozycji do wydania")
            return@launch
        }
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
        if (items.isEmpty()) {
            emptyList()
        } else {
            listOf(
                "Łącznie: ${items.sumOf { it.qty }} • wydane: ${items.filter { it.issued }.sumOf { it.qty }} • niewydane: ${items.filterNot { it.issued }.sumOf { it.qty }}",
            ) + items.groupBy { it.item.trim() to it.size.trim().ifBlank { "-" } }
                .toList()
                .sortedWith(compareBy({ it.first.first.lowercase() }, { it.first.second.lowercase() }))
                .map { (key, groupedItems) ->
                    "${key.first} • rozmiar: ${key.second} • suma: ${groupedItems.sumOf { it.qty }}"
                }
        }

    private fun parseImportRows(rawInput: String): List<ClothesOrderImportRow> =
        rawInput.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split('\t', ';', ',').map { it.trim() }
                if (parts.size < 3) {
                    null
                } else {
                    ClothesOrderImportRow(
                        name = parts.getOrElse(0) { "" },
                        surname = parts.getOrElse(1) { "" },
                        item = parts.getOrElse(2) { "" },
                        size = parts.getOrElse(3) { "" },
                        qty = parts.getOrElse(4) { "1" },
                    )
                }
            }
            .filterNot { row ->
                val firstCell = row.name.lowercase()
                firstCell.contains("imi") || firstCell.contains("name")
            }
            .toList()

    private fun canIssueClothesOrder(status: String): Boolean {
        val normalized = status.trim().lowercase()
        return normalized == "zamówione" || normalized == "częściowo wydane"
    }

    private fun canMarkClothesOrderOrdered(status: String): Boolean {
        val normalized = status.trim().lowercase()
        return normalized != "częściowo wydane" && normalized != "wydane"
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

    fun validate() = viewModelScope.launch {
        val current = _uiState.value.settings
        _uiState.value = _uiState.value.copy(message = "Trwa test połączenia SMTP...")
        runCatching {
            repository.validateSmtpConnection(current)
        }.onSuccess {
            _uiState.value = _uiState.value.copy(message = "Połączenie SMTP zakończone powodzeniem")
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(message = error.message ?: "Test SMTP nie powiódł się")
        }
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
        repository.observeDriverRemoteSettings().onEach { remoteSettings ->
            _uiState.value = _uiState.value.copy(remoteSettings = remoteSettings)
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

    fun importDatabaseWorkbook(
        fileName: String?,
        mimeType: String?,
        bytes: ByteArray,
    ) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isImportingDatabase = true, actionMessage = "Trwa import bazy z Excela...")
        runCatching {
            repository.importDatabaseWorkbook(
                fileName = fileName,
                mimeType = mimeType,
                bytes = bytes,
            )
        }.onSuccess { result ->
            _uiState.value = _uiState.value.copy(
                isImportingDatabase = false,
                actionMessage = "Import zakończony: kontakty ${result.contactsImported}, pracownicy ${result.workersImported}, zakłady ${result.plantsImported}, rozmiary ${result.clothesSizesImported}.",
            )
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(
                isImportingDatabase = false,
                actionMessage = error.message ?: "Import bazy z Excela nie powiódł się",
            )
        }
    }

    fun updateDriverRemoteApiUrl(value: String) {
        _uiState.value = _uiState.value.copy(
            remoteSettings = _uiState.value.remoteSettings.copy(apiUrl = value),
            actionMessage = null,
        )
    }

    fun saveDriverRemoteSettings() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isSavingRemoteSettings = true, actionMessage = null)
        repository.saveDriverRemoteSettings(_uiState.value.remoteSettings)
        _uiState.value = _uiState.value.copy(
            isSavingRemoteSettings = false,
            actionMessage = "Ustawienia zdalnej integracji zapisane",
        )
    }

    fun validateDriverRemoteSettings() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isValidatingRemoteSettings = true, actionMessage = "Trwa walidacja endpointu...")
        runCatching {
            repository.validateDriverRemoteSettings(_uiState.value.remoteSettings)
        }.onSuccess { message ->
            _uiState.value = _uiState.value.copy(
                isValidatingRemoteSettings = false,
                actionMessage = message,
            )
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(
                isValidatingRemoteSettings = false,
                actionMessage = error.message ?: "Walidacja endpointu nie powiodła się",
            )
        }
    }
}

class AdminViewModelFactory(private val repository: AdminRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
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
