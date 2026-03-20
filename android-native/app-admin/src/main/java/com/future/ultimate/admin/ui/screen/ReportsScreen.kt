package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.future.ultimate.admin.AdminApp
import com.future.ultimate.admin.ui.viewmodel.AdminViewModelFactory
import com.future.ultimate.admin.ui.viewmodel.ReportsViewModel

@Composable
fun ReportsScreen() {
    val app = LocalContext.current.applicationContext as AdminApp
    val viewModel: ReportsViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val query = remember { mutableStateOf("") }
    val filteredItems = uiState.items.filter {
        val blob = "${it.date} ${it.details} ${it.ok} ${it.fail} ${it.skip}".lowercase()
        query.value.isBlank() || query.value.lowercase() in blob
    }
    val totalOk = filteredItems.sumOf { it.ok }
    val totalFail = filteredItems.sumOf { it.fail }
    val totalSkip = filteredItems.sumOf { it.skip }

    ScreenColumn("Historia sesji", "Wyszukiwanie, eksport i szybki podgląd wyników wysyłek") {
        item {
            SectionCard(
                title = "Filtry i eksport",
                subtitle = "Wyszukaj konkretną sesję albo pobierz dane w CSV.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = viewModel::exportCsv, modifier = Modifier.fillMaxWidth()) {
                        Text(if (uiState.isExporting) "Eksportowanie..." else "Eksport CSV raportów")
                    }
                    uiState.exportMessage?.let { Text(it) }
                    OutlinedTextField(
                        value = query.value,
                        onValueChange = { query.value = it },
                        label = { Text("Filtruj po dacie lub logach") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        item {
            SectionCard(
                title = "Podsumowanie",
                subtitle = "Agregacja aktualnie przefiltrowanych wyników.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Sesje: ${filteredItems.size}")
                    Text("Łącznie OK: $totalOk • Błędy: $totalFail • Pominięte: $totalSkip")
                    Text(filteredItems.firstOrNull()?.let { "Najnowsza sesja: ${it.date}" } ?: "Najnowsza sesja: brak")
                }
            }
        }
        if (filteredItems.isEmpty()) {
            item {
                SectionCard(
                    title = "Brak wyników",
                    subtitle = "Nie znaleziono sesji pasujących do bieżącego filtra.",
                ) {
                    Text("Wyczyść filtr lub wykonaj nową synchronizację, aby zapełnić listę raportów.")
                }
            }
        }
        filteredItems.forEach { itemData ->
            item {
                SectionCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(0.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Sesja: ${itemData.date}")
                        Text("OK: ${itemData.ok} • Błędy: ${itemData.fail} • Pominięte: ${itemData.skip}")
                        Text(if (itemData.details.isBlank()) "Brak logów" else itemData.details)
                    }
                }
            }
        }
    }
}
