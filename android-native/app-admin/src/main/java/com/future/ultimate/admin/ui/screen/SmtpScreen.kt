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
import com.future.ultimate.admin.ui.viewmodel.SmtpViewModel

@Composable
fun SmtpScreen() {
    val app = LocalContext.current.applicationContext as AdminApp
    val viewModel: SmtpViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings

    ScreenColumn("Ustawienia SMTP", "Konfiguracja serwera SMTP") {
        item {
            Column {
                OutlinedTextField(settings.host, { viewModel.updateSettings(settings.copy(host = it)) }, label = { Text("Host") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(settings.port, { viewModel.updateSettings(settings.copy(port = it)) }, label = { Text("Port") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(settings.user, { viewModel.updateSettings(settings.copy(user = it)) }, label = { Text("Email/Login") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(settings.password, { viewModel.updateSettings(settings.copy(password = it)) }, label = { Text("Hasło/Klucz") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) { Text(if (uiState.isSaving) "Zapisywanie..." else "Zapisz") }
                Button(onClick = viewModel::validate, modifier = Modifier.fillMaxWidth()) { Text("Test połączenia") }
                uiState.message?.let { Text(it) }
            }
        }
    }
}
