package com.yaqeen.app

import android.content.ComponentName
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

private val AudioPanel = Color(0xFF17231C)
private val AudioGold = Color(0xFFD7C18A)
private val AudioMuted = Color(0xFFAAB7AE)
private val AudioWhite = Color(0xFFF1F3EF)

class QuranPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player)
            .setId("yaqeen-quran-player")
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.let { session ->
            session.player.release()
            session.release()
        }
        mediaSession = null
        super.onDestroy()
    }
}

@Composable
fun rememberQuranMediaController(): MediaController? {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }

    DisposableEffect(context) {
        val token = SessionToken(context, ComponentName(context, QuranPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                controller = try {
                    future.get()
                } catch (_: Exception) {
                    null
                }
            },
            ContextCompat.getMainExecutor(context)
        )
        onDispose {
            controller = null
            MediaController.releaseFuture(future)
        }
    }
    return controller
}

fun playMinshawiMurattal(controller: MediaController, startSurah: Int) {
    val playlist = surahNames.mapIndexed { index, name ->
        val number = index + 1
        val audioUrl = String.format(
            Locale.US,
            "https://download.quranicaudio.com/quran/muhammad_siddeeq_al-minshaawee/%03d.mp3",
            number
        )
        MediaItem.Builder()
            .setMediaId("minshawi-murattal-$number")
            .setUri(audioUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(name)
                    .setArtist("محمد صديق المنشاوي · مرتل")
                    .build()
            )
            .build()
    }
    controller.setMediaItems(playlist, (startSurah - 1).coerceIn(0, playlist.lastIndex), 0L)
    controller.prepare()
    controller.play()
}

@Composable
fun QuranAudioPlayerBar(controller: MediaController?, modifier: Modifier = Modifier) {
    if (controller == null) return

    var isPlaying by remember(controller) { mutableStateOf(controller.isPlaying) }
    var title by remember(controller) {
        mutableStateOf(controller.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty())
    }
    var position by remember(controller) { mutableStateOf(0L) }
    var duration by remember(controller) { mutableStateOf(0L) }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                title = mediaItem?.mediaMetadata?.title?.toString().orEmpty()
                position = 0L
                duration = controller.duration.coerceAtLeast(0L)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                duration = controller.duration.coerceAtLeast(0L)
            }
        }
        controller.addListener(listener)
        onDispose { controller.removeListener(listener) }
    }

    LaunchedEffect(controller) {
        while (isActive) {
            position = controller.currentPosition.coerceAtLeast(0L)
            duration = controller.duration.coerceAtLeast(0L)
            isPlaying = controller.isPlaying
            delay(500)
        }
    }

    val progress = if (duration > 0L) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AudioPanel)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title.ifBlank { "اختر سورة لبدء التلاوة" },
                        color = AudioWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("محمد صديق المنشاوي · مرتل · QuranicAudio", color = AudioMuted, fontSize = 10.sp)
                }
                IconButton(onClick = { controller.seekToPreviousMediaItem() }) {
                    Icon(Icons.Outlined.SkipPrevious, contentDescription = "السورة السابقة", tint = AudioGold)
                }
                IconButton(onClick = {
                    if (controller.isPlaying) controller.pause() else controller.play()
                }) {
                    Icon(
                        if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                        tint = AudioGold
                    )
                }
                IconButton(onClick = { controller.seekToNextMediaItem() }) {
                    Icon(Icons.Outlined.SkipNext, contentDescription = "السورة التالية", tint = AudioGold)
                }
            }
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                color = AudioGold,
                trackColor = AudioMuted.copy(alpha = 0.25f)
            )
        }
    }
}
