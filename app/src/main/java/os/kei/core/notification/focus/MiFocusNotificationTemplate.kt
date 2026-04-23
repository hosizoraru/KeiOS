package os.kei.core.notification.focus

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Bundle
import com.xzakota.hyper.notification.focus.FocusNotification
import os.kei.R

internal enum class MiFocusNotificationCompactMode {
    TEXT,
    PROGRESS
}

internal enum class MiFocusNotificationExpandedMode {
    BASE_INFO,
    ICON_TEXT
}

internal enum class MiFocusNotificationDisplayIconStyle {
    ORIGINAL,
    MONO_TINTED
}

internal data class MiFocusNotificationAction(
    val key: String,
    val title: String,
    val pendingIntent: PendingIntent,
    val iconResId: Int? = null,
    val isHighlighted: Boolean = false,
    val collapsePanel: Boolean? = null,
    val backgroundColor: String = "#006EFF",
    val titleColor: String = "#FFFFFF"
)

internal data class MiFocusNotificationSpec(
    val title: String,
    val content: String,
    val compactTitle: String,
    val compactContent: String? = null,
    val displayIconResId: Int,
    val expandedIconResId: Int = displayIconResId,
    val actionIconResId: Int = displayIconResId,
    val tickerIconResId: Int = R.drawable.ic_notification_logo,
    val compactMode: MiFocusNotificationCompactMode = MiFocusNotificationCompactMode.TEXT,
    val expandedMode: MiFocusNotificationExpandedMode = MiFocusNotificationExpandedMode.ICON_TEXT,
    val displayIconStyle: MiFocusNotificationDisplayIconStyle = MiFocusNotificationDisplayIconStyle.ORIGINAL,
    val allowFloat: Boolean = true,
    val islandFirstFloat: Boolean = true,
    val outerGlow: Boolean = false,
    val updatable: Boolean = true,
    val showNotification: Boolean? = null,
    val aodTitle: String = compactTitle,
    val progressPercent: Int = 0,
    val progressColor: String? = null,
    val progressTrackColor: String? = null,
    val showExpandedProgress: Boolean = false,
    val showHighlightColor: Boolean = false,
    val embedTextButtons: Boolean = false,
    val actions: List<MiFocusNotificationAction> = emptyList(),
    val narrowFont: Boolean? = null
)

internal object MiFocusNotificationTemplate {
    fun build(context: Context, spec: MiFocusNotificationSpec): Bundle {
        val compactTitle = spec.compactTitle.ifBlank { spec.title }
        val compactContent = spec.compactContent?.trim()?.takeIf { it.isNotEmpty() }
        val progressPercent = spec.progressPercent.coerceIn(0, 100)
        val narrowFont = spec.narrowFont ?: (
            compactTitle.length >= 6 || (compactContent?.length ?: 0) >= 12
        )

        return FocusNotification.buildV3 {
            val tickerLightKey = createPicture(
                "mi_focus_ticker_light",
                Icon.createWithResource(context, spec.tickerIconResId).setTint(Color.BLACK)
            )
            val tickerDarkKey = createPicture(
                "mi_focus_ticker_dark",
                Icon.createWithResource(context, spec.tickerIconResId).setTint(Color.WHITE)
            )
            val displayIcon = when (spec.displayIconStyle) {
                MiFocusNotificationDisplayIconStyle.ORIGINAL -> {
                    Icon.createWithResource(context, spec.displayIconResId)
                }

                MiFocusNotificationDisplayIconStyle.MONO_TINTED -> {
                    Icon.createWithResource(context, spec.displayIconResId).setTint(Color.WHITE)
                }
            }
            val displayKey = createPicture("mi_focus_display", displayIcon)
            val expandedKey = createPicture(
                "mi_focus_expanded",
                Icon.createWithResource(context, spec.expandedIconResId)
            )

            islandFirstFloat = spec.islandFirstFloat
            enableFloat = spec.allowFloat
            updatable = spec.updatable
            isShowNotification = spec.showNotification
            ticker = compactTitle
            tickerPic = tickerLightKey
            tickerPicDark = tickerDarkKey
            aodTitle = spec.aodTitle

            if (spec.outerGlow) {
                outEffectSrc = "outer_glow"
            }

            island {
                islandProperty = 1
                bigIslandArea {
                    imageTextInfoLeft {
                        type = 1
                        picInfo {
                            type = 1
                            pic = displayKey
                        }
                    }
                    when (spec.compactMode) {
                        MiFocusNotificationCompactMode.PROGRESS -> {
                            progressTextInfo {
                                progressInfo {
                                    progress = progressPercent
                                    isCCW = true
                                    spec.progressColor?.let { colorReach = it }
                                    spec.progressTrackColor?.let { colorUnReach = it }
                                }
                                textInfo {
                                    title = compactTitle
                                    content = compactContent
                                    this.narrowFont = narrowFont
                                    showHighlightColor = spec.showHighlightColor
                                }
                            }
                        }

                        MiFocusNotificationCompactMode.TEXT -> {
                            imageTextInfoRight {
                                type = 3
                                textInfo {
                                    title = compactTitle
                                    content = compactContent
                                    this.narrowFont = narrowFont
                                    showHighlightColor = spec.showHighlightColor
                                }
                            }
                        }
                    }
                }
                smallIslandArea {
                    picInfo {
                        type = 1
                        pic = displayKey
                    }
                }
            }

            when (spec.expandedMode) {
                MiFocusNotificationExpandedMode.BASE_INFO -> {
                    baseInfo {
                        type = 2
                        title = spec.title
                        content = spec.content.ifBlank { " " }
                    }
                }

                MiFocusNotificationExpandedMode.ICON_TEXT -> {
                    iconTextInfo {
                        title = spec.title
                        content = spec.content.ifBlank { " " }
                        animIconInfo {
                            type = 0
                            src = expandedKey
                        }
                    }
                }
            }

            if (spec.showExpandedProgress) {
                multiProgressInfo {
                    progress = progressPercent
                    spec.progressColor?.let { color = it }
                }
            }

            picInfo {
                type = 1
                pic = tickerLightKey
                picDark = tickerDarkKey
            }

            if (spec.embedTextButtons && spec.actions.isNotEmpty()) {
                textButton {
                    spec.actions.take(2).forEach { actionItem ->
                        addActionInfo {
                            type = 2
                            val nativeAction = Notification.Action.Builder(
                                Icon.createWithResource(
                                    context,
                                    actionItem.iconResId ?: spec.actionIconResId
                                ),
                                actionItem.title,
                                actionItem.pendingIntent
                            ).build()
                            action = createAction(actionItem.key, nativeAction)
                            actionTitle = actionItem.title
                            clickWithCollapse = actionItem.collapsePanel
                            if (actionItem.isHighlighted) {
                                actionBgColor = actionItem.backgroundColor
                                actionBgColorDark = actionItem.backgroundColor
                                actionTitleColor = actionItem.titleColor
                                actionTitleColorDark = actionItem.titleColor
                            }
                        }
                    }
                }
            }
        }
    }
}
