package com.example.keios.mcp

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Icon
import com.example.keios.R
import com.xzakota.hyper.notification.focus.FocusNotification

object McpIslandNotificationBuilder {

    fun buildExtras(
        context: Context,
        title: String,
        content: String,
        shortText: String,
        statusText: String,
        onlineText: String,
        openPendingIntent: PendingIntent,
        stopPendingIntent: PendingIntent
    ) = runCatching {
        val lightIcon = Icon.createWithResource(context, R.drawable.ic_notification_logo).setTint(Color.BLACK)
        val darkIcon = Icon.createWithResource(context, R.drawable.ic_notification_logo).setTint(Color.WHITE)

        FocusNotification.buildV3 {
            val lightIconKey = createPicture("mcp_logo_light", lightIcon)
            val darkIconKey = createPicture("mcp_logo_dark", darkIcon)

            islandFirstFloat = true
            enableFloat = true
            updatable = true
            ticker = shortText
            tickerPic = lightIconKey
            outEffectSrc = "outer_glow"

            island {
                islandProperty = 1
                bigIslandArea {
                    imageTextInfoLeft {
                        type = 3
                        textInfo {
                            this.title = statusText
                        }
                    }
                    imageTextInfoRight {
                        type = 3
                        textInfo {
                            this.title = onlineText
                        }
                    }
                }
                smallIslandArea {
                    picInfo {
                        type = 1
                        pic = lightIconKey
                    }
                }
            }

            baseInfo {
                type = 2
                this.title = title
                this.content = if (content.isBlank()) " " else content
            }

            iconTextInfo {
                this.title = title
                this.content = if (content.isBlank()) " " else content
                animIconInfo {
                    type = 0
                    src = lightIconKey
                }
            }

            picInfo {
                type = 1
                pic = lightIconKey
                picDark = darkIconKey
            }

            textButton {
                addActionInfo {
                    val openAction = Notification.Action.Builder(
                        Icon.createWithResource(context, R.drawable.ic_notification_logo),
                        "打开",
                        openPendingIntent
                    ).build()
                    action = createAction("mcp_action_open", openAction)
                    actionTitle = "打开"
                    actionBgColor = "#006EFF"
                    actionBgColorDark = "#006EFF"
                    actionTitleColor = "#FFFFFF"
                    actionTitleColorDark = "#FFFFFF"
                }
                addActionInfo {
                    val stopAction = Notification.Action.Builder(
                        Icon.createWithResource(context, R.drawable.ic_notification_logo),
                        "停止",
                        stopPendingIntent
                    ).build()
                    action = createAction("mcp_action_stop", stopAction)
                    actionTitle = "停止"
                }
            }
        }
    }.getOrNull()
}
