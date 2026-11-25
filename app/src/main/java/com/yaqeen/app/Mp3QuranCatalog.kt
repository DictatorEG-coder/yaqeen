package com.yaqeen.app

import android.content.Context
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
    private const val CACHE_FILE = "mp3quran_catalog.json"

    suspend fun loadCachedRecitations(context: Context): List<QuranRecording> =
        withContext(Dispatchers.IO) {
            val file = context.applicationContext.filesDir.resolve(CACHE_FILE)
            if (!file.isFile) emptyList()
            else runCatching { parseRecitations(file.readText(Charsets.UTF_8)) }.getOrDefault(emptyList())
        }

    suspend fun fetchRecitations(context: Context? = null): List<QuranRecording> =
        withContext(Dispatchers.IO) {
            val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 12_000
                connection.readTimeout = 18_000
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("User-Agent", "YaqeenAndroid/0.2.1")
                check(connection.responseCode in 200..299) { "MP3Quran catalog request failed" }
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val parsed = parseRecitations(body)
                check(parsed.isNotEmpty()) { "MP3Quran catalog returned no valid recitations" }
                context?.applicationContext?.filesDir?.resolve(CACHE_FILE)?.writeText(body, Charsets.UTF_8)
                parsed
            } finally {
                connection.disconnect()
            }
        }

    internal fun parseRecitations(body: String): List<QuranRecording> {
        val reciters = JSONObject(body).optJSONArray("reciters") ?: return emptyList()
        return buildList {
            for (i in 0 until reciters.length()) {
                val reciter = reciters.optJSONObject(i) ?: continue
                val reciterName = reciter.optString("name").trim()
                val reciterId = reciter.optInt("id", 0)
                if (reciterName.isBlank() || reciterId <= 0) continue
                val moshaf = reciter.optJSONArray("moshaf") ?: continue
                for (j in 0 until moshaf.length()) {
                    val item = moshaf.optJSONObject(j) ?: continue
                    val baseUrl = item.optString("server").trim()
                    if (!baseUrl.startsWith("https://", ignoreCase = true)) continue
                    val moshafId = item.optInt("id", j + 1)
                    val styleName = item.optString("name", "تلاوة").trim().ifBlank { "تلاوة" }
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
        }.distinctBy { it.source.id }
    }
}
