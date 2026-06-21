package com.mio.voice.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mio.voice.BuildConfig
import com.mio.voice.cache.AudioCache
import com.mio.voice.cache.WavTone
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
import com.mio.voice.data.TtsResult
import com.mio.voice.data.VoiceAvatarStorage
import com.mio.voice.data.VoiceLibrary
import com.mio.voice.data.VoiceProfile
import com.mio.voice.data.generation.AudioCollection
import com.mio.voice.data.generation.AudioCollectionRepository
import com.mio.voice.data.generation.CollectionSummary
import com.mio.voice.data.generation.GeneratedAudioGroup
import com.mio.voice.data.generation.GeneratedAudioRecord
import com.mio.voice.data.generation.GeneratedAudioRepository
import com.mio.voice.data.generation.GenerationType
import com.mio.voice.data.generation.GenerationStatus
import com.mio.voice.director.DirectorDraft
import com.mio.voice.director.DirectorDraftSegment
import com.mio.voice.director.DirectorDraftSerializer
import com.mio.voice.director.DirectorPresetInfo
import com.mio.voice.director.DirectorRequest
import com.mio.voice.director.DirectorResultValidator
import com.mio.voice.director.DirectorValidationResult
import com.mio.voice.director.OpenAiCompatibleDirectorProvider
import com.mio.voice.playback.PlaybackState
import com.mio.voice.playback.PlayerController
import com.mio.voice.provider.FakeTtsProvider
import com.mio.voice.provider.MiniMaxTtsProvider
import com.mio.voice.provider.MiniMaxVoiceCloneProvider
import com.mio.voice.export.AudioExporter
import com.mio.voice.provider.OfficialVoice
import com.mio.voice.provider.OfficialVoiceClassifier
import com.mio.voice.provider.TtsProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class HomeMode { Text, Words }

enum class TextGenerationMode { FixedPreset, AiDirector }

/** 试听状态机的活动态；null（PreviewUiState.status）表示空闲 Idle。 */
enum class PreviewStatus { Generating, Playing, Error }

/**
 * 单一全局试听状态。同时只允许一个目标处于活动态，由 targetKey 区分：
 * 预设卡片用该预设的 presetId；预设编辑页用固定值 "editor"。
 */
data class PreviewUiState(
    val targetKey: String? = null,
    val voiceProfileId: String? = null,
    val presetId: String? = null,
    val status: PreviewStatus? = null,
    val errorMessage: String? = null
) {
    companion object {
        const val EDITOR_TARGET = "editor"
    }
}

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

data class AudioDetailState(
    val isLoading: Boolean = false,
    val group: GeneratedAudioGroup? = null,
    val segments: List<GeneratedAudioRecord> = emptyList(),
    val fileSizeBytes: Long = 0L,
    val localFilesOk: Boolean = true,
    val error: String? = null
)

/** 语音库「组」详情页状态。 */
data class CollectionDetailState(
    val isLoading: Boolean = false,
    val collection: AudioCollection? = null,
    val memberGroups: List<GeneratedAudioGroup> = emptyList(),
    val error: String? = null
)

/**
 * 「从历史选记录加入本组」选择页状态。
 * candidates = 进入时不在本组的历史记录（已在组的直接隐藏）。
 * addedIds = 本次会话内点「+」加入的记录 id（用于把加号变勾，仍保留在列表里给反馈）。
 */
data class CollectionPickerState(
    val isLoading: Boolean = false,
    val collectionId: String? = null,
    val candidates: List<GeneratedAudioGroup> = emptyList(),
    val addedIds: Set<String> = emptySet()
)

data class AppUiState(
    val settings: AppSettings = AppSettings(),
    val apiKeyInput: String = "",
    val aiApiKeyInput: String = "",
    val hasSavedTtsKey: Boolean = false,
    val hasSavedDirectorKey: Boolean = false,
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
    val recentGroups: List<GeneratedAudioGroup> = emptyList(),
    val expandedGroupId: String? = null,
    val historyGroups: List<GeneratedAudioGroup> = emptyList(),
    val detail: AudioDetailState = AudioDetailState(),
    val collections: List<CollectionSummary> = emptyList(),
    val collectionDetail: CollectionDetailState = CollectionDetailState(),
    val collectionPicker: CollectionPickerState = CollectionPickerState(),
    val isGenerating: Boolean = false,
    val generatedCount: Int = 0,
    val totalCount: Int = 0,
    val statusMessage: String? = null,
    val playback: PlaybackState = PlaybackState(),
    val preview: PreviewUiState = PreviewUiState(),
    val fetchedModels: List<String> = TTS_MODEL_PRESETS,
    val isFetchingModels: Boolean = false,
    val directorFetchedModels: List<String> = emptyList(),
    val isFetchingDirectorModels: Boolean = false,
    val isTestingConnection: Boolean = false,
    val isTestingDirector: Boolean = false,
    val isAnalyzingDirector: Boolean = false,
    val directorSegments: List<DirectorDraftSegment> = emptyList(),
    val directorWarnings: List<String> = emptyList(),
    val directorValidationMessage: String? = null,
    val directorVoiceProfileId: String? = null,
    // 一键添加 MiniMax 官方音色
    val officialVoices: List<OfficialVoice> = emptyList(),
    val isFetchingOfficialVoices: Boolean = false,
    val showOfficialVoicePicker: Boolean = false,
    // 音色克隆：上传样本并克隆为自定义音色
    val isCloningVoice: Boolean = false,
    val voiceCloneDemoUrl: String? = null,
    // 导出语音：一次性事件，UI 观察到后拉起系统「保存到...」文件选择器，处理完置 null。
    val pendingExport: PendingExport? = null
)

/** 待导出的音频：源文件路径 + 建议文件名 + MIME，供 SAF（ACTION_CREATE_DOCUMENT）使用。 */
data class PendingExport(
    val sourcePath: String,
    val suggestedName: String,
    val mimeType: String
)

