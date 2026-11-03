package com.example.domain

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.data.PeriodAudit
import com.example.data.SemesterConfig
import com.example.data.TimetableSlot
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Generates high-resolution A4 Landscape PDF timetables & audit reports,
 * plus JSON and CSV data exports for backup and institutional sharing.
 */
object PdfAndExportHelper {

    fun generateA4LandscapeTimetablePdf(
        context: Context,
        campus: String,
        batchName: String,
        slots: List<TimetableSlot>,
        audits: List<PeriodAudit>,
        semesterConfig: SemesterConfig?
    ): File {
        val pdfDocument = PdfDocument()
        // A4 Landscape in points (842 x 595)
        val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val headerPaint = Paint().apply {
            color = Color.parseColor("#064E3B")
            style = Paint.Style.FILL
        }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 16f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val subTitlePaint = Paint().apply {
            color = Color.parseColor("#FEF3C7")
            textSize = 10f
            isAntiAlias = true
        }
        val gridPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val cellHeaderBg = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            style = Paint.Style.FILL
        }
        val cellTextBold = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 8.5f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val cellTextMuted = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 7.5f
            isAntiAlias = true
        }
        val badgeTextPaint = Paint().apply {
            color = Color.parseColor("#B45309")
            textSize = 7f
            isFakeBoldText = true
            isAntiAlias = true
        }

        // Top Institutional Banner
        canvas.drawRect(24f, 20f, 818f, 78f, headerPaint)
        val collegeTitle = if (campus == "WAFY") {
            "KKHM ISLAMIC & ARTS COLLEGE (WAFY - BOYS CAMPUS)"
        } else {
            "AL-GAITH ISLAMIC & ARTS COLLEGE FOR GIRLS (WAFIYYA)"
        }
        canvas.drawText(collegeTitle, 36f, 44f, titlePaint)
        val semSummary = semesterConfig?.let {
            "Target Days: ${it.targetWorkingDays} | Net Working Days: ${it.netAvailableWorkingDays} | Deficit: ${it.workingDayDeficit}d"
        } ?: "Zero-Clash Co-ordinated Academic Timetable"
        canvas.drawText(
            "Batch: $batchName   |   $semSummary   |   3:30 PM Daily Audit Logged: ${audits.size}",
            36f,
            64f,
            subTitlePaint
        )

        // Draw Timetable Grid (6 Days x 7 Periods)
        val startX = 24f
        val startY = 88f
        val dayColWidth = 58f
        val periodColWidth = (818f - 24f - dayColWidth) / 7f
        val headerRowHeight = 28f
        val dayRowHeight = (545f - startY - headerRowHeight) / 6f

        // Header Row
        canvas.drawRect(startX, startY, 818f, startY + headerRowHeight, cellHeaderBg)
        canvas.drawRect(startX, startY, 818f, startY + headerRowHeight, gridPaint)
        canvas.drawText("DAY / PRD", startX + 6f, startY + 18f, cellTextBold)

        for (p in 1..7) {
            val colLeft = startX + dayColWidth + (p - 1) * periodColWidth
            canvas.drawLine(colLeft, startY, colLeft, 545f, gridPaint)
            val timing = TimetableOptimizerEngine.PERIOD_TIMINGS[p] ?: ""
            canvas.drawText("P$p ($timing)", colLeft + 5f, startY + 18f, cellTextBold)
        }

        // Day Rows
        val days = TimetableOptimizerEngine.DAYS
        for ((dIdx, day) in days.withIndex()) {
            val rowTop = startY + headerRowHeight + dIdx * dayRowHeight
            val rowBottom = rowTop + dayRowHeight
            canvas.drawRect(startX, rowTop, 818f, rowBottom, gridPaint)
            canvas.drawText(day, startX + 12f, rowTop + 34f, cellTextBold)

            for (p in 1..7) {
                val colLeft = startX + dayColWidth + (p - 1) * periodColWidth
                val slot = slots.firstOrNull { it.dayOfWeek == day && it.periodNumber == p }
                if (slot != null) {
                    val subjShort = slot.subjectName.take(22)
                    val facShort = slot.facultyName.take(22)
                    val tag = when {
                        slot.isDoublePeriod -> "[DOUBLE LAB]"
                        slot.slotType == "SPLIT_ELECTIVE" -> "[3-BATCH ELECTIVE]"
                        slot.isCrossCampusFaculty -> "[CLUSTERED]"
                        slot.slotType == "PG_SEMINAR" -> "[PG MON-WED]"
                        else -> slot.roomName.take(16)
                    }
                    canvas.drawText(subjShort, colLeft + 4f, rowTop + 18f, cellTextBold)
                    canvas.drawText(facShort, colLeft + 4f, rowTop + 32f, cellTextMuted)
                    canvas.drawText(tag, colLeft + 4f, rowTop + 46f, badgeTextPaint)
                }
            }
        }

        // Footer
        canvas.drawText(
            "Generated by WAFY & WAFIYYA Co-ordinated Academic Timetable & Audit Portal (A4 Landscape Official Copy)",
            24f,
            572f,
            cellTextMuted
        )

        pdfDocument.finishPage(page)
        val outFile = File(context.cacheDir, "Wafy_Wafiyya_Timetable_${campus}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { pdfDocument.writeTo(it) }
        pdfDocument.close()
        return outFile
    }

    fun buildCsvExport(slots: List<TimetableSlot>, audits: List<PeriodAudit>): String {
        val sb = StringBuilder()
        sb.appendLine("TYPE,CAMPUS,BATCH,DAY,PERIOD,SUBJECT,FACULTY,SLOT_TYPE,DOUBLE_LAB,CROSS_CAMPUS,STATUS_OR_ROOM,NOTES")
        slots.forEach { s ->
            sb.appendLine(
                "TIMETABLE,${s.campus},\"${s.batchName}\",${s.dayOfWeek},P${s.periodNumber},\"${s.subjectName}\",\"${s.facultyName}\",${s.slotType},${s.isDoublePeriod},${s.isCrossCampusFaculty},\"${s.roomName}\",\"${s.electiveSubBatches}\""
            )
        }
        audits.forEach { a ->
            sb.appendLine(
                "AUDIT_330PM,${a.campus},\"${a.batchName}\",${a.dayOfWeek},P${a.periodNumber},\"${a.subjectName}\",\"${a.facultyName}\",${a.dateString},false,false,${a.status},\"Reason: ${a.missReason} | Makeup: ${a.compensationDate}\""
            )
        }
        return sb.toString()
    }

    fun buildJsonExport(
        campus: String,
        slots: List<TimetableSlot>,
        audits: List<PeriodAudit>,
        semesterConfig: SemesterConfig?
    ): String {
        val root = JSONObject()
        root.put("portal", "WAFY & WAFIYYA Co-ordinated Academic Timetable & Audit Portal")
        root.put("activeCampus", campus)
        semesterConfig?.let {
            val semObj = JSONObject().apply {
                put("semesterName", it.semesterName)
                put("targetWorkingDays", it.targetWorkingDays)
                put("netAvailableWorkingDays", it.netAvailableWorkingDays)
                put("workingDayDeficit", it.workingDayDeficit)
                put("completedWorkingDays", it.completedWorkingDays)
                put("makeupDaysScheduled", it.makeupDaysScheduled)
            }
            root.put("semesterAnalysis", semObj)
        }
        val slotsArr = JSONArray()
        slots.forEach { s ->
            slotsArr.put(
                JSONObject().apply {
                    put("campus", s.campus)
                    put("batchName", s.batchName)
                    put("dayOfWeek", s.dayOfWeek)
                    put("periodNumber", s.periodNumber)
                    put("subjectName", s.subjectName)
                    put("subjectNameArabic", s.subjectNameArabic)
                    put("facultyName", s.facultyName)
                    put("slotType", s.slotType)
                    put("isDoublePeriod", s.isDoublePeriod)
                    put("isCrossCampusFaculty", s.isCrossCampusFaculty)
                    put("electiveSubBatches", s.electiveSubBatches)
                    put("roomName", s.roomName)
                }
            )
        }
        root.put("timetableSlots", slotsArr)

        val auditsArr = JSONArray()
        audits.forEach { a ->
            auditsArr.put(
                JSONObject().apply {
                    put("dateString", a.dateString)
                    put("campus", a.campus)
                    put("batchName", a.batchName)
                    put("periodNumber", a.periodNumber)
                    put("subjectName", a.subjectName)
                    put("status", a.status)
                    put("missReason", a.missReason)
                    put("classLeaderName", a.classLeaderName)
                    put("compensationDate", a.compensationDate)
                }
            )
        }
        root.put("dailyAudits", auditsArr)
        return root.toString(2)
    }

    fun shareTextContent(context: Context, title: String, content: String, mimeType: String = "text/plain") {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(sendIntent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
