package com.example.keios.ui.page.main.student.page.support

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.documentfile.provider.DocumentFile
import com.example.keios.feature.ba.data.remote.GameKeeFetchHelper
import com.example.keios.ui.page.main.student.BaGuideGalleryItem
import com.example.keios.ui.page.main.student.BaStudentGuideInfo
import com.example.keios.ui.page.main.student.fetch.normalizeGuideUrl
import com.example.keios.ui.page.main.student.hasRenderableGalleryMedia
import com.example.keios.ui.page.main.student.isMemoryHallFileGalleryItem
import com.example.keios.ui.page.main.student.isRenderableGalleryStaticImageUrl
import com.example.keios.ui.page.main.widget.motion.resolvedMotionDuration
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal fun normalizeGuidePlaybackSource(raw: String): String {
    val value = raw.trim()
    if (value.isBlank()) return ""
    val scheme = runCatching { Uri.parse(value).scheme.orEmpty() }.getOrDefault("")
    return if (scheme.equals("file", ignoreCase = true)) {
        value
    } else {
        normalizeGuideUrl(value)
    }
}

internal fun isGuideAudioPlaybackUrl(raw: String): Boolean {
    val normalized = normalizeGuidePlaybackSource(raw)
    if (normalized.isBlank()) return false
    val scheme = runCatching { Uri.parse(normalized).scheme.orEmpty() }.getOrDefault("")
    return scheme.equals("http", ignoreCase = true) ||
        scheme.equals("https", ignoreCase = true) ||
        scheme.equals("file", ignoreCase = true)
}

internal fun collectGuideStaticImagePrefetchUrls(info: BaStudentGuideInfo): List<String> {
    val orderedUrls = mutableListOf<String>()
    val coverUrl = normalizeGuideUrl(info.imageUrl)
    if (isRenderableGalleryStaticImageUrl(coverUrl)) {
        orderedUrls += coverUrl
    }

    val galleryItems = if (info.galleryItems.isNotEmpty()) {
        info.galleryItems
            .filter(::hasRenderableGalleryMedia)
            .filterNot(::isMemoryHallFileGalleryItem)
            .distinctBy { "${it.mediaType}|${it.mediaUrl.ifBlank { it.imageUrl }}" }
    } else {
        listOfNotNull(
            info.imageUrl.takeIf { it.isNotBlank() }?.let {
                BaGuideGalleryItem(
                    title = "立绘",
                    imageUrl = it,
                    mediaType = "image",
                    mediaUrl = it
                )
            }
        )
    }

    galleryItems.forEach { item ->
        val imageUrl = normalizeGuideUrl(item.imageUrl)
        if (isRenderableGalleryStaticImageUrl(imageUrl)) {
            orderedUrls += imageUrl
        }
        val mediaUrl = normalizeGuideUrl(item.mediaUrl)
        if (isRenderableGalleryStaticImageUrl(mediaUrl)) {
            orderedUrls += mediaUrl
        }
    }
    return orderedUrls
        .filter { it.isNotBlank() }
        .distinct()
}

internal data class GuideMediaSaveRequest(
    val sourceUrl: String,
    val title: String,
    val fileName: String,
    val mimeType: String
)

internal data class GuideMediaPackSaveRequest(
    val entries: List<GuideMediaSaveRequest>,
    val fileName: String
)

internal data class GuideMediaPackSaveResult(
    val totalCount: Int,
    val savedCount: Int
) {
    val success: Boolean
        get() = savedCount > 0
}

