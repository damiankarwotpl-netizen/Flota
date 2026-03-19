package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.future.ultimate.admin.AdminApp
import com.future.ultimate.admin.ui.viewmodel.AdminViewModelFactory
import com.future.ultimate.admin.ui.viewmodel.PayrollViewModel
import com.future.ultimate.core.common.model.AdminRoute

@Composable
fun EmailScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as AdminApp
    val viewModel: PayrollViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenColumn("Moduł Email", "Wysyłka i komunikacja") {
        item {
            Column {
                androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
                    Switch(checked = uiState.autoSend, onCheckedChange = { viewModel.toggleAutoSend() })
                    Text("AUTOMATYCZNA WYSYŁKA")
                }
                Text("Baza: ${uiState.totalRecipients} | Załączniki: ${uiState.attachmentCount}")
                Text(uiState.progressLabel)
                Text(if (uiState.isMailingRunning) "Wysyłka w toku" else "Wysyłka zatrzymana / nieuruchomiona")
                Button(onClick = { navController.navigate(AdminRoute.Smtp.route) }, modifier = Modifier.fillMaxWidth()) { Text("SMTP") }
                Button(onClick = { navController.navigate(AdminRoute.Template.route) }, modifier = Modifier.fillMaxWidth()) { Text("Edytuj szablon") }
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Dodaj załącznik") }
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Wyślij jeden plik") }
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Start masowa wysyłka") }
                Button(onClick = { navController.navigate(AdminRoute.Reports.route) }, modifier = Modifier.fillMaxWidth()) { Text("Raporty sesji") }
            }
        }
    }
}
