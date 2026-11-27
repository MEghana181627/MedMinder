@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package uk.ac.tees.mad.s3640901.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uk.ac.tees.mad.s3640901.data.DoseStatus
import uk.ac.tees.mad.s3640901.data.MedicationDao
import uk.ac.tees.mad.s3640901.data.MedicationDoseOutcomeEntity
import uk.ac.tees.mad.s3640901.data.MedicationEntity
import uk.ac.tees.mad.s3640901.ui.components.formatDoseTime
import uk.ac.tees.mad.s3640901.ui.components.formatStatus
import uk.ac.tees.mad.s3640901.ui.theme.MedMinderTheme

@Composable
fun HistoryScreen(
    medicationDao: MedicationDao,
    onBack: () -> Unit
) {
    val medications by medicationDao
        .observeAllMedications()
        .collectAsState(initial = emptyList())

    val outcomes by medicationDao
        .observeDoseOutcomes()
        .collectAsState(initial = emptyList())

    val medicationById = medications.associateBy {
        it.id
    }

    val history = outcomes
        .mapNotNull { outcome ->
            medicationById[outcome.medicationId]?.let { medication ->
                medication to outcome
            }
        }
        .sortedByDescending {
            it.second.scheduledAt
        }

    HistoryContent(
        history = history,
        onBack = onBack
    )
}

@Composable
private fun HistoryContent(
    history: List<Pair<MedicationEntity, MedicationDoseOutcomeEntity>>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("My History")
                }
            )
        }
    ) { paddingValues ->

        if (history.isEmpty()) {
            EmptyHistoryContent(
                paddingValues = paddingValues,
                onBack = onBack
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(
                    items = history,
                    key = {
                        "${it.second.medicationId}_${it.second.scheduledAt}"
                    }
                ) { pair ->
                    HistoryCard(
                        medication = pair.first,
                        outcome = pair.second
                    )
                }

                item {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back to Dashboard")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryContent(
    paddingValues: PaddingValues,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("No medication history yet.")

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Text("Back to Dashboard")
        }
    }
}

@Composable
private fun HistoryCard(
    medication: MedicationEntity,
    outcome: MedicationDoseOutcomeEntity
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = medication.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text("Dosage: ${medication.dosage}")
            Text("Schedule: ${medication.scheduleType}")

            Text(
                text = "Scheduled: ${
                    formatDoseTime(outcome.scheduledAt)
                }"
            )

            Text(
                text = "Status: ${formatStatus(outcome.status)}",
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Recorded: ${
                    formatDoseTime(outcome.recordedAt)
                }"
            )

            outcome.dismissedAt?.let { dismissedAt ->
                Text(
                    text = "Dismissed: ${
                        formatDoseTime(dismissedAt)
                    }"
                )
            }

            if (!medication.isActive) {
                Text(
                    text = "Medication inactive",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    val medication = MedicationEntity(
        id = 1L,
        name = "Paracetamol",
        dosage = "500 mg",
        instructions = "Take after food",
        scheduleType = "Daily",
        scheduleValue = "Every day at 09:00",
        isActive = false,
        photoPath = null
    )

    val outcome = MedicationDoseOutcomeEntity(
        medicationId = 1L,
        scheduledAt = System.currentTimeMillis(),
        status = DoseStatus.TAKEN,
        recordedAt = System.currentTimeMillis()
    )

    MedMinderTheme {
        HistoryContent(
            history = listOf(medication to outcome),
            onBack = {}
        )
    }
}