private data class PersistedGenerationQueue(
    val groupId: String,
    val records: List<GeneratedAudioRecord>,
    val queue: List<QueueSegment>
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsStore = AppSettingsStore(application)
    private val credentialStore = SecureCredentialStore(application)
    private val audioCache = AudioCache(application)
    private val generatedAudioRepository = GeneratedAudioRepository(application)
    private val audioCollectionRepository = AudioCollectionRepository(application, generatedAudioRepository)
    private val playerController = PlayerController(application)
    private val fakeProvider = FakeTtsProvider()
    private val miniMaxProvider = MiniMaxTtsProvider()
    private val voiceCloneProvider = MiniMaxVoiceCloneProvider()
    private val directorProvider = OpenAiCompatibleDirectorProvider()
    private var generationJob: Job? = null
    private var previewJob: Job? = null
    /** 是否已从 DataStore 恢复过 AI 导演分段草稿（只在进程启动后恢复一次，避免覆盖用户编辑）。 */
    private var directorDraftRestored = false

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
                maybeRestoreDirectorDraft(settings)
            }
        }
        viewModelScope.launch {
            playerController.state.collect { playback ->
                _uiState.update { state ->
                    // 正在播放的最近生成组优先保持展开。
                    val playingId = playback.activeGenerationGroupId
                    val newExpanded = if (
                        playingId != null &&
                        playback.readyCount > 0 &&
                        state.recentGroups.any { it.id == playingId }
                    ) {
                        playingId
                    } else {
                        state.expandedGroupId
                    }
                    // 试听播放自然结束（曾处于 Playing 且现已停止）→ 复位为 Idle。
                    val newPreview = if (
                        state.preview.status == PreviewStatus.Playing && !playback.isPlaying
                    ) {
                        PreviewUiState()
                    } else {
                        state.preview
                    }
                    state.copy(playback = playback, expandedGroupId = newExpanded, preview = newPreview)
                }
            }
        }
        viewModelScope.launch {
            runCatching { generatedAudioRepository.cleanupInterruptedWork() }
            loadRecentGroups()
        }
        viewModelScope.launch {
            runCatching { audioCache.trimPreviewCache() }
        }
        viewModelScope.launch {
            when (val credential = credentialStore.readApiKey(CredentialSlot.Tts)) {
                is CredentialReadResult.Available -> {
                    if (!credential.apiKey.isNullOrBlank()) {
                        _uiState.update { it.copy(hasSavedTtsKey = true) }
                    }
                }
                is CredentialReadResult.DecryptionFailed -> {
                    _uiState.update { it.copy(credentialMessage = credential.message) }
                }
            }
            when (val credential = credentialStore.readApiKey(CredentialSlot.Director)) {
                is CredentialReadResult.Available -> {
                    if (!credential.apiKey.isNullOrBlank()) {
                        _uiState.update { it.copy(hasSavedDirectorKey = true) }
                    }
                }
                is CredentialReadResult.DecryptionFailed -> {
                    _uiState.update { it.copy(credentialMessage = credential.message) }
                }
            }
        }
    }

    fun updateText(value: String) = _uiState.update { it.copy(textInput = value) }
    fun updateWordInput(value: String) = _uiState.update { it.copy(wordInput = value) }
    fun updateMode(mode: HomeMode) {
        _uiState.update {
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
        if (mode != HomeMode.Text) clearDirectorDraftPersist()
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
    fun selectVoice(profileId: String?) = _uiState.update { state ->
        val profile = state.settings.voices.firstOrNull { it.id == profileId }
        state.copy(
            selectedVoiceProfileId = profileId,
            selectedPresetId = profile?.defaultPresetId ?: profile?.presets?.firstOrNull()?.id
        )
    }
    fun selectPreset(presetId: String?) = _uiState.update { it.copy(selectedPresetId = presetId) }

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
            val wroteTtsKey = apiKey.isNotBlank()
            if (wroteTtsKey) credentialStore.saveApiKey(apiKey, CredentialSlot.Tts)
            val aiApiKey = _uiState.value.aiApiKeyInput
            val wroteDirectorKey = aiApiKey.isNotBlank()
            if (wroteDirectorKey) credentialStore.saveApiKey(aiApiKey, CredentialSlot.Director)
            _uiState.update {
                it.copy(
                    statusMessage = "设置已保存。",
                    apiKeyInput = "",
                    aiApiKeyInput = "",
                    hasSavedTtsKey = it.hasSavedTtsKey || wroteTtsKey,
                    hasSavedDirectorKey = it.hasSavedDirectorKey || wroteDirectorKey
                )
            }
        }
    }

    /** 保存 AI 导演自定义系统提示词；传入空白表示恢复内置默认（持久化为空字符串）。 */
    fun saveDirectorSystemPrompt(prompt: String) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            settingsStore.save(current.copy(directorSystemPrompt = prompt.trim()))
            _uiState.update { it.copy(statusMessage = "提示词已保存。") }
        }
    }

    /** 切换 AI 导演「自动表演标记」开关并持久化。 */
    fun setDirectorAutoTags(enabled: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            if (current.directorAutoTags == enabled) return@launch
            settingsStore.save(current.copy(directorAutoTags = enabled))
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
                // 若正在试听被删的预设，停止试听。
                if (_uiState.value.preview.targetKey == presetId) stopPreview()
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

    /** 预设卡片试听：使用已保存的预设参数与 previewText（空回退默认文本）。targetKey = presetId。 */
    fun previewPreset(voiceProfileId: String, presetId: String?) {
        val state = _uiState.value
        val resolved = resolvePreset(voiceProfileId, presetId)
        if (resolved == null || presetId == null) {
            _uiState.update { it.copy(statusMessage = "请先选择音色和预设。") }
            return
        }
        val text = state.settings.voices
            .firstOrNull { it.id == voiceProfileId }
            ?.presets
            ?.firstOrNull { it.id == resolved.presetId }
            ?.previewText
            ?.takeIf { it.isNotBlank() }
            ?: VoiceLibrary.DEFAULT_PREVIEW_TEXT
        startPreview(
            targetKey = presetId,
            voiceProfileId = voiceProfileId,
            presetId = presetId,
            text = text,
            resolved = resolved
        )
    }

    /**
     * 预设编辑页“试听当前效果”：使用表单上的未保存参数试听，绝不触发保存。targetKey = "editor"。
     */
    fun previewDraft(
        voiceProfileId: String,
        presetId: String?,
        emotion: String,
        speed: Float,
        pitch: Int,
        previewText: String,
        providerExtras: Map<String, String> = emptyMap()
    ) {
        val voice = _uiState.value.settings.voices.firstOrNull { it.id == voiceProfileId }
        if (voice == null) {
            _uiState.update { it.copy(statusMessage = "音色不存在或已被删除。") }
            return
        }
        val resolved = ResolvedVoiceSettings(
            voiceProfileId = voiceProfileId,
            presetId = presetId ?: "",
            voiceId = voice.voiceId,
            emotion = emotion,
            speed = speed,
            pitch = pitch,
            providerExtras = providerExtras
        )
        startPreview(
            targetKey = PreviewUiState.EDITOR_TARGET,
            voiceProfileId = voiceProfileId,
            presetId = presetId,
            text = previewText.trim(),
            resolved = resolved
        )
    }

    /**
     * 停止当前试听（取消生成中的任务、停止播放）并复位为 Idle。
     * 仅当确有试听在进行（Generating / Playing）时才触碰播放器，避免误停由首页/历史
     * 发起的「生成组」正常播放——二者共用同一个 PlayerController。
     */
    fun stopPreview() {
        val hadActivePreview = previewJob != null || _uiState.value.preview.status != null
        previewJob?.cancel()
        previewJob = null
        if (hadActivePreview) {
            playerController.stop()
            _uiState.update { it.copy(preview = PreviewUiState()) }
        }
    }

    /**
     * 统一试听入口：先停旧任务，校验文本与配置完整性，强制使用真实 MiniMax provider，
     * 经缓存生成后播放。整个生命周期更新 [PreviewUiState]。
     */
    private fun startPreview(
        targetKey: String,
        voiceProfileId: String,
        presetId: String?,
        text: String,
        resolved: ResolvedVoiceSettings
    ) {
        // 切换 / 重复点击：先停旧任务与旧播放。
        previewJob?.cancel()
        playerController.stop()

        if (text.isBlank()) {
            _uiState.update {
                it.copy(
                    preview = PreviewUiState(
                        targetKey = targetKey,
                        voiceProfileId = voiceProfileId,
                        presetId = presetId,
                        status = PreviewStatus.Error,
                        errorMessage = "试听文本不能为空。"
                    ),
                    statusMessage = "试听文本不能为空。"
                )
            }
            return
        }

        previewJob = viewModelScope.launch {
            val state = _uiState.value
            // 配置完整性校验：Base URL / 模型 / API Key 任一缺失则不发请求。
            val apiKey = readApiKeyOrNull()
            val configError = previewConfigError(state.settings, apiKey)
            if (configError != null) {
                _uiState.update {
                    it.copy(
                        preview = PreviewUiState(
                            targetKey = targetKey,
                            voiceProfileId = voiceProfileId,
                            presetId = presetId,
                            status = PreviewStatus.Error,
                            errorMessage = configError
                        ),
                        statusMessage = configError
                    )
                }
                return@launch
            }
            _uiState.update {
                it.copy(
                    preview = PreviewUiState(
                        targetKey = targetKey,
                        voiceProfileId = voiceProfileId,
                        presetId = presetId,
                        status = PreviewStatus.Generating
                    )
                )
            }
            try {
                // 试听强制真实 provider（忽略 useFakeProvider），providerProfileId 固定 minimax
                // 以与正式生成共享缓存键。
                val request = TtsRequest(
                    providerProfileId = "minimax",
                    config = state.settings.providerConfig(apiKey),
                    text = text,
                    voiceId = resolved.voiceId,
                    model = state.modelInput.ifBlank { state.settings.model },
                    speed = resolved.speed,
                    emotion = resolved.emotion,
                    pitch = resolved.pitch,
                    audioFormat = "wav",
                    // 透传预设私有袋（voice_modify 等）；漏写会导致试听听不到嗓音改造且缓存键不变。
                    extraParams = resolved.providerExtras
                )
                val file = audioCache.getOrGenerate(request, miniMaxProvider)
                playerController.playFile(file)
                _uiState.update {
                    it.copy(
                        preview = PreviewUiState(
                            targetKey = targetKey,
                            voiceProfileId = voiceProfileId,
                            presetId = presetId,
                            status = PreviewStatus.Playing
                        )
                    )
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                val message = sanitize(error)
                _uiState.update {
                    it.copy(
                        preview = PreviewUiState(
                            targetKey = targetKey,
                            voiceProfileId = voiceProfileId,
                            presetId = presetId,
                            status = PreviewStatus.Error,
                            errorMessage = message
                        ),
                        statusMessage = message
                    )
                }
            }
        }
    }

    /** 试听必需的 TTS 配置校验；缺失返回友好中文提示，否则 null。 */
    private fun previewConfigError(settings: AppSettings, apiKey: String?): String? =
        PreviewValidation.configError(settings, apiKey, _uiState.value.modelInput)

    fun deleteVoice(id: String) {
        viewModelScope.launch {
            settingsStore.deleteVoice(id)
            _uiState.update { it.copy(statusMessage = "音色已删除。") }
        }
    }

    // ------------------------------------------------------------------------
    // 第二阶段 2A：父音色 / 子情绪预设的真实增删改（显式参数，不依赖全局草稿）。
    // ------------------------------------------------------------------------

    /**
     * 新增或编辑父音色。校验通过并持久化后回调 onResult(true)，否则给出错误提示并 onResult(false)。
     * @param editingId 为 null 表示新增；非空表示编辑该音色（保留其预设与创建时间）。
     */
    fun saveVoiceProfile(
        editingId: String?,
        displayName: String,
        voiceId: String,
        description: String,
        language: String,
        style: String,
        avatarUri: String? = null,
        removeAvatar: Boolean = false,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val name = displayName.trim()
            val vid = voiceId.trim()
            val state = _uiState.value
            when {
                name.isEmpty() -> {
                    _uiState.update { it.copy(statusMessage = "请填写音色名称。") }
                    onResult(false); return@launch
                }
                name.length > MAX_VOICE_NAME_LENGTH -> {
                    _uiState.update { it.copy(statusMessage = "音色名称过长（最多 $MAX_VOICE_NAME_LENGTH 字）。") }
                    onResult(false); return@launch
                }
                vid.isEmpty() -> {
                    _uiState.update { it.copy(statusMessage = "请填写 voice_id。") }
                    onResult(false); return@launch
                }
                vid.length > MAX_VOICE_ID_LENGTH -> {
                    _uiState.update { it.copy(statusMessage = "voice_id 过长（最多 $MAX_VOICE_ID_LENGTH 字）。") }
                    onResult(false); return@launch
                }
                VoiceLibrary.isVoiceIdTaken(state.settings.voices, vid, excludeProfileId = editingId) -> {
                    _uiState.update { it.copy(statusMessage = "voice_id 已存在，请使用其它值。") }
                    onResult(false); return@launch
                }
            }
            val desc = description.trim().take(MAX_DESCRIPTION_LENGTH)
            val lang = language.trim().take(MAX_SHORT_FIELD_LENGTH)
            val sty = style.trim().take(MAX_SHORT_FIELD_LENGTH)
            val existing = state.settings.voices.firstOrNull { it.id == editingId }
            val oldPath = existing?.avatarPath
            // 目标音色 id（新建时先生成，供头像文件命名使用）。
            val targetId = existing?.id ?: UUID.randomUUID().toString()
            val filesDir = getApplication<Application>().filesDir
            var newlyWrittenPath: String? = null
            try {
                // 1) 头像落盘（仅在用户选了新图时写新文件，不覆盖旧文件）。
                val resolvedAvatarPath: String? = when {
                    avatarUri != null -> {
                        val target = VoiceAvatarStorage.newAvatarFile(filesDir, targetId)
                        withContext(Dispatchers.IO) {
                            VoiceAvatarStorage.saveAvatar(
                                open = { getApplication<Application>().contentResolver.openInputStream(Uri.parse(avatarUri)) },
                                target = target
                            )
                        }
                        newlyWrittenPath = target.absolutePath
                        target.absolutePath
                    }
                    removeAvatar -> null
                    else -> oldPath
                }

                // 2) 构造并持久化音色。
                val profile = if (existing == null) {
                    VoiceLibrary.createVoice(
                        id = targetId,
                        displayName = name,
                        voiceId = vid,
                        defaultEmotion = state.settings.defaultEmotion,
                        defaultSpeed = state.settings.defaultSpeed,
                        description = desc,
                        language = lang,
                        style = sty,
                        avatarPath = resolvedAvatarPath
                    )
                } else {
                    VoiceLibrary.normalizeVoice(
                        existing.copy(
                            displayName = name,
                            voiceId = vid,
                            description = desc,
                            language = lang,
                            style = sty,
                            avatarPath = resolvedAvatarPath
                            // createdAt / presets / defaultPresetId 保持原值，不因编辑重置。
                        ),
                        state.settings.defaultEmotion,
                        state.settings.defaultSpeed
                    )
                }
                settingsStore.upsertVoice(profile, makeDefault = false)

                // 3) 持久化成功后，清理被替换/移除掉的旧头像文件（路径不同才删）。
                if (oldPath != null && oldPath != resolvedAvatarPath) {
                    withContext(Dispatchers.IO) { VoiceAvatarStorage.deleteAvatar(filesDir, oldPath) }
                }
                _uiState.update { it.copy(statusMessage = if (existing == null) "音色已创建。" else "音色已更新。") }
                onResult(true)
            } catch (error: Exception) {
                // 任一步失败：删除刚写的新头像，原数据/原头像保持不变。
                newlyWrittenPath?.let {
                    withContext(Dispatchers.IO) { VoiceAvatarStorage.deleteAvatar(filesDir, it) }
                }
                _uiState.update { it.copy(statusMessage = "保存失败：${sanitize(error)}") }
                onResult(false)
            }
        }
    }

    /** 删除父音色（含其全部子预设）；完成后回调，便于页面在确认对话框后导航返回。 */
    fun deleteVoiceProfile(id: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // 删除前先记下头像路径（删除后从 store 中就读不到了）。
                val avatarPath = _uiState.value.settings.voices.firstOrNull { it.id == id }?.avatarPath
                // 若正在试听该音色下的任意预设，先停止试听。
                if (_uiState.value.preview.voiceProfileId == id) stopPreview()
                settingsStore.deleteVoice(id)
                // 数据删除成功后清理头像文件；失败仅忽略，不回滚已完成的数据删除。
                if (avatarPath != null) {
                    withContext(Dispatchers.IO) {
                        VoiceAvatarStorage.deleteAvatar(getApplication<Application>().filesDir, avatarPath)
                    }
                }
                _uiState.update {
                    val cleared = if (it.selectedVoiceProfileId == id) {
                        it.copy(selectedVoiceProfileId = null, selectedPresetId = null)
                    } else it
                    cleared.copy(statusMessage = "音色已删除。")
                }
                onResult(true)
            } catch (error: Exception) {
                _uiState.update { it.copy(statusMessage = "删除失败：${sanitize(error)}") }
                onResult(false)
            }
        }
    }

    /**
     * 新增或编辑子情绪预设（显式 voiceProfileId / editingPresetId，绝不依赖全局草稿）。
     * 校验通过并持久化后回调 onResult(true)。
     */
    fun savePresetForm(
        voiceProfileId: String?,
        editingPresetId: String?,
        label: String,
        emotion: String,
        speed: Float,
        pitch: Int,
        description: String,
        previewText: String,
        providerExtras: Map<String, String> = emptyMap(),
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val state = _uiState.value
            val voices = state.settings.voices.toMutableList()
            val index = voices.indexOfFirst { it.id == voiceProfileId }
            if (voiceProfileId == null || index < 0) {
                _uiState.update { it.copy(statusMessage = "音色不存在或已被删除。") }
                onResult(false); return@launch
            }
            if (editingPresetId != null && voices[index].presets.none { it.id == editingPresetId }) {
                _uiState.update { it.copy(statusMessage = "预设不存在或已被删除。") }
                onResult(false); return@launch
            }
            val name = label.trim()
            when {
                name.isEmpty() -> {
                    _uiState.update { it.copy(statusMessage = "请填写预设名称。") }
                    onResult(false); return@launch
                }
                name.length > MAX_PRESET_LABEL_LENGTH -> {
                    _uiState.update { it.copy(statusMessage = "预设名称过长（最多 $MAX_PRESET_LABEL_LENGTH 字）。") }
                    onResult(false); return@launch
                }
                emotion !in VoiceLibrary.allowedEmotions -> {
                    _uiState.update { it.copy(statusMessage = "请选择有效的情绪。") }
                    onResult(false); return@launch
                }
                speed < SPEED_MIN || speed > SPEED_MAX -> {
                    _uiState.update { it.copy(statusMessage = "语速需在 $SPEED_MIN ~ $SPEED_MAX 之间。") }
                    onResult(false); return@launch
                }
                pitch < PITCH_MIN || pitch > PITCH_MAX -> {
                    _uiState.update { it.copy(statusMessage = "音高需在 $PITCH_MIN ~ $PITCH_MAX 之间。") }
                    onResult(false); return@launch
                }
                previewText.trim().length > MAX_PREVIEW_TEXT_LENGTH -> {
                    _uiState.update { it.copy(statusMessage = "试听文本过长（最多 $MAX_PREVIEW_TEXT_LENGTH 字）。") }
                    onResult(false); return@launch
                }
            }
            try {
                val preset = EmotionPreset(
                    id = editingPresetId ?: UUID.randomUUID().toString(),
                    label = name,
                    emotion = emotion,
                    speed = speed,
                    pitch = pitch,
                    previewText = previewText.trim().ifBlank { VoiceLibrary.DEFAULT_PREVIEW_TEXT },
                    description = description.trim().take(MAX_DESCRIPTION_LENGTH),
                    providerExtras = providerExtras
                )
                voices[index] = VoiceLibrary.upsertPreset(voices[index], preset, makeDefault = false)
                settingsStore.save(state.settings.copy(voices = voices))
                _uiState.update { it.copy(statusMessage = if (editingPresetId == null) "预设已创建。" else "预设已更新。") }
                onResult(true)
            } catch (error: Exception) {
                _uiState.update { it.copy(statusMessage = "保存失败：${sanitize(error)}") }
                onResult(false)
            }
        }
    }

    fun clearCredentials() {
        viewModelScope.launch {
            credentialStore.clear(CredentialSlot.Tts)
            _uiState.update { it.copy(apiKeyInput = "", hasSavedTtsKey = false, statusMessage = "凭证已清除。") }
        }
    }

    fun clearDirectorCredentials() {
        viewModelScope.launch {
            credentialStore.clear(CredentialSlot.Director)
            _uiState.update { it.copy(aiApiKeyInput = "", hasSavedDirectorKey = false, statusMessage = "AI 导演凭证已清除。") }
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
                val result = generateWordSegments(
                    words = items,
                    originalText = state.wordInput,
                    repeatCount = state.repeatCount,
                    repeatPauseMs = state.repeatPauseMs,
                    wordPauseMs = state.wordPauseMs
                )
                val queue = result.queue
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        segments = queue,
                        statusMessage = "已生成 ${queue.count { segment -> segment.status == SegmentStatus.Ready }} 个可播放片段。"
                    )
                }
                loadRecentGroups()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { current ->
                    current.copy(
                        isGenerating = false,
                        segments = current.segments.map {
                            it.copy(status = SegmentStatus.Failed, audioFile = null, errorMessage = sanitize(error))
                        },
                        statusMessage = sanitize(error)
                    )
                }
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
                config = state.settings.directorConfig(apiKey),
                systemPromptOverride = state.settings.directorSystemPrompt,
                autoPerformanceTags = state.settings.directorAutoTags
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
                when (val validation = DirectorResultValidator.validate(text, voice, result, allowTags = state.settings.directorAutoTags)) {
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
                        persistDirectorDraft()
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
                        clearDirectorDraftPersist()
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
        persistDirectorDraft()
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
        persistDirectorDraft()
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
        clearDirectorDraftPersist()
    }

    /** 把当前内存中的分段预览写入 DataStore（切 tab/切后台/重建后可恢复）。空则清除。 */
    private fun persistDirectorDraft() {
        val state = _uiState.value
        val voiceId = state.directorVoiceProfileId
        if (state.directorSegments.isEmpty() || voiceId == null) {
            clearDirectorDraftPersist()
            return
        }
        val json = DirectorDraftSerializer.serialize(
            DirectorDraft(
                voiceProfileId = voiceId,
                segments = state.directorSegments,
                warnings = state.directorWarnings
            )
        )
        directorDraftRestored = true
        viewModelScope.launch { settingsStore.saveDirectorDraft(json) }
    }

    private fun clearDirectorDraftPersist() {
        directorDraftRestored = true
        viewModelScope.launch { settingsStore.clearDirectorDraft() }
    }

    /**
     * 进程启动后从 DataStore 恢复一次分段草稿。仅当：尚未恢复过、内存里没有分段、持久化里有草稿，
     * 且草稿引用的父音色仍存在时才回填；并把生成方式切到 AI 导演，使预览可见。
     */
    private fun maybeRestoreDirectorDraft(settings: AppSettings) {
        if (directorDraftRestored) return
        directorDraftRestored = true
        if (_uiState.value.directorSegments.isNotEmpty()) return
        val draft = DirectorDraftSerializer.deserialize(settings.directorDraftJson) ?: return
        val voiceExists = settings.voices.any { it.id == draft.voiceProfileId }
        if (!voiceExists) {
            viewModelScope.launch { settingsStore.clearDirectorDraft() }
            return
        }
        _uiState.update {
            it.copy(
                homeMode = HomeMode.Text,
                textGenerationMode = TextGenerationMode.AiDirector,
                directorSegments = draft.segments,
                directorWarnings = draft.warnings,
                directorValidationMessage = null,
                directorVoiceProfileId = draft.voiceProfileId
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
            var group: GeneratedAudioGroup? = null
            try {
                val firstDraft = state.directorSegments.first()
                val firstResolved = VoiceLibrary.resolvePreset(
                    state.settings.voices,
                    voiceProfileId,
                    firstDraft.presetId
                ) ?: error("预设解析失败。")
                val firstRequest = buildRequest(firstDraft.text, state, firstResolved)
                val createdGroup = createGenerationGroup(
                    originalText = state.textInput,
                    request = firstRequest,
                    state = state,
                    type = GenerationType.AiDirector,
                    expectedSegmentCount = state.directorSegments.size
                )
                group = createdGroup
                val records = mutableListOf<GeneratedAudioRecord>()
                state.directorSegments.forEachIndexed { index, draft ->
                    val resolved = VoiceLibrary.resolvePreset(
                        _uiState.value.settings.voices,
                        voiceProfileId,
                        draft.presetId
                    ) ?: error("预设解析失败。")
                    val request = if (index == 0) firstRequest else buildRequest(draft.text, _uiState.value, resolved)
                    updateSegment(draft.id) { it.copy(status = SegmentStatus.Generating, request = request) }
                    val record = generatedAudioRepository.generateAndSaveSegment(
                        groupId = createdGroup.id,
                        segmentIndex = index,
                        segmentText = draft.text,
                        request = request,
                        provider = providerFor(_uiState.value.settings)
                    )
                    records += record
                    updateSegment(draft.id) {
                        it.copy(
                            status = SegmentStatus.Ready,
                            audioFile = File(record.localAudioPath),
                            errorMessage = null,
                            request = request
                        )
                    }
                    _uiState.update { it.copy(generatedCount = it.generatedCount + 1) }
                }
                generatedAudioRepository.completeGenerationGroup(createdGroup.id)
                playerController.setGenerationGroup(createdGroup.id, records).getOrThrow()
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        statusMessage = "AI 导演生成完成：${records.size}/${it.totalCount} 段可播放。"
                    )
                }
                loadRecentGroups()
            } catch (cancelled: CancellationException) {
                group?.let { generatedAudioRepository.failGenerationGroup(it.id, "生成已取消。") }
                throw cancelled
            } catch (error: Exception) {
                group?.let { generatedAudioRepository.failGenerationGroup(it.id, sanitize(error)) }
                _uiState.update { current ->
                    current.copy(
                        isGenerating = false,
                        segments = current.segments.map {
                            it.copy(status = SegmentStatus.Failed, audioFile = null, errorMessage = sanitize(error))
                        },
                        statusMessage = sanitize(error)
                    )
                }
            }
        }
    }

    fun retrySegment(@Suppress("UNUSED_PARAMETER") segmentId: String) {
        val state = _uiState.value
        if (state.homeMode == HomeMode.Text && state.textGenerationMode == TextGenerationMode.AiDirector) {
            confirmDirectorGeneration()
        } else {
            generate()
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

    fun playGeneratedRecord(id: String) {
        viewModelScope.launch {
            val record = generatedAudioRepository.getById(id)
            if (record == null) {
                _uiState.update { it.copy(statusMessage = "生成记录不存在。") }
                return@launch
            }
            if (record.status != GenerationStatus.Success) {
                _uiState.update { it.copy(statusMessage = record.errorMessage ?: "本地音频不可用。") }
                return@launch
            }
            playerController.playLocalPath(record.localAudioPath, record.text)
                .onFailure { error ->
                    _uiState.update { it.copy(statusMessage = sanitize(error)) }
                }
        }
    }

    fun playGenerationGroup(groupId: String) {
        viewModelScope.launch {
            val group = generatedAudioRepository.getGenerationGroup(groupId)
            if (group == null || group.status != GenerationStatus.Success) {
                _uiState.update {
                    it.copy(statusMessage = group?.errorMessage ?: "生成组不存在或不可播放。")
                }
                return@launch
            }
            val segments = generatedAudioRepository.getSegmentsForGroup(groupId)
            playerController.playGenerationGroup(groupId, segments)
                .onFailure { error -> _uiState.update { it.copy(statusMessage = sanitize(error)) } }
        }
    }

    fun seekGenerationGroupTo(positionMs: Long) {
        playerController.seekGroupTo(positionMs)
            .onFailure { error -> _uiState.update { it.copy(statusMessage = sanitize(error)) } }
    }

    fun seekGenerationGroupBy(deltaMs: Long) {
        playerController.seekGroupBy(deltaMs)
            .onFailure { error -> _uiState.update { it.copy(statusMessage = sanitize(error)) } }
    }

    fun setPlaybackSpeed(speed: Float) {
        playerController.setPlaybackSpeed(speed)
            .onFailure { error -> _uiState.update { it.copy(statusMessage = sanitize(error)) } }
    }

    /** 用户手动切换展开的最近生成记录。 */
    fun expandRecentGroup(groupId: String) {
        _uiState.update { it.copy(expandedGroupId = groupId) }
    }

    /**
     * 重新查询最近三条成功且文件完整的生成组并刷新展开项。
     * 在 init、每次成功生成后、删除后调用，无响应式 Flow，因此显式刷新。
     */
    fun loadRecentGroups() {
        viewModelScope.launch {
            val groups = runCatching { generatedAudioRepository.getRecentThreeSuccessfulGroups() }
                .getOrDefault(emptyList())
            _uiState.update { state ->
                val expanded = RecentGenerationsLogic.resolveExpandedId(
                    recentIds = groups.map { it.id },
                    playingGroupId = state.playback.activeGenerationGroupId
                        ?.takeIf { state.playback.readyCount > 0 },
                    previousExpandedId = state.expandedGroupId
                )
                state.copy(recentGroups = groups, expandedGroupId = expanded)
            }
        }
    }

    fun deleteGenerationGroup(groupId: String) {
        viewModelScope.launch {
            // 正在播放该组则先安全停止。
            playerController.clearGenerationGroup(groupId)
            val deleted = generatedAudioRepository.deleteGenerationGroup(groupId)
            // 删历史记录后清理它在所有组里的孤儿成员行（逻辑引用，无 FK 级联）。
            if (deleted) runCatching { audioCollectionRepository.removeGroupEverywhere(groupId) }
            val groups = runCatching { generatedAudioRepository.getRecentThreeSuccessfulGroups() }
                .getOrDefault(emptyList())
            val history = runCatching { generatedAudioRepository.getAllSuccessfulGroups() }
                .getOrDefault(emptyList())
            _uiState.update { state ->
                val expanded = RecentGenerationsLogic.resolveExpandedId(
                    recentIds = groups.map { it.id },
                    playingGroupId = state.playback.activeGenerationGroupId
                        ?.takeIf { state.playback.readyCount > 0 },
                    previousExpandedId = state.expandedGroupId?.takeIf { it != groupId }
                )
                state.copy(
                    recentGroups = groups,
                    historyGroups = history,
                    expandedGroupId = expanded,
                    // 当前详情若就是被删的组，清空详情状态。
                    detail = if (state.detail.group?.id == groupId) AudioDetailState() else state.detail,
                    statusMessage = if (deleted) "生成记录已删除。" else "生成记录不存在。"
                )
            }
        }
    }

    /** 重命名整条生成记录（自定义标题）。空白=清除恢复默认。改后显式重查刷新所有展示该记录的列表。 */
    fun renameGroup(groupId: String, title: String?) {
        viewModelScope.launch {
            runCatching { generatedAudioRepository.renameGroup(groupId, title) }
                .onFailure { e -> _uiState.update { it.copy(statusMessage = sanitize(e)) } }
            val history = runCatching { generatedAudioRepository.getAllSuccessfulGroups() }
                .getOrDefault(emptyList())
            val recent = runCatching { generatedAudioRepository.getRecentThreeSuccessfulGroups() }
                .getOrDefault(emptyList())
            val collectionId = _uiState.value.collectionDetail.collection?.id
            _uiState.update { state ->
                state.copy(
                    historyGroups = history,
                    recentGroups = recent,
                    // 当前详情若是该记录，用刷新后的对象替换，确保详情页标题即时更新。
                    detail = if (state.detail.group?.id == groupId) {
                        state.detail.copy(group = history.firstOrNull { it.id == groupId } ?: state.detail.group)
                    } else state.detail,
                    statusMessage = "已重命名。"
                )
            }
            // 若正查看的组详情含该成员，重查刷新成员卡标题。
            collectionId?.let { loadCollectionDetail(it) }
        }
    }

    // ------------------------------------------------------------------------
    // 语音库「组」（收藏集）
    // ------------------------------------------------------------------------

    /** 语音库页：加载全部组（按更新时间倒序）+ 成员数。改后显式重查，无响应式 Flow。 */
    fun loadCollections() {
        viewModelScope.launch {
            val collections = runCatching { audioCollectionRepository.listCollections() }
                .getOrDefault(emptyList())
            _uiState.update { it.copy(collections = collections) }
        }
    }

    fun createCollection(name: String) {
        viewModelScope.launch {
            runCatching { audioCollectionRepository.createCollection(name) }
                .onSuccess {
                    loadCollections()
                    _uiState.update { it.copy(statusMessage = "组「$name」已创建。") }
                }
                .onFailure { error -> _uiState.update { it.copy(statusMessage = sanitize(error)) } }
        }
    }

    fun renameCollection(collectionId: String, name: String) {
        viewModelScope.launch {
            runCatching { audioCollectionRepository.renameCollection(collectionId, name) }
                .onFailure { error -> _uiState.update { it.copy(statusMessage = sanitize(error)) } }
            loadCollections()
            // 若正在查看该组详情，刷新标题。
            if (_uiState.value.collectionDetail.collection?.id == collectionId) {
                loadCollectionDetail(collectionId)
            }
            _uiState.update { it.copy(statusMessage = "组已重命名。") }
        }
    }

    fun deleteCollection(collectionId: String) {
        viewModelScope.launch {
            // 仅删除组与成员关系（CASCADE），不动历史记录与音频文件。
            playerController.clearGenerationGroup(collectionId)
            val deleted = runCatching { audioCollectionRepository.deleteCollection(collectionId) }
                .getOrDefault(false)
            loadCollections()
            _uiState.update { state ->
                state.copy(
                    collectionDetail = if (state.collectionDetail.collection?.id == collectionId) {
                        CollectionDetailState()
                    } else state.collectionDetail,
                    statusMessage = if (deleted) "组已删除。" else "组不存在。"
                )
            }
        }
    }

    /** 组详情页：加载组本身 + 有效成员记录。 */
    fun loadCollectionDetail(collectionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(collectionDetail = CollectionDetailState(isLoading = true)) }
            val collection = runCatching { audioCollectionRepository.getCollection(collectionId) }.getOrNull()
            if (collection == null) {
                _uiState.update {
                    it.copy(collectionDetail = CollectionDetailState(isLoading = false, error = "组不存在或已被删除。"))
                }
                return@launch
            }
            val members = runCatching { audioCollectionRepository.getMemberGroups(collectionId) }
                .getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    collectionDetail = CollectionDetailState(
                        isLoading = false,
                        collection = collection,
                        memberGroups = members
                    )
                )
            }
        }
    }

    fun addGroupToCollection(collectionId: String, groupId: String) {
        viewModelScope.launch {
            runCatching { audioCollectionRepository.addToCollection(collectionId, groupId) }
                .onFailure { error -> _uiState.update { it.copy(statusMessage = sanitize(error)) } }
            loadCollections()
            if (_uiState.value.collectionDetail.collection?.id == collectionId) {
                loadCollectionDetail(collectionId)
            }
        }
    }

    fun removeGroupFromCollection(collectionId: String, groupId: String) {
        viewModelScope.launch {
            runCatching { audioCollectionRepository.removeFromCollection(collectionId, groupId) }
                .onFailure { error -> _uiState.update { it.copy(statusMessage = sanitize(error)) } }
            loadCollections()
            if (_uiState.value.collectionDetail.collection?.id == collectionId) {
                loadCollectionDetail(collectionId)
            }
            _uiState.update { it.copy(statusMessage = "已移出本组。") }
        }
    }

    /** 「加入组」弹窗用：查询某条记录当前归属的组集合，结果通过回调返回（不污染全局 state）。 */
    fun collectionIdsForGroup(groupId: String, onResult: (Set<String>) -> Unit) {
        viewModelScope.launch {
            val ids = runCatching { audioCollectionRepository.collectionIdsForGroup(groupId) }
                .getOrDefault(emptySet())
            onResult(ids)
        }
    }

    /** 组内连续播放：拼接所有有效成员的分段为一条时间轴，复用生成组播放器。 */
    fun playCollection(collectionId: String) {
        viewModelScope.launch {
            val result = runCatching {
                audioCollectionRepository.getPlayableSegmentsForCollection(collectionId)
            }.getOrNull()
            if (result == null || result.segments.isEmpty()) {
                _uiState.update { it.copy(statusMessage = "该组没有可播放的音频。") }
                return@launch
            }
            playerController.playGenerationGroup(collectionId, result.segments)
                .onFailure { error -> _uiState.update { it.copy(statusMessage = sanitize(error)) } }
            if (result.missingCount > 0) {
                _uiState.update { it.copy(statusMessage = "${result.missingCount} 条音频已失效，已跳过。") }
            }
        }
    }

    /** 选记录页：加载「不在本组」的历史记录作为候选（已在组的直接隐藏）。 */
    fun loadCollectionPicker(collectionId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(collectionPicker = CollectionPickerState(isLoading = true, collectionId = collectionId))
            }
            val history = runCatching { generatedAudioRepository.getAllSuccessfulGroups() }
                .getOrDefault(emptyList())
            val memberIds = runCatching { audioCollectionRepository.memberGroupIds(collectionId) }
                .getOrDefault(emptySet())
            val candidates = history.filter { it.id !in memberIds }
            _uiState.update {
                it.copy(
                    collectionPicker = CollectionPickerState(
                        isLoading = false,
                        collectionId = collectionId,
                        candidates = candidates,
                        addedIds = emptySet()
                    )
                )
            }
        }
    }

    /**
     * 选记录页：切换该条记录在本组的归属。
     * 未加入 → 加入（加号变勾）；已加入（本次会话内勾选的）→ 取消（勾变加号，并移出本组）。
     */
    fun pickerToggleGroup(collectionId: String, groupId: String) {
        viewModelScope.launch {
            val alreadyAdded = _uiState.value.collectionPicker
                .takeIf { it.collectionId == collectionId }
                ?.addedIds?.contains(groupId) == true
            if (alreadyAdded) {
                runCatching { audioCollectionRepository.removeFromCollection(collectionId, groupId) }
                    .onFailure { error -> _uiState.update { it.copy(statusMessage = sanitize(error)) } }
            } else {
                runCatching { audioCollectionRepository.addToCollection(collectionId, groupId) }
                    .onFailure { error -> _uiState.update { it.copy(statusMessage = sanitize(error)) } }
            }
            loadCollections()
            // 若组详情在后台，保持其成员列表新鲜。
            if (_uiState.value.collectionDetail.collection?.id == collectionId) {
                loadCollectionDetail(collectionId)
            }
            _uiState.update { state ->
                val picker = state.collectionPicker
                if (picker.collectionId == collectionId) {
                    val newAdded = if (alreadyAdded) picker.addedIds - groupId else picker.addedIds + groupId
                    state.copy(collectionPicker = picker.copy(addedIds = newAdded))
                } else state
            }
        }
    }

    /** 历史页：加载全部成功生成组（倒序）。 */
    fun loadHistory() {
        viewModelScope.launch {
            val history = runCatching { generatedAudioRepository.getAllSuccessfulGroups() }
                .getOrDefault(emptyList())
            _uiState.update { it.copy(historyGroups = history) }
        }
    }

    /** 详情页：按 groupId 加载生成组、分段、文件大小与本地文件状态。 */
    fun loadAudioDetail(groupId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(detail = AudioDetailState(isLoading = true)) }
            val group = runCatching { generatedAudioRepository.getGenerationGroup(groupId) }.getOrNull()
            if (group == null || group.status != GenerationStatus.Success) {
                _uiState.update {
                    it.copy(
                        detail = AudioDetailState(
                            isLoading = false,
                            error = "记录不存在或已被删除。"
                        )
                    )
                }
                return@launch
            }
            val segments = runCatching { generatedAudioRepository.getSegmentsForGroup(groupId) }
                .getOrDefault(emptyList())
            val fileSize = runCatching { generatedAudioRepository.getGroupFileSizeBytes(groupId) }
                .getOrDefault(0L)
            val localOk = segments.isNotEmpty() &&
                segments.all { generatedAudioRepository.localAudioExists(it.localAudioPath) }
            _uiState.update {
                it.copy(
                    detail = AudioDetailState(
                        isLoading = false,
                        group = group,
                        segments = segments,
                        fileSizeBytes = fileSize,
                        localFilesOk = localOk,
                        error = null
                    )
                )
            }
        }
    }

    fun clearAudioDetail() {
        _uiState.update { it.copy(detail = AudioDetailState()) }
    }

    /** 显示一条简短状态提示（如复制成功）。 */
    fun notify(message: String) {
        _uiState.update { it.copy(statusMessage = message) }
    }

    /**
     * 请求导出当前详情页的语音：发出 pendingExport 事件，UI 据此拉起系统「保存到...」选择器。
     * 仅支持单分段记录；多分段暂不支持（逐段/合并后续再做）。
     */
    fun requestExportCurrentDetail() {
        val detail = _uiState.value.detail
        val group = detail.group
        val segment = detail.segments.singleOrNull()
        if (group == null || segment == null) {
            notify("仅支持导出单段语音，多段语音暂不支持。")
            return
        }
        val title = group.customTitle?.takeIf { it.isNotBlank() }
            ?: group.previewText?.takeIf { it.isNotBlank() }
            ?: group.originalText
        _uiState.update {
            it.copy(
                pendingExport = PendingExport(
                    sourcePath = segment.localAudioPath,
                    suggestedName = AudioExporter.exportFileName(title, group.format),
                    mimeType = AudioExporter.mimeForFormat(group.format)
                )
            )
        }
    }

    /** 清除导出事件（UI 已消费 / 用户取消了选择器）。 */
    fun clearPendingExport() {
        _uiState.update { it.copy(pendingExport = null) }
    }

    /**
     * 用户在 SAF 选好目标后回调：把源音频字节拷进目标 Uri。拷贝走 IO 线程。
     * @param destUri ACTION_CREATE_DOCUMENT 返回的目标位置
     */
    fun performExport(sourcePath: String, destUri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val out = context.contentResolver.openOutputStream(destUri)
                        ?: error("无法写入所选位置。")
                    out.use { AudioExporter.copyToStream(File(sourcePath), it) }
                }
            }
            result.fold(
                onSuccess = { bytes ->
                    if (bytes <= 0L) notify("音频文件不存在，导出失败。")
                    else notify("语音已导出。")
                },
                onFailure = { notify("导出失败：${sanitize(it)}") }
            )
        }
    }

    /**
     * 重新生成：把该生成组的原文、音色、情绪、模型、生成语速带回首页（普通文本路径），
     * 不立即调用接口，由用户确认后再生成。
     */
    fun prepareRegenerate(group: GeneratedAudioGroup) {
        _uiState.update { state ->
            val voiceProfileId = state.settings.voices
                .firstOrNull { it.voiceId == group.voiceId }
                ?.id
                ?: state.selectedVoiceProfileId
            state.copy(
                homeMode = HomeMode.Text,
                textGenerationMode = TextGenerationMode.FixedPreset,
                textInput = group.originalText,
                selectedVoiceProfileId = voiceProfileId,
                modelInput = group.model ?: state.modelInput,
                segments = emptyList(),
                generatedCount = 0,
                totalCount = 0,
                statusMessage = "已带回生成内容，请确认后重新生成。"
            )
        }
    }

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
            _uiState.update { it.copy(isTestingConnection = true, statusMessage = "正在测试连接...") }
            try {
                val result = providerFor(settings).testConnection(config)
                val file = audioCache.writeTemporary("test_connection", result.audioBytes, result.audioFormat)
                playerController.playFile(file)
                _uiState.update { it.copy(isTestingConnection = false, statusMessage = "测试连接已生成短语音并开始试听。") }
            } catch (error: Exception) {
                _uiState.update { it.copy(isTestingConnection = false, statusMessage = sanitize(error)) }
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
            _uiState.update { it.copy(isTestingDirector = true, statusMessage = "正在测试 AI 连接...") }
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
                        config = settings.directorConfig(apiKey),
                        systemPromptOverride = settings.directorSystemPrompt
                    )
                )
                _uiState.update { it.copy(isTestingDirector = false, statusMessage = "AI 导演连接测试成功。") }
            } catch (error: Exception) {
                _uiState.update { it.copy(isTestingDirector = false, statusMessage = sanitize(error)) }
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

    /** 拉取 MiniMax 官方系统音色列表，成功后打开多选弹窗。复用 TTS 的 API Key 与 baseUrl。 */
    fun fetchOfficialVoices() {
        viewModelScope.launch {
            val state = _uiState.value
            val apiKey = state.apiKeyInput.takeIf { it.isNotBlank() } ?: readApiKeyOrNull()
            if (apiKey.isNullOrBlank()) {
                _uiState.update { it.copy(statusMessage = "请先在设置里填写 API Key。") }
                return@launch
            }
            val config = state.settings.providerConfig(apiKey)
            _uiState.update { it.copy(isFetchingOfficialVoices = true, statusMessage = "正在拉取官方音色...") }
            try {
                val all = miniMaxProvider.fetchSystemVoices(config)
                // 只保留性别/语言能从命名强信号可靠判定的音色，剔除标签易错的模糊项。
                val voices = OfficialVoiceClassifier.confidentVoices(all)
                _uiState.update {
                    it.copy(
                        isFetchingOfficialVoices = false,
                        officialVoices = voices,
                        showOfficialVoicePicker = true,
                        statusMessage = "已筛选出 ${voices.size} 个特征清晰的官方音色（共 ${all.size} 个）。"
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(isFetchingOfficialVoices = false, statusMessage = sanitize(error))
                }
            }
        }
    }

    fun dismissOfficialVoicePicker() {
        _uiState.update { it.copy(showOfficialVoicePicker = false) }
    }

    /** 把用户勾选的官方音色批量加入音色库（按 voiceId 去重）。voices 列表经 settings Flow 自动刷新。 */
    fun importOfficialVoices(selected: List<OfficialVoice>) {
        viewModelScope.launch {
            if (selected.isEmpty()) {
                _uiState.update { it.copy(showOfficialVoicePicker = false) }
                return@launch
            }
            val profiles = selected.map {
                VoiceLibrary.createVoice(
                    displayName = it.voiceName,
                    voiceId = it.voiceId,
                    description = it.description
                )
            }
            val added = runCatching { settingsStore.upsertVoices(profiles) }
                .getOrElse { error ->
                    _uiState.update { it.copy(statusMessage = sanitize(error)) }
                    0
                }
            _uiState.update {
                it.copy(
                    showOfficialVoicePicker = false,
                    statusMessage = if (added > 0) "已添加 $added 个音色。" else "所选音色都已在音色库中。"
                )
            }
        }
    }

    /**
     * 音色克隆：上传本地音频样本 → 调用 MiniMax voice_clone → 成功后入库音色库。
     * 复用 TTS 的 API Key 与 baseUrl。校验通过并克隆成功回调 onResult(true)，否则提示并 onResult(false)。
     *
     * @param sampleUri 本次选择的本地音频文件 content Uri 字符串。
     * @param demoText 试听文本；非空时一并请求 demo 试听音频（首次合成可能产生费用）。
     */
    fun cloneVoice(
        displayName: String,
        voiceId: String,
        sampleUri: String?,
        demoText: String?,
        description: String,
        language: String,
        style: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val name = displayName.trim()
            val vid = voiceId.trim()
            val state = _uiState.value
            when {
                name.isEmpty() -> {
                    _uiState.update { it.copy(statusMessage = "请填写音色名称。") }
                    onResult(false); return@launch
                }
                name.length > MAX_VOICE_NAME_LENGTH -> {
                    _uiState.update { it.copy(statusMessage = "音色名称过长（最多 $MAX_VOICE_NAME_LENGTH 字）。") }
                    onResult(false); return@launch
                }
                vid.isEmpty() -> {
                    _uiState.update { it.copy(statusMessage = "请填写 voice_id。") }
                    onResult(false); return@launch
                }
                !isValidCloneVoiceId(vid) -> {
                    _uiState.update { it.copy(statusMessage = "voice_id 需至少 8 位、同时包含字母和数字、且首字符为字母。") }
                    onResult(false); return@launch
                }
                VoiceLibrary.isVoiceIdTaken(state.settings.voices, vid) -> {
                    _uiState.update { it.copy(statusMessage = "voice_id 已存在，请使用其它值。") }
                    onResult(false); return@launch
                }
                sampleUri.isNullOrBlank() -> {
                    _uiState.update { it.copy(statusMessage = "请先选择音频样本文件。") }
                    onResult(false); return@launch
                }
            }

            val apiKey = state.apiKeyInput.takeIf { it.isNotBlank() } ?: readApiKeyOrNull()
            if (apiKey.isNullOrBlank()) {
                _uiState.update { it.copy(statusMessage = "请先在设置里填写 API Key。") }
                onResult(false); return@launch
            }
            val config = state.settings.providerConfig(apiKey)
            val desc = description.trim().take(MAX_DESCRIPTION_LENGTH)
            val lang = language.trim().take(MAX_SHORT_FIELD_LENGTH)
            val sty = style.trim().take(MAX_SHORT_FIELD_LENGTH)

            _uiState.update { it.copy(isCloningVoice = true, voiceCloneDemoUrl = null, statusMessage = "正在上传样本并克隆音色...") }
            try {
                val app = getApplication<Application>()
                val uri = Uri.parse(sampleUri)
                val fileName = resolveDisplayName(uri)
                val bytes = withContext(Dispatchers.IO) {
                    app.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalStateException("无法读取所选音频文件。")
                }
                if (bytes.size > MAX_CLONE_SAMPLE_BYTES) {
                    _uiState.update { it.copy(isCloningVoice = false, statusMessage = "音频文件过大（上限约 ${MAX_CLONE_SAMPLE_BYTES / (1024 * 1024)}MB）。") }
                    onResult(false); return@launch
                }

                val fileId = voiceCloneProvider.uploadSample(config, bytes, fileName)
                val result = voiceCloneProvider.cloneVoice(
                    config = config,
                    fileId = fileId,
                    voiceId = vid,
                    demoText = demoText?.trim()?.takeIf { it.isNotBlank() },
                    model = state.modelInput.ifBlank { state.settings.model }
                )

                settingsStore.upsertVoice(
                    VoiceLibrary.createVoice(
                        displayName = name,
                        voiceId = result.voiceId,
                        description = desc,
                        language = lang,
                        style = sty
                    ),
                    makeDefault = false
                )
                _uiState.update {
                    it.copy(
                        isCloningVoice = false,
                        voiceCloneDemoUrl = result.demoAudioUrl,
                        statusMessage = "音色克隆成功，已加入音色库。" +
                            if (result.demoAudioUrl != null) "（已生成试听）" else ""
                    )
                }
                onResult(true)
            } catch (error: Exception) {
                _uiState.update { it.copy(isCloningVoice = false, statusMessage = sanitize(error)) }
                onResult(false)
            }
        }
    }

    /** 从 content Uri 查询展示用文件名；失败时回退到 Uri 末段，再退到默认名。 */
    private fun resolveDisplayName(uri: Uri): String {
        val app = getApplication<Application>()
        val queried = runCatching {
            app.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        }.getOrNull()
        val name = queried?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: "sample.mp3"
        // 确保带扩展名，便于服务端识别格式。
        return if (name.contains('.')) name else "$name.mp3"
    }

    /** voice_id 软校验：至少 8 位、同时含字母和数字、首字符为字母。 */
    private fun isValidCloneVoiceId(value: String): Boolean =
        value.length >= 8 &&
            value.first().isLetter() &&
            value.any { it.isLetter() } &&
            value.any { it.isDigit() } &&
            value.all { it.isLetterOrDigit() }

    fun fetchDirectorModels(
        baseUrl: String? = null,
        endpointPath: String? = null
    ) {
        viewModelScope.launch {
            val state = _uiState.value
            val settings = state.settings.copy(
                directorBaseUrl = baseUrl ?: state.settings.directorBaseUrl,
                directorEndpointPath = endpointPath ?: state.settings.directorEndpointPath
            )
            val apiKey = state.aiApiKeyInput.takeIf { it.isNotBlank() } ?: readDirectorApiKeyOrNull()
            _uiState.update { it.copy(isFetchingDirectorModels = true, statusMessage = "正在拉取 AI 模型列表...") }
            try {
                val fetched = directorProvider.fetchModels(settings.directorConfig(apiKey))
                _uiState.update {
                    it.copy(
                        isFetchingDirectorModels = false,
                        directorFetchedModels = fetched,
                        statusMessage = "已拉取 ${fetched.size} 个 AI 模型。"
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isFetchingDirectorModels = false,
                        statusMessage = "${sanitize(error)} 可继续手动填写模型名。"
                    )
                }
            }
        }
    }

    override fun onCleared() {
        previewJob?.cancel()
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

        var group: GeneratedAudioGroup? = null
        try {
            val firstRequest = buildRequest(chunks.first(), state)
            val createdGroup = createGenerationGroup(
                originalText = text,
                request = firstRequest,
                state = state,
                type = GenerationType.PlainText,
                expectedSegmentCount = chunks.size
            )
            group = createdGroup
            val records = mutableListOf<GeneratedAudioRecord>()
            val queue = mutableListOf<QueueSegment>()
            chunks.forEachIndexed { index, chunk ->
                val request = if (index == 0) firstRequest else buildRequest(chunk, _uiState.value)
                val record = generatedAudioRepository.generateAndSaveSegment(
                    groupId = createdGroup.id,
                    segmentIndex = index,
                    segmentText = chunk,
                    request = request,
                    provider = providerFor(_uiState.value.settings)
                )
                records += record
                queue += QueueSegment(
                    "text-tech-$index",
                    "全文",
                    File(record.localAudioPath),
                    SegmentStatus.Ready,
                    request = request
                )
            }
            generatedAudioRepository.completeGenerationGroup(createdGroup.id)
            playerController.setGenerationGroup(createdGroup.id, records).getOrThrow()
            _uiState.update {
                it.copy(
                    isGenerating = false,
                    generatedCount = 1,
                    totalCount = 1,
                    segments = listOf(
                        QueueSegment(
                            id = TEXT_TASK_ID,
                            text = text,
                            audioFile = File(records.first().localAudioPath),
                            status = SegmentStatus.Ready
                        )
                    ),
                    statusMessage = if (chunks.size == 1) "全文已生成。"
                    else "全文已生成，后台使用 ${chunks.size} 个技术分块。"
                )
            }
            loadRecentGroups()
        } catch (cancelled: CancellationException) {
            group?.let { generatedAudioRepository.failGenerationGroup(it.id, "生成已取消。") }
            throw cancelled
        } catch (error: Exception) {
            group?.let { generatedAudioRepository.failGenerationGroup(it.id, sanitize(error)) }
            playerController.setQueue(emptyList())
            _uiState.update {
                it.copy(
                    isGenerating = false,
                    generatedCount = 0,
                    segments = listOf(
                        QueueSegment(TEXT_TASK_ID, text, null, SegmentStatus.Failed, sanitize(error))
                    ),
                    statusMessage = sanitize(error)
                )
            }
        }
    }

    private suspend fun generateWordSegments(
        words: List<String>,
        originalText: String,
        repeatCount: Int,
        repeatPauseMs: Int,
        wordPauseMs: Int
    ): PersistedGenerationQueue {
        val segmentCount = words.sumOf {
            repeatCount +
                (if (repeatPauseMs > 0) repeatCount - 1 else 0) +
                (if (wordPauseMs > 0) 1 else 0)
        }
        val firstRequest = buildRequest(words.first(), _uiState.value)
        val group = createGenerationGroup(
            originalText = originalText,
            request = firstRequest,
            state = _uiState.value,
            type = GenerationType.Words,
            expectedSegmentCount = segmentCount
        )
        val sourceRecords = mutableMapOf<String, GeneratedAudioRecord>()
        val records = mutableListOf<GeneratedAudioRecord>()
        val queue = mutableListOf<QueueSegment>()
        var outputIndex = 0
        try {
            words.forEachIndexed { wordIndex, word ->
                val request = if (wordIndex == 0) firstRequest else buildRequest(word, _uiState.value)
                updateSegment(wordIndex.toString()) {
                    it.copy(status = SegmentStatus.Generating, request = request)
                }
                repeat(repeatCount) { repeatIndex ->
                    val source = sourceRecords[word]
                    val record = if (source == null) {
                        generatedAudioRepository.generateAndSaveSegment(
                            group.id,
                            outputIndex,
                            word,
                            request,
                            providerFor(_uiState.value.settings)
                        ).also {
                            sourceRecords[word] = it
                            _uiState.update { state -> state.copy(generatedCount = state.generatedCount + 1) }
                        }
                    } else {
                        generatedAudioRepository.addExistingFileSegment(
                            group.id,
                            outputIndex,
                            word,
                            source
                        )
                    }
                    records += record
                    queue += QueueSegment(
                        "$wordIndex-$repeatIndex",
                        word,
                        File(record.localAudioPath),
                        SegmentStatus.Ready,
                        request = request
                    )
                    outputIndex++
                    if (repeatIndex < repeatCount - 1 && repeatPauseMs > 0) {
                        val pause = saveSilenceSegment(group.id, outputIndex, repeatPauseMs, request)
                        records += pause
                        queue += QueueSegment(
                            "$wordIndex-$repeatIndex-repeat-pause",
                            "停顿",
                            File(pause.localAudioPath),
                            SegmentStatus.Ready
                        )
                        outputIndex++
                    }
                }
                if (wordPauseMs > 0) {
                    val pause = saveSilenceSegment(group.id, outputIndex, wordPauseMs, request)
                    records += pause
                    queue += QueueSegment(
                        "$wordIndex-word-pause",
                        "停顿",
                        File(pause.localAudioPath),
                        SegmentStatus.Ready
                    )
                    outputIndex++
                }
            }
            check(outputIndex == segmentCount) { "单词生成组分段计数不一致。" }
            generatedAudioRepository.completeGenerationGroup(group.id)
            playerController.setGenerationGroup(group.id, records).getOrThrow()
            return PersistedGenerationQueue(group.id, records, queue)
        } catch (cancelled: CancellationException) {
            generatedAudioRepository.failGenerationGroup(group.id, "生成已取消。")
            throw cancelled
        } catch (error: Exception) {
            generatedAudioRepository.failGenerationGroup(group.id, sanitize(error))
            throw error
        }
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
            audioFormat = "wav",
            // 预设里存的 provider 私有袋一路透传进请求 → GenerationFingerprint（已 hash extraParams）→ provider。
            extraParams = resolved.providerExtras
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
        // Fake Provider 仅在 Debug 构建可用，且需用户显式开启；Release 构建恒用真实 provider，
        // 即使 DataStore 残留 use_fake_provider=true 也不会静默使用本地模拟音。
        return if (settings.useFakeProvider && BuildConfig.DEBUG) fakeProvider else miniMaxProvider
    }

    private suspend fun createGenerationGroup(
        originalText: String,
        request: TtsRequest,
        state: AppUiState,
        type: GenerationType,
        expectedSegmentCount: Int
    ): GeneratedAudioGroup {
        val voiceName = state.settings.voices
            .firstOrNull { it.voiceId == request.voiceId }
            ?.displayName
            ?: request.voiceId
        return generatedAudioRepository.createGenerationGroup(
            originalText = originalText,
            voiceId = request.voiceId,
            voiceName = voiceName,
            emotion = request.emotion,
            speed = request.speed,
            format = request.audioFormat,
            generationType = type,
            expectedSegmentCount = expectedSegmentCount,
            provider = providerLabel(request.providerProfileId),
            model = request.model.ifBlank { null }
        )
    }

    private fun providerLabel(providerProfileId: String): String = when (providerProfileId) {
        "minimax" -> "MiniMax"
        "fake" -> "本地模拟"
        else -> providerProfileId
    }

    private suspend fun saveSilenceSegment(
        groupId: String,
        segmentIndex: Int,
        durationMs: Int,
        baseRequest: TtsRequest
    ): GeneratedAudioRecord = generatedAudioRepository.saveSegmentResult(
        groupId = groupId,
        segmentIndex = segmentIndex,
        segmentText = "停顿",
        request = baseRequest.copy(text = "停顿", audioFormat = "wav"),
        result = TtsResult(
            audioBytes = WavTone.silence(durationMs),
            audioFormat = "wav",
            contentType = "audio/wav"
        )
    )

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

        // 音色克隆样本大小软上限（约 20MB），超过即在本地拦截，避免无谓上传。
        const val MAX_CLONE_SAMPLE_BYTES = 20L * 1024 * 1024

        const val MAX_VOICE_NAME_LENGTH = VoiceFormLimits.MAX_VOICE_NAME_LENGTH
        const val MAX_VOICE_ID_LENGTH = VoiceFormLimits.MAX_VOICE_ID_LENGTH
        const val MAX_DESCRIPTION_LENGTH = VoiceFormLimits.MAX_DESCRIPTION_LENGTH
        const val MAX_SHORT_FIELD_LENGTH = VoiceFormLimits.MAX_SHORT_FIELD_LENGTH
        const val MAX_PRESET_LABEL_LENGTH = VoiceFormLimits.MAX_PRESET_LABEL_LENGTH
        const val MAX_PREVIEW_TEXT_LENGTH = VoiceFormLimits.MAX_PREVIEW_TEXT_LENGTH
        const val SPEED_MIN = VoiceFormLimits.SPEED_MIN
        const val SPEED_MAX = VoiceFormLimits.SPEED_MAX
        const val PITCH_MIN = VoiceFormLimits.PITCH_MIN
        const val PITCH_MAX = VoiceFormLimits.PITCH_MAX
    }
}

