// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.example.keios.mcp.framework.notification.builder

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.keios.R
import com.example.keios.mcp.McpNotificationHelper
import com.example.keios.mcp.McpNotificationPayload
import kotlin.math.roundToInt

class ModernNotificationBuilder(
    private val context: Context
) : InstallerNotificationBuilder {

    private companion object {
        private const val PROGRESS_ACTIVE_COLOR = 0xFF2E7D32.toInt()
        private const val PROGRESS_IDLE_COLOR = 0xFF64748B.toInt()
        private const val ICON_DEFAULT = R.drawable.ic_kei_logo_color
        private const val ICON_AP = R.drawable.ba_ap_icon
        private const val ICON_BA_CAFE_VISIT = R.drawable.ic_ba_schale
    }

    private val baseNotificationBuilder by lazy {
        NotificationCompat.Builder(context, McpNotificationHelper.LIVE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_kei_logo_color)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
    }

    override fun build(payload: NotificationPayload): Notification {
        val state = payload.state
        val builder = createBaseBuilder(state)
        builder.addAction(0, context.getString(R.string.common_open), state.openPendingIntent)
        if (state.running) {
            builder.addAction(0, state.stopActionTitle(context), state.stopPendingIntent)
        }
        return builder.build()
    }

    private fun createBaseBuilder(state: McpNotificationPayload): NotificationCompat.Builder {
        val progress = computeProgress(state)
        val isBlueArchiveAp = McpNotificationPayload.isBaApServerName(state.serverName)
        val isBlueArchiveCafeVisit = McpNotificationPayload.isBaCafeVisitServerName(state.serverName)
        val iconRes = when {
            isBlueArchiveAp -> ICON_AP
            isBlueArchiveCafeVisit -> ICON_BA_CAFE_VISIT
            else -> ICON_DEFAULT
        }
        val baseBuilder = baseNotificationBuilder
        baseBuilder
            .clearActions()
            // Prevent state leakage from previous state
            .setContentText(null)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setOngoing(state.running || state.ongoing)
            .setSmallIcon(iconRes)
            .setContentIntent(state.openPendingIntent)

        val segmentColor = if (state.running) PROGRESS_ACTIVE_COLOR else PROGRESS_IDLE_COLOR
        val progressStyle = NotificationCompat.ProgressStyle()
            .setProgressSegments(
                listOf(
                    NotificationCompat.ProgressStyle.Segment(100).setColor(segmentColor)
                )
            )
            .setStyledByProgress(true)
            .setProgress(progress)

        baseBuilder.setContentTitle(state.title(context))
        baseBuilder.setContentText(state.content(context).ifBlank { " " })
        if (state.running) {
            val shortCriticalText = if (McpNotificationPayload.isBaCafeVisitServerName(state.serverName)) {
                state.onlineText(context)
            } else {
                state.shortText
            }
            baseBuilder.setShortCriticalText(shortCriticalText)
        }
        baseBuilder.setStyle(progressStyle)
        return baseBuilder
    }

    private fun computeProgress(state: McpNotificationPayload): Int {
        if (!state.running) return 0
        val isBlueArchiveAp = McpNotificationPayload.isBaApServerName(state.serverName)
        if (McpNotificationPayload.isBaCafeVisitServerName(state.serverName)) {
            return 100
        }
        if (isBlueArchiveAp) {
            val apLimit = state.clients.coerceAtLeast(1)
            val apCurrent = state.port.coerceAtLeast(0).coerceAtMost(apLimit)
            return ((apCurrent.toFloat() / apLimit.toFloat()) * 100f)
                .roundToInt()
                .coerceIn(0, 100)
        }
        return (state.clients.coerceAtLeast(0) * 24).coerceIn(8, 100)
    }
}
