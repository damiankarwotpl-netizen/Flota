package com.future.ultimate.driver.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.future.ultimate.core.common.model.DriverRoute
import com.future.ultimate.driver.ui.screen.DriverChangePasswordScreen
import com.future.ultimate.driver.ui.screen.DriverLoginScreen
import com.future.ultimate.driver.ui.screen.DriverMileageScreen
import com.future.ultimate.driver.ui.screen.DriverVehicleReportScreen

@Composable
fun DriverRoot() {
    MaterialTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = DriverRoute.Login.route) {
            composable(DriverRoute.Login.route) { DriverLoginScreen(navController) }
            composable(DriverRoute.ChangePassword.route) { DriverChangePasswordScreen(navController) }
            composable(DriverRoute.Mileage.route) { DriverMileageScreen(navController) }
            composable(DriverRoute.VehicleReport.route) { DriverVehicleReportScreen(navController) }
        }
    }
}
