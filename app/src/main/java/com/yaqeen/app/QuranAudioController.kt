package com.yaqeen.app

import android.content.ComponentName
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
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

internal fun sleepTimerDelayMs(minutes: Int): Long {
    require(minutes in 1..180) { "Sleep timer must be between 1 and 180 minutes" }
    return minutes.toLong() * 60_000L
}

class QuranPlaybackService : MediaSessionService() {
    companion object {
        @Volatile private var activeInstance: QuranPlaybackService? = null

        fun scheduleSleepTimer(minutes: Int): Boolean {
            if (minutes !in 1..180) return false
            val service = activeInstance ?: return false
            service.scheduleTimer(minutes)
            return true
        }

        fun cancelSleepTimer() {
            activeInstance?.cancelTimer()
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var sleepTask: Runnable? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        val player = ExoPlayer.Builder(this).build()
        player.setAudioAttributes(AudioAttributes.DEFAULT, true)
        player.setHandleAudioBecomingNoisy(true)
        mediaSession = MediaSession.Builder(this, player)
            .setId("yaqeen-quran-player")
            .build()
    }

    private fun scheduleTimer(minutes: Int) {
        cancelTimer()
        val task = Runnable {
            mediaSession?.player?.pause()
            sleepTask = null
        }
        sleepTask = task
        mainHandler.postDelayed(task, sleepTimerDelayMs(minutes))
    }

    private fun cancelTimer() {
        sleepTask?.let(mainHandler::removeCallbacks)
        sleepTask = null
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        cancelTimer()
        if (activeInstance === this) activeInstance = null
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

internal data class QuranAudioSource(
    val id: String,
    val reciterName: String,
    val style: String,
    val baseUrl: String
)

internal val quranAudioSources = listOf(
    QuranAudioSource(
        "minshawi_murattal", "محمد صديق المنشاوي", "مرتل",
        "https://download.quranicaudio.com/quran/muhammad_siddeeq_al-minshaawee/"
    ),
    QuranAudioSource(
        "abdulbaset_murattal", "عبد الباسط عبد الصمد", "مرتل",
        "https://download.quranicaudio.com/quran/abdul_basit_murattal/"
    ),
    QuranAudioSource(
        "abdulbaset_mujawwad", "عبد الباسط عبد الصمد", "مجود",
        "https://download.quranicaudio.com/quran/abdulbaset_mujawwad/"
    ),
    QuranAudioSource(
        "husary_radio", "محمود خليل الحصري", "إذاعي",
        "https://download.quranicaudio.com/quran/mahmood_khaleel_al-husaree_iza3a/"
    ),
    QuranAudioSource(
        "mustafa_ismail", "مصطفى إسماعيل", "تلاوة كاملة",
        "https://download.quranicaudio.com/quran/mostafa_ismaeel/"
    ),
    QuranAudioSource(
        "sudais", "عبدالرحمن السديس", "تلاوة كاملة",
        "https://download.quranicaudio.com/quran/abdurrahmaan_as-sudays/"
    ),
    QuranAudioSource(
        "shuraim", "سعود الشريم", "تلاوة كاملة",
        "https://download.quranicaudio.com/quran/sa3ood_al-shuraym/"
    ),
    QuranAudioSource(
        "afasy", "مشاري راشد العفاسي", "تلاوة كاملة",
        "https://download.quranicaudio.com/quran/mishaari_raashid_al_3afaasee/"
    ),
    QuranAudioSource(
        "luhaidan", "محمد اللحيدان", "مرتل · حفص عن عاصم",
        "https://cdn.mp3quran.net/audio/muhammad-luhaidan/r1/"
    )
)

internal fun quranAudioUrl(sourceId: String, surahNumber: Int): String {
    require(surahNumber in 1..114) { "Surah number must be between 1 and 114" }
    val source = quranAudioSources.firstOrNull { it.id == sourceId }
        ?: throw IllegalArgumentException("Unknown Quran audio source: $sourceId")
    return source.baseUrl + String.format(Locale.US, "%03d.mp3", surahNumber)
}

internal fun surahNumberFromMediaId(mediaId: String?): Int? =
    mediaId?.substringAfterLast('-')?.toIntOrNull()?.takeIf { it in 1..114 }

internal fun minshawiMurattalUrl(surahNumber: Int): String =
    quranAudioUrl("minshawi_murattal", surahNumber)

internal data class CuratedQuranTrack(
    val sourceId: String,
    val surahNumber: Int
)

internal data class CuratedQuranPlaylist(
    val id: String,
    val title: String,
    val tracks: List<CuratedQuranTrack>
)

internal val curatedQuranPlaylists = listOf(
    CuratedQuranPlaylist(
        "classic_mix",
        "مختارات قرآنية متنوعة",
        listOf(
            CuratedQuranTrack("abdulbaset_murattal", 2),
            CuratedQuranTrack("abdulbaset_murattal", 12),
            CuratedQuranTrack("minshawi_murattal", 53),
            CuratedQuranTrack("minshawi_murattal", 50),
            CuratedQuranTrack("minshawi_murattal", 90),
            CuratedQuranTrack("luhaidan", 19)
        )
    ),
    CuratedQuranPlaylist(
        "juz_qad_sami",
        "جزء قَدْ سَمِعَ · الحصري",
        (58..66).map { CuratedQuranTrack("husary_radio", it) }
    )
)

internal fun playCuratedQuranPlaylist(
    controller: MediaController,
    playlist: CuratedQuranPlaylist
) {
    require(playlist.tracks.isNotEmpty()) { "Curated playlist must contain tracks" }
    val items = playlist.tracks.map { track ->
        val source = quranAudioSources.firstOrNull { it.id == track.sourceId }
            ?: throw IllegalArgumentException("Unknown Quran audio source: ${track.sourceId}")
        require(track.surahNumber in 1..114) { "Surah number must be between 1 and 114" }
        MediaItem.Builder()
            .setMediaId("curated-${playlist.id}-${track.sourceId}-${track.surahNumber}")
            .setUri(quranAudioUrl(track.sourceId, track.surahNumber))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(surahNames[track.surahNumber - 1])
                    .setArtist("${source.reciterName} · ${source.style} · ${playlist.title}")
                    .build()
            )
            .build()
    }
    controller.setMediaItems(items, 0, 0L)
    controller.prepare()
    controller.play()
}


internal fun playQuranFoundationPlaylist(
    controller: MediaController,
    reciter: QFChapterReciter,
    audioFiles: List<QFChapterAudioFile>,
    startChapter: Int
) {
    require(audioFiles.isNotEmpty()) { "No chapter audio files are available for this reciter" }
    require(startChapter in 1..114) { "Chapter number must be between 1 and 114" }

    val displayName = reciter.translatedName ?: reciter.name
    val styleName = when (reciter.style?.lowercase()) {
        "murattal" -> "مرتل"
        "mujawwad" -> "مجود"
        else -> reciter.style ?: ""
    }
    val artist = listOf(displayName, styleName, reciter.qirat.orEmpty(), "Quran Foundation")
        .filter { it.isNotBlank() }
        .joinToString(" · ")

    val playlist = audioFiles.map { file ->
        MediaItem.Builder()
            .setMediaId("qf-${reciter.id}-${file.chapterId}")
            .setUri(file.audioUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(surahNames[file.chapterId - 1])
                    .setArtist(artist)
                    .build()
            )
            .build()
    }
    val startIndex = audioFiles.indexOfFirst { it.chapterId == startChapter }.let {
        if (it >= 0) it else 0
    }
    controller.setMediaItems(playlist, startIndex, 0L)
    controller.prepare()
    controller.play()
}

internal fun playQuranRecitation(controller: MediaController, startSurah: Int, source: QuranAudioSource, availableSurahs: List<Int> = (1..114).toList()) {
    require(startSurah in 1..114) { "Surah number must be between 1 and 114" }
    val orderedSurahs = availableSurahs.filter { it in 1..114 }.distinct().sorted()
    require(orderedSurahs.isNotEmpty()) { "No surahs are available for this recording" }
    val playlist = orderedSurahs.map { number ->
        val name = surahNames[number - 1]
        MediaItem.Builder()
            .setMediaId("${source.id}-$number")
            .setUri(source.baseUrl + String.format(Locale.US, "%03d.mp3", number))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(name)
                    .setArtist("${source.reciterName} · ${source.style} · MP3Quran")
                    .build()
            )
            .build()
    }
    val startIndex = orderedSurahs.indexOf(startSurah).coerceAtLeast(0)
    controller.setMediaItems(playlist, startIndex, 0L)
    controller.prepare()
    controller.play()
}

@Composable
internal fun QuranAudioPlayerBar(
    controller: MediaController?,
    selectedSource: QuranAudioSource,
    onSourceSelected: (QuranAudioSource) -> Unit,
    modifier: Modifier = Modifier,
    availableSources: List<QuranAudioSource> = quranAudioSources
) {
    if (controller == null) return

    var isPlaying by remember(controller) { mutableStateOf(controller.isPlaying) }
    var title by remember(controller) {
        mutableStateOf(controller.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty())
    }
    var position by remember(controller) { mutableStateOf(0L) }
    var duration by remember(controller) { mutableStateOf(0L) }
    var speed by remember(controller) { mutableStateOf(controller.playbackParameters.speed) }
    var repeatMode by remember(controller) { mutableStateOf(controller.repeatMode) }
    var speedMenuExpanded by remember { mutableStateOf(false) }
    var timerMenuExpanded by remember { mutableStateOf(false) }
    var sourceMenuExpanded by remember { mutableStateOf(false) }
    var curatedMenuExpanded by remember { mutableStateOf(false) }
    var artist by remember(controller) {
        mutableStateOf(controller.currentMediaItem?.mediaMetadata?.artist?.toString().orEmpty())
    }
    var sleepTimerLabel by remember { mutableStateOf("مؤقت النوم") }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                title = mediaItem?.mediaMetadata?.title?.toString().orEmpty()
                artist = mediaItem?.mediaMetadata?.artist?.toString().orEmpty()
                position = 0L
                duration = controller.duration.coerceAtLeast(0L)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                duration = controller.duration.coerceAtLeast(0L)
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                speed = playbackParameters.speed
            }

