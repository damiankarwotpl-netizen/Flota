package com.future.ultimate.admin.ui.screen

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.FileProvider
import com.future.ultimate.admin.AdminApp
import com.future.ultimate.admin.ui.viewmodel.AdminViewModelFactory
import com.future.ultimate.admin.ui.viewmodel.VehicleReportViewModel
import com.future.ultimate.core.common.model.VehicleReportDraft
import java.io.File
import java.time.LocalDate

@Composable
fun VehicleReportScreen() {
    val app = LocalContext.current.applicationContext as AdminApp
    val context = LocalContext.current
    val viewModel: VehicleReportViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val draft = uiState.draft
    val requiredPhotoCount = 6
    val guidedPhotoSteps = buildList {
        add("Przód + prawy bok")
        add("Przód + lewy bok")
        add("Tył + prawy bok")
        add("Tył + lewy bok")
        add("Wnętrze przód")
        add("Wnętrze tył")
        if (draft.warningLights) add("Deska rozdzielcza (lampki ostrzegawcze)")
    }
    val capturedSteps = draft.photoPaths.size + if (draft.dashboardPhotoPath.isNotBlank()) 1 else 0
    val isGuidedCaptureComplete = capturedSteps >= guidedPhotoSteps.size && guidedPhotoSteps.isNotEmpty()
    val nextStepIndex = capturedSteps.coerceAtMost((guidedPhotoSteps.size - 1).coerceAtLeast(0))
    val nextStepLabel = guidedPhotoSteps.getOrElse(nextStepIndex) { "-" }
    val cameraLaunchErrorMessage = "Nie udało się uruchomić aparatu."
    var isGuidedCaptureActive by remember { mutableStateOf(false) }
    var pendingCaptureMode by remember { mutableStateOf("vehicle") }
    var pendingCapturePath by remember { mutableStateOf<String?>(null) }
    var showGuidedStepDialog by remember { mutableStateOf(false) }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        val savedPath = pendingCapturePath
        pendingCapturePath = null
        if (!success || savedPath.isNullOrBlank()) {
            if (pendingCaptureMode == "guided") isGuidedCaptureActive = false
            savedPath?.let { File(it).delete() }
            return@rememberLauncherForActivityResult
        }
        if (pendingCaptureMode == "damage") {
            viewModel.updateDraft(draft.copy(damagePhotoPaths = draft.damagePhotoPaths + savedPath))
            return@rememberLauncherForActivityResult
        }
        val shouldSaveDashboardPhoto = draft.warningLights && draft.photoPaths.size >= requiredPhotoCount
        val updatedDraft = if (shouldSaveDashboardPhoto) {
            draft.copy(dashboardPhotoPath = savedPath)
        } else {
            draft.copy(photoPaths = draft.photoPaths + savedPath)
        }
        viewModel.updateDraft(updatedDraft)
        if (pendingCaptureMode == "guided") {
            val updatedCapturedSteps = updatedDraft.photoPaths.size + if (updatedDraft.dashboardPhotoPath.isNotBlank()) 1 else 0
            if (updatedCapturedSteps < guidedPhotoSteps.size) {
                showGuidedStepDialog = true
            } else {
                isGuidedCaptureActive = false
            }
        }
    }
    val launchHighResPhotoCapture = { mode: String ->
        createReportPhotoUri(context)?.let { (uri, path) ->
            pendingCaptureMode = mode
            pendingCapturePath = path
            photoLauncher.launch(uri)
        } ?: Toast.makeText(context, cameraLaunchErrorMessage, Toast.LENGTH_SHORT).show()
    }

    ScreenColumn("Raport samochody", "Formularz zgodny z aplikacją kierowcy 1:1") {
        item {
            SectionCard {
                VehicleEditableFields(
                    draft = draft,
                    onDraftChange = viewModel::updateDraft,
                    onAddDamagePhoto = { launchHighResPhotoCapture("damage") },
                    onClearDamagePhotos = { viewModel.updateDraft(draft.copy(damagePhotoPaths = emptyList())) },
                )
            }
        }

        item {
            SectionCard(title = "Wyposażenie") {
                VehicleChecklist(draft = draft, onDraftChange = viewModel::updateDraft)
            }
        }

        item {
            SectionCard(title = "Zdjęcia samochodu") {
                Text(
                    "Wymagane zdjęcia: 1) przód+prawy bok, 2) przód+lewy bok, 3) tył+prawy bok, 4) tył+lewy bok, 5) wnętrze przód, 6) wnętrze tył.",
                )
                val capturedSteps = draft.photoPaths.size + if (draft.dashboardPhotoPath.isNotBlank()) 1 else 0
                val requiredSteps = 6 + if (draft.warningLights) 1 else 0
                Text("Dodano: $capturedSteps/$requiredSteps")
                if (draft.warningLights) {
                    Text(if (draft.dashboardPhotoPath.isNotBlank()) "Zdjęcie deski: dodane" else "Zdjęcie deski: wymagane")
                }
                Text("Zdjęcia uszkodzeń: ${draft.damagePhotoPaths.size}")
                Button(
                    onClick = {
                        isGuidedCaptureActive = true
                        showGuidedStepDialog = true
                    },
                    enabled = !isGuidedCaptureComplete && !isGuidedCaptureActive,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        when {
                            isGuidedCaptureActive -> "Trwa sesja zdjęć..."
                            isGuidedCaptureComplete -> "Wszystkie wymagane zdjęcia dodane"
                            else -> "Dodaj zdjęcia"
                        },
                    )
                }
                TextButton(
                    onClick = {
                        isGuidedCaptureActive = false
                        showGuidedStepDialog = false
                        viewModel.updateDraft(draft.copy(photoPaths = emptyList(), dashboardPhotoPath = ""))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Zacznij od nowa (wyczyść zdjęcia)")
                }
            }
        }

        item {
            SectionCard {
                Button(onClick = viewModel::exportPdf, modifier = Modifier.fillMaxWidth()) {
                    Text(if (uiState.isSaving) "Zapisywanie..." else "Zapisz PDF")
                }
                uiState.exportMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }

    if (showGuidedStepDialog && isGuidedCaptureActive && !isGuidedCaptureComplete) {
        AlertDialog(
            onDismissRequest = {
                showGuidedStepDialog = false
                isGuidedCaptureActive = false
            },
            title = { Text("Następne zdjęcie") },
            text = { Text(nextStepLabel) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showGuidedStepDialog = false
                        launchHighResPhotoCapture("guided")
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showGuidedStepDialog = false
                        isGuidedCaptureActive = false
                    },
                ) { Text("Anuluj") }
            },
        )
    }
}

