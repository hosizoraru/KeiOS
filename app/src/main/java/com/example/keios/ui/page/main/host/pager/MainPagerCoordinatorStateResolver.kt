package com.example.keios.ui.page.main.host.pager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.keios.ui.page.main.model.BottomPage

internal data class MainPagerBackgroundState(
    val hasNonHomeBackground: Boolean,
    val effectiveNonHomeBackgroundUri: String
)

@Composable
internal fun rememberMainPagerBackgroundState(
    nonHomeBackgroundEnabled: Boolean,
    nonHomeBackgroundUri: String
): MainPagerBackgroundState {
    val effectiveNonHomeBackgroundUri = remember(nonHomeBackgroundEnabled, nonHomeBackgroundUri) {
        if (nonHomeBackgroundEnabled) nonHomeBackgroundUri.trim() else ""
    }
    val hasNonHomeBackground = remember(effectiveNonHomeBackgroundUri) {
        effectiveNonHomeBackgroundUri.isNotBlank()
    }
    return remember(hasNonHomeBackground, effectiveNonHomeBackgroundUri) {
        MainPagerBackgroundState(
            hasNonHomeBackground = hasNonHomeBackground,
            effectiveNonHomeBackgroundUri = effectiveNonHomeBackgroundUri
        )
    }
}

internal data class MainPagerTabsState(
    val tabs: List<BottomPage>,
    val initialPageIndex: Int,
    val visibleTabsSnapshot: Set<BottomPage>
)

@Composable
internal fun rememberMainPagerTabsState(
    visibleBottomPageNames: Set<String>,
    requestedBottomPage: String?,
    requestedBottomPageToken: Int
): MainPagerTabsState {
    val tabs = remember(visibleBottomPageNames) {
        BottomPage.entries.filter { page ->
            page == BottomPage.Home || visibleBottomPageNames.contains(page.name)
        }
    }
    val initialPageIndex = remember(tabs, requestedBottomPage, requestedBottomPageToken) {
        val target = requestedBottomPage?.trim().orEmpty()
        tabs.indexOfFirst { it.name == target }
            .takeIf { it >= 0 }
            ?: 0
    }
    val visibleTabsSnapshot = remember(tabs) { tabs.toSet() }
    return remember(tabs, initialPageIndex, visibleTabsSnapshot) {
        MainPagerTabsState(
            tabs = tabs,
            initialPageIndex = initialPageIndex,
            visibleTabsSnapshot = visibleTabsSnapshot
        )
    }
}

internal fun buildMainPagerVisibilityChangeAction(
    visibleBottomPageNames: Set<String>,
    onVisibleBottomPageNamesChange: (Set<String>) -> Unit
): (BottomPage, Boolean) -> Unit {
    return { page, visible ->
        if (page != BottomPage.Home) {
            val updated = visibleBottomPageNames
                .toMutableSet()
                .apply {
                    if (visible) add(page.name) else remove(page.name)
                }
                .toSet()
            onVisibleBottomPageNamesChange(updated)
        }
    }
}
