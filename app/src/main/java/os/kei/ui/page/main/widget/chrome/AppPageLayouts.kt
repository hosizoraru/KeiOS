@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.chrome

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.ToolbarPosition
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold

internal val LocalAppScaffoldContainerColor = staticCompositionLocalOf<Color?> { null }

/**
 * True when a managed background is painting this page, so page-level fills must step aside.
 *
 * The non-Home background is meant to apply everywhere except Home, but a page that paints its own
 * opaque plate covers it and the setting silently does nothing there. Rather than delete those plates —
 * the BA calendar and pool use theirs for a designed accent wash, and it doubles as their glass
 * backdrop producer — a page asks whether it is being backed and drops only the opaque part.
 *
 * Reads the same signal `AppManagedBackgroundHost` already publishes to make scaffolds transparent, so
 * there is one source of truth for "a background is active" rather than re-deriving it from prefs.
 *
 * **Only answers the question inside a route.** `MainPagerPageHost` makes every non-Home main page's
 * scaffold transparent whether or not a background is enabled, so on those pages this reads true
 * regardless. Anything that must know a background is genuinely painting — glass sampling it, for
 * instance — should ask [LocalAppManagedSceneBackdrop], which is only non-null when one is.
 */
@Composable
fun appManagedPageBackgroundActive(): Boolean = LocalAppScaffoldContainerColor.current == Color.Transparent

/**
 * The colour actually behind this page's content, which is what its glass should treat as its base.
 *
 * `MiuixTheme.colorScheme.surface` is not it. `AppScaffold` defaults its container to `surface`, but
 * `MainPagerPageHost` overrides every non-Home main page to transparent, leaving `MainPagerLayout`'s
 * `background(colorScheme.background)` as the visible base. miuix sets those two tokens apart —
 * `background` is White / `#242424` against `surface` = `#F7F7F7` / Black — so a page backdrop recorded
 * over `surface` had its glass sampling pure black while the page rendered `#242424`, 36 levels lighter.
 * Home keeps `surface`, because Home is the one page that really does paint it.
 */
@Composable
internal fun appPageBackdropBaseColor(): Color {
    val containerColor = LocalAppScaffoldContainerColor.current
    return when {
        containerColor == null -> MiuixTheme.colorScheme.surface
        containerColor == Color.Transparent -> MiuixTheme.colorScheme.background
        else -> containerColor
    }
}

/**
 * The side gutters are the large-screen centring insets from [appPageSideGutterStart] and
 * [appPageSideGutterEnd]; both are `0.dp` on every phone, so the defaults keep this function's old behaviour
 * exactly. Call sites that can see a composition should pass the real ones — [AppPageLazyColumn] does.
 *
 * Two of them, not one, because only the leading edge has the sidebar rail on it. A symmetric gutter is
 * right for centring and wrong for the rail, and taking one number for both is what slid every list card on
 * OS, MCP, GitHub and BA underneath it the moment the navigation moved to the side.
 */
fun appPageContentPadding(
    innerPadding: PaddingValues,
    bottomExtra: Dp = AppChromeTokens.pageBottomInsetExtra,
    topExtra: Dp = 0.dp,
    sideGutterStart: Dp = 0.dp,
    sideGutterEnd: Dp = 0.dp,
): PaddingValues =
    AppPageInsetPaddingValues(
        base = innerPadding,
        topExtra = topExtra,
        bottomExtra = bottomExtra,
        start = AppChromeTokens.pageHorizontalPadding + sideGutterStart,
        end = AppChromeTokens.pageHorizontalPadding + sideGutterEnd,
    )

/** Preserve dynamic scaffold insets until the consuming layout reads them. */
private class AppPageInsetPaddingValues(
    private val base: PaddingValues,
    private val topExtra: Dp = 0.dp,
    private val bottomExtra: Dp = 0.dp,
    private val start: Dp = 0.dp,
    private val end: Dp = 0.dp,
    private val includeTop: Boolean = true,
    private val includeBottom: Boolean = true,
) : PaddingValues {
    override fun calculateTopPadding(): Dp = if (includeTop) base.calculateTopPadding() + topExtra else 0.dp
    override fun calculateBottomPadding(): Dp = (if (includeBottom) base.calculateBottomPadding() else 0.dp) + bottomExtra
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = if (layoutDirection == LayoutDirection.Ltr) start else end
    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = if (layoutDirection == LayoutDirection.Ltr) end else start
}

fun appPageBottomPaddingWithFloatingOverlay(contentBottomPadding: Dp): Dp =
    contentBottomPadding + AppChromeTokens.pageFloatingOverlayBottomExtra

