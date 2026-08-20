package com.example.workouttracker.export

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.workouttracker.domain.model.Exercise
import com.example.workouttracker.domain.model.WorkoutSessionWithSets
import com.example.workouttracker.domain.model.WorkoutStatus
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Offline PDF report generator using Android PdfDocument API.
 * A4 format (595 x 842 points), Russian typography, multi-page pagination.
 */
object PdfReportExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_LEFT = 40f
    private const val MARGIN_TOP = 50f
    private const val MARGIN_BOTTOM = 50f
    private const val LINE_HEIGHT = 18f
    private const val SECTION_GAP = 12f

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))

    fun generateReportToStream(
        context: Context,
        sessions: List<WorkoutSessionWithSets>,
        exercises: Map<Long, Exercise>,
        startDate: Long,
        endDate: Long,
        outputStream: OutputStream
    ) {
        val document = PdfDocument()
        var pageNumber = 1
        var currentPage = startNewPage(document, pageNumber)
        var canvas = currentPage.canvas
        var yPos = MARGIN_TOP

        val titlePaint = Paint().apply {
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            textSize = 12f
            isAntiAlias = true
        }
        val smallPaint = Paint().apply {
            textSize = 10f
            color = android.graphics.Color.DKGRAY
            isAntiAlias = true
        }

        val sortedSessions = sessions
            .filter { it.session.status == WorkoutStatus.COMPLETED }
            .sortedByDescending { it.session.date }

        // ========== Title ==========
        val periodStr = "${dateFormat.format(Date(startDate))} — ${dateFormat.format(Date(endDate))}"
        canvas.drawText("Отчёт по тренировкам", MARGIN_LEFT, yPos, titlePaint)
        yPos += LINE_HEIGHT + 4f
        canvas.drawText("Период: $periodStr", MARGIN_LEFT, yPos, bodyPaint)
        yPos += LINE_HEIGHT * 2

        // ========== Summary Statistics ==========
        val totalSessions = sortedSessions.size
        val totalSets = sortedSessions.sumOf { it.sets.size }
        val totalVolume = sortedSessions.sumOf { sw -> sw.sets.sumOf { it.weightKg * it.reps } }
        val avgSetsPerSession = if (totalSessions > 0) totalSets.toFloat() / totalSessions else 0f

        canvas.drawText("Сводная статистика", MARGIN_LEFT, yPos, subtitlePaint)
        yPos += LINE_HEIGHT + 2f
        canvas.drawText("Всего тренировок: $totalSessions", MARGIN_LEFT + 16f, yPos, bodyPaint)
        yPos += LINE_HEIGHT
        canvas.drawText("Всего подходов: $totalSets", MARGIN_LEFT + 16f, yPos, bodyPaint)
        yPos += LINE_HEIGHT
        canvas.drawText("Суммарный объём: ${"%.1f".format(totalVolume)} кг", MARGIN_LEFT + 16f, yPos, bodyPaint)
        yPos += LINE_HEIGHT
        canvas.drawText("Среднее кол-во подходов: ${"%.1f".format(avgSetsPerSession)}", MARGIN_LEFT + 16f, yPos, bodyPaint)
        yPos += LINE_HEIGHT * 2

        // ========== Session Details ==========
        canvas.drawText("Детали тренировок", MARGIN_LEFT, yPos, subtitlePaint)
        yPos += LINE_HEIGHT + SECTION_GAP

        for (sw in sortedSessions) {
            val session = sw.session
            val sessionDateStr = dateFormat.format(Date(session.date))
            val setsByExercise = sw.sets.groupBy { it.exerciseId }
            val uniqueExercises = setsByExercise.size
            val sessionVolume = sw.sets.sumOf { it.weightKg * it.reps }

            // Check if session header + at least a few lines fit on current page
            val neededHeight = LINE_HEIGHT * (3 + sw.sets.size + setsByExercise.size)
            if (yPos + neededHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                document.finishPage(currentPage)
                pageNumber++
                currentPage = startNewPage(document, pageNumber)
                canvas = currentPage.canvas
                yPos = MARGIN_TOP
            }

            // Session header
            canvas.drawText(
                "$sessionDateStr — Упражнений: $uniqueExercises, Подходов: ${sw.sets.size}, Объём: ${"%.1f".format(sessionVolume)} кг",
                MARGIN_LEFT,
                yPos,
                subtitlePaint
            )
            yPos += LINE_HEIGHT + 4f

            if (session.notes.isNotBlank()) {
                canvas.drawText("Заметки: ${session.notes}", MARGIN_LEFT + 16f, yPos, smallPaint)
                yPos += LINE_HEIGHT
            }

            // Table header
            val colX = floatArrayOf(MARGIN_LEFT + 16f, MARGIN_LEFT + 200f, MARGIN_LEFT + 290f, MARGIN_LEFT + 370f, MARGIN_LEFT + 440f)
            canvas.drawText("Упражнение", colX[0], yPos, smallPaint)
            canvas.drawText("Подход", colX[1], yPos, smallPaint)
            canvas.drawText("Вес (кг)", colX[2], yPos, smallPaint)
            canvas.drawText("Повт.", colX[3], yPos, smallPaint)
            canvas.drawText("RIR", colX[4], yPos, smallPaint)
            yPos += LINE_HEIGHT

            for ((exerciseId, sets) in setsByExercise) {
                val exerciseName = exercises[exerciseId]?.name ?: "Упражнение #$exerciseId"
                for (set in sets.sortedBy { it.setNumber }) {
                    if (yPos + LINE_HEIGHT > PAGE_HEIGHT - MARGIN_BOTTOM) {
                        document.finishPage(currentPage)
                        pageNumber++
                        currentPage = startNewPage(document, pageNumber)
                        canvas = currentPage.canvas
                        yPos = MARGIN_TOP
                    }

                    canvas.drawText(exerciseName, colX[0], yPos, bodyPaint)
                    canvas.drawText("${set.setNumber}", colX[1], yPos, bodyPaint)
                    canvas.drawText("${"%.1f".format(set.weightKg)}", colX[2], yPos, bodyPaint)
                    canvas.drawText("${set.reps}", colX[3], yPos, bodyPaint)
                    canvas.drawText("${set.rir}", colX[4], yPos, bodyPaint)
                    yPos += LINE_HEIGHT
                }
            }

            yPos += SECTION_GAP
        }

        // Footer with page numbers
        document.finishPage(currentPage)

        document.writeTo(outputStream)
        document.close()
    }

    private fun startNewPage(document: PdfDocument, pageNumber: Int): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        val page = document.startPage(pageInfo)

        // Page number footer
        val footerPaint = Paint().apply {
            textSize = 9f
            color = android.graphics.Color.GRAY
            isAntiAlias = true
        }
        page.canvas.drawText(
            "Стр. $pageNumber",
            (PAGE_WIDTH / 2f) - 15f,
            PAGE_HEIGHT - 20f,
            footerPaint
        )

        return page
    }
}
