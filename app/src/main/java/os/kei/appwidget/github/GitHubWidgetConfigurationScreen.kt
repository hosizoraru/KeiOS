package os.kei.appwidget.github

import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import os.kei.R
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.glass.AppSwitch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GitHubWidgetConfigurationScreen(
    snapshot: GitHubWidgetSnapshot,
    showProjectNames: Boolean,
    isGuide: Boolean,
    ready: Boolean,
    saving: Boolean,
    pinSupported: Boolean,
    pinRequested: Boolean = false,
    pinAdded: Boolean = false,
    message: String?,
    onShowProjectNamesChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onOpenHome: () -> Unit = {},
    onClose: () -> Unit,
) {
    val previewDescription = if (snapshot.available) {
        stringResource(R.string.github_widget_preview, snapshot.trackedCount, snapshot.updateCount)
    } else {
        stringResource(R.string.github_widget_open_to_load)
    }
    val namesLabel = stringResource(R.string.github_widget_show_names)
    val scrollState = rememberScrollState()
    LaunchedEffect(message, scrollState.maxValue) {
        if (message != null) scrollState.animateScrollTo(scrollState.maxValue)
    }
    Box(
        modifier = Modifier.fillMaxSize().background(MiuixTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 560.dp).fillMaxSize(),
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .verticalScroll(scrollState).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    stringResource(R.string.github_widget_title),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(R.string.github_widget_description),
                    fontSize = 15.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
                Text(
                    stringResource(R.string.github_widget_preview_label),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onBackground,
                )
                AndroidView(
                    factory = {
                        FrameLayout(it).apply {
                            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                        .height(if (LocalDensity.current.fontScale > 1.3f) 192.dp else 160.dp)
                        .clearAndSetSemantics { contentDescription = previewDescription },
                    update = { host ->
                        host.removeAllViews()
                        host.addView(
                            GitHubWidgetRenderer.layout(
                                host.context, 0, snapshot,
                                GitHubWidgetConfiguration(showProjectNames), GitHubWidgetLayout.Board, interactive = false,
                            ).apply(host.context, host),
                            FrameLayout.LayoutParams(-1, -1),
                        )
                    },
                )
                Text(
                    stringResource(R.string.github_widget_size_hint),
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(MiuixTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            stringResource(R.string.github_widget_show_names),
                            fontSize = 16.sp,
                            color = MiuixTheme.colorScheme.onBackground,
                        )
                        Text(
                            stringResource(R.string.github_widget_show_names_summary),
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                        )
                    }
                    AppSwitch(
                        checked = showProjectNames,
                        onCheckedChange = onShowProjectNamesChange,
                        enabled = ready && !saving,
                        modifier = Modifier.semantics { contentDescription = namesLabel },
                    )
                }
                Text(
                    stringResource(R.string.github_widget_update_policy),
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
                if (isGuide && !pinSupported) {
                    Text(
                        stringResource(R.string.github_widget_pin_unavailable),
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                    )
                }
                if (message != null) {
                    Text(
                        message,
                        color = MiuixTheme.colorScheme.onBackground,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                if (isGuide && (!pinSupported || pinRequested && !pinAdded)) {
                    Text(
                        stringResource(R.string.github_widget_manual_hint),
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                    )
                }
                if (isGuide && (!pinSupported || pinRequested)) {
                    AppLiquidTextButton(
                        backdrop = null,
                        text = stringResource(R.string.github_widget_open_home),
                        onClick = onOpenHome,
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppLiquidTextButton(
                    backdrop = null,
                    text = stringResource(
                        when {
                            saving && isGuide -> R.string.github_widget_preparing
                            saving -> R.string.github_widget_saving
                            isGuide && pinAdded -> R.string.github_widget_add_another
                            isGuide && pinRequested -> R.string.github_widget_retry_pin
                            isGuide -> R.string.github_widget_add
                            else -> R.string.github_widget_save
                        },
                    ),
                    onClick = onConfirm,
                    enabled = ready && !saving && (!isGuide || pinSupported),
                    containerColor = MiuixTheme.colorScheme.primary,
                    textColor = MiuixTheme.colorScheme.onPrimary,
                    modifier = Modifier.fillMaxWidth(),
                )
                AppLiquidTextButton(
                    backdrop = null,
                    text = stringResource(R.string.common_close),
                    onClick = onClose,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
