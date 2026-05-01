package os.kei.ui.page.main.student.section.gallery

import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import os.kei.R
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LiquidCircularProgressBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh

@Composable
internal fun GuideFullscreenImageLoadingIndicator(
    modifier: Modifier = Modifier
) {
    LiquidCircularProgressBar(
        progress = { 0.28f },
        size = 24.dp,
        strokeWidth = 2.dp,
        activeColor = Color(0xFF60A5FA),
        inactiveColor = Color(0x3360A5FA),
        modifier = modifier
    )
}

@Composable
internal fun GuideFullscreenImageRetryHint(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.guide_gallery_image_load_error),
            color = Color(0xFFBFDBFE)
        )
        GlassTextButton(
            backdrop = null,
            text = stringResource(R.string.guide_action_retry),
            leadingIcon = MiuixIcons.Regular.Refresh,
            textColor = Color(0xFF60A5FA),
            variant = GlassVariant.Compact,
            onClick = onRetry
        )
    }
}

@Composable
internal fun GuideVideoFullscreenPlayerLayer(
    activePlayer: Player,
    videoRatio: Float,
    forceLandscape: Boolean
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val safeRatio = videoRatio.coerceAtLeast(0.2f)
        val shouldRotateLandscape = forceLandscape && maxHeight > maxWidth

        fun fitSize(targetRatio: Float): Pair<Dp, Dp> {
            val normalizedRatio = targetRatio.coerceAtLeast(0.2f)
            val viewportRatio = if (maxHeight.value > 0f) maxWidth.value / maxHeight.value else 1f
            return if (viewportRatio >= normalizedRatio) {
                val h = maxHeight
                (h * normalizedRatio) to h
            } else {
                val w = maxWidth
                w to (w / normalizedRatio)
            }
        }

        val playerModifier = if (shouldRotateLandscape) {
            val rotatedFinal = fitSize((1f / safeRatio).coerceAtLeast(0.2f))
            val preRotate = rotatedFinal.second to rotatedFinal.first
            Modifier
                .width(preRotate.first)
                .height(preRotate.second)
                .rotate(90f)
                .align(Alignment.Center)
        } else {
            Modifier.fillMaxSize()
        }

        AndroidView(
            modifier = playerModifier,
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    this.player = activePlayer
                }
            },
            update = { view ->
                view.player = activePlayer
                view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        )
    }
}
