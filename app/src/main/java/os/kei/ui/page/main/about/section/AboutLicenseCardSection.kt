package os.kei.ui.page.main.about.section

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import os.kei.BuildConfig
import os.kei.R
import os.kei.ui.page.main.os.appLucideAppWindowIcon
import os.kei.ui.page.main.os.appLucideInfoIcon
import os.kei.ui.page.main.os.appLucideLayersIcon
import os.kei.ui.page.main.os.appLucideLockIcon
import os.kei.ui.page.main.os.appLucideMediaIcon
import os.kei.ui.page.main.os.appLucideNotesIcon
import os.kei.ui.page.main.os.appLucidePackageIcon
import os.kei.ui.page.main.os.osLucideSettingsIcon
import os.kei.ui.page.main.about.ui.AboutCompactInfoRow
import os.kei.ui.page.main.about.ui.AboutSectionCard

private data class AboutLicenseEntry(
    @get:StringRes val titleRes: Int,
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
            titleRes = R.string.about_license_row_androidx_runtime,
            value = stringResource(
                R.string.about_license_value_androidx_runtime,
                BuildConfig.CORE_KTX_VERSION,
                BuildConfig.ACTIVITY_COMPOSE_VERSION
            ),
            sourceUrl = stringResource(R.string.about_license_url_androidx_runtime),
            icon = appLucideAppWindowIcon()
        ),
        AboutLicenseEntry(
            titleRes = R.string.about_license_row_material_components,
            value = stringResource(R.string.about_license_value_material_components, BuildConfig.MATERIAL_VERSION),
            sourceUrl = stringResource(R.string.about_license_url_material_components),
            icon = appLucideLayersIcon()
        ),
        AboutLicenseEntry(
            titleRes = R.string.about_license_row_androidx_stack,
            value = stringResource(
                R.string.about_license_value_androidx_stack,
                BuildConfig.COMPOSE_VERSION,
                BuildConfig.NAVIGATION3_VERSION,
                BuildConfig.NAVIGATION_COMMON_VERSION,
                BuildConfig.LIFECYCLE_VIEWMODEL_COMPOSE_VERSION,
                BuildConfig.DOCUMENTFILE_VERSION,
                BuildConfig.METRICS_PERFORMANCE_VERSION,
                BuildConfig.CONSTRAINT_LAYOUT_COMPOSE_VERSION
            ),
            sourceUrl = stringResource(R.string.about_license_url_androidx_stack),
            icon = appLucideAppWindowIcon()
        ),
        AboutLicenseEntry(
            titleRes = R.string.about_license_row_lucide,
            value = stringResource(R.string.about_license_value_lucide, BuildConfig.LUCIDE_ICONS_VERSION),
            sourceUrl = stringResource(R.string.about_license_url_lucide),
            icon = appLucideLayersIcon()
        ),
        AboutLicenseEntry(
            titleRes = R.string.about_license_row_blue_archive_logos,
            value = stringResource(R.string.about_license_value_blue_archive_logos),
            sourceUrl = stringResource(R.string.about_license_url_blue_archive_logos),
            icon = appLucideLayersIcon()
        ),
        AboutLicenseEntry(
            titleRes = R.string.about_license_row_backdrop,
            value = stringResource(R.string.about_license_value_backdrop, BuildConfig.BACKDROP_VERSION),
            sourceUrl = stringResource(R.string.about_license_url_backdrop),
            icon = appLucideAppWindowIcon()
        ),
        AboutLicenseEntry(
            titleRes = R.string.about_license_row_capsule,
            value = stringResource(R.string.about_license_value_capsule, BuildConfig.CAPSULE_VERSION),
            sourceUrl = stringResource(R.string.about_license_url_capsule),
            icon = appLucideAppWindowIcon()
        ),
        AboutLicenseEntry(
            titleRes = R.string.about_license_row_shapes,
            value = stringResource(R.string.about_license_value_shapes, BuildConfig.SHAPES_VERSION),
            sourceUrl = stringResource(R.string.about_license_url_shapes),
            icon = appLucideAppWindowIcon()
        ),
        AboutLicenseEntry(
            titleRes = R.string.about_license_row_android_liquid_glass_view,
            value = stringResource(
                R.string.about_license_value_android_liquid_glass_view,
                BuildConfig.LIQUID_GLASS_VERSION
            ),
            sourceUrl = stringResource(R.string.about_license_url_android_liquid_glass_view),
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
                title = stringResource(R.string.about_license_row_scope),
                value = stringResource(R.string.about_license_value_scope),
                titleIcon = appLucideInfoIcon()
            )
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
