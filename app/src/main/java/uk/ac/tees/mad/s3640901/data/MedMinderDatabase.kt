package uk.ac.tees.mad.s3640901.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MedicationEntity::class],
    version = 2,
    exportSchema = false
)
abstract class MedMinderDatabase : RoomDatabase() {

    abstract fun medicationDao(): MedicationDao

    companion object {

        @Volatile
        private var instance: MedMinderDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {

            override fun migrate(
                database: SupportSQLiteDatabase
            ) {
                database.execSQL(
                    "ALTER TABLE medications ADD COLUMN photoPath TEXT"
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
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also {
                        instance = it
                    }
            }
        }
    }
}