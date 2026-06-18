package com.mio.voice.director

import com.mio.voice.data.DirectorConfig

data class DirectorPresetInfo(
    val presetId: String,
    val label: String,
    val description: String
)

data class DirectorRequest(
    val text: String,
    val voiceProfileId: String,
    val defaultPresetId: String,
    val presets: List<DirectorPresetInfo>,
    val config: DirectorConfig
)

data class DirectorResult(
    val segments: List<DirectorRawSegment>,
    val rawJson: String
)

data class DirectorRawSegment(
    val text: String,
    val presetId: String
)

data class DirectorDraftSegment(
    val id: String,
    val text: String,
    val presetId: String,
    val warnings: List<String> = emptyList()
)

sealed class DirectorValidationResult {
    data class Valid(
        val segments: List<DirectorDraftSegment>,
        val warnings: List<String> = emptyList()
    ) : DirectorValidationResult()

    data class Invalid(val message: String) : DirectorValidationResult()
}

