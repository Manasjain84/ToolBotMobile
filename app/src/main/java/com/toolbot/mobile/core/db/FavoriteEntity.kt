package com.toolbot.mobile.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val toolName: String,
    val timestamp: Long = System.currentTimeMillis()
)
