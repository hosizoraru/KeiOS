package os.kei.ui.page.main.widget.glass

import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativePaint
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.ceil

/**
 * `drawBackdrop` for a surface over a flat field ([liquidFlatField]), drawing what the library draws there
 * with one difference: where the shape is clipped.
 *
 * The library places every glass surface in an offscreen layer clipped to its outline, and HWUI applies
 * that clip when it composites the finished layer — in screen space. For the app's continuous-corner
 * shapes the outline is a path, and an anti-aliased path clip is a software mask on this GPU path, so every
 * frame a card moved (every scroll frame) each card's mask was rasterised on the CPU and uploaded again:
 * 18 uploads a frame on the BA office, all card-sized. Here the same cut is made inside the layer, in the
 * layer's own space — everything outside the outline is cleared after the surface is drawn — so it is
 * rendered with the layer and reused while the layer is unchanged. The idea
 * is miuix-squircle's (a squircle surface composites through one offscreen layer, "cheap to cache and
 * re-blit while scrolling"); the silhouette stays the library's own path.
 *
 * The rest is the library's, reproduced from the 2.0.1 bytecode because its nodes are internal: the
 * transform layer first, then the inner shadow over the highlight over the surface, with the outer shadow
 * underneath, each with the library's paint, layer, blend mode and clip. The highlight and both shadows stay
 * outside the offscreen layer on purpose: the highlight blends `Plus` against what is already below it, and
 * inside a transparent layer it would blend against the transparent rim instead; the outer shadow draws
 * outside the bounds, which the layer would cut.
 *
 * Nothing samples a backdrop here, so the library's inverse transform of [layerBlock] has nothing to act on
 * and the plain layer is exact.
 *
 * Against the library's path on the A17 AVD the only differences are on the anti-aliased rim, where the
 * coverage is now rounded into the layer instead of applied while compositing it: at most 20/765 in light and
 * 37/765 in dark, the latter on the one-pixel edge of a scaled card in the BA pile, where the rim is resampled
 * by the pile's transform. See docs/planning/liquid-glass-clip-and-resolution.md.
 */
fun Modifier.drawFlatLiquidBackdrop(
    field: Backdrop,
    shape: () -> Shape,
    highlight: (() -> Highlight?)? = null,
    shadow: (() -> Shadow?)? = null,
    innerShadow: (() -> InnerShadow?)? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
): Modifier {
    val outline = LiquidOutlineProvider(shape)
    return this
        .then(if (layerBlock != null) Modifier.graphicsLayer(layerBlock) else Modifier)
        .then(if (innerShadow != null) LiquidInnerShadowElement(outline, innerShadow) else Modifier)
        .then(if (shadow != null) LiquidShadowElement(outline, shadow) else Modifier)
        .then(if (highlight != null) LiquidHighlightElement(outline, highlight) else Modifier)
        .then(FlatBackdropElement(outline, field, onDrawSurface))
}

/**
 * The library's `ShapeProvider`: one outline per shape, size, direction and density, shared by every node
 * of a surface. [shape] is a delegating wrapper like the library's, and it matters: the highlight shader
 * reads corner radii only from a `CornerBasedShape`, and the library always hands it the wrapper.
 */
internal class LiquidOutlineProvider(
    val shapeBlock: () -> Shape,
) {
    // Cache contents are not modifier inputs; the shape producer defines logical equality.
    override fun equals(other: Any?): Boolean = other is LiquidOutlineProvider && shapeBlock == other.shapeBlock

    override fun hashCode(): Int = shapeBlock.hashCode()

    private var cachedShape: Shape? = null
    private var cachedOutline: Outline? = null
    private var cachedSize = Size.Unspecified
    private var cachedLayoutDirection: LayoutDirection? = null
    private var cachedDensity = Float.NaN

    val shape: Shape =
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density,
            ): Outline {
                val current = shapeBlock()
                val cached = cachedOutline
                if (
                    cached != null &&
                    current == cachedShape &&
                    size == cachedSize &&
                    layoutDirection == cachedLayoutDirection &&
                    density.density == cachedDensity
                ) {
                    return cached
                }
                return current.createOutline(size, layoutDirection, density).also {
                    cachedShape = current
                    cachedOutline = it
                    cachedSize = size
                    cachedLayoutDirection = layoutDirection
                    cachedDensity = density.density
                }
            }
        }
}

