package com.future.ultimate.admin.ui.screen

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Factory
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.future.ultimate.core.common.model.AdminRoute
import com.future.ultimate.core.common.ui.theme.FlotaThemeDefaults

private data class HomeShortcut(
    val label: String,
    val icon: ImageVector,
    val route: AdminRoute,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    onEnableDarkTheme: () -> Unit,
    onEnableLightTheme: () -> Unit,
) {
    val activity = LocalContext.current as? Activity
    val shortcuts = listOf(
        HomeShortcut("Kontakty", Icons.Rounded.Call, AdminRoute.Contacts),
        HomeShortcut("Samochody", Icons.Rounded.DirectionsCar, AdminRoute.Cars),
        HomeShortcut("Raport auta", Icons.Rounded.Description, AdminRoute.VehicleReport),
        HomeShortcut("Odzież", Icons.Rounded.Checkroom, AdminRoute.Clothes),
        HomeShortcut("Paski", Icons.Rounded.ReceiptLong, AdminRoute.Payroll),
        HomeShortcut("Pracownicy", Icons.Rounded.Badge, AdminRoute.Workers),
        HomeShortcut("Zakłady", Icons.Rounded.Factory, AdminRoute.Plants),
        HomeShortcut("Ustawienia", Icons.Rounded.Settings, AdminRoute.Settings),
    )

    ScreenColumn(
        title = "Panel główny aplikacji",
        subtitle = "Najważniejsze moduły i skróty w jednym miejscu.",
    ) {
        item {
            SectionCard(
                title = "Moduły",
                subtitle = "Dotknij ikonę, aby od razu przejść do wybranego modułu.",
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = 4,
                ) {
                    shortcuts.forEach { shortcut ->
                        HomeShortcutTile(
                            label = shortcut.label,
                            icon = shortcut.icon,
                            onClick = { navController.navigate(shortcut.route.route) },
                        )
                    }
                }
            }
        }
        item {
            SectionCard(
                title = "Sterowanie aplikacją",
                subtitle = "Szybka zmiana motywu i zamknięcie aplikacji.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onEnableDarkTheme, modifier = Modifier.fillMaxWidth()) {
                        Text("Dark")
                    }
                    OutlinedButton(onClick = onEnableLightTheme, modifier = Modifier.fillMaxWidth()) {
                        Text("Light")
                    }
                    Button(onClick = { activity?.finish() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Wyjście")
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeShortcutTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .size(width = 78.dp, height = 94.dp)
            .clickable(onClick = onClick),
        shape = FlotaThemeDefaults.cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
