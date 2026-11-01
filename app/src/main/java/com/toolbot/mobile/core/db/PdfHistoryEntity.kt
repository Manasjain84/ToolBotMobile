package com.toolbot.mobile.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_history")
data class PdfHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val operationType: String,
    val originalFilename: String,
    val outputFilename: String,
    val status: String,
    val timestamp: Long = System.currentTimeMillis()
)
