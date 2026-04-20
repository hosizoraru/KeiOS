package com.example.keios.ui.page.main.student.fetch

import com.example.keios.ui.page.main.student.BaGuideGalleryItem
import org.json.JSONArray

internal fun parseGalleryItemsFromBaseData(baseData: JSONArray, sourceUrl: String): List<BaGuideGalleryItem> {
    val out = mutableListOf<BaGuideGalleryItem>()
    val galleryKeywords = listOf(
        "立绘", "本家画", "TV动画设定图", "回忆大厅视频", "回忆大厅", "PV", "Live", "巧克力图",
        "互动家具", "角色表情", "表情", "角色演示", "设定集", "官方介绍", "官方衍生", "情人节巧克力", "BGM"
    )
    val galleryContextStartKeywords = galleryKeywords + listOf("视频")
    val nonGallerySectionKeywords = listOf(
        "技能", "技能类型", "技能名词", "EX技能升级材料", "其他技能升级材料",
        "专武", "爱用品", "能力解放", "礼物偏好", "初始数据", "顶级数据",
        "学生信息", "介绍", "配音"
    )
    val nonGalleryFallbackKeywords = listOf(
        "头像", "技能", "图标", "语音", "台词", "专武", "武器", "装备", "材料",
        "能力解放", "礼物偏好", "初始数据", "学生信息", "角色名称", "稀有度", "所属学园", "所属社团",
        "战术作用", "攻击类型", "防御类型", "位置", "武器类型", "市街", "屋外", "屋内", "室内"
    )

    fun noteForImageIndex(texts: List<String>, index: Int, imageCount: Int): String {
        val normalized = texts.map { it.trim() }.filter { it.isNotBlank() }
        if (normalized.isEmpty()) return ""
        if (imageCount <= 1) return normalized.joinToString(" / ")
        if (normalized.size == imageCount) return normalized.getOrElse(index) { "" }
        if (normalized.size == 1) return if (index == imageCount - 1) normalized.first() else ""
        return normalized.getOrElse(index) { normalized.last() }
    }

    fun isGalleryKey(raw: String): Boolean {
        val key = stripHtml(raw).trim()
        if (key.isBlank()) return false
        return galleryKeywords.any { key.contains(it, ignoreCase = true) }
    }

    val memoryUnlockLevel = run {
        var level = ""
        for (i in 0 until baseData.length()) {
            val row = baseData.optJSONArray(i) ?: continue
            if (row.length() == 0) continue
            val key = stripHtml((row.optJSONObject(0)?.optString("value") ?: "").trim())
            if (key != "回忆大厅解锁等级") continue
            val value = buildString {
                for (j in 1 until row.length()) {
                    val cell = row.optJSONObject(j) ?: continue
                    val text = stripHtml(cell.optString("value"))
                    if (text.isNotBlank()) {
                        append(text)
                        break
                    }
                }
            }
            val digits = Regex("""\d+""").find(value)?.value.orEmpty()
            level = if (digits.isNotBlank()) digits else value
            break
        }
        level
    }

    var inGalleryContext = false
    var lastGalleryTitle = ""
    for (i in 0 until baseData.length()) {
        val row = baseData.optJSONArray(i) ?: continue
        if (row.length() == 0) continue
        val key = stripHtml((row.optJSONObject(0)?.optString("value") ?: "").trim())
        if (key == "回忆大厅解锁等级") continue
        if (key.replace(" ", "").startsWith("回忆大厅文件")) continue
        val isGalleryContextStart = galleryContextStartKeywords.any { key.contains(it, ignoreCase = true) }
        val isNonGallerySectionStart = key.isNotBlank() && nonGallerySectionKeywords.any {
            key.contains(it, ignoreCase = true)
        }
        if (isNonGallerySectionStart && !isGalleryContextStart) {
            inGalleryContext = false
            lastGalleryTitle = ""
        }
        if (isGalleryContextStart) {
            inGalleryContext = true
            if (key.isNotBlank()) {
                lastGalleryTitle = key
            }
        }

        val rowImages = linkedSetOf<String>()
        val rowVideos = linkedSetOf<String>()
        val rowAudios = linkedSetOf<String>()
        val rowTexts = mutableListOf<String>()

        for (j in 1 until row.length()) {
            val cell = row.optJSONObject(j) ?: continue
            val type = cell.optString("type").trim().lowercase()
            val valueAny = cell.opt("value")
            val valueText = cell.optString("value").trim()

            when (type) {
                "image" -> {
                    if (isPlaceholderMediaToken(valueText)) continue
                    val normalized = normalizeImageUrl(sourceUrl, valueText)
                    if (looksLikeImageUrl(normalized)) rowImages += normalized
                }
                "imageset", "live2d" -> {
                    rowImages += extractImageUrlsFromAny(sourceUrl, valueAny)
                }
                "video" -> {
                    val direct = normalizeMediaUrl(sourceUrl, valueText)
                    if (looksLikeVideoUrl(direct)) rowVideos += direct
                    rowVideos += extractVideoUrlsFromAny(sourceUrl, valueAny)
                    rowImages += extractImageUrlsFromAny(sourceUrl, valueAny)
                }
                "audio" -> {
                    val direct = normalizeMediaUrl(sourceUrl, valueText)
                    if (isAudioUrl(direct)) rowAudios += direct
                    rowAudios += extractAudioUrlsFromRaw(sourceUrl, valueText)
                }
                else -> {
                    rowImages += extractImageUrlsFromHtml(sourceUrl, valueText)
                    rowImages += extractImageUrlsFromAny(sourceUrl, valueAny)
                    rowVideos += extractVideoUrlsFromAny(sourceUrl, valueAny)
                    rowAudios += extractAudioUrlsFromRaw(sourceUrl, valueText)
                    val plain = stripHtml(valueText)
                    if (plain.isNotBlank()) rowTexts += plain
                }
            }
        }

        val hasMedia = rowImages.isNotEmpty() || rowVideos.isNotEmpty() || rowAudios.isNotEmpty()
        val isFallbackGallery =
            hasMedia &&
                inGalleryContext &&
                nonGalleryFallbackKeywords.none { key.contains(it, ignoreCase = true) }
        if (!isGalleryKey(key) && !isFallbackGallery) continue
        val galleryTitle = key.ifBlank { lastGalleryTitle.ifBlank { "影画" } }

        if (rowImages.isNotEmpty()) {
            out += rowImages.mapIndexed { index, imageUrl ->
                BaGuideGalleryItem(
                    title = if (rowImages.size > 1) "$galleryTitle ${index + 1}" else galleryTitle,
                    imageUrl = imageUrl,
                    mediaType = "image",
                    mediaUrl = imageUrl,
                    memoryUnlockLevel = if (key.startsWith("回忆大厅")) memoryUnlockLevel else "",
                    note = noteForImageIndex(rowTexts, index, rowImages.size)
                )
            }
        }
        if (rowVideos.isNotEmpty()) {
            val videoNote = rowTexts.joinToString(" / ").trim()
            out += rowVideos.mapIndexed { index, videoUrl ->
                BaGuideGalleryItem(
                    title = if (rowVideos.size > 1) "$galleryTitle ${index + 1}" else galleryTitle,
                    imageUrl = rowImages.firstOrNull().orEmpty(),
                    mediaType = "video",
                    mediaUrl = videoUrl,
                    memoryUnlockLevel = if (key.startsWith("回忆大厅")) memoryUnlockLevel else "",
                    note = videoNote
                )
            }
        }
        if (rowAudios.isNotEmpty()) {
            val audioNote = rowTexts.joinToString(" / ").trim()
            out += rowAudios.mapIndexed { index, audioUrl ->
                BaGuideGalleryItem(
                    title = if (rowAudios.size > 1) "$galleryTitle ${index + 1}" else galleryTitle,
                    imageUrl = rowImages.firstOrNull().orEmpty(),
                    mediaType = "audio",
                    mediaUrl = audioUrl,
                    memoryUnlockLevel = if (key.startsWith("回忆大厅")) memoryUnlockLevel else "",
                    note = audioNote
                )
            }
        }
    }

    return out
}

