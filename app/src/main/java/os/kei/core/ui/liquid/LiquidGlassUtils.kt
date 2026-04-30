package os.kei.core.ui.liquid
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRenderEffectSupported
import top.yukonga.miuix.kmp.blur.textureBlur

@Composable
fun rememberBottomBarBlurColors(): BlurColors {
    val isDark = isSystemInDarkTheme()
    return remember(isDark) {
        BlurColors(
            blendColors = if (isDark) {
                LiquidGlassBlendTokens.BottomBarDark
            } else {
                LiquidGlassBlendTokens.BottomBarLight
            }
        )
    }
}

@Composable
fun rememberCardBlurColors(): BlurColors {
    val isDark = isSystemInDarkTheme()
    return remember(isDark) {
        BlurColors(
            blendColors = if (isDark) {
                LiquidGlassBlendTokens.CardDark
            } else {
                LiquidGlassBlendTokens.CardLight
            }
        )
    }
}

fun isLiquidGlassSupported(): Boolean {
    return isRenderEffectSupported()
}

fun Modifier.liquidGlassCompatBlend(
    backdrop: LayerBackdrop?,
    blurColors: BlurColors,
    cornerRadiusDp: Int,
    blurRadius: Dp = BlurDefaults.BlurRadius.dp,
    contentBlendMode: BlendMode = BlendMode.SrcOver,
): Modifier {
    val shape = RoundedCornerShape(cornerRadiusDp.dp)
    val blurEnabled = backdrop != null && isLiquidGlassSupported()
    val blurPart = if (blurEnabled) {
        this.textureBlur(
            backdrop = backdrop,
            shape = shape,
            blurRadius = blurRadius.value,
            noiseCoefficient = BlurDefaults.NoiseCoefficient,
            colors = blurColors,
            contentBlendMode = contentBlendMode,
            enabled = true
        )
    } else {
        this
    }
    return blurPart
        .background(Color.White.copy(alpha = if (blurEnabled) 0.16f else 0.10f), shape)
        .border(1.dp, Color.White.copy(alpha = if (blurEnabled) 0.20f else 0.12f), shape)
}
