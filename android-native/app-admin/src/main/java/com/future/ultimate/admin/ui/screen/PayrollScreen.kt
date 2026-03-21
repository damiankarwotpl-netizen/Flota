package com.future.ultimate.admin.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
                Button(onClick = { excelPicker.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Wczytaj Excel")
                }
                Button(onClick = { folderPicker.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Wybierz folder eksportu")
                }
                Text(
                    if (uiState.exportFolderUri.isBlank()) {
                        "Folder eksportu: nie wybrano"
                    } else {
                        "Folder eksportu: wybrany"
                    },
                )
                OutlinedTextField(
                    value = uiState.workbookImportText,
                    onValueChange = viewModel::updateWorkbookImportText,
                    label = { Text("Podgląd surowych danych (CSV/TSV)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                )
                Button(onClick = viewModel::stageWorkbookImport, modifier = Modifier.fillMaxWidth()) {
                    Text("Odśwież podgląd")
                }
            }
        }

        item {
            SectionCard(title = "Podgląd/Export", subtitle = "Otwiera osobne okno z tabelą jak w Excelu.") {
                Button(
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
                            Checkbox(
                                checked = index in uiState.selectedPreviewColumnIndexes,
                                onCheckedChange = { viewModel.togglePreviewColumnSelection(index) },
                            )
                            Text(header.ifBlank { "kolumna_${index + 1}" }, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                    Button(onClick = viewModel::selectAllPreviewColumns, modifier = Modifier.fillMaxWidth()) {
                        Text("Zaznacz kolumny")
                    }
                    uiState.previewRows.forEach { row ->
                        val selected = row.index in uiState.selectedPreviewRowIndexes
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Checkbox(
                                checked = selected,
                                onCheckedChange = { viewModel.togglePreviewRowSelection(row.index) },
                            )
                            Text(
                                text = row.cells.filterIndexed { columnIndex, _ ->
                                    uiState.selectedPreviewColumnIndexes.isEmpty() || columnIndex in uiState.selectedPreviewColumnIndexes
                                }.joinToString(" | "),
                                modifier = Modifier.padding(top = 10.dp),
                            )
                            Button(onClick = { viewModel.exportSinglePreviewRowToFolder(app, row.index) }) {
                                Text("Eksport")
                            }
                        }
                    }
                    Button(onClick = { isPreviewDialogOpen = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Zamknij")
                    }
                }
                uiState.actionMessage?.let { Text(it) }
            }
        }
    }
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
