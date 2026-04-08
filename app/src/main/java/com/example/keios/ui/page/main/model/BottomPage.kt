package com.example.keios.ui.page.main.model

import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.CloudFill
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Tasks
import top.yukonga.miuix.kmp.icon.extended.Tune

enum class BottomPage(
    val label: String,
    val icon: ImageVector
) {
    Home("主页", MiuixIcons.Regular.Tasks),
    System("系统", MiuixIcons.Regular.Tune),
    Mcp("MCP", MiuixIcons.Regular.CloudFill),
    GitHub("GitHub", MiuixIcons.Regular.Link),
    About("关于", MiuixIcons.Regular.Info)
}
