package os.kei.ui.page.main.about.section

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import os.kei.BuildConfig
import os.kei.R
import os.kei.core.platform.AndroidPlatformVersions
import os.kei.core.security.AdvancedProtectionCompat
import os.kei.core.system.ShizukuApiUtils
import os.kei.ui.page.main.os.appLucideAlertIcon
import os.kei.ui.page.main.os.appLucideAppWindowIcon
import os.kei.ui.page.main.os.appLucideConfigIcon
import os.kei.ui.page.main.os.appLucideFilterIcon
import os.kei.ui.page.main.os.appLucideInfoIcon
import os.kei.ui.page.main.os.appLucideLayersIcon
import os.kei.ui.page.main.os.appLucideListIcon
import os.kei.ui.page.main.os.appLucideLockIcon
import os.kei.ui.page.main.os.appLucideMediaIcon
import os.kei.ui.page.main.os.appLucideNotesIcon
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.os.appLucideVersionIcon
import os.kei.ui.page.main.os.osLucideSettingsIcon
import os.kei.ui.page.main.about.ui.AboutCompactInfoRow
import os.kei.ui.page.main.about.ui.AboutSectionCard

private data class AboutInfoRow(
    @get:StringRes val titleRes: Int,
    val value: String,
    val icon: ImageVector
)

@Composable
fun AboutBuildSdkCardSection(
    cardColor: Color,
    accent: Color,
    subtitleColor: Color,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val rows = listOf(
        AboutInfoRow(R.string.about_row_kotlin, KotlinVersion.CURRENT.toString(), appLucideConfigIcon()),
        AboutInfoRow(R.string.about_row_gradle, BuildConfig.GRADLE_VERSION, appLucideConfigIcon()),
        AboutInfoRow(R.string.about_row_java, BuildConfig.JAVA_VERSION, appLucideConfigIcon()),
        AboutInfoRow(R.string.about_row_jvm_target, BuildConfig.JVM_TARGET_VERSION, appLucideConfigIcon()),
        AboutInfoRow(R.string.about_row_compile_sdk, BuildConfig.COMPILE_SDK_VERSION.toString(), appLucideFilterIcon()),
        AboutInfoRow(R.string.about_row_min_sdk, BuildConfig.MIN_SDK_VERSION.toString(), appLucideFilterIcon()),
        AboutInfoRow(R.string.about_row_target_sdk, BuildConfig.TARGET_SDK_VERSION.toString(), appLucideFilterIcon()),
        AboutInfoRow(R.string.about_row_runtime_api, Build.VERSION.SDK_INT.toString(), appLucideFilterIcon()),
        AboutInfoRow(
            R.string.about_row_runtime_api_full,
            AndroidPlatformVersions.sdkIntFull.toString(),
            appLucideFilterIcon()
        ),
        AboutInfoRow(
            R.string.about_row_advanced_protection,
            when (AdvancedProtectionCompat.isEnabled(context)) {
                true -> stringResource(R.string.about_value_advanced_protection_enabled)
                false -> stringResource(R.string.about_value_advanced_protection_disabled)
                null -> stringResource(R.string.common_na)
            },
            appLucideLockIcon()
        )
    )
    AboutSectionCard(
        cardColor = cardColor,
        title = stringResource(R.string.about_card_build_title),
        subtitle = stringResource(R.string.about_card_build_subtitle),
        titleColor = accent,
        subtitleColor = subtitleColor,
        sectionIcon = appLucideConfigIcon(),
        collapsible = true,
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            rows.forEach { row ->
                AboutCompactInfoRow(
                    title = stringResource(row.titleRes),
                    value = row.value,
                    titleIcon = row.icon
                )
            }
        }
    }
}

