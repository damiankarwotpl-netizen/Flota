package com.future.ultimate.admin.payroll

import com.future.ultimate.core.common.repository.ContactListItem
import com.future.ultimate.core.common.repository.PayrollWorkbookRow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.random.Random

data class PayslipRow(
    val raw: List<String>,
    val name: String,
    val surname: String,
    val pesel: String?,
    val email: String?,
)

data class PayslipData(
    val headers: List<String>,
    val rows: List<PayslipRow>,
    val columnIndex: ColumnIndex,
)

data class ColumnIndex(
    val nameIdx: Int,
    val surnameIdx: Int,
    val peselIdx: Int?,
    val emailIdx: Int?,
)

data class RawExcelData(
    val headers: List<String>,
    val rows: List<List<String>>,
)

interface ExcelParser {
    fun parse(file: File): RawExcelData
}

class DelimitedExcelParser : ExcelParser {
    override fun parse(file: File): RawExcelData {
        val lines = file.readLines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return RawExcelData(emptyList(), emptyList())
        val splitter = detectDelimiter(lines.first())
        val headers = lines.first().split(splitter).map { it.trim() }
        val rows = lines.drop(1).map { line -> line.split(splitter).map { it.trim() } }
        return RawExcelData(headers = headers, rows = rows)
    }

    private fun detectDelimiter(line: String): Char = when {
        line.count { it == '\t' } > 0 -> '\t'
        line.count { it == ';' } > 0 -> ';'
        else -> ','
    }
}

class PayslipMapper {
    fun map(rawData: RawExcelData): PayslipData {
        val headers = rawData.headers
        val columnIndex = mapColumns(headers)
        val rows = rawData.rows.map { row ->
            PayslipRow(
                raw = row,
                name = row.getOrElse(columnIndex.nameIdx) { "" }.trim(),
                surname = row.getOrElse(columnIndex.surnameIdx) { "" }.trim(),
                pesel = columnIndex.peselIdx?.let { row.getOrElse(it) { "" }.trim().ifBlank { null } },
                email = columnIndex.emailIdx?.let { row.getOrElse(it) { "" }.trim().ifBlank { null } },
            )
        }
        return PayslipData(headers = headers, rows = rows, columnIndex = columnIndex)
    }

    private fun mapColumns(headers: List<String>): ColumnIndex {
        val normalized = headers.map { it.trim().lowercase() }
        val nameIdx = normalized.indexOfFirst { it.contains("imi") || it == "name" }.takeIf { it >= 0 } ?: 0
        val surnameIdx = normalized.indexOfFirst { it.contains("nazw") || it == "surname" || it == "last name" }.takeIf { it >= 0 } ?: 1
        val peselIdx = normalized.indexOfFirst { it.contains("pesel") }.takeIf { it >= 0 }
        val emailIdx = normalized.indexOfFirst { it.contains("mail") || it.contains("e-mail") }.takeIf { it >= 0 }
        return ColumnIndex(
            nameIdx = nameIdx,
            surnameIdx = surnameIdx,
            peselIdx = peselIdx,
            emailIdx = emailIdx,
        )
    }
}

class PayslipFilter {
    fun search(data: PayslipData, query: String): List<PayslipRow> {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isBlank()) return data.rows
        return data.rows.filter { row ->
            row.raw.any { it.lowercase().contains(normalizedQuery) }
        }
    }
}

class PayslipGenerator {
    fun toWorkbookRows(rows: List<PayslipRow>, workplace: String = ""): List<PayrollWorkbookRow> = rows.map { row ->
        PayrollWorkbookRow(
            name = row.name.ifBlank { "0" },
            surname = row.surname.ifBlank { "0" },
            workplace = workplace.ifBlank { "0" },
            email = row.email.orEmpty().ifBlank { "0" },
            amount = row.raw.lastOrNull().orEmpty().ifBlank { "0" },
        )
    }
}

interface ExportService {
    suspend fun export(rows: List<PayrollWorkbookRow>): String
}

class EmailResolver {
    fun resolve(row: PayslipRow, contacts: List<ContactListItem>): String? {
        val normalizedPesel = row.pesel.orEmpty().trim()
        if (normalizedPesel.isNotBlank()) {
            val byPesel = contacts.firstOrNull { it.notes.contains(normalizedPesel, ignoreCase = true) }?.email?.trim()
            if (!byPesel.isNullOrBlank()) return byPesel
        }
        val byName = contacts.firstOrNull {
            it.name.equals(row.name, ignoreCase = true) && it.surname.equals(row.surname, ignoreCase = true)
        }?.email?.trim()
        return byName?.ifBlank { null } ?: row.email
    }
}

