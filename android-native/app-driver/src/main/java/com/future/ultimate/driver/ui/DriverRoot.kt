package com.future.ultimate.driver.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.future.ultimate.core.common.model.DriverRoute
import com.future.ultimate.driver.DriverApp
import com.future.ultimate.driver.ui.screen.DriverChangePasswordScreen
import com.future.ultimate.driver.ui.screen.DriverLoginScreen
import com.future.ultimate.driver.ui.screen.DriverMileageScreen
import com.future.ultimate.driver.ui.screen.DriverVehicleReportScreen

@Composable
fun DriverRoot() {
    MaterialTheme {
        val app = LocalContext.current.applicationContext as DriverApp
        val session by app.container.repository.observeSession().collectAsStateWithLifecycle(initialValue = null)
        val navController = rememberNavController()
        val startDestination = when {
            session == null -> DriverRoute.Login.route
            session?.changePasswordRequired == true -> DriverRoute.ChangePassword.route
            else -> DriverRoute.Mileage.route
        }
        NavHost(navController = navController, startDestination = startDestination) {
            composable(DriverRoute.Login.route) { DriverLoginScreen(navController) }
            composable(DriverRoute.ChangePassword.route) { DriverChangePasswordScreen(navController) }
            composable(DriverRoute.Mileage.route) { DriverMileageScreen(navController) }
            composable(DriverRoute.VehicleReport.route) { DriverVehicleReportScreen(navController) }
        }
    }
}
