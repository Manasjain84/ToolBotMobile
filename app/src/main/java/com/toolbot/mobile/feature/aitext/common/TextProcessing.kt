package com.toolbot.mobile.feature.aitext.common

object TextProcessing {
    val englishStopWords = setOf(
        "a", "about", "above", "after", "again", "against", "all", "am", "an", "and",
        "any", "are", "as", "at", "be", "because", "been", "before", "being", "below",
        "between", "both", "but", "by", "can", "cannot", "could", "did", "do", "does",
        "doing", "down", "during", "each", "few", "for", "from", "further", "had", "has",
        "have", "having", "he", "her", "here", "hers", "herself", "him", "himself", "his",
        "how", "i", "if", "in", "into", "is", "it", "its", "itself", "just", "me", "more",
        "most", "my", "myself", "no", "nor", "not", "of", "off", "on", "once", "only", "or",
        "other", "our", "ours", "ourselves", "out", "over", "own", "same", "she", "should",
        "so", "some", "such", "than", "that", "the", "their", "theirs", "them", "themselves",
        "then", "there", "these", "they", "this", "those", "through", "to", "too", "under",
        "until", "up", "very", "was", "we", "were", "what", "when", "where", "which", "while",
        "who", "whom", "why", "with", "you", "your", "yours", "yourself", "yourselves"
    )

    fun normalizeWhitespace(input: String): String {
        return input
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace(Regex("[\\t\\u000B\\f ]+"), " ")
            .replace(Regex(" \\n|\\n "), "\n")
            .trim()
    }

    fun splitIntoSentences(input: String): List<String> {
        val normalized = normalizeWhitespace(input)
        if (normalized.isBlank()) return emptyList()
        return normalized
            .split(Regex("(?<=[.!?])\\s+|\\n+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    fun tokenizeWords(input: String): List<String> {
        val sanitized = normalizeWhitespace(input)
        if (sanitized.isBlank()) return emptyList()
        return Regex("[A-Za-z0-9][A-Za-z0-9'’-]*")
            .findAll(sanitized)
            .map { it.value }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
    }

    fun normalizeWord(word: String): String = word
        .trim()
        .lowercase()
        .replace(Regex("[^[A-Za-z0-9]]"), "")

    fun isMeaningfulWord(word: String): Boolean {
        val normalized = normalizeWord(word)
        return normalized.length > 1 && normalized !in englishStopWords
    }

    fun wordFrequency(input: String): Map<String, Int> {
        val frequencies = linkedMapOf<String, Int>()
        for (word in tokenizeWords(input)) {
            val normalized = normalizeWord(word)
            if (normalized.isNotEmpty() && normalized.length > 1 && normalized !in englishStopWords) {
                frequencies[normalized] = frequencies.getOrDefault(normalized, 0) + 1
            }
        }
        return frequencies
    }

    fun countWords(input: String): Int = tokenizeWords(input).size

    fun countUniqueWords(input: String): Int = wordFrequency(input).size

    fun isUsableText(input: String, minWords: Int = 6): Boolean {
        val words = tokenizeWords(input)
        return words.size >= minWords && words.any { isMeaningfulWord(it) }
    }
}
