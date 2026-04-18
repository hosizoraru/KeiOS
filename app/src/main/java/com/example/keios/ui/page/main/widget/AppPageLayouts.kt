package com.example.keios.ui.page.main.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.layout.RowScope

fun appPageContentPadding(
    innerPadding: PaddingValues,
    bottomExtra: Dp = AppChromeTokens.pageBottomInsetExtra,
    topExtra: Dp = 0.dp
): PaddingValues {
    return PaddingValues(
        top = innerPadding.calculateTopPadding() + topExtra,
        bottom = innerPadding.calculateBottomPadding() + bottomExtra,
        start = AppChromeTokens.pageHorizontalPadding,
        end = AppChromeTokens.pageHorizontalPadding
    )
}

fun appPageBottomPaddingWithFloatingOverlay(contentBottomPadding: Dp): Dp {
    return contentBottomPadding + AppChromeTokens.pageFloatingOverlayBottomExtra
}

@Composable
fun AppPageScaffold(
    title: String,
    modifier: Modifier = Modifier,
    largeTitle: String = title,
    scrollBehavior: ScrollBehavior? = null,
    topBarColor: Color = Color.Transparent,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    searchBarVisible: Boolean = false,
    searchBarAnimationLabelPrefix: String = "appPageSearch",
    searchBarContent: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBarSection(
                title = title,
                largeTitle = largeTitle,
                scrollBehavior = scrollBehavior,
                color = topBarColor,
                navigationIcon = navigationIcon,
                actions = actions,
                searchBarVisible = searchBarVisible,
                searchBarAnimationLabelPrefix = searchBarAnimationLabelPrefix,
                searchBarContent = searchBarContent
            )
        },
        content = content
    )
}

@Composable
fun AppPageLazyColumn(
    innerPadding: PaddingValues,
    state: LazyListState,
    modifier: Modifier = Modifier,
    bottomExtra: Dp = AppChromeTokens.pageBottomInsetExtra,
    topExtra: Dp = AppChromeTokens.pageSectionGap,
    sectionSpacing: Dp = AppChromeTokens.pageSectionGapLarge,
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = appPageContentPadding(
            innerPadding = innerPadding,
            bottomExtra = bottomExtra,
            topExtra = topExtra
        ),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing),
        content = content
    )
}