@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingToolbar: @Composable () -> Unit = {},
    floatingToolbarPosition: ToolbarPosition = ToolbarPosition.BottomCenter,
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val resolvedContainerColor =
        containerColor
            ?: LocalAppScaffoldContainerColor.current
            ?: MiuixTheme.colorScheme.surface
    MiuixScaffold(
        modifier = modifier,
        containerColor = resolvedContainerColor,
        topBar = topBar,
        bottomBar = bottomBar,
        floatingToolbar = floatingToolbar,
        floatingToolbarPosition = floatingToolbarPosition,
        snackbarHost = snackbarHost,
        content = content,
    )
}

@Composable
fun AppPageScaffold(
    title: String,
    modifier: Modifier = Modifier,
    largeTitle: String = title,
    scrollBehavior: ScrollBehavior? = null,
    topBarColor: Color = Color.Transparent,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    titleBackdrop: Backdrop? = null,
    reserveTopEndActionSpace: Boolean = false,
    bottomBar: @Composable () -> Unit = {},
    floatingToolbar: @Composable () -> Unit = {},
    floatingToolbarPosition: ToolbarPosition = ToolbarPosition.BottomCenter,
    snackbarHost: @Composable () -> Unit = {},
    onTitleClick: () -> Unit = {},
    /**
     * Widest this page's content column may get.
     *
     * Published to the whole page rather than handed to the list, because the top row is laid out beside the
     * content, not inside it: both ends of that row centre against this, so a page that widens its column
     * without saying so leaves its own back button behind on the old one.
     */
    contentMaxWidth: Dp = AppPageContentMaxWidth,
    content: @Composable (PaddingValues) -> Unit,
) {
    val currentContent = rememberUpdatedState(content)
    val scaffoldTopBar: @Composable () -> Unit =
        remember(
            scrollBehavior,
            topBarColor,
        ) {
            {
                AppTopBarSection(
                    title = "",
                    largeTitle = "",
                    scrollBehavior = scrollBehavior,
                    color = topBarColor,
                )
            }
        }
    CompositionLocalProvider(LocalAppPageContentMaxWidth provides contentMaxWidth) {
        Box(modifier = modifier) {
            AppScaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = scaffoldTopBar,
                bottomBar = bottomBar,
                floatingToolbar = floatingToolbar,
                floatingToolbarPosition = floatingToolbarPosition,
                snackbarHost = snackbarHost,
                content = { innerPadding ->
                    currentContent.value(innerPadding)
                },
            )
            AppTopBarSection(
                title = title,
                largeTitle = largeTitle,
                scrollBehavior = scrollBehavior,
                color = Color.Transparent,
                navigationIcon = navigationIcon,
                titleBackdrop = titleBackdrop,
                titleEndReserve =
                    if (reserveTopEndActionSpace) {
                        AppChromeTokens.topBarTitleActionReserve
                    } else {
                        null
                    },
                onTitleClick = onTitleClick,
            )
            AppTopEndActionBarOverlay {
                Row {
                    actions()
                }
            }
        }
    }
}

@Composable
fun AppPageLazyColumn(
    innerPadding: PaddingValues,
    state: LazyListState,
    modifier: Modifier = Modifier,
    bottomExtra: Dp = AppChromeTokens.pageBottomInsetExtra,
    topExtra: Dp = AppChromeTokens.topBarToHeaderGap,
    sectionSpacing: Dp = AppChromeTokens.pageSectionGapLarge,
    userScrollEnabled: Boolean = true,
    /**
     * Widest this page's content column may get before the surplus becomes gutter.
     *
     * Opt-in, and the default is the single-column cap every page has had. A page that lays its cards out in
     * two columns passes [appPageContentMaxWidthFor], which roughly doubles it — otherwise two columns would
     * be carved out of one column's width and each half would be narrower than a phone.
     */
    maxContentWidth: Dp = LocalAppPageContentMaxWidth.current,
    // Follows LocalOverscrollFactory: MiuixOverscrollFactory app-wide (spring placement
    // translation, no RenderEffect), lifting the 767b191c3 global disable.
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        state = state,
        overscrollEffect = overscrollEffect,
        userScrollEnabled = userScrollEnabled,
        contentPadding =
            appPageContentPadding(
                innerPadding = innerPadding,
                bottomExtra = bottomExtra,
                topExtra = topExtra,
                // Padding rather than a width constraint on the list itself, so the scroll surface, the
                // overscroll stretch and the edge-stacked card pile still span the whole panel. Only the
                // content is centred; the gesture area is not narrowed.
                sideGutterStart = appPageSideGutterStart(maxContentWidth),
                sideGutterEnd = appPageSideGutterEnd(maxContentWidth),
            ),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing),
        content = content,
    )
}

