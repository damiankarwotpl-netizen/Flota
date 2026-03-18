package com.future.ultimate.driver.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.future.ultimate.core.common.model.DriverRoute

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
    val login = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    DriverScreen("Login") {
        OutlinedTextField(login.value, { login.value = it }, label = { Text("Login") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password.value, { password.value = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { navController.navigate(DriverRoute.Mileage.route) }, modifier = Modifier.fillMaxWidth()) { Text("Login") }
    }
}

@Composable
fun DriverChangePasswordScreen(navController: NavController) {
    val password = remember { mutableStateOf("") }
    DriverScreen("Change password") {
        OutlinedTextField(password.value, { password.value = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { navController.navigate(DriverRoute.Mileage.route) }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
    }
}

@Composable
fun DriverMileageScreen(navController: NavController) {
    val mileage = remember { mutableStateOf("") }
    DriverScreen("Mileage") {
        Text("Car")
        Text("REGISTRATION")
        OutlinedTextField(mileage.value, { mileage.value = it }, label = { Text("Mileage") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Save mileage") }
        Button(onClick = { navController.navigate(DriverRoute.VehicleReport.route) }, modifier = Modifier.fillMaxWidth()) { Text("Raport stanu samochodu") }
    }
}

@Composable
fun DriverVehicleReportScreen(navController: NavController) {
    DriverScreen("Raport stanu samochodu") {
        listOf("Marka", "Rejestracja", "Przebieg", "Poziom oleju", "Wskaźnik paliwa", "Rodzaj paliwa", "Lewy przedni", "Prawy przedni", "Lewy tylny", "Prawy tylny", "Nowe uszkodzenia", "Od kiedy?", "Przegląd / Service", "Przegląd techniczny", "Uwagi").forEach {
            OutlinedTextField("", onValueChange = {}, label = { Text(it) }, modifier = Modifier.fillMaxWidth())
        }
        listOf("Trójkąt", "Kamizelki", "Koło zapasowe", "Dowód rejestracyjny", "Apteczka").forEach {
            androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = false, onCheckedChange = {})
                Text(it)
            }
        }
        Button(onClick = { navController.navigate(DriverRoute.Mileage.route) }, modifier = Modifier.fillMaxWidth()) { Text("Wróć") }
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Zapisz PDF") }
    }
}
