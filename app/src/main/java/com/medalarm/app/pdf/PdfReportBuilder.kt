package com.medalarm.app.pdf

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.medalarm.app.R
import com.medalarm.app.data.local.dao.DoseLogDao
import com.medalarm.app.data.local.dao.MedicationDao
import com.medalarm.app.domain.model.DoseStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Renders a one-page PDF adherence report for a date range. No third-party
 * deps — uses [PdfDocument] from the platform.
 *
 * Format intentionally minimal: header, per-medication adherence table,
 * footer with disclaimer reminder. The report exists to be shared with
 * a doctor, not as a substitute for a medical record.
 */
@Singleton
class PdfReportBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val medicationDao: MedicationDao,
    private val doseLogDao: DoseLogDao
) {

    suspend fun export(target: Uri, days: Int = 30): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val start = today.minusDays(days.toLong() - 1).atStartOfDay(zone).toInstant()
            val end = today.plusDays(1).atStartOfDay(zone).toInstant()

            val meds = medicationDao.observeAll().first()
            val logs = doseLogDao.observeRange(start.toEpochMilli(), end.toEpochMilli()).first()

            val byMed = logs.groupBy { it.medicationId }
            val rows = meds.map { med ->
                val these = byMed[med.id].orEmpty()
                val taken = these.count { it.status == DoseStatus.TAKEN }
                val total = these.count { it.status != DoseStatus.PENDING }
                Row(med.name, med.unit.name.lowercase(), taken, total)
            }

            val doc = PdfDocument()
            try {
                val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create()
                val page = doc.startPage(pageInfo)
                drawPage(page, today, days, rows)
                doc.finishPage(page)

                context.contentResolver.openOutputStream(target, "w")?.use { stream ->
                    doc.writeTo(stream)
                } ?: error("Could not open output stream for $target")
            } finally {
                doc.close()
            }
        }
    }.onFailure { Timber.e(it, "PDF export failed") }

    private fun drawPage(page: PdfDocument.Page, today: LocalDate, days: Int, rows: List<Row>) {
        val canvas = page.canvas
        val titlePaint = Paint().apply {
            color = 0xFF00696C.toInt()
            textSize = 22f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val labelPaint = Paint().apply {
            color = 0xFF333333.toInt()
            textSize = 12f
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = 0xFF111111.toInt()
            textSize = 14f
            isAntiAlias = true
        }
        val mutedPaint = Paint().apply {
            color = 0xFF666666.toInt()
            textSize = 10f
            isAntiAlias = true
        }
        val divider = Paint().apply {
            color = 0xFFCCCCCC.toInt()
            strokeWidth = 1f
        }

        var y = MARGIN + 32f
        canvas.drawText("MedAlarm — " + context.getString(R.string.history_title), MARGIN.toFloat(), y, titlePaint)
        y += 22f
        val rangeStart = today.minusDays(days.toLong() - 1)
        canvas.drawText(
            "${rangeStart.format(DATE_FMT)} → ${today.format(DATE_FMT)}  ·  " +
                "${context.getString(R.string.settings_about_version)} " +
                com.medalarm.app.BuildConfig.VERSION_NAME,
            MARGIN.toFloat(),
            y,
            labelPaint
        )
        y += 18f
        canvas.drawLine(MARGIN.toFloat(), y, (A4_WIDTH - MARGIN).toFloat(), y, divider)
        y += 24f

        // Table header
        canvas.drawText("Medication", MARGIN.toFloat(), y, mutedPaint)
        canvas.drawText("Taken / Total", (A4_WIDTH - MARGIN - 180).toFloat(), y, mutedPaint)
        canvas.drawText("Adherence", (A4_WIDTH - MARGIN - 80).toFloat(), y, mutedPaint)
        y += 16f
        canvas.drawLine(MARGIN.toFloat(), y, (A4_WIDTH - MARGIN).toFloat(), y, divider)
        y += 20f

        if (rows.isEmpty()) {
            canvas.drawText(context.getString(R.string.history_empty), MARGIN.toFloat(), y, bodyPaint)
            y += 20f
        } else {
            rows.forEach { row ->
                canvas.drawText(row.name + " (" + row.unit + ")", MARGIN.toFloat(), y, bodyPaint)
                canvas.drawText("${row.taken} / ${row.total}", (A4_WIDTH - MARGIN - 180).toFloat(), y, bodyPaint)
                val pct = if (row.total > 0) (row.taken * 100 / row.total) else 0
                canvas.drawText("$pct%", (A4_WIDTH - MARGIN - 80).toFloat(), y, bodyPaint)
                y += 22f
                if (y > A4_HEIGHT - 100) return@forEach   // single-page truncation
            }
        }

        // Footer disclaimer
        val footerY = A4_HEIGHT - MARGIN - 24f
        canvas.drawLine(MARGIN.toFloat(), footerY - 12f, (A4_WIDTH - MARGIN).toFloat(), footerY - 12f, divider)
        canvas.drawText(context.getString(R.string.disclaimer_short), MARGIN.toFloat(), footerY, mutedPaint)
        canvas.drawText(
            "Generated " + Instant.now().toString(),
            MARGIN.toFloat(),
            footerY + 14f,
            mutedPaint
        )
    }

    private data class Row(val name: String, val unit: String, val taken: Int, val total: Int)

    private companion object {
        // A4 at 72 dpi
        const val A4_WIDTH = 595
        const val A4_HEIGHT = 842
        const val MARGIN = 36

        val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
