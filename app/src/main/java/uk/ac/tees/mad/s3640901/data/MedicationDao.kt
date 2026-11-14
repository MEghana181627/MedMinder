package uk.ac.tees.mad.s3640901.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
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

    @Insert
    suspend fun insertMedication(
        medication: MedicationEntity
    )
}