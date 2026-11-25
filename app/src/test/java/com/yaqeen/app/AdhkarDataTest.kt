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
    fun dailyCounterKeysAreSeparatedByDateAndDhikr() {
        assertEquals("adhkar:20261124:hisn_28_1", adhkarStorageKey("20261124", "hisn_28_1"))
        assertTrue(adhkarStorageKey("20261124", "hisn_28_1") != adhkarStorageKey("20261125", "hisn_28_1"))
    }
}
