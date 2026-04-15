package com.example.keios.mcp

import android.app.PendingIntent
import android.content.Context
import com.example.keios.R

data class McpNotificationPayload(
    val serverName: String,
    val running: Boolean,
    val port: Int,
    val path: String,
    val clients: Int,
    val ongoing: Boolean,
    val onlyAlertOnce: Boolean,
    val openPendingIntent: PendingIntent,
    val stopPendingIntent: PendingIntent,
    val outerGlow: Boolean = true
) {
    private val normalizedServerName: String
        get() = serverName.trim().ifBlank { "KeiOS MCP" }

    private val isBlueArchiveAp: Boolean
        get() = normalizedServerName == "BlueArchive AP"

    fun title(context: Context): String {
        return if (running) {
            normalizedServerName
        } else {
            context.getString(R.string.mcp_notification_title_stopped, normalizedServerName)
        }
    }

    fun content(context: Context): String {
        return if (running) {
            if (isBlueArchiveAp) {
                context.getString(R.string.mcp_notification_content_ap, port, path, clients)
            } else {
                context.getString(R.string.mcp_notification_content_default, port, path, clients)
            }
        } else {
            context.getString(R.string.mcp_notification_content_tap_restart)
        }
    }

    fun statusText(context: Context): String {
        return if (running) {
            context.getString(R.string.mcp_status_running)
        } else {
            context.getString(R.string.mcp_status_stopped)
        }
    }

    fun onlineText(context: Context): String {
        return if (isBlueArchiveAp) {
            context.getString(R.string.mcp_online_text_ap_limit, clients)
        } else {
            context.getString(R.string.mcp_online_text_clients, clients)
        }
    }

    val shortText: String
        get() = if (running) {
            if (isBlueArchiveAp) {
                "${port.coerceAtLeast(0)}/${clients.coerceAtLeast(0)}"
            } else {
                "${statusDot} $clients"
            }
        } else {
            "${statusDot} 0"
        }

    val statusDot: String
        get() = when {
            !running -> "○"
            clients > 0 -> "●"
            else -> "◐"
        }

    fun stopActionTitle(context: Context): String {
        return if (isBlueArchiveAp) {
            context.getString(R.string.common_close)
        } else {
            context.getString(R.string.mcp_action_toggle_service)
        }
    }

    val expandedTitle: String
        get() = normalizedServerName

    fun expandedContent(context: Context): String {
        return if (running) {
            if (isBlueArchiveAp) {
                context.getString(R.string.mcp_notification_content_ap, port, path, clients)
            } else {
                context.getString(R.string.mcp_notification_content_default, port, path, clients)
            }
        } else {
            context.getString(R.string.mcp_notification_service_not_running)
        }
    }
}
