package com.toolbot.mobile.core.db

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ToolBotRepository(context: Context) {
    private val db = ToolBotDatabase.getInstance(context)
    private val favoriteDao = db.favoriteDao()
    private val calculationHistoryDao = db.calculationHistoryDao()
    private val pdfHistoryDao = db.pdfHistoryDao()
    private val aiTextHistoryDao = db.aiTextHistoryDao()

    val favoritesFlow: Flow<List<FavoriteEntity>> = favoriteDao.observeFavorites()
    val calculationHistoryFlow: Flow<List<CalculationHistoryEntity>> = calculationHistoryDao.observeHistory()
    val pdfHistoryFlow: Flow<List<PdfHistoryEntity>> = pdfHistoryDao.observeHistory()
    val aiTextHistoryFlow: Flow<List<AiTextHistoryEntity>> = aiTextHistoryDao.observeHistory()

    suspend fun getAllFavoritesList(): List<FavoriteEntity> = withContext(Dispatchers.IO) {
        favoriteDao.getAllFavorites()
    }

    suspend fun isFavorite(toolName: String): Boolean = withContext(Dispatchers.IO) {
        favoriteDao.isFavorite(toolName)
    }

    suspend fun addFavorite(toolName: String) = withContext(Dispatchers.IO) {
        favoriteDao.insertFavorite(FavoriteEntity(toolName = toolName))
    }

    suspend fun removeFavorite(toolName: String) = withContext(Dispatchers.IO) {
        favoriteDao.deleteFavorite(toolName)
    }

    suspend fun toggleFavorite(toolName: String) = withContext(Dispatchers.IO) {
        if (favoriteDao.isFavorite(toolName)) {
            favoriteDao.deleteFavorite(toolName)
        } else {
            favoriteDao.insertFavorite(FavoriteEntity(toolName = toolName))
        }
    }

    suspend fun saveCalculationHistory(toolType: String, inputsSummary: String, resultSummary: String) = withContext(Dispatchers.IO) {
        calculationHistoryDao.insert(
            CalculationHistoryEntity(
                toolType = toolType,
                inputsSummary = inputsSummary,
                resultSummary = resultSummary
            )
        )
    }

    suspend fun deleteCalculationHistory(id: Long) = withContext(Dispatchers.IO) {
        calculationHistoryDao.deleteById(id)
    }

    suspend fun clearCalculationHistory() = withContext(Dispatchers.IO) {
        calculationHistoryDao.clearAll()
    }

    suspend fun savePdfHistory(operationType: String, originalFilename: String, outputFilename: String, status: String) = withContext(Dispatchers.IO) {
        pdfHistoryDao.insert(
            PdfHistoryEntity(
                operationType = operationType,
                originalFilename = originalFilename,
                outputFilename = outputFilename,
                status = status
            )
        )
    }

    suspend fun deletePdfHistory(id: Long) = withContext(Dispatchers.IO) {
        pdfHistoryDao.deleteById(id)
    }

    suspend fun clearPdfHistory() = withContext(Dispatchers.IO) {
        pdfHistoryDao.clearAll()
    }

    suspend fun saveAiTextHistory(toolType: String, inputText: String, outputText: String) = withContext(Dispatchers.IO) {
        aiTextHistoryDao.insert(
            AiTextHistoryEntity(
                toolType = toolType,
                inputText = inputText,
                outputText = outputText
            )
        )
    }

    suspend fun deleteAiTextHistory(id: Long) = withContext(Dispatchers.IO) {
        aiTextHistoryDao.deleteById(id)
    }

    suspend fun clearAiTextHistory() = withContext(Dispatchers.IO) {
        aiTextHistoryDao.clearAll()
    }

    companion object {
        @Volatile
        private var INSTANCE: ToolBotRepository? = null

        fun getInstance(context: Context): ToolBotRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ToolBotRepository(context).also { INSTANCE = it }
            }
        }
    }
}
