package com.example.keios.ui.page.main.student.fetch.parser

import com.example.keios.ui.page.main.student.BaGuideGalleryItem
import com.example.keios.ui.page.main.student.BaGuideRow
import com.example.keios.ui.page.main.student.fetch.extractAudioUrlsFromAny
import com.example.keios.ui.page.main.student.fetch.extractImageUrlsFromAny
import com.example.keios.ui.page.main.student.fetch.extractVideoUrlsFromAny
import com.example.keios.ui.page.main.student.fetch.isMeaningfulGuideRowValue
import com.example.keios.ui.page.main.student.fetch.looksLikeImageUrl
import com.example.keios.ui.page.main.student.fetch.normalizeImageUrl

internal class ArrayGuideParseAccumulator(
    private val sourceUrl: String
) {
    val profileRows = mutableListOf<BaGuideRow>()
    val galleryItems = mutableListOf<BaGuideGalleryItem>()
    val stats = mutableListOf<Pair<String, String>>()
    val summaryCandidates = mutableListOf<String>()
    var firstImage: String = ""
    var tabGalleryIconUrl: String = ""

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

    fun pushGalleryItems(rawTitle: String, rawAny: Any?) {
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
}
