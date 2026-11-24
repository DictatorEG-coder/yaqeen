package com.yaqeen.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal data class SunnahCollection(
    val name: String,
    val hasBooks: Boolean,
    val hasChapters: Boolean,
    val arabicTitle: String?,
    val englishTitle: String?,
    val totalHadith: Int,
    val totalAvailableHadith: Int
) {
    val isComplete: Boolean
        get() = totalHadith > 0 && totalAvailableHadith == totalHadith
}

internal data class SunnahBook(
    val bookNumber: String,
    val arabicName: String?,
    val englishName: String?,
    val hadithStartNumber: Int,
    val hadithEndNumber: Int,
    val numberOfHadith: Int
)

internal data class SunnahGrade(
    val gradedBy: String,
    val grade: String
)

internal data class SunnahHadithText(
    val language: String,
    val chapterNumber: String?,
    val chapterTitle: String?,
    val urn: Int?,
    val body: String
)

internal data class SunnahHadith(
    val collection: String,
    val bookNumber: String,
    val chapterId: String?,
    val hadithNumber: String,
    val texts: List<SunnahHadithText>,
    val grades: List<SunnahGrade>
) {
    val arabicText: String?
        get() = texts.firstOrNull { it.language == "ar" }?.body

    val englishText: String?
        get() = texts.firstOrNull { it.language == "en" }?.body
}

internal data class SunnahHadithReference(
    val collection: String,
    val hadithNumber: String
)

internal data class SunnahHadithComparison(
    val hadiths: List<SunnahHadith>,
    val missingReferences: List<String>
)

internal data class SunnahPage<T>(
    val data: List<T>,
    val total: Int,
    val limit: Int,
    val previousPage: Int?,
    val nextPage: Int?
)

internal data class SunnahMetadataCache(
    val bukhari: SunnahCollection,
    val muslim: SunnahCollection,
    val bukhariBooks: List<SunnahBook>,
    val muslimBooks: List<SunnahBook>
)

internal object SunnahApiMemoryCache {
    private const val TTL_MS = 6 * 60 * 60 * 1000L
    private var metadata: Pair<Long, SunnahMetadataCache>? = null
    private val hadithPages = mutableMapOf<String, Pair<Long, SunnahPage<SunnahHadith>>>()

    @Synchronized
    fun getMetadata(): SunnahMetadataCache? {
        val cached = metadata ?: return null
        return if (System.currentTimeMillis() - cached.first < TTL_MS) cached.second else null
    }

    @Synchronized
    fun putMetadata(value: SunnahMetadataCache) {
        metadata = System.currentTimeMillis() to value
    }

    @Synchronized
    fun getHadithPage(collection: String, bookNumber: String, page: Int): SunnahPage<SunnahHadith>? {
        val key = "$collection/$bookNumber/$page"
        val cached = hadithPages[key] ?: return null
        return if (System.currentTimeMillis() - cached.first < TTL_MS) cached.second else null
    }

    @Synchronized
    fun putHadithPage(
        collection: String,
        bookNumber: String,
        page: Int,
        value: SunnahPage<SunnahHadith>
    ) {
        val key = "$collection/$bookNumber/$page"
        hadithPages[key] = System.currentTimeMillis() to value
    }
}

