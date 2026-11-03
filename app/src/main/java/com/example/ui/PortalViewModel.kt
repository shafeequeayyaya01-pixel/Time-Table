package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FacultyMember
import com.example.data.PeriodAudit
import com.example.data.PortalRepository
import com.example.data.PortalRole
import com.example.data.SemesterConfig
import com.example.data.TimetableSlot
import com.example.domain.TimetableOptimizerEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class PortalViewModel(
    private val repository: PortalRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _selectedCampus = MutableStateFlow("WAFY") // "WAFY" | "WAFIYYA"
    val selectedCampus: StateFlow<String> = _selectedCampus.asStateFlow()

    private val _selectedBatch = MutableStateFlow(TimetableOptimizerEngine.WAFY_BATCHES.first())
    val selectedBatch: StateFlow<String> = _selectedBatch.asStateFlow()

    private val _selectedDay = MutableStateFlow("MON")
    val selectedDay: StateFlow<String> = _selectedDay.asStateFlow()

    private val _currentRole = MutableStateFlow(PortalRole.ADMIN)
    val currentRole: StateFlow<PortalRole> = _currentRole.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _lastOptimizationSummary = MutableStateFlow<TimetableOptimizerEngine.OptimizationSummary?>(null)
    val lastOptimizationSummary: StateFlow<TimetableOptimizerEngine.OptimizationSummary?> = _lastOptimizationSummary.asStateFlow()

    private val _isOptimizing = MutableStateFlow(false)
    val isOptimizing: StateFlow<Boolean> = _isOptimizing.asStateFlow()

    private var shuffleCounter = 0

    // Two-tier Flow error handling per firebase-firestore-android skill
    val timetableSlotsState: StateFlow<UiState<List<TimetableSlot>>> =
        repository.observeTimetableSlots(currentUserId)
            .map<List<TimetableSlot>, UiState<List<TimetableSlot>>> { UiState.Success(it) }
            .catch { error ->
                Log.w(TAG, "Error observing timetable slots", error)
                emit(UiState.Error(error.message ?: "Failed to observe timetable slots"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val periodAuditsState: StateFlow<UiState<List<PeriodAudit>>> =
        repository.observePeriodAudits(currentUserId)
            .map<List<PeriodAudit>, UiState<List<PeriodAudit>>> { UiState.Success(it) }
            .catch { error ->
                Log.w(TAG, "Error observing period audits", error)
                emit(UiState.Error(error.message ?: "Failed to observe period audits"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val semesterConfigsState: StateFlow<UiState<List<SemesterConfig>>> =
        repository.observeSemesterConfigs(currentUserId)
            .map<List<SemesterConfig>, UiState<List<SemesterConfig>>> { UiState.Success(it) }
            .catch { error ->
                Log.w(TAG, "Error observing semester configs", error)
                emit(UiState.Error(error.message ?: "Failed to observe semester configs"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val facultyMembersState: StateFlow<UiState<List<FacultyMember>>> =
        repository.observeFacultyMembers(currentUserId)
            .map<List<FacultyMember>, UiState<List<FacultyMember>>> { UiState.Success(it) }
            .catch { error ->
                Log.w(TAG, "Error observing faculty members", error)
                emit(UiState.Error(error.message ?: "Failed to observe faculty members"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    init {
        seedInitialInstitutionalDataIfEmpty()
    }

    private fun seedInitialInstitutionalDataIfEmpty() {
        viewModelScope.launch {
            try {
                val existingSlots = repository.observeTimetableSlots(currentUserId).first()
                if (existingSlots.isEmpty()) {
                    _isOptimizing.value = true
                    // Seed Faculty Roster
                    TimetableOptimizerEngine.defaultFacultyRoster(currentUserId).forEach {
                        repository.addFacultyMember(it)
                    }
                    // Seed Semester Configs for Wafy & Wafiyya
                    TimetableOptimizerEngine.defaultSemesterConfigs(currentUserId).forEach {
                        repository.saveSemesterConfig(it)
                    }
                    // Seed Initial 3:30 PM Daily Audits
                    TimetableOptimizerEngine.defaultPeriodAudits(currentUserId).forEach {
                        repository.submitPeriodAudit(it)
                    }
                    // Seed Optimized Timetable for Default Wafy & Wafiyya Batches
                    val (wafySlots, summary) = TimetableOptimizerEngine.generateOptimizedTimetable(
                        userId = currentUserId,
                        campus = "WAFY",
                        batchName = TimetableOptimizerEngine.WAFY_BATCHES.first(),
                        shuffleSeed = 0
                    )
                    wafySlots.forEach { repository.createTimetableSlot(it) }

                    val (wafiyyaSlots, _) = TimetableOptimizerEngine.generateOptimizedTimetable(
                        userId = currentUserId,
                        campus = "WAFIYYA",
                        batchName = TimetableOptimizerEngine.WAFIYYA_BATCHES.first(),
                        shuffleSeed = 0
                    )
                    wafiyyaSlots.forEach { repository.createTimetableSlot(it) }

                    _lastOptimizationSummary.value = summary
                    _isOptimizing.value = false
                }
            } catch (e: Exception) {
                Log.w(TAG, "Initial institutional seeding skipped or failed", e)
                _isOptimizing.value = false
            }
        }
    }

    fun selectCampus(campus: String) {
        _selectedCampus.value = campus
        val batches = if (campus == "WAFY") {
            TimetableOptimizerEngine.WAFY_BATCHES
        } else {
            TimetableOptimizerEngine.WAFIYYA_BATCHES
        }
        if (_selectedBatch.value !in batches) {
            _selectedBatch.value = batches.first()
        }
    }

    fun selectBatch(batch: String) {
        _selectedBatch.value = batch
    }

    fun selectDay(day: String) {
        _selectedDay.value = day
    }

    fun switchRole(role: PortalRole) {
        _currentRole.value = role
        _statusBannerMessage.value = "Switched role to ${role.title} (${role.subtitleMalayalam})"
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    fun runHighDensityOptimizer() {
        if (_currentRole.value != PortalRole.ADMIN) {
            _statusBannerMessage.value = "Only Academic Admin can run the High-Density Timetable Optimizer."
            return
        }
        viewModelScope.launch {
            _isOptimizing.value = true
            try {
                shuffleCounter++
                val campus = _selectedCampus.value
                val batch = _selectedBatch.value
                val (generatedSlots, summary) = TimetableOptimizerEngine.generateOptimizedTimetable(
                    userId = currentUserId,
                    campus = campus,
                    batchName = batch,
                    shuffleSeed = shuffleCounter
                )
                generatedSlots.forEach { slot ->
                    val uniqueDocId = "slot_${campus.lowercase()}_${batch.hashCode().toUInt()}_${slot.dayOfWeek}_p${slot.periodNumber}"
                    repository.createTimetableSlot(slot.copy(id = uniqueDocId))
                }
                _lastOptimizationSummary.value = summary
                _statusBannerMessage.value =
                    "Optimized ${summary.slotsGenerated} slots (0 Empty Periods | ${summary.doubleLabBlocksPlaced} Double Labs | ${summary.splitElectiveTriadsPlaced} Split Electives)"
            } catch (e: Exception) {
                _statusBannerMessage.value = "Optimization failed: ${e.localizedMessage}"
            } finally {
                _isOptimizing.value = false
            }
        }
    }

    fun saveCustomSlot(slot: TimetableSlot) {
        viewModelScope.launch {
            val uniqueDocId = slot.id.ifBlank {
                "slot_${slot.campus.lowercase()}_${slot.batchName.hashCode().toUInt()}_${slot.dayOfWeek}_p${slot.periodNumber}"
            }
            val res = repository.createTimetableSlot(slot.copy(id = uniqueDocId))
            if (res.isSuccess) {
                _statusBannerMessage.value = "Saved P${slot.periodNumber} (${slot.subjectName}) to Cloud Firestore"
            } else {
                _statusBannerMessage.value = "Error saving slot: ${res.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun submitDailyAudit(
        slot: TimetableSlot,
        dateString: String,
        status: String,
        missReason: String,
        substituteFaculty: String,
        classLeaderName: String,
        compensationDate: String
    ) {
        if (_currentRole.value == PortalRole.PUBLIC_STUDENT) {
            _statusBannerMessage.value = "Read-Only Mode: Switch to Class Leader or Admin to submit 3:30 PM Audit."
            return
        }
        viewModelScope.launch {
            val auditId = "audit_${slot.campus.lowercase()}_${dateString}_p${slot.periodNumber}"
            val audit = PeriodAudit(
                id = auditId,
                userId = currentUserId,
                slotId = slot.id.ifBlank { "slot_manual" },
                campus = slot.campus,
                batchName = slot.batchName,
                dateString = dateString,
                dayOfWeek = slot.dayOfWeek,
                periodNumber = slot.periodNumber,
                subjectName = slot.subjectName,
                facultyName = slot.facultyName,
                status = status,
                missReason = missReason,
                substituteFaculty = substituteFaculty,
                classLeaderName = classLeaderName.ifBlank { "Class Leader" },
                compensationDate = compensationDate,
                compensationNotes = if (status == "COMPENSATED") "Makeup scheduled" else ""
            )
            val result = repository.submitPeriodAudit(audit)
            if (result.isSuccess) {
                _statusBannerMessage.value = "3:30 PM Audit logged for P${slot.periodNumber}: $status"
            } else {
                _statusBannerMessage.value = "Failed to log audit: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun scheduleCompensation(
        auditId: String,
        compensationDate: String,
        compensationNotes: String
    ) {
        viewModelScope.launch {
            val res = repository.updateAuditCompensation(
                auditId = auditId,
                newStatus = "COMPENSATED",
                compensationDate = compensationDate,
                compensationNotes = compensationNotes
            )
            if (res.isSuccess) {
                _statusBannerMessage.value = "Makeup Class scheduled & marked COMPENSATED ($compensationDate)"
            } else {
                _statusBannerMessage.value = "Failed to update compensation: ${res.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun updateSemesterConfig(config: SemesterConfig) {
        viewModelScope.launch {
            val res = repository.saveSemesterConfig(config)
            if (res.isSuccess) {
                _statusBannerMessage.value = "Updated ${config.campus} Academic Semester Working Days & Deficit"
            } else {
                _statusBannerMessage.value = "Failed to save semester config: ${res.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun addFacultyMember(
        name: String,
        designation: String,
        primaryCampus: String,
        subjects: String,
        preferredCluster: String,
        maxWeeklyPeriods: Int
    ) {
        viewModelScope.launch {
            val res = repository.addFacultyMember(
                FacultyMember(
                    userId = currentUserId,
                    name = name,
                    designation = designation,
                    primaryCampus = primaryCampus,
                    subjects = subjects,
                    preferredCluster = preferredCluster,
                    maxWeeklyPeriods = maxWeeklyPeriods,
                    assignedWeeklyPeriods = (maxWeeklyPeriods * 0.75).toInt()
                )
            )
            if (res.isSuccess) {
                _statusBannerMessage.value = "Added faculty '$name' with cluster rule $preferredCluster"
            } else {
                _statusBannerMessage.value = "Failed to add faculty: ${res.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    private companion object {
        const val TAG = "PortalViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
