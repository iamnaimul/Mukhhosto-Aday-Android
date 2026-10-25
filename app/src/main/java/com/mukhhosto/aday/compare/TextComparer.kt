
package com.mukhhosto.aday.compare

import com.mukhhosto.aday.data.ResultData
import com.mukhhosto.aday.data.WordDiff
import com.mukhhosto.aday.data.WordStatus
import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min

object TextComparer {

    fun compare(
        referenceText: String,
        transcript: String,
        studentName: String
    ): ResultData {
        val refTokens = tokenize(referenceText)
        val hypTokens = tokenize(transcript)

        val diffs = align(refTokens, hypTokens)

        var s = 0; var d = 0; var i = 0; var correct = 0
        for (diff in diffs) when (diff.status) {
            WordStatus.CORRECT -> correct++
            WordStatus.SUBSTITUTION -> s++
            WordStatus.DELETION -> d++
            WordStatus.INSERTION -> i++
        }

        val n = refTokens.size
        val wer = if (n == 0) 0.0 else (s + d + i).toDouble() / n
        val accuracy = max(0.0, 100.0 * (1.0 - wer))
        val exact = s == 0 && d == 0 && i == 0 && refTokens.isNotEmpty()

        return ResultData(
            studentName = studentName,
            referenceWordCount = n,
            recognizedWordCount = hypTokens.size,
            correctWords = correct,
            wrongWords = s,
            missingWords = d,
            extraWords = i,
            wordErrorRate = wer,
            accuracy = accuracy,
            exactMatch = exact,
            transcript = transcript,
            diffs = diffs
        )
    }

    private fun tokenize(text: String): List<String> {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFC)
        val cleaned = normalized
            .replace(Regex("[\\p{Punct}।॥\"'“”‘’()\\[\\]{}<>—–\\-\\.,!?;:]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (cleaned.isEmpty()) return emptyList()
        return cleaned
            .split(" ")
            .filter { it.isNotBlank() }
            .map { it.lowercase().trim() }
    }

    private fun align(ref: List<String>, hyp: List<String>): List<WordDiff> {
        val m = ref.size
        val n = hyp.size
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) for (j in 1..n) {
            dp[i][j] = if (ref[i - 1] == hyp[j - 1]) dp[i - 1][j - 1]
            else 1 + minOf(dp[i - 1][j - 1], dp[i - 1][j], dp[i][j - 1])
        }

        val out = ArrayList<WordDiff>()
        var i = m; var j = n
        while (i > 0 || j > 0) {
            when {
                i > 0 && j > 0 && ref[i - 1] == hyp[j - 1] -> {
                    out.add(WordDiff(ref[i - 1], hyp[j - 1], WordStatus.CORRECT))
                    i--; j--
                }
                i > 0 && j > 0 && dp[i][j] == dp[i - 1][j - 1] + 1 -> {
                    out.add(WordDiff(ref[i - 1], hyp[j - 1], WordStatus.SUBSTITUTION))
                    i--; j--
                }
                i > 0 && dp[i][j] == dp[i - 1][j] + 1 -> {
                    out.add(WordDiff(ref[i - 1], null, WordStatus.DELETION))
                    i--
                }
                else -> {
                    out.add(WordDiff(null, hyp[j - 1], WordStatus.INSERTION))
                    j--
                }
            }
        }
        out.reverse()
        return out
    }
}
