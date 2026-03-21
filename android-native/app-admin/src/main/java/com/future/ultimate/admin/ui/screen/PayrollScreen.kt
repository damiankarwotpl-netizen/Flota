package com.future.ultimate.admin.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.Button as M3Button
import androidx.compose.material3.Checkbox as M3Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField as M3OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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

    val excelPicker = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mimeType = app.contentResolver.getType(uri)
        val fileName = resolveDisplayName(app, uri)
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
                M3Button(onClick = { excelPicker.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Wczytaj Excel")
                }
                M3Button(onClick = { folderPicker.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Wybierz folder eksportu")
                }
                Text(
                    if (uiState.exportFolderUri.isBlank()) {
                        "Folder eksportu: nie wybrano"
                    } else {
                        "Folder eksportu: wybrany"
                    },
                )
                M3OutlinedTextField(
                    value = uiState.workbookImportText,
                    onValueChange = viewModel::updateWorkbookImportText,
                    label = { Text("Podgląd surowych danych (CSV/TSV)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                )
                M3Button(onClick = viewModel::stageWorkbookImport, modifier = Modifier.fillMaxWidth()) {
                    Text("Odśwież podgląd")
                }
            }
        }

        item {
            SectionCard(title = "Podgląd/Export", subtitle = "Otwiera osobne okno z tabelą jak w Excelu.") {
                M3Button(
                    onClick = { isPreviewDialogOpen = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.previewRows.isNotEmpty(),
                ) {
                    Text("Podgląd/Export")
                }
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
                    Text("Wybierz kolumny do eksportu:")
                    displayHeaders.forEachIndexed { index, header ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            M3Checkbox(
                                checked = index in uiState.selectedPreviewColumnIndexes,
                                onCheckedChange = { viewModel.togglePreviewColumnSelection(index) },
                            )
                            Text(header.ifBlank { "kolumna_${index + 1}" }, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                    M3Button(onClick = viewModel::selectAllPreviewColumns, modifier = Modifier.fillMaxWidth()) {
                        Text("Zaznacz kolumny")
                    }
                    PreviewSpreadsheetTable(
                        headers = displayHeaders,
                        rows = uiState.previewRows,
                        selectedColumns = uiState.selectedPreviewColumnIndexes,
                        selectedRows = uiState.selectedPreviewRowIndexes,
                        onToggleRow = viewModel::togglePreviewRowSelection,
                        onExportRow = { rowIndex -> viewModel.exportSinglePreviewRowToFolder(app, rowIndex) },
                    )
                    M3Button(onClick = { isPreviewDialogOpen = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Zamknij")
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewSpreadsheetTable(
    headers: List<String>,
    rows: List<com.future.ultimate.core.common.ui.PayrollPreviewRow>,
    selectedColumns: Set<Int>,
    selectedRows: Set<Int>,
    onToggleRow: (Int) -> Unit,
    onExportRow: (Int) -> Unit,
) {
    val horizontalState = rememberScrollState()
    val verticalState = rememberScrollState()
    val visibleColumns = if (selectedColumns.isEmpty()) headers.indices.toList() else selectedColumns.sorted()
    val headerColor = MaterialTheme.colorScheme.surfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val cellWidth = 140.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp, max = 420.dp)
            .border(1.dp, gridColor)
            .horizontalScroll(horizontalState),
    ) {
        Column(modifier = Modifier.verticalScroll(verticalState)) {
            Row(modifier = Modifier.background(headerColor)) {
                SpreadsheetCell(text = "#", width = 48.dp, isHeader = true, borderColor = gridColor)
                visibleColumns.forEach { columnIndex ->
                    SpreadsheetCell(
                        text = headers.getOrNull(columnIndex).orEmpty().ifBlank { "kolumna_${columnIndex + 1}" },
                        width = cellWidth,
                        isHeader = true,
                        borderColor = gridColor,
                    )
                }
                SpreadsheetCell(text = "Eksport", width = 110.dp, isHeader = true, borderColor = gridColor)
            }

            rows.forEach { row ->
                Row {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .defaultMinSize(minHeight = 44.dp)
                            .border(0.5.dp, gridColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        M3Checkbox(
                            checked = row.index in selectedRows,
                            onCheckedChange = { onToggleRow(row.index) },
                        )
                    }
                    visibleColumns.forEach { columnIndex ->
                        SpreadsheetCell(
                            text = row.cells.getOrNull(columnIndex).orEmpty(),
                            width = cellWidth,
                            borderColor = gridColor,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .defaultMinSize(minHeight = 44.dp)
                            .border(0.5.dp, gridColor)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        M3Button(onClick = { onExportRow(row.index) }) {
                            Text("Eksport")
                        }
                    }
                    M3Button(onClick = { isPreviewDialogOpen = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Zamknij")
                    }
                }
                uiState.actionMessage?.let { Text(it) }
            }
        }
    }
}

@Composable
private fun SpreadsheetCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    isHeader: Boolean = false,
    borderColor: Color,
) {
    Box(
        modifier = Modifier
            .width(width)
            .defaultMinSize(minHeight = 44.dp)
            .border(0.5.dp, borderColor)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text.ifBlank { "-" },
            style = if (isHeader) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
        )
    }
    return null
}

private fun resolveDisplayName(context: android.content.Context, uri: Uri): String? {
    val projection = arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val columnIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (columnIndex >= 0 && cursor.moveToFirst()) {
            return cursor.getString(columnIndex)
        }
    }
    return null
}
