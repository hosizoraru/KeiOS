package os.kei.ui.page.main.widget.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import os.kei.ui.page.main.widget.status.StatusPill
import os.kei.ui.page.main.widget.support.LocalTextCopyExpandedOverride
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun AppCardBodyColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = CardLayoutRhythm.cardContentPadding,
    verticalSpacing: Dp = CardLayoutRhythm.sectionGap,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        content = content
    )
}

@Composable
fun AppInfoListBody(
    modifier: Modifier = Modifier,
    verticalSpacing: Dp = CardLayoutRhythm.compactSectionGap,
    content: @Composable ColumnScope.() -> Unit
) {
    AppCardBodyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(0.dp),
        verticalSpacing = verticalSpacing,
        content = content
    )
}

@Preview(name = "Body Skeleton Light", showBackground = true, backgroundColor = 0xFFF3F4F6)
@Composable
private fun AppCardBodyColumnPreviewLight() {
    CompositionLocalProvider(LocalTextCopyExpandedOverride provides false) {
        MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
            AppOverviewCard(
                title = "List Body",
                subtitle = "Shared card rhythm",
                containerColor = Color(0xFFFFFFFF),
                borderColor = Color(0xFFD7DFEA),
                headerEndActions = {
                    StatusPill(label = "3 items", color = Color(0xFF2563EB))
                }
            ) {
                AppInfoListBody {
                    AppInfoRow(label = "Strategy", value = "Shared body layout")
                    AppInfoRow(label = "Note", value = "Multi-line values keep label and value rhythm aligned.")
                    AppSupportingBlock(text = "Settings, GitHub, and MCP cards can reuse this body layer.")
                }
            }
        }
    }
}
