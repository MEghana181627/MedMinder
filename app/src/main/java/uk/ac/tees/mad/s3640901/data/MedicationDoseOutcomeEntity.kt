package uk.ac.tees.mad.s3640901.data

import androidx.room.Entity

@Entity(
    tableName = "medication_dose_outcomes",
    primaryKeys = [
        "medicationId",
        "scheduledAt"
    ]
)
data class MedicationDoseOutcomeEntity(

    val medicationId: Long,

    val scheduledAt: Long,

    val status: String,

    val recordedAt: Long
)

object DoseStatus {

    const val TAKEN = "TAKEN"

    const val MISSED = "MISSED"

    const val SKIPPED = "SKIPPED"
}