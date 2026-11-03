package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
class PortalRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveTimetableSlot_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = PortalRepository(firestore)
        val uniqueId = "slot_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        val slot = TimetableSlot(
            id = uniqueId,
            userId = uid,
            campus = "WAFY",
            batchName = "Aliya Ula (Deg 1)",
            dayOfWeek = "MON",
            periodNumber = 1,
            subjectName = "Fiqh (Fathhul Mueen)",
            subjectNameArabic = "فتح المعين",
            facultyId = "fac_1",
            facultyName = "Usthad Abdul Hakeem Wafy",
            slotType = "ISLAMIC",
            isDoublePeriod = false,
            isCrossCampusFaculty = true,
            electiveSubBatches = "",
            roomName = "Hall 101"
        )

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createTimetableSlot(slot) }
        assertTrue(createResult.isSuccess)
        val createdId = createResult.getOrThrow()
        assertEquals(uniqueId, createdId)

        val emittedSlots = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTimetableSlots(uid).first { list -> list.any { it.id == uniqueId } }
        }
        assertTrue(emittedSlots.any { it.id == uniqueId && it.subjectName == "Fiqh (Fathhul Mueen)" })
    }

    @Test
    fun submitAndCompensatePeriodAudit_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = PortalRepository(firestore)
        val auditId = "audit_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        val audit = PeriodAudit(
            id = auditId,
            userId = uid,
            slotId = "slot_1",
            campus = "WAFIYYA",
            batchName = "Tamheediyya Ula",
            dateString = "2026-11-03",
            dayOfWeek = "TUE",
            periodNumber = 3,
            subjectName = "Nahw & Sarf",
            facultyName = "Usthada Fathima Wafiyya",
            status = "MISSED",
            missReason = "University Exam Invigilation",
            substituteFaculty = "",
            classLeaderName = "Ayisha Noura (Class Leader)",
            compensationDate = "",
            compensationNotes = ""
        )

        val submitResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.submitPeriodAudit(audit) }
        assertTrue(submitResult.isSuccess)

        val compResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateAuditCompensation(
                auditId = auditId,
                newStatus = "COMPENSATED",
                compensationDate = "2026-11-07 (Sat P6)",
                compensationNotes = "Conducted extra session"
            )
        }
        assertTrue(compResult.isSuccess)

        val emittedAudits = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePeriodAudits(uid).first { list ->
                list.any { it.id == auditId && it.status == "COMPENSATED" }
            }
        }
        assertTrue(emittedAudits.any { it.id == auditId && it.compensationDate == "2026-11-07 (Sat P6)" })
    }

    @Test
    fun getTimetableSlotById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = PortalRepository(firestore)
        val uniqueId = "slot_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createTimetableSlot(
                TimetableSlot(
                    id = uniqueId,
                    userId = aliceUid,
                    campus = "WAFY",
                    batchName = "Aliya Ula",
                    dayOfWeek = "MON",
                    periodNumber = 2,
                    subjectName = "Tafsir",
                    facultyId = "fac_2",
                    facultyName = "Usthad Salman Wafy",
                    slotType = "ISLAMIC",
                    roomName = "Hall 102"
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = PortalRepository(firestore)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getTimetableSlotById(uniqueId, targetUserId = aliceUid).getOrThrow()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException but got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun observeTimetableSlots_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = PortalRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeTimetableSlots("unauth_user_123").first()
            }
            fail("Expected Exception wrapping PERMISSION_DENIED")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@wafyportal.com"
        const val BOB_EMAIL = "bob@wafyportal.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
