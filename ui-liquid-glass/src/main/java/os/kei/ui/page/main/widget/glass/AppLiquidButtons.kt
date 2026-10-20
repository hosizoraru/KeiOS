@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import os.kei.ui.page.main.widget.isAppInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import os.kei.ui.animation.InteractiveHighlight
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.motion.appMotionFloatState
import os.kei.ui.page.main.widget.shape.appSquircleBackground
import os.kei.ui.page.main.widget.shape.appSquircleBorder
import os.kei.ui.page.main.widget.shape.drawAppSquircleBackground
import os.kei.ui.page.main.widget.shape.drawAppSquircleBorder
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipAnchorPosition
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Shared effects configuration for liquid buttons.
 * Extracted to avoid duplication and ensure consistent visual behavior.
 */
private fun BackdropEffectScope.applyLiquidButtonEffects(
    glass: GlassStyle,
    variant: GlassVariant,
    pressProgress: Float,
) {
    vibrancy()
    blur(
        lerp(
            glass.blur.toPx(),
            (glass.blur * 0.68f).toPx(),
            pressProgress,
        ),
    )
    safeLiquidLens(
        glass.lensStart.toPx() + 3.dp.toPx() * pressProgress,
        glass.lensEnd.toPx() + 5.dp.toPx() * pressProgress,
        chromaticAberration = variant != GlassVariant.Compact,
        depthEffect = true,
    )
}

@Composable
fun AppLiquidIconButton(
    backdrop: Backdrop?,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    width: Dp = Dp.Unspecified,
    height: Dp = Dp.Unspecified,
    shape: Shape = ContinuousCapsule,
    blurRadius: Dp? = null,
    variant: GlassVariant = GlassVariant.Content,
    iconTint: Color = MiuixTheme.colorScheme.primary,
    iconModifier: Modifier = Modifier,
    containerColor: Color? = null,
    enabled: Boolean = true,
    onPressedChange: ((Boolean) -> Unit)? = null,
    tooltipText: String? = contentDescription.takeIf { it.isNotBlank() },
    badgeLabel: String? = null,
    badgeColor: Color? = null,
    badgeContentColor: Color? = null,
    /**
     * Opt-in Liquid material for the badge. Off by default: Apple's Materials guidance is to use the
     * effect sparingly, and a badge on ordinary content wants the flat fill.
     */
    badgeBackdrop: Backdrop? = null,
    containerAlphaOverride: Float? = null,
) {
    val isDark = isAppInDarkTheme()
    val resolvedWidth = if (width == Dp.Unspecified) defaultAppLiquidIconButtonSize(variant) else width
    val resolvedHeight = if (height == Dp.Unspecified) defaultAppLiquidIconButtonSize(variant) else height
    AppLiquidIconButtonTooltip(
        tooltipText = tooltipText,
        enabled = enabled && onLongClick == null,
    ) {
        AppLiquidIconButtonContainer(
            backdrop = backdrop,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier,
            width = resolvedWidth,
            height = resolvedHeight,
            shape = shape,
            blurRadius = blurRadius,
            variant = variant,
            isDark = isDark,
            containerColor = containerColor,
            containerAlphaOverride = containerAlphaOverride,
            contentTint = iconTint,
            enabled = enabled,
            onPressedChange = onPressedChange,
        ) {
            AppLiquidIconButtonIcon(
                icon = icon,
                contentDescription = contentDescription,
                modifier = iconModifier,
                iconTint = iconTint,
                badgeLabel = badgeLabel,
                badgeColor = badgeColor,
                badgeContentColor = badgeContentColor,
                badgeBackdrop = badgeBackdrop,
            )
        }
    }
}

