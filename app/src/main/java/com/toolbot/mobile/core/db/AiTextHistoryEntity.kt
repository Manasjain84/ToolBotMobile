package com.toolbot.mobile.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_text_history")
data class AiTextHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val toolType: String,
    val inputText: String,
    val outputText: String,
    val timestamp: Long = System.currentTimeMillis()
)
