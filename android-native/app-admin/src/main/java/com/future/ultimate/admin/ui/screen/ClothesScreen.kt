package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
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
                        Button(onClick = sizesViewModel::save, modifier = Modifier.fillMaxWidth()) {
                            Text(if (sizesUiState.isSaving) "Zapisywanie..." else if (sizesUiState.editor.id == null) "Dodaj rozmiar pracownika" else "Zapisz zmiany rozmiaru")
                        }
                        if (sizesUiState.editor.id != null) {
                            Button(onClick = sizesViewModel::clearEditor, modifier = Modifier.fillMaxWidth()) { Text("Anuluj edycję rozmiaru") }
                        }
                    }
                    1 -> {
                        Text("Generator zamówienia startowego na bazie pracowników i zapisanych rozmiarów")
                        OutlinedTextField(ordersUiState.editor.date, { ordersViewModel.updateEditor(ordersUiState.editor.copy(date = it)) }, label = { Text("Data (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(ordersUiState.editor.plant, { ordersViewModel.updateEditor(ordersUiState.editor.copy(plant = it)) }, label = { Text("Zakład") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(ordersUiState.editor.status, { ordersViewModel.updateEditor(ordersUiState.editor.copy(status = it)) }, label = { Text("Status") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(ordersUiState.editor.orderDesc, { ordersViewModel.updateEditor(ordersUiState.editor.copy(orderDesc = it)) }, label = { Text("Opis zamówienia") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                        OutlinedTextField(
                            ordersUiState.workerQuery,
                            ordersViewModel::updateWorkerQuery,
                            label = { Text("Filtruj pracowników do zestawu startowego") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                ordersUiState.shirtQty,
                                { ordersViewModel.updateStarterQuantities(shirtQty = it) },
                                label = { Text("Koszulka") },
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                ordersUiState.hoodieQty,
                                { ordersViewModel.updateStarterQuantities(hoodieQty = it) },
                                label = { Text("Bluza") },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                ordersUiState.pantsQty,
                                { ordersViewModel.updateStarterQuantities(pantsQty = it) },
                                label = { Text("Spodnie") },
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                ordersUiState.jacketQty,
                                { ordersViewModel.updateStarterQuantities(jacketQty = it) },
                                label = { Text("Kurtka") },
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                ordersUiState.shoesQty,
                                { ordersViewModel.updateStarterQuantities(shoesQty = it) },
                                label = { Text("Buty") },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Text("Wybierz pracowników, a aplikacja utworzy pozycje Koszulka/Bluza/Spodnie/Kurtka/Buty z ich zapisanymi rozmiarami.")
                        Button(onClick = ordersViewModel::createStarterOrder, modifier = Modifier.fillMaxWidth()) {
                            Text(if (ordersUiState.isCreatingStarterOrder) "Tworzenie zamówienia..." else "Utwórz zamówienie startowe")
                        }
                        Button(onClick = ordersViewModel::save, modifier = Modifier.fillMaxWidth()) {
                            Text(if (ordersUiState.isSaving) "Zapisywanie..." else if (ordersUiState.editor.id == null) "Zapisz pusty nagłówek zamówienia" else "Zapisz zmiany zamówienia")
                        }
                        if (ordersUiState.editor.id != null) {
                            Button(onClick = ordersViewModel::clearOrderEditor, modifier = Modifier.fillMaxWidth()) { Text("Anuluj edycję zamówienia") }
                        }
                        if (ordersUiState.selectedWorkerIds.isNotEmpty()) {
                            Button(onClick = ordersViewModel::clearStarterSelection, modifier = Modifier.fillMaxWidth()) { Text("Wyczyść wybór pracowników") }
                        }
                        ordersUiState.actionMessage?.let { Text(it) }
                        Text("Po zapisaniu lub wygenerowaniu zamówienia rozwiń kartę poniżej, aby dodać pozycje ręcznie i oznaczyć status.")
                    }
                    else -> {
                        Button(onClick = reportsViewModel::exportCsv, modifier = Modifier.fillMaxWidth()) {
                            Text(if (reportsUiState.isExporting) "Eksportowanie..." else "Eksport CSV historii")
                        }
                        OutlinedTextField(
                            reportsUiState.year,
                            reportsViewModel::updateYear,
                            label = { Text("Rok statystyk") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            reportsUiState.workerQuery,
                            reportsViewModel::updateWorkerQuery,
                            label = { Text("Filtruj pracownika / pozycję") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        reportsUiState.exportMessage?.let { Text(it) }
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
                            Button(onClick = { sizesViewModel.edit(itemData) }, modifier = Modifier.fillMaxWidth()) { Text("Edytuj rozmiar") }
                            Button(onClick = { sizesViewModel.delete(itemData.id) }, modifier = Modifier.fillMaxWidth()) { Text("Usuń rozmiar") }
                        }
                    }
                }
            }
            1 -> {
                val workerFilter = ordersUiState.workerQuery.trim().lowercase()
                val visibleWorkers = ordersUiState.availableWorkers.filter { worker ->
                    workerFilter.isBlank() || listOf(worker.name, worker.surname, worker.plant).joinToString(" ").lowercase().contains(workerFilter)
                }
                val visibleWorkerIds = visibleWorkers.map { it.id }.toSet()
                val selectedVisibleCount = visibleWorkers.count { it.id in ordersUiState.selectedWorkerIds }
                val starterItemTemplates = listOf(
                    ordersUiState.shirtQty.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                    ordersUiState.hoodieQty.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                    ordersUiState.pantsQty.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                    ordersUiState.jacketQty.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                    ordersUiState.shoesQty.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                ).count { it > 0 }
                val estimatedItemCount = ordersUiState.selectedWorkerIds.size * starterItemTemplates
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Pracownicy do zestawu startowego: ${ordersUiState.selectedWorkerIds.size} / ${visibleWorkers.size}")
                            Text("Zaznaczeni w bieżącym filtrze: $selectedVisibleCount • Szacowane pozycje: $estimatedItemCount")
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { ordersViewModel.selectWorkers(visibleWorkerIds) },
                                    modifier = Modifier.weight(1f),
                                    enabled = visibleWorkerIds.isNotEmpty(),
                                ) {
                                    Text("Zaznacz filtr")
                                }
                                Button(
                                    onClick = { ordersViewModel.unselectWorkers(visibleWorkerIds) },
                                    modifier = Modifier.weight(1f),
                                    enabled = visibleWorkerIds.isNotEmpty(),
                                ) {
                                    Text("Odznacz filtr")
                                }
                            }
                            if (visibleWorkers.isEmpty()) {
                                Text("Brak pracowników pasujących do filtra.")
                            } else {
                                visibleWorkers.forEach { worker ->
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Checkbox(
                                            checked = worker.id in ordersUiState.selectedWorkerIds,
                                            onCheckedChange = { ordersViewModel.toggleWorkerSelection(worker.id) },
                                        )
                                        Column(modifier = Modifier.weight(1f).padding(top = 12.dp)) {
                                            Text("${worker.name} ${worker.surname} • ${worker.plant.ifBlank { "Bez zakładu" }}")
                                            Text(
                                                "Koszulka ${worker.shirt.ifBlank { "-" }}, Bluza ${worker.hoodie.ifBlank { "-" }}, " +
                                                    "Spodnie ${worker.pants.ifBlank { "-" }}, Kurtka ${worker.jacket.ifBlank { "-" }}, Buty ${worker.shoes.ifBlank { "-" }}",
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                ordersUiState.items.forEach { itemData ->
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${itemData.date} • ${itemData.plant.ifBlank { "Bez zakładu" }}")
                                Text("Status: ${itemData.status}")
                                Text(if (itemData.orderDesc.isBlank()) "Brak opisu" else itemData.orderDesc)
                                Button(onClick = { ordersViewModel.editOrder(itemData) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Edytuj nagłówek zamówienia")
                                }
                                Button(onClick = { ordersViewModel.toggleOrderSelection(itemData.id) }, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (ordersUiState.selectedOrderId == itemData.id) "Ukryj pozycje" else "Pokaż pozycje")
                                }
                                Button(onClick = { ordersViewModel.markOrdered(itemData.id) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Oznacz jako zamówione")
                                }
                                Button(onClick = { ordersViewModel.issueAll(itemData.id) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Wydaj wszystkie pozycje")
                                }
                                Button(onClick = { ordersViewModel.exportOrderCsv(itemData.id) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Eksport CSV zamówienia")
                                }
                                Button(
                                    onClick = { ordersViewModel.exportOrderPdf(itemData.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(if (ordersUiState.isExportingPdf) "Eksportowanie PDF..." else "Eksport PDF zamówienia")
                                }
                                Button(
                                    onClick = { ordersViewModel.exportIssuePdf(itemData.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(if (ordersUiState.isExportingIssuePdf) "Eksportowanie PDF wydania..." else "Eksport PDF wydania")
                                }
                                Button(
                                    onClick = { ordersViewModel.exportOrderXlsx(itemData.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(if (ordersUiState.isExportingXlsx) "Eksportowanie XLSX..." else "Eksport XLSX zamówienia")
                                }
                                Button(onClick = { ordersViewModel.deleteOrder(itemData.id) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Usuń zamówienie")
                                }
                                if (ordersUiState.selectedOrderId == itemData.id) {
                                    if (ordersUiState.selectedOrderSummary.isNotEmpty()) {
                                        Card(modifier = Modifier.fillMaxWidth()) {
                                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("Podsumowanie zamówienia")
                                                ordersUiState.selectedOrderSummary.forEach { summary ->
                                                    Text(summary)
                                                }
                                            }
                                        }
                                    }
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
                                        Text(if (ordersUiState.isSavingItem) "Zapisywanie pozycji..." else if (ordersUiState.itemEditor.id == null) "Dodaj pozycję" else "Zapisz zmiany pozycji")
                                    }
                                    if (ordersUiState.itemEditor.id != null) {
                                        Button(onClick = ordersViewModel::clearItemEditor, modifier = Modifier.fillMaxWidth()) { Text("Anuluj edycję pozycji") }
                                    }
                                    ordersUiState.selectedOrderItems.forEach { orderItem ->
                                        Card(modifier = Modifier.fillMaxWidth()) {
                                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("${orderItem.name} ${orderItem.surname}".trim().ifBlank { "Pracownik nieuzupełniony" })
                                                Text("${orderItem.item} • rozmiar: ${orderItem.size.ifBlank { "-" }} • ilość: ${orderItem.qty}")
                                                Text(if (orderItem.issued) "Status pozycji: wydane" else "Status pozycji: niewydane")
                                                Button(onClick = { ordersViewModel.editItem(orderItem) }, modifier = Modifier.fillMaxWidth()) {
                                                    Text("Edytuj pozycję")
                                                }
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
                reportsUiState.history.filter { historyItem ->
                    val matchesYear = reportsUiState.year.isBlank() || historyItem.date.startsWith(reportsUiState.year.trim())
                    val matchesQuery = reportsUiState.workerQuery.isBlank() || listOf(
                        historyItem.name,
                        historyItem.surname,
                        historyItem.item,
                        historyItem.size,
                    ).joinToString(" ").lowercase().contains(reportsUiState.workerQuery.trim().lowercase())
                    matchesYear && matchesQuery
                }.forEach { historyItem ->
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
