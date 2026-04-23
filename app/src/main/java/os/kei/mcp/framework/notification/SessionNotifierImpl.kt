package os.kei.mcp.framework.notification

import os.kei.core.log.AppLogger
import os.kei.core.prefs.UiPrefs
import os.kei.mcp.notification.McpNotificationPayload
import os.kei.mcp.domain.notification.SessionNotifier
import os.kei.mcp.framework.notification.builder.EnvironmentContext
import os.kei.mcp.framework.notification.builder.LegacyNotificationBuilder
import os.kei.mcp.framework.notification.builder.MiIslandNotificationBuilder
import os.kei.mcp.framework.notification.builder.ModernNotificationBuilder
import os.kei.mcp.framework.notification.builder.NotificationPayload
import os.kei.mcp.framework.notification.builder.NotificationRenderStyle
import os.kei.mcp.framework.notification.builder.UserSettings

class SessionNotifierImpl(
    private val helper: NotificationHelper
) : SessionNotifier {
    private companion object {
        private const val TAG = "McpSessionNotifier"
    }

    private val modernBuilder by lazy { ModernNotificationBuilder(helper.context) }
    private val legacyBuilder by lazy { LegacyNotificationBuilder(helper.context) }
    private val miIslandBuilder by lazy { MiIslandNotificationBuilder(helper.context) }

    override fun build(payload: McpNotificationPayload): SessionNotifier.NotificationBuildResult {
        val preferSuperIsland = UiPrefs.isSuperIslandNotificationEnabled(defaultValue = false)
        val bypassRestriction = UiPrefs.isSuperIslandBypassRestrictionEnabled(defaultValue = false)
        val style = resolveStyle(preferSuperIsland = preferSuperIsland)
        AppLogger.i(
            TAG,
            "build preferSuperIsland=$preferSuperIsland supportMiIsland=${helper.isSupportMiIsland} " +
                "focusPermission=${helper.hasMiIslandPermission} available=${helper.isMiIslandAvailable} " +
                "style=$style bypass=$bypassRestriction"
        )
        val wrapped = NotificationPayload(
            state = payload,
            settings = UserSettings(miIslandOuterGlow = payload.outerGlow),
            environment = EnvironmentContext(
                channelId = helper.resolveChannel(style),
                isHyperOS = helper.isHyperOS
            )
        )
        val notification = when (style) {
            NotificationRenderStyle.MI_ISLAND -> miIslandBuilder.build(wrapped)
            NotificationRenderStyle.LIVE_UPDATE -> {
                if (helper.isModernLiveUpdateEligible) modernBuilder.build(wrapped) else legacyBuilder.build(wrapped)
            }
            NotificationRenderStyle.LEGACY -> legacyBuilder.build(wrapped)
        }
        return SessionNotifier.NotificationBuildResult(
            notification = notification,
            style = style,
            useXiaomiMagic = style == NotificationRenderStyle.MI_ISLAND && bypassRestriction
        )
    }

    private fun resolveStyle(preferSuperIsland: Boolean): NotificationRenderStyle {
        if (preferSuperIsland && helper.isMiIslandAvailable) {
            return NotificationRenderStyle.MI_ISLAND
        }
        return NotificationRenderStyle.LIVE_UPDATE
    }
}
