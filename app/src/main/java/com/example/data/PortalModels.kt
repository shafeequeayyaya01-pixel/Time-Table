package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class TimetableSlot(
    @DocumentId val id: String = "",
    val userId: String = "",
    val campus: String = "WAFY", // "WAFY" | "WAFIYYA"
    val batchName: String = "",
    val dayOfWeek: String = "MON", // "MON", "TUE", "WED", "THU", "FRI", "SAT"
    val periodNumber: Int = 1, // 1..7
    val subjectName: String = "",
    val subjectNameArabic: String = "",
    val facultyId: String = "",
    val facultyName: String = "",
    val slotType: String = "ISLAMIC", // "ISLAMIC", "ARTS_DEGREE", "SCIENCE_LAB", "SPLIT_ELECTIVE", "PG_SEMINAR"
    @get:PropertyName("isDoublePeriod") @set:PropertyName("isDoublePeriod")
    var isDoublePeriod: Boolean = false,
    @get:PropertyName("isCrossCampusFaculty") @set:PropertyName("isCrossCampusFaculty")
    var isCrossCampusFaculty: Boolean = false,
    val electiveSubBatches: String = "",
    val roomName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PeriodAudit(
    @DocumentId val id: String = "",
    val userId: String = "",
    val slotId: String = "",
    val campus: String = "WAFY",
    val batchName: String = "",
    val dateString: String = "",
    val dayOfWeek: String = "MON",
    val periodNumber: Int = 1,
    val subjectName: String = "",
    val facultyName: String = "",
    val status: String = "CONDUCTED", // "CONDUCTED", "MISSED", "SUBSTITUTED", "COMPENSATED"
    val missReason: String = "",
    val substituteFaculty: String = "",
    val classLeaderName: String = "",
    val compensationDate: String = "",
    val compensationNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SemesterConfig(
    @DocumentId val id: String = "",
    val userId: String = "",
    val campus: String = "WAFY",
    val semesterName: String = "",
    val totalCalendarDays: Int = 180,
    val sundaysAndFridaysOff: Int = 52,
    val monthlyAndFestivalHolidays: Int = 14,
    val examAndEventDays: Int = 12,
    val targetWorkingDays: Int = 105,
    val completedWorkingDays: Int = 68,
    val makeupDaysScheduled: Int = 4,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val netAvailableWorkingDays: Int
        get() = (totalCalendarDays - sundaysAndFridaysOff - monthlyAndFestivalHolidays - examAndEventDays + makeupDaysScheduled).coerceAtLeast(0)

    val workingDayDeficit: Int
        get() = (targetWorkingDays - netAvailableWorkingDays).coerceAtLeast(0)

    val workingDaySurplus: Int
        get() = (netAvailableWorkingDays - targetWorkingDays).coerceAtLeast(0)
}

@IgnoreExtraProperties
data class FacultyMember(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val designation: String = "",
    val primaryCampus: String = "BOTH", // "WAFY", "WAFIYYA", "BOTH"
    val subjects: String = "",
    val preferredCluster: String = "MORNING_WAFY_AFTERNOON_WAFIYYA", // "MORNING_WAFY_AFTERNOON_WAFIYYA", "ALT_DAYS", "FULL_DAY_SINGLE_CAMPUS"
    val maxWeeklyPeriods: Int = 24,
    val assignedWeeklyPeriods: Int = 18,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class PortalRole(val title: String, val subtitleMalayalam: String) {
    ADMIN("Academic Admin", "അഡ്മിൻ (മുഴുവൻ നിയന്ത്രണം)"),
    CLASS_LEADER("Class Leader", "ക്ലാസ് ലീഡർ (3:30 PM ഡെയ്ലി ഓഡിറ്റ്)"),
    PUBLIC_STUDENT("Student / Public", "വിദ്യാർത്ഥികൾ (റീഡ്-ഓൺലി സുതാര്യത)")
}
