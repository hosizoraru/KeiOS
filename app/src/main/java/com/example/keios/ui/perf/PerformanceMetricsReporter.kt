package com.example.keios.ui.perf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.metrics.performance.PerformanceMetricsState

@Composable
fun ReportPagerPerformanceState(
    scope: String,
    currentPage: String,
    targetPage: String,
    scrolling: Boolean
) {
    val view = LocalView.current
    val holder = remember(view) { PerformanceMetricsState.getHolderForHierarchy(view) }
    val metricsState = holder.state
    val currentKey = "$scope.current"
    val targetKey = "$scope.target"
    val scrollingKey = "$scope.scrolling"

    LaunchedEffect(metricsState, currentPage, targetPage, scrolling) {
        val state = metricsState ?: return@LaunchedEffect
        state.putState(currentKey, currentPage)
        state.putState(targetKey, targetPage)
        state.putState(scrollingKey, if (scrolling) "1" else "0")
    }

    DisposableEffect(metricsState, currentKey, targetKey, scrollingKey) {
        onDispose {
            metricsState?.removeState(currentKey)
            metricsState?.removeState(targetKey)
            metricsState?.removeState(scrollingKey)
        }
    }
}
