package com.future.ultimate.admin.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog as M3AlertDialog
import androidx.compose.material3.Button as M3Button
import androidx.compose.material3.OutlinedTextField as M3OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton as M3TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.future.ultimate.admin.AdminApp
import com.future.ultimate.admin.ui.viewmodel.AdminViewModelFactory
import com.future.ultimate.admin.ui.viewmodel.PayrollViewModel

@Composable
fun PayrollScreen(_navController: NavController) {
    val app = LocalContext.current.applicationContext as AdminApp
    val viewModel: PayrollViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isPreviewDialogOpen by remember { mutableStateOf(false) }
    var isSpreadsheetDialogOpen by remember { mutableStateOf(false) }
    var isCashReportDialogOpen by remember { mutableStateOf(false) }

    val displayNameFromUri: (Uri) -> String? = { uri ->
        val projection = arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)
        app.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        }
    }

    val excelPicker = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mimeType = app.contentResolver.getType(uri)
        val fileName = displayNameFromUri(uri)
        val bytes = runCatching {
            app.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: byteArrayOf()
        }.getOrDefault(byteArrayOf())

        viewModel.loadWorkbookFromFile(
            fileName = fileName,
            mimeType = mimeType,
            bytes = bytes,
        )
    }

    val folderPicker = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                app.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
        viewModel.updateExportFolderUri(uri)
    }

    val columnCount = maxOf(
        uiState.previewHeaders.size,
        uiState.previewRows.maxOfOrNull { it.cells.size } ?: 0,
    )
    val displayHeaders = if (uiState.previewHeaders.isNotEmpty()) {
        uiState.previewHeaders
    } else {
        (0 until columnCount).map { "kolumna_${it + 1}" }
    }

    ScreenColumn("Wypłaty", "Nowy moduł: import Excel + podgląd + eksport") {
        item {
            SectionCard(title = "Wczytaj Excel", subtitle = "Wczytuje plik z pamięci telefonu.") {
                M3Button(onClick = { excelPicker.launch("*/*") }, modifier = Modifier.fillMaxWidth()) { Text("Wczytaj Excel") }
                M3Button(onClick = { folderPicker.launch(null) }, modifier = Modifier.fillMaxWidth()) { Text("Wybierz folder eksportu") }

                Text(if (uiState.exportFolderUri.isBlank()) "Folder eksportu: nie wybrano" else "Folder eksportu: wybrany")

                M3OutlinedTextField(
                    value = uiState.workbookImportText,
                    onValueChange = viewModel::updateWorkbookImportText,
                    label = { Text("Podgląd surowych danych (CSV/TSV)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                )

                M3Button(onClick = viewModel::stageWorkbookImport, modifier = Modifier.fillMaxWidth()) { Text("Odśwież podgląd") }
            }
        }

        item {
            SectionCard(title = "Podgląd/Export", subtitle = "Otwiera osobne okno z tabelą jak w Excelu.") {
                M3Button(
                    onClick = { isPreviewDialogOpen = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.previewRows.isNotEmpty(),
                ) { Text("Podgląd/Export") }
                M3Button(
                    onClick = {
                        viewModel.prepareCashReportSelection()
                        isCashReportDialogOpen = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.previewRows.isNotEmpty(),
                ) { Text("Generuj raport gotówki") }

                if (uiState.previewRows.isEmpty()) {
                    Text("Brak danych do podglądu. Najpierw wczytaj plik Excel/CSV.")
                }
                uiState.actionMessage?.let { Text(it) }
            }
        }
    }

    if (isPreviewDialogOpen) {
        Dialog(onDismissRequest = { isPreviewDialogOpen = false }) {
            SectionCard(title = "Podgląd arkusza Excel") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Otwórz tabelę, aby wybrać kolumny i wiersze do eksportu.")
                    M3Button(
                        onClick = {
                            isPreviewDialogOpen = false
                            isSpreadsheetDialogOpen = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Otwórz tabelę w nowym oknie") }

                    M3Button(onClick = { isPreviewDialogOpen = false }, modifier = Modifier.fillMaxWidth()) { Text("Zamknij") }
                }
            }
        }
    }

    if (isSpreadsheetDialogOpen) {
        Dialog(
            onDismissRequest = { isSpreadsheetDialogOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                SectionCard(title = "Tabela arkusza Excel") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        M3Button(onClick = viewModel::exportSelectedPreviewRows, modifier = Modifier.fillMaxWidth()) {
                            Text("Eksportuj zaznaczone wiersze")
                        }
                        PreviewSpreadsheetTable(
                            headers = displayHeaders,
                            rows = uiState.previewRows,
                            selectedColumns = uiState.selectedPreviewColumnIndexes,
                            selectedRows = uiState.selectedPreviewRowIndexes,
                            onToggleRow = viewModel::togglePreviewRowSelection,
                            onToggleColumn = viewModel::togglePreviewColumnSelection,
                            onExportRow = { rowIndex -> viewModel.exportSinglePreviewRowToFolder(app, rowIndex) },
                            onSendRow = viewModel::sendSinglePreviewRowMail,
                        )
                        M3Button(onClick = { isSpreadsheetDialogOpen = false }, modifier = Modifier.fillMaxWidth()) {
                            Text("Zamknij tabelę")
                        }
                        M3Button(onClick = viewModel::selectAllPreviewColumns, modifier = Modifier.fillMaxWidth()) {
                            Text("Zaznacz kolumny")
                        }
                    }
                }
            }
        }
    }

    if (isCashReportDialogOpen) {
        Dialog(
            onDismissRequest = { isCashReportDialogOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                SectionCard(title = "Raport gotówki") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        M3Button(onClick = { viewModel.generateCashReportToFolder(app) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Generuj raport gotówki")
                        }
                        PreviewSpreadsheetTable(
                            headers = displayHeaders,
                            rows = uiState.previewRows,
                            selectedColumns = uiState.selectedPreviewColumnIndexes,
                            selectedRows = uiState.selectedPreviewRowIndexes,
                            onToggleRow = viewModel::togglePreviewRowSelection,
                            onToggleColumn = viewModel::togglePreviewColumnSelection,
                            showRowActions = false,
                        )
                        M3Button(onClick = { isCashReportDialogOpen = false }, modifier = Modifier.fillMaxWidth()) {
                            Text("Zamknij")
                        }
                        M3Button(onClick = viewModel::selectAllPreviewColumns, modifier = Modifier.fillMaxWidth()) {
                            Text("Zaznacz kolumny")
                        }
                    }
                }
            }
        }
    }

    if (isSpreadsheetDialogOpen || isCashReportDialogOpen) {
        uiState.actionMessage?.let { message ->
            M3AlertDialog(
                onDismissRequest = viewModel::clearActionMessage,
                confirmButton = {
                    M3TextButton(onClick = viewModel::clearActionMessage) {
                        Text("OK")
                    }
                },
                title = { Text("Komunikat") },
                text = { Text(message) },
            )
        }
    }
}
