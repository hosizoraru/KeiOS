@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package os.kei.ui.page.main.widget.glass

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.BackdropEffectScopeImpl
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.ceil

internal val LocalRetainedLiquidMaterial = staticCompositionLocalOf { false }

internal fun Modifier.drawLiquidSurfaceBackdrop(
    cacheRecording: Boolean,
    backdrop: Backdrop,
    shape: () -> Shape,
    effects: BackdropEffectScope.() -> Unit,
    highlight: (() -> Highlight?)?,
    shadow: (() -> Shadow?)?,
    innerShadow: (() -> InnerShadow?)?,
    layerBlock: (GraphicsLayerScope.() -> Unit)?,
    exportedBackdrop: LayerBackdrop?,
    onDrawSurface: DrawScope.() -> Unit,
    onDrawFront: (DrawScope.() -> Unit)? = null,
    exportedFrontBackdrop: LayerBackdrop? = null,
): Modifier = if (cacheRecording) {
    drawSheetGlassBackdrop(
        backdrop, shape, effects, layerBlock, exportedBackdrop, highlight, shadow, innerShadow,
        onDrawSurface, onDrawFront ?: {},
        exportedFrontBackdrop,
    )
} else {
    drawBackdrop(
        backdrop = backdrop,
        shape = shape,
        effects = effects,
        highlight = highlight,
        shadow = shadow,
        innerShadow = innerShadow,
        layerBlock = layerBlock,
        exportedBackdrop = exportedBackdrop,
        onDrawSurface = onDrawSurface,
        onDrawFront = onDrawFront,
    )
}

/**
 * The effect scope and export coordinates are the two Backdrop 2.0.1 internal integration points.
 * Reusing its effect scope preserves the actual blur/lens shaders, rather than approximating them.
 * The record/export order follows Backdrop's Apache-2.0 DrawBackdropModifier.kt, with one effect
 * recording shared by display and export instead of recording the same source twice.
 */
internal fun Modifier.drawSheetGlassBackdrop(
    backdrop: Backdrop,
    shape: () -> Shape,
    effects: BackdropEffectScope.() -> Unit,
    layerBlock: (GraphicsLayerScope.() -> Unit)?,
    exportedBackdrop: LayerBackdrop?,
    highlight: (() -> Highlight?)?,
    shadow: (() -> Shadow?)?,
    innerShadow: (() -> InnerShadow?)?,
    onDrawSurface: DrawScope.() -> Unit,
    onDrawFront: DrawScope.() -> Unit,
    exportedFrontBackdrop: LayerBackdrop? = null,
    clip: Boolean = true,
): Modifier {
    val outline = LiquidOutlineProvider(shape)
    return this
        .then(if (layerBlock != null) Modifier.graphicsLayer(layerBlock) else Modifier)
        .cachedLiquidBackdropDecorations(outline, highlight, shadow, innerShadow)
        .then(
            if (clip) Modifier.graphicsLayer {
                this.clip = true
                this.shape = outline.shape
                compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
            } else Modifier,
        )
        .then(
            SheetGlassElement(
                backdrop, outline, effects, layerBlock, exportedBackdrop, onDrawSurface, onDrawFront, exportedFrontBackdrop,
            ),
        )
}

private data class SheetGlassElement(
    val backdrop: Backdrop,
    val outline: LiquidOutlineProvider,
    val effects: BackdropEffectScope.() -> Unit,
    val layerBlock: (GraphicsLayerScope.() -> Unit)?,
    val exportedBackdrop: LayerBackdrop?,
    val onDrawSurface: DrawScope.() -> Unit,
    val onDrawFront: DrawScope.() -> Unit,
    val exportedFrontBackdrop: LayerBackdrop?,
) : ModifierNodeElement<SheetGlassNode>() {
    override fun create() = SheetGlassNode(
        backdrop, outline, effects, layerBlock, exportedBackdrop, onDrawSurface, onDrawFront, exportedFrontBackdrop,
    )

    override fun update(node: SheetGlassNode) {
        val opticsChanged = node.outline != outline || node.effects != effects
        if (node.backdrop != backdrop) {
            node.backdrop = backdrop
            node.invalidateSource()
        }
        if (node.outline != outline) node.outline = outline
        node.effects = effects
        if (node.layerBlock != layerBlock) {
            node.layerBlock = layerBlock
            node.invalidateSource()
        }
        if (node.exportedBackdrop != exportedBackdrop) {
            node.exportedBackdrop?.layerCoordinates = null
            node.exportedBackdrop = exportedBackdrop
            node.exportedBackdrop?.layerCoordinates = node.coordinates
            node.configureExport()
            node.invalidateExport()
        }
        if (node.exportedFrontBackdrop != exportedFrontBackdrop) {
            node.exportedFrontBackdrop?.layerCoordinates = null
            node.exportedFrontBackdrop = exportedFrontBackdrop
            exportedFrontBackdrop?.layerCoordinates = node.coordinates
            node.invalidateExport()
        }
        if (node.onDrawSurface != onDrawSurface) node.invalidateMaterial()
        if (node.onDrawFront != onDrawFront) node.invalidateFront()
        node.onDrawSurface = onDrawSurface
        node.onDrawFront = onDrawFront
        if (opticsChanged) node.invalidateEffects()
    }
}

