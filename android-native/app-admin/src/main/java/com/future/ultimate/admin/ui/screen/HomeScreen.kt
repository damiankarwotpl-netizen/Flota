package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
fun HomeScreen(navController: NavController) {
    val shortcuts = listOf(
        HomeShortcut("Kontakt", Icons.Rounded.Call, AdminRoute.Contacts),
        HomeShortcut("Auta", Icons.Rounded.DirectionsCar, AdminRoute.Cars),
        HomeShortcut("Raport", Icons.Rounded.Description, AdminRoute.VehicleReport),
        HomeShortcut("Odzież", Icons.Rounded.Checkroom, AdminRoute.Clothes),
        HomeShortcut("Paski", Icons.Rounded.ReceiptLong, AdminRoute.Payroll),
        HomeShortcut("Kadry", Icons.Rounded.Badge, AdminRoute.Workers),
        HomeShortcut("Zakłady", Icons.Rounded.Factory, AdminRoute.Plants),
        HomeShortcut("Opcje", Icons.Rounded.Settings, AdminRoute.Settings),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 18.dp, bottom = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(14.dp),
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

@Composable
private fun HomeShortcutTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .size(width = 86.dp, height = 108.dp)
            .clickable(onClick = onClick),
        shape = FlotaThemeDefaults.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier.size(58.dp),
                shape = FlotaThemeDefaults.pillShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                ),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(34.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                text = label,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
