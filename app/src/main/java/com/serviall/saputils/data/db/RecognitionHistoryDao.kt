package com.serviall.saputils.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecognitionHistoryDao {
    @Query("SELECT * FROM recognition_history ORDER BY id DESC")
    fun getAllHistory(): Flow<List<RecognitionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: RecognitionHistoryEntity)

    @Query("DELETE FROM recognition_history")
    suspend fun clearHistory()
}
