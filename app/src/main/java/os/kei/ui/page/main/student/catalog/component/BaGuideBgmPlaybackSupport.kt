package os.kei.ui.page.main.student.catalog.component

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import os.kei.ui.page.main.student.BaGuideTempMediaCache
import os.kei.ui.page.main.student.GUIDE_BGM_FAVORITE_AUDIO_SCOPE_KEY
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.GuideBgmFavoritePlaybackStore
import os.kei.ui.page.main.student.GuideBgmPlayerStore
import os.kei.ui.page.main.student.normalizeGuideMediaSource

internal data class BaGuideBgmPlaybackRuntimeState(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isEnded: Boolean = false,
    val volume: Float = 1f
) {
    val progress: Float
        get() {
            if (durationMs <= 0L) return 0f
            return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        }
}

internal fun favoriteCacheScope(favorite: GuideBgmFavoriteItem): String {
    return favorite.sourceUrl.ifBlank { GUIDE_BGM_FAVORITE_AUDIO_SCOPE_KEY }
}

internal fun resolveFavoriteBgmMediaUrl(
    context: Context,
    favorite: GuideBgmFavoriteItem,
    rawUrl: String
): String {
    return BaGuideTempMediaCache.resolveCachedUrl(
        context = context,
        sourceUrl = favoriteCacheScope(favorite),
        rawUrl = rawUrl
    )
}

internal fun isFavoriteBgmCached(
    context: Context,
    favorite: GuideBgmFavoriteItem
): Boolean {
    val raw = normalizeGuideMediaSource(favorite.audioUrl)
    if (raw.isBlank()) return false
    val resolved = resolveFavoriteBgmMediaUrl(context, favorite, raw)
    return resolved.startsWith("file:", ignoreCase = true) && resolved != raw
}

internal fun favoriteBgmCachedBytes(
    context: Context,
    favorite: GuideBgmFavoriteItem
): Long {
    return BaGuideTempMediaCache.cachedMediaBytes(
        context = context,
        sourceUrl = favoriteCacheScope(favorite),
        rawUrl = favorite.audioUrl
    )
}

internal fun favoriteBgmRuntimeState(
    context: Context,
    favorite: GuideBgmFavoriteItem
): BaGuideBgmPlaybackRuntimeState {
    val player = existingFavoriteBgmPlayer(context, favorite)
        ?: return BaGuideBgmPlaybackRuntimeState(volume = GuideBgmFavoritePlaybackStore.volume())
    val duration = player.duration.coerceAtLeast(0L)
    val position = player.currentPosition.coerceAtLeast(0L)
    return BaGuideBgmPlaybackRuntimeState(
        positionMs = if (duration > 0L) position.coerceAtMost(duration) else position,
        durationMs = duration,
        isPlaying = player.isPlaying,
        isBuffering = player.playbackState == Player.STATE_BUFFERING,
        isEnded = player.playbackState == Player.STATE_ENDED,
        volume = player.volume.coerceIn(0f, 1f)
    )
}

internal fun prepareFavoriteBgmPlayback(
    context: Context,
    favorite: GuideBgmFavoriteItem,
    queueMode: BaGuideBgmQueueMode,
    startPositionMs: Long = 0L
): Player? {
    val playbackUrl = favoritePlaybackUrl(context, favorite)
    if (playbackUrl.isBlank()) return null
    val player = GuideBgmPlayerStore.getOrCreate(
        context = context,
        scopeKey = GUIDE_BGM_FAVORITE_AUDIO_SCOPE_KEY,
        audioUrl = playbackUrl
    ) ?: return null
    val shouldAttachMedia = player.currentMediaItem == null
    if (shouldAttachMedia) {
        player.setMediaItem(MediaItem.fromUri(playbackUrl))
        player.prepare()
    }
    val savedVolume = GuideBgmFavoritePlaybackStore.volume()
    if (player.volume != savedVolume) {
        player.volume = savedVolume
    }
    player.repeatMode = if (queueMode == BaGuideBgmQueueMode.SingleLoop) {
        Player.REPEAT_MODE_ONE
    } else {
        Player.REPEAT_MODE_OFF
    }
    val safeStartPosition = startPositionMs.coerceAtLeast(0L)
    if (
        player.playbackState == Player.STATE_ENDED ||
        (safeStartPosition > 0L && (shouldAttachMedia || player.currentPosition <= 0L))
    ) {
        runCatching { player.seekTo(safeStartPosition) }
    }
    return player
}

