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
}