/**
 * Two content columns that scroll independently.
 *
 * The staggered grid this replaces gave the width but not the throughput: one scroll container means a
 * flick anywhere moves both columns, so a tablet still walks a page at a phone's pace and the second column
 * is only ever decoration. Two containers make each column its own list — the one under your finger moves
 * and the other stays where you left it, which is the point of having two.
 *
 * The cost is that lanes can no longer be packed by height. A masonry layout decides which column a card
 * lands in by measuring the columns, and that answer is meaningless once they scroll apart. So the caller
 * assigns lanes, and every page that uses this has a rule for it: alternate for peers, or by kind where the
 * page has a reason — see `osCardLanes` and BA's card groups.
 *
 * Horizontal padding sits on the row rather than in each list's content padding, because the two lists now
 * have an inside edge that is not a page edge; [AppPageColumnGap] is that edge, and only the outer two
 * carry the page gutter.
 */
@Composable
fun AppPageTwoColumnLists(
    innerPadding: PaddingValues,
    primaryState: LazyListState,
    secondaryState: LazyListState,
    modifier: Modifier = Modifier,
    bottomExtra: Dp = AppChromeTokens.pageBottomInsetExtra,
    topExtra: Dp = AppChromeTokens.topBarToHeaderGap,
    sectionSpacing: Dp = AppChromeTokens.pageSectionGapLarge,
    userScrollEnabled: Boolean = true,
    maxContentWidth: Dp = LocalAppPageContentMaxWidth.current,
    header: (@Composable () -> Unit)? = null,
    primary: LazyListScope.() -> Unit,
    secondary: LazyListScope.() -> Unit,
) {
    // Asymmetric: only the leading edge has the sidebar rail on it, so the two edges cannot share one
    // number. Identical on a phone and on any tablet whose navigation is at the top, where the rail's
    // contribution is zero.
    val gutterStart = appPageSideGutterStart(maxContentWidth)
    val gutterEnd = appPageSideGutterEnd(maxContentWidth)
    // The top inset belongs to whichever element is actually at the top. Handing it to both would inset the
    // columns a second time below the header; handing it to neither loses the header entirely — a page
    // wrapped in AppEdgeStackKeepAlive is measured taller than its bounds and placed that far *up*, so an
    // un-inset header sits in the invisible headroom above the screen and is clipped away. That is exactly
    // what hid BA's account card.
    val columnPadding =
        remember(innerPadding, topExtra, bottomExtra, header == null) {
            AppPageInsetPaddingValues(innerPadding, topExtra, bottomExtra, includeTop = header == null)
        }
    Column(
        modifier =
            modifier.padding(
                start = AppChromeTokens.pageHorizontalPadding + gutterStart,
                end = AppChromeTokens.pageHorizontalPadding + gutterEnd,
            ),
    ) {
        // Anything that belongs to the page rather than to one column — an account switcher, a status hub —
        // sits above both and does not scroll with either. In one column it could simply be the first card;
        // across two there is no "first". It stays put while the columns move under it, which for the card
        // that names whose data this is reads as a gain rather than a loss.
        if (header != null) {
            val headerPadding = remember(innerPadding, topExtra, sectionSpacing) {
                AppPageInsetPaddingValues(innerPadding, topExtra, sectionSpacing, includeBottom = false)
            }
            Box(modifier = Modifier.padding(headerPadding)) {
                header()
            }
        }
        Row(
            // Weighted, so a header takes its height off the columns rather than out of the window.
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(AppPageColumnGap),
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                state = primaryState,
                userScrollEnabled = userScrollEnabled,
                contentPadding = columnPadding,
                verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                content = primary,
            )
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                state = secondaryState,
                userScrollEnabled = userScrollEnabled,
                contentPadding = columnPadding,
                verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                content = secondary,
            )
        }
    }
}

/**
 * Two content panes side by side, each filling the page's height.
 *
 * [AppPageTwoColumnLists] is for a page whose columns are *lists* -- an unknown number of cards, packed by a
 * lane rule, scrolled independently. This is for the other shape: a page that is exactly two things, both of
 * which want to be as tall as the window rather than as tall as their content. Shell is that page -- a
 * command editor and its output -- and stacking them left a card of each in the middle of the panel with the
 * rest of it empty.
 *
 * Neither pane scrolls the page; each scrolls inside itself, which is why nothing here takes a
 * [LazyListState] and why the caller should not hand this a nested-scroll connection. Collapsing the top bar
 * would change [innerPadding] under panes that are measured from it, so the bar stays put and the split
 * holds still.
 *
 * Same gutter and same content cap as the rest of the page family, so a page can switch between this and a
 * single column without its chrome moving.
 */
