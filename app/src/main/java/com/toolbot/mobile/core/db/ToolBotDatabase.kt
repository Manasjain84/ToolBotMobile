package com.toolbot.mobile.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        CalculationHistoryEntity::class,
        PdfHistoryEntity::class,
        AiTextHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ToolBotDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun calculationHistoryDao(): CalculationHistoryDao
    abstract fun pdfHistoryDao(): PdfHistoryDao
    abstract fun aiTextHistoryDao(): AiTextHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: ToolBotDatabase? = null

        fun getInstance(context: Context): ToolBotDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ToolBotDatabase::class.java,
                    "toolbot_database.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