@Composable
fun AppLiquidIconButton(
    backdrop: Backdrop?,
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    width: Dp = Dp.Unspecified,
    height: Dp = Dp.Unspecified,
    shape: Shape = ContinuousCapsule,
    blurRadius: Dp? = null,
    variant: GlassVariant = GlassVariant.Content,
    iconTint: Color = Color.Unspecified,
    iconModifier: Modifier = Modifier,
    containerColor: Color? = null,
    enabled: Boolean = true,
    onPressedChange: ((Boolean) -> Unit)? = null,
    tooltipText: String? = contentDescription.takeIf { it.isNotBlank() },
    containerAlphaOverride: Float? = null,
) {
    val isDark = isAppInDarkTheme()
    val resolvedWidth = if (width == Dp.Unspecified) defaultAppLiquidIconButtonSize(variant) else width
    val resolvedHeight = if (height == Dp.Unspecified) defaultAppLiquidIconButtonSize(variant) else height
    AppLiquidIconButtonTooltip(
        tooltipText = tooltipText,
        enabled = enabled && onLongClick == null,
    ) {
        AppLiquidIconButtonContainer(
            backdrop = backdrop,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier,
            width = resolvedWidth,
            height = resolvedHeight,
            shape = shape,
            blurRadius = blurRadius,
            variant = variant,
            isDark = isDark,
            containerColor = containerColor,
            containerAlphaOverride = containerAlphaOverride,
            contentTint = iconTint,
            enabled = enabled,
            onPressedChange = onPressedChange,
        ) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                modifier = iconModifier,
                tint = iconTint,
            )
        }
    }
}

@Composable
private fun AppLiquidIconButtonIcon(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier,
    iconTint: Color,
    badgeLabel: String?,
    badgeColor: Color?,
    badgeContentColor: Color?,
    badgeBackdrop: Backdrop?,
) {
    AppLiquidBadgedIcon(
        icon = icon,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = iconTint,
        badgeLabel = badgeLabel,
        badgeColor = badgeColor,
        badgeContentColor = badgeContentColor,
        badgeBackdrop = badgeBackdrop,
    )
}

@Composable
private fun AppLiquidIconButtonTooltip(
    tooltipText: String?,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    val resolvedText = tooltipText?.takeIf { it.isNotBlank() }
    if (resolvedText == null || !enabled) {
        content()
        return
    }
    TooltipBox(
        text = resolvedText,
        positioning = TooltipAnchorPosition.Above,
        content = content,
    )
}

