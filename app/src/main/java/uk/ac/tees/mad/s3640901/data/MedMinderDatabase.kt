package uk.ac.tees.mad.s3640901.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MedicationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MedMinderDatabase : RoomDatabase() {

    abstract fun medicationDao(): MedicationDao

    companion object {
        @Volatile
        private var instance: MedMinderDatabase? = null

        fun getInstance(context: Context): MedMinderDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MedMinderDatabase::class.java,
                    "medminder.db"
                ).build().also {
                    instance = it
                }
            }
        }
    }
}