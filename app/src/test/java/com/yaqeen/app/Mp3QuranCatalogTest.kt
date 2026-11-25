package com.yaqeen.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Mp3QuranCatalogTest {
    @Test
    fun parsesEveryValidReciterInsteadOfAWhitelistedSubset() {
        val json = """{
            "reciters": [
                {
                    "id": 91,
                    "name": "قارئ غير موجود في القائمة القديمة",
                    "moshaf": [
                        {"id": 4, "name": "حفص عن عاصم", "server": "https://audio.example/hafs", "surah_list": "1,2,114"},
                        {"id": 5, "name": "رواية ورش", "server": "https://audio.example/warsh", "surah_list": "1,3"}
                    ]
                }
            ]
        }"""
        val result = Mp3QuranCatalogClient.parseRecitations(json)
        assertEquals(2, result.size)
        assertEquals("قارئ غير موجود في القائمة القديمة", result.first().source.reciterName)
        assertEquals("حفص عن عاصم", result.first().source.style)
        assertEquals(listOf(1, 2, 114), result.first().availableSurahs)
        assertEquals("mp3quran_91_4", result.first().source.id)
        assertTrue(result.last().source.baseUrl.startsWith("https://"))
    }

    @Test
    fun ignoresInvalidServersAndInvalidSurahNumbers() {
        val json = """{
            "reciters": [{
                "id": 12,
                "name": "قارئ",
                "moshaf": [
                    {"id": 1, "name": "مرتل", "server": "http://insecure.example", "surah_list": "1,2"},
                    {"id": 2, "name": "مجود", "server": "https://audio.example", "surah_list": "0,115"}
                ]
            }]
        }"""
        assertTrue(Mp3QuranCatalogClient.parseRecitations(json).isEmpty())
    }
}
