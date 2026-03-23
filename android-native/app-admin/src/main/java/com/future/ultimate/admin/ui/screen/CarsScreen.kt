package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
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

@Composable
fun CarsScreen() {
    val app = LocalContext.current.applicationContext as AdminApp
    val viewModel: CarsViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var isDialogOpen by remember { mutableStateOf(false) }
    var editedCarId by remember { mutableStateOf<Long?>(null) }
    var detailsCar by remember { mutableStateOf<CarListItem?>(null) }
    var assignDriverCar by remember { mutableStateOf<CarListItem?>(null) }
    var driverPickerQuery by remember { mutableStateOf("") }

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
    val filteredKnownDrivers = remember(uiState.knownCarDrivers, uiState.query, uiState.items) {
        uiState.knownCarDrivers.filter { driverName ->
            val currentAssignments = uiState.items.filter { it.driver.equals(driverName, ignoreCase = true) }
                .joinToString(", ") { it.registration }
            val blob = "$driverName $currentAssignments".lowercase()
            uiState.query.isBlank() || uiState.query.lowercase() in blob
        }
    }
    val filteredContactDrivers = remember(uiState.contactDriverSuggestions, driverPickerQuery) {
        uiState.contactDriverSuggestions.filter { suggestion ->
            driverPickerQuery.isBlank() || suggestion.lowercase().contains(driverPickerQuery.lowercase())
        }
    }

    ScreenColumn("Samochody", "Moduł aut i kierowców") {
        item {
            SectionCard(title = "Widok modułu", subtitle = "Przełączaj się między listą aut a historią kierowców przypisanych do samochodów.") {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Auta") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Kierowcy") })
                }
            }
        }

        item {
            SectionCard(
                title = if (selectedTab == 0) "Wyszukiwarka aut" else "Wyszukiwarka kierowców",
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
                items(filteredKnownDrivers) { driverName ->
                    val currentAssignments = uiState.items.filter { it.driver.equals(driverName, ignoreCase = true) }
                    SectionCard(
                        title = driverName,
                        subtitle = if (currentAssignments.isEmpty()) {
                            "Brak aktywnie przypisanego auta"
                        } else {
                            "Aktualnie przypisane: ${currentAssignments.joinToString(", ") { it.registration }}"
                        },
                    ) {
                        Text(
                            if (currentAssignments.isEmpty()) {
                                "Kierowca znajduje się w historii przypisań samochodów."
                            } else {
                                "Liczba aktywnych aut: ${currentAssignments.size}"
                            },
                        )
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
            onQueryChange = { driverPickerQuery = it },
            onDismiss = {
                assignDriverCar = null
                driverPickerQuery = ""
            },
            onDriverSelected = { driverName ->
                viewModel.assignDriver(car.id, driverName)
                assignDriverCar = null
                driverPickerQuery = ""
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
        Button(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
            Text(if (actionInFlightId == car.id) "Usuwanie..." else "Usuń auto")
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
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onDriverSelected: (String) -> Unit,
) {
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
                        SectionCard(
                            title = driverName,
                            subtitle = "Dostępny z kontaktów",
                        ) {
                            Button(onClick = { onDriverSelected(driverName) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Przypisz do ${car.registration}")
                            }
                        }
                    }
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
