package com.toolbot.mobile.feature.aitext.translator

import org.json.JSONArray
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets

data class LanguageOption(
    val code: String,
    val label: String,
)

sealed class TranslationException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NetworkFailure(message: String, cause: Throwable? = null) : TranslationException(message, cause)
    class RateLimited(message: String) : TranslationException(message)
    class HttpError(message: String) : TranslationException(message)
    class ServiceError(message: String, cause: Throwable? = null) : TranslationException(message, cause)
    class ParsingFailed(message: String) : TranslationException(message)
}

fun interface TranslationFetcher {
    fun fetch(text: String, sourceLang: String, targetLang: String): String
}

private object LogUtils {
    fun w(tag: String, message: String) {
        try {
            android.util.Log.w(tag, message)
        } catch (_: Throwable) {
            println("[$tag] WARN: $message")
        }
    }

    fun e(tag: String, message: String) {
        try {
            android.util.Log.e(tag, message)
        } catch (_: Throwable) {
            println("[$tag] ERROR: $message")
        }
    }
}

object DefaultTranslationFetcher : TranslationFetcher {
    override fun fetch(text: String, sourceLang: String, targetLang: String): String {
        val encodedText = URLEncoder.encode(text, "UTF-8")
        val urlString = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$sourceLang&tl=$targetLang&dt=t&q=$encodedText"
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile)")

        return try {
            val statusCode = connection.responseCode
            if (statusCode == 200) {
                val jsonText = connection.inputStream.use { stream ->
                    stream.bufferedReader(StandardCharsets.UTF_8).readText()
                }
                val parsed = OfflineTranslator.parseTranslationResponse(jsonText)
                if (parsed.isBlank()) {
                    LogUtils.w("TranslatorEngine", "HTTP 200 OK received but response body yielded blank translated text.")
                    throw TranslationException.ParsingFailed("Unable to parse translation output.")
                }
                parsed
            } else {
                val errorBody = connection.errorStream?.use { it.bufferedReader(StandardCharsets.UTF_8).readText() }.orEmpty()
                LogUtils.w("TranslatorEngine", "HTTP $statusCode error from translation endpoint. Error response size: ${errorBody.length}")
                when (statusCode) {
                    429 -> throw TranslationException.RateLimited("Rate limit exceeded. Please wait a moment and try again.")
                    403 -> throw TranslationException.ServiceError("Translation service access forbidden.")
                    in 500..599 -> throw TranslationException.ServiceError("Translation service is temporarily unavailable.")
                    else -> throw TranslationException.HttpError("Translation service error (HTTP $statusCode).")
                }
            }
        } catch (e: TranslationException) {
            throw e
        } catch (e: UnknownHostException) {
            LogUtils.w("TranslatorEngine", "Network failure (UnknownHost): ${e.message}")
            throw TranslationException.NetworkFailure("Internet connection required for full translation.", e)
        } catch (e: ConnectException) {
            LogUtils.w("TranslatorEngine", "Network failure (ConnectException): ${e.message}")
            throw TranslationException.NetworkFailure("Internet connection required for full translation.", e)
        } catch (e: SocketTimeoutException) {
            LogUtils.w("TranslatorEngine", "Network timeout (SocketTimeout): ${e.message}")
            throw TranslationException.NetworkFailure("Translation request timed out. Please try again.", e)
        } catch (e: IOException) {
            LogUtils.w("TranslatorEngine", "IO Error during translation: ${e.javaClass.simpleName}: ${e.message}")
            throw TranslationException.NetworkFailure("Internet connection required for full translation.", e)
        } catch (e: Exception) {
            LogUtils.e("TranslatorEngine", "Unexpected error during translation: ${e.javaClass.simpleName}: ${e.message}")
            throw TranslationException.ServiceError("Translation failed: ${e.message ?: "Unexpected error"}", e)
        } finally {
            connection.disconnect()
        }
    }
}

