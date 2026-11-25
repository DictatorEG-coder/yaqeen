package com.yaqeen.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer

private val ListenBackground = Color(0xFF090E0B)
private val ListenPanel = Color(0xFF141D18)
private val ListenGold = Color(0xFFD7C18A)
private val ListenMuted = Color(0xFFAAB7AE)
private val ListenText = Color(0xFFF1F3EF)

@Composable
internal fun QuranListeningScreen(
    modifier: Modifier = Modifier,
    sharedMediaController: androidx.media3.session.MediaController? = null
) {
    val controller = sharedMediaController ?: rememberQuranMediaController()
    val downloads by QuranPlaybackService.downloads.collectAsState()
    val context = LocalContext.current
    var quranTextData by remember { mutableStateOf<List<QuranSurah>>(emptyList()) }
    LaunchedEffect(context) { quranTextData = withContext(Dispatchers.IO) { loadQuran(context) } }
    var recordings by remember { mutableStateOf<List<QuranRecording>>(emptyList()) }
    var loadingCatalog by remember { mutableStateOf(true) }
    var catalogError by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        runCatching { Mp3QuranCatalogClient.fetchRecitations() }
            .onSuccess { recordings = it }
            .onFailure { catalogError = true }
        loadingCatalog = false
    }
    val activeRecordings = if (recordings.isNotEmpty()) recordings else quranAudioSources.map {
        QuranRecording(it, (1..114).toList())
    }
    val availableSources = activeRecordings.map { it.source }
    val grouped = activeRecordings.groupBy { it.source.reciterName }
    var selectedReciter by remember { mutableStateOf<String?>(null) }
    var selectedSource by remember { mutableStateOf<QuranAudioSource?>(null) }
    var selectedSurahs by remember { mutableStateOf((1..114).toList()) }
    var reciterSearch by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    val surahs = remember { surahNames.mapIndexed { index, name -> (index + 1) to name } }
    val normalizedQuery = remember(search) { normalizeListenSearch(search) }
    val filteredSurahs = surahs.filter { item ->
        item.first in selectedSurahs && (search.isBlank() ||
            normalizeListenSearch(item.second).contains(normalizedQuery) ||
            quranTextData.firstOrNull { it.number == item.first }?.verses?.any {
                normalizeListenSearch(it.text).contains(normalizedQuery)
            } == true)
    }

    Column(
        modifier = modifier.fillMaxSize().background(ListenBackground)
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("استماع القرآن", color = ListenGold, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text("اختر القارئ، ثم نمط التلاوة، ثم السورة.", color = ListenMuted, fontSize = 12.sp)
        Spacer(Modifier.height(22.dp))

        when {
            selectedReciter == null -> {
                Text("القراء المتاحون", color = ListenText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                if (loadingCatalog) Text("جارٍ تحميل فهرس القراء والتلاوات…", color = ListenMuted, fontSize = 11.sp)
                if (catalogError) Text("تعذّر تحديث الفهرس؛ نعرض القائمة المحلية المتاحة في التطبيق.", color = ListenMuted, fontSize = 11.sp)
                OutlinedTextField(value = reciterSearch, onValueChange = { reciterSearch = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("ابحث عن قارئ") })
                grouped.keys.filter { it.contains(reciterSearch.trim(), ignoreCase = true) }.forEach { name ->
                    ListenChoice(Icons.Outlined.Headphones, name, "عرض أنماط التسجيل المتاحة") {
                        selectedReciter = name
                    }
                }
            }
            selectedSource == null -> {
                ListenChoice(Icons.Outlined.ArrowBack, "قائمة القراء", "تغيير القارئ") {
                    selectedReciter = null
                }
                Spacer(Modifier.height(12.dp))
                Text("أنماط التلاوة المتاحة", color = ListenText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                grouped[selectedReciter].orEmpty().forEach { recording ->
                    ListenChoice(Icons.Outlined.PlayCircleOutline, recording.source.style, recording.availableSurahs.size.toString() + " سورة متاحة") {
                        selectedSource = recording.source
                        selectedSurahs = recording.availableSurahs
                    }
                }
            }
            else -> {
                ListenChoice(Icons.Outlined.ArrowBack, "تغيير نمط التلاوة", selectedReciter.orEmpty()) {
                    selectedSource = null
                }
                Spacer(Modifier.height(12.dp))
                Text(selectedSource!!.reciterName + " · " + selectedSource!!.style, color = ListenGold, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("ابحث باسم السورة أو جزء من آية") }
                )
                Spacer(Modifier.height(12.dp))
                Text("اختر السورة", color = ListenText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                val source = selectedSource!!
                Button(
                    onClick = { selectedSurahs.forEach { QuranPlaybackService.enqueueDownload(source, it) } },
                    enabled = controller != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = ListenGold),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("تنزيل كل السور المتاحة · ${selectedSurahs.size}", color = ListenBackground)
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp)
                ) {
                    items(filteredSurahs, key = { it.first }) { (number, name) ->
                        val downloadId = "quran-${source.id}-${number}"
                        val currentDownload = downloads.firstOrNull { it.request.id == downloadId }
                        val alreadyComplete = currentDownload?.state == androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
                        val alreadyQueued = currentDownload?.state == androidx.media3.exoplayer.offline.Download.STATE_QUEUED ||
                            currentDownload?.state == androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ListenChoice(
                                Icons.Outlined.PlayCircleOutline,
                                name,
                                "تشغيل السورة",
                                modifier = Modifier.weight(1f)
                            ) {
                                val player = controller
                                if (player != null) playQuranRecitation(player, number, source, selectedSurahs)
                            }
                            IconButton(
                                enabled = controller != null && !alreadyComplete && !alreadyQueued,
                                onClick = { QuranPlaybackService.enqueueDownload(source, number) }
                            ) {
                                Icon(
                                    Icons.Outlined.Download,
                                    contentDescription = if (alreadyComplete) "تم تنزيل سورة $name" else "تنزيل سورة $name",
                                    tint = if (alreadyComplete) ListenMuted else ListenGold
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                val sourceDownloads = downloads.filter { it.request.id.startsWith("quran-${source.id}-") }
                if (sourceDownloads.isNotEmpty()) {
                    Text("إدارة التنزيلات", color = ListenGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { QuranPlaybackService.pauseDownloads() }) {
                            Icon(Icons.Outlined.Pause, contentDescription = null, tint = ListenGold)
                            Text("إيقاف جميع التنزيلات", color = ListenGold, fontSize = 11.sp)
                        }
                        TextButton(onClick = { QuranPlaybackService.resumeDownloads() }) {
                            Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = ListenGold)
                            Text("استئناف الجميع", color = ListenGold, fontSize = 11.sp)
                        }
                    }
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
                        items(sourceDownloads, key = { it.request.id }) { download ->
                            val surahNumber = download.request.id.substringAfterLast('-').toIntOrNull()
                            val surahName = surahNumber?.let { surahNames.getOrNull(it - 1) } ?: "تلاوة"
                            val status = when (download.state) {
                                androidx.media3.exoplayer.offline.Download.STATE_COMPLETED -> "مكتملة · متاحة دون إنترنت"
                                androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING -> "جارٍ التنزيل"
                                androidx.media3.exoplayer.offline.Download.STATE_QUEUED -> "في الانتظار أو متوقفة مؤقتًا"
                                androidx.media3.exoplayer.offline.Download.STATE_FAILED -> "فشل التنزيل · يمكنك إعادة المحاولة"
                                else -> "قيد المعالجة"
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = ListenPanel),
                                shape = RoundedCornerShape(13.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(surahName, color = ListenText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            Text(status, color = ListenMuted, fontSize = 10.sp)
                                        }
                                        IconButton(onClick = { QuranPlaybackService.removeDownload(download.request.id) }) {
                                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "حذف التنزيل", tint = ListenMuted)
                                        }
                                    }
                                    if (download.state != androidx.media3.exoplayer.offline.Download.STATE_COMPLETED) {
                                        val progress = (download.percentDownloaded / 100f).coerceIn(0f, 1f)
                                        LinearProgressIndicator(
                                            progress = progress,
                                            modifier = Modifier.fillMaxWidth(),
                                            color = ListenGold,
                                            trackColor = ListenMuted.copy(alpha = 0.2f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                QuranAudioPlayerBar(
                    controller = controller,
                    selectedSource = source,
                    onSourceSelected = {
                        selectedSource = it
                        selectedReciter = it.reciterName
                        selectedSurahs = activeRecordings.firstOrNull { recording -> recording.source.id == it.id }?.availableSurahs ?: (1..114).toList()
                    },
                    availableSources = availableSources,
                    availableSurahMap = activeRecordings.associate { it.source.id to it.availableSurahs }
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "فهرس القراء من MP3Quran. التشغيل الحالي عبر الإنترنت؛ لا توجد تنزيلات محلية بعد، ولذلك لا تعمل التلاوات دون اتصال.",
                    color = ListenMuted, fontSize = 11.sp
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ListenChoice(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 5.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ListenPanel)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.material3.Icon(icon, contentDescription = null, tint = ListenGold)
            Column(Modifier.weight(1f)) {
                Text(title, color = ListenText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = ListenMuted, fontSize = 10.sp)
            }
        }
    }
}

private fun normalizeListenSearch(value: String): String {
    return Normalizer.normalize(value, Normalizer.Form.NFD)
        .filterNot {
            Character.getType(it) == Character.NON_SPACING_MARK.toInt() ||
                it == 'ـ' || it == 'ٰ'
        }
        .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
        .replace('ى', 'ي').replace('ؤ', 'و').replace('ئ', 'ي')
        .lowercase()
}
