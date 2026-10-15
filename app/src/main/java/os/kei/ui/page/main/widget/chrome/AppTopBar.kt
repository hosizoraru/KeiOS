package os.kei.ui.page.main.widget.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.ScrollBehavior

@Composable
fun AppTopBarSection(
    title: String,
    modifier: Modifier = Modifier,
    largeTitle: String = title,
    color: Color = Color.Transparent,
    scrollBehavior: ScrollBehavior? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    titleBackdrop: Backdrop? = null,
    titleStartReserve: Dp? = null,
    titleEndReserve: Dp? = null,
    onTitleClick: () -> Unit = {},
) {
    val safeTop = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val barHeight = AppChromeTokens.topBarCollapsedHeight
    val topBarTitle = title.ifBlank { largeTitle }
    val resolvedTitleStartReserve =
        titleStartReserve ?: if (navigationIcon != null) {
            AppChromeTokens.topBarTitleNavigationReserve
        } else {
            AppChromeTokens.topBarTitleEdgePadding
        }
    val resolvedTitleEndReserve =
        titleEndReserve ?: if (navigationIcon != null) {
            AppChromeTokens.topBarTitleNavigationReserve
        } else {
            AppChromeTokens.topBarTitleEdgePadding
        }
    SideEffect {
        scrollBehavior?.state?.let { state ->
            if (state.heightOffsetLimit != 0f) {
                state.heightOffsetLimit = 0f
                state.heightOffset = 0f
            }
        }
    }
    Column(modifier = modifier) {
        androidx.compose.foundation.layout.Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(safeTop + barHeight)
                    .background(color)
                    .padding(top = safeTop),
        ) {
            androidx.compose.foundation.layout.Box(
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(
                            // The same term the actions take at the other end. A back button pinned to the
                            // true window edge returns you to a page whose column starts a gutter away.
                            start = appTopBarEdgePaddingStart() + appTopBarChromeGutter(),
                            top = AppChromeTokens.topBarChromeTopPadding,
                        ),
            ) {
                navigationIcon?.invoke()
            }
            // At regular width the centre of this row is the tab bar's, so the title moves to the leading
            // edge — which is where iPadOS puts it, next to the navigation icon, with the tab bar centred
            // between it and the actions. On a phone nothing is centred here but the title, and it stays.
            val titleIsLeading = appTopBarCentreIsNavigation()
            // The title yields when the row cannot hold all three. The tab bar already names the section, so
            // the title is the redundant one; three overlapping items would say nothing.
            val titleWidth = appContentWidth()
            val showTitle =
                !titleIsLeading ||
                    appTopRowFitsTitle(
                        availableWidth = titleWidth,
                        barWidth = appTopBarNavigationMaxWidth(titleWidth).coerceAtMost(460.dp),
                    )
            if (showTitle) {
            AppTopBarTitleCard(
                title = topBarTitle,
                backdrop = titleBackdrop,
                startReserve = resolvedTitleStartReserve,
                endReserve = resolvedTitleEndReserve,
                onClick = onTitleClick,
                modifier =
                    Modifier
                        .then(if (titleIsLeading) Modifier else Modifier.fillMaxWidth())
                        .align(if (titleIsLeading) Alignment.TopStart else Alignment.TopCenter)
                        .padding(
                            start = if (titleIsLeading) appTopBarEdgePaddingStart() + appTopBarTitleLeadingInset() else 0.dp,
                            top = AppChromeTokens.topBarChromeTopPadding,
                        ),
            )
            }
            Row(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(
                            end = appTopBarEdgePadding() + appTopBarChromeGutter(),
                            top = AppChromeTokens.topBarChromeTopPadding,
                        ),
                content = actions,
            )
        }
    }
}
