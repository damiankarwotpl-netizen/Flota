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
        )

        canvas.drawText(sectionTitle, margin, y, sectionPaint)
        y += 18f

        items.forEachIndexed { index, item ->
            if (y > pageHeight - 90f) {
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
                )
                canvas.drawText("$sectionTitle (cd.)", margin, y, sectionPaint)
                y += 18f
            }

            val worker = "${item.name} ${item.surname}".trim().ifBlank { "Pracownik #${item.workerId}" }
            canvas.drawRect(margin, y - 12f, pageWidth - margin, y + 42f, linePaint)
            canvas.drawText("${index + 1}. $worker", margin + 12f, y + 4f, textPaint)
            canvas.drawText("Pozycja: ${safe(item.item)}", margin + 12f, y + 20f, textPaint)
            canvas.drawText("Rozmiar: ${safe(item.size)}", margin + 220f, y + 20f, textPaint)
            canvas.drawText("Ilość: ${item.qty}", margin + 360f, y + 20f, textPaint)
            canvas.drawText("Status: ${if (item.issued) "wydane" else "niewydane"}", margin + 440f, y + 20f, textPaint)
            y += 58f
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
    ): Float {
        var y = 42f
        canvas.drawText(title, margin, y, titlePaint)
        y += 18f
        canvas.drawText("Eksport Android-native 1:1", margin, y, subtitlePaint)
        y += 24f
        canvas.drawRect(margin, y, 559f, y + 62f, linePaint)
        canvas.drawText("ID: $orderId", margin + 12f, y + 18f, textPaint)
        canvas.drawText("Data: ${safe(date)}", margin + 120f, y + 18f, textPaint)
        canvas.drawText("Zakład: ${safe(plant)}", margin + 280f, y + 18f, textPaint)
        canvas.drawText("Status: ${safe(status)}", margin + 12f, y + 38f, textPaint)
        canvas.drawText("Opis: ${safe(description)}", margin + 120f, y + 38f, textPaint)
        return y + 86f
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
}
