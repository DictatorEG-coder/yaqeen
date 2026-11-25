package com.yaqeen.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val TafsirInk = Color(0xFF090E0B)
private val TafsirPanel = Color(0xFF141D18)
private val TafsirGold = Color(0xFFD7C18A)
private val TafsirMuted = Color(0xFFAAB7AE)
private val TafsirText = Color(0xFFF1F3EF)

@Composable
internal fun TafsirLibraryScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var quran by remember { mutableStateOf<List<QuranSurah>>(emptyList()) }
    var selectedSurah by remember { mutableStateOf(1) }
    var selectedVerse by remember { mutableStateOf(1) }
    var surahMenu by remember { mutableStateOf(false) }
    var tafsir by remember { mutableStateOf<QFTafsirEntry?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(context) {
        quran = withContext(Dispatchers.IO) { loadQuran(context.applicationContext) }
    }
    LaunchedEffect(selectedSurah, selectedVerse, quran.size) {
        if (quran.isEmpty()) return@LaunchedEffect
        loading = true
        error = null
        tafsir = null
        val key = "$selectedSurah:$selectedVerse"
        val result = withContext(Dispatchers.IO) {
            OfflineTafsirRepository.find(context.applicationContext, key)
        }
        if (result != null) {
            tafsir = result
        } else {
            error = "تعذّر العثور على تفسير هذه الآية في النسخة المحلية."
        }
        loading = false
    }

    val surah = quran.firstOrNull { it.number == selectedSurah }
    val verse = surah?.verses?.firstOrNull { it.number == selectedVerse }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(TafsirInk).padding(horizontal = 20.dp)
    ) {
        item {
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "رجوع", tint = TafsirGold)
                }
                Column {
                    Text("التفسير الميسّر", color = TafsirGold, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("تدبّر معاني الآيات من مصدر تفسيري موثّق", color = TafsirMuted, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("اختر السورة", color = TafsirText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(7.dp))
            Button(
                onClick = { surahMenu = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = TafsirPanel),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(surah?.name ?: surahNames.first(), color = TafsirGold)
            }
            DropdownMenu(expanded = surahMenu, onDismissRequest = { surahMenu = false }) {
                surahNames.forEachIndexed { index, name ->
                    DropdownMenuItem(
                        text = { Text("${index + 1}. $name") },
                        onClick = {
                            selectedSurah = index + 1
                            selectedVerse = 1
                            surahMenu = false
                        }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TafsirPanel),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (selectedVerse > 1) selectedVerse-- },
                        enabled = selectedVerse > 1
                    ) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = "الآية السابقة", tint = TafsirGold)
                    }
                    Text("الآية $selectedVerse من ${surah?.verses?.size ?: expectedVerseCounts[selectedSurah - 1]}", color = TafsirText)
                    IconButton(
                        onClick = { if (selectedVerse < (surah?.verses?.size ?: 1)) selectedVerse++ },
                        enabled = selectedVerse < (surah?.verses?.size ?: 1)
                    ) {
                        Icon(Icons.Outlined.ChevronLeft, contentDescription = "الآية التالية", tint = TafsirGold)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("نص الآية", color = TafsirGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TafsirPanel),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    verse?.text ?: "جارٍ تحميل المصحف…",
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    color = TafsirText,
                    fontSize = 23.sp,
                    lineHeight = 43.sp
                )
            }
            Spacer(Modifier.height(18.dp))
            Text("التفسير", color = TafsirGold, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TafsirPanel),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(17.dp)) {
                    when {
                        loading -> Text("جارٍ تحميل التفسير…", color = TafsirMuted)
                        tafsir != null -> {
                            Text(
                                listOfNotNull(tafsir?.resourceName, tafsir?.authorName).joinToString(" · "),
                                color = TafsirGold,
                                fontSize = 11.sp
                            )
                            Spacer(Modifier.height(9.dp))
                            Text(
                                androidx.core.text.HtmlCompat.fromHtml(
                                    tafsir?.text.orEmpty(),
                                    androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
                                ).toString().trim(),
                                color = TafsirText,
                                fontSize = 15.sp,
                                lineHeight = 27.sp
                            )
                        }
                        else -> Text(error ?: "لا يتوفر التفسير حاليًا.", color = TafsirMuted, lineHeight = 24.sp)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = onBack) { Text("العودة إلى المزيد", color = TafsirGold) }
            Spacer(Modifier.height(24.dp))
        }
    }
}
