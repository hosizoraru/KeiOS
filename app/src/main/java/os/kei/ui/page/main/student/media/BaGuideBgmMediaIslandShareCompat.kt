package os.kei.ui.page.main.student

import android.app.Notification
import android.content.Context
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import org.json.JSONObject
import os.kei.R

internal const val BA_GUIDE_BGM_MEDIA_ISLAND_PARAMS_KEY = "miui.focus.param.media"
internal const val BA_GUIDE_BGM_MEDIA_METADATA_AUDIO_URL = "os.kei.ba.bgm.audio_url"
internal const val BA_GUIDE_BGM_MEDIA_METADATA_SOURCE_URL = "os.kei.ba.bgm.source_url"
internal const val BA_GUIDE_BGM_MEDIA_METADATA_STUDENT_TITLE = "os.kei.ba.bgm.student_title"

@OptIn(UnstableApi::class)
internal class BaGuideBgmMediaIslandShareNotificationProvider(
    private val context: Context,
    private val delegate: MediaNotification.Provider,
    private val enabled: Boolean
) : MediaNotification.Provider {
    override fun createNotification(
        mediaSession: MediaSession,
        mediaButtonPreferences: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {
        // Artwork may complete after a track change or session release.
        val shareParams = if (enabled) {
            BaGuideBgmMediaIslandShareCompat.buildShareParams(
                context,
                mediaSession.player.currentMediaItem
            )
        } else {
            null
        }
        val callback = if (enabled) {
            MediaNotification.Provider.Callback { notification ->
                onNotificationChangedCallback.onNotificationChanged(
                    notification.withMediaIslandShareParams(shareParams)
                )
            }
        } else {
            onNotificationChangedCallback
        }
        val notification = delegate.createNotification(
            mediaSession,
            mediaButtonPreferences,
            actionFactory,
            callback
        )
        return if (enabled) {
            notification.withMediaIslandShareParams(shareParams)
        } else {
            notification
        }
    }

    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: android.os.Bundle
    ): Boolean {
        return delegate.handleCustomCommand(session, action, extras)
    }

    override fun getNotificationChannelInfo(): MediaNotification.Provider.NotificationChannelInfo {
        return delegate.notificationChannelInfo
    }
}

private fun MediaNotification.withMediaIslandShareParams(
    params: String?
): MediaNotification {
    val decorated = notification.clone()
    if (params.isNullOrBlank()) {
        decorated.extras.remove(BA_GUIDE_BGM_MEDIA_ISLAND_PARAMS_KEY)
    } else {
        decorated.extras.putString(BA_GUIDE_BGM_MEDIA_ISLAND_PARAMS_KEY, params)
    }
    return MediaNotification(notificationId, decorated)
}

internal object BaGuideBgmMediaIslandShareCompat {
    fun buildShareParams(context: Context, mediaItem: MediaItem?): String? {
        if (mediaItem == null) return null
        val title = mediaItem.mediaMetadata.resolveShareTitle(context)
        val shareLink = mediaItem.resolveShareLink()
        if (shareLink.isBlank()) return null
        val content = context.getString(R.string.ba_catalog_bgm_media_share_content, title)
        val shareContent = context.getString(
            R.string.ba_catalog_bgm_media_share_text,
            title,
            shareLink
        )
        val shareData = JSONObject()
            .put("title", title)
            .put("content", content)
            .put("shareContent", shareContent)
        return JSONObject()
            .put(
                "param_v2",
                JSONObject()
                    .put(
                        "param_island",
                        JSONObject().put("shareData", shareData)
                    )
            )
            .toString()
    }

    fun notificationShareParams(notification: Notification): String? {
        return notification.extras.getString(BA_GUIDE_BGM_MEDIA_ISLAND_PARAMS_KEY)
    }
}

private fun MediaMetadata.resolveShareTitle(context: Context): String {
    return extras
        ?.getString(BA_GUIDE_BGM_MEDIA_METADATA_STUDENT_TITLE)
        .orEmpty()
        .ifBlank { displayTitle?.toString().orEmpty() }
        .ifBlank { title?.toString().orEmpty() }
        .ifBlank { context.getString(R.string.ba_catalog_bgm_student_unknown) }
}

private fun MediaItem.resolveShareLink(): String {
    val metadataExtras = mediaMetadata.extras
    return metadataExtras
        ?.getString(BA_GUIDE_BGM_MEDIA_METADATA_SOURCE_URL)
        .orEmpty()
        .asShareableUrl()
        .ifBlank {
            metadataExtras
                ?.getString(BA_GUIDE_BGM_MEDIA_METADATA_AUDIO_URL)
                .orEmpty()
                .asShareableUrl()
        }
        .ifBlank { mediaId.asShareableUrl() }
        .ifBlank { localConfiguration?.uri?.toString().orEmpty().asShareableUrl() }
}

private fun String.asShareableUrl(): String {
    val value = trim()
    if (value.isBlank()) return ""
    val scheme = runCatching { value.toUri().scheme.orEmpty() }.getOrDefault("")
    return if (scheme.equals("http", ignoreCase = true) ||
        scheme.equals("https", ignoreCase = true)
    ) {
        value
    } else {
        ""
    }
}