private val englishToHindiWords = mapOf(
    "hello" to "नमस्कार",
    "good morning" to "सुप्रभात",
    "thank you" to "धन्यवाद",
    "welcome" to "स्वागत है",
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
        LanguageOption("fr", "French"),
        LanguageOption("es", "Spanish"),
        LanguageOption("de", "German"),
        LanguageOption("it", "Italian"),
        LanguageOption("pt", "Portuguese"),
        LanguageOption("ru", "Russian"),
        LanguageOption("ja", "Japanese"),
        LanguageOption("ko", "Korean"),
        LanguageOption("zh-CN", "Chinese"),
        LanguageOption("ar", "Arabic"),
    )

    fun translate(
        text: String,
        fromLanguage: String,
        toLanguage: String,
        fetcher: TranslationFetcher = DefaultTranslationFetcher,
        maxChunkSize: Int = 1000,
    ): Result<String> {
        val source = fromLanguage.lowercase().trim()
        val target = toLanguage.lowercase().trim()
        if (text.isBlank()) return Result.success("")
        if (source == target) return Result.success(text)

        return runCatching {
            val paragraphs = text.split("\n\n")
            val translatedParagraphs = mutableListOf<String>()

            for (paragraph in paragraphs) {
                val trimmedParagraph = paragraph.trim()
                if (trimmedParagraph.isBlank()) {
                    translatedParagraphs.add("")
                    continue
                }

                val localPhrase = translateLocalPhrase(trimmedParagraph, source, target)
                if (localPhrase != null) {
                    translatedParagraphs.add(localPhrase)
                    continue
                }

                val chunks = chunkText(trimmedParagraph, maxChunkSize)
                val translatedChunkList = mutableListOf<String>()

                for (chunk in chunks) {
                    val translatedChunk = fetcher.fetch(chunk, source, target)
                    if (translatedChunk.isBlank()) {
                        throw TranslationException.ParsingFailed("Unable to parse translation output.")
                    }
                    translatedChunkList.add(translatedChunk)
                }

                translatedParagraphs.add(translatedChunkList.joinToString(" "))
            }

            val finalOutput = translatedParagraphs.joinToString("\n\n")
            if (finalOutput.isBlank()) {
                throw TranslationException.ParsingFailed("Unable to parse translation output.")
            }
            finalOutput
        }
    }

    fun parseTranslationResponse(jsonString: String): String {
        if (jsonString.isBlank()) return ""

        try {
            val rootArray = JSONArray(jsonString)
            if (rootArray.length() > 0) {
                val sentencesArray = rootArray.optJSONArray(0)
                if (sentencesArray != null && sentencesArray.length() > 0) {
                    val builder = StringBuilder()
                    var foundSentence = false
                    for (i in 0 until sentencesArray.length()) {
                        val sentencePair = sentencesArray.optJSONArray(i)
                        if (sentencePair != null && sentencePair.length() > 0) {
                            val translatedChunk = sentencePair.optString(0, "")
                            if (translatedChunk.isNotBlank() && translatedChunk != "null") {
                                builder.append(translatedChunk)
                                foundSentence = true
                            }
                        }
                    }
                    if (foundSentence) {
                        return builder.toString().trim()
                    }
                }
            }
        } catch (_: Exception) {
        }

        return parseJsonFallback(jsonString)
    }

    private fun parseJsonFallback(jsonString: String): String {
        val sentencePattern = Regex("\\[\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*,\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
        val matches = sentencePattern.findAll(jsonString).map { it.groupValues[1] }.toList()

        if (matches.isEmpty()) return ""

        val builder = StringBuilder()
        for (rawMatch in matches) {
            val unescaped = unescapeJsonString(rawMatch)
            if (unescaped.isNotBlank() && unescaped != "null") {
                builder.append(unescaped)
            }
        }

        return builder.toString().trim()
    }

    private fun unescapeJsonString(input: String): String {
        return input
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\/", "/")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace(Regex("\\\\u([0-9a-fA-F]{4})")) { match ->
                match.groupValues[1].toInt(16).toChar().toString()
            }
    }

    fun chunkText(text: String, maxChunkSize: Int = 1000): List<String> {
        val trimmed = text.trim()
        if (trimmed.length <= maxChunkSize) return listOf(trimmed)

        val sentences = trimmed.split(Regex("(?<=[.!?।\\n])\\s+")).filter { it.isNotBlank() }
        val resultChunks = mutableListOf<String>()
        var currentChunk = StringBuilder()

        for (sentence in sentences) {
            if (sentence.length > maxChunkSize) {
                if (currentChunk.isNotEmpty()) {
                    resultChunks.add(currentChunk.toString().trim())
                    currentChunk = StringBuilder()
                }
                val words = sentence.split(Regex("\\s+"))
                var wordChunk = StringBuilder()
                for (word in words) {
                    if (wordChunk.length + word.length + 1 <= maxChunkSize) {
                        if (wordChunk.isNotEmpty()) wordChunk.append(" ")
                        wordChunk.append(word)
                    } else {
                        if (wordChunk.isNotEmpty()) {
                            resultChunks.add(wordChunk.toString().trim())
                        }
                        wordChunk = StringBuilder(word)
                    }
                }
                if (wordChunk.isNotEmpty()) {
                    resultChunks.add(wordChunk.toString().trim())
                }
            } else if (currentChunk.length + sentence.length + 1 <= maxChunkSize) {
                if (currentChunk.isNotEmpty()) currentChunk.append(" ")
                currentChunk.append(sentence)
            } else {
                resultChunks.add(currentChunk.toString().trim())
                currentChunk = StringBuilder(sentence)
            }
        }

        if (currentChunk.isNotEmpty()) {
            resultChunks.add(currentChunk.toString().trim())
        }

        return resultChunks.filter { it.isNotBlank() }
    }

    private fun translateLocalPhrase(text: String, source: String, target: String): String? {
        val trimmed = text.trim()
        val lowered = trimmed.lowercase()

        if (source == "en" && target == "hi") {
            englishPhrases[lowered]?.let { return it }
            englishToHindiWords[lowered]?.let { return it }
        } else if (source == "hi" && target == "en") {
            hindiPhrases[lowered]?.let { return it }
            hindiToEnglishWords[lowered]?.let { return it }
        }

        return null
    }
}
