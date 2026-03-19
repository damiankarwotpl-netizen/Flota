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
import com.future.ultimate.admin.ui.viewmodel.TableViewModel

@Composable
fun TableScreen() {
    val app = LocalContext.current.applicationContext as AdminApp
    val viewModel: TableViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredItems = uiState.items.filter { item ->
        val blob = "${item.name} ${item.surname} ${item.email} ${item.phone} ${item.workplace} ${item.apartment} ${item.notes}".lowercase()
        uiState.query.isBlank() || uiState.query.lowercase() in blob
    }

    ScreenColumn("Podgląd i eksport", "Tabela kontaktów do eksportu") {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::updateQuery,
                    label = { Text("Szukaj w tabeli...") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = viewModel::exportCsv, modifier = Modifier.fillMaxWidth()) {
                    Text(if (uiState.isExporting) "Eksportowanie..." else "Eksport CSV kontaktów")
                }
                Button(onClick = { viewModel.selectVisible(filteredItems) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Zaznacz rekordy z bieżącego filtra")
                }
                Button(onClick = viewModel::exportPackage, modifier = Modifier.fillMaxWidth()) {
                    Text(if (uiState.isExporting) "Eksportowanie..." else "Eksport paczki płac")
                }
                if (uiState.selectedContactKeys.isNotEmpty()) {
                    Button(onClick = viewModel::clearSelection, modifier = Modifier.fillMaxWidth()) {
                        Text("Wyczyść wybór (${uiState.selectedContactKeys.size})")
                    }
                }
                Text("Wybrane do paczki: ${uiState.selectedContactKeys.size}")
                Text("Kolumny: imię, nazwisko, email, telefon, miejsce pracy, mieszkanie, notatki")
                uiState.exportMessage?.let { Text(it) }
            }
        }
        filteredItems.forEach { itemData ->
            item {
                val isSelected = "${itemData.name.trim().lowercase()}|${itemData.surname.trim().lowercase()}" in uiState.selectedContactKeys
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("${itemData.name} ${itemData.surname}".trim())
                        Text("Email: ${itemData.email.ifBlank { "-" }}")
                        Text("Telefon: ${itemData.phone.ifBlank { "-" }}")
                        Text("Miejsce pracy: ${itemData.workplace.ifBlank { "-" }}")
                        Text("Mieszkanie: ${itemData.apartment.ifBlank { "-" }}")
                        if (itemData.notes.isNotBlank()) {
                            Text("Notatki: ${itemData.notes}")
                        }
                        Button(onClick = { viewModel.toggleSelection(itemData) }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (isSelected) "Usuń z paczki płac" else "Dodaj do paczki płac")
                        }
                        Button(onClick = { viewModel.exportRow(itemData) }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (uiState.isExporting) "Eksportowanie..." else "Zapisz XLSX rekordu")
                        }
                    }
                }
            }
        }
    }
}
