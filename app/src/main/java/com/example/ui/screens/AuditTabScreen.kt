package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PeriodAudit
import com.example.data.PortalRole
import com.example.data.TimetableSlot
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ScholarlyAmber
import com.example.ui.theme.StatusCompensatedBg
import com.example.ui.theme.StatusCompensatedBlue
import com.example.ui.theme.StatusConductedBg
import com.example.ui.theme.StatusConductedGreen
import com.example.ui.theme.StatusMissedBg
import com.example.ui.theme.StatusMissedRed
import com.example.ui.theme.StatusSubstitutedAmber
import com.example.ui.theme.StatusSubstitutedBg
import com.example.ui.theme.WafyEmeraldDeep

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuditTabScreen(
    selectedCampus: String,
    selectedBatch: String,
    selectedDay: String,
    currentRole: PortalRole,
    slots: List<TimetableSlot>,
    audits: List<PeriodAudit>,
    onSubmitAudit: (
        slot: TimetableSlot,
        dateString: String,
        status: String,
        missReason: String,
        substituteFaculty: String,
        classLeaderName: String,
        compensationDate: String
    ) -> Unit,
    onScheduleCompensation: (
        auditId: String,
        compensationDate: String,
        compensationNotes: String
    ) -> Unit
) {
    var statusFilter by remember { mutableStateOf("ALL") } // "ALL", "MISSED", "SUBSTITUTED", "COMPENSATED", "CONDUCTED"
    var slotToAudit by remember { mutableStateOf<TimetableSlot?>(null) }
    var auditToCompensate by remember { mutableStateOf<PeriodAudit?>(null) }

    val campusAudits = remember(audits, selectedCampus, statusFilter) {
        audits.filter {
            it.campus == selectedCampus && (statusFilter == "ALL" || it.status == statusFilter)
        }
    }

    val todaySlots = remember(slots, selectedCampus, selectedBatch, selectedDay) {
        slots.filter {
            it.campus == selectedCampus && it.batchName == selectedBatch && it.dayOfWeek == selectedDay
        }.sortedBy { it.periodNumber }
    }

    val conductedCount = audits.count { it.campus == selectedCampus && it.status == "CONDUCTED" }
    val missedCount = audits.count { it.campus == selectedCampus && it.status == "MISSED" }
    val substitutedCount = audits.count { it.campus == selectedCampus && it.status == "SUBSTITUTED" }
    val compensatedCount = audits.count { it.campus == selectedCampus && it.status == "COMPENSATED" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // 3:30 PM Daily Period Audit Header Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = WafyEmeraldDeep),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTimeFilled,
                                contentDescription = "3:30 PM Audit",
                                tint = Color(0xFFFDE68A),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "3:30 PM Daily Period Audit",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }
                        Surface(
                            color = ScholarlyAmber,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = currentRole.title,
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "എല്ലാ ദിവസവും വൈകുന്നേരം 3:30-ന് ക്ലാസ് ലീഡർമാർക്ക് നടന്ന പീരിയഡുകളും നഷ്ടപ്പെട്ടവയുടെ കാരണവും മേക്കപ്പ് ക്ലാസുകളും രേഖപ്പെടുത്താം.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Audit Summary Counters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AuditStatBox(
                            label = "Conducted",
                            count = conductedCount,
                            color = Color(0xFF4ADE80),
                            modifier = Modifier.weight(1f)
                        )
                        AuditStatBox(
                            label = "Missed",
                            count = missedCount,
                            color = Color(0xFFF87171),
                            modifier = Modifier.weight(1f)
                        )
                        AuditStatBox(
                            label = "Substituted",
                            count = substitutedCount,
                            color = Color(0xFFFBBF24),
                            modifier = Modifier.weight(1f)
                        )
                        AuditStatBox(
                            label = "Compensated",
                            count = compensatedCount,
                            color = Color(0xFF60A5FA),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Quick 3:30 PM Class Leader Checklist for Today's Batch
        if (currentRole != PortalRole.PUBLIC_STUDENT && todaySlots.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Quick 3:30 PM Audit • $selectedBatch ($selectedDay)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tap any period below to record Conducted, Missed, or Substituted status",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            todaySlots.forEach { slot ->
                                OutlinedButton(
                                    onClick = { slotToAudit = slot },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("quick_audit_p${slot.periodNumber}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddTask,
                                        contentDescription = "Audit P${slot.periodNumber}",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("P${slot.periodNumber}: ${slot.subjectName.take(14)}")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Filter Chips for Audit Log & Missed Period Tracker
        item {
            Column {
                Text(
                    text = "Missed Periods Tracker & Audit Log ($selectedCampus)",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ALL", "MISSED", "SUBSTITUTED", "COMPENSATED", "CONDUCTED").forEach { st ->
                        FilterChip(
                            selected = statusFilter == st,
                            onClick = { statusFilter = st },
                            label = { Text(st) },
                            modifier = Modifier.testTag("audit_filter_$st")
                        )
                    }
                }
            }
        }

        if (campusAudits.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No audit records matching filter '$statusFilter' for $selectedCampus.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(campusAudits, key = { it.id }) { audit ->
                PeriodAuditCard(
                    audit = audit,
                    canCompensate = currentRole != PortalRole.PUBLIC_STUDENT &&
                        (audit.status == "MISSED" || audit.status == "SUBSTITUTED"),
                    onCompensateClicked = { auditToCompensate = audit }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    slotToAudit?.let { slot ->
        SubmitDailyAuditDialog(
            slot = slot,
            onDismiss = { slotToAudit = null },
            onSubmit = { dateStr, status, reason, subFac, leader, compDate ->
                onSubmitAudit(slot, dateStr, status, reason, subFac, leader, compDate)
                slotToAudit = null
            }
        )
    }

    auditToCompensate?.let { audit ->
        ScheduleCompensationDialog(
            audit = audit,
            onDismiss = { auditToCompensate = null },
            onConfirm = { compDate, notes ->
                onScheduleCompensation(audit.id, compDate, notes)
                auditToCompensate = null
            }
        )
    }
}

@Composable
private fun AuditStatBox(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Text(
                text = count.toString(),
                fontFamily = JetBrainsMonoFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun PeriodAuditCard(
    audit: PeriodAudit,
    canCompensate: Boolean,
    onCompensateClicked: () -> Unit
) {
    val (badgeBg, badgeFg, statusIcon) = when (audit.status) {
        "CONDUCTED" -> Triple(StatusConductedBg, StatusConductedGreen, Icons.Default.CheckCircle)
        "MISSED" -> Triple(StatusMissedBg, StatusMissedRed, Icons.Default.ErrorOutline)
        "SUBSTITUTED" -> Triple(StatusSubstitutedBg, StatusSubstitutedAmber, Icons.Default.SwapHoriz)
        else -> Triple(StatusCompensatedBg, StatusCompensatedBlue, Icons.Default.EventRepeat)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("audit_card_${audit.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(badgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = audit.status,
                            tint = badgeFg,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "P${audit.periodNumber} • ${audit.subjectName}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${audit.batchName} • ${audit.dateString} (${audit.dayOfWeek})",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = audit.status,
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Faculty: ${audit.facultyName} • Logged by: ${audit.classLeaderName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (audit.missReason.isNotBlank()) {
                Text(
                    text = "Reason: ${audit.missReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusMissedRed,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (audit.substituteFaculty.isNotBlank()) {
                Text(
                    text = "Substitute Faculty: ${audit.substituteFaculty}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusSubstitutedAmber,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (audit.compensationDate.isNotBlank()) {
                Text(
                    text = "Compensation Slot: ${audit.compensationDate} (${audit.compensationNotes})",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusCompensatedBlue,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (canCompensate) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onCompensateClicked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusCompensatedBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("compensate_button_${audit.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.EventRepeat,
                        contentDescription = "Schedule Compensation Class",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Schedule Makeup / Compensation Class")
                }
            }
        }
    }
}

@Composable
fun SubmitDailyAuditDialog(
    slot: TimetableSlot,
    onDismiss: () -> Unit,
    onSubmit: (
        dateString: String,
        status: String,
        missReason: String,
        substituteFaculty: String,
        classLeaderName: String,
        compensationDate: String
    ) -> Unit
) {
    var dateString by remember { mutableStateOf("2026-11-03") }
    var status by remember { mutableStateOf("CONDUCTED") }
    var missReason by remember { mutableStateOf("") }
    var substituteFaculty by remember { mutableStateOf("") }
    var classLeaderName by remember {
        mutableStateOf(
            if (slot.campus == "WAFY") "Muhammed Sinan (Class Leader)" else "Ayisha Noura (Class Leader)"
        )
    }
    var compensationDate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("3:30 PM Audit • P${slot.periodNumber} ${slot.subjectName}")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Batch: ${slot.batchName} | Faculty: ${slot.facultyName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("CONDUCTED", "MISSED", "SUBSTITUTED").forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = dateString,
                    onValueChange = { dateString = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = classLeaderName,
                    onValueChange = { classLeaderName = it },
                    label = { Text("Class Leader Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (status != "CONDUCTED") {
                    OutlinedTextField(
                        value = missReason,
                        onValueChange = { missReason = it },
                        label = { Text("Reason for Missed / Substituted Period") },
                        placeholder = { Text("e.g., University Duty / Cross-Campus Delay") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (status == "SUBSTITUTED") {
                    OutlinedTextField(
                        value = substituteFaculty,
                        onValueChange = { substituteFaculty = it },
                        label = { Text("Substitute Faculty Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (status == "MISSED") {
                    OutlinedTextField(
                        value = compensationDate,
                        onValueChange = { compensationDate = it },
                        label = { Text("Proposed Makeup Date/Slot (Optional)") },
                        placeholder = { Text("e.g., Sat Special P6") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        dateString.trim().ifBlank { "2026-11-03" },
                        status,
                        missReason.trim(),
                        substituteFaculty.trim(),
                        classLeaderName.trim().ifBlank { "Class Leader" },
                        compensationDate.trim()
                    )
                },
                modifier = Modifier.testTag("confirm_submit_audit_button")
            ) {
                Text("Submit 3:30 PM Audit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ScheduleCompensationDialog(
    audit: PeriodAudit,
    onDismiss: () -> Unit,
    onConfirm: (compensationDate: String, compensationNotes: String) -> Unit
) {
    var compDate by remember {
        mutableStateOf(audit.compensationDate.ifBlank { "2026-11-07 (Sat Special P5)" })
    }
    var compNotes by remember {
        mutableStateOf("Extra makeup session arranged with ${audit.facultyName}")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Schedule Makeup Class • P${audit.periodNumber}")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${audit.subjectName} (${audit.batchName})",
                    style = MaterialTheme.typography.titleSmall
                )
                OutlinedTextField(
                    value = compDate,
                    onValueChange = { compDate = it },
                    label = { Text("Makeup Date & Period Slot") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = compNotes,
                    onValueChange = { compNotes = it },
                    label = { Text("Compensation Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (compDate.isNotBlank()) {
                        onConfirm(compDate.trim(), compNotes.trim())
                    }
                }
            ) {
                Text("Mark Compensated")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
