@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import os.kei.ui.page.main.widget.isAppInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import os.kei.ui.page.main.widget.motion.appMotionFloatState
import os.kei.ui.page.main.widget.shape.appSquircleBorder
import os.kei.ui.page.main.widget.shape.drawAppSquircleBackground
import androidx.compose.ui.graphics.lerp as lerpColor

@Composable
fun AppLiquidCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    val isDark = isAppInDarkTheme()
    val activeBackdrop = activeGlassBackdrop(backdrop)
    val liquidFlat = liquidFlatField(activeBackdrop)
    val accent = if (isDark) Color(0xFF7AB8FF) else Color(0xFF3B82F6)
    val uncheckedSurface =
        if (isDark) {
            Color(0xFF15181E).copy(alpha = 0.48f)
        } else {
            Color.White.copy(alpha = 0.78f)
        }
    val checkedSurface =
        liquidCheckboxCheckedSurfaceColor(isDark)
    val checkmarkColor = liquidCheckboxCheckmarkColor(isDark)
    val borderColor =
        if (checked) {
            accent.copy(alpha = if (isDark) 0.72f else 0.58f)
        } else if (isDark) {
            Color.White.copy(alpha = 0.20f)
        } else {
            Color(0xFFC9DCF7).copy(alpha = 0.90f)
        }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val checkProgressState =
        appMotionFloatState(
            targetValue = if (checked) 1f else 0f,
            durationMillis = 140,
            label = "app_liquid_checkbox_check",
        )
    val pressProgressState =
        appMotionFloatState(
            targetValue = if (isPressed) 1f else 0f,
            durationMillis = 110,
            label = "app_liquid_checkbox_press",
        )
    val checkProgressProvider = remember(checkProgressState) { { checkProgressState.value } }
    val pressProgressProvider = remember(pressProgressState) { { pressProgressState.value } }

    Box(
        modifier =
            modifier
                .requiredSize(48.dp)
                .graphicsLayer {
                    val pressProgress = pressProgressProvider()
                    val scale = 1f - pressProgress * 0.035f
                    scaleX = scale
                    scaleY = scale
                    alpha = if (enabled) 1f else AppInteractiveTokens.disabledContentAlpha
                }.then(
                    if (onCheckedChange != null) {
                        Modifier.toggleable(
                            value = checked,
                            enabled = enabled,
                            role = Role.Checkbox,
                            interactionSource = interactionSource,
                            indication = null,
                            onValueChange = onCheckedChange,
                        )
                    } else {
                        Modifier
                    },
                ).semantics {
                    role = Role.Checkbox
                    toggleableState = ToggleableState(checked)
                    if (!enabled) disabled()
                    contentDescription?.let { this.contentDescription = it }
                },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(AppLiquidCheckboxBoxSize)
                    .then(
                        if (activeBackdrop != null) {
                            if (liquidFlat != null) {
                                Modifier.drawFlatLiquidBackdrop(
                                    field = liquidFlat,
                                    shape = { ContinuousCapsule },
                                    highlight = {
                                        Highlight.Default.copy(alpha = if (isDark) 0.42f else 0.74f)
                                    },
                                    shadow = {
                                        // Not `Shadow.Default`: its 24dp blur is a card's shadow, spreading
                                        // 48dp around a 30dp box with no corner rounding left to see.
                                        liquidGlassShadow(Color.Black.copy(alpha = if (isDark) 0.10f else 0.08f))
                                    },
                                    innerShadow = {
                                        val pressProgress = pressProgressProvider()
                                        InnerShadow(radius = 4.dp * pressProgress, alpha = pressProgress)
                                    },
                                    onDrawSurface = {
                                        val checkProgress = checkProgressProvider()
                                        val pressProgress = pressProgressProvider()
                                        val surfaceColor = lerpColor(uncheckedSurface, checkedSurface, checkProgress)
                                        drawRect(surfaceColor)
                                        if (pressProgress > 0f) {
                                            drawRect(
                                                appControlPressedOverlayColor(
                                                    isDark = isDark,
                                                    variant = GlassVariant.Content,
                                                    accentColor = accent,
                                                ).copy(
                                                    alpha =
                                                        appControlPressedOverlayAlpha(
                                                            true,
                                                            isDark,
                                                        ) * pressProgress,
                                                ),
                                            )
                                        }
                                    },
                                )
                            } else {
                                Modifier.drawBackdrop(
                                    backdrop = liquidFlat ?: activeBackdrop,
                                    shape = { ContinuousCapsule },
                                    effects = pooledLiquidEffects(LocalLiquidShaderPooling.current) {
                                        vibrancy()
                                        blur(4.dp.toPx())
                                        safeLiquidLens(
                                            16.dp.toPx(),
                                            24.dp.toPx(),
                                            chromaticAberration = true,
                                            depthEffect = true,
                                        )
                                    },
                                    highlight = {
                                        Highlight.Default.copy(alpha = if (isDark) 0.42f else 0.74f)
                                    },
                                    shadow = {
                                        // Not `Shadow.Default`: its 24dp blur is a card's shadow, spreading
                                        // 48dp around a 30dp box with no corner rounding left to see.
                                        liquidGlassShadow(Color.Black.copy(alpha = if (isDark) 0.10f else 0.08f))
                                    },
                                    innerShadow = {
                                        val pressProgress = pressProgressProvider()
                                        InnerShadow(radius = 4.dp * pressProgress, alpha = pressProgress)
                                    },
                                    onDrawSurface = {
                                        val checkProgress = checkProgressProvider()
                                        val pressProgress = pressProgressProvider()
                                        val surfaceColor = lerpColor(uncheckedSurface, checkedSurface, checkProgress)
                                        drawRect(surfaceColor)
                                        if (pressProgress > 0f) {
                                            drawRect(
                                                appControlPressedOverlayColor(
                                                    isDark = isDark,
                                                    variant = GlassVariant.Content,
                                                    accentColor = accent,
                                                ).copy(
                                                    alpha =
                                                        appControlPressedOverlayAlpha(
                                                            true,
                                                            isDark,
                                                        ) * pressProgress,
                                                ),
                                            )
                                        }
                                    },
                                )
                            }
                        } else {
                            Modifier.drawAppSquircleBackground(999.dp) {
                                lerpColor(uncheckedSurface, checkedSurface, checkProgressProvider())
                            }
                        },
                    ).appSquircleBorder(
                        width = 1.dp,
                        color = borderColor,
                        cornerRadius = 999.dp,
                    ),
        )
        Canvas(modifier = Modifier.size(18.dp)) {
            val checkProgress = checkProgressProvider()
            if (checkProgress <= 0.01f) return@Canvas
            val stroke =
                Stroke(
                    width = 2.35.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            val firstEndProgress = (checkProgress / 0.52f).coerceIn(0f, 1f)
            val secondEndProgress = ((checkProgress - 0.34f) / 0.66f).coerceIn(0f, 1f)
            val start = Offset(size.width * 0.22f, size.height * 0.52f)
            val corner = Offset(size.width * 0.43f, size.height * 0.70f)
            val end = Offset(size.width * 0.80f, size.height * 0.30f)
            val firstEnd =
                Offset(
                    x = start.x + (corner.x - start.x) * firstEndProgress,
                    y = start.y + (corner.y - start.y) * firstEndProgress,
                )
            val secondEnd =
                Offset(
                    x = corner.x + (end.x - corner.x) * secondEndProgress,
                    y = corner.y + (end.y - corner.y) * secondEndProgress,
                )
            drawLine(
                color = checkmarkColor.copy(alpha = checkProgress),
                start = start,
                end = firstEnd,
                strokeWidth = stroke.width,
                cap = stroke.cap,
            )
            if (checkProgress > 0.34f) {
                drawLine(
                    color = checkmarkColor.copy(alpha = checkProgress),
                    start = corner,
                    end = secondEnd,
                    strokeWidth = stroke.width,
                    cap = stroke.cap,
                )
            }
        }
    }
}

internal fun liquidCheckboxCheckedSurfaceColor(isDark: Boolean): Color =
    if (isDark) {
        Color(0xFF7AB8FF).copy(alpha = 0.88f)
    } else {
        Color(0xFF3B82F6).copy(alpha = 0.92f)
    }

internal fun liquidCheckboxCheckmarkColor(isDark: Boolean): Color =
    if (isDark) {
        Color(0xFF071526)
    } else {
        Color.White
    }

/** The glass box itself, inside the 48dp touch target. */
private val AppLiquidCheckboxBoxSize = 30.dp