@Composable
private fun AppLiquidIconButtonContainer(
    backdrop: Backdrop?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier,
    width: Dp,
    height: Dp,
    shape: Shape,
    blurRadius: Dp?,
    variant: GlassVariant,
    isDark: Boolean,
    containerColor: Color?,
    containerAlphaOverride: Float?,
    contentTint: Color,
    enabled: Boolean,
    onPressedChange: ((Boolean) -> Unit)?,
    content: @Composable () -> Unit,
) {
    val activeBackdrop = activeGlassBackdrop(backdrop)
    val liquidFlat = liquidFlatField(activeBackdrop)
    val fallbackSurface = MiuixTheme.colorScheme.surfaceContainer
    val glass =
        glassStyle(
            isDark = isDark,
            variant = variant,
            blurRadius = blurRadius,
        ).let { baseStyle ->
            val accentSource = containerColor ?: contentTint
            baseStyle.tintWithAccent(
                accentColor = resolveGlassAccentColor(accentSource, isDark),
                isDark = isDark,
            )
        }
    val surfaceOverlayColor =
        resolveDarkCapsuleOverlayColor(
            defaultOverlayColor = glass.overlayColor,
            isDark = isDark,
        )
    val surfaceHighlightAlpha =
        resolveDarkCapsuleHighlightAlpha(
            defaultAlpha = glass.highlightAlpha,
            isDark = isDark,
            variant = variant,
        )
    val containerOverlay =
        resolveLiquidButtonContainerOverlay(
            containerColor = containerColor,
            containerAlphaOverride = containerAlphaOverride,
            variant = variant,
            isDark = isDark,
        )
    val transparentContainer = containerColor?.alpha == 0f
    val showBorder = glass.showBorder && !transparentContainer && containerColor == null
    val pressedOverlayColor =
        appControlPressedOverlayColor(
            isDark = isDark,
            variant = variant,
            accentColor = containerOverlay ?: Color.Unspecified,
        )
    val animationScope = rememberCoroutineScope()
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    val interactiveHighlight =
        remember(animationScope, transitionAnimationsEnabled) {
            InteractiveHighlight(
                animationScope = animationScope,
                animationsEnabled = transitionAnimationsEnabled,
            )
        }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScaleState =
        appMotionFloatState(
            targetValue =
                if (transitionAnimationsEnabled && isPressed) {
                    AppInteractiveTokens.pressedScale
                } else {
                    1f
                },
            durationMillis = 110,
            label = "glass_icon_button_scale",
        )
    val pressedOverlayAlphaState =
        appMotionFloatState(
            targetValue = appControlPressedOverlayAlpha(isPressed = isPressed, isDark = isDark),
            durationMillis = 110,
            label = "glass_icon_button_overlay",
        )
    val borderAlphaState =
        appMotionFloatState(
            targetValue = if (isPressed) 0f else 1f,
            durationMillis = 110,
            label = "glass_icon_button_border_alpha",
        )
    val animatedScaleProvider = remember(animatedScaleState) { { animatedScaleState.value } }
    val pressedOverlayAlphaProvider = remember(pressedOverlayAlphaState) { { pressedOverlayAlphaState.value } }
    val borderAlphaProvider = remember(borderAlphaState) { { borderAlphaState.value } }
    LaunchedEffect(isPressed, onPressedChange) {
        onPressedChange?.invoke(isPressed)
    }
    DisposableEffect(onPressedChange) {
        onDispose { onPressedChange?.invoke(false) }
    }
    Box(
        modifier =
            modifier
                .then(
                    if (onLongClick != null) {
                        Modifier.combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = enabled,
                            role = Role.Button,
                            onClick = onClick,
                            onLongClick = onLongClick,
                        )
                    } else {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = enabled,
                            role = Role.Button,
                            onClick = onClick,
                        )
                    },
                ).minimumLiquidTouchTargetSize()
                .width(width)
                .height(height)
                .graphicsLayer {
                    val scale = animatedScaleProvider()
                    scaleX = scale
                    scaleY = scale
                    clip = false
                }.then(
                    if (activeBackdrop != null) {
                        if (liquidFlat != null) {
                            Modifier.drawFlatLiquidBackdrop(
                                field = liquidFlat,
                                shape = { shape },
                                highlight = {
                                    Highlight.Default.copy(alpha = surfaceHighlightAlpha)
                                },
                                shadow = {
                                    // Not `Shadow.Default`, whose 24dp blur loses every trace of the corner
                                    // rounding on a button this size.
                                    liquidGlassShadow(
                                        color =
                                            Color.Black.copy(
                                                alpha =
                                                    appLiquidButtonShadowAlpha(
                                                        baseAlpha = glass.shadowAlpha,
                                                        variant = variant,
                                                        isPressed = isPressed,
                                                    ),
                                            ),
                                    )
                                },
                                innerShadow = {
                                    val progress = if (enabled) interactiveHighlight.pressProgress else 0f
                                    InnerShadow(radius = 6.dp * progress, alpha = progress)
                                },
                                layerBlock = if (enabled) {
                                        { applyLiquidButtonLayer(interactiveHighlight) }
                                    } else {
                                        null
                                    },
                                onDrawSurface = {
                                    if (containerAlphaOverride == null || containerOverlay == null) {
                                        if (variant == GlassVariant.Bar) {
                                            drawRect(fallbackSurface.copy(alpha = glass.fallbackAlpha))
                                        } else {
                                            drawRect(glass.baseColor)
                                            if (surfaceOverlayColor != Color.Transparent) {
                                                drawRect(surfaceOverlayColor)
                                            }
                                        }
                                    }
                                    containerOverlay?.let { drawRect(it) }
                                },
                            )
                        } else {
                            Modifier.drawBackdrop(
                                backdrop = liquidFlat ?: activeBackdrop,
                                shape = { shape },
                                layerBlock =
                                    if (enabled) {
                                        { applyLiquidButtonLayer(interactiveHighlight) }
                                    } else {
                                        null
                                    },
                                effects = pooledLiquidEffects(LocalLiquidShaderPooling.current) {
                                    applyLiquidButtonEffects(
                                        glass = glass,
                                        variant = variant,
                                        pressProgress = if (enabled) interactiveHighlight.pressProgress else 0f,
                                    )
                                },
                                highlight = {
                                    Highlight.Default.copy(alpha = surfaceHighlightAlpha)
                                },
                                shadow = {
                                    // Not `Shadow.Default`, whose 24dp blur loses every trace of the corner
                                    // rounding on a button this size.
                                    liquidGlassShadow(
                                        color =
                                            Color.Black.copy(
                                                alpha =
                                                    appLiquidButtonShadowAlpha(
                                                        baseAlpha = glass.shadowAlpha,
                                                        variant = variant,
                                                        isPressed = isPressed,
                                                    ),
                                            ),
                                    )
                                },
                                innerShadow = {
                                    val progress = if (enabled) interactiveHighlight.pressProgress else 0f
                                    InnerShadow(radius = 6.dp * progress, alpha = progress)
                                },
                                onDrawSurface = {
                                    if (containerAlphaOverride == null || containerOverlay == null) {
                                        if (variant == GlassVariant.Bar) {
                                            drawRect(fallbackSurface.copy(alpha = glass.fallbackAlpha))
                                        } else {
                                            drawRect(glass.baseColor)
                                            if (surfaceOverlayColor != Color.Transparent) {
                                                drawRect(surfaceOverlayColor)
                                            }
                                        }
                                    }
                                    containerOverlay?.let { drawRect(it) }
                                },
                            )
                        }
                    } else {
                        val fallbackColor =
                            when {
                                transparentContainer -> Color.Transparent
                                containerOverlay != null -> containerOverlay
                                else -> fallbackSurface.copy(alpha = glass.fallbackAlpha)
                            }
                        Modifier
                            .appSquircleBackground(fallbackColor, 999.dp)
                    },
                ).then(
                    if (enabled) {
                        Modifier
                            .then(interactiveHighlight.modifier)
                            .then(interactiveHighlight.gestureModifier)
                    } else {
                        disabledContentAlphaModifier(enabled = false)
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (showBorder) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .drawAppSquircleBorder(
                            width = glass.borderWidth,
                            cornerRadius = 999.dp,
                        ) {
                            glass.borderColor.copy(alpha = glass.borderColor.alpha * borderAlphaProvider())
                        },
            )
        }
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .drawAppSquircleBackground(999.dp) {
                        pressedOverlayColor.copy(alpha = pressedOverlayAlphaProvider())
                    },
        )
        content()
    }
}

