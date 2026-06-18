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
import com.mio.voice.data.AppSettings
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
                            TextButton(onClick = { tab = "home" }) { Text("Home") }
                            TextButton(onClick = { tab = "settings" }) { Text("Settings") }
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
                    if (tab == "home") {
                        HomeScreen(state, viewModel)
                    } else {
                        SettingsScreen(state, viewModel)
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
            label = { Text("Text") }
        )
        FilterChip(
            selected = state.homeMode == HomeMode.Words,
            onClick = { viewModel.updateMode(HomeMode.Words) },
            label = { Text("Words") }
        )
    }

    if (state.homeMode == HomeMode.Text) {
        OutlinedTextField(
            value = state.textInput,
            onValueChange = viewModel::updateText,
            label = { Text("Text") },
            minLines = 7,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        OutlinedTextField(
            value = state.wordInput,
            onValueChange = viewModel::updateWordInput,
            label = { Text("Word list") },
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
    OutlinedTextField(
        value = state.manualVoiceId,
        onValueChange = viewModel::updateManualVoiceId,
        label = { Text("voice_id") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = state.modelInput,
        onValueChange = viewModel::updateModel,
        label = { Text("Model") },
        modifier = Modifier.fillMaxWidth()
    )
    Text("Speed ${"%.2f".format(state.speed)}")
    Slider(
        value = state.speed,
        onValueChange = viewModel::updateSpeed,
        valueRange = 0.5f..2.0f,
        steps = 14
    )
    EmotionDropdown(state.emotion, viewModel::updateEmotion)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = viewModel::generate, enabled = !state.isGenerating) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Generate")
        }
        OutlinedButton(onClick = viewModel::stopGeneration) {
            Icon(Icons.Default.Stop, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Stop")
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
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
            }
            IconButton(onClick = viewModel::play) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
            }
            IconButton(onClick = viewModel::pause) {
                Icon(Icons.Default.Pause, contentDescription = "Pause")
            }
            IconButton(onClick = viewModel::next) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next")
            }
            Text("${state.playback.currentIndex + 1}/${state.playback.readyCount}")
        }
        Text(state.playback.currentText.ifBlank { "No active segment" })
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
                        Text(segment.status.name, style = MaterialTheme.typography.labelLarge)
                        if (segment.status == SegmentStatus.Failed) {
                            IconButton(onClick = { viewModel.retrySegment(segment.id) }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Retry")
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
    var useFakeProvider by rememberSaveable { mutableStateOf(state.settings.useFakeProvider) }

    LaunchedEffect(state.settings) {
        baseUrl = state.settings.baseUrl
        endpoint = state.settings.endpointPath
        model = state.settings.model
        defaultVoiceId = state.settings.defaultVoiceId
        defaultSpeed = state.settings.defaultSpeed
        defaultEmotion = state.settings.defaultEmotion
        useFakeProvider = state.settings.useFakeProvider
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = useFakeProvider, onCheckedChange = { useFakeProvider = it })
            Text("Fake Provider")
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
        OutlinedTextField(model, { model = it }, label = { Text("Default model") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(defaultVoiceId, { defaultVoiceId = it }, label = { Text("Default voice_id") }, modifier = Modifier.fillMaxWidth())
        Text("Default speed ${"%.2f".format(defaultSpeed)}")
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
                        useFakeProvider = useFakeProvider
                    )
                }
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save")
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
                Text("Test")
            }
            OutlinedButton(onClick = viewModel::clearCredentials) { Text("Clear credentials") }
            OutlinedButton(onClick = viewModel::clearAudioCache) { Text("Clear cache") }
        }
        HorizontalDivider()
        VoiceManager(state, viewModel)
    }
}

@Composable
private fun VoiceManager(state: AppUiState, viewModel: AppViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Voices", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.voiceNameDraft,
            onValueChange = { viewModel.updateVoiceDraft(it, state.voiceIdDraft) },
            label = { Text("Display name") },
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
                Text(if (state.editingVoiceId == null) "Add" else "Save")
            }
            OutlinedButton(onClick = { viewModel.saveVoice(makeDefault = true) }) {
                Text("Save default")
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
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isDefault) Text("Default", color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = { viewModel.startEditVoice(profile) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = { viewModel.deleteVoice(profile.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
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
        NumberField("Repeat", repeatCount, onRepeatCount, Modifier.weight(1f))
        NumberField("Repeat pause ms", repeatPauseMs, onRepeatPause, Modifier.weight(1f))
        NumberField("Word pause ms", wordPauseMs, onWordPause, Modifier.weight(1f))
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
            Text(selected?.displayName ?: "Manual voice_id")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Manual voice_id") }, onClick = {
                expanded = false
                onSelected(null)
            })
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
private fun EmotionDropdown(selected: String?, onSelected: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val emotions = listOf(null, "happy", "sad", "angry", "calm", "surprised")
    Column {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected ?: "No emotion")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            emotions.forEach { emotion ->
                DropdownMenuItem(text = { Text(emotion ?: "No emotion") }, onClick = {
                    expanded = false
                    onSelected(emotion)
                })
            }
        }
    }
}
