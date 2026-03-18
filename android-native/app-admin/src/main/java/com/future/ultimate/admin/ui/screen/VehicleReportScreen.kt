package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.future.ultimate.core.common.model.VehicleReportDraft

@Composable
fun VehicleReportScreen() {
    val state = remember { mutableStateOf(VehicleReportDraft()) }
    ScreenColumn("Raport stanu samochodu", "Formularz raportu stanu pojazdu") {
        item {
            Column {
                listOf(
                    "Marka" to state.value.marka,
                    "Rejestracja" to state.value.rej,
                    "Przebieg" to state.value.przebieg,
                    "Poziom oleju" to state.value.olej,
                    "Wskaźnik paliwa" to state.value.paliwo,
                    "Rodzaj paliwa" to state.value.rodzajPaliwa,
                    "Lewy przedni" to state.value.lp,
                    "Prawy przedni" to state.value.pp,
                    "Lewy tylny" to state.value.lt,
                    "Prawy tylny" to state.value.pt,
                    "Nowe uszkodzenia" to state.value.uszkodzenia,
                    "Od kiedy?" to state.value.odKiedy,
                    "Przegląd / Service" to state.value.serwis,
                    "Przegląd techniczny" to state.value.przeglad,
                    "Uwagi" to state.value.uwagi,
                ).forEach { (label, value) ->
                    OutlinedTextField(value = value, onValueChange = {}, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
                }
                listOf("Trójkąt", "Kamizelki", "Koło zapasowe", "Dowód rejestracyjny", "Apteczka").forEach {
                    androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = false, onCheckedChange = {})
                        Text(it)
                    }
                }
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Zapisz PDF") }
            }
        }
    }
}
