package uk.ac.tees.mad.s3640901.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.ac.tees.mad.s3640901.data.DoseStatus
import uk.ac.tees.mad.s3640901.data.MedicationDoseOutcomeEntity
import uk.ac.tees.mad.s3640901.data.MedicationEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MedicationDoseCard(
    medication: MedicationEntity,
    scheduledAt: Long,
    currentStatus: String?,
    onStatusSelected: (String) -> Unit,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                MedicationThumbnail(
                    photoPath = medication.photoPath,
                    modifier = Modifier
                        .width(72.dp)
                        .height(72.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = medication.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text("Dosage: ${medication.dosage}")

                    if (medication.instructions.isNotBlank()) {
                        Text("Instructions: ${medication.instructions}")
                    }

                    Text("Schedule: ${medication.scheduleType}")
                    Text(medication.scheduleValue)

                    Text(
                        "Next dose: ${formatDoseTime(scheduledAt)}"
                    )

                    Text(
                        text = "Status: ${formatStatus(currentStatus)}",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                MedicationActionMenu(
                    onEdit = onEdit,
                    onDeactivate = onDeactivate,
                    onDelete = onDelete,
                    onShare = onShare
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onStatusSelected(DoseStatus.TAKEN)
                    }
                ) {
                    Text("Taken")
                }

                Button(
                    onClick = {
                        onStatusSelected(DoseStatus.SKIPPED)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("Skipped")
                }

                Button(
                    onClick = {
                        onStatusSelected(DoseStatus.MISSED)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Missed")
                }
            }
        }
    }
}

@Composable
fun MissedMedicationCard(
    medication: MedicationEntity,
    outcome: MedicationDoseOutcomeEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = medication.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text("Dosage: ${medication.dosage}")

                    Text(
                        "Scheduled: ${
                            formatDoseTime(outcome.scheduledAt)
                        }"
                    )

                    Text(
                        text = "Status: Missed",
                        fontWeight = FontWeight.Bold
                    )
                }

                MedicationActionMenu(
                    onEdit = onEdit,
                    onDeactivate = onDeactivate,
                    onDelete = onDelete,
                    onShare = onShare
                )
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
private fun MedicationActionMenu(
    onEdit: () -> Unit,
    onDeactivate: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box {
        Button(
            onClick = {
                expanded = true
            },
            contentPadding = ButtonDefaults.ContentPadding
        ) {
            Text(
                text = "⋮",
                fontSize = 22.sp
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            DropdownMenuItem(
                text = {
                    Text("Edit")
                },
                onClick = {
                    expanded = false
                    onEdit()
                }
            )

            DropdownMenuItem(
                text = {
                    Text("Share")
                },
                onClick = {
                    expanded = false
                    onShare()
                }
            )

            DropdownMenuItem(
                text = {
                    Text("Deactivate")
                },
                onClick = {
                    expanded = false
                    onDeactivate()
                }
            )

            DropdownMenuItem(
                text = {
                    Text("Delete")
                },
                onClick = {
                    expanded = false
                    onDelete()
                }
            )
        }
    }
}

@Composable
private fun MedicationThumbnail(
    photoPath: String?,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(photoPath) {
        photoPath
            ?.takeIf { it.isNotBlank() }
            ?.let {
                BitmapFactory.decodeFile(it)
            }
            ?.asImageBitmap()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Medication photo",
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(
                RoundedCornerShape(10.dp)
            )
        )
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No photo",
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

fun formatDoseTime(timeMillis: Long): String {
    return SimpleDateFormat(
        "EEE, dd MMM yyyy HH:mm",
        Locale.getDefault()
    ).format(Date(timeMillis))
}

fun formatStatus(status: String?): String {
    return status
        ?.lowercase(Locale.getDefault())
        ?.replaceFirstChar {
            it.titlecase(Locale.getDefault())
        }
        ?: "Pending"
}