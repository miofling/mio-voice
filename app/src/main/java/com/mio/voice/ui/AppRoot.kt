@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.mio.voice.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mio.voice.data.EmotionPreset
import com.mio.voice.data.SegmentStatus
import com.mio.voice.data.VoiceProfile

@Composable
fun AppRoot(viewModel: AppViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf("home") }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Mio Voice") },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        actions = {
                            TextButton(onClick = { tab = "home" }) { Text("首页") }
                            TextButton(onClick = { tab = "voices" }) { Text("音色库") }
                            TextButton(onClick = { tab = "settings" }) { Text("设置") }
                        }
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    state.statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                    state.credentialMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    when (tab) {
                        "home" -> HomeScreen(state, viewModel)
                        "voices" -> VoiceLibraryScreen(state, viewModel)
                        else -> SettingsScreen(state, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(state: AppUiState, viewModel: AppViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = state.homeMode == HomeMode.Text,
            onClick = { viewModel.updateMode(HomeMode.Text) },
            label = { Text("普通文本") }
        )
        FilterChip(
            selected = state.homeMode == HomeMode.Words,
            onClick = { viewModel.updateMode(HomeMode.Words) },
            label = { Text("单词列表") }
        )
    }

    if (state.homeMode == HomeMode.Text) {
        OutlinedTextField(
            value = state.textInput,
            onValueChange = viewModel::updateText,
            label = { Text("文本") },
            minLines = 7,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        OutlinedTextField(
            value = state.wordInput,
            onValueChange = viewModel::updateWordInput,
            label = { Text("单词列表") },
            minLines = 7,
            modifier = Modifier.fillMaxWidth()
        )
        NumberRow(
            repeatCount = state.repeatCount,
            repeatPauseMs = state.repeatPauseMs,
            wordPauseMs = state.wordPauseMs,
            onRepeatCount = viewModel::updateRepeatCount,
            onRepeatPause = viewModel::updateRepeatPause,
            onWordPause = viewModel::updateWordPause
        )
    }

    GenerationControls(state, viewModel)
    PlayerControls(state, viewModel)
    SegmentList(state, viewModel)
}

@Composable
private fun GenerationControls(state: AppUiState, viewModel: AppViewModel) {
    VoiceDropdown(
        voices = state.settings.voices,
        selectedId = state.selectedVoiceProfileId,
        onSelected = viewModel::selectVoice
    )
    PresetDropdown(
        presets = state.settings.voices.firstOrNull { it.id == state.selectedVoiceProfileId }?.presets.orEmpty(),
        selectedId = state.selectedPresetId,
        onSelected = viewModel::selectPreset
    )
    ModelDropdown(
        models = state.fetchedModels,
        selected = state.modelInput,
        onSelected = viewModel::updateModel,
        label = "模型"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = viewModel::generate, enabled = !state.isGenerating) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("生成")
        }
        OutlinedButton(onClick = viewModel::stopGeneration) {
            Icon(Icons.Default.Stop, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("停止")
        }
    }
    if (state.isGenerating || state.totalCount > 0) {
        val progress = if (state.totalCount == 0) 0f else state.generatedCount.toFloat() / state.totalCount
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Text("${state.generatedCount}/${state.totalCount}")
    }
}
@Composable
private fun PlayerControls(state: AppUiState, viewModel: AppViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = viewModel::previous) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "上一段")
            }
            IconButton(onClick = viewModel::play) {
                Icon(Icons.Default.PlayArrow, contentDescription = "播放")
            }
            IconButton(onClick = viewModel::pause) {
                Icon(Icons.Default.Pause, contentDescription = "暂停")
            }
            IconButton(onClick = viewModel::next) {
                Icon(Icons.Default.SkipNext, contentDescription = "下一段")
            }
            Text("${state.playback.currentIndex + 1}/${state.playback.readyCount}")
        }
        Text(state.playback.currentText.ifBlank { "暂无播放片段" })
    }
}

