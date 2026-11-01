package com.toolbot.mobile.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AiTextHistoryDao {
    @Query("SELECT * FROM ai_text_history ORDER BY timestamp DESC")
    fun observeHistory(): Flow<List<AiTextHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AiTextHistoryEntity)

    @Query("DELETE FROM ai_text_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM ai_text_history")
    suspend fun clearAll()
}
