package com.toolbot.mobile.feature.aitext

import com.toolbot.mobile.feature.aitext.grammar.GrammarImprover
import com.toolbot.mobile.feature.aitext.keywords.KeywordExtractor
import com.toolbot.mobile.feature.aitext.summarizer.SummaryLength
import com.toolbot.mobile.feature.aitext.summarizer.TextSummarizerEngine
import com.toolbot.mobile.feature.aitext.translator.OfflineTranslator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun translatorHandlesSupportedLanguagePair() {
        val translated = OfflineTranslator.translate("hello student", "en", "hi")
        assertTrue(translated.contains("नमस्कार") || translated.contains("विद्यार्थी"))
        assertFalse(translated.contains("hello"))
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

    @Test
    fun translatorRejectsUnsupportedPair() {
        val translated = OfflineTranslator.translate("hello", "en", "fr")
        assertTrue(translated.startsWith("Unsupported pair"))
    }
}
