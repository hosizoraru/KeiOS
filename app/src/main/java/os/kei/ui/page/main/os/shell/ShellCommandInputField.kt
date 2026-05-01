package os.kei.ui.page.main.os.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ShellCommandInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    minHeight: Dp = 136.dp,
    focusRequestToken: Int = 0,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(focusRequestToken) {
        if (focusRequestToken > 0) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val isDark = isSystemInDarkTheme()
    val shape: CornerBasedShape = RoundedCornerShape(18.dp)
    val borderColor = if (isDark) {
        Color(0xFF9CCBFF).copy(alpha = 0.24f)
    } else {
        Color(0xFFC4DCF9).copy(alpha = 0.90f)
    }
    val baseColor = if (isDark) {
        Color(0xFF121A24).copy(alpha = 0.40f)
    } else {
        Color.White.copy(alpha = 0.66f)
    }
    val overlayColor = if (isDark) {
        Color(0xFF82B6F5).copy(alpha = 0.07f)
    } else {
        Color(0xFFE4F1FF).copy(alpha = 0.22f)
    }
    val textStyle = TextStyle(
        color = MiuixTheme.colorScheme.onBackground,
        fontSize = AppTypographyTokens.Body.fontSize,
        lineHeight = AppTypographyTokens.Body.lineHeight,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(shape)
            .background(baseColor, shape)
            .background(overlayColor, shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.TopStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle,
            cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight - 24.dp)
                .focusRequester(focusRequester),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopStart
                ) {
                    if (value.isBlank()) {
                        BasicText(
                            text = label,
                            style = textStyle.copy(
                                color = MiuixTheme.colorScheme.onBackgroundVariant
                            ),
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
