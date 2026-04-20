package com.example.keios.ui.page.main.settings.section

import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.ui.page.main.os.appLucideMediaIcon
import com.example.keios.ui.page.main.settings.support.NON_HOME_BACKGROUND_OPACITY_DEFAULT
import com.example.keios.ui.page.main.settings.support.NON_HOME_BACKGROUND_OPACITY_KEY_POINTS
import com.example.keios.ui.page.main.settings.support.NON_HOME_BACKGROUND_OPACITY_MAGNET_THRESHOLD
import com.example.keios.ui.page.main.settings.support.NON_HOME_BACKGROUND_OPACITY_MAX
import com.example.keios.ui.page.main.settings.support.NON_HOME_BACKGROUND_OPACITY_MIN
import com.example.keios.ui.page.main.settings.support.SettingsActionItem
import com.example.keios.ui.page.main.settings.support.SettingsGroupCard
import com.example.keios.ui.page.main.settings.support.SettingsInfoItem
import com.example.keios.ui.page.main.settings.support.SettingsToggleItem
import com.example.keios.ui.page.main.settings.support.formatOpacityPercent
import com.example.keios.ui.page.main.widget.core.AppDualActionRow
import com.example.keios.ui.page.main.widget.glass.GlassTextButton
import com.example.keios.ui.page.main.widget.glass.GlassVariant
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
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
    disabledCardColor: Color
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
                GlassTextButton(
                    backdrop = null,
                    variant = GlassVariant.SheetPrimaryAction,
                    text = stringResource(R.string.settings_non_home_background_action_select),
                    modifier = modifier,
                    textColor = MiuixTheme.colorScheme.primary,
                    onClick = { backgroundPickerLauncher.launch(arrayOf("image/*")) }
                )
            },
            second = { modifier ->
                GlassTextButton(
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
        SettingsActionItem(
            title = stringResource(R.string.settings_non_home_background_opacity_title),
            summary = stringResource(
                R.string.settings_non_home_background_opacity_summary,
                formatOpacityPercent(nonHomeBackgroundOpacity)
            )
        )
        Slider(
            value = nonHomeBackgroundOpacity.coerceIn(
                NON_HOME_BACKGROUND_OPACITY_MIN,
                NON_HOME_BACKGROUND_OPACITY_MAX
            ),
            onValueChange = onNonHomeBackgroundOpacityChanged,
            valueRange = NON_HOME_BACKGROUND_OPACITY_MIN..NON_HOME_BACKGROUND_OPACITY_MAX,
            showKeyPoints = true,
            keyPoints = NON_HOME_BACKGROUND_OPACITY_KEY_POINTS,
            magnetThreshold = NON_HOME_BACKGROUND_OPACITY_MAGNET_THRESHOLD,
            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
            enabled = nonHomeBackgroundEnabled,
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