@Composable
private fun VehicleEditableFields(
    draft: VehicleReportDraft,
    onDraftChange: (VehicleReportDraft) -> Unit,
    onAddDamagePhoto: () -> Unit,
    onClearDamagePhotos: () -> Unit,
) {
    Text("Dane podstawowe", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    VehicleField(value = draft.marka, onChange = { onDraftChange(draft.copy(marka = it)) }, label = "Marka")
    VehicleField(value = draft.rej, onChange = {}, label = "Rejestracja", enabled = false)
    VehicleField(value = draft.filledBy, onChange = {}, label = "Wypełnione przez (login)", enabled = false)
    VehicleField(value = draft.przebieg, onChange = { onDraftChange(draft.copy(przebieg = it)) }, label = "Przebieg")

    Text("Ilość miejsc")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (4..9).forEach { seats ->
            Button(
                onClick = { onDraftChange(draft.copy(seats = seats.toString())) },
                modifier = Modifier.width(72.dp),
            ) {
                Text(if (draft.seats == seats.toString()) "✓ $seats" else seats.toString())
            }
        }
    }

    Text("Stan pojazdu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Text("Rodzaj paliwa")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Benzyna", "Disel").forEach { fuelType ->
            Button(
                onClick = { onDraftChange(draft.copy(rodzajPaliwa = fuelType)) },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (draft.rodzajPaliwa == fuelType) "✓ $fuelType" else fuelType)
            }
        }
    }

    VehicleYesNoSelector(
        label = "Poziom oleju OK",
        value = draft.olej != "Niski",
        onValueChange = { isOilOk -> onDraftChange(draft.copy(olej = if (isOilOk) "OK" else "Niski")) },
    )

    VehicleField(
        value = draft.tireProducer,
        onChange = { onDraftChange(draft.copy(tireProducer = it)) },
        label = "Producent opon",
    )

    VehicleYesNoSelector(
        label = "Bez uszkodzeń",
        value = draft.noDamage,
        onValueChange = { onDraftChange(draft.copy(noDamage = it)) },
    )

    if (!draft.noDamage) {
        val context = LocalContext.current
        Button(
            onClick = {
                showDatePicker(context, draft.damageSince) { onDraftChange(draft.copy(damageSince = it)) }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (draft.damageSince.isBlank()) "Wybierz datę uszkodzenia" else "Data uszkodzenia: ${draft.damageSince}")
        }
        VehicleField(
            value = draft.damageDescription,
            onChange = { onDraftChange(draft.copy(damageDescription = it)) },
            label = "Nowe uszkodzenie (opis)",
            singleLine = false,
        )
        Button(onClick = onAddDamagePhoto, modifier = Modifier.fillMaxWidth()) {
            Text("Dodaj zdjęcie uszkodzenia")
        }
        Text("Zdjęcia nowego uszkodzenia: ${draft.damagePhotoPaths.size}")
        if (draft.damagePhotoPaths.isNotEmpty()) {
            Button(
                onClick = onClearDamagePhotos,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Usuń zdjęcia uszkodzenia")
            }
        }
    }

    VehicleYesNoSelector(
        label = "Czy samochód został wysprzątany/umyty",
        value = draft.cleaned,
        onValueChange = { onDraftChange(draft.copy(cleaned = it)) },
    )

    VehicleYesNoSelector(
        label = "Czy na wyświetlaczu są lampki ostrzegawcze",
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
        VehicleField(
            value = draft.warningLightsDescription,
            onChange = { onDraftChange(draft.copy(warningLightsDescription = it)) },
            label = "Opisz lampkę ostrzegawczą",
        )
    }

    Text("Stan opon", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    VehicleTireStateSelector("Lewy przedni", draft.lp) { onDraftChange(draft.copy(lp = it)) }
    VehicleTireStateSelector("Prawy przedni", draft.pp) { onDraftChange(draft.copy(pp = it)) }
    VehicleTireStateSelector("Lewy tylny", draft.lt) { onDraftChange(draft.copy(lt = it)) }
    VehicleTireStateSelector("Prawy tylny", draft.pt) { onDraftChange(draft.copy(pt = it)) }
}

