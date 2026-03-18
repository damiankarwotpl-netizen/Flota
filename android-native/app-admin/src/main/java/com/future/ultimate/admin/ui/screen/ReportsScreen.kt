package com.future.ultimate.admin.ui.screen

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ReportsScreen() {
    ScreenColumn("Historia sesji", "Historia sesji i raporty z wysyłek") {
        items(listOf("Sesja: data • OK / BŁĘDY / POMINIĘTE • Pokaż logi"))
    }
}
