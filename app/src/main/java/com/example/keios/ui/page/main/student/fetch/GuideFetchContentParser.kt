package com.example.keios.ui.page.main.student

import android.net.Uri
import com.example.keios.feature.ba.data.remote.GameKeeFetchHelper
import org.json.JSONArray
import org.json.JSONObject

internal fun firstImageFromAny(any: Any?, sourceUrl: String, depth: Int = 0): String {
    if (any == null || depth > 4) return ""
    return when (any) {
        is String -> {
            val normalized = normalizeImageUrl(sourceUrl, any)
            if (looksLikeImageUrl(normalized)) normalized else ""
        }

        is JSONObject -> {
            val keys = any.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val found = firstImageFromAny(any.opt(key), sourceUrl, depth + 1)
                if (found.isNotBlank()) return found
            }
            ""
        }

        is JSONArray -> {
            for (i in 0 until any.length()) {
                val found = firstImageFromAny(any.opt(i), sourceUrl, depth + 1)
                if (found.isNotBlank()) return found
            }
            ""
        }

        else -> ""
    }
}

internal fun normalizeGuideTabLabel(raw: String): String {
    return stripHtml(raw)
        .replace(Regex("\\s+"), "")
        .replace("（", "(")
        .replace("）", ")")
        .trim()
}

internal fun mapGuideTabByLabel(rawLabel: String, strict: Boolean): GuideTab? {
    val label = normalizeGuideTabLabel(rawLabel)
    if (label.isBlank()) return null
    if (strict) {
        return when (label) {
            GuideTab.Skills.label -> GuideTab.Skills
            GuideTab.Profile.label -> GuideTab.Profile
            GuideTab.Voice.label -> GuideTab.Voice
            GuideTab.Gallery.label -> GuideTab.Gallery
            GuideTab.Simulate.label -> GuideTab.Simulate
            else -> null
        }
    }
    return when {
        label.contains("语音") || label.contains("台词") -> GuideTab.Voice
        label.contains("影画") || label.contains("鉴赏") -> GuideTab.Gallery
        label.contains("养成") || label.contains("模拟") -> GuideTab.Simulate
        label.contains("档案") -> GuideTab.Profile
        label.contains("技能") -> GuideTab.Skills
        else -> null
    }
}

internal fun findImageByKnownKeys(obj: JSONObject, sourceUrl: String): String {
    val keys = listOf(
        "icon", "iconUrl", "icon_url", "tabIcon", "tab_icon",
        "img", "image", "imageUrl", "image_url",
        "thumb", "cover", "src", "url"
    )
    keys.forEach { key ->
        val any = obj.opt(key) ?: return@forEach
        val fromAny = when (any) {
            is String -> normalizeImageUrl(sourceUrl, any)
            is JSONObject, is JSONArray -> firstImageFromAny(any, sourceUrl)
            else -> ""
        }
        if (looksLikeImageUrl(fromAny)) return fromAny
    }
    return ""
}

internal fun extractGuideTabIcons(root: JSONObject, sourceUrl: String): Map<GuideTab, String> {
    val strict = mutableMapOf<GuideTab, String>()
    val fuzzy = mutableMapOf<GuideTab, String>()
    val labelKeys = listOf("name", "title", "label", "tabName", "tab_name", "text", "key")

    fun tryPut(tab: GuideTab?, icon: String, strictMode: Boolean) {
        if (tab == null || icon.isBlank()) return
        if (strictMode) {
            strict.putIfAbsent(tab, icon)
        } else {
            fuzzy.putIfAbsent(tab, icon)
        }
    }

    fun walk(any: Any?) {
        when (any) {
            is JSONObject -> {
                val label = labelKeys
                    .asSequence()
                    .map { key -> any.optString(key).trim() }
                    .firstOrNull { it.isNotBlank() }
                    .orEmpty()
                val icon = findImageByKnownKeys(any, sourceUrl)
                if (label.isNotBlank() && icon.isNotBlank()) {
                    tryPut(mapGuideTabByLabel(label, strict = true), icon, strictMode = true)
                    tryPut(mapGuideTabByLabel(label, strict = false), icon, strictMode = false)
                }

                val keys = any.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = any.opt(key)
                    val keyTabStrict = mapGuideTabByLabel(key, strict = true)
                    val keyTabFuzzy = mapGuideTabByLabel(key, strict = false)
                    if ((keyTabStrict != null || keyTabFuzzy != null) && value != null) {
                        val iconByValue = firstImageFromAny(value, sourceUrl)
                        if (iconByValue.isNotBlank()) {
                            tryPut(keyTabStrict, iconByValue, strictMode = true)
                            tryPut(keyTabFuzzy, iconByValue, strictMode = false)
                        }
                    }
                    walk(value)
                }
            }

            is JSONArray -> {
                for (i in 0 until any.length()) {
                    walk(any.opt(i))
                }
            }
        }
    }

    walk(root.opt("dataSource"))
    walk(root.opt("tabs"))
    walk(root)

    return buildMap {
        GuideTab.entries.forEach { tab ->
            val icon = strict[tab].orEmpty().ifBlank { fuzzy[tab].orEmpty() }
            if (icon.isNotBlank()) put(tab, icon)
        }
    }
}

internal data class ArrayVoiceEntryAccumulator(
    val section: String,
    val title: String,
    val order: Int,
    val linesByLanguage: LinkedHashMap<String, String> = linkedMapOf(),
    val audioByLanguage: LinkedHashMap<String, String> = linkedMapOf()
)

internal fun looksLikeMediaTokenText(raw: String): Boolean {
    val value = raw.trim()
    if (value.isBlank()) return false
    val lower = value.lowercase()
    if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("//")) return true
    if (lower.contains("cdnimg") || lower.contains("gamekee.com/")) return true
    return Regex("""\.(png|jpg|jpeg|webp|gif|bmp|svg|avif|mp4|webm|mov|m3u8|mp3|ogg|wav|m4a|aac)(\?.*)?(#.*)?$""")
        .containsMatchIn(lower)
}

internal fun extractEditorTextLines(any: Any?, depth: Int = 0): List<String> {
    if (any == null || depth > 10) return emptyList()
    return when (any) {
        is String -> {
            val plain = stripHtml(any)
            if (plain.isBlank() || looksLikeMediaTokenText(plain)) {
                emptyList()
            } else {
                listOf(plain)
            }
        }

        is JSONObject -> {
            val lines = mutableListOf<String>()
            val direct = any.opt("text")
            if (direct is String) {
                val text = stripHtml(direct)
                if (text.isNotBlank()) lines += text
            }
            val richKeys = listOf("children", "data", "content", "title", "name", "label", "desc", "value")
            richKeys.forEach { key ->
                if (!any.has(key)) return@forEach
                lines += extractEditorTextLines(any.opt(key), depth + 1)
            }
            lines
        }

        is JSONArray -> {
            buildList {
                for (i in 0 until any.length()) {
                    addAll(extractEditorTextLines(any.opt(i), depth + 1))
                }
            }
        }

        else -> emptyList()
    }
}

