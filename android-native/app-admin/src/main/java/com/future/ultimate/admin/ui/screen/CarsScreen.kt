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

@Composable
fun CarsScreen() {
    val app = LocalContext.current.applicationContext as AdminApp
    val viewModel: CarsViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenColumn("Samochody", "Flota pojazdów • szybkie akcje") {
        item {
            Column {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::updateQuery,
                    label = { Text("Szukaj: nazwa / rejestracja / kierowca") },
                    modifier = Modifier.fillMaxWidth(),
                )
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
                OutlinedTextField(
                    value = uiState.editor.serviceInterval,
                    onValueChange = { viewModel.updateEditor(uiState.editor.copy(serviceInterval = it)) },
                    label = { Text("Interwał serwisowy") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                    Text(if (uiState.isSaving) "Zapisywanie..." else "+ DODAJ SAMOCHÓD")
                }
            }
        }
        uiState.items
            .filter {
                val blob = "${it.name} ${it.registration} ${it.driver}".lowercase()
                uiState.query.isBlank() || uiState.query.lowercase() in blob
            }
            .forEach { car ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("${car.name} • ${car.registration}")
                            Text("Kierowca: ${car.driver.ifBlank { "nieprzypisany" }}")
                            Text("Przebieg: ${car.mileage} km • do serwisu: ${car.remainingToService} km")
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
