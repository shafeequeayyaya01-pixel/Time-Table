package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.PortalRole
import com.example.data.TimetableSlot
import com.example.domain.TimetableOptimizerEngine
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ScholarlyAmber
import com.example.ui.theme.SlotArtsIndigo
import com.example.ui.theme.SlotElectiveTeal
import com.example.ui.theme.SlotIslamicEmerald
import com.example.ui.theme.SlotLabPurple
import com.example.ui.theme.SlotPgAmber
import com.example.ui.theme.WafyEmeraldDeep
import com.example.ui.theme.WafiyyaTealDeep

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimetableTabScreen(
    selectedCampus: String,
    selectedBatch: String,
    selectedDay: String,
    currentRole: PortalRole,
    slots: List<TimetableSlot>,
    isOptimizing: Boolean,
    optimizationSummary: TimetableOptimizerEngine.OptimizationSummary?,
    onSelectBatch: (String) -> Unit,
    onSelectDay: (String) -> Unit,
    onRunOptimizer: () -> Unit,
    onSaveSlot: (TimetableSlot) -> Unit,
    onQuickAuditSlot: (TimetableSlot) -> Unit,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit
) {
    val batches = if (selectedCampus == "WAFY") {
        TimetableOptimizerEngine.WAFY_BATCHES
    } else {
        TimetableOptimizerEngine.WAFIYYA_BATCHES
    }

    val filteredSlots = remember(slots, selectedCampus, selectedBatch, selectedDay) {
        slots.filter {
            it.campus == selectedCampus &&
                it.batchName == selectedBatch &&
                it.dayOfWeek == selectedDay
        }.sortedBy { it.periodNumber }
    }

    val campusBatchSlots = remember(slots, selectedCampus, selectedBatch) {
        slots.filter { it.campus == selectedCampus && it.batchName == selectedBatch }
    }

    var editingSlot by remember { mutableStateOf<TimetableSlot?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // High-Density Optimizer Control Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedCampus == "WAFY") WafyEmeraldDeep else WafiyyaTealDeep
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedCampus == "WAFY") {
                                    "KKHM ISLAMIC & ARTS COLLEGE (WAFY)"
                                } else {
                                    "AL-GAITH COLLEGE FOR GIRLS (WAFIYYA)"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFFFDE68A)
                            )
                            Text(
                                text = "High-Density Timetable Optimizer",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "0 Empty Guaranteed",
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "അധ്യാപകരുടെ ക്ലസ്റ്ററിംഗ് (Wafy Morning / Wafiyya Afternoon), സയൻസ് ഡബിൾ ലാബുകൾ (P3-P4 / P5-P6), 3-ബാച്ച് ഇലക്ടീവുകൾ, പി.ജി (Mon-Wed) എന്നിവ യാതൊരു ക്ലാഷുമില്ലാതെ ക്രമീകരിക്കുന്നു.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Metrics Strip
                    val crossCount = campusBatchSlots.count { it.isCrossCampusFaculty }
                    val doubleCount = campusBatchSlots.count { it.isDoublePeriod } / 2
                    val electiveCount = campusBatchSlots.count { it.slotType == "SPLIT_ELECTIVE" }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ConstraintStatPill("Weekly Slots", "${campusBatchSlots.size}/42")
                        ConstraintStatPill("Clustered Faculty", "$crossCount slots")
                        ConstraintStatPill("Double Labs", "$doubleCount blocks")
                        ConstraintStatPill("3-Way Electives", "$electiveCount triads")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onRunOptimizer,
                            enabled = !isOptimizing && currentRole == PortalRole.ADMIN,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ScholarlyAmber,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("run_optimizer_button")
                        ) {
                            if (isOptimizing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Optimizing...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = "Auto-Optimize Timetable",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Auto-Optimize Batch")
                            }
                        }

                        IconButton(
                            onClick = onExportPdf,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.16f))
                                .testTag("export_pdf_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Export A4 Landscape PDF",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = onExportCsv,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.16f))
                                .testTag("export_csv_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share CSV/JSON Backup",
                                tint = Color.White
                            )
                        }
                    }

                    AnimatedVisibility(visible = optimizationSummary != null) {
                        optimizationSummary?.let { sum ->
                            Text(
                                text = "✓ Multi-pass stochastic solver converged in ${sum.passesExecuted} passes • Zero clashes across Wafy & Wafiyya.",
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.sp,
                                color = Color(0xFFA7F3D0),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Batch Selector Row
        item {
            Column {
                Text(
                    text = "Select Academic Batch / Class",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    batches.forEach { batch ->
                        val isSelected = batch == selectedBatch
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectBatch(batch) },
                            label = {
                                Text(
                                    text = batch,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("batch_chip_${batch.take(8)}")
                        )
                    }
                }
            }
        }

        // Day Selector Strip (MON..SAT)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TimetableOptimizerEngine.DAYS.forEach { day ->
                    val isSelected = day == selectedDay
                    val isPgCoreDay = selectedBatch.contains("PG") && day in listOf("MON", "TUE", "WED")
                    Surface(
                        onClick = { onSelectDay(day) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("day_tab_$day")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            if (isPgCoreDay) {
                                Text(
                                    text = "PG CORE",
                                    fontSize = 8.sp,
                                    fontFamily = JetBrainsMonoFamily,
                                    color = if (isSelected) Color(0xFFFDE68A) else ScholarlyAmber
                                )
                            }
                        }
                    }
                }
            }
        }

        // Daily Period Header + Custom Slot Add Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$selectedDay Schedule • 7 High-Density Periods",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap any period to log 3:30 PM Daily Audit or edit slot",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (currentRole == PortalRole.ADMIN) {
                    AssistChip(
                        onClick = {
                            editingSlot = TimetableSlot(
                                campus = selectedCampus,
                                batchName = selectedBatch,
                                dayOfWeek = selectedDay,
                                periodNumber = 1,
                                subjectName = "Fiqh: Fathhul Mueen",
                                subjectNameArabic = "فتح المعين",
                                facultyId = "fac_hakeem",
                                facultyName = "Usthad Abdul Hakeem Wafy",
                                slotType = "ISLAMIC",
                                roomName = "Hall 101"
                            )
                        },
                        label = { Text("Custom Slot") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Custom Slot",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("add_custom_slot_button")
                    )
                }
            }
        }

        if (filteredSlots.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No slots generated for $selectedBatch on $selectedDay yet.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap 'Auto-Optimize Batch' above to populate all 42 weekly periods with zero empty gaps.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (currentRole == PortalRole.ADMIN) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onRunOptimizer) {
                                Text("Generate Zero-Clash Timetable Now")
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredSlots, key = { "${it.id}_${it.periodNumber}" }) { slot ->
                TimetablePeriodCard(
                    slot = slot,
                    canEdit = currentRole == PortalRole.ADMIN,
                    canAudit = currentRole != PortalRole.PUBLIC_STUDENT,
                    onEditClicked = { editingSlot = slot },
                    onAuditClicked = { onQuickAuditSlot(slot) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    editingSlot?.let { slotToEdit ->
        SlotEditorDialog(
            initialSlot = slotToEdit,
            onDismiss = { editingSlot = null },
            onConfirm = { updated ->
                onSaveSlot(updated)
                editingSlot = null
            }
        )
    }
}

@Composable
private fun ConstraintStatPill(label: String, value: String) {
    Surface(
        color = Color.White.copy(alpha = 0.13f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFDE68A)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimetablePeriodCard(
    slot: TimetableSlot,
    canEdit: Boolean,
    canAudit: Boolean,
    onEditClicked: () -> Unit,
    onAuditClicked: () -> Unit
) {
    val accentColor = when (slot.slotType) {
        "SCIENCE_LAB" -> SlotLabPurple
        "SPLIT_ELECTIVE" -> SlotElectiveTeal
        "PG_SEMINAR" -> SlotPgAmber
        "ARTS_DEGREE" -> SlotArtsIndigo
        else -> SlotIslamicEmerald
    }

    val timing = TimetableOptimizerEngine.PERIOD_TIMINGS[slot.periodNumber] ?: "08:30 - 09:20"

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (slot.isDoublePeriod) 1.5.dp else 1.dp,
                color = if (slot.isDoublePeriod) accentColor.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { if (canAudit) onAuditClicked() }
            .testTag("period_card_p${slot.periodNumber}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Period Index & Time Column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .padding(vertical = 10.dp, horizontal = 6.dp)
            ) {
                Text(
                    text = "P${slot.periodNumber}",
                    style = MaterialTheme.typography.titleLarge,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = timing.substringBefore(" -"),
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 10.sp,
                    color = accentColor
                )
                Text(
                    text = timing.substringAfter("- "),
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Subject, Arabic Kitab Name, Faculty & Badges
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = slot.subjectName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (slot.subjectNameArabic.isNotBlank()) {
                            Text(
                                text = slot.subjectNameArabic,
                                fontFamily = AmiriFontFamily,
                                fontSize = 16.sp,
                                color = accentColor,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                    if (canEdit) {
                        IconButton(
                            onClick = onEditClicked,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Period P${slot.periodNumber}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${slot.facultyName} • ${slot.roomName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (slot.electiveSubBatches.isNotBlank()) {
                    Surface(
                        color = accentColor.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Text(
                            text = slot.electiveSubBatches,
                            style = MaterialTheme.typography.bodySmall,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SlotTagBadge(text = slot.slotType.replace("_", " "), color = accentColor)

                    if (slot.isDoublePeriod) {
                        SlotTagBadge(
                            text = "CONSECUTIVE DOUBLE LAB",
                            color = SlotLabPurple
                        )
                    }
                    if (slot.isCrossCampusFaculty) {
                        SlotTagBadge(
                            text = "CROSS-CAMPUS CLUSTERED",
                            color = ScholarlyAmber
                        )
                    }
                    if (canAudit) {
                        Surface(
                            onClick = onAuditClicked,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FactCheck,
                                    contentDescription = "Audit Period",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "3:30 PM Audit",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotTagBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun SlotEditorDialog(
    initialSlot: TimetableSlot,
    onDismiss: () -> Unit,
    onConfirm: (TimetableSlot) -> Unit
) {
    var periodNumber by remember { mutableIntStateOf(initialSlot.periodNumber) }
    var subjectName by remember { mutableStateOf(initialSlot.subjectName) }
    var subjectNameArabic by remember { mutableStateOf(initialSlot.subjectNameArabic) }
    var facultyName by remember { mutableStateOf(initialSlot.facultyName) }
    var roomName by remember { mutableStateOf(initialSlot.roomName) }
    var slotType by remember { mutableStateOf(initialSlot.slotType) }
    var isDoublePeriod by remember { mutableStateOf(initialSlot.isDoublePeriod) }
    var isCrossCampus by remember { mutableStateOf(initialSlot.isCrossCampusFaculty) }
    var electiveSubBatches by remember { mutableStateOf(initialSlot.electiveSubBatches) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Configure Period Slot (${initialSlot.campus} • ${initialSlot.dayOfWeek})")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (1..7).forEach { p ->
                        FilterChip(
                            selected = periodNumber == p,
                            onClick = { periodNumber = p },
                            label = { Text("P$p") }
                        )
                    }
                }

                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text("Subject Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subjectNameArabic,
                    onValueChange = { subjectNameArabic = it },
                    label = { Text("Arabic Kitab Title (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = facultyName,
                    onValueChange = { facultyName = it },
                    label = { Text("Faculty Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("Hall / Lab Room") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = electiveSubBatches,
                    onValueChange = { electiveSubBatches = it },
                    label = { Text("Split-Batch Electives / Cluster Note") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Science Double Period", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isDoublePeriod, onCheckedChange = {
                        isDoublePeriod = it
                        if (it) slotType = "SCIENCE_LAB"
                    })
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cross-Campus Clustered Faculty", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isCrossCampus, onCheckedChange = { isCrossCampus = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subjectName.isNotBlank() && facultyName.isNotBlank()) {
                        onConfirm(
                            initialSlot.copy(
                                periodNumber = periodNumber,
                                subjectName = subjectName.trim(),
                                subjectNameArabic = subjectNameArabic.trim(),
                                facultyName = facultyName.trim(),
                                roomName = roomName.trim().ifBlank { "Hall 101" },
                                slotType = slotType,
                                isDoublePeriod = isDoublePeriod,
                                isCrossCampusFaculty = isCrossCampus,
                                electiveSubBatches = electiveSubBatches.trim()
                            )
                        )
                    }
                }
            ) {
                Text("Save to Cloud")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
