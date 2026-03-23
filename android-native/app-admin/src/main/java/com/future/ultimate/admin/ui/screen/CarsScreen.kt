package com.future.ultimate.admin.ui.screen

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    var isDialogOpen by remember { mutableStateOf(false) }
    var editedCarId by remember { mutableStateOf<Long?>(null) }

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

    ScreenColumn("Samochody", "Szukaj pojazdów i wykonuj szybkie akcje bez opuszczania listy.") {
        item {
            SectionCard(title = "Wyszukiwarka", subtitle = "Szukaj po nazwie, rejestracji lub kierowcy oraz filtruj status serwisu.") {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::updateQuery,
                    label = { Text("Szukaj samochodu") },
                    modifier = Modifier.fillMaxWidth(),
                )
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
                uiState.actionMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

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
                        driverDraft = uiState.driverDrafts[car.id].orEmpty(),
                        mileageDraft = uiState.mileageDrafts[car.id].orEmpty(),
                        driverSuggestions = uiState.driverSuggestions,
                        actionInFlightId = uiState.actionInFlightId,
                        onEdit = {
                            editedCarId = car.id
                            viewModel.editCar(car)
                            isDialogOpen = true
                        },
                        onDriverDraftChange = { viewModel.updateDriverDraft(car.id, it) },
                        onMileageDraftChange = { viewModel.updateMileageDraft(car.id, it) },
                        onApplyDriverSuggestion = { viewModel.applyDriverDraftSuggestion(car.id, it) },
                        onSaveDriver = { viewModel.saveDriver(car.id) },
                        onSaveMileage = { viewModel.saveMileage(car.id) },
                        onResetDriverCredentials = { viewModel.resetDriverCredentials(car.id) },
                        onRetryRemoteDriverSync = { viewModel.retryRemoteDriverSync(car.id) },
                        onConfirmService = { viewModel.confirmService(car.id) },
                        onDelete = { viewModel.deleteCar(car.id) },
                    )
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
}

@Composable
private fun CarCard(
    car: CarListItem,
    driverDraft: String,
    mileageDraft: String,
    driverSuggestions: List<String>,
    actionInFlightId: Long?,
    onEdit: () -> Unit,
    onDriverDraftChange: (String) -> Unit,
    onMileageDraftChange: (String) -> Unit,
    onApplyDriverSuggestion: (String) -> Unit,
    onSaveDriver: () -> Unit,
    onSaveMileage: () -> Unit,
    onResetDriverCredentials: () -> Unit,
    onRetryRemoteDriverSync: () -> Unit,
    onConfirmService: () -> Unit,
    onDelete: () -> Unit,
) {
    val serviceStatus = serviceStatusLabel(car.remainingToService)

    SectionCard(
        title = "${car.name} • ${car.registration}",
        subtitle = "${serviceStatus} • ${serviceDistanceLabel(car.remainingToService)}",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            FilledIconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edytuj samochód",
                )
            }
        }
        Text("Kierowca: ${car.driver.ifBlank { "nieprzypisany" }}")
        Text("Przebieg: ${car.mileage} km")
        Text("Status serwisu: $serviceStatus")
        Text(
            buildString {
                append("Sync przebiegu: ")
                append(car.lastMileageSyncStatus.ifBlank { "brak danych" })
                if (car.pendingMileageSync) {
                    append(" • w kolejce: ${car.queuedMileage ?: "-"} km")
                }
            },
        )
        if (car.lastMileageSyncAt.isNotBlank()) {
            Text("Ostatnia synchronizacja przebiegu: ${car.lastMileageSyncAt}")
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

        OutlinedTextField(
            value = driverDraft,
            onValueChange = onDriverDraftChange,
            label = { Text("Zmień kierowcę") },
            modifier = Modifier.fillMaxWidth(),
        )
        driverSuggestions
            .filter { suggestion ->
                driverDraft.isNotBlank() &&
                    suggestion.lowercase().contains(driverDraft.lowercase()) &&
                    !suggestion.equals(driverDraft, ignoreCase = true)
            }
            .take(3)
            .forEach { suggestion ->
                Button(onClick = { onApplyDriverSuggestion(suggestion) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Podpowiedź: $suggestion")
                }
            }
        Button(onClick = onSaveDriver, modifier = Modifier.fillMaxWidth()) {
            Text(if (actionInFlightId == car.id) "Zapisywanie..." else "Zapisz kierowcę")
        }

        OutlinedTextField(
            value = mileageDraft,
            onValueChange = onMileageDraftChange,
            label = { Text("Nowy przebieg") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = onSaveMileage, modifier = Modifier.fillMaxWidth()) {
            Text(if (actionInFlightId == car.id) "Zapisywanie..." else "Zapisz przebieg")
        }
        Button(onClick = onResetDriverCredentials, modifier = Modifier.fillMaxWidth()) {
            Text(if (actionInFlightId == car.id) "Zapisywanie..." else "Resetuj dane kierowcy")
        }
        Button(onClick = onRetryRemoteDriverSync, modifier = Modifier.fillMaxWidth()) {
            Text(if (actionInFlightId == car.id) "Synchronizowanie..." else "Ponów zdalny sync kierowcy")
        }
        Button(onClick = onConfirmService, modifier = Modifier.fillMaxWidth()) {
            Text(if (actionInFlightId == car.id) "Zapisywanie..." else "Potwierdź serwis")
        }
        Button(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
            Text(if (actionInFlightId == car.id) "Usuwanie..." else "Usuń samochód")
        }
    }
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
