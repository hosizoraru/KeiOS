package os.kei.ui.page.main.widget.sheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Stable
internal class LiquidSheetDrawViewport {
    var bounds: Rect? by mutableStateOf(null)
    var coordinates: LayoutCoordinates? by mutableStateOf(null)
}

internal val LocalLiquidSheetDrawViewport = staticCompositionLocalOf<LiquidSheetDrawViewport?> { null }

internal fun Modifier.liquidSheetDrawViewport(viewport: LiquidSheetDrawViewport): Modifier =
    onGloballyPositioned {
        viewport.coordinates = it
        viewport.bounds = it.boundsInWindow()
    }

/**
 * A scroll clip alone does not stop offscreen glass descendants recording effect layers. Skip their
 * draw traversal while keeping composition, measurement, placement, focus and semantics intact.
 */
@Composable
internal fun Modifier.cullOutsideLiquidSheetViewport(): Modifier {
    val viewport = LocalLiquidSheetDrawViewport.current ?: return this
    // The shared controls' shadows and press transforms can extend beyond their layout bounds.
    val outset = with(LocalDensity.current) { 64.dp.toPx() }
    return then(LiquidSheetViewportDrawElement(viewport, outset))
}

private data class LiquidSheetViewportDrawElement(
    val viewport: LiquidSheetDrawViewport,
    val outsetPx: Float,
) : ModifierNodeElement<LiquidSheetViewportDrawNode>() {
    override fun create() = LiquidSheetViewportDrawNode(viewport, outsetPx)

    override fun update(node: LiquidSheetViewportDrawNode) {
        node.viewport = viewport
        node.outsetPx = outsetPx
        node.invalidateDraw()
    }
}

private class LiquidSheetViewportDrawNode(
    var viewport: LiquidSheetDrawViewport,
    var outsetPx: Float,
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {
    private var coordinates: LayoutCoordinates? = null
    private var visible = true

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        val nextVisible = intersectsViewport()
        if (visible != nextVisible) {
            visible = nextVisible
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        // Read the viewport here too: resizing or moving its clip can expose a stationary row.
        visible = intersectsViewport()
        if (visible) drawContent()
    }

    override fun onDetach() {
        coordinates = null
        visible = true
    }

    private fun intersectsViewport(): Boolean {
        val clip = viewport.bounds ?: return true
        val current = coordinates?.takeIf { it.isAttached } ?: return true
        val viewportCoordinates = viewport.coordinates?.takeIf { it.isAttached } ?: return true
        // Portals and popups can inherit the composition locals without belonging to the scroller.
        var ancestor: LayoutCoordinates? = current
        while (ancestor != null && ancestor !== viewportCoordinates) {
            ancestor = ancestor.parentCoordinates
        }
        if (ancestor == null) return true
        val root = current.findRootCoordinates()
        val boundsInRoot = root.localBoundingBoxOf(current, clipBounds = false)
        val rootInWindow = root.localToWindow(Offset.Zero)
        val bounds = boundsInRoot.translate(rootInWindow).inflate(outsetPx)
        return bounds.overlaps(clip)
    }
}