@Composable
fun AboutUiFrameworkCardSection(
    cardColor: Color,
    accent: Color,
    subtitleColor: Color,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    val rows = listOf(
        AboutInfoRow(
            R.string.about_row_ui_framework,
            stringResource(R.string.about_value_ui_framework, BuildConfig.MIUIX_VERSION),
            appLucideAppWindowIcon()
        ),
        AboutInfoRow(
            R.string.about_row_declarative_ui,
            stringResource(R.string.about_value_declarative_ui, BuildConfig.COMPOSE_VERSION),
            appLucideLayersIcon()
        ),
        AboutInfoRow(
            R.string.about_row_navigation,
            stringResource(R.string.about_value_navigation, BuildConfig.NAVIGATION3_VERSION),
            appLucideListIcon()
        ),
        AboutInfoRow(
            R.string.about_row_ui_state_holder,
            stringResource(
                R.string.about_value_ui_state_holder,
                BuildConfig.LIFECYCLE_VIEWMODEL_COMPOSE_VERSION
            ),
            appLucideNotesIcon()
        ),
        AboutInfoRow(
            R.string.about_row_glass_material,
            stringResource(
                R.string.about_value_glass_material,
                BuildConfig.BACKDROP_VERSION,
                BuildConfig.CAPSULE_VERSION,
                BuildConfig.SHAPES_VERSION,
                BuildConfig.LIQUID_GLASS_VERSION
            ),
            appLucideMediaIcon()
        ),
        AboutInfoRow(
            R.string.about_row_icon_set,
            stringResource(
                R.string.about_value_icon_set,
                BuildConfig.LUCIDE_ICONS_VERSION
            ),
            appLucideAppWindowIcon()
        ),
        AboutInfoRow(
            R.string.about_row_permission_bridge,
            stringResource(
                R.string.about_value_permission_bridge,
                BuildConfig.SHIZUKU_VERSION,
                ShizukuApiUtils.API_VERSION
            ),
            appLucideLockIcon()
        )
    )
    AboutSectionCard(
        cardColor = cardColor,
        title = stringResource(R.string.about_card_ui_title),
        subtitle = stringResource(R.string.about_card_ui_subtitle),
        titleColor = accent,
        subtitleColor = subtitleColor,
        sectionIcon = appLucideAppWindowIcon(),
        collapsible = true,
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            rows.forEach { row ->
                AboutCompactInfoRow(
                    title = stringResource(row.titleRes),
                    value = row.value,
                    titleIcon = row.icon
                )
            }
        }
    }
}

@Composable
fun AboutNetworkServiceCardSection(
    cardColor: Color,
    titleColor: Color,
    subtitleColor: Color,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    val rows = listOf(
        AboutInfoRow(R.string.about_row_mcp_sdk, BuildConfig.MCP_KOTLIN_SDK_VERSION, appLucideInfoIcon()),
        AboutInfoRow(R.string.about_row_ktor, BuildConfig.KTOR_VERSION, osLucideSettingsIcon()),
        AboutInfoRow(R.string.about_row_okhttp, BuildConfig.OKHTTP_VERSION, osLucideSettingsIcon()),
        AboutInfoRow(R.string.about_row_focus_api, BuildConfig.FOCUS_API_VERSION, appLucideAlertIcon())
    )
    AboutSectionCard(
        cardColor = cardColor,
        title = stringResource(R.string.about_card_network_title),
        subtitle = stringResource(R.string.about_card_network_subtitle),
        titleColor = titleColor,
        subtitleColor = subtitleColor,
        sectionIcon = osLucideSettingsIcon(),
        collapsible = true,
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            rows.forEach { row ->
                AboutCompactInfoRow(
                    title = stringResource(row.titleRes),
                    value = row.value,
                    titleIcon = row.icon
                )
            }
        }
    }
}

