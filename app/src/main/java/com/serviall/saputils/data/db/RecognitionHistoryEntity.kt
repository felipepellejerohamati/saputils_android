package com.serviall.saputils.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recognition_history")
data class RecognitionHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: String,
    val whscode: String,
    val responseJson: String,
    val summary: String
)
