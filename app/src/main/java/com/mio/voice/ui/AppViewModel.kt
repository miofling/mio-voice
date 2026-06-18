package com.mio.voice.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mio.voice.cache.AudioCache
import com.mio.voice.core.TechnicalTextChunker
import com.mio.voice.data.AppSettings
import com.mio.voice.data.AppSettingsStore
import com.mio.voice.data.CredentialReadResult
import com.mio.voice.data.CredentialSlot
import com.mio.voice.data.EmotionPreset
import com.mio.voice.data.QueueSegment
import com.mio.voice.data.ResolvedVoiceSettings
import com.mio.voice.data.SegmentStatus
import com.mio.voice.data.SecureCredentialStore
import com.mio.voice.data.TtsRequest
import com.mio.voice.data.VoiceLibrary
import com.mio.voice.data.VoiceProfile
import com.mio.voice.director.DirectorDraftSegment
import com.mio.voice.director.DirectorPresetInfo
import com.mio.voice.director.DirectorRequest
import com.mio.voice.director.DirectorResultValidator
import com.mio.voice.director.DirectorValidationResult
import com.mio.voice.director.OpenAiCompatibleDirectorProvider
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

enum class TextGenerationMode { FixedPreset, AiDirector }

private val TTS_MODEL_PRESETS = listOf(
    "speech-2.8-hd",
    "speech-2.8-turbo",
    "speech-2.6-hd",
    "speech-2.6-turbo",
    "speech-02-hd",
    "speech-02-turbo",
    "speech-01-hd",
    "speech-01-turbo"
)

