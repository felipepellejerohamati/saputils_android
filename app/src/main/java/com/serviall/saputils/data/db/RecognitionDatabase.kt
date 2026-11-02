package com.serviall.saputils.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RecognitionHistoryEntity::class], version = 1, exportSchema = false)
abstract class RecognitionDatabase : RoomDatabase() {
    abstract fun recognitionHistoryDao(): RecognitionHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: RecognitionDatabase? = null

        fun getDatabase(context: Context): RecognitionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RecognitionDatabase::class.java,
                    "recognition_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
