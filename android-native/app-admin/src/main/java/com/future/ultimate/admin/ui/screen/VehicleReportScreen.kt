package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.future.ultimate.admin.AdminApp
import com.future.ultimate.admin.ui.viewmodel.AdminViewModelFactory
import com.future.ultimate.admin.ui.viewmodel.VehicleReportViewModel
import com.future.ultimate.core.common.model.VehicleReportDraft

@Composable
fun VehicleReportScreen() {
    val app = LocalContext.current.applicationContext as AdminApp
    val viewModel: VehicleReportViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val draft = uiState.draft

    ScreenColumn("Raport samochody", "Formularz zgodny z aplikacją kierowcy 1:1") {
        item {
            SectionCard {
                VehicleEditableFields(draft = draft, onDraftChange = viewModel::updateDraft)
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
}

@Composable
private fun VehicleEditableFields(
    draft: VehicleReportDraft,
    onDraftChange: (VehicleReportDraft) -> Unit,
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
        VehicleField(
            value = draft.damageSince,
            onChange = { onDraftChange(draft.copy(damageSince = it)) },
            label = "Data uszkodzenia",
        )
        VehicleField(
            value = draft.damageDescription,
            onChange = { onDraftChange(draft.copy(damageDescription = it)) },
            label = "Nowe uszkodzenie (opis)",
            singleLine = false,
        )
        Text("Zdjęcia nowego uszkodzenia: ${draft.damagePhotoPaths.size}")
        if (draft.damagePhotoPaths.isNotEmpty()) {
            Button(
                onClick = { onDraftChange(draft.copy(damagePhotoPaths = emptyList())) },
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
    Text(label)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("OK", "Średni", "Do wymiany").forEach { option ->
            Button(
                onClick = { onValueChange(option) },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = if (value == option) "✓ $option" else option,
                    fontSize = 13.sp,
                    maxLines = 1,
                )
            }
        }
    }
}
