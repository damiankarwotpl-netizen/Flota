package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    val showPassword = remember { mutableStateOf(false) }
    val portValid = settings.port.toIntOrNull() != null
    val smtpReady = settings.host.isNotBlank() && settings.user.isNotBlank() && settings.password.isNotBlank() && portValid

    ScreenColumn("Ustawienia SMTP", "Konfiguracja serwera SMTP") {
        item {
            Column {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(if (smtpReady) "Konfiguracja kompletna" else "Konfiguracja niekompletna")
                        Text(if (settings.host.isBlank()) "Host: brak" else "Host: OK")
                        Text(if (portValid) "Port: OK" else "Port: nieprawidłowy")
                        Text(if (settings.user.isBlank()) "Login: brak" else "Login: OK")
                        Text(if (settings.password.isBlank()) "Hasło: brak" else "Hasło: ustawione")
                    }
                }
                OutlinedTextField(settings.host, { viewModel.updateSettings(settings.copy(host = it)) }, label = { Text("Host") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(settings.port, { viewModel.updateSettings(settings.copy(port = it)) }, label = { Text("Port") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(settings.user, { viewModel.updateSettings(settings.copy(user = it)) }, label = { Text("Email/Login") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    settings.password,
                    { viewModel.updateSettings(settings.copy(password = it)) },
                    label = { Text("Hasło/Klucz") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showPassword.value) VisualTransformation.None else PasswordVisualTransformation(),
                )
                Button(onClick = { showPassword.value = !showPassword.value }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (showPassword.value) "Ukryj hasło" else "Pokaż hasło")
                }
                Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) { Text(if (uiState.isSaving) "Zapisywanie..." else "Zapisz") }
                Button(onClick = viewModel::validate, modifier = Modifier.fillMaxWidth()) { Text("Test połączenia") }
                uiState.message?.let { Text(it) }
            }
        }
    }
}
