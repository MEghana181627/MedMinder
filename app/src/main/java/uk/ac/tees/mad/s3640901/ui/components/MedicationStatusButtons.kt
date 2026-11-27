package uk.ac.tees.mad.s3640901.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uk.ac.tees.mad.s3640901.data.DoseStatus
import uk.ac.tees.mad.s3640901.ui.theme.MedMinderTheme

@Composable
fun MedicationStatusButtons(
    onStatusSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(
            rememberScrollState()
        ),
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

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun MedicationStatusButtonsPreview() {
    MedMinderTheme {
        MedicationStatusButtons(
            onStatusSelected = {}
        )
    }
}