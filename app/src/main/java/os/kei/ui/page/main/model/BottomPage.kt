package os.kei.ui.page.main.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import os.kei.R

enum class BottomPage(
    val label: String,
    val icon: ImageVector? = null,
    @get:DrawableRes val iconRes: Int? = null,
    val keepOriginalColors: Boolean = false,
    val iconScale: Float = 1f,
) {
    Home("Home", iconRes = R.drawable.ic_kei_logo_color, keepOriginalColors = true, iconScale = 1.22f),
    Os("OS", iconRes = R.drawable.ic_hyperos_symbol),
    Mcp("MCP", iconRes = R.drawable.ic_mcp_lobehub),
    GitHub("GitHub", iconRes = R.drawable.ic_github_invertocat),
    Ba("BA", iconRes = R.drawable.ic_ba_schale, iconScale = 1.16f)
}