internal class SunnahApiClient(
    baseUrl: String = BuildConfig.QF_PROXY_BASE_URL
) {
    private val normalizedBaseUrl = baseUrl.trim().trimEnd('/')

    val isConfigured: Boolean
        get() = runCatching {
            val parsed = URL(normalizedBaseUrl)
            parsed.protocol.equals("https", ignoreCase = true) && parsed.host.isNotBlank()
        }.getOrDefault(false)

    suspend fun fetchCollection(collection: String): SunnahCollection {
        val slug = validateCollection(collection)
        return parseCollection(getJson("/api/sunnah/collections/$slug"))
    }

    suspend fun fetchBooks(
        collection: String,
        page: Int = 1,
        limit: Int = 100
    ): SunnahPage<SunnahBook> {
        val slug = validateCollection(collection)
        validatePagination(page, limit)
        return parseBooks(
            getJson("/api/sunnah/collections/$slug/books?page=$page&limit=$limit")
        )
    }

    suspend fun fetchBookHadiths(
        collection: String,
        bookNumber: String,
        page: Int = 1,
        limit: Int = 100
    ): SunnahPage<SunnahHadith> {
        val slug = validateCollection(collection)
        require(bookNumber.matches(Regex("^[0-9]+$"))) { "Invalid book number" }
        validatePagination(page, limit)
        val encodedBook = URLEncoder.encode(bookNumber, StandardCharsets.UTF_8.name())
        return parseHadithPage(
            getJson("/api/sunnah/collections/$slug/books/$encodedBook/hadiths?page=$page&limit=$limit")
        )
    }

    suspend fun fetchHadithReferences(references: List<SunnahHadithReference>): SunnahHadithComparison {
        require(references.isNotEmpty() && references.size <= 20) { "Provide between 1 and 20 references" }
        val refs = references.joinToString(",") { reference ->
            val collection = validateCollection(reference.collection)
            require(reference.hadithNumber.matches(Regex("^[0-9]+$"))) { "Invalid hadith number" }
            "$collection:${reference.hadithNumber}"
        }
        val encodedRefs = URLEncoder.encode(refs, StandardCharsets.UTF_8.name())
        return parseHadithComparison(getJson("/api/sunnah/hadiths/refs?refs=$encodedRefs"))
    }

    suspend fun fetchHadith(collection: String, hadithNumber: String): SunnahHadith {
        val slug = validateCollection(collection)
        require(hadithNumber.matches(Regex("^[0-9]+$"))) { "Invalid hadith number" }
        val encodedNumber = URLEncoder.encode(hadithNumber, StandardCharsets.UTF_8.name())
        return parseHadith(getJson("/api/sunnah/collections/$slug/hadiths/$encodedNumber"))
    }

    private fun validateCollection(collection: String): String {
        val normalized = collection.trim().lowercase()
        require(normalized == "bukhari" || normalized == "muslim") {
            "Only Sahih al-Bukhari and Sahih Muslim are enabled"
        }
        return normalized
    }

    private fun validatePagination(page: Int, limit: Int) {
        require(page >= 1) { "Page must be at least 1" }
        require(limit in 1..100) { "Page size must be between 1 and 100" }
    }

    private suspend fun getJson(path: String): String = withContext(Dispatchers.IO) {
        check(isConfigured) {
            "Sunnah.com proxy URL is not configured. Set QF_PROXY_BASE_URL."
        }
        val connection = URL(normalizedBaseUrl + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "YaqeenAndroid/0.1.0")

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw IOException("Sunnah.com proxy returned HTTP $status")
            }
            body
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private fun JSONObject.localizedName(arrayKey: String, language: String): String? {
            val array = optJSONArray(arrayKey) ?: return null
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                if (item.optString("lang") == language) {
                    return item.optString("title", item.optString("name", ""))
                        .takeIf { it.isNotBlank() && it != "null" }
                }
            }
            return null
        }

        internal fun parseCollection(json: String): SunnahCollection {
            val item = JSONObject(json)
            val name = item.optString("name", "")
            require(name.isNotBlank()) { "Missing collection name" }
            return SunnahCollection(
                name = name,
                hasBooks = item.optBoolean("hasBooks", false),
                hasChapters = item.optBoolean("hasChapters", false),
                arabicTitle = item.localizedName("collection", "ar"),
                englishTitle = item.localizedName("collection", "en"),
                totalHadith = item.optInt("totalHadith", 0),
                totalAvailableHadith = item.optInt("totalAvailableHadith", 0)
            )
        }

        internal fun parseBooks(json: String): SunnahPage<SunnahBook> {
            val root = JSONObject(json)
            val array = root.optJSONArray("data") ?: JSONArray()
            val books = buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val bookNumber = item.optString("bookNumber", "")
                    if (bookNumber.isBlank()) continue
                    add(
                        SunnahBook(
                            bookNumber = bookNumber,
                            arabicName = item.localizedName("book", "ar"),
                            englishName = item.localizedName("book", "en"),
                            hadithStartNumber = item.optInt("hadithStartNumber", 0),
                            hadithEndNumber = item.optInt("hadithEndNumber", 0),
                            numberOfHadith = item.optInt("numberOfHadith", 0)
                        )
                    )
                }
            }
            return parsePage(root, books)
        }

        internal fun parseHadithPage(json: String): SunnahPage<SunnahHadith> {
            val root = JSONObject(json)
            val array = root.optJSONArray("data") ?: JSONArray()
            val hadiths = buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    runCatching { parseHadithObject(item) }.getOrNull()?.let(::add)
                }
            }
            return parsePage(root, hadiths)
        }

        internal fun parseHadithComparison(json: String): SunnahHadithComparison {
            val root = JSONObject(json)
            val data = root.optJSONArray("data") ?: JSONArray()
            val hadiths = buildList {
                for (index in 0 until data.length()) {
                    val item = data.optJSONObject(index) ?: continue
                    runCatching { parseHadithObject(item) }.getOrNull()?.let(::add)
                }
            }
            val missingJson = root.optJSONArray("missing") ?: JSONArray()
            val missing = buildList {
                for (index in 0 until missingJson.length()) {
                    val reference = missingJson.optString(index)
                    if (reference.isNotBlank() && reference != "null") add(reference)
                }
            }
            return SunnahHadithComparison(hadiths, missing)
        }

        internal fun parseHadith(json: String): SunnahHadith =
            parseHadithObject(JSONObject(json))

        private fun parseHadithObject(item: JSONObject): SunnahHadith {
            val collection = item.optString("collection", "")
            val bookNumber = item.optString("bookNumber", "")
            val hadithNumber = item.optString("hadithNumber", "")
            require(collection.isNotBlank() && bookNumber.isNotBlank() && hadithNumber.isNotBlank()) {
                "Hadith reference is incomplete"
            }

            val textsJson = item.optJSONArray("hadith") ?: JSONArray()
            val texts = buildList {
                for (index in 0 until textsJson.length()) {
                    val text = textsJson.optJSONObject(index) ?: continue
                    val language = text.optString("lang", "")
                    val body = text.optString("body", "")
                    if (language.isNotBlank() && body.isNotBlank()) {
                        add(
                            SunnahHadithText(
                                language = language,
                                chapterNumber = text.optString("chapterNumber").takeIf { it.isNotBlank() && it != "null" },
                                chapterTitle = text.optString("chapterTitle").takeIf { it.isNotBlank() && it != "null" },
                                urn = if (text.isNull("urn")) null else text.optInt("urn").takeIf { it > 0 },
                                body = body
                            )
                        )
                    }
                }
            }

            val gradesJson = item.optJSONArray("grades") ?: JSONArray()
            val grades = buildList {
                for (index in 0 until gradesJson.length()) {
                    val grade = gradesJson.optJSONObject(index) ?: continue
                    val gradedBy = grade.optString("graded_by", "")
                    val value = grade.optString("grade", "")
                    if (gradedBy.isNotBlank() && value.isNotBlank()) add(SunnahGrade(gradedBy, value))
                }
            }

            return SunnahHadith(
                collection = collection,
                bookNumber = bookNumber,
                chapterId = item.optString("chapterId").takeIf { it.isNotBlank() && it != "null" },
                hadithNumber = hadithNumber,
                texts = texts,
                grades = grades
            )
        }

        private fun <T> parsePage(root: JSONObject, data: List<T>): SunnahPage<T> =
            SunnahPage(
                data = data,
                total = root.optInt("total", data.size),
                limit = root.optInt("limit", data.size),
                previousPage = if (root.isNull("previous")) null else root.optInt("previous").takeIf { it > 0 },
                nextPage = if (root.isNull("next")) null else root.optInt("next").takeIf { it > 0 }
            )
    }
}
