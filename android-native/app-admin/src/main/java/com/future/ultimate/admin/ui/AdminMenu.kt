package com.future.ultimate.admin.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Factory
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.House
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.future.ultimate.core.common.model.AdminRoute

data class AdminMenuItem(
    val route: AdminRoute,
    val icon: ImageVector,
    val label: String,
)

val adminModuleMenuItems = listOf(
    AdminMenuItem(AdminRoute.Contacts, Icons.Rounded.Call, "Kontakty"),
    AdminMenuItem(AdminRoute.Cars, Icons.Rounded.DirectionsCar, "Samochody"),
    AdminMenuItem(AdminRoute.VehicleReport, Icons.Rounded.Description, "Raport\nsamochody"),
    AdminMenuItem(AdminRoute.Clothes, Icons.Rounded.Checkroom, "Ubrania\nrobocze"),
    AdminMenuItem(AdminRoute.Payroll, Icons.Rounded.ReceiptLong, "Wypłaty"),
    AdminMenuItem(AdminRoute.Workers, Icons.Rounded.Badge, "Pracownicy"),
    AdminMenuItem(AdminRoute.Plants, Icons.Rounded.Factory, "Zakłady"),
    AdminMenuItem(AdminRoute.Housing, Icons.Rounded.House, "Mieszkania"),
    AdminMenuItem(AdminRoute.Settings, Icons.Rounded.Settings, "Ustawienia"),
)

val adminBottomMenuItems = listOf(
    AdminMenuItem(AdminRoute.Home, Icons.Rounded.Home, "Start"),
) + adminModuleMenuItems
