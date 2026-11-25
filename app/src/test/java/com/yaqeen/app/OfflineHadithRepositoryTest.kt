package com.yaqeen.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineHadithRepositoryTest {
    @Test
    fun arabicSearchIgnoresTashkeelAndAlefVariants() {
        assertEquals(
            "انما الاعمال بالنيات",
            normalizeHadithSearchText("إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ")
        )
    }

    @Test
    fun arabicSearchRemovesTatweelAndInvisibleMarks() {
        assertEquals(
            "الحمد لله",
            normalizeHadithSearchText("الـحَمْدُ‏ لِلَّهِ")
        )
    }

    @Test
    fun searchNormalizationKeepsArabicWordsReadable() {
        val normalized = normalizeHadithSearchText("من كان يؤمن بالله")
        assertTrue(normalized.contains("يؤمن") || normalized.contains("يومن"))
    }
}
