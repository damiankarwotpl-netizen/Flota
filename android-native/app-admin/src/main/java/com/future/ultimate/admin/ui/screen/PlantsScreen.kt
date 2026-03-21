package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import com.future.ultimate.admin.ui.viewmodel.PlantsViewModel

@Composable
fun PlantsScreen() {
    val app = LocalContext.current.applicationContext as AdminApp
    val viewModel: PlantsViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenColumn("Zakłady", "Szybkie wyszukiwanie") {
        item {
            Column {
                OutlinedTextField(uiState.query, viewModel::updateQuery, label = { Text("Szukaj zakładu") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(uiState.editor.name, { viewModel.updateEditor(uiState.editor.copy(name = it)) }, label = { Text("Nazwa zakładu") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(uiState.editor.city, { viewModel.updateEditor(uiState.editor.copy(city = it)) }, label = { Text("Miasto") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(uiState.editor.address, { viewModel.updateEditor(uiState.editor.copy(address = it)) }, label = { Text("Adres") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(uiState.editor.contactPhone, { viewModel.updateEditor(uiState.editor.copy(contactPhone = it)) }, label = { Text("Telefon") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(uiState.editor.notes, { viewModel.updateEditor(uiState.editor.copy(notes = it)) }, label = { Text("Notatki") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (uiState.isSaving) {
                            "Zapisywanie..."
                        } else if (uiState.editor.id == null) {
                            "Dodaj zakład"
                        } else {
                            "Zapisz zmiany zakładu"
                        },
                    )
                }
                if (uiState.editor.id != null) {
                    Button(onClick = viewModel::clearEditor, modifier = Modifier.fillMaxWidth()) { Text("Anuluj edycję") }
                }
            }
        }
        uiState.items
            .filter {
                val blob = "${it.name} ${it.city} ${it.address} ${it.contactPhone} ${it.notes}".lowercase()
                uiState.query.isBlank() || uiState.query.lowercase() in blob
            }
            .forEach { plant ->
                item {
                    SectionCard(
                        title = plant.name,
                        subtitle = plant.city.ifBlank { "Brak miasta" },
                    ) {
                        if (plant.address.isNotBlank()) {
                            Text("Adres: ${plant.address}")
                        }
                        Text("Telefon: ${plant.contactPhone.ifBlank { "Brak numeru" }}")
                        if (plant.notes.isNotBlank()) {
                            Text("Notatki: ${plant.notes}")
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { viewModel.edit(plant) },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            ) {
                                Text("Edytuj zakład")
                            }
                        }
                    }
                }
            }
    }
}
