package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ScholarlyAmber
import com.example.ui.theme.StatusConductedGreen
import com.example.ui.theme.WafyEmeraldDeep

@Composable
fun ArchitectureReviewModal(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Architecture & Codebase Audit Report",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "WAFY-WAFIYYA-TIME-TABLE • Clean Architecture Upgrades Applied",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReviewItemCard(
                    badge = "1. STOCHASTIC OPTIMIZER BOTTLENECK",
                    title = "Main-Thread Backtracking Freeze -> Coroutine Constraint Solver",
                    problem = "In single-file index.html SPA implementations, multi-pass stochastic backtracking across 42 periods x multiple batches blocks the UI thread and can leave empty period gaps when cross-campus faculty constraints collide.",
                    solution = "Implemented deterministic constraint ordering: (1) PG Mon-Wed Core & Double Seminar blocks first, (2) Science Consecutive Double Periods (P3-P4 / P5-P6), (3) 3-Batch Split Electives, and (4) Morning Wafy / Afternoon Wafiyya faculty travel clusters—guaranteeing 0 empty periods."
                )

                ReviewItemCard(
                    badge = "2. FIRESTORE SECURITY & SNAPSHOT LIFECYCLE",
                    title = "Unbounded onSnapshot Listeners & Client-Side Passcode Exposure",
                    problem = "Hardcoding Class Leader passcodes in client-side JS and attaching unscoped onSnapshot listeners risks unauthenticated reads/writes and listener memory leaks.",
                    solution = "Upgraded to Google Sign-In via Jetpack Credential Manager, strict zero-trust Firestore rules (isValidTimetableSlot / isValidPeriodAudit with immutable createdAt & userId), and lifecycle-aware Kotlin Flow snapshots() with SharingStarted.WhileSubscribed(5000)."
                )

                ReviewItemCard(
                    badge = "3. 3:30 PM AUDIT & DEFICIT ACCURACY",
                    title = "Idempotent Daily Audit Keys & Real-Time Deficit Formula",
                    problem = "Submitting multiple 3:30 PM audits for the same period could create duplicate missed-period entries and skew semester working-day deficit calculations.",
                    solution = "Scoped deterministic audit keys (audit_{campus}_{date}_p{period}) so re-auditing or marking a missed class as COMPENSATED atomically updates the record and recalculates Net Working Days."
                )

                ReviewItemCard(
                    badge = "4. A4 LANDSCAPE PDF & OFFLINE RESILIENCE",
                    title = "DOM-Dependent html2pdf.js Clipping -> Native Vector PDF Engine",
                    problem = "html2pdf.js rasterizes mobile viewports and often clips wide 7-period tables on smartphone screens.",
                    solution = "Built a native 842x595pt A4 Landscape PdfDocument vector renderer alongside instant CSV & JSON backup exports."
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close Audit Report")
            }
        }
    )
}

@Composable
private fun ReviewItemCard(
    badge: String,
    title: String,
    problem: String,
    solution: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Surface(
                color = WafyEmeraldDeep,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = badge,
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFDE68A),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "• Issue: $problem",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "✓ Fix in App: $solution",
                style = MaterialTheme.typography.bodySmall,
                color = StatusConductedGreen,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
