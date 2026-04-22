package os.kei.mcp.framework.notification.builder

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import androidx.core.app.NotificationCompat
import com.xzakota.hyper.notification.focus.FocusNotification
import com.xzakota.hyper.notification.island.model.TextInfo
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

    private data class IslandIconBundle(
        val lightIcon: Icon,
        val darkIcon: Icon
    )

    private companion object {
        private const val TAG = "McpMiIslandBuilder"
        private const val HIGHLIGHT_BG_COLOR = "#006EFF"
        private const val HIGHLIGHT_TITLE_COLOR = "#FFFFFF"
        private const val ISLAND_ICON_RES_ID_DEFAULT = R.drawable.ic_notification_logo
        private const val ISLAND_ICON_RES_ID_AP = R.drawable.ic_ba_ap_island_notification
        private const val ISLAND_ICON_RES_ID_BA_CAFE_VISIT = R.drawable.ic_ba_schale_island
        private const val ISLAND_ICON_RES_ID_BA_ARENA_REFRESH = R.drawable.ic_ba_schale_island
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
        val iconBundle = if (isBlueArchiveNotification) {
            IslandIconBundle(
                lightIcon = Icon.createWithResource(context, islandIconResId),
                darkIcon = Icon.createWithResource(context, islandIconResId)
            )
        } else {
            buildDefaultLauncherIslandIcons()
        }
        val lightLogoIcon = iconBundle.lightIcon
        val darkLogoIcon = iconBundle.darkIcon
        val rightTitle = if (isBlueArchiveAp && state.running) {
            "${state.port.coerceAtLeast(0)}/${state.clients.coerceAtLeast(0)}"
        } else if (isBlueArchiveCafeVisit && state.running) {
            context.getString(R.string.ba_cafe_visit_notification_island_text)
        } else if (isBlueArchiveArenaRefresh && state.running) {
            context.getString(R.string.ba_arena_refresh_notification_island_text)
        } else {
            state.shortText.ifEmpty { state.title(context) }
        }
        val compactContent = if (state.running) {
            rightTitle
        } else {
            state.statusText(context)
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
            val displayIconKey = darkLogoKey
            val openActionKey = createAction(
                "mcp_reopen",
                Notification.Action.Builder(
                    Icon.createWithResource(context, islandIconResId),
                    context.getString(R.string.common_open),
                    state.openPendingIntent
                ).build()
            )

            islandFirstFloat = true
            // Keep island clickable in status bar even for ongoing sessions.
            enableFloat = true
            updatable = true
            showSmallIcon = false
            reopen = openActionKey
            ticker = state.title(context)
            tickerPic = lightLogoKey
            tickerPicDark = darkLogoKey
            if (payload.settings.miIslandOuterGlow) {
                outEffectSrc = "outer_glow"
            }

            island {
                islandProperty = 1
                bigIslandArea {
                    if (isBlueArchiveNotification) {
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
                                title = if (isBlueArchiveAp && state.running) {
                                    rightTitle
                                } else {
                                    state.title(context)
                                }
                            }
                        }
                    } else {
                        picInfo {
                            type = 1
                            pic = displayIconKey
                        }
                        textInfo = TextInfo().apply {
                            title = state.title(context)
                            content = compactContent
                            narrowFont = true
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

            if (actions.isNotEmpty()) {
                textButton {
                    actions.take(2).forEach { actionItem ->
                        addActionInfo {
                            action = if (actionItem.key == "mcp_action_open") {
                                openActionKey
                            } else {
                                val nativeAction = Notification.Action.Builder(
                                    Icon.createWithResource(context, islandIconResId),
                                    actionItem.title,
                                    actionItem.pendingIntent
                                ).build()
                                createAction(actionItem.key, nativeAction)
                            }
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

    private fun buildDefaultLauncherIslandIcons(): IslandIconBundle {
        val drawable = resolveDefaultIslandDrawable().mutate()
        val sizePx = (context.resources.displayMetrics.density * 48f).toInt().coerceAtLeast(72)
        val insetPx = (sizePx * 0.18f).toInt().coerceAtLeast(6)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(
            insetPx,
            insetPx,
            sizePx - insetPx,
            sizePx - insetPx
        )
        drawable.draw(canvas)
        val icon = Icon.createWithAdaptiveBitmap(bitmap)
        return IslandIconBundle(
            lightIcon = icon,
            darkIcon = icon
        )
    }

    private fun resolveDefaultIslandDrawable(): Drawable {
        return context.packageManager.getApplicationIcon(context.applicationInfo)
    }
}
