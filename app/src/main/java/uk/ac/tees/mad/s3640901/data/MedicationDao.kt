package uk.ac.tees.mad.s3640901.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    @Query(
        """
        SELECT * FROM medications
        WHERE isActive = 1
        ORDER BY name COLLATE NOCASE
        """
    )
    fun observeActiveMedications(): Flow<List<MedicationEntity>>

    @Query(
        """
        SELECT * FROM medications
        ORDER BY name COLLATE NOCASE
        """
    )
    fun observeAllMedications(): Flow<List<MedicationEntity>>

    @Query(
        "SELECT * FROM medications WHERE id = :medicationId LIMIT 1"
    )

    suspend fun getMedicationById(
        medicationId: Long
    ): MedicationEntity?

    @Query(
        """
        SELECT * FROM medications
        WHERE isActive = 1
        ORDER BY name COLLATE NOCASE
        """
    )
    suspend fun getActiveMedications(): List<MedicationEntity>

    @Insert
    suspend fun insertMedication(
        medication: MedicationEntity
    ): Long

    @Update
    suspend fun updateMedication(
        medication: MedicationEntity
    )

    @Delete
    suspend fun deleteMedication(
        medication: MedicationEntity
    )

    @Query(
        """
        UPDATE medications
        SET isActive = 0
        WHERE id = :medicationId
        """
    )
    suspend fun deactivateMedication(
        medicationId: Long
    )

    @Query(
        """
        SELECT * FROM medication_dose_outcomes
        ORDER BY scheduledAt DESC
        """
    )
    fun observeDoseOutcomes(): Flow<List<MedicationDoseOutcomeEntity>>

    @Query(
        """
        SELECT * FROM medication_dose_outcomes
        WHERE medicationId = :medicationId
        AND scheduledAt = :scheduledAt
        LIMIT 1
        """
    )
    suspend fun findDoseOutcome(
        medicationId: Long,
        scheduledAt: Long
    ): MedicationDoseOutcomeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDoseOutcome(
        outcome: MedicationDoseOutcomeEntity
    )

    @Query(
        """
        UPDATE medication_dose_outcomes
        SET dismissedAt = :dismissedAt
        WHERE medicationId = :medicationId
        AND scheduledAt = :scheduledAt
        """
    )
    suspend fun dismissDoseOutcome(
        medicationId: Long,
        scheduledAt: Long,
        dismissedAt: Long
    )

    @Query(
        """
        DELETE FROM medication_dose_outcomes
        WHERE medicationId = :medicationId
        """
    )
    suspend fun deleteDoseOutcomesForMedication(
        medicationId: Long
    )
}