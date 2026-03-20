package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
fun PayrollScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as AdminApp
    val viewModel: PayrollViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val smtpViewModel: SmtpViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val smtpUiState by smtpViewModel.uiState.collectAsStateWithLifecycle()
    val templateViewModel: TemplateViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val templateUiState by templateViewModel.uiState.collectAsStateWithLifecycle()
    val reportsViewModel: ReportsViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val reportsUiState by reportsViewModel.uiState.collectAsStateWithLifecycle()

    val smtpConfigured = smtpUiState.settings.host.isNotBlank() &&
        smtpUiState.settings.user.isNotBlank() &&
        smtpUiState.settings.password.isNotBlank()
    val templateConfigured = templateUiState.template.subject.isNotBlank() && templateUiState.template.body.isNotBlank()
    val lastSession = reportsUiState.items.firstOrNull()
    val filteredRecipients = uiState.contacts.filter {
        val query = uiState.recipientQuery.trim().lowercase()
        query.isBlank() || listOf(it.name, it.surname, it.email, it.workplace, it.phone)
            .joinToString(" ")
            .lowercase()
            .contains(query)
    }

    ScreenColumn("Moduł Paski", "Kompletna logika finansowa, workbook, załączniki i wysyłka jak w legacy main") {
        item {
            SectionCard(
                title = "Tryb pracy i status",
                subtitle = "Główne parametry modułu płac oraz stan bieżącej sesji wysyłki.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Switch(checked = uiState.autoSend, onCheckedChange = { viewModel.toggleAutoSend() })
                        Text("AUTOMATYCZNA WYSYŁKA", modifier = Modifier.padding(top = 14.dp, start = 10.dp))
                    }
                    if (!uiState.autoSend) {
                        Text("Tryb ręcznej akceptacji: każdy odbiorca masowej wysyłki wymaga decyzji operatora.")
                    }
                    Text("Operator: ${uiState.operatorLabel}")
                    Text("Baza: ${uiState.totalRecipients} | Załączniki: ${uiState.attachmentCount}")
                    Text(uiState.progressLabel)
                    uiState.actionMessage?.let { Text(it) }
                    Text(if (uiState.isMailingRunning) "Wysyłka w toku" else "Wysyłka zatrzymana / nieuruchomiona")
                    Text(if (smtpConfigured) "SMTP: skonfigurowane" else "SMTP: brak pełnej konfiguracji")
                    Text(if (templateConfigured) "Szablon: gotowy" else "Szablon: uzupełnij temat i treść")
                    Text(
                        lastSession?.let {
                            "Ostatnia sesja: ${it.date} • OK ${it.ok} • Błędy ${it.fail} • Pominięte ${it.skip}"
                        } ?: "Ostatnia sesja: brak danych"
                    )
                }
            }
        }
        item {
            SectionCard(
                title = "Kalkulator finansowy",
                subtitle = "Lokalna kalkulacja płac netto, podatku i kosztu pracodawcy.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = uiState.grossAmount,
                        onValueChange = viewModel::updateGrossAmount,
                        label = { Text("Kwota brutto") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = uiState.bonusAmount,
                        onValueChange = viewModel::updateBonusAmount,
                        label = { Text("Premia") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = uiState.deductionsAmount,
                        onValueChange = viewModel::updateDeductionsAmount,
                        label = { Text("Potrącenia") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = uiState.taxPercent,
                        onValueChange = viewModel::updateTaxPercent,
                        label = { Text("Podatek %") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(onClick = viewModel::calculatePayroll, modifier = Modifier.fillMaxWidth()) {
                        Text("Przelicz płace")
                    }
                    Text("Netto: ${uiState.netAmount} | Koszt pracodawcy: ${uiState.employerCostAmount}")
                    Text(uiState.calculationSummary)
                }
            }
        }
        item {
            SectionCard(
                title = "Workbook płac",
                subtitle = "Wklej CSV/TSV/średnik, przygotuj staging i dołącz eksport workbooka.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = uiState.workbookImportText,
                        onValueChange = viewModel::updateWorkbookImportText,
                        label = { Text("Wklej workbook CSV/TSV/;") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                    )
                    Button(onClick = viewModel::stageWorkbookImport, modifier = Modifier.fillMaxWidth()) {
                        Text("Wczytaj arkusz płac")
                    }
                    Button(onClick = viewModel::attachStagedWorkbookCsv, modifier = Modifier.fillMaxWidth()) {
                        Text("Dołącz staging workbooka")
                    }
                    if (uiState.stagedWorkbookRows.isNotEmpty()) {
                        Text("Staged workbook rows: ${uiState.stagedWorkbookRows.size}")
                        uiState.stagedWorkbookRows.take(5).forEach { row ->
                            Text("${row.name} ${row.surname} • ${row.workplace.ifBlank { "-" }} • ${row.amount.ifBlank { "-" }}")
                        }
                        Button(onClick = viewModel::clearWorkbookImport, modifier = Modifier.fillMaxWidth()) {
                            Text("Wyczyść staging workbooka")
                        }
                    }
                }
            }
        }
        item {
            SectionCard(
                title = "Załączniki i eksport",
                subtitle = "Zbuduj komplet danych do wysyłki jak w starym module Paski.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (uiState.attachmentPaths.isEmpty()) {
                        Text("Brak załączników dołączonych do sesji.")
                    } else {
                        uiState.attachmentPaths.forEach { path ->
                            Text("• ${path.substringAfterLast('/')}")
                        }
                    }
                    Button(onClick = { navController.navigate(AdminRoute.Table.route) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Podgląd i eksport")
                    }
                    Button(onClick = { navController.navigate(AdminRoute.Smtp.route) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Ustawienia SMTP")
                    }
                    Button(onClick = { navController.navigate(AdminRoute.Template.route) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Edytuj szablon")
                    }
                    Button(onClick = viewModel::attachSessionReportsCsv, modifier = Modifier.fillMaxWidth()) {
                        Text("Dodaj CSV raportów")
                    }
                    Button(onClick = viewModel::attachContactsCsv, modifier = Modifier.fillMaxWidth()) {
                        Text("Dołącz CSV kontaktów")
                    }
                    Button(onClick = viewModel::attachPayrollPackage, modifier = Modifier.fillMaxWidth()) {
                        Text("Dołącz paczkę płac")
                    }
                    Button(onClick = viewModel::clearAttachments, modifier = Modifier.fillMaxWidth()) {
                        Text("Wyczyść załączniki")
                    }
                }
            }
        }
        item {
            SectionCard(
                title = "Wysyłka płac",
                subtitle = "Jednorazowy podgląd, masowa sesja, pause/resume oraz anulowanie kolejki.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = viewModel::sendSingle, modifier = Modifier.fillMaxWidth()) {
                        Text("Wyślij jeden plik")
                    }
                    Button(onClick = viewModel::startMassMailing, modifier = Modifier.fillMaxWidth()) {
                        Text("Start masowa wysyłka")
                    }
                    Button(onClick = viewModel::togglePauseMailing, modifier = Modifier.fillMaxWidth()) {
                        Text(if (uiState.isMailingPaused) "Wznów kolejkę" else "PAUZA/RESUME")
                    }
                    Button(onClick = viewModel::cancelMailing, modifier = Modifier.fillMaxWidth()) {
                        Text(if (uiState.isCancellingMailing) "Anulowanie..." else "Anuluj aktywną wysyłkę")
                    }
                    Button(onClick = { navController.navigate(AdminRoute.Reports.route) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Raporty sesji")
                    }
                }
            }
        }
        item {
            SectionCard(
                title = "Wysyłka specjalna",
                subtitle = "Zaznacz odbiorców, załaduj szablon i wyślij dedykowaną wiadomość z załącznikami.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Zaznaczeni odbiorcy: ${uiState.selectedRecipientKeys.size} / ${uiState.contacts.size}")
                    Text("Załączniki dołączone do wysyłki specjalnej: ${uiState.attachmentCount}")
                    OutlinedTextField(
                        value = uiState.recipientQuery,
                        onValueChange = viewModel::updateRecipientQuery,
                        label = { Text("Filtruj odbiorców") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::selectVisibleRecipients, modifier = Modifier.weight(1f)) {
                            Text("Zaznacz widocznych")
                        }
                        Button(onClick = viewModel::clearSpecialRecipients, modifier = Modifier.weight(1f)) {
                            Text("Wyczyść zaznaczenie")
                        }
                    }
                    Button(onClick = viewModel::loadTemplateIntoSpecial, modifier = Modifier.fillMaxWidth()) {
                        Text("Wczytaj zapisany szablon")
                    }
                    OutlinedTextField(
                        value = uiState.specialSubject,
                        onValueChange = viewModel::updateSpecialSubject,
                        label = { Text("Temat specjalnej wysyłki") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = uiState.specialBody,
                        onValueChange = viewModel::updateSpecialBody,
                        label = { Text("Treść specjalnej wysyłki") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                    )
                    Button(onClick = viewModel::sendSpecial, modifier = Modifier.fillMaxWidth()) {
                        Text("Wyślij do zaznaczonych odbiorców")
                    }
                }
            }
        }
        item {
            if (filteredRecipients.isEmpty()) {
                SectionCard(
                    title = "Odbiorcy",
                    subtitle = "Brak odbiorców pasujących do bieżącego filtra.",
                ) {
                    Text("Zmień filtr lub dodaj kontakty, aby przygotować wysyłkę specjalną.")
                }
            } else {
                SectionCard(
                    title = "Lista odbiorców specjalnej wysyłki",
                    subtitle = "Podgląd pierwszych 30 dopasowanych rekordów.",
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        filteredRecipients.take(30).forEach { recipient ->
                            val key = "${recipient.name.trim().lowercase()}|${recipient.surname.trim().lowercase()}|${recipient.email.trim().lowercase()}"
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Checkbox(
                                    checked = key in uiState.selectedRecipientKeys,
                                    onCheckedChange = { viewModel.toggleSpecialRecipient(recipient) },
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${recipient.name} ${recipient.surname}".trim().ifBlank { "Bez nazwy" })
                                    Text(recipient.email.ifBlank { "Brak email" })
                                    Text(recipient.workplace.ifBlank { "Brak miejsca pracy" })
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    if (uiState.isAwaitingMailApproval) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Weryfikacja odbiorcy") },
            text = {
                Column {
                    Text(uiState.pendingApprovalRecipientName.ifBlank { "Bez nazwy" })
                    Text(uiState.pendingApprovalRecipientEmail.ifBlank { "Brak email" })
                    Text("Czy kontynuować wysyłkę do tego odbiorcy?")
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.resolvePendingApproval(true) }) {
                    Text("Wyślij")
                }
            },
            dismissButton = {
                Button(onClick = { viewModel.resolvePendingApproval(false) }) {
                    Text("Pomiń")
                }
            },
        )
    }
}
