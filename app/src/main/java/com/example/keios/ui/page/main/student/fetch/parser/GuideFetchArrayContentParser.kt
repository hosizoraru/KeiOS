package com.example.keios.ui.page.main.student.fetch.parser

import com.example.keios.ui.page.main.student.BaGuideGalleryItem
import com.example.keios.ui.page.main.student.BaGuideRow
import com.example.keios.ui.page.main.student.BaGuideVoiceEntry
import com.example.keios.ui.page.main.student.fetch.GuideDetailExtract
import com.example.keios.ui.page.main.student.fetch.canonicalVoiceLanguageLabel
import com.example.keios.ui.page.main.student.fetch.deriveVoiceCvLegacyFields
import com.example.keios.ui.page.main.student.fetch.extractAudioUrlsFromAny
import com.example.keios.ui.page.main.student.fetch.extractImageUrlsFromAny
import com.example.keios.ui.page.main.student.fetch.extractVideoUrlsFromAny
import com.example.keios.ui.page.main.student.fetch.extractWebUrlsFromAny
import com.example.keios.ui.page.main.student.fetch.isAudioUrl
import com.example.keios.ui.page.main.student.fetch.isMeaningfulGuideRowValue
import com.example.keios.ui.page.main.student.fetch.looksLikeImageUrl
import com.example.keios.ui.page.main.student.fetch.mergeVoiceLanguageHeaders
import com.example.keios.ui.page.main.student.fetch.normalizeGuideUrl
import com.example.keios.ui.page.main.student.fetch.normalizeImageUrl
import com.example.keios.ui.page.main.student.fetch.normalizeMediaUrl
import com.example.keios.ui.page.main.student.fetch.normalizeVoiceEntriesWithHeaderCount
import com.example.keios.ui.page.main.student.fetch.sortVoiceLinePairsForDisplay
import com.example.keios.ui.page.main.student.fetch.stripHtml
import org.json.JSONArray
import org.json.JSONObject
import kotlin.collections.plusAssign

internal data class ArrayVoiceEntryAccumulator(
    val section: String,
    val title: String,
    val order: Int,
    val linesByLanguage: LinkedHashMap<String, String> = linkedMapOf(),
    val audioByLanguage: LinkedHashMap<String, String> = linkedMapOf()
)

internal fun parseGuideDetailFromArrayContentJson(raw: String, sourceUrl: String): GuideDetailExtract {
    if (raw.isBlank()) return GuideDetailExtract()
    return runCatching {
        val root = JSONArray(raw)
        val accumulator = ArrayGuideParseAccumulator(sourceUrl)
        val voiceAccumulators = linkedMapOf<String, ArrayVoiceEntryAccumulator>()
        val rawVoiceLanguageHeaders = mutableListOf<String>()

        var summary = ""
        var voiceOrder = 0

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
                accumulator.pushProfileRow(
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
                    accumulator.pushProfileRow(
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
                accumulator.pushProfileRow(
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
                if (accumulator.firstImage.isBlank()) accumulator.firstImage = images.first()
                if (accumulator.tabGalleryIconUrl.isBlank()) accumulator.tabGalleryIconUrl = images.first()
                accumulator.galleryItems += images.mapIndexed { index, url ->
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
                    accumulator.pushProfileRow(
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
                    accumulator.pushProfileRow(
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
                accumulator.pushProfileRow(
                    key = "巧克力",
                    value = name.ifBlank { title },
                    imageUrl = icon,
                    imageUrls = listOf(icon)
                )
            }
            if (desc.isNotBlank()) {
                accumulator.pushProfileRow(
                    key = "巧克力简介",
                    value = desc
                )
            }
            if (icon.isNotBlank() && looksLikeImageUrl(icon)) {
                accumulator.galleryItems += BaGuideGalleryItem(
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
                accumulator.pushGalleryItems(
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
                    accumulator.pushProfileRow(
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

        val cvFromProfile = accumulator.profileRows.asSequence()
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

        val distinctGallery = accumulator.galleryItems
            .filter {
                val media = it.mediaUrl.ifBlank { it.imageUrl }
                media.isNotBlank()
            }
            .distinctBy {
                val media = it.mediaUrl.ifBlank { it.imageUrl }
                "${it.mediaType}|$media"
            }
            .sortedWith(
                compareBy<BaGuideGalleryItem> { guideGalleryCategoryOrder(it.title) }
                    .thenBy { guideGalleryTitleGroupKey(it.title) }
                    .thenBy { guideGalleryItemIndex(it.title) }
            )
            .take(100)

        val mergedProfileRows = accumulator.profileRows
            .distinctBy { row ->
                val packedImages = row.imageUrls.joinToString("|")
                "${row.key.trim()}|${row.value.trim()}|${row.imageUrl.trim()}|$packedImages"
            }
            .take(180)

        val resolvedFirstImage = accumulator.firstImage
            .ifBlank { distinctGallery.firstOrNull { it.imageUrl.isNotBlank() }?.imageUrl.orEmpty() }
        val resolvedSummary = summary
            .ifBlank { accumulator.summaryCandidates.joinToString(" · ") }
            .ifBlank { "NPC及卫星图鉴条目数据较少，已展示可用信息。" }

        GuideDetailExtract(
            imageUrl = resolvedFirstImage,
            summary = resolvedSummary,
            stats = accumulator.stats.take(14),
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
            tabGalleryIconUrl = accumulator.tabGalleryIconUrl.ifBlank { resolvedFirstImage },
            tabSimulateIconUrl = ""
        )
    }.getOrDefault(GuideDetailExtract())
}
