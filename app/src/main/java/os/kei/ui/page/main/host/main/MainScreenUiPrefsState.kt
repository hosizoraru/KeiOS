package os.kei.ui.page.main.host.main

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import os.kei.core.log.AppLogger
import os.kei.core.prefs.UiPrefsSnapshot
import os.kei.mcp.notification.McpNotificationHelper
import os.kei.mcp.server.McpServerManager

@Stable
internal class MainScreenUiPrefsState(
    private val snapshot: UiPrefsSnapshot,
    private val appContext: Context,
    private val mcpServerManager: McpServerManager,
    private val viewModel: MainScreenPrefsViewModel
) {
    val liquidBottomBarEnabled: Boolean get() = snapshot.liquidBottomBarEnabled
    val bottomBarScrollEffectReductionEnabled: Boolean get() = snapshot.bottomBarScrollEffectReductionEnabled
    val liquidActionBarLayeredStyleEnabled: Boolean get() = snapshot.liquidActionBarLayeredStyleEnabled
    val liquidGlassSwitchEnabled: Boolean get() = snapshot.liquidGlassSwitchEnabled
    val transitionAnimationsEnabled: Boolean get() = snapshot.transitionAnimationsEnabled
    val predictiveBackAnimationsEnabled: Boolean get() = snapshot.predictiveBackAnimationsEnabled
    val cardPressFeedbackEnabled: Boolean get() = snapshot.cardPressFeedbackEnabled
    val homeIconHdrEnabled: Boolean get() = snapshot.homeIconHdrEnabled
    val preloadingEnabled: Boolean get() = snapshot.preloadingEnabled
    val nonHomeBackgroundEnabled: Boolean get() = snapshot.nonHomeBackgroundEnabled
    val nonHomeBackgroundUri: String get() = snapshot.nonHomeBackgroundUri
    val nonHomeBackgroundOpacity: Float get() = snapshot.nonHomeBackgroundOpacity
    val superIslandNotificationEnabled: Boolean get() = snapshot.superIslandNotificationEnabled
    val superIslandBypassRestrictionEnabled: Boolean get() = snapshot.superIslandBypassRestrictionEnabled
    val superIslandRestoreDelayMs: Int get() = snapshot.superIslandRestoreDelayMs
    val logDebugEnabled: Boolean get() = snapshot.logDebugEnabled
    val textCopyCapabilityExpanded: Boolean get() = snapshot.textCopyCapabilityExpanded
    val cacheDiagnosticsEnabled: Boolean get() = snapshot.cacheDiagnosticsEnabled
    val visibleBottomPageNames: Set<String> get() = snapshot.visibleBottomPageNames

    fun updateLiquidBottomBarEnabled(value: Boolean) {
        viewModel.updateLiquidBottomBarEnabled(value)
    }

    fun updateBottomBarScrollEffectReductionEnabled(value: Boolean) {
        viewModel.updateBottomBarScrollEffectReductionEnabled(value)
    }

    fun updateLiquidActionBarLayeredStyleEnabled(value: Boolean) {
        viewModel.updateLiquidActionBarLayeredStyleEnabled(value)
    }

    fun updateLiquidGlassSwitchEnabled(value: Boolean) {
        viewModel.updateLiquidGlassSwitchEnabled(value)
    }

    fun updateTransitionAnimationsEnabled(value: Boolean) {
        viewModel.updateTransitionAnimationsEnabled(value)
    }

    fun updatePredictiveBackAnimationsEnabled(value: Boolean) {
        viewModel.updatePredictiveBackAnimationsEnabled(value)
    }

    fun updateCardPressFeedbackEnabled(value: Boolean) {
        viewModel.updateCardPressFeedbackEnabled(value)
    }

    fun updateHomeIconHdrEnabled(value: Boolean) {
        viewModel.updateHomeIconHdrEnabled(value)
    }

    fun updatePreloadingEnabled(value: Boolean) {
        viewModel.updatePreloadingEnabled(value)
    }

    fun updateNonHomeBackgroundEnabled(value: Boolean) {
        viewModel.updateNonHomeBackgroundEnabled(value)
    }

    fun updateNonHomeBackgroundUri(value: String) {
        viewModel.updateNonHomeBackgroundUri(value)
    }

    fun updateNonHomeBackgroundOpacity(value: Float) {
        viewModel.updateNonHomeBackgroundOpacity(value)
    }

    fun updateSuperIslandNotificationEnabled(value: Boolean) {
        viewModel.updateSuperIslandNotificationEnabled(value)
        mcpServerManager.refreshNotificationNow()
        McpNotificationHelper.refreshCurrentNotificationStyle(appContext)
    }

    fun updateSuperIslandBypassRestrictionEnabled(value: Boolean) {
        viewModel.updateSuperIslandBypassRestrictionEnabled(value)
        mcpServerManager.refreshNotificationNow()
        McpNotificationHelper.refreshCurrentNotificationStyle(appContext)
    }

    fun updateSuperIslandRestoreDelayMs(value: Int) {
        viewModel.updateSuperIslandRestoreDelayMs(value)
        mcpServerManager.refreshNotificationNow()
        McpNotificationHelper.refreshCurrentNotificationStyle(appContext)
    }

    fun updateLogDebugEnabled(value: Boolean) {
        viewModel.updateLogDebugEnabled(value)
        AppLogger.setDebugEnabled(value)
    }

    fun updateTextCopyCapabilityExpanded(value: Boolean) {
        viewModel.updateTextCopyCapabilityExpanded(value)
    }

    fun updateCacheDiagnosticsEnabled(value: Boolean) {
        viewModel.updateCacheDiagnosticsEnabled(value)
    }

    fun updateVisibleBottomPageNames(value: Set<String>) {
        viewModel.updateVisibleBottomPageNames(value)
    }
}

@Composable
internal fun rememberMainScreenUiPrefsState(
    snapshot: UiPrefsSnapshot,
    appContext: Context,
    mcpServerManager: McpServerManager,
    viewModel: MainScreenPrefsViewModel
): MainScreenUiPrefsState {
    return remember(snapshot, appContext, mcpServerManager, viewModel) {
        MainScreenUiPrefsState(
            snapshot = snapshot,
            appContext = appContext,
            mcpServerManager = mcpServerManager,
            viewModel = viewModel
        )
    }
}
