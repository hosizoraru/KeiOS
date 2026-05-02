package os.kei.ui.page.main.github.sheet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.feature.github.model.GitHubApiAuthMode
import os.kei.feature.github.model.GitHubApiCredentialStatus
import os.kei.feature.github.model.GitHubActionsLookupStrategyOption
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubLookupStrategyOption
import os.kei.feature.github.model.GitHubStrategyBenchmarkReport
import os.kei.ui.page.main.github.GitHubActionsStrategyGuideCard
import os.kei.ui.page.main.github.GitHubCredentialStatusCard
import os.kei.ui.page.main.github.GitHubRecommendedTokenGuideCard
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.github.GitHubStrategyBenchmarkCard
import os.kei.ui.page.main.github.GitHubStrategyDraftSummaryCard
import os.kei.ui.page.main.github.GitHubStrategyGuideCard
import os.kei.ui.page.main.os.appLucideCloseIcon
import os.kei.ui.page.main.os.appLucideConfirmIcon
import os.kei.ui.page.main.github.buildGitHubFineGrainedTokenTemplateUrl
import os.kei.ui.page.main.github.githubFineGrainedPatDocsUrl
import os.kei.ui.page.main.github.githubActionsStrategyGuides
import os.kei.ui.page.main.github.githubRecommendedTokenGuide
import os.kei.ui.page.main.github.githubStrategyGuides
import os.kei.ui.page.main.widget.glass.GlassIconButton
import os.kei.ui.page.main.widget.glass.GlassSearchField
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.sheet.SheetActionGroup
import os.kei.ui.page.main.widget.sheet.SheetContentColumn
import os.kei.ui.page.main.widget.sheet.SheetControlRow
import os.kei.ui.page.main.widget.sheet.SheetDescriptionText
import os.kei.ui.page.main.widget.sheet.SheetFieldBlock
import os.kei.ui.page.main.widget.sheet.SheetSectionCard
import os.kei.ui.page.main.widget.sheet.SheetSectionTitle
import os.kei.ui.page.main.widget.sheet.SnapshotWindowBottomSheet
import os.kei.ui.page.main.widget.status.StatusPill
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GitHubStrategySheet(
    show: Boolean,
    backdrop: LayerBackdrop,
    lookupConfig: GitHubLookupConfig,
    selectedStrategyInput: GitHubLookupStrategyOption,
    selectedActionsStrategyInput: GitHubActionsLookupStrategyOption,
    githubApiTokenInput: String,
    showApiTokenPlainText: Boolean,
    credentialCheckRunning: Boolean,
    credentialCheckError: String?,
    credentialCheckStatus: GitHubApiCredentialStatus?,
    strategyBenchmarkRunning: Boolean,
    strategyBenchmarkError: String?,
    strategyBenchmarkReport: GitHubStrategyBenchmarkReport?,
    trackedCount: Int,
    recommendedTokenGuideExpanded: Boolean,
    onDismissRequest: () -> Unit,
    onApply: () -> Unit,
    onSelectedStrategyChange: (GitHubLookupStrategyOption) -> Unit,
    onSelectedActionsStrategyChange: (GitHubActionsLookupStrategyOption) -> Unit,
    onTokenInputChange: (String) -> Unit,
    onToggleTokenVisibility: () -> Unit,
    onRunCredentialCheck: () -> Unit,
    onRunStrategyBenchmark: () -> Unit,
    onRecommendedTokenGuideExpandedChange: (Boolean) -> Unit,
    onOpenExternalUrl: (String, String) -> Unit
) {
    val context = LocalContext.current
    val guides = remember(context) { githubStrategyGuides(context) }
    val actionsGuides = remember(context) { githubActionsStrategyGuides(context) }
    val tokenGuide = remember(context) { githubRecommendedTokenGuide(context) }
    SnapshotWindowBottomSheet(
        show = show,
        title = stringResource(R.string.github_strategy_sheet_title),
        onDismissRequest = onDismissRequest,
        startAction = {
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = appLucideCloseIcon(),
                contentDescription = stringResource(R.string.common_close),
                onClick = onDismissRequest
            )
        },
        endAction = {
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = appLucideConfirmIcon(),
                contentDescription = stringResource(R.string.github_strategy_sheet_cd_save),
                onClick = onApply
            )
        }
    ) {
        val sanitizedTokenInput = githubApiTokenInput.trim()
        val draftChanged = selectedStrategyInput != lookupConfig.selectedStrategy ||
            selectedActionsStrategyInput != lookupConfig.actionsStrategy ||
            sanitizedTokenInput != lookupConfig.apiToken
        val tokenUsedByDraft = selectedStrategyInput == GitHubLookupStrategyOption.GitHubApiToken ||
            selectedActionsStrategyInput == GitHubActionsLookupStrategyOption.GitHubApiToken
        val tokenStatusLabel = when {
            !tokenUsedByDraft -> context.getString(R.string.common_not_used)
            sanitizedTokenInput.isBlank() -> context.getString(R.string.common_guest)
            else -> context.getString(R.string.common_filled)
        }
        val tokenStatusColor = when {
            !tokenUsedByDraft -> MiuixTheme.colorScheme.onBackgroundVariant
            sanitizedTokenInput.isBlank() -> GitHubStatusPalette.PreRelease
            else -> GitHubStatusPalette.Update
        }
        val credentialAvailabilityLabel = when {
            credentialCheckRunning -> context.getString(R.string.common_checking)
            credentialCheckStatus != null -> context.getString(R.string.common_available)
            credentialCheckError != null -> context.getString(R.string.common_unavailable)
            else -> context.getString(R.string.common_not_checked)
        }
        val credentialAvailabilityColor = when {
            credentialCheckRunning -> MiuixTheme.colorScheme.primary
            credentialCheckStatus != null -> GitHubStatusPalette.Update
            credentialCheckError != null -> GitHubStatusPalette.Error
            else -> MiuixTheme.colorScheme.onBackgroundVariant
        }

        SheetContentColumn(verticalSpacing = 8.dp) {
            SheetSectionTitle(stringResource(R.string.github_strategy_section_draft_summary))
            GitHubStrategyDraftSummaryCard(
                selectedStrategy = selectedStrategyInput,
                selectedActionsStrategy = selectedActionsStrategyInput,
                tokenInput = sanitizedTokenInput,
                trackedCount = trackedCount,
                changed = draftChanged
            )

            SheetSectionTitle(stringResource(R.string.github_strategy_section_strategy))
            guides.forEach { guide ->
                GitHubStrategyGuideCard(
                    guide = guide,
                    selected = selectedStrategyInput == guide.option,
                    onSelect = { onSelectedStrategyChange(guide.option) }
                )
            }

            SheetSectionTitle(stringResource(R.string.github_strategy_section_actions_strategy))
            actionsGuides.forEach { guide ->
                GitHubActionsStrategyGuideCard(
                    guide = guide,
                    selected = selectedActionsStrategyInput == guide.option,
                    onSelect = { onSelectedActionsStrategyChange(guide.option) }
                )
            }

            if (tokenUsedByDraft) {
                SheetSectionTitle(stringResource(R.string.github_strategy_section_credential))
                SheetSectionCard {
                    SheetControlRow(label = stringResource(R.string.github_strategy_label_token_status)) {
                        StatusPill(
                            label = tokenStatusLabel,
                            color = tokenStatusColor
                        )
                    }
                    SheetControlRow(label = stringResource(R.string.github_strategy_label_availability)) {
                        StatusPill(
                            label = credentialAvailabilityLabel,
                            color = credentialAvailabilityColor
                        )
                    }
                    SheetFieldBlock(
                        title = stringResource(R.string.github_strategy_field_token_title),
                        summary = if (sanitizedTokenInput.isBlank()) {
                            stringResource(R.string.github_strategy_field_token_summary_guest)
                        } else {
                            stringResource(R.string.github_strategy_field_token_summary_filled)
                        },
                        trailing = {
                            AppLiquidTextButton(
                                backdrop = backdrop,
                                variant = GlassVariant.SheetAction,
                                text = if (showApiTokenPlainText) {
                                    stringResource(R.string.github_strategy_btn_hide_token)
                                } else {
                                    stringResource(R.string.github_strategy_btn_show_token)
                                },
                                onClick = onToggleTokenVisibility
                            )
                        }
                    ) {
                        GlassSearchField(
                            value = githubApiTokenInput,
                            onValueChange = onTokenInputChange,
                            label = stringResource(R.string.github_strategy_token_input_label),
                            backdrop = backdrop,
                            variant = GlassVariant.SheetInput,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            visualTransformation = if (showApiTokenPlainText) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            }
                        )
                    }
                }
                SheetSectionTitle(stringResource(R.string.github_strategy_section_verify_now))
                SheetSectionCard {
                    SheetActionGroup {
                        AppLiquidTextButton(
                            backdrop = backdrop,
                            variant = GlassVariant.SheetAction,
                            text = if (credentialCheckRunning) {
                                stringResource(R.string.github_strategy_btn_check_running)
                            } else {
                                stringResource(R.string.github_strategy_btn_check_now)
                            },
                            enabled = !credentialCheckRunning,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onRunCredentialCheck
                        )
                    }
                    SheetDescriptionText(
                        text = stringResource(R.string.github_strategy_desc_guest_api)
                    )
                    SheetDescriptionText(
                        text = stringResource(R.string.github_strategy_desc_pat_scope)
                    )
                    credentialCheckError?.let { error ->
                        SheetDescriptionText(
                            text = stringResource(R.string.github_strategy_desc_check_failed, error)
                        )
                    }
                }
                credentialCheckStatus?.let { status ->
                    GitHubCredentialStatusCard(status = status)
                    SheetDescriptionText(
                        text = if (status.authMode == GitHubApiAuthMode.Guest) {
                            stringResource(R.string.github_strategy_desc_guest_ok)
                        } else {
                            stringResource(R.string.github_strategy_desc_token_ok)
                        }
                    )
                }
                GitHubStrategyBenchmarkSection(
                    backdrop = backdrop,
                    trackedCount = trackedCount,
                    strategyBenchmarkRunning = strategyBenchmarkRunning,
                    strategyBenchmarkError = strategyBenchmarkError,
                    strategyBenchmarkReport = strategyBenchmarkReport,
                    onRunStrategyBenchmark = onRunStrategyBenchmark
                )
                SheetDescriptionText(
                    text = stringResource(R.string.github_strategy_desc_api_actions_recommendation)
                )
                SheetSectionTitle(stringResource(R.string.github_strategy_section_recommended_create))
                GitHubRecommendedTokenGuideCard(
                    guide = tokenGuide,
                    expanded = recommendedTokenGuideExpanded,
                    onExpandedChange = onRecommendedTokenGuideExpandedChange
                )
                SheetActionGroup {
                    AppLiquidTextButton(
                        backdrop = backdrop,
                        variant = GlassVariant.SheetAction,
                        text = stringResource(R.string.github_strategy_btn_open_prefilled_create),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onOpenExternalUrl(
                                buildGitHubFineGrainedTokenTemplateUrl(),
                                context.getString(R.string.github_strategy_error_open_create_page)
                            )
                        }
                    )
                    AppLiquidTextButton(
                        backdrop = backdrop,
                        variant = GlassVariant.SheetAction,
                        text = stringResource(R.string.github_strategy_btn_open_docs),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onOpenExternalUrl(
                                githubFineGrainedPatDocsUrl,
                                context.getString(R.string.github_strategy_error_open_docs)
                            )
                        }
                    )
                }
                SheetDescriptionText(
                    text = stringResource(R.string.github_strategy_desc_dedicated_token)
                )
            } else {
                SheetSectionTitle(stringResource(R.string.github_strategy_section_strategy_note))
                SheetSectionCard {
                    SheetDescriptionText(
                        text = stringResource(R.string.github_strategy_desc_atom_note)
                    )
                }
                GitHubStrategyBenchmarkSection(
                    backdrop = backdrop,
                    trackedCount = trackedCount,
                    strategyBenchmarkRunning = strategyBenchmarkRunning,
                    strategyBenchmarkError = strategyBenchmarkError,
                    strategyBenchmarkReport = strategyBenchmarkReport,
                    onRunStrategyBenchmark = onRunStrategyBenchmark
                )
            }
        }
    }
}