private fun sanitizeGuideMediaTitle(raw: String): String {
    val cleaned = raw
        .replace(Regex("""[\\/:*?"<>|]"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
    return cleaned.ifBlank { "BA_media" }.take(96)
}

private fun sanitizeGuideMediaToken(raw: String): String {
    return raw
        .replace(Regex("""[\\/:*?"<>|]"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
        .take(48)
}

private val guideKnownMediaExts = setOf(
    "jpg", "jpeg", "png", "webp", "gif", "bmp",
    "mp4", "webm", "mkv", "mov", "m3u8",
    "ogg", "mp3", "wav", "flac", "aac", "m4a"
)

private fun readGuideFileHeader(path: String, maxBytes: Int = 16): ByteArray {
    if (path.isBlank()) return ByteArray(0)
    val file = File(path)
    if (!file.exists() || file.length() <= 0L) return ByteArray(0)
    return runCatching {
        file.inputStream().use { input ->
            val buffer = ByteArray(maxBytes)
            val read = input.read(buffer)
            if (read <= 0) ByteArray(0) else buffer.copyOf(read)
        }
    }.getOrDefault(ByteArray(0))
}

private fun ByteArray.startsWithAscii(prefix: String): Boolean {
    if (size < prefix.length) return false
    return prefix.indices.all { index ->
        this[index].toInt().toChar() == prefix[index]
    }
}

private fun ByteArray.asciiAt(offset: Int, length: Int): String {
    if (offset < 0 || length <= 0 || size < offset + length) return ""
    return buildString(length) {
        for (i in offset until offset + length) {
            append(this@asciiAt[i].toInt().toChar())
        }
    }
}

private fun inferGuideMediaExtFromLocalFile(rawSourceUrl: String, rawTitle: String): String? {
    val parsed = runCatching { Uri.parse(rawSourceUrl) }.getOrNull()
    val path = when {
        parsed?.scheme.equals("file", ignoreCase = true) -> parsed?.path.orEmpty()
        rawSourceUrl.startsWith("/") -> rawSourceUrl
        else -> ""
    }
    val header = readGuideFileHeader(path = path, maxBytes = 16)
    if (header.isEmpty()) return null

    fun byteAt(index: Int): Int = header.getOrNull(index)?.toInt()?.and(0xFF) ?: -1

    return when {
        header.size >= 4 && header.startsWithAscii("OggS") -> "ogg"
        header.size >= 3 && header.startsWithAscii("ID3") -> "mp3"
        header.size >= 2 && byteAt(0) == 0xFF && (byteAt(1) and 0xE0) == 0xE0 -> "mp3"
        header.size >= 4 && header.startsWithAscii("fLaC") -> "flac"
        header.size >= 12 &&
            header.startsWithAscii("RIFF") &&
            header.asciiAt(8, 4) == "WAVE" -> "wav"
        header.size >= 2 && byteAt(0) == 0xFF && (byteAt(1) and 0xF6) == 0xF0 -> "aac"
        header.size >= 12 && header.asciiAt(4, 4) == "ftyp" -> {
            val lowerTitle = rawTitle.lowercase()
            if (lowerTitle.contains("bgm") || lowerTitle.contains("音频") || lowerTitle.contains("语音")) {
                "m4a"
            } else {
                "mp4"
            }
        }
        header.size >= 4 && byteAt(0) == 0x89 && header.asciiAt(1, 3) == "PNG" -> "png"
        header.size >= 3 && byteAt(0) == 0xFF && byteAt(1) == 0xD8 && byteAt(2) == 0xFF -> "jpg"
        header.size >= 6 && (header.startsWithAscii("GIF87a") || header.startsWithAscii("GIF89a")) -> "gif"
        header.size >= 12 &&
            header.startsWithAscii("RIFF") &&
            header.asciiAt(8, 4) == "WEBP" -> "webp"
        header.size >= 2 && header.startsWithAscii("BM") -> "bmp"
        else -> null
    }
}

private fun guessGuideMediaExt(rawSourceUrl: String, rawTitle: String = ""): String {
    val normalized = normalizeGuidePlaybackSource(rawSourceUrl)
    val source = normalized.substringBefore('#').substringBefore('?')
    val ext = source.substringAfterLast('.', "").lowercase()
    if (ext in guideKnownMediaExts) return ext

    inferGuideMediaExtFromLocalFile(normalized, rawTitle)?.let { inferred ->
        return inferred
    }

    val lowerSource = normalized.lowercase()
    val lowerTitle = rawTitle.lowercase()
    return when {
        lowerSource.contains("audio") || lowerTitle.contains("bgm") || lowerTitle.contains("音频") -> "ogg"
        lowerSource.contains("video") || lowerTitle.contains("视频") -> "mp4"
        else -> "bin"
    }
}

private fun mimeTypeFromGuideExt(ext: String): String {
    val normalizedExt = ext.lowercase()
    val mapMime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(normalizedExt).orEmpty()
    if (mapMime.isNotBlank()) return mapMime
    return when (normalizedExt) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "bmp" -> "image/bmp"
        "mp4" -> "video/mp4"
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        "mov" -> "video/quicktime"
        "ogg" -> "audio/ogg"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "flac" -> "audio/flac"
        "aac" -> "audio/aac"
        "m4a" -> "audio/mp4"
        else -> "application/octet-stream"
    }
}

internal fun buildGuideMediaSaveRequest(
    rawUrl: String,
    rawTitle: String,
    rawPrefix: String = ""
): GuideMediaSaveRequest? {
    val source = normalizeGuidePlaybackSource(rawUrl)
    if (source.isBlank()) return null
    val ext = guessGuideMediaExt(source, rawTitle)
    val mime = mimeTypeFromGuideExt(ext)
    val baseTitle = sanitizeGuideMediaTitle(rawTitle)
    val prefix = sanitizeGuideMediaToken(rawPrefix)
        .takeIf { it.isNotBlank() && it != "学生图鉴" }
        .orEmpty()
    val title = when {
        prefix.isBlank() -> baseTitle
        baseTitle.startsWith(prefix) -> baseTitle
        else -> sanitizeGuideMediaTitle("${prefix}_${baseTitle}")
    }
    val fileName = if (title.endsWith(".$ext", ignoreCase = true)) {
        title
    } else {
        "$title.$ext"
    }
    return GuideMediaSaveRequest(
        sourceUrl = source,
        title = title,
        fileName = fileName,
        mimeType = mime
    )
}

private fun buildGuideMediaPackZipFileName(
    rawPackTitle: String,
    rawPrefix: String
): String {
    val packTitle = sanitizeGuideMediaToken(rawPackTitle).ifBlank { "角色表情" }
    val prefix = sanitizeGuideMediaToken(rawPrefix)
        .takeIf { it.isNotBlank() && it != "学生图鉴" }
        .orEmpty()
    val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
    val base = if (prefix.isBlank()) {
        "${packTitle}_打包_$stamp"
    } else {
        "${prefix}_${packTitle}_打包_$stamp"
    }
    return "${sanitizeGuideMediaTitle(base)}.zip"
}

internal fun buildGuideMediaPackSaveRequest(
    rawItems: List<Pair<String, String>>,
    rawPackTitle: String,
    rawPrefix: String = ""
): GuideMediaPackSaveRequest? {
    if (rawItems.isEmpty()) return null
    val entries = rawItems
        .mapNotNull { (url, title) ->
            buildGuideMediaSaveRequest(
                rawUrl = url,
                rawTitle = title,
                rawPrefix = rawPrefix
            )
        }
        .distinctBy { it.sourceUrl }
    if (entries.isEmpty()) return null
    return GuideMediaPackSaveRequest(
        entries = entries,
        fileName = buildGuideMediaPackZipFileName(
            rawPackTitle = rawPackTitle,
            rawPrefix = rawPrefix
        )
    )
}

private inline fun copyGuideMediaFromSource(
    context: Context,
    sourceUrl: String,
    onInputStream: (InputStream) -> Boolean
): Boolean {
    return runCatching {
        val normalized = normalizeGuidePlaybackSource(sourceUrl)
        if (normalized.isBlank()) {
            false
        } else {
            val sourceUri = runCatching { Uri.parse(normalized) }.getOrNull()
            val scheme = sourceUri?.scheme.orEmpty().lowercase()
            when {
                scheme == "file" -> {
                    val path = sourceUri?.path.orEmpty()
                    if (path.isBlank()) {
                        false
                    } else {
                        File(path).inputStream().use(onInputStream)
                    }
                }

                scheme == "content" -> {
                    val parsedUri = sourceUri
                    val input = parsedUri?.let { context.contentResolver.openInputStream(it) }
                    if (input == null) {
                        false
                    } else {
                        input.use(onInputStream)
                    }
                }

                scheme == "http" || scheme == "https" || normalized.startsWith("//") -> {
                    val tempDir = File(context.cacheDir, "guide_media_save")
                    if (!tempDir.exists()) tempDir.mkdirs()
                    val tempFile = File(tempDir, "tmp_${System.nanoTime().toString(16)}")
                    try {
                        val downloaded = GameKeeFetchHelper.downloadToFile(normalized, tempFile)
                        if (!downloaded || !tempFile.exists() || tempFile.length() <= 0L) {
                            false
                        } else {
                            tempFile.inputStream().use(onInputStream)
                        }
                    } finally {
                        runCatching { tempFile.delete() }
                    }
                }

                else -> false
            }
        }
    }.getOrDefault(false)
}

internal fun copyGuideMediaToUri(
    context: Context,
    sourceUrl: String,
    outputUri: Uri
): Boolean {
    return runCatching {
        val output = context.contentResolver.openOutputStream(outputUri) ?: return@runCatching false
        output.use { out ->
            copyGuideMediaFromSource(context, sourceUrl) { input ->
                input.copyTo(out)
                true
            }
        }
    }.getOrDefault(false)
}

private fun createUniqueZipEntryName(
    usedNames: MutableSet<String>,
    rawFileName: String
): String {
    val sanitized = sanitizeGuideMediaTitle(rawFileName).ifBlank { "BA_media.bin" }
    val base = sanitized.substringBeforeLast('.', sanitized).ifBlank { "BA_media" }
    val ext = sanitized.substringAfterLast('.', "")
    val suffix = ext.takeIf { it.isNotBlank() }?.let { ".$it" }.orEmpty()
    val maxAttempts = 120
    for (index in 0 until maxAttempts) {
        val candidate = if (index == 0) {
            "$base$suffix"
        } else {
            "$base ($index)$suffix"
        }
        if (usedNames.add(candidate)) {
            return candidate
        }
    }
    val fallback = "BA_media_${System.nanoTime().toString(16)}$suffix"
    usedNames += fallback
    return fallback
}

internal fun copyGuideMediaPackToUri(
    context: Context,
    request: GuideMediaPackSaveRequest,
    outputUri: Uri
): GuideMediaPackSaveResult {
    val total = request.entries.size
    if (total <= 0) return GuideMediaPackSaveResult(totalCount = 0, savedCount = 0)
    var savedCount = 0
    val success = runCatching {
        val output = context.contentResolver.openOutputStream(outputUri) ?: return@runCatching false
        output.use { out ->
            ZipOutputStream(BufferedOutputStream(out)).use { zip ->
                val usedEntryNames = mutableSetOf<String>()
                request.entries.forEach { item ->
                    val entryName = createUniqueZipEntryName(
                        usedNames = usedEntryNames,
                        rawFileName = item.fileName
                    )
                    var entryOpened = false
                    val copied = copyGuideMediaFromSource(context, item.sourceUrl) { input ->
                        runCatching {
                            zip.putNextEntry(ZipEntry(entryName))
                            entryOpened = true
                            input.copyTo(zip)
                            true
                        }.getOrDefault(false)
                    }
                    if (entryOpened) {
                        runCatching { zip.closeEntry() }
                    }
                    if (copied) {
                        savedCount += 1
                    }
                }
            }
        }
        true
    }.getOrDefault(false)
    if (!success) {
        return GuideMediaPackSaveResult(totalCount = total, savedCount = 0)
    }
    return GuideMediaPackSaveResult(totalCount = total, savedCount = savedCount)
}

internal fun createUniqueDocumentInTree(
    tree: DocumentFile,
    mimeType: String,
    fileName: String
): DocumentFile? {
    val base = fileName.substringBeforeLast('.', fileName).ifBlank { "BA_media" }
    val ext = fileName.substringAfterLast('.', "")
    val normalizedExt = ext.takeIf { it.isNotBlank() }?.let { ".$it" }.orEmpty()
    val maxAttempts = 120
    for (index in 0 until maxAttempts) {
        val candidate = if (index == 0) {
            "$base$normalizedExt"
        } else {
            "$base ($index)$normalizedExt"
        }
        val exists = tree.findFile(candidate)
        if (exists == null) {
            return tree.createFile(mimeType, candidate)
        }
    }
    return null
}

@Composable
internal fun rememberGuideSyncProgress(
    loading: Boolean,
    animationsEnabled: Boolean
): Float {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(loading, animationsEnabled) {
        if (!animationsEnabled) {
            progress.snapTo(if (loading) 0.9f else 1f)
            return@LaunchedEffect
        }
        if (loading) {
            progress.snapTo(0.12f)
            progress.animateTo(
                targetValue = 0.68f,
                animationSpec = tween(
                    durationMillis = resolvedMotionDuration(520, animationsEnabled),
                    easing = FastOutSlowInEasing
                ),
            )
            progress.animateTo(
                targetValue = 0.90f,
                animationSpec = tween(
                    durationMillis = resolvedMotionDuration(1800, animationsEnabled),
                    easing = LinearEasing
                ),
            )
        } else {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = resolvedMotionDuration(260, animationsEnabled),
                    easing = FastOutSlowInEasing
                ),
            )
        }
    }
    return progress.value
}
