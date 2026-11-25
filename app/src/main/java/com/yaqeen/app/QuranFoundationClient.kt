package com.yaqeen.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal data class QFTafsirResource(
    val id: Int,
    val name: String,
    val authorName: String?,
    val languageName: String
)

internal data class QFTafsirEntry(
    val verseKey: String,
    val resourceName: String,
    val authorName: String?,
    val text: String
)

internal fun parseOfflineMuyassarTafsir(json: String, verseKey: String): QFTafsirEntry? =
    runCatching { parseOfflineMuyassarTafsir(JSONObject(json), verseKey) }.getOrNull()

private fun parseOfflineMuyassarTafsir(root: JSONObject, verseKey: String): QFTafsirEntry? {
    if (!verseKey.matches(Regex("^\\d{1,3}:\\d{1,3}$"))) return null
    val parts = verseKey.split(":")
    val verse = root.optJSONObject("verses")
        ?.optJSONObject(parts[0])?.optJSONObject(parts[1]) ?: return null
    val rawText = verse.optString("text").takeIf { it.isNotBlank() && it != "null" } ?: return null
    val rawFootnotes = verse.optString("footnotes").takeIf { it.isNotBlank() && it != "null" }
    val completeText = listOfNotNull(rawText, rawFootnotes).joinToString("\n\n")
    return QFTafsirEntry(verseKey, "التفسير الميسر", "QuranEnc.com · الإصدار 1.0.0", completeText)
}

internal object OfflineTafsirRepository {
    @Volatile private var cachedRoot: JSONObject? = null

    fun find(context: Context, verseKey: String): QFTafsirEntry? {
        val root = cachedRoot ?: synchronized(this) {
            cachedRoot ?: runCatching {
                val json = java.util.zip.GZIPInputStream(context.assets.open("arabic_moyassar.json.gz"))
                    .bufferedReader(Charsets.UTF_8).use { it.readText() }
                JSONObject(json)
            }.getOrNull()?.also { cachedRoot = it }
        } ?: return null
        return runCatching { parseOfflineMuyassarTafsir(root, verseKey) }.getOrNull()
    }
}

internal data class QFChapterReciter(
    val id: Int,
    val name: String,
    val style: String?,
    val qirat: String?,
    val translatedName: String?
)