/** The library's `clipOutline`. */
private fun Canvas.clipLiquidOutline(
    outline: Outline,
    roundedPath: Path?,
) {
    when (outline) {
        is Outline.Rectangle -> clipRect(outline.rect)
        is Outline.Rounded -> {
            val path = roundedPath ?: Path()
            path.rewind()
            path.addRoundRect(outline.roundRect)
            clipPath(path)
        }
        is Outline.Generic -> clipPath(outline.path)
    }
}

// ---- the surface itself ------------------------------------------------------------------------------

private class FlatBackdropElement(
    val outline: LiquidOutlineProvider,
    val field: Backdrop,
    val onDrawSurface: (DrawScope.() -> Unit)?,
) : ModifierNodeElement<FlatBackdropNode>() {
    override fun create() = FlatBackdropNode(outline, field, onDrawSurface)

    override fun update(node: FlatBackdropNode) {
        node.outline = outline
        node.field = field
        node.onDrawSurface = onDrawSurface
        node.invalidateDraw()
    }

    override fun equals(other: Any?): Boolean =
        other is FlatBackdropElement &&
            other.outline.shapeBlock == outline.shapeBlock &&
            other.field == field &&
            other.onDrawSurface == onDrawSurface

    override fun hashCode(): Int {
        var result = outline.shapeBlock.hashCode()
        result = 31 * result + field.hashCode()
        result = 31 * result + (onDrawSurface?.hashCode() ?: 0)
        return result
    }
}

private class FlatBackdropNode(
    var outline: LiquidOutlineProvider,
    var field: Backdrop,
    var onDrawSurface: (DrawScope.() -> Unit)?,
) : Modifier.Node(),
    LayoutModifierNode,
    DrawModifierNode {
    // The library's placement layer, less the clip, which moves into the draw below.
    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        compositingStrategy = CompositingStrategy.Offscreen
    }

    private var roundedPath: Path? = null

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(IntOffset.Zero, layerBlock = layerBlock)
        }
    }

    override fun ContentDrawScope.draw() {
        with(field) { drawBackdrop(this@draw, null, null) }
        onDrawSurface?.invoke(this)
        drawContent()
        // The library clips the finished layer once, so a rim pixel keeps coverage × (everything drawn).
        // Clipping each draw instead would compound the coverage on translucent layers; clearing what lies
        // outside the outline, after everything is drawn, applies it once, the same way.
        val resolved = outline.shape.createOutline(size, layoutDirection, this)
        val clipPath =
            when (resolved) {
                is Outline.Rectangle -> return
                is Outline.Rounded -> (roundedPath ?: Path().also { roundedPath = it }).apply {
                    rewind()
                    addRoundRect(resolved.roundRect)
                }
                is Outline.Generic -> resolved.path
            }
        val canvas = drawContext.canvas
        canvas.save()
        canvas.clipPath(clipPath, ClipOp.Difference)
        drawRect(Color.Black, blendMode = BlendMode.Clear)
        canvas.restore()
    }
}

// ---- highlight ---------------------------------------------------------------------------------------

private class LiquidHighlightElement(
    val outline: LiquidOutlineProvider,
    val highlight: () -> Highlight?,
    val cacheRecording: Boolean = false,
) : ModifierNodeElement<LiquidHighlightNode>() {
    override fun create() = LiquidHighlightNode(outline, highlight, cacheRecording)

    override fun update(node: LiquidHighlightNode) {
        node.outline = outline
        node.highlight = highlight
        node.cacheRecording = cacheRecording
        node.invalidateDraw()
    }

    override fun equals(other: Any?): Boolean =
        other is LiquidHighlightElement && other.outline.shapeBlock == outline.shapeBlock &&
            other.highlight == highlight && other.cacheRecording == cacheRecording

    override fun hashCode(): Int = 31 * (31 * outline.shapeBlock.hashCode() + highlight.hashCode()) + cacheRecording.hashCode()
}

