package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    ScreenColumn("Raport samochody", "Formularz zgodny z aplikacją kierowcy") {
        item {
            SectionCard(title = "Dane podstawowe") {
                VehicleField("Marka", draft.marka) { viewModel.updateDraft(draft.copy(marka = it)) }
                VehicleField("Rejestracja", draft.rej) { viewModel.updateDraft(draft.copy(rej = it)) }
                VehicleField("Wypełnione przez (login)", draft.filledBy) { viewModel.updateDraft(draft.copy(filledBy = it)) }
                VehicleField("Przebieg", draft.przebieg) { viewModel.updateDraft(draft.copy(przebieg = it)) }
                VehicleField("Ilość miejsc", draft.seats) { viewModel.updateDraft(draft.copy(seats = it)) }
            }
        }

        item {
            SectionCard(title = "Stan pojazdu") {
                VehicleField("Rodzaj paliwa", draft.rodzajPaliwa) { viewModel.updateDraft(draft.copy(rodzajPaliwa = it)) }
                VehicleField("Poziom oleju", draft.olej) { viewModel.updateDraft(draft.copy(olej = it)) }
                VehicleField("Wskaźnik paliwa", draft.paliwo) { viewModel.updateDraft(draft.copy(paliwo = it)) }
                VehicleField("Producent opon", draft.tireProducer) { viewModel.updateDraft(draft.copy(tireProducer = it)) }
                YesNoRow("Bez uszkodzeń", draft.noDamage) { viewModel.updateDraft(draft.copy(noDamage = it)) }
                if (!draft.noDamage) {
                    VehicleField("Data uszkodzenia", draft.damageSince) { viewModel.updateDraft(draft.copy(damageSince = it)) }
                    VehicleField("Opis uszkodzenia", draft.damageDescription, singleLine = false) {
                        viewModel.updateDraft(draft.copy(damageDescription = it))
                    }
                }
                YesNoRow("Czy auto wyczyszczone/umyte", draft.cleaned) { viewModel.updateDraft(draft.copy(cleaned = it)) }
                YesNoRow("Czy są lampki ostrzegawcze", draft.warningLights) { checked ->
                    viewModel.updateDraft(
                        draft.copy(
                            warningLights = checked,
                            warningLightsDescription = if (checked) draft.warningLightsDescription else "",
                        ),
                    )
                }
                if (draft.warningLights) {
                    VehicleField("Opis lampki ostrzegawczej", draft.warningLightsDescription, singleLine = false) {
                        viewModel.updateDraft(draft.copy(warningLightsDescription = it))
                    }
                }
            }
        }

        item {
            SectionCard(title = "Stan opon") {
                VehicleField("Lewy przedni", draft.lp) { viewModel.updateDraft(draft.copy(lp = it)) }
                VehicleField("Prawy przedni", draft.pp) { viewModel.updateDraft(draft.copy(pp = it)) }
                VehicleField("Lewy tylny", draft.lt) { viewModel.updateDraft(draft.copy(lt = it)) }
                VehicleField("Prawy tylny", draft.pt) { viewModel.updateDraft(draft.copy(pt = it)) }
            }
        }

        item {
            SectionCard(title = "Wyposażenie") {
                ChecklistRow("Trójkąt", draft.trojkat) { viewModel.updateDraft(draft.copy(trojkat = it)) }
                ChecklistRow("Kamizelki", draft.kamizelki) { viewModel.updateDraft(draft.copy(kamizelki = it)) }
                ChecklistRow("Koło zapasowe", draft.kolo) { viewModel.updateDraft(draft.copy(kolo = it)) }
                ChecklistRow("Dowód rejestracyjny", draft.dowod) { viewModel.updateDraft(draft.copy(dowod = it)) }
                ChecklistRow("Apteczka", draft.apteczka) { viewModel.updateDraft(draft.copy(apteczka = it)) }
            }
        }

        item {
            SectionCard(title = "Serwis / dodatkowe") {
                VehicleField("Przegląd / Service", draft.serwis) { viewModel.updateDraft(draft.copy(serwis = it)) }
                VehicleField("Przegląd techniczny", draft.przeglad) { viewModel.updateDraft(draft.copy(przeglad = it)) }
                VehicleField("Uwagi", draft.uwagi, singleLine = false) { viewModel.updateDraft(draft.copy(uwagi = it)) }
                Text(
                    "Zdjęcia samochodu: ${draft.photoPaths.size}, zdjęcia uszkodzeń: ${draft.damagePhotoPaths.size}",
                    style = MaterialTheme.typography.bodySmall,
                )
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
private fun VehicleField(
    label: String,
    value: String,
    singleLine: Boolean = true,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
    )
}

@Composable
private fun ChecklistRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}

@Composable
private fun YesNoRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { onCheckedChange(true) },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (checked) "✓ Tak" else "Tak")
            }
            Button(
                onClick = { onCheckedChange(false) },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (!checked) "✓ Nie" else "Nie")
            }
        }
    }
}
