package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.future.ultimate.admin.AdminApp
import com.future.ultimate.admin.ui.viewmodel.AdminViewModelFactory
import com.future.ultimate.admin.ui.viewmodel.CarsViewModel
import com.future.ultimate.core.common.ui.CarsServiceFilter

@Composable
fun CarsScreen() {
    val app = LocalContext.current.applicationContext as AdminApp
    val viewModel: CarsViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val urgentCars = uiState.items.count { it.remainingToService <= 0 }
    val dueSoonCars = uiState.items.count { it.remainingToService in 1..3000 }
    val okCars = uiState.items.count { it.remainingToService > 3000 }

    ScreenColumn("Samochody", "Flota pojazdów • szybkie akcje") {
        item {
            Column {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::updateQuery,
                    label = { Text("Szukaj: nazwa / rejestracja / kierowca") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    }
                }
                OutlinedTextField(
                    value = uiState.editor.name,
                    onValueChange = { viewModel.updateEditor(uiState.editor.copy(name = it)) },
                    label = { Text("Nazwa samochodu") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = uiState.editor.registration,
                    onValueChange = { viewModel.updateEditor(uiState.editor.copy(registration = it)) },
                    label = { Text("Rejestracja") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = uiState.editor.driver,
                    onValueChange = { viewModel.updateEditor(uiState.editor.copy(driver = it)) },
                    label = { Text("Kierowca") },
                    modifier = Modifier.fillMaxWidth(),
                )
                uiState.driverSuggestions
                    .filter { suggestion ->
                        uiState.editor.driver.isNotBlank() &&
                            suggestion.lowercase().contains(uiState.editor.driver.lowercase()) &&
                            !suggestion.equals(uiState.editor.driver, ignoreCase = true)
                    }
                    .take(5)
                    .forEach { suggestion ->
                        Button(
                            onClick = { viewModel.applyEditorDriverSuggestion(suggestion) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Użyj kierowcy: $suggestion")
                        }
                    }
                OutlinedTextField(
                    value = uiState.editor.serviceInterval,
                    onValueChange = { viewModel.updateEditor(uiState.editor.copy(serviceInterval = it)) },
                    label = { Text("Interwał serwisowy") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (uiState.isSaving) {
                            "Zapisywanie..."
                        } else if (uiState.editor.id == null) {
                            "+ DODAJ SAMOCHÓD"
                        } else {
                            "ZAPISZ ZMIANY SAMOCHODU"
                        },
                    )
                }
                if (uiState.editor.id != null) {
                    Button(onClick = viewModel::clearEditor, modifier = Modifier.fillMaxWidth()) { Text("Anuluj edycję samochodu") }
                }
                uiState.actionMessage?.let { Text(it) }
            }
        }
        val filteredCars = uiState.items
            .filter {
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
        if (filteredCars.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Brak samochodów dla bieżącego filtra.")
                        Text("Zmień wyszukiwanie albo filtr serwisowy, aby zobaczyć rekordy.")
                    }
                }
            }
        }
        filteredCars.forEach { car ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val serviceStatus = serviceStatusLabel(car.remainingToService)
                            Text("${car.name} • ${car.registration}")
                            Text("Kierowca: ${car.driver.ifBlank { "nieprzypisany" }}")
                            Text("Przebieg: ${car.mileage} km • ${serviceDistanceLabel(car.remainingToService)}")
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
                            Button(onClick = { viewModel.editCar(car) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Edytuj samochód")
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
                                value = uiState.driverDrafts[car.id].orEmpty(),
                                onValueChange = { viewModel.updateDriverDraft(car.id, it) },
                                label = { Text("Zmień kierowcę") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            uiState.driverSuggestions
                                .filter { suggestion ->
                                    uiState.driverDrafts[car.id].orEmpty().isNotBlank() &&
                                        suggestion.lowercase().contains(uiState.driverDrafts[car.id].orEmpty().lowercase()) &&
                                        !suggestion.equals(uiState.driverDrafts[car.id].orEmpty(), ignoreCase = true)
                                }
                                .take(3)
                                .forEach { suggestion ->
                                    Button(
                                        onClick = { viewModel.applyDriverDraftSuggestion(car.id, suggestion) },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("Podpowiedź: $suggestion")
                                    }
                                }
                            Button(
                                onClick = { viewModel.saveDriver(car.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (uiState.actionInFlightId == car.id) "Zapisywanie..." else "Zapisz kierowcę")
                            }
                            OutlinedTextField(
                                value = uiState.mileageDrafts[car.id].orEmpty(),
                                onValueChange = { viewModel.updateMileageDraft(car.id, it) },
                                label = { Text("Nowy przebieg") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                onClick = { viewModel.saveMileage(car.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (uiState.actionInFlightId == car.id) "Zapisywanie..." else "Zapisz przebieg")
                            }
                            Button(
                                onClick = { viewModel.resetDriverCredentials(car.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (uiState.actionInFlightId == car.id) "Zapisywanie..." else "Resetuj dane kierowcy")
                            }
                            Button(
                                onClick = { viewModel.retryRemoteDriverSync(car.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (uiState.actionInFlightId == car.id) "Synchronizowanie..." else "Ponów zdalny sync kierowcy")
                            }
                            Button(
                                onClick = { viewModel.confirmService(car.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (uiState.actionInFlightId == car.id) "Zapisywanie..." else "Potwierdź serwis")
                            }
                            Button(
                                onClick = { viewModel.deleteCar(car.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (uiState.actionInFlightId == car.id) "Usuwanie..." else "Usuń samochód")
                            }
                        }
                    }
                }
            }
    }
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
