
package com.mukhhosto.aday

import com.mukhhosto.aday.compare.TextComparer
import com.mukhhosto.aday.data.WordStatus
import org.junit.Assert.*
import org.junit.Test

class TextComparerTest {

    @Test fun exactMatch() {
        val r = TextComparer.compare("আমি প্রতিদিন বিদ্যালয়ে যাই।", "আমি প্রতিদিন বিদ্যালয়ে যাই।", "A")
        assertTrue(r.exactMatch)
        assertEquals(100.0, r.accuracy, 0.001)
        assertEquals(0.0, r.wordErrorRate, 0.001)
    }

    @Test fun oneSubstitution() {
        val r = TextComparer.compare("আমি প্রতিদিন বিদ্যালয়ে যাই।", "আমি প্রতিদিন স্কুলে যাই।", "A")
        assertFalse(r.exactMatch)
        assertEquals(1, r.wrongWords)
        assertEquals(0, r.missingWords)
        assertEquals(0, r.extraWords)
        assertEquals(0.25, r.wordErrorRate, 0.001)
        assertEquals(75.0, r.accuracy, 0.001)
        assertTrue(r.diffs.any { it.status == WordStatus.SUBSTITUTION })
    }

    @Test fun deletion() {
        val r = TextComparer.compare("আমি প্রতিদিন বিদ্যালয়ে যাই।", "আমি প্রতিদিন যাই।", "A")
        assertEquals(1, r.missingWords)
        assertEquals(0.25, r.wordErrorRate, 0.001)
    }

    @Test fun insertion() {
        val r = TextComparer.compare("আমি যাই।", "আমি আজ যাই।", "A")
        assertEquals(1, r.extraWords)
        assertEquals(0.5, r.wordErrorRate, 0.001)
        assertEquals(50.0, r.accuracy, 0.001)
    }

    @Test fun emptyTranscript() {
        val r = TextComparer.compare("আমি যাই।", "", "A")
        assertEquals(2, r.missingWords)
        assertEquals(1.0, r.wordErrorRate, 0.001)
        assertEquals(0.0, r.accuracy, 0.001)
    }
}
