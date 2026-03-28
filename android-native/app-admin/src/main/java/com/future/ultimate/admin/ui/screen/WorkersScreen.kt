package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.future.ultimate.admin.AdminApp
import com.future.ultimate.admin.ui.viewmodel.AdminViewModelFactory
import com.future.ultimate.admin.ui.viewmodel.WorkersViewModel
import com.future.ultimate.core.common.model.WorkerDraft
import com.future.ultimate.core.common.repository.WorkerListItem

@Composable
fun WorkersScreen() {
    val app = LocalContext.current.applicationContext as AdminApp
    val viewModel: WorkersViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isDialogOpen by remember { mutableStateOf(false) }

    val filteredWorkers = remember(uiState.items, uiState.query) {
        uiState.items.filter {
            val blob = "${it.name} ${it.surname} ${it.plant} ${it.phone} ${it.position} ${it.hireDate}".lowercase()
            uiState.query.isBlank() || uiState.query.lowercase() in blob
        }
    }

    ScreenColumn("Pracownicy", "Lista i zarządzanie") {
        item {
            SectionCard {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::updateQuery,
                    label = { Text("Szukaj pracownika") },
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
                            contentDescription = "Dodaj pracownika",
                        )
                    }
                }
            }
        }

        if (filteredWorkers.isEmpty()) {
            item {
                SectionCard {
                    Text(
                        text = "Brak pracowników pasujących do wyszukiwania.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        } else {
            filteredWorkers.forEach { worker ->
                item {
                    WorkerCard(
                        worker = worker,
                        onEdit = {
                            viewModel.edit(worker)
                            isDialogOpen = true
                        },
                    )
                }
            }
        }
    }

    if (isDialogOpen) {
        WorkerDialog(
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
private fun WorkerCard(
    worker: WorkerListItem,
    onEdit: () -> Unit,
) {
    SectionCard(
        title = "${worker.name} ${worker.surname}",
        subtitle = "${worker.position} • ${worker.plant}",
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
                    contentDescription = "Edytuj pracownika",
                )
            }
        }
        Text("Telefon: ${worker.phone.ifBlank { "Brak numeru" }}")
        if (worker.hireDate.isNotBlank()) {
            Text("Data zatrudnienia: ${worker.hireDate}")
        }
    }
}

@Composable
private fun WorkerDialog(
    draft: WorkerDraft,
    isSaving: Boolean,
    isEditing: Boolean,
    onDraftChange: (WorkerDraft) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val isSaveEnabled = draft.name.isNotBlank() && draft.surname.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edytuj pracownika" else "Nowy pracownik",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(draft.name, { onDraftChange(draft.copy(name = it)) }, label = { Text("Imię *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(draft.surname, { onDraftChange(draft.copy(surname = it)) }, label = { Text("Nazwisko *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(draft.plant, { onDraftChange(draft.copy(plant = it)) }, label = { Text("Zakład") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    draft.phone,
                    { onDraftChange(draft.copy(phone = it)) },
                    label = { Text("Telefon") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(draft.position, { onDraftChange(draft.copy(position = it)) }, label = { Text("Stanowisko") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(draft.hireDate, { onDraftChange(draft.copy(hireDate = it)) }, label = { Text("Data zatrudnienia") }, modifier = Modifier.fillMaxWidth())
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
                        else -> "Dodaj pracownika"
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
