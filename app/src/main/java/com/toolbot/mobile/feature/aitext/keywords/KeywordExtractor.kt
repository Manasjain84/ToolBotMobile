package com.toolbot.mobile.feature.aitext.keywords

import com.toolbot.mobile.feature.aitext.common.TextProcessing
import kotlin.math.max

private data class Candidate(
    val text: String,
    val score: Double,
)

data class KeywordResult(
    val keywords: List<String>,
    val totalWords: Int,
    val uniqueWords: Int,
    val extractedCount: Int,
)

class KeywordExtractor {
    fun extractKeywords(text: String, limit: Int = 10): KeywordResult {
        val normalized = TextProcessing.normalizeWhitespace(text)
        if (normalized.isBlank()) {
            return KeywordResult(emptyList(), 0, 0, 0)
        }

        val words = TextProcessing.tokenizeWords(normalized)
        val frequencies = TextProcessing.wordFrequency(normalized)
        val meaningful = frequencies.entries
            .filter { entry ->
                val word = entry.key
                word.length > 2 && word !in TextProcessing.englishStopWords
            }
            .sortedByDescending { it.value }
            .associate { it.key to it.value }

        val candidates = mutableListOf<Candidate>()
        meaningful.forEach { (word, frequency) ->
            val sentencePosition = 1.0 / (1.0 + countFirstAppearance(words, word))
            val score = frequency * 4.0 + sentencePosition * 3.0 + (word.length * 0.5)
            candidates += Candidate(word, score)
        }

        val phrases = buildPhrases(words)
        phrases.forEach { phrase ->
            val phraseWords = phrase.split(" ")
            val phraseScore = phraseWords.sumOf { frequencies[it] ?: 0 } * 1.5
            candidates += Candidate(phrase, phraseScore)
        }

        val unique = candidates
            .distinctBy { it.text }
            .sortedByDescending { it.score }
            .map { it.text }
            .filter { it.length > 2 }
            .take(limit)

        val ranked = unique.map { value ->
            value.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        return KeywordResult(
            keywords = ranked,
            totalWords = words.size,
            uniqueWords = frequencies.size,
            extractedCount = ranked.size,
        )
    }

    private fun countFirstAppearance(words: List<String>, target: String): Int {
        var index = 0
        for (word in words) {
            if (word.lowercase() == target) return index
            index++
        }
        return Int.MAX_VALUE
    }

    private fun buildPhrases(words: List<String>): List<String> {
        val phraseMap = linkedMapOf<String, Int>()
        for (i in 0 until words.size - 1) {
            val first = words[i].lowercase()
            val second = words[i + 1].lowercase()
            if (first.length > 2 && second.length > 2 && first !in TextProcessing.englishStopWords && second !in TextProcessing.englishStopWords) {
                val phrase = "$first $second"
                phraseMap[phrase] = phraseMap.getOrDefault(phrase, 0) + 1
            }
        }
        return phraseMap.entries.sortedByDescending { it.value }.take(8).map { it.key }
    }
}