/**
 * 音色 / 预设表单的取值限制，供 ViewModel 校验与 UI 控件共用。
 * speed / pitch 范围与 VoiceLibrary 的归一化范围保持一致（0.5~2.0 / -12~12）。
 */
/**
 * 试听相关的纯逻辑校验，抽出以便单元测试（不依赖 Android Context / ViewModel）。
 */
object PreviewValidation {
    /** TTS 配置完整性校验；缺失返回友好中文提示，齐全返回 null。 */
    fun configError(settings: AppSettings, apiKey: String?, modelInput: String): String? {
        if (settings.baseUrl.isBlank()) return "请先在 TTS 配置页填写 Base URL 后再试听。"
        if (settings.model.isBlank() && modelInput.isBlank()) {
            return "请先在 TTS 配置页填写模型名后再试听。"
        }
        if (apiKey.isNullOrBlank()) return "请先在 TTS 配置页填写 API Key 后再试听。"
        return null
    }

    /** 试听文本是否有效（去空格后非空）。 */
    fun isTextValid(text: String): Boolean = text.trim().isNotEmpty()
}

object VoiceFormLimits {
    const val MAX_VOICE_NAME_LENGTH = 40
    const val MAX_VOICE_ID_LENGTH = 80
    const val MAX_DESCRIPTION_LENGTH = 200
    const val MAX_SHORT_FIELD_LENGTH = 40
    const val MAX_PRESET_LABEL_LENGTH = 30
    const val MAX_PREVIEW_TEXT_LENGTH = 200
    const val SPEED_MIN = 0.5f
    const val SPEED_MAX = 2.0f
    const val PITCH_MIN = -12
    const val PITCH_MAX = 12
}

/**
 * 情绪枚举的中文显示名（顺序即 UI 展示顺序）。
 * 保存值始终使用接口需要的英文枚举（与 VoiceLibrary.allowedEmotions 一致）。
 */
object EmotionOptions {
    val ordered: List<Pair<String, String>> = listOf(
        "calm" to "中性",
        "happy" to "开心",
        "sad" to "悲伤",
        "angry" to "愤怒",
        "fearful" to "恐惧",
        "disgusted" to "厌恶",
        "surprised" to "惊讶"
    )

    fun labelFor(emotion: String): String =
        ordered.firstOrNull { it.first == emotion }?.second ?: emotion
}
