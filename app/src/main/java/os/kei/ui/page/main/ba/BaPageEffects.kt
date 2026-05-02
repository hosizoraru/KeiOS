package os.kei.ui.page.main.ba

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import os.kei.core.ui.snapshot.rememberAppSnapshotFlowManager
import os.kei.ui.page.main.ba.support.BA_AP_REGEN_TICK_MS
import os.kei.ui.page.main.widget.chrome.expandTopAppBarToPageTop
import os.kei.ui.page.main.widget.chrome.isPageSettledAtTop
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import top.yukonga.miuix.kmp.basic.ScrollBehavior

@Composable
internal fun BaPageCommonEffects(
    listState: LazyListState,
    scrollBehavior: ScrollBehavior,
    scrollToTopSignal: Int,
    isPageActive: Boolean,
    consumedScrollToTopSignal: Int,
    onConsumedScrollToTopSignalChange: (Int) -> Unit,
    onDisposeActionBarInteraction: () -> Unit,
    office: BaOfficeController,
    onUiNowMsChange: (Long) -> Unit,
    serverIndex: Int,
    onServerChanged: suspend () -> Unit,
    context: Context,
) {
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    val snapshotFlowManager = rememberAppSnapshotFlowManager()

    DisposableEffect(Unit) {
        onDispose { onDisposeActionBarInteraction() }
    }

    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > consumedScrollToTopSignal) {
            onConsumedScrollToTopSignalChange(scrollToTopSignal)
            listState.animateScrollToItem(0)
            expandTopAppBarToPageTop(
                scrollBehavior = scrollBehavior,
                animationsEnabled = transitionAnimationsEnabled
            )
        } else {
            onConsumedScrollToTopSignalChange(scrollToTopSignal)
        }
    }

    LaunchedEffect(listState, scrollBehavior, transitionAnimationsEnabled, snapshotFlowManager) {
        snapshotFlowManager.snapshotFlow {
            isPageSettledAtTop(
                firstVisibleItemIndex = listState.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
                listScrollInProgress = listState.isScrollInProgress
            )
        }
            .distinctUntilChanged()
            .collectLatest { settledAtTop ->
                if (settledAtTop) {
                    expandTopAppBarToPageTop(
                        scrollBehavior = scrollBehavior,
                        animationsEnabled = transitionAnimationsEnabled
                    )
                }
            }
    }

    LaunchedEffect(isPageActive) {
        office.ensureRegenBase()
        office.ensureCafeHourBase()
        office.clampCafeStoredToCap()
        office.applyCafeStorage()
        office.applyApRegen()
        while (true) {
            if (isPageActive) {
                delay(BA_AP_REGEN_TICK_MS)
                office.applyCafeStorage()
                office.applyApRegen()
            } else {
                // Keep background overhead low on offscreen pager pages.
                delay(5_000L)
            }
        }
    }

    LaunchedEffect(isPageActive) {
        while (true) {
            val tick = if (isPageActive) 1_000L else 3_000L
            delay(tick)
            onUiNowMsChange(System.currentTimeMillis())
        }
    }

    LaunchedEffect(office.apCurrent) {
        val target = office.displayApInputText()
        if (office.apCurrentInput != target) office.apCurrentInput = target
    }

    LaunchedEffect(office.apLimit) {
        val target = office.apLimit.toString()
        if (office.apLimitInput != target) office.apLimitInput = target
    }

    LaunchedEffect(office.idNickname) {
        if (office.idNicknameInput != office.idNickname) office.idNicknameInput = office.idNickname
    }

    LaunchedEffect(office.idFriendCode) {
        if (office.idFriendCodeInput != office.idFriendCode) office.idFriendCodeInput = office.idFriendCode
    }

    LaunchedEffect(serverIndex) {
        onServerChanged()
    }

    LaunchedEffect(office.apCurrent, office.apNotifyEnabled, office.apNotifyThreshold) {
        office.tryApThresholdNotification(context)
    }
}
