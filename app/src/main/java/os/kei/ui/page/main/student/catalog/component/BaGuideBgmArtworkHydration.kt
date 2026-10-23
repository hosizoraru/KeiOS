package os.kei.ui.page.main.student.catalog.component

import androidx.media3.common.MediaItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import os.kei.ui.page.main.student.GuideBgmFavoriteItem

internal fun MediaItem.withUpdatedBaGuideBgmMetadata(next: MediaItem): MediaItem {
    if (mediaId != next.mediaId) return this
    val metadata = next.mediaMetadata.buildUpon()
    if (mediaMetadata.artworkUri == next.mediaMetadata.artworkUri &&
        next.mediaMetadata.artworkData == null
    ) {
        metadata.setArtworkData(mediaMetadata.artworkData, mediaMetadata.artworkDataType)
    }
    // Keep the prepared audio source and position; only replace descriptive metadata.
    return buildUpon().setMediaMetadata(metadata.build()).build()
}

internal suspend fun hydrateBaGuideBgmArtwork(
    targets: List<GuideBgmFavoriteItem>,
    isCurrent: () -> Boolean,
    resolve: suspend (GuideBgmFavoriteItem) -> ByteArray?,
    publish: (GuideBgmFavoriteItem, ByteArray) -> Unit,
) {
    var pending = targets
    for (backoffMs in listOf(0L, 1_000L, 4_000L)) {
        if (pending.isEmpty() || !isCurrent()) return
        if (backoffMs > 0L) delay(backoffMs)
        val missing = mutableListOf<GuideBgmFavoriteItem>()
        for (favorite in pending) {
            if (!isCurrent()) return
            val data = try {
                resolve(favorite)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
            if (!isCurrent()) return
            if (data == null) missing += favorite else publish(favorite, data)
        }
        pending = missing
    }
}
