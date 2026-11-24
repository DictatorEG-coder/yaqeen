package com.yaqeen.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranCuratedPlaylistTest {
    @Test
    fun includesMuhammadAlLuhaidanWithVerifiedMp3QuranBase() {
        val source = quranAudioSources.first { it.id == "luhaidan" }
        assertEquals("محمد اللحيدان", source.reciterName)
        assertEquals(
            "https://cdn.mp3quran.net/audio/muhammad-luhaidan/r1/019.mp3",
            quranAudioUrl("luhaidan", 19)
        )
    }

    @Test
    fun mixedSelectionsUseTheRequestedRecitersAndSurahs() {
        val playlist = curatedQuranPlaylists.first { it.id == "classic_mix" }
        assertEquals(
            listOf(
                CuratedQuranTrack("abdulbaset_murattal", 2),
                CuratedQuranTrack("abdulbaset_murattal", 12),
                CuratedQuranTrack("minshawi_murattal", 53),
                CuratedQuranTrack("minshawi_murattal", 50),
                CuratedQuranTrack("minshawi_murattal", 90),
                CuratedQuranTrack("luhaidan", 19)
            ),
            playlist.tracks
        )
    }

    @Test
    fun husaryJuzQadSamiContainsSurahsFiftyEightThroughSixtySixInOrder() {
        val playlist = curatedQuranPlaylists.first { it.id == "juz_qad_sami" }
        assertEquals(9, playlist.tracks.size)
        assertEquals((58..66).toList(), playlist.tracks.map { it.surahNumber })
        assertTrue(playlist.tracks.all { it.sourceId == "husary_radio" })
    }
}