private class SheetGlassNode(
    var backdrop: Backdrop,
    var outline: LiquidOutlineProvider,
    var effects: BackdropEffectScope.() -> Unit,
    var layerBlock: (GraphicsLayerScope.() -> Unit)?,
    var exportedBackdrop: LayerBackdrop?,
    var onDrawSurface: DrawScope.() -> Unit,
    var onDrawFront: DrawScope.() -> Unit,
    var exportedFrontBackdrop: LayerBackdrop?,
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode, ObserverModifierNode {
    private val effectScope = object : BackdropEffectScopeImpl() {
        override val shape: Shape get() = outline.shapeBlock()
    }
    private var effectLayer: GraphicsLayer? = null
    var coordinates: LayoutCoordinates? = null
        private set
    private var samplePosition: Offset? = null
    private var sourceDirty = true
    private var materialDirty = true
    private var frontDirty = true
    private var effectsDirty = true
    private var exportOutset = -1
    private var hasDrawn = false
    private var sizeChanging = false
    private val handler = Handler(Looper.getMainLooper())
    // Independent read scopes avoid one delegated observer replacing another group's dependencies.
    private val drawObserver = SnapshotStateObserver { callback ->
        if (Looper.myLooper() == Looper.getMainLooper()) callback() else handler.post(callback)
    }
    private val sourceReads = Any()
    private val surfaceReads = Any()
    private val frontReads = Any()
    private val sourceChanged: (Any) -> Unit = { invalidateSource() }
    private val surfaceChanged: (Any) -> Unit = { invalidateMaterial() }
    private val frontChanged: (Any) -> Unit = { invalidateFront() }

    override fun onAttach() {
        drawObserver.start()
        effectLayer = requireGraphicsContext().createGraphicsLayer()
        configureExport()
        invalidateEffects()
    }

    override fun onDetach() {
        drawObserver.stop()
        drawObserver.clear()
        effectLayer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        effectLayer = null
        effectScope.reset()
        coordinates = null
        samplePosition = null
        sourceDirty = true
        materialDirty = true
        frontDirty = true
        effectsDirty = true
        exportOutset = -1
        hasDrawn = false
        sizeChanging = false
        exportedBackdrop?.layerCoordinates = null
        exportedFrontBackdrop?.layerCoordinates = null
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (!coordinates.isAttached) return
        val position = coordinates.positionInWindow()
        if (samplePosition != position) {
            samplePosition = position
            invalidateSource()
            invalidateFront()
        }
        this.coordinates = if (backdrop.isCoordinatesDependent) coordinates else null
        exportedBackdrop?.layerCoordinates = coordinates
        exportedFrontBackdrop?.layerCoordinates = coordinates
    }

    override fun onObservedReadsChanged() = invalidateEffects()

    fun invalidateEffects() {
        // Attachment and modifier updates can precede measurement. Build the native filter once
        // in draw, with the final density/size, rather than also compiling an unmeasured version.
        effectsDirty = true
        if (isAttached) invalidateDraw()
    }

    private fun updateEffects() {
        effectScope.apply { withLiquidShaderPool(enabled = true, effects = effects) }
        effectLayer?.renderEffect = effectScope.renderEffect
        invalidateSource()
        invalidateExport()
    }

    fun invalidateSource() {
        sourceDirty = true
        invalidateMaterial()
    }

    fun invalidateMaterial() {
        materialDirty = true
        if (isAttached) invalidateDraw()
    }

    fun invalidateFront() {
        frontDirty = true
        if (isAttached) invalidateDraw()
    }

    fun invalidateExport() {
        materialDirty = true
        frontDirty = true
        if (isAttached) invalidateDraw()
    }

    fun configureExport() {
        exportedBackdrop?.graphicsLayer?.apply {
            compositingStrategy = androidx.compose.ui.graphics.layer.CompositingStrategy.Offscreen
            val outset = ceil(effectScope.padding).toInt()
            setOutsets(outset, outset, outset, outset)
        }
    }

    override fun ContentDrawScope.draw() {
        if (effectScope.update(this)) {
            sizeChanging = hasDrawn
            effectsDirty = true
        }
        if (effectsDirty) {
            observeReads { updateEffects() }
            effectsDirty = false
        }
        val effect = effectLayer ?: return
        val padding = effectScope.padding
        if (sourceDirty || backdrop !is LayerBackdrop) {
            recordLayer(
                effect,
                IntSize(size.width.toInt() + padding.toInt() * 2, size.height.toInt() + padding.toInt() * 2),
            ) {
                // Register inside record's snapshot scope so canvas state and inverse motion
                // invalidate this recording too, independently of body/content redraws.
                drawObserver.observeReads(sourceReads, sourceChanged) {
                    drawContext.canvas.translate(padding, padding)
                    with(backdrop) {
                        drawBackdrop(density = effectScope, coordinates = coordinates, layerBlock = layerBlock)
                    }
                    drawContext.canvas.translate(-padding, -padding)
                }
            }
            sourceDirty = false
        }
        effect.topLeft = IntOffset(-padding.toInt(), -padding.toInt())
        // Position changes resample the source but can reuse a same-sized optical target. Only
        // resizing needs the transient Auto strategy, to avoid reallocating an extra target.
        val retainPixels = exportedBackdrop == null || (hasDrawn && !sizeChanging)
        val strategy =
            if (effectScope.renderEffect != null && retainPixels) androidx.compose.ui.graphics.layer.CompositingStrategy.Offscreen
            else androidx.compose.ui.graphics.layer.CompositingStrategy.Auto
        if (effect.compositingStrategy != strategy) effect.compositingStrategy = strategy
        val outset = ceil(padding).toInt()
        if (exportOutset != outset) {
            exportedBackdrop?.graphicsLayer?.setOutsets(outset, outset, outset, outset)
            exportOutset = outset
        }
        val export = exportedBackdrop
        val front = exportedFrontBackdrop
        val recordingSize = IntSize(size.width.toInt(), size.height.toInt())
        if (export != null && front != null) {
            // Foreground animation must not re-record the material that child controls sample.
            if (materialDirty) {
                recordLayer(export.graphicsLayer, recordingSize) {
                    drawLayer(effect)
                    drawObserver.observeReads(surfaceReads, surfaceChanged) { onDrawSurface(this) }
                }
                materialDirty = false
            }
            if (frontDirty) {
                recordLayer(front.graphicsLayer, recordingSize) {
                    drawObserver.observeReads(frontReads, frontChanged) { onDrawFront(this) }
                }
                frontDirty = false
            }
            // Reuse the already-rasterized exported material on screen. Keeping the front in its
            // own display list preserves its position above child content and in control samples.
            drawLayer(export.graphicsLayer)
            drawContent()
            drawLayer(front.graphicsLayer)
        } else {
            drawLayer(effect)
            drawObserver.observeReads(surfaceReads, surfaceChanged) { onDrawSurface(this) }
            drawContent()
            drawObserver.observeReads(frontReads, frontChanged) { onDrawFront(this) }
            if (export != null && (materialDirty || frontDirty)) {
                recordLayer(export.graphicsLayer, recordingSize) {
                    drawLayer(effect)
                    onDrawSurface(this)
                    onDrawFront(this)
                }
                materialDirty = false
                frontDirty = false
            }
        }
        hasDrawn = true
        sizeChanging = false
    }

    private fun DrawScope.recordLayer(
        layer: GraphicsLayer,
        size: IntSize,
        block: DrawScope.() -> Unit,
    ) {
        val density = requireDensity()
        layer.record(size) {
            val previousDensity = drawContext.density
            drawContext.density = density
            try {
                this.block()
            } finally {
                drawContext.density = previousDensity
            }
        }
    }
}
