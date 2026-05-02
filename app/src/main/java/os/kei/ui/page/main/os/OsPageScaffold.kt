package os.kei.ui.page.main.os

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import os.kei.ui.page.main.widget.chrome.AppPageScaffold
import os.kei.ui.page.main.widget.chrome.LiquidActionBar
import os.kei.ui.page.main.widget.chrome.LiquidActionItem
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.ScrollBehavior

@Composable
internal fun OsPageScaffoldShell(
    scrollBehavior: ScrollBehavior,
    topBarColor: Color,
    topBarBackdrop: LayerBackdrop,
    layeredStyleEnabled: Boolean,
    reduceEffectsDuringPagerScroll: Boolean,
    manageCardsContentDescription: String,
    manageActivitiesContentDescription: String,
    manageShellCardsContentDescription: String,
    refreshParamsContentDescription: String,
    refreshing: Boolean,
    onOpenCardManager: () -> Unit,
    onOpenActivityVisibilityManager: () -> Unit,
    onOpenShellCardVisibilityManager: () -> Unit,
    onRefresh: () -> Unit,
    onActionBarInteractingChanged: (Boolean) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val manageCardsIcon = appLucideLayersIcon()
    val manageActivitiesIcon = appLucideAppWindowIcon()
    val manageShellCardsIcon = osLucideShellIcon()
    val refreshIcon = appLucideRefreshIcon()
    val actionItems = remember(
        manageCardsContentDescription,
        manageActivitiesContentDescription,
        manageShellCardsContentDescription,
        refreshParamsContentDescription,
        refreshing,
        onOpenCardManager,
        onOpenActivityVisibilityManager,
        onOpenShellCardVisibilityManager,
        onRefresh
    ) {
        listOf(
            LiquidActionItem(
                icon = manageCardsIcon,
                contentDescription = manageCardsContentDescription,
                onClick = onOpenCardManager
            ),
            LiquidActionItem(
                icon = manageActivitiesIcon,
                contentDescription = manageActivitiesContentDescription,
                onClick = onOpenActivityVisibilityManager
            ),
            LiquidActionItem(
                icon = manageShellCardsIcon,
                contentDescription = manageShellCardsContentDescription,
                onClick = onOpenShellCardVisibilityManager
            ),
            LiquidActionItem(
                icon = refreshIcon,
                contentDescription = refreshParamsContentDescription,
                onClick = {
                    if (refreshing) return@LiquidActionItem
                    onRefresh()
                }
            )
        )
    }

    AppPageScaffold(
        title = "",
        modifier = Modifier.fillMaxSize(),
        largeTitle = "OS",
        scrollBehavior = scrollBehavior,
        topBarColor = topBarColor,
        actions = {
            LiquidActionBar(
                backdrop = topBarBackdrop,
                layeredStyleEnabled = layeredStyleEnabled,
                reduceEffectsDuringPagerScroll = reduceEffectsDuringPagerScroll,
                items = actionItems,
                onInteractionChanged = onActionBarInteractingChanged
            )
        },
        content = content
    )
}
