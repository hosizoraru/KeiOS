package os.kei.ui.page.main.widget.glass

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppLiquidTextButton(
    backdrop: Backdrop?,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = MiuixTheme.colorScheme.primary,
    containerColor: Color? = null,
    leadingIcon: ImageVector? = null,
    iconTint: Color = textColor,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onPressedChange: ((Boolean) -> Unit)? = null,
    blurRadius: Dp? = null,
    variant: GlassVariant = GlassVariant.Content,
    minHeight: Dp = defaultGlassTextButtonMinHeight(variant),
    horizontalPadding: Dp = defaultGlassTextButtonHorizontalPadding(variant),
    verticalPadding: Dp = defaultGlassTextButtonVerticalPadding(variant),
    textMaxLines: Int = Int.MAX_VALUE,
    textOverflow: TextOverflow = TextOverflow.Clip,
    textSoftWrap: Boolean = true,
    pressScaleEnabled: Boolean = true,
    pressOverlayEnabled: Boolean = true
) {
    GlassTextButton(
        backdrop = backdrop,
        text = text,
        onClick = onClick,
        modifier = modifier,
        textColor = textColor,
        containerColor = containerColor,
        leadingIcon = leadingIcon,
        iconTint = iconTint,
        enabled = enabled,
        onLongClick = onLongClick,
        onPressedChange = onPressedChange,
        blurRadius = blurRadius,
        variant = variant,
        minHeight = minHeight,
        horizontalPadding = horizontalPadding,
        verticalPadding = verticalPadding,
        textMaxLines = textMaxLines,
        textOverflow = textOverflow,
        textSoftWrap = textSoftWrap,
        pressScaleEnabled = pressScaleEnabled,
        pressOverlayEnabled = pressOverlayEnabled
    )
}