internal fun parseGalleryItemsFromStyleData(styleData: JSONArray?, sourceUrl: String): List<BaGuideGalleryItem> {
    if (styleData == null || styleData.length() == 0) return emptyList()
    val out = mutableListOf<BaGuideGalleryItem>()
    for (i in 0 until styleData.length()) {
        val block = styleData.optJSONObject(i) ?: continue
        val blockName = stripHtml(block.optString("name"))
            .ifBlank { "样式 ${i + 1}" }
        val blockData = block.opt("data") ?: continue
        val imageUrls = extractImageUrlsFromAny(sourceUrl, blockData)
            .filter { it.isNotBlank() }
            .distinct()
        val videoUrls = extractVideoUrlsFromAny(sourceUrl, blockData)
            .filter { it.isNotBlank() }
            .distinct()
        if (imageUrls.isEmpty() && videoUrls.isEmpty()) continue

        imageUrls.forEachIndexed { index, imageUrl ->
            out += BaGuideGalleryItem(
                title = if (imageUrls.size > 1) "$blockName ${index + 1}" else blockName,
                imageUrl = imageUrl,
                mediaType = "image",
                mediaUrl = imageUrl
            )
        }
        videoUrls.forEachIndexed { index, videoUrl ->
            out += BaGuideGalleryItem(
                title = if (videoUrls.size > 1) "$blockName ${index + 1}" else blockName,
                imageUrl = imageUrls.firstOrNull().orEmpty(),
                mediaType = "video",
                mediaUrl = videoUrl
            )
        }
    }
    return out
}
