package com.example.keios.ui.page.main.host.pager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.keios.ui.page.main.model.BottomPage

internal data class MainPagerScrollSignalControllerState(
    val osScrollToTopSignal: Int,
    val baScrollToTopSignal: Int,
    val mcpScrollToTopSignal: Int,
    val githubScrollToTopSignal: Int,
    val onPageSelected: (Int) -> Unit
)

@Composable
internal fun rememberMainPagerScrollSignalController(
    tabs: List<BottomPage>,
    pagerRuntime: MainPagerRuntimeSnapshot,
    onPageSelected: (Int) -> Unit
): MainPagerScrollSignalControllerState {
    var osScrollToTopSignal by remember { mutableIntStateOf(0) }
    var baScrollToTopSignal by remember { mutableIntStateOf(0) }
    var mcpScrollToTopSignal by remember { mutableIntStateOf(0) }
    var githubScrollToTopSignal by remember { mutableIntStateOf(0) }

    val onPageSelectedWithScrollSignal = remember(tabs, pagerRuntime, onPageSelected) {
        { index: Int ->
            onPageSelected(index)
            val shouldScrollToTop = index in tabs.indices &&
                !pagerRuntime.isPagerScrollInProgress &&
                index == pagerRuntime.stablePageIndex
            if (shouldScrollToTop) {
                when (tabs[index]) {
                    BottomPage.Os -> osScrollToTopSignal += 1
                    BottomPage.Ba -> baScrollToTopSignal += 1
                    BottomPage.Mcp -> mcpScrollToTopSignal += 1
                    BottomPage.GitHub -> githubScrollToTopSignal += 1
                    BottomPage.Home -> Unit
                }
            }
        }
    }

    return remember(
        osScrollToTopSignal,
        baScrollToTopSignal,
        mcpScrollToTopSignal,
        githubScrollToTopSignal,
        onPageSelectedWithScrollSignal
    ) {
        MainPagerScrollSignalControllerState(
            osScrollToTopSignal = osScrollToTopSignal,
            baScrollToTopSignal = baScrollToTopSignal,
            mcpScrollToTopSignal = mcpScrollToTopSignal,
            githubScrollToTopSignal = githubScrollToTopSignal,
            onPageSelected = onPageSelectedWithScrollSignal
        )
    }
}
