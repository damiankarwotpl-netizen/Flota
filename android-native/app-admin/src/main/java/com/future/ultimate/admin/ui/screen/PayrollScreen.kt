package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.future.ultimate.core.common.model.AdminRoute

@Composable
fun PayrollScreen(navController: NavController) {
    val autoSend = remember { mutableStateOf(false) }
    ScreenColumn("Moduł Paski", "Moduł płac i wysyłki") {
        item {
            Column {
                androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
                    Switch(checked = autoSend.value, onCheckedChange = { autoSend.value = it })
                    Text("AUTOMATYCZNA WYSYŁKA")
                }
                Text("Baza: 0 | Załączniki: 0")
                Text("Gotowy")
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Wczytaj arkusz płac") }
                Button(onClick = { navController.navigate(AdminRoute.Table.route) }, modifier = Modifier.fillMaxWidth()) { Text("Podgląd i eksport") }
                Button(onClick = { navController.navigate(AdminRoute.Template.route) }, modifier = Modifier.fillMaxWidth()) { Text("Edytuj szablon") }
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Dołącz załącznik") }
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Wyślij jeden plik") }
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Start masowa wysyłka") }
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("PAUZA/RESUME") }
                Button(onClick = { navController.navigate(AdminRoute.Reports.route) }, modifier = Modifier.fillMaxWidth()) { Text("Raporty sesji") }
            }
        }
    }
}
