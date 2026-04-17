package com.example.keios.ui.page.main.about.section

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
import com.example.keios.R
import com.example.keios.ui.page.main.AppIcon
import com.example.keios.ui.page.main.about.ui.AboutCompactInfoRow
import com.example.keios.ui.page.main.widget.AppCardHeader
import com.example.keios.ui.page.main.widget.AppInfoListBody
import com.example.keios.ui.page.main.about.util.formatTime
import com.example.keios.ui.page.main.widget.CardLayoutRhythm
import com.example.keios.ui.page.main.widget.appExpandIn
import com.example.keios.ui.page.main.widget.appExpandOut
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.Timer
import top.yukonga.miuix.kmp.icon.extended.Update
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AboutAppCardSection(
    appLabel: String,
    packageInfo: PackageInfo?,
    cardColor: Color,
    accent: Color,
    subtitleColor: Color,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
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
    val updatedAt = packageInfo?.lastUpdateTime
        ?.let(::formatTime)
        ?.ifBlank { unknown }
        ?: unknown
    val debugEnabled = (((applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_DEBUGGABLE) != 0)
    val testOnlyEnabled = (((applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_TEST_ONLY) != 0)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = cardColor,
            contentColor = MiuixTheme.colorScheme.onBackground
        ),
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
                        size = 20.dp
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
                        titleIcon = MiuixIcons.Regular.Info
                    )
                    AboutCompactInfoRow(
                        title = stringResource(R.string.about_label_package_name),
                        value = packageName,
                        titleIcon = MiuixIcons.Regular.Notes
                    )
                    AboutCompactInfoRow(
                        title = stringResource(R.string.about_label_version),
                        value = versionText,
                        titleIcon = MiuixIcons.Regular.Update
                    )
                    AboutCompactInfoRow(
                        title = stringResource(R.string.about_label_last_update),
                        value = updatedAt,
                        titleIcon = MiuixIcons.Regular.Timer
                    )
                    AboutCompactInfoRow(
                        title = stringResource(R.string.about_label_debug),
                        value = if (debugEnabled) yesText else noText,
                        titleIcon = MiuixIcons.Regular.Report
                    )
                    AboutCompactInfoRow(
                        title = stringResource(R.string.about_label_test_only),
                        value = if (testOnlyEnabled) yesText else noText,
                        titleIcon = MiuixIcons.Regular.Report
                    )
                    AboutCompactInfoRow(
                        title = stringResource(R.string.about_label_api_level),
                        value = android.os.Build.VERSION.SDK_INT.toString(),
                        titleIcon = MiuixIcons.Regular.Filter
                    )
                    AboutCompactInfoRow(
                        title = stringResource(R.string.about_label_security_patch),
                        value = android.os.Build.VERSION.SECURITY_PATCH ?: unknown,
                        titleIcon = MiuixIcons.Regular.Lock
                    )
                }
            }
        }
    }
}