@Composable
private fun SegmentList(state: AppUiState, viewModel: AppViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.segments.forEach { segment ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(segment.status.toChinese(), style = MaterialTheme.typography.labelLarge)
                        if (segment.status == SegmentStatus.Failed) {
                            IconButton(onClick = { viewModel.retrySegment(segment.id) }) {
                                Icon(Icons.Default.Refresh, contentDescription = "重试")
                            }
                        }
                    }
                    Text(segment.text)
                    segment.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(state: AppUiState, viewModel: AppViewModel) {
    var baseUrl by rememberSaveable { mutableStateOf(state.settings.baseUrl) }
    var endpoint by rememberSaveable { mutableStateOf(state.settings.endpointPath) }
    var model by rememberSaveable { mutableStateOf(state.settings.model) }
    var defaultVoiceId by rememberSaveable { mutableStateOf(state.settings.defaultVoiceId) }
    var defaultSpeed by rememberSaveable { mutableStateOf(state.settings.defaultSpeed) }
    var defaultEmotion by rememberSaveable { mutableStateOf(state.settings.defaultEmotion) }
    var maxCharsPerRequest by rememberSaveable { mutableStateOf(state.settings.maxCharsPerRequest.toString()) }
    var useFakeProvider by rememberSaveable { mutableStateOf(state.settings.useFakeProvider) }

    LaunchedEffect(state.settings) {
        baseUrl = state.settings.baseUrl
        endpoint = state.settings.endpointPath
        model = state.settings.model
        defaultVoiceId = state.settings.defaultVoiceId
        defaultSpeed = state.settings.defaultSpeed
        defaultEmotion = state.settings.defaultEmotion
        maxCharsPerRequest = state.settings.maxCharsPerRequest.toString()
        useFakeProvider = state.settings.useFakeProvider
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = useFakeProvider, onCheckedChange = { useFakeProvider = it })
            Text("使用 Fake Provider")
        }
        OutlinedTextField(baseUrl, { baseUrl = it }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(endpoint, { endpoint = it }, label = { Text("Endpoint Path") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = state.apiKeyInput,
            onValueChange = viewModel::updateApiKey,
            label = { Text("API Key") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        ModelDropdown(
            models = state.fetchedModels,
            selected = model,
            onSelected = { model = it },
            label = "默认模型"
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { viewModel.fetchModels(baseUrl = baseUrl, apiKey = state.apiKeyInput) },
                enabled = !state.isFetchingModels
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.isFetchingModels) "拉取中..." else "拉取模型")
            }
        }
        OutlinedTextField(defaultVoiceId, { defaultVoiceId = it }, label = { Text("默认 voice_id") }, modifier = Modifier.fillMaxWidth())
        Text("默认语速 ${"%.2f".format(defaultSpeed)}")
        OutlinedTextField(
            value = maxCharsPerRequest,
            onValueChange = { maxCharsPerRequest = it.filter(Char::isDigit) },
            label = { Text("单次安全长度") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Slider(defaultSpeed, { defaultSpeed = it }, valueRange = 0.5f..2.0f, steps = 14)
        EmotionDropdown(defaultEmotion, { defaultEmotion = it })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    viewModel.saveSettings(
                        baseUrl = baseUrl,
                        endpointPath = endpoint,
                        model = model,
                        defaultVoiceId = defaultVoiceId,
                        defaultSpeed = defaultSpeed,
                        defaultEmotion = defaultEmotion,
                        maxCharsPerRequest = maxCharsPerRequest.toIntOrNull() ?: state.settings.maxCharsPerRequest,
                        useFakeProvider = useFakeProvider
                    )
                }
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("保存")
            }
            ElevatedButton(
                onClick = {
                    viewModel.testConnection(
                        baseUrl = baseUrl,
                        endpointPath = endpoint,
                        model = model,
                        defaultVoiceId = defaultVoiceId,
                        defaultSpeed = defaultSpeed,
                        defaultEmotion = defaultEmotion,
                        useFakeProvider = useFakeProvider
                    )
                }
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("测试连接")
            }
            OutlinedButton(onClick = viewModel::clearCredentials) { Text("清除凭证") }
            OutlinedButton(onClick = viewModel::clearAudioCache) { Text("清理缓存") }
        }
        HorizontalDivider()
    }
}

