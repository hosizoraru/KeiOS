package os.kei.ui.page.main.widget.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.kyant.backdrop.Backdrop
import os.kei.ui.page.main.widget.core.AppCardBodyColumn
import os.kei.ui.page.main.widget.core.AppCardHeader
import os.kei.ui.page.main.widget.core.AppSurfaceCard
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.motion.appExpandIn
import os.kei.ui.page.main.widget.motion.appExpandOut
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppLiquidAccordionCard(
    backdrop: Backdrop?,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    headerStartAction: (@Composable () -> Unit)? = null,
    titleAccessory: (@Composable RowScope.() -> Unit)? = null,
    headerActions: (@Composable () -> Unit)? = null,
    onHeaderLongClick: (() -> Unit)? = null,
    containerColor: Color? = null,
    contentPadding: PaddingValues = CardLayoutRhythm.cardContentPadding,
    verticalSpacing: Dp = CardLayoutRhythm.sectionGap,
    content: @Composable () -> Unit
) {
    AppLiquidExpandableCardFrame(
        backdrop = backdrop,
        title = title,
        subtitle = subtitle,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        headerStartAction = headerStartAction,
        titleAccessory = titleAccessory,
        headerActions = headerActions,
        onHeaderLongClick = onHeaderLongClick,
        containerColor = containerColor,
        contentPadding = contentPadding,
        verticalSpacing = verticalSpacing,
        content = content
    )
}

@Composable
fun AppLiquidExpandableSection(
    backdrop: Backdrop?,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    headerStartAction: (@Composable () -> Unit)? = null,
    headerActions: (@Composable () -> Unit)? = null,
    onHeaderLongClick: (() -> Unit)? = null,
    containerColor: Color? = null,
    content: @Composable () -> Unit
) {
    AppLiquidExpandableCardFrame(
        backdrop = backdrop,
        title = title,
        subtitle = subtitle,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        headerStartAction = headerStartAction,
        headerActions = headerActions,
        onHeaderLongClick = onHeaderLongClick,
        containerColor = containerColor,
        contentPadding = PaddingValues(
            start = CardLayoutRhythm.cardHorizontalPadding,
            end = CardLayoutRhythm.cardHorizontalPadding,
            bottom = CardLayoutRhythm.cardVerticalPadding
        ),
        verticalSpacing = CardLayoutRhythm.sectionGap,
        content = content
    )
}

@Composable
private fun AppLiquidExpandableCardFrame(
    backdrop: Backdrop?,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    headerStartAction: (@Composable () -> Unit)?,
    titleAccessory: (@Composable RowScope.() -> Unit)? = null,
    headerActions: (@Composable () -> Unit)?,
    onHeaderLongClick: (() -> Unit)?,
    containerColor: Color?,
    contentPadding: PaddingValues,
    verticalSpacing: Dp,
    content: @Composable () -> Unit
) {
    val sectionSurface = containerColor ?: MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)

    AppSurfaceCard(
        backdrop = backdrop,
        containerColor = sectionSurface,
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
            AppCardBodyColumn(
                contentPadding = contentPadding,
                verticalSpacing = verticalSpacing,
                content = { content() }
            )
        }
    }
}
