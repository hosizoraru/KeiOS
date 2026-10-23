package os.kei.ui.page.main.student.catalog.component

import android.content.Context
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import os.kei.R
import os.kei.ui.page.main.student.BA_GUIDE_BGM_MEDIA_METADATA_AUDIO_URL
import os.kei.ui.page.main.student.BA_GUIDE_BGM_MEDIA_METADATA_SOURCE_URL
import os.kei.ui.page.main.student.BA_GUIDE_BGM_MEDIA_METADATA_STUDENT_TITLE
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.normalizeGuideMediaSource

private const val BA_GUIDE_BGM_MEDIA_METADATA_ORIGINAL_TITLE = "os.kei.ba.bgm.original_title"
private const val BA_GUIDE_BGM_MEDIA_METADATA_ORIGINAL_STUDENT_TITLE =
    "os.kei.ba.bgm.original_student_title"
private const val BA_GUIDE_BGM_MEDIA_METADATA_IMAGE_URL = "os.kei.ba.bgm.image_url"
private const val BA_GUIDE_BGM_MEDIA_METADATA_STUDENT_IMAGE_URL = "os.kei.ba.bgm.student_image_url"
private const val BA_GUIDE_BGM_MEDIA_METADATA_NOTE = "os.kei.ba.bgm.note"

internal fun GuideBgmFavoriteItem.toBaGuideBgmMediaItem(
    context: Context,
    playbackUrlResolver: (Context, GuideBgmFavoriteItem) -> String = ::resolveFavoriteBgmPlaybackUrl,
    artworkUrlResolver: (GuideBgmFavoriteItem) -> String = { favorite ->
        favorite.resolvePlaybackArtworkImageUrl()
    },
    artworkData: ByteArray? = null
): MediaItem? {
    val mediaId = normalizeGuideMediaSource(audioUrl)
    if (mediaId.isBlank()) return null
    val playbackUrl = playbackUrlResolver(context, this)
    if (playbackUrl.isBlank()) return null
    val studentText = studentTitle.trim()
        .ifBlank { title.trim() }
        .ifBlank { context.getString(R.string.ba_catalog_bgm_student_unknown) }
    val subtitleText = context.getString(R.string.ba_catalog_bgm_media_subtitle)
    val normalizedSourceUrl = normalizeGuideMediaSource(this.sourceUrl)
    val metadataExtras = Bundle().apply {
        putString(BA_GUIDE_BGM_MEDIA_METADATA_AUDIO_URL, mediaId)
        putString(BA_GUIDE_BGM_MEDIA_METADATA_SOURCE_URL, normalizedSourceUrl)
        putString(BA_GUIDE_BGM_MEDIA_METADATA_STUDENT_TITLE, studentText)
        putString(BA_GUIDE_BGM_MEDIA_METADATA_ORIGINAL_TITLE, title)
        putString(BA_GUIDE_BGM_MEDIA_METADATA_ORIGINAL_STUDENT_TITLE, studentTitle)
        putString(BA_GUIDE_BGM_MEDIA_METADATA_IMAGE_URL, imageUrl)
        putString(BA_GUIDE_BGM_MEDIA_METADATA_STUDENT_IMAGE_URL, studentImageUrl)
        putString(BA_GUIDE_BGM_MEDIA_METADATA_NOTE, note)
    }
    val metadataBuilder = MediaMetadata.Builder()
        .setTitle(studentText)
        .setDisplayTitle(studentText)
        .setArtist(subtitleText)
        .setSubtitle(subtitleText)
        .setAlbumTitle(context.getString(R.string.ba_catalog_bgm_album_title))
        .setExtras(metadataExtras)
    if (artworkData != null) {
        metadataBuilder.setArtworkData(
            artworkData,
            MediaMetadata.PICTURE_TYPE_FRONT_COVER
        )
    }
    val artworkUri = artworkUrlResolver(this)
        .takeIf { it.isNotBlank() }
        ?.let { raw -> runCatching { raw.toUri() }.getOrNull() }
    return MediaItem.Builder()
        .setMediaId(mediaId)
        .setUri(playbackUrl)
        .setMediaMetadata(metadataBuilder.setArtworkUri(artworkUri).build())
        .build()
}

internal fun MediaItem.toBaGuideBgmFavoriteItem(): GuideBgmFavoriteItem? {
    val metadata = mediaMetadata
    val extras = metadata.extras
    // Playback URIs can point to a cache or a signed transport URL instead of the original audio.
    val audioUrl = normalizeGuideMediaSource(
        extras?.getString(BA_GUIDE_BGM_MEDIA_METADATA_AUDIO_URL).orEmpty()
    ).ifBlank { normalizeGuideMediaSource(mediaId) }
    if (audioUrl.isBlank()) return null

    val metadataTitle = metadata.title?.toString().orEmpty()
        .ifBlank { metadata.displayTitle?.toString().orEmpty() }
    val studentTitle = extras?.getString(BA_GUIDE_BGM_MEDIA_METADATA_ORIGINAL_STUDENT_TITLE)
        ?: extras?.getString(BA_GUIDE_BGM_MEDIA_METADATA_STUDENT_TITLE).orEmpty()
            .ifBlank { metadataTitle }
    val artworkUrl = metadata.artworkUri?.toString().orEmpty()

    // Present but empty originals must survive notification display and artwork fallbacks.
    return GuideBgmFavoriteItem(
        audioUrl = audioUrl,
        title = extras?.getString(BA_GUIDE_BGM_MEDIA_METADATA_ORIGINAL_TITLE)
            ?: metadataTitle.ifBlank { studentTitle },
        studentTitle = studentTitle,
        studentImageUrl = extras?.getString(BA_GUIDE_BGM_MEDIA_METADATA_STUDENT_IMAGE_URL)
            ?: artworkUrl,
        imageUrl = extras?.getString(BA_GUIDE_BGM_MEDIA_METADATA_IMAGE_URL) ?: artworkUrl,
        sourceUrl = normalizeGuideMediaSource(
            extras?.getString(BA_GUIDE_BGM_MEDIA_METADATA_SOURCE_URL).orEmpty()
        ),
        note = extras?.getString(BA_GUIDE_BGM_MEDIA_METADATA_NOTE).orEmpty(),
        favoritedAtMs = 0L
    )
}

internal fun List<GuideBgmFavoriteItem>.toBaGuideBgmMediaItems(
    context: Context,
    artworkDataByMediaId: Map<String, ByteArray> = emptyMap()
): List<MediaItem> {
    return filter { it.audioUrl.isNotBlank() }
        .distinctBy { normalizeGuideMediaSource(it.audioUrl) }
        .mapNotNull { favorite ->
            val mediaId = normalizeGuideMediaSource(favorite.audioUrl)
            favorite.toBaGuideBgmMediaItem(
                context = context,
                artworkData = artworkDataByMediaId[mediaId]
            )
        }
}
