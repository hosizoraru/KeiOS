package com.example.keios.ui.page.main

import androidx.compose.foundation.pager.PagerState
import kotlin.math.abs

internal suspend fun PagerState.animateTabSwitch(
    fromIndex: Int,
    targetIndex: Int,
    animationsEnabled: Boolean = true,
    onFarJumpBefore: suspend () -> Unit = {},
    onFarJumpAfter: suspend () -> Unit = {}
) {
    val total = pageCount
    if (total <= 0) return

    val from = fromIndex.coerceIn(0, total - 1)
    val target = targetIndex.coerceIn(0, total - 1)
    if (target == from && !isScrollInProgress) return

    if (!animationsEnabled) {
        onFarJumpBefore()
        if (currentPage != target || isScrollInProgress) {
            scrollToPage(target)
        }
        onFarJumpAfter()
        return
    }

    val distance = abs(target - from)
    if (distance <= 1) {
        animateScrollToPage(target)
        return
    }
    onFarJumpBefore()
    if (currentPage != target || isScrollInProgress) {
        scrollToPage(target)
    }
    onFarJumpAfter()
}
