package com.yaqeen.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer

private val QuranInk = Color(0xFF090E0B)
private val QuranPanel = Color(0xFF141D18)
private val QuranEmerald = Color(0xFF287A59)
private val QuranGold = Color(0xFFD7C18A)
private val QuranMuted = Color(0xFFAAB7AE)
private val QuranWhite = Color(0xFFF1F3EF)
private const val QURAN_PREFS = "yaqeen_quran_reader"

private data class QuranVerse(val number: Int, val text: String)
private data class QuranSurah(val number: Int, val name: String, val verses: List<QuranVerse>)

// Standard verse totals, cross-checked against the Quran.com surah index.
private val expectedVerseCounts = listOf(
    7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128,
    111, 110, 98, 135, 112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30,
    73, 54, 45, 83, 182, 88, 75, 85, 54, 53, 89, 59, 37, 35, 38, 29, 18,
    45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12, 12,
    30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19, 36,
    25, 22, 17, 19, 26, 30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11, 11,
    8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6
)

private val surahNames = listOf(
    "الفاتحة", "البقرة", "آل عمران", "النساء", "المائدة", "الأنعام", "الأعراف", "الأنفال",
    "التوبة", "يونس", "هود", "يوسف", "الرعد", "إبراهيم", "الحجر", "النحل", "الإسراء",
    "الكهف", "مريم", "طه", "الأنبياء", "الحج", "المؤمنون", "النور", "الفرقان", "الشعراء",
    "النمل", "القصص", "العنكبوت", "الروم", "لقمان", "السجدة", "الأحزاب", "سبأ", "فاطر",
    "يس", "الصافات", "ص", "الزمر", "غافر", "فصلت", "الشورى", "الزخرف", "الدخان",
    "الجاثية", "الأحقاف", "محمد", "الفتح", "الحجرات", "ق", "الذاريات", "الطور", "النجم",
    "القمر", "الرحمن", "الواقعة", "الحديد", "المجادلة", "الحشر", "الممتحنة", "الصف",
    "الجمعة", "المنافقون", "التغابن", "الطلاق", "التحريم", "الملك", "القلم", "الحاقة",
    "المعارج", "نوح", "الجن", "المزمل", "المدثر", "القيامة", "الإنسان", "المرسلات",
    "النبأ", "النازعات", "عبس", "التكوير", "الانفطار", "المطففين", "الانشقاق", "البروج",
    "الطارق", "الأعلى", "الغاشية", "الفجر", "البلد", "الشمس", "الليل", "الضحى",
    "الشرح", "التين", "العلق", "القدر", "البينة", "الزلزلة", "العاديات", "القارعة",
    "التكاثر", "العصر", "الهمزة", "الفيل", "قريش", "الماعون", "الكوثر", "الكافرون",
    "النصر", "المسد", "الإخلاص", "الفلق", "الناس"
)

private fun loadQuran(context: android.content.Context): List<QuranSurah> {
    val verses = context.assets.open("quran-uthmani.txt").bufferedReader(Charsets.UTF_8).useLines { lines ->
        lines.mapNotNull { line ->
            val parts = line.split('|', limit = 3)
            if (parts.size != 3) return@mapNotNull null
            val surahNumber = parts[0].toIntOrNull() ?: return@mapNotNull null
            val verseNumber = parts[1].toIntOrNull() ?: return@mapNotNull null
            if (surahNumber !in 1..114 || verseNumber < 1 || parts[2].isBlank()) return@mapNotNull null
            QuranSurahVerse(surahNumber, QuranVerse(verseNumber, parts[2]))
        }.toList()
    }
    val grouped = verses.groupBy { it.surahNumber }
    return surahNames.mapIndexed { index, name ->
        val number = index + 1
        QuranSurah(number, name, grouped[number].orEmpty().map { it.verse }.sortedBy { it.number })
    }
}

private fun isCompleteQuran(surahs: List<QuranSurah>): Boolean {
    if (surahs.size != 114 || expectedVerseCounts.size != 114) return false
    if (surahs.sumOf { it.verses.size } != 6236) return false
    return surahs.withIndex().all { (index, surah) ->
        surah.number == index + 1 &&
            surah.verses.size == expectedVerseCounts[index] &&
            surah.verses.withIndex().all { (verseIndex, verse) -> verse.number == verseIndex + 1 } &&
            (surah.number == 1 || surah.number == 9 || openingBasmala(surah.verses.firstOrNull() ?: QuranVerse(0, "")) != null)
    }
}

