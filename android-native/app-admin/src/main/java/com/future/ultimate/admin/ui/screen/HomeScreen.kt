package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.future.ultimate.admin.ui.adminModuleMenuItems
import com.future.ultimate.core.common.ui.theme.FlotaThemeDefaults

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(navController: NavController) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 18.dp, bottom = 24.dp),
    ) {
        val horizontalSpacing = 12.dp
        val verticalSpacing = 12.dp
        val tileWidth = ((maxWidth - horizontalSpacing * 2) / 3).coerceAtMost(108.dp)
        val tileHeight = when {
            tileWidth < 96.dp -> 112.dp
            tileWidth < 104.dp -> 118.dp
            else -> 124.dp
        }
        val iconContainerSize = when {
            tileWidth < 96.dp -> 50.dp
            tileWidth < 104.dp -> 54.dp
            else -> 58.dp
        }
        val iconSize = when {
            tileWidth < 96.dp -> 28.dp
            tileWidth < 104.dp -> 30.dp
            else -> 34.dp
        }

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            maxItemsInEachRow = 3,
        ) {
            adminModuleMenuItems.forEach { item ->
                HomeShortcutTile(
                    label = item.label,
                    contentDescription = item.label.replace('\n', ' '),
                    icon = item.icon,
                    tileWidth = tileWidth,
                    tileHeight = tileHeight,
                    iconContainerSize = iconContainerSize,
                    iconSize = iconSize,
                    onClick = { navController.navigate(item.route.route) },
                )
            }
        }
    }
}

@Composable
private fun HomeShortcutTile(
    label: String,
    contentDescription: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tileWidth: Dp,
    tileHeight: Dp,
    iconContainerSize: Dp,
    iconSize: Dp,
    onClick: () -> Unit,
) {
    val labelLines = label.lines()
    val labelLineHeight = if (tileWidth < 100.dp) 16.sp else 18.sp

    Card(
        modifier = Modifier
            .width(tileWidth)
            .heightIn(min = tileHeight, max = tileHeight)
            .clickable(onClick = onClick),
        shape = FlotaThemeDefaults.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Card(
                modifier = Modifier.size(iconContainerSize),
                shape = FlotaThemeDefaults.pillShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                ),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        modifier = Modifier.size(iconSize),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (labelLines.size > 1) 38.dp else 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                labelLines.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.labelLarge.copy(lineHeight = labelLineHeight),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = false,
                    )
                }
            }
        }
    }
}
