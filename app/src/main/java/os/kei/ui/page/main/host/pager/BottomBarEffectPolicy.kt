package os.kei.ui.page.main.host.pager

internal fun shouldReduceBottomBarEffectsDuringMotion(
    scrollEffectReductionEnabled: Boolean,
    pagerScrollInProgress: Boolean,
    activePageListScrollInProgress: Boolean
): Boolean {
    return scrollEffectReductionEnabled &&
        (pagerScrollInProgress || activePageListScrollInProgress)
}
