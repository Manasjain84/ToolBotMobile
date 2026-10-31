package com.toolbot.mobile.feature.aitext

import com.toolbot.mobile.feature.aitext.grammar.GrammarImprover
import com.toolbot.mobile.feature.aitext.keywords.KeywordExtractor
import com.toolbot.mobile.feature.aitext.summarizer.SummaryLength
import com.toolbot.mobile.feature.aitext.summarizer.TextSummarizerEngine
import com.toolbot.mobile.feature.aitext.translator.OfflineTranslator
import com.toolbot.mobile.feature.aitext.translator.TranslationException
import com.toolbot.mobile.feature.aitext.translator.TranslationFetcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class AiTextToolsTest {

    @Test
    fun summarizerReturnsCoherentResults() {
        val text = """
            Machine learning helps students understand patterns in data. 
            It allows computer systems to improve with experience. 
            Neural networks are a common approach in modern AI. 
            They can classify images, understand language, and support decision making. 
            Students often use these techniques in projects, assignments, and research work.
        """.trimIndent()

        val result = TextSummarizerEngine().summarize(text, SummaryLength.MEDIUM)
        assertFalse(result.summary.isBlank())
        assertTrue(result.originalWordCount > result.summaryWordCount)
        assertTrue(result.summaryWordCount > 0)
        assertTrue(result.reductionPercent >= 0)
    }

    @Test
    fun grammarImproverFixesCommonMistakes() {
        val input = "The students was working on there project. They is ready."
        val result = GrammarImprover().improve(input)

        assertTrue(result.improvedText.contains("were"))
        assertTrue(result.improvedText.contains("their"))
        assertTrue(result.correctionCount > 0)
    }

    @Test
    fun parseTranslationResponseValidJsonMultipleChunks() {
        val json = """[[["Bonjour le monde. ","Hello world.",null,null,10],["Comment allez-vous?","How are you?",null,null,10]],null,"en"]"""
        val parsed = OfflineTranslator.parseTranslationResponse(json)
        assertEquals("Bonjour le monde. Comment allez-vous?", parsed)
    }

    @Test
    fun parseTranslationResponseUnicodeHindi() {
        val json = """[[["ज़ोंबी नाइटमेयर 1987 की एक फ़िल्म है।","Zombie Nightmare is a 1987 movie.",null,null,10]],null,"en"]"""
        val parsed = OfflineTranslator.parseTranslationResponse(json)
        assertEquals("ज़ोंबी नाइटमेयर 1987 की एक फ़िल्म है।", parsed)
    }

    @Test
    fun parseTranslationResponseEmptyAndMalformedJson() {
        assertEquals("", OfflineTranslator.parseTranslationResponse(""))
        assertEquals("", OfflineTranslator.parseTranslationResponse("[]"))
        assertEquals("", OfflineTranslator.parseTranslationResponse("Not a valid json response"))
    }

    @Test
    fun testChunkTextSplitsLongParagraphsOnSentenceBoundaries() {
        val sentence1 = "Sentence one is a detailed explanation of modern computer systems. "
        val sentence2 = "Sentence two provides background information regarding artificial intelligence. "
        val sentence3 = "Sentence three summarizes the conclusion of the research study."
        val longText = sentence1 + sentence2 + sentence3

        val chunks = OfflineTranslator.chunkText(longText, maxChunkSize = 75)
        assertTrue(chunks.size > 1)
        for (chunk in chunks) {
            assertTrue(chunk.length <= 75)
        }
        val reconstructed = chunks.joinToString(" ")
        assertTrue(reconstructed.contains("Sentence one"))
        assertTrue(reconstructed.contains("Sentence two"))
        assertTrue(reconstructed.contains("Sentence three"))
    }

    @Test
    fun testParagraphOrderPreservation() {
        val multiParagraphInput = "First paragraph content here.\n\nSecond paragraph content here."
        val mockFetcher = TranslationFetcher { text, _, _ ->
            if (text.contains("First")) "Premier paragraphe." else "Deuxième paragraphe."
        }

        val result = OfflineTranslator.translate(multiParagraphInput, "en", "fr", fetcher = mockFetcher)
        assertTrue(result.isSuccess)

        val output = result.getOrThrow()
        val paragraphs = output.split("\n\n")
        assertEquals(2, paragraphs.size)
        assertEquals("Premier paragraphe.", paragraphs[0])
        assertEquals("Deuxième paragraphe.", paragraphs[1])
    }

    @Test
    fun testRateLimitErrorPreserved() {
        val fetcher = TranslationFetcher { _, _, _ ->
            throw TranslationException.RateLimited("Rate limit exceeded. Please wait a moment and try again.")
        }
        val result = OfflineTranslator.translate("Some text", "en", "fr", fetcher = fetcher)
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is TranslationException.RateLimited)
        assertEquals("Rate limit exceeded. Please wait a moment and try again.", ex?.message)
    }

    @Test
    fun testServiceAccessForbiddenErrorPreserved() {
        val fetcher = TranslationFetcher { _, _, _ ->
            throw TranslationException.ServiceError("Translation service access forbidden.")
        }
        val result = OfflineTranslator.translate("Some text", "en", "fr", fetcher = fetcher)
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is TranslationException.ServiceError)
        assertEquals("Translation service access forbidden.", ex?.message)
    }

    @Test
    fun testOneFailedChunkFailsEntireTranslation() {
        val text = "Sentence one is long. Sentence two is also long."
        var callCount = 0
        val failingFetcher = TranslationFetcher { _, _, _ ->
            callCount++
            if (callCount == 1) "Translated sentence one." else ""
        }

        val result = OfflineTranslator.translate(text, "en", "fr", fetcher = failingFetcher, maxChunkSize = 25)
        assertTrue(result.isFailure)

        val exception = result.exceptionOrNull()
        assertTrue(exception is TranslationException.ParsingFailed)
        assertEquals("Unable to parse translation output.", exception?.message)
    }

    @Test
    fun testFailedNetworkRequestReturnsClearError() {
        val mockFetcher = TranslationFetcher { _, _, _ ->
            throw TranslationException.NetworkFailure("Internet connection required for full translation.", IOException("Offline"))
        }

        val result = OfflineTranslator.translate("Complex text to translate", "en", "fr", fetcher = mockFetcher)
        assertTrue(result.isFailure)

        val exception = result.exceptionOrNull()
        assertTrue(exception is TranslationException.NetworkFailure)
        assertEquals("Internet connection required for full translation.", exception?.message)
    }

    @Test
    fun translatorSameLanguageReturnsOriginalText() {
        val result = OfflineTranslator.translate("Hello world", "en", "en")
        assertTrue(result.isSuccess)
        assertEquals("Hello world", result.getOrNull())
    }

    @Test
    fun translatorKnownPhraseOfflineTranslation() {
        val result = OfflineTranslator.translate("good morning", "en", "hi")
        assertTrue(result.isSuccess)
        assertEquals("सुप्रभात", result.getOrNull())
    }

    @Test
    fun keywordExtractorRanksKeyTerms() {
        val text = "Machine learning is a branch of artificial intelligence. It studies algorithms and data training. Neural networks support better prediction and pattern recognition. Students use machine learning to solve real problems."
        val result = KeywordExtractor().extractKeywords(text, 5)

        assertTrue(result.keywords.size <= 5)
        assertTrue(result.totalWords > 0)
        assertTrue(result.uniqueWords > 0)
        assertTrue(result.extractedCount > 0)
    }
}
