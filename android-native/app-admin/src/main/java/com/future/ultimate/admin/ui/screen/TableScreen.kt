package com.future.ultimate.admin.ui.screen

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.fillMaxWidth

@Composable
fun TableScreen() {
    ScreenColumn("Podgląd i eksport", "Tabela danych i eksport") {
        item { Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Kolumny") } }
        items(listOf("Wiersz 1 • ZAPISZ • WYŚLIJ", "Wiersz 2 • ZAPISZ • WYŚLIJ"))
    }
}
