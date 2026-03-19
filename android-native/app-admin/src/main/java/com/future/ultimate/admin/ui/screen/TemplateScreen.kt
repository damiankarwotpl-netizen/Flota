package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
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
import com.future.ultimate.admin.ui.viewmodel.TemplateViewModel

@Composable
fun TemplateScreen() {
    val app = LocalContext.current.applicationContext as AdminApp
    val viewModel: TemplateViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val template = uiState.template

    ScreenColumn("Szablon email", "Edycja szablonu wiadomości") {
        item {
            Column {
                OutlinedTextField(template.subject, { viewModel.updateTemplate(template.copy(subject = it)) }, label = { Text("Temat {Imię}") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(template.body, { viewModel.updateTemplate(template.copy(body = it)) }, label = { Text("Treść...") }, modifier = Modifier.fillMaxWidth(), minLines = 6)
                OutlinedTextField(uiState.previewName, viewModel::updatePreviewName, label = { Text("Podgląd: imię") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(uiState.previewDate, viewModel::updatePreviewDate, label = { Text("Podgląd: data") }, modifier = Modifier.fillMaxWidth())
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Podgląd tematu:")
                        Text(if (uiState.subjectPreview.isBlank()) "—" else uiState.subjectPreview)
                        Text("Podgląd treści:")
                        Text(if (uiState.bodyPreview.isBlank()) "—" else uiState.bodyPreview)
                    }
                }
                Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) { Text(if (uiState.isSaving) "Zapisywanie..." else "Zapisz") }
                uiState.message?.let { Text(it) }
            }
        }
    }
}
