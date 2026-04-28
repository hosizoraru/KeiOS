package os.kei.mcp.framework.notification.builder

import os.kei.mcp.notification.McpNotificationPayload

data class NotificationPayload(
    val state: McpNotificationPayload,
    val settings: UserSettings,
    val environment: EnvironmentContext
)

data class UserSettings(
    val miIslandOuterGlow: Boolean
)

data class EnvironmentContext(
    val channelId: String,
    val isHyperOS: Boolean,
    val preferOemLiveIconLayout: Boolean = false
)

enum class NotificationRenderStyle {
    MI_ISLAND,
    LIVE_UPDATE,
    LEGACY
}
