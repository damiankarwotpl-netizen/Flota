package com.future.ultimate.admin.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.future.ultimate.admin.ui.screen.CarsScreen
import com.future.ultimate.admin.ui.screen.ClothesScreen
import com.future.ultimate.admin.ui.screen.ContactsScreen
import com.future.ultimate.admin.ui.screen.EmailScreen
import com.future.ultimate.admin.ui.screen.HomeScreen
import com.future.ultimate.admin.ui.screen.PayrollScreen
import com.future.ultimate.admin.ui.screen.PlantsScreen
import com.future.ultimate.admin.ui.screen.ReportsScreen
import com.future.ultimate.admin.ui.screen.SettingsScreen
import com.future.ultimate.admin.ui.screen.SmtpScreen
import com.future.ultimate.admin.ui.screen.TableScreen
import com.future.ultimate.admin.ui.screen.TemplateScreen
import com.future.ultimate.admin.ui.screen.VehicleReportScreen
import com.future.ultimate.admin.ui.screen.WorkersScreen
import com.future.ultimate.core.common.model.AdminRoute

private data class AdminNavItem(
    val route: AdminRoute,
    val icon: ImageVector,
    val shortLabel: String,
)

private val bottomRoutes = listOf(
    AdminNavItem(AdminRoute.Home, Icons.Outlined.Home, "Start"),
    AdminNavItem(AdminRoute.Contacts, Icons.Outlined.Call, "Czaty"),
    AdminNavItem(AdminRoute.Cars, Icons.Outlined.DirectionsCar, "Auta"),
    AdminNavItem(AdminRoute.Clothes, Icons.Outlined.Checkroom, "Odzież"),
    AdminNavItem(AdminRoute.Payroll, Icons.Outlined.ReceiptLong, "Paski"),
    AdminNavItem(AdminRoute.Settings, Icons.Outlined.Settings, "Ustawienia"),
)

private val allRoutes = listOf(
    AdminRoute.Home,
    AdminRoute.Contacts,
    AdminRoute.Cars,
    AdminRoute.VehicleReport,
    AdminRoute.Clothes,
    AdminRoute.Payroll,
    AdminRoute.Table,
    AdminRoute.Email,
    AdminRoute.Smtp,
    AdminRoute.Template,
    AdminRoute.Reports,
    AdminRoute.Workers,
    AdminRoute.Plants,
    AdminRoute.Settings,
)

private val WhatsAppLightColors = lightColorScheme(
    primary = Color(0xFF128C7E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7F8F0),
    onPrimaryContainer = Color(0xFF062E28),
    secondary = Color(0xFF25D366),
    onSecondary = Color(0xFF072117),
    secondaryContainer = Color(0xFFDCFCE7),
    onSecondaryContainer = Color(0xFF0E2A1B),
    background = Color(0xFFEFFAF5),
    onBackground = Color(0xFF111B21),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111B21),
    surfaceVariant = Color(0xFFE7F2EE),
    onSurfaceVariant = Color(0xFF4A635A),
    outline = Color(0xFFB8D0C7),
)

private val WhatsAppDarkColors = darkColorScheme(
    primary = Color(0xFF25D366),
    onPrimary = Color(0xFF062A1C),
    primaryContainer = Color(0xFF103B32),
    onPrimaryContainer = Color(0xFFD8FBE8),
    secondary = Color(0xFF128C7E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF173F38),
    onSecondaryContainer = Color(0xFFD4F8F0),
    background = Color(0xFF0B141A),
    onBackground = Color(0xFFE9EDEF),
    surface = Color(0xFF111B21),
    onSurface = Color(0xFFE9EDEF),
    surfaceVariant = Color(0xFF202C33),
    onSurfaceVariant = Color(0xFF9DB3AA),
    outline = Color(0xFF31433D),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRoot() {
    var useDarkTheme by rememberSaveable { androidx.compose.runtime.mutableStateOf(true) }

    MaterialTheme(colorScheme = if (useDarkTheme) WhatsAppDarkColors else WhatsAppLightColors) {
        val navController = rememberNavController()
        val backStack by navController.currentBackStackEntryAsState()
        val currentRoute = backStack?.destination.currentAdminRoute()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(currentRoute?.title ?: "Flota Messenger")
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    bottomRoutes.forEach { item ->
                        NavigationBarItem(
                            selected = backStack?.destination?.route == item.route.route,
                            onClick = {
                                navController.navigate(item.route.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            label = { Text(item.shortLabel) },
                            icon = { Icon(item.icon, contentDescription = item.route.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondary,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.secondary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = AdminRoute.Home.route,
                modifier = Modifier.padding(padding),
            ) {
                composable(AdminRoute.Home.route) {
                    HomeScreen(
                        navController = navController,
                        onEnableDarkTheme = { useDarkTheme = true },
                        onEnableLightTheme = { useDarkTheme = false },
                    )
                }
                composable(AdminRoute.Contacts.route) { ContactsScreen() }
                composable(AdminRoute.Cars.route) { CarsScreen() }
                composable(AdminRoute.VehicleReport.route) { VehicleReportScreen() }
                composable(AdminRoute.Clothes.route) { ClothesScreen() }
                composable(AdminRoute.Payroll.route) { PayrollScreen(navController) }
                composable(AdminRoute.Table.route) { TableScreen() }
                composable(AdminRoute.Email.route) { EmailScreen(navController) }
                composable(AdminRoute.Smtp.route) { SmtpScreen() }
                composable(AdminRoute.Template.route) { TemplateScreen() }
                composable(AdminRoute.Reports.route) { ReportsScreen() }
                composable(AdminRoute.Workers.route) { WorkersScreen() }
                composable(AdminRoute.Plants.route) { PlantsScreen() }
                composable(AdminRoute.Settings.route) { SettingsScreen(navController) }
            }
        }
    }
}

private fun NavDestination?.currentAdminRoute(): AdminRoute? =
    allRoutes.firstOrNull { route ->
        this?.hierarchy?.any { destination -> destination.route == route.route } == true
    }
