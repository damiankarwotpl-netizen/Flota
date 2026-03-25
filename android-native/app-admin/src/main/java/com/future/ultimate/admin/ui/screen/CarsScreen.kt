package com.future.ultimate.admin.ui.screen

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.future.ultimate.admin.AdminApp
import com.future.ultimate.admin.ui.viewmodel.AdminViewModelFactory
import com.future.ultimate.admin.ui.viewmodel.CarsViewModel
import com.future.ultimate.core.common.model.CarDraft
import com.future.ultimate.core.common.repository.CarListItem
import com.future.ultimate.core.common.ui.CarsServiceFilter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CarsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as AdminApp
    val viewModel: CarsViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var isDialogOpen by remember { mutableStateOf(false) }
    var editedCarId by remember { mutableStateOf<Long?>(null) }
    var detailsCar by remember { mutableStateOf<CarListItem?>(null) }
    var assignDriverCar by remember { mutableStateOf<CarListItem?>(null) }
    var driverPickerQuery by remember { mutableStateOf("") }
    var pendingAssignmentConflict by remember { mutableStateOf<DriverAssignmentConflict?>(null) }
    var pendingMultiDriverConfirmation by remember { mutableStateOf<MultiDriverAssignmentConfirmation?>(null) }

    val urgentCars = uiState.items.count { it.remainingToService <= 0 }
    val dueSoonCars = uiState.items.count { it.remainingToService in 1..3000 }
    val okCars = uiState.items.count { it.remainingToService > 3000 }
    val filteredCars = remember(uiState.items, uiState.query, uiState.serviceFilter) {
        uiState.items.filter {
            val serviceStatus = serviceStatusLabel(it.remainingToService)
            val blob = "${it.name} ${it.registration} ${it.driver} ${it.driverLogin} $serviceStatus ${it.remainingToService}".lowercase()
            val matchesQuery = uiState.query.isBlank() || uiState.query.lowercase() in blob
            val matchesService = when (uiState.serviceFilter) {
                CarsServiceFilter.All -> true
                CarsServiceFilter.DueSoon -> carNeedsService(it.remainingToService)
                CarsServiceFilter.Urgent -> it.remainingToService <= 0
            }
            matchesQuery && matchesService
        }
    }
    val assignmentsByDriver = remember(uiState.items) {
        uiState.items
            .flatMap { car ->
                car.driver.assignedDrivers().map { driverName -> driverName.lowercase() to car }
            }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
    }
    val filteredKnownDrivers = remember(uiState.knownCarDrivers, uiState.query, uiState.items) {
        uiState.knownCarDrivers.filter { driverName ->
            val currentAssignments = assignmentsByDriver[driverName.lowercase()].orEmpty()
            .joinToString(", ") { it.registration }
            val blob = "$driverName $currentAssignments".lowercase()
            uiState.query.isBlank() || uiState.query.lowercase() in blob
        }
    }
    val assignableDrivers = remember(uiState.contactDriverSuggestions, uiState.knownCarDrivers) {
        (uiState.contactDriverSuggestions + uiState.knownCarDrivers)
            .groupBy { it.trim().lowercase() }
            .mapNotNull { (_, values) -> values.firstOrNull()?.trim()?.takeIf { it.isNotBlank() } }
            .sorted()
    }
    val filteredContactDrivers = remember(assignableDrivers, driverPickerQuery) {
        assignableDrivers.filter { suggestion ->
            driverPickerQuery.isBlank() || suggestion.lowercase().contains(driverPickerQuery.lowercase())
        }
    }

    ScreenColumn("Samochody", "Moduł aut i kierowców") {
        item {
            SectionCard {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Auta") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Kierowcy") })
                }
            }
        }

        item {
            SectionCard(
                subtitle = if (selectedTab == 0) {
                    "Szukaj po nazwie, rejestracji lub kierowcy oraz filtruj status serwisu."
                } else {
                    "Lista kierowców, którzy mają albo mieli przypisane auto."
                },
            ) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::updateQuery,
                    label = { Text(if (selectedTab == 0) "Szukaj samochodu" else "Szukaj kierowcy") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (selectedTab == 0) {
                    Text("Serwis — pilne: $urgentCars • wkrótce: $dueSoonCars • OK: $okCars")
                    Button(onClick = viewModel::cycleServiceFilter, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            when (uiState.serviceFilter) {
                                CarsServiceFilter.All -> "Filtr serwisu: wszystkie auta"
                                CarsServiceFilter.DueSoon -> "Filtr serwisu: do serwisu wkrótce"
                                CarsServiceFilter.Urgent -> "Filtr serwisu: tylko pilne"
                            },
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        FilledIconButton(
                            onClick = {
                                viewModel.clearEditor()
                                editedCarId = null
                                isDialogOpen = true
                            },
                            modifier = Modifier.size(42.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Dodaj nowy samochód",
                            )
                        }
                    }
                }
                uiState.actionMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        if (selectedTab == 0) {
            if (filteredCars.isEmpty()) {
                item {
                    SectionCard {
                        Text(
                            text = "Brak samochodów pasujących do wyszukiwania lub filtra serwisu.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            } else {
                filteredCars.forEach { car ->
                    item {
                        CarCard(
                            car = car,
                            actionInFlightId = uiState.actionInFlightId,
                            onDetails = { detailsCar = car },
                            onEdit = {
                                editedCarId = car.id
                                viewModel.editCar(car)
                                isDialogOpen = true
                            },
                            onAssignDriver = {
                                assignDriverCar = car
                                driverPickerQuery = ""
                            },
                            onConfirmService = { viewModel.confirmService(car.id) },
                            onConfirmInspection = { viewModel.confirmInspection(car.id) },
                            onDelete = { viewModel.deleteCar(car.id) },
                        )
                    }
                }
            }
        } else {
            if (filteredKnownDrivers.isEmpty()) {
                item {
                    SectionCard {
                        Text(
                            text = "Brak kierowców pasujących do wyszukiwania.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            } else {
                filteredKnownDrivers.forEach { driverName ->
                    item {
                        val currentAssignments = assignmentsByDriver[driverName.lowercase()].orEmpty()
                        SectionCard(title = driverName) {
                            Text(
                                if (currentAssignments.isEmpty()) {
                                    "Brak aktywnie przypisanego auta."
                                } else {
                                    "Aktywne auta: ${currentAssignments.joinToString(", ") { it.registration }}"
                                },
                            )
                            Text("Liczba aktywnych aut: ${currentAssignments.size}")
                            if (currentAssignments.isEmpty()) {
                                Text("Login: brak")
                                Text("Hasło startowe: brak")
                            } else {
                                val primaryAssignment = currentAssignments.first()
                                Text("Login: ${primaryAssignment.driverLogin.ifBlank { "brak" }}")
                                Text(
                                    when {
                                        primaryAssignment.driverPassword.isBlank() -> "Hasło startowe: brak"
                                        primaryAssignment.changePasswordRequired -> "Hasło startowe: ${primaryAssignment.driverPassword}"
                                        else -> "Hasło startowe zostało już zmienione"
                                    },
                                )
                                Text("Typ prawa jazdy: ${primaryAssignment.licenseType.ifBlank { "PL" }}")
                                Text("Data ważności prawa jazdy: ${formatDateLabel(primaryAssignment.licenseValidUntil, "Brak daty")}")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Button(
                                        onClick = { viewModel.updateDriverLicense(primaryAssignment.id, "PL", primaryAssignment.licenseValidUntil) },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("PL")
                                    }
                                    Button(
                                        onClick = { viewModel.updateDriverLicense(primaryAssignment.id, "MIĘDZYNARODOWE", primaryAssignment.licenseValidUntil) },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("MIĘDZYNARODOWE")
                                    }
                                }
                                Button(
                                    onClick = {
                                        showDatePicker(context, primaryAssignment.licenseValidUntil) {
                                            viewModel.updateDriverLicense(
                                                primaryAssignment.id,
                                                primaryAssignment.licenseType.ifBlank { "PL" },
                                                it,
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Ustaw datę ważności prawa jazdy")
                                }
                                Button(
                                    onClick = { viewModel.resetDriverCredentials(primaryAssignment.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        if (uiState.actionInFlightId == primaryAssignment.id) {
                                            "Resetowanie hasła..."
                                        } else {
                                            "Resetuj hasło kierowcy"
                                        },
                                    )
                                }
                            }
                            Button(
                                onClick = { viewModel.deleteKnownDriver(driverName, currentAssignments.firstOrNull()?.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    if (currentAssignments.any { uiState.actionInFlightId == it.id }) {
                                        "Usuwanie kierowcy..."
                                    } else {
                                        "Usuń kierowcę"
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isDialogOpen) {
        AddCarDialog(
            draft = uiState.editor,
            isSaving = uiState.isSaving,
            isEditing = editedCarId != null,
            driverSuggestions = uiState.driverSuggestions,
            onDraftChange = viewModel::updateEditor,
            onApplyDriverSuggestion = viewModel::applyEditorDriverSuggestion,
            onDismiss = {
                isDialogOpen = false
                editedCarId = null
                viewModel.clearEditor()
            },
            onSave = {
                viewModel.save()
                isDialogOpen = false
                editedCarId = null
            },
        )
    }

    detailsCar?.let { car ->
        CarDetailsDialog(car = car, onDismiss = { detailsCar = null })
    }

    assignDriverCar?.let { car ->
        DriverAssignmentDialog(
            car = car,
            query = driverPickerQuery,
            availableDrivers = filteredContactDrivers,
            allDrivers = assignableDrivers,
            onQueryChange = { driverPickerQuery = it },
            onDismiss = {
                assignDriverCar = null
                driverPickerQuery = ""
            },
            onAssignDrivers = { selectedDrivers ->
                val normalizedDrivers = selectedDrivers
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinctBy { it.lowercase() }
                if (normalizedDrivers.isEmpty()) {
                    assignDriverCar = null
                    driverPickerQuery = ""
                    return@DriverAssignmentDialog
                }
                if (normalizedDrivers.size >= 2) {
                    pendingMultiDriverConfirmation = MultiDriverAssignmentConfirmation(
                        carId = car.id,
                        registration = car.registration,
                        selectedDrivers = normalizedDrivers,
                    )
                } else {
                    val normalizedDriver = normalizedDrivers.first()
                    val otherAssignments = uiState.items.filter {
                        it.id != car.id && it.driver.assignedDrivers().any { assigned ->
                            assigned.equals(normalizedDriver, ignoreCase = true)
                        }
                    }
                    val currentDriver = car.driver.assignedDrivers().firstOrNull {
                        !it.equals(normalizedDriver, ignoreCase = true)
                    }
                    if (otherAssignments.isNotEmpty() || currentDriver != null) {
                        pendingAssignmentConflict = DriverAssignmentConflict(
                            carId = car.id,
                            registration = car.registration,
                            selectedDriver = normalizedDriver,
                            currentDriver = currentDriver,
                            otherRegistrations = otherAssignments.map { it.registration },
                        )
                    } else {
                        viewModel.assignDriverAllowConflict(car.id, normalizedDriver)
                    }
                }
                assignDriverCar = null
                driverPickerQuery = ""
            },
        )
    }

    pendingMultiDriverConfirmation?.let { confirmation ->
        MultiDriverAssignmentConfirmationDialog(
            confirmation = confirmation,
            onDismiss = { pendingMultiDriverConfirmation = null },
            onConfirm = {
                viewModel.assignDriverAllowConflict(
                    confirmation.carId,
                    confirmation.selectedDrivers.joinToString(", "),
                )
                pendingMultiDriverConfirmation = null
            },
        )
    }

    pendingAssignmentConflict?.let { conflict ->
        DriverAssignmentConflictDialog(
            conflict = conflict,
            onDismiss = { pendingAssignmentConflict = null },
            onAllow = {
                viewModel.assignDriverAllowConflict(conflict.carId, conflict.selectedDriver)
                pendingAssignmentConflict = null
            },
            onEnforceSingle = {
                viewModel.assignDriverEnforceSingle(conflict.carId, conflict.selectedDriver)
                pendingAssignmentConflict = null
            },
        )
    }
}

@Composable
private fun CarCard(
    car: CarListItem,
    actionInFlightId: Long?,
    onDetails: () -> Unit,
    onEdit: () -> Unit,
    onAssignDriver: () -> Unit,
    onConfirmService: () -> Unit,
    onConfirmInspection: () -> Unit,
    onDelete: () -> Unit,
) {
    val serviceStatus = serviceStatusLabel(car.remainingToService)

    SectionCard(
        title = "${car.name} • ${car.registration}",
        subtitle = serviceDistanceLabel(car.remainingToService),
    ) {
        Text("Kierowca: ${car.driver.ifBlank { "nieprzypisany" }}")
        Text("Przebieg: ${car.mileage} km")
        Text("Status serwisu: $serviceStatus")
        Text("Ostatni przegląd: ${formatDateLabel(car.lastInspectionDate, "Brak daty")}")
        Text("Następny przegląd: ${formatDateLabel(car.nextInspectionDate, "Brak daty")}")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onDetails, modifier = Modifier.weight(1f)) {
                Text("Szczegóły")
            }
            FilledIconButton(
                onClick = onEdit,
                modifier = Modifier.size(42.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edytuj samochód",
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onAssignDriver, modifier = Modifier.weight(1f)) {
                Text("Przypisz kierowcę")
            }
            Button(onClick = onConfirmService, modifier = Modifier.weight(1f)) {
                Text(if (actionInFlightId == car.id) "Zapisywanie..." else "Potwierdź serwis")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onConfirmInspection, modifier = Modifier.weight(1f)) {
                Text(if (actionInFlightId == car.id) "Zapisywanie..." else "Potwierdź przegląd")
            }
            Button(onClick = onDelete, modifier = Modifier.weight(1f)) {
                Text(if (actionInFlightId == car.id) "Usuwanie..." else "Usuń auto")
            }
        }
    }
}

@Composable
private fun CarDetailsDialog(
    car: CarListItem,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Szczegóły auta", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Nazwa: ${car.name}")
                Text("Rejestracja: ${car.registration}")
                Text("Kierowca: ${car.driver.ifBlank { "nieprzypisany" }}")
                Text("Interwał serwisowy: ${car.serviceInterval} km")
                Text("Ostatni serwis przy: ${car.lastService} km")
                Text("Ostatni przegląd: ${formatDateLabel(car.lastInspectionDate, "Brak daty")}")
                Text("Następny przegląd: ${formatDateLabel(car.nextInspectionDate, "Brak daty")}")
                Text("Sync przebiegu: ${car.lastMileageSyncStatus.ifBlank { "brak danych" }}")
                if (car.pendingMileageSync) {
                    Text("Przebieg oczekujący w kolejce: ${car.queuedMileage ?: "-"} km")
                }
                if (car.lastMileageSyncAt.isNotBlank()) {
                    Text("Ostatni sync przebiegu: ${car.lastMileageSyncAt}")
                }
                Text("Zdalny sync kierowcy: ${car.remoteDriverSyncStatus.ifBlank { "brak danych" }}")
                if (car.remoteDriverSyncAt.isNotBlank()) {
                    Text("Ostatni zdalny sync kierowcy: ${car.remoteDriverSyncAt}")
                }
                if (car.remoteDriverSyncError.isNotBlank()) {
                    Text("Błąd zdalnego syncu: ${car.remoteDriverSyncError}")
                }
                if (car.driverLogin.isNotBlank()) {
                    Text("Login kierowcy: ${car.driverLogin}")
                    Text("Typ prawa jazdy: ${car.licenseType.ifBlank { "PL" }}")
                    Text("Ważność prawa jazdy: ${formatDateLabel(car.licenseValidUntil, "Brak daty")}")
                    Text(
                        if (car.changePasswordRequired) {
                            "Hasło startowe: ${car.driverPassword}"
                        } else {
                            "Hasło zostało już zmienione przez kierowcę"
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zamknij")
            }
        },
    )
}

@Composable
private fun DriverAssignmentDialog(
    car: CarListItem,
    query: String,
    availableDrivers: List<String>,
    allDrivers: List<String>,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onAssignDrivers: (List<String>) -> Unit,
) {
    val initiallySelected = remember(car.id) {
        car.driver.assignedDrivers().map { it.lowercase() }.toSet()
    }
    var selectedDrivers by remember(car.id) { mutableStateOf(initiallySelected) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Przypisz kierowcę • ${car.registration}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text("Szukaj kierowcy z kontaktów") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (availableDrivers.isEmpty()) {
                    Text("Brak kierowców pasujących do wyszukiwania.")
                } else {
                    availableDrivers.take(20).forEach { driverName ->
                        val normalized = driverName.trim().lowercase()
                        val isSelected = normalized in selectedDrivers
                        SectionCard(
                            title = driverName,
                            subtitle = if (isSelected) "Wybrany" else "Dostępny z kontaktów",
                        ) {
                            Button(
                                onClick = {
                                    selectedDrivers = if (isSelected) {
                                        selectedDrivers - normalized
                                    } else {
                                        selectedDrivers + normalized
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (isSelected) "Odznacz" else "Wybierz")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedNames = allDrivers.filter { it.trim().lowercase() in selectedDrivers }
                    onAssignDrivers(selectedNames)
                },
                enabled = selectedDrivers.isNotEmpty(),
            ) {
                Text("Przypisz (${selectedDrivers.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        },
    )
}

@Composable
private fun AddCarDialog(
    draft: CarDraft,
    isSaving: Boolean,
    isEditing: Boolean,
    driverSuggestions: List<String>,
    onDraftChange: (CarDraft) -> Unit,
    onApplyDriverSuggestion: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val context = LocalContext.current
    val isSaveEnabled = draft.name.isNotBlank() && draft.registration.isNotBlank() && (draft.serviceInterval.toIntOrNull()?.let { it > 0 } == true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edytuj samochód" else "Nowy samochód",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { onDraftChange(draft.copy(name = it)) },
                    label = { Text("Nazwa samochodu *") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.registration,
                    onValueChange = { onDraftChange(draft.copy(registration = it)) },
                    label = { Text("Rejestracja *") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.driver,
                    onValueChange = { onDraftChange(draft.copy(driver = it)) },
                    label = { Text("Kierowca") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!isEditing) {
                    OutlinedTextField(
                        value = draft.initialMileage,
                        onValueChange = { onDraftChange(draft.copy(initialMileage = it)) },
                        label = { Text("Pierwszy przebieg (km)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                driverSuggestions
                    .filter { suggestion ->
                        draft.driver.isNotBlank() &&
                            suggestion.lowercase().contains(draft.driver.lowercase()) &&
                            !suggestion.equals(draft.driver, ignoreCase = true)
                    }
                    .take(5)
                    .forEach { suggestion ->
                        Button(onClick = { onApplyDriverSuggestion(suggestion) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Użyj kierowcy: $suggestion")
                        }
                    }
                OutlinedTextField(
                    value = draft.serviceInterval,
                    onValueChange = { onDraftChange(draft.copy(serviceInterval = it)) },
                    label = { Text("Interwał serwisowy *") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.lastInspectionDate,
                    onValueChange = {},
                    label = { Text("Ostatni przegląd") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showDatePicker(context, draft.lastInspectionDate) {
                                onDraftChange(draft.copy(lastInspectionDate = it))
                            }
                        },
                    readOnly = true,
                )
                TextButton(
                    onClick = {
                        showDatePicker(context, draft.lastInspectionDate) {
                            onDraftChange(draft.copy(lastInspectionDate = it))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Wybierz datę przeglądu")
                }
                TextButton(onClick = { onDraftChange(draft.copy(lastInspectionDate = "")) }) {
                    Text("Wyczyść datę przeglądu")
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = isSaveEnabled && !isSaving) {
                Text(if (isSaving) "Zapisywanie..." else if (isEditing) "Zapisz zmiany" else "Dodaj samochód")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        },
    )
}

private fun carNeedsService(remainingToService: Int): Boolean = remainingToService <= 3000

private fun serviceStatusLabel(remainingToService: Int): String = when {
    remainingToService <= 0 -> "Serwis pilny"
    remainingToService <= 3000 -> "Serwis wkrótce"
    else -> "OK"
}

private fun serviceDistanceLabel(remainingToService: Int): String = when {
    remainingToService < 0 -> "po serwisie spóźnione o ${-remainingToService} km"
    remainingToService == 0 -> "serwis wymagany teraz"
    else -> "do serwisu: $remainingToService km"
}


private data class DriverAssignmentConflict(
    val carId: Long,
    val registration: String,
    val selectedDriver: String,
    val currentDriver: String?,
    val otherRegistrations: List<String>,
)

private data class MultiDriverAssignmentConfirmation(
    val carId: Long,
    val registration: String,
    val selectedDrivers: List<String>,
)

@Composable
private fun DriverAssignmentConflictDialog(
    conflict: DriverAssignmentConflict,
    onDismiss: () -> Unit,
    onAllow: () -> Unit,
    onEnforceSingle: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Konflikt przypisania", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Wybrany kierowca: ${conflict.selectedDriver}")
                Text("Docelowe auto: ${conflict.registration}")
                conflict.currentDriver?.let {
                    Text("Auto ma już kierowcę: $it")
                }
                if (conflict.otherRegistrations.isNotEmpty()) {
                    Text("Kierowca jest już przypisany do: ${conflict.otherRegistrations.joinToString(", ")}")
                }
                Text("TAK = zostaw dodatkowe przypisania jak są. NIE = wymuś zasadę 1 kierowca = 1 samochód i usuń inne przypisania tego kierowcy.")
            }
        },
        confirmButton = {
            TextButton(onClick = onAllow) { Text("Tak") }
        },
        dismissButton = {
            TextButton(onClick = onEnforceSingle) { Text("Nie") }
        },
    )
}

@Composable
private fun MultiDriverAssignmentConfirmationDialog(
    confirmation: MultiDriverAssignmentConfirmation,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Potwierdź wielu kierowców", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Auto: ${confirmation.registration}")
                Text("Wybrani kierowcy: ${confirmation.selectedDrivers.joinToString(", ")}")
                Text("Czy na pewno chcesz przypisać 2 lub więcej kierowców do jednego auta?")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Tak, przypisz") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Nie") }
        },
    )
}

private fun showDatePicker(
    context: android.content.Context,
    initialDate: String,
    onDateSelected: (String) -> Unit,
) {
    val initial = initialDate.toLocalDateOrNull() ?: LocalDate.now()
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected(LocalDate.of(year, month + 1, dayOfMonth).toString())
        },
        initial.year,
        initial.monthValue - 1,
        initial.dayOfMonth,
    ).show()
}

private fun formatDateLabel(date: String, fallback: String = "Brak daty"): String =
    date.toLocalDateOrNull()?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        ?: if (date.isBlank()) fallback else date

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

private fun String.assignedDrivers(): List<String> = split(",")
    .map { it.trim() }
    .filter { it.isNotBlank() }
