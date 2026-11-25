package com.yaqeen.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF090E0B)
private val Panel = Color(0xFF141D18)
private val PanelSoft = Color(0xFF1B2821)
private val Emerald = Color(0xFF287A59)
private val Gold = Color(0xFFD7C18A)
private val Muted = Color(0xFFAAB7AE)
private val MainText = Color(0xFFF1F3EF)
private const val TELEGRAM_CHANNEL = "https://t.me/Dictator_channel2"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YaqeenApp(
                initialSurahNumber = intent.getIntExtra(EXTRA_OPEN_SURAH, 0),
                openFridayReminders = intent.getBooleanExtra(EXTRA_OPEN_FRIDAY_REMINDERS, false)
            )
        }
    }

    companion object {
        const val EXTRA_OPEN_SURAH = "com.yaqeen.app.OPEN_SURAH"
        const val EXTRA_OPEN_FRIDAY_REMINDERS = "com.yaqeen.app.OPEN_FRIDAY_REMINDERS"
    }
}

@Composable
private fun YaqeenApp(initialSurahNumber: Int = 0, openFridayReminders: Boolean = false) {
    var selectedTab by remember { mutableIntStateOf(if (initialSurahNumber in 1..114) 1 else if (openFridayReminders) 4 else 0) }
    var openBookmarksRequested by remember { mutableStateOf(false) }
    MaterialTheme {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Scaffold(
                containerColor = Ink,
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier.navigationBarsPadding(),
                        containerColor = Panel,
                        contentColor = MainText
                    ) {
                        val tabs = listOf(
                            Triple("الرئيسية", Icons.Outlined.Home, 0),
                            Triple("القرآن", Icons.Outlined.MenuBook, 1),
                            Triple("الأحاديث", Icons.Outlined.LibraryBooks, 2),
                            Triple("الأذكار", Icons.Outlined.Bookmarks, 3),
                            Triple("التذكيرات", Icons.Outlined.NotificationsNone, 4),
                            Triple("المزيد", Icons.Outlined.MoreHoriz, 5)
                        )
                        tabs.forEach { (label, icon, index) ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Gold,
                                    selectedTextColor = Gold,
                                    indicatorColor = Emerald.copy(alpha = 0.22f),
                                    unselectedIconColor = Muted,
                                    unselectedTextColor = Muted
                                )
                            )
                        }
                    }
                }
            ) { padding ->
                when (selectedTab) {
                    0 -> HomeScreen(
                        modifier = Modifier.padding(padding),
                        onNavigate = { selectedTab = it }
                    )
                    1 -> QuranReaderScreen(
                        modifier = Modifier.padding(padding),
                        showBookmarksInitially = openBookmarksRequested,
                        initialSurahNumber = initialSurahNumber,
                        onBookmarksOpened = { openBookmarksRequested = false }
                    )
                    2 -> HadithLibraryScreen(Modifier.padding(padding))
                    6 -> QuranListeningScreen(Modifier.padding(padding))
                    7 -> QuranReaderScreen(
                        modifier = Modifier.padding(padding),
                        initialSurahNumber = initialSurahNumber,
                        audioMode = true
                    )
                    8 -> SunnahMahjooraScreen(Modifier.padding(padding))
                    3 -> AdhkarScreen(Modifier.padding(padding))
                    4 -> RemindersScreen(Modifier.padding(padding))
                    else -> MoreScreen(
                        modifier = Modifier.padding(padding),
                        onOpenBookmarks = { selectedTab = 1; openBookmarksRequested = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun PageColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) { content() }
}

@Composable
private fun PageHeader(title: String, subtitle: String) {
    Spacer(Modifier.height(22.dp))
    Text(title, color = Gold, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(5.dp))
    Text(subtitle, color = Muted, fontSize = 12.sp)
    Spacer(Modifier.height(22.dp))
}

@Composable
private fun HomeScreen(modifier: Modifier = Modifier, onNavigate: (Int) -> Unit) {
    PageColumn(modifier) {
        Spacer(Modifier.height(26.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("يَقِين", color = Gold, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("مساحة هادئة للقرآن والسنة", color = Muted, fontSize = 12.sp)
            }
            Surface(shape = CircleShape, color = PanelSoft) {
                Icon(Icons.Outlined.NotificationsNone, contentDescription = "التذكيرات", tint = Gold,
                    modifier = Modifier.padding(13.dp).size(22.dp).clickable { onNavigate(4) })
            }
        }
        Spacer(Modifier.height(26.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = PanelSoft)
        ) {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 23.dp)) {
                Text("وَرَتِّلِ الْقُرْآنَ تَرْتِيلًا", color = MainText, fontSize = 23.sp,
                    fontWeight = FontWeight.Medium, lineHeight = 39.sp)
                Spacer(Modifier.height(8.dp))
                Text("سورة المزمل · ٤", color = Gold, fontSize = 11.sp)
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { onNavigate(1) },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Outlined.MenuBook, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("متابعة القراءة")
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("المصحف الشريف", color = Muted, fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(29.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("استكشف يقين", color = MainText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("المحتوى", color = Muted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Outlined.Headphones, null, tint = Gold) },
                title = "استماع القرآن",
                subtitle = "القراء والتلاوات",
                onClick = { onNavigate(6) }
            )
            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Outlined.MenuBook, null, tint = Gold) },
                title = "قراءة القرآن",
                subtitle = "السور والآيات",
                onClick = { onNavigate(1) }
            )
        }
        Spacer(Modifier.height(11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Outlined.AutoStories, null, tint = Gold) },
                title = "قراءة واستماع",
                subtitle = "المتابعة مع التلاوة",
                onClick = { onNavigate(7) }
            )
            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Outlined.LibraryBooks, null, tint = Gold) },
                title = "الأحاديث",
                subtitle = "الكتب الستة",
                onClick = { onNavigate(2) }
            )
        }
        Spacer(Modifier.height(11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Outlined.WbTwilight, null, tint = Gold) },
                title = "الأذكار",
                subtitle = "حصن المسلم",
                onClick = { onNavigate(3) }
            )
            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Outlined.Spa, null, tint = Gold) },
                title = "السنن المهجورة",
                subtitle = "سنن بأدلتها",
                onClick = { onNavigate(8) }
            )
        }
        Spacer(Modifier.height(25.dp))
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onNavigate(4) },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Panel)
        ) {
            Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(13.dp), color = Emerald.copy(alpha = 0.16f)) {
                    Icon(Icons.Outlined.NotificationsActive, null, tint = Gold,
                        modifier = Modifier.padding(11.dp).size(22.dp))
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text("التذكيرات", color = MainText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("اضبط أوقات ما تحب المواظبة عليه", color = Muted, fontSize = 11.sp)
                }
                Icon(Icons.Outlined.ChevronLeft, null, tint = Muted)
            }
        }
        Spacer(Modifier.height(25.dp))
        Text("يقين", modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            color = Muted.copy(alpha = 0.55f), fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun HadithLibraryScreen(modifier: Modifier = Modifier) {
    val client = remember { SunnahApiClient() }
    val scope = rememberCoroutineScope()
    val collectionNames = remember { linkedMapOf(
        "bukhari" to "صحيح البخاري", "muslim" to "صحيح مسلم",
        "abudawud" to "سنن أبي داود", "tirmidhi" to "جامع الترمذي",
        "nasai" to "سنن النسائي", "ibnmajah" to "سنن ابن ماجه"
    ) }
    var collections by remember { mutableStateOf<Map<String, SunnahCollection>>(emptyMap()) }
    var booksByCollection by remember { mutableStateOf<Map<String, List<SunnahBook>>>(emptyMap()) }
    var selectedCollection by remember { mutableStateOf<String?>(null) }
    var selectedBook by remember { mutableStateOf<SunnahBook?>(null) }
    var hadiths by remember { mutableStateOf<List<SunnahHadith>>(emptyList()) }
    var nextPage by remember { mutableStateOf<Int?>(null) }
    var loading by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }

    LaunchedEffect(client.isConfigured) {
        if (!client.isConfigured) { initialized = true; return@LaunchedEffect }
        loading = true
        val loadedCollections = collections.toMutableMap()
        val loadedBooks = booksByCollection.toMutableMap()
        collectionNames.keys.forEach { slug ->
            try {
                val collection = client.fetchCollection(slug)
                val books = client.fetchBooks(slug).data
                loadedCollections[slug] = collection
                loadedBooks[slug] = books
                collections = loadedCollections.toMap()
                booksByCollection = loadedBooks.toMap()
            } catch (_: Exception) {
                if (error == null) error = "تعذّر تحميل بعض المجموعات. ستظل المجموعات التي تم تحميلها متاحة."
            }
        }
        loading = false
        initialized = true
    }

    fun loadHadithPage(collection: String, book: SunnahBook, page: Int, append: Boolean) {
        scope.launch {
            loading = true
            error = null
            try {
                val result = client.fetchBookHadiths(collection, book.bookNumber, page, 100)
                hadiths = if (append) hadiths + result.data else result.data
                nextPage = result.nextPage
            } catch (_: Exception) {
                error = "تعذّر تحميل أحاديث هذا الكتاب من المصدر."
            } finally { loading = false }
        }
    }

    val visibleHadiths = remember(hadiths, search) {
        if (search.isBlank()) hadiths else hadiths.filter {
            (it.arabicText.orEmpty() + " " + it.hadithNumber + " " + it.texts.mapNotNull { t -> t.chapterTitle }.joinToString(" ")).contains(search.trim(), ignoreCase = true)
        }
    }

    PageColumn(modifier) {
        PageHeader("مكتبة الحديث", "الكتب الستة · بحث بالنص والمرجع · مع بيان المصدر")
        if (!client.isConfigured) {
            InfoCard("مكتبة الحديث غير مهيأة", "يلزم ربط مصدر البيانات بالخادم قبل عرض النصوص. لن نعرض أحاديث وهمية أو غير موثقة.")
        } else if (!initialized && loading) {
            InfoCard("جارٍ تحميل فهارس الكتب", "يتم جلب بيانات المجموعات والكتب من المصدر.")
        }
        if (selectedBook != null && selectedCollection != null) {
            val collection = selectedCollection!!
            val book = selectedBook!!
            SimpleRow(Icons.Outlined.ArrowBack, "العودة إلى الكتب", collectionNames[collection] ?: collection, onClick = { selectedBook = null; hadiths = emptyList(); nextPage = null; search = "" })
            Spacer(Modifier.height(8.dp))
            Text(book.arabicName ?: book.englishName ?: "كتاب ${book.bookNumber}", color = Gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("${book.numberOfHadith} حديثًا · المرجع ${book.hadithStartNumber}–${book.hadithEndNumber}", color = Muted, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(), label = { Text("ابحث في الأحاديث المحمّلة") }, singleLine = true)
            visibleHadiths.forEach { hadith ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Column(Modifier.padding(15.dp)) {
                        Text("حديث رقم ${hadith.hadithNumber}", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        hadith.texts.firstOrNull { it.language == "ar" }?.chapterTitle?.let { Text(it, color = Muted, fontSize = 11.sp) }
                        Spacer(Modifier.height(8.dp))
                        Text(hadith.arabicText ?: "النص العربي غير متاح في سجل المصدر.", color = MainText, fontSize = 18.sp, lineHeight = 32.sp)
                        if (hadith.grades.isNotEmpty()) Text(hadith.grades.joinToString(" · ") { "${it.grade} — ${it.gradedBy}" }, color = Gold, fontSize = 10.sp)
                        Text("المصدر: ${collectionNames[collection]} · ${hadith.hadithNumber}", color = Muted, fontSize = 10.sp)
                    }
                }
            }
            if (loading) Text("جارٍ التحميل…", color = Muted)
            if (error != null) InfoCard("ملاحظة", error!!)
            nextPage?.let { page -> Button(onClick = { loadHadithPage(collection, book, page, true) }, enabled = !loading, colors = ButtonDefaults.buttonColors(containerColor = Emerald)) { Text("تحميل الصفحة التالية") } }
            if (search.isNotBlank() && visibleHadiths.isEmpty()) InfoCard("لا توجد نتيجة في الصفحة المحمّلة", "جرّب عبارة أخرى أو حمّل الصفحة التالية. البحث هنا لا يدّعي فهرسة الكتاب كاملًا.")
        } else if (selectedCollection != null) {
            val collection = selectedCollection!!
            SimpleRow(Icons.Outlined.ArrowBack, "العودة إلى المجموعات", collectionNames[collection] ?: collection, onClick = { selectedCollection = null })
            (booksByCollection[collection] ?: emptyList()).forEach { book ->
                SimpleRow(Icons.Outlined.LibraryBooks, book.arabicName ?: book.englishName ?: "كتاب ${book.bookNumber}", "${book.numberOfHadith} حديثًا", onClick = { selectedBook = book; hadiths = emptyList(); nextPage = null; search = ""; loadHadithPage(collection, book, 1, false) })
            }
            if ((booksByCollection[collection] ?: emptyList()).isEmpty() && !loading) InfoCard("فهرس الكتاب غير متاح", error ?: "لم يرجع المصدر قائمة الكتب لهذه المجموعة.")
        } else {
            collectionNames.forEach { (slug, title) ->
                val info = collections[slug]
                SimpleRow(Icons.Outlined.LibraryBooks, title, if (info == null) "لم يتم تحميل بيانات هذه المجموعة" else "${info.totalAvailableHadith} من ${info.totalHadith} حديث متاح", onClick = { selectedCollection = slug })
            }
            if (error != null) InfoCard("بعض المصادر لم تستجب", error!!)
            InfoCard("تنبيه بشأن الأحكام", "نعرض أحكام المحدّثين فقط إذا أعادها المصدر صراحةً. وجود الحديث في أحد هذه الكتب لا يعني وحده أن كل رواياته صحيحة.")
        }
        Spacer(Modifier.height(22.dp))
    }
}
@Composable
private fun AdhkarScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("yaqeen_adhkar_counts", android.content.Context.MODE_PRIVATE) }
    fun todayKey(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    fun loadCounts(date: String): Map<String, Int> = verifiedAdhkarEntries.associate { it.id to preferences.getInt(adhkarStorageKey(date, it.id), 0) }
    var dateKey by remember { mutableStateOf(todayKey()) }
    var counts by remember(dateKey) { mutableStateOf(loadCounts(dateKey)) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    val categories = remember { verifiedAdhkarEntries.map { it.category }.distinct() }
    val query = search.trim()
    // Search across the actual dhikr text as well as category titles, not just the index labels.
    val matchingEntries = remember(query) {
        if (query.isBlank()) emptyList()
        else verifiedAdhkarEntries.filter { entry ->
            entry.text.contains(query, ignoreCase = true) ||
                entry.category.contains(query, ignoreCase = true)
        }
    }
    val visibleCategories = remember(categories, query, matchingEntries) {
        if (query.isBlank()) categories
        else categories.filter { category -> matchingEntries.any { it.category == category } }
    }
    val entries = remember(selectedCategory, query) {
        verifiedAdhkarEntries.filter { entry ->
            (selectedCategory == null || entry.category == selectedCategory) &&
                (query.isBlank() || entry.text.contains(query, ignoreCase = true) || entry.category.contains(query, ignoreCase = true))
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            val today = todayKey()
            if (today != dateKey) { preferences.edit().clear().apply(); dateKey = today; counts = loadCounts(today) }
        }
    }
    PageColumn(modifier) {
        PageHeader("الأذكار", "حصن المسلم · 132 بابًا · 267 نصًا من بيانات المصدر")
        OutlinedTextField(value = search, onValueChange = { search = it; selectedCategory = null }, modifier = Modifier.fillMaxWidth(), label = { Text("ابحث داخل نصوص الأذكار وأسماء الأبواب") }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        if (selectedCategory == null) {
            SectionHeading("أبواب حصن المسلم")
            Text("اختر الباب لعرض أذكاره وعدّاد كل ذكر.", color = Muted, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            visibleCategories.forEach { category ->
                val amount = if (query.isBlank()) verifiedAdhkarEntries.count { it.category == category }
                    else matchingEntries.count { it.category == category }
                SimpleRow(
                    Icons.Outlined.Bookmarks,
                    category,
                    if (query.isBlank()) "$amount من الأذكار" else "$amount نتيجة",
                    onClick = { selectedCategory = category }
                )
            }
            if (visibleCategories.isEmpty()) InfoCard("لا توجد نتائج مطابقة", "لم نعثر على هذه العبارة داخل نصوص الأذكار أو أبوابها.")
        } else {
            SimpleRow(Icons.Outlined.ArrowBack, "كل الأبواب", "العودة إلى فهرس حصن المسلم", onClick = { selectedCategory = null })
            SectionHeading(selectedCategory!!)
            Spacer(Modifier.height(8.dp))
            entries.forEach { entry ->
                val count = counts[entry.id] ?: 0
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(entry.text, color = MainText, fontSize = 19.sp, lineHeight = 34.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(entry.sourceLabel, color = Muted, fontSize = 10.sp)
                        Spacer(Modifier.height(9.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("العدد: $count / ${entry.targetCount}", color = Gold, fontSize = 12.sp)
                            Text(if (count >= entry.targetCount) "اكتمل" else "المتبقي: ${entry.targetCount - count}", color = Muted, fontSize = 11.sp)
                        }
                        LinearProgressIndicator(progress = (count.toFloat() / entry.targetCount).coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth(), color = Emerald, trackColor = Muted.copy(alpha = 0.22f))
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { val next = (counts[entry.id] ?: 0) + 1; counts = counts + (entry.id to next); preferences.edit().putInt(adhkarStorageKey(dateKey, entry.id), next).apply() }, enabled = count < entry.targetCount, colors = ButtonDefaults.buttonColors(containerColor = Emerald), shape = RoundedCornerShape(12.dp)) { Text(if (entry.targetCount == 1) "تم الذكر" else "سجّل ذكرًا") }
                            OutlinedButton(onClick = { counts = counts + (entry.id to 0); preferences.edit().remove(adhkarStorageKey(dateKey, entry.id)).apply() }, enabled = count > 0, shape = RoundedCornerShape(12.dp)) { Text("إعادة") }
                        }
                    }
                }
            }
            if (entries.isEmpty()) InfoCard("لا توجد نتيجة", "لا توجد أذكار مطابقة في هذا الباب.")
        }
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun MoreScreen(modifier: Modifier = Modifier, onOpenBookmarks: () -> Unit) {
    val context = LocalContext.current
    PageColumn(modifier) {
        PageHeader("المزيد", "إعدادات يقين ومعلومات التطبيق")
        SimpleRow(Icons.Outlined.Bookmarks, "المحفوظات", "الآيات المحفوظة من المصحف", onClick = onOpenBookmarks)
        SimpleRow(Icons.Outlined.MenuBook, "حول تطبيق يقين", "الرؤية والمصادر وحقوق المحتوى")
        Spacer(Modifier.height(14.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Panel)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("تواصل معنا", color = Gold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "إذا وجدت خطأً أو لديك ملاحظة أو اقتراح، يسعدنا أن تخبرنا عبر قناة يقين على تيليجرام.",
                    color = Muted,
                    fontSize = 13.sp,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_CHANNEL))
                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_CHANNEL)))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("قناة يقين على تيليجرام")
                }
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_CHANNEL)))
                }) {
                    Text("t.me/Dictator_channel2", color = Gold, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PanelSoft)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("دعوة طيبة", color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(9.dp))
                Text(
                    "إن أعجبك تطبيق يقين، فلا تنسَ الدعاء لصاحبه بالتوفيق والنجاح في دراسته، وأن يجعل الله هذا العمل نافعًا وخالصًا لوجهه.",
                    color = MainText,
                    fontSize = 13.sp,
                    lineHeight = 24.sp
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(
            "يقين · إصدار تأسيسي",
            modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp),
            color = Muted.copy(alpha = 0.7f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DailyReminderCard(kind: DailyReminderKind) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(DailyReminderScheduler.enabled(context, kind)) }
    var mode by remember { mutableStateOf(DailyReminderScheduler.mode(context, kind)) }
    var hour by remember { mutableIntStateOf(DailyReminderScheduler.hour(context, kind)) }
    var minute by remember { mutableIntStateOf(DailyReminderScheduler.minute(context, kind)) }
    var interval by remember { mutableIntStateOf(DailyReminderScheduler.interval(context, kind)) }
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        if (granted) {
            DailyReminderScheduler.configure(context, kind, true, mode, hour, minute, interval)
            enabled = true
        }
    }
    fun save(nextEnabled: Boolean = enabled) {
        DailyReminderScheduler.configure(context, kind, nextEnabled, mode, hour, minute, interval)
        enabled = nextEnabled
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Column(Modifier.padding(15.dp)) {
            Text(kind.title, color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(kind.message, color = Muted, fontSize = 11.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(onClick = {
                    mode = "interval"
                    interval = if (interval >= 24) 1 else when (interval) {
                        1 -> 2; 2 -> 3; 3 -> 4; 4 -> 6; 6 -> 8; 8 -> 12; 12 -> 24; else -> 1
                    }
                    if (enabled) save()
                }, shape = RoundedCornerShape(10.dp)) {
                    Text("كل $interval ساعة", color = if (mode == "interval") Gold else Muted, fontSize = 11.sp)
                }
                OutlinedButton(onClick = {
                    mode = "daily"
                    TimePickerDialog(context, { _, h, m -> hour = h; minute = m; if (enabled) save() }, hour, minute, false).show()
                }, shape = RoundedCornerShape(10.dp)) {
                    Text("وقت يومي %02d:%02d".format(hour, minute), color = if (mode == "daily") Gold else Muted, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(7.dp))
            if (enabled) {
                OutlinedButton(onClick = { save(false) }, shape = RoundedCornerShape(10.dp)) {
                    Text("إيقاف التذكير", color = MainText)
                }
            } else {
                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= 33 &&
                        context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    else { save(true); permissionDenied = false }
                }, colors = ButtonDefaults.buttonColors(containerColor = Emerald), shape = RoundedCornerShape(10.dp)) {
                    Text("تفعيل التذكير", color = MainText)
                }
            }
            if (permissionDenied) Text("إذن الإشعارات غير ممنوح.", color = Gold, fontSize = 10.sp)
        }
    }
}

@Composable
private fun FridayReminderCard(
    kind: FridayReminderKind,
    title: String,
    description: String,
    defaultHour: Int
) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(FridayReminderScheduler.isEnabled(context, kind)) }
    var hour by remember { mutableIntStateOf(FridayReminderScheduler.hour(context, kind, defaultHour)) }
    var minute by remember { mutableIntStateOf(FridayReminderScheduler.minute(context, kind)) }
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        if (granted) {
            FridayReminderScheduler.enable(context, kind, hour, minute)
            enabled = true
        }
    }
    fun activate() {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            FridayReminderScheduler.enable(context, kind, hour, minute)
            enabled = true
            permissionDenied = false
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(description, color = Muted, fontSize = 12.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, false).show()
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("وقت التذكير: %02d:%02d".format(hour, minute), color = MainText)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { if (enabled) { FridayReminderScheduler.disable(context, kind); enabled = false } else activate() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (enabled) PanelSoft else Emerald),
                    shape = RoundedCornerShape(10.dp)
                ) { Text(if (enabled) "إيقاف" else "تفعيل", color = MainText) }
                Text(if (enabled) "مفعّل كل يوم جمعة" else "غير مفعّل", color = if (enabled) Gold else Muted, fontSize = 11.sp)
            }
            if (permissionDenied) {
                Spacer(Modifier.height(5.dp))
                Text("يلزم السماح بإشعارات يقين من إعدادات الهاتف.", color = Gold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun FridaySalawatReminderCard() {
    val context = LocalContext.current
    val kind = FridayReminderKind.SALAWAT
    var enabled by remember { mutableStateOf(FridayReminderScheduler.isEnabled(context, kind)) }
    var hour by remember { mutableIntStateOf(FridayReminderScheduler.hour(context, kind)) }
    var minute by remember { mutableIntStateOf(FridayReminderScheduler.minute(context, kind)) }
    var interval by remember { mutableIntStateOf(FridayReminderScheduler.intervalHours(context)) }
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        if (granted) {
            FridayReminderScheduler.enable(context, kind, hour, minute, interval)
            enabled = true
        }
    }
    fun activate() {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        else {
            FridayReminderScheduler.enable(context, kind, hour, minute, interval)
            enabled = true
            permissionDenied = false
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("الإكثار من الصلاة على النبي ﷺ", color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text("تذكير متكرر يوم الجمعة فقط، ويتوقف التكرار بعد نهاية اليوم حتى الجمعة التالية.", color = Muted, fontSize = 12.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(9.dp))
            OutlinedButton(
                onClick = { TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, false).show() },
                shape = RoundedCornerShape(10.dp)
            ) { Text("بداية التذكير: %02d:%02d".format(hour, minute), color = MainText) }
            Spacer(Modifier.height(8.dp))
            Text("التكرار كل:", color = MainText, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(1, 2, 3, 4, 6, 8, 12).forEach { hours ->
                    OutlinedButton(
                        onClick = {
                            interval = hours
                            if (enabled) FridayReminderScheduler.enable(context, kind, hour, minute, interval)
                        },
                        shape = RoundedCornerShape(9.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 9.dp, vertical = 4.dp)
                    ) { Text("$hours س", color = if (interval == hours) Gold else Muted, fontSize = 10.sp) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { if (enabled) { FridayReminderScheduler.disable(context, kind); enabled = false } else activate() },
                colors = ButtonDefaults.buttonColors(containerColor = if (enabled) PanelSoft else Emerald),
                shape = RoundedCornerShape(10.dp)
            ) { Text(if (enabled) "إيقاف" else "تفعيل", color = MainText) }
            if (permissionDenied) Text("يلزم السماح بإشعارات يقين من إعدادات الهاتف.", color = Gold, fontSize = 11.sp)
        }
    }
}

@Composable
private fun FridayHadithCard() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PanelSoft)
    ) {
        Column(Modifier.padding(17.dp)) {
            Text("حديث الجمعة", color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(9.dp))
            Text(
                "«إِنَّ مِنْ أَفْضَلِ أَيَّامِكُمْ يَوْمَ الْجُمُعَةِ، فِيهِ خُلِقَ آدَمُ وَفِيهِ قُبِضَ، وَفِيهِ النَّفْخَةُ وَفِيهِ الصَّعْقَةُ، فَأَكْثِرُوا عَلَيَّ مِنَ الصَّلَاةِ فِيهِ، فَإِنَّ صَلَاتَكُمْ مَعْرُوضَةٌ عَلَيَّ»",
                color = MainText, fontSize = 15.sp, lineHeight = 29.sp
            )
            Spacer(Modifier.height(9.dp))
            Text("الراوي: أوس بن أوس · سنن أبي داود (1047) · صححه الألباني", color = Gold, fontSize = 11.sp)
            Text("يُعرض هذا الحديث داخل التطبيق دون الحاجة إلى اتصال بالإنترنت.", color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun SalawatReminderCard() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(SalawatReminderScheduler.isEnabled(context)) }
    var intervalHours by remember { mutableIntStateOf(SalawatReminderScheduler.intervalHours(context)) }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionDenied = !granted
        if (granted) {
            SalawatReminderScheduler.schedule(context, intervalHours)
            enabled = true
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("تذكير: صلِّ على النبي ﷺ", color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text(
                "فعّل تذكيرًا اختياريًا يصلك على هاتفك كل فترة تختارها.",
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(13.dp))
            Text("التذكير كل:", color = MainText, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(1, 2, 3, 4, 6, 8, 12, 24).forEach { hours ->
                    OutlinedButton(
                        onClick = {
                            intervalHours = hours
                            if (enabled) SalawatReminderScheduler.schedule(context, hours)
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "$hours س",
                            color = if (intervalHours == hours) Gold else Muted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (enabled) {
                Button(
                    onClick = {
                        SalawatReminderScheduler.cancel(context)
                        enabled = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PanelSoft)
                ) { Text("إيقاف التذكير", color = MainText) }
            } else {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= 33 &&
                            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            SalawatReminderScheduler.schedule(context, intervalHours)
                            enabled = true
                            permissionDenied = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald)
                ) { Text("تفعيل التذكير", color = MainText) }
            }
            if (permissionDenied) {
                Spacer(Modifier.height(6.dp))
                Text("لم يتم منح إذن الإشعارات؛ لن تظهر التذكيرات حتى تسمح بها من إعدادات الهاتف.", color = Gold, fontSize = 11.sp)
            }
            Spacer(Modifier.height(7.dp))
            Text(
                "التذكير اختياري ويمكن إيقافه في أي وقت. قد يختلف موعد وصوله قليلًا لتوفير البطارية.",
                color = Muted,
                fontSize = 10.sp,
                lineHeight = 17.sp
            )
        }
    }
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun SectionHeading(title: String) {
    Text(title, color = MainText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun FeatureCard(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            icon()
            Spacer(Modifier.height(15.dp))
            Text(title, color = MainText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Column(Modifier.padding(17.dp)) {
            Text(title, color = Gold, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(body, color = Muted, fontSize = 12.sp, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun SimpleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 5.dp)
            .background(Panel, RoundedCornerShape(16.dp))
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Emerald.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Gold, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column {
            Text(title, color = MainText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun RemindersScreen(modifier: Modifier = Modifier) {
    PageColumn(modifier) {
        PageHeader("التذكيرات", "تذكيرات مستقلة قابلة للتحكم")
        SectionHeading("تذكيرات يوم الجمعة")
        Spacer(Modifier.height(10.dp))
        FridayReminderCard(FridayReminderKind.KAHF, "قراءة سورة الكهف", "تذكير أسبوعي يوم الجمعة ويفتح السورة مباشرة.", 9)
        FridaySalawatReminderCard()
        FridayReminderCard(FridayReminderKind.HADITH, "حديث يوم الجمعة", "تذكير أسبوعي بحديث متعلق بفضل الجمعة.", 10)
        FridayHadithCard()
        SectionHeading("التذكيرات اليومية")
        Spacer(Modifier.height(8.dp))
        DailyReminderKind.entries.forEach { kind -> DailyReminderCard(kind) }
        Spacer(Modifier.height(22.dp))
    }
}
