package com.mio.voice.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * 仅测试 VoiceAvatarStorage 的纯文件逻辑（不触发 Android BitmapFactory）。
 * 图像解码/压缩依赖 Android 框架，由人工检查覆盖。
 */
class VoiceAvatarStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val filesDir: File get() = tempFolder.root

    @Test
    fun avatarDirIsCreatedUnderFilesDir() {
        val dir = VoiceAvatarStorage.avatarDir(filesDir)
        assertTrue(dir.exists())
        assertTrue(dir.isDirectory)
        assertEquals(File(filesDir, "voice_avatars"), dir)
    }

    @Test
    fun newAvatarFileNamesAreUniqueAndInsideDir() {
        val a = VoiceAvatarStorage.newAvatarFile(filesDir, "voice-1")
        Thread.sleep(2) // 确保时间戳不同
        val b = VoiceAvatarStorage.newAvatarFile(filesDir, "voice-1")

        assertFalse("两次生成的文件名应不同", a.name == b.name)
        assertTrue(a.name.endsWith(".webp"))
        assertTrue(VoiceAvatarStorage.isInsideAvatarDir(filesDir, a.absolutePath))
        assertTrue(VoiceAvatarStorage.isInsideAvatarDir(filesDir, b.absolutePath))
    }

    @Test
    fun newAvatarFileSanitizesUnsafeIdCharacters() {
        val file = VoiceAvatarStorage.newAvatarFile(filesDir, "../../evil id")
        // 不应包含路径分隔或空格，仍落在头像目录内。
        assertFalse(file.name.contains("/"))
        assertFalse(file.name.contains(" "))
        assertTrue(VoiceAvatarStorage.isInsideAvatarDir(filesDir, file.absolutePath))
    }

    @Test
    fun deleteNonexistentFileIsTreatedAsCleaned() {
        val missing = File(VoiceAvatarStorage.avatarDir(filesDir), "voice_x_999.webp")
        assertFalse(missing.exists())
        assertTrue(VoiceAvatarStorage.deleteAvatar(filesDir, missing.absolutePath))
    }

    @Test
    fun deleteNullOrBlankPathIsTreatedAsCleaned() {
        assertTrue(VoiceAvatarStorage.deleteAvatar(filesDir, null))
        assertTrue(VoiceAvatarStorage.deleteAvatar(filesDir, ""))
        assertTrue(VoiceAvatarStorage.deleteAvatar(filesDir, "   "))
    }

    @Test
    fun deleteFileInsideDirSucceeds() {
        val dir = VoiceAvatarStorage.avatarDir(filesDir)
        val file = File(dir, "voice_keep_1.webp").apply { writeText("data") }
        assertTrue(file.exists())

        assertTrue(VoiceAvatarStorage.deleteAvatar(filesDir, file.absolutePath))
        assertFalse(file.exists())
    }

    @Test
    fun deleteFileOutsideDirIsRejected() {
        // 头像目录之外的文件不允许删除，避免误删其他文件。
        val outside = File(filesDir, "important.txt").apply { writeText("keep me") }
        assertTrue(outside.exists())

        val result = VoiceAvatarStorage.deleteAvatar(filesDir, outside.absolutePath)

        assertFalse("越权路径应拒绝删除", result)
        assertTrue("文件不应被删除", outside.exists())
    }

    @Test
    fun isInsideAvatarDirRejectsOutsidePaths() {
        assertFalse(VoiceAvatarStorage.isInsideAvatarDir(filesDir, File(filesDir, "x.txt").absolutePath))
        assertFalse(VoiceAvatarStorage.isInsideAvatarDir(filesDir, null))
        assertTrue(
            VoiceAvatarStorage.isInsideAvatarDir(
                filesDir,
                File(VoiceAvatarStorage.avatarDir(filesDir), "voice_a_1.webp").absolutePath
            )
        )
    }
}
