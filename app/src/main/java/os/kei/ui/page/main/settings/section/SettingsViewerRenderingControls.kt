package os.kei.ui.page.main.settings.section

import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import os.kei.R
import os.kei.ui.page.main.settings.support.SettingsPickerItem
import os.kei.ui.page.main.student.rendering.GuideViewerRendering
import os.kei.ui.page.main.widget.glass.AppDropdownSelector
import os.kei.ui.page.main.widget.glass.GlassVariant

@Composable
internal fun SettingsViewerRenderingControls(
    lobby: GuideViewerRendering, model: GuideViewerRendering,
    onLobbyChanged: (GuideViewerRendering) -> Unit, onModelChanged: (GuideViewerRendering) -> Unit,
) {
    RenderingPicker(stringResource(R.string.settings_lobby_rendering), lobby, onLobbyChanged)
    RenderingPicker(stringResource(R.string.settings_model_rendering), model, onModelChanged)
}

@Composable
private fun RenderingPicker(title: String, selected: GuideViewerRendering, onSelected: (GuideViewerRendering) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val labels = listOf(stringResource(R.string.settings_viewer_rendering_system), stringResource(R.string.settings_viewer_rendering_compatible))
    SettingsPickerItem(title = title, summary = stringResource(
        if (selected == GuideViewerRendering.SystemWebView) R.string.settings_viewer_rendering_system_summary
        else R.string.settings_viewer_rendering_compatible_summary,
    )) {
        AppDropdownSelector(selectedText = labels[selected.ordinal], options = labels, selectedIndex = selected.ordinal,
            expanded = expanded, onExpandedChange = { expanded = it },
            onSelectedIndexChange = { onSelected(GuideViewerRendering.entries[it]) }, variant = GlassVariant.SheetAction)
    }
}
