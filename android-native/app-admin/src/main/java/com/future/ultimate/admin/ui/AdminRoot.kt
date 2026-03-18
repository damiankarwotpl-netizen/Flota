package com.future.ultimate.admin.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.future.ultimate.admin.ui.screen.ClothesScreen
import com.future.ultimate.admin.ui.screen.CarsScreen
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

private val bottomRoutes = listOf(
    AdminRoute.Home,
    AdminRoute.Contacts,
    AdminRoute.Cars,
    AdminRoute.Clothes,
    AdminRoute.Payroll,
    AdminRoute.Settings,
)

@Composable
fun AdminRoot() {
    MaterialTheme {
        val navController = rememberNavController()
        val backStack by navController.currentBackStackEntryAsState()
        Scaffold(
            bottomBar = {
                NavigationBar {
                    bottomRoutes.forEach { route ->
                        NavigationBarItem(
                            selected = backStack?.destination?.route == route.route,
                            onClick = { navController.navigate(route.route) },
                            label = { Text(route.title) },
                            icon = { Text(route.title.take(1)) },
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
                composable(AdminRoute.Home.route) { HomeScreen(navController) }
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
