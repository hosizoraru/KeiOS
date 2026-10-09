@file:Suppress("FunctionName")

package os.kei.ui.page.main.student.section.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.R
import os.kei.ui.page.main.os.appLucideCloseIcon
import os.kei.ui.page.main.os.appLucideFullscreenIcon
import os.kei.ui.page.main.os.appLucidePauseIcon
import os.kei.ui.page.main.os.appLucidePlayIcon
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.os.appLucideUndoIcon
import os.kei.ui.page.main.os.appLucideVolume2Icon
import os.kei.ui.page.main.os.appLucideVolumeOffIcon
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.chrome.LiquidToolbar
import os.kei.ui.page.main.widget.chrome.LiquidToolbarAction
import os.kei.ui.page.main.widget.chrome.LiquidToolbarTextButton
import os.kei.ui.page.main.widget.glass.AppDropdownSelector
import os.kei.ui.page.main.widget.glass.GlassVariant

@Composable
internal fun GuideWebMemoryLobbyHeader(
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    hasBgm: Boolean = false,
    bgmMuted: Boolean = false,
    onToggleBgm: () -> Unit = {},
) {
    Box(Modifier.fillMaxWidth()) {
        LiquidToolbar(
            backdrop = backdrop,
            actions = listOf(
                LiquidToolbarAction(appLucideCloseIcon(), stringResource(R.string.common_close), onDismiss),
            ),
        )
        if (hasBgm) {
            LiquidToolbar(
                backdrop = backdrop,
                modifier = Modifier.align(Alignment.TopEnd),
                actions = listOf(LiquidToolbarAction(
                    if (bgmMuted) appLucideVolumeOffIcon() else appLucideVolume2Icon(),
                    stringResource(if (bgmMuted) R.string.ba_catalog_bgm_action_restore_volume else R.string.ba_catalog_bgm_action_mute),
                    onToggleBgm,
                )),
            )
        }
    }
}

@Composable
internal fun GuideWebMemoryLobbyControls(
    backdrop: Backdrop,
    playing: Boolean,
    actions: List<String>,
    selectedAction: String,
    onTogglePlayback: () -> Unit,
    onSelectAction: (String) -> Unit,
    onRetry: () -> Unit,
    onResetView: () -> Unit,
    onHideControls: () -> Unit,
    onActionsRequested: () -> Unit = {},
) {
    var expanded by remember(actions, playing) { mutableStateOf(false) }
    Row(
        // Actions arrive only after decoded readiness (and a drawn frame in compatible mode).
        // Keep the marker on chrome: the full-screen gesture layer occludes the media's semantics.
        Modifier.fillMaxWidth().then(if (actions.isNotEmpty()) Modifier.testTag(GuideWebMemoryLobbyReadyTag) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(AppChromeTokens.liquidToolbarGroupSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiquidToolbar(
            backdrop = backdrop,
            actions = listOf(
                LiquidToolbarAction(
                    if (playing) appLucidePauseIcon() else appLucidePlayIcon(),
                    stringResource(if (playing) R.string.guide_action_pause else R.string.guide_action_play),
                    onTogglePlayback,
                ),
                LiquidToolbarAction(appLucideRefreshIcon(), stringResource(R.string.guide_action_retry), onRetry),
            ),
        )
        AppDropdownSelector(
            selectedText = stringResource(R.string.guide_gallery_dynamic_lobby_actions),
            options = actions,
            selectedIndex = actions.indexOf(selectedAction),
            expanded = expanded,
            onExpandedChange = { expanded = it; if (it) onActionsRequested() },
            onSelectedIndexChange = { index -> actions.getOrNull(index)?.let(onSelectAction) },
            modifier = Modifier.weight(1f),
            backdrop = backdrop,
            variant = GlassVariant.Bar,
            minHeight = AppChromeTokens.liquidActionBarOuterHeight,
            anchorFillMaxWidth = true,
            anchorAlignment = Alignment.Center,
            enabled = playing && actions.isNotEmpty(),
            popupMaxHeight = 360.dp,
            anchorContent = { enabled, onClick ->
                LiquidToolbarTextButton(
                    backdrop = backdrop,
                    text = stringResource(R.string.guide_gallery_dynamic_lobby_actions),
                    onClick = onClick,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )
        LiquidToolbar(
            backdrop = backdrop,
            actions = listOf(
                LiquidToolbarAction(
                    appLucideUndoIcon(),
                    stringResource(R.string.guide_gallery_dynamic_lobby_reset_view),
                    onResetView,
                ),
                LiquidToolbarAction(
                    appLucideFullscreenIcon(),
                    stringResource(R.string.guide_gallery_dynamic_lobby_hide_controls),
                    onHideControls,
                ),
            ),
        )
    }
}

internal const val GuideWebMemoryLobbyReadyTag = "guide_lobby_ready"