/** The library's `HighlightNode`. */
private class LiquidHighlightNode(
    var outline: LiquidOutlineProvider,
    var highlight: () -> Highlight?,
    var cacheRecording: Boolean,
) : Modifier.Node(),
    DrawModifierNode {
    private var layer: GraphicsLayer? = null
    private val paint = Paint().apply { style = PaintingStyle.Stroke }
    private var roundedPath: Path? = null
    private var recordedHighlight: Highlight? = null
    private val geometry = LiquidDecorationGeometry()
    private var recordedOutline: Outline? = null
    private var recordedSize = Size.Unspecified
    private var recordedDensity = Float.NaN

    override fun onAttach() {
        layer = requireGraphicsContext().createGraphicsLayer()
    }

    override fun onDetach() {
        layer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        layer = null
        roundedPath = null
        paint.nativePaint.shader = null
        releaseShaders()
        defaultShader = null
        ambientShader = null
        recordedHighlight = null
        recordedOutline = null
        recordedSize = Size.Unspecified
        recordedDensity = Float.NaN
    }

    override fun ContentDrawScope.draw() {
        val resolved = highlight()
        if (resolved == null || resolved.width.value <= 0f) {
            drawContent()
            return
        }
        drawContent()
        val layer = layer ?: return
        val (materialSize, shapeOutline) = geometry.resolve(this, outline, resolved.blurRadius.toPx() * 4f, cacheRecording)
        val layerSize = IntSize(ceil(materialSize.width).toInt() + 2, ceil(materialSize.height).toInt() + 2)
        layer.compositingStrategy = if (materialSize != size) {
            androidx.compose.ui.graphics.layer.CompositingStrategy.Offscreen
        } else androidx.compose.ui.graphics.layer.CompositingStrategy.Auto
        val path =
            if (shapeOutline is Outline.Rounded) roundedPath ?: Path().also { roundedPath = it } else null
        layer.alpha = resolved.alpha
        layer.blendMode = resolved.style.blendMode
        if (
            !cacheRecording || recordedHighlight != resolved || recordedOutline != shapeOutline ||
            recordedSize != materialSize || recordedDensity != density
        ) {
            try {
                configurePaint(resolved, materialSize)
                layer.record(layerSize) {
                    translate(1f, 1f) {
                        drawIntoCanvas { canvas ->
                            canvas.save()
                            canvas.clipLiquidOutline(shapeOutline, path)
                            canvas.drawOutline(shapeOutline, paint)
                            canvas.restore()
                        }
                    }
                }
            } finally {
                if (cacheRecording) {
                    paint.nativePaint.shader = null
                    releaseShaders()
                    defaultShader = null
                    ambientShader = null
                }
            }
            recordedHighlight = resolved
            recordedOutline = shapeOutline
            recordedSize = materialSize
            recordedDensity = density
        }
        val extraHeight = size.height - materialSize.height
        translate(-1f, -1f) { drawLiquidDecoration(layer, extraHeight) }
    }

    private fun DrawScope.configurePaint(highlight: Highlight, materialSize: Size) {
        paint.color = highlight.style.color
        val width = highlight.width.toPx()
        val maxWidth = materialSize.minDimension / 2f
        paint.strokeWidth = ceil(if (width > maxWidth) maxWidth else width) * 2f
        paint.applyBlur(highlight.blurRadius.toPx())
        // The library draws the highlight shader only where `RuntimeShader` exists.
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            paint.nativePaint.shader = highlightShader(highlight.style, materialSize)
        }
    }

    private var defaultShader: android.graphics.RuntimeShader? = null
    private var ambientShader: android.graphics.RuntimeShader? = null

    /**
     * `HighlightStyle.Default.createShader` and `HighlightStyle.Ambient.createShader`, whose API is closed to
     * other modules. The library passes the shape wrapper, which is never a `CornerBasedShape`, so every
     * corner radius is half the short side.
     */
    @androidx.annotation.RequiresApi(33)
    private fun DrawScope.highlightShader(style: HighlightStyle, materialSize: Size): android.graphics.RuntimeShader {
        val radius = materialSize.minDimension / 2f
        val shader =
            when (style) {
                is HighlightStyle.Default -> {
                    val shader = defaultShader ?: createShader(DefaultHighlightShader).also { defaultShader = it }
                    shader.setColorUniform("color", style.color.copy(alpha = 1f).toArgb())
                    shader.setFloatUniform("angle", style.angle * 0.017453292f)
                    shader.setFloatUniform("falloff", style.falloff)
                    shader
                }
                is HighlightStyle.Ambient -> {
                    val shader = ambientShader ?: createShader(AmbientHighlightShader).also { ambientShader = it }
                    shader.setFloatUniform("angle", 0.7853982f)
                    shader.setFloatUniform("falloff", 1f)
                    shader
                }
                else -> error("drawFlatLiquidBackdrop draws the library's Default and Ambient highlights only")
            }
        shader.setFloatUniform("size", materialSize.width, materialSize.height)
        shader.setFloatUniform("cornerRadii", floatArrayOf(radius, radius, radius, radius))
        return shader
    }

    private fun createShader(program: String): android.graphics.RuntimeShader =
        if (cacheRecording) LiquidShaderPool.acquire(program) else android.graphics.RuntimeShader(program)

    private fun releaseShaders() {
        if (cacheRecording) {
            defaultShader?.let { LiquidShaderPool.release(DefaultHighlightShader, it) }
            ambientShader?.let { LiquidShaderPool.release(AmbientHighlightShader, it) }
        }
    }
}

