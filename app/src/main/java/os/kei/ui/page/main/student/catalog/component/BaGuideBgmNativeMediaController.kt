package os.kei.ui.page.main.student.catalog.component

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import os.kei.core.concurrency.AppDispatchers
import os.kei.ui.page.main.student.BaGuideBgmMediaButtonPreferences
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.media.BaGuideBgmMediaSessionService
import os.kei.ui.page.main.student.normalizeGuideMediaSource

internal class BaGuideBgmNativeMediaController(
    context: Context,
    private val volumeProvider: () -> Float,
    artworkDispatcher: CoroutineDispatcher = AppDispatchers.uiDerivation,
) {
    private val appContext = context.applicationContext
    private val mainExecutor = appContext.mainExecutor
    private val artworkScope = CoroutineScope(SupervisorJob() + artworkDispatcher)
    private val lock = Any()
    private val connection =
        BaGuideBgmControllerConnection<MediaController>(
            executor = mainExecutor,
            connect = { onControllerDisconnected ->
                val token =
                    SessionToken(
                        appContext,
                        ComponentName(appContext, BaGuideBgmMediaSessionService::class.java),
                    )
                MediaController
                    .Builder(appContext, token)
                    .setApplicationLooper(appContext.mainLooper)
                    .setListener(
                        object : MediaController.Listener {
                            override fun onDisconnected(controller: MediaController) {
                                onControllerDisconnected(controller)
                            }
                        },
                    ).buildAsync()
            },
            isConnected = MediaController::isConnected,
            releaseController = MediaController::release,
            releaseFuture = { future -> MediaController.releaseFuture(future) },
            onDisconnected = ::cancelArtworkHydration,
        )
    private val controller: MediaController?
        get() = connection.current
    private var artworkHydrationJob: Job? = null
    private var artworkHydrationGeneration = 0L

    fun syncQueue(
        queue: List<GuideBgmFavoriteItem>,
        selectedAudioUrl: String,
        queueMode: BaGuideBgmQueueMode,
        startPositionMs: Long,
        playWhenReady: Boolean? = null,
        restart: Boolean = false,
    ) {
        val artworkGeneration = nextArtworkHydrationGeneration()
        val mediaItems = queue.toBaGuideBgmMediaItems(appContext)
        if (mediaItems.isEmpty()) return
        submit { mediaController ->
            val currentIds =
                (0 until mediaController.mediaItemCount)
                    .map { index -> mediaController.getMediaItemAt(index).mediaId }
            val nextIds = mediaItems.map { item -> item.mediaId }
            val currentMediaId = mediaController.currentMediaItem?.mediaId.orEmpty()
            val queuePosition =
                resolveBaGuideBgmNativeQueuePosition(
                    nextMediaIds = nextIds,
                    selectedMediaId = normalizeGuideMediaSource(selectedAudioUrl),
                    currentMediaId = currentMediaId,
                    currentPositionMs = mediaController.currentPosition,
                    startPositionMs = startPositionMs,
                    playWhenReady = playWhenReady,
                    restart = restart,
                )
            val resolvedIndex = queuePosition.index
            val resolvedStartPositionMs = queuePosition.positionMs
            if (
                currentIds != nextIds ||
                currentMediaId != mediaItems[resolvedIndex].mediaId ||
                restart ||
                mediaController.playbackState == Player.STATE_IDLE
            ) {
                mediaController.setMediaItems(mediaItems, resolvedIndex, resolvedStartPositionMs)
                mediaController.prepare()
            }
            mediaController.repeatMode =
                BaGuideBgmMediaButtonPreferences.nativeRepeatMode(queueMode)
            mediaController.volume = volumeProvider().coerceIn(0f, 1f)
            when (playWhenReady) {
                true -> mediaController.play()
                false -> mediaController.pause()
                null -> Unit
            }
        }
        scheduleArtworkHydration(
            generation = artworkGeneration,
            queue = queue,
            selectedAudioUrl = selectedAudioUrl,
        )
    }

    fun pause() {
        submit { mediaController -> mediaController.pause() }
    }

    fun seekToProgress(progress: Float) {
        submit { mediaController ->
            val duration = mediaController.duration.coerceAtLeast(0L)
            if (duration <= 0L) return@submit
            val position =
                (duration * progress.coerceIn(0f, 1f))
                    .toLong()
                    .coerceIn(0L, duration)
            mediaController.seekTo(position)
        }
    }

    fun updateVolume(volume: Float) {
        val safeVolume = volume.coerceIn(0f, 1f)
        submit { mediaController -> mediaController.volume = safeVolume }
    }

    fun updateRepeatMode(queueMode: BaGuideBgmQueueMode) {
        submit { mediaController ->
            mediaController.repeatMode =
                BaGuideBgmMediaButtonPreferences.nativeRepeatMode(queueMode)
        }
    }

    fun currentQueueMode(): BaGuideBgmQueueMode? {
        val mediaController = controller ?: return null
        return runCatching {
            BaGuideBgmMediaButtonPreferences.queueModeFromNativeRepeatMode(mediaController.repeatMode)
        }.getOrNull()
    }

    fun runtimeState(): BaGuideBgmPlaybackRuntimeState {
        val mediaController =
            controller ?: return BaGuideBgmPlaybackRuntimeState(
                volume = volumeProvider().coerceIn(0f, 1f),
            )
        return runCatching {
            val duration = mediaController.duration.coerceAtLeast(0L)
            val position = mediaController.currentPosition.coerceAtLeast(0L)
            BaGuideBgmPlaybackRuntimeState(
                positionMs = if (duration > 0L) position.coerceAtMost(duration) else position,
                durationMs = duration,
                isPlaying = mediaController.isPlaying,
                isBuffering = mediaController.playbackState == Player.STATE_BUFFERING,
                isEnded = mediaController.playbackState == Player.STATE_ENDED,
                volume = mediaController.volume.coerceIn(0f, 1f),
            )
        }.getOrDefault(
            BaGuideBgmPlaybackRuntimeState(volume = volumeProvider().coerceIn(0f, 1f)),
        )
    }

    fun currentAudioUrl(): String {
        val mediaController = controller ?: return ""
        return runCatching {
            normalizeGuideMediaSource(mediaController.currentMediaItem?.mediaId.orEmpty())
        }.getOrDefault("")
    }

    fun stopSession() {
        connection.disconnect { mediaController ->
            stopBaGuideBgmNativePlayback(
                requestTerminalStop = {
                    mediaController.sendCustomCommand(
                        BaGuideBgmMediaButtonPreferences.stopPlaybackCommand(),
                        Bundle.EMPTY,
                    )
                },
                pause = mediaController::pause,
                stop = mediaController::stop,
                clearMediaItems = mediaController::clearMediaItems,
            )
        }
        runCatching {
            appContext.stopService(Intent(appContext, BaGuideBgmMediaSessionService::class.java))
        }
    }

    fun disconnect() {
        connection.disconnect()
    }

    private fun nextArtworkHydrationGeneration(): Long =
        synchronized(lock) {
            artworkHydrationJob?.cancel()
            artworkHydrationJob = null
            artworkHydrationGeneration += 1L
            artworkHydrationGeneration
        }

    private fun cancelArtworkHydration() {
        synchronized(lock) {
            artworkHydrationGeneration += 1L
            artworkHydrationJob?.cancel()
            artworkHydrationJob = null
        }
    }

    private fun scheduleArtworkHydration(
        generation: Long,
        queue: List<GuideBgmFavoriteItem>,
        selectedAudioUrl: String,
    ) {
        val targets = queue.orderedArtworkHydrationTargets(selectedAudioUrl)
        if (targets.isEmpty()) return
        val job =
            artworkScope.launch {
                targets.forEach { favorite ->
                    if (!isArtworkHydrationCurrent(generation)) return@launch
                    val mediaId = normalizeGuideMediaSource(favorite.audioUrl)
                    val artworkData =
                        BaGuideBgmMediaArtworkPayloadResolver.resolve(appContext, favorite)
                            ?: return@forEach
                    if (!isArtworkHydrationCurrent(generation)) return@launch
                    connection.submit(connectIfNeeded = false) { mediaController ->
                        if (!isArtworkHydrationCurrent(generation)) return@submit
                        mediaController.replaceArtworkMetadata(mediaId, artworkData)
                    }
                }
            }
        synchronized(lock) {
            if (artworkHydrationGeneration == generation) {
                artworkHydrationJob = job
            } else {
                job.cancel()
            }
        }
    }

    private fun isArtworkHydrationCurrent(generation: Long): Boolean =
        synchronized(lock) {
            artworkHydrationGeneration == generation
        }

    private fun MediaController.replaceArtworkMetadata(
        mediaId: String,
        artworkData: ByteArray,
    ) {
        val index =
            (0 until mediaItemCount)
                .firstOrNull { itemIndex -> getMediaItemAt(itemIndex).mediaId == mediaId }
                ?: return
        val currentItem = getMediaItemAt(index)
        if (currentItem.mediaMetadata.artworkData?.contentEquals(artworkData) == true) return
        replaceMediaItem(index, currentItem.withArtworkData(artworkData))
    }

    private fun submit(command: (MediaController) -> Unit) {
        connection.submit(command = command)
    }
}

