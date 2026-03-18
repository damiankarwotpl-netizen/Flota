package com.future.ultimate.driver.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.future.ultimate.core.common.model.DriverRoute
import com.future.ultimate.driver.DriverApp
import com.future.ultimate.driver.ui.viewmodel.DriverLoginViewModel
import com.future.ultimate.driver.ui.viewmodel.DriverMileageViewModel
import com.future.ultimate.driver.ui.viewmodel.DriverVehicleReportViewModel
import com.future.ultimate.driver.ui.viewmodel.DriverViewModelFactory

@Composable
private fun DriverScreen(title: String, content: @Composable () -> Unit) {
    androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                content()
            }
        }
    }
}

@Composable
fun DriverLoginScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as DriverApp
    val viewModel: DriverLoginViewModel = viewModel(factory = DriverViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DriverScreen("Login") {
        OutlinedTextField(uiState.login, viewModel::updateLogin, label = { Text("Login") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(uiState.password, viewModel::updatePassword, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { viewModel.login { navController.navigate(DriverRoute.Mileage.route) } }, modifier = Modifier.fillMaxWidth()) {
            Text(if (uiState.isLoading) "Logowanie..." else "Login")
        }
        uiState.error?.let { Text(it) }
    }
}

@Composable
fun DriverChangePasswordScreen(navController: NavController) {
    DriverScreen("Change password") {
        Button(onClick = { navController.navigate(DriverRoute.Mileage.route) }, modifier = Modifier.fillMaxWidth()) { Text("Przejdź dalej") }
    }
}

@Composable
fun DriverMileageScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as DriverApp
    val viewModel: DriverMileageViewModel = viewModel(factory = DriverViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DriverScreen("Mileage") {
        Text("Car")
        OutlinedTextField(uiState.registration, viewModel::setRegistration, label = { Text("Registration") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(uiState.mileage, viewModel::updateMileage, label = { Text("Mileage") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { viewModel.save("driver") }, modifier = Modifier.fillMaxWidth()) {
            Text(if (uiState.isSaving) "Zapisywanie..." else "Save mileage")
        }
        Button(onClick = { navController.navigate(DriverRoute.VehicleReport.route) }, modifier = Modifier.fillMaxWidth()) { Text("Raport stanu samochodu") }
        uiState.status?.let { Text(it) }
    }
}

@Composable
fun DriverVehicleReportScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as DriverApp
    val viewModel: DriverVehicleReportViewModel = viewModel(factory = DriverViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val draft = uiState.draft

    DriverScreen("Raport stanu samochodu") {
        listOf(
            "Marka" to draft.marka,
            "Rejestracja" to draft.rej,
            "Przebieg" to draft.przebieg,
            "Poziom oleju" to draft.olej,
            "Wskaźnik paliwa" to draft.paliwo,
            "Rodzaj paliwa" to draft.rodzajPaliwa,
            "Lewy przedni" to draft.lp,
            "Prawy przedni" to draft.pp,
            "Lewy tylny" to draft.lt,
            "Prawy tylny" to draft.pt,
            "Nowe uszkodzenia" to draft.uszkodzenia,
            "Od kiedy?" to draft.odKiedy,
            "Przegląd / Service" to draft.serwis,
            "Przegląd techniczny" to draft.przeglad,
            "Uwagi" to draft.uwagi,
        ).forEach { (label, value) ->
            OutlinedTextField(value = value, onValueChange = {}, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
        }
        listOf("Trójkąt", "Kamizelki", "Koło zapasowe", "Dowód rejestracyjny", "Apteczka").forEach {
            androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = false, onCheckedChange = {})
                Text(it)
            }
        }
        Button(onClick = { navController.navigate(DriverRoute.Mileage.route) }, modifier = Modifier.fillMaxWidth()) { Text("Wróć") }
        Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) { Text(if (uiState.isSaving) "Zapisywanie..." else "Zapisz szkic PDF") }
        uiState.message?.let { Text(it) }
    }
}