data class AppUiState(
    val settings: AppSettings = AppSettings(),
    val apiKeyInput: String = "",
    val aiApiKeyInput: String = "",
    val credentialMessage: String? = null,
    val homeMode: HomeMode = HomeMode.Text,
    val textGenerationMode: TextGenerationMode = TextGenerationMode.FixedPreset,
    val textInput: String = "",
    val wordInput: String = "",
    val repeatCount: Int = 2,
    val repeatPauseMs: Int = 400,
    val wordPauseMs: Int = 900,
    val selectedVoiceProfileId: String? = null,
    val selectedPresetId: String? = null,
    val modelInput: String = "",
    val segments: List<QueueSegment> = emptyList(),
    val isGenerating: Boolean = false,
    val generatedCount: Int = 0,
    val totalCount: Int = 0,
    val statusMessage: String? = null,
    val playback: PlaybackState = PlaybackState(),
    val voiceNameDraft: String = "",
    val voiceIdDraft: String = "",
    val editingVoiceId: String? = null,
    val presetLabelDraft: String = "",
    val presetEmotionDraft: String = "neutral",
    val presetSpeedDraft: Float = 1.0f,
    val presetPitchDraft: Int = 0,
    val presetPreviewTextDraft: String = VoiceLibrary.DEFAULT_PREVIEW_TEXT,
    val presetDescriptionDraft: String = "",
    val editingPresetId: String? = null,
    val fetchedModels: List<String> = TTS_MODEL_PRESETS,
    val isFetchingModels: Boolean = false,
    val isAnalyzingDirector: Boolean = false,
    val directorSegments: List<DirectorDraftSegment> = emptyList(),
    val directorWarnings: List<String> = emptyList(),
    val directorValidationMessage: String? = null,
    val directorVoiceProfileId: String? = null
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsStore = AppSettingsStore(application)
    private val credentialStore = SecureCredentialStore(application)
    private val audioCache = AudioCache(application)
    private val playerController = PlayerController(application)
    private val fakeProvider = FakeTtsProvider()
    private val miniMaxProvider = MiniMaxTtsProvider()
    private val directorProvider = OpenAiCompatibleDirectorProvider()
    private var generationJob: Job? = null

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsStore.settings.collect { settings ->
                _uiState.update { state ->
                    val selectedVoiceId = state.selectedVoiceProfileId
                        ?: settings.defaultVoiceProfileId
                        ?: settings.voices.firstOrNull()?.id
                    val selectedVoice = settings.voices.firstOrNull { it.id == selectedVoiceId }
                    state.copy(
                        settings = settings,
                        selectedVoiceProfileId = selectedVoiceId,
                        selectedPresetId = state.selectedPresetId
                            ?: selectedVoice?.defaultPresetId
                            ?: selectedVoice?.presets?.firstOrNull()?.id,
                        modelInput = state.modelInput.ifBlank { settings.model }
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
            when (val credential = credentialStore.readApiKey(CredentialSlot.Tts)) {
                is CredentialReadResult.Available -> Unit
                is CredentialReadResult.DecryptionFailed -> {
                    _uiState.update { it.copy(credentialMessage = credential.message) }
                }
            }
            when (val credential = credentialStore.readApiKey(CredentialSlot.Director)) {
                is CredentialReadResult.Available -> Unit
                is CredentialReadResult.DecryptionFailed -> {
                    _uiState.update { it.copy(credentialMessage = credential.message) }
                }
            }
        }
    }

    fun updateText(value: String) = _uiState.update { it.copy(textInput = value) }
    fun updateWordInput(value: String) = _uiState.update { it.copy(wordInput = value) }
    fun updateMode(mode: HomeMode) = _uiState.update {
        it.copy(
            homeMode = mode,
            segments = emptyList(),
            directorSegments = if (mode == HomeMode.Text) it.directorSegments else emptyList(),
            directorWarnings = if (mode == HomeMode.Text) it.directorWarnings else emptyList(),
            directorValidationMessage = if (mode == HomeMode.Text) it.directorValidationMessage else null,
            generatedCount = 0,
            totalCount = 0,
            statusMessage = null
        )
    }
    fun updateTextGenerationMode(mode: TextGenerationMode) = _uiState.update {
        it.copy(
            textGenerationMode = mode,
            segments = emptyList(),
            generatedCount = 0,
            totalCount = 0,
            statusMessage = null
        )
    }
    fun updateModel(value: String) = _uiState.update { it.copy(modelInput = value) }
    fun updateRepeatCount(value: Int) = _uiState.update { it.copy(repeatCount = value.coerceIn(1, 5)) }
    fun updateRepeatPause(value: Int) = _uiState.update { it.copy(repeatPauseMs = value.coerceIn(0, 5_000)) }
    fun updateWordPause(value: Int) = _uiState.update { it.copy(wordPauseMs = value.coerceIn(0, 10_000)) }
    fun updateApiKey(value: String) = _uiState.update { it.copy(apiKeyInput = value) }
    fun updateAiApiKey(value: String) = _uiState.update { it.copy(aiApiKeyInput = value) }
    fun updateVoiceDraft(name: String, voiceId: String) = _uiState.update { it.copy(voiceNameDraft = name, voiceIdDraft = voiceId) }
    fun startNewVoice() = _uiState.update {
        it.copy(
            editingVoiceId = null,
            voiceNameDraft = "",
            voiceIdDraft = ""
        )
    }
    fun selectVoice(profileId: String?) = _uiState.update { state ->
        val profile = state.settings.voices.firstOrNull { it.id == profileId }
        state.copy(
            selectedVoiceProfileId = profileId,
            selectedPresetId = profile?.defaultPresetId ?: profile?.presets?.firstOrNull()?.id
        )
    }
    fun selectPreset(presetId: String?) = _uiState.update { it.copy(selectedPresetId = presetId) }
    fun updatePresetDraft(
        label: String,
        emotion: String,
        speed: Float,
        pitch: Int,
        previewText: String,
        description: String = _uiState.value.presetDescriptionDraft
    ) = _uiState.update {
        it.copy(
            presetLabelDraft = label,
            presetEmotionDraft = emotion,
            presetSpeedDraft = speed.coerceIn(0.5f, 2.0f),
            presetPitchDraft = pitch.coerceIn(-12, 12),
            presetPreviewTextDraft = previewText,
            presetDescriptionDraft = description
        )
    }

    fun saveSettings(
        baseUrl: String,
        endpointPath: String,
        model: String,
        defaultVoiceId: String,
        defaultSpeed: Float,
        defaultEmotion: String?,
        maxCharsPerRequest: Int,
        useFakeProvider: Boolean,
        directorBaseUrl: String,
        directorEndpointPath: String,
        directorModel: String
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
                    maxCharsPerRequest = maxCharsPerRequest.coerceIn(200, 20_000),
                    useFakeProvider = useFakeProvider,
                    directorBaseUrl = directorBaseUrl,
                    directorEndpointPath = directorEndpointPath.ifBlank { "/v1/chat/completions" },
                    directorModel = directorModel
                )
            )
            val apiKey = _uiState.value.apiKeyInput
            if (apiKey.isNotBlank()) credentialStore.saveApiKey(apiKey, CredentialSlot.Tts)
            val aiApiKey = _uiState.value.aiApiKeyInput
            if (aiApiKey.isNotBlank()) credentialStore.saveApiKey(aiApiKey, CredentialSlot.Director)
            _uiState.update { it.copy(statusMessage = "设置已保存。", apiKeyInput = "", aiApiKeyInput = "") }
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
                _uiState.update { it.copy(statusMessage = "请先填写 voice_id。") }
                return@launch
            }
            val profile = VoiceProfile(
                id = state.editingVoiceId ?: UUID.randomUUID().toString(),
                displayName = state.voiceNameDraft.ifBlank { state.voiceIdDraft },
                voiceId = state.voiceIdDraft,
                defaultPresetId = state.settings.voices.firstOrNull { it.id == state.editingVoiceId }?.defaultPresetId.orEmpty(),
                presets = state.settings.voices.firstOrNull { it.id == state.editingVoiceId }?.presets.orEmpty()
            ).let {
                if (state.editingVoiceId == null) {
                    VoiceLibrary.createVoice(
                        id = it.id,
                        displayName = it.displayName,
                        voiceId = it.voiceId,
                        defaultEmotion = state.settings.defaultEmotion,
                        defaultSpeed = state.settings.defaultSpeed
                    )
                } else {
                    VoiceLibrary.normalizeVoice(it, state.settings.defaultEmotion, state.settings.defaultSpeed)
                }
            }
            settingsStore.upsertVoice(profile, makeDefault)
            _uiState.update {
                it.copy(
                    editingVoiceId = null,
                    voiceNameDraft = "",
                    voiceIdDraft = "",
                    statusMessage = "音色已保存。"
                )
            }
        }
    }

    fun startEditPreset(preset: EmotionPreset) {
        _uiState.update {
            it.copy(
                editingPresetId = preset.id,
                presetLabelDraft = preset.label,
                presetEmotionDraft = preset.emotion,
                presetSpeedDraft = preset.speed,
                presetPitchDraft = preset.pitch,
                presetPreviewTextDraft = preset.previewText,
                presetDescriptionDraft = preset.description
            )
        }
    }

    fun startNewPreset() {
        _uiState.update {
            it.copy(
                editingPresetId = null,
                presetLabelDraft = "",
                presetEmotionDraft = "neutral",
                presetSpeedDraft = 1.0f,
                presetPitchDraft = 0,
                presetPreviewTextDraft = VoiceLibrary.DEFAULT_PREVIEW_TEXT,
                presetDescriptionDraft = ""
            )
        }
    }

    fun savePreset(voiceProfileId: String, makeDefault: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value
            val voices = state.settings.voices.toMutableList()
            val index = voices.indexOfFirst { it.id == voiceProfileId }
            if (index < 0) return@launch
            val preset = EmotionPreset(
                id = state.editingPresetId ?: UUID.randomUUID().toString(),
                label = state.presetLabelDraft.ifBlank { "默认" },
                emotion = state.presetEmotionDraft,
                speed = state.presetSpeedDraft,
                pitch = state.presetPitchDraft,
                previewText = state.presetPreviewTextDraft,
                description = state.presetDescriptionDraft
            )
            voices[index] = VoiceLibrary.upsertPreset(voices[index], preset, makeDefault)
            settingsStore.save(state.settings.copy(voices = voices))
            _uiState.update {
                it.copy(
                    editingPresetId = null,
                    presetLabelDraft = "",
                    presetEmotionDraft = "neutral",
                    presetSpeedDraft = 1.0f,
                    presetPitchDraft = 0,
                    presetPreviewTextDraft = VoiceLibrary.DEFAULT_PREVIEW_TEXT,
                    presetDescriptionDraft = "",
                    selectedPresetId = if (makeDefault) preset.id else it.selectedPresetId,
                    statusMessage = "预设已保存。"
                )
            }
        }
    }

    fun deletePreset(voiceProfileId: String, presetId: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val voices = state.settings.voices.toMutableList()
            val index = voices.indexOfFirst { it.id == voiceProfileId }
            if (index < 0) return@launch
            val result = VoiceLibrary.deletePreset(voices[index], presetId)
            voices[index] = result.voice
            if (result.deleted) {
                settingsStore.save(state.settings.copy(voices = voices))
            }
            _uiState.update { it.copy(statusMessage = result.message ?: "预设已删除。") }
        }
    }

    fun setDefaultPreset(voiceProfileId: String, presetId: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val voices = state.settings.voices.toMutableList()
            val index = voices.indexOfFirst { it.id == voiceProfileId }
            if (index < 0) return@launch
            voices[index] = VoiceLibrary.setDefaultPreset(voices[index], presetId)
            settingsStore.save(state.settings.copy(voices = voices))
            _uiState.update { it.copy(selectedPresetId = presetId, statusMessage = "默认预设已更新。") }
        }
    }

    fun previewPreset(voiceProfileId: String, presetId: String?) {
        viewModelScope.launch {
            val state = _uiState.value
            val resolved = resolvePreset(voiceProfileId, presetId)
            if (resolved == null) {
                _uiState.update { it.copy(statusMessage = "请先选择音色和预设。") }
                return@launch
            }
            val text = state.settings.voices
                .firstOrNull { it.id == voiceProfileId }
                ?.presets
                ?.firstOrNull { it.id == resolved.presetId }
                ?.previewText
                ?.takeIf { it.isNotBlank() }
                ?: "Mio Voice 预设试听。"
            try {
                val request = buildRequest(text, state, resolved)
                val file = audioCache.getOrGenerate(request, providerFor(state.settings))
                playerController.playFile(file)
                _uiState.update { it.copy(statusMessage = "预设试听已开始播放。") }
            } catch (error: Exception) {
                _uiState.update { it.copy(statusMessage = sanitize(error)) }
            }
        }
    }

    fun deleteVoice(id: String) {
        viewModelScope.launch {
            settingsStore.deleteVoice(id)
            _uiState.update { it.copy(statusMessage = "音色已删除。") }
        }
    }

    fun clearCredentials() {
        viewModelScope.launch {
            credentialStore.clear(CredentialSlot.Tts)
            _uiState.update { it.copy(apiKeyInput = "", statusMessage = "凭证已清除。") }
        }
    }

    fun clearDirectorCredentials() {
        viewModelScope.launch {
            credentialStore.clear(CredentialSlot.Director)
            _uiState.update { it.copy(aiApiKeyInput = "", statusMessage = "AI 导演凭证已清除。") }
        }
    }

    fun clearAudioCache() {
        viewModelScope.launch {
            audioCache.clear()
            _uiState.update { it.copy(statusMessage = "音频缓存已清理。") }
        }
    }

    fun generate() {
        val current = _uiState.value
        if (current.homeMode == HomeMode.Text && current.textGenerationMode == TextGenerationMode.AiDirector) {
            if (current.directorSegments.isEmpty()) analyzeDirector() else confirmDirectorGeneration()
            return
        }
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val state = _uiState.value
            if (state.homeMode == HomeMode.Text) {
                generatePlainText(state)
                return@launch
            }
            val items = state.wordInput.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (items.isEmpty()) {
                _uiState.update { it.copy(statusMessage = "请先输入文本。") }
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
                val queue = generateWordSegments(items, state.repeatCount, state.repeatPauseMs, state.wordPauseMs)
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        segments = queue,
                        statusMessage = "已生成 ${queue.count { segment -> segment.status == SegmentStatus.Ready }} 个可播放片段。"
                    )
                }
                playerController.setQueue(queue.filter { it.status == SegmentStatus.Ready })
            } catch (error: Exception) {
                _uiState.update { it.copy(isGenerating = false, statusMessage = sanitize(error)) }
            }
        }
    }

    fun analyzeDirector() {
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val state = _uiState.value
            val text = state.textInput
            val voice = selectedVoiceForDirector(state)
            val error = directorPrerequisiteError(state, text, voice)
            if (error != null || voice == null) {
                _uiState.update { it.copy(statusMessage = error ?: "请先选择父音色。") }
                return@launch
            }
            val apiKey = state.aiApiKeyInput.takeIf { it.isNotBlank() } ?: readDirectorApiKeyOrNull()
            val request = DirectorRequest(
                text = text,
                voiceProfileId = voice.id,
                defaultPresetId = voice.defaultPresetId,
                presets = voice.presets.map { preset ->
                    DirectorPresetInfo(
                        presetId = preset.id,
                        label = preset.label,
                        description = preset.description
                    )
                },
                config = state.settings.directorConfig(apiKey)
            )
            _uiState.update {
                it.copy(
                    isAnalyzingDirector = true,
                    directorSegments = emptyList(),
                    directorWarnings = emptyList(),
                    directorValidationMessage = null,
                    directorVoiceProfileId = voice.id,
                    statusMessage = "AI 导演正在分析文本..."
                )
            }
            try {
                val result = directorProvider.analyze(request)
                when (val validation = DirectorResultValidator.validate(text, voice, result)) {
                    is DirectorValidationResult.Valid -> {
                        _uiState.update {
                            it.copy(
                                isAnalyzingDirector = false,
                                directorSegments = validation.segments,
                                directorWarnings = validation.warnings,
                                directorValidationMessage = null,
                                statusMessage = "AI 导演已生成 ${validation.segments.size} 个配音块，请预览后确认生成。"
                            )
                        }
                    }
                    is DirectorValidationResult.Invalid -> {
                        _uiState.update {
                            it.copy(
                                isAnalyzingDirector = false,
                                directorSegments = emptyList(),
                                directorWarnings = emptyList(),
                                directorValidationMessage = validation.message,
                                statusMessage = validation.message
                            )
                        }
                    }
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isAnalyzingDirector = false,
                        directorValidationMessage = sanitize(error),
                        statusMessage = sanitize(error)
                    )
                }
            }
        }
    }

    fun updateDirectorSegmentPreset(segmentId: String, presetId: String) {
        _uiState.update { state ->
            state.copy(
                directorSegments = state.directorSegments.map { segment ->
                    if (segment.id == segmentId) segment.copy(presetId = presetId, warnings = emptyList()) else segment
                }
            )
        }
    }

    fun mergeDirectorSegmentWithNext(segmentId: String) {
        _uiState.update { state ->
            val index = state.directorSegments.indexOfFirst { it.id == segmentId }
            if (index < 0 || index >= state.directorSegments.lastIndex) return@update state
            val merged = state.directorSegments.toMutableList()
            val current = merged[index]
            val next = merged.removeAt(index + 1)
            merged[index] = current.copy(
                text = current.text + next.text,
                warnings = current.warnings + next.warnings
            )
            state.copy(directorSegments = merged.mapIndexed { i, segment -> segment.copy(id = "director-$i") })
        }
    }

    fun discardDirectorResult() {
        _uiState.update {
            it.copy(
                textGenerationMode = TextGenerationMode.FixedPreset,
                directorSegments = emptyList(),
                directorWarnings = emptyList(),
                directorValidationMessage = null,
                directorVoiceProfileId = null,
                statusMessage = "已放弃 AI 导演结果。"
            )
        }
    }

    fun confirmDirectorGeneration() {
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val state = _uiState.value
            val voiceProfileId = state.directorVoiceProfileId ?: state.selectedVoiceProfileId
            if (state.directorSegments.isEmpty() || voiceProfileId == null) {
                _uiState.update { it.copy(statusMessage = "请先运行 AI 导演分析。") }
                return@launch
            }
            val initial = state.directorSegments.map { draft ->
                QueueSegment(
                    id = draft.id,
                    text = draft.text,
                    audioFile = null,
                    status = SegmentStatus.Pending
                )
            }
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    generatedCount = 0,
                    totalCount = initial.size,
                    segments = initial,
                    statusMessage = null
                )
            }
            state.directorSegments.forEach { draft ->
                val resolved = VoiceLibrary.resolvePreset(_uiState.value.settings.voices, voiceProfileId, draft.presetId)
                if (resolved == null) {
                    updateSegment(draft.id) { it.copy(status = SegmentStatus.Failed, errorMessage = "预设解析失败。") }
                    return@forEach
                }
                val request = buildRequest(draft.text, _uiState.value, resolved)
                updateSegment(draft.id) { it.copy(status = SegmentStatus.Generating, request = request) }
                try {
                    val file = audioCache.getOrGenerate(request, providerFor(_uiState.value.settings))
                    updateSegment(draft.id) {
                        it.copy(status = SegmentStatus.Ready, audioFile = file, errorMessage = null, request = request)
                    }
                    _uiState.update { it.copy(generatedCount = it.generatedCount + 1) }
                } catch (error: Exception) {
                    updateSegment(draft.id) {
                        it.copy(status = SegmentStatus.Failed, errorMessage = sanitize(error), request = request)
                    }
                }
            }
            val ready = _uiState.value.segments.filter { it.status == SegmentStatus.Ready }
            playerController.setQueue(ready)
            _uiState.update {
                it.copy(
                    isGenerating = false,
                    statusMessage = "AI 导演生成完成：${ready.size}/${it.totalCount} 段可播放。"
                )
            }
        }
    }

    fun retrySegment(segmentId: String) {
        viewModelScope.launch {
            if (_uiState.value.homeMode == HomeMode.Text && segmentId == TEXT_TASK_ID) {
                generate()
                return@launch
            }
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
        _uiState.update { it.copy(isGenerating = false, statusMessage = "已停止。") }
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
                _uiState.update { it.copy(statusMessage = "测试连接已生成短语音并开始试听。") }
            } catch (error: Exception) {
                _uiState.update { it.copy(statusMessage = sanitize(error)) }
            }
        }
    }

    fun testDirectorConnection(
        baseUrl: String? = null,
        endpointPath: String? = null,
        model: String? = null
    ) {
        viewModelScope.launch {
            val state = _uiState.value
            val settings = state.settings.copy(
                directorBaseUrl = baseUrl ?: state.settings.directorBaseUrl,
                directorEndpointPath = endpointPath ?: state.settings.directorEndpointPath,
                directorModel = model ?: state.settings.directorModel
            )
            val apiKey = state.aiApiKeyInput.takeIf { it.isNotBlank() } ?: readDirectorApiKeyOrNull()
            val error = directorConfigError(settings, apiKey)
            if (error != null) {
                _uiState.update { it.copy(statusMessage = error) }
                return@launch
            }
            try {
                directorProvider.analyze(
                    DirectorRequest(
                        text = "今天很开心。后来有点难过。",
                        voiceProfileId = "test",
                        defaultPresetId = "neutral",
                        presets = listOf(
                            DirectorPresetInfo("neutral", "默认", "平静自然。"),
                            DirectorPresetInfo("happy", "开心", "愉快明亮。")
                        ),
                        config = settings.directorConfig(apiKey)
                    )
                )
                _uiState.update { it.copy(statusMessage = "AI 导演连接测试成功。") }
            } catch (error: Exception) {
                _uiState.update { it.copy(statusMessage = sanitize(error)) }
            }
        }
    }

    fun fetchModels(
        baseUrl: String? = null,
        apiKey: String? = null
    ) {
        viewModelScope.launch {
            val state = _uiState.value
            val settings = state.settings.copy(baseUrl = baseUrl ?: state.settings.baseUrl)
            val effectiveApiKey = apiKey?.takeIf { it.isNotBlank() }
                ?: state.apiKeyInput.takeIf { it.isNotBlank() }
                ?: readApiKeyOrNull()
            val config = settings.providerConfig(effectiveApiKey)
            _uiState.update { it.copy(isFetchingModels = true, statusMessage = "正在拉取模型列表...") }
            try {
                val fetched = miniMaxProvider.fetchModels(config)
                val models = (TTS_MODEL_PRESETS + fetched).distinct()
                _uiState.update {
                    it.copy(
                        isFetchingModels = false,
                        fetchedModels = models,
                        modelInput = it.modelInput.ifBlank { models.firstOrNull().orEmpty() },
                        statusMessage = "已拉取 ${fetched.size} 个模型。"
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isFetchingModels = false,
                        fetchedModels = TTS_MODEL_PRESETS,
                        statusMessage = "${sanitize(error)} 已保留官方 TTS 预设，可继续手动填写模型名。"
                    )
                }
            }
        }
    }

    override fun onCleared() {
        playerController.release()
        super.onCleared()
    }

    private suspend fun generatePlainText(state: AppUiState) {
        val text = state.textInput
        if (text.isBlank()) {
            _uiState.update { it.copy(statusMessage = "请先输入文本。") }
            return
        }
        val chunks = TechnicalTextChunker(state.settings.maxCharsPerRequest.coerceAtLeast(200)).split(text)
        _uiState.update {
            it.copy(
                isGenerating = true,
                generatedCount = 0,
                totalCount = 1,
                segments = listOf(QueueSegment(TEXT_TASK_ID, text, null, SegmentStatus.Generating)),
                statusMessage = null
            )
        }

        val queue = mutableListOf<QueueSegment>()
        var firstError: String? = null
        chunks.forEachIndexed { index, chunk ->
            val request = buildRequest(chunk, _uiState.value)
            try {
                val file = audioCache.getOrGenerate(request, providerFor(_uiState.value.settings))
                queue += QueueSegment("text-tech-$index", "全文", file, SegmentStatus.Ready, request = request)
            } catch (error: Exception) {
                firstError = firstError ?: sanitize(error)
                queue += QueueSegment("text-tech-$index", "全文", null, SegmentStatus.Failed, firstError, request)
            }
        }
        val readyQueue = queue.filter { it.status == SegmentStatus.Ready }
        val status = if (firstError == null) SegmentStatus.Ready else SegmentStatus.Failed
        _uiState.update {
            it.copy(
                isGenerating = false,
                generatedCount = if (firstError == null) 1 else 0,
                totalCount = 1,
                segments = listOf(
                    QueueSegment(
                        id = TEXT_TASK_ID,
                        text = text,
                        audioFile = readyQueue.firstOrNull()?.audioFile,
                        status = status,
                        errorMessage = firstError
                    )
                ),
                statusMessage = if (firstError == null) {
                    if (chunks.size == 1) "全文已生成。"
                    else "全文已生成，后台使用 ${chunks.size} 个技术分块。"
                } else {
                    firstError
                }
            )
        }
        if (firstError == null) {
            playerController.setQueue(readyQueue)
        } else {
            playerController.setQueue(emptyList())
        }
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
                            text = "停顿",
                            audioFile = audioCache.silence(repeatPauseMs),
                            status = SegmentStatus.Ready
                        )
                    }
                }
                if (wordPauseMs > 0) {
                    queue += QueueSegment(
                        id = "$wordIndex-word-pause",
                        text = "停顿",
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

    fun getAvailablePresets(voiceProfileId: String): List<EmotionPreset> =
        VoiceLibrary.getAvailablePresets(_uiState.value.settings.voices, voiceProfileId)

    fun resolvePreset(voiceProfileId: String, presetId: String?): ResolvedVoiceSettings? =
        VoiceLibrary.resolvePreset(_uiState.value.settings.voices, voiceProfileId, presetId)

    private suspend fun buildRequest(
        text: String,
        state: AppUiState,
        resolvedOverride: ResolvedVoiceSettings? = null
    ): TtsRequest {
        val apiKey = readApiKeyOrNull()
        val config = state.settings.providerConfig(apiKey)
        val voiceProfileId = state.selectedVoiceProfileId
            ?: state.settings.defaultVoiceProfileId
            ?: state.settings.voices.firstOrNull()?.id
            ?: error("请先在音色库添加父音色。")
        val resolved = resolvedOverride
            ?: VoiceLibrary.resolvePreset(state.settings.voices, voiceProfileId, state.selectedPresetId)
            ?: error("请先选择可用的音色预设。")
        return TtsRequest(
            providerProfileId = if (state.settings.useFakeProvider) "fake" else "minimax",
            config = config,
            text = text,
            voiceId = resolved.voiceId,
            model = state.modelInput.ifBlank { state.settings.model },
            speed = resolved.speed,
            emotion = resolved.emotion,
            pitch = resolved.pitch,
            audioFormat = "wav"
        )
    }

    private fun selectedVoiceForDirector(state: AppUiState): VoiceProfile? {
        val voiceProfileId = state.selectedVoiceProfileId
            ?: state.settings.defaultVoiceProfileId
            ?: state.settings.voices.firstOrNull()?.id
        return state.settings.voices.firstOrNull { it.id == voiceProfileId }
    }

    private suspend fun directorPrerequisiteError(
        state: AppUiState,
        text: String,
        voice: VoiceProfile?
    ): String? {
        if (text.isBlank()) return "请先输入文本。"
        if (voice == null) return "请先选择父音色。"
        if (voice.presets.isEmpty()) return "当前父音色没有可用预设。"
        val apiKey = state.aiApiKeyInput.takeIf { it.isNotBlank() } ?: readDirectorApiKeyOrNull()
        return directorConfigError(state.settings, apiKey)
    }

    private fun directorConfigError(settings: AppSettings, apiKey: String?): String? {
        if (settings.directorBaseUrl.isBlank()) return "请先填写 AI 导演 Base URL。"
        if (settings.directorEndpointPath.isBlank()) return "请先填写 AI 导演 Endpoint。"
        if (settings.directorModel.isBlank()) return "请先填写 AI 导演模型名。"
        if (apiKey.isNullOrBlank()) return "请先填写 AI 导演 API Key。"
        return null
    }

    private fun providerFor(settings: AppSettings): TtsProvider {
        return if (settings.useFakeProvider) fakeProvider else miniMaxProvider
    }

    private suspend fun readApiKeyOrNull(): String? =
        when (val credential = credentialStore.readApiKey(CredentialSlot.Tts)) {
            is CredentialReadResult.Available -> credential.apiKey
            is CredentialReadResult.DecryptionFailed -> null
        }

    private suspend fun readDirectorApiKeyOrNull(): String? =
        when (val credential = credentialStore.readApiKey(CredentialSlot.Director)) {
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
            ?: "操作失败。"

    private companion object {
        const val TEXT_TASK_ID = "plain-text-full"
    }
}
