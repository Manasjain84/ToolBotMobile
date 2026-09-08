package com.toolbot.mobile.feature.aitext.summarizer

import com.toolbot.mobile.feature.aitext.common.TextProcessing
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class SummaryLength(val label: String) {
    SHORT("Short"),
    MEDIUM("Medium"),
    DETAILED("Detailed")
}

data class SummaryResult(
    val summary: String,
    val originalWordCount: Int,
    val summaryWordCount: Int,
    val reductionPercent: Int,
    val selectedSentenceCount: Int,
)

class TextSummarizerEngine {
    fun summarize(text: String, length: SummaryLength): SummaryResult {
        val normalized = TextProcessing.normalizeWhitespace(text)
        require(normalized.isNotBlank()) { "Enter some text to summarize first." }

        val sentences = TextProcessing.splitIntoSentences(normalized)
        require(sentences.size >= 2) { "Add a bit more text so the summarizer has enough meaning to rank sentences." }

        val totalWords = TextProcessing.countWords(normalized)
        require(totalWords >= 8) { "Please add a longer passage for a useful summary." }

        val frequencies = TextProcessing.wordFrequency(normalized)
        val topKeywords = frequencies.entries
            .sortedByDescending { it.value }
            .take(20)
            .map { it.key }
            .toSet()

        val sentenceScores = sentences.mapIndexed { index, sentence ->
            val words = TextProcessing.tokenizeWords(sentence)
            val meaningful = words.filter { TextProcessing.isMeaningfulWord(it) }
            val meaningfulCount = meaningful.size
            val frequencyScore = meaningful.sumOf { frequencies[it.lowercase()] ?: 0 }
            val keywordDensity = meaningful.count { it.lowercase() in topKeywords }.toDouble() / max(1, meaningfulCount)
            val positionBoost = when {
                index == 0 || index == sentences.lastIndex -> 2.8
                index <= 2 -> 1.8
                else -> 1.0
            }
            val lengthBoost = when {
                meaningfulCount in 7..28 -> 1.5
                meaningfulCount in 29..40 -> 1.2
                meaningfulCount < 5 -> 0.6
                else -> 0.9
            }
            val redundancyPenalty = if (Regex("(\\b\\w+\\b)\\s+\\1", RegexOption.IGNORE_CASE).containsMatchIn(sentence)) 0.6 else 1.0
            val score = (frequencyScore * 1.5) + (keywordDensity * 18.0) + (meaningfulCount * 1.1) + positionBoost + lengthBoost
            index to score * redundancyPenalty
        }

        val rankedSentenceIndices = sentenceScores
            .sortedByDescending { it.second }
            .map { it.first }

        val targetRatio = when (length) {
            SummaryLength.SHORT -> 0.25
            SummaryLength.MEDIUM -> 0.4
            SummaryLength.DETAILED -> 0.55
        }

        val targetCount = max(1, min(sentences.size, (sentences.size * targetRatio).roundToInt()))
        val selectedIndices = mutableSetOf<Int>()

        for (index in rankedSentenceIndices) {
            if (selectedIndices.size >= targetCount) break
            val candidateSentence = sentences[index]
            val isRedundant = selectedIndices.any { selectedIndex ->
                val selectedSentence = sentences[selectedIndex]
                val candidateTokens = TextProcessing.tokenizeWords(candidateSentence)
                val selectedTokens = TextProcessing.tokenizeWords(selectedSentence)
                val commonTokens = candidateTokens.filter { it.lowercase() in selectedTokens.map { token -> token.lowercase() } }
                val overlap = if (candidateTokens.isEmpty() || selectedTokens.isEmpty()) 0.0 else commonTokens.size.toDouble() / max(1, min(candidateTokens.size, selectedTokens.size))
                overlap > 0.75
            }
            if (!isRedundant) {
                selectedIndices += index
            }
        }

        if (selectedIndices.isEmpty()) {
            selectedIndices += rankedSentenceIndices.first()
        }

        val orderedSelectedIndices = selectedIndices.sorted()
        val summarySentences = orderedSelectedIndices.map { sentences[it] }
        val summaryText = summarySentences.joinToString(" ")
        val summaryWordCount = TextProcessing.countWords(summaryText)
        val reductionPercent = if (totalWords == 0) 0 else ((1.0 - (summaryWordCount.toDouble() / totalWords.toDouble())) * 100.0).roundToInt()

        return SummaryResult(
            summary = summaryText,
            originalWordCount = totalWords,
            summaryWordCount = summaryWordCount,
            reductionPercent = reductionPercent.coerceIn(0, 100),
            selectedSentenceCount = summarySentences.size,
        )
    }
}
