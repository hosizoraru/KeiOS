package os.kei.ui.page.main.widget.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import os.kei.ui.page.main.widget.core.AppCardBodyColumn
import os.kei.ui.page.main.widget.core.AppCardHeader
import os.kei.ui.page.main.widget.core.AppSurfaceCard
import os.kei.ui.page.main.widget.motion.appExpandIn
import os.kei.ui.page.main.widget.motion.appExpandOut
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixAccordionCard(
    backdrop: Backdrop?,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    headerStartAction: (@Composable () -> Unit)? = null,
    titleAccessory: (@Composable RowScope.() -> Unit)? = null,
    headerActions: (@Composable () -> Unit)? = null,
    onHeaderLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val surface = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)

    AppSurfaceCard(
        backdrop = backdrop,
        containerColor = surface,
        borderColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.14f),
        showIndication = false
    ) {
        AppCardHeader(
            title = title,
            subtitle = subtitle,
            startAction = headerStartAction,
            titleAccessory = titleAccessory,
            endActions = if (headerActions != null) {
                { headerActions.invoke() }
            } else {
                null
            },
            expandable = true,
            expanded = expanded,
            expandTint = MiuixTheme.colorScheme.primary,
            onClick = { onExpandedChange(!expanded) },
            onLongClick = onHeaderLongClick
        )
        AnimatedVisibility(
            visible = expanded,
            enter = appExpandIn(),
            exit = appExpandOut()
        ) {
            AppCardBodyColumn(content = { content() })
        }
    }
}
