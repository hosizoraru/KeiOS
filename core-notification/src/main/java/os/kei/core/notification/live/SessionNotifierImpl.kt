package os.kei.core.notification.live

import os.kei.core.log.AppLogger
import os.kei.core.prefs.UiPrefs
import os.kei.core.notification.live.LiveNotificationPayload
import os.kei.core.notification.live.SessionNotifier
import os.kei.core.notification.live.builder.EnvironmentContext
import os.kei.core.notification.live.builder.LegacyNotificationBuilder
import os.kei.core.notification.live.builder.MiIslandNotificationBuilder
import os.kei.core.notification.live.builder.ModernNotificationBuilder
import os.kei.core.notification.live.builder.NotificationPayload
import os.kei.core.notification.live.builder.NotificationRenderStyle
import os.kei.core.notification.live.builder.UserSettings

class SessionNotifierImpl(
    private val helper: NotificationHelper
) : SessionNotifier {
    private companion object {
        private const val TAG = "McpSessionNotifier"
    }

    private val modernBuilder by lazy { ModernNotificationBuilder(helper.context) }
    private val legacyBuilder by lazy { LegacyNotificationBuilder(helper.context) }
    private val miIslandBuilder by lazy { MiIslandNotificationBuilder(helper.context) }

    override fun build(payload: LiveNotificationPayload): SessionNotifier.NotificationBuildResult {
        val preferSuperIsland = UiPrefs.isSuperIslandNotificationEnabled(defaultValue = false)
        val bypassRestriction = UiPrefs.isSuperIslandBypassRestrictionEnabled(defaultValue = false)
        val decision = helper.resolveRenderDecision(
            preferSuperIsland = preferSuperIsland,
            bypassRestriction = bypassRestriction,
        )
        val style = decision.style
        AppLogger.i(
            TAG,
            "build ${decision.logSummary()}"
        )
        val floatBehavior = UiPrefs.getSuperIslandFloatBehavior()
        val autoClose = UiPrefs.getSuperIslandAutoClose()
        AppLogger.d(TAG) {
            "buildDetail ${decision.logSummary()} server=${payload.serverName} " +
                "notificationId=${payload.notificationId} running=${payload.running} " +
                "ongoing=${payload.ongoing} behavior=${floatBehavior.storageId} " +
                "firstFloat=${floatBehavior.firstFloatEnabled} finishFloat=${floatBehavior.finishFloatEnabled} " +
                "autoClose=${autoClose.storageId} " +
                "channel=${helper.resolveChannel(style)}"
        }
        val wrapped = NotificationPayload(
            state = payload,
            settings =
                UserSettings(
                    miIslandOuterGlow = payload.outerGlow,
                    miIslandFirstFloat = floatBehavior.firstFloatEnabled,
                    miIslandFinishFloat = floatBehavior.finishFloatEnabled,
                    miIslandFocusTimeoutMinutes = autoClose.focusTimeoutMinutes,
                    miIslandTimeoutSeconds = autoClose.islandTimeoutSeconds,
                ),
            environment = EnvironmentContext(
                channelId = helper.resolveChannel(style),
                isHyperOS = helper.isHyperOS,
            ),
            miIslandProgressColorOverride = payload.overrideAccentColor
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
            useXiaomiMagic = decision.useXiaomiMagic
        )
    }
}
