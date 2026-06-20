package com.mio.voice.ui

import com.mio.voice.data.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewValidationTest {
    private val fullSettings = AppSettings(
        baseUrl = "https://example.test",
        model = "speech-01"
    )

    @Test
    fun configCompleteReturnsNull() {
        assertNull(PreviewValidation.configError(fullSettings, apiKey = "k", modelInput = ""))
    }

    @Test
    fun missingBaseUrlReported() {
        val error = PreviewValidation.configError(
            fullSettings.copy(baseUrl = ""), apiKey = "k", modelInput = ""
        )
        assertTrue(error?.contains("Base URL") == true)
    }

    @Test
    fun missingApiKeyReported() {
        val error = PreviewValidation.configError(fullSettings, apiKey = null, modelInput = "")
        assertTrue(error?.contains("API Key") == true)
        val blank = PreviewValidation.configError(fullSettings, apiKey = "", modelInput = "")
        assertTrue(blank?.contains("API Key") == true)
    }

    @Test
    fun modelInputCanSubstituteSettingsModel() {
        // settings.model 为空但 modelInput 有值时，应视为配置完整。
        val error = PreviewValidation.configError(
            fullSettings.copy(model = ""), apiKey = "k", modelInput = "speech-02"
        )
        assertNull(error)
    }

    @Test
    fun missingModelReportedWhenBothBlank() {
        val error = PreviewValidation.configError(
            fullSettings.copy(model = ""), apiKey = "k", modelInput = ""
        )
        assertTrue(error?.contains("模型") == true)
    }

    @Test
    fun textValidity() {
        assertFalse(PreviewValidation.isTextValid(""))
        assertFalse(PreviewValidation.isTextValid("   "))
        assertTrue(PreviewValidation.isTextValid("你好"))
        assertEquals(true, PreviewValidation.isTextValid("  x  "))
    }
}
