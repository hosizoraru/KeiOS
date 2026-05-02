package os.kei.ui.page.main.settings.section

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.core.prefs.CacheEntrySummary
import os.kei.ui.page.main.os.appLucidePackageIcon
import os.kei.ui.page.main.settings.support.SettingsCacheRow
import os.kei.ui.page.main.settings.support.SettingsGroupCard
import os.kei.ui.page.main.settings.support.SettingsToggleItem
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SettingsCacheSection(
    cacheDiagnosticsEnabled: Boolean,
    onCacheDiagnosticsChanged: (Boolean) -> Unit,
    cacheEntries: List<CacheEntrySummary>?,
    cacheEntriesLoading: Boolean,
    clearingAllCaches: Boolean,
    clearingCacheId: String?,
    onClearAllCaches: () -> Unit,
    onClearCache: (String) -> Unit,
    enabledCardColor: Color,
    disabledCardColor: Color
) {
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.90f)
    SettingsGroupCard(
        header = stringResource(R.string.settings_cache_header),
        title = stringResource(R.string.settings_cache_diagnostics_title),
        sectionIcon = appLucidePackageIcon(),
        containerColor = if (cacheDiagnosticsEnabled) enabledCardColor else disabledCardColor
    ) {
        SettingsToggleItem(
            title = stringResource(R.string.settings_cache_diagnostics_title),
            summary = if (cacheDiagnosticsEnabled) {
                stringResource(R.string.settings_cache_diagnostics_summary_enabled)
            } else {
                stringResource(R.string.settings_cache_diagnostics_summary_disabled)
            },
            checked = cacheDiagnosticsEnabled,
            onCheckedChange = onCacheDiagnosticsChanged,
            infoKey = stringResource(R.string.common_scope),
            infoValue = if (cacheDiagnosticsEnabled) {
                stringResource(R.string.settings_cache_scope_enabled)
            } else {
                stringResource(R.string.settings_cache_scope_disabled)
            }
        )
        when {
            !cacheDiagnosticsEnabled -> {
                Text(
                    text = stringResource(R.string.settings_cache_disabled_desc),
                    color = subtitleColor,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight
                )
            }
            cacheEntries == null && cacheEntriesLoading -> {
                Text(
                    text = stringResource(R.string.settings_cache_loading_desc),
                    color = subtitleColor,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight
                )
            }
            cacheEntries.isNullOrEmpty() -> {
                Text(
                    text = stringResource(R.string.settings_cache_empty_desc),
                    color = subtitleColor,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight
                )
            }
            else -> {
                AppLiquidTextButton(
                    backdrop = null,
                    variant = GlassVariant.SheetDangerAction,
                    text = if (clearingAllCaches) {
                        stringResource(R.string.common_processing)
                    } else {
                        stringResource(R.string.settings_cache_action_clear_all)
                    },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    textColor = MiuixTheme.colorScheme.error,
                    enabled = !clearingAllCaches && clearingCacheId == null,
                    onClick = onClearAllCaches
                )
                Spacer(modifier = Modifier.height(8.dp))
                cacheEntries.forEachIndexed { index, entry ->
                    SettingsCacheRow(
                        entry = entry,
                        clearing = clearingAllCaches || clearingCacheId == entry.id,
                        onClear = {
                            if (clearingAllCaches || clearingCacheId != null) return@SettingsCacheRow
                            onClearCache(entry.id)
                        }
                    )
                    if (index < cacheEntries.lastIndex) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}
