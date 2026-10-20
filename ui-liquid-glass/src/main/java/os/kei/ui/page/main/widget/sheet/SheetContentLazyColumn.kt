@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import os.kei.ui.page.main.widget.chrome.AppChromeTokens

private val SheetContentShadowEdgePadding = 8.dp
private val SheetContentBottomPadding = 12.dp

/**
 * Lazy counterpart to [SheetContentColumn], for sheets whose content is long enough that composing
 * all of it is wasteful.
 *
 * ## What this is and is not worth for
 *
 * Measured on the API 37 AVD: a glass control inside a sheet costs roughly 3.7ms of RenderThread per
 * frame, and — this is the surprising part — **it costs that whether or not it is inside the
 * viewport**. Varying the window so 5, 7 or 9 of one sheet's switches were visible moved the frame
 * cost only 36.1 -> 40.4ms, while the *composed* count stayed at 9 throughout. Clipping is not
 * culling: the effect layer is still recorded and rasterized.
 *
 * So the lever this offers is real but narrow: it helps exactly in proportion to the glass it stops
 * *composing*. A `LazyColumn` only drops items that fall entirely outside the viewport, which means
 * a sheet whose controls are nested inside one tall always-visible card gains nothing from being
 * moved here — the card is a single item and it is always partly on screen. Reach for this when the
 * sheet's top level is a long run of independently-sized children, not to make a two-card sheet lazy.
 *
 * See `docs/planning/liquid-sheet-frame-cost.md` for the measurements.
 *
 * ## Contract differences from [SheetContentColumn]
 *
 * There is no `scrollable = false` mode: a lazy list is a scroll container by construction, and a
 * caller that does not want one wants the eager column. Padding moves to `contentPadding` so it
 * still scrolls with the content, matching where the eager column puts it (inside the scroll,
 * navigation-bar inset included). Override [contentPadding] for a viewport inside a card whose
 * parent already supplies those insets and shadow spacing.
 */
@Composable
fun SheetContentLazyColumn(
    modifier: Modifier = Modifier,
    verticalSpacing: Dp = AppChromeTokens.pageSectionGapLarge,
    contentPadding: PaddingValues? = null,
    content: LazyListScope.() -> Unit,
) {
    val state = rememberLazyListState()
    val drawViewport = remember { LiquidSheetDrawViewport() }
    val overflowReporter = rememberUpdatedState(LocalLiquidSheetContentOverflowReporter.current)
    val managedScrollableContentReporter = rememberUpdatedState(
        LocalLiquidSheetManagedScrollableContentReporter.current,
    )

    LaunchedEffect(Unit) {
        snapshotFlow { managedScrollableContentReporter.value }
            .collect { it(true) }
    }
    // `canScrollForward || canScrollBackward` stands in for the eager column's `maxValue > 0`. Both
    // flags are false only while the content fits, and they settle after the first layout pass, so
    // this reports the same thing one frame later than a measured extent would.
    LaunchedEffect(state) {
        // Reaching either list edge changes the individual flags even when the list still overflows.
        // Keep these layout/scroll reads out of composition and publish only their combined result.
        snapshotFlow { (state.canScrollForward || state.canScrollBackward) to overflowReporter.value }
            .collect { (overflows, report) -> report(overflows) }
    }

    val contentScroll = LocalLiquidSheetContentScroll.current
    val scrollSource = remember(state) { LazyListSheetContentSource(state) }
    DisposableEffect(contentScroll, scrollSource) {
        contentScroll?.attach(scrollSource)
        onDispose { contentScroll?.detach(scrollSource) }
    }
    DisposableEffect(Unit) {
        onDispose {
            overflowReporter.value(false)
            managedScrollableContentReporter.value(false)
        }
    }

    val resolvedContentPadding = contentPadding ?: run {
        val navigationBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        PaddingValues(
            top = SheetContentShadowEdgePadding,
            bottom = SheetContentBottomPadding + SheetContentShadowEdgePadding + navigationBarsBottom,
        )
    }
    CompositionLocalProvider(LocalLiquidSheetDrawViewport provides drawViewport) {
        LazyColumn(
            state = state,
            modifier = modifier.fillMaxWidth().liquidSheetDrawViewport(drawViewport),
            contentPadding = resolvedContentPadding,
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            content = content,
        )
    }
}
