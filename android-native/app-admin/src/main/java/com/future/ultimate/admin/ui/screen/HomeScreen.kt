package com.future.ultimate.admin.ui.screen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Factory
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.future.ultimate.core.common.model.AdminRoute

private data class HomeShortcut(
    val label: String,
    val description: String,
    val route: AdminRoute,
    val icon: ImageVector,
)

@Composable
fun HomeScreen(
    navController: NavController,
    onEnableDarkTheme: () -> Unit,
    onEnableLightTheme: () -> Unit,
) {
    val activity = LocalContext.current as? Activity
    val shortcuts = listOf(
        HomeShortcut("Kontakty", "Czaty, kontakt telefoniczny i szybkie akcje.", AdminRoute.Contacts, Icons.Outlined.Call),
        HomeShortcut("Samochody", "Pojazdy, przebiegi i dalsze działania.", AdminRoute.Cars, Icons.Outlined.DirectionsCar),
        HomeShortcut("Raport auta", "Podsumowanie stanu i eksport raportu.", AdminRoute.VehicleReport, Icons.Outlined.Verified),
        HomeShortcut("Ubranie robocze", "Rozmiary, zamówienia i historia wydań.", AdminRoute.Clothes, Icons.Outlined.Checkroom),
        HomeShortcut("Paski", "Wypłaty, kalkulacja i wysyłka.", AdminRoute.Payroll, Icons.Outlined.Paid),
        HomeShortcut("Pracownicy", "Dane kadrowe i przypisania.", AdminRoute.Workers, Icons.Outlined.Groups),
        HomeShortcut("Zakłady", "Lokalizacje oraz zakłady pracy.", AdminRoute.Plants, Icons.Outlined.Factory),
        HomeShortcut("Ustawienia", "Motyw, SMTP i narzędzia serwisowe.", AdminRoute.Settings, Icons.Outlined.Settings),
    )

    ScreenColumn(
        title = "Flota Messenger",
        subtitle = "Nowe, natywne UI inspirowane WhatsApp: szybki dostęp do modułów, czytelne karty i zielony motyw.",
    ) {
        item {
            SectionCard(
                title = "Wygląd aplikacji",
                subtitle = "Przełączaj motyw jak w nowoczesnym komunikatorze.",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onEnableLightTheme,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text("Jasny")
                    }
                    Button(
                        onClick = onEnableDarkTheme,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text("Ciemny")
                    }
                }
                OutlinedButton(
                    onClick = { activity?.finish() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text("Zamknij aplikację")
                }
            }
        }

        shortcuts.forEach { shortcut ->
            item {
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = shortcut.icon,
                                contentDescription = shortcut.label,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(shortcut.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(shortcut.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { navController.navigate(shortcut.route.route) },
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Text("Otwórz")
                        }
                    }
                }
            }
        }
    }
}
