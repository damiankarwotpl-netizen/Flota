package com.future.ultimate.core.common.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.BitmapFactory
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
        canvas.drawText("Miejsca: ${safe(normalizedDraft.seats)}", 430f, y + 18f, textPaint)
        canvas.drawText("Przebieg: ${safe(normalizedDraft.przebieg)}", 48f, y + 36f, textPaint)
        canvas.drawText("Wypełnione przez: ${safe(normalizedDraft.filledBy.ifBlank { ownerTag })}", 250f, y + 36f, textPaint)
        y += 64f

        y = drawSection(canvas, y, "Stan pojazdu", sectionPaint)
        y = drawBlock(
            canvas = canvas,
            top = y,
            linePaint = linePaint,
            textPaint = textPaint,
            rows = listOf(
                "Bez uszkodzeń" to flag(normalizedDraft.noDamage),
                "Od kiedy" to safe(normalizedDraft.damageSince),
                "Nowe uszkodzenie (opis)" to safe(normalizedDraft.damageDescription),
                "Nowe uszkodzenie (zdjęcia)" to normalizedDraft.damagePhotoPaths.size.toString(),
                "Przebieg" to safe(normalizedDraft.przebieg),
                "Rodzaj paliwa" to safe(normalizedDraft.rodzajPaliwa),
                "Poziom oleju" to safe(normalizedDraft.olej),
                "Auto wysprzątane/umyte" to flag(normalizedDraft.cleaned),
                "Producent opon" to safe(normalizedDraft.tireProducer),
                "Lampki ostrzegawcze" to flag(normalizedDraft.warningLights),
                "Opis lampki ostrzegawczej" to safe(normalizedDraft.warningLightsDescription),
            ),
        )

        y = drawSection(canvas, y, "Stan opon", sectionPaint)
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
        canvas.drawText("Zdjęcia dodano jako kolejne strony dokumentu.", 36f, minOf(y + 16f, 810f), subtlePaint)

        document.finishPage(page)
        appendPhotoPages(document, normalizedDraft)
        outputFile.outputStream().use(document::writeTo)
        document.close()
        return outputFile.absolutePath
    }

    private fun appendPhotoPages(document: PdfDocument, draft: VehicleReportDraft) {
        val basePhotos = draft.photoPaths + listOfNotNull(draft.dashboardPhotoPath.takeIf { it.isNotBlank() })
        basePhotos.forEachIndexed { index, path ->
            val bitmap = BitmapFactory.decodeFile(path) ?: return@forEachIndexed
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, index + 2).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val paint = paint(size = 12f, bold = true)
            canvas.drawText("Zdjęcie ${index + 1}", 36f, 36f, paint)
            val scale = minOf(523f / bitmap.width.toFloat(), 760f / bitmap.height.toFloat())
            val width = bitmap.width * scale
            val height = bitmap.height * scale
            val left = (595f - width) / 2f
            val top = 50f
            canvas.drawBitmap(bitmap, null, android.graphics.RectF(left, top, left + width, top + height), null)
            document.finishPage(page)
        }
        val damageLabel = buildString {
            append("Nowe uszkodzenie")
            if (draft.damageSince.isNotBlank()) append(" - ${draft.damageSince}")
        }
        draft.damagePhotoPaths.forEachIndexed { index, path ->
            val bitmap = BitmapFactory.decodeFile(path) ?: return@forEachIndexed
            val pageNumber = basePhotos.size + index + 2
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val paint = paint(size = 12f, bold = true)
            canvas.drawText("$damageLabel (${index + 1})", 36f, 36f, paint)
            val scale = minOf(523f / bitmap.width.toFloat(), 760f / bitmap.height.toFloat())
            val width = bitmap.width * scale
            val height = bitmap.height * scale
            val left = (595f - width) / 2f
            val top = 50f
            canvas.drawBitmap(bitmap, null, android.graphics.RectF(left, top, left + width, top + height), null)
            document.finishPage(page)
        }
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
