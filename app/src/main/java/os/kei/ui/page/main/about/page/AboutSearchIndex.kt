package os.kei.ui.page.main.about.page

import android.content.Context
import androidx.compose.runtime.Immutable
import os.kei.R
import os.kei.ui.page.main.about.model.AboutComponentEntry
import os.kei.ui.page.main.about.model.AboutPermissionEntry
import java.util.Locale

internal enum class AboutSearchCard {
    App,
    Release,
    GitHub,
    Runtime,
    Network,
    Media,
    Permission,
    Component,
    Build,
    Ui,
    Acknowledgements,
    ProjectLicense,
    License,
    Lab,
}

@Immutable
internal data class AboutSearchTarget(
    val card: AboutSearchCard,
    val category: AboutCategory,
    private val tokens: List<String>,
) {
    private val normalizedTokens: List<String> =
        tokens
            .asSequence()
            .map { it.normalizedAboutSearchToken() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()

    fun matches(query: String): Boolean {
        val normalizedQuery = query.normalizedAboutSearchToken()
        if (normalizedQuery.isBlank()) return true
        return normalizedTokens.any { token -> token.contains(normalizedQuery) }
    }
}

@Immutable
internal data class AboutSearchUiState(
    val active: Boolean = false,
    val matchingTargets: List<AboutSearchTarget> = emptyList(),
    val matchingCards: Set<AboutSearchCard> = emptySet(),
)

internal fun buildAboutSearchTargets(
    context: Context,
    appLabel: String,
    privilegeStatus: String,
    permissionEntries: List<AboutPermissionEntry>,
    componentEntries: List<AboutComponentEntry>,
): List<AboutSearchTarget> =
    listOf(
        AboutSearchTarget(
            card = AboutSearchCard.App,
            category = AboutCategory.Overview,
            tokens =
                aboutTokens(
                    context.getString(R.string.about_card_app_title),
                    context.getString(R.string.about_card_app_subtitle),
                    appLabel,
                    context.getString(R.string.about_label_name),
                    context.getString(R.string.about_label_package_name),
                    context.getString(R.string.about_label_version),
                    context.getString(R.string.about_label_build_type),
                    context.getString(R.string.about_label_commit_time),
                    context.getString(R.string.about_label_build_time),
                    context.getString(R.string.about_label_last_update),
                    context.getString(R.string.about_label_debug),
                    context.getString(R.string.about_label_test_only),
                    context.getString(R.string.about_label_api_level),
                    context.getString(R.string.about_label_security_patch),
                ),
        ),
        AboutSearchTarget(
            card = AboutSearchCard.Release,
            category = AboutCategory.Overview,
            tokens =
                aboutTokens(
                    context.getString(R.string.about_card_release_title),
                    context.getString(R.string.about_card_release_subtitle),
                    context.getString(R.string.about_release_row_version),
                    context.getString(R.string.about_release_row_tracking),
                    context.getString(R.string.about_release_row_lobby),
                    context.getString(R.string.about_release_row_models),
                    context.getString(R.string.about_release_row_rendering),
                    context.getString(R.string.about_release_row_input),
                    context.getString(R.string.about_release_row_startup),
                    context.getString(R.string.about_release_row_notifications),
                    context.getString(R.string.about_release_row_performance),
                    context.getString(R.string.about_release_row_next),
                    context.getString(R.string.about_release_value_version),
                    context.getString(R.string.about_release_value_tracking),
                    context.getString(R.string.about_release_value_lobby),
                    context.getString(R.string.about_release_value_models),
                    context.getString(R.string.about_release_value_rendering),
                    context.getString(R.string.about_release_value_input),
                    context.getString(R.string.about_release_value_startup),
                    context.getString(R.string.about_release_value_notifications),
                    context.getString(R.string.about_release_value_performance),
                    context.getString(R.string.about_release_value_next),
                ),
        ),
        AboutSearchTarget(
            card = AboutSearchCard.GitHub,
            category = AboutCategory.Overview,
            tokens =
                aboutTokens(
                    context.getString(R.string.about_card_github_title),
                    context.getString(R.string.about_card_github_subtitle),
                    context.getString(R.string.about_label_project_url),
                    context.getString(R.string.about_row_github_repo_id),
                    context.getString(R.string.about_row_github_anchor),
                    context.getString(R.string.about_row_github_build_version),
                    context.getString(R.string.about_row_github_branch),
                    context.getString(R.string.about_row_github_commit_count),
                    context.getString(R.string.about_row_github_total_commit_count),
                    context.getString(R.string.about_row_github_commit_hash),
                    context.getString(R.string.about_row_github_worktree),
                    context.getString(R.string.about_row_github_data_source),
                    context.getString(R.string.about_row_github_version_source),
                    context.getString(R.string.about_row_github_strategy),
                    context.getString(R.string.about_row_github_tracking),
                    context.getString(R.string.about_row_github_notify),
                    context.getString(R.string.about_row_broadcast_handler),
                    context.getString(R.string.about_row_foreground_info_handler),
                    context.getString(R.string.about_row_background_jobs),
                    context.getString(R.string.about_row_github_cache),
                ),
        ),
        AboutSearchTarget(
            card = AboutSearchCard.Runtime,
            category = AboutCategory.System,
            tokens =
                aboutTokens(
                    context.getString(R.string.about_card_runtime_title),
                    context.getString(R.string.about_card_runtime_subtitle),
                    privilegeStatus,
                    context.getString(R.string.about_runtime_label_notification_permission),
                    context.getString(R.string.about_runtime_label_selinux),
                    context.getString(R.string.about_runtime_label_uname),
                    context.getString(R.string.about_runtime_label_permission_count),
                    context.getString(R.string.about_runtime_label_component_count),
                ),
        ),
        AboutSearchTarget(
            card = AboutSearchCard.Network,
            category = AboutCategory.System,
            tokens =
                aboutTokens(
                    context.getString(R.string.about_card_network_title),
                    context.getString(R.string.about_card_network_subtitle),
                    context.getString(R.string.about_row_mcp_sdk),
                    context.getString(R.string.about_row_ktor),
                    context.getString(R.string.about_row_okhttp),
                    context.getString(R.string.about_row_focus_api),
                ),
        ),
        AboutSearchTarget(
            card = AboutSearchCard.Media,
            category = AboutCategory.System,
            tokens =
                aboutTokens(
                    context.getString(R.string.about_card_media_title),
                    context.getString(R.string.about_card_media_subtitle),
                    context.getString(R.string.about_row_media3),
                    context.getString(R.string.about_row_zoomimage),
                    context.getString(R.string.about_row_coil3),
                    context.getString(R.string.about_row_ucrop),
                    context.getString(R.string.about_row_documentfile),
                    context.getString(R.string.about_row_mmkv),
                ),
        ),
        AboutSearchTarget(
            card = AboutSearchCard.Permission,
            category = AboutCategory.System,
            tokens =
                aboutTokens(
                    context.getString(R.string.about_card_permission_title),
                    context.getString(R.string.about_card_permission_subtitle),
                    context.getString(R.string.about_label_status),
                    context.getString(R.string.about_permission_empty),
                    context.getString(R.string.about_permission_label_permission),
                    context.getString(R.string.about_permission_label_granted),
                    context.getString(R.string.about_permission_label_system_name),
                    context.getString(R.string.about_permission_label_purpose),
                    context.getString(R.string.about_permission_label_used_in),
                    *permissionEntries
                        .flatMap { entry ->
                            listOf(entry.title, entry.name, entry.purpose, entry.usedIn)
                        }.toTypedArray(),
                ),
        ),
        AboutSearchTarget(
            card = AboutSearchCard.Component,
            category = AboutCategory.System,
            tokens =
                aboutTokens(
                    context.getString(R.string.about_card_component_title),
                    context.getString(R.string.about_card_component_subtitle),
                    context.getString(R.string.about_label_status),
                    context.getString(R.string.about_component_empty),
                    context.getString(R.string.about_component_label_export_state),
                    context.getString(R.string.about_permission_label_purpose),
                    context.getString(R.string.about_permission_label_used_in),
                    context.getString(R.string.about_component_type_service),
                    context.getString(R.string.about_component_type_receiver),
                    context.getString(R.string.about_component_type_provider),
                    *componentEntries
                        .flatMap { entry ->
                            buildList {
                                add(entry.name)
                                add(entry.purpose)
                                add(entry.usedIn)
                                entry.extra.forEach { extra ->
                                    add(context.getString(extra.labelRes))
                                    add(extra.value)
                                }
                            }
                        }.toTypedArray(),
                ),
        ),
        AboutSearchTarget(
            card = AboutSearchCard.Build,
            category = AboutCategory.Tech,
            tokens =
                aboutTokens(
                    context.getString(R.string.about_card_build_title),
                    context.getString(R.string.about_card_build_subtitle),
                    context.getString(R.string.about_row_kotlin),
                    context.getString(R.string.about_row_gradle),
                    context.getString(R.string.about_row_java),
                    context.getString(R.string.about_row_jvm_target),
                    context.getString(R.string.about_row_compile_sdk),
                    context.getString(R.string.about_row_min_sdk),
                    context.getString(R.string.about_row_target_sdk),
                    context.getString(R.string.about_row_runtime_api),
                    context.getString(R.string.about_row_runtime_api_full),
                    context.getString(R.string.about_row_advanced_protection),
                    context.getString(R.string.about_row_profile_installer),
                    context.getString(R.string.about_row_baseline_profile_status),
                    context.getString(R.string.about_row_baseline_profile_artifacts),
                    context.getString(R.string.about_row_baseline_profile_rules),
                    context.getString(R.string.about_row_baseline_profile_journeys),
                    context.getString(R.string.about_value_baseline_profile_packaged),
                    context.getString(R.string.about_value_baseline_profile_variant_missing),
                    context.getString(R.string.about_value_baseline_profile_artifacts),
                    context.getString(R.string.about_value_baseline_profile_journeys),
                ),
        ),
        AboutSearchTarget(
            card = AboutSearchCard.Ui,
            category = AboutCategory.Tech,
            tokens =
                aboutTokens(
                    context.getString(R.string.about_card_ui_title),
                    context.getString(R.string.about_card_ui_subtitle),
                    context.getString(R.string.about_row_ui_framework),
                    context.getString(R.string.about_row_declarative_ui),
                    context.getString(R.string.about_row_navigation),
                    context.getString(R.string.about_row_ui_state_holder),
                    context.getString(R.string.about_row_glass_material),
                    context.getString(R.string.about_row_icon_set),
                    context.getString(R.string.about_row_permission_bridge),
                ),
        ),
        AboutSearchTarget(
            card = AboutSearchCard.Acknowledgements,
            category = AboutCategory.Tech,
            tokens = aboutTokens(
                context.getString(R.string.about_card_acknowledgements_title),
                context.getString(R.string.about_card_acknowledgements_subtitle),
                context.getString(R.string.about_acknowledgements_intro),
                context.getString(R.string.about_acknowledgements_wiki_role),
                context.getString(R.string.about_acknowledgements_wiki_name),
                context.getString(R.string.about_acknowledgements_models_role),
                context.getString(R.string.about_acknowledgements_models_name),
                context.getString(R.string.about_acknowledgements_wiki_url),
                context.getString(R.string.about_acknowledgements_models_url),
            ),
        ),
        AboutSearchTarget(
            card = AboutSearchCard.ProjectLicense,
            category = AboutCategory.Tech,
            tokens =
                aboutTokens(
                    context.getString(R.string.about_card_project_license_title),
                    context.getString(R.string.about_card_project_license_subtitle),
                    context.getString(R.string.about_project_license_row_name),
                    context.getString(R.string.about_project_license_row_spdx),
                    context.getString(R.string.about_project_license_row_file),
                    context.getString(R.string.about_project_license_row_copyright),
                    context.getString(R.string.about_project_license_row_url),
                ),
        ),
        AboutSearchTarget(
            card = AboutSearchCard.License,
            category = AboutCategory.Tech,
            tokens =
                aboutTokens(
                    context.getString(R.string.about_card_license_title),
                    context.getString(R.string.about_card_license_subtitle),
                    context.getString(R.string.about_license_row_scope),
                    context.getString(R.string.about_license_row_mix),
                    context.getString(R.string.about_license_row_compliance),
                    context.getString(R.string.about_license_row_webdav),
                    context.getString(R.string.about_license_row_miuix),
                    context.getString(R.string.about_license_row_androidx_runtime),
                    context.getString(R.string.about_license_row_material_components),
                    context.getString(R.string.about_license_row_androidx_stack),
                    context.getString(R.string.about_license_row_lucide),
                    context.getString(R.string.about_license_row_blue_archive_logos),
                    context.getString(R.string.about_license_row_backdrop),
                    context.getString(R.string.about_license_row_capsule),
                    context.getString(R.string.about_license_row_shapes),
                    context.getString(R.string.about_license_row_shizuku),
                    context.getString(R.string.about_license_row_hiddenapi_bypass),
                    context.getString(R.string.about_license_row_mmkv),
                    context.getString(R.string.about_license_row_mcp),
                    context.getString(R.string.about_license_row_network_stack),
                    context.getString(R.string.about_license_row_media_stack),
                    context.getString(R.string.about_license_row_package_installer),
                ),
        ),
        AboutSearchTarget(
            card = AboutSearchCard.Lab,
            category = AboutCategory.Lab,
            tokens =
                aboutTokens(
                    context.getString(R.string.about_card_component_lab_title),
                    context.getString(R.string.about_card_component_lab_subtitle),
                    context.getString(R.string.about_component_lab_row_entry),
                    context.getString(R.string.about_component_lab_row_scope),
                    context.getString(R.string.about_component_lab_row_activity),
                    context.getString(R.string.about_component_lab_row_components),
                    context.getString(R.string.debug_component_lab_title),
                    context.getString(R.string.debug_component_lab_liquid_catalog_title),
                    context.getString(R.string.debug_component_lab_liquid_row_components_value),
                ),
        ),
    )

private fun aboutTokens(vararg values: String): List<String> =
    values
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .toList()

private fun String.normalizedAboutSearchToken(): String = trim().lowercase(Locale.ROOT)
