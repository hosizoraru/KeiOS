package os.kei.core.notification.live.builder

import android.graphics.Bitmap
import os.kei.core.notification.live.LiveNotificationPayload

data class NotificationPayload(
    val state: LiveNotificationPayload,
    val settings: UserSettings,
    val environment: EnvironmentContext,
    val semanticIconBitmap: Bitmap? = null,
    val miIslandProgressColorOverride: String? = null
)

data class UserSettings(
    val miIslandOuterGlow: Boolean,
    val miIslandFirstFloat: Boolean = true,
    val miIslandFinishFloat: Boolean = true,
    /** `param_v2.timeout` in minutes; null leaves HyperOS's default (720). */
    val miIslandFocusTimeoutMinutes: Int? = null,
    /** `param_island.islandTimeout` in seconds; null leaves HyperOS's default (3600). */
    val miIslandTimeoutSeconds: Int? = null
)

data class EnvironmentContext(
    val channelId: String,
    val isHyperOS: Boolean,
)

enum class NotificationRenderStyle {
    MI_ISLAND,
    LIVE_UPDATE,
    LEGACY
}
