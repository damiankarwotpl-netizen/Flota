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
                      <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
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
                      <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                    </Relationships>
                """.trimIndent(),
            )
            zip.writeEntry(
                name = "xl/styles.xml",
                content = buildStylesXml(),
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
                val styleAttribute = if (rowIndex == 0) " s=\"1\"" else ""
                when (cell.type) {
                    Cell.Type.TEXT -> appendLine(
                        "      <c r=\"$ref\"$styleAttribute t=\"inlineStr\"><is><t xml:space=\"preserve\">${escapeXmlText(cell.value)}</t></is></c>",
                    )
                    Cell.Type.NUMBER -> appendLine(
                        "      <c r=\"$ref\"$styleAttribute><v>${escapeXmlText(cell.value)}</v></c>",
                    )
                }
            }
            appendLine("    </row>")
        }
        appendLine("  </sheetData>")
        appendLine("</worksheet>")
    }

    private fun buildStylesXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
          <fonts count="2">
            <font><sz val="11"/><color theme="1"/><name val="Calibri"/><family val="2"/></font>
            <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/><family val="2"/></font>
          </fonts>
          <fills count="3">
            <fill><patternFill patternType="none"/></fill>
            <fill><patternFill patternType="gray125"/></fill>
            <fill><patternFill patternType="solid"><fgColor rgb="FF1F4E78"/><bgColor indexed="64"/></patternFill></fill>
          </fills>
          <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
          <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
          <cellXfs count="2">
            <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
            <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
          </cellXfs>
          <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
        </styleSheet>
    """.trimIndent()

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
