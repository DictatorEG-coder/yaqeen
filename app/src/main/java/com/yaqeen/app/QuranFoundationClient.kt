package com.yaqeen.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal data class QFChapterReciter(
    val id: Int,
    val name: String,
    val style: String?,
    val qirat: String?,
    val translatedName: String?
)

internal data class QFWordSegment(
    val wordIndex: Int,
    val startMs: Int,
    val endMs: Int
)

internal data class QFVerseTiming(
    val verseKey: String,
    val startMs: Int,
    val endMs: Int,
    val segments: List<QFWordSegment>
)

internal data class QFChapterAudio(
    val id: Int,
    val chapterId: Int,
    val audioUrl: String,
    val durationMs: Long?,
    val timestamps: List<QFVerseTiming>
)

internal class QuranFoundationClient(
    baseUrl: String = BuildConfig.QF_PROXY_BASE_URL
) {
    private val normalizedBaseUrl = baseUrl.trim().trimEnd('/')
    val isConfigured: Boolean
        get() = normalizedBaseUrl.startsWith("https://")

    suspend fun fetchChapterReciters(): List<QFChapterReciter> {
        val body = getJson("/api/quran/resources/chapter_reciters?language=ar")
        return parseChapterReciters(body)
    }

    suspend fun fetchChapterAudio(
        reciterId: Int,
        chapterNumber: Int,
        includeSegments: Boolean = true
    ): QFChapterAudio {
        require(reciterId > 0) { "Reciter ID must be positive" }
        require(chapterNumber in 1..114) { "Chapter number must be between 1 and 114" }
        val segments = if (includeSegments) "true" else "false"
        val body = getJson(
            "/api/quran/chapter_recitations/$reciterId/$chapterNumber?segments=$segments"
        )
        return parseChapterAudio(body)
    }

    private suspend fun getJson(path: String): String = withContext(Dispatchers.IO) {
        check(isConfigured) {
            "Quran Foundation proxy URL is not configured. Set Gradle property QF_PROXY_BASE_URL."
        }
        val connection = (URL(normalizedBaseUrl + path).openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "YaqeenAndroid/0.1.0")

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw IOException("Quran Foundation proxy returned HTTP $status")
            }
            responseBody
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        internal fun parseChapterReciters(json: String): List<QFChapterReciter> {
            val root = JSONObject(json)
            val array = root.optJSONArray("reciters")
                ?: throw IllegalArgumentException("Missing reciters array")
            return buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optInt("id", 0)
                    val name = item.optString("name", "").trim()
                    if (id <= 0 || name.isEmpty()) continue
                    add(
                        QFChapterReciter(
                            id = id,
                            name = name,
                            style = item.optJSONObject("style")?.optString("name")?.takeIf { it.isNotBlank() },
                            qirat = item.optJSONObject("qirat")?.optString("name")?.takeIf { it.isNotBlank() },
                            translatedName = item.optJSONObject("translated_name")
                                ?.optString("name")?.takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        }

        internal fun parseChapterAudio(json: String): QFChapterAudio {
            val root = JSONObject(json)
            val audio = root.optJSONObject("audio_file")
                ?: throw IllegalArgumentException("Missing audio_file object")
            val audioUrl = audio.optString("audio_url", "").trim()
            require(audioUrl.startsWith("https://")) { "Audio URL must use HTTPS" }

            val timestampsJson = audio.optJSONArray("timestamps") ?: JSONArray()
            val timestamps = buildList {
                for (index in 0 until timestampsJson.length()) {
                    val item = timestampsJson.optJSONObject(index) ?: continue
                    val verseKey = item.optString("verse_key", "")
                    if (!verseKey.matches(Regex("^\\d{1,3}:\\d{1,3}$"))) continue

                    val startMs = item.optInt("timestamp_from", -1)
                    val endMs = item.optInt("timestamp_to", -1)
                    if (startMs < 0 || endMs <= startMs) continue

                    val segmentsJson = item.optJSONArray("segments") ?: JSONArray()
                    val segments = buildList {
                        for (segmentIndex in 0 until segmentsJson.length()) {
                            val triplet = segmentsJson.optJSONArray(segmentIndex) ?: continue
                            if (triplet.length() != 3) continue
                            val wordIndex = triplet.optInt(0, 0)
                            val wordStartMs = triplet.optInt(1, -1)
                            val wordEndMs = triplet.optInt(2, -1)
                            if (wordIndex > 0 && wordStartMs >= startMs &&
                                wordEndMs > wordStartMs && wordEndMs <= endMs
                            ) {
                                add(QFWordSegment(wordIndex, wordStartMs, wordEndMs))
                            }
                        }
                    }.sortedBy { it.startMs }

                    add(QFVerseTiming(verseKey, startMs, endMs, segments))
                }
            }.sortedBy { it.startMs }

            val duration = if (audio.isNull("duration")) null else audio.optLong("duration").takeIf { it > 0L }
            return QFChapterAudio(
                id = audio.optInt("id", 0),
                chapterId = audio.optInt("chapter_id", 0),
                audioUrl = audioUrl,
                durationMs = duration,
                timestamps = timestamps
            )
        }
    }
}
