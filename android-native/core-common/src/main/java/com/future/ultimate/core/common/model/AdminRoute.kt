package com.future.ultimate.core.common.model

sealed class AdminRoute(val route: String, val title: String) {
    data object Home : AdminRoute("home", "FUTURE ULTIMATE v20")
    data object Contacts : AdminRoute("contacts", "Kontakty")
    data object Cars : AdminRoute("cars", "Samochody")
    data object VehicleReport : AdminRoute("vehicle_report", "Raport stanu samochodu")
    data object Clothes : AdminRoute("clothes", "Ubranie robocze")
    data object Payroll : AdminRoute("payroll", "Paski")
    data object Table : AdminRoute("table", "Podgląd i eksport")
    data object Email : AdminRoute("email", "Moduł Email")
    data object Smtp : AdminRoute("smtp", "Ustawienia SMTP")
    data object Template : AdminRoute("template", "Szablon email")
    data object Reports : AdminRoute("reports", "Historia sesji")
    data object Workers : AdminRoute("workers", "Pracownicy")
    data object Plants : AdminRoute("plants", "Zakłady")
    data object Settings : AdminRoute("settings", "Ustawienia")
}
