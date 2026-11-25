package com.yaqeen.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranFoundationClientTest {

    @Test
    fun parsesChapterReciterCatalog() {
        val json = """
            {
              "reciters": [
                {
                  "id": 3,
                  "name": "Abu Bakr al-Shatri",
                  "style": {"name": "Murattal", "language_name": "english"},
                  "qirat": {"name": "Hafs", "language_name": "english"},
                  "translated_name": {"name": "أبو بكر الشاطري", "language_name": "arabic"}
                },
                {
                  "id": 4,
                  "name": "Another Reciter",
                  "style": {"name": null},
                  "qirat": {"name": "Warsh"}
                },
                {"id": 0, "name": "Invalid"}
              ]
            }
        """.trimIndent()

        val reciters = QuranFoundationClient.parseChapterReciters(json)
        assertEquals(2, reciters.size)
        assertEquals(3, reciters[0].id)
        assertEquals("Murattal", reciters[0].style)
        assertEquals("Hafs", reciters[0].qirat)
        assertEquals("أبو بكر الشاطري", reciters[0].translatedName)
        assertEquals(null, reciters[1].style)
        assertEquals("Warsh", reciters[1].qirat)
    }

    @Test
    fun parsesAndOrdersAvailableChapterAudioFiles() {
        val json = """
            {
              "audio_files": [
                {"id": 2, "chapter_id": 2, "file_size": 2000, "format": "mp3", "audio_url": "https://audio.example/002.mp3"},
                {"id": 1, "chapter_id": 1, "file_size": 1000, "format": "mp3", "audio_url": "https://audio.example/001.mp3"},
                {"id": 3, "chapter_id": 2, "file_size": 3000, "format": "mp3", "audio_url": "https://audio.example/duplicate.mp3"},
                {"id": 4, "chapter_id": 115, "file_size": 4000, "format": "mp3", "audio_url": "https://audio.example/115.mp3"},
                {"id": 5, "chapter_id": 3, "file_size": 5000, "format": "mp3", "audio_url": "http://insecure.example/003.mp3"}
              ]
            }
        """.trimIndent()

        val files = QuranFoundationClient.parseChapterAudioFiles(json)
        assertEquals(2, files.size)
        assertEquals(1, files[0].chapterId)
        assertEquals(2, files[1].chapterId)
        assertEquals("https://audio.example/001.mp3", files[0].audioUrl)
        assertEquals("https://audio.example/002.mp3", files[1].audioUrl)
    }

    @Test
    fun parsesChapterAudioAndAbsoluteWordSegments() {
        val json = """
            {
              "audio_file": {
                "id": 457,
                "chapter_id": 1,
                "file_size": 710784,
                "format": "mp3",
                "audio_url": "https://download.example.org/audio/001.mp3",
                "duration": 53000,
                "timestamps": [
                  {
                    "verse_key": "1:1",
                    "timestamp_from": 0,
                    "timestamp_to": 6493,
                    "duration": 6493,
                    "segments": [[1, 120, 810], [2, 820, 1510], [0, 200, 300], [3, 7000, 7100]]
                  },
                  {
                    "verse_key": "1:2",
                    "timestamp_from": 6500,
                    "timestamp_to": 12000,
                    "segments": [[1, 6600, 7100]]
                  }
                ]
              }
            }
        """.trimIndent()

        val audio = QuranFoundationClient.parseChapterAudio(json)
        assertEquals(457, audio.id)
        assertEquals(1, audio.chapterId)
        assertEquals("https://download.example.org/audio/001.mp3", audio.audioUrl)
        assertEquals(53_000L, audio.durationMs)
        assertEquals(2, audio.timestamps.size)
        assertEquals("1:1", audio.timestamps[0].verseKey)
        assertEquals(2, audio.timestamps[0].segments.size)
        assertEquals(1, audio.timestamps[0].segments[0].wordIndex)
        assertEquals(120, audio.timestamps[0].segments[0].startMs)
        assertEquals(1510, audio.timestamps[0].segments[1].endMs)
        assertEquals("1:2", audio.timestamps[1].verseKey)
    }

    @Test
    fun resolvesActiveVerseAndWordFromAudioPosition() {
        val audio = QFChapterAudio(
            id = 1,
            chapterId = 1,
            audioUrl = "https://audio.example/001.mp3",
            durationMs = 12_000L,
            timestamps = listOf(
                QFVerseTiming(
                    verseKey = "1:1",
                    startMs = 0,
                    endMs = 6_000,
                    segments = listOf(
                        QFWordSegment(1, 100, 800),
                        QFWordSegment(2, 900, 1_500)
                    )
                ),
                QFVerseTiming(
                    verseKey = "1:2",
                    startMs = 6_000,
                    endMs = 12_000,
                    segments = listOf(QFWordSegment(1, 6_100, 6_800))
                )
            )
        )

        assertEquals(null, activeQuranPosition(audio, -1))
        assertEquals(ActiveQuranPosition("1:1", null), activeQuranPosition(audio, 50))
        assertEquals(ActiveQuranPosition("1:1", 1), activeQuranPosition(audio, 500))
        assertEquals(ActiveQuranPosition("1:1", 2), activeQuranPosition(audio, 1_000))
        assertEquals(ActiveQuranPosition("1:2", 1), activeQuranPosition(audio, 6_500))
        assertEquals(null, activeQuranPosition(audio, 12_000))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonHttpsAudioUrl() {
        QuranFoundationClient.parseChapterAudio(
            """{"audio_file":{"audio_url":"http://unsafe.example.org/1.mp3","timestamps":[]}}"""
        )
    }

    @Test
    fun proxyConfigurationRequiresHttps() {
        assertTrue(QuranFoundationClient("https://yaqeen.example.workers.dev").isConfigured)
        assertFalse(QuranFoundationClient("").isConfigured)
        assertFalse(QuranFoundationClient("http://insecure.example").isConfigured)
    }

    @Test
    fun offlineMuyassarParserReadsVerseAndFootnotes() {
        val json = """{"verses":{"1":{"1":{"text":"بسم الله","footnotes":"هامش موثق"}}}}"""
        val result = parseOfflineMuyassarTafsir(json, "1:1")
        assertEquals("1:1", result?.verseKey)
        assertEquals("التفسير الميسر", result?.resourceName)
        assertTrue(result?.text?.contains("بسم الله") == true)
        assertTrue(result?.text?.contains("هامش موثق") == true)
    }

    @Test
    fun offlineMuyassarParserRejectsMissingOrInvalidVerses() {
        val json = """{"verses":{"1":{"1":{"text":"بسم الله"}}}}"""
        assertEquals(null, parseOfflineMuyassarTafsir(json, "1:2"))
        assertEquals(null, parseOfflineMuyassarTafsir(json, "1/x"))
    }
}
