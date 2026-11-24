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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
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

private data class QuranVerse(val number: Int, val text: String)
private data class QuranSurah(val number: Int, val name: String, val verses: List<QuranVerse>)

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
            QuranSurahVerse(surahNumber, QuranVerse(verseNumber, parts[2]))
        }.toList()
    }
    val grouped = verses.groupBy { it.surahNumber }
    return surahNames.mapIndexed { index, name ->
        val number = index + 1
        QuranSurah(number, name, grouped[number].orEmpty().map { it.verse }.sortedBy { it.number })
    }
}

private data class QuranSurahVerse(val surahNumber: Int, val verse: QuranVerse)

private fun normalizeArabic(value: String): String {
    val withoutMarks = Normalizer.normalize(value, Normalizer.Form.NFD)
        .filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() || it == 'ـ' }
    return withoutMarks
        .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
        .replace('ى', 'ي').replace('ؤ', 'و').replace('ئ', 'ي')
}

@Composable
fun QuranReaderScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var surahs by remember { mutableStateOf<List<QuranSurah>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var selectedSurah by remember { mutableStateOf<QuranSurah?>(null) }
    var selectedVerse by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        try {
            surahs = withContext(Dispatchers.IO) { loadQuran(context.applicationContext) }
            loadError = surahs.size != 114 || surahs.sumOf { it.verses.size } != 6236
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

    val current = selectedSurah
    if (current != null) {
        val listState = rememberLazyListState()
        LaunchedEffect(current.number, selectedVerse) {
            selectedVerse?.let { listState.scrollToItem(it.coerceAtMost(current.verses.size)) }
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
                Spacer(Modifier.size(48.dp))
            }
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                if (current.number != 1 && current.number != 9) {
                    item {
                        Text(
                            "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                            color = QuranGold,
                            fontSize = 25.sp,
                            textAlign = TextAlign.Center
                        )
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
                            verse.text,
                            modifier = Modifier.fillMaxWidth(),
                            color = QuranWhite,
                            fontSize = 25.sp,
                            lineHeight = 48.sp,
                            textAlign = TextAlign.Start
                        )
                        Spacer(Modifier.height(9.dp))
                        Box(
                            modifier = Modifier.size(27.dp).background(QuranEmerald, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(verse.number.toString(), color = Color.White, fontSize = 11.sp)
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
        Text("القرآن الكريم", modifier = Modifier.padding(top = 22.dp), color = QuranGold, fontSize = 26.sp, fontWeight = FontWeight.Bold)
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
                                Text(verse.text, color = QuranWhite, fontSize = 20.sp, lineHeight = 36.sp)
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
