package com.example.keios.ui.page.main.student.fetch.parser

import com.example.keios.ui.page.main.student.BaGuideGalleryItem
import com.example.keios.ui.page.main.student.BaGuideRow
import com.example.keios.ui.page.main.student.fetch.GuideDetailExtract
import com.example.keios.ui.page.main.student.GuideTab
import com.example.keios.ui.page.main.student.fetch.deriveVoiceCvLegacyFields
import com.example.keios.ui.page.main.student.fetch.extractImageUrlsFromAny
import com.example.keios.ui.page.main.student.fetch.extractImageUrlsFromHtml
import com.example.keios.ui.page.main.student.fetch.extractVideoUrlsFromAny
import com.example.keios.ui.page.main.student.fetch.isAudioUrl
import com.example.keios.ui.page.main.student.fetch.isMeaningfulGuideRowValue
import com.example.keios.ui.page.main.student.fetch.isPlaceholderMediaToken
import com.example.keios.ui.page.main.student.fetch.isTopDataStatKey
import com.example.keios.ui.page.main.student.fetch.isVoiceBlockTailKey
import com.example.keios.ui.page.main.student.fetch.isVoiceCategoryKey
import com.example.keios.ui.page.main.student.fetch.isWeaponExtraAttributeKey
import com.example.keios.ui.page.main.student.fetch.looksLikeImageUrl
import com.example.keios.ui.page.main.student.fetch.looksLikeVideoUrl
import com.example.keios.ui.page.main.student.fetch.mergeVoiceLanguageHeaders
import com.example.keios.ui.page.main.student.fetch.normalizeGuideRowKey
import com.example.keios.ui.page.main.student.fetch.normalizeImageUrl
import com.example.keios.ui.page.main.student.fetch.normalizeMediaUrl
import com.example.keios.ui.page.main.student.fetch.normalizeVoiceEntriesWithHeaderCount
import com.example.keios.ui.page.main.student.fetch.parseGalleryItemsFromBaseData
import com.example.keios.ui.page.main.student.fetch.parseGalleryItemsFromStyleData
import com.example.keios.ui.page.main.student.fetch.parseGiftPreferenceRowsFromBaseData
import com.example.keios.ui.page.main.student.fetch.parseSimulateRowsFromBaseData
import com.example.keios.ui.page.main.student.fetch.parseVoiceCvByLanguageFromBaseData
import com.example.keios.ui.page.main.student.fetch.parseVoiceDataFromBaseData
import com.example.keios.ui.page.main.student.fetch.stripHtml
import org.json.JSONObject
import kotlin.collections.plusAssign

internal fun parseGuideDetailFromObjectContentJson(raw: String, sourceUrl: String): GuideDetailExtract {
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
        val (rawVoiceLanguageHeaders, rawVoiceEntries) = parseVoiceDataFromBaseData(
            baseData,
            sourceUrl
        )
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
        val tokenizedBaseData = tokenizeGuideBaseRows(baseData, sourceUrl)
        val baseRows = tokenizedBaseData.baseRows
        var firstImage = tokenizedBaseData.firstImage

        val memoryUnlockLevel = run {
            val rawLevel = baseRows.firstOrNull { it.key == "回忆大厅解锁等级" }
                ?.textValues
                ?.joinToString(" ")
                .orEmpty()
            val digits = Regex("""\d+""").find(rawLevel)?.value.orEmpty()
            if (digits.isNotBlank()) digits else rawLevel
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
                                note = noteForGalleryImage(
                                    row.textValues,
                                    index,
                                    row.imageValues.size
                                )
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
                        isAudioUrl(normalized) || looksLikeVideoUrl(normalized) || looksLikeImageUrl(
                            normalized
                        )
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
                compareBy<BaGuideGalleryItem> { guideGalleryCategoryOrder(it.title) }
                    .thenBy { guideGalleryTitleGroupKey(it.title) }
                    .thenBy { guideGalleryItemIndex(it.title) }
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
