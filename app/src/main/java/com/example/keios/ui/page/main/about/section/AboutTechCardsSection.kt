package com.example.keios.ui.page.main.about.section

import android.os.Build
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
import com.example.keios.core.system.ShizukuApiUtils
import com.example.keios.ui.page.main.about.ui.AboutCompactInfoRow
import com.example.keios.ui.page.main.about.ui.AboutSectionCard
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.icon.extended.Update

private data class AboutInfoRow(
    @StringRes val titleRes: Int,
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
    val rows = listOf(
        AboutInfoRow(R.string.about_row_kotlin, KotlinVersion.CURRENT.toString(), MiuixIcons.Regular.Tune),
        AboutInfoRow(R.string.about_row_gradle, BuildConfig.GRADLE_VERSION, MiuixIcons.Regular.Tune),
        AboutInfoRow(R.string.about_row_java, BuildConfig.JAVA_VERSION, MiuixIcons.Regular.Tune),
        AboutInfoRow(R.string.about_row_jvm_target, BuildConfig.JVM_TARGET_VERSION, MiuixIcons.Regular.Tune),
        AboutInfoRow(R.string.about_row_compile_sdk, BuildConfig.COMPILE_SDK_VERSION.toString(), MiuixIcons.Regular.Filter),
        AboutInfoRow(R.string.about_row_min_sdk, BuildConfig.MIN_SDK_VERSION.toString(), MiuixIcons.Regular.Filter),
        AboutInfoRow(R.string.about_row_target_sdk, BuildConfig.TARGET_SDK_VERSION.toString(), MiuixIcons.Regular.Filter),
        AboutInfoRow(R.string.about_row_runtime_api, Build.VERSION.SDK_INT.toString(), MiuixIcons.Regular.Filter)
    )
    AboutSectionCard(
        cardColor = cardColor,
        title = stringResource(R.string.about_card_build_title),
        subtitle = stringResource(R.string.about_card_build_subtitle),
        titleColor = accent,
        subtitleColor = subtitleColor,
        sectionIcon = MiuixIcons.Regular.Tune,
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
            MiuixIcons.Regular.GridView
        ),
        AboutInfoRow(
            R.string.about_row_declarative_ui,
            stringResource(R.string.about_value_declarative_ui, BuildConfig.COMPOSE_VERSION),
            MiuixIcons.Regular.Layers
        ),
        AboutInfoRow(
            R.string.about_row_navigation,
            stringResource(R.string.about_value_navigation, BuildConfig.NAVIGATION3_VERSION),
            MiuixIcons.Regular.ListView
        ),
        AboutInfoRow(
            R.string.about_row_ui_state_holder,
            stringResource(
                R.string.about_value_ui_state_holder,
                BuildConfig.LIFECYCLE_VIEWMODEL_COMPOSE_VERSION
            ),
            MiuixIcons.Regular.Notes
        ),
        AboutInfoRow(
            R.string.about_row_glass_material,
            stringResource(
                R.string.about_value_glass_material,
                BuildConfig.BACKDROP_VERSION,
                BuildConfig.CAPSULE_VERSION
            ),
            MiuixIcons.Regular.Album
        ),
        AboutInfoRow(
            R.string.about_row_permission_bridge,
            stringResource(R.string.about_value_permission_bridge, ShizukuApiUtils.API_VERSION),
            MiuixIcons.Regular.Lock
        )
    )
    AboutSectionCard(
        cardColor = cardColor,
        title = stringResource(R.string.about_card_ui_title),
        subtitle = stringResource(R.string.about_card_ui_subtitle),
        titleColor = accent,
        subtitleColor = subtitleColor,
        sectionIcon = MiuixIcons.Regular.GridView,
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
        AboutInfoRow(R.string.about_row_mcp_sdk, BuildConfig.MCP_KOTLIN_SDK_VERSION, MiuixIcons.Regular.Info),
        AboutInfoRow(R.string.about_row_ktor, BuildConfig.KTOR_VERSION, MiuixIcons.Regular.Settings),
        AboutInfoRow(R.string.about_row_okhttp, BuildConfig.OKHTTP_VERSION, MiuixIcons.Regular.Settings)
    )
    AboutSectionCard(
        cardColor = cardColor,
        title = stringResource(R.string.about_card_network_title),
        subtitle = stringResource(R.string.about_card_network_subtitle),
        titleColor = titleColor,
        subtitleColor = subtitleColor,
        sectionIcon = MiuixIcons.Regular.Settings,
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
        BuildConfig.BASE_VERSION_NAME,
        BuildConfig.GIT_COMMIT_COUNT,
        BuildConfig.GIT_SHORT_HASH,
        dirtySuffix,
        BuildConfig.VERSION_CODE
    )
    val rows = listOf(
        AboutInfoRow(
            R.string.about_row_github_repo_id,
            repoId,
            MiuixIcons.Regular.Layers
        ),
        AboutInfoRow(
            R.string.about_row_github_build_version,
            buildVersionText,
            MiuixIcons.Regular.Update
        ),
        AboutInfoRow(
            R.string.about_row_github_branch,
            BuildConfig.GIT_BRANCH_NAME,
            MiuixIcons.Regular.GridView
        ),
        AboutInfoRow(
            R.string.about_row_github_commit_count,
            BuildConfig.GIT_COMMIT_COUNT.toString(),
            MiuixIcons.Regular.ListView
        ),
        AboutInfoRow(
            R.string.about_row_github_commit_hash,
            BuildConfig.GIT_SHORT_HASH,
            MiuixIcons.Regular.Notes
        ),
        AboutInfoRow(
            R.string.about_row_github_worktree,
            worktreeState,
            MiuixIcons.Regular.Report
        ),
        AboutInfoRow(
            R.string.about_row_github_data_source,
            stringResource(R.string.about_value_github_data_source),
            MiuixIcons.Regular.Info
        ),
        AboutInfoRow(
            R.string.about_row_github_version_source,
            versionSourceText,
            MiuixIcons.Regular.Tune
        ),
        AboutInfoRow(
            R.string.about_row_github_strategy,
            stringResource(R.string.about_value_github_strategy),
            MiuixIcons.Regular.Filter
        ),
        AboutInfoRow(
            R.string.about_row_github_tracking,
            stringResource(R.string.about_value_github_tracking),
            MiuixIcons.Regular.Layers
        ),
        AboutInfoRow(
            R.string.about_row_github_notify,
            stringResource(R.string.about_value_github_notify),
            MiuixIcons.Regular.Report
        ),
        AboutInfoRow(
            R.string.about_row_broadcast_handler,
            stringResource(R.string.about_value_broadcast_handler),
            MiuixIcons.Regular.Refresh
        ),
        AboutInfoRow(
            R.string.about_row_foreground_info_handler,
            stringResource(R.string.about_value_foreground_info_handler),
            MiuixIcons.Regular.Info
        ),
        AboutInfoRow(
            R.string.about_row_background_jobs,
            stringResource(R.string.about_value_background_jobs),
            MiuixIcons.Regular.Tune
        ),
        AboutInfoRow(
            R.string.about_row_github_cache,
            stringResource(R.string.about_value_github_cache),
            MiuixIcons.Regular.Lock
        )
    )
    AboutSectionCard(
        cardColor = cardColor,
        title = stringResource(R.string.about_card_github_title),
        subtitle = stringResource(R.string.about_card_github_subtitle),
        titleColor = accent,
        subtitleColor = subtitleColor,
        sectionIcon = MiuixIcons.Regular.Layers,
        collapsible = true,
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            AboutCompactInfoRow(
                title = stringResource(R.string.about_label_project_url),
                value = projectUrl,
                titleIcon = MiuixIcons.Regular.Layers,
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
        AboutInfoRow(R.string.about_row_media3, BuildConfig.MEDIA3_VERSION, MiuixIcons.Regular.Album),
        AboutInfoRow(R.string.about_row_zoomimage, BuildConfig.ZOOMIMAGE_VERSION, MiuixIcons.Regular.GridView),
        AboutInfoRow(R.string.about_row_coil3, BuildConfig.COIL3_VERSION, MiuixIcons.Regular.Album),
        AboutInfoRow(R.string.about_row_documentfile, BuildConfig.DOCUMENTFILE_VERSION, MiuixIcons.Regular.ListView),
        AboutInfoRow(R.string.about_row_mmkv, BuildConfig.MMKV_VERSION, MiuixIcons.Regular.Lock)
    )
    AboutSectionCard(
        cardColor = cardColor,
        title = stringResource(R.string.about_card_media_title),
        subtitle = stringResource(R.string.about_card_media_subtitle),
        titleColor = accent,
        subtitleColor = subtitleColor,
        sectionIcon = MiuixIcons.Regular.Album,
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
