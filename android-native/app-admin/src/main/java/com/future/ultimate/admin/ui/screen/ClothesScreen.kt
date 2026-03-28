package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import com.future.ultimate.admin.AdminApp
import com.future.ultimate.admin.ui.viewmodel.AdminViewModelFactory
import com.future.ultimate.admin.ui.viewmodel.ClothesOrdersViewModel
import com.future.ultimate.admin.ui.viewmodel.ClothesReportsViewModel
import com.future.ultimate.admin.ui.viewmodel.ClothesSizesViewModel
import com.future.ultimate.core.common.model.ClothesSizeDraft

@Composable
fun ClothesScreen() {
    val selected = remember { mutableIntStateOf(0) }
    var isSizeDialogOpen by remember { mutableStateOf(false) }
    val tabs = listOf("Rozmiary", "Zamówienia", "Raporty")
    val app = LocalContext.current.applicationContext as AdminApp
    val sizesViewModel: ClothesSizesViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val sizesUiState by sizesViewModel.uiState.collectAsStateWithLifecycle()
    val ordersViewModel: ClothesOrdersViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val ordersUiState by ordersViewModel.uiState.collectAsStateWithLifecycle()
    val reportsViewModel: ClothesReportsViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val reportsUiState by reportsViewModel.uiState.collectAsStateWithLifecycle()

    ScreenColumn("Ubrania robocze", "Moduły odzieżowe 1:1") {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            FilledIconButton(
                                onClick = {
                                    sizesViewModel.clearEditor()
                                    isSizeDialogOpen = true
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Dodaj rozmiar pracownika",
                                )
                            }
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
            0 -> {
                val filteredSizes = sizesUiState.items.filter {
                    val blob = "${it.name} ${it.surname} ${it.plant} ${it.shirt} ${it.hoodie} ${it.pants} ${it.jacket} ${it.shoes}".lowercase()
                    sizesUiState.query.isBlank() || sizesUiState.query.lowercase() in blob
                }
                if (filteredSizes.isEmpty()) {
                    item {
                        SectionCard {
                            Text("Brak rozmiarów pasujących do wyszukiwania.")
                        }
                    }
                } else {
                    filteredSizes.forEach { itemData ->
                        item {
                            SectionCard(title = "${itemData.name} ${itemData.surname}", subtitle = itemData.plant.ifBlank { "Bez zakładu" }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    FilledIconButton(
                                        onClick = {
                                            sizesViewModel.edit(itemData)
                                            isSizeDialogOpen = true
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = "Edytuj rozmiar",
                                        )
                                    }
                                }
                                Text("Koszulka: ${itemData.shirt} • Bluza: ${itemData.hoodie}")
                                Text("Spodnie: ${itemData.pants} • Kurtka: ${itemData.jacket} • Buty: ${itemData.shoes}")
                                Button(onClick = { sizesViewModel.delete(itemData.id) }, modifier = Modifier.fillMaxWidth()) { Text("Usuń rozmiar") }
                            }
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
                    SectionCard(title = "Pracownicy do zestawu startowego") {
                        Text("Pracownicy: ${ordersUiState.selectedWorkerIds.size} / ${visibleWorkers.size}")
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
                ordersUiState.items.forEach { itemData ->
                    item {
                        SectionCard(
                            title = "${itemData.date} • ${itemData.plant.ifBlank { "Bez zakładu" }}",
                            subtitle = "Status: ${itemData.status}",
                        ) {
                            Text(if (itemData.orderDesc.isBlank()) "Brak opisu" else itemData.orderDesc)
                            Button(onClick = { ordersViewModel.editOrder(itemData) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Edytuj nagłówek zamówienia")
                            }
                            Button(onClick = { ordersViewModel.toggleOrderSelection(itemData.id) }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (ordersUiState.selectedOrderId == itemData.id) "Ukryj pozycje" else "Pokaż pozycje")
                            }
                                Button(
                                    onClick = { ordersViewModel.markOrdered(itemData.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = canMarkClothesOrderOrdered(itemData.status),
                                ) {
                                    Text("Oznacz jako zamówione")
                                }
                                Button(
                                    onClick = { ordersViewModel.issueAll(itemData.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = canIssueClothesOrder(itemData.status),
                                ) {
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
                                        SectionCard(title = "Podsumowanie zamówienia") {
                                            ordersUiState.selectedOrderSummary.forEach { summary ->
                                                Text(summary)
                                            }
                                        }
                                    }
                                    Button(onClick = ordersViewModel::togglePendingItemsFilter, modifier = Modifier.fillMaxWidth()) {
                                        Text(if (ordersUiState.showOnlyPendingItems) "Pokaż wszystkie pozycje" else "Pokaż tylko niewydane")
                                    }
                                    OutlinedTextField(
                                        ordersUiState.importText,
                                        ordersViewModel::updateImportText,
                                        label = { Text("Wklej import CSV/TSV pozycji") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 4,
                                    )
                                    Button(onClick = ordersViewModel::stageImportRows, modifier = Modifier.fillMaxWidth()) {
                                        Text("Parsuj pozycje importu")
                                    }
                                    Button(onClick = ordersViewModel::applyImportedRows, modifier = Modifier.fillMaxWidth()) {
                                        Text("Importuj pozycje do zamówienia")
                                    }
                                    if (ordersUiState.importPreview.isNotEmpty()) {
                                        Text("Podgląd importu: ${ordersUiState.importPreview.size} pozycji")
                                        ordersUiState.importPreview.take(5).forEach { row ->
                                            Text("${row.name} ${row.surname} • ${row.item} • ${row.size.ifBlank { "-" }} • qty ${row.qty}")
                                        }
                                        Button(onClick = ordersViewModel::clearImport, modifier = Modifier.fillMaxWidth()) {
                                            Text("Wyczyść import")
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
                                    ordersUiState.selectedOrderItems
                                        .filter { !ordersUiState.showOnlyPendingItems || !it.issued }
                                        .forEach { orderItem ->
                                        SectionCard(title = "${orderItem.name} ${orderItem.surname}".trim().ifBlank { "Pracownik nieuzupełniony" }) {
                                            Text("${orderItem.item} • rozmiar: ${orderItem.size.ifBlank { "-" }} • ilość: ${orderItem.qty}")
                                            Text(if (orderItem.issued) "Status pozycji: wydane" else "Status pozycji: niewydane")
                                            Button(onClick = { ordersViewModel.editItem(orderItem) }, modifier = Modifier.fillMaxWidth()) {
                                                Text("Edytuj pozycję")
                                            }
                                            Button(
                                                onClick = { ordersViewModel.issueItem(orderItem.id) },
                                                modifier = Modifier.fillMaxWidth(),
                                                enabled = !orderItem.issued && canIssueClothesOrder(itemData.status),
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
        if (selected.intValue == 2) {
            reportsUiState.yearlySummary.forEach { summary ->
                item {
                    SectionCard {
                        Text(summary)
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
                    SectionCard(title = "${historyItem.date} • ${historyItem.name} ${historyItem.surname}".trim()) {
                        Text("${historyItem.item} • rozmiar: ${historyItem.size.ifBlank { "-" }}")
                    }
                }
            }
        }
    }

    if (isSizeDialogOpen) {
        ClothesSizeDialog(
            draft = sizesUiState.editor,
            isSaving = sizesUiState.isSaving,
            isEditing = sizesUiState.editor.id != null,
            onDraftChange = sizesViewModel::updateEditor,
            onDismiss = {
                isSizeDialogOpen = false
                sizesViewModel.clearEditor()
            },
            onSave = {
                sizesViewModel.save()
                isSizeDialogOpen = false
            },
        )
    }
}

@Composable
private fun ClothesSizeDialog(
    draft: ClothesSizeDraft,
    isSaving: Boolean,
    isEditing: Boolean,
    onDraftChange: (ClothesSizeDraft) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val isSaveEnabled = draft.name.isNotBlank() && draft.surname.isNotBlank()
    var sizePickerLabel by remember { mutableStateOf<String?>(null) }
    var sizePickerOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var sizePickerValue by remember { mutableStateOf("") }
    var sizePickerOnSelect by remember { mutableStateOf<(String) -> Unit>({}) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edytuj rozmiar pracownika" else "Dodaj rozmiar pracownika") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { onDraftChange(draft.copy(name = it)) },
                    label = { Text("Imię") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.surname,
                    onValueChange = { onDraftChange(draft.copy(surname = it)) },
                    label = { Text("Nazwisko") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.plant,
                    onValueChange = { onDraftChange(draft.copy(plant = it)) },
                    label = { Text("Zakład") },
                    modifier = Modifier.fillMaxWidth(),
                )
                SizeSelectField(
                    label = "Koszulka",
                    value = draft.shirt,
                    onOpenPicker = {
                        sizePickerLabel = "Koszulka"
                        sizePickerOptions = CLOTH_PART_SIZE_OPTIONS
                        sizePickerValue = draft.shirt
                        sizePickerOnSelect = { onDraftChange(draft.copy(shirt = it)) }
                    },
                )
                SizeSelectField(
                    label = "Bluza",
                    value = draft.hoodie,
                    onOpenPicker = {
                        sizePickerLabel = "Bluza"
                        sizePickerOptions = CLOTH_PART_SIZE_OPTIONS
                        sizePickerValue = draft.hoodie
                        sizePickerOnSelect = { onDraftChange(draft.copy(hoodie = it)) }
                    },
                )
                SizeSelectField(
                    label = "Spodnie",
                    value = draft.pants,
                    onOpenPicker = {
                        sizePickerLabel = "Spodnie"
                        sizePickerOptions = PANTS_SIZE_OPTIONS
                        sizePickerValue = draft.pants
                        sizePickerOnSelect = { onDraftChange(draft.copy(pants = it)) }
                    },
                )
                SizeSelectField(
                    label = "Kurtka",
                    value = draft.jacket,
                    onOpenPicker = {
                        sizePickerLabel = "Kurtka"
                        sizePickerOptions = CLOTH_PART_SIZE_OPTIONS
                        sizePickerValue = draft.jacket
                        sizePickerOnSelect = { onDraftChange(draft.copy(jacket = it)) }
                    },
                )
                SizeSelectField(
                    label = "Buty",
                    value = draft.shoes,
                    onOpenPicker = {
                        sizePickerLabel = "Buty"
                        sizePickerOptions = SHOES_SIZE_OPTIONS
                        sizePickerValue = draft.shoes
                        sizePickerOnSelect = { onDraftChange(draft.copy(shoes = it)) }
                    },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = isSaveEnabled && !isSaving,
            ) {
                Text(if (isSaving) "Zapisywanie..." else if (isEditing) "Zapisz zmiany" else "Dodaj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        },
    )

    if (sizePickerLabel != null) {
        AlertDialog(
            onDismissRequest = { sizePickerLabel = null },
            title = { Text("Wybierz rozmiar • ${sizePickerLabel.orEmpty()}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sizePickerOptions.forEach { option ->
                        Button(
                            onClick = {
                                sizePickerOnSelect(option)
                                sizePickerLabel = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (sizePickerValue == option) "✓ $option" else option)
                        }
                    }
                    TextButton(
                        onClick = {
                            sizePickerOnSelect("")
                            sizePickerLabel = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Wyczyść rozmiar")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { sizePickerLabel = null }) { Text("Zamknij") }
            },
        )
    }
}

@Composable
private fun SizeSelectField(
    label: String,
    value: String,
    onOpenPicker: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenPicker() },
        readOnly = true,
    )
    TextButton(onClick = onOpenPicker, modifier = Modifier.fillMaxWidth()) {
        Text("Wybierz z listy")
    }
}

private val CLOTH_PART_SIZE_OPTIONS = listOf("XS", "S", "M", "L", "XL", "2XL", "3XL", "4XL", "5XL")
private val PANTS_SIZE_OPTIONS = (46..62 step 2).map(Int::toString)
private val SHOES_SIZE_OPTIONS = (36..50).map(Int::toString)

private fun canIssueClothesOrder(status: String): Boolean {
    val normalized = status.trim().lowercase()
    return normalized == "zamówione" || normalized == "częściowo wydane"
}

private fun canMarkClothesOrderOrdered(status: String): Boolean {
    val normalized = status.trim().lowercase()
    return normalized != "częściowo wydane" && normalized != "wydane"
}
