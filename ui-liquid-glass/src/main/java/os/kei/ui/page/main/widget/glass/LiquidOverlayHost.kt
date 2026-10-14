@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Renders on-top chrome inside the activity window instead of a Dialog window.
 *
 * A `LayerBackdrop` only samples correctly inside the window that produced it — the offset between
 * consumer and producer is resolved through shared `LayoutCoordinates`, which two windows do not
 * have. That is why [os.kei.ui.page.main.widget.glass.LiquidBackdropWindowBoundary] blanks
 * `LocalSceneBackdrop` for Dialog and Popup content: a sheet hosted in its own window can only fake
 * glass with a flat translucent fill, because every `blur()` it asks for resolves against
 * `emptyBackdrop()` and draws nothing.
 *
 * So anything that wants real Liquid Glass has to stay in the activity window. Compose has no
 * in-window z-order escape — drawing order follows the tree, so a sheet declared inside a page draws
 * underneath the floating docks, toolbar and bottom bar that the host declares after it. This host
 * is the escape hatch: callers register content from wherever they sit in the tree and it is composed
 * at the top of the activity window, as a sibling of (and therefore above) the whole app.
 *
 * The host must sit *outside* the `Modifier.layerBackdrop` producer. Inside it, the overlay would be
 * captured into the very backdrop it samples, which the library documents as a draw loop and a
 * RenderThread SIGSEGV, not a soft failure.
 */
@Stable
class LiquidOverlayHostState internal constructor() {
    private val entries: SnapshotStateList<LiquidOverlayEntry> = mutableStateListOf()

    internal fun register(entry: LiquidOverlayEntry) {
        if (!entries.contains(entry)) entries.add(entry)
    }

    internal fun unregister(entry: LiquidOverlayEntry) {
        entries.remove(entry)
    }

    /**
     * True while at least one modal presentation — sheet, alert or action sheet — is registered.
     *
     * Exists so the page underneath can stop doing work that only exists to be looked at. KeiOS's
     * dynamic background invalidates the draw tree at its 60fps cap, and *every* one of those
     * invalidations re-rasterizes each glass surface above it, because each `drawBackdrop` re-records
     * its own offscreen effect layer per draw. With a modal up, all of that lands underneath a
     * blurred, dimmed plate where none of it can be seen.
     *
     * Measured before this gate existed: a sheet open over Home, untouched, sat at 124-141ms per
     * frame against 16ms for the same page with no sheet — about 7fps for a static screen. See
     * `docs/planning/liquid-sheet-frame-cost.md`.
     *
     * Notifications are deliberately excluded. A toast is transient chrome that does not cover the
     * page, so freezing the background behind one would be a visible stall bought for nothing.
     *
     * Reads the backing [SnapshotStateList], so a composable that consults this recomposes when a
     * presentation opens or closes.
     */
    val hasPresentation: Boolean
        get() = entries.any { it.layer == LiquidOverlayLayer.Presentation }

    /**
     * Composes every registered entry: presentations in registration order, then notifications.
     *
     * Registration order alone is not enough. A presentation registers when it opens, so a sheet
     * opened from another sheet stacks above it, which is what you want. But a notification host is
     * mounted once at the app root and stays mounted, so it always registers *first* — and would
     * therefore draw underneath every sheet and alert opened later, which is the opposite of what a
     * toast is for. Splitting the pass fixes that without giving callers an ordering knob to get
     * wrong.
     */
    @Composable
    internal fun Content() {
        // Order a copy: an entry that unregisters while composing (a sheet that finishes its exit
        // animation and drops itself) would otherwise mutate the list mid-iteration.
        liquidOverlayPlacements(entries.toList()) { it.layer }.forEach { placement ->
            key(placement.entry) {
                CompositionLocalProvider(LocalLiquidOverlayDepth provides placement.depth) {
                    placement.entry.Content()
                }
            }
        }
    }
}

internal class LiquidOverlayPlacement<T>(
    val entry: T,
    val depth: Int,
)

