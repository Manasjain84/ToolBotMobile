package com.toolbot.mobile.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY timestamp DESC")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites")
    suspend fun getAllFavorites(): List<FavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE toolName = :toolName")
    suspend fun deleteFavorite(toolName: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE toolName = :toolName)")
    suspend fun isFavorite(toolName: String): Boolean
}
