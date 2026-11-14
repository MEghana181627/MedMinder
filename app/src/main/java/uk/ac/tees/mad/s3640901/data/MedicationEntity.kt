package uk.ac.tees.mad.s3640901.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val dosage: String,
    val instructions: String,
    val scheduleType: String,
    val scheduleValue: String,
    val isActive: Boolean = true
)