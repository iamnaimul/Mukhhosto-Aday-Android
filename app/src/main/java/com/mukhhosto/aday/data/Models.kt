
package com.mukhhosto.aday.data

enum class LanguageMode(val apiCode: String, val label: String) {
    BANGLA("bn", "বাংলা"),
    ENGLISH("en", "English"),
    MIXED("auto", "বাংলা + English")
}

enum class WordStatus { CORRECT, SUBSTITUTION, DELETION, INSERTION }

data class WordDiff(
    val reference: String?,
    val recognized: String?,
    val status: WordStatus
)

data class ResultData(
    val studentName: String,
    val referenceWordCount: Int,
    val recognizedWordCount: Int,
    val correctWords: Int,
    val wrongWords: Int,
    val missingWords: Int,
    val extraWords: Int,
    val wordErrorRate: Double,
    val accuracy: Double,
    val exactMatch: Boolean,
    val transcript: String,
    val diffs: List<WordDiff>
)

enum class ScreenState { READY, RECORDING, PROCESSING, RESULT, ERROR }
