package com.example.keios.ui.page.main.github.sheet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.feature.github.model.GitHubApiAuthMode
import com.example.keios.feature.github.model.GitHubApiCredentialStatus
import com.example.keios.feature.github.model.GitHubLookupConfig
import com.example.keios.feature.github.model.GitHubLookupStrategyOption
import com.example.keios.feature.github.model.GitHubStrategyBenchmarkReport
import com.example.keios.ui.page.main.GitHubCredentialStatusCard
import com.example.keios.ui.page.main.GitHubRecommendedTokenGuideCard
import com.example.keios.ui.page.main.GitHubStatusPalette
import com.example.keios.ui.page.main.GitHubStrategyBenchmarkCard
import com.example.keios.ui.page.main.GitHubStrategyDraftSummaryCard
import com.example.keios.ui.page.main.GitHubStrategyGuideCard
import com.example.keios.ui.page.main.buildGitHubFineGrainedTokenTemplateUrl
import com.example.keios.ui.page.main.githubFineGrainedPatDocsUrl
import com.example.keios.ui.page.main.githubRecommendedTokenGuide
import com.example.keios.ui.page.main.githubStrategyGuides
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassSearchField
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.SheetActionGroup
import com.example.keios.ui.page.main.widget.SheetContentColumn
import com.example.keios.ui.page.main.widget.SheetControlRow
import com.example.keios.ui.page.main.widget.SheetDescriptionText
import com.example.keios.ui.page.main.widget.SheetFieldBlock
import com.example.keios.ui.page.main.widget.SheetSectionCard
import com.example.keios.ui.page.main.widget.SheetSectionTitle
import com.example.keios.ui.page.main.widget.SnapshotWindowBottomSheet
import com.example.keios.ui.page.main.widget.StatusPill
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GitHubStrategySheet(
    show: Boolean,
    backdrop: LayerBackdrop,
    lookupConfig: GitHubLookupConfig,
    selectedStrategyInput: GitHubLookupStrategyOption,
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
    onTokenInputChange: (String) -> Unit,
    onToggleTokenVisibility: () -> Unit,
    onRunCredentialCheck: () -> Unit,
    onRunStrategyBenchmark: () -> Unit,
    onRecommendedTokenGuideExpandedChange: (Boolean) -> Unit,
    onOpenExternalUrl: (String, String) -> Unit
) {
    val context = LocalContext.current
    val guides = remember(context) { githubStrategyGuides(context) }
    val tokenGuide = remember(context) { githubRecommendedTokenGuide(context) }
    SnapshotWindowBottomSheet(
        show = show,
        title = stringResource(R.string.github_strategy_sheet_title),
        onDismissRequest = onDismissRequest,
        startAction = {
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = MiuixIcons.Regular.Close,
                contentDescription = stringResource(R.string.common_close),
                onClick = onDismissRequest
            )
        },
        endAction = {
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = MiuixIcons.Regular.Ok,
                contentDescription = stringResource(R.string.github_strategy_sheet_cd_save),
                onClick = onApply
            )
        }
    ) {
        val sanitizedTokenInput = githubApiTokenInput.trim()
        val draftChanged = selectedStrategyInput != lookupConfig.selectedStrategy ||
            sanitizedTokenInput != lookupConfig.apiToken
        val tokenStatusLabel = when {
            selectedStrategyInput != GitHubLookupStrategyOption.GitHubApiToken ->
                context.getString(R.string.common_not_used)
            sanitizedTokenInput.isBlank() -> context.getString(R.string.common_guest)
            else -> context.getString(R.string.common_filled)
        }
        val tokenStatusColor = when {
            selectedStrategyInput != GitHubLookupStrategyOption.GitHubApiToken ->
                MiuixTheme.colorScheme.onBackgroundVariant
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

            if (selectedStrategyInput == GitHubLookupStrategyOption.GitHubApiToken) {
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
                            GlassTextButton(
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
                        GlassTextButton(
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
                    text = stringResource(R.string.github_strategy_desc_api_recommendation)
                )
                SheetSectionTitle(stringResource(R.string.github_strategy_section_recommended_create))
                GitHubRecommendedTokenGuideCard(
                    guide = tokenGuide,
                    expanded = recommendedTokenGuideExpanded,
                    onExpandedChange = onRecommendedTokenGuideExpandedChange
                )
                SheetActionGroup {
                    GlassTextButton(
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
                    GlassTextButton(
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
                GlassTextButton(
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
