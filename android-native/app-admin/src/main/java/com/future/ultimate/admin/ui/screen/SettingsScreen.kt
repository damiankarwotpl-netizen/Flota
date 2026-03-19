package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.future.ultimate.admin.AdminApp
import com.future.ultimate.admin.ui.viewmodel.AdminViewModelFactory
import com.future.ultimate.admin.ui.viewmodel.SettingsViewModel
import com.future.ultimate.core.common.model.AdminRoute

@Composable
fun SettingsScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as AdminApp
    val viewModel: SettingsViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenColumn("Ustawienia", "Ustawienia i narzędzia systemowe") {
        item {
            Column {
                Text("Kontakty: ${uiState.stats.contactCount}   Pracownicy: ${uiState.stats.workerCount}")
                Text("Auta: ${uiState.stats.carCount}   Zakłady: ${uiState.stats.plantCount}")
                Text("Rozmiary odzieży: ${uiState.stats.clothesSizeCount}   Zamówienia: ${uiState.stats.clothesOrderCount}")
                Text("Historia wydań odzieży: ${uiState.stats.clothesHistoryCount}")
                Button(onClick = { navController.navigate(AdminRoute.Reports.route) }, modifier = Modifier.fillMaxWidth()) { Text("Pokaż raporty sesji") }
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Wczytaj arkusz płac") }
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Dodaj bazę danych") }
                Button(onClick = { navController.navigate(AdminRoute.Smtp.route) }, modifier = Modifier.fillMaxWidth()) { Text("Ustawienia SMTP") }
                Button(onClick = { navController.navigate(AdminRoute.Template.route) }, modifier = Modifier.fillMaxWidth()) { Text("Edytuj szablon email") }
            }
        }
    }
}
