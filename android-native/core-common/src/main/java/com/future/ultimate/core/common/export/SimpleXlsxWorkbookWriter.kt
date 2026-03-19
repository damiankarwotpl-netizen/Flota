package com.future.ultimate.core.common.export

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object SimpleXlsxWorkbookWriter {
    data class Cell(
        val value: String,
        val type: Type = Type.TEXT,
    ) {
        enum class Type {
            TEXT,
            NUMBER,
        }

        companion object {
            fun text(value: String): Cell = Cell(value = value, type = Type.TEXT)
            fun number(value: Number): Cell = Cell(value = value.toString(), type = Type.NUMBER)
        }
    }

    fun writeSingleSheet(
        file: File,
        sheetName: String,
        rows: List<List<Cell>>,
    ) {
        file.parentFile?.mkdirs()
        ZipOutputStream(file.outputStream().buffered()).use { zip ->
            zip.writeEntry(
                name = "[Content_Types].xml",
                content = """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                      <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    </Types>
                """.trimIndent(),
            )
            zip.writeEntry(
                name = "_rels/.rels",
                content = """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                    </Relationships>
                """.trimIndent(),
            )
            zip.writeEntry(
                name = "xl/workbook.xml",
                content = """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                      <sheets>
                        <sheet name="${escapeXmlAttribute(sheetName.take(31).ifBlank { "Sheet1" })}" sheetId="1" r:id="rId1"/>
                      </sheets>
                    </workbook>
                """.trimIndent(),
            )
            zip.writeEntry(
                name = "xl/_rels/workbook.xml.rels",
                content = """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                    </Relationships>
                """.trimIndent(),
            )
            zip.writeEntry(
                name = "xl/worksheets/sheet1.xml",
                content = buildSheetXml(rows),
            )
        }
    }

    private fun ZipOutputStream.writeEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun buildSheetXml(rows: List<List<Cell>>): String = buildString {
        appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        appendLine("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        appendLine("  <sheetData>")
        rows.forEachIndexed { rowIndex, columns ->
            appendLine("    <row r=\"${rowIndex + 1}\">")
            columns.forEachIndexed { columnIndex, cell ->
                val ref = "${columnName(columnIndex)}${rowIndex + 1}"
                when (cell.type) {
                    Cell.Type.TEXT -> appendLine(
                        "      <c r=\"$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${escapeXmlText(cell.value)}</t></is></c>",
                    )
                    Cell.Type.NUMBER -> appendLine(
                        "      <c r=\"$ref\"><v>${escapeXmlText(cell.value)}</v></c>",
                    )
                }
            }
            appendLine("    </row>")
        }
        appendLine("  </sheetData>")
        appendLine("</worksheet>")
    }

    private fun columnName(index: Int): String {
        var current = index
        val result = StringBuilder()
        do {
            result.insert(0, ('A'.code + (current % 26)).toChar())
            current = current / 26 - 1
        } while (current >= 0)
        return result.toString()
    }

    private fun escapeXmlText(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun escapeXmlAttribute(value: String): String = escapeXmlText(value)
}
