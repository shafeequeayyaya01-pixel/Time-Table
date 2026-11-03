package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.example.data.FacultyMember
import com.example.data.PortalRole
import com.example.data.SemesterConfig
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ScholarlyAmber
import com.example.ui.theme.StatusConductedGreen
import com.example.ui.theme.StatusMissedRed
import com.example.ui.theme.WafyEmeraldDeep
import com.example.ui.theme.WafiyyaTealDeep

@Composable
fun SemesterAndFacultyTabScreen(
    selectedCampus: String,
    currentRole: PortalRole,
    semesterConfigs: List<SemesterConfig>,
    facultyMembers: List<FacultyMember>,
    onUpdateSemesterConfig: (SemesterConfig) -> Unit,
    onAddFacultyMember: (
        name: String,
        designation: String,
        primaryCampus: String,
        subjects: String,
        preferredCluster: String,
        maxWeeklyPeriods: Int
    ) -> Unit
) {
    val activeConfig = remember(semesterConfigs, selectedCampus) {
        semesterConfigs.firstOrNull { it.campus == selectedCampus }
            ?: SemesterConfig(
                id = "sem_${selectedCampus.lowercase()}",
                campus = selectedCampus,
                semesterName = "Odd Semester 2026-27 ($selectedCampus)"
            )
    }

    var showEditSemesterDialog by remember { mutableStateOf(false) }
    var showAddFacultyDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Academic Semesters & Working Days Deficit Card
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
                                text = "Academic Semester & Working Days",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = activeConfig.semesterName,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFDE68A)
                            )
                        }
                        if (currentRole == PortalRole.ADMIN) {
                            AssistChip(
                                onClick = { showEditSemesterDialog = true },
                                label = { Text("Adjust Days", color = Color.White) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.EditCalendar,
                                        contentDescription = "Adjust Semester Working Days",
                                        tint = Color(0xFFFDE68A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("edit_semester_button")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Working Days Breakdown Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SemesterMetricTile(
                            label = "Calendar Days",
                            value = "${activeConfig.totalCalendarDays}d",
                            sub = "-${activeConfig.sundaysAndFridaysOff + activeConfig.monthlyAndFestivalHolidays}d holidays",
                            modifier = Modifier.weight(1f)
                        )
                        SemesterMetricTile(
                            label = "Target Days",
                            value = "${activeConfig.targetWorkingDays}d",
                            sub = "Done: ${activeConfig.completedWorkingDays}d",
                            modifier = Modifier.weight(1f)
                        )
                        SemesterMetricTile(
                            label = "Net Available",
                            value = "${activeConfig.netAvailableWorkingDays}d",
                            sub = "+${activeConfig.makeupDaysScheduled}d makeup",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Deficit / Surplus Alert Strip
                    val deficit = activeConfig.workingDayDeficit
                    Surface(
                        color = if (deficit > 0) Color(0xFF7F1D1D) else Color(0xFF065F46),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (deficit > 0) Icons.Default.TrendingDown else Icons.Default.Verified,
                                    contentDescription = "Deficit Status",
                                    tint = Color(0xFFFDE68A),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (deficit > 0) {
                                            "Working Days Deficit: $deficit Days Short"
                                        } else {
                                            "Zero Working Day Deficit (Target Met)"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "സൺഡേ/മാസാന്ത അവധികളും പരീക്ഷാ ദിനങ്ങളും കഴിച്ച് കുറവു വന്ന ദിവസങ്ങൾ (Deficit) മേക്കപ്പ് ക്ലാസുകളിലൂടെ പരിഹരിക്കുന്നു.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    val progress = (activeConfig.completedWorkingDays.toFloat() /
                        activeConfig.targetWorkingDays.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                    Text(
                        text = "Semester Completion Progress: ${(progress * 100).toInt()}% (${activeConfig.completedWorkingDays}/${activeConfig.targetWorkingDays} days)",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        color = Color(0xFFFBBF24),
                        trackColor = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        // Cross-Campus Faculty Clustering Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Faculty & Cross-Campus Travel Clustering",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "രണ്ട് കാമ്പസുകളിലും പഠിപ്പിക്കുന്ന അധ്യാപകരുടെ യാത്രാ സൗകര്യം (Morning Wafy / Afternoon Wafiyya)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (currentRole == PortalRole.ADMIN) {
                    AssistChip(
                        onClick = { showAddFacultyDialog = true },
                        label = { Text("Add Faculty") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Faculty Member",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("add_faculty_button")
                    )
                }
            }
        }

        items(facultyMembers, key = { it.id }) { faculty ->
            FacultyClusterCard(faculty = faculty)
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showEditSemesterDialog) {
        EditSemesterConfigDialog(
            config = activeConfig,
            onDismiss = { showEditSemesterDialog = false },
            onSave = { updated ->
                onUpdateSemesterConfig(updated)
                showEditSemesterDialog = false
            }
        )
    }

    if (showAddFacultyDialog) {
        AddFacultyMemberDialog(
            onDismiss = { showAddFacultyDialog = false },
            onAdd = { name, desig, campus, subjects, cluster, maxP ->
                onAddFacultyMember(name, desig, campus, subjects, cluster, maxP)
                showAddFacultyDialog = false
            }
        )
    }
}

@Composable
private fun SemesterMetricTile(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.13f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp
            )
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFDE68A)
            )
            Text(
                text = sub,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun FacultyClusterCard(faculty: FacultyMember) {
    val isShared = faculty.primaryCampus == "BOTH"
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isShared) ScholarlyAmber.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isShared) Icons.Default.CompareArrows else Icons.Default.Groups,
                    contentDescription = faculty.name,
                    tint = if (isShared) ScholarlyAmber else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = faculty.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Surface(
                        color = if (isShared) ScholarlyAmber.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isShared) "WAFY + WAFIYYA" else faculty.primaryCampus,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isShared) ScholarlyAmber else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = faculty.designation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Subjects: ${faculty.subjects}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cluster: ${faculty.preferredCluster.replace("_", " ")}",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${faculty.assignedWeeklyPeriods}/${faculty.maxWeeklyPeriods} prds/wk",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusConductedGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun EditSemesterConfigDialog(
    config: SemesterConfig,
    onDismiss: () -> Unit,
    onSave: (SemesterConfig) -> Unit
) {
    var totalCalendar by remember { mutableStateOf(config.totalCalendarDays.toString()) }
    var weeklyOffs by remember { mutableStateOf(config.sundaysAndFridaysOff.toString()) }
    var festivalOffs by remember { mutableStateOf(config.monthlyAndFestivalHolidays.toString()) }
    var examDays by remember { mutableStateOf(config.examAndEventDays.toString()) }
    var targetDays by remember { mutableStateOf(config.targetWorkingDays.toString()) }
    var completedDays by remember { mutableStateOf(config.completedWorkingDays.toString()) }
    var makeupDays by remember { mutableStateOf(config.makeupDaysScheduled.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Semester Working Days & Deficit (${config.campus})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = totalCalendar,
                    onValueChange = { totalCalendar = it.filter { c -> c.isDigit() } },
                    label = { Text("Total Calendar Days") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weeklyOffs,
                        onValueChange = { weeklyOffs = it.filter { c -> c.isDigit() } },
                        label = { Text("Sun/Weekly Off") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = festivalOffs,
                        onValueChange = { festivalOffs = it.filter { c -> c.isDigit() } },
                        label = { Text("Monthly/Fest Off") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = examDays,
                        onValueChange = { examDays = it.filter { c -> c.isDigit() } },
                        label = { Text("Exam/Event Days") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = targetDays,
                        onValueChange = { targetDays = it.filter { c -> c.isDigit() } },
                        label = { Text("Target Working") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = completedDays,
                        onValueChange = { completedDays = it.filter { c -> c.isDigit() } },
                        label = { Text("Completed Days") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = makeupDays,
                        onValueChange = { makeupDays = it.filter { c -> c.isDigit() } },
                        label = { Text("Makeup Scheduled") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        config.copy(
                            totalCalendarDays = totalCalendar.toIntOrNull() ?: config.totalCalendarDays,
                            sundaysAndFridaysOff = weeklyOffs.toIntOrNull() ?: config.sundaysAndFridaysOff,
                            monthlyAndFestivalHolidays = festivalOffs.toIntOrNull() ?: config.monthlyAndFestivalHolidays,
                            examAndEventDays = examDays.toIntOrNull() ?: config.examAndEventDays,
                            targetWorkingDays = targetDays.toIntOrNull() ?: config.targetWorkingDays,
                            completedWorkingDays = completedDays.toIntOrNull() ?: config.completedWorkingDays,
                            makeupDaysScheduled = makeupDays.toIntOrNull() ?: config.makeupDaysScheduled
                        )
                    )
                }
            ) {
                Text("Save Calculation")
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
private fun AddFacultyMemberDialog(
    onDismiss: () -> Unit,
    onAdd: (
        name: String,
        designation: String,
        primaryCampus: String,
        subjects: String,
        preferredCluster: String,
        maxWeeklyPeriods: Int
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var designation by remember { mutableStateOf("Senior Lecturer") }
    var primaryCampus by remember { mutableStateOf("BOTH") }
    var subjects by remember { mutableStateOf("") }
    var preferredCluster by remember { mutableStateOf("MORNING_WAFY_AFTERNOON_WAFIYYA") }
    var maxPeriods by remember { mutableStateOf("22") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Faculty & Travel Cluster Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Faculty Name") },
                    placeholder = { Text("e.g., Usthad Jafar Wafy") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("Designation") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("WAFY", "WAFIYYA", "BOTH").forEach { c ->
                        FilterChip(
                            selected = primaryCampus == c,
                            onClick = { primaryCampus = c },
                            label = { Text(c) }
                        )
                    }
                }
                OutlinedTextField(
                    value = subjects,
                    onValueChange = { subjects = it },
                    label = { Text("Assigned Subjects") },
                    placeholder = { Text("e.g., Aqeedah, Tarikh, English") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = maxPeriods,
                    onValueChange = { maxPeriods = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Max Weekly Periods") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && subjects.isNotBlank()) {
                        onAdd(
                            name.trim(),
                            designation.trim(),
                            primaryCampus,
                            subjects.trim(),
                            preferredCluster,
                            maxPeriods.toIntOrNull() ?: 22
                        )
                    }
                }
            ) {
                Text("Save Faculty")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
