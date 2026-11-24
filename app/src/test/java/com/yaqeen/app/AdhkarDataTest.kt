package com.yaqeen.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdhkarDataTest {

    @Test
    fun everyDhikrHasUniqueIdValidCountAndVerifiedHttpsSource() {
        assertEquals(
            verifiedAdhkarEntries.size,
            verifiedAdhkarEntries.map { it.id }.distinct().size
        )
        verifiedAdhkarEntries.forEach { entry ->
            assertTrue(entry.id.isNotBlank())
            assertTrue(entry.text.isNotBlank())
            assertTrue(entry.targetCount > 0)
            assertTrue(entry.sourceLabel.isNotBlank())
            assertTrue(entry.sourceUrl.startsWith("https://sunnah.com/"))
        }
    }

    @Test
    fun includesOnlyTheReviewedInitialCategories() {
        assertEquals(
            setOf("morning", "evening", "sleep"),
            verifiedAdhkarEntries.map { it.category }.toSet()
        )
        assertEquals(2, verifiedAdhkarEntries.count { it.category == "morning" })
        assertEquals(1, verifiedAdhkarEntries.count { it.category == "evening" })
        assertEquals(4, verifiedAdhkarEntries.count { it.category == "sleep" })
    }

    @Test
    fun dailyCounterKeysAreSeparatedByDateAndDhikr() {
        assertEquals(
            "adhkar:20261124:sleep_tasbih",
            adhkarStorageKey("20261124", "sleep_tasbih")
        )
        assertTrue(
            adhkarStorageKey("20261124", "sleep_tasbih") !=
                adhkarStorageKey("20261125", "sleep_tasbih")
        )
    }
}
