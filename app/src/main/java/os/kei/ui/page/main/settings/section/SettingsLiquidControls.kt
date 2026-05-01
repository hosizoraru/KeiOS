package os.kei.ui.page.main.settings.section

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import os.kei.ui.page.main.widget.glass.LiquidKeyPointSlider
import os.kei.ui.page.main.widget.glass.LiquidSliderKeyPoint
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SettingsLiquidKeyPointSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    keyPoints: List<Float>,
    magnetThreshold: Float,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    activeColor: Color = MiuixTheme.colorScheme.primary,
    onInteractionChanged: (Boolean) -> Unit = {}
) {
    val sliderBackdrop = rememberLayerBackdrop()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .semantics {
                contentDescription?.let { this.contentDescription = it }
            }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .layerBackdrop(sliderBackdrop)
        )
        LiquidKeyPointSlider(
            value = { value.coerceIn(valueRange) },
            onValueChange = { next -> onValueChange(next.coerceIn(valueRange)) },
            valueRange = valueRange,
            visibilityThreshold = 0.001f,
            backdrop = sliderBackdrop,
            keyPoints = keyPoints.map { point -> LiquidSliderKeyPoint(point) },
            enabled = enabled,
            snapToKeyPoints = true,
            snapThreshold = magnetThreshold,
            activeColor = activeColor,
            inactiveColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.34f),
            onInteractionChanged = onInteractionChanged,
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 4.dp)
        )
    }
}
