package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.PortalRepository
import com.example.data.PortalRole
import com.example.data.TimetableSlot
import com.example.domain.PdfAndExportHelper
import com.example.ui.PortalViewModel
import com.example.ui.UiState
import com.example.ui.screens.ArchitectureReviewModal
import com.example.ui.screens.AuditTabScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.SemesterAndFacultyTabScreen
import com.example.ui.screens.SubmitDailyAuditDialog
import com.example.ui.screens.TimetableTabScreen
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ScholarlyAmber
import com.example.ui.theme.WafyEmeraldDeep
import com.example.ui.theme.WafyPortalTheme
import com.example.ui.theme.WafiyyaTealDeep
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WafyPortalTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = { /* AuthStateListener updates currentUser automatically */ })
    } else {
        MainPortalScreen(currentUserId = user.uid)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPortalScreen(currentUserId: String) {
    val viewModel: PortalViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                PortalViewModel(PortalRepository(db), currentUserId)
            }
        }
    )

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val selectedCampus by viewModel.selectedCampus.collectAsStateWithLifecycle()
    val selectedBatch by viewModel.selectedBatch.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val isOptimizing by viewModel.isOptimizing.collectAsStateWithLifecycle()
    val optimizationSummary by viewModel.lastOptimizationSummary.collectAsStateWithLifecycle()

    val slotsState by viewModel.timetableSlotsState.collectAsStateWithLifecycle()
    val auditsState by viewModel.periodAuditsState.collectAsStateWithLifecycle()
    val semestersState by viewModel.semesterConfigsState.collectAsStateWithLifecycle()
    val facultyState by viewModel.facultyMembersState.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showRoleDialog by remember { mutableStateOf(false) }
    var showArchitectureModal by remember { mutableStateOf(false) }
    var quickAuditSlot by remember { mutableStateOf<TimetableSlot?>(null) }

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    val slots = (slotsState as? UiState.Success)?.data ?: emptyList()
    val audits = (auditsState as? UiState.Success)?.data ?: emptyList()
    val semesters = (semestersState as? UiState.Success)?.data ?: emptyList()
    val faculty = (facultyState as? UiState.Success)?.data ?: emptyList()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (selectedCampus == "WAFY") WafyEmeraldDeep else WafiyyaTealDeep)
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "WAFY & WAFIYYA PORTAL",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (selectedCampus == "WAFY") {
                                    "KKHM Islamic & Arts College (Boys)"
                                } else {
                                    "Al-Gaith Islamic & Arts College (Girls)"
                                },
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.sp,
                                color = Color(0xFFFDE68A)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showArchitectureModal = true },
                            modifier = Modifier.testTag("architecture_review_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Architecture,
                                contentDescription = "Codebase & Architecture Review",
                                tint = Color(0xFFFDE68A)
                            )
                        }
                        IconButton(
                            onClick = { showRoleDialog = true },
                            modifier = Modifier.testTag("switch_role_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Switch Access Role (${currentRole.title})",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = {
                                signOutUser(
                                    credentialManager = credentialManager,
                                    onSignOutComplete = {},
                                    scope = scope
                                )
                            },
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign Out",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )

                // Twin Campus Switcher Pill Row (WAFY vs WAFIYYA)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CampusToggleChip(
                        title = "WAFY (Boys Campus)",
                        subtitle = "KKHM Islamic & Arts",
                        selected = selectedCampus == "WAFY",
                        onClick = { viewModel.selectCampus("WAFY") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("campus_switch_wafy")
                    )
                    CampusToggleChip(
                        title = "WAFIYYA (Girls Campus)",
                        subtitle = "Al-Gaith College",
                        selected = selectedCampus == "WAFIYYA",
                        onClick = { viewModel.selectCampus("WAFIYYA") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("campus_switch_wafiyya")
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Timetable Optimizer"
                        )
                    },
                    label = { Text("Timetable") },
                    modifier = Modifier.testTag("nav_tab_timetable")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.FactCheck,
                            contentDescription = "3:30 PM Daily Audit"
                        )
                    },
                    label = { Text("3:30 PM Audit") },
                    modifier = Modifier.testTag("nav_tab_audit")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Semester & Faculty"
                        )
                    },
                    label = { Text("Semester & Staff") },
                    modifier = Modifier.testTag("nav_tab_semester")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedVisibility(visible = statusBanner != null) {
                statusBanner?.let { msg ->
                    Surface(
                        color = ScholarlyAmber.copy(alpha = 0.16f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.clearStatusBanner() }) {
                                Text("Dismiss", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            if (slotsState is UiState.Loading && slots.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Syncing Wafy & Wafiyya Cloud Timetable...")
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> TimetableTabScreen(
                        selectedCampus = selectedCampus,
                        selectedBatch = selectedBatch,
                        selectedDay = selectedDay,
                        currentRole = currentRole,
                        slots = slots,
                        isOptimizing = isOptimizing,
                        optimizationSummary = optimizationSummary,
                        onSelectBatch = viewModel::selectBatch,
                        onSelectDay = viewModel::selectDay,
                        onRunOptimizer = viewModel::runHighDensityOptimizer,
                        onSaveSlot = viewModel::saveCustomSlot,
                        onQuickAuditSlot = { slot -> quickAuditSlot = slot },
                        onExportPdf = {
                            try {
                                val batchSlots = slots.filter {
                                    it.campus == selectedCampus && it.batchName == selectedBatch
                                }
                                val semCfg = semesters.firstOrNull { it.campus == selectedCampus }
                                val pdfFile = PdfAndExportHelper.generateA4LandscapeTimetablePdf(
                                    context = context,
                                    campus = selectedCampus,
                                    batchName = selectedBatch,
                                    slots = batchSlots,
                                    audits = audits.filter { it.campus == selectedCampus },
                                    semesterConfig = semCfg
                                )
                                Toast.makeText(
                                    context,
                                    "Saved A4 Landscape PDF: ${pdfFile.name}",
                                    Toast.LENGTH_LONG
                                ).show()
                                PdfAndExportHelper.shareTextContent(
                                    context = context,
                                    title = "Wafy & Wafiyya Official Timetable ($selectedCampus - $selectedBatch)",
                                    content = PdfAndExportHelper.buildJsonExport(
                                        campus = selectedCampus,
                                        slots = batchSlots,
                                        audits = audits.filter { it.campus == selectedCampus },
                                        semesterConfig = semCfg
                                    )
                                )
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "PDF generation error: ${e.localizedMessage}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onExportCsv = {
                            val csv = PdfAndExportHelper.buildCsvExport(
                                slots = slots.filter { it.campus == selectedCampus },
                                audits = audits.filter { it.campus == selectedCampus }
                            )
                            PdfAndExportHelper.shareTextContent(
                                context = context,
                                title = "Wafy_Wafiyya_${selectedCampus}_Export.csv",
                                content = csv,
                                mimeType = "text/csv"
                            )
                        }
                    )

                    1 -> AuditTabScreen(
                        selectedCampus = selectedCampus,
                        selectedBatch = selectedBatch,
                        selectedDay = selectedDay,
                        currentRole = currentRole,
                        slots = slots,
                        audits = audits,
                        onSubmitAudit = viewModel::submitDailyAudit,
                        onScheduleCompensation = viewModel::scheduleCompensation
                    )

                    2 -> SemesterAndFacultyTabScreen(
                        selectedCampus = selectedCampus,
                        currentRole = currentRole,
                        semesterConfigs = semesters,
                        facultyMembers = faculty,
                        onUpdateSemesterConfig = viewModel::updateSemesterConfig,
                        onAddFacultyMember = viewModel::addFacultyMember
                    )
                }
            }
        }
    }

    quickAuditSlot?.let { slot ->
        SubmitDailyAuditDialog(
            slot = slot,
            onDismiss = { quickAuditSlot = null },
            onSubmit = { dateStr, status, reason, subFac, leader, compDate ->
                viewModel.submitDailyAudit(slot, dateStr, status, reason, subFac, leader, compDate)
                quickAuditSlot = null
            }
        )
    }

    if (showRoleDialog) {
        RoleSwitcherDialog(
            currentRole = currentRole,
            onDismiss = { showRoleDialog = false },
            onSelectRole = { role ->
                viewModel.switchRole(role)
                showRoleDialog = false
            }
        )
    }

    if (showArchitectureModal) {
        ArchitectureReviewModal(onDismiss = { showArchitectureModal = false })
    }
}

@Composable
private fun CampusToggleChip(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Color.White else Color.White.copy(alpha = 0.14f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) WafyEmeraldDeep else Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = if (selected) WafiyyaTealDeep else Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun RoleSwitcherDialog(
    currentRole: PortalRole,
    onDismiss: () -> Unit,
    onSelectRole: (PortalRole) -> Unit
) {
    var selected by remember { mutableStateOf(currentRole) }
    var passcode by remember { mutableStateOf("3300") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("3-Tier Role-Based Access Control") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select your institutional portal role:",
                    style = MaterialTheme.typography.bodySmall
                )
                PortalRole.entries.forEach { role ->
                    Surface(
                        onClick = { selected = role },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected == role) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = role.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = role.subtitleMalayalam,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (selected == PortalRole.CLASS_LEADER) {
                    OutlinedTextField(
                        value = passcode,
                        onValueChange = { passcode = it },
                        label = { Text("Class Leader 3:30 PM Audit Passcode") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSelectRole(selected) }
            ) {
                Text("Activate Role")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
