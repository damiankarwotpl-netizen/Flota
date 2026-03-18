package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.future.ultimate.admin.ui.viewmodel.ReportsViewModel

@Composable
fun ReportsScreen() {
    val app = LocalContext.current.applicationContext as AdminApp
    val viewModel: ReportsViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenColumn("Historia sesji", "Historia sesji i raporty z wysyłek") {
        item {
            Column {
                Button(onClick = viewModel::exportCsv, modifier = Modifier.fillMaxWidth()) {
                    Text(if (uiState.isExporting) "Eksportowanie..." else "Eksport CSV raportów")
                }
                uiState.exportMessage?.let { Text(it) }
            }
        }
        if (uiState.items.isEmpty()) {
            item { Text("Brak zapisanych raportów sesji.") }
        }
        uiState.items.forEach { itemData ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Sesja: ${itemData.date}")
                        Text("OK: ${itemData.ok} • Błędy: ${itemData.fail} • Pominięte: ${itemData.skip}")
                        Text(if (itemData.details.isBlank()) "Brak logów" else itemData.details)
                    }
                }
            }
        }
    }
}
