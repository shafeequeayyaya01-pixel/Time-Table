package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PortalRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- 1. TIMETABLE SLOTS ---

    fun observeTimetableSlots(userId: String = auth.currentUser?.uid ?: "unauthenticated"): Flow<List<TimetableSlot>> {
        val path = "users/$userId/timetable_slots"
        return db.collection("users").document(userId).collection("timetable_slots")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(TimetableSlot::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedWith(compareBy({ it.dayOfWeekOrder() }, { it.periodNumber }))
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun createTimetableSlot(slot: TimetableSlot): Result<String> {
        val uid = requireUserId()
        val slotId = slot.id.ifBlank { "slot_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
        val docRef = db.collection("users").document(uid).collection("timetable_slots").document(slotId)
        val payload = mapOf(
            "userId" to uid,
            "campus" to slot.campus,
            "batchName" to slot.batchName.take(80),
            "dayOfWeek" to slot.dayOfWeek,
            "periodNumber" to slot.periodNumber.coerceIn(1, 7),
            "subjectName" to slot.subjectName.take(120),
            "subjectNameArabic" to slot.subjectNameArabic.take(120),
            "facultyId" to slot.facultyId.ifBlank { "fac_default" }.take(80),
            "facultyName" to slot.facultyName.take(100),
            "slotType" to slot.slotType,
            "isDoublePeriod" to slot.isDoublePeriod,
            "isCrossCampusFaculty" to slot.isCrossCampusFaculty,
            "electiveSubBatches" to slot.electiveSubBatches.take(250),
            "roomName" to slot.roomName.ifBlank { "Hall 101" }.take(60),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(slotId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getTimetableSlotById(slotId: String, targetUserId: String = requireUserId()): Result<TimetableSlot?> {
        val docRef = db.collection("users").document(targetUserId).collection("timetable_slots").document(slotId)
        return try {
            val snap = docRef.get().await()
            Result.success(snap.toObject(TimetableSlot::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun deleteTimetableSlot(slotId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("timetable_slots").document(slotId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- 2. 3:30 PM DAILY PERIOD AUDITS & COMPENSATION ---

    fun observePeriodAudits(userId: String = auth.currentUser?.uid ?: "unauthenticated"): Flow<List<PeriodAudit>> {
        val path = "users/$userId/period_audits"
        return db.collection("users").document(userId).collection("period_audits")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(PeriodAudit::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedWith(compareByDescending<PeriodAudit> { it.dateString }.thenBy { it.periodNumber })
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun submitPeriodAudit(audit: PeriodAudit): Result<String> {
        val uid = requireUserId()
        val auditId = audit.id.ifBlank { "audit_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
        val docRef = db.collection("users").document(uid).collection("period_audits").document(auditId)
        val payload = mapOf(
            "userId" to uid,
            "slotId" to audit.slotId.ifBlank { "slot_manual" }.take(128),
            "campus" to audit.campus,
            "batchName" to audit.batchName.take(80),
            "dateString" to audit.dateString.take(20),
            "dayOfWeek" to audit.dayOfWeek,
            "periodNumber" to audit.periodNumber.coerceIn(1, 7),
            "subjectName" to audit.subjectName.take(120),
            "facultyName" to audit.facultyName.take(100),
            "status" to audit.status,
            "missReason" to audit.missReason.take(200),
            "substituteFaculty" to audit.substituteFaculty.take(100),
            "classLeaderName" to audit.classLeaderName.ifBlank { "Class Leader" }.take(100),
            "compensationDate" to audit.compensationDate.take(60),
            "compensationNotes" to audit.compensationNotes.take(200),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(auditId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateAuditCompensation(
        auditId: String,
        newStatus: String,
        compensationDate: String,
        compensationNotes: String
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("period_audits").document(auditId)
        val updates = mapOf(
            "status" to newStatus,
            "compensationDate" to compensationDate.take(60),
            "compensationNotes" to compensationNotes.take(200),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- 3. ACADEMIC SEMESTERS & WORKING DAYS DEFICIT ---

    fun observeSemesterConfigs(userId: String = auth.currentUser?.uid ?: "unauthenticated"): Flow<List<SemesterConfig>> {
        val path = "users/$userId/semester_configs"
        return db.collection("users").document(userId).collection("semester_configs")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(SemesterConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun saveSemesterConfig(config: SemesterConfig): Result<String> {
        val uid = requireUserId()
        val configId = config.id.ifBlank { "sem_${config.campus.lowercase()}" }
        val docRef = db.collection("users").document(uid).collection("semester_configs").document(configId)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updates = mapOf(
                    "semesterName" to config.semesterName.take(100),
                    "totalCalendarDays" to config.totalCalendarDays.coerceIn(1, 366),
                    "sundaysAndFridaysOff" to config.sundaysAndFridaysOff.coerceIn(0, 200),
                    "monthlyAndFestivalHolidays" to config.monthlyAndFestivalHolidays.coerceIn(0, 200),
                    "examAndEventDays" to config.examAndEventDays.coerceIn(0, 200),
                    "targetWorkingDays" to config.targetWorkingDays.coerceIn(1, 300),
                    "completedWorkingDays" to config.completedWorkingDays.coerceIn(0, 300),
                    "makeupDaysScheduled" to config.makeupDaysScheduled.coerceIn(0, 100),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "campus" to config.campus,
                    "semesterName" to config.semesterName.take(100),
                    "totalCalendarDays" to config.totalCalendarDays.coerceIn(1, 366),
                    "sundaysAndFridaysOff" to config.sundaysAndFridaysOff.coerceIn(0, 200),
                    "monthlyAndFestivalHolidays" to config.monthlyAndFestivalHolidays.coerceIn(0, 200),
                    "examAndEventDays" to config.examAndEventDays.coerceIn(0, 200),
                    "targetWorkingDays" to config.targetWorkingDays.coerceIn(1, 300),
                    "completedWorkingDays" to config.completedWorkingDays.coerceIn(0, 300),
                    "makeupDaysScheduled" to config.makeupDaysScheduled.coerceIn(0, 100),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(configId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // --- 4. FACULTY ROSTER & CROSS-CAMPUS CLUSTERING ---

    fun observeFacultyMembers(userId: String = auth.currentUser?.uid ?: "unauthenticated"): Flow<List<FacultyMember>> {
        val path = "users/$userId/faculty_members"
        return db.collection("users").document(userId).collection("faculty_members")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FacultyMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun addFacultyMember(faculty: FacultyMember): Result<String> {
        val uid = requireUserId()
        val facId = faculty.id.ifBlank { "fac_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
        val docRef = db.collection("users").document(uid).collection("faculty_members").document(facId)
        val payload = mapOf(
            "userId" to uid,
            "name" to faculty.name.take(100),
            "designation" to faculty.designation.take(100),
            "primaryCampus" to faculty.primaryCampus,
            "subjects" to faculty.subjects.take(200),
            "preferredCluster" to faculty.preferredCluster,
            "maxWeeklyPeriods" to faculty.maxWeeklyPeriods.coerceIn(1, 42),
            "assignedWeeklyPeriods" to faculty.assignedWeeklyPeriods.coerceIn(0, 42),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(facId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }
}

private fun TimetableSlot.dayOfWeekOrder(): Int = when (dayOfWeek) {
    "MON" -> 1
    "TUE" -> 2
    "WED" -> 3
    "THU" -> 4
    "FRI" -> 5
    "SAT" -> 6
    else -> 7
}
