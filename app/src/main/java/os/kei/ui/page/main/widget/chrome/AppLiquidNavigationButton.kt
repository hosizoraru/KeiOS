package os.kei.ui.page.main.widget.chrome

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.ui.page.main.widget.glass.GlassIconButton
import os.kei.ui.page.main.widget.glass.GlassVariant

@Composable
fun AppLiquidNavigationButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    layeredStyleEnabled: Boolean = true,
    onInteractionChanged: (Boolean) -> Unit = {}
) {
    GlassIconButton(
        modifier = modifier,
        backdrop = backdrop,
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        width = 52.dp,
        height = 52.dp,
        variant = GlassVariant.Bar
    )
}
