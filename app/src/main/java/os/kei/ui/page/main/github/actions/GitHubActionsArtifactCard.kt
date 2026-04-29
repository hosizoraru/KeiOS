package os.kei.ui.page.main.github.actions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import os.kei.R
import os.kei.feature.github.model.GitHubActionsArtifactMatch
import os.kei.feature.github.model.GitHubActionsRunMatch
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.github.asset.assetRelativeTimeLabel
import os.kei.ui.page.main.github.asset.formatAssetSize
import os.kei.ui.page.main.github.asset.formatReleaseUpdatedAtNoYear
import os.kei.ui.page.main.os.appLucideDownloadIcon
import os.kei.ui.page.main.os.appLucideShareIcon
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.glass.GlassIconButton
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GitHubActionsArtifactCard(
    runMatch: GitHubActionsRunMatch,
    artifactMatch: GitHubActionsArtifactMatch,
    recommended: Boolean,
    hasToken: Boolean,
    downloading: Boolean,
    sharing: Boolean,
    context: Context,
    isDark: Boolean,
    backdrop: LayerBackdrop,
    onDownload: () -> Unit,
    onShare: () -> Unit
) {
    val artifact = artifactMatch.artifact
    val actionColor = if (artifact.expired) GitHubStatusPalette.Error else MiuixTheme.colorScheme.primary
    val kindColor = artifactKindColor(artifactMatch.traits.kind)
    val busy = downloading || sharing
    val canDownload = hasToken && runMatch.traits.completed && !artifact.expired && !busy
    val canShare = hasToken && runMatch.traits.completed && !artifact.expired && !busy
    val copyDigestLabel = stringResource(R.string.github_actions_cd_copy_digest)
    val digestCopiedToast = stringResource(R.string.github_actions_toast_digest_copied)
    val hasDigest = artifact.digest.isNotBlank()
    val digestText = if (hasDigest) {
        shortArtifactDigest(artifact.digest)
    } else {
        formatReleaseUpdatedAtNoYear(artifact.updatedAtMillis)
    }
    val artifactSizeLabel = artifact.sizeBytes.takeIf { it > 0L }
        ?.let { formatAssetSize(it, context) }
        ?: stringResource(R.string.common_download)
    GitHubActionsSelectableCard(
        selected = false,
        isDark = isDark,
        onClick = null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = artifactDisplayName(artifactMatch),
                modifier = Modifier.weight(1f),
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = AppTypographyTokens.Body.fontSize,
                lineHeight = AppTypographyTokens.Body.lineHeight,
                fontWeight = AppTypographyTokens.BodyEmphasis.fontWeight,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (recommended) {
                GitHubActionsInfoPill(
                    label = stringResource(R.string.github_actions_badge_recommended),
                    color = GitHubStatusPalette.Update,
                    emphasized = true
                )
            }
            if (artifactMatch.lastDownload != null) {
                GitHubActionsInfoPill(
                    label = stringResource(R.string.github_actions_badge_last_downloaded),
                    color = GitHubStatusPalette.Active,
                    emphasized = true
                )
            }
        }
        GitHubActionsPillRow {
            GitHubActionsInfoPill(
                label = artifactKindLabel(artifactMatch.traits.kind),
                color = kindColor
            )
            artifactMatch.traits.version.takeIf { it.isNotBlank() }?.let { version ->
                GitHubActionsInfoPill(label = version, color = GitHubStatusPalette.Update)
            }
            artifactMatch.traits.abi.takeIf { it.isNotBlank() }?.let { abi ->
                GitHubActionsInfoPill(label = abi, color = GitHubStatusPalette.Active)
            }
            artifactMatch.traits.flavors.forEach { flavor ->
                GitHubActionsInfoPill(label = flavor, color = GitHubStatusPalette.PreRelease)
            }
            artifactMatch.traits.buildTypes
                .filterNot { it == "release" || it == "debug" }
                .forEach { buildType ->
                    GitHubActionsInfoPill(
                        label = artifactBuildTypeLabel(buildType),
                        color = artifactBuildTypeColor(buildType)
                    )
                }
            if (artifactMatch.traits.releaseLike) {
                GitHubActionsInfoPill(
                    label = stringResource(R.string.github_actions_badge_release),
                    color = GitHubStatusPalette.Update
                )
            }
            if (artifactMatch.traits.debugLike) {
                GitHubActionsInfoPill(
                    label = stringResource(R.string.github_actions_badge_debug),
                    color = GitHubStatusPalette.PreRelease
                )
            }
            if (artifactMatch.traits.universalLike) {
                GitHubActionsInfoPill(
                    label = stringResource(R.string.github_actions_badge_universal),
                    color = GitHubStatusPalette.Active
                )
            }
            if (artifact.expired) {
                GitHubActionsInfoPill(
                    label = stringResource(R.string.github_actions_badge_expired),
                    color = GitHubStatusPalette.Error,
                    emphasized = true
                )
            }
            assetRelativeTimeLabel(artifact.updatedAtMillis, context)?.let { label ->
                GitHubActionsInfoPill(label = label, color = MiuixTheme.colorScheme.onBackgroundVariant)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (digestText != null) {
                Text(
                    text = digestText,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (hasDigest) {
                                Modifier.clickable(onClickLabel = copyDigestLabel) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                        as? ClipboardManager
                                    clipboard?.setPrimaryClip(
                                        ClipData.newPlainText("sha256", artifact.digest)
                                    )
                                    Toast.makeText(context, digestCopiedToast, Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Modifier
                            }
                        ),
                    color = if (hasDigest) {
                        MiuixTheme.colorScheme.primary
                    } else {
                        MiuixTheme.colorScheme.onBackgroundVariant
                    },
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            GlassTextButton(
                backdrop = backdrop,
                variant = GlassVariant.SheetAction,
                text = artifactDownloadLabel(
                    hasToken = hasToken,
                    completed = runMatch.traits.completed,
                    expired = artifact.expired,
                    downloading = downloading,
                    readyLabel = artifactSizeLabel
                ),
                leadingIcon = appLucideDownloadIcon(),
                enabled = canDownload,
                textColor = actionColor,
                iconTint = actionColor,
                modifier = Modifier.widthIn(min = 104.dp),
                onClick = onDownload,
                textMaxLines = 1,
                textOverflow = TextOverflow.Ellipsis
            )
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.SheetAction,
                icon = appLucideShareIcon(),
                contentDescription = stringResource(
                    R.string.github_actions_cd_share_artifact,
                    artifact.name
                ),
                iconTint = if (canShare) actionColor else MiuixTheme.colorScheme.onBackgroundVariant,
                width = 54.dp,
                height = 54.dp,
                onClick = onShare
            )
        }
    }
}
