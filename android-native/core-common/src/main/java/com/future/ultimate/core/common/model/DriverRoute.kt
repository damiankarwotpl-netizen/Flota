package com.future.ultimate.core.common.model

sealed class DriverRoute(val route: String, val title: String) {
    data object Login : DriverRoute("login", "Login")
    data object ChangePassword : DriverRoute("change_password", "Change password")
    data object Mileage : DriverRoute("mileage", "Mileage")
    data object VehicleReport : DriverRoute("vehicle_report", "Raport stanu samochodu")
}
