package com.future.ultimate.admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.future.ultimate.admin.ui.screen.HousingScreen
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
import com.future.ultimate.core.common.ui.theme.FlotaTheme
import com.future.ultimate.core.common.ui.theme.FlotaThemeMode
import com.future.ultimate.core.common.ui.theme.topBarContainerColor

private val allRoutes = listOf(
    AdminRoute.Home,
    AdminRoute.Contacts,
    AdminRoute.Cars,
    AdminRoute.VehicleReport,
    AdminRoute.Clothes,
    AdminRoute.Payroll,
    AdminRoute.Housing,
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
    var themeMode by rememberSaveable { mutableStateOf(FlotaThemeMode.Dark) }

    FlotaTheme(mode = themeMode) {
        val navController = rememberNavController()
        val backStack by navController.currentBackStackEntryAsState()
        val currentRoute = backStack?.destination.currentAdminRoute()

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.topBarContainerColor(),
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    title = {
                        Text(
                            text = currentRoute?.title ?: "Future Ultimate Admin",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                )
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 10.dp,
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(adminBottomMenuItems, key = { item -> item.route.route }) { item ->
                            val selected = backStack?.destination?.route == item.route.route
                            IconButton(
                                onClick = {
                                    navController.navigate(item.route.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    contentColor = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                                modifier = Modifier.size(52.dp),
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label.replace('\n', ' '),
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = AdminRoute.Home.route,
                modifier = Modifier.padding(padding),
            ) {
                composable(AdminRoute.Home.route) { HomeScreen(navController = navController) }
                composable(AdminRoute.Contacts.route) { ContactsScreen() }
                composable(AdminRoute.Cars.route) { CarsScreen() }
                composable(AdminRoute.VehicleReport.route) { VehicleReportScreen() }
                composable(AdminRoute.Clothes.route) { ClothesScreen() }
                composable(AdminRoute.Payroll.route) { PayrollScreen(navController) }
                composable(AdminRoute.Housing.route) { HousingScreen() }
                composable(AdminRoute.Table.route) { TableScreen() }
                composable(AdminRoute.Email.route) { EmailScreen(navController) }
                composable(AdminRoute.Smtp.route) { SmtpScreen() }
                composable(AdminRoute.Template.route) { TemplateScreen() }
                composable(AdminRoute.Reports.route) { ReportsScreen() }
                composable(AdminRoute.Workers.route) { WorkersScreen() }
                composable(AdminRoute.Plants.route) { PlantsScreen() }
                composable(AdminRoute.Settings.route) {
                    SettingsScreen(
                        navController = navController,
                        onEnableDarkTheme = { themeMode = FlotaThemeMode.Dark },
                        onEnableLightTheme = { themeMode = FlotaThemeMode.Light },
                        onEnablePinkTheme = { themeMode = FlotaThemeMode.Pink },
                    )
                }
            }
        }
    }
}

private fun NavDestination?.currentAdminRoute(): AdminRoute? =
    allRoutes.firstOrNull { route ->
        this?.hierarchy?.any { destination -> destination.route == route.route } == true
    }
