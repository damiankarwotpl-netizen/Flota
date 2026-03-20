package com.future.ultimate.core.common.model

sealed class DriverRoute(val route: String, val title: String) {
    data object Login : DriverRoute("login", "Logowanie")
    data object ChangePassword : DriverRoute("change_password", "Zmiana hasła")
    data object Mileage : DriverRoute("mileage", "Przebieg")
    data object VehicleReport : DriverRoute("vehicle_report", "Raport stanu samochodu")
}