@Composable
fun AppLiquidTextButton(
    backdrop: Backdrop?,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    surfaceModifier: Modifier = Modifier,
    textColor: Color = MiuixTheme.colorScheme.primary,
    containerColor: Color? = null,
    leadingIcon: ImageVector? = null,
    iconTint: Color = textColor,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onPressedChange: ((Boolean) -> Unit)? = null,
    blurRadius: Dp? = null,
    variant: GlassVariant = GlassVariant.Content,
    minHeight: Dp = defaultAppLiquidTextButtonMinHeight(variant),
    horizontalPadding: Dp = defaultAppLiquidTextButtonHorizontalPadding(variant),
    verticalPadding: Dp = defaultAppLiquidTextButtonVerticalPadding(variant),
    textMaxLines: Int = Int.MAX_VALUE,
    textOverflow: TextOverflow = TextOverflow.Clip,
    textSoftWrap: Boolean = true,
    textSize: TextUnit = AppTypographyTokens.Body.fontSize,
    textLineHeight: TextUnit = AppTypographyTokens.Body.lineHeight,
    textFontWeight: FontWeight = AppTypographyTokens.BodyEmphasis.fontWeight,
    pressScaleEnabled: Boolean = true,
    pressOverlayEnabled: Boolean = true,
    consumeDragChangesForInteraction: Boolean = false,
    containerAlphaOverride: Float? = null,
    leadingIconModifier: Modifier = Modifier,
    leadingContentGap: Dp = AppInteractiveTokens.controlContentGap,
    role: Role = Role.Button,
    selected: Boolean? = null,
    toggleableState: ToggleableState? = null,
) {
    val liquidControlsEnabled = LocalLiquidControlsEnabled.current
    val activeBackdrop = activeGlassBackdrop(backdrop)
    val liquidFlat = liquidFlatField(activeBackdrop)
    val isDark = isAppInDarkTheme()
    val fallbackSurface = MiuixTheme.colorScheme.surfaceContainer
    val longClick = onLongClick
    val glass =
        glassStyle(
            isDark = isDark,
            variant = variant,
            blurRadius = blurRadius,
        ).let { baseStyle ->
            val accentSource = containerColor ?: textColor
            baseStyle.tintWithAccent(
                accentColor = resolveGlassAccentColor(accentSource, isDark),
                isDark = isDark,
            )
        }
    val surfaceOverlayColor =
        resolveDarkCapsuleOverlayColor(
            defaultOverlayColor = glass.overlayColor,
            isDark = isDark,
        )
    val surfaceHighlightAlpha =
        resolveDarkCapsuleHighlightAlpha(
            defaultAlpha = glass.highlightAlpha,
            isDark = isDark,
            variant = variant,
        )
    val containerOverlay =
        resolveLiquidButtonContainerOverlay(
            containerColor = containerColor,
            containerAlphaOverride = containerAlphaOverride,
            variant = variant,
            isDark = isDark,
        )
    val transparentContainer = containerColor?.alpha == 0f
    val pressedOverlayColor =
        appControlPressedOverlayColor(
            isDark = isDark,
            variant = variant,
            accentColor = containerOverlay ?: textColor,
        )
    val liquidInteractionEnabled = enabled && liquidControlsEnabled && (pressScaleEnabled || pressOverlayEnabled)
    val animationScope = rememberCoroutineScope()
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    val interactiveHighlight =
        remember(
            animationScope,
            consumeDragChangesForInteraction,
            transitionAnimationsEnabled,
        ) {
            InteractiveHighlight(
                animationScope = animationScope,
                consumeDragChanges = consumeDragChangesForInteraction,
                animationsEnabled = transitionAnimationsEnabled,
            )
        }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScaleState =
        appMotionFloatState(
            targetValue =
                if (
                    transitionAnimationsEnabled &&
                    enabled &&
                    isPressed &&
                    pressScaleEnabled
                ) {
                    AppInteractiveTokens.pressedScale
                } else {
                    1f
                },
            durationMillis = 110,
            label = "app_liquid_text_button_scale",
        )
    val pressedOverlayAlphaState =
        appMotionFloatState(
            targetValue =
                appControlPressedOverlayAlpha(
                    isPressed = enabled && isPressed && pressOverlayEnabled,
                    isDark = isDark,
                ),
            durationMillis = 110,
            label = "app_liquid_text_button_overlay",
        )
    val borderAlphaState =
        appMotionFloatState(
            targetValue = if (enabled && isPressed) 0f else 1f,
            durationMillis = 110,
            label = "app_liquid_text_button_border_alpha",
        )
    val animatedScaleProvider = remember(animatedScaleState) { { animatedScaleState.value } }
    val pressedOverlayAlphaProvider = remember(pressedOverlayAlphaState) { { pressedOverlayAlphaState.value } }
    val borderAlphaProvider = remember(borderAlphaState) { { borderAlphaState.value } }
    val borderModifier =
        if (glass.showBorder && containerColor == null) {
            Modifier.drawAppSquircleBorder(
                width = glass.borderWidth,
                cornerRadius = 999.dp,
            ) {
                glass.borderColor.copy(alpha = glass.borderColor.alpha * borderAlphaProvider())
            }
        } else {
            Modifier
        }
    val stateSemanticsModifier =
        if (selected != null || toggleableState != null) {
            Modifier.semantics {
                selected?.let { this.selected = it }
                toggleableState?.let { this.toggleableState = it }
            }
        } else {
            Modifier
        }

    LaunchedEffect(isPressed, onPressedChange) {
        onPressedChange?.invoke(isPressed)
    }
    DisposableEffect(onPressedChange) {
        onDispose { onPressedChange?.invoke(false) }
    }

    Box(
        modifier =
            modifier
                .then(
                    if (longClick != null) {
                        Modifier.combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = enabled,
                            role = role,
                            onClick = { if (enabled) onClick() },
                            onLongClick = longClick,
                        )
                    } else {
                        Modifier.clickable(
                            enabled = enabled,
                            interactionSource = interactionSource,
                            indication = null,
                            role = role,
                            onClick = onClick,
                        )
                    },
                ).then(stateSemanticsModifier)
                .minimumLiquidTouchTargetSize()
                .then(surfaceModifier)
                .defaultMinSize(minHeight = minHeight)
                .graphicsLayer {
                    val scale = animatedScaleProvider()
                    scaleX = scale
                    scaleY = scale
                    alpha = if (enabled) 1f else AppInteractiveTokens.disabledContentAlpha
                    clip = false
                }.then(
                    if (activeBackdrop != null) {
                        if (liquidFlat != null) {
                            Modifier.drawFlatLiquidBackdrop(
                                field = liquidFlat,
                                shape = { ContinuousCapsule },
                                highlight = {
                                    Highlight.Default.copy(alpha = surfaceHighlightAlpha)
                                },
                                shadow = {
                                    // Not `Shadow.Default`, whose 24dp blur loses every trace of the corner
                                    // rounding on a button this size.
                                    liquidGlassShadow(
                                        color =
                                            Color.Black.copy(
                                                alpha =
                                                    appLiquidButtonShadowAlpha(
                                                        baseAlpha = glass.shadowAlpha,
                                                        variant = variant,
                                                        isPressed = isPressed,
                                                    ),
                                            ),
                                    )
                                },
                                innerShadow = {
                                    val progress =
                                        if (liquidInteractionEnabled) interactiveHighlight.pressProgress else 0f
                                    InnerShadow(radius = 6.dp * progress, alpha = progress)
                                },
                                layerBlock = if (liquidInteractionEnabled) {
                                        { applyLiquidButtonLayer(interactiveHighlight) }
                                    } else {
                                        null
                                    },
                                onDrawSurface = {
                                    if (containerAlphaOverride == null || containerOverlay == null) {
                                        if (variant == GlassVariant.Bar) {
                                            drawRect(fallbackSurface.copy(alpha = glass.fallbackAlpha))
                                        } else {
                                            drawRect(glass.baseColor)
                                            if (surfaceOverlayColor != Color.Transparent) {
                                                drawRect(surfaceOverlayColor)
                                            }
                                        }
                                    }
                                    containerOverlay?.let { drawRect(it) }
                                },
                            )
                        } else {
                            Modifier.drawBackdrop(
                                backdrop = liquidFlat ?: activeBackdrop,
                                shape = { ContinuousCapsule },
                                layerBlock =
                                    if (liquidInteractionEnabled) {
                                        { applyLiquidButtonLayer(interactiveHighlight) }
                                    } else {
                                        null
                                    },
                                effects = pooledLiquidEffects(LocalLiquidShaderPooling.current) {
                                    applyLiquidButtonEffects(
                                        glass = glass,
                                        variant = variant,
                                        pressProgress =
                                            if (liquidInteractionEnabled) {
                                                interactiveHighlight.pressProgress
                                            } else {
                                                0f
                                            },
                                    )
                                },
                                highlight = {
                                    Highlight.Default.copy(alpha = surfaceHighlightAlpha)
                                },
                                shadow = {
                                    // Not `Shadow.Default`, whose 24dp blur loses every trace of the corner
                                    // rounding on a button this size.
                                    liquidGlassShadow(
                                        color =
                                            Color.Black.copy(
                                                alpha =
                                                    appLiquidButtonShadowAlpha(
                                                        baseAlpha = glass.shadowAlpha,
                                                        variant = variant,
                                                        isPressed = isPressed,
                                                    ),
                                            ),
                                    )
                                },
                                innerShadow = {
                                    val progress =
                                        if (liquidInteractionEnabled) interactiveHighlight.pressProgress else 0f
                                    InnerShadow(radius = 6.dp * progress, alpha = progress)
                                },
                                onDrawSurface = {
                                    if (containerAlphaOverride == null || containerOverlay == null) {
                                        if (variant == GlassVariant.Bar) {
                                            drawRect(fallbackSurface.copy(alpha = glass.fallbackAlpha))
                                        } else {
                                            drawRect(glass.baseColor)
                                            if (surfaceOverlayColor != Color.Transparent) {
                                                drawRect(surfaceOverlayColor)
                                            }
                                        }
                                    }
                                    containerOverlay?.let { drawRect(it) }
                                },
                            )
                        }
                    } else {
                        val fallbackColor =
                            when {
                                transparentContainer -> Color.Transparent
                                containerOverlay != null -> containerOverlay
                                else -> fallbackSurface.copy(alpha = glass.fallbackAlpha)
                            }
                        Modifier.appSquircleBackground(fallbackColor, 999.dp)
                    },
                ).then(
                    if (liquidInteractionEnabled) {
                        Modifier
                            .then(interactiveHighlight.modifier)
                            .then(interactiveHighlight.gestureModifier)
                    } else {
                        Modifier
                    },
                ).then(borderModifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .drawAppSquircleBackground(999.dp) {
                        pressedOverlayColor.copy(alpha = pressedOverlayAlphaProvider())
                    },
        )
        DisableSelection {
            Row(
                modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
                horizontalArrangement = Arrangement.spacedBy(leadingContentGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leadingIcon?.let { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = leadingIconModifier,
                    )
                }
                if (text.isNotBlank()) {
                    Text(
                        text = text,
                        color = textColor,
                        fontSize = textSize,
                        lineHeight = textLineHeight,
                        fontWeight = textFontWeight,
                        maxLines = textMaxLines,
                        overflow = textOverflow,
                        softWrap = textSoftWrap,
                    )
                }
            }
        }
    }
}

