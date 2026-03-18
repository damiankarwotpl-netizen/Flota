package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.future.ultimate.admin.AdminApp
import com.future.ultimate.admin.ui.viewmodel.AdminViewModelFactory
import com.future.ultimate.admin.ui.viewmodel.ClothesSizesViewModel

@Composable
fun ClothesScreen() {
    val selected = remember { mutableIntStateOf(0) }
    val tabs = listOf("Rozmiary", "Zamówienia", "Raporty")
    val app = LocalContext.current.applicationContext as AdminApp
    val viewModel: ClothesSizesViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenColumn("Ubranie robocze", "Moduły odzieżowe 1:1") {
        item {
            Column {
                TabRow(selectedTabIndex = selected.intValue) {
                    tabs.forEachIndexed { index, title ->
                        Tab(selected = selected.intValue == index, onClick = { selected.intValue = index }, text = { Text(title) })
                    }
                }
                when (selected.intValue) {
                    0 -> {
                        OutlinedTextField(uiState.query, viewModel::updateQuery, label = { Text("Szukaj pracownika") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(uiState.editor.name, { viewModel.updateEditor(uiState.editor.copy(name = it)) }, label = { Text("Imię") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(uiState.editor.surname, { viewModel.updateEditor(uiState.editor.copy(surname = it)) }, label = { Text("Nazwisko") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(uiState.editor.plant, { viewModel.updateEditor(uiState.editor.copy(plant = it)) }, label = { Text("Zakład") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(uiState.editor.shirt, { viewModel.updateEditor(uiState.editor.copy(shirt = it)) }, label = { Text("Koszulka") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(uiState.editor.hoodie, { viewModel.updateEditor(uiState.editor.copy(hoodie = it)) }, label = { Text("Bluza") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(uiState.editor.pants, { viewModel.updateEditor(uiState.editor.copy(pants = it)) }, label = { Text("Spodnie") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(uiState.editor.jacket, { viewModel.updateEditor(uiState.editor.copy(jacket = it)) }, label = { Text("Kurtka") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(uiState.editor.shoes, { viewModel.updateEditor(uiState.editor.copy(shoes = it)) }, label = { Text("Buty") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) { Text(if (uiState.isSaving) "Zapisywanie..." else "Dodaj rozmiar pracownika") }
                    }
                    1 -> {
                        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Nowe zamówienie") }
                        Text("Lista zamówień i statusów")
                    }
                    else -> {
                        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Export CSV") }
                        Text("Raporty wydanych ubrań i statystyki")
                    }
                }
            }
        }
        if (selected.intValue == 0) {
            uiState.items.filter {
                val blob = "${it.name} ${it.surname} ${it.plant} ${it.shirt} ${it.hoodie} ${it.pants} ${it.jacket} ${it.shoes}".lowercase()
                uiState.query.isBlank() || uiState.query.lowercase() in blob
            }.forEach { itemData ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${itemData.name} ${itemData.surname} • ${itemData.plant}")
                            Text("Koszulka: ${itemData.shirt} • Bluza: ${itemData.hoodie}")
                            Text("Spodnie: ${itemData.pants} • Kurtka: ${itemData.jacket} • Buty: ${itemData.shoes}")
                            Button(onClick = { viewModel.delete(itemData.id) }, modifier = Modifier.fillMaxWidth()) { Text("Usuń rozmiar") }
                        }
                    }
                }
            }
        }
    }
}
