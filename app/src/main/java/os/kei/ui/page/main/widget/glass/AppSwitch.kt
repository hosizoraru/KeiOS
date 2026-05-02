package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlinx.coroutines.flow.collectLatest
import os.kei.core.ui.snapshot.rememberAppSnapshotFlowManager
import os.kei.ui.animation.DampedDragAnimation
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch

val LocalLiquidControlsEnabled = staticCompositionLocalOf { true }

private val AppLiquidSwitchLightBlue = Color(0xFF3B82F6)
private val AppLiquidSwitchDarkBlue = Color(0xFF7AB8FF)

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val touchModifier = modifier
        .defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)

    if (!LocalLiquidControlsEnabled.current) {
        Box(
            modifier = touchModifier,
            contentAlignment = Alignment.Center
        ) {
            MiuixSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
        return
    }

    val switchBackdrop = rememberLayerBackdrop()
    Box(
        modifier = touchModifier,
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.matchParentSize().layerBackdrop(switchBackdrop))
        LiquidSwitchToggle(
            selected = { checked },
            onSelect = onCheckedChange,
            backdrop = switchBackdrop,
            enabled = enabled,
            modifier = Modifier.matchParentSize(),
            checkedColor = if (androidx.compose.foundation.isSystemInDarkTheme()) {
                AppLiquidSwitchDarkBlue
            } else {
                AppLiquidSwitchLightBlue
            }
        )
    }
}

@Composable
private fun LiquidSwitchToggle(
    selected: () -> Boolean,
    onSelect: (Boolean) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedColor: Color = Color.Unspecified
) {
    val isLightTheme = !androidx.compose.foundation.isSystemInDarkTheme()
    val accentColor = if (checkedColor.isSpecified) {
        checkedColor
    } else if (isLightTheme) {
        Color(0xFF34C759)
    } else {
        Color(0xFF30D158)
    }
    val trackColor = if (isLightTheme) {
        Color(0xFF787878).copy(alpha = 0.20f)
    } else {
        Color(0xFF787880).copy(alpha = 0.36f)
    }
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val dragWidth = with(density) { 20.dp.toPx() }
    val animationScope = rememberCoroutineScope()
    var didDrag by remember { mutableStateOf(false) }
    var dragDistancePx by remember { mutableFloatStateOf(0f) }
    var fraction by remember { mutableFloatStateOf(if (selected()) 1f else 0f) }
    val toggleInteractionSource = remember { MutableInteractionSource() }
    val dampedDragAnimation = remember(animationScope, dragWidth, isLtr, touchSlop) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.5f,
            consumeDragChanges = true,
            onDragStarted = {
                dragDistancePx = 0f
            },
            onDragStopped = {
                if (!enabled) return@DampedDragAnimation
                if (didDrag) {
                    fraction = if (targetValue >= 0.5f) 1f else 0f
                    onSelect(fraction == 1f)
                } else {
                    fraction = if (selected()) 1f else 0f
                }
                didDrag = false
                dragDistancePx = 0f
            },
            onDrag = { _, dragAmount ->
                if (!enabled) return@DampedDragAnimation
                dragDistancePx += dragAmount.getDistance()
                if (!didDrag) {
                    didDrag = dragDistancePx > touchSlop
                }
                if (!didDrag) {
                    return@DampedDragAnimation
                }
                val delta = dragAmount.x / dragWidth
                fraction = if (isLtr) {
                    (fraction + delta).fastCoerceIn(0f, 1f)
                } else {
                    (fraction - delta).fastCoerceIn(0f, 1f)
                }
            }
        )
    }
    val snapshotFlowManager = rememberAppSnapshotFlowManager()
    LaunchedEffect(dampedDragAnimation, snapshotFlowManager) {
        snapshotFlowManager.snapshotFlow { fraction }
            .collectLatest { value -> dampedDragAnimation.updateValue(value) }
    }
    val externalSelected = selected()
    LaunchedEffect(externalSelected) {
        val target = if (externalSelected) 1f else 0f
        if (target != fraction) {
            fraction = target
            dampedDragAnimation.animateToValue(target)
        }
    }

    val trackBackdrop = rememberLayerBackdrop()
    val combinedBackdrop = rememberCombinedBackdrop(
        backdrop,
        rememberBackdrop(trackBackdrop) { drawBackdrop ->
            val progress = dampedDragAnimation.pressProgress
            val scaleX = lerp(2f / 3f, 0.75f, progress)
            val scaleY = lerp(0f, 0.75f, progress)
            scale(scaleX, scaleY) { drawBackdrop() }
        }
    )

    Box(
        modifier = modifier
            .then(if (enabled) dampedDragAnimation.modifier else Modifier)
            .toggleable(
                value = externalSelected,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = toggleInteractionSource,
                indication = null,
                onValueChange = onSelect
            )
            .graphicsLayer {
                alpha = if (enabled) 1f else AppInteractiveTokens.disabledContentAlpha
            }
            .semantics {
                role = Role.Switch
                toggleableState = ToggleableState(externalSelected)
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .layerBackdrop(trackBackdrop)
                .clip(Capsule())
                .drawBehind {
                    drawRect(lerpColor(trackColor, accentColor, dampedDragAnimation.value))
                }
                .size(64.dp, 28.dp)
        )

        Box(
            Modifier
                .graphicsLayer {
                    val padding = 2.dp.toPx()
                    translationX = if (isLtr) {
                        lerp(padding, padding + dragWidth, dampedDragAnimation.value)
                    } else {
                        lerp(-padding, -(padding + dragWidth), dampedDragAnimation.value)
                    }
                }
                .semantics { role = Role.Switch }
                .drawBackdrop(
                    backdrop = combinedBackdrop,
                    shape = { Capsule() },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8.dp.toPx() * (1f - progress))
                        lens(
                            5.dp.toPx() * progress,
                            10.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = progress
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 4.dp,
                            color = Color.Black.copy(alpha = 0.05f)
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(radius = 4.dp * progress, alpha = progress)
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 50f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 1f - dampedDragAnimation.pressProgress))
                    }
                )
                .size(40.dp, 24.dp)
        )
    }
}
