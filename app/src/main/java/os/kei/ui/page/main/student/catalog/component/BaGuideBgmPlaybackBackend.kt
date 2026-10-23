package os.kei.ui.page.main.student.catalog.component

import android.content.Context
import os.kei.ui.page.main.student.BaGuideBgmPlaybackRepository
import os.kei.ui.page.main.student.GuideBgmFavoriteItem

internal enum class BaGuideBgmPlaybackBackendMode {
    Lightweight,
    NativeMedia,
}

internal data class BaGuideBgmNativeSessionSnapshot(
    val queue: List<GuideBgmFavoriteItem>,
    val selectedAudioUrl: String,
    val queueMode: BaGuideBgmQueueMode,
)

internal fun resolveBaGuideBgmPlaybackBackendMode(nativeMediaNotificationEnabled: Boolean): BaGuideBgmPlaybackBackendMode =
    if (nativeMediaNotificationEnabled) {
        BaGuideBgmPlaybackBackendMode.NativeMedia
    } else {
        BaGuideBgmPlaybackBackendMode.Lightweight
    }

internal interface BaGuideBgmPlaybackBackend {
    val mode: BaGuideBgmPlaybackBackendMode

    val isConnected: Boolean
        get() = true

    fun connect(onConnected: () -> Unit) = onConnected()

    fun currentSession(): BaGuideBgmNativeSessionSnapshot? = null

    fun updateQueue(
        queue: List<GuideBgmFavoriteItem>,
        selectedAudioUrl: String,
        queueMode: BaGuideBgmQueueMode,
        startPositionMs: Long,
    )

    fun prepare(
        favorite: GuideBgmFavoriteItem,
        queue: List<GuideBgmFavoriteItem>,
        queueMode: BaGuideBgmQueueMode,
        startPositionMs: Long,
    )

    fun play(
        favorite: GuideBgmFavoriteItem,
        queue: List<GuideBgmFavoriteItem>,
        queueMode: BaGuideBgmQueueMode,
        startPositionMs: Long,
        restart: Boolean,
    )

    fun toggle(
        favorite: GuideBgmFavoriteItem,
        queue: List<GuideBgmFavoriteItem>,
        queueMode: BaGuideBgmQueueMode,
        startPositionMs: Long,
    )

    fun pause(favorite: GuideBgmFavoriteItem): BaGuideBgmPlaybackRuntimeState

    fun seek(
        favorite: GuideBgmFavoriteItem,
        queueMode: BaGuideBgmQueueMode,
        progress: Float,
    ): BaGuideBgmPlaybackRuntimeState

    fun updateVolume(
        favorite: GuideBgmFavoriteItem,
        volume: Float,
    ): BaGuideBgmPlaybackRuntimeState

    fun applyQueueMode(
        favorite: GuideBgmFavoriteItem,
        queueMode: BaGuideBgmQueueMode,
    )

    fun runtimeState(favorite: GuideBgmFavoriteItem?): BaGuideBgmPlaybackRuntimeState

    fun currentAudioUrl(): String = ""

    fun currentQueueMode(): BaGuideBgmQueueMode? = null

    fun disconnect() = Unit

    fun stopSession() = disconnect()
}

internal class BaGuideBgmLightweightPlaybackBackend(
    private val context: Context,
    private val playbackRepository: BaGuideBgmPlaybackRepository,
) : BaGuideBgmPlaybackBackend {
    override val mode: BaGuideBgmPlaybackBackendMode = BaGuideBgmPlaybackBackendMode.Lightweight

    override fun updateQueue(
        queue: List<GuideBgmFavoriteItem>,
        selectedAudioUrl: String,
        queueMode: BaGuideBgmQueueMode,
        startPositionMs: Long,
    ) = Unit

    override fun prepare(
        favorite: GuideBgmFavoriteItem,
        queue: List<GuideBgmFavoriteItem>,
        queueMode: BaGuideBgmQueueMode,
        startPositionMs: Long,
    ) {
        prepareFavoriteBgmPlayback(
            context = context,
            favorite = favorite,
            queueMode = queueMode,
            startPositionMs = startPositionMs,
            savedVolume = playbackRepository.volume(),
        )
    }

    override fun play(
        favorite: GuideBgmFavoriteItem,
        queue: List<GuideBgmFavoriteItem>,
        queueMode: BaGuideBgmQueueMode,
        startPositionMs: Long,
        restart: Boolean,
    ) {
        playFavoriteBgm(
            context = context,
            favorite = favorite,
            queueMode = queueMode,
            startPositionMs = startPositionMs,
            restart = restart,
            savedVolume = playbackRepository.volume(),
        )
    }

    override fun toggle(
        favorite: GuideBgmFavoriteItem,
        queue: List<GuideBgmFavoriteItem>,
        queueMode: BaGuideBgmQueueMode,
        startPositionMs: Long,
    ) {
        toggleFavoriteBgmPlayback(
            context = context,
            favorite = favorite,
            queueMode = queueMode,
            startPositionMs = startPositionMs,
            savedVolume = playbackRepository.volume(),
        )
    }

    override fun pause(favorite: GuideBgmFavoriteItem): BaGuideBgmPlaybackRuntimeState =
        pauseFavoriteBgmPlayback(
            context = context,
            favorite = favorite,
            savedVolume = playbackRepository.volume(),
        )

    override fun seek(
        favorite: GuideBgmFavoriteItem,
        queueMode: BaGuideBgmQueueMode,
        progress: Float,
    ): BaGuideBgmPlaybackRuntimeState =
        seekFavoriteBgmPlayback(
            context = context,
            favorite = favorite,
            queueMode = queueMode,
            progress = progress,
            savedVolume = playbackRepository.volume(),
        )

    override fun updateVolume(
        favorite: GuideBgmFavoriteItem,
        volume: Float,
    ): BaGuideBgmPlaybackRuntimeState = updateFavoriteBgmVolume(context, favorite, volume)

    override fun applyQueueMode(
        favorite: GuideBgmFavoriteItem,
        queueMode: BaGuideBgmQueueMode,
    ) {
        applyFavoriteBgmQueueMode(context, favorite, queueMode)
    }

    override fun runtimeState(favorite: GuideBgmFavoriteItem?): BaGuideBgmPlaybackRuntimeState =
        favorite
            ?.let { favoriteBgmRuntimeState(context, it, savedVolume = playbackRepository.volume()) }
            ?: BaGuideBgmPlaybackRuntimeState(volume = playbackRepository.volume())
}

