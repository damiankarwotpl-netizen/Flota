package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.future.ultimate.admin.AdminApp
import com.future.ultimate.admin.ui.viewmodel.AdminViewModelFactory
import com.future.ultimate.admin.ui.viewmodel.ContactsViewModel
import com.future.ultimate.core.common.model.ContactDraft

@Composable
fun ContactsScreen() {
    val app = LocalContext.current.applicationContext as AdminApp
    val viewModel: ContactsViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenColumn("Kontakty", "Szukaj, filtruj i zarządzaj kontaktami") {
        item {
            Column {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::updateQuery,
                    label = { Text("Szukaj po imieniu, nazwisku, email, telefonie...") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = uiState.editor.name,
                    onValueChange = { viewModel.updateEditor(uiState.editor.copy(name = it)) },
                    label = { Text("Imię") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = uiState.editor.surname,
                    onValueChange = { viewModel.updateEditor(uiState.editor.copy(surname = it)) },
                    label = { Text("Nazwisko") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = uiState.editor.email,
                    onValueChange = { viewModel.updateEditor(uiState.editor.copy(email = it)) },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = uiState.editor.phone,
                    onValueChange = { viewModel.updateEditor(uiState.editor.copy(phone = it)) },
                    label = { Text("Telefon") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = uiState.editor.workplace,
                    onValueChange = { viewModel.updateEditor(uiState.editor.copy(workplace = it)) },
                    label = { Text("Zakład pracy") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                    Text(if (uiState.isSaving) "Zapisywanie..." else "Dodaj kontakt")
                }
            }
        }
        items(
            uiState.items
                .filter {
                    val blob = "${it.name} ${it.surname} ${it.email} ${it.phone} ${it.workplace} ${it.apartment} ${it.notes}".lowercase()
                    uiState.query.isBlank() || uiState.query.lowercase() in blob
                }
                .map { "${it.name} ${it.surname} • ${it.email} • ${it.phone} • ${it.workplace}" },
        )
    }
}
