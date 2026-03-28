package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import com.future.ultimate.admin.AdminApp
import com.future.ultimate.admin.ui.viewmodel.AdminViewModelFactory
import com.future.ultimate.admin.ui.viewmodel.ClothesReportsViewModel
import com.future.ultimate.admin.ui.viewmodel.ClothesOrdersViewModel
import com.future.ultimate.admin.ui.viewmodel.ClothesSizesViewModel
import com.future.ultimate.core.common.model.ClothesSizeDraft
import java.time.LocalDate

@Composable
fun ClothesScreen() {
    val selected = remember { mutableIntStateOf(0) }
    val ordersTab = remember { mutableIntStateOf(0) }
    val clothingParts = remember { listOf("Koszulka", "Bluza", "Spodnie", "Kurtka", "Buty") }
    var orderPlantPickerOpen by remember { mutableStateOf(false) }
    var orderFlowStep by remember { mutableIntStateOf(0) }
    var globalPartQuantities by remember { mutableStateOf(clothingParts.associateWith { "1" }) }
    var workerPartQuantities by remember { mutableStateOf<Map<Long, Map<String, String>>>(emptyMap()) }
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
                        TabRow(selectedTabIndex = ordersTab.intValue) {
                            listOf("Nowe zamówienie", "Oczekujące", "Do wydania").forEachIndexed { index, title ->
                                Tab(
                                    selected = ordersTab.intValue == index,
                                    onClick = {
                                        ordersTab.intValue = index
                                        if (index == 0) {
                                            orderFlowStep = 0
                                        }
                                    },
                                    text = { Text(title) },
                                )
                            }
                        }
                        if (ordersTab.intValue == 0) {
                            OutlinedTextField(
                                value = ordersUiState.editor.plant,
                                onValueChange = {},
                                label = { Text("Zakład") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectTapGestures(onTap = { orderPlantPickerOpen = true })
                                    },
                                readOnly = true,
                            )
                            if (ordersUiState.editor.plant.isNotBlank()) {
                                Text("Wybrany zakład: ${ordersUiState.editor.plant}")
                            }
                            ordersUiState.actionMessage?.let { Text(it) }
                        }
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
                when (ordersTab.intValue) {
                    0 -> {
                        val selectedPlant = ordersUiState.editor.plant.trim()
                        val plantWorkers = ordersUiState.availableWorkers.filter { worker ->
                            selectedPlant.isNotBlank() && worker.plant.trim().equals(selectedPlant, ignoreCase = true)
                        }
                        item {
                            SectionCard(title = "Krok 1 • Wybór pracowników") {
                                if (selectedPlant.isBlank()) {
                                    Text("Najpierw wybierz zakład.")
                                } else if (plantWorkers.isEmpty()) {
                                    Text("Brak pracowników przypisanych do zakładu: $selectedPlant")
                                } else {
                                    plantWorkers.forEach { worker ->
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Checkbox(
                                                checked = worker.id in ordersUiState.selectedWorkerIds,
                                                onCheckedChange = { ordersViewModel.toggleWorkerSelection(worker.id) },
                                                modifier = Modifier.padding(top = 10.dp),
                                            )
                                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                                Text("${worker.name} ${worker.surname}")
                                                Text(worker.plant.ifBlank { "Bez zakładu" })
                                            }
                                        }
                                    }
                                    Button(
                                        onClick = { orderFlowStep = 1 },
                                        enabled = ordersUiState.selectedWorkerIds.isNotEmpty(),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("Dalej")
                                    }
                                }
                            }
                        }

                        if (orderFlowStep == 1 && ordersUiState.selectedWorkerIds.isNotEmpty()) {
                            item {
                                SectionCard(title = "Krok 2 • Co i ile zamówić") {
                                    Text("Ilości dla wszystkich wybranych pracowników")
                                    clothingParts.forEach { part ->
                                        OutlinedTextField(
                                            value = globalPartQuantities[part].orEmpty(),
                                            onValueChange = { value ->
                                                globalPartQuantities = globalPartQuantities + (part to value.filter(Char::isDigit))
                                            },
                                            label = { Text("$part (globalnie)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            val selected = ordersUiState.selectedWorkerIds
                                            workerPartQuantities = selected.associateWith { workerId ->
                                                val existing = workerPartQuantities[workerId].orEmpty()
                                                clothingParts.associateWith { part ->
                                                    existing[part] ?: globalPartQuantities[part].orEmpty()
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("Ustaw ilości globalne dla zaznaczonych")
                                    }
                                }
                            }
                            val selectedWorkers = plantWorkers.filter { it.id in ordersUiState.selectedWorkerIds }
                            selectedWorkers.forEach { worker ->
                                item {
                                    SectionCard(title = "${worker.name} ${worker.surname}") {
                                        clothingParts.forEach { part ->
                                            OutlinedTextField(
                                                value = workerPartQuantities[worker.id]?.get(part) ?: globalPartQuantities[part].orEmpty(),
                                                onValueChange = { value ->
                                                    val digits = value.filter(Char::isDigit)
                                                    val current = workerPartQuantities[worker.id].orEmpty()
                                                    workerPartQuantities = workerPartQuantities + (worker.id to (current + (part to digits)))
                                                },
                                                label = { Text("$part (ilość)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                    }
                                }
                            }
                            item {
                                SectionCard {
                                    Button(
                                        onClick = {
                                            val payload = ordersUiState.selectedWorkerIds.associateWith { workerId ->
                                                clothingParts.associateWith { part ->
                                                    workerPartQuantities[workerId]?.get(part)?.toIntOrNull()
                                                        ?: globalPartQuantities[part].orEmpty().toIntOrNull()
                                                        ?: 0
                                                }
                                            }
                                            ordersViewModel.createOrderFromSelections(payload, selectedPlant)
                                            ordersTab.intValue = 1
                                            orderFlowStep = 0
                                            workerPartQuantities = emptyMap()
                                            globalPartQuantities = clothingParts.associateWith { "1" }
                                        },
                                        enabled = !ordersUiState.isCreatingStarterOrder,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(if (ordersUiState.isCreatingStarterOrder) "Generowanie..." else "Generuj zamówienie")
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        val waitingOrders = ordersUiState.items.filter { it.status.trim().lowercase() == "zamówione" }
                        if (waitingOrders.isEmpty()) {
                            item { SectionCard { Text("Brak zamówień oczekujących.") } }
                        } else {
                            waitingOrders.forEach { order ->
                                item {
                                    SectionCard(title = order.orderDesc.ifBlank { "${order.plant} • ${order.date}" }) {
                                        Text("Zakład: ${order.plant.ifBlank { "-" }}")
                                        Text("Data: ${order.date}")
                                        Text("Status: ${order.status}")
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        val issueOrders = ordersUiState.items.filter { order ->
                            val normalized = order.status.trim().lowercase()
                            normalized == "częściowo wydane" || normalized == "wydane"
                        }
                        if (issueOrders.isEmpty()) {
                            item { SectionCard { Text("Brak zamówień do wydania.") } }
                        } else {
                            issueOrders.forEach { order ->
                                item {
                                    SectionCard(title = order.orderDesc.ifBlank { "${order.plant} • ${order.date}" }) {
                                        Text("Zakład: ${order.plant.ifBlank { "-" }}")
                                        Text("Data: ${order.date}")
                                        Text("Status: ${order.status}")
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

    if (orderPlantPickerOpen) {
        AlertDialog(
            onDismissRequest = { orderPlantPickerOpen = false },
            title = { Text("Wybierz zakład") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ordersUiState.availableWorkers
                        .map { it.plant.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()
                        .forEach { plant ->
                            Button(
                                onClick = {
                                    ordersViewModel.updateEditor(
                                        ordersUiState.editor.copy(
                                            plant = plant,
                                            date = LocalDate.now().toString(),
                                        ),
                                    )
                                    ordersViewModel.clearStarterSelection()
                                    orderFlowStep = 0
                                    workerPartQuantities = emptyMap()
                                    orderPlantPickerOpen = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(plant)
                            }
                        }
                }
            },
            confirmButton = {
                TextButton(onClick = { orderPlantPickerOpen = false }) { Text("Zamknij") }
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
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onOpenPicker() })
            },
        readOnly = true,
    )
}

private val CLOTH_PART_SIZE_OPTIONS = listOf("XS", "S", "M", "L", "XL", "2XL", "3XL", "4XL", "5XL")
private val PANTS_SIZE_OPTIONS = (46..62 step 2).map(Int::toString)
private val SHOES_SIZE_OPTIONS = (36..50).map(Int::toString)
