package os.kei.ui.page.main.widget.glass

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.capsule.ContinuousCapsule
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import androidx.compose.foundation.text.BasicText
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppFloatingSearchDock(
    backdrop: Backdrop?,
    expanded: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    searchIcon: ImageVector,
    contentDescription: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    width: Dp = 276.dp,
    size: Dp = AppChromeTokens.floatingBottomBarOuterHeight,
    iconSize: Dp = 27.dp,
    gap: Dp = 8.dp,
    accent: Color = MiuixTheme.colorScheme.primary
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val fieldWidth by animateDpAsState(
        targetValue = if (expanded) width else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 260
        ),
        label = "app_floating_search_field_width"
    )
    val totalWidth by animateDpAsState(
        targetValue = size + if (expanded) gap + width else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 260
        ),
        label = "app_floating_search_total_width"
    )

    LaunchedEffect(expanded) {
        if (expanded) {
            focusRequester.requestFocus()
        } else {
            focusManager.clearFocus()
        }
    }

    Row(
        modifier = modifier
            .width(totalWidth)
            .height(size),
        horizontalArrangement = Arrangement.spacedBy(gap, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (fieldWidth > 1.dp) {
            AppLiquidFloatingSurface(
                modifier = Modifier
                    .width(fieldWidth)
                    .height(size),
                shape = ContinuousCapsule,
                backdrop = backdrop,
                pressDurationMillis = 120,
                pressLabel = "app_floating_search_field_press"
            ) {
                AppFloatingSearchField(
                    query = query,
                    onQueryChange = onQueryChange,
                    focusRequester = focusRequester,
                    onFocusActiveChange = { active ->
                        if (active) onExpandedChange(true)
                    },
                    searchIcon = searchIcon,
                    placeholder = placeholder,
                    accent = accent,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        AppFloatingLiquidActionButton(
            backdrop = backdrop,
            icon = searchIcon,
            contentDescription = contentDescription,
            onClick = { onExpandedChange(!expanded) },
            size = size,
            iconSize = iconSize,
            iconTint = if (expanded) accent else MiuixTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun AppFloatingSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onFocusActiveChange: (Boolean) -> Unit,
    searchIcon: ImageVector,
    placeholder: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val contentColor = MiuixTheme.colorScheme.onBackground
    val placeholderColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.78f)
    val interactionSource = remember { MutableInteractionSource() }
    val textStyle = TextStyle(
        color = contentColor,
        fontSize = AppTypographyTokens.CardHeader.fontSize,
        lineHeight = AppTypographyTokens.CardHeader.lineHeight,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
    Row(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onFocusActiveChange(true)
                focusRequester.requestFocus()
            }
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = searchIcon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = accent
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = textStyle,
            cursorBrush = SolidColor(accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onFocusActiveChange(false)
                    focusManager.clearFocus()
                }
            ),
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 24.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { state -> onFocusActiveChange(state.isFocused) },
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isBlank()) {
                        BasicText(
                            text = placeholder,
                            style = textStyle.copy(color = placeholderColor),
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
