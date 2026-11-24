package com.yaqeen.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SunnahApiClientTest {

    @Test
    fun detectsPartialCollectionCoverageInsteadOfClaimingComplete() {
        val incomplete = SunnahApiClient.parseCollection(
            """
            {
              "name": "bukhari",
              "hasBooks": true,
              "hasChapters": true,
              "collection": [
                {"lang": "ar", "title": "صحيح البخاري"},
                {"lang": "en", "title": "Sahih al-Bukhari"}
              ],
              "totalHadith": 7563,
              "totalAvailableHadith": 7540
            }
            """.trimIndent()
        )
        assertEquals("صحيح البخاري", incomplete.arabicTitle)
        assertEquals(7563, incomplete.totalHadith)
        assertEquals(7540, incomplete.totalAvailableHadith)
        assertFalse(incomplete.isComplete)

        val complete = SunnahApiClient.parseCollection(
            """
            {
              "name": "muslim",
              "hasBooks": true,
              "hasChapters": true,
              "collection": [{"lang": "ar", "title": "صحيح مسلم"}],
              "totalHadith": 100,
              "totalAvailableHadith": 100
            }
            """.trimIndent()
        )
        assertTrue(complete.isComplete)
    }

    @Test
    fun parsesBooksAndAvailableHadithCounts() {
        val page = SunnahApiClient.parseBooks(
            """
            {
              "total": 1,
              "limit": 100,
              "previous": null,
              "next": null,
              "data": [
                {
                  "bookNumber": "1",
                  "book": [
                    {"lang": "ar", "name": "بدء الوحي"},
                    {"lang": "en", "name": "Revelation"}
                  ],
                  "hadithStartNumber": 1,
                  "hadithEndNumber": 7,
                  "numberOfHadith": 7
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(1, page.data.size)
        assertEquals("بدء الوحي", page.data[0].arabicName)
        assertEquals("Revelation", page.data[0].englishName)
        assertEquals(7, page.data[0].numberOfHadith)
        assertNull(page.nextPage)
    }

    @Test
    fun parsesArabicEnglishHadithAndPublishedGradesOnly() {
        val page = SunnahApiClient.parseHadithPage(
            """
            {
              "total": 1,
              "limit": 100,
              "previous": null,
              "next": null,
              "data": [
                {
                  "collection": "bukhari",
                  "bookNumber": "1",
                  "chapterId": "1",
                  "hadithNumber": "1",
                  "hadith": [
                    {
                      "lang": "ar",
                      "chapterNumber": "1",
                      "chapterTitle": "كيف كان بدء الوحي",
                      "urn": 1,
                      "body": "نص الحديث العربي"
                    },
                    {
                      "lang": "en",
                      "chapterNumber": "1",
                      "chapterTitle": "How Revelation Began",
                      "urn": 1,
                      "body": "English hadith text"
                    }
                  ],
                  "grades": [
                    {"graded_by": "Darussalam", "grade": "Sahih"}
                  ]
                }
              ]
            }
            """.trimIndent()
        )

        val hadith = page.data.single()
        assertEquals("نص الحديث العربي", hadith.arabicText)
        assertEquals("English hadith text", hadith.englishText)
        assertEquals(1, hadith.grades.size)
        assertEquals("Darussalam", hadith.grades[0].gradedBy)
        assertEquals("Sahih", hadith.grades[0].grade)
        assertEquals("bukhari", hadith.collection)
    }

    @Test
    fun doesNotInventGradesWhenSourceHasNone() {
        val hadith = SunnahApiClient.parseHadith(
            """
            {
              "collection": "muslim",
              "bookNumber": "1",
              "chapterId": "1",
              "hadithNumber": "1",
              "hadith": [{"lang": "ar", "body": "نص"}],
              "grades": []
            }
            """.trimIndent()
        )
        assertTrue(hadith.grades.isEmpty())
    }

    @Test
    fun proxyUrlMustUseHttps() {
        assertTrue(SunnahApiClient("https://yaqeen.example.workers.dev").isConfigured)
        assertFalse(SunnahApiClient("").isConfigured)
        assertFalse(SunnahApiClient("http://insecure.example").isConfigured)
    }
}