private data class QuranSurahVerse(val surahNumber: Int, val verse: QuranVerse)

private fun openingBasmala(verse: QuranVerse): String? {
    val marker = "ٱلرَّحِيمِ"
    val markerStart = verse.text.indexOf(marker)
    if (markerStart < 0) return null
    val end = markerStart + marker.length
    if (end >= verse.text.length || !verse.text[end].isWhitespace()) return null
    return verse.text.substring(0, end)
}

private fun displayVerseText(surahNumber: Int, verse: QuranVerse): String {
    val isOpeningBasmala = surahNumber != 1 && surahNumber != 9 && verse.number == 1
    val basmala = if (isOpeningBasmala) openingBasmala(verse) else null
    return if (basmala != null) verse.text.removePrefix(basmala).trimStart() else verse.text
}

private fun normalizeArabic(value: String): String {
    val withoutMarks = Normalizer.normalize(value, Normalizer.Form.NFD)
        .filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() || it == 'ـ' }
    return withoutMarks
        .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
        .replace('ى', 'ي').replace('ؤ', 'و').replace('ئ', 'ي')
}

@Composable
fun QuranReaderScreen(
    modifier: Modifier = Modifier,
    showBookmarksInitially: Boolean = false,
    onBookmarksOpened: () -> Unit = {}
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var surahs by remember { mutableStateOf<List<QuranSurah>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var selectedSurah by remember { mutableStateOf<QuranSurah?>(null) }
    var selectedVerse by remember { mutableStateOf<Int?>(null) }
    var bookmarkedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showBookmarks by remember { mutableStateOf(showBookmarksInitially) }

    LaunchedEffect(showBookmarksInitially) {
        if (showBookmarksInitially) showBookmarks = true
    }

    LaunchedEffect(Unit) {
        try {
            bookmarkedKeys = context.getSharedPreferences(QURAN_PREFS, android.content.Context.MODE_PRIVATE)
                .getStringSet("bookmarks", emptySet())?.toSet() ?: emptySet()
            surahs = withContext(Dispatchers.IO) { loadQuran(context.applicationContext) }
            loadError = !isCompleteQuran(surahs)
            if (!loadError) {
                val prefs = context.getSharedPreferences(QURAN_PREFS, android.content.Context.MODE_PRIVATE)
                val savedSurahNumber = prefs.getInt("last_surah", 0)
                val savedVerseNumber = prefs.getInt("last_verse", 1)
                val savedSurah = surahs.firstOrNull { it.number == savedSurahNumber }
                if (savedSurah != null) {
                    selectedSurah = savedSurah
                    selectedVerse = savedVerseNumber.coerceIn(1, savedSurah.verses.size.coerceAtLeast(1))
                }
            }
        } catch (_: Exception) {
            loadError = true
        } finally {
            loading = false
        }
    }

    val normalizedQuery = remember(search) { normalizeArabic(search.trim()) }
    val verseResults = remember(normalizedQuery, surahs) {
        if (normalizedQuery.length < 2) emptyList()
        else surahs.flatMap { surah ->
            surah.verses.filter { normalizeArabic(it.text).contains(normalizedQuery) }
                .map { surah to it }
        }.take(100)
    }
    val matchingSurahs = remember(normalizedQuery, surahs) {
        if (normalizedQuery.isBlank() || normalizedQuery.length >= 2) surahs
        else surahs.filter { normalizeArabic(it.name).contains(normalizedQuery) }
    }

    val toggleBookmark: (QuranSurah, QuranVerse) -> Unit = { surah, verse ->
        val key = "${surah.number}:${verse.number}"
        val updated = bookmarkedKeys.toMutableSet()
        if (!updated.add(key)) updated.remove(key)
        bookmarkedKeys = updated
        context.getSharedPreferences(QURAN_PREFS, android.content.Context.MODE_PRIVATE)
            .edit()
            .putStringSet("bookmarks", updated)
            .apply()
    }
    val savedVerseEntries = remember(bookmarkedKeys, surahs) {
        bookmarkedKeys.mapNotNull { key ->
            val parts = key.split(":")
            if (parts.size != 2) null
            else {
                val surahNumber = parts[0].toIntOrNull()
                val verseNumber = parts[1].toIntOrNull()
                val surah = surahs.firstOrNull { it.number == surahNumber }
                val verse = surah?.verses?.firstOrNull { it.number == verseNumber }
                if (surah != null && verse != null) surah to verse else null
            }
        }.sortedWith(compareBy({ it.first.number }, { it.second.number }))
    }

    val current = selectedSurah
    LaunchedEffect(current?.number, selectedVerse) {
        current?.let { surah ->
            context.getSharedPreferences(QURAN_PREFS, android.content.Context.MODE_PRIVATE)
                .edit()
                .putInt("last_surah", surah.number)
                .putInt("last_verse", selectedVerse ?: 1)
                .apply()
        }
    }
    if (showBookmarks) {
        Column(modifier = modifier.fillMaxSize().background(QuranInk).padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showBookmarks = false; onBookmarksOpened() }) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "العودة", tint = QuranGold)
                }
                Text("المحفوظات القرآنية", color = QuranGold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            when {
                loading -> Text("جارٍ تجهيز المحفوظات…", color = QuranMuted, modifier = Modifier.padding(16.dp))
                loadError -> Text("تعذّر التحقق من المصحف، لذلك لم نعرض مواضع محفوظة.", color = QuranGold, modifier = Modifier.padding(16.dp))
                savedVerseEntries.isEmpty() -> Text("لم تحفظ أي آية بعد. يمكنك حفظ الآيات من داخل المصحف.", color = QuranMuted, modifier = Modifier.padding(16.dp))
                else -> LazyColumn {
                    items(savedVerseEntries, key = { "${it.first.number}:${it.second.number}" }) { entry ->
                        val surah = entry.first
                        val verse = entry.second
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                .clickable {
                                    selectedSurah = surah
                                    selectedVerse = verse.number
                                    showBookmarks = false
                                    onBookmarksOpened()
                                },
                            colors = CardDefaults.cardColors(containerColor = QuranPanel),
                            shape = RoundedCornerShape(15.dp)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text("${surah.name} · الآية ${verse.number}", color = QuranGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(7.dp))
                                Text(displayVerseText(surah.number, verse), color = QuranWhite, fontSize = 20.sp, lineHeight = 36.sp)
                            }
                        }
                    }
                }
            }
        }
        return
    }

    if (current != null) {
        val listState = rememberLazyListState()
        LaunchedEffect(current.number, selectedVerse) {
            selectedVerse?.let { verseNumber ->
                val hasOpeningBasmala = current.number != 1 && current.number != 9
                val itemIndex = verseNumber - 1 + if (hasOpeningBasmala) 1 else 0
                if (current.verses.isNotEmpty()) {
                    listState.scrollToItem(itemIndex.coerceIn(0, current.verses.lastIndex + if (hasOpeningBasmala) 1 else 0))
                }
            }
        }
        Column(
            modifier = modifier.fillMaxSize().background(QuranInk).padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedSurah = null; selectedVerse = null }) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "العودة إلى السور", tint = QuranGold)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(current.name, color = QuranGold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("سورة رقم ${current.number} · ${current.verses.size} آية", color = QuranMuted, fontSize = 11.sp)
                }
                IconButton(onClick = { showBookmarks = true }) {
                    Icon(Icons.Outlined.BookmarkBorder, contentDescription = "المحفوظات", tint = QuranGold)
                }
            }
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                if (current.number != 1 && current.number != 9) {
                    openingBasmala(current.verses.firstOrNull() ?: QuranVerse(0, ""))?.let { basmala ->
                        item {
                            Text(
                                basmala,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                color = QuranGold,
                                fontSize = 25.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                items(current.verses, key = { "${current.number}:${it.number}" }) { verse ->
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .background(
                                if (selectedVerse == verse.number) QuranEmerald.copy(alpha = 0.22f) else QuranPanel,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            displayVerseText(current.number, verse),
                            modifier = Modifier.fillMaxWidth().clickable { selectedVerse = verse.number },
                            color = QuranWhite,
                            fontSize = 25.sp,
                            lineHeight = 48.sp,
                            textAlign = TextAlign.Start
                        )
                        Spacer(Modifier.height(9.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(27.dp).background(QuranEmerald, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(verse.number.toString(), color = Color.White, fontSize = 11.sp)
                            }
                            IconButton(onClick = { toggleBookmark(current, verse) }) {
                                Icon(
                                    if ("${current.number}:${verse.number}" in bookmarkedKeys) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = "حفظ الآية",
                                    tint = QuranGold
                                )
                            }
                        }
                    }
                }
                item {
                    Text(
                        "النص بالرسم العثماني من مشروع تنزيل (Tanzil).",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp)
                            .clickable { uriHandler.openUri("https://tanzil.net") },
                        color = QuranMuted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    Column(modifier = modifier.fillMaxSize().background(QuranInk).padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("القرآن الكريم", color = QuranGold, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showBookmarks = true }) {
                Icon(Icons.Outlined.BookmarkBorder, contentDescription = "المحفوظات", tint = QuranGold)
            }
        }
        Text("مصحف كامل · ١١٤ سورة · ٦٢٣٦ آية", modifier = Modifier.padding(top = 5.dp, bottom = 16.dp), color = QuranMuted, fontSize = 12.sp)
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("ابحث باسم السورة أو جزء من آية", color = QuranMuted) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = QuranGold) },
            trailingIcon = {
                if (search.isNotEmpty()) IconButton(onClick = { search = "" }) {
                    Icon(Icons.Outlined.Close, contentDescription = "مسح البحث", tint = QuranMuted)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = QuranWhite,
                unfocusedTextColor = QuranWhite,
                focusedBorderColor = QuranEmerald,
                unfocusedBorderColor = QuranMuted.copy(alpha = 0.45f),
                cursorColor = QuranGold
            ),
            shape = RoundedCornerShape(15.dp)
        )
        Spacer(Modifier.height(12.dp))
        when {
            loading -> Text("جارٍ تجهيز المصحف…", modifier = Modifier.padding(16.dp), color = QuranMuted)
            loadError -> Text("تعذّر التحقق من اكتمال ملف المصحف. لم نعرض محتوى غير مكتمل.", modifier = Modifier.padding(16.dp), color = QuranGold)
            normalizedQuery.length >= 2 -> {
                Text("نتائج البحث: ${verseResults.size}", modifier = Modifier.padding(vertical = 8.dp), color = QuranMuted, fontSize = 12.sp)
                LazyColumn {
                    items(verseResults, key = { "${it.first.number}:${it.second.number}" }) { result ->
                        val surah = result.first
                        val verse = result.second
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                .clickable { selectedSurah = surah; selectedVerse = verse.number },
                            colors = CardDefaults.cardColors(containerColor = QuranPanel),
                            shape = RoundedCornerShape(15.dp)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text("${surah.name} · الآية ${verse.number}", color = QuranGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(7.dp))
                                if (surah.number != 1 && surah.number != 9 && verse.number == 1) {
                                    openingBasmala(verse)?.let { basmala ->
                                        Text(basmala, color = QuranGold, fontSize = 18.sp, lineHeight = 30.sp)
                                        Spacer(Modifier.height(4.dp))
                                    }
                                }
                                Text(displayVerseText(surah.number, verse), color = QuranWhite, fontSize = 20.sp, lineHeight = 36.sp)
                            }
                        }
                    }
                }
            }
            else -> {
                LazyColumn {
                    items(matchingSurahs, key = { it.number }) { surah ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                .clickable { selectedSurah = surah },
                            colors = CardDefaults.cardColors(containerColor = QuranPanel),
                            shape = RoundedCornerShape(15.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(15.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(37.dp).background(QuranEmerald.copy(alpha = 0.25f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) { Text(surah.number.toString(), color = QuranGold, fontSize = 12.sp) }
                                    Spacer(Modifier.size(12.dp))
                                    Column {
                                        Text(surah.name, color = QuranWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                        Text("سورة رقم ${surah.number}", color = QuranMuted, fontSize = 10.sp)
                                    }
                                }
                                Text("${surah.verses.size} آية", color = QuranMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
