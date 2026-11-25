package com.yaqeen.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdhkarDataTest {
    @Test
    fun completeHisnAlMuslimDatasetHasUniqueIdsAndValidCounts() {
        assertEquals(267, verifiedAdhkarEntries.size)
        assertEquals(267, verifiedAdhkarEntries.map { it.id }.distinct().size)
        assertEquals(132, verifiedAdhkarEntries.map { it.category }.distinct().size)
        verifiedAdhkarEntries.forEach { entry ->
            assertTrue(entry.id.startsWith("hisn_"))
            assertTrue(entry.text.isNotBlank())
            assertTrue(entry.targetCount > 0)
            assertTrue(entry.sourceLabel.contains("حصن المسلم"))
            assertEquals("https://github.com/YousefAsalya/Islamic-Pro-azkar-API", entry.sourceUrl)
        }
    }

    @Test
    fun searchFindsDhikrTextWithoutArabicDiacritics() {
        val results = filterAdhkarEntries("الحمد لله")
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { normalizeAdhkarSearch(it.text).contains("الحمد لله") ||
            normalizeAdhkarSearch(it.category).contains("الحمد لله") })
    }

    @Test
    fun categoryFilterAndTextSearchCanBeCombined() {
        val category = verifiedAdhkarEntries.first().category
        val results = filterAdhkarEntries("الله", category)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.category == category })
    }

    @Test
    fun dailyCounterKeysAreSeparatedByDateAndDhikr() {
        assertEquals("adhkar:20261124:hisn_28_1", adhkarStorageKey("20261124", "hisn_28_1"))
        assertTrue(adhkarStorageKey("20261124", "hisn_28_1") != adhkarStorageKey("20261125", "hisn_28_1"))
    }
}
