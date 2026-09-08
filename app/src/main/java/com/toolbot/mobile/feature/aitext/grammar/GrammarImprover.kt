package com.toolbot.mobile.feature.aitext.grammar

import com.toolbot.mobile.feature.aitext.common.TextProcessing

private val sentenceStartWords = setOf("i", "we", "you", "they", "he", "she", "it", "the", "this", "that", "there", "here")

private val subjectVerbRules = listOf(
    "students was" to "students were",
    "students is" to "students are",
    "he are" to "he is",
    "they is" to "they are",
    "it are" to "it is",
    "we is" to "we are",
    "you is" to "you are",
    "i are" to "i am",
)

private val commonConfusions = listOf(
    " there project" to " their project",
    " there work" to " their work",
    " there idea" to " their idea",
    " there essay" to " their essay",
    " there report" to " their report",
    " there answer" to " their answer",
    " there task" to " their task",
    " there paper" to " their paper",
    " there story" to " their story",
    " your doing" to " you're doing",
    " your going" to " you're going",
    " your working" to " you're working",
    " its okay" to " it's okay",
    " its fine" to " it's fine",
    " its time" to " it's time",
    " its important" to " it's important",
)

data class GrammarResult(
    val improvedText: String,
    val correctionCount: Int,
    val changes: List<String>,
)

class GrammarImprover {
    fun improve(text: String): GrammarResult {
        if (text.isBlank()) {
            return GrammarResult("", 0, emptyList())
        }

        var workingText = TextProcessing.normalizeWhitespace(text)
        val changes = mutableListOf<String>()

        fun replaceWithChange(pattern: Regex, replacement: (MatchResult) -> String, description: (MatchResult) -> String) {
            var nextText = workingText
            val matches = pattern.findAll(nextText).toList()
            if (matches.isEmpty()) return
            matches.forEach { match ->
                val newValue = replacement(match)
                val before = match.value
                nextText = nextText.replaceFirst(before, newValue)
                changes += description(match)
            }
            workingText = nextText
        }

        val repeatedWordsPattern = Regex("\\b([A-Za-z]+)\\s+\\1\\b", RegexOption.IGNORE_CASE)
        workingText = repeatedWordsPattern.replace(workingText) { match ->
            val value = match.groupValues[1]
            changes += "${match.value} → $value"
            value
        }

        workingText = Regex("\\s+([,.;:!?])").replace(workingText) { match -> match.groupValues[1] }
        workingText = Regex("([,.;:!?])([A-Za-z0-9])").replace(workingText) { match -> "${match.groupValues[1]} ${match.groupValues[2]}" }
        workingText = Regex("\\s{2,}").replace(workingText, " ")

        val loadedText = workingText
        val sentenceRegex = Regex("(?<=[.!?])\\s+|\\n+")
        val sentences = sentenceRegex.split(loadedText).filter { it.isNotBlank() }
        val fixedSentences = sentences.mapIndexed { index, sentence ->
            var fixed = sentence.trim()
            if (fixed.isNotBlank()) {
                fixed = fixed.replace(Regex("^([a-z])")) { match -> match.groupValues[1].uppercase() }
                if (fixed.startsWith("i ") && !fixed.startsWith("I ")) {
                    fixed = fixed.replaceFirst("(?i)^i\\b", "I")
                }
            }

            val before = fixed
            subjectVerbRules.forEach { (wrong, correct) ->
                val pattern = Regex("(?i)\\b$wrong\\b")
                if (pattern.containsMatchIn(fixed)) {
                    fixed = pattern.replace(fixed, correct)
                    if (before != fixed) {
                        changes += "$wrong → $correct"
                    }
                }
            }

            commonConfusions.forEach { (wrong, correct) ->
                val pattern = Regex("(?i)\\b${wrong.replace(" ", "\\s+")}\\b")
                if (pattern.containsMatchIn(fixed)) {
                    fixed = pattern.replace(fixed, correct)
                    changes += "$wrong → $correct"
                }
            }

            val wordPattern = Regex("\\b(there|their|their|you|its|it's|your|you're)\\b", RegexOption.IGNORE_CASE)
            if (sentenceStartWords.contains(fixed.lowercase().substringBefore(" ")) && fixed.firstOrNull()?.isLetter() == true) {
                fixed = fixed.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }

            if (wordPattern.containsMatchIn(fixed)) {
                val therePattern = Regex("(?i)\\bthere\\b(?=\\s+(project|work|idea|essay|report|answer|task|paper|story|homework|assignment|document|presentation)\\b)")
                fixed = therePattern.replace(fixed) { _ -> "their" }
                if (fixed != before) {
                    changes += "there → their"
                }

                val itsPattern = Regex("(?i)\\bits\\b(?=\\s+(okay|fine|time|important|clear|good|done)\\b)")
                fixed = itsPattern.replace(fixed) { _ -> "it's" }
                if (fixed != before) {
                    changes += "its → it's"
                }

                val yourPattern = Regex("(?i)\\byour\\b(?=\\s+(doing|going|working|thinking|trying|writing|reading)\\b)")
                fixed = yourPattern.replace(fixed) { _ -> "you're" }
                if (fixed != before) {
                    changes += "your → you're"
                }
            }

            fixed = fixed.replace(Regex("\\b([A-Za-z])([A-Za-z]+)\\s*([,.;:!?])"), "$1$2$3")
            fixed = fixed.replace(Regex("\\b([A-Za-z])([A-Za-z]+)\\s+([,.;:!?])\\s+"), "$1$2$3 ")
            fixed
        }

        val uniqueChanges = changes.distinct().filter { it.isNotBlank() }
        val improved = fixedSentences.joinToString(" ")
        return GrammarResult(
            improvedText = improved,
            correctionCount = uniqueChanges.size,
            changes = uniqueChanges,
        )
    }
}
