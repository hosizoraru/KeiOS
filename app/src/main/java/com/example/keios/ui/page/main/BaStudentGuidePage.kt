package com.example.keios.ui.page.main

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keios.ba.helper.GameKeeFetchHelper
import com.example.keios.ui.page.main.widget.FrostedBlock
import com.example.keios.ui.page.main.widget.FloatingBottomBar
import com.example.keios.ui.page.main.widget.FloatingBottomBarItem
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.ContactsBook
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.icon.extended.Mic
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Stopwatch
import top.yukonga.miuix.kmp.icon.extended.Tasks
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val BA_GUIDE_KV_ID = "ba_student_guide"
private const val BA_GUIDE_KEY_CURRENT_URL = "current_url"
private const val BA_GUIDE_KEY_CACHE_PREFIX = "cache_"

data class BaStudentGuideInfo(
    val sourceUrl: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val imageUrl: String,
    val summary: String,
    val stats: List<Pair<String, String>>,
    val skillRows: List<BaGuideRow> = emptyList(),
    val profileRows: List<BaGuideRow> = emptyList(),
    val galleryItems: List<BaGuideGalleryItem> = emptyList(),
    val growthRows: List<BaGuideRow> = emptyList(),
    val voiceRows: List<BaGuideRow> = emptyList(),
    val syncedAtMs: Long
)

data class BaGuideRow(
    val key: String,
    val value: String,
    val imageUrl: String = ""
)

data class BaGuideGalleryItem(
    val title: String,
    val imageUrl: String
)

private data class BaGuideMetaItem(
    val title: String,
    val value: String,
    val imageUrl: String
)

private enum class GuideTab(val label: String) {
    Skills("角色技能"),
    Profile("学生档案"),
    Voice("语音台词"),
    Gallery("影画鉴赏"),
    Simulate("养成模拟")
}

private enum class GuideBottomTab(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val guideTab: GuideTab? = null
) {
    Archive("档案", MiuixIcons.Regular.ContactsBook),
    Skills(GuideTab.Skills.label, MiuixIcons.Regular.Tasks, GuideTab.Skills),
    Profile(GuideTab.Profile.label, MiuixIcons.Regular.ContactsBook, GuideTab.Profile),
    Voice(GuideTab.Voice.label, MiuixIcons.Regular.Mic, GuideTab.Voice),
    Gallery(GuideTab.Gallery.label, MiuixIcons.Regular.Album, GuideTab.Gallery),
    Simulate(GuideTab.Simulate.label, MiuixIcons.Regular.Stopwatch, GuideTab.Simulate)
}

object BaStudentGuideStore {
    private fun kv(): MMKV = MMKV.mmkvWithID(BA_GUIDE_KV_ID)

    private fun encodeGuideRows(rows: List<BaGuideRow>): JSONArray {
        return JSONArray().apply {
            rows.forEach { row ->
                put(
                    JSONObject().apply {
                        put("k", row.key)
                        put("v", row.value)
                        put("img", row.imageUrl)
                    }
                )
            }
        }
    }

    private fun decodeGuideRows(obj: JSONObject, key: String): List<BaGuideRow> {
        val arr = obj.optJSONArray(key) ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val row = arr.optJSONObject(i) ?: continue
                val k = row.optString("k").trim()
                val v = row.optString("v").trim()
                val img = row.optString("img").trim()
                if (k.isBlank() && v.isBlank() && img.isBlank()) continue
                add(BaGuideRow(key = k, value = v, imageUrl = img))
            }
        }
    }

    private fun encodeGalleryItems(items: List<BaGuideGalleryItem>): JSONArray {
        return JSONArray().apply {
            items.forEach { item ->
                put(
                    JSONObject().apply {
                        put("t", item.title)
                        put("img", item.imageUrl)
                    }
                )
            }
        }
    }

    private fun decodeGalleryItems(obj: JSONObject, key: String): List<BaGuideGalleryItem> {
        val arr = obj.optJSONArray(key) ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val title = item.optString("t").trim()
                val imageUrl = item.optString("img").trim()
                if (imageUrl.isBlank()) continue
                add(BaGuideGalleryItem(title = title, imageUrl = imageUrl))
            }
        }
    }

    fun setCurrentUrl(url: String) {
        kv().encode(BA_GUIDE_KEY_CURRENT_URL, url.trim())
    }

    fun loadCurrentUrl(): String = kv().decodeString(BA_GUIDE_KEY_CURRENT_URL, "").orEmpty()

    private fun cacheKey(url: String): String {
        val id = url.trim().hashCode().toUInt().toString(16)
        return BA_GUIDE_KEY_CACHE_PREFIX + id
    }

    fun saveInfo(info: BaStudentGuideInfo) {
        val statsArr = JSONArray().apply {
            info.stats.forEach { (k, v) ->
                put(JSONObject().apply {
                    put("k", k)
                    put("v", v)
                })
            }
        }
        val raw = JSONObject().apply {
            put("sourceUrl", info.sourceUrl)
            put("title", info.title)
            put("subtitle", info.subtitle)
            put("description", info.description)
            put("imageUrl", info.imageUrl)
            put("summary", info.summary)
            put("syncedAtMs", info.syncedAtMs)
            put("stats", statsArr)
            put("skillRows", encodeGuideRows(info.skillRows))
            put("profileRows", encodeGuideRows(info.profileRows))
            put("galleryItems", encodeGalleryItems(info.galleryItems))
            put("growthRows", encodeGuideRows(info.growthRows))
            put("voiceRows", encodeGuideRows(info.voiceRows))
        }.toString()
        kv().encode(cacheKey(info.sourceUrl), raw)
    }

    fun loadInfo(url: String): BaStudentGuideInfo? {
        val source = url.trim()
        if (source.isBlank()) return null
        val raw = kv().decodeString(cacheKey(source), "").orEmpty()
        if (raw.isBlank()) return null
        return runCatching {
            val obj = JSONObject(raw)
            val stats = mutableListOf<Pair<String, String>>()
            val arr = obj.optJSONArray("stats") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val row = arr.optJSONObject(i) ?: continue
                val k = row.optString("k").trim()
                val v = row.optString("v").trim()
                if (k.isNotBlank() && v.isNotBlank()) stats += k to v
            }
            BaStudentGuideInfo(
                sourceUrl = obj.optString("sourceUrl").ifBlank { source },
                title = obj.optString("title"),
                subtitle = obj.optString("subtitle"),
                description = obj.optString("description"),
                imageUrl = obj.optString("imageUrl"),
                summary = obj.optString("summary"),
                stats = stats,
                skillRows = decodeGuideRows(obj, "skillRows"),
                profileRows = decodeGuideRows(obj, "profileRows"),
                galleryItems = decodeGalleryItems(obj, "galleryItems"),
                growthRows = decodeGuideRows(obj, "growthRows"),
                voiceRows = decodeGuideRows(obj, "voiceRows"),
                syncedAtMs = obj.optLong("syncedAtMs", 0L)
            )
        }.getOrNull()
    }
}

