package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun CarsScreen() {
    val query = remember { mutableStateOf("") }
    ScreenColumn("Samochody", "Flota pojazdów • szybkie akcje") {
        item {
            Column {
                OutlinedTextField(query.value, { query.value = it }, label = { Text("Szukaj: nazwa / rejestracja / kierowca") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("+ DODAJ SAMOCHÓD") }
            }
        }
        items(listOf("Auto • Zmień kierowcę • Dodaj przebieg • Potwierdź serwis • Usuń"))
    }
}
