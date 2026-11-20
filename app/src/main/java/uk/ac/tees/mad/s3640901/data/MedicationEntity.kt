package uk.ac.tees.mad.s3640901.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class MedicationEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Existing medication details
    val name: String,
    val dosage: String,
    val instructions: String,

    // Existing schedule details
    val scheduleType: String,
    val scheduleValue: String,

    // Existing medication status
    val isActive: Boolean = true,

    // Existing private medicine photo path
    val photoPath: String? = null,

    // Prevents old doses being marked as missed
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = System.currentTimeMillis(),

    // Optional OpenFDA name information
    val genericName: String? = null,
    val brandName: String? = null
)