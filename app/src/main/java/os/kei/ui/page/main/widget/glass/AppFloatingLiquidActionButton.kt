package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppFloatingLiquidActionButton(
    backdrop: Backdrop?,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = AppChromeTokens.floatingBottomBarOuterHeight,
    iconSize: Dp = 27.dp,
    iconTint: Color = MiuixTheme.colorScheme.primary,
    enabled: Boolean = true
) {
    AppLiquidFloatingSurface(
        modifier = modifier
            .then(if (enabled) Modifier else Modifier.alpha(AppInteractiveTokens.disabledContentAlpha))
            .size(size),
        shape = CircleShape,
        backdrop = backdrop,
        onClick = if (enabled) onClick else null,
        consumeTouches = !enabled,
        pressDurationMillis = 120,
        pressLabel = "app_floating_liquid_action_press"
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = iconTint
        )
    }
}
