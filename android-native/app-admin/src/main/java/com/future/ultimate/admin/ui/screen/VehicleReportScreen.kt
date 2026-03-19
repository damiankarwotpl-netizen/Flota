package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.future.ultimate.admin.AdminApp
import com.future.ultimate.admin.ui.viewmodel.AdminViewModelFactory
import com.future.ultimate.admin.ui.viewmodel.VehicleReportViewModel

@Composable
fun VehicleReportScreen() {
    val app = LocalContext.current.applicationContext as AdminApp
    val viewModel: VehicleReportViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenColumn("Raport stanu samochodu", "Formularz raportu stanu pojazdu") {
        item {
            Column {
                val draft = uiState.draft
                listOf(
                    "Marka" to draft.marka,
                    "Rejestracja" to draft.rej,
                    "Przebieg" to draft.przebieg,
                    "Poziom oleju" to draft.olej,
                    "Wskaźnik paliwa" to draft.paliwo,
                    "Rodzaj paliwa" to draft.rodzajPaliwa,
                    "Lewy przedni" to draft.lp,
                    "Prawy przedni" to draft.pp,
                    "Lewy tylny" to draft.lt,
                    "Prawy tylny" to draft.pt,
                    "Nowe uszkodzenia" to draft.uszkodzenia,
                    "Od kiedy?" to draft.odKiedy,
                    "Przegląd / Service" to draft.serwis,
                    "Przegląd techniczny" to draft.przeglad,
                    "Uwagi" to draft.uwagi,
                ).forEach { (label, value) ->
                    androidx.compose.material3.OutlinedTextField(
                        value = value,
                        onValueChange = { newValue ->
                            val updated = when (label) {
                                "Marka" -> draft.copy(marka = newValue)
                                "Rejestracja" -> draft.copy(rej = newValue)
                                "Przebieg" -> draft.copy(przebieg = newValue)
                                "Poziom oleju" -> draft.copy(olej = newValue)
                                "Wskaźnik paliwa" -> draft.copy(paliwo = newValue)
                                "Rodzaj paliwa" -> draft.copy(rodzajPaliwa = newValue)
                                "Lewy przedni" -> draft.copy(lp = newValue)
                                "Prawy przedni" -> draft.copy(pp = newValue)
                                "Lewy tylny" -> draft.copy(lt = newValue)
                                else -> draft.copy(
                                    pt = if (label == "Prawy tylny") newValue else draft.pt,
                                    uszkodzenia = if (label == "Nowe uszkodzenia") newValue else draft.uszkodzenia,
                                    odKiedy = if (label == "Od kiedy?") newValue else draft.odKiedy,
                                    serwis = if (label == "Przegląd / Service") newValue else draft.serwis,
                                    przeglad = if (label == "Przegląd techniczny") newValue else draft.przeglad,
                                    uwagi = if (label == "Uwagi") newValue else draft.uwagi,
                                )
                            }
                            viewModel.updateDraft(updated)
                        },
                        label = { Text(label) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                listOf(
                    "Trójkąt" to draft.trojkat,
                    "Kamizelki" to draft.kamizelki,
                    "Koło zapasowe" to draft.kolo,
                    "Dowód rejestracyjny" to draft.dowod,
                    "Apteczka" to draft.apteczka,
                ).forEach { (label, checked) ->
                    androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { value ->
                                viewModel.updateDraft(
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
                        Text(label)
                    }
                }
                Button(onClick = viewModel::exportPdf, modifier = Modifier.fillMaxWidth()) {
                    Text(if (uiState.isSaving) "Zapisywanie..." else "Zapisz PDF")
                }
                uiState.exportMessage?.let { Text(it) }
            }
        }
    }
}