private fun Modifier.minimumLiquidTouchTargetSize(): Modifier =
    layout { measurable, constraints ->
        val placeable =
            measurable.measure(
                constraints.copy(
                    minWidth = 0,
                    minHeight = 0,
                ),
            )
        val minimumSizePx = 48.dp.roundToPx()
        val width = max(placeable.width, minimumSizePx).coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = max(placeable.height, minimumSizePx).coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            placeable.placeRelative(
                x = (width - placeable.width) / 2,
                y = (height - placeable.height) / 2,
            )
        }
    }

internal fun glassContainerOverlayAlpha(
    variant: GlassVariant,
    isDark: Boolean,
): Float =
    when (variant) {
        GlassVariant.Bar -> 0.34f
        GlassVariant.SheetInput -> 0.20f
        GlassVariant.SheetAction -> if (isDark) 0.24f else 0.34f
        GlassVariant.SheetPrimaryAction -> 0.18f
        GlassVariant.Compact -> if (isDark) 0.22f else 0.28f
        GlassVariant.SheetDangerAction -> 0.18f
        GlassVariant.Floating -> if (isDark) 0.20f else 0.18f
        GlassVariant.SearchField -> if (isDark) 0.18f else 0.22f
        GlassVariant.Content -> if (isDark) 0.26f else 0.32f
    }

