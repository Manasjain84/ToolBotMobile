package com.toolbot.mobile

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.toolbot.mobile.core.db.AiTextHistoryEntity
import com.toolbot.mobile.core.db.CalculationHistoryEntity
import com.toolbot.mobile.core.db.FavoriteEntity
import com.toolbot.mobile.core.db.PdfHistoryEntity
import com.toolbot.mobile.core.db.ToolBotDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ToolBotDatabaseTest {
    private lateinit var database: ToolBotDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ToolBotDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun testFavoritesAddRemoveAndDuplicatePrevention() = runBlocking {
        val dao = database.favoriteDao()
        dao.insertFavorite(FavoriteEntity("GPA Calculator"))
        dao.insertFavorite(FavoriteEntity("GPA Calculator")) // duplicate test (REPLACE strategy)

        val favorites = dao.getAllFavorites()
        assertEquals(1, favorites.size)
        assertTrue(dao.isFavorite("GPA Calculator"))

        dao.deleteFavorite("GPA Calculator")
        assertFalse(dao.isFavorite("GPA Calculator"))
        assertTrue(dao.getAllFavorites().isEmpty())
    }

    @Test
    fun testCalculationHistorySaveRetrieveDeleteAndClear() = runBlocking {
        val dao = database.calculationHistoryDao()
        dao.insert(CalculationHistoryEntity(toolType = "CGPA Calculator", inputsSummary = "Sem: 2", resultSummary = "CGPA: 8.5"))

        val history = dao.observeHistory().first()
        assertEquals(1, history.size)
        assertEquals("CGPA Calculator", history[0].toolType)

        val id = history[0].id
        dao.deleteById(id)
        assertTrue(dao.observeHistory().first().isEmpty())

        dao.insert(CalculationHistoryEntity(toolType = "Smart Attendance", inputsSummary = "75%", resultSummary = "Required: 2"))
        assertEquals(1, dao.observeHistory().first().size)

        dao.clearAll()
        assertTrue(dao.observeHistory().first().isEmpty())
    }

    @Test
    fun testPdfHistorySaveAndRetrieve() = runBlocking {
        val dao = database.pdfHistoryDao()
        dao.insert(PdfHistoryEntity(operationType = "PDF Merger", originalFilename = "a.pdf", outputFilename = "merged.pdf", status = "Success"))

        val history = dao.observeHistory().first()
        assertEquals(1, history.size)
        assertEquals("PDF Merger", history[0].operationType)
        assertEquals("Success", history[0].status)
    }

    @Test
    fun testAiTextHistorySaveAndRetrieve() = runBlocking {
        val dao = database.aiTextHistoryDao()
        dao.insert(AiTextHistoryEntity(toolType = "AI Text Summarizer", inputText = "Long text...", outputText = "Summary..."))

        val history = dao.observeHistory().first()
        assertEquals(1, history.size)
        assertEquals("AI Text Summarizer", history[0].toolType)
        assertEquals("Summary...", history[0].outputText)
    }
}
