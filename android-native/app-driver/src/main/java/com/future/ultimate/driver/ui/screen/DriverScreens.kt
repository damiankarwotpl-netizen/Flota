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
import com.future.ultimate.driver.sync.DriverSyncNotifier
import com.future.ultimate.driver.ui.tr
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
        title = tr("Login kierowcy", "Inicio de sesión del conductor"),
        subtitle = tr(
            "Zaloguj się, aby dodać przebieg lub wysłać raport stanu pojazdu.",
            "Inicia sesión para agregar kilometraje o enviar un informe del vehículo.",
        ),
    ) {
        item {
            DriverSectionCard(
                title = tr("Endpoint synchronizacji", "Endpoint de sincronización"),
                subtitle = tr(
                    "APK kierowcy działa domyślnie na stałym endpointcie. Edycja jest ukryta za hasłem serwisowym.",
                    "La app del conductor usa por defecto un endpoint fijo. La edición está protegida por contraseña de servicio.",
                ),
            ) {
                StatusMessage(tr("Aktywny endpoint APK:", "Endpoint activo de la app:"), emphasis = true)
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
                        label = tr("Endpoint zdalnego syncu kierowców", "Endpoint remoto de sincronización"),
                        singleLine = false,
                    )
                    DriverActionButton(
                        text = if (uiState.isSavingRemoteSettings) tr("Zapisywanie endpointu...", "Guardando endpoint...") else tr("Zapisz endpoint", "Guardar endpoint"),
                        onClick = viewModel::saveRemoteSettings,
                        enabled = !uiState.isSavingRemoteSettings && uiState.remoteApiUrl.isNotBlank(),
                        secondary = true,
                    )
                    DriverActionButton(
                        text = if (uiState.isValidatingRemoteSettings) tr("Sprawdzanie endpointu...", "Validando endpoint...") else tr("Sprawdź endpoint", "Validar endpoint"),
                        onClick = viewModel::validateRemoteSettings,
                        enabled = !uiState.isValidatingRemoteSettings && uiState.remoteApiUrl.isNotBlank(),
                        secondary = true,
                    )
                    DriverActionButton(
                        text = tr("Ukryj edycję endpointu", "Ocultar edición de endpoint"),
                        onClick = viewModel::lockEndpointEditor,
                        secondary = true,
                    )
                } else {
                    DriverInputField(
                        value = uiState.endpointAccessPassword,
                        onValueChange = viewModel::updateEndpointAccessPassword,
                        label = tr("Hasło serwisowe do edycji endpointu", "Contraseña de servicio para editar endpoint"),
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    DriverActionButton(
                        text = tr("Odblokuj edycję endpointu", "Desbloquear edición de endpoint"),
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
                    label = tr("Login", "Usuario"),
                )
                DriverInputField(
                    value = uiState.password,
                    onValueChange = viewModel::updatePassword,
                    label = tr("Hasło", "Contraseña"),
                    visualTransformation = PasswordVisualTransformation(),
                )
                DriverActionButton(
                    text = if (uiState.isLoading) tr("Logowanie...", "Iniciando sesión...") else tr("Zaloguj się", "Iniciar sesión"),
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
        title = tr("Zmiana hasła", "Cambiar contraseña"),
        subtitle = tr(
            "Pierwsze logowanie wymaga ustawienia nowego hasła przed przejściem dalej.",
            "En el primer inicio de sesión debes establecer una nueva contraseña antes de continuar.",
        ),
    ) {
        item {
            DriverSectionCard {
                DriverInputField(
                    value = uiState.login,
                    onValueChange = {},
                    label = tr("Login", "Usuario"),
                    enabled = false,
                )
                DriverInputField(
                    value = uiState.password,
                    onValueChange = viewModel::updatePassword,
                    label = tr("Nowe hasło", "Nueva contraseña"),
                    visualTransformation = PasswordVisualTransformation(),
                )
                DriverActionButton(
                    text = if (uiState.isLoading) tr("Zapisywanie...", "Guardando...") else tr("Zapisz hasło", "Guardar contraseña"),
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
    val context = LocalContext.current
    val app = LocalContext.current.applicationContext as DriverApp
    val viewModel: DriverMileageViewModel = viewModel(factory = DriverViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DriverScreen(
        title = tr("Przebieg", "Kilometraje"),
        subtitle = tr(
            "Dodaj aktualny stan licznika i sprawdź status synchronizacji danych.",
            "Agrega el kilometraje actual y revisa el estado de sincronización.",
        ),
    ) {
        item {
            DriverSectionCard {
                StatusMessage(
                    message = if (uiState.driverName.isNotBlank()) {
                        "${tr("Kierowca", "Conductor")}: ${uiState.driverName}"
                    } else {
                        tr("Brak aktywnego kierowcy.", "No hay conductor activo.")
                    },
                    emphasis = true,
                )
                if (uiState.registration.isBlank()) {
                    StatusMessage(
                        message = tr(
                            "W tej chwili nie masz przypisanego samochodu. Zaloguj się ponownie po zmianie przypisania przez admina.",
                            "Ahora mismo no tienes un vehículo asignado. Inicia sesión de nuevo después de que el administrador cambie la asignación.",
                        ),
                        emphasis = true,
                    )
                } else {
                    StatusMessage("${tr("Aktualnie przypisane auto", "Vehículo asignado")}: ${uiState.registration}")
                }
                StatusMessage("${tr("Status synchronizacji", "Estado de sincronización")}: ${uiState.syncStatus}")
                StatusMessage("${tr("Kolejka", "Cola")}: ${uiState.pendingSyncCount} • ${tr("Oczekujący przebieg", "Kilometraje pendiente")}: ${uiState.queuedMileage.ifBlank { "-" }}")
                StatusMessage("${tr("Ostatnia próba", "Último intento")}: ${uiState.lastAttemptAt.ifBlank { "-" }}")
                StatusMessage("${tr("Ostatnia synchronizacja", "Última sincronización")}: ${uiState.lastSyncedAt.ifBlank { "-" }}")
                if (uiState.syncError.isNotBlank()) {
                    StatusMessage("${tr("Błąd synchronizacji", "Error de sincronización")}: ${uiState.syncError}", emphasis = true)
                }
            }
        }
        item {
            DriverSectionCard {
                DriverInputField(
                    value = uiState.registration,
                    onValueChange = {},
                    label = tr("Rejestracja", "Matrícula"),
                    enabled = false,
                )
                if (uiState.availableRegistrations.size > 1) {
                    StatusMessage(tr("Wybierz aktywną rejestrację:", "Selecciona la matrícula activa:"))
                    uiState.availableRegistrations.forEach { registration ->
                        DriverActionButton(
                            text = if (registration == uiState.registration) "✓ $registration" else registration,
                            onClick = { viewModel.selectRegistration(registration) },
                            secondary = registration != uiState.registration,
                        )
                    }
                }
                DriverInputField(
                    value = uiState.mileage,
                    onValueChange = viewModel::updateMileage,
                    label = tr("Przebieg", "Kilometraje"),
                    keyboardType = KeyboardType.Number,
                )
                DriverActionButton(
                    text = if (uiState.isSaving) tr("Zapisywanie...", "Guardando...") else tr("Zapisz przebieg", "Guardar kilometraje"),
                    onClick = viewModel::save,
                    enabled = !uiState.isSaving && uiState.registration.isNotBlank() && uiState.mileage.isNotBlank(),
                )
                uiState.status?.let { StatusMessage(it, emphasis = true) }
            }
        }
        item {
            DriverSectionCard {
                DriverActionButton(
                    text = tr("Przejdź do raportu stanu samochodu", "Ir al informe del vehículo"),
                    onClick = { navController.navigate(DriverRoute.VehicleReport.route) },
                )
                DriverActionButton(
                    text = tr("Test powiadomienia przypomnienia", "Probar notificación de recordatorio"),
                    onClick = { DriverSyncNotifier.notifyMileageReminder(context, uiState.registration) },
                    secondary = true,
                )
                DriverActionButton(
                    text = tr("Wyloguj", "Cerrar sesión"),
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
        title = tr("Raport stanu samochodu", "Informe del vehículo"),
        subtitle = tr(
            "Uzupełnij pola formularza, zaznacz wyposażenie i wygeneruj PDF.",
            "Completa el formulario, marca el equipamiento y genera el PDF.",
        ),
    ) {
        item {
            DriverSectionCard {
                StatusMessage(
                    message = if (uiState.driverName.isNotBlank()) {
                        "${tr("Kierowca", "Conductor")}: ${uiState.driverName}"
                    } else {
                        tr("Brak aktywnego kierowcy.", "No hay conductor activo.")
                    },
                    emphasis = true,
                )
                if (uiState.availableRegistrations.size > 1) {
                    StatusMessage(tr("Wybierz auto do raportu:", "Selecciona vehículo para el informe:"))
                    uiState.availableRegistrations.forEach { registration ->
                        DriverActionButton(
                            text = if (registration == draft.rej) "✓ $registration" else registration,
                            onClick = { viewModel.selectRegistration(registration) },
                            secondary = registration != draft.rej,
                        )
                    }
                }
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
                    text = tr("Wróć do przebiegu", "Volver al kilometraje"),
                    onClick = { navController.navigate(DriverRoute.Mileage.route) },
                    secondary = true,
                )
                DriverActionButton(
                    text = if (uiState.isSaving) tr("Zapisywanie...", "Guardando...") else tr("Zapisz PDF", "Guardar PDF"),
                    onClick = viewModel::save,
                    enabled = !uiState.isSaving,
                )
                DriverActionButton(
                    text = tr("Wyloguj", "Cerrar sesión"),
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
    data class ReportFieldGroup(val titlePl: String, val titleEs: String, val fields: List<ReportField>)
    data class ReportField(
        val id: String,
        val labelPl: String,
        val labelEs: String,
        val value: String,
        val keyboardType: KeyboardType,
    )
    val groups = listOf(
        ReportFieldGroup(
            titlePl = "Dane pojazdu",
            titleEs = "Datos del vehículo",
            fields = listOf(
                ReportField("marka", "Marka", "Marca", draft.marka, KeyboardType.Text),
                ReportField("rej", "Rejestracja", "Matrícula", draft.rej, KeyboardType.Text),
                ReportField("przebieg", "Przebieg", "Kilometraje", draft.przebieg, KeyboardType.Number),
            ),
        ),
        ReportFieldGroup(
            titlePl = "Stan pojazdu",
            titleEs = "Estado del vehículo",
            fields = listOf(
                ReportField("olej", "Poziom oleju", "Nivel de aceite", draft.olej, KeyboardType.Text),
                ReportField("paliwo", "Wskaźnik paliwa", "Indicador de combustible", draft.paliwo, KeyboardType.Text),
                ReportField("rodzajPaliwa", "Rodzaj paliwa", "Tipo de combustible", draft.rodzajPaliwa, KeyboardType.Text),
                ReportField("uszkodzenia", "Nowe uszkodzenia", "Nuevos daños", draft.uszkodzenia, KeyboardType.Text),
                ReportField("odKiedy", "Od kiedy?", "¿Desde cuándo?", draft.odKiedy, KeyboardType.Text),
            ),
        ),
        ReportFieldGroup(
            titlePl = "Stan opon / przeglądy",
            titleEs = "Estado de neumáticos / revisiones",
            fields = listOf(
                ReportField("lp", "Lewy przedni", "Delantero izquierdo", draft.lp, KeyboardType.Text),
                ReportField("pp", "Prawy przedni", "Delantero derecho", draft.pp, KeyboardType.Text),
                ReportField("lt", "Lewy tylny", "Trasero izquierdo", draft.lt, KeyboardType.Text),
                ReportField("pt", "Prawy tylny", "Trasero derecho", draft.pt, KeyboardType.Text),
                ReportField("serwis", "Przegląd / Service", "Revisión / Servicio", draft.serwis, KeyboardType.Text),
                ReportField("przeglad", "Przegląd techniczny", "Inspección técnica", draft.przeglad, KeyboardType.Text),
            ),
        ),
        ReportFieldGroup(
            titlePl = "Uwagi",
            titleEs = "Observaciones",
            fields = listOf(
                ReportField("uwagi", "Uwagi", "Observaciones", draft.uwagi, KeyboardType.Text),
            ),
        ),
    )

    groups.forEach { group ->
        Text(
            text = tr(group.titlePl, group.titleEs),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        group.fields.forEach { field ->
            DriverInputField(
                value = field.value,
                onValueChange = { newValue ->
                    onDraftChange(
                        when (field.id) {
                            "marka" -> draft.copy(marka = newValue)
                            "rej" -> draft.copy(rej = newValue)
                            "przebieg" -> draft.copy(przebieg = newValue)
                            "olej" -> draft.copy(olej = newValue)
                            "paliwo" -> draft.copy(paliwo = newValue)
                            "rodzajPaliwa" -> draft.copy(rodzajPaliwa = newValue)
                            "lp" -> draft.copy(lp = newValue)
                            "pp" -> draft.copy(pp = newValue)
                            "lt" -> draft.copy(lt = newValue)
                            "pt" -> draft.copy(pt = newValue)
                            "uszkodzenia" -> draft.copy(uszkodzenia = newValue)
                            "odKiedy" -> draft.copy(odKiedy = newValue)
                            "serwis" -> draft.copy(serwis = newValue)
                            "przeglad" -> draft.copy(przeglad = newValue)
                            "uwagi" -> draft.copy(uwagi = newValue)
                            else -> draft
                        },
                    )
                },
                label = tr(field.labelPl, field.labelEs),
                keyboardType = field.keyboardType,
            )
        }
    }
}

@Composable
private fun checklist(draft: VehicleReportDraft, onDraftChange: (VehicleReportDraft) -> Unit) {
    data class ChecklistItem(val id: String, val labelPl: String, val labelEs: String, val checked: Boolean)
    listOf(
        ChecklistItem("trojkat", "Trójkąt", "Triángulo", draft.trojkat),
        ChecklistItem("kamizelki", "Kamizelki", "Chalecos", draft.kamizelki),
        ChecklistItem("kolo", "Koło zapasowe", "Rueda de repuesto", draft.kolo),
        ChecklistItem("dowod", "Dowód rejestracyjny", "Permiso de circulación", draft.dowod),
        ChecklistItem("apteczka", "Apteczka", "Botiquín", draft.apteczka),
    ).forEach { item ->
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
                    checked = item.checked,
                    onCheckedChange = { value ->
                        onDraftChange(
                            when (item.id) {
                                "trojkat" -> draft.copy(trojkat = value)
                                "kamizelki" -> draft.copy(kamizelki = value)
                                "kolo" -> draft.copy(kolo = value)
                                "dowod" -> draft.copy(dowod = value)
                                else -> draft.copy(apteczka = value)
                            },
                        )
                    },
                )
                Text(
                    tr(item.labelPl, item.labelEs),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
