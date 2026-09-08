package com.toolbot.mobile.feature.aitext.translator

import com.toolbot.mobile.feature.aitext.common.TextProcessing

data class LanguageOption(
    val code: String,
    val label: String,
)

private val englishToHindiWords = mapOf(
    "hello" to "नमस्कार",
    "good" to "अच्छा",
    "morning" to "सुबह",
    "student" to "विद्यार्थी",
    "students" to "विद्यार्थी",
    "teacher" to "शिक्षक",
    "school" to "स्कूल",
    "book" to "किताब",
    "home" to "घर",
    "water" to "पानी",
    "food" to "भोजन",
    "time" to "समय",
    "study" to "अध्ययन",
    "project" to "प्रोजेक्ट",
    "work" to "काम",
    "language" to "भाषा",
    "friend" to "दोस्त",
    "family" to "परिवार",
    "day" to "दिन",
    "night" to "रात",
    "happy" to "खुश",
    "sad" to "उदास",
    "learn" to "सीखना",
    "read" to "पढ़ना",
    "write" to "लिखना",
    "play" to "खेलना",
    "thank" to "धन्यवाद",
    "you" to "आप",
    "i" to "मैं",
    "we" to "हम",
    "they" to "वे",
    "he" to "वह",
    "she" to "वह",
    "is" to "है",
    "are" to "हैं",
    "was" to "था",
    "were" to "थे",
    "this" to "यह",
    "that" to "वह",
    "here" to "यहाँ",
    "there" to "वहाँ",
    "today" to "आज",
    "tomorrow" to "कल",
    "yesterday" to "कल",
    "how" to "कैसे",
    "what" to "क्या",
    "when" to "कब",
    "where" to "कहाँ",
    "why" to "क्यों",
)

private val hindiToEnglishWords = englishToHindiWords.entries.associate { (k, v) -> v to k }

private val englishPhrases = mapOf(
    "good morning" to "सुप्रभात",
    "thank you" to "धन्यवाद",
    "how are you" to "आप कैसे हैं",
    "what is your name" to "आपका नाम क्या है",
    "i am happy" to "मैं खुश हूँ",
)

private val hindiPhrases = englishPhrases.entries.associate { (k, v) -> v to k }

object OfflineTranslator {
    val supportedLanguages = listOf(
        LanguageOption("en", "English"),
        LanguageOption("hi", "Hindi"),
    )

    fun translate(text: String, fromLanguage: String, toLanguage: String): String {
        val source = fromLanguage.lowercase()
        val target = toLanguage.lowercase()
        if (text.isBlank()) return ""
        if (source == target) return text

        return when {
            source == "en" && target == "hi" -> translateEnglishToHindi(text)
            source == "hi" && target == "en" -> translateHindiToEnglish(text)
            else -> "Unsupported pair: ${supportedLanguages.firstOrNull { it.code == source }?.label ?: source} → ${supportedLanguages.firstOrNull { it.code == target }?.label ?: target}."
        }
    }

    private fun translateEnglishToHindi(text: String): String {
        val normalizedText = TextProcessing.normalizeWhitespace(text)
        val phrases = englishPhrases.entries.sortedByDescending { it.key.length }
        var output = normalizedText
        for ((phrase, translated) in phrases) {
            output = Regex("(?i)\\b${Regex.escape(phrase)}\\b").replace(output, translated)
        }

        val tokens = TextProcessing.tokenizeWords(output)
        val translatedWords = tokens.map { token ->
            val lowered = token.lowercase()
            englishToHindiWords[lowered] ?: token
        }

        val translatedText = translatedWords.joinToString(" ")
        return restoredCase(normalizedText, translatedText)
    }

    private fun translateHindiToEnglish(text: String): String {
        val normalizedText = TextProcessing.normalizeWhitespace(text)
        val phrases = hindiPhrases.entries.sortedByDescending { it.key.length }
        var output = normalizedText
        for ((phrase, translated) in phrases) {
            output = Regex("(?i)\\b${Regex.escape(phrase)}\\b").replace(output, translated)
        }

        val tokens = TextProcessing.tokenizeWords(output)
        val translatedWords = tokens.map { token ->
            val lowered = token.lowercase()
            hindiToEnglishWords[lowered] ?: token
        }

        val translatedText = translatedWords.joinToString(" ")
        return restoredCase(normalizedText, translatedText)
    }

    private fun restoredCase(original: String, translated: String): String {
        if (original.isBlank()) return translated
        val sourceWords = TextProcessing.tokenizeWords(original)
        val targetWords = translated.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (sourceWords.size != targetWords.size) return translated

        return targetWords.mapIndexed { index, word ->
            val sourceWord = sourceWords[index]
            if (sourceWord.length > 1 && sourceWord.first().isUpperCase()) word.replaceFirstChar { it.uppercase() } else word
        }.joinToString(" ")
    }
}
