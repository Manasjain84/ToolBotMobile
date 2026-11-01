package com.toolbot.mobile.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfHistoryDao {
    @Query("SELECT * FROM pdf_history ORDER BY timestamp DESC")
    fun observeHistory(): Flow<List<PdfHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PdfHistoryEntity)

    @Query("DELETE FROM pdf_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pdf_history")
    suspend fun clearAll()
}
