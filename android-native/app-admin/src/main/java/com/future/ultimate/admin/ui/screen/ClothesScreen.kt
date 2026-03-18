package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun ClothesScreen() {
    val selected = remember { mutableIntStateOf(0) }
    val tabs = listOf("Rozmiary", "Zamówienia", "Raporty")
    ScreenColumn("Ubranie robocze", "Moduły odzieżowe 1:1") {
        item {
            Column {
                TabRow(selectedTabIndex = selected.intValue) {
                    tabs.forEachIndexed { index, title ->
                        Tab(selected = selected.intValue == index, onClick = { selected.intValue = index }, text = { Text(title) })
                    }
                }
                when (selected.intValue) {
                    0 -> {
                        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Dodaj rozmiar pracownika") }
                        Text("Lista rozmiarów pracowników")
                    }
                    1 -> {
                        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Nowe zamówienie") }
                        Text("Lista zamówień i statusów")
                    }
                    else -> {
                        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Export CSV") }
                        Text("Raporty wydanych ubrań i statystyki")
                    }
                }
            }
        }
    }
}
