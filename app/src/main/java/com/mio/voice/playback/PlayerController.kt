package com.mio.voice.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.mio.voice.data.QueueSegment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentIndex: Int = 0,
    val currentText: String = "",
    val readyCount: Int = 0
)

class PlayerController(context: Context) {
    private val appContext = context.applicationContext
    private val player = ExoPlayer.Builder(appContext).build()
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state
    private var activeSegments: List<QueueSegment> = emptyList()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateState()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateState()
            }
        })
    }

    fun setQueue(segments: List<QueueSegment>) {
        activeSegments = segments.filter { it.audioFile != null }
        player.stop()
        player.clearMediaItems()
        activeSegments.forEach { segment ->
            val mediaItem = MediaItem.fromUri(Uri.fromFile(segment.audioFile))
            player.addMediaItem(mediaItem)
        }
        player.prepare()
        updateState()
    }

    fun play() {
        if (player.mediaItemCount > 0) player.play()
        updateState()
    }

    fun pause() {
        player.pause()
        updateState()
    }

    fun previous() {
        if (player.mediaItemCount > 0) player.seekToPreviousMediaItem()
        updateState()
    }

    fun next() {
        if (player.mediaItemCount > 0) player.seekToNextMediaItem()
        updateState()
    }

    fun stop() {
        player.stop()
        updateState()
    }

    fun release() {
        player.release()
    }

    fun playFile(file: File) {
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
        player.prepare()
        player.play()
        activeSegments = listOf(QueueSegment("test", "Connection test", file, com.mio.voice.data.SegmentStatus.Ready))
        updateState()
    }

    private fun updateState() {
        val index = player.currentMediaItemIndex.coerceAtLeast(0)
        _state.value = PlaybackState(
            isPlaying = player.isPlaying,
            currentIndex = index,
            currentText = activeSegments.getOrNull(index)?.text.orEmpty(),
            readyCount = activeSegments.size
        )
    }
}
