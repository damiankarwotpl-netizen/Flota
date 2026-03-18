package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.future.ultimate.core.common.model.AdminRoute

@Composable
fun SettingsScreen(navController: NavController) {
    ScreenColumn("Ustawienia", "Ustawienia i narzędzia systemowe") {
        item {
            Column {
                Text("Kontakty: 0   Pracownicy: 0")
                Text("Auta: 0   Zakłady: 0")
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Pokaż logi") }
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Wczytaj arkusz płac") }
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Dodaj bazę danych") }
                Button(onClick = { navController.navigate(AdminRoute.Smtp.route) }, modifier = Modifier.fillMaxWidth()) { Text("Ustawienia SMTP") }
                Button(onClick = { navController.navigate(AdminRoute.Template.route) }, modifier = Modifier.fillMaxWidth()) { Text("Edytuj szablon email") }
            }
        }
    }
}