/**
 * Resolves compose order and depth for the registered overlays.
 *
 * Pure so the ordering rule is unit-testable — it is the kind of thing that silently regresses into
 * "whatever registration order happened to be", and registration order is exactly what it exists to
 * override.
 *
 * Notification depth is the presentation *count*, not an index within the notification layer. Depth
 * answers "is there an overlay beneath me that my full-screen blurred plate would erase", so counting
 * notifications against each other would be meaningless — they do not draw such a plate. Reporting the
 * truthful count keeps the invariant usable if one ever does.
 */
internal fun <T> liquidOverlayPlacements(
    entries: List<T>,
    layerOf: (T) -> LiquidOverlayLayer,
): List<LiquidOverlayPlacement<T>> {
    val presentations = entries.filter { layerOf(it) == LiquidOverlayLayer.Presentation }
    val notifications = entries.filter { layerOf(it) == LiquidOverlayLayer.Notification }
    return presentations.mapIndexed { index, entry -> LiquidOverlayPlacement(entry, index) } +
        notifications.map { LiquidOverlayPlacement(it, presentations.size) }
}

/**
 * Which pass an overlay is composed in. Notifications always draw above presentations regardless of
 * when either registered.
 */
enum class LiquidOverlayLayer {
    /** Sheets, alerts and action sheets — modal, one at a time, stacking in the order they open. */
    Presentation,

    /** Toasts and other transient status chrome — never modal, always on top. */
    Notification,
}

/**
 * One portalled subtree. Holds the caller's content plus the caller's `CompositionLocal`s, so
 * content composed at the host still sees the theme, backdrop and app locals from where it was
 * declared rather than the host's.
 */
internal class LiquidOverlayEntry(
    val layer: LiquidOverlayLayer,
) {
    var locals: CompositionLocalContext? by mutableStateOf(null)
    var content: @Composable () -> Unit by mutableStateOf({})

    @Composable
    fun Content() {
        val resolvedLocals = locals
        if (resolvedLocals != null) {
            CompositionLocalProvider(resolvedLocals) { content() }
        } else {
            content()
        }
    }
}

/**
 * Null outside [LiquidOverlayContainer]. Previews, screenshot harnesses and unit tests compose page
 * content without an activity root, and a sheet there should render in place rather than vanish.
 */
val LocalLiquidOverlayHost = staticCompositionLocalOf<LiquidOverlayHostState?> { null }

/** Retained pages may keep their state, but only the foreground owner may present modal chrome. */
val LocalLiquidPresentationOwnerActive = staticCompositionLocalOf { true }

/**
 * How many overlays are already stacked below this one. `0` is the bottom-most.
 *
 * Matters because the scene backdrop contains the *app*, not other overlays — overlays live outside
 * the `layerBackdrop` producer by necessity. So a stacked presentation that paints a full-screen
 * blurred plate from the scene backdrop draws a blurred copy of the page directly over whatever
 * overlay is beneath it, and that overlay appears to vanish. A dialog opened from a sheet should dim
 * the sheet, not replace it with the page.
 */
val LocalLiquidOverlayDepth = staticCompositionLocalOf { 0 }

/**
 * Moves [content] to the top of the activity window.
 *
 * Registration is a snapshot write, so the host picks the content up one composition after the
 * caller declares it. Sheets animate in from off-screen, so that frame is not observable; do not use
 * this for anything that must be correct on its very first composed frame.
 *
 * Falls back to composing in place when there is no host, which keeps previews and Robolectric
 * harnesses working.
 */
@Composable
fun LiquidOverlayPortal(
    layer: LiquidOverlayLayer = LiquidOverlayLayer.Presentation,
    content: @Composable () -> Unit,
) {
    val ownerActive = LocalLiquidPresentationOwnerActive.current
    if (layer == LiquidOverlayLayer.Presentation && !ownerActive) return
    val host = LocalLiquidOverlayHost.current
    if (host == null) {
        content()
        return
    }
    val locals = currentCompositionLocalContext
    val entry = remember(layer) { LiquidOverlayEntry(layer) }
    // Assigning during composition is what lets the host observe the newest content in the frame it
    // recomposes. The host only reads these, so there is no write-after-read loop.
    entry.locals = locals
    entry.content = content
    DisposableEffect(host, entry) {
        host.register(entry)
        onDispose { host.unregister(entry) }
    }
}