            override fun onRepeatModeChanged(mode: Int) {
                repeatMode = mode
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
                    Text(artist.ifBlank { "${selectedSource.reciterName} · ${selectedSource.style}" }, color = AudioMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                androidx.compose.foundation.layout.Box {
                    TextButton(onClick = { sourceMenuExpanded = true }) {
                        Text(
                            "القارئ: ${selectedSource.reciterName} · ${selectedSource.style}",
                            color = AudioGold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    DropdownMenu(
                        expanded = sourceMenuExpanded,
                        onDismissRequest = { sourceMenuExpanded = false }
                    ) {
                        availableSources.forEach { source ->
                            DropdownMenuItem(
                                text = { Text("${source.reciterName} · ${source.style}") },
                                onClick = {
                                    onSourceSelected(source)
                                    if (controller.mediaItemCount > 0) {
                                        val currentSurah = surahNumberFromMediaId(controller.currentMediaItem?.mediaId) ?: 1
                                        playQuranRecitation(controller, currentSurah, source)
                                    }
                                    sourceMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                androidx.compose.foundation.layout.Box {
                    TextButton(onClick = { curatedMenuExpanded = true }) {
                        Text("تلاوات مختارة", color = AudioGold, fontSize = 11.sp)
                    }
                    DropdownMenu(
                        expanded = curatedMenuExpanded,
                        onDismissRequest = { curatedMenuExpanded = false }
                    ) {
                        curatedQuranPlaylists.forEach { playlist ->
                            DropdownMenuItem(
                                text = { Text(playlist.title) },
                                onClick = {
                                    playCuratedQuranPlaylist(controller, playlist)
                                    val firstSource = quranAudioSources.firstOrNull {
                                        it.id == playlist.tracks.first().sourceId
                                    }
                                    if (firstSource != null) onSourceSelected(firstSource)
                                    curatedMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                color = AudioGold,
                trackColor = AudioMuted.copy(alpha = 0.25f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                androidx.compose.foundation.layout.Box {
                    TextButton(onClick = { speedMenuExpanded = true }) {
                        Text("${speed}×", color = AudioGold, fontSize = 11.sp)
                    }
                    DropdownMenu(
                        expanded = speedMenuExpanded,
                        onDismissRequest = { speedMenuExpanded = false }
                    ) {
                        listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { selectedSpeed ->
                            DropdownMenuItem(
                                text = { Text("${selectedSpeed}×") },
                                onClick = {
                                    controller.setPlaybackParameters(PlaybackParameters(selectedSpeed, 1f))
                                    speed = selectedSpeed
                                    speedMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                TextButton(
                    onClick = {
                        controller.repeatMode = if (controller.repeatMode == Player.REPEAT_MODE_ONE) {
                            Player.REPEAT_MODE_OFF
                        } else {
                            Player.REPEAT_MODE_ONE
                        }
                    }
                ) {
                    Icon(Icons.Outlined.RepeatOne, contentDescription = null, tint = AudioGold)
                    Text(
                        if (repeatMode == Player.REPEAT_MODE_ONE) "تكرار السورة" else "التكرار متوقف",
                        color = AudioMuted,
                        fontSize = 10.sp
                    )
                }
                androidx.compose.foundation.layout.Box {
                    TextButton(onClick = { timerMenuExpanded = true }) {
                        Text(sleepTimerLabel, color = AudioGold, fontSize = 10.sp)
                    }
                    DropdownMenu(
                        expanded = timerMenuExpanded,
                        onDismissRequest = { timerMenuExpanded = false }
                    ) {
                        listOf(15, 30, 45, 60, 90).forEach { minutes ->
                            DropdownMenuItem(
                                text = { Text("${minutes} دقيقة") },
                                onClick = {
                                    sleepTimerLabel = if (QuranPlaybackService.scheduleSleepTimer(minutes)) {
                                        "المؤقت: ${minutes} د"
                                    } else {
                                        "ابدأ التلاوة أولًا"
                                    }
                                    timerMenuExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("إلغاء المؤقت") },
                            onClick = {
                                QuranPlaybackService.cancelSleepTimer()
                                sleepTimerLabel = "مؤقت النوم"
                                timerMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
