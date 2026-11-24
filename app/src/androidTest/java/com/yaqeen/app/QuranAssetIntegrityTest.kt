package com.yaqeen.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuranAssetIntegrityTest {

    @Test
    fun bundledQuranHasCanonicalSurahAndVerseStructure() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val assetText = context.assets.open("quran-uthmani.txt")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val dataLines = assetText.lineSequence()
            .filter { it.matches(Regex("^\\d+\\|\\d+\\|.*$")) }
            .toList()

        assertEquals("Expected 6,236 Quran verse records", 6236, dataLines.size)
        assertEquals("Expected 114 canonical surah counts", 114, expectedVerseCounts.size)

        val counts = IntArray(114)
        val keys = mutableSetOf<String>()
        val firstVerses = mutableMapOf<Int, String>()

        dataLines.forEach { line ->
            val parts = line.split('|', limit = 3)
            assertEquals("Malformed verse record: $line", 3, parts.size)

            val surah = parts[0].toInt()
            val verse = parts[1].toInt()
            assertTrue("Surah number out of range: $surah", surah in 1..114)
            assertTrue("Verse number must be positive: $line", verse > 0)
            assertTrue("Verse text must not be empty: $line", parts[2].isNotBlank())

            val key = "$surah:$verse"
            assertTrue("Duplicate verse key: $key", keys.add(key))
            counts[surah - 1]++

            if (verse == 1) firstVerses[surah] = parts[2]
        }

        assertEquals(6236, keys.size)
        counts.forEachIndexed { index, count ->
            assertEquals(
                "Unexpected verse count for surah ${index + 1}",
                expectedVerseCounts[index],
                count
            )
            for (verse in 1..count) {
                assertTrue("Missing verse ${index + 1}:$verse", "${index + 1}:$verse" in keys)
            }
        }

        assertEquals("Every surah must have a first verse", 114, firstVerses.size)
        val basmalaEndMarker = "ٱلرَّحِيمِ"
        for (surah in 2..114) {
            if (surah == 9) continue
            val firstVerse = firstVerses[surah] ?: error("Missing first verse for surah $surah")
            val markerStart = firstVerse.indexOf(basmalaEndMarker)
            assertTrue("Opening basmala not found in source for surah $surah", markerStart >= 0)
            val afterMarker = markerStart + basmalaEndMarker.length
            assertTrue("Opening basmala boundary invalid for surah $surah", afterMarker < firstVerse.length)
            assertTrue("Opening basmala is not separated for surah $surah", firstVerse[afterMarker].isWhitespace())
        }

        assertTrue("Tanzil attribution is missing", assetText.contains("Tanzil Quran Text"))
        assertTrue("Tanzil license notice is missing", assetText.contains("Creative Commons Attribution 3.0"))
        assertTrue("Tanzil source URL is missing", assetText.contains("tanzil.net/updates"))
    }
}