private fun normalizeGuideUrl(raw: String): String {
    val value = raw.trim()
    if (value.isBlank()) return ""
    return when {
        value.startsWith("http://") || value.startsWith("https://") -> value
        value.startsWith("//") -> "https:$value"
        value.startsWith("/") -> "https://www.gamekee.com$value"
        else -> "https://www.gamekee.com/$value"
    }
}

private fun decodeBasicHtmlEntity(raw: String): String {
    return raw
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
}

private fun stripHtml(raw: String): String {
    return decodeBasicHtmlEntity(raw.replace(Regex("<[^>]+>"), " "))
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun extractMeta(html: String, key: String): String {
    val escaped = Regex.escape(key)
    val regexes = listOf(
        Regex(
            """<meta[^>]+(?:property|name)\s*=\s*["']$escaped["'][^>]*content\s*=\s*["']([^"']+)["'][^>]*>""",
            setOf(RegexOption.IGNORE_CASE)
        ),
        Regex(
            """<meta[^>]*content\s*=\s*["']([^"']+)["'][^>]+(?:property|name)\s*=\s*["']$escaped["'][^>]*>""",
            setOf(RegexOption.IGNORE_CASE)
        )
    )
    regexes.forEach { regex ->
        val value = regex.find(html)?.groupValues?.getOrNull(1).orEmpty().trim()
        if (value.isNotBlank()) return decodeBasicHtmlEntity(value)
    }
    return ""
}

private fun normalizeImageUrl(sourceUrl: String, imageRaw: String): String {
    val img = imageRaw.trim()
    if (img.isBlank()) return ""
    if (img.startsWith("http://") || img.startsWith("https://")) return img
    if (img.startsWith("//")) return "https:$img"
    return if (img.startsWith("/")) {
        val source = runCatching { Uri.parse(sourceUrl) }.getOrNull()
        val host = source?.scheme?.plus("://")?.plus(source.host.orEmpty()).orEmpty()
        (host.ifBlank { "https://www.gamekee.com" }) + img
    } else {
        "https://www.gamekee.com/$img"
    }
}

private fun extractStatsFromHtml(html: String): List<Pair<String, String>> {
    val rows = mutableListOf<Pair<String, String>>()
    val trRegex = Regex(
        """<tr[^>]*>\s*<th[^>]*>(.*?)</th>\s*<td[^>]*>(.*?)</td>\s*</tr>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    trRegex.findAll(html).forEach { m ->
        val key = stripHtml(m.groupValues[1])
        val value = stripHtml(m.groupValues[2])
        if (key.isNotBlank() && value.isNotBlank()) rows += key to value
    }
    if (rows.isNotEmpty()) return rows.take(8)

    val dlRegex = Regex(
        """<dt[^>]*>(.*?)</dt>\s*<dd[^>]*>(.*?)</dd>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    dlRegex.findAll(html).forEach { m ->
        val key = stripHtml(m.groupValues[1])
        val value = stripHtml(m.groupValues[2])
        if (key.isNotBlank() && value.isNotBlank()) rows += key to value
    }
    return rows.take(8)
}

private data class GuideDetailExtract(
    val imageUrl: String = "",
    val summary: String = "",
    val stats: List<Pair<String, String>> = emptyList(),
    val skillRows: List<BaGuideRow> = emptyList(),
    val profileRows: List<BaGuideRow> = emptyList(),
    val galleryItems: List<BaGuideGalleryItem> = emptyList(),
    val growthRows: List<BaGuideRow> = emptyList(),
    val voiceRows: List<BaGuideRow> = emptyList()
)

private data class GuideBaseRow(
    val key: String,
    val textValues: List<String>,
    val imageValues: List<String>
)

private fun looksLikeImageUrl(raw: String): Boolean {
    val value = raw.lowercase()
    if (value.isBlank()) return false
    return value.startsWith("http://") ||
        value.startsWith("https://") ||
        value.startsWith("//") ||
        value.endsWith(".png") ||
        value.endsWith(".jpg") ||
        value.endsWith(".jpeg") ||
        value.endsWith(".webp") ||
        value.contains("cdnimg")
}

private fun firstImageFromAny(any: Any?, sourceUrl: String, depth: Int = 0): String {
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

private fun parseGuideDetailFromContentJson(raw: String, sourceUrl: String): GuideDetailExtract {
    if (raw.isBlank()) return GuideDetailExtract()
    return runCatching {
        val root = JSONObject(raw)
        val baseData = root.optJSONArray("baseData") ?: return@runCatching GuideDetailExtract()
        val baseRows = mutableListOf<GuideBaseRow>()
        var firstImage = ""

        for (i in 0 until baseData.length()) {
            val row = baseData.optJSONArray(i) ?: continue
            if (row.length() == 0) continue
            val key = stripHtml((row.optJSONObject(0)?.optString("value") ?: "").trim())
            val textValues = mutableListOf<String>()
            val imageValues = mutableListOf<String>()
            for (j in 1 until row.length()) {
                val cell = row.optJSONObject(j) ?: continue
                val type = cell.optString("type").trim().lowercase()
                val rawValue = cell.optString("value").trim()
                if (rawValue.isBlank()) continue
                if (type == "image") {
                    val normalized = normalizeImageUrl(sourceUrl, rawValue)
                    if (normalized.isNotBlank()) imageValues += normalized
                } else {
                    val normalized = stripHtml(rawValue)
                    if (normalized.isNotBlank()) textValues += normalized
                }
            }
            if (firstImage.isBlank() && imageValues.isNotEmpty()) {
                firstImage = imageValues.first()
            }
            baseRows += GuideBaseRow(
                key = key,
                textValues = textValues,
                imageValues = imageValues
            )
        }

        fun containsAny(target: String, keywords: List<String>): Boolean {
            return keywords.any { key -> target.contains(key, ignoreCase = true) }
        }

        val skillKeywords = listOf(
            "技能", "EX", "普通技能", "被动技能", "辅助技能", "固有", "技能COST", "技能图标", "技能描述", "技能名称",
            "技能类型", "技能名词"
        )
        val profileKeywords = listOf(
            "学生信息", "角色名称", "全名", "假名注音", "简中译名", "繁中译名", "稀有度", "战术作用", "所属学园",
            "所属社团", "实装日期", "攻击类型", "防御类型", "位置", "市街", "屋外", "屋内", "武器类型", "年龄", "生日",
            "兴趣爱好", "声优", "画师", "介绍", "个人简介", "MomoTalk", "回忆大厅解锁等级", "同名角色", "角色头像"
        )
        val galleryKeywords = listOf(
            "立绘", "本家画", "TV动画设定图", "回忆大厅文件", "回忆大厅视频", "回忆大厅", "PV", "Live", "巧克力图",
            "互动家具"
        )
        val growthKeywords = listOf(
            "装备", "专武", "能力解放", "礼物偏好", "羁绊", "升级材料", "所需", "LV", "T1", "T2", "爱用品", "初始数据",
            "攻击力增加", "生命值增加", "暴击值增加", "暴击伤害增加", "装弹数", "命中值", "治愈力", "稳定值"
        )
        val voiceKeywords = listOf("战斗", "活动", "大厅及咖啡馆", "事件", "好感度")

        val skillRows = mutableListOf<BaGuideRow>()
        val profileRows = mutableListOf<BaGuideRow>()
        val growthRows = mutableListOf<BaGuideRow>()
        val voiceRows = mutableListOf<BaGuideRow>()
        val galleryItems = mutableListOf<BaGuideGalleryItem>()
        val stats = mutableListOf<Pair<String, String>>()
        val summaryCandidates = mutableListOf<String>()

        baseRows.forEach { row ->
            val key = row.key
            val value = row.textValues.joinToString(" / ")
            val imageUrl = row.imageValues.firstOrNull().orEmpty()
            if (key.isBlank() && value.isBlank() && imageUrl.isBlank()) return@forEach

            val guideRow = BaGuideRow(
                key = key.ifBlank { "信息" },
                value = value,
                imageUrl = imageUrl
            )
            val normalizedKey = key.ifBlank { value }
            val isVoice = containsAny(normalizedKey, voiceKeywords)
            val isSkill = containsAny(normalizedKey, skillKeywords)
            val isGrowth = containsAny(normalizedKey, growthKeywords)
            val isGallery = containsAny(normalizedKey, galleryKeywords)
            val isProfile = containsAny(normalizedKey, profileKeywords)

            when {
                isVoice -> voiceRows += guideRow
                isSkill -> skillRows += guideRow
                isGrowth -> growthRows += guideRow
                isGallery -> {
                    if (imageUrl.isNotBlank()) {
                        galleryItems += BaGuideGalleryItem(
                            title = guideRow.key.ifBlank { "影画" },
                            imageUrl = imageUrl
                        )
                    } else if (guideRow.value.isNotBlank()) {
                        profileRows += guideRow
                    }
                }
                isProfile -> profileRows += guideRow
                else -> {
                    if (guideRow.key.isNotBlank() && guideRow.value.isNotBlank()) {
                        profileRows += guideRow
                    }
                }
            }

            if (imageUrl.isNotBlank() && (isGallery || guideRow.key.contains("立绘") || guideRow.key.contains("本家画"))) {
                galleryItems += BaGuideGalleryItem(
                    title = guideRow.key.ifBlank { "影画" },
                    imageUrl = imageUrl
                )
            }
            if (guideRow.key.isNotBlank() && guideRow.value.isNotBlank() && stats.none { it.first == guideRow.key }) {
                stats += guideRow.key to guideRow.value
                if (summaryCandidates.size < 4) {
                    summaryCandidates += "${guideRow.key}：${guideRow.value}"
                }
            }
        }

        val distinctGallery = galleryItems
            .distinctBy { it.imageUrl }
            .take(24)

        GuideDetailExtract(
            imageUrl = firstImage,
            summary = summaryCandidates.joinToString(" · "),
            stats = stats.take(14),
            skillRows = skillRows.take(120),
            profileRows = profileRows.take(120),
            galleryItems = distinctGallery,
            growthRows = growthRows.take(160),
            voiceRows = voiceRows.take(160)
        )
    }.getOrDefault(GuideDetailExtract())
}

private fun extractContentIdFromGuideUrl(sourceUrl: String): Long? {
    val target = normalizeGuideUrl(sourceUrl)
    if (target.isBlank()) return null
    val patterns = listOf(
        Regex("/v1/content/detail/(\\d+)"),
        Regex("/tj/(\\d+)\\.html"),
        Regex("/ba/(\\d+)\\.html"),
        Regex("/(\\d+)\\.html")
    )
    patterns.forEach { regex ->
        val id = regex.find(target)?.groupValues?.getOrNull(1)?.toLongOrNull()
        if (id != null && id > 0L) return id
    }
    val uri = runCatching { Uri.parse(target) }.getOrNull() ?: return null
    val qpKeys = listOf("content_id", "id", "cid")
    qpKeys.forEach { key ->
        val id = uri.getQueryParameter(key)?.toLongOrNull()
        if (id != null && id > 0L) return id
    }
    uri.pathSegments
        .asReversed()
        .firstNotNullOfOrNull { segment -> segment.toLongOrNull() }
        ?.let { id ->
            if (id > 0L) return id
        }
    return null
}

private fun fetchGuideInfoByApi(sourceUrl: String): BaStudentGuideInfo {
    val target = normalizeGuideUrl(sourceUrl)
    require(target.isNotBlank()) { "empty url" }
    val contentId = extractContentIdFromGuideUrl(target)
        ?: error("unable to resolve content_id")

    val refererPath = runCatching { Uri.parse(target).path.orEmpty() }
        .getOrDefault("/ba/$contentId.html")
        .ifBlank { "/ba/$contentId.html" }

    val body = GameKeeFetchHelper.fetchJson(
        pathOrUrl = "/v1/content/detail/$contentId",
        refererPath = refererPath,
        extraHeaders = mapOf(
            "device-num" to "1",
            "game-alias" to "ba"
        )
    )
    val root = JSONObject(body)
    if (root.optInt("code", -1) != 0) {
        error("api code=${root.optInt("code", -1)}")
    }
    val data = root.optJSONObject("data") ?: error("empty data")
    val title = data.optString("title").trim().ifBlank { "图鉴信息" }
    val subtitle = data.optJSONObject("game")?.optString("name").orEmpty().ifBlank { "GameKee" }
    val summaryFromApi = stripHtml(data.optString("summary"))
    val detail = parseGuideDetailFromContentJson(data.optString("content_json"), target)
    val imageFromData = firstImageFromAny(
        any = JSONObject().apply {
            put("thumb", data.opt("thumb"))
            put("image_list", data.opt("image_list"))
            put("thumb_list", data.opt("thumb_list"))
            put("video_list", data.opt("video_list"))
        },
        sourceUrl = target
    )
    val imageUrl = detail.imageUrl.ifBlank { imageFromData }
    val stats = detail.stats
    val summary = detail.summary
        .ifBlank { summaryFromApi }
        .ifBlank {
            stats.take(4).joinToString(" · ") { "${it.first}：${it.second}" }
        }
        .ifBlank { "暂无更多参数，可点击来源查看完整图鉴。" }
    val description = summaryFromApi.ifBlank { summary }

    return BaStudentGuideInfo(
        sourceUrl = target,
        title = title,
        subtitle = subtitle,
        description = description,
        imageUrl = imageUrl,
        summary = summary,
        stats = stats,
        skillRows = detail.skillRows,
        profileRows = detail.profileRows,
        galleryItems = detail.galleryItems,
        growthRows = detail.growthRows,
        voiceRows = detail.voiceRows,
        syncedAtMs = System.currentTimeMillis()
    )
}

private fun fetchGuideInfoFromHtml(sourceUrl: String): BaStudentGuideInfo {
    val target = normalizeGuideUrl(sourceUrl)
    require(target.isNotBlank()) { "empty url" }
    val html = GameKeeFetchHelper.fetchHtml(
        pathOrUrl = target,
        refererPath = "/ba/"
    )
    if (html.isBlank()) error("empty html")

    val ogTitle = extractMeta(html, "og:title")
    val titleTag = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        .find(html)
        ?.groupValues
        ?.getOrNull(1)
        .orEmpty()
    val rawTitle = if (ogTitle.isNotBlank()) ogTitle else stripHtml(titleTag)
    val title = rawTitle.ifBlank { "图鉴信息" }

    val siteName = extractMeta(html, "og:site_name").ifBlank { "GameKee" }
    val description = extractMeta(html, "og:description")
        .ifBlank { extractMeta(html, "description") }
        .ifBlank { "未解析到详细描述，可点击下方来源查看完整图鉴。" }
    val imageUrl = normalizeImageUrl(target, extractMeta(html, "og:image"))

    val paragraphRegex = Regex(
        "<p[^>]*>(.*?)</p>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    val summary = paragraphRegex.findAll(html)
        .map { stripHtml(it.groupValues[1]) }
        .firstOrNull { it.length >= 12 }
        ?: description

    val stats = extractStatsFromHtml(html)

    return BaStudentGuideInfo(
        sourceUrl = target,
        title = title,
        subtitle = siteName,
        description = description,
        imageUrl = imageUrl,
        summary = summary,
        stats = stats,
        profileRows = stats.map { (k, v) -> BaGuideRow(k, v) },
        syncedAtMs = System.currentTimeMillis()
    )
}

private fun fetchGuideInfo(sourceUrl: String): BaStudentGuideInfo {
    return runCatching { fetchGuideInfoByApi(sourceUrl) }
        .recoverCatching { fetchGuideInfoFromHtml(sourceUrl) }
        .getOrThrow()
}

@Composable
private fun GuideRemoteImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    imageHeight: androidx.compose.ui.unit.Dp = 220.dp
) {
    val target = remember(imageUrl) { normalizeGuideUrl(imageUrl) }
    if (target.isBlank()) return
    val bitmap by produceState<Bitmap?>(initialValue = null, target) {
        value = withContext(Dispatchers.IO) {
            runCatching { GameKeeFetchHelper.fetchImage(target) }.getOrNull()
        }
    }
    val rendered = bitmap ?: return
    Image(
        bitmap = rendered.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .height(imageHeight)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
    )
}

@Composable
private fun GuideRemoteIcon(
    imageUrl: String,
    modifier: Modifier = Modifier,
    iconWidth: androidx.compose.ui.unit.Dp = 20.dp,
    iconHeight: androidx.compose.ui.unit.Dp = iconWidth
) {
    val target = remember(imageUrl) { normalizeGuideUrl(imageUrl) }
    if (target.isBlank()) return
    val bitmap by produceState<Bitmap?>(initialValue = null, target) {
        value = withContext(Dispatchers.IO) {
            runCatching { GameKeeFetchHelper.fetchImage(target) }.getOrNull()
        }
    }
    val rendered = bitmap ?: return
    Image(
        bitmap = rendered.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .width(iconWidth)
            .height(iconHeight)
    )
}

@Composable
private fun GuideRowsSection(
    rows: List<BaGuideRow>,
    emptyText: String,
    imageHeight: androidx.compose.ui.unit.Dp = 96.dp
) {
    if (rows.isEmpty()) {
        Text(emptyText, color = MiuixTheme.colorScheme.onBackgroundVariant)
        return
    }
    val visibleRows = rows.take(120)
    visibleRows.forEachIndexed { index, row ->
        val key = row.key.ifBlank { "信息" }
        val hasImage = row.imageUrl.isNotBlank()
        val value = row.value
            .takeIf { it.isNotBlank() && it != "图片" }
            ?: if (hasImage) "见下图" else "-"
        MiuixInfoItem(key, value)
        if (hasImage) {
            Spacer(modifier = Modifier.height(6.dp))
            GuideRemoteImage(
                imageUrl = row.imageUrl,
                imageHeight = imageHeight
            )
        }
        if (index < visibleRows.lastIndex) {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun GuideGallerySection(
    items: List<BaGuideGalleryItem>,
    emptyText: String
) {
    if (items.isEmpty()) {
        Text(emptyText, color = MiuixTheme.colorScheme.onBackgroundVariant)
        return
    }
    val visibleItems = items.distinctBy { it.imageUrl }.take(24)
    visibleItems.forEachIndexed { index, item ->
        if (item.title.isNotBlank()) {
            Text(item.title, color = MiuixTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(6.dp))
        }
        GuideRemoteImage(
            imageUrl = item.imageUrl,
            imageHeight = 220.dp
        )
        if (index < visibleItems.lastIndex) {
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun GuideProfileMetaLine(item: BaGuideMetaItem) {
    val isPosition = item.title == "位置"
    val summary = if (isPosition) "" else item.value.ifBlank { "-" }
    val titleSlotWidth = 70.dp
    val iconSlotWidth = 34.dp
    val iconSlotHeight = 24.dp
    val iconWidth = if (isPosition) 30.dp else 20.dp
    val iconHeight = 20.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.title,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.width(titleSlotWidth),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .width(iconSlotWidth)
                .height(iconSlotHeight),
            contentAlignment = Alignment.Center
        ) {
            if (item.imageUrl.isNotBlank()) {
                GuideRemoteIcon(
                    imageUrl = item.imageUrl,
                    iconWidth = iconWidth,
                    iconHeight = iconHeight
                )
            }
        }
        if (!isPosition) {
            Text(
                text = summary,
                color = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Spacer(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun GuideCombatMetaTile(
    item: BaGuideMetaItem,
    modifier: Modifier = Modifier
) {
    val value = item.value.ifBlank { "-" }
    val adaptiveWide = item.title == "战术作用" || item.title == "武器类型"
    val iconWidth = if (adaptiveWide) 28.dp else 18.dp
    val iconHeight = if (adaptiveWide) 18.dp else 18.dp
    val iconSlotWidth = 30.dp
    val iconSlotHeight = 22.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.36f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(iconSlotWidth)
                .height(iconSlotHeight),
            contentAlignment = Alignment.Center
        ) {
            if (item.imageUrl.isNotBlank()) {
                GuideRemoteIcon(
                    imageUrl = item.imageUrl,
                    iconWidth = iconWidth,
                    iconHeight = iconHeight
                )
            }
        }
        Text(
            text = item.title,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.width(74.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = MiuixTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BaStudentGuidePage(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val accent = MiuixTheme.colorScheme.primary
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop: LayerBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val scrollBehavior = MiuixScrollBehavior()

    val sourceUrl = remember { BaStudentGuideStore.loadCurrentUrl() }
    var info by remember(sourceUrl) { mutableStateOf(BaStudentGuideStore.loadInfo(sourceUrl)) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshSignal by remember { mutableStateOf(0) }
    var selectedBottomTabIndex by rememberSaveable(sourceUrl) { mutableIntStateOf(0) }
    val bottomTabs = GuideBottomTab.entries
    val activeBottomTab = bottomTabs.getOrElse(selectedBottomTabIndex) { GuideBottomTab.Archive }
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val pageTitle = info?.title?.ifBlank { "学生图鉴" } ?: "学生图鉴"

    fun openExternal(url: String) {
        val target = normalizeGuideUrl(url)
        if (target.isBlank()) return
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(sourceUrl, refreshSignal) {
        if (sourceUrl.isBlank()) return@LaunchedEffect
        val cached = BaStudentGuideStore.loadInfo(sourceUrl)
        if (cached != null) info = cached
        loading = true
        val result = withContext(Dispatchers.IO) {
            runCatching { fetchGuideInfo(sourceUrl) }
        }
        result.onSuccess { latest ->
            info = latest
            error = null
            withContext(Dispatchers.IO) { BaStudentGuideStore.saveInfo(latest) }
        }.onFailure {
            error = if (info != null) "网络请求失败，已显示本地缓存" else "图鉴信息加载失败"
        }
        loading = false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = pageTitle,
                scrollBehavior = scrollBehavior,
                color = MiuixTheme.colorScheme.surface,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Back,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            refreshSignal += 1
                        }
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Refresh,
                            contentDescription = "刷新",
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
                FloatingBottomBar(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .padding(
                            horizontal = 12.dp,
                            vertical = 12.dp + navigationBarBottom
                        ),
                    selectedIndex = { selectedBottomTabIndex },
                    onSelected = { index -> selectedBottomTabIndex = index },
                    backdrop = backdrop,
                    tabsCount = bottomTabs.size,
                    isBlurEnabled = true
                ) {
                    bottomTabs.forEachIndexed { index, tab ->
                        val dynamicIconUrl = info?.bottomTabIconUrl(tab).orEmpty()
                        FloatingBottomBarItem(
                            onClick = { selectedBottomTabIndex = index },
                            modifier = Modifier.defaultMinSize(minWidth = 72.dp)
                        ) {
                            if (dynamicIconUrl.isNotBlank()) {
                                GuideRemoteIcon(
                                    imageUrl = dynamicIconUrl,
                                    iconWidth = 18.dp,
                                    iconHeight = 18.dp
                                )
                            } else {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    tint = MiuixTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .graphicsLayer {
                                            scaleX = 1f
                                            scaleY = 1f
                                        }
                                )
                            }
                            Text(
                                text = tab.label,
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                color = MiuixTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Visible
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp
            )
        ) {
            item { SmallTitle(activeBottomTab.label) }
            item { Spacer(modifier = Modifier.height(14.dp)) }

            if (sourceUrl.isBlank()) {
                item {
                    FrostedBlock(
                        backdrop = backdrop,
                        title = "未选择学生",
                        subtitle = "请从 BA 卡池信息中点击对应卡池进入",
                        accent = accent
                    )
                }
            } else {
                when (activeBottomTab) {
                    GuideBottomTab.Archive -> {
                        item {
                            val guide = info
                            val profileItems = guide?.buildProfileMetaItems().orEmpty()
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.defaultColors(
                                    color = Color(0x223B82F6),
                                    contentColor = MiuixTheme.colorScheme.onBackground
                                ),
                                onClick = {}
                            ) {
                                if (showLoadingText(loading = loading, hasInfo = guide != null)) {
                                    Text(
                                        text = "同步中...",
                                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                    )
                                }
                                if (guide != null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(modifier = Modifier.width(112.dp)) {
                                                if (guide.imageUrl.isNotBlank()) {
                                                    GuideRemoteImage(
                                                        imageUrl = guide.imageUrl,
                                                        imageHeight = 152.dp
                                                    )
                                                } else {
                                                    Text(
                                                        text = "暂无图片",
                                                        color = MiuixTheme.colorScheme.onBackgroundVariant
                                                    )
                                                }
                                            }
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(152.dp),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                profileItems.forEach { item ->
                                                    GuideProfileMetaLine(item)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(10.dp)) }
                        item {
                            val guide = info
                            val combatItems = guide?.buildCombatMetaItems().orEmpty()
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.defaultColors(
                                    color = Color(0x223B82F6),
                                    contentColor = MiuixTheme.colorScheme.onBackground
                                ),
                                onClick = {}
                            ) {
                                if (showLoadingText(loading = loading, hasInfo = guide != null)) {
                                    Text(
                                        text = "同步中...",
                                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                    )
                                }
                                if (guide != null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        combatItems.forEachIndexed { index, item ->
                                            GuideCombatMetaTile(
                                                item = item,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            if (index < combatItems.lastIndex) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        item {
                            FrostedBlock(
                                backdrop = backdrop,
                                title = activeBottomTab.label,
                                subtitle = info?.subtitle?.ifBlank { "GameKee" } ?: "GameKee",
                                accent = accent,
                                content = {
                                    if (showLoadingText(loading = loading, hasInfo = info != null)) {
                                        Text("同步中...", color = MiuixTheme.colorScheme.onBackgroundVariant)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    error?.takeIf { it.isNotBlank() }?.let {
                                        Text(
                                            text = it,
                                            color = MiuixTheme.colorScheme.error,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    val guide = info
                                    if (guide != null) {
                                        val skillRows = guide.skillRowsForDisplay()
                                        val profileRows = guide.profileRowsForDisplay()
                                        val visibleSkillRows = skillRows.filterNot { shouldHideMovedHeaderRow(it) }
                                        val visibleProfileRows = profileRows.filterNot { shouldHideMovedHeaderRow(it) }
                                        val growthRows = guide.growthRowsForDisplay()
                                        val galleryItems = if (guide.galleryItems.isNotEmpty()) {
                                            guide.galleryItems
                                        } else {
                                            listOfNotNull(
                                                guide.imageUrl
                                                    .takeIf { it.isNotBlank() }
                                                    ?.let { BaGuideGalleryItem("立绘", it) }
                                            )
                                        }
                                        val activeGuideTab = activeBottomTab.guideTab

                                        Text(
                                            text = guide.summary.ifBlank { guide.description },
                                            color = MiuixTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))

                                        when (activeGuideTab) {
                                            GuideTab.Skills -> {
                                                GuideRowsSection(
                                                    rows = visibleSkillRows,
                                                    emptyText = "暂未解析到角色技能数据。"
                                                )
                                            }

                                            GuideTab.Profile -> {
                                                GuideRowsSection(
                                                    rows = visibleProfileRows,
                                                    emptyText = "暂未解析到学生档案数据。"
                                                )
                                            }

                                            GuideTab.Voice -> {
                                                GuideRowsSection(
                                                    rows = guide.voiceRows,
                                                    emptyText = "语音台词解析中，当前版本先完善其他栏目。"
                                                )
                                            }

                                            GuideTab.Gallery -> {
                                                GuideGallerySection(
                                                    items = galleryItems,
                                                    emptyText = "暂未解析到影画鉴赏内容。"
                                                )
                                            }

                                            GuideTab.Simulate -> {
                                                GuideRowsSection(
                                                    rows = growthRows,
                                                    emptyText = "暂未解析到养成模拟数据。"
                                                )
                                            }

                                            else -> {}
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        MiuixInfoItem(
                                            "来源",
                                            guide.sourceUrl,
                                            onClick = { openExternal(guide.sourceUrl) }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun showLoadingText(loading: Boolean, hasInfo: Boolean): Boolean {
    return loading && hasInfo
}

private fun BaStudentGuideInfo.bottomTabIconUrl(tab: GuideBottomTab): String {
    return when (tab) {
        GuideBottomTab.Archive -> ""
        GuideBottomTab.Skills -> skillRowsForDisplay()
            .firstOrNull { row ->
                row.imageUrl.isNotBlank() &&
                    (row.key.contains("技能", ignoreCase = true) || row.key.contains("EX", ignoreCase = true))
            }
            ?.imageUrl
            .orEmpty()

        GuideBottomTab.Profile -> profileRowsForDisplay()
            .firstOrNull { row ->
                row.imageUrl.isNotBlank() &&
                    (row.key.contains("学园", ignoreCase = true) ||
                        row.key.contains("学院", ignoreCase = true) ||
                        row.key.contains("稀有度", ignoreCase = true) ||
                        row.key.contains("头像", ignoreCase = true))
            }
            ?.imageUrl
            .orEmpty()

        GuideBottomTab.Voice -> voiceRows.firstOrNull { it.imageUrl.isNotBlank() }?.imageUrl.orEmpty()
        GuideBottomTab.Gallery -> galleryItems.firstOrNull()?.imageUrl.orEmpty()
        GuideBottomTab.Simulate -> growthRowsForDisplay()
            .firstOrNull { row ->
                row.imageUrl.isNotBlank() &&
                    !row.key.contains("材料", ignoreCase = true)
            }
            ?.imageUrl
            .orEmpty()
    }
}

private fun BaStudentGuideInfo.skillRowsForDisplay(): List<BaGuideRow> {
    if (skillRows.isNotEmpty()) return skillRows
    return stats
        .filter { (k, v) ->
            k.contains("技能") ||
                k.contains("EX", ignoreCase = true) ||
                v.contains("技能")
        }
        .map { (k, v) -> BaGuideRow(k, v) }
}

private fun BaStudentGuideInfo.profileRowsForDisplay(): List<BaGuideRow> {
    if (profileRows.isNotEmpty()) return profileRows
    return stats.map { (k, v) -> BaGuideRow(k, v) }
}

private fun BaStudentGuideInfo.growthRowsForDisplay(): List<BaGuideRow> {
    if (growthRows.isNotEmpty()) return growthRows
    return stats
        .filter { (k, v) ->
            listOf("装备", "专武", "羁绊", "等级", "升级", "材料", "LV").any {
                k.contains(it, ignoreCase = true) || v.contains(it, ignoreCase = true)
            }
        }
        .map { (k, v) -> BaGuideRow(k, v) }
}

private fun BaStudentGuideInfo.findFirstRowByKeywords(
    rows: List<BaGuideRow>,
    keywords: List<String>,
    requireImage: Boolean = false
): BaGuideRow? {
    return rows.firstOrNull { row ->
        val key = row.key
        val value = row.value
        val hasKeyword = keywords.any {
            key.contains(it, ignoreCase = true) || value.contains(it, ignoreCase = true)
        }
        hasKeyword && (!requireImage || row.imageUrl.isNotBlank())
    }
}

private fun BaStudentGuideInfo.buildMetaItem(
    title: String,
    valueKeywords: List<String>,
    imageKeywords: List<String> = valueKeywords
): BaGuideMetaItem {
    val rows = profileRowsForDisplay() + skillRowsForDisplay()
    val iconRow = findFirstRowByKeywords(rows, imageKeywords, requireImage = true)
    val value = findGuideFieldValue(*valueKeywords.toTypedArray()).ifBlank { "-" }
    return BaGuideMetaItem(
        title = title,
        value = value,
        imageUrl = iconRow?.imageUrl.orEmpty()
    )
}

private fun BaStudentGuideInfo.buildProfileMetaItems(): List<BaGuideMetaItem> {
    return listOf(
        buildMetaItem("稀有度", listOf("稀有度", "星级")),
        buildMetaItem("学院", listOf("所属学园", "所属学院", "学园")),
        buildMetaItem("所属社团", listOf("所属社团", "社团")),
        buildMetaItem("位置", listOf("位置"))
    )
}

private fun BaStudentGuideInfo.buildCombatMetaItems(): List<BaGuideMetaItem> {
    return listOf(
        buildMetaItem("战术作用", listOf("战术作用", "作用")),
        buildMetaItem("攻击类型", listOf("攻击类型")),
        buildMetaItem("防御类型", listOf("防御类型")),
        buildMetaItem("武器类型", listOf("武器类型")),
        buildMetaItem("市街", listOf("市街")),
        buildMetaItem("屋外", listOf("屋外")),
        buildMetaItem("室内", listOf("屋内", "室内"))
    )
}

private fun shouldHideMovedHeaderRow(row: BaGuideRow): Boolean {
    val key = row.key
    val movedKeywords = listOf(
        "头像", "角色头像",
        "稀有度", "星级", "所属学园", "所属学院", "所属社团",
        "战术作用", "攻击类型", "防御类型", "位置", "武器类型",
        "市街", "屋外", "屋内", "室内"
    )
    if (movedKeywords.any { key.contains(it, ignoreCase = true) }) return true
    if (key.trim().equals("作用", ignoreCase = true)) return true
    return false
}

private fun BaStudentGuideInfo.findGuideFieldValue(vararg keywords: String): String {
    val normalizedKeywords = keywords.map { it.trim() }.filter { it.isNotBlank() }
    if (normalizedKeywords.isEmpty()) return "-"

    fun keyMatched(key: String): Boolean {
        return normalizedKeywords.any { key.contains(it, ignoreCase = true) }
    }

    profileRows.firstOrNull { row ->
        keyMatched(row.key) && row.value.isNotBlank()
    }?.let { return it.value }

    stats.firstOrNull { (key, value) ->
        keyMatched(key) && value.isNotBlank()
    }?.let { return it.second }

    return "-"
}