class MailingManager(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun sendSingle(sendBlock: suspend () -> Unit) = withContext(ioDispatcher) {
        sendBlock()
    }

    suspend fun sendBulk(
        recipients: List<String>,
        sendBlock: suspend (String) -> Unit,
        onSkip: (String) -> Unit = {},
        onError: (String, Throwable) -> Unit = { _, _ -> },
    ) = withContext(ioDispatcher) {
        var sentInBatch = 0
        recipients.forEach { email ->
            if (email.isBlank()) {
                onSkip(email)
                return@forEach
            }
            runCatching { sendBlock(email) }
                .onFailure { error ->
                    delay(750)
                    runCatching { sendBlock(email) }
                        .onFailure { onError(email, it) }
                }
            sentInBatch++
            delay(Random.nextLong(3_000, 7_000))
            if (sentInBatch == 30) {
                delay(60_000)
                sentInBatch = 0
            }
        }
    }
}

class PayslipModule(
    private val excelParser: ExcelParser,
    private val mapper: PayslipMapper,
    private val filter: PayslipFilter,
    private val generator: PayslipGenerator,
    private val exportService: ExportService,
) {
    private var loaded: PayslipData = PayslipData(emptyList(), emptyList(), ColumnIndex(0, 1, null, null))

    fun load(file: File): PayslipData {
        loaded = mapper.map(excelParser.parse(file))
        return loaded
    }

    fun loadFromDelimitedText(rawInput: String): PayslipData {
        val rows = rawInput.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        if (rows.isEmpty()) {
            loaded = PayslipData(emptyList(), emptyList(), ColumnIndex(0, 1, null, null))
            return loaded
        }
        val splitter = when {
            rows.first().contains('\t') -> '\t'
            rows.first().contains(';') -> ';'
            else -> ','
        }
        val raw = RawExcelData(
            headers = rows.first().split(splitter).map { it.trim() },
            rows = rows.drop(1).map { it.split(splitter).map(String::trim) },
        )
        loaded = mapper.map(raw)
        return loaded
    }

    fun loadFromBytes(
        fileName: String?,
        mimeType: String?,
        bytes: ByteArray,
    ): PayslipData {
        if (bytes.isEmpty()) {
            loaded = PayslipData(emptyList(), emptyList(), ColumnIndex(0, 1, null, null))
            return loaded
        }
        val isXlsx = mimeType.orEmpty().contains("spreadsheetml", ignoreCase = true) ||
            fileName.orEmpty().endsWith(".xlsx", ignoreCase = true)
        return if (isXlsx) {
            val rawData = parseXlsxRawData(bytes)
            loaded = mapper.map(rawData)
            loaded
        } else {
            loadFromDelimitedText(bytes.toString(Charsets.UTF_8))
        }
    }

    private fun parseXlsxRawData(bytes: ByteArray): RawExcelData {
        ByteArrayInputStream(bytes).use { input ->
            WorkbookFactory.create(input).use { workbook ->
                val sheet = workbook.getSheetAt(0) ?: return RawExcelData(emptyList(), emptyList())
                val formatter = DataFormatter()
                val evaluator = workbook.creationHelper.createFormulaEvaluator()
                val allRows = mutableListOf<List<String>>()
                val maxColumns = (sheet.firstRowNum..sheet.lastRowNum)
                    .mapNotNull { idx -> sheet.getRow(idx)?.lastCellNum?.toInt()?.takeIf { it > 0 } }
                    .maxOrNull() ?: 0
                if (maxColumns == 0) return RawExcelData(emptyList(), emptyList())
                for (rowIndex in sheet.firstRowNum..sheet.lastRowNum) {
                    val row = sheet.getRow(rowIndex)
                    val values = (0 until maxColumns).map { cellIndex ->
                        formatter.formatCellValue(row?.getCell(cellIndex), evaluator).trim()
                    }
                    if (values.any { it.isNotBlank() }) {
                        allRows += values
                    }
                }
                if (allRows.isEmpty()) return RawExcelData(emptyList(), emptyList())
                return RawExcelData(
                    headers = allRows.first(),
                    rows = allRows.drop(1),
                )
            }
        }
    }

    fun getAll(): List<PayslipRow> = loaded.rows

    fun search(query: String): List<PayslipRow> = filter.search(loaded, query)

    suspend fun export(rows: List<PayslipRow>, workplace: String = ""): String =
        exportService.export(generator.toWorkbookRows(rows, workplace))
}
