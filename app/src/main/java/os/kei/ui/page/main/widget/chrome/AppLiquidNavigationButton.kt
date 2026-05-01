package os.kei.ui.page.main.widget.chrome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.kyant.backdrop.Backdrop

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
    val items = remember(icon, contentDescription, onClick) {
        listOf(
            LiquidActionItem(
                icon = icon,
                contentDescription = contentDescription,
                onClick = onClick
            )
        )
    }
    LiquidActionBar(
        modifier = modifier,
        backdrop = backdrop,
        items = items,
        compactSingleItem = true,
        layeredStyleEnabled = layeredStyleEnabled,
        onInteractionChanged = onInteractionChanged
    )
}
