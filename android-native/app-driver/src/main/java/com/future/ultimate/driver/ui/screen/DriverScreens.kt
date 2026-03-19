package com.future.ultimate.driver.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.future.ultimate.core.common.model.VehicleReportDraft
import com.future.ultimate.driver.DriverApp
import com.future.ultimate.driver.ui.viewmodel.DriverChangePasswordViewModel
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

    DriverScreen("Login kierowcy") {
        OutlinedTextField(uiState.login, viewModel::updateLogin, label = { Text("Login") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(uiState.password, viewModel::updatePassword, label = { Text("Hasło") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                viewModel.login { requiresPasswordChange ->
                    navController.navigate(
                        if (requiresPasswordChange) DriverRoute.ChangePassword.route else DriverRoute.Mileage.route,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isLoading) "Logowanie..." else "Login")
        }
        uiState.error?.let { Text(it) }
    }
}

@Composable
fun DriverChangePasswordScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as DriverApp
    val viewModel: DriverChangePasswordViewModel = viewModel(factory = DriverViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DriverScreen("Zmiana hasła") {
        Text("Pierwsze logowanie wymaga ustawienia nowego hasła.")
        OutlinedTextField(uiState.login, onValueChange = {}, label = { Text("Login") }, modifier = Modifier.fillMaxWidth(), enabled = false)
        OutlinedTextField(uiState.password, viewModel::updatePassword, label = { Text("Nowe hasło") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { viewModel.save { navController.navigate(DriverRoute.Mileage.route) } }, modifier = Modifier.fillMaxWidth()) {
            Text(if (uiState.isLoading) "Zapisywanie..." else "Zapisz hasło")
        }
        uiState.error?.let { Text(it) }
    }
}

@Composable
fun DriverMileageScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as DriverApp
    val viewModel: DriverMileageViewModel = viewModel(factory = DriverViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DriverScreen("Przebieg") {
        if (uiState.driverName.isNotBlank()) {
            Text("Kierowca: ${uiState.driverName}")
        }
        OutlinedTextField(uiState.registration, viewModel::setRegistration, label = { Text("Rejestracja") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(uiState.mileage, viewModel::updateMileage, label = { Text("Przebieg") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
            Text(if (uiState.isSaving) "Zapisywanie..." else "Save mileage")
        }
        Button(onClick = { navController.navigate(DriverRoute.VehicleReport.route) }, modifier = Modifier.fillMaxWidth()) {
            Text("Raport stanu samochodu")
        }
        Button(
            onClick = {
                viewModel.logout {
                    navController.navigate(DriverRoute.Login.route) {
                        popUpTo(DriverRoute.Login.route) { inclusive = true }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Wyloguj")
        }
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
        if (uiState.driverName.isNotBlank()) {
            Text("Kierowca: ${uiState.driverName}")
        }
        editableFields(draft) { viewModel.updateDraft(it) }
        checklist(draft) { viewModel.updateDraft(it) }
        Button(onClick = { navController.navigate(DriverRoute.Mileage.route) }, modifier = Modifier.fillMaxWidth()) { Text("Wróć") }
        Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) { Text(if (uiState.isSaving) "Zapisywanie..." else "Zapisz PDF") }
        Button(
            onClick = {
                viewModel.logout {
                    navController.navigate(DriverRoute.Login.route) {
                        popUpTo(DriverRoute.Login.route) { inclusive = true }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Wyloguj") }
        uiState.message?.let { Text(it) }
    }
}

@Composable
private fun editableFields(draft: VehicleReportDraft, onDraftChange: (VehicleReportDraft) -> Unit) {
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
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                onDraftChange(
                    when (label) {
                        "Marka" -> draft.copy(marka = newValue)
                        "Rejestracja" -> draft.copy(rej = newValue)
                        "Przebieg" -> draft.copy(przebieg = newValue)
                        "Poziom oleju" -> draft.copy(olej = newValue)
                        "Wskaźnik paliwa" -> draft.copy(paliwo = newValue)
                        "Rodzaj paliwa" -> draft.copy(rodzajPaliwa = newValue)
                        "Lewy przedni" -> draft.copy(lp = newValue)
                        "Prawy przedni" -> draft.copy(pp = newValue)
                        "Lewy tylny" -> draft.copy(lt = newValue)
                        else -> draft.copy(
                            pt = if (label == "Prawy tylny") newValue else draft.pt,
                            uszkodzenia = if (label == "Nowe uszkodzenia") newValue else draft.uszkodzenia,
                            odKiedy = if (label == "Od kiedy?") newValue else draft.odKiedy,
                            serwis = if (label == "Przegląd / Service") newValue else draft.serwis,
                            przeglad = if (label == "Przegląd techniczny") newValue else draft.przeglad,
                            uwagi = if (label == "Uwagi") newValue else draft.uwagi,
                        ),
                    },
                )
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun checklist(draft: VehicleReportDraft, onDraftChange: (VehicleReportDraft) -> Unit) {
    listOf(
        "Trójkąt" to draft.trojkat,
        "Kamizelki" to draft.kamizelki,
        "Koło zapasowe" to draft.kolo,
        "Dowód rejestracyjny" to draft.dowod,
        "Apteczka" to draft.apteczka,
    ).forEach { (label, checked) ->
        Row(modifier = Modifier.fillMaxWidth()) {
            Checkbox(
                checked = checked,
                onCheckedChange = { value ->
                    onDraftChange(
                        when (label) {
                            "Trójkąt" -> draft.copy(trojkat = value)
                            "Kamizelki" -> draft.copy(kamizelki = value)
                            "Koło zapasowe" -> draft.copy(kolo = value)
                            "Dowód rejestracyjny" -> draft.copy(dowod = value)
                            else -> draft.copy(apteczka = value)
                        },
                    )
                },
            )
            Text(label)
        }
    }
}