@Composable
private fun VehicleField(
    label: String,
    value: String,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        enabled = enabled,
    )
}

@Composable
private fun VehicleChecklist(draft: VehicleReportDraft, onDraftChange: (VehicleReportDraft) -> Unit) {
    VehicleChecklistRow(
        label = "Trójkąt",
        checked = draft.trojkat,
        onCheckedChange = { onDraftChange(draft.copy(trojkat = it)) },
    )
    VehicleChecklistRow(
        label = "Kamizelki",
        checked = draft.kamizelki,
        onCheckedChange = { onDraftChange(draft.copy(kamizelki = it)) },
    )
    VehicleChecklistRow(
        label = "Koło zapasowe",
        checked = draft.kolo,
        onCheckedChange = { onDraftChange(draft.copy(kolo = it)) },
    )
    VehicleChecklistRow(
        label = "Dowód rejestracyjny",
        checked = draft.dowod,
        onCheckedChange = { onDraftChange(draft.copy(dowod = it)) },
    )
    VehicleChecklistRow(
        label = "Apteczka",
        checked = draft.apteczka,
        onCheckedChange = { onDraftChange(draft.copy(apteczka = it)) },
    )
}

@Composable
private fun VehicleChecklistRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (checked) "✓ $label" else label,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(
                onClick = { onCheckedChange(!checked) },
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                Text(if (checked) "Odznacz" else "Zaznacz")
            }
        }
    }
}

private fun createReportPhotoUri(context: android.content.Context): Pair<Uri, String>? = runCatching {
    val outputDir = File(context.filesDir, "vehicle-report-photos").apply { mkdirs() }
    val outputFile = File(outputDir, "photo_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        outputFile,
    )
    uri to outputFile.absolutePath
}.getOrNull()

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

@Composable
private fun VehicleYesNoSelector(
    label: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
) {
    Text(label)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { onValueChange(true) }, modifier = Modifier.weight(1f)) {
            Text(if (value) "✓ Tak" else "Tak")
        }
        Button(onClick = { onValueChange(false) }, modifier = Modifier.weight(1f)) {
            Text(if (!value) "✓ Nie" else "Nie")
        }
    }
}

@Composable
private fun VehicleTireStateSelector(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    var isPickerOpen by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val options = listOf("OK", "Średni", "Do wymiany")
    Text(label)
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text("Stan opony") },
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { state ->
                if (state.isFocused) {
                    isPickerOpen = true
                    focusManager.clearFocus(force = true)
                }
            },
        readOnly = true,
    )
    if (isPickerOpen) {
        AlertDialog(
            onDismissRequest = { isPickerOpen = false },
            title = { Text("Wybierz stan opony • $label") },
            text = {
                androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { option ->
                        Button(
                            onClick = {
                                onValueChange(option)
                                isPickerOpen = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (value == option) "✓ $option" else option, fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isPickerOpen = false }) { Text("Zamknij") }
            }
        )
    }
}
