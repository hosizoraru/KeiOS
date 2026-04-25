package os.kei.ui.page.main.widget.chrome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BoxScope.AppTopEndActionBarOverlay(
    modifier: Modifier = Modifier,
    topSpacing: Dp = 8.dp,
    endSpacing: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    val safeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues()
    Box(
        modifier = modifier
            .align(Alignment.TopEnd)
            .padding(
                top = safeDrawingPadding.calculateTopPadding() + topSpacing,
                end = safeDrawingPadding.calculateEndPadding(layoutDirection) + endSpacing
            )
    ) {
        content()
    }
}
