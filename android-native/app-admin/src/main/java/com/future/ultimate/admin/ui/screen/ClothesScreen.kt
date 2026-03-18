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
import com.future.ultimate.admin.ui.viewmodel.ClothesOrdersViewModel
import com.future.ultimate.admin.ui.viewmodel.ClothesReportsViewModel
import com.future.ultimate.admin.ui.viewmodel.ClothesSizesViewModel

@Composable
fun ClothesScreen() {
    val selected = remember { mutableIntStateOf(0) }
    val tabs = listOf("Rozmiary", "Zamówienia", "Raporty")
    val app = LocalContext.current.applicationContext as AdminApp
    val sizesViewModel: ClothesSizesViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val sizesUiState by sizesViewModel.uiState.collectAsStateWithLifecycle()
    val ordersViewModel: ClothesOrdersViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val ordersUiState by ordersViewModel.uiState.collectAsStateWithLifecycle()
    val reportsViewModel: ClothesReportsViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val reportsUiState by reportsViewModel.uiState.collectAsStateWithLifecycle()

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
                        OutlinedTextField(sizesUiState.query, sizesViewModel::updateQuery, label = { Text("Szukaj pracownika") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(sizesUiState.editor.name, { sizesViewModel.updateEditor(sizesUiState.editor.copy(name = it)) }, label = { Text("Imię") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(sizesUiState.editor.surname, { sizesViewModel.updateEditor(sizesUiState.editor.copy(surname = it)) }, label = { Text("Nazwisko") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(sizesUiState.editor.plant, { sizesViewModel.updateEditor(sizesUiState.editor.copy(plant = it)) }, label = { Text("Zakład") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(sizesUiState.editor.shirt, { sizesViewModel.updateEditor(sizesUiState.editor.copy(shirt = it)) }, label = { Text("Koszulka") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(sizesUiState.editor.hoodie, { sizesViewModel.updateEditor(sizesUiState.editor.copy(hoodie = it)) }, label = { Text("Bluza") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(sizesUiState.editor.pants, { sizesViewModel.updateEditor(sizesUiState.editor.copy(pants = it)) }, label = { Text("Spodnie") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(sizesUiState.editor.jacket, { sizesViewModel.updateEditor(sizesUiState.editor.copy(jacket = it)) }, label = { Text("Kurtka") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(sizesUiState.editor.shoes, { sizesViewModel.updateEditor(sizesUiState.editor.copy(shoes = it)) }, label = { Text("Buty") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = sizesViewModel::save, modifier = Modifier.fillMaxWidth()) { Text(if (sizesUiState.isSaving) "Zapisywanie..." else "Dodaj rozmiar pracownika") }
                    }
                    1 -> {
                        OutlinedTextField(ordersUiState.editor.date, { ordersViewModel.updateEditor(ordersUiState.editor.copy(date = it)) }, label = { Text("Data (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(ordersUiState.editor.plant, { ordersViewModel.updateEditor(ordersUiState.editor.copy(plant = it)) }, label = { Text("Zakład") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(ordersUiState.editor.status, { ordersViewModel.updateEditor(ordersUiState.editor.copy(status = it)) }, label = { Text("Status") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(ordersUiState.editor.orderDesc, { ordersViewModel.updateEditor(ordersUiState.editor.copy(orderDesc = it)) }, label = { Text("Opis zamówienia") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                        Button(onClick = ordersViewModel::save, modifier = Modifier.fillMaxWidth()) { Text(if (ordersUiState.isSaving) "Zapisywanie..." else "Nowe zamówienie") }
                        Text("Po zapisaniu zamówienia rozwiń kartę poniżej, aby dodać pozycje i oznaczyć status.")
                    }
                    else -> {
                        OutlinedTextField(
                            reportsUiState.year,
                            reportsViewModel::updateYear,
                            label = { Text("Rok statystyk") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text("Podsumowanie wydań dla roku ${reportsUiState.year.ifBlank { "----" }}")
                    }
                }
            }
        }
        when (selected.intValue) {
            0 -> sizesUiState.items.filter {
                val blob = "${it.name} ${it.surname} ${it.plant} ${it.shirt} ${it.hoodie} ${it.pants} ${it.jacket} ${it.shoes}".lowercase()
                sizesUiState.query.isBlank() || sizesUiState.query.lowercase() in blob
            }.forEach { itemData ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${itemData.name} ${itemData.surname} • ${itemData.plant}")
                            Text("Koszulka: ${itemData.shirt} • Bluza: ${itemData.hoodie}")
                            Text("Spodnie: ${itemData.pants} • Kurtka: ${itemData.jacket} • Buty: ${itemData.shoes}")
                            Button(onClick = { sizesViewModel.delete(itemData.id) }, modifier = Modifier.fillMaxWidth()) { Text("Usuń rozmiar") }
                        }
                    }
                }
            }
            1 -> ordersUiState.items.forEach { itemData ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${itemData.date} • ${itemData.plant.ifBlank { "Bez zakładu" }}")
                            Text("Status: ${itemData.status}")
                            Text(if (itemData.orderDesc.isBlank()) "Brak opisu" else itemData.orderDesc)
                            Button(onClick = { ordersViewModel.toggleOrderSelection(itemData.id) }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (ordersUiState.selectedOrderId == itemData.id) "Ukryj pozycje" else "Pokaż pozycje")
                            }
                            Button(onClick = { ordersViewModel.markOrdered(itemData.id) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Oznacz jako zamówione")
                            }
                            Button(onClick = { ordersViewModel.issueAll(itemData.id) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Wydaj wszystkie pozycje")
                            }
                            if (ordersUiState.selectedOrderId == itemData.id) {
                                OutlinedTextField(
                                    ordersUiState.itemEditor.name,
                                    { ordersViewModel.updateItemEditor(ordersUiState.itemEditor.copy(name = it)) },
                                    label = { Text("Imię pracownika") },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    ordersUiState.itemEditor.surname,
                                    { ordersViewModel.updateItemEditor(ordersUiState.itemEditor.copy(surname = it)) },
                                    label = { Text("Nazwisko pracownika") },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    ordersUiState.itemEditor.item,
                                    { ordersViewModel.updateItemEditor(ordersUiState.itemEditor.copy(item = it)) },
                                    label = { Text("Pozycja") },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    ordersUiState.itemEditor.size,
                                    { ordersViewModel.updateItemEditor(ordersUiState.itemEditor.copy(size = it)) },
                                    label = { Text("Rozmiar (opcjonalnie)") },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    ordersUiState.itemEditor.qty,
                                    { ordersViewModel.updateItemEditor(ordersUiState.itemEditor.copy(qty = it)) },
                                    label = { Text("Ilość") },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Text("Gdy rozmiar zostanie pusty, aplikacja spróbuje pobrać go z zapisanych rozmiarów pracownika.")
                                Button(onClick = ordersViewModel::saveItem, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (ordersUiState.isSavingItem) "Zapisywanie pozycji..." else "Dodaj pozycję")
                                }
                                ordersUiState.selectedOrderItems.forEach { orderItem ->
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("${orderItem.name} ${orderItem.surname}".trim().ifBlank { "Pracownik nieuzupełniony" })
                                            Text("${orderItem.item} • rozmiar: ${orderItem.size.ifBlank { "-" }} • ilość: ${orderItem.qty}")
                                            Text(if (orderItem.issued) "Status pozycji: wydane" else "Status pozycji: niewydane")
                                            Button(
                                                onClick = { ordersViewModel.issueItem(orderItem.id) },
                                                modifier = Modifier.fillMaxWidth(),
                                                enabled = !orderItem.issued,
                                            ) {
                                                Text(if (orderItem.issued) "Pozycja wydana" else "Wydaj pozycję")
                                            }
                                            Button(onClick = { ordersViewModel.deleteItem(orderItem.id) }, modifier = Modifier.fillMaxWidth()) {
                                                Text("Usuń pozycję")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                reportsUiState.yearlySummary.forEach { summary ->
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(summary)
                            }
                        }
                    }
                }
                reportsUiState.history.forEach { historyItem ->
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${historyItem.date} • ${historyItem.name} ${historyItem.surname}".trim())
                                Text("${historyItem.item} • rozmiar: ${historyItem.size.ifBlank { "-" }}")
                            }
                        }
                    }
                }
            }
        }
    }
}
