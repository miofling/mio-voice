package com.mio.voice.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mio.voice.cache.AudioCache
import com.mio.voice.core.TextSegmenter
import com.mio.voice.data.AppSettings
import com.mio.voice.data.AppSettingsStore
import com.mio.voice.data.CredentialReadResult
import com.mio.voice.data.QueueSegment
import com.mio.voice.data.SegmentStatus
import com.mio.voice.data.SecureCredentialStore
import com.mio.voice.data.TtsRequest
import com.mio.voice.data.VoiceProfile
import com.mio.voice.playback.PlaybackState
import com.mio.voice.playback.PlayerController
import com.mio.voice.provider.FakeTtsProvider
import com.mio.voice.provider.MiniMaxTtsProvider
import com.mio.voice.provider.TtsProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class HomeMode { Text, Words }

data class AppUiState(
    val settings: AppSettings = AppSettings(),
    val apiKeyInput: String = "",
    val credentialMessage: String? = null,
    val homeMode: HomeMode = HomeMode.Text,
    val textInput: String = "",
    val wordInput: String = "",
    val repeatCount: Int = 2,
    val repeatPauseMs: Int = 400,
    val wordPauseMs: Int = 900,
    val selectedVoiceProfileId: String? = null,
    val manualVoiceId: String = "",
    val modelInput: String = "",
    val speed: Float = 1.0f,
    val emotion: String? = null,
    val segments: List<QueueSegment> = emptyList(),
    val isGenerating: Boolean = false,
    val generatedCount: Int = 0,
    val totalCount: Int = 0,
    val statusMessage: String? = null,
    val playback: PlaybackState = PlaybackState(),
    val voiceNameDraft: String = "",
    val voiceIdDraft: String = "",
    val editingVoiceId: String? = null
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsStore = AppSettingsStore(application)
    private val credentialStore = SecureCredentialStore(application)
    private val audioCache = AudioCache(application)
    private val playerController = PlayerController(application)
    private val fakeProvider = FakeTtsProvider()
    private val miniMaxProvider = MiniMaxTtsProvider()
    private val segmenter = TextSegmenter()
    private var generationJob: Job? = null

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsStore.settings.collect { settings ->
                _uiState.update { state ->
                    state.copy(
                        settings = settings,
                        selectedVoiceProfileId = state.selectedVoiceProfileId ?: settings.defaultVoiceProfileId,
                        manualVoiceId = state.manualVoiceId.ifBlank { settings.defaultVoiceId },
                        modelInput = state.modelInput.ifBlank { settings.model },
                        speed = if (state.speed == 1.0f) settings.defaultSpeed else state.speed,
                        emotion = state.emotion ?: settings.defaultEmotion
                    )
                }
            }
        }
        viewModelScope.launch {
            playerController.state.collect { playback ->
                _uiState.update { it.copy(playback = playback) }
            }
        }
        viewModelScope.launch {
            when (val credential = credentialStore.readApiKey()) {
                is CredentialReadResult.Available -> Unit
                is CredentialReadResult.DecryptionFailed -> {
                    _uiState.update { it.copy(credentialMessage = credential.message) }
                }
            }
        }
    }

    fun updateText(value: String) = _uiState.update { it.copy(textInput = value) }
    fun updateWordInput(value: String) = _uiState.update { it.copy(wordInput = value) }
    fun updateMode(mode: HomeMode) = _uiState.update { it.copy(homeMode = mode) }
    fun updateManualVoiceId(value: String) = _uiState.update { it.copy(manualVoiceId = value) }
    fun updateModel(value: String) = _uiState.update { it.copy(modelInput = value) }
    fun updateSpeed(value: Float) = _uiState.update { it.copy(speed = value) }
    fun updateEmotion(value: String?) = _uiState.update { it.copy(emotion = value) }
    fun updateRepeatCount(value: Int) = _uiState.update { it.copy(repeatCount = value.coerceIn(1, 5)) }
    fun updateRepeatPause(value: Int) = _uiState.update { it.copy(repeatPauseMs = value.coerceIn(0, 5_000)) }
    fun updateWordPause(value: Int) = _uiState.update { it.copy(wordPauseMs = value.coerceIn(0, 10_000)) }
    fun updateApiKey(value: String) = _uiState.update { it.copy(apiKeyInput = value) }
    fun updateVoiceDraft(name: String, voiceId: String) = _uiState.update { it.copy(voiceNameDraft = name, voiceIdDraft = voiceId) }
    fun selectVoice(profileId: String?) = _uiState.update { state ->
        val profile = state.settings.voices.firstOrNull { it.id == profileId }
        state.copy(selectedVoiceProfileId = profileId, manualVoiceId = profile?.voiceId ?: state.manualVoiceId)
    }

    fun saveSettings(
        baseUrl: String,
        endpointPath: String,
        model: String,
        defaultVoiceId: String,
        defaultSpeed: Float,
        defaultEmotion: String?,
        useFakeProvider: Boolean
    ) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            settingsStore.save(
                current.copy(
                    baseUrl = baseUrl,
                    endpointPath = endpointPath,
                    model = model,
                    defaultVoiceId = defaultVoiceId,
                    defaultSpeed = defaultSpeed,
                    defaultEmotion = defaultEmotion,
                    useFakeProvider = useFakeProvider
                )
            )
            val apiKey = _uiState.value.apiKeyInput
            if (apiKey.isNotBlank()) credentialStore.saveApiKey(apiKey)
            _uiState.update { it.copy(statusMessage = "Settings saved.", apiKeyInput = "") }
        }
    }

    fun startEditVoice(profile: VoiceProfile) {
        _uiState.update {
            it.copy(
                editingVoiceId = profile.id,
                voiceNameDraft = profile.displayName,
                voiceIdDraft = profile.voiceId
            )
        }
    }

    fun saveVoice(makeDefault: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.voiceIdDraft.isBlank()) {
                _uiState.update { it.copy(statusMessage = "voice_id is required.") }
                return@launch
            }
            val profile = VoiceProfile(
                id = state.editingVoiceId ?: UUID.randomUUID().toString(),
                displayName = state.voiceNameDraft.ifBlank { state.voiceIdDraft },
                voiceId = state.voiceIdDraft
            )
            settingsStore.upsertVoice(profile, makeDefault)
            _uiState.update {
                it.copy(
                    editingVoiceId = null,
                    voiceNameDraft = "",
                    voiceIdDraft = "",
                    statusMessage = "Voice saved."
                )
            }
        }
    }

    fun deleteVoice(id: String) {
        viewModelScope.launch {
            settingsStore.deleteVoice(id)
            _uiState.update { it.copy(statusMessage = "Voice deleted.") }
        }
    }

    fun clearCredentials() {
        viewModelScope.launch {
            credentialStore.clear()
            _uiState.update { it.copy(apiKeyInput = "", statusMessage = "Credentials cleared.") }
        }
    }

    fun clearAudioCache() {
        viewModelScope.launch {
            audioCache.clear()
            _uiState.update { it.copy(statusMessage = "Audio cache cleared.") }
        }
    }

    fun generate() {
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val state = _uiState.value
            val items = if (state.homeMode == HomeMode.Text) {
                segmenter.split(state.textInput)
            } else {
                state.wordInput.lines().map { it.trim() }.filter { it.isNotEmpty() }
            }
            if (items.isEmpty()) {
                _uiState.update { it.copy(statusMessage = "Enter text first.") }
                return@launch
            }
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    generatedCount = 0,
                    totalCount = items.size,
                    segments = items.mapIndexed { index, text ->
                        QueueSegment(index.toString(), text, null, SegmentStatus.Pending)
                    },
                    statusMessage = null
                )
            }
            try {
                val queue = if (state.homeMode == HomeMode.Text) {
                    generateTextSegments(items)
                } else {
                    generateWordSegments(items, state.repeatCount, state.repeatPauseMs, state.wordPauseMs)
                }
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        segments = queue,
                        statusMessage = "Generated ${queue.count { segment -> segment.status == SegmentStatus.Ready }} playable items."
                    )
                }
                playerController.setQueue(queue.filter { it.status == SegmentStatus.Ready })
            } catch (error: Exception) {
                _uiState.update { it.copy(isGenerating = false, statusMessage = sanitize(error)) }
            }
        }
    }

    fun retrySegment(segmentId: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val segment = state.segments.firstOrNull { it.id == segmentId } ?: return@launch
            val request = segment.request ?: buildRequest(segment.text, state)
            updateSegment(segmentId) { it.copy(status = SegmentStatus.Generating, errorMessage = null) }
            try {
                val file = audioCache.getOrGenerate(request, providerFor(state.settings))
                updateSegment(segmentId) { it.copy(status = SegmentStatus.Ready, audioFile = file, request = request) }
                playerController.setQueue(_uiState.value.segments.filter { it.status == SegmentStatus.Ready })
            } catch (error: Exception) {
                updateSegment(segmentId) { it.copy(status = SegmentStatus.Failed, errorMessage = sanitize(error), request = request) }
            }
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        generationJob = null
        playerController.stop()
        _uiState.update { it.copy(isGenerating = false, statusMessage = "Stopped.") }
    }

    fun play() = playerController.play()
    fun pause() = playerController.pause()
    fun previous() = playerController.previous()
    fun next() = playerController.next()

    fun testConnection(
        baseUrl: String? = null,
        endpointPath: String? = null,
        model: String? = null,
        defaultVoiceId: String? = null,
        defaultSpeed: Float? = null,
        defaultEmotion: String? = null,
        useFakeProvider: Boolean? = null
    ) {
        viewModelScope.launch {
            val state = _uiState.value
            val settings = state.settings.copy(
                baseUrl = baseUrl ?: state.settings.baseUrl,
                endpointPath = endpointPath ?: state.settings.endpointPath,
                model = model ?: state.settings.model,
                defaultVoiceId = defaultVoiceId ?: state.settings.defaultVoiceId,
                defaultSpeed = defaultSpeed ?: state.settings.defaultSpeed,
                defaultEmotion = defaultEmotion ?: state.settings.defaultEmotion,
                useFakeProvider = useFakeProvider ?: state.settings.useFakeProvider
            )
            val apiKey = state.apiKeyInput.takeIf { it.isNotBlank() } ?: readApiKeyOrNull()
            val config = settings.providerConfig(apiKey)
            try {
                val result = providerFor(settings).testConnection(config)
                val file = audioCache.writeTemporary("test_connection", result.audioBytes, result.audioFormat)
                playerController.playFile(file)
                _uiState.update { it.copy(statusMessage = "Connection test generated audio and started playback.") }
            } catch (error: Exception) {
                _uiState.update { it.copy(statusMessage = sanitize(error)) }
            }
        }
    }

    override fun onCleared() {
        playerController.release()
        super.onCleared()
    }

    private suspend fun generateTextSegments(items: List<String>): List<QueueSegment> {
        val generated = mutableListOf<QueueSegment>()
        items.forEachIndexed { index, text ->
            val id = index.toString()
            val request = buildRequest(text, _uiState.value)
            updateSegment(id) { it.copy(status = SegmentStatus.Generating, request = request) }
            try {
                val file = audioCache.getOrGenerate(request, providerFor(_uiState.value.settings))
                val ready = QueueSegment(id, text, file, SegmentStatus.Ready, request = request)
                generated += ready
                updateSegment(id) { ready }
                _uiState.update { it.copy(generatedCount = it.generatedCount + 1) }
            } catch (error: Exception) {
                val failed = QueueSegment(id, text, null, SegmentStatus.Failed, sanitize(error), request)
                generated += failed
                updateSegment(id) { failed }
            }
        }
        return generated
    }

    private suspend fun generateWordSegments(
        words: List<String>,
        repeatCount: Int,
        repeatPauseMs: Int,
        wordPauseMs: Int
    ): List<QueueSegment> {
        val uniqueFiles = mutableMapOf<String, QueueSegment>()
        words.distinct().forEachIndexed { index, word ->
            val request = buildRequest(word, _uiState.value)
            updateSegment(index.toString()) { it.copy(status = SegmentStatus.Generating, request = request) }
            try {
                val file = audioCache.getOrGenerate(request, providerFor(_uiState.value.settings))
                uniqueFiles[word] = QueueSegment("word-$index", word, file, SegmentStatus.Ready, request = request)
                _uiState.update { it.copy(generatedCount = it.generatedCount + 1) }
            } catch (error: Exception) {
                uniqueFiles[word] = QueueSegment("word-$index", word, null, SegmentStatus.Failed, sanitize(error), request)
            }
        }

        val queue = mutableListOf<QueueSegment>()
        words.forEachIndexed { wordIndex, word ->
            val source = uniqueFiles.getValue(word)
            if (source.status == SegmentStatus.Ready && source.audioFile != null) {
                repeat(repeatCount) { repeatIndex ->
                    queue += source.copy(id = "$wordIndex-$repeatIndex", text = word)
                    if (repeatIndex < repeatCount - 1 && repeatPauseMs > 0) {
                        queue += QueueSegment(
                            id = "$wordIndex-$repeatIndex-repeat-pause",
                            text = "Pause",
                            audioFile = audioCache.silence(repeatPauseMs),
                            status = SegmentStatus.Ready
                        )
                    }
                }
                if (wordPauseMs > 0) {
                    queue += QueueSegment(
                        id = "$wordIndex-word-pause",
                        text = "Pause",
                        audioFile = audioCache.silence(wordPauseMs),
                        status = SegmentStatus.Ready
                    )
                }
            } else {
                queue += source.copy(id = "$wordIndex-failed")
            }
        }
        return queue
    }

    private suspend fun buildRequest(text: String, state: AppUiState): TtsRequest {
        val apiKey = readApiKeyOrNull()
        val config = state.settings.providerConfig(apiKey)
        val selectedVoice = state.settings.voices.firstOrNull { it.id == state.selectedVoiceProfileId }
        val voiceId = selectedVoice?.voiceId ?: state.manualVoiceId.ifBlank { state.settings.defaultVoiceId }
        return TtsRequest(
            providerProfileId = if (state.settings.useFakeProvider) "fake" else "minimax",
            config = config,
            text = text,
            voiceId = voiceId,
            model = state.modelInput.ifBlank { state.settings.model },
            speed = state.speed,
            emotion = state.emotion,
            audioFormat = "wav"
        )
    }

    private fun providerFor(settings: AppSettings): TtsProvider {
        return if (settings.useFakeProvider) fakeProvider else miniMaxProvider
    }

    private suspend fun readApiKeyOrNull(): String? =
        when (val credential = credentialStore.readApiKey()) {
            is CredentialReadResult.Available -> credential.apiKey
            is CredentialReadResult.DecryptionFailed -> null
        }

    private fun updateSegment(id: String, transform: (QueueSegment) -> QueueSegment) {
        _uiState.update { state ->
            state.copy(segments = state.segments.map { if (it.id == id) transform(it) else it })
        }
    }

    private fun sanitize(error: Throwable): String =
        error.message
            ?.replace(Regex("Bearer\\s+[^\\s]+", RegexOption.IGNORE_CASE), "Bearer ***")
            ?.replace(Regex("sk-[A-Za-z0-9_-]+"), "sk-***")
            ?: "Operation failed."
}
