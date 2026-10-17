package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.AttendanceRecord
import com.example.data.model.BookingMessage
import java.io.File
import java.io.FileOutputStream

object PrintUtils {

    fun generateAttendancePdf(
        context: Context,
        dutyDate: String,
        groupName: String,
        records: List<AttendanceRecord>
    ): File? {
        return try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.rgb(6, 61, 41)
                textSize = 20f
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }

            val subtitlePaint = Paint().apply {
                color = Color.rgb(23, 53, 42)
                textSize = 12f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            val headerPaint = Paint().apply {
                color = Color.rgb(6, 61, 41)
                textSize = 12f
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.RIGHT
            }

            val textPaint = Paint().apply {
                color = Color.rgb(23, 53, 42)
                textSize = 11f
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }

            val linePaint = Paint().apply {
                color = Color.rgb(225, 234, 228)
                strokeWidth = 1f
            }

            val bgHeaderPaint = Paint().apply {
                color = Color.rgb(221, 245, 234)
            }

            // Header Banner
            canvas.drawText("روزانہ حاضری رپورٹ — کارکنان ڈیوٹی لسٹ", 595f / 2f, 40f, titlePaint)
            canvas.drawText("تاریخ: $dutyDate  |  گروپ: $groupName  |  کل کارکنان: ${records.size}", 595f / 2f, 65f, subtitlePaint)

            var y = 100f
            // Table Header Box
            canvas.drawRect(30f, y - 18f, 565f, y + 10f, bgHeaderPaint)
            canvas.drawText("کارکن کا نام", 550f, y, headerPaint)
            canvas.drawText("آنے کا وقت", 320f, y, headerPaint)
            canvas.drawText("جانے کا وقت", 180f, y, headerPaint)
            canvas.drawText("حیثیت", 80f, y, headerPaint)

            y += 20f
            canvas.drawLine(30f, y - 5f, 565f, y - 5f, linePaint)

            records.forEachIndexed { index, record ->
                if (y > 800f) return@forEachIndexed
                val statusText = when {
                    record.arrivalTime != "-" && record.departureTime != "-" -> "مکمل"
                    record.arrivalTime != "-" -> "حاضر"
                    else -> "غیر حاضر"
                }

                val coordText = if (record.isCoordinator) " (کوآرڈینیٹر)" else ""
                canvas.drawText("${index + 1}. ${record.workerName}$coordText", 550f, y, textPaint)
                canvas.drawText(record.arrivalTime, 320f, y, textPaint)
                canvas.drawText(record.departureTime, 180f, y, textPaint)
                canvas.drawText(statusText, 80f, y, textPaint)

                canvas.drawLine(30f, y + 8f, 565f, y + 8f, linePaint)
                y += 26f
            }

            document.finishPage(page)

            val dir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(dir, "attendance_${dutyDate}_$groupName.pdf")
            val outputStream = FileOutputStream(file)
            document.writeTo(outputStream)
            outputStream.close()
            document.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun sharePdf(context: Context, file: File, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "رپورٹ شیئر کریں"))
    }
}
