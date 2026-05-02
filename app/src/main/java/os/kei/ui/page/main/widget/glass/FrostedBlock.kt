package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedRectangle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun FrostedBlock(
    backdrop: Backdrop?,
    title: String,
    subtitle: String,
    body: String = "",
    accent: Color,
    content: (@Composable () -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val cardSurface = if (isDark) {
        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.84f)
    } else {
        Color.White.copy(alpha = 0.66f)
    }
    val localBackdrop = rememberLayerBackdrop()
    val blockBackdrop = backdrop ?: localBackdrop
    val cornerRadius = 16.dp

    Box(modifier = Modifier.fillMaxWidth()) {
        if (backdrop == null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .layerBackdrop(localBackdrop)
            )
        }
        LiquidSurface(
            backdrop = blockBackdrop,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedRectangle(cornerRadius),
            isInteractive = false,
            surfaceColor = cardSurface,
            blurRadius = resolvedGlassBlurDp(UiPerformanceBudget.backdropBlur, GlassVariant.Content),
            lensRadius = resolvedGlassLensDp(UiPerformanceBudget.backdropLens, GlassVariant.Content)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(cornerRadius))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .then(
                                Modifier
                                    .padding(horizontal = 0.dp, vertical = 0.dp)
                            )
                    ) {
                        LiquidSurface(
                            backdrop = blockBackdrop,
                            modifier = Modifier,
                            shape = RoundedRectangle(999.dp),
                            isInteractive = false,
                            surfaceColor = accent.copy(alpha = 0.18f),
                            blurRadius = 3.dp,
                            lensRadius = 12.dp,
                            shadow = false
                        ) {
                            Text(
                                text = title,
                                color = accent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (content != null) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        content()
                    }
                } else if (body.isNotBlank()) {
                    Text(
                        text = body,
                        color = MiuixTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