internal data class QFChapterAudioFile(
    val id: Int,
    val chapterId: Int,
    val audioUrl: String,
    val fileSize: Long,
    val format: String
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

internal data class ActiveQuranPosition(
    val verseKey: String,
    val wordIndex: Int?
)

internal data class QFChapterAudio(
    val id: Int,
    val chapterId: Int,
    val audioUrl: String,
    val durationMs: Long?,
    val timestamps: List<QFVerseTiming>
)

internal fun activeQuranPosition(
    audio: QFChapterAudio,
    positionMs: Long
): ActiveQuranPosition? {
    if (positionMs < 0L) return null
    val verse = audio.timestamps.firstOrNull {
        positionMs >= it.startMs && positionMs < it.endMs
    } ?: return null
    val word = verse.segments.firstOrNull {
        positionMs >= it.startMs && positionMs < it.endMs
    }
    return ActiveQuranPosition(verse.verseKey, word?.wordIndex)
}

internal class QuranFoundationClient(
    baseUrl: String = BuildConfig.QF_PROXY_BASE_URL
) {
    private val normalizedBaseUrl = baseUrl.trim().trimEnd('/')
    val isConfigured: Boolean
        get() = runCatching {
            val parsed = URL(normalizedBaseUrl)
            parsed.protocol.equals("https", ignoreCase = true) && parsed.host.isNotBlank()
        }.getOrDefault(false)

    suspend fun fetchArabicTafsirResources(): List<QFTafsirResource> {
        val body = getJson("/api/quran/resources/tafsirs?language=ar")
        return parseTafsirResources(body).filter { it.languageName.equals("arabic", true) }
    }

    suspend fun fetchTafsirForVerse(resourceId: Int, verseKey: String): QFTafsirEntry {
        require(resourceId > 0) { "Tafsir resource ID must be positive" }
        require(verseKey.matches(Regex("^\\d{1,3}:\\d{1,3}$"))) { "Invalid verse key" }
        val body = getJson("/api/quran/tafsirs/$resourceId/by_ayah/$verseKey")
        return parseTafsirForVerse(body, verseKey)
    }

    suspend fun fetchChapterReciters(): List<QFChapterReciter> {
        val body = getJson("/api/quran/resources/chapter_reciters?language=ar")
        return parseChapterReciters(body)
    }

    suspend fun fetchChapterAudioFiles(reciterId: Int): List<QFChapterAudioFile> {
        require(reciterId > 0) { "Reciter ID must be positive" }
        val body = getJson("/api/quran/chapter_recitations/$reciterId")
        return parseChapterAudioFiles(body)
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
        private fun JSONObject.readNonBlankString(key: String): String? {
            if (isNull(key)) return null
            return optString(key).trim().takeIf { it.isNotEmpty() && it != "null" }
        }

        internal fun parseTafsirResources(json: String): List<QFTafsirResource> {
            val array = JSONObject(json).optJSONArray("tafsirs")
                ?: throw IllegalArgumentException("Missing tafsirs array")
            return buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optInt("id", 0)
                    val name = item.optString("name", "").trim()
                    val language = item.optString("language_name", "").trim()
                    if (id > 0 && name.isNotBlank() && language.isNotBlank()) {
                        add(
                            QFTafsirResource(
                                id,
                                name,
                                item.optString("author_name").takeIf { it.isNotBlank() && it != "null" },
                                language
                            )
                        )
                    }
                }
            }
        }

        internal fun parseTafsirForVerse(json: String, requestedVerseKey: String): QFTafsirEntry {
            val root = JSONObject(json)
            val tafsirObject = root.optJSONObject("tafsir") ?: root
            val verses = tafsirObject.optJSONObject("verses")
                ?: throw IllegalArgumentException("Missing tafsir verses")
            val record = verses.optJSONObject(requestedVerseKey)
                ?: throw IllegalArgumentException("Tafsir for requested verse is missing")
            val text = record.optString("text", "").trim()
            require(text.isNotBlank()) { "Tafsir text is empty" }
            val translated = record.optJSONObject("translated_name")
            val resourceName = translated?.optString("name")?.takeIf { it.isNotBlank() && it != "null" }
                ?: record.optString("resource_name", "التفسير")
            val authorName = record.optString("author_name").takeIf { it.isNotBlank() && it != "null" }
            return QFTafsirEntry(requestedVerseKey, resourceName, authorName, text)
        }

        internal fun parseChapterReciters(json: String): List<QFChapterReciter> {
            val root = JSONObject(json)
            val array = root.optJSONArray("reciters")
                ?: throw IllegalArgumentException("Missing reciters array")
            return buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optInt("id", 0)
                    val name = item.readNonBlankString("name")
                    if (id <= 0 || name == null) continue
                    add(
                        QFChapterReciter(
                            id = id,
                            name = name,
                            style = item.optJSONObject("style")?.readNonBlankString("name"),
                            qirat = item.optJSONObject("qirat")?.readNonBlankString("name"),
                            translatedName = item.optJSONObject("translated_name")
                                ?.readNonBlankString("name")
                        )
                    )
                }
            }
        }

        internal fun parseChapterAudioFiles(json: String): List<QFChapterAudioFile> {
            val root = JSONObject(json)
            val array = root.optJSONArray("audio_files")
                ?: throw IllegalArgumentException("Missing audio_files array")
            return buildList {
                val seenChapters = mutableSetOf<Int>()
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optInt("id", 0)
                    val chapterId = item.optInt("chapter_id", 0)
                    val audioUrl = item.optString("audio_url", "").trim()
                    val fileSize = item.optLong("file_size", 0L)
                    val format = item.optString("format", "").trim()
                    if (id <= 0 || chapterId !in 1..114 || fileSize <= 0L ||
                        !audioUrl.startsWith("https://") || format.isBlank() ||
                        !seenChapters.add(chapterId)
                    ) continue
                    add(QFChapterAudioFile(id, chapterId, audioUrl, fileSize, format))
                }
            }.sortedBy { it.chapterId }
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
