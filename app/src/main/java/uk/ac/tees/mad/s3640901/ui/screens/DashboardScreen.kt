@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package uk.ac.tees.mad.s3640901.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.tees.mad.s3640901.data.DoseStatus
import uk.ac.tees.mad.s3640901.data.MedicationDao
import uk.ac.tees.mad.s3640901.data.MedicationDoseOutcomeEntity
import uk.ac.tees.mad.s3640901.data.MedicationEntity
import uk.ac.tees.mad.s3640901.notifications.MedicationOverdueProcessor
import uk.ac.tees.mad.s3640901.notifications.MedicationReminderScheduler
import uk.ac.tees.mad.s3640901.notifications.MedicationScheduleCalculator
import uk.ac.tees.mad.s3640901.ui.components.MedicationDoseCard
import uk.ac.tees.mad.s3640901.ui.components.MissedMedicationCard
import uk.ac.tees.mad.s3640901.ui.theme.MedMinderTheme
import java.io.File

private data class UpcomingDose(
    val medication: MedicationEntity,
    val scheduledAt: Long,
    val outcome: MedicationDoseOutcomeEntity?
)

@Composable
fun DashboardScreen(
    medicationDao: MedicationDao,
    onAddMedication: () -> Unit,
    onOpenHistory: () -> Unit,
    onEditMedication: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val medications by medicationDao
        .observeActiveMedications()
        .collectAsState(initial = emptyList())

    val outcomes by medicationDao
        .observeDoseOutcomes()
        .collectAsState(initial = emptyList())

    var medicationToDeactivate by remember {
        mutableStateOf<MedicationEntity?>(null)
    }

    var medicationToDelete by remember {
        mutableStateOf<MedicationEntity?>(null)
    }

    LaunchedEffect(medications) {
        withContext(Dispatchers.IO) {
            MedicationOverdueProcessor.process(
                context = context,
                medicationDao = medicationDao,
                medications = medications
            )
        }
    }

    val medicationById = remember(medications) {
        medications.associateBy { medication ->
            medication.id
        }
    }

    val missedOutcomes = remember(
        medications,
        outcomes
    ) {
        outcomes
            .filter { outcome ->
                outcome.status == DoseStatus.MISSED &&
                        outcome.dismissedAt == null
            }
            .mapNotNull { outcome ->
                medicationById[outcome.medicationId]
                    ?.let { medication ->
                        medication to outcome
                    }
            }
            .sortedByDescending { pair ->
                pair.second.scheduledAt
            }
    }

    val upcomingDoses = remember(
        medications,
        outcomes
    ) {
        medications
            .mapNotNull { medication ->
                val nextDose =
                    MedicationScheduleCalculator.nextDoseTime(
                        medication = medication
                    ) ?: return@mapNotNull null

                val existingOutcome = outcomes.firstOrNull { outcome ->
                    outcome.medicationId == medication.id &&
                            outcome.scheduledAt == nextDose
                }

                UpcomingDose(
                    medication = medication,
                    scheduledAt = nextDose,
                    outcome = existingOutcome
                )
            }
            .filter { dose ->
                dose.outcome == null
            }
            .sortedBy { dose ->
                dose.scheduledAt
            }
    }

    fun saveDoseStatus(
        medication: MedicationEntity,
        scheduledAt: Long,
        status: String
    ) {
        scope.launch {
            withContext(Dispatchers.IO) {
                medicationDao.saveDoseOutcome(
                    MedicationDoseOutcomeEntity(
                        medicationId = medication.id,
                        scheduledAt = scheduledAt,
                        status = status,
                        recordedAt = System.currentTimeMillis()
                    )
                )

                MedicationReminderScheduler.scheduleNextForMedication(
                    context = context,
                    medication = medication,
                    afterMillis = scheduledAt + 1L
                )
            }
        }
    }

    fun dismissMissedDose(
        outcome: MedicationDoseOutcomeEntity
    ) {
        scope.launch {
            withContext(Dispatchers.IO) {
                medicationDao.dismissDoseOutcome(
                    medicationId = outcome.medicationId,
                    scheduledAt = outcome.scheduledAt,
                    dismissedAt = System.currentTimeMillis()
                )
            }
        }
    }

    fun deactivateMedication(
        medication: MedicationEntity
    ) {
        scope.launch {
            withContext(Dispatchers.IO) {
                MedicationReminderScheduler.cancelAllForMedication(
                    context = context,
                    medication = medication
                )

                medicationDao.deactivateMedication(
                    medicationId = medication.id
                )
            }
        }
    }

    fun deleteMedication(
        medication: MedicationEntity
    ) {
        scope.launch {
            withContext(Dispatchers.IO) {
                MedicationReminderScheduler.cancelAllForMedication(
                    context = context,
                    medication = medication
                )

                medicationDao.deleteDoseOutcomesForMedication(
                    medicationId = medication.id
                )

                medicationDao.deleteMedication(medication)

                medication.photoPath
                    ?.takeIf { it.isNotBlank() }
                    ?.let { photoPath ->
                        File(photoPath).delete()
                    }
            }
        }
    }

    fun shareMedication(
        medication: MedicationEntity
    ) {
        val shareText = buildString {
            appendLine("Medication: ${medication.name}")
            appendLine("Dosage: ${medication.dosage}")

            if (medication.instructions.isNotBlank()) {
                appendLine(
                    "Instructions: ${medication.instructions}"
                )
            }

            appendLine(
                "Schedule: ${medication.scheduleType}"
            )
            appendLine(
                "Details: ${medication.scheduleValue}"
            )
        }

        val shareIntent = Intent(
            Intent.ACTION_SEND
        ).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        runCatching {
            context.startActivity(
                Intent.createChooser(
                    shareIntent,
                    "Share medication"
                )
            )
        }
    }

    DashboardContent(
        medications = medications,
        missedOutcomes = missedOutcomes,
        upcomingDoses = upcomingDoses,
        onAddMedication = onAddMedication,
        onOpenHistory = onOpenHistory,
        onEditMedication = onEditMedication,
        onRequestDeactivate = {
            medicationToDeactivate = it
        },
        onRequestDelete = {
            medicationToDelete = it
        },
        onStatusSelected = { medication, scheduledAt, status ->
            saveDoseStatus(
                medication = medication,
                scheduledAt = scheduledAt,
                status = status
            )
        },
        onDismissMissedDose = {
            dismissMissedDose(it)
        },
        onShareMedication = {
            shareMedication(it)
        }
    )

    medicationToDeactivate?.let { medication ->
        AlertDialog(
            onDismissRequest = {
                medicationToDeactivate = null
            },
            title = {
                Text("Deactivate medication?")
            },
            text = {
                Text(
                    "${medication.name} will remain in History, " +
                            "but future reminders will stop."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deactivateMedication(medication)
                        medicationToDeactivate = null
                    }
                ) {
                    Text("Deactivate")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        medicationToDeactivate = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    medicationToDelete?.let { medication ->
        AlertDialog(
            onDismissRequest = {
                medicationToDelete = null
            },
            title = {
                Text("Delete medication?")
            },
            text = {
                Text(
                    "This permanently deletes ${medication.name}, " +
                            "its photo, history and reminders."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteMedication(medication)
                        medicationToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        medicationToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DashboardContent(
    medications: List<MedicationEntity>,
    missedOutcomes: List<Pair<MedicationEntity, MedicationDoseOutcomeEntity>>,
    upcomingDoses: List<UpcomingDose>,
    onAddMedication: () -> Unit,
    onOpenHistory: () -> Unit,
    onEditMedication: (Long) -> Unit,
    onRequestDeactivate: (MedicationEntity) -> Unit,
    onRequestDelete: (MedicationEntity) -> Unit,
    onStatusSelected: (
        MedicationEntity,
        Long,
        String
    ) -> Unit,
    onDismissMissedDose: (
        MedicationDoseOutcomeEntity
    ) -> Unit,
    onShareMedication: (MedicationEntity) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Medication Dashboard")
                }
            )
        }
    ) { paddingValues ->

        if (medications.isEmpty()) {
            EmptyDashboardContent(
                paddingValues = paddingValues,
                onAddMedication = onAddMedication,
                onOpenHistory = onOpenHistory
            )

            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (missedOutcomes.isNotEmpty()) {
                item {
                    Text(
                        text = "Missed Medications",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(
                    items = missedOutcomes,
                    key = { pair ->
                        "${pair.second.medicationId}_" +
                                pair.second.scheduledAt
                    }
                ) { pair ->
                    MissedMedicationCard(
                        medication = pair.first,
                        outcome = pair.second,
                        onDismiss = {
                            onDismissMissedDose(pair.second)
                        },
                        onEdit = {
                            onEditMedication(pair.first.id)
                        },
                        onDeactivate = {
                            onRequestDeactivate(pair.first)
                        },
                        onDelete = {
                            onRequestDelete(pair.first)
                        },
                        onShare = {
                            onShareMedication(pair.first)
                        }
                    )
                }
            }

            item {
                Text(
                    text = "Upcoming Medications",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (upcomingDoses.isEmpty()) {
                item {
                    Text("No upcoming medication doses.")
                }
            } else {
                items(
                    items = upcomingDoses,
                    key = { dose ->
                        "${dose.medication.id}_${dose.scheduledAt}"
                    }
                ) { dose ->
                    MedicationDoseCard(
                        medication = dose.medication,
                        scheduledAt = dose.scheduledAt,
                        currentStatus = dose.outcome?.status,
                        onStatusSelected = { status ->
                            onStatusSelected(
                                dose.medication,
                                dose.scheduledAt,
                                status
                            )
                        },
                        onEdit = {
                            onEditMedication(dose.medication.id)
                        },
                        onDeactivate = {
                            onRequestDeactivate(dose.medication)
                        },
                        onDelete = {
                            onRequestDelete(dose.medication)
                        },
                        onShare = {
                            onShareMedication(dose.medication)
                        }
                    )
                }
            }

            item {
                Button(
                    onClick = onAddMedication,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Another Medication")
                }
            }

            item {
                OutlinedButton(
                    onClick = onOpenHistory,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("My History")
                }
            }
        }
    }
}

@Composable
private fun EmptyDashboardContent(
    paddingValues: PaddingValues,
    onAddMedication: () -> Unit,
    onOpenHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No active medication",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Add a medication to begin managing your schedule.",
            modifier = Modifier.padding(top = 12.dp)
        )

        Button(
            onClick = onAddMedication,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text("Add Medication")
        }

        OutlinedButton(
            onClick = onOpenHistory,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text("My History")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    MedMinderTheme {
        EmptyDashboardContent(
            paddingValues = PaddingValues(),
            onAddMedication = {},
            onOpenHistory = {}
        )
    }
}