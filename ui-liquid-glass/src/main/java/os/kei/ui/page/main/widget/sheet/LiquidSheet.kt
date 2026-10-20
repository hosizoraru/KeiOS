@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.sheet

import android.view.WindowInsets as AndroidWindowInsets
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.launch
import os.kei.ui.page.main.widget.isAppInDarkTheme
import os.kei.ui.page.main.widget.glass.claimFloatingChromeCrossAxisDrags
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdropOverridesFallback
import os.kei.ui.page.main.widget.glass.LocalRetainedLiquidMaterial
import os.kei.ui.page.main.widget.motion.LocalPredictiveBackAnimationsEnabled
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.theme.LocalDismissState
import kotlin.math.roundToInt

private const val LIQUID_SHEET_BLOCKED_DRAG_RESISTANCE = 0.35f
/**
 * Identifies the sheet's panel, for anything that has to wait for a sheet without knowing which one.
 *
 * Mirrors `SnapshotMenuPanelTestTag`. The baseline profile needs it: a sheet's first composition runs
 * inside the present transition, so an interpreted class there costs a dropped frame rather than a
 * slower launch, and no journey could reach a sheet at all before this.
 */
const val LiquidSheetPanelTestTag = "liquid_sheet_panel"

/**
 * The shared drag region above every Liquid sheet.
 *
 * A profile or frame benchmark needs to start the gesture on the grabber instead of the scrolling
 * content. Keeping this component-owned mirrors [LiquidSheetPanelTestTag] and avoids one tag per
 * business sheet.
 */
const val LiquidSheetDragRegionTestTag = "liquid_sheet_drag_region"

private val LiquidSheetDismissVelocityThreshold = 800.dp

private const val LIQUID_SHEET_ENTER_DAMPING = 0.92f
private const val LIQUID_SHEET_ENTER_RESPONSE = 0.38f

// Critically damped. A sheet that overshoots on the way out bounces back into view before
// disappearing, which reads as a flinch rather than an exit.
private const val LIQUID_SHEET_EXIT_DAMPING = 1f
private const val LIQUID_SHEET_EXIT_RESPONSE = 0.30f

private const val LIQUID_SHEET_SETTLE_DAMPING = 0.85f
private const val LIQUID_SHEET_SETTLE_RESPONSE = 0.40f

private fun enterSpec(): AnimationSpec<Float> =
    folmeSpring(damping = LIQUID_SHEET_ENTER_DAMPING, response = LIQUID_SHEET_ENTER_RESPONSE)

private fun exitSpec(): AnimationSpec<Float> =
    folmeSpring(damping = LIQUID_SHEET_EXIT_DAMPING, response = LIQUID_SHEET_EXIT_RESPONSE)

private fun settleSpec(): AnimationSpec<Float> =
    folmeSpring(damping = LIQUID_SHEET_SETTLE_DAMPING, response = LIQUID_SHEET_SETTLE_RESPONSE)

/**
 * An in-window Liquid Glass sheet.
 *
 * ## One driver, one exit
 *
 * The sheet's whole vertical state is a single normalised float: `hidden`, where `0` is resting and
 * `1` is translated fully below the window. Placement, the scrim's dim, the background blur's alpha
 * and the glass strength are all *derived* from it — nothing else animates.
 *
 * That is the fix for the dismiss flinch. The sheet this replaces animated four independent values
 * (`animationProgress`, `dismissOffsetY`, `dimAlpha`, `visibleSheetHeightPx`) along two different
 * exit paths: dragging away drove `dimAlpha` to zero, while tapping outside drove only
 * `animationProgress` and left `dimAlpha` at 1. Since the dim was floored at 28% of full opacity, an
 * outside tap faded the sheet out and then cut a 28%-dimmed screen to nothing the instant the Dialog
 * window was torn down. With one driver the scrim reaches zero exactly as the sheet reaches the
 * bottom edge, so there is nothing left to cut — whichever gesture started the dismissal.
 *
 * Every dismissal — outside tap, back, predictive back, drag, or the caller flipping `show` — routes
 * through `onDismissRequest`, and the single `LaunchedEffect(show)` below owns the animation. Release
 * velocity reaches it through `exitVelocity` so a fling stays continuous instead of restarting.
 *
 * ## Why it is not a Dialog
 *
 * See [os.kei.ui.page.main.widget.glass.LiquidOverlayHostState]: a `LayerBackdrop` cannot be sampled
 * from another window, so a sheet in its own window can only imitate glass with a flat fill.
 */
