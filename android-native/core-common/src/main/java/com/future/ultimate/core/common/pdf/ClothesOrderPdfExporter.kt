package com.future.ultimate.core.common.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.future.ultimate.core.common.repository.ClothesOrderItemListItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ClothesOrderPdfExporter {
    fun export(
        context: Context,
        orderId: Long,
        date: String,
        plant: String,
        invoiceCompany: String,
        invoiceNip: String,
        description: String,
        items: List<ClothesOrderItemListItem>,
    ): String {
        return exportDocument(
            context = context,
            orderId = orderId,
            date = date,
            plant = plant,
            invoiceCompany = invoiceCompany,
            invoiceNip = invoiceNip,
            description = description,
            items = items,
            title = "Zamówienie Agencja Future-group",
            sectionTitle = "Zamówienie",
            filePrefix = "clothes_order",
        )
    }

    fun exportIssueReport(
        context: Context,
        orderId: Long,
        date: String,
        plant: String,
        invoiceCompany: String,
        invoiceNip: String,
        description: String,
        items: List<ClothesOrderItemListItem>,
    ): String {
        return exportDocument(
            context = context,
            orderId = orderId,
            date = date,
            plant = plant,
            invoiceCompany = invoiceCompany,
            invoiceNip = invoiceNip,
            description = description,
            items = items.sortedWith(compareBy({ it.surname.lowercase() }, { it.name.lowercase() }, { it.item.lowercase() })),
            title = "Raport wydania odzieży",
            sectionTitle = "Pozycje do wydania",
            filePrefix = "clothes_issue",
        )
    }

    private fun exportDocument(
        context: Context,
        orderId: Long,
        date: String,
        plant: String,
        invoiceCompany: String,
        invoiceNip: String,
        description: String,
        items: List<ClothesOrderItemListItem>,
        title: String,
        sectionTitle: String,
        filePrefix: String,
    ): String {
        val outputDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
            "clothes-orders",
        ).apply { mkdirs() }
        val outputFile = File(outputDir, buildFileName(filePrefix, orderId))

        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val contentWidth = pageWidth - (margin * 2)
        val titlePaint = paint(size = 18f, bold = true)
        val textPaint = paint(size = 11f, color = Color.rgb(44, 44, 44))
        val sectionPaint = paint(size = 13f, bold = true)
        val sectionBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(238, 242, 247)
            style = Paint.Style.FILL
        }
        val cardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(250, 250, 250)
            style = Paint.Style.FILL
        }
        val linePaint = Paint().apply {
            color = Color.rgb(210, 215, 222)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(247, 249, 252)
            style = Paint.Style.FILL
        }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = drawHeader(
            canvas = canvas,
            titlePaint = titlePaint,
            textPaint = textPaint,
            sectionPaint = sectionPaint,
            cardFillPaint = cardFillPaint,
            linePaint = linePaint,
            orderId = orderId,
            date = date,
            plant = plant,
            invoiceCompany = invoiceCompany,
            invoiceNip = invoiceNip,
            description = description,
            title = title,
            margin = margin,
            contentWidth = contentWidth,
        )

        val sectionRect = RectF(margin, y - 14f, pageWidth - margin, y + 8f)
        canvas.drawRoundRect(sectionRect, 10f, 10f, sectionBgPaint)
        canvas.drawText(sectionTitle, margin + 10f, y, sectionPaint)
        y += 24f

        val isOrderPdf = filePrefix == "clothes_order"
        val renderedLines = if (isOrderPdf) {
            items.groupBy { safe(it.item) to safe(it.size) }
                .toList()
                .sortedWith(compareBy({ it.first.first.lowercase() }, { it.first.second.lowercase() }))
                .mapIndexed { index, (key, grouped) ->
                    "${index + 1}. ${key.first} ${key.second} - ${grouped.sumOf { it.qty }} sztuki"
                }
        } else {
            items.mapIndexed { index, item ->
                val worker = "${item.name} ${item.surname}".trim().ifBlank { "Pracownik #${item.workerId}" }
                "${index + 1}. $worker • ${safe(item.item)} • ${safe(item.size)} • ${item.qty} szt. • ${if (item.issued) "wydane" else "niewydane"}"
            }
        }

        renderedLines.forEachIndexed { index, line ->
            val wrapped = wrapText(line, textPaint, contentWidth - 24f)
            val rowHeight = 20f + (wrapped.size * 14f)

            if (y + rowHeight > pageHeight - 50f) {
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = drawHeader(
                    canvas = canvas,
                    titlePaint = titlePaint,
                    textPaint = textPaint,
                    sectionPaint = sectionPaint,
                    cardFillPaint = cardFillPaint,
                    linePaint = linePaint,
                    orderId = orderId,
                    date = date,
                    plant = plant,
                    invoiceCompany = invoiceCompany,
                    invoiceNip = invoiceNip,
                    description = description,
                    title = title,
                    margin = margin,
                    contentWidth = contentWidth,
                )
                val continuedRect = RectF(margin, y - 14f, pageWidth - margin, y + 8f)
                canvas.drawRoundRect(continuedRect, 10f, 10f, sectionBgPaint)
                canvas.drawText("$sectionTitle (cd.)", margin + 10f, y, sectionPaint)
                y += 24f
            }

            val rowRect = RectF(margin, y - 12f, pageWidth - margin, y + rowHeight - 4f)
            if (((index + 1) % 2) == 0) {
                canvas.drawRoundRect(rowRect, 6f, 6f, rowPaint)
            }
            canvas.drawRect(margin, y - 12f, pageWidth - margin, y + rowHeight - 4f, linePaint)
            y = drawLines(canvas, wrapped, margin + 12f, y + 4f, textPaint)
            y += 10f
        }

        document.finishPage(page)
        outputFile.outputStream().use(document::writeTo)
        document.close()
        return outputFile.absolutePath
    }

    private fun drawHeader(
        canvas: android.graphics.Canvas,
        titlePaint: Paint,
        textPaint: Paint,
        sectionPaint: Paint,
        cardFillPaint: Paint,
        linePaint: Paint,
        orderId: Long,
        date: String,
        plant: String,
        invoiceCompany: String,
        invoiceNip: String,
        description: String,
        title: String,
        margin: Float,
        contentWidth: Float,
    ): Float {
        var y = 42f
        canvas.drawText(title, margin, y, titlePaint)
        y += 24f
        val boxTop = y
        val metaLines = listOf(
            "ID: $orderId • Data: ${safe(date)}",
            "Zakład: ${safe(plant)}",
            "Dane do faktury:",
            safe(invoiceCompany),
            "NIP: ${safe(invoiceNip)}",
            "Adres wysyłki:",
            "Szkolna 15, 47-225 Kędzierzyn-Koźle",
        )
        val descriptionLines = wrapText("Opis: ${safe(description)}", textPaint, contentWidth - 24f)
        val boxHeight = 18f + ((metaLines.size + descriptionLines.size) * 14f) + 12f
        val cardRect = RectF(margin, boxTop, margin + contentWidth, boxTop + boxHeight)
        canvas.drawRoundRect(cardRect, 10f, 10f, cardFillPaint)
        canvas.drawRoundRect(cardRect, 10f, 10f, linePaint)
        var textY = boxTop + 18f
        metaLines.forEach { line ->
            val paint = if (line == "Dane do faktury:" || line == "Adres wysyłki:") sectionPaint else textPaint
            canvas.drawText(line, margin + 12f, textY, paint)
            textY += 14f
        }
        textY = drawLines(canvas, descriptionLines, margin + 12f, textY, textPaint)
        return boxTop + boxHeight + 24f
    }

    private fun paint(size: Float, bold: Boolean = false, color: Int = Color.BLACK): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
    }

    private fun buildFileName(prefix: String, orderId: Long): String {
        val datePart = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return "${datePart}_${prefix}_${orderId}.pdf"
    }

    private fun safe(value: String): String = value.trim().ifBlank { "-" }

    private fun drawLines(
        canvas: android.graphics.Canvas,
        lines: List<String>,
        x: Float,
        startY: Float,
        paint: Paint,
        lineHeight: Float = 14f,
    ): Float {
        var y = startY
        lines.forEach { line ->
            canvas.drawText(line, x, y, paint)
            y += lineHeight
        }
        return y
    }

    private fun wrapText(value: String, paint: Paint, maxWidth: Float): List<String> {
        val normalized = safe(value)
        if (paint.measureText(normalized) <= maxWidth) return listOf(normalized)

        val lines = mutableListOf<String>()
        var currentLine = ""
        normalized.split(Regex("\\s+")).forEach { word ->
            val candidate = if (currentLine.isBlank()) word else "$currentLine $word"
            if (paint.measureText(candidate) <= maxWidth) {
                currentLine = candidate
            } else {
                if (currentLine.isNotBlank()) {
                    lines += currentLine
                }
                currentLine = word
            }
        }
        if (currentLine.isNotBlank()) {
            lines += currentLine
        }
        return if (lines.isEmpty()) listOf(normalized) else lines
    }
}
