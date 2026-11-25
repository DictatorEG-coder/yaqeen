package com.yaqeen.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

internal data class OfflineHadithCollection(
    val id: String,
    val nameArabic: String,
    val totalHadiths: Int,
    val bookCount: Int
)

internal data class OfflineHadithBook(
    val number: String,
    val nameArabic: String,
    val totalHadiths: Int
)

internal data class OfflineHadith(
    val collectionId: String,
    val bookNumber: String,
    val chapterNumber: String?,
    val number: String,
    val reference: String?,
    val chapterNameArabic: String?,
    val textArabic: String,
    val narrator: String?,
    val gradeArabic: String?,
    val sourceReference: String?,
    val sourceUrl: String?
)

internal data class OfflineHadithPage(
    val items: List<OfflineHadith>,
    val total: Int,
    val offset: Int,
    val pageSize: Int
) {
    val hasMore: Boolean get() = offset + items.size < total
}

internal fun normalizeHadithSearchText(value: String): String =
    value.lowercase()
        .replace(Regex("[\\u064B-\\u065F\\u0670\\u0640\\u200B-\\u200F\\uFEFF]"), "")
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')
        .replace('ٱ', 'ا')
        .replace('ى', 'ي')
        .replace('ؤ', 'و')
        .replace('ئ', 'ي')
        .trim()

internal object OfflineHadithRepository {
    private const val DATABASE_NAME = "hadith_ar.db"
    private const val DATABASE_VERSION = 1

    @Synchronized
    private fun databaseFile(context: Context): File {
        val target = File(context.noBackupFilesDir, DATABASE_NAME)
        var needsCopy = !target.exists()
        if (!needsCopy) {
            val currentVersion = runCatching {
                SQLiteDatabase.openDatabase(target.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { it.version }
            }.getOrDefault(0)
            needsCopy = currentVersion < DATABASE_VERSION
        }
        if (needsCopy) {
            target.parentFile?.mkdirs()
            val temporary = File(target.parentFile, "$DATABASE_NAME.tmp")
            context.assets.open(DATABASE_NAME).use { input ->
                temporary.outputStream().use { output -> input.copyTo(output) }
            }
            if (target.exists()) target.delete()
            check(temporary.renameTo(target)) { "تعذّر تجهيز قاعدة الأحاديث المحلية." }
        }
        return target
    }

    private inline fun <T> withDatabase(context: Context, block: (SQLiteDatabase) -> T): T {
        val database = SQLiteDatabase.openDatabase(
            databaseFile(context).absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
        )
        return database.use(block)
    }

    fun collections(context: Context): List<OfflineHadithCollection> = withDatabase(context) { db ->
        db.rawQuery(
            """
            SELECT c.id, c.name_ar, c.total_hadiths,
                   (SELECT COUNT(*) FROM books b WHERE b.collection_id = c.id) AS book_count
            FROM collections c
            ORDER BY CASE c.collection_type WHEN 'primary' THEN 0 WHEN 'compilation' THEN 1 ELSE 2 END,
                     c.name_ar
            """.trimIndent(),
            null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(
                    OfflineHadithCollection(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getInt(2),
                        cursor.getInt(3)
                    )
                )
            }
        }
    }

    fun books(context: Context, collectionId: String): List<OfflineHadithBook> = withDatabase(context) { db ->
        db.rawQuery(
            "SELECT book_number, name_ar, total_hadiths FROM books WHERE collection_id = ? ORDER BY CAST(book_number AS INTEGER), book_number",
            arrayOf(collectionId)
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(
                    OfflineHadithBook(cursor.getString(0), cursor.getString(1), cursor.getInt(2))
                )
            }
        }
    }

    fun search(
        context: Context,
        collectionId: String,
        bookNumber: String?,
        query: String,
        limit: Int = 100,
        offset: Int = 0
    ): OfflineHadithPage = withDatabase(context) { db ->
        require(limit in 1..200)
        require(offset >= 0)
        val normalized = normalizeHadithSearchText(query)
        val conditions = mutableListOf("collection_id = ?")
        val args = mutableListOf(collectionId)
        if (bookNumber != null) {
            conditions += "book_number = ?"
            args += bookNumber
        }
        if (normalized.isNotBlank()) {
            conditions += "search_text LIKE ?"
            args += "%$normalized%"
        }
        val where = conditions.joinToString(" AND ")
        val total = db.rawQuery(
            "SELECT COUNT(*) FROM hadiths WHERE $where",
            args.toTypedArray()
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }

        val pageArgs = args + listOf(limit.toString(), offset.toString())
        val rows = db.rawQuery(
            """
            SELECT collection_id, book_number, chapter_number, hadith_number, reference,
                   chapter_name_ar, text_ar, narrator, grade_ar, source_reference, url_source
            FROM hadiths
            WHERE $where
            ORDER BY id
            LIMIT ? OFFSET ?
            """.trimIndent(),
            pageArgs.toTypedArray()
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(
                    OfflineHadith(
                        collectionId = cursor.getString(0),
                        bookNumber = cursor.getString(1),
                        chapterNumber = cursor.getString(2),
                        number = cursor.getString(3),
                        reference = cursor.getString(4),
                        chapterNameArabic = cursor.getString(5),
                        textArabic = cursor.getString(6),
                        narrator = cursor.getString(7),
                        gradeArabic = cursor.getString(8),
                        sourceReference = cursor.getString(9),
                        sourceUrl = cursor.getString(10)
                    )
                )
            }
        }
        OfflineHadithPage(rows, total, offset, limit)
    }
}
