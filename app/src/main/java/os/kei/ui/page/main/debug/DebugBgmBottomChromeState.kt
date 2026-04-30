package os.kei.ui.page.main.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Stable
internal class DebugBgmBottomChromeScrollState(
    private val scrollThresholdPx: Float
) : NestedScrollConnection {
    var isCompact by mutableStateOf(false)
        private set

    private var accumulatedScroll = 0f

    fun expand() {
        isCompact = false
        accumulatedScroll = 0f
    }

    fun compact() {
        isCompact = true
        accumulatedScroll = 0f
    }

    override fun onPreScroll(
        available: androidx.compose.ui.geometry.Offset,
        source: NestedScrollSource
    ): androidx.compose.ui.geometry.Offset {
        val scrollDelta = available.y
        if ((accumulatedScroll > 0f && scrollDelta < 0f) || (accumulatedScroll < 0f && scrollDelta > 0f)) {
            accumulatedScroll = 0f
        }

        accumulatedScroll += scrollDelta
        if (accumulatedScroll <= -scrollThresholdPx && !isCompact) {
            compact()
        } else if (accumulatedScroll >= scrollThresholdPx && isCompact) {
            expand()
        }
        return androidx.compose.ui.geometry.Offset.Zero
    }
}

@Composable
internal fun rememberDebugBgmBottomChromeScrollState(
    scrollThreshold: Dp = 56.dp
): DebugBgmBottomChromeScrollState = with(LocalDensity.current) {
    val thresholdPx = scrollThreshold.toPx()
    remember(thresholdPx) { DebugBgmBottomChromeScrollState(thresholdPx) }
}
