package os.kei.core.notification.live.oem.vivo

import android.content.Context
import android.graphics.drawable.Icon
import os.kei.core.notification.live.LiveNotificationPayload

/** Maps rendering data only. Activity identity, business admission and lifecycle remain explicit. */
object VivoAtomicLivePayloadMapper {
    fun content(
        context: Context,
        payload: LiveNotificationPayload,
        icon: Icon,
        shortDescription: String,
        shortCoreInfo: String,
        capsuleText: String,
        newNode: Int = 0,
        progressState: Int = 0,
        landingPageVisible: Boolean = false,
    ): VivoAtomicContent = VivoAtomicContent(
        title = payload.title(context),
        content = payload.content(context).ifBlank { shortDescription },
        shortDescription = shortDescription,
        shortCoreInfo = shortCoreInfo,
        capsuleText = capsuleText,
        icon = icon,
        openIntent = payload.focusOpenPendingIntent,
        progress = payload.overrideProgressPercent,
        progressState = progressState,
        newNode = newNode,
        landingPageVisible = landingPageVisible,
    )
}