@Composable
private fun GitHubStrategyBenchmarkSection(
    backdrop: LayerBackdrop,
    trackedCount: Int,
    strategyBenchmarkRunning: Boolean,
    strategyBenchmarkError: String?,
    strategyBenchmarkReport: GitHubStrategyBenchmarkReport?,
    onRunStrategyBenchmark: () -> Unit
) {
    SheetSectionTitle(stringResource(R.string.github_strategy_section_local_benchmark))
    SheetSectionCard {
        SheetDescriptionText(
            text = if (trackedCount == 0) {
                stringResource(R.string.github_strategy_benchmark_desc_no_target)
            } else {
                stringResource(R.string.github_strategy_benchmark_desc_with_target)
            }
        )
        if (trackedCount > 0) {
            SheetActionGroup {
                AppLiquidTextButton(
                    backdrop = backdrop,
                    variant = GlassVariant.SheetAction,
                    text = if (strategyBenchmarkRunning) {
                        stringResource(R.string.github_strategy_benchmark_btn_running)
                    } else {
                        stringResource(R.string.github_strategy_benchmark_btn_run)
                    },
                    enabled = !strategyBenchmarkRunning,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRunStrategyBenchmark
                )
            }
        }
        strategyBenchmarkError?.let { error ->
            SheetDescriptionText(
                text = stringResource(R.string.github_strategy_benchmark_error, error)
            )
        }
    }
    strategyBenchmarkReport?.results?.forEach { result ->
        GitHubStrategyBenchmarkCard(result = result)
    }
}