internal fun resolveLiquidButtonContainerOverlay(
    containerColor: Color?,
    containerAlphaOverride: Float?,
    variant: GlassVariant,
    isDark: Boolean,
): Color? {
    if (containerColor == null || containerColor.alpha == 0f) return null
    val resolvedContainerColor =
        if (containerAlphaOverride != null) {
            containerColor
        } else {
            sanitizeCapsuleContainerColor(
                containerColor = containerColor,
                isDark = isDark,
            )
        }
    return resolvedContainerColor?.copy(
        alpha =
            containerAlphaOverride?.coerceIn(0f, 1f)
                ?: glassContainerOverlayAlpha(variant, isDark),
    )
}

private fun appLiquidButtonShadowAlpha(
    baseAlpha: Float,
    variant: GlassVariant,
    isPressed: Boolean,
): Float {
    val variantScale =
        when (variant) {
            GlassVariant.Floating -> 0.42f

            GlassVariant.Compact -> 0.58f

            GlassVariant.SearchField,
            GlassVariant.SheetAction,
            GlassVariant.SheetPrimaryAction,
            GlassVariant.SheetDangerAction,
            GlassVariant.SheetInput,
            -> 0.64f

            GlassVariant.Content -> 0.72f

            GlassVariant.Bar -> 0.84f
        }
    val pressScale = if (isPressed) 0.45f else 1f
    return baseAlpha * variantScale * pressScale
}

