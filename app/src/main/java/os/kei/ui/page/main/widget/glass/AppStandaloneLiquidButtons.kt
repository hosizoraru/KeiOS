package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.capsule.ContinuousCapsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppStandaloneLiquidTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonModifier: Modifier = Modifier,
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
    pressScaleEnabled: Boolean = true,
    pressOverlayEnabled: Boolean = true
) {
    val localBackdrop = rememberLayerBackdrop()
    AppStandaloneBackdropHost(
        backdrop = localBackdrop,
        modifier = modifier
    ) {
        AppLiquidTextButton(
            backdrop = localBackdrop,
            text = text,
            onClick = onClick,
            modifier = buttonModifier,
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
}

@Composable
fun AppStandaloneLiquidIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonModifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    width: Dp = Dp.Unspecified,
    height: Dp = Dp.Unspecified,
    shape: Shape = ContinuousCapsule,
    blurRadius: Dp? = null,
    variant: GlassVariant = GlassVariant.Content,
    iconTint: Color = MiuixTheme.colorScheme.primary,
    containerColor: Color? = null,
    enabled: Boolean = true
) {
    val localBackdrop = rememberLayerBackdrop()
    AppStandaloneBackdropHost(
        backdrop = localBackdrop,
        modifier = modifier
    ) {
        AppLiquidIconButton(
            backdrop = localBackdrop,
            icon = icon,
            contentDescription = contentDescription,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = buttonModifier,
            width = width,
            height = height,
            shape = shape,
            blurRadius = blurRadius,
            variant = variant,
            iconTint = iconTint,
            containerColor = containerColor,
            enabled = enabled
        )
    }
}

@Composable
fun AppStandaloneLiquidIconButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonModifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    width: Dp = Dp.Unspecified,
    height: Dp = Dp.Unspecified,
    shape: Shape = ContinuousCapsule,
    blurRadius: Dp? = null,
    variant: GlassVariant = GlassVariant.Content,
    iconTint: Color = Color.Unspecified,
    iconModifier: Modifier = Modifier,
    containerColor: Color? = null,
    enabled: Boolean = true
) {
    val localBackdrop = rememberLayerBackdrop()
    AppStandaloneBackdropHost(
        backdrop = localBackdrop,
        modifier = modifier
    ) {
        AppLiquidIconButton(
            backdrop = localBackdrop,
            painter = painter,
            contentDescription = contentDescription,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = buttonModifier,
            width = width,
            height = height,
            shape = shape,
            blurRadius = blurRadius,
            variant = variant,
            iconTint = iconTint,
            iconModifier = iconModifier,
            containerColor = containerColor,
            enabled = enabled
        )
    }
}

@Composable
internal fun AppStandaloneBackdropHost(
    backdrop: LayerBackdrop,
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (LocalLiquidControlsEnabled.current) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .layerBackdrop(backdrop)
            )
        }
        content()
    }
}
