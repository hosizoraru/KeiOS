package os.kei.ui.page.main.settings.section

import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.os.appLucideMediaIcon
import os.kei.ui.page.main.settings.support.NON_HOME_BACKGROUND_OPACITY_DEFAULT
import os.kei.ui.page.main.settings.support.NON_HOME_BACKGROUND_OPACITY_KEY_POINTS
import os.kei.ui.page.main.settings.support.NON_HOME_BACKGROUND_OPACITY_MAGNET_THRESHOLD
import os.kei.ui.page.main.settings.support.NON_HOME_BACKGROUND_OPACITY_MAX
import os.kei.ui.page.main.settings.support.NON_HOME_BACKGROUND_OPACITY_MIN
import os.kei.ui.page.main.settings.support.SettingsActionItem
import os.kei.ui.page.main.settings.support.SettingsGroupCard
import os.kei.ui.page.main.settings.support.SettingsInfoItem
import os.kei.ui.page.main.settings.support.SettingsToggleItem
import os.kei.ui.page.main.settings.support.formatOpacityPercent
import os.kei.ui.page.main.widget.core.AppDualActionRow
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SettingsBackgroundSection(
    nonHomeBackgroundEnabled: Boolean,
    onNonHomeBackgroundEnabledChanged: (Boolean) -> Unit,
    nonHomeBackgroundUri: String,
    nonHomeBackgroundOpacity: Float,
    onNonHomeBackgroundOpacityChanged: (Float) -> Unit,
    backgroundPickerLauncher: ActivityResultLauncher<Array<String>>,
    onClearBackground: () -> Unit,
    enabledCardColor: Color,
    disabledCardColor: Color,
    onSliderInteractionChanged: (Boolean) -> Unit = {}
) {
    val backgroundGroupActive = nonHomeBackgroundEnabled || nonHomeBackgroundUri.isNotBlank()
    SettingsGroupCard(
        header = stringResource(R.string.settings_group_background_header),
        title = stringResource(R.string.settings_group_background_title),
        sectionIcon = appLucideMediaIcon(),
        containerColor = if (backgroundGroupActive) enabledCardColor else disabledCardColor
    ) {
        SettingsToggleItem(
            title = stringResource(R.string.settings_non_home_background_title),
            summary = if (nonHomeBackgroundEnabled) {
                stringResource(R.string.settings_non_home_background_summary_enabled)
            } else {
                stringResource(R.string.settings_non_home_background_summary_disabled)
            },
            checked = nonHomeBackgroundEnabled,
            onCheckedChange = onNonHomeBackgroundEnabledChanged,
            infoKey = stringResource(R.string.common_scope),
            infoValue = stringResource(R.string.settings_non_home_background_scope)
        )
        SettingsActionItem(
            title = stringResource(R.string.settings_non_home_background_image_title),
            summary = if (nonHomeBackgroundUri.isBlank()) {
                stringResource(R.string.settings_non_home_background_image_summary_empty)
            } else {
                stringResource(R.string.settings_non_home_background_image_summary_ready)
            }
        )
        AppDualActionRow(
            first = { modifier ->
                AppLiquidTextButton(
                    backdrop = null,
                    variant = GlassVariant.SheetPrimaryAction,
                    text = stringResource(R.string.settings_non_home_background_action_select),
                    modifier = modifier,
                    textColor = MiuixTheme.colorScheme.primary,
                    onClick = { backgroundPickerLauncher.launch(arrayOf("image/*")) }
                )
            },
            second = { modifier ->
                AppLiquidTextButton(
                    backdrop = null,
                    variant = GlassVariant.SheetDangerAction,
                    text = stringResource(R.string.settings_non_home_background_action_clear),
                    modifier = modifier,
                    textColor = MiuixTheme.colorScheme.error,
                    enabled = nonHomeBackgroundUri.isNotBlank(),
                    onClick = {
                        onClearBackground()
                    }
                )
            }
        )
        val opacityTitle = stringResource(R.string.settings_non_home_background_opacity_title)
        SettingsActionItem(
            title = opacityTitle,
            summary = stringResource(
                R.string.settings_non_home_background_opacity_summary,
                formatOpacityPercent(nonHomeBackgroundOpacity)
            )
        )
        SettingsLiquidKeyPointSlider(
            value = nonHomeBackgroundOpacity.coerceIn(
                NON_HOME_BACKGROUND_OPACITY_MIN,
                NON_HOME_BACKGROUND_OPACITY_MAX
            ),
            onValueChange = onNonHomeBackgroundOpacityChanged,
            valueRange = NON_HOME_BACKGROUND_OPACITY_MIN..NON_HOME_BACKGROUND_OPACITY_MAX,
            keyPoints = NON_HOME_BACKGROUND_OPACITY_KEY_POINTS,
            magnetThreshold = NON_HOME_BACKGROUND_OPACITY_MAGNET_THRESHOLD,
            enabled = nonHomeBackgroundEnabled,
            contentDescription = opacityTitle,
            onInteractionChanged = onSliderInteractionChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        )
        SettingsInfoItem(
            key = stringResource(R.string.common_note),
            value = stringResource(
                R.string.settings_non_home_background_opacity_default,
                formatOpacityPercent(NON_HOME_BACKGROUND_OPACITY_DEFAULT)
            )
        )
    }
}
