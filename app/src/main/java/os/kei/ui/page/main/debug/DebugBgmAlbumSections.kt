package os.kei.ui.page.main.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugBgmAlbumFooter(
    sectionTitle: String,
    trackCount: Int,
    offlineTrackCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 30.dp, end = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.debug_component_lab_album_footer_date),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = AppTypographyTokens.Body.fontSize,
            lineHeight = AppTypographyTokens.Body.lineHeight
        )
        Text(
            text = stringResource(R.string.debug_component_lab_section_footer_current, sectionTitle),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = AppTypographyTokens.Body.fontSize,
            lineHeight = AppTypographyTokens.Body.lineHeight
        )
        Text(
            text = stringResource(
                R.string.debug_component_lab_section_footer_state,
                trackCount,
                offlineTrackCount
            ),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = AppTypographyTokens.Body.fontSize,
            lineHeight = AppTypographyTokens.Body.lineHeight
        )
    }
}

@Composable
internal fun rememberDebugBgmDockSectionText(selectedDockKey: String): DebugBgmDockSectionText {
    val libraryLabel = stringResource(R.string.debug_component_lab_nav_library)
    return when (selectedDockKey) {
        DebugBgmDockKeys.Home -> DebugBgmDockSectionText(
            heroTitle = stringResource(R.string.debug_component_lab_section_home_title),
            heroMeta = stringResource(R.string.debug_component_lab_section_home_meta),
            footerTitle = stringResource(R.string.debug_component_lab_nav_home)
        )
        DebugBgmDockKeys.Discover -> DebugBgmDockSectionText(
            heroTitle = stringResource(R.string.debug_component_lab_section_discover_title),
            heroMeta = stringResource(R.string.debug_component_lab_section_discover_meta),
            footerTitle = stringResource(R.string.debug_component_lab_nav_discover)
        )
        DebugBgmDockKeys.Radio -> DebugBgmDockSectionText(
            heroTitle = stringResource(R.string.debug_component_lab_section_radio_title),
            heroMeta = stringResource(R.string.debug_component_lab_section_radio_meta),
            footerTitle = stringResource(R.string.debug_component_lab_nav_radio)
        )
        else -> DebugBgmDockSectionText(
            heroTitle = stringResource(R.string.debug_component_lab_album_artist),
            heroMeta = stringResource(R.string.debug_component_lab_album_meta),
            footerTitle = libraryLabel
        )
    }
}

internal data class DebugBgmDockSectionText(
    val heroTitle: String,
    val heroMeta: String,
    val footerTitle: String
)