/** The library's `Paint.blur`. */
private fun Paint.applyBlur(radius: Float) {
    nativePaint.maskFilter = if (radius > 0f) BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL) else null
}

// ---- outer shadow ------------------------------------------------------------------------------------

private class LiquidShadowElement(
    val outline: LiquidOutlineProvider,
    val shadow: () -> Shadow?,
    val cacheRecording: Boolean = false,
) : ModifierNodeElement<LiquidShadowNode>() {
    override fun create() = LiquidShadowNode(outline, shadow, cacheRecording)

    override fun update(node: LiquidShadowNode) {
        node.outline = outline
        node.shadow = shadow
        node.cacheRecording = cacheRecording
        node.invalidateDraw()
    }

    override fun equals(other: Any?): Boolean =
        other is LiquidShadowElement && other.outline.shapeBlock == outline.shapeBlock &&
            other.shadow == shadow && other.cacheRecording == cacheRecording

    override fun hashCode(): Int = 31 * (31 * outline.shapeBlock.hashCode() + shadow.hashCode()) + cacheRecording.hashCode()
}

/**
 * The library's `ShadowNode`: the outline blurred in an offscreen layer grown by twice the radius on each
 * side, the outline itself cleared out of it, drawn under the content.
 */
private class LiquidShadowNode(
    var outline: LiquidOutlineProvider,
    var shadow: () -> Shadow?,
    var cacheRecording: Boolean,
) : Modifier.Node(),
    DrawModifierNode {
    private var layer: GraphicsLayer? = null
    private val paint = Paint()
    private var recordedShadow: Shadow? = null
    private val geometry = LiquidDecorationGeometry()
    private var recordedOutline: Outline? = null
    private var recordedSize = Size.Unspecified
    private var recordedDensity = Float.NaN

    override fun onAttach() {
        layer = requireGraphicsContext().createGraphicsLayer().apply {
            compositingStrategy = androidx.compose.ui.graphics.layer.CompositingStrategy.Offscreen
        }
    }

    override fun onDetach() {
        layer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        layer = null
        recordedShadow = null
        recordedOutline = null
        recordedSize = Size.Unspecified
        recordedDensity = Float.NaN
    }

    override fun ContentDrawScope.draw() {
        val resolved = shadow()
        val layer = layer
        if (resolved == null || layer == null) {
            drawContent()
            return
        }
        val radius = resolved.radius.toPx()
        val offsetX = resolved.offset.x.toPx()
        val offsetY = resolved.offset.y.toPx()
        val (materialSize, shapeOutline) = geometry.resolve(
            this, outline, radius * 4f + kotlin.math.abs(offsetY), cacheRecording,
        )
        val layerSize =
            IntSize(
                ceil(materialSize.width + radius * 4f + offsetX).toInt(),
                ceil(materialSize.height + radius * 4f + offsetY).toInt(),
            )
        layer.alpha = resolved.alpha
        layer.blendMode = resolved.blendMode
        if (
            !cacheRecording || recordedShadow != resolved || recordedOutline != shapeOutline ||
            recordedSize != materialSize || recordedDensity != density
        ) {
            paint.color = resolved.color
            paint.applyBlur(radius)
            layer.record(layerSize) {
                translate(radius * 2f + offsetX, radius * 2f + offsetY) {
                    drawIntoCanvas { canvas ->
                        canvas.drawOutline(shapeOutline, paint)
                        canvas.translate(-offsetX, -offsetY)
                        canvas.drawOutline(shapeOutline, ShadowMaskPaint)
                        canvas.translate(offsetX, offsetY)
                    }
                }
            }
            recordedShadow = resolved
            recordedOutline = shapeOutline
            recordedSize = materialSize
            recordedDensity = density
        }
        val extraHeight = size.height - materialSize.height
        translate(-radius * 2f, -radius * 2f) { drawLiquidDecoration(layer, extraHeight) }
        drawContent()
    }
}

