package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.future.ultimate.admin.AdminApp
import com.future.ultimate.admin.ui.viewmodel.AdminViewModelFactory
import com.future.ultimate.admin.ui.viewmodel.PlantsViewModel
import com.future.ultimate.core.common.model.PlantDraft
import com.future.ultimate.core.common.repository.PlantListItem

@Composable
fun PlantsScreen() {
    val app = LocalContext.current.applicationContext as AdminApp
    val viewModel: PlantsViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isDialogOpen by remember { mutableStateOf(false) }

    val filteredPlants = remember(uiState.items, uiState.query) {
        uiState.items.filter {
            val blob = "${it.name} ${it.city} ${it.address} ${it.contactPhone} ${it.notes}".lowercase()
            uiState.query.isBlank() || uiState.query.lowercase() in blob
        }
    }

    ScreenColumn("Zakłady", "Szybkie wyszukiwanie") {
        item {
            SectionCard(title = "Wyszukiwarka", subtitle = "Szukaj po nazwie, mieście, adresie i notatkach.") {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::updateQuery,
                    label = { Text("Szukaj zakładu") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    FilledIconButton(
                        onClick = {
                            viewModel.clearEditor()
                            isDialogOpen = true
                        },
                        modifier = Modifier.size(42.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Dodaj zakład",
                        )
                    }
                }
            }
        }

        if (filteredPlants.isEmpty()) {
            item {
                SectionCard {
                    Text(
                        text = "Brak zakładów pasujących do wyszukiwania.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        } else {
            filteredPlants.forEach { plant ->
                item {
                    PlantCard(
                        plant = plant,
                        onEdit = {
                            viewModel.edit(plant)
                            isDialogOpen = true
                        },
                    )
                }
            }
        }
    }

    if (isDialogOpen) {
        PlantDialog(
            draft = uiState.editor,
            isSaving = uiState.isSaving,
            isEditing = uiState.editor.id != null,
            onDraftChange = viewModel::updateEditor,
            onDismiss = {
                isDialogOpen = false
                viewModel.clearEditor()
            },
            onSave = {
                viewModel.save()
                isDialogOpen = false
                viewModel.clearEditor()
            },
        )
    }
}

@Composable
private fun PlantCard(
    plant: PlantListItem,
    onEdit: () -> Unit,
) {
    SectionCard(
        title = plant.name,
        subtitle = plant.city.ifBlank { "Brak miasta" },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            FilledIconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edytuj zakład",
                )
            }
        }
        if (plant.address.isNotBlank()) {
            Text("Adres: ${plant.address}")
        }
        Text("Telefon: ${plant.contactPhone.ifBlank { "Brak numeru" }}")
        if (plant.notes.isNotBlank()) {
            Text("Notatki: ${plant.notes}")
        }
    }
}

@Composable
private fun PlantDialog(
    draft: PlantDraft,
    isSaving: Boolean,
    isEditing: Boolean,
    onDraftChange: (PlantDraft) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val isSaveEnabled = draft.name.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edytuj zakład" else "Nowy zakład",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(draft.name, { onDraftChange(draft.copy(name = it)) }, label = { Text("Nazwa zakładu *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(draft.city, { onDraftChange(draft.copy(city = it)) }, label = { Text("Miasto") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(draft.address, { onDraftChange(draft.copy(address = it)) }, label = { Text("Adres") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(draft.contactPhone, { onDraftChange(draft.copy(contactPhone = it)) }, label = { Text("Telefon") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(draft.notes, { onDraftChange(draft.copy(notes = it)) }, label = { Text("Notatki") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = isSaveEnabled && !isSaving,
            ) {
                Text(
                    when {
                        isSaving -> "Zapisywanie..."
                        isEditing -> "Zapisz zmiany"
                        else -> "Dodaj zakład"
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        },
    )
}
