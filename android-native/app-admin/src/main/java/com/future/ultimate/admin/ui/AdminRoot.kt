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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    AdminNavItem(AdminRoute.Contacts, Icons.Outlined.Call, "Kontakty"),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRoot() {
    var useDarkTheme by rememberSaveable { androidx.compose.runtime.mutableStateOf(true) }

    MaterialTheme(colorScheme = if (useDarkTheme) darkColorScheme() else lightColorScheme()) {
        val navController = rememberNavController()
        val backStack by navController.currentBackStackEntryAsState()
        val currentRoute = backStack?.destination.currentAdminRoute()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(currentRoute?.title ?: "Future Ultimate Admin")
                    },
                )
            },
            bottomBar = {
                NavigationBar {
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
                        )
                    }
                }
            },
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
