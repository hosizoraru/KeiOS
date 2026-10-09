package os.kei.ui.page.main.student.model3d

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import os.kei.ui.page.main.widget.motion.AppMotionTokens
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.motion.resolvedMotionDuration

internal fun guideModel3dUsesSideTools(width: Dp, height: Dp) = width >= 840.dp && height >= 480.dp && width > height

/** The media stays in the same composition slot during panel, rotation and immersion changes. */
@Composable
internal fun GuideModel3dAdaptiveLayout(
    toolsVisible: Boolean, controlsVisible: Boolean, modifier: Modifier = Modifier,
    tools: @Composable () -> Unit, content: @Composable (Boolean, State<Dp>) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val sideTools = guideModel3dUsesSideTools(maxWidth, maxHeight)
        val duration = resolvedMotionDuration(AppMotionTokens.floatingFadeInMs, LocalTransitionAnimationsEnabled.current)
        val motion = tween<Float>(duration)
        val panelWidth = (maxWidth * 0.30f).coerceIn(304.dp, 360.dp)
        val visible = sideTools && toolsVisible && controlsVisible
        val chromeInset = animateDpAsState(if (visible) panelWidth else 0.dp, tween(duration))
        // Only chrome and the floating panel move. Resizing WebGL on every transition frame
        // distorts the last presented image and repeatedly reallocates its drawing buffer.
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().testTag("guide_model3d_media")) { content(sideTools, chromeInset) }
            AnimatedVisibility(visible = visible, modifier = Modifier.align(Alignment.CenterEnd),
                enter = slideInHorizontally(tween(duration)) { it } + fadeIn(motion),
                exit = slideOutHorizontally(tween(duration)) { it } + fadeOut(motion)) {
                Box(Modifier.width(panelWidth).fillMaxHeight().safeDrawingPadding()
                    .padding(start = 8.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
                    .testTag("guide_model3d_side_tools")) { tools() }
            }
        }
    }
}
