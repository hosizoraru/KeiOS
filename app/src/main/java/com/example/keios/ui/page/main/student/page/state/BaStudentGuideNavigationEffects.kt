package com.example.keios.ui.page.main.student.page.state

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.keios.ui.page.main.student.BaStudentGuideStore

@Composable
internal fun BindBaStudentGuideSourceRestoreEffect(
    sourceUrl: String,
    onSourceUrlChange: (String) -> Unit,
    onErrorChange: (String?) -> Unit
) {
    LaunchedEffect(Unit) {
        val latestStored = BaStudentGuideStore.loadCurrentUrl()
        if (latestStored.isNotBlank() && latestStored != sourceUrl) {
            onSourceUrlChange(latestStored)
            onErrorChange(null)
        }
    }
}

@Composable
internal fun BindBaStudentGuidePagerSyncEffects(
    sourceUrl: String,
    bottomTabsSize: Int,
    selectedBottomTabIndex: Int,
    pagerState: PagerState,
    onSelectedBottomTabIndexChange: (Int) -> Unit
) {
    LaunchedEffect(sourceUrl, bottomTabsSize) {
        val targetIndex = selectedBottomTabIndex.coerceIn(0, (bottomTabsSize - 1).coerceAtLeast(0))
        if (pagerState.currentPage != targetIndex) {
            pagerState.scrollToPage(targetIndex)
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        if (selectedBottomTabIndex != pagerState.settledPage) {
            onSelectedBottomTabIndexChange(pagerState.settledPage)
        }
    }
}
