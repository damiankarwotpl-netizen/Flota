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
import com.future.ultimate.admin.ui.viewmodel.WorkersViewModel

@Composable
fun WorkersScreen() {
    val app = LocalContext.current.applicationContext as AdminApp
    val viewModel: WorkersViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenColumn("Pracownicy", "Lista i zarządzanie") {
        item {
            Column {
                OutlinedTextField(uiState.query, viewModel::updateQuery, label = { Text("Szukaj pracownika") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(uiState.editor.name, { viewModel.updateEditor(uiState.editor.copy(name = it)) }, label = { Text("Imię") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(uiState.editor.surname, { viewModel.updateEditor(uiState.editor.copy(surname = it)) }, label = { Text("Nazwisko") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(uiState.editor.plant, { viewModel.updateEditor(uiState.editor.copy(plant = it)) }, label = { Text("Zakład") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(uiState.editor.phone, { viewModel.updateEditor(uiState.editor.copy(phone = it)) }, label = { Text("Telefon") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(uiState.editor.position, { viewModel.updateEditor(uiState.editor.copy(position = it)) }, label = { Text("Stanowisko") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(uiState.editor.hireDate, { viewModel.updateEditor(uiState.editor.copy(hireDate = it)) }, label = { Text("Data zatrudnienia") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) { Text(if (uiState.isSaving) "Zapisywanie..." else "Dodaj") }
            }
        }
        items(
            uiState.items.filter {
                val blob = "${it.name} ${it.surname} ${it.plant} ${it.phone} ${it.position} ${it.hireDate}".lowercase()
                uiState.query.isBlank() || uiState.query.lowercase() in blob
            }.map { "${it.name} ${it.surname} • ${it.position} • ${it.plant} • ${it.phone}" },
        )
    }
}
