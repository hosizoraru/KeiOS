package com.example.keios.ui.page.main.widget

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GlassSearchField(
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
    minHeight: Dp = AppInteractiveTokens.glassSearchFieldMinHeight,
    horizontalPadding: Dp = AppInteractiveTokens.glassSearchFieldHorizontalPadding,
    verticalPadding: Dp = AppInteractiveTokens.glassSearchFieldVerticalPadding,
) {
    val focusManager = LocalFocusManager.current
    val isDark = isSystemInDarkTheme()
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
            color = glass.borderColor,
            shape = ContinuousCapsule
        )
    }

    val contentAlignment = when (textAlign) {
        TextAlign.Center -> Alignment.Center
        TextAlign.End, TextAlign.Right -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .clip(ContinuousCapsule)
            .then(
                if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        effects = {
                            vibrancy()
                            blur(glass.blur.toPx())
                            lens(
                                glass.lensStart.toPx(),
                                glass.lensEnd.toPx()
                            )
                        },
                        highlight = {
                            Highlight.Default.copy(alpha = glass.highlightAlpha)
                        },
                        shadow = {
                            Shadow.Default.copy(
                                color = Color.Black.copy(alpha = glass.shadowAlpha)
                            )
                        },
                        onDrawSurface = {
                            if (variant == GlassVariant.Bar) {
                                drawRect(fallbackSurface.copy(alpha = glass.fallbackAlpha))
                            } else {
                                drawRect(glass.baseColor)
                                if (glass.overlayColor != Color.Transparent) drawRect(glass.overlayColor)
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
            .then(borderModifier)
            .padding(
                horizontal = horizontalPadding,
                vertical = verticalPadding
            )
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = inputTextStyle,
            cursorBrush = SolidColor(textColor),
            visualTransformation = visualTransformation,
            keyboardOptions = if (singleLine) KeyboardOptions(imeAction = ImeAction.Done) else KeyboardOptions.Default,
            keyboardActions = KeyboardActions(
                onDone = {
                    onImeActionDone?.invoke()
                    focusManager.clearFocus()
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(align = Alignment.CenterVertically),
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
