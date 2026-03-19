package com.future.ultimate.core.common.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
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
        status: String,
        description: String,
        items: List<ClothesOrderItemListItem>,
    ): String {
        return exportDocument(
            context = context,
            orderId = orderId,
            date = date,
            plant = plant,
            status = status,
            description = description,
            items = items,
            title = "Zamówienie odzieży roboczej",
            sectionTitle = "Pozycje zamówienia",
            filePrefix = "clothes_order",
        )
    }

    fun exportIssueReport(
        context: Context,
        orderId: Long,
        date: String,
        plant: String,
        status: String,
        description: String,
        items: List<ClothesOrderItemListItem>,
    ): String {
        return exportDocument(
            context = context,
            orderId = orderId,
            date = date,
            plant = plant,
            status = status,
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
        status: String,
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
        val subtitlePaint = paint(size = 10f, color = Color.DKGRAY)
        val textPaint = paint(size = 10f)
        val sectionPaint = paint(size = 12f, bold = true)
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = drawHeader(
            canvas = canvas,
            titlePaint = titlePaint,
            subtitlePaint = subtitlePaint,
            textPaint = textPaint,
            linePaint = linePaint,
            orderId = orderId,
            date = date,
            plant = plant,
            status = status,
            description = description,
            title = title,
            margin = margin,
            contentWidth = contentWidth,
        )

        canvas.drawText(sectionTitle, margin, y, sectionPaint)
        y += 18f

        items.forEachIndexed { index, item ->
            val worker = "${item.name} ${item.surname}".trim().ifBlank { "Pracownik #${item.workerId}" }
            val workerLines = wrapText("${index + 1}. $worker", textPaint, contentWidth - 24f)
            val itemLines = wrapText("Pozycja: ${safe(item.item)}", textPaint, contentWidth - 24f)
            val detailsLine = "Rozmiar: ${safe(item.size)} • Ilość: ${item.qty} • Status: ${if (item.issued) "wydane" else "niewydane"}"
            val detailsLines = wrapText(detailsLine, textPaint, contentWidth - 24f)
            val rowHeight = 20f + ((workerLines.size + itemLines.size + detailsLines.size) * 14f)

            if (y + rowHeight > pageHeight - 50f) {
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = drawHeader(
                    canvas = canvas,
                    titlePaint = titlePaint,
                    subtitlePaint = subtitlePaint,
                    textPaint = textPaint,
                    linePaint = linePaint,
                    orderId = orderId,
                    date = date,
                    plant = plant,
                    status = status,
                    description = description,
                    title = title,
                    margin = margin,
                    contentWidth = contentWidth,
                )
                canvas.drawText("$sectionTitle (cd.)", margin, y, sectionPaint)
                y += 18f
            }

            canvas.drawRect(margin, y - 12f, pageWidth - margin, y + rowHeight - 4f, linePaint)
            y = drawLines(canvas, workerLines, margin + 12f, y + 4f, textPaint)
            y = drawLines(canvas, itemLines, margin + 12f, y + 2f, textPaint)
            y = drawLines(canvas, detailsLines, margin + 12f, y + 2f, textPaint)
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
        subtitlePaint: Paint,
        textPaint: Paint,
        linePaint: Paint,
        orderId: Long,
        date: String,
        plant: String,
        status: String,
        description: String,
        title: String,
        margin: Float,
        contentWidth: Float,
    ): Float {
        var y = 42f
        canvas.drawText(title, margin, y, titlePaint)
        y += 18f
        canvas.drawText("Eksport Android-native 1:1", margin, y, subtitlePaint)
        y += 24f
        val boxTop = y
        val metaLines = listOf(
            "ID: $orderId • Data: ${safe(date)}",
            "Zakład: ${safe(plant)} • Status: ${safe(status)}",
        )
        val descriptionLines = wrapText("Opis: ${safe(description)}", textPaint, contentWidth - 24f)
        val boxHeight = 18f + ((metaLines.size + descriptionLines.size) * 14f) + 12f
        canvas.drawRect(margin, boxTop, margin + contentWidth, boxTop + boxHeight, linePaint)
        var textY = boxTop + 18f
        metaLines.forEach { line ->
            canvas.drawText(line, margin + 12f, textY, textPaint)
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
