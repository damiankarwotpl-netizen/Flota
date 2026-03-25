package com.future.ultimate.driver.ui.screen

import android.app.DatePickerDialog
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

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
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = singleLine,
        readOnly = readOnly,
        shape = RoundedCornerShape(18.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
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
    val context = LocalContext.current
    val viewModel: DriverVehicleReportViewModel = viewModel(factory = DriverViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val draft = uiState.draft
    var captureDashboardPhoto by remember { mutableStateOf(false) }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        val savedPath = bitmap?.saveReportPhoto(context)
        if (savedPath.isNullOrBlank()) return@rememberLauncherForActivityResult
        val updatedDraft = if (captureDashboardPhoto) {
            draft.copy(dashboardPhotoPath = savedPath)
        } else {
            draft.copy(photoPaths = draft.photoPaths + savedPath)
        }
        viewModel.updateDraft(updatedDraft)
        captureDashboardPhoto = false
    }
    val requiredPhotoCount = 6
    val hasMinimumPhotos = draft.photoPaths.size >= requiredPhotoCount
    val needsDashboardPhoto = draft.warningLights
    val hasRequiredDashboardPhoto = !needsDashboardPhoto || draft.dashboardPhotoPath.isNotBlank()
    val isReportReadyToSave = hasMinimumPhotos && hasRequiredDashboardPhoto

    DriverScreen(
        title = tr("Raport stanu samochodu", "Informe del vehículo"),
        subtitle = tr(
            "Uzupełnij pola formularza, dodaj zdjęcia i wygeneruj PDF.",
            "Completa el formulario, agrega fotos y genera el PDF.",
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
            DriverSectionCard(title = tr("Zdjęcia samochodu", "Fotos del vehículo")) {
                Text(
                    tr(
                        "Wymagane zdjęcia: 1) przód+prawy bok, 2) przód+lewy bok, 3) tył+prawy bok, 4) tył+lewy bok, 5) wnętrze przód, 6) wnętrze tył.",
                        "Fotos obligatorias: 1) frente+lado derecho, 2) frente+lado izquierdo, 3) trasera+lado derecho, 4) trasera+lado izquierdo, 5) interior delantero, 6) interior trasero.",
                    ),
                )
                Text("${tr("Dodano", "Añadidas")}: ${draft.photoPaths.size}/$requiredPhotoCount")
                DriverActionButton(
                    text = tr("Zrób zdjęcie samochodu", "Tomar foto del vehículo"),
                    onClick = {
                        captureDashboardPhoto = false
                        photoLauncher.launch(null)
                    },
                )
                if (draft.warningLights) {
                    DriverActionButton(
                        text = tr("Zrób zdjęcie deski rozdzielczej", "Tomar foto del tablero"),
                        onClick = {
                            captureDashboardPhoto = true
                            photoLauncher.launch(null)
                        },
                    )
                    Text(
                        if (draft.dashboardPhotoPath.isNotBlank()) {
                            tr("Zdjęcie deski: dodane", "Foto del tablero: añadida")
                        } else {
                            tr("Zdjęcie deski: wymagane", "Foto del tablero: obligatoria")
                        },
                    )
                }
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
                    enabled = !uiState.isSaving && isReportReadyToSave,
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
                if (!isReportReadyToSave) {
                    StatusMessage(
                        tr(
                            "Przed zapisem dodaj wymagane zdjęcia samochodu (minimum 6) oraz zdjęcie deski, jeśli są lampki ostrzegawcze.",
                            "Antes de guardar, agrega las fotos requeridas del vehículo (mínimo 6) y foto del tablero si hay luces de advertencia.",
                        ),
                        emphasis = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun editableFields(draft: VehicleReportDraft, onDraftChange: (VehicleReportDraft) -> Unit) {
    val context = LocalContext.current
    Text(tr("Dane podstawowe", "Datos básicos"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    DriverInputField(value = draft.marka, onValueChange = {}, label = tr("Marka", "Marca"), enabled = false)
    DriverInputField(value = draft.rej, onValueChange = {}, label = tr("Rejestracja", "Matrícula"), enabled = false)
    DriverInputField(value = draft.filledBy, onValueChange = {}, label = tr("Wypełnione przez (login)", "Rellenado por (usuario)"), enabled = false)
    DriverInputField(value = draft.przebieg, onValueChange = {}, label = tr("Przebieg", "Kilometraje"), enabled = false)
    Text(tr("Ilość miejsc", "Cantidad de asientos"))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (4..9).forEach { seats ->
            DriverActionButton(
                text = seats.toString(),
                onClick = { onDraftChange(draft.copy(seats = seats.toString())) },
                secondary = draft.seats != seats.toString(),
                enabled = true,
            )
        }
    }

    Text(tr("Stan pojazdu", "Estado del vehículo"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Text("${tr("Wskaźnik paliwa", "Indicador de combustible")}: OK")
    DriverInputField(
        value = draft.tireProducer,
        onValueChange = { onDraftChange(draft.copy(tireProducer = it)) },
        label = tr("Producent opon", "Fabricante de neumáticos"),
    )
    yesNoSelector(
        label = tr("Bez uszkodzeń", "Sin daños"),
        value = draft.noDamage,
        onValueChange = { onDraftChange(draft.copy(noDamage = it)) },
    )
    if (!draft.noDamage) {
        DriverInputField(
            value = draft.damageSince,
            onValueChange = {},
            label = tr("Od kiedy", "Desde cuándo"),
            readOnly = true,
            onClick = { showDatePicker(context, draft.damageSince) { onDraftChange(draft.copy(damageSince = it)) } },
        )
        DriverInputField(
            value = draft.damageDescription,
            onValueChange = { onDraftChange(draft.copy(damageDescription = it)) },
            label = tr("Opisz uszkodzenie", "Describe el daño"),
            singleLine = false,
        )
    }
    yesNoSelector(
        label = tr("Czy samochód został wysprzątany/umyty", "¿El vehículo fue limpiado/lavado?"),
        value = draft.cleaned,
        onValueChange = { onDraftChange(draft.copy(cleaned = it)) },
    )
    yesNoSelector(
        label = tr("Czy na wyświetlaczu są lampki ostrzegawcze", "¿Hay luces de advertencia en el tablero?"),
        value = draft.warningLights,
        onValueChange = { hasWarnings ->
            onDraftChange(
                draft.copy(
                    warningLights = hasWarnings,
                    warningLightsDescription = if (hasWarnings) draft.warningLightsDescription else "",
                    dashboardPhotoPath = if (hasWarnings) draft.dashboardPhotoPath else "",
                ),
            )
        },
    )
    if (draft.warningLights) {
        DriverInputField(
            value = draft.warningLightsDescription,
            onValueChange = { onDraftChange(draft.copy(warningLightsDescription = it)) },
            label = tr("Opisz lampkę ostrzegawczą", "Describe la luz de advertencia"),
        )
    }

    Text(tr("Stan opon", "Estado de neumáticos"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    tireStateSelector(tr("Lewy przedni", "Delantero izquierdo"), draft.lp) { onDraftChange(draft.copy(lp = it)) }
    tireStateSelector(tr("Prawy przedni", "Delantero derecho"), draft.pp) { onDraftChange(draft.copy(pp = it)) }
    tireStateSelector(tr("Lewy tylny", "Trasero izquierdo"), draft.lt) { onDraftChange(draft.copy(lt = it)) }
    tireStateSelector(tr("Prawy tylny", "Trasero derecho"), draft.pt) { onDraftChange(draft.copy(pt = it)) }
}

@Composable
private fun checklist(draft: VehicleReportDraft, onDraftChange: (VehicleReportDraft) -> Unit) {
    checklistRow(
        label = tr("Trójkąt", "Triángulo"),
        checked = draft.trojkat,
        onCheckedChange = { onDraftChange(draft.copy(trojkat = it)) },
    )
    checklistRow(
        label = tr("Kamizelki", "Chalecos"),
        checked = draft.kamizelki,
        onCheckedChange = { onDraftChange(draft.copy(kamizelki = it)) },
    )
    checklistRow(
        label = tr("Koło zapasowe", "Rueda de repuesto"),
        checked = draft.kolo,
        onCheckedChange = { onDraftChange(draft.copy(kolo = it)) },
    )
    checklistRow(
        label = tr("Dowód rejestracyjny", "Permiso de circulación"),
        checked = draft.dowod,
        onCheckedChange = { onDraftChange(draft.copy(dowod = it)) },
    )
    checklistRow(
        label = tr("Apteczka", "Botiquín"),
        checked = draft.apteczka,
        onCheckedChange = { onDraftChange(draft.copy(apteczka = it)) },
    )
}

@Composable
private fun checklistRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
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
                onCheckedChange = onCheckedChange,
            )
            Text(
                label,
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun yesNoSelector(
    label: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
) {
    Text(label)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { onValueChange(true) }, modifier = Modifier.weight(1f)) {
            Text(if (value) "✓ ${tr("Tak", "Sí")}" else tr("Tak", "Sí"))
        }
        Button(onClick = { onValueChange(false) }, modifier = Modifier.weight(1f)) {
            Text(if (!value) "✓ ${tr("Nie", "No")}" else tr("Nie", "No"))
        }
    }
}

@Composable
private fun tireStateSelector(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Text(label)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            tr("OK", "OK"),
            tr("Średni", "Medio"),
            tr("Do wymiany", "Para cambiar"),
        ).forEach { option ->
            Button(
                onClick = { onValueChange(option) },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (value == option) "✓ $option" else option)
            }
        }
    }
}

private fun showDatePicker(
    context: android.content.Context,
    initialDate: String,
    onDateSelected: (String) -> Unit,
) {
    val initial = runCatching { LocalDate.parse(initialDate) }.getOrNull() ?: LocalDate.now()
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected(LocalDate.of(year, month + 1, dayOfMonth).toString())
        },
        initial.year,
        initial.monthValue - 1,
        initial.dayOfMonth,
    ).show()
}

private fun Bitmap.saveReportPhoto(context: android.content.Context): String? = runCatching {
    val outputDir = File(context.filesDir, "vehicle-report-photos").apply { mkdirs() }
    val outputFile = File(outputDir, "photo_${System.currentTimeMillis()}.jpg")
    FileOutputStream(outputFile).use { out ->
        compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    outputFile.absolutePath
}.getOrNull()
