package com.example.keios.ui.page.main.student.fetch.parser

import com.example.keios.ui.page.main.student.fetch.GuideBaseRow
import com.example.keios.ui.page.main.student.fetch.extractImageUrlsFromAny
import com.example.keios.ui.page.main.student.fetch.extractImageUrlsFromHtml
import com.example.keios.ui.page.main.student.fetch.extractVideoUrlsFromAny
import com.example.keios.ui.page.main.student.fetch.isPlaceholderMediaToken
import com.example.keios.ui.page.main.student.fetch.looksLikeImageUrl
import com.example.keios.ui.page.main.student.fetch.looksLikeVideoUrl
import com.example.keios.ui.page.main.student.fetch.normalizeImageUrl
import com.example.keios.ui.page.main.student.fetch.normalizeMediaUrl
import com.example.keios.ui.page.main.student.fetch.stripHtml
import org.json.JSONArray

internal data class GuideJsonBaseDataParseResult(
    val baseRows: List<GuideBaseRow>,
    val firstImage: String
)

internal fun tokenizeGuideBaseRows(
    baseData: JSONArray,
    sourceUrl: String
): GuideJsonBaseDataParseResult {
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

    return GuideJsonBaseDataParseResult(
        baseRows = baseRows,
        firstImage = firstImage
    )
}
