package os.kei.ui.page.main.widget.glass

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.LayoutDirection
import com.kyant.shapes.RoundedRectangle
import kotlin.math.ceil

/** Only the straight, constant part of a tall decoration is repeated; corners stay at native size. */
internal class LiquidDecorationGeometry {
    private var shape: Shape? = null
    private var size = Size.Unspecified
    private var density = Float.NaN
    private var fontScale = Float.NaN
    private var direction: LayoutDirection? = null
    private var cached: Outline? = null

    fun resolve(scope: DrawScope, provider: LiquidOutlineProvider, supportPx: Float, stretch: Boolean): Pair<Size, Outline> {
        val current = provider.shapeBlock()
        val actual = scope.size
        val band = if (stretch && current is RoundedRectangle) {
            ceil(maxOf(actual.width / 2f, current.corners(actual, scope.layoutDirection, scope).topLeft * 3f) + supportPx + 2f)
        } else Float.POSITIVE_INFINITY
        val materialSize = Size(actual.width, minOf(actual.height, band * 2f + 1f))
        if (cached == null || shape != current || size != materialSize || density != scope.density ||
            fontScale != scope.fontScale || direction != scope.layoutDirection
        ) {
            shape = current
            size = materialSize
            density = scope.density
            fontScale = scope.fontScale
            direction = scope.layoutDirection
            cached = current.createOutline(materialSize, scope.layoutDirection, scope)
        }
        return materialSize to checkNotNull(cached)
    }
}

internal fun DrawScope.drawLiquidDecoration(layer: GraphicsLayer, extraHeight: Float) {
    if (extraHeight <= 0f) {
        drawLayer(layer)
        return
    }
    val cut = (layer.size.height / 2).toFloat()
    val width = layer.size.width.toFloat()
    val height = layer.size.height.toFloat()
    val canvas = drawContext.canvas
    canvas.save()
    canvas.clipRect(-width, -height, width * 2f, cut)
    drawLayer(layer)
    canvas.restore()
    canvas.save()
    canvas.clipRect(-width, cut, width * 2f, cut + extraHeight)
    withTransform({
        translate(0f, cut)
        scale(1f, extraHeight + 1f, Offset.Zero)
        translate(0f, -cut)
    }) { drawLayer(layer) }
    canvas.restore()
    canvas.save()
    canvas.clipRect(-width, cut + extraHeight, width * 2f, (height + extraHeight) * 2f)
    canvas.translate(0f, extraHeight)
    drawLayer(layer)
    canvas.restore()
}