@Composable
fun AboutGitHubCardSection(
    cardColor: Color,
    accent: Color,
    subtitleColor: Color,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOpenProjectUrl: (String) -> Unit
) {
    val projectUrl = stringResource(R.string.about_project_url)
    val repoId = parseGitHubRepoId(projectUrl)
    val dirtySuffix = if (BuildConfig.GIT_WORKTREE_DIRTY) "-dirty" else ""
    val worktreeState = if (BuildConfig.GIT_WORKTREE_DIRTY) {
        stringResource(R.string.about_value_github_worktree_dirty)
    } else {
        stringResource(R.string.about_value_github_worktree_clean)
    }
    val buildVersionText = stringResource(
        R.string.about_value_version_format,
        BuildConfig.VERSION_NAME,
        BuildConfig.VERSION_CODE
    )
    val versionSourceText = stringResource(
        R.string.about_value_github_version_source,
        BuildConfig.VERSION_ANCHOR_TAG,
        BuildConfig.BASE_VERSION_NAME,
        BuildConfig.NEXT_VERSION_NAME,
        BuildConfig.GIT_COMMIT_COUNT,
        BuildConfig.GIT_TOTAL_COMMIT_COUNT,
        BuildConfig.GIT_SHORT_HASH,
        dirtySuffix,
        BuildConfig.VERSION_CODE
    )
    val rows = listOf(
        AboutInfoRow(
            R.string.about_row_github_repo_id,
            repoId,
            appLucideLayersIcon()
        ),
        AboutInfoRow(
            R.string.about_row_github_anchor,
            BuildConfig.VERSION_ANCHOR_TAG,
            appLucideVersionIcon()
        ),
        AboutInfoRow(
            R.string.about_row_github_build_version,
            buildVersionText,
            appLucideVersionIcon()
        ),
        AboutInfoRow(
            R.string.about_row_github_branch,
            BuildConfig.GIT_BRANCH_NAME,
            appLucideAppWindowIcon()
        ),
        AboutInfoRow(
            R.string.about_row_github_commit_count,
            BuildConfig.GIT_COMMIT_COUNT.toString(),
            appLucideListIcon()
        ),
        AboutInfoRow(
            R.string.about_row_github_total_commit_count,
            BuildConfig.GIT_TOTAL_COMMIT_COUNT.toString(),
            appLucideListIcon()
        ),
        AboutInfoRow(
            R.string.about_row_github_commit_hash,
            BuildConfig.GIT_SHORT_HASH,
            appLucideNotesIcon()
        ),
        AboutInfoRow(
            R.string.about_row_github_worktree,
            worktreeState,
            appLucideAlertIcon()
        ),
        AboutInfoRow(
            R.string.about_row_github_data_source,
            stringResource(R.string.about_value_github_data_source, BuildConfig.VERSION_ANCHOR_TAG),
            appLucideInfoIcon()
        ),
        AboutInfoRow(
            R.string.about_row_github_version_source,
            versionSourceText,
            appLucideConfigIcon()
        ),
        AboutInfoRow(
            R.string.about_row_github_strategy,
            stringResource(R.string.about_value_github_strategy),
            appLucideFilterIcon()
        ),
        AboutInfoRow(
            R.string.about_row_github_tracking,
            stringResource(R.string.about_value_github_tracking),
            appLucideLayersIcon()
        ),
        AboutInfoRow(
            R.string.about_row_github_notify,
            stringResource(R.string.about_value_github_notify),
            appLucideAlertIcon()
        ),
        AboutInfoRow(
            R.string.about_row_broadcast_handler,
            stringResource(R.string.about_value_broadcast_handler),
            appLucideRefreshIcon()
        ),
        AboutInfoRow(
            R.string.about_row_foreground_info_handler,
            stringResource(R.string.about_value_foreground_info_handler),
            appLucideInfoIcon()
        ),
        AboutInfoRow(
            R.string.about_row_background_jobs,
            stringResource(R.string.about_value_background_jobs),
            appLucideConfigIcon()
        ),
        AboutInfoRow(
            R.string.about_row_github_cache,
            stringResource(R.string.about_value_github_cache),
            appLucideLockIcon()
        )
    )
    AboutSectionCard(
        cardColor = cardColor,
        title = stringResource(R.string.about_card_github_title),
        subtitle = stringResource(R.string.about_card_github_subtitle),
        titleColor = accent,
        subtitleColor = subtitleColor,
        sectionIcon = appLucideLayersIcon(),
        collapsible = true,
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            AboutCompactInfoRow(
                title = stringResource(R.string.about_label_project_url),
                value = projectUrl,
                titleIcon = appLucideLayersIcon(),
                valueColor = accent,
                onClick = { onOpenProjectUrl(projectUrl) }
            )
            rows.forEach { row ->
                AboutCompactInfoRow(
                    title = stringResource(row.titleRes),
                    value = row.value,
                    titleIcon = row.icon
                )
            }
        }
    }
}

private fun parseGitHubRepoId(projectUrl: String): String {
    val trimmed = projectUrl.trim().removeSuffix("/")
    val marker = "github.com/"
    val index = trimmed.indexOf(marker)
    if (index < 0) return trimmed
    val path = trimmed.substring(index + marker.length)
    if (path.isBlank()) return trimmed
    return path
}

@Composable
fun AboutMediaStorageCardSection(
    cardColor: Color,
    accent: Color,
    subtitleColor: Color,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    val rows = listOf(
        AboutInfoRow(R.string.about_row_media3, BuildConfig.MEDIA3_VERSION, appLucideMediaIcon()),
        AboutInfoRow(R.string.about_row_zoomimage, BuildConfig.ZOOMIMAGE_VERSION, appLucideAppWindowIcon()),
        AboutInfoRow(R.string.about_row_coil3, BuildConfig.COIL3_VERSION, appLucideMediaIcon()),
        AboutInfoRow(R.string.about_row_ucrop, BuildConfig.UCROP_VERSION, appLucideMediaIcon()),
        AboutInfoRow(R.string.about_row_documentfile, BuildConfig.DOCUMENTFILE_VERSION, appLucideListIcon()),
        AboutInfoRow(R.string.about_row_mmkv, BuildConfig.MMKV_VERSION, appLucideLockIcon())
    )
    AboutSectionCard(
        cardColor = cardColor,
        title = stringResource(R.string.about_card_media_title),
        subtitle = stringResource(R.string.about_card_media_subtitle),
        titleColor = accent,
        subtitleColor = subtitleColor,
        sectionIcon = appLucideMediaIcon(),
        collapsible = true,
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            rows.forEach { row ->
                AboutCompactInfoRow(
                    title = stringResource(row.titleRes),
                    value = row.value,
                    titleIcon = row.icon
                )
            }
        }
    }
}
