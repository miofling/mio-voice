package com.mio.voice.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AboutInfoTest {

    @Test
    fun formatVersionNameAddsVPrefixWhenMissing() {
        assertEquals("v0.1.0-alpha", AboutInfo.formatVersionName("0.1.0-alpha"))
        assertEquals("v1.2.3", AboutInfo.formatVersionName("1.2.3"))
    }

    @Test
    fun formatVersionNameDoesNotDuplicateExistingPrefix() {
        // 避免出现 vv0.1.0。
        assertEquals("v0.1.0", AboutInfo.formatVersionName("v0.1.0"))
        assertEquals("V2.0", AboutInfo.formatVersionName("V2.0"))
    }

    @Test
    fun licenseTextIsMitAndOffline() {
        // 离线内置文案，须包含 MIT 关键标识，且与 LICENSE 文件一致的关键句子。
        assertTrue(AboutInfo.LICENSE_TEXT.startsWith("MIT License"))
        assertTrue(AboutInfo.LICENSE_TEXT.contains("Copyright (c) 2026 miofling"))
        assertTrue(AboutInfo.LICENSE_TEXT.contains("THE SOFTWARE IS PROVIDED \"AS IS\""))
    }

    @Test
    fun privacyParagraphsCoverRequiredPoints() {
        val joined = AboutInfo.PRIVACY_PARAGRAPHS.joinToString("\n")
        assertEquals(6, AboutInfo.PRIVACY_PARAGRAPHS.size)
        assertTrue(joined.contains("不会主动"))
        assertTrue(joined.contains("本设备本地"))
        assertTrue(joined.contains("第三方服务"))
        assertTrue(joined.contains("云同步"))
    }

    @Test
    fun urlsPointToCorrectRepo() {
        assertEquals("https://github.com/miofling/mio-voice", AboutInfo.GITHUB_URL)
        assertEquals("https://github.com/miofling/mio-voice/issues", AboutInfo.ISSUES_URL)
    }
}
