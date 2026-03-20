package com.future.ultimate.driver.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.future.ultimate.core.common.model.DriverRoute
import com.future.ultimate.core.common.model.VehicleReportDraft
import com.future.ultimate.driver.DriverApp
import com.future.ultimate.driver.ui.viewmodel.DriverChangePasswordViewModel
import com.future.ultimate.driver.ui.viewmodel.DriverLoginViewModel
import com.future.ultimate.driver.ui.viewmodel.DriverMileageViewModel
import com.future.ultimate.driver.ui.viewmodel.DriverVehicleReportViewModel
import com.future.ultimate.driver.ui.viewmodel.DriverViewModelFactory

private fun LazyListScope.screenHeader(title: String, subtitle: String? = null) {
    item {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DriverSectionCard(
    title: String? = null,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            title?.let {
                Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
private fun DriverScreen(
    title: String,
    subtitle: String? = null,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        screenHeader(title, subtitle)
        content()
    }
}

@Composable
private fun StatusMessage(message: String, emphasis: Boolean = false) {
    Text(
        text = message,
        style = if (emphasis) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
        color = if (emphasis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun DriverLoginScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as DriverApp
    val viewModel: DriverLoginViewModel = viewModel(factory = DriverViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DriverScreen(
        title = "Login kierowcy",
        subtitle = "Zaloguj się, aby dodać przebieg lub wysłać raport stanu pojazdu.",
    ) {
        item {
            DriverSectionCard(title = "Dane logowania") {
                OutlinedTextField(
                    value = uiState.login,
                    onValueChange = viewModel::updateLogin,
                    label = { Text("Login") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = viewModel::updatePassword,
                    label = { Text("Hasło") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Button(
                    onClick = {
                        viewModel.login { requiresPasswordChange ->
                            navController.navigate(
                                if (requiresPasswordChange) DriverRoute.ChangePassword.route else DriverRoute.Mileage.route,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading && uiState.login.isNotBlank() && uiState.password.isNotBlank(),
                ) {
                    Text(if (uiState.isLoading) "Logowanie..." else "Zaloguj się")
                }
                uiState.error?.let { StatusMessage(it, emphasis = true) }
            }
        }
    }
}

@Composable
fun DriverChangePasswordScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as DriverApp
    val viewModel: DriverChangePasswordViewModel = viewModel(factory = DriverViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DriverScreen(
        title = "Zmiana hasła",
        subtitle = "Pierwsze logowanie wymaga ustawienia nowego hasła przed przejściem dalej.",
    ) {
        item {
            DriverSectionCard(title = "Nowe hasło") {
                OutlinedTextField(
                    value = uiState.login,
                    onValueChange = {},
                    label = { Text("Login") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = viewModel::updatePassword,
                    label = { Text("Nowe hasło") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Button(
                    onClick = { viewModel.save { navController.navigate(DriverRoute.Mileage.route) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading && uiState.password.isNotBlank(),
                ) {
                    Text(if (uiState.isLoading) "Zapisywanie..." else "Zapisz hasło")
                }
                uiState.error?.let { StatusMessage(it, emphasis = true) }
            }
        }
    }
}

@Composable
fun DriverMileageScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as DriverApp
    val viewModel: DriverMileageViewModel = viewModel(factory = DriverViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DriverScreen(
        title = "Przebieg",
        subtitle = "Dodaj aktualny stan licznika i sprawdź status synchronizacji danych.",
    ) {
        item {
            DriverSectionCard(title = "Bieżący kierowca") {
                StatusMessage(
                    message = if (uiState.driverName.isNotBlank()) "Kierowca: ${uiState.driverName}" else "Brak aktywnego kierowcy.",
                    emphasis = true,
                )
                StatusMessage("Status synchronizacji: ${uiState.syncStatus}")
                StatusMessage("Kolejka: ${uiState.pendingSyncCount} • Oczekujący przebieg: ${uiState.queuedMileage.ifBlank { "-" }}")
                StatusMessage("Ostatnia próba: ${uiState.lastAttemptAt.ifBlank { "-" }}")
                StatusMessage("Ostatnia synchronizacja: ${uiState.lastSyncedAt.ifBlank { "-" }}")
                if (uiState.syncError.isNotBlank()) {
                    StatusMessage("Błąd synchronizacji: ${uiState.syncError}", emphasis = true)
                }
            }
        }
        item {
            DriverSectionCard(title = "Dodaj przebieg") {
                OutlinedTextField(
                    value = uiState.registration,
                    onValueChange = viewModel::setRegistration,
                    label = { Text("Rejestracja") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = uiState.mileage,
                    onValueChange = viewModel::updateMileage,
                    label = { Text("Przebieg") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Button(
                    onClick = viewModel::save,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving && uiState.registration.isNotBlank() && uiState.mileage.isNotBlank(),
                ) {
                    Text(if (uiState.isSaving) "Zapisywanie..." else "Zapisz przebieg")
                }
                Button(onClick = viewModel::flushSyncNow, modifier = Modifier.fillMaxWidth()) {
                    Text(if (uiState.isSaving) "Synchronizowanie..." else "Wymuś synchronizację teraz")
                }
                uiState.status?.let { StatusMessage(it, emphasis = true) }
            }
        }
        item {
            DriverSectionCard(title = "Dalsze akcje") {
                Button(onClick = { navController.navigate(DriverRoute.VehicleReport.route) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Przejdź do raportu stanu samochodu")
                }
                Button(
                    onClick = {
                        viewModel.logout {
                            navController.navigate(DriverRoute.Login.route) {
                                popUpTo(DriverRoute.Login.route) { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Wyloguj")
                }
            }
        }
    }
}

@Composable
fun DriverVehicleReportScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as DriverApp
    val viewModel: DriverVehicleReportViewModel = viewModel(factory = DriverViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val draft = uiState.draft

    DriverScreen(
        title = "Raport stanu samochodu",
        subtitle = "Uzupełnij pola formularza, zaznacz wyposażenie i wygeneruj PDF.",
    ) {
        item {
            DriverSectionCard(title = "Kierowca") {
                StatusMessage(
                    message = if (uiState.driverName.isNotBlank()) "Kierowca: ${uiState.driverName}" else "Brak aktywnego kierowcy.",
                    emphasis = true,
                )
            }
        }
        item {
            DriverSectionCard(title = "Dane pojazdu i wpisy") {
                editableFields(draft) { viewModel.updateDraft(it) }
            }
        }
        item {
            DriverSectionCard(title = "Wyposażenie obowiązkowe") {
                checklist(draft) { viewModel.updateDraft(it) }
            }
        }
        item {
            DriverSectionCard(title = "Akcje") {
                Button(onClick = { navController.navigate(DriverRoute.Mileage.route) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Wróć do przebiegu")
                }
                Button(
                    onClick = viewModel::save,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving,
                ) {
                    Text(if (uiState.isSaving) "Zapisywanie..." else "Zapisz PDF")
                }
                Button(
                    onClick = {
                        viewModel.logout {
                            navController.navigate(DriverRoute.Login.route) {
                                popUpTo(DriverRoute.Login.route) { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Wyloguj")
                }
                uiState.message?.let { StatusMessage(it, emphasis = true) }
            }
        }
    }
}

@Composable
private fun editableFields(draft: VehicleReportDraft, onDraftChange: (VehicleReportDraft) -> Unit) {
    listOf(
        Triple("Marka", draft.marka, KeyboardType.Text),
        Triple("Rejestracja", draft.rej, KeyboardType.Text),
        Triple("Przebieg", draft.przebieg, KeyboardType.Number),
        Triple("Poziom oleju", draft.olej, KeyboardType.Text),
        Triple("Wskaźnik paliwa", draft.paliwo, KeyboardType.Text),
        Triple("Rodzaj paliwa", draft.rodzajPaliwa, KeyboardType.Text),
        Triple("Lewy przedni", draft.lp, KeyboardType.Text),
        Triple("Prawy przedni", draft.pp, KeyboardType.Text),
        Triple("Lewy tylny", draft.lt, KeyboardType.Text),
        Triple("Prawy tylny", draft.pt, KeyboardType.Text),
        Triple("Nowe uszkodzenia", draft.uszkodzenia, KeyboardType.Text),
        Triple("Od kiedy?", draft.odKiedy, KeyboardType.Text),
        Triple("Przegląd / Service", draft.serwis, KeyboardType.Text),
        Triple("Przegląd techniczny", draft.przeglad, KeyboardType.Text),
        Triple("Uwagi", draft.uwagi, KeyboardType.Text),
    ).forEach { (label, value, keyboardType) ->
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                onDraftChange(
                    when (label) {
                        "Marka" -> draft.copy(marka = newValue)
                        "Rejestracja" -> draft.copy(rej = newValue)
                        "Przebieg" -> draft.copy(przebieg = newValue)
                        "Poziom oleju" -> draft.copy(olej = newValue)
                        "Wskaźnik paliwa" -> draft.copy(paliwo = newValue)
                        "Rodzaj paliwa" -> draft.copy(rodzajPaliwa = newValue)
                        "Lewy przedni" -> draft.copy(lp = newValue)
                        "Prawy przedni" -> draft.copy(pp = newValue)
                        "Lewy tylny" -> draft.copy(lt = newValue)
                        "Prawy tylny" -> draft.copy(pt = newValue)
                        "Nowe uszkodzenia" -> draft.copy(uszkodzenia = newValue)
                        "Od kiedy?" -> draft.copy(odKiedy = newValue)
                        "Przegląd / Service" -> draft.copy(serwis = newValue)
                        "Przegląd techniczny" -> draft.copy(przeglad = newValue)
                        "Uwagi" -> draft.copy(uwagi = newValue)
                        else -> draft
                    },
                )
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = VisualTransformation.None,
        )
    }
}

@Composable
private fun checklist(draft: VehicleReportDraft, onDraftChange: (VehicleReportDraft) -> Unit) {
    listOf(
        "Trójkąt" to draft.trojkat,
        "Kamizelki" to draft.kamizelki,
        "Koło zapasowe" to draft.kolo,
        "Dowód rejestracyjny" to draft.dowod,
        "Apteczka" to draft.apteczka,
    ).forEach { (label, checked) ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { value ->
                    onDraftChange(
                        when (label) {
                            "Trójkąt" -> draft.copy(trojkat = value)
                            "Kamizelki" -> draft.copy(kamizelki = value)
                            "Koło zapasowe" -> draft.copy(kolo = value)
                            "Dowód rejestracyjny" -> draft.copy(dowod = value)
                            else -> draft.copy(apteczka = value)
                        },
                    )
                },
            )
            Text(label, modifier = Modifier.padding(top = 12.dp))
        }
    }
}
