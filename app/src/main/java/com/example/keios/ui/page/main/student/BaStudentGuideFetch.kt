package com.example.keios.ui.page.main.student

import android.net.Uri
import com.example.keios.feature.ba.data.remote.GameKeeFetchHelper
import org.json.JSONArray
import org.json.JSONObject

private fun fetchGuideInfoByApi(sourceUrl: String): BaStudentGuideInfo {
    val target = normalizeGuideUrl(sourceUrl)
    require(target.isNotBlank()) { "empty url" }
    val contentId = extractGuideContentIdFromUrl(target)
        ?: error("unable to resolve content_id")

    val refererPath = runCatching { Uri.parse(target).path.orEmpty() }
        .getOrDefault("/ba/tj/$contentId.html")
        .ifBlank { "/ba/tj/$contentId.html" }

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
        simulateRows = detail.simulateRows,
        voiceRows = detail.voiceRows,
        voiceCvJp = detail.voiceCvJp,
        voiceCvCn = detail.voiceCvCn,
        voiceCvByLanguage = detail.voiceCvByLanguage,
        voiceLanguageHeaders = detail.voiceLanguageHeaders,
        voiceEntries = detail.voiceEntries,
        tabSkillIconUrl = detail.tabSkillIconUrl,
        tabProfileIconUrl = detail.tabProfileIconUrl,
        tabVoiceIconUrl = detail.tabVoiceIconUrl,
        tabGalleryIconUrl = detail.tabGalleryIconUrl,
        tabSimulateIconUrl = detail.tabSimulateIconUrl,
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

fun fetchGuideInfo(sourceUrl: String): BaStudentGuideInfo {
    return runCatching { fetchGuideInfoByApi(sourceUrl) }
        .recoverCatching { fetchGuideInfoFromHtml(sourceUrl) }
        .getOrThrow()
}