internal fun extractEditorText(any: Any?, separator: String = " "): String {
    val lines = extractEditorTextLines(any)
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (lines.isEmpty()) return ""
    return lines.joinToString(separator).trim()
}

internal fun normalizeArrayGalleryTitle(rawTitle: String): String {
    val title = stripHtml(rawTitle).replace(Regex("\\s+"), "").trim()
    if (title.isBlank()) return "影画"
    if (title == "表情包" || title.startsWith("表情包(") || title.startsWith("表情包（")) {
        return title.replace("表情包", "角色表情包")
    }
    if (title == "差分" || title == "表情差分") return "角色表情"
    if (title.startsWith("表情")) {
        val suffix = title.removePrefix("表情").trim()
        return "角色表情$suffix"
    }
    if (title.contains("表情差分")) {
        return title.replace("表情差分", "角色表情")
    }
    if (title.contains("差分")) {
        val context = title
            .replace("差分", "")
            .trim('（', '）', '(', ')', '-', '·', ' ')
        if (context.isBlank()) return "角色表情"
        return "角色表情（$context）"
    }
    return title
}

internal fun normalizeArrayProfileKey(rawKey: String): String {
    val key = stripHtml(rawKey).trim()
    if (key.isBlank()) return ""
    return when (key) {
        "所属" -> "所属学园"
        "学院" -> "所属学园"
        "社团" -> "所属社团"
        "兴趣" -> "兴趣爱好"
        "兴趣爱好（补充）" -> "兴趣爱好"
        "其他名字" -> "其他译名"
        "其他称呼" -> "其他译名"
        "译名" -> "其他译名"
        "国际服译名" -> "其他译名"
        "其他翻译" -> "其他译名"
        "黑话(别名)" -> "其他译名"
        "日语全名" -> "假名注音"
        else -> key
    }
}

internal fun parseVoiceCvByLanguageFromRaw(raw: String): Map<String, String> {
    if (raw.isBlank()) return emptyMap()
    val normalized = stripHtml(raw)
        .replace('｜', '|')
        .replace('：', ':')
        .replace('，', ',')
        .replace('；', ';')
        .replace('\u3000', ' ')
        .trim()
    if (normalized.isBlank()) return emptyMap()

    val labelPattern = "日配|日语|日|jp|jpn|中配|中|cn|国语|国配|中文|韩配|韩|kr|kor|korean|官翻|官中|官方翻译|官方中文"
    val pairRegex = Regex(
        """(?i)($labelPattern)\s*(?:[\|:：\-－—])\s*([\s\S]*?)(?=(?:\s*[,，;；/／\n]\s*|\s+)(?:$labelPattern)\s*(?:[\|:：\-－—])|$)"""
    )
    val out = linkedMapOf<String, String>()

    fun assign(labelRaw: String, valueRaw: String) {
        val label = canonicalVoiceLanguageLabel(labelRaw)
        if (label.isBlank()) return
        val value = valueRaw.trim()
            .trim(',', '，', ';', '；', '|', '/', '／')
            .trim()
        if (value.isBlank()) return
        if (out[label].isNullOrBlank()) {
            out[label] = value
        }
    }

    pairRegex.findAll(normalized).forEach { match ->
        val label = match.groupValues.getOrNull(1).orEmpty()
        val value = match.groupValues.getOrNull(2).orEmpty()
        assign(label, value)
    }

    if (out.isEmpty() && normalized.isNotBlank()) {
        out["日配"] = normalized
    }

    if (out.isEmpty()) return emptyMap()
    val ordered = linkedMapOf<String, String>()
    listOf("日配", "中配", "韩配", "官翻").forEach { key ->
        out[key]?.takeIf { it.isNotBlank() }?.let { ordered[key] = it }
    }
    out.forEach { (key, value) ->
        if (key !in ordered && value.isNotBlank()) {
            ordered[key] = value
        }
    }
    return ordered
}

