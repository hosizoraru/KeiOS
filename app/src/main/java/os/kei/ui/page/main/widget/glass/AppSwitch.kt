package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch

val LocalLiquidSwitchEnabled = staticCompositionLocalOf { true }

private val AppLiquidSwitchLightBlue = Color(0xFF3B82F6)
private val AppLiquidSwitchDarkBlue = Color(0xFF7AB8FF)

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val switchModifier = modifier
        .size(width = 64.dp, height = 40.dp)

    if (!LocalLiquidSwitchEnabled.current) {
        Box(
            modifier = switchModifier,
            contentAlignment = Alignment.Center
        ) {
            MiuixSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
        return
    }

    val switchBackdrop = rememberLayerBackdrop()
    Box(
        modifier = switchModifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(switchBackdrop)
        )
        LiquidToggle(
            selected = { checked },
            onSelect = onCheckedChange,
            backdrop = switchBackdrop,
            enabled = enabled,
            modifier = Modifier.fillMaxSize(),
            checkedColor = if (androidx.compose.foundation.isSystemInDarkTheme()) {
                AppLiquidSwitchDarkBlue
            } else {
                AppLiquidSwitchLightBlue
            }
        )
    }
}
