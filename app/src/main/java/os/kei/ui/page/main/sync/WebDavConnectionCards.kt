@file:Suppress("FunctionName")

package os.kei.ui.page.main.sync

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.os.appLucideDatabaseIcon
import os.kei.ui.page.main.os.appLucideFolderIcon
import os.kei.ui.page.main.os.appLucideLockIcon
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.settings.support.SettingsGroupCard
import os.kei.ui.page.main.settings.support.SettingsInfoItem
import os.kei.ui.page.main.settings.support.SettingsNavigationItem
import os.kei.ui.page.main.settings.support.SettingsPickerItem
import os.kei.ui.page.main.widget.core.AppDualActionRow
import os.kei.ui.page.main.widget.core.AppOverviewCard
import os.kei.ui.page.main.widget.core.AppOverviewPill
import os.kei.ui.page.main.widget.core.AppOverviewPillFlow
import os.kei.ui.page.main.widget.core.AppSupportingBlock
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppDropdownSelector
import os.kei.ui.page.main.widget.glass.AppStandaloneLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.status.AppStatusColors
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Connection setup is intentionally split into task-shaped cards. The overview owns provider
 * selection, the form cards own editable values, and the final card owns validation/persistence.
 */
@Composable
internal fun WebDavConnectionOverviewCard(
    state: WebDavSyncUiState,
    cardColor: Color,
    providerExpanded: Boolean,
    onProviderExpandedChange: (Boolean) -> Unit,
    onSelectProvider: (WebDavProvider) -> Unit,
) {
    val providerEntries = remember { WebDavProvider.entries.toList() }
    val providerLabels =
        providerEntries.map { provider ->
            when (provider) {
                WebDavProvider.Jianguoyun ->
                    stringResource(R.string.webdav_sync_provider_jianguoyun)

                WebDavProvider.Custom ->
                    stringResource(R.string.webdav_sync_provider_custom)
            }
        }
    val selectedProviderIndex = providerEntries.indexOf(state.provider).coerceAtLeast(0)
    val providerSummary =
        when (state.provider) {
            WebDavProvider.Jianguoyun ->
                stringResource(R.string.webdav_sync_provider_jianguoyun_desc)

            WebDavProvider.Custom ->
                stringResource(R.string.webdav_sync_provider_custom_desc)
        }
    val status = webDavConnectionVisualStatus(state)

    AppOverviewCard(
        title = stringResource(R.string.webdav_sync_workspace_title),
        subtitle = providerSummary,
        containerColor = cardColor,
        startAction = {
            top.yukonga.miuix.kmp.basic.Icon(
                imageVector = appLucideDatabaseIcon(),
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onBackground,
            )
        },
        headerEndActions = {
            StatusPill(
                label = status.label,
                color = status.color,
            )
        },
        contentVerticalSpacing = CardLayoutRhythm.compactSectionGap,
    ) {
        AppOverviewPillFlow(
            pills =
                listOf(
                    AppOverviewPill(
                        label = providerLabels.getOrElse(selectedProviderIndex) { state.provider.name },
                        color = MiuixTheme.colorScheme.primary,
                    ),
                    AppOverviewPill(
                        label =
                            stringResource(
                                if (state.canConnect) {
                                    R.string.webdav_sync_credentials_ready
                                } else {
                                    R.string.webdav_sync_credentials_incomplete
                                },
                            ),
                        color =
                            if (state.canConnect) {
                                AppStatusColors.Fresh
                            } else {
                                MiuixTheme.colorScheme.onBackgroundVariant
                            },
                    ),
                ),
        )
        SettingsPickerItem(
            title = stringResource(R.string.webdav_sync_provider_label),
            summary = stringResource(R.string.webdav_sync_provider_picker_summary),
            onClick = { onProviderExpandedChange(true) },
            trailing = {
                AppDropdownSelector(
                    selectedText =
                        providerLabels.getOrElse(selectedProviderIndex) {
                            state.provider.name
                        },
                    options = providerLabels,
                    selectedIndex = selectedProviderIndex,
                    expanded = providerExpanded,
                    onExpandedChange = onProviderExpandedChange,
                    onSelectedIndexChange = { index ->
                        providerEntries.getOrNull(index)?.let(onSelectProvider)
                        onProviderExpandedChange(false)
                    },
                    popupMaxWidth = 220.dp,
                    popupMatchAnchorWidth = true,
                )
            },
        )
    }
}

