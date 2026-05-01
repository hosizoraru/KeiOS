package os.kei.ui.page.main.about.section

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.BuildConfig
import os.kei.ui.page.main.github.AppIcon
import os.kei.ui.page.main.os.appLucideAlertIcon
import os.kei.ui.page.main.os.appLucideFlaskIcon
import os.kei.ui.page.main.os.appLucideFilterIcon
import os.kei.ui.page.main.os.appLucideInfoIcon
import os.kei.ui.page.main.os.appLucideLockIcon
import os.kei.ui.page.main.os.appLucideNotesIcon
import os.kei.ui.page.main.os.appLucideTimeIcon
import os.kei.ui.page.main.os.appLucideVersionIcon
import os.kei.ui.page.main.about.ui.AboutCompactInfoRow
import os.kei.ui.page.main.widget.core.AppCardHeader
import os.kei.ui.page.main.widget.core.AppInfoListBody
import os.kei.ui.page.main.widget.core.AppSurfaceCard
import os.kei.ui.page.main.widget.glass.AppInteractiveTokens
import os.kei.ui.page.main.widget.glass.GlassIconButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.about.util.formatTime
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.motion.appExpandIn
import os.kei.ui.page.main.widget.motion.appExpandOut
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AboutAppCardSection(
    appLabel: String,
    packageInfo: PackageInfo?,
    cardColor: Color,
    accent: Color,
    subtitleColor: Color,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOpenDebugActivity: () -> Unit
) {
    val context = LocalContext.current
    val unknown = stringResource(R.string.common_unknown)
    val yesText = stringResource(R.string.about_value_yes)
    val noText = stringResource(R.string.about_value_no)
    val packageName = packageInfo?.packageName ?: unknown
    val applicationInfo: ApplicationInfo? = packageInfo?.applicationInfo
    val versionText = packageInfo?.let {
        stringResource(
            R.string.about_value_version_format,
            it.versionName ?: unknown,
            it.longVersionCode
        )
    } ?: unknown
    val buildTime = formatTime(BuildConfig.BUILD_TIME_MILLIS)
        .ifBlank { unknown }
    val updatedAt = packageInfo?.lastUpdateTime
        ?.let(::formatTime)
        ?.ifBlank { unknown }
        ?: unknown
    val debugEnabled = (((applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_DEBUGGABLE) != 0)
    val testOnlyEnabled = (((applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_TEST_ONLY) != 0)
    AppSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = cardColor,
        contentColor = MiuixTheme.colorScheme.onBackground,
        onClick = { onExpandedChange(!expanded) }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.sectionGap)
        ) {
            AppCardHeader(
                title = stringResource(R.string.about_card_app_title),
                subtitle = stringResource(R.string.about_card_app_subtitle),
                titleColor = accent,
                subtitleColor = subtitleColor,
                startAction = {
                    AppIcon(
                        packageName = packageInfo?.packageName ?: context.packageName,
                        size = AppInteractiveTokens.cardHeaderLeadingSlotSize
                    )
                },
                endActions = {
                    GlassIconButton(
                        backdrop = null,
                        icon = appLucideFlaskIcon(),
                        contentDescription = stringResource(R.string.about_action_open_debug_activity),
                        onClick = onOpenDebugActivity,
                        iconTint = accent,
                        containerColor = Color.Transparent,
                        variant = GlassVariant.Compact
                    )
                },
                expandable = true,
                expanded = expanded,
                expandTint = accent,
                onClick = { onExpandedChange(!expanded) }
            )
            AnimatedVisibility(
                visible = expanded,
                enter = appExpandIn(),
                exit = appExpandOut()
            ) {
                AppInfoListBody(
                    modifier = Modifier.padding(
                        start = CardLayoutRhythm.cardHorizontalPadding,
                        end = CardLayoutRhythm.cardHorizontalPadding,
                        bottom = CardLayoutRhythm.cardVerticalPadding
                    ),
                    verticalSpacing = 0.dp
                ) {
                    AboutCompactInfoRow(
                        title = stringResource(R.string.about_label_name),
                        value = appLabel,
                        titleIcon = appLucideInfoIcon()
                    )
                    AboutCompactInfoRow(
                        title = stringResource(R.string.about_label_package_name),
                        value = packageName,
                        titleIcon = appLucideNotesIcon()
                    )
                    AboutCompactInfoRow(
                        title = stringResource(R.string.about_label_version),
                        value = versionText,
                        titleIcon = appLucideVersionIcon()
                    )
                    AboutCompactInfoRow(
                        title = stringResource(R.string.about_label_build_type),
                        value = BuildConfig.BUILD_TYPE,
                        titleIcon = appLucideFilterIcon()
                    )
                    AboutCompactInfoRow(
                        title = stringResource(R.string.about_label_build_time),
                        value = buildTime,
                        titleIcon = appLucideTimeIcon()
                    )
                    AboutCompactInfoRow(
                        title = stringResource(R.string.about_label_last_update),
                        value = updatedAt,
                        titleIcon = appLucideTimeIcon()
                    )
                    AboutCompactInfoRow(
                        title = stringResource(R.string.about_label_debug),
                        value = if (debugEnabled) yesText else noText,
                        titleIcon = appLucideAlertIcon()
                    )
                    AboutCompactInfoRow(
                        title = stringResource(R.string.about_label_test_only),
                        value = if (testOnlyEnabled) yesText else noText,
                        titleIcon = appLucideAlertIcon()
                    )
                    AboutCompactInfoRow(
                        title = stringResource(R.string.about_label_api_level),
                        value = android.os.Build.VERSION.SDK_INT.toString(),
                        titleIcon = appLucideFilterIcon()
                    )
                    AboutCompactInfoRow(
                        title = stringResource(R.string.about_label_security_patch),
                        value = android.os.Build.VERSION.SECURITY_PATCH ?: unknown,
                        titleIcon = appLucideLockIcon()
                    )
                }
            }
        }
    }
}
