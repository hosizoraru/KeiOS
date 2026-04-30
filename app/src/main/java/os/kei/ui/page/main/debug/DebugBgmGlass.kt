package os.kei.ui.page.main.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugBgmGlassCapsule(
    accent: Color,
    modifier: Modifier = Modifier,
    shape: Shape = ContinuousCapsule,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    content: @Composable () -> Unit
) {
    DebugBgmGlassSurface(
        modifier = modifier,
        accent = accent,
        shape = shape
    ) {
        Box(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
internal fun DebugBgmGlassIcon(
    icon: ImageVector,
    contentDescription: String,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 24.dp,
    selected: Boolean = false,
    onClick: () -> Unit = {}
) {
    DebugBgmGlassSurface(
        modifier = modifier
            .size(size)
            .clickable(onClick = onClick),
        accent = accent,
        shape = CircleShape,
        selected = selected
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) accent else MiuixTheme.colorScheme.onBackground,
            modifier = Modifier
                .align(Alignment.Center)
                .size(iconSize)
        )
    }
}

@Composable
internal fun DebugBgmInlineIcon(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    size: Dp = 36.dp,
    iconSize: Dp = 22.dp,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
internal fun DebugBgmGlassSurface(
    modifier: Modifier = Modifier,
    accent: Color,
    shape: Shape,
    selected: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = if (selected) 0.98f else 0.96f),
                        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = if (selected) 0.94f else 0.92f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = if (selected) accent.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.24f),
                shape = shape
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}
