package os.kei.mcp.framework.notification.builder

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Icon
import androidx.core.app.NotificationCompat
import com.xzakota.hyper.notification.focus.FocusNotification
import os.kei.R
import os.kei.core.log.AppLogger
import os.kei.mcp.notification.McpNotificationPayload

class MiIslandNotificationBuilder(
    private val context: Context
) : SessionNotificationBuilder {
    private data class IslandAction(
        val key: String,
        val title: String,
        val pendingIntent: PendingIntent,
        val isHighlighted: Boolean = false
    )

    private data class IslandPresentation(
        val allowFloat: Boolean,
        val showTextButtons: Boolean,
        val rightTitle: String,
        val rightContent: String? = null
    )

    private companion object {
        private const val TAG = "McpMiIslandBuilder"
        private const val HIGHLIGHT_BG_COLOR = "#006EFF"
        private const val HIGHLIGHT_TITLE_COLOR = "#FFFFFF"
        private const val ISLAND_ICON_RES_ID_DEFAULT = R.drawable.ic_kei_logo_island
        private const val ISLAND_ICON_RES_ID_AP = R.drawable.ic_ba_ap_island_notification
        private const val ISLAND_ICON_RES_ID_BA_CAFE_VISIT = R.drawable.ic_ba_schale_island
        private const val ISLAND_ICON_RES_ID_BA_ARENA_REFRESH = R.drawable.ic_ba_schale_island
    }

    override fun build(payload: NotificationPayload): Notification {
        val state = payload.state
        val isBlueArchiveAp = McpNotificationPayload.isBaApServerName(state.serverName)
        val isBlueArchiveCafeVisit = McpNotificationPayload.isBaCafeVisitServerName(state.serverName)
        val isBlueArchiveArenaRefresh = McpNotificationPayload.isBaArenaRefreshServerName(state.serverName)
        val isBlueArchiveNotification =
            isBlueArchiveAp || isBlueArchiveCafeVisit || isBlueArchiveArenaRefresh
        val islandIconResId = when {
            isBlueArchiveAp -> ISLAND_ICON_RES_ID_AP
            isBlueArchiveCafeVisit -> ISLAND_ICON_RES_ID_BA_CAFE_VISIT
            isBlueArchiveArenaRefresh -> ISLAND_ICON_RES_ID_BA_ARENA_REFRESH
            else -> ISLAND_ICON_RES_ID_DEFAULT
        }
        val shortCriticalText = resolveShortCriticalText(
            state = state,
            isBlueArchiveAp = isBlueArchiveAp,
            isBlueArchiveCafeVisit = isBlueArchiveCafeVisit,
            isBlueArchiveArenaRefresh = isBlueArchiveArenaRefresh
        )
        val builder = NotificationCompat.Builder(context, payload.environment.channelId)
            .setSmallIcon(islandIconResId)
            .setContentTitle(state.title(context))
            .setContentText(state.content(context).ifBlank { " " })
            .setContentIntent(state.openPendingIntent)
            .setCategory(
                if (!isBlueArchiveNotification && state.running) {
                    NotificationCompat.CATEGORY_SERVICE
                } else {
                    NotificationCompat.CATEGORY_STATUS
                }
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(state.ongoing)
            .setOnlyAlertOnce(state.onlyAlertOnce)
            .setAutoCancel(false)
            .setRequestPromotedOngoing(state.running || state.ongoing)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        shortCriticalText?.let(builder::setShortCriticalText)
        buildFocusExtras(payload, islandIconResId)?.let(builder::addExtras)
        return builder.build()
    }

    private fun buildFocusExtras(payload: NotificationPayload, islandIconResId: Int) = runCatching {
        val state = payload.state
        val isBlueArchiveAp = McpNotificationPayload.isBaApServerName(state.serverName)
        val isBlueArchiveCafeVisit = McpNotificationPayload.isBaCafeVisitServerName(state.serverName)
        val isBlueArchiveArenaRefresh = McpNotificationPayload.isBaArenaRefreshServerName(state.serverName)
        val isBlueArchiveNotification =
            isBlueArchiveAp || isBlueArchiveCafeVisit || isBlueArchiveArenaRefresh
        val presentation = resolvePresentation(
            state = state,
            isBlueArchiveAp = isBlueArchiveAp,
            isBlueArchiveCafeVisit = isBlueArchiveCafeVisit,
            isBlueArchiveArenaRefresh = isBlueArchiveArenaRefresh
        )
        val lightLogoIcon = if (isBlueArchiveNotification) {
            Icon.createWithResource(context, islandIconResId)
        } else {
            Icon.createWithResource(context, islandIconResId).setTint(Color.BLACK)
        }
        val darkLogoIcon = if (isBlueArchiveNotification) {
            Icon.createWithResource(context, islandIconResId)
        } else {
            Icon.createWithResource(context, islandIconResId).setTint(Color.WHITE)
        }
        val actions = mutableListOf(
            IslandAction(
                key = "mcp_action_open",
                title = context.getString(R.string.common_open),
                pendingIntent = state.openPendingIntent,
                isHighlighted = true
            )
        ).apply {
            add(
                IslandAction(
                    key = "mcp_action_stop",
                    title = state.stopActionTitle(context),
                    pendingIntent = state.stopPendingIntent
                )
            )
        }

        FocusNotification.buildV3 {
            val lightLogoKey = createPicture("key_logo_light", lightLogoIcon)
            val darkLogoKey = createPicture("key_logo_dark", darkLogoIcon)
            val displayIconKey = darkLogoKey

            islandFirstFloat = true
            enableFloat = presentation.allowFloat
            updatable = true
            ticker = state.title(context)
            tickerPic = lightLogoKey
            tickerPicDark = darkLogoKey
            if (payload.settings.miIslandOuterGlow) {
                outEffectSrc = "outer_glow"
            }

            island {
                islandProperty = 1
                bigIslandArea {
                    imageTextInfoLeft {
                        type = 1
                        picInfo {
                            type = 1
                            pic = displayIconKey
                        }
                    }
                    imageTextInfoRight {
                        type = 3
                        textInfo {
                            title = presentation.rightTitle
                            content = presentation.rightContent
                            narrowFont = presentation.rightTitle.length >= 6 ||
                                (presentation.rightContent?.length ?: 0) >= 12
                        }
                    }
                }
                smallIslandArea {
                    picInfo {
                        type = 1
                        pic = displayIconKey
                    }
                }
            }

            baseInfo {
                type = 2
                title = state.title(context)
                content = state.content(context).ifBlank { " " }
            }

            picInfo {
                type = 1
                pic = lightLogoKey
                picDark = darkLogoKey
            }

            if (presentation.showTextButtons && actions.isNotEmpty()) {
                textButton {
                    actions.take(2).forEach { actionItem ->
                        addActionInfo {
                            val nativeAction = Notification.Action.Builder(
                                Icon.createWithResource(context, islandIconResId),
                                actionItem.title,
                                actionItem.pendingIntent
                            ).build()
                            action = createAction(actionItem.key, nativeAction)
                            actionTitle = actionItem.title
                            if (actionItem.isHighlighted) {
                                actionBgColor = HIGHLIGHT_BG_COLOR
                                actionBgColorDark = HIGHLIGHT_BG_COLOR
                                actionTitleColor = HIGHLIGHT_TITLE_COLOR
                                actionTitleColorDark = HIGHLIGHT_TITLE_COLOR
                            }
                        }
                    }
                }
            }
        }
    }.onFailure {
        AppLogger.e(TAG, "Build FocusNotification extras failed", it)
    }.getOrNull()

    private fun resolvePresentation(
        state: McpNotificationPayload,
        isBlueArchiveAp: Boolean,
        isBlueArchiveCafeVisit: Boolean,
        isBlueArchiveArenaRefresh: Boolean
    ): IslandPresentation {
        if (isBlueArchiveAp && state.running) {
            return IslandPresentation(
                allowFloat = true,
                showTextButtons = true,
                rightTitle = "${state.port.coerceAtLeast(0)}/${state.clients.coerceAtLeast(0)}"
            )
        }
        if (isBlueArchiveCafeVisit && state.running) {
            return IslandPresentation(
                allowFloat = true,
                showTextButtons = true,
                rightTitle = context.getString(R.string.ba_cafe_visit_notification_island_text)
            )
        }
        if (isBlueArchiveArenaRefresh && state.running) {
            return IslandPresentation(
                allowFloat = true,
                showTextButtons = true,
                rightTitle = context.getString(R.string.ba_arena_refresh_notification_island_text)
            )
        }
        if (state.running) {
            return IslandPresentation(
                allowFloat = false,
                showTextButtons = false,
                rightTitle = state.onlineText(context),
                rightContent = resolveDefaultEndpointSummary(state)
            )
        }
        return IslandPresentation(
            allowFloat = true,
            showTextButtons = true,
            rightTitle = state.statusText(context)
        )
    }

    private fun resolveShortCriticalText(
        state: McpNotificationPayload,
        isBlueArchiveAp: Boolean,
        isBlueArchiveCafeVisit: Boolean,
        isBlueArchiveArenaRefresh: Boolean
    ): String? {
        return when {
            !state.running -> state.statusText(context)
            isBlueArchiveAp -> state.shortText
            isBlueArchiveCafeVisit || isBlueArchiveArenaRefresh -> state.onlineText(context)
            else -> state.onlineText(context)
        }.takeIf { it.isNotBlank() }
    }

    private fun resolveDefaultEndpointSummary(state: McpNotificationPayload): String? {
        if (!state.running) return null
        val path = state.path.trim().ifBlank { "/mcp" }
        val port = state.port.coerceAtLeast(0)
        return if (path.length <= 12) {
            "$port $path"
        } else {
            port.toString()
        }
    }
}
