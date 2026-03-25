package com.future.ultimate.driver.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember

enum class DriverLanguage(val code: String, val flag: String, val label: String) {
    Polish(code = "pl", flag = "🇵🇱", label = "Polski"),
    Spanish(code = "es", flag = "🇪🇸", label = "Español"),
}

val LocalDriverLanguage = compositionLocalOf { DriverLanguage.Polish }

@Composable
fun tr(pl: String, es: String): String {
    val language = LocalDriverLanguage.current
    return remember(language, pl, es) {
        when (language) {
            DriverLanguage.Polish -> pl
            DriverLanguage.Spanish -> es
        }
    }
}