private fun resolveDarkCapsuleOverlayColor(
    defaultOverlayColor: Color,
    isDark: Boolean,
): Color {
    if (!isDark) return defaultOverlayColor
    if (defaultOverlayColor == Color.Transparent) return Color.Transparent
    return if (defaultOverlayColor.isNearNeutralWhite()) Color.Transparent else defaultOverlayColor
}

private fun resolveDarkCapsuleHighlightAlpha(
    defaultAlpha: Float,
    isDark: Boolean,
    variant: GlassVariant,
): Float {
    if (!isDark) return defaultAlpha
    val maxAlpha =
        when (variant) {
            GlassVariant.Bar -> 0.40f
            GlassVariant.SheetInput -> 0.42f
            GlassVariant.SheetAction -> 0.44f
            GlassVariant.SheetPrimaryAction -> 0.44f
            GlassVariant.SheetDangerAction -> 0.44f
            GlassVariant.Floating -> 0.46f
            GlassVariant.SearchField -> 0.48f
            GlassVariant.Compact -> 0.36f
            GlassVariant.Content -> 0.44f
        }
    return min(defaultAlpha, maxAlpha)
}

private fun GraphicsLayerScope.applyLiquidButtonLayer(interactiveHighlight: InteractiveHighlight) {
    val progress = interactiveHighlight.deformationProgress
    if (progress <= 0f) {
        translationX = 0f
        translationY = 0f
        scaleX = 1f
        scaleY = 1f
        return
    }

    val liquidScale = lerp(1f, 1f + 3.dp.toPx() / size.height.coerceAtLeast(1f), progress)
    val maxOffset = size.minDimension.coerceAtLeast(1f)
    val offset = interactiveHighlight.offset
    val offsetAngle = atan2(offset.y, offset.x)
    val maxDragScale = 3.dp.toPx() / size.height.coerceAtLeast(1f)

    translationX = maxOffset * tanh(0.045f * offset.x / maxOffset)
    translationY = maxOffset * tanh(0.045f * offset.y / maxOffset)
    scaleX = liquidScale +
        maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension.coerceAtLeast(1f)) *
        (size.width / size.height.coerceAtLeast(1f)).fastCoerceAtMost(1f)
    scaleY = liquidScale +
        maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension.coerceAtLeast(1f)) *
        (size.height / size.width.coerceAtLeast(1f)).fastCoerceAtMost(1f)
}

private fun sanitizeCapsuleContainerColor(
    containerColor: Color?,
    isDark: Boolean,
): Color? {
    if (containerColor == null) return null
    if (!isDark) return containerColor
    return if (containerColor.isNearNeutralWhite()) null else containerColor
}

private fun Color.isNearNeutralWhite(): Boolean {
    val maxChannel = maxOf(red, green, blue)
    val minChannel = minOf(red, green, blue)
    val chroma = maxChannel - minChannel
    return luminance() >= 0.88f && chroma <= 0.08f
}