private val ShadowMaskPaint = Paint().apply { blendMode = BlendMode.Clear }

// ---- inner shadow ------------------------------------------------------------------------------------

internal fun Modifier.cachedLiquidBackdropDecorations(
    outline: LiquidOutlineProvider,
    highlight: (() -> Highlight?)?,
    shadow: (() -> Shadow?)?,
    innerShadow: (() -> InnerShadow?)?,
): Modifier = this
    .then(if (innerShadow != null) LiquidInnerShadowElement(outline, innerShadow, cacheRecording = true) else Modifier)
    .then(if (shadow != null) LiquidShadowElement(outline, shadow, cacheRecording = true) else Modifier)
    .then(if (highlight != null) LiquidHighlightElement(outline, highlight, cacheRecording = true) else Modifier)

private class LiquidInnerShadowElement(
    val outline: LiquidOutlineProvider,
    val shadow: () -> InnerShadow?,
    val cacheRecording: Boolean = false,
) : ModifierNodeElement<LiquidInnerShadowNode>() {
    override fun create() = LiquidInnerShadowNode(outline, shadow, cacheRecording)

    override fun update(node: LiquidInnerShadowNode) {
        node.outline = outline
        node.shadow = shadow
        node.cacheRecording = cacheRecording
        node.invalidateDraw()
    }

    override fun equals(other: Any?): Boolean =
        other is LiquidInnerShadowElement && other.outline.shapeBlock == outline.shapeBlock &&
            other.shadow == shadow && other.cacheRecording == cacheRecording

    override fun hashCode(): Int = 31 * (31 * outline.shapeBlock.hashCode() + shadow.hashCode()) + cacheRecording.hashCode()
}

/** The library's `InnerShadowNode`. */
private class LiquidInnerShadowNode(
    var outline: LiquidOutlineProvider,
    var shadow: () -> InnerShadow?,
    var cacheRecording: Boolean,
) : Modifier.Node(),
    DrawModifierNode {
    private var layer: GraphicsLayer? = null
    private val paint = Paint()
    private var roundedPath: Path? = null
    private var previousRadius = Float.NaN
    private var recordedShadow: InnerShadow? = null
    private val geometry = LiquidDecorationGeometry()
    private var recordedOutline: Outline? = null
    private var recordedSize = Size.Unspecified
    private var recordedDensity = Float.NaN

    override fun onAttach() {
        layer = requireGraphicsContext().createGraphicsLayer()
    }

    override fun onDetach() {
        layer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        layer = null
        roundedPath = null
        previousRadius = Float.NaN
        recordedShadow = null
        recordedOutline = null
        recordedSize = Size.Unspecified
        recordedDensity = Float.NaN
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val resolved = shadow() ?: return
        val layer = layer ?: return
        val radius = resolved.radius.toPx()
        val offsetX = resolved.offset.x.toPx()
        val offsetY = resolved.offset.y.toPx()
        val (materialSize, shapeOutline) = geometry.resolve(
            this, outline, radius * 4f + kotlin.math.abs(offsetY), cacheRecording,
        )
        layer.compositingStrategy = if (materialSize != size) {
            androidx.compose.ui.graphics.layer.CompositingStrategy.Offscreen
        } else androidx.compose.ui.graphics.layer.CompositingStrategy.Auto
        val path =
            if (shapeOutline is Outline.Rounded) roundedPath ?: Path().also { roundedPath = it } else null
        paint.color = resolved.color
        layer.alpha = resolved.alpha
        layer.blendMode = resolved.blendMode
        if (radius != previousRadius) {
            layer.renderEffect = if (radius > 0f) BlurEffect(radius, radius, TileMode.Decal) else null
            previousRadius = radius
        }
        if (
            !cacheRecording || recordedShadow != resolved || recordedOutline != shapeOutline ||
            recordedSize != materialSize || recordedDensity != density
        ) {
            // The shadow contains no sampled backdrop or child content. Retain its exact recording
            // across content scrolls; re-record when its material or geometry actually changes.
            layer.record(IntSize(materialSize.width.toInt(), materialSize.height.toInt())) {
                drawIntoCanvas { canvas ->
                    canvas.save()
                    canvas.clipLiquidOutline(shapeOutline, path)
                    canvas.drawOutline(shapeOutline, paint)
                    canvas.translate(offsetX, offsetY)
                    canvas.drawOutline(shapeOutline, InnerShadowMaskPaint)
                    canvas.translate(-offsetX, -offsetY)
                    canvas.restore()
                }
            }
            recordedShadow = resolved
            recordedOutline = shapeOutline
            recordedSize = materialSize
            recordedDensity = density
        }
        val canvas = drawContext.canvas
        canvas.save()
        val displayOutline = outline.shape.createOutline(size, layoutDirection, this)
        canvas.clipLiquidOutline(displayOutline, path)
        drawLiquidDecoration(layer, size.height - materialSize.height)
        canvas.restore()
    }
}