@Composable
private fun VoiceLibraryScreen(state: AppUiState, viewModel: AppViewModel) {
    VoiceManager(state, viewModel)
}

@Composable
private fun VoiceManager(state: AppUiState, viewModel: AppViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("音色", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.voiceNameDraft,
            onValueChange = { viewModel.updateVoiceDraft(it, state.voiceIdDraft) },
            label = { Text("显示名称") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.voiceIdDraft,
            onValueChange = { viewModel.updateVoiceDraft(state.voiceNameDraft, it) },
            label = { Text("voice_id") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.saveVoice(makeDefault = false) }) {
                Icon(if (state.editingVoiceId == null) Icons.Default.Add else Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.editingVoiceId == null) "添加" else "保存")
            }
            OutlinedButton(onClick = { viewModel.saveVoice(makeDefault = true) }) {
                Text("保存并设为默认")
            }
        }
        state.settings.voices.forEach { profile ->
            VoiceRow(profile, isDefault = profile.id == state.settings.defaultVoiceProfileId, viewModel = viewModel)
        }
    }
}

@Composable
private fun VoiceRow(profile: VoiceProfile, isDefault: Boolean, viewModel: AppViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(profile.displayName, style = MaterialTheme.typography.titleSmall)
            Text(profile.voiceId, style = MaterialTheme.typography.bodySmall)
            val defaultPreset = profile.presets.firstOrNull { it.id == profile.defaultPresetId }
            Text("默认预设：${defaultPreset?.label ?: "默认"} / 预设数：${profile.presets.size}")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isDefault) Text("默认", color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = { viewModel.startEditVoice(profile) }) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = { viewModel.deleteVoice(profile.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
            PresetManager(profile, viewModel)
        }
    }
}

@Composable
private fun PresetManager(profile: VoiceProfile, viewModel: AppViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("预设", style = MaterialTheme.typography.titleSmall)
        profile.presets.forEach { preset ->
            PresetRow(profile, preset, isDefault = preset.id == profile.defaultPresetId, viewModel = viewModel)
        }
        OutlinedTextField(
            value = state.presetLabelDraft,
            onValueChange = { viewModel.updatePresetDraft(it, state.presetEmotionDraft, state.presetSpeedDraft, state.presetPitchDraft, state.presetPreviewTextDraft) },
            label = { Text("预设标签") },
            modifier = Modifier.fillMaxWidth()
        )
        EmotionDropdown(state.presetEmotionDraft) {
            viewModel.updatePresetDraft(state.presetLabelDraft, it ?: "neutral", state.presetSpeedDraft, state.presetPitchDraft, state.presetPreviewTextDraft)
        }
        Text("预设语速 ${"%.2f".format(state.presetSpeedDraft)}")
        Slider(
            value = state.presetSpeedDraft,
            onValueChange = { viewModel.updatePresetDraft(state.presetLabelDraft, state.presetEmotionDraft, it, state.presetPitchDraft, state.presetPreviewTextDraft) },
            valueRange = 0.5f..2.0f,
            steps = 14
        )
        NumberField("预设 pitch", state.presetPitchDraft, {
            viewModel.updatePresetDraft(state.presetLabelDraft, state.presetEmotionDraft, state.presetSpeedDraft, it, state.presetPreviewTextDraft)
        })
        OutlinedTextField(
            value = state.presetPreviewTextDraft,
            onValueChange = { viewModel.updatePresetDraft(state.presetLabelDraft, state.presetEmotionDraft, state.presetSpeedDraft, state.presetPitchDraft, it) },
            label = { Text("试听文本") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.savePreset(profile.id, makeDefault = false) }) {
                Text(if (state.editingPresetId == null) "新增预设" else "保存预设")
            }
            OutlinedButton(onClick = { viewModel.savePreset(profile.id, makeDefault = true) }) {
                Text("保存并设默认")
            }
        }
    }
}