@Composable
internal fun LiquidSheetPresentation(
    show: Boolean,
    onMountedChanged: (Boolean) -> Unit,
    modifier: Modifier,
    title: String?,
    startAction: @Composable (() -> Unit)?,
    endAction: @Composable (() -> Unit)?,
    solidness: Float,
    surfaceTone: LiquidSheetSurfaceTone,
    explicitBackgroundColor: Color?,
    fallbackSurfaceColor: Color,
    enableDim: Boolean,
    scrimBlurRadius: Dp,
    cornerRadius: Dp,
    sheetMaxWidth: Dp,
    outsideMargin: DpSize,
    insideMargin: DpSize,
    applyImePadding: Boolean,
    dragHandleColor: Color,
    allowDismiss: Boolean,
    enableNestedScroll: Boolean,
    minimumHeight: Dp,
    topInset: Dp,
    dismissDragThreshold: Dp,
    onDismissRequest: (() -> Unit)?,
    onDismissFinished: (() -> Unit)?,
    onBlockedDismissRequest: (() -> Unit)?,
    preferExportedBackdrop: Boolean,
    content: @Composable () -> Unit,
) {
    // The sheet's own live handle on its content's scroll position, provided to the content below and
    // read straight out of the nested-scroll callbacks. Replaces a reporter round trip that was both
    // late and lossy — see [LiquidSheetContentScroll].
    val contentScroll = rememberLiquidSheetContentScroll()
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val scope = rememberCoroutineScope()
    val transitionsEnabled = LocalTransitionAnimationsEnabled.current
    val predictiveBackEnabled = LocalPredictiveBackAnimationsEnabled.current

    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val currentOnDismissFinished by rememberUpdatedState(onDismissFinished)
    val currentOnBlockedDismissRequest by rememberUpdatedState(onBlockedDismissRequest)
    val currentAllowDismiss by rememberUpdatedState(allowDismiss)
    val currentOnMountedChanged by rememberUpdatedState(onMountedChanged)

    // The single motion driver, in units of sheet-heights below rest.
    val hidden = remember { mutableFloatStateOf(1f) }
    val exitVelocity = remember { mutableFloatStateOf(0f) }
    // Latches for the duration of one dismissal, so a second gesture cannot dispatch a second
    // request and the finger cannot grab a sheet that is already leaving.
    val dismissInProgress = remember { mutableStateOf(false) }
    // Bumped whenever the finger takes over, so an animation still in flight stops writing instead
    // of fighting the drag for the same value.
    val motionGeneration = remember { mutableIntStateOf(0) }
    // The same guard for the height, which needs its own counter because height and dismiss progress
    // animate independently. `resizeTo` used to be the one animated value no generation covered, so
    // two grabber taps — or a tap racing a drag — left two coroutines writing `resizedHeightPx` from
    // two different spring trajectories, and `currentHeightPx()` was whichever landed last. Every
    // drag decision is taken against that value.
    val heightGeneration = remember { mutableIntStateOf(0) }

    // Height is orthogonal to presentation: dragging the grabber resizes the sheet while its bottom
    // edge stays anchored, which has nothing to do with sliding it off-screen.
    val naturalHeightPx = remember { mutableIntStateOf(0) }
    val renderedHeightPx = remember { mutableIntStateOf(0) }
    val overlayHeightPx = remember { mutableIntStateOf(0) }
    val resizedHeightPx = remember { mutableFloatStateOf(0f) }
    val userResized = remember { mutableStateOf(false) }

    val windowHeightPx = with(density) { windowInfo.containerDpSize.height.toPx() }
    // Whether the sheet's own surface reaches both window edges, which is what decides how far the
    // scrim's blurred plate has to reach. See [liquidSheetScrimBlurHeightPx].
    val sheetSpansWindowWidth =
        liquidSheetSpansWindowWidth(
            windowWidth = windowInfo.containerDpSize.width,
            sheetMaxWidth = sheetMaxWidth,
            outsideMarginWidth = outsideMargin.width,
        )
    val topInsetPx = with(density) { topInset.toPx() }
    val minimumHeightPx = with(density) { minimumHeight.toPx() }
    val dismissThresholdPx = with(density) { dismissDragThreshold.toPx() }
    val dismissVelocityPx = with(density) { LiquidSheetDismissVelocityThreshold.toPx() }
    val maxSheetHeight = with(density) { (windowHeightPx - topInsetPx).coerceAtLeast(0f).toDp() }

    fun maxHeightPx(): Float = (windowHeightPx - topInsetPx).coerceAtLeast(0f)

    fun currentHeightPx(): Float {
        val requested = when {
            resizedHeightPx.floatValue > 0f -> resizedHeightPx.floatValue
            naturalHeightPx.intValue > 0 -> naturalHeightPx.intValue.toFloat()
            else -> maxHeightPx()
        }
        return requested.coerceIn(0f, maxHeightPx())
    }

    fun minVisibleHeightPx(): Float {
        val floor = minimumHeightPx.coerceAtMost(maxHeightPx())
        val natural = naturalHeightPx.intValue.toFloat()
        // A sheet already shorter than the floor is as small as it goes.
        return if (natural in 1f..floor) natural else floor
    }

    fun offsetPx(): Float = liquidSheetOffsetPx(hidden.floatValue, currentHeightPx())

    fun presentation(): Float = liquidSheetPresentation(hidden.floatValue)

    fun scrimBlurHeightPx(): Int {
        val overlayHeight = overlayHeightPx.intValue
        val renderedHeight = renderedHeightPx.intValue
        return liquidSheetScrimBlurHeightPx(
            windowHeightPx = if (overlayHeight > 0) overlayHeight.toFloat() else windowHeightPx,
            sheetHeightPx = if (renderedHeight > 0) renderedHeight.toFloat() else currentHeightPx(),
            offsetPx = offsetPx(),
            cornerRadiusPx = with(density) { cornerRadius.toPx() },
            sheetSpansWindowWidth = sheetSpansWindowWidth,
        )
    }

    // Built here rather than passed in: the material has to invert the sheet's own translation for
    // its backdrop sample, so it needs `offsetPx` — which only exists at this level.
    val surface = rememberLiquidSheetSurface(
        cornerRadius = cornerRadius,
        solidness = solidness,
        surfaceTone = surfaceTone,
        isDark = isAppInDarkTheme(),
        explicitBackgroundColor = explicitBackgroundColor,
        fallbackColor = fallbackSurfaceColor,
        offsetProvider = ::offsetPx,
    )

    suspend fun animateHiddenTo(
        target: Float,
        spec: AnimationSpec<Float>,
        initialVelocity: Float = 0f,
    ) {
        val generation = motionGeneration.intValue + 1
        motionGeneration.intValue = generation
        animate(
            initialValue = hidden.floatValue,
            targetValue = target,
            initialVelocity = initialVelocity,
            animationSpec = spec,
        ) { value, _ ->
            if (motionGeneration.intValue == generation) hidden.floatValue = value
        }
        if (motionGeneration.intValue == generation) hidden.floatValue = target
    }

    /** Hands both values to the finger and silences any running animation. */
    fun takeOverMotion() {
        motionGeneration.intValue += 1
        heightGeneration.intValue += 1
    }

    /** Animates the sheet's height, last writer winning over any snap already in flight. */
    suspend fun animateHeightTo(
        target: Float,
        spec: AnimationSpec<Float>,
        initialVelocity: Float = 0f,
    ) {
        val generation = heightGeneration.intValue + 1
        heightGeneration.intValue = generation
        userResized.value = true
        animate(
            initialValue = currentHeightPx(),
            targetValue = target,
            initialVelocity = initialVelocity,
            animationSpec = spec,
        ) { value, _ ->
            if (heightGeneration.intValue == generation) resizedHeightPx.floatValue = value
        }
        // Landing on the target *exactly* is the point, not a tidy-up: `liquidSheetCanGrow` compares
        // against the maximum, and a sub-pixel shortfall there re-arms expand-to-scroll forever.
        if (heightGeneration.intValue == generation) resizedHeightPx.floatValue = target
    }

    /** The one exit animation. Both the gesture path and the programmatic path call exactly this. */
    suspend fun runExit(velocity: Float) {
        if (!transitionsEnabled) {
            takeOverMotion()
            hidden.floatValue = 1f
            return
        }
        val height = currentHeightPx().coerceAtLeast(1f)
        animateHiddenTo(target = 1f, spec = exitSpec(), initialVelocity = velocity / height)
    }

    LaunchedEffect(show, transitionsEnabled) {
        if (show) {
            currentOnMountedChanged(true)
            dismissInProgress.value = false
            exitVelocity.floatValue = 0f
            if (transitionsEnabled) {
                animateHiddenTo(0f, enterSpec())
            } else {
                takeOverMotion()
                hidden.floatValue = 0f
            }
        } else {
            // A gesture-driven dismissal has already flown the sheet out and is about to unmount it;
            // re-running the exit here would restart the animation from the bottom edge.
            if (hidden.floatValue < 1f) {
                runExit(exitVelocity.floatValue)
            }
            currentOnDismissFinished?.invoke()
            currentOnMountedChanged(false)
        }
    }

    /**
     * Requests dismissal, animating the sheet out first.
     *
     * Two properties here are load-bearing and were both in the sheet this replaces:
     *
     * - **At most one request per dismissal.** Without the latch, frantic back presses or a
     *   double-tapped Cancel each dispatch, and callers that pop a route or reset state would do it
     *   twice — the request goes out immediately while `show` needs a frame to come back false.
     * - **`allowDismiss` is re-read after the animation.** A sheet whose content turns dirty while it
     *   is flying out springs back instead of vanishing with unsaved edits.
     */
    fun requestDismiss(velocity: Float) {
        if (dismissInProgress.value) return
        if (!currentAllowDismiss) {
            currentOnBlockedDismissRequest?.invoke()
            scope.launch { animateHiddenTo(0f, settleSpec()) }
            return
        }
        dismissInProgress.value = true
        exitVelocity.floatValue = velocity
        scope.launch {
            try {
                runExit(velocity)
                if (currentAllowDismiss) {
                    currentOnDismissRequest?.invoke()
                } else {
                    currentOnBlockedDismissRequest?.invoke()
                    animateHiddenTo(0f, settleSpec())
                }
            } finally {
                dismissInProgress.value = false
            }
        }
    }

    /**
     * Turns a vertical drag into either a resize or dismiss progress, reporting what it consumed so
     * nested scrolling stays honest.
     */
    fun applyDrag(delta: Float): Float {
        if (delta == 0f || dismissInProgress.value) return 0f
        takeOverMotion()
        val result = liquidSheetResolveDrag(
            deltaPx = delta,
            heightPx = currentHeightPx(),
            hidden = hidden.floatValue,
            minVisibleHeightPx = minVisibleHeightPx(),
            maxVisibleHeightPx = maxHeightPx(),
            resistance = if (currentAllowDismiss) 1f else LIQUID_SHEET_BLOCKED_DRAG_RESISTANCE,
        )
        if (result.heightPx != currentHeightPx()) userResized.value = true
        resizedHeightPx.floatValue = result.heightPx
        hidden.floatValue = result.hidden
        return result.consumedPx
    }

    fun resizeTo(targetHeightPx: Float) {
        if (dismissInProgress.value) return
        val resolved = targetHeightPx.coerceIn(minVisibleHeightPx(), maxHeightPx())
        if (!transitionsEnabled) {
            userResized.value = true
            heightGeneration.intValue += 1
            resizedHeightPx.floatValue = resolved
            return
        }
        scope.launch { animateHeightTo(resolved, settleSpec()) }
    }

    // These are queried by semantics and pointer callbacks. Keeping the hot resized-height read out
    // of Composition prevents every drag delta and every height-spring frame from recomposing the
    // whole sheet tree; the required height change still invalidates Layout below.
    fun canExpand(): Boolean = liquidSheetCanGrow(currentHeightPx(), maxHeightPx())

    fun canCollapse(): Boolean =
        currentHeightPx() > minVisibleHeightPx() + LIQUID_SHEET_HEIGHT_EPSILON_PX

    /**
     * Ends a drag the sheet owned: dismiss, or spring back to rest.
     *
     * Deliberately does **not** snap the height to a detent. A resize is meant to hold where the
     * finger left it — the grabber is a continuous control and tapping it is what cycles detents —
     * and `LiquidGlassBottomSheetTest` pins that in three places. Leaving the sheet a little below
     * its maximum costs only that gap out of the next upward drag, because `applyDrag` reports what
     * it actually used and hands the remainder straight on to the content.
     */
    fun settle(velocity: Float) {
        if (dismissInProgress.value) return
        val height = currentHeightPx().coerceAtLeast(1f)
        val draggedPx = hidden.floatValue * height
        val effectiveDrag =
            if (currentAllowDismiss) draggedPx else draggedPx / LIQUID_SHEET_BLOCKED_DRAG_RESISTANCE
        if (
            liquidSheetDismissDragExceeded(
                draggedPx = effectiveDrag,
                thresholdPx = dismissThresholdPx,
                velocity = velocity,
                velocityThresholdPx = dismissVelocityPx,
            )
        ) {
            requestDismiss(velocity)
            return
        }
        if (hidden.floatValue != 0f) {
            scope.launch { animateHiddenTo(0f, settleSpec(), initialVelocity = velocity / height) }
        }
    }

    // ---- back ---------------------------------------------------------------------------------

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    // The insets object is stable; only `getBottom` reads state. Calling it inside callbacks rather
    // than during composition keeps the IME's per-frame inset animation from recomposing the whole
    // sheet — and the sheet's glass is the most expensive thing on screen to re-record.
    val imeInsets = WindowInsets.ime
    val completeBack: () -> Unit = {
        val imeVisible = liquidSheetImeVisible(
            composeImeBottomPx = imeInsets.getBottom(density),
            platformImeVisible =
                view.rootWindowInsets?.isVisible(AndroidWindowInsets.Type.ime()) == true,
        )
        if (imeVisible) {
            // Back closes the keyboard first, as it does on every other text surface in the app.
            focusManager.clearFocus(force = true)
            view.windowInsetsController?.hide(AndroidWindowInsets.Type.ime())
                ?: keyboardController?.hide()
            scope.launch { animateHiddenTo(0f, settleSpec()) }
        } else {
            requestDismiss(0f)
        }
    }

    val navigationEventOwnerAvailable = LocalNavigationEventDispatcherOwner.current != null
    if (navigationEventOwnerAvailable) {
        val navigationEventState =
            rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(
            state = navigationEventState,
            isBackEnabled = show && predictiveBackEnabled,
            onBackCancelled = { scope.launch { animateHiddenTo(0f, settleSpec()) } },
            onBackCompleted = completeBack,
        )
        LaunchedEffect(navigationEventState, predictiveBackEnabled, allowDismiss) {
            if (!predictiveBackEnabled) return@LaunchedEffect
            snapshotFlow { navigationEventState.transitionState }.collect { transitionState ->
                if (
                    transitionState is NavigationEventTransitionState.InProgress &&
                    transitionState.direction == NavigationEventTransitionState.TRANSITIONING_BACK
                ) {
                    takeOverMotion()
                    val progress = transitionState.latestEvent.progress.coerceIn(0f, 1f)
                    val resistance =
                        if (allowDismiss) 1f else LIQUID_SHEET_BLOCKED_DRAG_RESISTANCE
                    hidden.floatValue = progress * resistance
                }
            }
        }
    }
    BackHandler(
        enabled = show && (!navigationEventOwnerAvailable || !predictiveBackEnabled),
        onBack = completeBack,
    )

    // ---- nested scroll ------------------------------------------------------------------------

    val nestedScrollConnection = remember(enableNestedScroll, allowDismiss, contentScroll) {
        // "The sheet consumed the delta that just arrived" — deliberately **not** "the sheet consumed
        // something during this gesture". See [liquidSheetShouldClaimFling]: the sticky version of
        // this flag is what ate every content fling.
        var sheetOwnsGesture = false
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!enableNestedScroll) return Offset.Zero
                val delta = available.y
                // Downward drags go to the content first; whatever it cannot use comes back to
                // `onPostScroll`, which is where shrinking and dismissing live.
                if (delta >= 0f) {
                    sheetOwnsGesture = false
                    return Offset.Zero
                }
                val owner =
                    liquidSheetUpwardDragOwner(
                        hidden = hidden.floatValue,
                        heightPx = currentHeightPx(),
                        maxHeightPx = maxHeightPx(),
                        contentCanScrollUp = contentScroll.canScrollUp,
                        contentOverflows = contentScroll.overflows,
                    )
                if (owner == LiquidSheetDragOwner.Content) {
                    sheetOwnsGesture = false
                    return Offset.Zero
                }
                val consumed = applyDrag(delta)
                sheetOwnsGesture = consumed != 0f
                return if (consumed != 0f) Offset(0f, consumed) else Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!enableNestedScroll) return Offset.Zero
                if (available.y > 0f) {
                    val consumedY = applyDrag(available.y)
                    if (consumedY != 0f) {
                        sheetOwnsGesture = true
                        return Offset(0f, consumedY)
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!enableNestedScroll) return Velocity.Zero
                if (available.y > 0f && contentScroll.canScrollUp) return Velocity.Zero
                if (
                    liquidSheetShouldClaimFling(
                        sheetOwnsGesture = sheetOwnsGesture,
                        hidden = hidden.floatValue,
                        heightPx = currentHeightPx(),
                    )
                ) {
                    settle(available.y)
                    sheetOwnsGesture = false
                    return available
                }
                // The sheet is at rest and the content was the one scrolling, so the fling belongs to
                // the content and the velocity is handed back untouched.
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (!enableNestedScroll) return Velocity.Zero
                if (
                    liquidSheetShouldClaimFling(
                        sheetOwnsGesture = sheetOwnsGesture,
                        hidden = hidden.floatValue,
                        heightPx = currentHeightPx(),
                    )
                ) {
                    settle(available.y)
                    sheetOwnsGesture = false
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    // ---- rendering ----------------------------------------------------------------------------

    Box(
        modifier = Modifier.fillMaxSize().onSizeChanged { size ->
            overlayHeightPx.intValue = size.height
        },
    ) {
        if (enableDim) {
            LiquidSheetScrim(
                backdrop = if (surface.glassEnabled) LocalSceneBackdrop.current else null,
                blurRadius = scrimBlurRadius,
                presentationProvider = ::presentation,
                blurHeightPxProvider = ::scrimBlurHeightPx,
            )
        }
        // Outside-tap dismissal, and a hard stop for drags that start off the sheet. Without the
        // claim a drag on the exposed area above the sheet reaches the pager, and the page behind a
        // modal surface scrolls or switches.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .liquidSheetOutsideDismiss(
                    allowDismiss = allowDismiss,
                    onDismissRequest = { requestDismiss(0f) },
                    onBlockedDismissRequest = onBlockedDismissRequest,
                ),
        )
        Column(
            modifier = modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = sheetMaxWidth)
                .fillMaxWidth()
                .padding(horizontal = outsideMargin.width)
                // The slide lives inside the surface, not in a graphicsLayer here: `drawBackdrop`
                // applies its `layerBlock` as the element's own layer *and* inverse-transforms the
                // sample by it, so translating separately would move the sheet twice.
                .then(surface.modifier)
                // Nothing that starts on the sheet may reach the pager underneath — without a claim
                // here, horizontal swipes across the panel dismissed the sheet outright.
                //
                // Cross-axis only, though. This surface *contains* a `verticalScroll`, and the
                // unrestricted claim consumed the very position changes that scrollable needs in
                // order to accumulate touch slop. A flick crossed slop in its first event or two and
                // survived; a slow, deliberate drag never crossed it at all and moved nothing. See
                // [claimFloatingChromeCrossAxisDrags] for the measurements.
                .claimFloatingChromeCrossAxisDrags()
                .then(
                    if (enableNestedScroll) Modifier.nestedScroll(nestedScrollConnection) else Modifier,
                ).wrapContentHeight()
                .heightIn(max = maxSheetHeight)
                // Reading `userResized` inside this Layout lambda avoids a one-off composition of
                // the complete material/content tree when the first drag takes ownership.
                .liquidSheetOptionalHeightPx {
                    if (userResized.value) currentHeightPx().roundToInt() else 0
                }.onSizeChanged { size ->
                    // The blur plate must meet the panel that is actually drawn. Natural height is
                    // deliberately frozen while the IME is open, but filtering can still shrink the
                    // content then. Reusing that pre-IME height leaves a sharp band above the sheet.
                    renderedHeightPx.intValue = size.height
                    // Read off the layout pass and never written back into it: placement is a
                    // graphicsLayer translation, so this cannot feed the measure loop the previous
                    // sheet created by writing its height from onGloballyPositioned.
                    if (imeInsets.getBottom(density) == 0 && !userResized.value) {
                        naturalHeightPx.intValue = size.height
                    }
                }.then(if (applyImePadding) Modifier.imePadding() else Modifier)
                .padding(horizontal = insideMargin.width)
                .padding(bottom = insideMargin.height)
                .semantics { title?.takeIf { it.isNotBlank() }?.let { paneTitle = it } }
                .testTag(LiquidSheetPanelTestTag),
        ) {
            LiquidSheetTopChrome(
                title = title,
                startAction = startAction,
                endAction = endAction,
                dragHandleColor = dragHandleColor,
                canExpand = ::canExpand,
                canCollapse = ::canCollapse,
                canDismiss = allowDismiss,
                onExpand = { resizeTo(maxHeightPx()) },
                onCollapse = { resizeTo(minVisibleHeightPx()) },
                onDismiss = { requestDismiss(0f) },
                onDrag = { applyDrag(it) },
                onDragStopped = { velocity -> settle(velocity) },
            )
            CompositionLocalProvider(
                LocalDismissState provides { requestDismiss(0f) },
                LocalLiquidSheetVisibleHeightPx provides {
                    if (userResized.value) currentHeightPx().roundToInt() else 0
                },
                LocalLiquidParentBackdrop provides surface.exportedBackdrop,
                LocalRetainedLiquidMaterial provides true,
                LocalLiquidParentBackdropOverridesFallback provides preferExportedBackdrop,
                LocalLiquidSheetContentScroll provides contentScroll,
            ) {
                Box(
                    modifier = Modifier.liquidSheetScrollEdge(
                        // Read in the draw phase, so the edge appears on the same frame the content
                        // moves and only invalidates draw. The reporter this replaced was a
                        // recomposition a frame or more later.
                        visible = { contentScroll.canScrollUp },
                        isDark = isAppInDarkTheme(),
                    ),
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * How present the sheet is, derived from the single motion driver.
 *
 * **This must reach exactly 0 when the sheet is fully hidden.** Everything that fades with the sheet —
 * the scrim dim, the background blur's alpha — is scaled by this, so a non-zero floor here means the
 * screen is still visibly dimmed at the instant the sheet is unmounted, and the user sees a cut. That
 * floor is precisely what made the old sheet flinch when dismissed by tapping outside: its dim depth
 * bottomed out at 0.28 instead of 0.
 */
internal fun liquidSheetPresentation(hidden: Float): Float = (1f - hidden).coerceIn(0f, 1f)

/**
 * Where to draw the sheet, in pixels below rest.
 *
 * Clamped at zero so the slightly underdamped enter spring cannot overshoot past rest and lift the
 * sheet off the bottom edge, which would flash a strip of background beneath it.
 */
internal fun liquidSheetOffsetPx(
    hidden: Float,
    heightPx: Float,
): Float = (hidden * heightPx).coerceAtLeast(0f)

/**
 * The scrim's blurred plate covers everything down to the sheet's top edge plus one corner radius, so
 * the blur continues behind the rounded shoulders instead of ending in a visible straight line across
 * them. It deliberately stops there: the sheet blurs its own region, and extending this underneath
 * would pay for those pixels twice.
 *
 * That trade only holds while the sheet actually covers everything below the plate, which is
 * [sheetSpansWindowWidth]. On a phone it always does — the width cap is 480dp against a 360-440dp window
 * and the outside margin is zero. On a tablet the cap wins and the sheet becomes a centred panel: measured
 * on the Pad AVD at 1280dp it is 592dp wide, leaving **344dp of bare window down each side**, and stopping
 * the plate at its top edge drew a hard horizontal line straight across all three — blurred above,
 * dimmed-but-sharp below, with the sheet floating in it.
 *
 * So a sheet that does not span the width asks for the whole height instead, the same way
 * [os.kei.ui.page.main.widget.dialog.LiquidModalPresentation] already does for cards, which never span it.
 * The double-draw under the sheet is the cheaper mistake: `docs/planning/liquid-sheet-frame-cost.md`
 * measured an open sheet's cost as the number of glass controls in it, explicitly *not* blurred area.
 */
/**
 * Whether the sheet's surface reaches both window edges.
 *
 * Both halves matter. The width cap is what turns the sheet into a centred panel on a tablet, and a
 * non-zero outside margin would leave a bare strip down each side even at a width that otherwise fills
 * the window — a seam is a seam whether it is 344dp or 8dp wide.
 */
internal fun liquidSheetSpansWindowWidth(
    windowWidth: Dp,
    sheetMaxWidth: Dp,
    outsideMarginWidth: Dp,
): Boolean = outsideMarginWidth <= 0.dp && sheetMaxWidth >= windowWidth

internal fun liquidSheetScrimBlurHeightPx(
    windowHeightPx: Float,
    sheetHeightPx: Float,
    offsetPx: Float,
    cornerRadiusPx: Float,
    sheetSpansWindowWidth: Boolean = true,
): Int {
    if (!sheetSpansWindowWidth) return Int.MAX_VALUE
    val sheetTop = windowHeightPx - sheetHeightPx + offsetPx
    return (sheetTop + cornerRadiusPx.coerceAtLeast(0f))
        .coerceIn(0f, windowHeightPx.coerceAtLeast(0f))
        .roundToInt()
}

internal data class LiquidSheetDragResult(
    val heightPx: Float,
    val hidden: Float,
    val consumedPx: Float,
)

/**
 * Resolves one vertical drag event into a new sheet height and dismiss progress.
 *
 * Dragging down shrinks the sheet until it reaches [minVisibleHeightPx]; only the part of the drag
 * *below* that floor turns into dismiss progress. Dragging up spends itself undoing dismiss progress
 * first, and grows the sheet with whatever is left.
 *
 * **Dismiss progress accumulates.** A drag arrives as a stream of small deltas, so assigning progress
 * from a single event instead of adding to it leaves the driver holding only the last delta's worth —
 * and a long drag past the floor never reaches the dismiss threshold no matter how far it is pulled.
 * That is a bug this function exists to keep fixed; [consumedPx] is what nested scrolling needs back.
 */
internal fun liquidSheetResolveDrag(
    deltaPx: Float,
    heightPx: Float,
    hidden: Float,
    minVisibleHeightPx: Float,
    maxVisibleHeightPx: Float,
    resistance: Float,
): LiquidSheetDragResult {
    val beforeOffset = liquidSheetOffsetPx(hidden, heightPx)
    var currentHidden = hidden
    var currentHeight = heightPx
    var remaining = deltaPx

    if (remaining < 0f && currentHidden > 0f) {
        val restoredPx = minOf(liquidSheetOffsetPx(currentHidden, currentHeight), -remaining)
        val normaliser = currentHeight.coerceAtLeast(1f)
        currentHidden = (currentHidden - restoredPx / normaliser).coerceAtLeast(0f)
        remaining += restoredPx
    }

    if (remaining != 0f) {
        val desired = currentHeight - remaining
        when {
            desired >= maxVisibleHeightPx -> currentHeight = maxVisibleHeightPx
            desired >= minVisibleHeightPx -> currentHeight = desired
            else -> {
                currentHeight = minVisibleHeightPx
                val surplusPx = minVisibleHeightPx - desired
                currentHidden += surplusPx * resistance / minVisibleHeightPx.coerceAtLeast(1f)
            }
        }
    }

    val consumed =
        (heightPx - currentHeight) +
            (liquidSheetOffsetPx(currentHidden, currentHeight) - beforeOffset)
    return LiquidSheetDragResult(
        heightPx = currentHeight,
        hidden = currentHidden,
        consumedPx = consumed,
    )
}

/**
 * How much slack counts as "already there", in pixels.
 *
 * Load-bearing. The sheet's height is animated and dragged in raw pixels, so it routinely comes to
 * rest a fraction of a pixel — or, when a gesture simply runs out, a few dozen pixels — short of the
 * maximum and stays there, because nothing snapped it. A bare `heightPx < maxHeightPx` is then
 * permanently true, so the sheet claimed the start of *every* upward drag to "grow", and returned
 * the whole delta as consumed. Measured on the Android 17 AVD: a sheet left at 2668px against a
 * 2700px maximum swallowed 100% of every subsequent upward drag, and the content never moved.
 *
 * [LiquidSheetTopChrome]'s `canExpand`/`canCollapse` already guarded themselves this way. The
 * gesture path did not, which is the whole difference between the grabber working and the content
 * scroll dying.
 */
internal const val LIQUID_SHEET_HEIGHT_EPSILON_PX = 1f

/** Who a vertical drag belongs to. */
internal enum class LiquidSheetDragOwner {
    /** Resize or dismiss the sheet; consume the delta. */
    Sheet,

    /** Leave the delta alone so the scrollable inside the sheet gets all of it. */
    Content,
}

/** Whether the sheet is far enough off its resting position that it must be put back first. */
internal fun liquidSheetIsOffRest(
    hidden: Float,
    heightPx: Float,
): Boolean = liquidSheetOffsetPx(hidden, heightPx) > LIQUID_SHEET_HEIGHT_EPSILON_PX

/** Whether the sheet has room to grow that is worth taking a drag for. */
internal fun liquidSheetCanGrow(
    heightPx: Float,
    maxHeightPx: Float,
): Boolean = heightPx < maxHeightPx - LIQUID_SHEET_HEIGHT_EPSILON_PX

/**
 * Decides who owns an upward drag — the gesture that scrolls content forward.
 *
 * Three rules, in order:
 *
 * 1. **A sheet that is off its resting position gets put back first.** Otherwise a half-dismissed
 *    sheet can never be pulled back up.
 * 2. **Expand-to-scroll**, Apple's behaviour for a sheet below its largest detent: growing the sheet
 *    reveals more content, so it wins over scrolling. But only when growing actually buys the
 *    content something — [contentOverflows] is what makes that true. Without that check a short
 *    sheet inflated to full height on the first upward drag and left several hundred pixels of empty
 *    glass under the content, which is measured behaviour, not a hypothetical.
 * 3. **Otherwise the content owns it.** Once the content has scrolled off its top it keeps the
 *    gesture, and once the sheet is at its maximum there is nothing left to grow.
 */
internal fun liquidSheetUpwardDragOwner(
    hidden: Float,
    heightPx: Float,
    maxHeightPx: Float,
    contentCanScrollUp: Boolean,
    contentOverflows: Boolean,
): LiquidSheetDragOwner =
    when {
        liquidSheetIsOffRest(hidden, heightPx) -> LiquidSheetDragOwner.Sheet
        contentCanScrollUp -> LiquidSheetDragOwner.Content
        contentOverflows && liquidSheetCanGrow(heightPx, maxHeightPx) -> LiquidSheetDragOwner.Sheet
        else -> LiquidSheetDragOwner.Content
    }

/**
 * Whether the sheet should take the release velocity instead of letting the content fling.
 *
 * [sheetOwnsGesture] must mean "the sheet consumed the **most recent** delta", not "the sheet
 * consumed something at some point during this gesture". That distinction is the bug: the flag it
 * replaces latched for the whole gesture, so the extremely common sequence *drag up → sheet grows to
 * full → content starts scrolling → flick* ended with the sheet claiming the entire fling and
 * returning it as consumed. The content stopped dead the instant the finger left the glass, on every
 * single scroll. Traced on the AVD as `preFling EAT v=… sheetConsumed=true hidden=0.0` — a fling
 * eaten while the sheet had nothing whatsoever to settle.
 */
internal fun liquidSheetShouldClaimFling(
    sheetOwnsGesture: Boolean,
    hidden: Float,
    heightPx: Float,
): Boolean = sheetOwnsGesture || liquidSheetIsOffRest(hidden, heightPx)

internal fun liquidSheetImeVisible(
    composeImeBottomPx: Int,
    platformImeVisible: Boolean,
): Boolean = composeImeBottomPx > 0 || platformImeVisible

internal fun liquidSheetDismissDragExceeded(
    draggedPx: Float,
    thresholdPx: Float,
    velocity: Float,
    velocityThresholdPx: Float,
): Boolean = draggedPx > 0f && (draggedPx > thresholdPx || velocity > velocityThresholdPx)
