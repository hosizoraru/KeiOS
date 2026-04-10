package com.example.keios.mcp

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Icon
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.keios.R
import com.xzakota.hyper.notification.focus.FocusNotification

object McpIslandNotificationBuilder : McpNotificationBuilder {

    private const val TAG = "McpIslandBuilder"

    private data class IslandAction(
        val key: String,
        val title: String,
        val pendingIntent: PendingIntent,
        val isHighlighted: Boolean = false
    )

    private const val HIGHLIGHT_BG_COLOR = "#006EFF"
    private const val HIGHLIGHT_TITLE_COLOR = "#FFFFFF"

    override fun build(context: Context, payload: McpNotificationPayload): Notification {
        val builder = NotificationCompat.Builder(context, McpNotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(payload.title)
            .setContentText(payload.content.ifBlank { " " })
            .setContentIntent(payload.openPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(payload.ongoing)
            .setOnlyAlertOnce(payload.onlyAlertOnce)
            .setAutoCancel(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        buildFocusExtras(context, payload)?.let(builder::addExtras)
        return builder.build()
    }

    private fun buildFocusExtras(
        context: Context,
        payload: McpNotificationPayload
    ) = runCatching {
        val lightLogoIcon = Icon.createWithResource(context, R.drawable.ic_notification_logo).setTint(Color.BLACK)
        val darkLogoIcon = Icon.createWithResource(context, R.drawable.ic_notification_logo).setTint(Color.WHITE)

        val actions = mutableListOf(
            IslandAction(
                key = "mcp_action_open",
                title = "打开",
                pendingIntent = payload.openPendingIntent,
                isHighlighted = true
            )
        ).apply {
            if (payload.running) {
                add(
                    IslandAction(
                        key = "mcp_action_stop",
                        title = payload.stopActionTitle,
                        pendingIntent = payload.stopPendingIntent
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
            enableFloat = !payload.ongoing
            updatable = true
            ticker = payload.title
            tickerPic = lightLogoKey
            if (payload.outerGlow) {
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
                            this.title = payload.shortText.ifEmpty { payload.title }
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
                    this.title = payload.title
                    this.content = payload.content.ifBlank { " " }
                }
            } else {
                iconTextInfo {
                    this.title = payload.title
                    this.content = payload.content.ifBlank { " " }
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
                                Icon.createWithResource(context, R.drawable.ic_notification_logo),
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
