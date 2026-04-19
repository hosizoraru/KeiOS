package com.example.keios.mcp.framework.notification.builder

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Icon
import androidx.core.app.NotificationCompat
import com.example.keios.R
import com.example.keios.core.log.AppLogger
import com.example.keios.mcp.notification.McpNotificationPayload
import com.xzakota.hyper.notification.focus.FocusNotification

class MiIslandNotificationBuilder(
    private val context: Context
) : SessionNotificationBuilder {

    private data class IslandAction(
        val key: String,
        val title: String,
        val pendingIntent: PendingIntent,
        val isHighlighted: Boolean = false
    )

    private companion object {
        private const val TAG = "McpMiIslandBuilder"
        private const val HIGHLIGHT_BG_COLOR = "#006EFF"
        private const val HIGHLIGHT_TITLE_COLOR = "#FFFFFF"
        private const val ISLAND_ICON_RES_ID_DEFAULT = R.drawable.ic_kei_logo_island
        private const val ISLAND_ICON_RES_ID_AP = R.drawable.ic_ba_ap_island_shift
        private const val ISLAND_ICON_RES_ID_BA_CAFE_VISIT = R.drawable.ic_ba_schale
        private const val ISLAND_ICON_RES_ID_BA_ARENA_REFRESH = R.drawable.ic_ba_schale
    }

    override fun build(payload: NotificationPayload): Notification {
        val state = payload.state
        val isBlueArchiveAp = McpNotificationPayload.isBaApServerName(state.serverName)
        val isBlueArchiveCafeVisit = McpNotificationPayload.isBaCafeVisitServerName(state.serverName)
        val isBlueArchiveArenaRefresh = McpNotificationPayload.isBaArenaRefreshServerName(state.serverName)
        val islandIconResId = when {
            isBlueArchiveAp -> ISLAND_ICON_RES_ID_AP
            isBlueArchiveCafeVisit -> ISLAND_ICON_RES_ID_BA_CAFE_VISIT
            isBlueArchiveArenaRefresh -> ISLAND_ICON_RES_ID_BA_ARENA_REFRESH
            else -> ISLAND_ICON_RES_ID_DEFAULT
        }
        val builder = NotificationCompat.Builder(context, payload.environment.channelId)
            .setSmallIcon(islandIconResId)
            .setContentTitle(state.title(context))
            .setContentText(state.content(context).ifBlank { " " })
            .setContentIntent(state.openPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(state.ongoing)
            .setOnlyAlertOnce(state.onlyAlertOnce)
            .setAutoCancel(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        buildFocusExtras(payload, islandIconResId)?.let(builder::addExtras)
        return builder.build()
    }

    private fun buildFocusExtras(payload: NotificationPayload, islandIconResId: Int) = runCatching {
        val state = payload.state
        val isBlueArchiveAp = McpNotificationPayload.isBaApServerName(state.serverName)
        val isBlueArchiveCafeVisit = McpNotificationPayload.isBaCafeVisitServerName(state.serverName)
        val isBlueArchiveArenaRefresh = McpNotificationPayload.isBaArenaRefreshServerName(state.serverName)
        val isBlueArchiveNotification = isBlueArchiveAp || isBlueArchiveCafeVisit || isBlueArchiveArenaRefresh
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
        val rightTitle = if (isBlueArchiveAp && state.running) {
            "${state.port.coerceAtLeast(0)}/${state.clients.coerceAtLeast(0)}"
        } else if (isBlueArchiveCafeVisit && state.running) {
            context.getString(R.string.ba_cafe_visit_notification_island_text)
        } else if (isBlueArchiveArenaRefresh && state.running) {
            context.getString(R.string.ba_arena_refresh_notification_island_text)
        } else {
            state.shortText.ifEmpty { state.title(context) }
        }
        val actions = mutableListOf(
            IslandAction(
                key = "mcp_action_open",
                title = context.getString(R.string.common_open),
                pendingIntent = state.openPendingIntent,
                isHighlighted = true
            )
        ).apply {
            if (state.running) {
                add(
                    IslandAction(
                        key = "mcp_action_stop",
                        title = state.stopActionTitle(context),
                        pendingIntent = state.stopPendingIntent
                    )
                )
            }
        }

        FocusNotification.buildV3 {
            val lightLogoKey = createPicture("key_logo_light", lightLogoIcon)
            val darkLogoKey = createPicture("key_logo_dark", darkLogoIcon)
            val showAppIcon = false
            val displayIconKey = if (showAppIcon) lightLogoKey else darkLogoKey

            islandFirstFloat = true
            // Keep island clickable in status bar even for ongoing sessions.
            enableFloat = true
            updatable = true
            ticker = state.title(context)
            tickerPic = lightLogoKey
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
                            title = rightTitle
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

            if (!showAppIcon) {
                baseInfo {
                    type = 2
                    title = state.title(context)
                    content = state.content(context).ifBlank { " " }
                }
            } else {
                iconTextInfo {
                    title = state.title(context)
                    content = state.content(context).ifBlank { " " }
                    animIconInfo {
                        type = 0
                        src = displayIconKey
                    }
                }
            }

            picInfo {
                type = 1
                pic = lightLogoKey
                picDark = darkLogoKey
            }

            if (actions.isNotEmpty()) {
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
}