internal class BaGuideBgmNativePlaybackBackend(
    private val controller: BaGuideBgmNativeMediaController,
) : BaGuideBgmPlaybackBackend {
    override val mode: BaGuideBgmPlaybackBackendMode = BaGuideBgmPlaybackBackendMode.NativeMedia

    override val isConnected: Boolean
        get() = controller.isConnected

    override fun connect(onConnected: () -> Unit) = controller.connect(onConnected)

    override fun currentSession(): BaGuideBgmNativeSessionSnapshot? = controller.currentSession()

    override fun updateQueue(
        queue: List<GuideBgmFavoriteItem>,
        selectedAudioUrl: String,
        queueMode: BaGuideBgmQueueMode,
        startPositionMs: Long,
    ) {
        controller.syncQueue(queue, selectedAudioUrl, queueMode, startPositionMs)
    }

    override fun prepare(
        favorite: GuideBgmFavoriteItem,
        queue: List<GuideBgmFavoriteItem>,
        queueMode: BaGuideBgmQueueMode,
        startPositionMs: Long,
    ) {
        controller.syncQueue(queue, favorite.audioUrl, queueMode, startPositionMs)
    }

    override fun play(
        favorite: GuideBgmFavoriteItem,
        queue: List<GuideBgmFavoriteItem>,
        queueMode: BaGuideBgmQueueMode,
        startPositionMs: Long,
        restart: Boolean,
    ) {
        controller.syncQueue(
            queue = queue,
            selectedAudioUrl = favorite.audioUrl,
            queueMode = queueMode,
            startPositionMs = startPositionMs,
            playWhenReady = true,
            restart = restart,
        )
    }

    override fun toggle(
        favorite: GuideBgmFavoriteItem,
        queue: List<GuideBgmFavoriteItem>,
        queueMode: BaGuideBgmQueueMode,
        startPositionMs: Long,
    ) {
        val current = controller.runtimeState()
        if (current.isPlaying) {
            controller.pause()
        } else {
            play(favorite, queue, queueMode, startPositionMs, restart = false)
        }
    }

    override fun pause(favorite: GuideBgmFavoriteItem): BaGuideBgmPlaybackRuntimeState {
        controller.pause()
        return controller.runtimeState().copy(isPlaying = false)
    }

    override fun seek(
        favorite: GuideBgmFavoriteItem,
        queueMode: BaGuideBgmQueueMode,
        progress: Float,
    ): BaGuideBgmPlaybackRuntimeState {
        controller.seekToProgress(progress)
        return controller.runtimeState()
    }

    override fun updateVolume(
        favorite: GuideBgmFavoriteItem,
        volume: Float,
    ): BaGuideBgmPlaybackRuntimeState {
        val safeVolume = volume.coerceIn(0f, 1f)
        controller.updateVolume(safeVolume)
        return controller.runtimeState().copy(volume = safeVolume)
    }

    override fun applyQueueMode(
        favorite: GuideBgmFavoriteItem,
        queueMode: BaGuideBgmQueueMode,
    ) {
        controller.updateRepeatMode(queueMode)
    }

    override fun runtimeState(favorite: GuideBgmFavoriteItem?): BaGuideBgmPlaybackRuntimeState = controller.runtimeState()

    override fun currentAudioUrl(): String = controller.currentAudioUrl()

    override fun currentQueueMode(): BaGuideBgmQueueMode? = controller.currentQueueMode()

    override fun disconnect() {
        controller.disconnect()
    }

    override fun stopSession() {
        controller.stopSession()
    }
}
