package com.future.ultimate.driver.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalContext
import com.future.ultimate.core.common.model.DriverRoute
import com.future.ultimate.core.common.ui.theme.FlotaTheme
import com.future.ultimate.core.common.ui.theme.FlotaThemeMode
import com.future.ultimate.core.common.ui.theme.topBarContainerColor
import com.future.ultimate.driver.DriverApp
import com.future.ultimate.driver.ui.screen.DriverChangePasswordScreen
import com.future.ultimate.driver.ui.screen.DriverLoginScreen
import com.future.ultimate.driver.ui.screen.DriverMileageScreen
import com.future.ultimate.driver.ui.screen.DriverVehicleReportScreen

private val driverRoutes = listOf(
    DriverRoute.Login,
    DriverRoute.ChangePassword,
    DriverRoute.Mileage,
    DriverRoute.VehicleReport,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverRoot() {
    FlotaTheme(mode = FlotaThemeMode.Dark) {
        var language by remember { mutableStateOf(DriverLanguage.Polish) }
        var languageMenuExpanded by remember { mutableStateOf(false) }
        val app = LocalContext.current.applicationContext as DriverApp
        val session by app.container.repository.observeSession().collectAsStateWithLifecycle(initialValue = null)
        val navController = rememberNavController()
        val backStack by navController.currentBackStackEntryAsState()
        val startDestination = when {
            session == null -> DriverRoute.Login.route
            session?.changePasswordRequired == true -> DriverRoute.ChangePassword.route
            else -> DriverRoute.Mileage.route
        }
        val currentRoute = backStack?.destination.currentDriverRoute()

        androidx.compose.runtime.CompositionLocalProvider(LocalDriverLanguage provides language) {
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
                                text = currentRoute.localizedTitle(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        actions = {
                            IconButton(onClick = { languageMenuExpanded = true }) {
                                Text(text = language.flag)
                            }
                            DropdownMenu(
                                expanded = languageMenuExpanded,
                                onDismissRequest = { languageMenuExpanded = false },
                            ) {
                                DriverLanguage.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text("${option.flag} ${option.label}") },
                                        onClick = {
                                            language = option
                                            languageMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        },
                    )
                }
            ) { padding ->
                key(startDestination) {
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(padding),
                    ) {
                        composable(DriverRoute.Login.route) { DriverLoginScreen(navController) }
                        composable(DriverRoute.ChangePassword.route) { DriverChangePasswordScreen(navController) }
                        composable(DriverRoute.Mileage.route) { DriverMileageScreen(navController) }
                        composable(DriverRoute.VehicleReport.route) { DriverVehicleReportScreen(navController) }
                    }
                }
            }
        }
    }
}

private fun NavDestination?.currentDriverRoute(): DriverRoute? =
    driverRoutes.firstOrNull { route ->
        this?.hierarchy?.any { destination -> destination.route == route.route } == true
    }

@Composable
private fun DriverRoute?.localizedTitle(): String = when (this) {
    DriverRoute.Login -> tr("Logowanie", "Inicio de sesión")
    DriverRoute.ChangePassword -> tr("Zmiana hasła", "Cambiar contraseña")
    DriverRoute.Mileage -> tr("Przebieg", "Kilometraje")
    DriverRoute.VehicleReport -> tr("Raport stanu samochodu", "Informe del vehículo")
    null -> tr("Panel kierowcy", "Panel del conductor")
}
