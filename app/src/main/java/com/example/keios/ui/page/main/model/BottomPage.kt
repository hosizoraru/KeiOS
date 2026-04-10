package com.example.keios.ui.page.main.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.keios.R

enum class BottomPage(
    val label: String,
    val icon: ImageVector? = null,
    @DrawableRes val iconRes: Int? = null,
    val keepOriginalColors: Boolean = false,
    val iconScale: Float = 1f,
) {
    Home("主页", iconRes = R.drawable.ic_kei_logo_color, keepOriginalColors = true, iconScale = 1.22f),
    System("系统", iconRes = R.drawable.ic_hyperos_symbol),
    Mcp("MCP", iconRes = R.drawable.ic_mcp_lobehub),
    GitHub("GitHub", iconRes = R.drawable.ic_github_invertocat),
    Ba("BA", iconRes = R.drawable.ic_ba_schale, iconScale = 1.16f)
}
