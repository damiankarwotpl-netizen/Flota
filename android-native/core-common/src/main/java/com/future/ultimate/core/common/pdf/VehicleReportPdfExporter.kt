package com.future.ultimate.core.common.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.future.ultimate.core.common.model.VehicleReportDraft
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VehicleReportPdfExporter {
    fun export(context: Context, draft: VehicleReportDraft, ownerTag: String): String {
        val normalizedDraft = draft.copy(rej = draft.rej.trim().uppercase())
        val outputDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
            "vehicle-reports",
        ).apply { mkdirs() }
        val outputFile = File(outputDir, buildFileName(normalizedDraft.rej, ownerTag))

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val titlePaint = paint(size = 18f, bold = true)
        val sectionPaint = paint(size = 12f, bold = true)
        val textPaint = paint(size = 10f)
        val subtlePaint = paint(size = 9f, color = Color.DKGRAY)
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        var y = 42f
        canvas.drawText("Raport stanu samochodu", 36f, y, titlePaint)
        y += 18f
        canvas.drawText("Eksport Android-native 1:1", 36f, y, subtlePaint)
        y += 24f

        canvas.drawRect(36f, y, 559f, y + 44f, linePaint)
        canvas.drawText("Marka: ${safe(normalizedDraft.marka)}", 48f, y + 18f, textPaint)
        canvas.drawText("Rejestracja: ${safe(normalizedDraft.rej)}", 250f, y + 18f, textPaint)
        canvas.drawText("Przebieg: ${safe(normalizedDraft.przebieg)}", 430f, y + 18f, textPaint)
        canvas.drawText("Wygenerowano przez: ${ownerTag}", 48f, y + 36f, textPaint)
        y += 64f

        y = drawSection(canvas, y, "Stan pojazdu", sectionPaint)
        y = drawBlock(
            canvas = canvas,
            top = y,
            linePaint = linePaint,
            textPaint = textPaint,
            rows = listOf(
                "Poziom oleju" to safe(normalizedDraft.olej),
                "Wskaźnik paliwa" to safe(normalizedDraft.paliwo),
                "Rodzaj paliwa" to safe(normalizedDraft.rodzajPaliwa),
                "Nowe uszkodzenia" to safe(normalizedDraft.uszkodzenia),
                "Od kiedy?" to safe(normalizedDraft.odKiedy),
            ),
        )

        y = drawSection(canvas, y, "Stan opon / przeglądy", sectionPaint)
        y = drawBlock(
            canvas = canvas,
            top = y,
            linePaint = linePaint,
            textPaint = textPaint,
            rows = listOf(
                "Lewy przedni" to safe(normalizedDraft.lp),
                "Prawy przedni" to safe(normalizedDraft.pp),
                "Lewy tylny" to safe(normalizedDraft.lt),
                "Prawy tylny" to safe(normalizedDraft.pt),
                "Przegląd / Service" to safe(normalizedDraft.serwis),
                "Przegląd techniczny" to safe(normalizedDraft.przeglad),
            ),
        )

        y = drawSection(canvas, y, "Wyposażenie", sectionPaint)
        y = drawBlock(
            canvas = canvas,
            top = y,
            linePaint = linePaint,
            textPaint = textPaint,
            rows = listOf(
                "Trójkąt" to flag(normalizedDraft.trojkat),
                "Kamizelki" to flag(normalizedDraft.kamizelki),
                "Koło zapasowe" to flag(normalizedDraft.kolo),
                "Dowód rejestracyjny" to flag(normalizedDraft.dowod),
                "Apteczka" to flag(normalizedDraft.apteczka),
            ),
        )

        y = drawSection(canvas, y, "Uwagi", sectionPaint)
        canvas.drawRect(36f, y, 559f, y + 140f, linePaint)
        drawWrappedText(canvas, safe(normalizedDraft.uwagi), 48f, y + 18f, 499f, textPaint)
        y += 164f

        canvas.drawText(
            "Dokument generowany automatycznie z modułu Raport stanu samochodu.",
            36f,
            minOf(y, 810f),
            subtlePaint,
        )

        document.finishPage(page)
        outputFile.outputStream().use(document::writeTo)
        document.close()
        return outputFile.absolutePath
    }

    private fun drawSection(canvas: android.graphics.Canvas, y: Float, title: String, paint: Paint): Float {
        canvas.drawText(title, 36f, y, paint)
        return y + 12f
    }

    private fun drawBlock(
        canvas: android.graphics.Canvas,
        top: Float,
        linePaint: Paint,
        textPaint: Paint,
        rows: List<Pair<String, String>>,
    ): Float {
        var y = top + 22f
        rows.forEach { (label, value) ->
            canvas.drawText("$label:", 48f, y, textPaint)
            canvas.drawText(value, 220f, y, textPaint)
            y += 20f
        }
        val bottom = y + 10f
        canvas.drawRect(36f, top, 559f, bottom, linePaint)
        return bottom + 22f
    }

    private fun drawWrappedText(
        canvas: android.graphics.Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint,
    ) {
        if (text.isBlank()) {
            canvas.drawText("-", x, startY, paint)
            return
        }
        var y = startY
        val line = StringBuilder()
        text.split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .forEach { word ->
                val candidate = if (line.isEmpty()) word else "${line} $word"
                if (paint.measureText(candidate) > maxWidth && line.isNotEmpty()) {
                    canvas.drawText(line.toString(), x, y, paint)
                    line.clear()
                    line.append(word)
                    y += 16f
                } else {
                    line.clear()
                    line.append(candidate)
                }
            }
        if (line.isNotEmpty()) {
            canvas.drawText(line.toString(), x, y, paint)
        }
    }

    private fun paint(size: Float, bold: Boolean = false, color: Int = Color.BLACK): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
    }

    private fun buildFileName(registration: String, ownerTag: String): String {
        val datePart = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val regPart = sanitize(registration).ifBlank { "brak_rejestracji" }
        val ownerPart = sanitize(ownerTag).ifBlank { "vehicle_report" }
        return "${datePart}_${ownerPart}_${regPart}.pdf"
    }

    private fun sanitize(value: String): String = value
        .trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')

    private fun safe(value: String): String = value.trim().ifBlank { "-" }
    private fun flag(value: Boolean): String = if (value) "TAK" else "NIE"
}
