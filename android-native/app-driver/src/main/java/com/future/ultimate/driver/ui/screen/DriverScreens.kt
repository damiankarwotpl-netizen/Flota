package com.future.ultimate.driver.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.future.ultimate.core.common.model.DriverRoute
import com.future.ultimate.core.common.model.VehicleReportDraft
import com.future.ultimate.core.common.ui.theme.FlotaThemeDefaults
import com.future.ultimate.driver.DriverApp
import com.future.ultimate.driver.ui.viewmodel.DriverChangePasswordViewModel
import com.future.ultimate.driver.ui.viewmodel.DriverLoginViewModel
import com.future.ultimate.driver.ui.viewmodel.DriverMileageViewModel
import com.future.ultimate.driver.ui.viewmodel.DriverVehicleReportViewModel
import com.future.ultimate.driver.ui.viewmodel.DriverViewModelFactory

@Composable
private fun DriverSectionCard(
    title: String? = null,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FlotaThemeDefaults.cardShape,
        colors = FlotaThemeDefaults.elevatedCardColors(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            title?.let {
                Text(it, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    ),
                ),
            )
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
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
private fun DriverActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    secondary: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        shape = FlotaThemeDefaults.pillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (secondary) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
            contentColor = if (secondary) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary,
        ),
        contentPadding = PaddingValues(vertical = 14.dp),
    ) {
        Text(text)
    }
}