@Composable
private fun PresetRow(profile: VoiceProfile, preset: EmotionPreset, isDefault: Boolean, viewModel: AppViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${preset.label} / ${preset.emotion} / speed ${"%.2f".format(preset.speed)} / pitch ${preset.pitch}")
            if (isDefault) Text("默认", color = MaterialTheme.colorScheme.primary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = { viewModel.startEditPreset(preset) }) { Text("编辑") }
            OutlinedButton(onClick = { viewModel.setDefaultPreset(profile.id, preset.id) }) { Text("设默认") }
            OutlinedButton(onClick = { viewModel.previewPreset(profile.id, preset.id) }) { Text("试听") }
            OutlinedButton(onClick = { viewModel.deletePreset(profile.id, preset.id) }) { Text("删除") }
        }
    }
}
@Composable
private fun NumberRow(
    repeatCount: Int,
    repeatPauseMs: Int,
    wordPauseMs: Int,
    onRepeatCount: (Int) -> Unit,
    onRepeatPause: (Int) -> Unit,
    onWordPause: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField("重复次数", repeatCount, onRepeatCount, Modifier.weight(1f))
        NumberField("重复间停顿 ms", repeatPauseMs, onRepeatPause, Modifier.weight(1f))
        NumberField("单词间停顿 ms", wordPauseMs, onWordPause, Modifier.weight(1f))
    }
}

@Composable
private fun NumberField(label: String, value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { onChange(it.toIntOrNull() ?: 0) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

@Composable
private fun VoiceDropdown(
    voices: List<VoiceProfile>,
    selectedId: String?,
    onSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = voices.firstOrNull { it.id == selectedId }
    Column {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected?.displayName ?: "请先在音色库添加父音色")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            voices.forEach { profile ->
                DropdownMenuItem(text = { Text(profile.displayName) }, onClick = {
                    expanded = false
                    onSelected(profile.id)
                })
            }
        }
    }
}

@Composable
private fun PresetDropdown(
    presets: List<EmotionPreset>,
    selectedId: String?,
    onSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = presets.firstOrNull { it.id == selectedId }
    Column {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected?.label ?: "选择预设")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            presets.forEach { preset ->
                DropdownMenuItem(text = { Text("${preset.label} / ${preset.emotion}") }, onClick = {
                    expanded = false
                    onSelected(preset.id)
                })
            }
        }
    }
}

@Composable
private fun ModelDropdown(
    models: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    label: String
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = selected,
            onValueChange = onSelected,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("选择模型")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            models.forEach { model ->
                DropdownMenuItem(text = { Text(model) }, onClick = {
                    expanded = false
                    onSelected(model)
                })
            }
        }
    }
}

@Composable
private fun EmotionDropdown(selected: String?, onSelected: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val emotions = listOf(
        "neutral" to "neutral",
        "happy" to "happy",
        "sad" to "sad",
        "angry" to "angry",
        "fearful" to "fearful",
        "disgusted" to "disgusted",
        "surprised" to "surprised"
    )
    Column {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(emotions.firstOrNull { it.first == selected }?.second ?: selected ?: "neutral")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            emotions.forEach { (emotion, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = {
                    expanded = false
                    onSelected(emotion)
                })
            }
        }
    }
}
private fun SegmentStatus.toChinese(): String = when (this) {
    SegmentStatus.Pending -> "等待中"
    SegmentStatus.Generating -> "生成中"
    SegmentStatus.Ready -> "可播放"
    SegmentStatus.Failed -> "失败"
    SegmentStatus.Skipped -> "已跳过"
}