private const val MAX_NATIVE_ARTWORK_HYDRATION_TARGETS = 8

internal fun List<GuideBgmFavoriteItem>.orderedArtworkHydrationTargets(
    selectedAudioUrl: String,
    maxCount: Int = MAX_NATIVE_ARTWORK_HYDRATION_TARGETS,
): List<GuideBgmFavoriteItem> {
    if (isEmpty() || maxCount <= 0) return emptyList()
    val selectedMediaId = normalizeGuideMediaSource(selectedAudioUrl)
    val selectedIndex =
        indexOfFirst { favorite ->
            normalizeGuideMediaSource(favorite.audioUrl) == selectedMediaId
        }.takeIf { it >= 0 } ?: 0
    return indices
        .asSequence()
        .map { offset -> (selectedIndex + offset) % size }
        .map { index -> this[index] }
        .distinctBy { favorite -> normalizeGuideMediaSource(favorite.audioUrl) }
        .take(maxCount)
        .toList()
}

private fun MediaItem.withArtworkData(artworkData: ByteArray): MediaItem {
    val metadata =
        mediaMetadata
            .buildUpon()
            .setArtworkData(artworkData, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            .build()
    return buildUpon()
        .setMediaMetadata(metadata)
        .build()
}

internal data class BaGuideBgmNativeQueuePosition(
    val index: Int,
    val positionMs: Long,
)

internal fun resolveBaGuideBgmNativeQueuePosition(
    nextMediaIds: List<String>,
    selectedMediaId: String,
    currentMediaId: String,
    currentPositionMs: Long,
    startPositionMs: Long,
    playWhenReady: Boolean?,
    restart: Boolean,
): BaGuideBgmNativeQueuePosition {
    require(nextMediaIds.isNotEmpty())
    val preservedIndex =
        nextMediaIds.indexOf(currentMediaId).takeIf { index ->
            playWhenReady == null && !restart && index >= 0
        }
    val targetIndex = nextMediaIds.indexOf(selectedMediaId).takeIf { it >= 0 } ?: 0
    return BaGuideBgmNativeQueuePosition(
        index = preservedIndex ?: targetIndex,
        positionMs =
            when {
                restart -> 0L
                preservedIndex != null -> currentPositionMs.coerceAtLeast(0L)
                else -> startPositionMs.coerceAtLeast(0L)
            },
    )
}

internal fun stopBaGuideBgmNativePlayback(
    requestTerminalStop: () -> Unit,
    pause: () -> Unit,
    stop: () -> Unit,
    clearMediaItems: () -> Unit,
) {
    runCatching { requestTerminalStop() }
    // Stop audio locally even if the asynchronous terminal command is rejected or fails.
    runCatching { pause() }
    runCatching { stop() }
    runCatching { clearMediaItems() }
}
