package com.example.keios.mcp

import android.app.PendingIntent

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

    val title: String
        get() = if (running) normalizedServerName else "$normalizedServerName 已停止"

    val content: String
        get() = if (running) {
            if (isBlueArchiveAp) {
                "AP $port · 阈值 $path · 上限 $clients"
            } else {
                "端口 $port · 路径 $path · 在线 $clients"
            }
        } else {
            "点击返回应用重新启动服务"
        }

    val statusText: String
        get() = if (running) "运行中" else "已停止"

    val onlineText: String
        get() = if (isBlueArchiveAp) "上限 $clients" else "在线 $clients"

    val shortText: String
        get() = if (running) {
            if (isBlueArchiveAp) "${statusDot} $port/$clients" else "${statusDot} $clients"
        } else {
            "${statusDot} 0"
        }

    val statusDot: String
        get() = when {
            !running -> "○"
            clients > 0 -> "●"
            else -> "◐"
        }

    val stopActionTitle: String
        get() = if (isBlueArchiveAp) "已读" else "停止"

    val expandedTitle: String
        get() = normalizedServerName

    val expandedContent: String
        get() = if (running) {
            if (isBlueArchiveAp) {
                "AP $port · 阈值 $path · 上限 $clients"
            } else {
                "端口 $port · 路径 $path · 在线 $clients"
            }
        } else {
            "服务未运行"
        }
}
