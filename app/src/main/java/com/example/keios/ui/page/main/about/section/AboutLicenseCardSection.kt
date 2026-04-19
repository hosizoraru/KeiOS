package com.example.keios.ui.page.main.about.section

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.keios.BuildConfig
import com.example.keios.R
import com.example.keios.ui.page.main.os.appLucideAppWindowIcon
import com.example.keios.ui.page.main.os.appLucideInfoIcon
import com.example.keios.ui.page.main.os.appLucideLayersIcon
import com.example.keios.ui.page.main.os.appLucideLockIcon
import com.example.keios.ui.page.main.os.appLucideMediaIcon
import com.example.keios.ui.page.main.os.appLucideNotesIcon
import com.example.keios.ui.page.main.os.appLucidePackageIcon
import com.example.keios.ui.page.main.os.osLucideSettingsIcon
import com.example.keios.ui.page.main.about.ui.AboutCompactInfoRow
import com.example.keios.ui.page.main.about.ui.AboutSectionCard

private data class AboutLicenseEntry(
    @StringRes val titleRes: Int,
    val value: String,
    val sourceUrl: String,
    val icon: ImageVector
)

@Composable
fun AboutLicenseCardSection(
    cardColor: Color,
    accent: Color,
    subtitleColor: Color,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOpenSourceUrl: (String) -> Unit
) {
    val entries = listOf(
        AboutLicenseEntry(
            titleRes = R.string.about_license_row_miuix,
            value = stringResource(R.string.about_license_value_miuix, BuildConfig.MIUIX_VERSION),
            sourceUrl = stringResource(R.string.about_license_url_miuix),
            icon = appLucideAppWindowIcon()
        ),
        AboutLicenseEntry(
            titleRes = R.string.about_license_row_lucide,
            value = stringResource(R.string.about_license_value_lucide, BuildConfig.LUCIDE_ICONS_VERSION),
            sourceUrl = stringResource(R.string.about_license_url_lucide),
            icon = appLucideLayersIcon()
        ),
        AboutLicenseEntry(
            titleRes = R.string.about_license_row_liquid_glass,
            value = stringResource(
                R.string.about_license_value_liquid_glass,
                BuildConfig.LIQUID_GLASS_VERSION
            ),
            sourceUrl = stringResource(R.string.about_license_url_liquid_glass),
            icon = appLucideAppWindowIcon()
        ),
        AboutLicenseEntry(
            titleRes = R.string.about_license_row_shizuku,
            value = stringResource(R.string.about_license_value_shizuku, BuildConfig.SHIZUKU_VERSION),
            sourceUrl = stringResource(R.string.about_license_url_shizuku),
            icon = appLucideLockIcon()
        ),
        AboutLicenseEntry(
            titleRes = R.string.about_license_row_mmkv,
            value = stringResource(R.string.about_license_value_mmkv, BuildConfig.MMKV_VERSION),
            sourceUrl = stringResource(R.string.about_license_url_mmkv),
            icon = appLucidePackageIcon()
        ),
        AboutLicenseEntry(
            titleRes = R.string.about_license_row_mcp,
            value = stringResource(R.string.about_license_value_mcp, BuildConfig.MCP_KOTLIN_SDK_VERSION),
            sourceUrl = stringResource(R.string.about_license_url_mcp),
            icon = appLucideInfoIcon()
        ),
        AboutLicenseEntry(
            titleRes = R.string.about_license_row_network_stack,
            value = stringResource(
                R.string.about_license_value_network_stack,
                BuildConfig.KTOR_VERSION,
                BuildConfig.OKHTTP_VERSION,
                BuildConfig.FOCUS_API_VERSION
            ),
            sourceUrl = stringResource(R.string.about_license_url_network_stack),
            icon = osLucideSettingsIcon()
        ),
        AboutLicenseEntry(
            titleRes = R.string.about_license_row_media_stack,
            value = stringResource(
                R.string.about_license_value_media_stack,
                BuildConfig.MEDIA3_VERSION,
                BuildConfig.COIL3_VERSION,
                BuildConfig.ZOOMIMAGE_VERSION,
                BuildConfig.UCROP_VERSION
            ),
            sourceUrl = stringResource(R.string.about_license_url_media_stack),
            icon = appLucideMediaIcon()
        )
    )

    AboutSectionCard(
        cardColor = cardColor,
        title = stringResource(R.string.about_card_license_title),
        subtitle = stringResource(R.string.about_card_license_subtitle),
        titleColor = accent,
        subtitleColor = subtitleColor,
        sectionIcon = appLucideLockIcon(),
        collapsible = true,
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            AboutCompactInfoRow(
                title = stringResource(R.string.about_license_row_mix),
                value = stringResource(R.string.about_license_value_mix),
                titleIcon = appLucideInfoIcon()
            )
            AboutCompactInfoRow(
                title = stringResource(R.string.about_license_row_compliance),
                value = stringResource(R.string.about_license_value_compliance),
                titleIcon = appLucideNotesIcon()
            )
            entries.forEach { entry ->
                AboutCompactInfoRow(
                    title = stringResource(entry.titleRes),
                    value = entry.value,
                    titleIcon = entry.icon,
                    valueColor = accent,
                    onClick = { onOpenSourceUrl(entry.sourceUrl) }
                )
            }
        }
    }
}