internal fun parseGuideDetailFromArrayContentJson(raw: String, sourceUrl: String): GuideDetailExtract {
    if (raw.isBlank()) return GuideDetailExtract()
    return runCatching {
        val root = JSONArray(raw)
        val profileRows = mutableListOf<BaGuideRow>()
        val galleryItems = mutableListOf<BaGuideGalleryItem>()
        val stats = mutableListOf<Pair<String, String>>()
        val voiceAccumulators = linkedMapOf<String, ArrayVoiceEntryAccumulator>()
        val rawVoiceLanguageHeaders = mutableListOf<String>()
        val summaryCandidates = mutableListOf<String>()

        var firstImage = ""
        var tabGalleryIconUrl = ""
        var summary = ""
        var voiceOrder = 0

        fun pushProfileRow(
            key: String,
            value: String,
            imageUrl: String = "",
            imageUrls: List<String> = emptyList()
        ) {
            val normalizedKey = normalizeArrayProfileKey(key)
            val normalizedValue = value.trim()
            val normalizedImageUrl = normalizeImageUrl(sourceUrl, imageUrl)
            val normalizedImages = buildList {
                if (normalizedImageUrl.isNotBlank()) add(normalizedImageUrl)
                imageUrls.forEach { rawImage ->
                    val normalized = normalizeImageUrl(sourceUrl, rawImage)
                    if (normalized.isNotBlank()) add(normalized)
                }
            }.filter { looksLikeImageUrl(it) }.distinct()
            if (normalizedKey.isBlank() && normalizedValue.isBlank() && normalizedImages.isEmpty()) return
            profileRows += BaGuideRow(
                key = normalizedKey.ifBlank { "信息" },
                value = normalizedValue,
                imageUrl = normalizedImages.firstOrNull().orEmpty(),
                imageUrls = normalizedImages
            )
            if (normalizedKey.isNotBlank() &&
                isMeaningfulGuideRowValue(normalizedValue) &&
                stats.none { it.first == normalizedKey }
            ) {
                stats += normalizedKey to normalizedValue
                if (summaryCandidates.size < 4) {
                    summaryCandidates += "$normalizedKey：$normalizedValue"
                }
            }
        }

        fun pushGalleryItems(
            rawTitle: String,
            rawAny: Any?
        ) {
            val title = normalizeArrayGalleryTitle(rawTitle)
            val imageUrls = extractImageUrlsFromAny(sourceUrl, rawAny)
            val videoUrls = extractVideoUrlsFromAny(sourceUrl, rawAny)
            val audioUrls = extractAudioUrlsFromAny(sourceUrl, rawAny)

            if (firstImage.isBlank()) {
                firstImage = imageUrls.firstOrNull().orEmpty()
            }
            if (tabGalleryIconUrl.isBlank()) {
                tabGalleryIconUrl = imageUrls.firstOrNull().orEmpty()
            }

            if (imageUrls.isNotEmpty()) {
                galleryItems += imageUrls.mapIndexed { index, url ->
                    BaGuideGalleryItem(
                        title = if (imageUrls.size > 1) "$title ${index + 1}" else title,
                        imageUrl = url,
                        mediaType = "image",
                        mediaUrl = url
                    )
                }
            }
            if (videoUrls.isNotEmpty()) {
                galleryItems += videoUrls.mapIndexed { index, url ->
                    BaGuideGalleryItem(
                        title = if (videoUrls.size > 1) "$title ${index + 1}" else title,
                        imageUrl = imageUrls.firstOrNull().orEmpty(),
                        mediaType = "video",
                        mediaUrl = url
                    )
                }
            }
            if (audioUrls.isNotEmpty()) {
                galleryItems += audioUrls.mapIndexed { index, url ->
                    BaGuideGalleryItem(
                        title = if (audioUrls.size > 1) "$title ${index + 1}" else title,
                        imageUrl = imageUrls.firstOrNull().orEmpty(),
                        mediaType = "audio",
                        mediaUrl = url
                    )
                }
            }
        }

        fun ensureVoiceHeader(rawLabel: String): String {
            val canonical = canonicalVoiceLanguageLabel(rawLabel).ifBlank { stripHtml(rawLabel).trim() }
            if (canonical.isNotBlank() && canonical != "官翻" && canonical !in rawVoiceLanguageHeaders) {
                rawVoiceLanguageHeaders += canonical
            }
            return canonical
        }

        fun sanitizeVoiceSection(rawTitle: String, fallback: String): String {
            val title = stripHtml(rawTitle).trim()
            if (title.isBlank()) return fallback
            if (title.contains("分组标题")) return fallback
            return title
        }

        fun processAudioInfo(node: JSONObject) {
            val data = node.optJSONObject("data") ?: return
            val voiceRootTitle = extractEditorText(data.opt("title")).ifBlank { "语音台词" }
            val tabKeyToLabel = linkedMapOf<String, String>()

            val tabs = data.optJSONArray("tabs")
            if (tabs != null) {
                for (i in 0 until tabs.length()) {
                    val tab = tabs.optJSONObject(i) ?: continue
                    val key = tab.optString("key").trim()
                    if (key.isBlank()) continue
                    val label = ensureVoiceHeader(extractEditorText(tab.opt("label")).ifBlank { "语言${i + 1}" })
                    if (label.isNotBlank()) {
                        tabKeyToLabel[key] = label
                    }
                }
            }

            val list = data.optJSONArray("list") ?: return
            for (groupIndex in 0 until list.length()) {
                val group = list.optJSONObject(groupIndex) ?: continue
                val filterTabKey = group.optString("filterTabKey").trim()
                val language = tabKeyToLabel[filterTabKey]
                    ?: ensureVoiceHeader(extractEditorText(group.opt("label")))
                        .ifBlank { ensureVoiceHeader("语言${groupIndex + 1}") }
                val section = sanitizeVoiceSection(
                    rawTitle = extractEditorText(group.opt("title")),
                    fallback = voiceRootTitle
                )

                val content = group.optJSONArray("content") ?: continue
                for (contentIndex in 0 until content.length()) {
                    val item = content.optJSONObject(contentIndex) ?: continue
                    val title = extractEditorText(item.opt("name"))
                        .ifBlank { extractEditorText(item.opt("title")) }
                        .ifBlank { "语音${contentIndex + 1}" }
                    val descLines = extractEditorTextLines(item.opt("desc"))
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    val primaryLine = descLines.firstOrNull().orEmpty()
                    val officialLine = descLines.drop(1).firstOrNull().orEmpty()
                    val audioUrl = buildList {
                        val rawAudio = normalizeMediaUrl(sourceUrl, item.optString("audio"))
                        if (isAudioUrl(rawAudio)) add(rawAudio)
                        addAll(extractAudioUrlsFromAny(sourceUrl, item))
                    }.firstOrNull { it.isNotBlank() }
                        .orEmpty()

                    val key = "$section|$title"
                    val accumulator = voiceAccumulators.getOrPut(key) {
                        ArrayVoiceEntryAccumulator(
                            section = section,
                            title = title,
                            order = voiceOrder++
                        )
                    }
                    if (language.isNotBlank() && primaryLine.isNotBlank() && accumulator.linesByLanguage[language].isNullOrBlank()) {
                        accumulator.linesByLanguage[language] = primaryLine
                    }
                    if (language.isNotBlank() && audioUrl.isNotBlank() && accumulator.audioByLanguage[language].isNullOrBlank()) {
                        accumulator.audioByLanguage[language] = audioUrl
                    }
                    if (officialLine.isNotBlank() && accumulator.linesByLanguage["官翻"].isNullOrBlank()) {
                        accumulator.linesByLanguage["官翻"] = officialLine
                    }
                }
            }
        }

        fun processCharacterProfile(node: JSONObject) {
            val data = node.optJSONObject("data") ?: return
            val profileName = extractEditorText(data.opt("name")).trim()
            if (profileName.isNotBlank()) {
                pushProfileRow(
                    key = "角色名称",
                    value = profileName
                )
            }

            val attrList = data.optJSONArray("attrList")
            if (attrList != null) {
                for (index in 0 until attrList.length()) {
                    val item = attrList.optJSONObject(index) ?: continue
                    val key = extractEditorText(item.opt("title")).trim()
                    val value = extractEditorText(item.opt("content"), separator = " / ").trim()
                    val icons = extractImageUrlsFromAny(sourceUrl, item.opt("content"))
                    pushProfileRow(
                        key = key,
                        value = value,
                        imageUrl = icons.firstOrNull().orEmpty(),
                        imageUrls = icons
                    )
                }
            }

            val descTitle = extractEditorText(data.opt("descTitle")).ifBlank { "个人简介" }
            val descLines = extractEditorTextLines(data.opt("desc"))
                .map { it.trim() }
                .filter { it.isNotBlank() }
            val descValue = descLines.joinToString("\n").trim()
            if (descValue.isNotBlank()) {
                pushProfileRow(
                    key = descTitle,
                    value = descValue
                )
                if (summary.isBlank()) {
                    summary = descLines.take(2).joinToString(" ")
                }
            }

            val images = buildList {
                addAll(extractImageUrlsFromAny(sourceUrl, data.opt("imageList")))
                addAll(extractImageUrlsFromAny(sourceUrl, data.opt("imagesList")))
            }.distinct()
            if (images.isNotEmpty()) {
                if (firstImage.isBlank()) firstImage = images.first()
                if (tabGalleryIconUrl.isBlank()) tabGalleryIconUrl = images.first()
                galleryItems += images.mapIndexed { index, url ->
                    BaGuideGalleryItem(
                        title = if (images.size > 1) "立绘 ${index + 1}" else "立绘",
                        imageUrl = url,
                        mediaType = "image",
                        mediaUrl = url
                    )
                }
            }
        }

        fun processRelationInfo(node: JSONObject) {
            val data = node.optJSONObject("data") ?: return
            val list = data.optJSONArray("list") ?: return
            for (groupIndex in 0 until list.length()) {
                val group = list.optJSONObject(groupIndex) ?: continue
                val relationTitle = extractEditorText(group.opt("title")).ifBlank { "相关人物" }
                if (relationTitle.contains("同名")) {
                    pushProfileRow(
                        key = "相关同名角色",
                        value = relationTitle
                    )
                }
                val content = group.optJSONArray("content") ?: continue
                for (i in 0 until content.length()) {
                    val item = content.optJSONObject(i) ?: continue
                    val name = extractEditorText(item.opt("name")).trim()
                    val link = normalizeGuideUrl(item.optString("jumpHref")).trim()
                    val avatar = normalizeImageUrl(sourceUrl, item.optString("avatar")).trim()
                    val value = buildString {
                        if (name.isNotBlank()) append(name)
                        if (link.isNotBlank()) {
                            if (isNotEmpty()) append(" / ")
                            append(link)
                        }
                    }.trim()
                    if (value.isBlank() && avatar.isBlank()) continue
                    pushProfileRow(
                        key = "同名角色名称",
                        value = value,
                        imageUrl = avatar,
                        imageUrls = listOf(avatar)
                    )
                }
            }
        }

        fun processWeaponInfo(node: JSONObject) {
            val data = node.optJSONObject("data") ?: return
            val title = extractEditorText(data.opt("title")).trim()
            if (!title.contains("巧克力")) return
            val icon = normalizeImageUrl(sourceUrl, data.optString("icon")).trim()
            val name = extractEditorText(data.opt("name"), separator = " / ").trim()
            val desc = extractEditorText(data.opt("desc"), separator = " / ").trim()
            if (name.isNotBlank() || icon.isNotBlank()) {
                pushProfileRow(
                    key = "巧克力",
                    value = name.ifBlank { title },
                    imageUrl = icon,
                    imageUrls = listOf(icon)
                )
            }
            if (desc.isNotBlank()) {
                pushProfileRow(
                    key = "巧克力简介",
                    value = desc
                )
            }
            if (icon.isNotBlank() && looksLikeImageUrl(icon)) {
                galleryItems += BaGuideGalleryItem(
                    title = "巧克力图",
                    imageUrl = icon,
                    mediaType = "image",
                    mediaUrl = icon
                )
            }
        }

        fun processTabInfo(node: JSONObject) {
            val data = node.optJSONObject("data") ?: return
            val tabList = data.optJSONArray("tabList") ?: return
            for (i in 0 until tabList.length()) {
                val tab = tabList.optJSONObject(i) ?: continue
                val title = extractEditorText(tab.opt("title"))
                    .ifBlank { tab.optString("title").trim() }
                    .ifBlank { "影画" }
                val galleryRaw = tab.opt("content") ?: tab
                pushGalleryItems(
                    rawTitle = title,
                    rawAny = galleryRaw
                )
                val links = buildList {
                    addAll(extractWebUrlsFromAny(sourceUrl, tab.opt("topDesc")))
                    addAll(extractWebUrlsFromAny(sourceUrl, tab.opt("bottomDesc")))
                    addAll(extractWebUrlsFromAny(sourceUrl, tab.opt("desc")))
                    val content = tab.opt("content")
                    when (content) {
                        is JSONArray -> if (content.length() in 1..8) {
                            addAll(extractWebUrlsFromAny(sourceUrl, content))
                        }

                        else -> addAll(extractWebUrlsFromAny(sourceUrl, content))
                    }
                }.distinct()
                links.forEach { link ->
                    pushProfileRow(
                        key = "影画相关链接",
                        value = if (title.isNotBlank()) "$title / $link" else link
                    )
                }
            }
        }

        fun walk(any: Any?) {
            when (any) {
                is JSONObject -> {
                    when (any.optString("type").trim()) {
                        "tab-info" -> processTabInfo(any)
                        "character-profile" -> processCharacterProfile(any)
                        "relation-info" -> processRelationInfo(any)
                        "weapon-info" -> processWeaponInfo(any)
                        "audio-info" -> processAudioInfo(any)
                    }
                    val keys = any.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        walk(any.opt(key))
                    }
                }

                is JSONArray -> {
                    for (i in 0 until any.length()) {
                        walk(any.opt(i))
                    }
                }
            }
        }

        walk(root)

        val cvFromProfile = profileRows.asSequence()
            .firstOrNull { row -> row.key.contains("声优") && row.value.isNotBlank() }
            ?.value
            .orEmpty()
        val voiceCvByLanguage = parseVoiceCvByLanguageFromRaw(cvFromProfile)

        val rawVoiceEntries = voiceAccumulators.values
            .sortedBy { it.order }
            .map { acc ->
                val linePairs = sortVoiceLinePairsForDisplay(
                    acc.linesByLanguage.map { it.key to it.value }
                )
                val lineHeaders = linePairs.map { it.first }
                val lines = linePairs.map { it.second }
                val audioUrls = lineHeaders.map { label ->
                    acc.audioByLanguage[label].orEmpty()
                }
                BaGuideVoiceEntry(
                    section = acc.section,
                    title = acc.title,
                    lineHeaders = lineHeaders,
                    lines = lines,
                    audioUrls = audioUrls,
                    audioUrl = audioUrls.firstOrNull { it.isNotBlank() }
                        ?: acc.audioByLanguage.values.firstOrNull { it.isNotBlank() }
                        .orEmpty()
                )
            }
            .filter { entry ->
                entry.lines.any { it.isNotBlank() } || entry.audioUrls.any { it.isNotBlank() }
            }

        val voiceLanguageHeaders = mergeVoiceLanguageHeaders(
            rawHeaders = rawVoiceLanguageHeaders,
            voiceEntries = rawVoiceEntries,
            cvByLanguage = voiceCvByLanguage
        )
        val voiceEntries = normalizeVoiceEntriesWithHeaderCount(
            entries = rawVoiceEntries,
            headerCount = voiceLanguageHeaders.size
        )
        val (voiceCvJp, voiceCvCn) = deriveVoiceCvLegacyFields(voiceCvByLanguage)

        fun normalizedGalleryTitle(rawTitle: String): String = rawTitle.replace(" ", "").trim()

        fun galleryCategoryOrder(rawTitle: String): Int {
            val title = normalizedGalleryTitle(rawTitle)
            return when {
                title.startsWith("立绘") -> 0
                title.startsWith("回忆大厅") && !title.startsWith("回忆大厅视频") -> 1
                title.startsWith("回忆大厅视频") -> 2
                title.startsWith("BGM") -> 3
                title.startsWith("官方介绍") -> 4
                title.startsWith("本家画") -> 5
                title.startsWith("官方衍生") -> 6
                title.startsWith("TV动画设定图") -> 7
                title.startsWith("设定集") -> 8
                isExpressionGalleryItem(BaGuideGalleryItem(title = title, imageUrl = "", mediaUrl = "")) -> 9
                title.startsWith("互动家具") -> 10
                title.startsWith("情人节巧克力") -> 11
                title.startsWith("巧克力图") -> 12
                title.startsWith("PV") -> 13
                title.startsWith("角色演示") -> 14
                title.startsWith("Live") -> 15
                else -> 99
            }
        }

        fun galleryTitleGroupKey(rawTitle: String): String {
            val normalized = normalizedGalleryTitle(rawTitle)
            return normalized.replace(Regex("""\d+$"""), "")
        }

        fun galleryItemIndex(rawTitle: String): Int {
            return Regex("""(\d+)(?!.*\d)""")
                .find(normalizedGalleryTitle(rawTitle))
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?: Int.MAX_VALUE
        }

        val distinctGallery = galleryItems
            .filter {
                val media = it.mediaUrl.ifBlank { it.imageUrl }
                media.isNotBlank()
            }
            .distinctBy {
                val media = it.mediaUrl.ifBlank { it.imageUrl }
                "${it.mediaType}|$media"
            }
            .sortedWith(
                compareBy<BaGuideGalleryItem> { galleryCategoryOrder(it.title) }
                    .thenBy { galleryTitleGroupKey(it.title) }
                    .thenBy { galleryItemIndex(it.title) }
            )
            .take(100)

        val mergedProfileRows = profileRows
            .distinctBy { row ->
                val packedImages = row.imageUrls.joinToString("|")
                "${row.key.trim()}|${row.value.trim()}|${row.imageUrl.trim()}|$packedImages"
            }
            .take(180)

        val resolvedFirstImage = firstImage
            .ifBlank { distinctGallery.firstOrNull { it.imageUrl.isNotBlank() }?.imageUrl.orEmpty() }
        val resolvedSummary = summary
            .ifBlank { summaryCandidates.joinToString(" · ") }
            .ifBlank { "NPC及卫星图鉴条目数据较少，已展示可用信息。" }

        GuideDetailExtract(
            imageUrl = resolvedFirstImage,
            summary = resolvedSummary,
            stats = stats.take(14),
            skillRows = emptyList(),
            profileRows = mergedProfileRows,
            galleryItems = distinctGallery,
            growthRows = emptyList(),
            simulateRows = emptyList(),
            voiceRows = emptyList(),
            voiceCvJp = voiceCvJp,
            voiceCvCn = voiceCvCn,
            voiceCvByLanguage = voiceCvByLanguage,
            voiceLanguageHeaders = voiceLanguageHeaders,
            voiceEntries = voiceEntries,
            tabSkillIconUrl = "",
            tabProfileIconUrl = resolvedFirstImage,
            tabVoiceIconUrl = "",
            tabGalleryIconUrl = tabGalleryIconUrl.ifBlank { resolvedFirstImage },
            tabSimulateIconUrl = ""
        )
    }.getOrDefault(GuideDetailExtract())
}

