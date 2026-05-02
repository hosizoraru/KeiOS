package os.kei.ui.page.main.host.main

import os.kei.core.system.ShizukuApiUtils
import os.kei.mcp.server.McpServerManager

internal data class MainScreenPagerCoordinator(
    val settingsReturnToken: Int,
    val liquidBottomBarEnabled: Boolean,
    val bottomBarScrollEffectReductionEnabled: Boolean,
    val liquidActionBarLayeredStyleEnabled: Boolean,
    val cardPressFeedbackEnabled: Boolean,
    val homeIconHdrEnabled: Boolean,
    val homeDynamicFullEffectEnabled: Boolean,
    val preloadingEnabled: Boolean,
    val nonHomeBackgroundEnabled: Boolean,
    val nonHomeBackgroundUri: String,
    val nonHomeBackgroundOpacity: Float,
    val visibleBottomPageNames: Set<String>,
    val onVisibleBottomPageNamesChange: (Set<String>) -> Unit,
    val shizukuStatus: String,
    val shizukuApiUtils: ShizukuApiUtils,
    val mcpServerManager: McpServerManager,
    val onOpenGuideDetail: (String) -> Unit,
    val requestedBottomPage: String?,
    val requestedBottomPageToken: Int,
    val requestedGitHubRefreshToken: Int,
    val onRequestedBottomPageConsumed: () -> Unit
)

internal fun buildMainScreenPagerCoordinator(
    settingsReturnToken: Int,
    prefsState: MainScreenUiPrefsState,
    shizukuStatus: String,
    shizukuApiUtils: ShizukuApiUtils,
    mcpServerManager: McpServerManager,
    onOpenGuideDetail: (String) -> Unit,
    requestedBottomPage: String?,
    requestedBottomPageToken: Int,
    requestedGitHubRefreshToken: Int,
    onRequestedBottomPageConsumed: () -> Unit
): MainScreenPagerCoordinator {
    return MainScreenPagerCoordinator(
        settingsReturnToken = settingsReturnToken,
        liquidBottomBarEnabled = prefsState.liquidBottomBarEnabled,
        bottomBarScrollEffectReductionEnabled = prefsState.bottomBarScrollEffectReductionEnabled,
        liquidActionBarLayeredStyleEnabled = prefsState.liquidActionBarLayeredStyleEnabled,
        cardPressFeedbackEnabled = prefsState.cardPressFeedbackEnabled,
        homeIconHdrEnabled = prefsState.homeIconHdrEnabled,
        homeDynamicFullEffectEnabled = prefsState.homeDynamicFullEffectEnabled,
        preloadingEnabled = prefsState.preloadingEnabled,
        nonHomeBackgroundEnabled = prefsState.nonHomeBackgroundEnabled,
        nonHomeBackgroundUri = prefsState.nonHomeBackgroundUri,
        nonHomeBackgroundOpacity = prefsState.nonHomeBackgroundOpacity,
        visibleBottomPageNames = prefsState.visibleBottomPageNames,
        onVisibleBottomPageNamesChange = prefsState::updateVisibleBottomPageNames,
        shizukuStatus = shizukuStatus,
        shizukuApiUtils = shizukuApiUtils,
        mcpServerManager = mcpServerManager,
        onOpenGuideDetail = onOpenGuideDetail,
        requestedBottomPage = requestedBottomPage,
        requestedBottomPageToken = requestedBottomPageToken,
        requestedGitHubRefreshToken = requestedGitHubRefreshToken,
        onRequestedBottomPageConsumed = onRequestedBottomPageConsumed
    )
}
