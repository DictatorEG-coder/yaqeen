package com.yaqeen.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal data class QuranRecording(
    val source: QuranAudioSource,
    val availableSurahs: List<Int>
)

internal object Mp3QuranCatalogClient {
    private const val ENDPOINT = "https://www.mp3quran.net/api/v3/reciters?language=ar"

    suspend fun fetchRecitations(): List<QuranRecording> = withContext(Dispatchers.IO) {
        val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 12_000
            connection.readTimeout = 18_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "YaqeenAndroid/0.1.0")
            check(connection.responseCode in 200..299) { "MP3Quran catalog request failed" }
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val reciters = JSONObject(body).optJSONArray("reciters") ?: return@withContext emptyList()
            val wanted = listOf(
                "المنشاوي", "عبد الباسط", "الحصري", "مصطفى إسماعيل",
                "محمود علي البنا", "محمد رفعت", "السديس", "سعود الشريم",
                "ماهر المعيقلي", "مشاري العفاسي", "محمد اللحيدان"
            )
            buildList {
                for (i in 0 until reciters.length()) {
                    val reciter = reciters.optJSONObject(i) ?: continue
                    val reciterName = reciter.optString("name").trim()
                    if (wanted.none { reciterName.contains(it) }) continue
                    val reciterId = reciter.optInt("id")
                    val moshaf = reciter.optJSONArray("moshaf") ?: continue
                    for (j in 0 until moshaf.length()) {
                        val item = moshaf.optJSONObject(j) ?: continue
                        val baseUrl = item.optString("server").trim()
                        if (!baseUrl.startsWith("https://")) continue
                        val moshafId = item.optInt("id")
                        val styleName = item.optString("name", "تلاوة")
                        val style = when {
                            styleName.contains("Mujawwad", true) || styleName.contains("مجود") -> "مجود"
                            styleName.contains("Murattal", true) || styleName.contains("مرتل") -> "مرتل"
                            else -> styleName
                        }
                        val available = item.optString("surah_list")
                            .split(',')
                            .mapNotNull { it.trim().toIntOrNull() }
                            .filter { it in 1..114 }
                            .distinct()
                            .sorted()
                        if (available.isEmpty()) continue
                        add(
                            QuranRecording(
                                QuranAudioSource("mp3quran_${reciterId}_${moshafId}", reciterName, style, baseUrl),
                                available
                            )
                        )
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