internal fun parseGuideDetailFromContentJson(raw: String, sourceUrl: String): GuideDetailExtract {
    if (raw.isBlank()) return GuideDetailExtract()
    val trimmed = raw.trimStart()
    if (trimmed.startsWith("[")) {
        return parseGuideDetailFromArrayContentJson(trimmed, sourceUrl)
    }
    return runCatching {
        val root = JSONObject(raw)
        val tabIcons = extractGuideTabIcons(root, sourceUrl)
        val styleData = root.optJSONArray("styleData")
        val galleryFromStyleData = parseGalleryItemsFromStyleData(styleData, sourceUrl)
        val baseData = root.optJSONArray("baseData")
            ?: return@runCatching GuideDetailExtract(
                galleryItems = galleryFromStyleData,
                tabSkillIconUrl = tabIcons[GuideTab.Skills].orEmpty(),
                tabProfileIconUrl = tabIcons[GuideTab.Profile].orEmpty(),
                tabVoiceIconUrl = tabIcons[GuideTab.Voice].orEmpty(),
                tabGalleryIconUrl = tabIcons[GuideTab.Gallery].orEmpty(),
                tabSimulateIconUrl = tabIcons[GuideTab.Simulate].orEmpty()
            )
        val (rawVoiceLanguageHeaders, rawVoiceEntries) = parseVoiceDataFromBaseData(baseData, sourceUrl)
        val voiceCvByLanguage = parseVoiceCvByLanguageFromBaseData(baseData)
        val voiceLanguageHeaders = mergeVoiceLanguageHeaders(
            rawHeaders = rawVoiceLanguageHeaders,
            voiceEntries = rawVoiceEntries,
            cvByLanguage = voiceCvByLanguage
        )
        val voiceEntries = normalizeVoiceEntriesWithHeaderCount(
            entries = rawVoiceEntries,
            headerCount = voiceLanguageHeaders.size
        )
        val (voiceCvJp, voiceCvCn) = deriveVoiceCvLegacyFields(voiceCvByLanguage)
        val galleryFromMediaTypes = parseGalleryItemsFromBaseData(baseData, sourceUrl)
        val giftPreferenceRows = parseGiftPreferenceRowsFromBaseData(baseData, sourceUrl)
        val simulateRows = parseSimulateRowsFromBaseData(baseData, sourceUrl)
        val baseRows = mutableListOf<GuideBaseRow>()
        var firstImage = ""

        for (i in 0 until baseData.length()) {
            val row = baseData.optJSONArray(i) ?: continue
            if (row.length() == 0) continue
            val key = stripHtml((row.optJSONObject(0)?.optString("value") ?: "").trim())
            val textValues = mutableListOf<String>()
            val imageValues = mutableListOf<String>()
            val videoValues = mutableListOf<String>()
            val mediaTypes = mutableSetOf<String>()
            for (j in 1 until row.length()) {
                val cell = row.optJSONObject(j) ?: continue
                val type = cell.optString("type").trim().lowercase()
                val rawValueAny = cell.opt("value")
                val rawValue = cell.optString("value").trim()
                if (rawValue.isBlank()) continue

                when (type) {
                    "image" -> {
                        if (isPlaceholderMediaToken(rawValue)) continue
                        val normalized = normalizeImageUrl(sourceUrl, rawValue)
                        if (looksLikeImageUrl(normalized)) {
                            mediaTypes += type
                            imageValues += normalized
                        }
                    }

                    "imageset", "live2d" -> {
                        val images = extractImageUrlsFromAny(sourceUrl, rawValueAny)
                        if (images.isNotEmpty()) {
                            mediaTypes += type
                            imageValues += images
                        }
                    }

                    "video" -> {
                        val directVideo = normalizeMediaUrl(sourceUrl, rawValue)
                        val videos = buildList {
                            if (looksLikeVideoUrl(directVideo)) add(directVideo)
                            addAll(extractVideoUrlsFromAny(sourceUrl, rawValueAny))
                        }.distinct()
                        if (videos.isNotEmpty()) {
                            mediaTypes += type
                            videoValues += videos
                        }
                        val inlineImages = extractImageUrlsFromAny(sourceUrl, rawValueAny)
                        if (inlineImages.isNotEmpty()) imageValues += inlineImages
                    }

                    else -> {
                        val inlineImages = extractImageUrlsFromHtml(sourceUrl, rawValue)
                        if (inlineImages.isNotEmpty()) imageValues += inlineImages
                        videoValues += extractVideoUrlsFromAny(sourceUrl, rawValueAny)
                        val normalized = stripHtml(rawValue)
                        if (normalized.isNotBlank()) textValues += normalized
                    }
                }
            }
            if (firstImage.isBlank() && imageValues.isNotEmpty()) {
                firstImage = imageValues.first()
            }
            baseRows += GuideBaseRow(
                key = key,
                textValues = textValues,
                imageValues = imageValues.distinct(),
                videoValues = videoValues.distinct(),
                mediaTypes = mediaTypes
            )
        }

        val memoryUnlockLevel = run {
            val raw = baseRows.firstOrNull { it.key == "回忆大厅解锁等级" }
                ?.textValues
                ?.joinToString(" ")
                .orEmpty()
            val digits = Regex("""\d+""").find(raw)?.value.orEmpty()
            if (digits.isNotBlank()) digits else raw
        }

        fun containsAny(target: String, keywords: List<String>): Boolean {
            return keywords.any { key -> target.contains(key, ignoreCase = true) }
        }

        fun isGrowthTitleVoiceKey(raw: String): Boolean {
            val normalized = raw.replace(" ", "").lowercase()
            if (normalized.isBlank()) return false
            return (normalized.contains("成长") && normalized.contains("title")) ||
                normalized.contains("成长标题") ||
                normalized.contains("growthtitle") ||
                normalized.contains("growth_title")
        }

        val skillKeywords = listOf(
            "技能", "EX", "普通技能", "被动技能", "辅助技能", "固有", "技能COST", "技能图标", "技能描述", "技能名称",
            "技能类型", "技能名词", "LV"
        )
        val profileKeywords = listOf(
            "学生信息", "角色名称", "全名", "假名注音", "简中译名", "繁中译名", "稀有度", "战术作用", "所属学园",
            "所属社团", "实装日期", "攻击类型", "防御类型", "位置", "市街", "屋外", "屋内", "武器类型", "年龄", "生日",
            "兴趣爱好", "声优", "画师", "介绍", "个人简介", "MomoTalk", "回忆大厅解锁等级", "同名角色", "角色头像"
        )
        val galleryKeywords = listOf(
            "立绘", "本家画", "TV动画设定图", "回忆大厅视频", "回忆大厅", "PV", "Live", "巧克力图",
            "互动家具", "角色表情", "设定集", "官方介绍", "官方衍生", "情人节巧克力", "BGM"
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
        val growthKeywords = listOf(
            "装备", "专武", "能力解放", "羁绊", "羁绊奖励", "升级材料",
            "所需", "爱用品", "羁绊等级奖励"
        )
        val voiceKeywords = listOf("通常", "战斗", "活动", "大厅及咖啡馆", "事件", "好感度", "成长")

        val skillRows = mutableListOf<BaGuideRow>()
        val profileRows = mutableListOf<BaGuideRow>()
        val growthRows = mutableListOf<BaGuideRow>()
        val voiceRows = mutableListOf<BaGuideRow>()
        val galleryItems = mutableListOf<BaGuideGalleryItem>()
        val stats = mutableListOf<Pair<String, String>>()
        val summaryCandidates = mutableListOf<String>()

        fun noteForGalleryImage(textValues: List<String>, index: Int, imageCount: Int): String {
            val normalized = textValues.map { it.trim() }.filter { it.isNotBlank() }
            if (normalized.isEmpty()) return ""
            if (imageCount <= 1) return normalized.joinToString(" / ")
            if (normalized.size == imageCount) return normalized.getOrElse(index) { "" }
            if (normalized.size == 1) return if (index == imageCount - 1) normalized.first() else ""
            return normalized.getOrElse(index) { normalized.last() }
        }

        var inSkillBlock = false
        var inSkillGlossaryBlock = false
        var inWeaponBlock = false
        var inGrowthBlock = false
        var inGalleryContext = false
        var inVoiceContext = false
        var currentVoiceSection = ""
        var inTopDataContext = false
        var lastGalleryTitle = ""

        fun isGrowthBlockStartKey(raw: String): Boolean {
            val key = normalizeGuideRowKey(raw)
            if (key.isBlank()) return false
            return key == "专武" ||
                key == "装备" ||
                key == "爱用品" ||
                key == "能力解放" ||
                key.contains("羁绊等级奖励") ||
                key.contains("羁绊奖励")
        }

        fun isGrowthBlockStopKey(raw: String): Boolean {
            val key = normalizeGuideRowKey(raw)
            if (key.isBlank()) return false
            if (isGrowthBlockStartKey(key)) return false
            return key == "礼物偏好" ||
                key == "相关同名角色" ||
                key == "同名角色名称" ||
                key == "技能类型" ||
                key == "技能名词" ||
                key == "学生信息" ||
                key == "介绍" ||
                key == "配音语言" ||
                key == "配音" ||
                key == "配音大类" ||
                key == "初始数据" ||
                key == "顶级数据" ||
                isVoiceCategoryKey(key) ||
                galleryContextStartKeywords.any { keyword ->
                    key.contains(keyword, ignoreCase = true)
                }
        }

        baseRows.forEach { row ->
            val key = row.key
            val value = row.textValues.joinToString(" / ")
            val imageUrl = row.imageValues.firstOrNull().orEmpty()
            val videoUrl = row.videoValues.firstOrNull().orEmpty()
            if (key.isBlank() && value.isBlank() && imageUrl.isBlank() && videoUrl.isBlank()) return@forEach

            val guideRow = BaGuideRow(
                key = key.ifBlank { "信息" },
                value = value,
                imageUrl = imageUrl,
                imageUrls = row.imageValues.distinct()
            )
            val hasMeaningfulRowValue = isMeaningfulGuideRowValue(guideRow.value)
            val normalizedKey = key.ifBlank { value }
                .replace("\n", " ")
                .trim()
            if (normalizedKey == "回忆大厅解锁等级") {
                return@forEach
            }
            if (normalizedKey.replace(" ", "").startsWith("回忆大厅文件")) {
                return@forEach
            }
            val normalizedGuideKey = normalizeGuideRowKey(normalizedKey)
            if (normalizedGuideKey == "顶级数据") {
                inTopDataContext = true
            } else if (inTopDataContext && (
                    normalizedGuideKey == "专武" ||
                        normalizedGuideKey == "装备" ||
                        normalizedGuideKey == "爱用品" ||
                        normalizedGuideKey == "能力解放" ||
                        normalizedGuideKey.contains("羁绊等级奖励")
                    )
            ) {
                inTopDataContext = false
            }
            if (normalizedKey == "配音语言") {
                inVoiceContext = true
                currentVoiceSection = ""
                return@forEach
            }
            if (normalizedKey == "配音" || normalizedKey == "配音大类") {
                return@forEach
            }
            val isVoiceCategoryRow = isVoiceCategoryKey(normalizedKey)
            if (isVoiceCategoryRow) {
                inVoiceContext = true
                currentVoiceSection = normalizedKey
            } else if (inVoiceContext && isVoiceBlockTailKey(normalizedKey)) {
                inVoiceContext = false
                currentVoiceSection = ""
            }
            val normalizedVoiceTexts = row.textValues
                .map { it.trim() }
                .filter { it.isNotBlank() }
            val hasMeaningfulVoicePayload = normalizedVoiceTexts.size >= 2
            val isVoiceByContext = inVoiceContext && (
                isVoiceCategoryRow || (currentVoiceSection.isNotBlank() && hasMeaningfulVoicePayload)
            )
            if (inVoiceContext &&
                !isVoiceCategoryRow &&
                !hasMeaningfulVoicePayload &&
                row.imageValues.isEmpty() &&
                row.videoValues.isEmpty()
            ) {
                // Skip placeholder rows such as "被CC5" that should not leak into profile.
                return@forEach
            }
            val isWeaponBlockStart = normalizedKey == "专武"
            val isWeaponBlockEnd = normalizedKey.contains("爱用品") ||
                normalizedKey.contains("专武考据") ||
                normalizedKey == "初始数据"
            val isSkillBlockStart = normalizedKey == "技能类型"
            val isSkillGlossaryStart = normalizedKey == "技能名词"
            val isSkillBlockEnd = isWeaponBlockStart ||
                normalizedKey.contains("升级材料") ||
                normalizedKey == "初始数据"
            val isSkillGlossaryEnd = isWeaponBlockStart ||
                normalizedKey.contains("升级材料") ||
                normalizedKey == "初始数据"
            if (isWeaponBlockStart) {
                inWeaponBlock = true
                inGrowthBlock = true
                inSkillBlock = false
                inSkillGlossaryBlock = false
            }
            if (isGrowthBlockStartKey(normalizedGuideKey)) {
                inGrowthBlock = true
            } else if (inGrowthBlock && isGrowthBlockStopKey(normalizedGuideKey)) {
                inGrowthBlock = false
            }
            if (isSkillBlockStart) {
                inSkillBlock = true
            }
            if (isSkillGlossaryStart) {
                inSkillGlossaryBlock = true
            }
            val isGalleryContextStart = galleryContextStartKeywords.any {
                normalizedKey.contains(it, ignoreCase = true)
            }
            val isNonGallerySectionStart = normalizedKey.isNotBlank() && nonGallerySectionKeywords.any {
                normalizedKey.contains(it, ignoreCase = true)
            }
            if (isNonGallerySectionStart && !isGalleryContextStart) {
                inGalleryContext = false
                lastGalleryTitle = ""
            }
            if (isGalleryContextStart) {
                inGalleryContext = true
                if (guideRow.key.isNotBlank()) {
                    lastGalleryTitle = guideRow.key
                }
            }

            val isVoice = containsAny(normalizedKey, voiceKeywords) || isGrowthTitleVoiceKey(normalizedKey) || isVoiceByContext
            val matchesSkillKeywords = containsAny(normalizedKey, skillKeywords)
            val matchesGrowthKeywords = containsAny(normalizedKey, growthKeywords)
            val isSkillMigratedRow =
                isWeaponExtraAttributeKey(normalizedKey) ||
                    normalizedGuideKey == "25级" ||
                    normalizedGuideKey == "顶级数据" ||
                    (inTopDataContext && isTopDataStatKey(normalizedKey))
            val isLevelRow = key.trim().matches(Regex("""(?i)^LV\.?\d{1,2}$"""))
            val isSkill = (inSkillBlock && (isLevelRow || normalizedKey == "技能COST" || normalizedKey == "技能描述" || normalizedKey == "技能图标" || normalizedKey == "技能名称" || normalizedKey == "技能类型")) ||
                (inSkillGlossaryBlock && normalizedKey.isNotBlank() && !inWeaponBlock) ||
                (matchesSkillKeywords && !inWeaponBlock) ||
                isSkillMigratedRow
            val isProfile = containsAny(normalizedKey, profileKeywords)
            val isGrowth = inWeaponBlock ||
                inGrowthBlock ||
                (matchesGrowthKeywords && !isSkill && !isVoice && !isProfile)
            val hasMedia = row.imageValues.isNotEmpty() || row.videoValues.isNotEmpty()
            val isFallbackGallery =
                hasMedia &&
                !isSkill &&
                !isGrowth &&
                !isVoice &&
                !isProfile &&
                inGalleryContext &&
                nonGalleryFallbackKeywords.none { normalizedKey.contains(it, ignoreCase = true) }
            val isGallery = containsAny(normalizedKey, galleryKeywords) || isFallbackGallery
            val galleryTitle = guideRow.key.ifBlank { lastGalleryTitle.ifBlank { "影画" } }

            when {
                isVoice && !inWeaponBlock -> voiceRows += guideRow
                isGrowth -> growthRows += guideRow
                isSkill -> skillRows += guideRow
                isGallery -> {
                    if (row.imageValues.isNotEmpty()) {
                        galleryItems += row.imageValues.mapIndexed { index, url ->
                            BaGuideGalleryItem(
                                title = if (row.imageValues.size > 1) "$galleryTitle ${index + 1}" else galleryTitle,
                                imageUrl = url,
                                mediaType = if (row.mediaTypes.contains("live2d")) "live2d" else "image",
                                mediaUrl = url,
                                memoryUnlockLevel = if (guideRow.key.startsWith("回忆大厅")) memoryUnlockLevel else "",
                                note = noteForGalleryImage(row.textValues, index, row.imageValues.size)
                            )
                        }
                    }
                    if (row.videoValues.isNotEmpty()) {
                        val videoNote = row.textValues.joinToString(" / ").trim()
                        galleryItems += row.videoValues.mapIndexed { index, url ->
                            BaGuideGalleryItem(
                                title = if (row.videoValues.size > 1) "$galleryTitle ${index + 1}" else galleryTitle,
                                imageUrl = row.imageValues.firstOrNull().orEmpty(),
                                mediaType = "video",
                                mediaUrl = url,
                                memoryUnlockLevel = if (guideRow.key.startsWith("回忆大厅")) memoryUnlockLevel else "",
                                note = videoNote
                            )
                        }
                    }
                    val isPureMediaText = row.textValues.any { text ->
                        val normalized = normalizeMediaUrl(sourceUrl, text)
                        isAudioUrl(normalized) || looksLikeVideoUrl(normalized) || looksLikeImageUrl(normalized)
                    }
                    if (row.imageValues.isEmpty() &&
                        row.videoValues.isEmpty() &&
                        hasMeaningfulRowValue &&
                        !isPureMediaText
                    ) {
                        profileRows += guideRow
                    }
                }

                isProfile -> profileRows += guideRow
                else -> {
                    if (guideRow.key.isNotBlank() && hasMeaningfulRowValue) {
                        profileRows += guideRow
                    }
                }
            }

            if (guideRow.key.isNotBlank() && hasMeaningfulRowValue && stats.none { it.first == guideRow.key }) {
                stats += guideRow.key to guideRow.value
                if (summaryCandidates.size < 4) {
                    summaryCandidates += "${guideRow.key}：${guideRow.value}"
                }
            }

            if (isWeaponBlockEnd) {
                inWeaponBlock = false
                if (!isGrowthBlockStartKey(normalizedGuideKey)) {
                    inGrowthBlock = false
                }
            }
            if (isSkillBlockEnd) {
                inSkillBlock = false
            }
            if (isSkillGlossaryEnd) {
                inSkillGlossaryBlock = false
            }
        }

        fun normalizedGalleryTitle(raw: String): String = raw.replace(" ", "").trim()

        fun galleryCategoryOrder(rawTitle: String): Int {
            val title = normalizedGalleryTitle(rawTitle)
            return when {
                title.startsWith("立绘") -> 0
                title.startsWith("回忆大厅") && !title.startsWith("回忆大厅视频") -> 1
                title.startsWith("回忆大厅视频") -> 2
                title.startsWith("BGM") -> 3
                title.startsWith("官方介绍") -> 4
                title.startsWith("本家画") -> 5
                title.startsWith("官方衍生") -> 6
                title.startsWith("TV动画设定图") -> 7
                title.startsWith("设定集") -> 8
                title.startsWith("角色表情") -> 9
                title.startsWith("互动家具") -> 10
                title.startsWith("情人节巧克力") -> 11
                title.startsWith("巧克力图") -> 12
                title.startsWith("PV") -> 13
                title.startsWith("角色演示") -> 14
                title.startsWith("Live") -> 15
                else -> 99
            }
        }

        fun galleryTitleGroupKey(rawTitle: String): String {
            val normalized = normalizedGalleryTitle(rawTitle)
            return normalized.replace(Regex("""\d+$"""), "")
        }

        fun galleryItemIndex(rawTitle: String): Int {
            return Regex("""(\d+)(?!.*\d)""")
                .find(normalizedGalleryTitle(rawTitle))
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?: Int.MAX_VALUE
        }

        val distinctGallery = galleryItems
            .plus(galleryFromMediaTypes)
            .plus(galleryFromStyleData)
            .filter {
                val media = it.mediaUrl.ifBlank { it.imageUrl }
                media.isNotBlank()
            }
            .distinctBy {
                val media = it.mediaUrl.ifBlank { it.imageUrl }
                "${it.mediaType}|$media"
            }
            .sortedWith(
                compareBy<BaGuideGalleryItem> { galleryCategoryOrder(it.title) }
                    .thenBy { galleryTitleGroupKey(it.title) }
                    .thenBy { galleryItemIndex(it.title) }
            )
            .take(100)
        val mergedProfileRows = (profileRows + giftPreferenceRows)
            .distinctBy { row ->
                val packedImages = row.imageUrls.joinToString("|")
                "${row.key.trim()}|${row.value.trim()}|${row.imageUrl.trim()}|$packedImages"
            }
            .take(180)

        GuideDetailExtract(
            imageUrl = firstImage,
            summary = summaryCandidates.joinToString(" · "),
            stats = stats.take(14),
            // 不截断技能行，避免长描述/术语图标在后段时被裁剪。
            skillRows = skillRows,
            profileRows = mergedProfileRows,
            galleryItems = distinctGallery,
            growthRows = growthRows.take(160),
            simulateRows = simulateRows,
            voiceRows = voiceRows.take(160),
            voiceCvJp = voiceCvJp,
            voiceCvCn = voiceCvCn,
            voiceCvByLanguage = voiceCvByLanguage,
            voiceLanguageHeaders = voiceLanguageHeaders,
            voiceEntries = voiceEntries,
            tabSkillIconUrl = tabIcons[GuideTab.Skills].orEmpty(),
            tabProfileIconUrl = tabIcons[GuideTab.Profile].orEmpty(),
            tabVoiceIconUrl = tabIcons[GuideTab.Voice].orEmpty(),
            tabGalleryIconUrl = tabIcons[GuideTab.Gallery].orEmpty(),
            tabSimulateIconUrl = tabIcons[GuideTab.Simulate].orEmpty()
        )
    }.getOrDefault(GuideDetailExtract())
}

