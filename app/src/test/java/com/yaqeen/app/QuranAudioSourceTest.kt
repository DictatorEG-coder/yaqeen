package com.yaqeen.app

import org.junit.Assert.assertEquals
import org.junit.Test

class QuranAudioSourceTest {

    @Test
    fun buildsCorrectMinshawiMurattalUrlsForBoundarySurahs() {
        val base = "https://download.quranicaudio.com/quran/muhammad_siddeeq_al-minshaawee/"
        assertEquals(base + "001.mp3", minshawiMurattalUrl(1))
        assertEquals(base + "047.mp3", minshawiMurattalUrl(47))
        assertEquals(base + "114.mp3", minshawiMurattalUrl(114))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsSurahNumbersOutsideTheQuran() {
        minshawiMurattalUrl(115)
    }

    @Test
    fun verifiedReciterSourcesUseTheirExpectedCdnPaths() {
        assertEquals(
            "https://download.quranicaudio.com/quran/abdul_basit_murattal/047.mp3",
            quranAudioUrl("abdulbaset_murattal", 47)
        )
        assertEquals(
            "https://download.quranicaudio.com/quran/abdulbaset_mujawwad/047.mp3",
            quranAudioUrl("abdulbaset_mujawwad", 47)
        )
        assertEquals(
            "https://download.quranicaudio.com/quran/mahmood_khaleel_al-husaree_iza3a/047.mp3",
            quranAudioUrl("husary_radio", 47)
        )
        assertEquals(
            "https://download.quranicaudio.com/quran/mostafa_ismaeel/047.mp3",
            quranAudioUrl("mustafa_ismail", 47)
        )
        assertEquals(
            "https://download.quranicaudio.com/quran/abdurrahmaan_as-sudays/047.mp3",
            quranAudioUrl("sudais", 47)
        )
        assertEquals(
            "https://download.quranicaudio.com/quran/sa3ood_al-shuraym/047.mp3",
            quranAudioUrl("shuraim", 47)
        )
        assertEquals(
            "https://download.quranicaudio.com/quran/mishaari_raashid_al_3afaasee/047.mp3",
            quranAudioUrl("afasy", 47)
        )
        assertEquals(
            "https://cdn.mp3quran.net/audio/muhammad-luhaidan/r1/047.mp3",
            quranAudioUrl("luhaidan", 47)
        )
        assertEquals(9, quranAudioSources.size)
    }

    @Test
    fun buildsDownloadUrlsForDynamicMp3QuranCatalogSources() {
        val hafs = QuranAudioSource(
            "mp3quran_41_7",
            "قارئ تجريبي",
            "حفص عن عاصم",
            "https://server.example/audio"
        )
        assertEquals(
            "https://server.example/audio/001.mp3",
            quranAudioUrl(hafs, 1)
        )
        assertEquals(
            "https://server.example/audio/114.mp3",
            quranAudioUrl(hafs, 114)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownAudioSource() {
        quranAudioUrl("unknown", 47)
    }

    @Test
    fun preservesSurahNumberWhenSwitchingReciters() {
        assertEquals(47, surahNumberFromMediaId("minshawi_murattal-47"))
        assertEquals(1, surahNumberFromMediaId("abdulbaset_mujawwad-1"))
        assertEquals(114, surahNumberFromMediaId("afasy-114"))
        assertEquals(null, surahNumberFromMediaId("afasy-115"))
        assertEquals(null, surahNumberFromMediaId(null))
    }

    @Test
    fun convertsSleepTimerMinutesToMilliseconds() {
        assertEquals(900_000L, sleepTimerDelayMs(15))
        assertEquals(5_400_000L, sleepTimerDelayMs(90))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidSleepTimerDuration() {
        sleepTimerDelayMs(0)
    }
}
