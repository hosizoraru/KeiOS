package com.example.keios.ui.page.main.model

import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Tasks

enum class BottomPage(
    val label: String,
    val icon: ImageVector
) {
    Home("主页", MiuixIcons.Regular.Tasks),
    System("系统", MiuixIcons.Regular.Info),
    Mcp("MCP", MiuixIcons.Regular.Tasks),
    GitHub("GitHub", MiuixIcons.Regular.Tasks),
    About("关于", MiuixIcons.Regular.Info),
    Settings("设置", MiuixIcons.Regular.Info)
}
