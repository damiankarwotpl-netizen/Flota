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
import com.future.ultimate.admin.ui.viewmodel.ReportsViewModel
import com.future.ultimate.admin.ui.viewmodel.SmtpViewModel
import com.future.ultimate.admin.ui.viewmodel.TemplateViewModel
import com.future.ultimate.core.common.model.AdminRoute

@Composable
fun EmailScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as AdminApp
    val viewModel: PayrollViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val smtpViewModel: SmtpViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val smtpUiState by smtpViewModel.uiState.collectAsStateWithLifecycle()
    val templateViewModel: TemplateViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val templateUiState by templateViewModel.uiState.collectAsStateWithLifecycle()
    val reportsViewModel: ReportsViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val reportsUiState by reportsViewModel.uiState.collectAsStateWithLifecycle()
    val smtpConfigured = smtpUiState.settings.host.isNotBlank() && smtpUiState.settings.user.isNotBlank() && smtpUiState.settings.password.isNotBlank()
    val templateConfigured = templateUiState.template.subject.isNotBlank() && templateUiState.template.body.isNotBlank()
    val lastSession = reportsUiState.items.firstOrNull()

    ScreenColumn("Moduł Email", "Wysyłka i komunikacja") {
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
                Text(if (uiState.isMailingRunning) "Wysyłka w toku" else "Wysyłka zatrzymana / nieuruchomiona")
                Text(if (smtpConfigured) "SMTP: skonfigurowane" else "SMTP: brak pełnej konfiguracji")
                Text(if (templateConfigured) "Szablon: gotowy" else "Szablon: uzupełnij temat i treść")
                Text(
                    lastSession?.let {
                        "Ostatnia sesja: ${it.date} • OK ${it.ok} • Błędy ${it.fail} • Pominięte ${it.skip}"
                    } ?: "Ostatnia sesja: brak danych"
                )
                Button(onClick = { navController.navigate(AdminRoute.Smtp.route) }, modifier = Modifier.fillMaxWidth()) { Text("SMTP") }
                Button(onClick = { navController.navigate(AdminRoute.Template.route) }, modifier = Modifier.fillMaxWidth()) { Text("Edytuj szablon") }
                Button(onClick = viewModel::attachSessionReportsCsv, modifier = Modifier.fillMaxWidth()) { Text("Dodaj CSV raportów") }
                Button(onClick = viewModel::attachContactsCsv, modifier = Modifier.fillMaxWidth()) { Text("Dodaj CSV kontaktów") }
                Button(onClick = viewModel::sendSingle, modifier = Modifier.fillMaxWidth()) { Text("Przygotuj jedną wysyłkę") }
                Button(onClick = viewModel::startMassMailing, modifier = Modifier.fillMaxWidth()) { Text("Przygotuj masową wysyłkę") }
                Button(onClick = viewModel::clearAttachments, modifier = Modifier.fillMaxWidth()) { Text("Wyczyść załączniki") }
                Button(onClick = { navController.navigate(AdminRoute.Reports.route) }, modifier = Modifier.fillMaxWidth()) { Text("Raporty sesji") }
            }
        }
    }
}
