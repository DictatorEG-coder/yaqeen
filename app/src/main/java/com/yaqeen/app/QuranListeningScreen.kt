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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ListenBackground = Color(0xFF090E0B)
private val ListenPanel = Color(0xFF141D18)
private val ListenGold = Color(0xFFD7C18A)
private val ListenMuted = Color(0xFFAAB7AE)
private val ListenText = Color(0xFFF1F3EF)

@Composable
internal fun QuranListeningScreen(modifier: Modifier = Modifier) {
    val controller = rememberQuranMediaController()
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
    val filteredSurahs = surahs.filter { it.second.contains(search.trim(), ignoreCase = true) }

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
                    label = { Text("ابحث باسم السورة") }
                )
                Spacer(Modifier.height(12.dp))
                Text("اختر السورة", color = ListenText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                filteredSurahs.forEach { (number, name) ->
                    ListenChoice(Icons.Outlined.PlayCircleOutline, name, "تشغيل السورة") {
                        val player = controller
                        val source = selectedSource
                        if (player != null && source != null) playQuranRecitation(player, number, source, selectedSurahs)
                    }
                }
                Spacer(Modifier.height(12.dp))
                val source = selectedSource!!
                QuranAudioPlayerBar(
                    controller = controller,
                    selectedSource = source,
                    onSourceSelected = {
                        selectedSource = it
                        selectedReciter = it.reciterName
                        selectedSurahs = activeRecordings.firstOrNull { recording -> recording.source.id == it.id }?.availableSurahs ?: (1..114).toList()
                    },
                    availableSources = availableSources
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
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable(onClick = onClick),
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
