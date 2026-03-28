package com.future.ultimate.admin.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.future.ultimate.admin.AdminApp
import com.future.ultimate.admin.ui.viewmodel.AdminViewModelFactory
import com.future.ultimate.admin.ui.viewmodel.SettingsViewModel
import com.future.ultimate.core.common.model.AdminRoute

@Composable
fun SettingsScreen(
    navController: NavController,
    onEnableDarkTheme: () -> Unit,
    onEnableLightTheme: () -> Unit,
    onEnablePinkTheme: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as AdminApp
    val viewModel: SettingsViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val excelPicker = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mimeType = app.contentResolver.getType(uri)
        val projection = arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)
        val fileName = app.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        }
        val bytes = runCatching {
            app.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: byteArrayOf()
        }.getOrDefault(byteArrayOf())
        viewModel.importDatabaseWorkbook(fileName = fileName, mimeType = mimeType, bytes = bytes)
    }

    ScreenColumn("Ustawienia", "Integracje, snapshoty i podstawowe statystyki systemu") {
        item {
            SectionCard(
                title = "Wygląd aplikacji",
                subtitle = "Tutaj zmienisz motyw interfejsu.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onEnableDarkTheme, modifier = Modifier.fillMaxWidth()) {
                        Text("Dark mode")
                    }
                    OutlinedButton(onClick = onEnableLightTheme, modifier = Modifier.fillMaxWidth()) {
                        Text("Light mode")
                    }
                    OutlinedButton(onClick = onEnablePinkTheme, modifier = Modifier.fillMaxWidth()) {
                        Text("Pink mode • neon")
                    }
                }
            }
        }
        item {
            SectionCard(
                title = "Stan danych lokalnych",
                subtitle = "Szybki przegląd rekordów zapisanych lokalnie. Dane admin nie synchronizują się automatycznie między urządzeniami.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Kontakty: ${uiState.stats.contactCount} • Pracownicy: ${uiState.stats.workerCount}")
                    Text("Auta: ${uiState.stats.carCount} • Zakłady: ${uiState.stats.plantCount}")
                    Text("Rozmiary odzieży: ${uiState.stats.clothesSizeCount} • Zamówienia: ${uiState.stats.clothesOrderCount}")
                    Text("Historia wydań odzieży: ${uiState.stats.clothesHistoryCount}")
                }
            }
        }
        item {
            SectionCard(
                title = "Integracja kierowców",
                subtitle = "Sekcja dotyczy endpointu kierowców (logi/przebiegi), a nie pełnej synchronizacji danych admin między urządzeniami.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Aktywny endpoint APK admin: ${uiState.remoteSettings.apiUrl}")
                    if (uiState.isEndpointEditorUnlocked) {
                        OutlinedTextField(
                            value = uiState.remoteSettings.apiUrl,
                            onValueChange = viewModel::updateDriverRemoteApiUrl,
                            label = { Text("Endpoint zdalnego syncu kierowców") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = viewModel::saveDriverRemoteSettings,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isSavingRemoteSettings && uiState.remoteSettings.apiUrl.isNotBlank(),
                        ) {
                            Text(if (uiState.isSavingRemoteSettings) "Zapisywanie integracji..." else "Zapisz ustawienia integracji")
                        }
                        Button(
                            onClick = viewModel::validateDriverRemoteSettings,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isValidatingRemoteSettings && uiState.remoteSettings.apiUrl.isNotBlank(),
                        ) {
                            Text(if (uiState.isValidatingRemoteSettings) "Sprawdzanie endpointu..." else "Sprawdź endpoint kierowców")
                        }
                        OutlinedButton(onClick = viewModel::lockEndpointEditor, modifier = Modifier.fillMaxWidth()) {
                            Text("Ukryj edycję endpointu")
                        }
                    } else {
                        OutlinedTextField(
                            value = uiState.endpointAccessPassword,
                            onValueChange = viewModel::updateEndpointAccessPassword,
                            label = { Text("Hasło serwisowe do edycji endpointu") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                        )
                        OutlinedButton(
                            onClick = viewModel::unlockEndpointEditor,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = uiState.endpointAccessPassword.isNotBlank(),
                        ) {
                            Text("Odblokuj edycję endpointu")
                        }
                    }
                    Button(
                        onClick = viewModel::importDriverRemoteLogs,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isImportingRemoteLogs,
                    ) {
                        Text(if (uiState.isImportingRemoteLogs) "Pobieranie logów kierowców..." else "Zaczytaj logi kierowców z endpointu")
                    }
                    uiState.actionMessage?.let { Text(it) }
                }
            }
        }
        item {
            SectionCard(
                title = "Narzędzia administracyjne",
                subtitle = "Najczęściej używane akcje serwisowe i przejścia do modułów pobocznych.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // TODO(main.py parity): legacy `show_logs` opened the app log buffer/file.
                    // Native admin currently exposes session reports, but there is no repository-backed application-log source here yet.
                    Button(onClick = { navController.navigate(AdminRoute.Reports.route) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Pokaż raporty sesji")
                    }
                    Button(onClick = { navController.navigate(AdminRoute.Payroll.route) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Przejdź do modułu wypłat")
                    }
                    Button(onClick = viewModel::exportDatabaseSnapshot, modifier = Modifier.fillMaxWidth()) {
                        Text(if (uiState.isExportingDatabase) "Eksportowanie bazy..." else "Eksportuj snapshot bazy")
                    }
                    Button(onClick = { excelPicker.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (uiState.isImportingDatabase) "Wgrywanie bazy..." else "Wgraj bazę danych z Excela")
                    }
                    Button(onClick = { navController.navigate(AdminRoute.Smtp.route) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Ustawienia SMTP")
                    }
                    Button(onClick = { navController.navigate(AdminRoute.Template.route) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Edytuj szablon email")
                    }
                    Button(
                        onClick = viewModel::clearAllTestData,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isClearingDatabase,
                    ) {
                        Text(if (uiState.isClearingDatabase) "Czyszczenie bazy i endpointu..." else "Wyczyść bazę lokalną i endpoint")
                    }
                    uiState.actionMessage?.let { Text(it) }
                }
            }
        }
    }
}