private val InnerShadowMaskPaint = Paint().apply { blendMode = BlendMode.Clear }

// The library's default highlight shader, verbatim from io.github.kyant0:backdrop 2.0.1 (Apache-2.0).
private const val DefaultHighlightShader = """
uniform float2 size;
uniform float4 cornerRadii;
layout(color) uniform half4 color;
uniform float angle;
uniform float falloff;


float radiusAt(float2 coord, float4 radii) {
    if (coord.x >= 0.0) {
        if (coord.y <= 0.0) return radii.y;
        else return radii.z;
    } else {
        if (coord.y <= 0.0) return radii.x;
        else return radii.w;
    }
}

float sdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    float outside = length(max(cornerCoord, 0.0)) - radius;
    float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);
    return outside + inside;
}

float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {
        return sign(coord) * normalize(max(cornerCoord, 0.0));
    } else {
        float gradX = step(cornerCoord.y, cornerCoord.x);
        return sign(coord) * float2(gradX, 1.0 - gradX);
    }
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = coord - halfSize;
    float radius = radiusAt(coord, cornerRadii);
    
    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    float2 grad = gradSdRoundedRect(centeredCoord, halfSize, gradRadius);
    float2 normal = float2(cos(angle), sin(angle));
    float d = dot(grad, normal);
    float intensity = pow(abs(d), falloff);
    return color * intensity;
}
"""

// The library's ambient highlight shader, verbatim from io.github.kyant0:backdrop 2.0.1 (Apache-2.0).
private const val AmbientHighlightShader = """
uniform float2 size;
uniform float4 cornerRadii;
uniform float angle;
uniform float falloff;



float radiusAt(float2 coord, float4 radii) {
    if (coord.x >= 0.0) {
        if (coord.y <= 0.0) return radii.y;
        else return radii.z;
    } else {
        if (coord.y <= 0.0) return radii.x;
        else return radii.w;
    }
}

float sdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    float outside = length(max(cornerCoord, 0.0)) - radius;
    float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);
    return outside + inside;
}

float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {
        return sign(coord) * normalize(max(cornerCoord, 0.0));
    } else {
        float gradX = step(cornerCoord.y, cornerCoord.x);
        return sign(coord) * float2(gradX, 1.0 - gradX);
    }
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = coord - halfSize;
    float radius = radiusAt(coord, cornerRadii);
    
    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    float2 grad = gradSdRoundedRect(centeredCoord, halfSize, gradRadius);
    float2 normal = float2(cos(angle), sin(angle));
    float d = dot(grad, normal);
    float intensity = pow(abs(d), falloff);
    float t = step(0.0, d);
    return half4(t, t, t, 1.0) * intensity;
}
"""