@Composable
fun AppPageTwoColumnPanes(
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
    topExtra: Dp = AppChromeTokens.topBarToHeaderGap,
    bottomExtra: Dp = AppChromeTokens.pageBottomInsetExtra,
    maxContentWidth: Dp = LocalAppPageContentMaxWidth.current,
    primary: @Composable () -> Unit,
    secondary: @Composable () -> Unit,
) {
    val gutterStart = appPageSideGutterStart(maxContentWidth)
    val gutterEnd = appPageSideGutterEnd(maxContentWidth)
    val panePadding = remember(innerPadding, topExtra, bottomExtra) {
        AppPageInsetPaddingValues(innerPadding, topExtra, bottomExtra)
    }
    Row(
        modifier =
            modifier
                .padding(
                    start = AppChromeTokens.pageHorizontalPadding + gutterStart,
                    end = AppChromeTokens.pageHorizontalPadding + gutterEnd,
                ).padding(panePadding),
        horizontalArrangement = Arrangement.spacedBy(AppPageColumnGap),
    ) {
        // The weight lives here rather than in the slots, so a caller cannot give one pane more than the
        // other by accident: two panes, one split, and each fills what it is given.
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) { primary() }
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) { secondary() }
    }
}

/**
 * Splits a page's cards into two lanes by alternating them.
 *
 * For a page whose cards are peers — Settings, About, MCP — this is the assignment that keeps reading order
 * intact across a row: first card top-left, second top-right, third under the first. Height packing would
 * read better down a single scroll and is exactly what independent columns cannot have, since the columns
 * no longer share a baseline to pack against.
 */
fun <T> appPageAlternatingLanes(cards: List<T>): Pair<List<T>, List<T>> =
    appPageAlternatingLanes(cards, columnCount = 2).let { lanes -> lanes[0] to lanes[1] }

/**
 * The same rule for any number of lanes, and for a list that is genuinely *ordered* rather than a set of
 * peer cards.
 *
 * Halving would be the obvious split and is the wrong one: lanes scroll independently, so a first half and
 * a second half would put the middle of the list at the top of the second lane and make browsing it
 * nonsense. Alternating keeps every lane sorted on its own, and while the lanes sit level with each other,
 * reading across a row is the list from start to end.
 *
 * Wrap the items in [withIndex] first where the position in the flat list still matters — a staggered
 * entrance animation, say — since a lane on its own no longer knows it.
 */
fun <T> appPageAlternatingLanes(
    items: List<T>,
    columnCount: Int,
): List<List<T>> {
    val columns = columnCount.coerceAtLeast(1)
    if (columns == 1) return listOf(items)
    return List(columns) { lane ->
        val size = (items.size - lane + columns - 1) / columns
        ArrayList<T>(size.coerceAtLeast(0)).also { laneItems ->
            var index = lane
            while (index < items.size) {
                laneItems += items[index]
                index += columns
            }
        }
    }
}

/**
 * [AppPageLazyColumn]'s layout contract, in columns that pack independently.
 *
 * The same content padding, the same gutter, the same section rhythm — only the flow differs. A staggered
 * grid rather than rows of equal-height cells because these cards are accordions: pairing them by row means
 * a collapsed card sits beside an expanded one and the shorter side leaves a dead half-column, which is the
 * exact complaint this exists to answer. Each column here takes the next card as soon as it has room, so a
 * tall card on the left simply lets the right column run ahead.
 *
 * Still lazy, which is the reason not to hand-build two [androidx.compose.foundation.layout.Column]s inside a
 * single list item: that would give the same packing while composing every card at once, and the Interface
 * category alone is nine cards of sliders and pickers.
 */
@Composable
fun AppPageStaggeredGrid(
    innerPadding: PaddingValues,
    state: LazyStaggeredGridState,
    columnCount: Int,
    modifier: Modifier = Modifier,
    bottomExtra: Dp = AppChromeTokens.pageBottomInsetExtra,
    topExtra: Dp = AppChromeTokens.topBarToHeaderGap,
    sectionSpacing: Dp = AppChromeTokens.pageSectionGapLarge,
    userScrollEnabled: Boolean = true,
    maxContentWidth: Dp = LocalAppPageContentMaxWidth.current,
    content: LazyStaggeredGridScope.() -> Unit,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columnCount),
        modifier = modifier,
        state = state,
        userScrollEnabled = userScrollEnabled,
        contentPadding =
            appPageContentPadding(
                innerPadding = innerPadding,
                bottomExtra = bottomExtra,
                topExtra = topExtra,
                sideGutterStart = appPageSideGutterStart(maxContentWidth),
                sideGutterEnd = appPageSideGutterEnd(maxContentWidth),
            ),
        verticalItemSpacing = sectionSpacing,
        horizontalArrangement = Arrangement.spacedBy(AppPageColumnGap),
        content = content,
    )
}
