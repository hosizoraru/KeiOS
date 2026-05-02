package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.motion.appMotionFloatState
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppLiquidSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    textAlign: TextAlign = TextAlign.Start,
    fontSize: TextUnit = AppTypographyTokens.Body.fontSize,
    textColor: Color = MiuixTheme.colorScheme.onBackground,
    onImeActionDone: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    blurRadius: Dp? = null,
    variant: GlassVariant = GlassVariant.Content,
    minHeight: Dp = AppInteractiveTokens.appLiquidSearchFieldMinHeight,
    horizontalPadding: Dp = AppInteractiveTokens.appLiquidSearchFieldHorizontalPadding,
    verticalPadding: Dp = AppInteractiveTokens.appLiquidSearchFieldVerticalPadding,
    keyboardOptions: KeyboardOptions? = null,
    focusRequester: FocusRequester? = null,
) {
    val focusManager = LocalFocusManager.current
    val isDark = isSystemInDarkTheme()
    val activeBackdrop = backdrop.takeIf { LocalLiquidControlsEnabled.current }
    var focused by remember { mutableStateOf(false) }
    val focusProgress by appMotionFloatState(
        targetValue = if (focused) 1f else 0f,
        durationMillis = 140,
        label = "app_liquid_search_field_focus"
    )
    val placeholderColor = if (variant == GlassVariant.SheetInput) {
        textColor.copy(alpha = if (isDark) 0.72f else 0.62f)
    } else {
        MiuixTheme.colorScheme.onBackgroundVariant
    }
    val effectiveLineHeight = if (singleLine && variant == GlassVariant.SheetInput) {
        fontSize
    } else {
        AppTypographyTokens.Body.lineHeight
    }
    val inputTextStyle = TextStyle(
        color = textColor,
        fontSize = fontSize,
        lineHeight = effectiveLineHeight,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        textAlign = textAlign
    )
    val glass = glassStyle(
        isDark = isDark,
        variant = variant,
        blurRadius = blurRadius
    ).let { baseStyle ->
        if (variant == GlassVariant.SheetInput) {
            baseStyle.tintWithAccent(
                accentColor = textColor,
                isDark = isDark
            )
        } else {
            baseStyle
        }
    }
    val fallbackSurface = MiuixTheme.colorScheme.surfaceContainer
    val borderModifier = if (!glass.showBorder) {
        Modifier
    } else {
        Modifier.border(
            width = glass.borderWidth,
            color = glass.borderColor.copy(
                alpha = (glass.borderColor.alpha + 0.14f * focusProgress).coerceAtMost(1f)
            ),
            shape = ContinuousCapsule
        )
    }

    val contentAlignment = when (textAlign) {
        TextAlign.Center -> Alignment.Center
        TextAlign.End, TextAlign.Right -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }
    val effectiveKeyboardOptions = keyboardOptions ?: if (singleLine) {
        KeyboardOptions(imeAction = ImeAction.Done)
    } else {
        KeyboardOptions.Default
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .clip(ContinuousCapsule)
            .then(
                if (activeBackdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = activeBackdrop,
                        shape = { ContinuousCapsule },
                        layerBlock = {
                            val focusScale = 1f + 2.dp.toPx() / size.height.coerceAtLeast(1f) * focusProgress
                            scaleX = focusScale
                            scaleY = focusScale
                        },
                        effects = {
                            vibrancy()
                            blur(glass.blur.toPx())
                            lens(
                                glass.lensStart.toPx() + 4.dp.toPx() * focusProgress,
                                glass.lensEnd.toPx() + 6.dp.toPx() * focusProgress,
                                chromaticAberration = focusProgress > 0.01f,
                                depthEffect = focusProgress > 0.01f
                            )
                        },
                        highlight = {
                            Highlight.Default.copy(
                                alpha = (glass.highlightAlpha + 0.10f * focusProgress).coerceAtMost(1f)
                            )
                        },
                        shadow = {
                            Shadow.Default.copy(
                                color = Color.Black.copy(alpha = glass.shadowAlpha + 0.04f * focusProgress)
                            )
                        },
                        innerShadow = {
                            InnerShadow(
                                radius = 6.dp * focusProgress,
                                alpha = 0.22f * focusProgress
                            )
                        },
                        onDrawSurface = {
                            if (variant == GlassVariant.Bar) {
                                drawRect(fallbackSurface.copy(alpha = glass.fallbackAlpha))
                            } else {
                                drawRect(glass.baseColor)
                                if (glass.overlayColor != Color.Transparent) drawRect(glass.overlayColor)
                                if (focusProgress > 0f) {
                                    drawRect(textColor.copy(alpha = 0.04f * focusProgress))
                                }
                            }
                        }
                    )
                } else {
                    Modifier
                        .background(
                            glass.baseColor.takeIf { it != Color.Transparent }
                                ?: fallbackSurface.copy(alpha = glass.fallbackAlpha),
                            ContinuousCapsule
                        )
                        .then(
                            if (glass.overlayColor != Color.Transparent) {
                                Modifier.background(glass.overlayColor, ContinuousCapsule)
                            } else {
                                Modifier
                            }
                        )
                }
            )
            .graphicsLayer {
                shadowElevation = 2.dp.toPx() * focusProgress
                ambientShadowColor = Color.Black.copy(alpha = 0.06f * focusProgress)
                spotShadowColor = Color.Black.copy(alpha = 0.08f * focusProgress)
            }
            .then(borderModifier)
            .padding(
                horizontal = horizontalPadding,
                vertical = verticalPadding
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = inputTextStyle,
            cursorBrush = SolidColor(textColor),
            visualTransformation = visualTransformation,
            keyboardOptions = effectiveKeyboardOptions,
            keyboardActions = KeyboardActions(
                onDone = {
                    onImeActionDone?.invoke()
                    focusManager.clearFocus()
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(align = Alignment.CenterVertically)
                .onFocusChanged { focused = it.isFocused || it.hasFocus }
                .then(
                    if (focusRequester != null) {
                        Modifier.focusRequester(focusRequester)
                    } else {
                        Modifier
                    }
                ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.CenterVertically),
                    contentAlignment = contentAlignment
                ) {
                    if (value.isBlank()) {
                        BasicText(
                            text = label,
                            style = inputTextStyle.copy(color = placeholderColor),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}
