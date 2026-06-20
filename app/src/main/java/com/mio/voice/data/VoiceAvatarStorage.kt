package com.mio.voice.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.InputStream

/**
 * 父音色自定义头像的本地存储工具。
 *
 * 设计要点：
 * - 头像统一存放在 App 私有目录 `filesDir/voice_avatars/` 下，文件名带音色 id + 时间戳，保证唯一、可换新不覆盖旧文件。
 * - 所有删除操作仅允许作用于头像目录内的文件（越权路径拒绝），避免误删其他文件。
 * - 文件相关方法为纯逻辑（仅依赖 java.io.File），便于单元测试；图像解码/压缩依赖 Android 框架，单独成方法。
 */
object VoiceAvatarStorage {

    private const val DIR_NAME = "voice_avatars"
    private const val TARGET_SIZE = 512
    private const val WEBP_QUALITY = 85

    /** 头像目录；惰性创建。 */
    fun avatarDir(filesDir: File): File {
        val dir = File(filesDir, DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** 为指定音色生成一个唯一的新头像文件（时间戳保证不与旧头像重名，从而不覆盖旧文件）。 */
    fun newAvatarFile(filesDir: File, voiceProfileId: String): File {
        val safeId = voiceProfileId.ifBlank { "voice" }.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return File(avatarDir(filesDir), "voice_${safeId}_${System.currentTimeMillis()}.webp")
    }

    /** 判断给定路径是否位于头像目录内（用于安全删除校验）。 */
    fun isInsideAvatarDir(filesDir: File, path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return runCatching {
            val dirCanonical = avatarDir(filesDir).canonicalPath + File.separator
            File(path).canonicalPath.startsWith(dirCanonical)
        }.getOrDefault(false)
    }

    /**
     * 删除某个头像文件。
     * @return 是否已处于“已清理”状态。path 为空或文件不存在均视为已清理（true）；
     *         越权（不在头像目录内）拒绝删除返回 false；删除失败返回 false。
     */
    fun deleteAvatar(filesDir: File, path: String?): Boolean {
        if (path.isNullOrBlank()) return true
        if (!isInsideAvatarDir(filesDir, path)) return false
        val file = File(path)
        if (!file.exists()) return true
        return runCatching { file.delete() }.getOrDefault(false)
    }

    /**
     * 从输入流读取图片，中心方形裁切并缩放到约 512px，以 WebP 写入目标文件。
     * 失败时抛异常，并删除可能写了一半的目标文件（不留半文件）。
     * 仅在 Android 运行时可用（依赖 BitmapFactory / Bitmap.compress）。
     */
    @Throws(Exception::class)
    fun saveAvatar(open: () -> InputStream?, target: File) {
        // 第一遍：仅取尺寸，计算采样率防止大图 OOM。
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        (open() ?: throw IllegalStateException("无法读取所选图片。")).use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalStateException("图片格式无法识别。")
        }
        val minEdge = minOf(bounds.outWidth, bounds.outHeight)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = computeSampleSize(minEdge, TARGET_SIZE)
        }
        val decoded = (open() ?: throw IllegalStateException("无法读取所选图片。")).use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: throw IllegalStateException("图片解码失败。")

        val processed = try {
            centerCropToSquare(decoded).let { square ->
                val scaled = Bitmap.createScaledBitmap(square, TARGET_SIZE, TARGET_SIZE, true)
                if (scaled != square && square != decoded) square.recycle()
                scaled
            }
        } finally {
            // decoded 若被后续复用则不在此回收；createScaledBitmap 不复用原图，安全。
        }

        target.parentFile?.let { if (!it.exists()) it.mkdirs() }
        try {
            target.outputStream().use { out ->
                @Suppress("DEPRECATION")
                val format = runCatching { Bitmap.CompressFormat.valueOf("WEBP_LOSSY") }
                    .getOrDefault(Bitmap.CompressFormat.WEBP)
                val ok = processed.compress(format, WEBP_QUALITY, out)
                if (!ok) throw IllegalStateException("头像写入失败。")
            }
        } catch (error: Exception) {
            // 失败时清掉半文件，不污染头像目录。
            runCatching { if (target.exists()) target.delete() }
            throw error
        } finally {
            if (processed != decoded) processed.recycle()
            decoded.recycle()
        }
    }

    private fun computeSampleSize(srcEdge: Int, targetEdge: Int): Int {
        var sample = 1
        var edge = srcEdge
        while (edge / 2 >= targetEdge) {
            edge /= 2
            sample *= 2
        }
        return sample
    }

    private fun centerCropToSquare(src: Bitmap): Bitmap {
        val edge = minOf(src.width, src.height)
        if (src.width == src.height) return src
        val x = (src.width - edge) / 2
        val y = (src.height - edge) / 2
        return Bitmap.createBitmap(src, x, y, edge, edge)
    }
}
