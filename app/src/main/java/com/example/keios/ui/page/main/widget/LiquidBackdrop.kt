package com.example.keios.ui.page.main.widget

import android.view.View
import android.view.ViewGroup
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import com.qmdeve.liquidglass.widget.LiquidGlassView
import java.lang.ref.WeakReference
import java.util.WeakHashMap

internal data class LiquidBackdropSpec(
    val cornerRadiusPx: Float,
    val refractionHeightPx: Float,
    val refractionOffsetPx: Float,
    val blurRadiusPx: Float,
    val dispersion: Float,
    val tintAlpha: Float,
    val tintRed: Float,
    val tintGreen: Float,
    val tintBlue: Float
)

@Composable
internal fun LiquidBackdropLayer(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    fallbackColor: Color,
    spec: LiquidBackdropSpec
) {
    val hostView = LocalView.current
    Box(modifier = modifier.background(fallbackColor)) {
        if (enabled) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    PassthroughLiquidGlassView(context).apply {
                        setDraggableEnabled(false)
                        setElasticEnabled(false)
                        setTouchEffectEnabled(false)
                        isClickable = false
                        isLongClickable = false
                        isFocusable = false
                        isFocusableInTouchMode = false
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }
                },
                update = { view ->
                    view.setCornerRadius(spec.cornerRadiusPx)
                    view.setRefractionHeight(spec.refractionHeightPx)
                    view.setRefractionOffset(spec.refractionOffsetPx)
                    view.setDispersion(spec.dispersion)
                    view.setBlurRadius(spec.blurRadiusPx)
                    view.setTintAlpha(spec.tintAlpha)
                    view.setTintColorRed(spec.tintRed)
                    view.setTintColorGreen(spec.tintGreen)
                    view.setTintColorBlue(spec.tintBlue)
                    bindLiquidBackdropSource(hostView, view)
                }
            )
        }
    }
}

private class PassthroughLiquidGlassView(context: android.content.Context) : LiquidGlassView(context) {
    override fun dispatchTouchEvent(event: MotionEvent): Boolean = false

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean = false

    override fun onTouchEvent(event: MotionEvent): Boolean = false
}

private object LiquidBackdropSourceRegistry {
    val lastBoundSource = WeakHashMap<LiquidGlassView, WeakReference<ViewGroup>>()
}

private fun bindLiquidBackdropSource(hostView: View, liquidView: LiquidGlassView): Boolean {
    val contentRoot = hostView.rootView.findViewById<ViewGroup>(android.R.id.content) ?: return false
    val source = resolveLiquidBackdropSource(contentRoot, liquidView) ?: return false
    val previous = LiquidBackdropSourceRegistry.lastBoundSource[liquidView]?.get()
    if (previous !== source) {
        liquidView.bind(source)
        LiquidBackdropSourceRegistry.lastBoundSource[liquidView] = WeakReference(source)
    }
    return true
}

private fun resolveLiquidBackdropSource(root: ViewGroup, liquidView: View): ViewGroup? {
    var bestCandidate: ViewGroup? = null
    var bestArea = -1L
    for (index in 0 until root.childCount) {
        val child = root.getChildAt(index)
        if (child !is ViewGroup) continue
        if (!isUsableLiquidSource(child, liquidView)) continue
        val area = child.width.toLong() * child.height.toLong()
        if (area > bestArea) {
            bestArea = area
            bestCandidate = child
        }
    }
    return bestCandidate
}

private fun isUsableLiquidSource(candidate: ViewGroup, liquidView: View): Boolean {
    if (!candidate.isAttachedToWindow || candidate.visibility != View.VISIBLE) return false
    if (candidate.alpha <= 0.01f) return false
    if (candidate.width <= 0 || candidate.height <= 0) return false
    if (candidate === liquidView) return false
    return !containsDescendant(candidate, liquidView)
}

private fun containsDescendant(parent: View, target: View): Boolean {
    if (parent === target) return true
    if (parent !is ViewGroup) return false
    for (index in 0 until parent.childCount) {
        if (containsDescendant(parent.getChildAt(index), target)) {
            return true
        }
    }
    return false
}
