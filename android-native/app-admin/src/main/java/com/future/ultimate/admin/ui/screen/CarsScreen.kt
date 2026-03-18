package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
        items(
            uiState.items
                .filter {
                    val blob = "${it.name} ${it.registration} ${it.driver}".lowercase()
                    uiState.query.isBlank() || uiState.query.lowercase() in blob
                }
                .map { "${it.name} | ${it.registration} | ${it.driver} | do serwisu: ${it.remainingToService} km" },
        )
    }
}