internal fun playFavoriteBgm(
    context: Context,
    favorite: GuideBgmFavoriteItem,
    queueMode: BaGuideBgmQueueMode,
    startPositionMs: Long = 0L,
    restart: Boolean = false
) {
    val playbackUrl = favoritePlaybackUrl(context, favorite)
    if (playbackUrl.isBlank()) return
    GuideBgmPlayerStore.pauseScopeExcept(
        scopeKey = GUIDE_BGM_FAVORITE_AUDIO_SCOPE_KEY,
        audioUrl = playbackUrl
    )
    val player = prepareFavoriteBgmPlayback(
        context = context,
        favorite = favorite,
        queueMode = queueMode,
        startPositionMs = if (restart) 0L else startPositionMs
    ) ?: return
    if (restart) {
        runCatching { player.seekTo(0L) }
    }
    player.play()
}

internal fun toggleFavoriteBgmPlayback(
    context: Context,
    favorite: GuideBgmFavoriteItem,
    queueMode: BaGuideBgmQueueMode,
    startPositionMs: Long = 0L
) {
    val player = prepareFavoriteBgmPlayback(
        context = context,
        favorite = favorite,
        queueMode = queueMode,
        startPositionMs = startPositionMs
    ) ?: return
    if (player.isPlaying) {
        player.pause()
    } else {
        playFavoriteBgm(
            context = context,
            favorite = favorite,
            queueMode = queueMode,
            startPositionMs = startPositionMs
        )
    }
}

internal fun pauseFavoriteBgmPlayback(
    context: Context,
    favorite: GuideBgmFavoriteItem
): BaGuideBgmPlaybackRuntimeState {
    existingFavoriteBgmPlayer(context, favorite)?.let { player ->
        runCatching { player.pause() }
    }
    val runtimeState = favoriteBgmRuntimeState(context, favorite)
    if (runtimeState.durationMs > 0L || runtimeState.positionMs > 0L) {
        GuideBgmFavoritePlaybackStore.saveProgress(
            audioUrl = favorite.audioUrl,
            positionMs = runtimeState.positionMs,
            durationMs = runtimeState.durationMs,
            isPlaying = false
        )
    }
    return runtimeState.copy(isPlaying = false)
}

internal fun seekFavoriteBgmPlayback(
    context: Context,
    favorite: GuideBgmFavoriteItem,
    queueMode: BaGuideBgmQueueMode,
    progress: Float
): BaGuideBgmPlaybackRuntimeState {
    val player = prepareFavoriteBgmPlayback(
        context = context,
        favorite = favorite,
        queueMode = queueMode
    ) ?: return BaGuideBgmPlaybackRuntimeState()
    val duration = player.duration.coerceAtLeast(0L)
    if (duration <= 0L) return favoriteBgmRuntimeState(context, favorite)
    val targetPosition = (duration * progress.coerceIn(0f, 1f))
        .toLong()
        .coerceIn(0L, duration)
    runCatching { player.seekTo(targetPosition) }
    return favoriteBgmRuntimeState(context, favorite)
}

internal fun updateFavoriteBgmVolume(
    context: Context,
    favorite: GuideBgmFavoriteItem,
    volume: Float
): BaGuideBgmPlaybackRuntimeState {
    val safeVolume = volume.coerceIn(0f, 1f)
    GuideBgmFavoritePlaybackStore.saveVolume(safeVolume)
    existingFavoriteBgmPlayer(context, favorite)?.volume = safeVolume
    return favoriteBgmRuntimeState(context, favorite)
}

internal fun applyFavoriteBgmQueueMode(
    context: Context,
    favorite: GuideBgmFavoriteItem,
    queueMode: BaGuideBgmQueueMode
) {
    existingFavoriteBgmPlayer(context, favorite)?.repeatMode =
        if (queueMode == BaGuideBgmQueueMode.SingleLoop) {
            Player.REPEAT_MODE_ONE
        } else {
            Player.REPEAT_MODE_OFF
        }
}

private fun existingFavoriteBgmPlayer(
    context: Context,
    favorite: GuideBgmFavoriteItem
): Player? {
    val playbackUrl = favoritePlaybackUrl(context, favorite)
    if (playbackUrl.isBlank()) return null
    return GuideBgmPlayerStore.getExisting(
        scopeKey = GUIDE_BGM_FAVORITE_AUDIO_SCOPE_KEY,
        audioUrl = playbackUrl
    )
}

private fun favoritePlaybackUrl(
    context: Context,
    favorite: GuideBgmFavoriteItem
): String {
    val raw = normalizeGuideMediaSource(favorite.audioUrl)
    if (raw.isBlank()) return ""
    return resolveFavoriteBgmMediaUrl(context, favorite, raw)
}
