package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.future.ultimate.core.common.model.AdminRoute

private data class HomeShortcut(
    val label: String,
    val description: String,
    val route: AdminRoute,
)

@Composable
fun HomeScreen(navController: NavController) {
    val shortcuts = listOf(
        HomeShortcut("Kontakty", "Szybki dostęp do kontaktów i numerów alarmowych.", AdminRoute.Contacts),
        HomeShortcut("Samochody", "Lista pojazdów i ich aktualnych danych.", AdminRoute.Cars),
        HomeShortcut("Raport stanu auta", "Weryfikacja stanu pojazdu i eksport raportu.", AdminRoute.VehicleReport),
        HomeShortcut("Ubranie robocze", "Rozmiary, zamówienia i historia wydań.", AdminRoute.Clothes),
        HomeShortcut("Paski", "Eksport oraz podgląd pasków wynagrodzeń.", AdminRoute.Payroll),
        HomeShortcut("Pracownicy", "Dane osobowe i przypisania pracowników.", AdminRoute.Workers),
        HomeShortcut("Zakłady", "Zarządzanie lokalizacjami i jednostkami.", AdminRoute.Plants),
        HomeShortcut("Ustawienia", "Integracje, SMTP oraz narzędzia serwisowe.", AdminRoute.Settings),
    )

    ScreenColumn(
        title = "Panel główny aplikacji",
        subtitle = "Najważniejsze moduły i skróty w jednym miejscu.",
    ) {
        item {
            SectionCard(
                title = "Co się zmieniło?",
                subtitle = "Układ jest bardziej czytelny i nastawiony na szybkie wejście do modułów.",
            ) {
                Text("Funkcje pozostały bez zmian — poprawiliśmy przede wszystkim nawigację i prezentację treści.")
            }
        }

        shortcuts.forEach { shortcut ->
            item {
                SectionCard(
                    title = shortcut.label,
                    subtitle = shortcut.description,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { navController.navigate(shortcut.route.route) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Otwórz moduł")
                        }
                    }
                }
            }
        }
    }
}
