package com.toolbot.mobile.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculation_history")
data class CalculationHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val toolType: String,
    val inputsSummary: String,
    val resultSummary: String,
    val timestamp: Long = System.currentTimeMillis()
)