@Composable
internal fun WebDavCredentialsCard(
    state: WebDavSyncUiState,
    cardColor: Color,
    onUpdateUsername: (String) -> Unit,
    onUpdateAppPassword: (String) -> Unit,
    onTogglePasswordVisible: () -> Unit,
) {
    SettingsGroupCard(
        header = stringResource(R.string.webdav_sync_section_connection),
        title = stringResource(R.string.webdav_sync_credentials_title),
        subtitle = stringResource(R.string.webdav_sync_credentials_summary),
        sectionIcon = appLucideLockIcon(),
        containerColor = cardColor,
    ) {
        WebDavFieldLabel(stringResource(R.string.webdav_sync_username))
        WebDavLiquidTextField(
            value = state.username,
            onValueChange = onUpdateUsername,
            label = stringResource(R.string.webdav_sync_username_placeholder),
        )
        WebDavFieldLabel(stringResource(R.string.webdav_sync_app_password))
        WebDavLiquidTextField(
            value = state.appPassword,
            onValueChange = onUpdateAppPassword,
            label = stringResource(R.string.webdav_sync_password_placeholder),
            visualTransformation =
                if (state.passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
        )
        SettingsNavigationItem(
            title =
                stringResource(
                    if (state.passwordVisible) {
                        R.string.webdav_sync_password_hide
                    } else {
                        R.string.webdav_sync_password_show
                    },
                ),
            summary =
                if (state.provider == WebDavProvider.Jianguoyun) {
                    stringResource(R.string.webdav_sync_jianguoyun_password_hint)
                } else {
                    stringResource(R.string.webdav_sync_password_visibility_summary)
                },
            onClick = onTogglePasswordVisible,
        )
    }
}

@Composable
internal fun WebDavRemoteWorkspaceCard(
    state: WebDavSyncUiState,
    cardColor: Color,
    onUpdateServerUrl: (String) -> Unit,
    onUpdateRemoteDir: (String) -> Unit,
    onOpenJianguoyunHelp: () -> Unit,
) {
    SettingsGroupCard(
        header = stringResource(R.string.webdav_sync_section_connection),
        title = stringResource(R.string.webdav_sync_remote_workspace_title),
        subtitle = stringResource(R.string.webdav_sync_remote_workspace_summary),
        sectionIcon = appLucideFolderIcon(),
        containerColor = cardColor,
    ) {
        if (state.provider.serverUrlLocked) {
            SettingsInfoItem(
                key = stringResource(R.string.webdav_sync_jianguoyun_server_label),
                value = state.provider.presetServerUrl.orEmpty(),
            )
        } else {
            WebDavFieldLabel(stringResource(R.string.webdav_sync_server_url))
            WebDavLiquidTextField(
                value = state.serverUrl,
                onValueChange = onUpdateServerUrl,
                label = stringResource(R.string.webdav_sync_server_url_placeholder),
            )
        }
        webDavUrlErrorText(state.urlError)?.let { error ->
            AppSupportingBlock(
                text = error,
                accentColor = MiuixTheme.colorScheme.error,
                containerColor = MiuixTheme.colorScheme.error.copy(alpha = 0.08f),
                contentColor = MiuixTheme.colorScheme.error,
                fillWidth = true,
            )
        }
        WebDavFieldLabel(stringResource(R.string.webdav_sync_remote_dir))
        WebDavLiquidTextField(
            value = state.remoteDir,
            onValueChange = onUpdateRemoteDir,
            label = WebDavSyncStore.DEFAULT_REMOTE_DIR,
        )
        if (state.provider == WebDavProvider.Jianguoyun) {
            AppSupportingBlock(
                text = stringResource(R.string.webdav_sync_jianguoyun_hint),
                accentColor = MiuixTheme.colorScheme.primary,
                fillWidth = true,
            )
            SettingsNavigationItem(
                title = stringResource(R.string.webdav_sync_jianguoyun_help_label),
                summary = stringResource(R.string.webdav_sync_jianguoyun_help_summary),
                onClick = onOpenJianguoyunHelp,
            )
        }
    }
}

@Composable
internal fun WebDavConnectionActionsCard(
    state: WebDavSyncUiState,
    cardColor: Color,
    onTestConnection: () -> Unit,
    onSave: () -> Unit,
) {
    val result = state.connectionResult
    SettingsGroupCard(
        header = stringResource(R.string.webdav_sync_section_connection),
        title = stringResource(R.string.webdav_sync_connection_actions_title),
        subtitle = stringResource(R.string.webdav_sync_connection_actions_summary),
        sectionIcon = appLucideRefreshIcon(),
        containerColor = cardColor,
    ) {
        if (result != null) {
            val success = result.isSuccess
            AppSupportingBlock(
                text = webDavConnectionOutcomeText(result),
                accentColor =
                    if (success) {
                        AppStatusColors.Fresh
                    } else {
                        MiuixTheme.colorScheme.error
                    },
                containerColor =
                    if (success) {
                        AppStatusColors.Fresh.copy(alpha = 0.08f)
                    } else {
                        MiuixTheme.colorScheme.error.copy(alpha = 0.08f)
                    },
                contentColor =
                    if (success) {
                        AppStatusColors.Fresh
                    } else {
                        MiuixTheme.colorScheme.error
                    },
                fillWidth = true,
            )
        }
        AppDualActionRow(
            first = { modifier ->
                AppStandaloneLiquidTextButton(
                    variant = GlassVariant.SheetPrimaryAction,
                    text =
                        if (state.testing) {
                            stringResource(R.string.webdav_sync_testing)
                        } else {
                            stringResource(R.string.webdav_sync_test_connection)
                        },
                    modifier = modifier,
                    buttonModifier = Modifier.fillMaxWidth(),
                    textColor = MiuixTheme.colorScheme.primary,
                    enabled = !state.interactionLocked && state.canConnect,
                    onClick = onTestConnection,
                )
            },
            second = { modifier ->
                AppStandaloneLiquidTextButton(
                    variant = GlassVariant.SheetAction,
                    text = stringResource(R.string.webdav_sync_save),
                    modifier = modifier,
                    buttonModifier = Modifier.fillMaxWidth(),
                    textColor = MiuixTheme.colorScheme.primary,
                    enabled = !state.interactionLocked && state.canConnect,
                    onClick = onSave,
                )
            },
        )
    }
}

private data class WebDavConnectionVisualStatus(
    val label: String,
    val color: Color,
)

@Composable
private fun webDavConnectionVisualStatus(state: WebDavSyncUiState): WebDavConnectionVisualStatus =
    when {
        state.testing ->
            WebDavConnectionVisualStatus(
                label = stringResource(R.string.webdav_sync_testing),
                color = AppStatusColors.Refreshing,
            )

        state.connectionResult?.isSuccess == true ->
            WebDavConnectionVisualStatus(
                label = stringResource(R.string.webdav_sync_status_connected),
                color = AppStatusColors.Fresh,
            )

        state.connectionResult != null ->
            WebDavConnectionVisualStatus(
                label = stringResource(R.string.webdav_sync_status_needs_attention),
                color = MiuixTheme.colorScheme.error,
            )

        state.isConfigured ->
            WebDavConnectionVisualStatus(
                label = stringResource(R.string.webdav_sync_status_saved),
                color = AppStatusColors.Cached,
            )

        else ->
            WebDavConnectionVisualStatus(
                label = stringResource(R.string.webdav_sync_status_setup_required),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
    }

@Composable
private fun webDavConnectionOutcomeText(outcome: WebDavConnectionOutcome): String {
    val base =
        when (outcome.status) {
            WebDavConnectionStatus.Success ->
                stringResource(R.string.webdav_sync_test_success)

            WebDavConnectionStatus.SuccessDirCreated ->
                stringResource(R.string.webdav_sync_test_success_dir_created)

            WebDavConnectionStatus.AuthFailed ->
                stringResource(R.string.webdav_sync_status_auth_failed)

            WebDavConnectionStatus.PermissionDenied ->
                stringResource(R.string.webdav_sync_status_permission_denied)

            WebDavConnectionStatus.NetworkError ->
                stringResource(R.string.webdav_sync_status_network_error)

            WebDavConnectionStatus.InvalidUrl ->
                stringResource(R.string.webdav_sync_status_invalid_url)

            WebDavConnectionStatus.Unknown ->
                stringResource(R.string.webdav_sync_status_unknown)
        }
    val detail = outcome.detail?.takeIf { it.isNotBlank() }
    return if (detail != null && !outcome.isSuccess) "$base · $detail" else base
}

@Composable
private fun webDavUrlErrorText(error: WebDavUrlError?): String? =
    when (error) {
        null -> null
        WebDavUrlError.Empty -> stringResource(R.string.webdav_sync_url_error_empty)
        WebDavUrlError.Scheme -> stringResource(R.string.webdav_sync_url_error_scheme)
        WebDavUrlError.Invalid -> stringResource(R.string.webdav_sync_url_error_invalid)
    }
