package com.mio.voice.playback

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.mio.voice.data.QueueSegment
import com.mio.voice.data.generation.GeneratedAudioRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentIndex: Int = 0,
    val currentText: String = "",
    val readyCount: Int = 0,
    val activeGenerationGroupId: String? = null,
    val currentGroupPositionMs: Long = 0L,
    val currentGroupDurationMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val errorMessage: String? = null
)

class PlayerController(context: Context) {
    private val player = ExoPlayer.Builder(context.applicationContext).build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state
    private var activeEntries: List<GroupPlaybackEntry> = emptyList()
    private var activeTimeline: GroupPlaybackTimeline? = null
    private var activeGroupId: String? = null
    private var progressJob: Job? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateState()
                if (isPlaying) startProgressUpdates() else stopProgressUpdates()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = updateState()

            override fun onPlaybackStateChanged(playbackState: Int) = updateState()

            override fun onPlayerError(error: PlaybackException) {
                stopProgressUpdates()
                _state.value = _state.value.copy(
                    isPlaying = false,
                    errorMessage = "本地音频播放失败。"
                )
                Log.w(TAG, "Local audio playback failed", error)
            }
        })
    }

    fun setQueue(segments: List<QueueSegment>) {
        val entries = segments.mapNotNull { segment ->
            val file = segment.audioFile ?: return@mapNotNull null
            if (!file.isFile || file.length() <= 0L) {
                Log.w(TAG, "Skipping missing audio file: ${file.name}")
                return@mapNotNull null
            }
            GroupPlaybackEntry(segment.id, segment.text, file, 0L, 0)
        }
        replacePlaylist(entries, groupId = null, timeline = null, autoPlay = false)
    }

    suspend fun playGenerationGroup(groupId: String, segments: List<GeneratedAudioRecord>): Result<Unit> =
        runCatching {
            // 文件 stat（File.isFile/length）是磁盘 IO，放到 IO 线程，避免阻塞主线程造成 UI 卡顿。
            val plan = withContext(Dispatchers.IO) {
                GenerationGroupPlaybackPlan.create(groupId, segments)
            }
            // ExoPlayer 操作必须在主线程执行。
            withContext(Dispatchers.Main.immediate) {
                replacePlaylist(plan.entries, plan.groupId, plan.timeline, autoPlay = true)
            }
        }

    suspend fun setGenerationGroup(groupId: String, segments: List<GeneratedAudioRecord>): Result<Unit> =
        runCatching {
            val plan = withContext(Dispatchers.IO) {
                GenerationGroupPlaybackPlan.create(groupId, segments)
            }
            withContext(Dispatchers.Main.immediate) {
                replacePlaylist(plan.entries, plan.groupId, plan.timeline, autoPlay = false)
            }
        }

    fun play() {
        if (player.mediaItemCount == 0) {
            updateState()
            return
        }
        // stop() 之后 ExoPlayer 进入 IDLE，停止时丢弃了已准备的媒体；
        // 播放结束后进入 ENDED。两种情况下直接 play() 不会发声，需先 prepare/seek。
        when (player.playbackState) {
            Player.STATE_IDLE -> player.prepare()
            Player.STATE_ENDED -> player.seekTo(0, 0L)
            else -> Unit
        }
        player.play()
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

    fun seekGroupTo(positionMs: Long): Result<Unit> = runCatching {
        val timeline = activeTimeline ?: error("当前没有可定位的生成组。")
        val target = timeline.toSegmentPosition(positionMs)
        player.seekTo(target.segmentIndex, target.positionMs)
        updateState()
    }

    fun seekGroupBy(deltaMs: Long): Result<Unit> {
        val timeline = activeTimeline ?: return Result.failure(
            IllegalStateException("当前没有可定位的生成组。")
        )
        val target = (currentGroupPositionMs() + deltaMs).coerceIn(0L, timeline.totalDurationMs)
        return seekGroupTo(target)
    }

    fun currentGroupPositionMs(): Long {
        val timeline = activeTimeline ?: return 0L
        val index = player.currentMediaItemIndex.coerceIn(0, timeline.segmentDurationsMs.lastIndex)
        return timeline.toGlobalPosition(index, player.currentPosition.coerceAtLeast(0L))
    }

    fun currentGroupDurationMs(): Long = activeTimeline?.totalDurationMs ?: 0L

    fun setPlaybackSpeed(speed: Float): Result<Unit> = runCatching {
        require(speed in 0.25f..3f) { "播放倍速必须在 0.25x 到 3x 之间。" }
        player.setPlaybackSpeed(speed)
        updateState()
    }

    fun stop() {
        stopProgressUpdates()
        player.stop()
        updateState()
    }

    fun clearGenerationGroup(groupId: String) {
        if (activeGroupId == groupId) {
            replacePlaylist(emptyList(), groupId = null, timeline = null, autoPlay = false)
        }
    }

    fun release() {
        stopProgressUpdates()
        player.release()
        activeEntries = emptyList()
        activeTimeline = null
        activeGroupId = null
        _state.value = PlaybackState()
        scope.cancel()
    }

    fun playFile(file: File, text: String = "本地音频"): Result<Unit> = runCatching {
        require(file.isFile && file.length() > 0L) { "本地音频文件不存在或为空。" }
        val entry = GroupPlaybackEntry("local", text, file, 0L, 0)
        replacePlaylist(listOf(entry), groupId = null, timeline = null, autoPlay = true)
    }.onFailure { error ->
        _state.value = PlaybackState(errorMessage = error.message)
    }

    fun playLocalPath(path: String, text: String = "本地音频"): Result<Unit> =
        playFile(File(path), text)

    private fun replacePlaylist(
        entries: List<GroupPlaybackEntry>,
        groupId: String?,
        timeline: GroupPlaybackTimeline?,
        autoPlay: Boolean
    ) {
        stopProgressUpdates()
        player.stop()
        player.clearMediaItems()
        activeEntries = entries
        activeGroupId = groupId
        activeTimeline = timeline
        entries.forEach { entry ->
            player.addMediaItem(
                MediaItem.Builder()
                    .setMediaId(entry.recordId)
                    .setUri(Uri.fromFile(entry.file))
                    .build()
            )
        }
        if (entries.isNotEmpty()) {
            player.prepare()
            if (autoPlay) player.play()
        }
        updateState()
    }

    private fun startProgressUpdates() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (isActive && player.isPlaying) {
                updateState()
                delay(PROGRESS_INTERVAL_MS)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun updateState() {
        val index = if (activeEntries.isEmpty()) 0 else {
            player.currentMediaItemIndex.coerceIn(0, activeEntries.lastIndex)
        }
        _state.value = PlaybackState(
            isPlaying = player.isPlaying,
            currentIndex = index,
            currentText = activeEntries.getOrNull(index)?.text.orEmpty(),
            readyCount = activeEntries.size,
            activeGenerationGroupId = activeGroupId,
            currentGroupPositionMs = currentGroupPositionMs(),
            currentGroupDurationMs = currentGroupDurationMs(),
            playbackSpeed = player.playbackParameters.speed,
            errorMessage = null
        )
    }

    private companion object {
        const val TAG = "PlayerController"
        const val PROGRESS_INTERVAL_MS = 250L
    }
}