@Composable
private fun DriverInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        shape = RoundedCornerShape(18.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
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
            DriverSectionCard(
                title = "Endpoint synchronizacji",
                subtitle = "APK kierowcy działa domyślnie na stałym endpointcie. Edycja jest ukryta za hasłem serwisowym.",
            ) {
                StatusMessage("Aktywny endpoint APK:", emphasis = true)
                Text(
                    text = uiState.remoteApiUrl,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (uiState.isEndpointEditorUnlocked) {
                    DriverInputField(
                        value = uiState.remoteApiUrl,
                        onValueChange = viewModel::updateRemoteApiUrl,
                        label = "Endpoint zdalnego syncu kierowców",
                        singleLine = false,
                    )
                    DriverActionButton(
                        text = if (uiState.isSavingRemoteSettings) "Zapisywanie endpointu..." else "Zapisz endpoint",
                        onClick = viewModel::saveRemoteSettings,
                        enabled = !uiState.isSavingRemoteSettings && uiState.remoteApiUrl.isNotBlank(),
                        secondary = true,
                    )
                    DriverActionButton(
                        text = if (uiState.isValidatingRemoteSettings) "Sprawdzanie endpointu..." else "Sprawdź endpoint",
                        onClick = viewModel::validateRemoteSettings,
                        enabled = !uiState.isValidatingRemoteSettings && uiState.remoteApiUrl.isNotBlank(),
                        secondary = true,
                    )
                    DriverActionButton(
                        text = "Ukryj edycję endpointu",
                        onClick = viewModel::lockEndpointEditor,
                        secondary = true,
                    )
                } else {
                    DriverInputField(
                        value = uiState.endpointAccessPassword,
                        onValueChange = viewModel::updateEndpointAccessPassword,
                        label = "Hasło serwisowe do edycji endpointu",
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    DriverActionButton(
                        text = "Odblokuj edycję endpointu",
                        onClick = viewModel::unlockEndpointEditor,
                        enabled = uiState.endpointAccessPassword.isNotBlank(),
                        secondary = true,
                    )
                }
            }
        }
        item {
            DriverSectionCard {
                DriverInputField(
                    value = uiState.login,
                    onValueChange = viewModel::updateLogin,
                    label = "Login",
                )
                DriverInputField(
                    value = uiState.password,
                    onValueChange = viewModel::updatePassword,
                    label = "Hasło",
                    visualTransformation = PasswordVisualTransformation(),
                )
                DriverActionButton(
                    text = if (uiState.isLoading) "Logowanie..." else "Zaloguj się",
                    onClick = {
                        viewModel.login { requiresPasswordChange ->
                            navController.navigate(
                                if (requiresPasswordChange) DriverRoute.ChangePassword.route else DriverRoute.Mileage.route,
                            )
                        }
                    },
                    enabled = !uiState.isLoading && uiState.login.isNotBlank() && uiState.password.isNotBlank(),
                )
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
            DriverSectionCard {
                DriverInputField(
                    value = uiState.login,
                    onValueChange = {},
                    label = "Login",
                    enabled = false,
                )
                DriverInputField(
                    value = uiState.password,
                    onValueChange = viewModel::updatePassword,
                    label = "Nowe hasło",
                    visualTransformation = PasswordVisualTransformation(),
                )
                DriverActionButton(
                    text = if (uiState.isLoading) "Zapisywanie..." else "Zapisz hasło",
                    onClick = { viewModel.save { navController.navigate(DriverRoute.Mileage.route) } },
                    enabled = !uiState.isLoading && uiState.password.isNotBlank(),
                )
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
            DriverSectionCard {
                StatusMessage(
                    message = if (uiState.driverName.isNotBlank()) "Kierowca: ${uiState.driverName}" else "Brak aktywnego kierowcy.",
                    emphasis = true,
                )
                if (uiState.registration.isBlank()) {
                    StatusMessage(
                        message = "W tej chwili nie masz przypisanego samochodu. Zaloguj się ponownie po zmianie przypisania przez admina.",
                        emphasis = true,
                    )
                } else {
                    StatusMessage("Aktualnie przypisane auto: ${uiState.registration}")
                }
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
            DriverSectionCard {
                DriverInputField(
                    value = uiState.registration,
                    onValueChange = {},
                    label = "Rejestracja",
                    enabled = false,
                )
                DriverInputField(
                    value = uiState.mileage,
                    onValueChange = viewModel::updateMileage,
                    label = "Przebieg",
                    keyboardType = KeyboardType.Number,
                )
                DriverActionButton(
                    text = if (uiState.isSaving) "Zapisywanie..." else "Zapisz przebieg",
                    onClick = viewModel::save,
                    enabled = !uiState.isSaving && uiState.registration.isNotBlank() && uiState.mileage.isNotBlank(),
                )
                uiState.status?.let { StatusMessage(it, emphasis = true) }
            }
        }
        item {
            DriverSectionCard {
                DriverActionButton(
                    text = "Przejdź do raportu stanu samochodu",
                    onClick = { navController.navigate(DriverRoute.VehicleReport.route) },
                )
                DriverActionButton(
                    text = "Wyloguj",
                    onClick = {
                        viewModel.logout {
                            navController.navigate(DriverRoute.Login.route) {
                                popUpTo(DriverRoute.Login.route) { inclusive = true }
                            }
                        }
                    },
                    secondary = true,
                )
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
            DriverSectionCard {
                StatusMessage(
                    message = if (uiState.driverName.isNotBlank()) "Kierowca: ${uiState.driverName}" else "Brak aktywnego kierowcy.",
                    emphasis = true,
                )
            }
        }
        item {
            DriverSectionCard {
                editableFields(draft) { viewModel.updateDraft(it) }
            }
        }
        item {
            DriverSectionCard {
                checklist(draft) { viewModel.updateDraft(it) }
            }
        }
        item {
            DriverSectionCard {
                DriverActionButton(
                    text = "Wróć do przebiegu",
                    onClick = { navController.navigate(DriverRoute.Mileage.route) },
                    secondary = true,
                )
                DriverActionButton(
                    text = if (uiState.isSaving) "Zapisywanie..." else "Zapisz PDF",
                    onClick = viewModel::save,
                    enabled = !uiState.isSaving,
                )
                DriverActionButton(
                    text = "Wyloguj",
                    onClick = {
                        viewModel.logout {
                            navController.navigate(DriverRoute.Login.route) {
                                popUpTo(DriverRoute.Login.route) { inclusive = true }
                            }
                        }
                    },
                    secondary = true,
                )
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
        DriverInputField(
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
            label = label,
            keyboardType = keyboardType,
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
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
                Text(label, modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
