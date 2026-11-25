package com.yaqeen.app

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.session.MediaController
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val NowPlayingInk = Color(0xFF090E0B)
private val NowPlayingPanel = Color(0xFF141D18)
private val NowPlayingGold = Color(0xFFD7C18A)
private val NowPlayingMuted = Color(0xFFAAB7AE)
private val NowPlayingText = Color(0xFFF1F3EF)

@Composable
internal fun QuranNowPlayingScreen(
    modifier: Modifier = Modifier,
    controller: MediaController?,
    onBack: () -> Unit
) {
    var recordings by remember { mutableStateOf<List<QuranRecording>>(emptyList()) }
    var selectedSource by remember { mutableStateOf(quranAudioSources.first()) }
    var trackTitle by remember { mutableStateOf("لم يبدأ التشغيل بعد") }
    var artist by remember { mutableStateOf("اختر قارئًا وسورة من مكتبة الاستماع") }

    LaunchedEffect(Unit) {
        recordings = Mp3QuranCatalogClient.loadCachedRecitations(
            // The catalog is cached in the application files directory by the listening screen.
            // A missing cache simply leaves the built-in verified sources available.
            YaqeenApplicationContextHolder.context ?: return@LaunchedEffect
        )
    }

    LaunchedEffect(controller, recordings) {
        while (isActive) {
            val media = controller?.currentMediaItem
            trackTitle = media?.mediaMetadata?.title?.toString()?.takeIf { it.isNotBlank() }
                ?: "لم يبدأ التشغيل بعد"
            artist = media?.mediaMetadata?.artist?.toString()?.takeIf { it.isNotBlank() }
                ?: "اختر قارئًا وسورة من مكتبة الاستماع"
            val mediaId = media?.mediaId
            val matched = recordings.firstOrNull { mediaId?.startsWith("${it.source.id}-") == true }?.source
                ?: quranAudioSources.firstOrNull { mediaId?.startsWith("${it.id}-") == true }
            if (matched != null) selectedSource = matched
            delay(500)
        }
    }

    val sources = (quranAudioSources + recordings.map { it.source }).distinctBy { it.id }
    val sourceMap = recordings.associate { it.source.id to it.availableSurahs }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(NowPlayingInk).padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "رجوع", tint = NowPlayingGold)
                }
                Text("مشغل يقين", color = NowPlayingText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(20.dp))
            Surface(
                modifier = Modifier.size(208.dp),
                shape = CircleShape,
                color = NowPlayingPanel,
                shadowElevation = 14.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier.size(174.dp),
                        shape = CircleShape,
                        color = Color(0xFF1C3025)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.GraphicEq,
                                contentDescription = null,
                                tint = NowPlayingGold,
                                modifier = Modifier.size(84.dp)
                            )
                        }
                    }
                    Icon(
                        Icons.Outlined.Headphones,
                        contentDescription = null,
                        tint = NowPlayingGold.copy(alpha = 0.25f),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp).size(26.dp)
                    )
                }
            }
            Spacer(Modifier.height(25.dp))
            Text(
                trackTitle,
                color = NowPlayingText,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(7.dp))
            Text(
                artist,
                color = NowPlayingMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(24.dp))
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NowPlayingPanel),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(10.dp)) {
                    if (controller != null) {
                        QuranAudioPlayerBar(
                            controller = controller,
                            selectedSource = selectedSource,
                            onSourceSelected = { selectedSource = it },
                            availableSources = sources,
                            availableSurahMap = sourceMap
                        )
                    } else {
                        Text(
                            "جارٍ تجهيز المشغل الداخلي…",
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            color = NowPlayingMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF287A59)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("العودة إلى القراء والسور", color = NowPlayingText)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
