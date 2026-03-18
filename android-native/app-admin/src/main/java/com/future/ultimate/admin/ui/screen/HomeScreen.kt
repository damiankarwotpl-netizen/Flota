package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.future.ultimate.core.common.model.AdminRoute

@Composable
fun HomeScreen(navController: NavController) {
    ScreenColumn(
        title = "Panel główny aplikacji",
        subtitle = "Wybierz moduł, aby kontynuować",
    ) {
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Nowy wygląd UI • Funkcje bez zmian")
                }
            }
        }
        listOf(
            "Kontakty" to AdminRoute.Contacts,
            "Samochody" to AdminRoute.Cars,
            "Raport stanu auta" to AdminRoute.VehicleReport,
            "Ubranie robocze" to AdminRoute.Clothes,
            "Paski" to AdminRoute.Payroll,
            "Pracownicy" to AdminRoute.Workers,
            "Zakłady" to AdminRoute.Plants,
            "Ustawienia" to AdminRoute.Settings,
        ).forEach { (label, route) ->
            item {
                Button(
                    onClick = { navController.navigate(route.route) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(label)
                }
            }
        }
    }
}
