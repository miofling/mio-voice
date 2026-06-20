package com.mio.voice.data.generation

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class GeneratedAudioFileStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun savesAudioByRenamingTemporaryFile() = runTest {
        val directory = temporaryFolder.newFolder("generated_audio")
        var movedFromTemporaryFile = false
        val store = GeneratedAudioFileStore(
            audioDirectory = directory,
            mover = { source, target ->
                movedFromTemporaryFile = source.name.endsWith(".tmp") && source.isFile
                check(source.renameTo(target))
            }
        )
        val bytes = byteArrayOf(1, 2, 3, 4)

        val saved = store.save("record-id", 123L, bytes, "audio/mpeg")

        assertTrue(movedFromTemporaryFile)
        assertEquals("123_record-id.mp3", saved.file.name)
        assertArrayEquals(bytes, saved.file.readBytes())
        assertTrue(store.exists(saved.file.absolutePath))
        assertTrue(directory.listFiles().orEmpty().none { it.name.endsWith(".tmp") })
    }

    @Test
    fun removesTemporaryFileWhenWriteFails() = runTest {
        val directory = temporaryFolder.newFolder("generated_audio")
        val store = GeneratedAudioFileStore(
            audioDirectory = directory,
            writer = { temporary, bytes ->
                temporary.writeBytes(bytes)
                throw IOException("disk full")
            }
        )

        val error = runCatching {
            store.save("record-id", 123L, byteArrayOf(1, 2), "wav")
        }.exceptionOrNull()

        assertTrue(error is IOException)
        assertTrue(directory.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun rejectsFilesOutsideOwnedDirectory() {
        val directory = temporaryFolder.newFolder("generated_audio")
        val outside = temporaryFolder.newFile("outside.wav").apply { writeBytes(byteArrayOf(1)) }
        val store = GeneratedAudioFileStore(directory)

        assertFalse(store.exists(outside.absolutePath))
        assertFalse(store.delete(outside.absolutePath))
        assertTrue(outside.exists())
    }
}
