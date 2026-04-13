package com.example.keios.ui.page.main.widget

import java.util.Locale

object StatusLabelText {
    const val FixedVisible = "固定显示"
    const val Running = "运行中"
    const val NotRunning = "未运行"
    const val Cached = "缓存"
    const val Checking = "检查中"
    const val Checked = "已检查"
    const val PendingCheck = "待检查"
    const val Syncing = "同步中"
    const val Synced = "已同步"
    const val PendingSync = "待同步"
    const val Authorized = "已授权"
    const val Unauthorized = "未授权"
    const val Activated = "已激活"

    fun selinux(rawValue: String): String {
        return when (rawValue.trim().lowercase(Locale.ROOT)) {
            "enforcing" -> "强制"
            "permissive" -> "宽容"
            "disabled" -> "已关闭"
            "n/a", "", "unknown" -> "未知"
            else -> rawValue.ifBlank { "未知" }
        }
    }
}
