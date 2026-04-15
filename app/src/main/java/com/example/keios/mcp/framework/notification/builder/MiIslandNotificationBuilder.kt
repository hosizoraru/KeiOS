package com.example.keios.mcp.framework.notification.builder

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Icon
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.keios.R
import com.xzakota.hyper.notification.focus.FocusNotification

class MiIslandNotificationBuilder(
    private val context: Context
) : InstallerNotificationBuilder {

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
        private const val ISLAND_ICON_RES_ID = R.drawable.ic_kei_logo_color
    }

    override fun build(payload: NotificationPayload): Notification {
        val state = payload.state
        val builder = NotificationCompat.Builder(context, payload.environment.channelId)
            .setSmallIcon(ISLAND_ICON_RES_ID)
            .setContentTitle(state.title)
            .setContentText(state.content.ifBlank { " " })
            .setContentIntent(state.openPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(state.ongoing)
            .setOnlyAlertOnce(state.onlyAlertOnce)
            .setAutoCancel(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        buildFocusExtras(payload)?.let(builder::addExtras)
        return builder.build()
    }

    private fun buildFocusExtras(payload: NotificationPayload) = runCatching {
        val state = payload.state
        val lightLogoIcon = Icon.createWithResource(context, ISLAND_ICON_RES_ID).setTint(Color.BLACK)
        val darkLogoIcon = Icon.createWithResource(context, ISLAND_ICON_RES_ID).setTint(Color.WHITE)
        val actions = mutableListOf(
            IslandAction(
                key = "mcp_action_open",
                title = "打开",
                pendingIntent = state.openPendingIntent,
                isHighlighted = true
            )
        ).apply {
            if (state.running) {
                add(
                    IslandAction(
                        key = "mcp_action_stop",
                        title = state.stopActionTitle,
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
            enableFloat = !state.ongoing
            updatable = true
            ticker = state.title
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
                            title = state.shortText.ifEmpty { state.title }
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
                    title = state.title
                    content = state.content.ifBlank { " " }
                }
            } else {
                iconTextInfo {
                    title = state.title
                    content = state.content.ifBlank { " " }
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
                                Icon.createWithResource(context, ISLAND_ICON_RES_ID),
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
        Log.e(TAG, "Build FocusNotification extras failed", it)
    }.getOrNull()
}
