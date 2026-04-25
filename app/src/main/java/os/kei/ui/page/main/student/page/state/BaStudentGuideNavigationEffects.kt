package os.kei.ui.page.main.student.page.state

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

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
