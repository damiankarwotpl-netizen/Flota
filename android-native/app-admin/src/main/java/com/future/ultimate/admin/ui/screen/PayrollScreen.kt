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
fun PayrollScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as AdminApp
    val viewModel: PayrollViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenColumn("Moduł Paski", "Moduł płac i wysyłki") {
        item {
            Column {
                androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
                    Switch(checked = uiState.autoSend, onCheckedChange = { viewModel.toggleAutoSend() })
                    Text("AUTOMATYCZNA WYSYŁKA")
                }
                Text("Baza: ${uiState.totalRecipients} | Załączniki: ${uiState.attachmentCount}")
                Text(uiState.progressLabel)
                uiState.actionMessage?.let { Text(it) }
                uiState.attachmentPaths.forEach { path ->
                    Text("• ${path.substringAfterLast('/')}")
                }
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Wczytaj arkusz płac") }
                Button(onClick = { navController.navigate(AdminRoute.Table.route) }, modifier = Modifier.fillMaxWidth()) { Text("Podgląd i eksport") }
                Button(onClick = { navController.navigate(AdminRoute.Template.route) }, modifier = Modifier.fillMaxWidth()) { Text("Edytuj szablon") }
                Button(onClick = viewModel::attachSessionReportsCsv, modifier = Modifier.fillMaxWidth()) { Text("Dodaj CSV raportów") }
                Button(onClick = viewModel::attachContactsCsv, modifier = Modifier.fillMaxWidth()) { Text("Dołącz CSV kontaktów") }
                Button(onClick = viewModel::sendSingle, modifier = Modifier.fillMaxWidth()) { Text("Przygotuj jedną wysyłkę") }
                Button(onClick = viewModel::startMassMailing, modifier = Modifier.fillMaxWidth()) { Text("Przygotuj masową wysyłkę") }
                Button(onClick = viewModel::clearAttachments, modifier = Modifier.fillMaxWidth()) { Text("Wyczyść załączniki") }
                Button(onClick = viewModel::togglePauseMailing, modifier = Modifier.fillMaxWidth()) { Text("PAUZA/RESUME") }
                Button(onClick = { navController.navigate(AdminRoute.Reports.route) }, modifier = Modifier.fillMaxWidth()) { Text("Raporty sesji") }
            }
        }
    }
}
