package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.future.ultimate.core.common.repository.PayrollPreviewRow

@Composable
internal fun PreviewSpreadsheetTable(
    headers: List<String>,
    rows: List<PayrollPreviewRow>,
    selectedColumns: Set<Int>,
    selectedRows: Set<Int>,
    onToggleRow: (Int) -> Unit,
    onToggleColumn: (Int) -> Unit,
    onExportRow: (Int) -> Unit,
    onSendRow: (Int) -> Unit,
) {
    val horizontalState = rememberScrollState()
    val verticalState = rememberScrollState()
    val visibleColumns = if (selectedColumns.isEmpty()) headers.indices.toList() else selectedColumns.sorted()
    val gridColor = MaterialTheme.colorScheme.outline
    val headerColor = MaterialTheme.colorScheme.surfaceVariant
    val cellWidth = 140.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp, max = 420.dp)
            .border(1.dp, gridColor)
            .horizontalScroll(horizontalState),
    ) {
        Column {
            Row(modifier = Modifier.background(headerColor)) {
                SpreadsheetCell(text = "#", width = 48.dp, borderColor = gridColor, isHeader = true)
                SpreadsheetCell(text = "Akcje", width = 220.dp, borderColor = gridColor, isHeader = true)
                visibleColumns.forEach { columnIndex ->
                    Box(
                        modifier = Modifier
                            .width(cellWidth)
                            .defaultMinSize(minHeight = 44.dp)
                            .border(0.5.dp, gridColor)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = columnIndex in selectedColumns,
                                onCheckedChange = { onToggleColumn(columnIndex) },
                            )
                            Text(headers.getOrNull(columnIndex).orEmpty().ifBlank { "kolumna_${columnIndex + 1}" })
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(verticalState),
            ) {
                rows.forEach { row ->
                    Row {
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .defaultMinSize(minHeight = 44.dp)
                                .border(0.5.dp, gridColor),
                            contentAlignment = Alignment.Center,
                        ) {
                            Checkbox(
                                checked = row.index in selectedRows,
                                onCheckedChange = { onToggleRow(row.index) },
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(220.dp)
                                .defaultMinSize(minHeight = 44.dp)
                                .border(0.5.dp, gridColor)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onExportRow(row.index) }) {
                                    Text("Eksport")
                                }
                                Button(onClick = { onSendRow(row.index) }) {
                                    Text("Wyślij")
                                }
                            }
                        }

                        visibleColumns.forEach { columnIndex ->
                            SpreadsheetCell(
                                text = row.cells.getOrNull(columnIndex).orEmpty(),
                                width = cellWidth,
                                borderColor = gridColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpreadsheetCell(
    text: String,
    width: Dp,
    borderColor: Color,
    isHeader: Boolean = false,
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
            text = text.ifBlank { "0" },
            style = if (isHeader) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
        )
    }
}
