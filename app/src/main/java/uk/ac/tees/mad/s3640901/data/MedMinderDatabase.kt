package uk.ac.tees.mad.s3640901.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MedicationEntity::class, MedicationDoseOutcomeEntity::class],
    version = 3,
    exportSchema = false
)
abstract class MedMinderDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao

    companion object {
        @Volatile
        private var instance: MedMinderDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE medications ADD COLUMN photoPath TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS medication_dose_outcomes (
                        medicationId INTEGER NOT NULL,
                        scheduledAt INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        recordedAt INTEGER NOT NULL,
                        PRIMARY KEY(medicationId, scheduledAt)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): MedMinderDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MedMinderDatabase::class.java,
                    "medminder.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
        }
    }
}