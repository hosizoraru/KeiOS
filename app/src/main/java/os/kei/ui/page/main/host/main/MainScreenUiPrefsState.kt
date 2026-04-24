package os.kei.ui.page.main.host.main

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import os.kei.core.log.AppLogger
import os.kei.core.prefs.UiPrefs
import os.kei.core.prefs.UiPrefsSnapshot
import os.kei.mcp.notification.McpNotificationHelper
import os.kei.mcp.server.McpServerManager

@Stable
internal class MainScreenUiPrefsState(
    snapshot: UiPrefsSnapshot,
    private val appContext: Context,
    private val mcpServerManager: McpServerManager
) {
    var liquidBottomBarEnabled by mutableStateOf(snapshot.liquidBottomBarEnabled)
    var liquidActionBarLayeredStyleEnabled by mutableStateOf(snapshot.liquidActionBarLayeredStyleEnabled)
    var liquidGlassSwitchEnabled by mutableStateOf(snapshot.liquidGlassSwitchEnabled)
    var transitionAnimationsEnabled by mutableStateOf(snapshot.transitionAnimationsEnabled)
    var cardPressFeedbackEnabled by mutableStateOf(snapshot.cardPressFeedbackEnabled)
    var homeIconHdrEnabled by mutableStateOf(snapshot.homeIconHdrEnabled)
    var preloadingEnabled by mutableStateOf(snapshot.preloadingEnabled)
    var nonHomeBackgroundEnabled by mutableStateOf(snapshot.nonHomeBackgroundEnabled)
    var nonHomeBackgroundUri by mutableStateOf(snapshot.nonHomeBackgroundUri)
    var nonHomeBackgroundOpacity by mutableStateOf(snapshot.nonHomeBackgroundOpacity)
    var superIslandNotificationEnabled by mutableStateOf(snapshot.superIslandNotificationEnabled)
    var superIslandBypassRestrictionEnabled by mutableStateOf(snapshot.superIslandBypassRestrictionEnabled)
    var superIslandRestoreDelayMs by mutableStateOf(snapshot.superIslandRestoreDelayMs)
    var logDebugEnabled by mutableStateOf(snapshot.logDebugEnabled)
    var textCopyCapabilityExpanded by mutableStateOf(snapshot.textCopyCapabilityExpanded)
    var cacheDiagnosticsEnabled by mutableStateOf(snapshot.cacheDiagnosticsEnabled)
    var visibleBottomPageNames by mutableStateOf(snapshot.visibleBottomPageNames)

    fun updateLiquidBottomBarEnabled(value: Boolean) {
        liquidBottomBarEnabled = value
        UiPrefs.setLiquidBottomBarEnabled(value)
    }

    fun updateLiquidActionBarLayeredStyleEnabled(value: Boolean) {
        liquidActionBarLayeredStyleEnabled = value
        UiPrefs.setLiquidActionBarLayeredStyleEnabled(value)
    }

    fun updateLiquidGlassSwitchEnabled(value: Boolean) {
        liquidGlassSwitchEnabled = value
        UiPrefs.setLiquidGlassSwitchEnabled(value)
    }

    fun updateTransitionAnimationsEnabled(value: Boolean) {
        transitionAnimationsEnabled = value
        UiPrefs.setTransitionAnimationsEnabled(value)
    }

    fun updateCardPressFeedbackEnabled(value: Boolean) {
        cardPressFeedbackEnabled = value
        UiPrefs.setCardPressFeedbackEnabled(value)
    }

    fun updateHomeIconHdrEnabled(value: Boolean) {
        homeIconHdrEnabled = value
        UiPrefs.setHomeIconHdrEnabled(value)
    }

    fun updatePreloadingEnabled(value: Boolean) {
        preloadingEnabled = value
        UiPrefs.setPreloadingEnabled(value)
    }

    fun updateNonHomeBackgroundEnabled(value: Boolean) {
        nonHomeBackgroundEnabled = value
        UiPrefs.setNonHomeBackgroundEnabled(value)
    }

    fun updateNonHomeBackgroundUri(value: String) {
        nonHomeBackgroundUri = value
        UiPrefs.setNonHomeBackgroundUri(value)
    }

    fun updateNonHomeBackgroundOpacity(value: Float) {
        nonHomeBackgroundOpacity = value
        UiPrefs.setNonHomeBackgroundOpacity(value)
    }

    fun updateSuperIslandNotificationEnabled(value: Boolean) {
        superIslandNotificationEnabled = value
        UiPrefs.setSuperIslandNotificationEnabled(value)
        mcpServerManager.refreshNotificationNow()
        McpNotificationHelper.refreshCurrentNotificationStyle(appContext)
    }

    fun updateSuperIslandBypassRestrictionEnabled(value: Boolean) {
        superIslandBypassRestrictionEnabled = value
        UiPrefs.setSuperIslandBypassRestrictionEnabled(value)
        mcpServerManager.refreshNotificationNow()
        McpNotificationHelper.refreshCurrentNotificationStyle(appContext)
    }

    fun updateSuperIslandRestoreDelayMs(value: Int) {
        superIslandRestoreDelayMs = value
        UiPrefs.setSuperIslandRestoreDelayMs(value)
        mcpServerManager.refreshNotificationNow()
        McpNotificationHelper.refreshCurrentNotificationStyle(appContext)
    }

    fun updateLogDebugEnabled(value: Boolean) {
        logDebugEnabled = value
        UiPrefs.setLogDebugEnabled(value)
        AppLogger.setDebugEnabled(value)
    }

    fun updateTextCopyCapabilityExpanded(value: Boolean) {
        textCopyCapabilityExpanded = value
        UiPrefs.setTextCopyCapabilityExpanded(value)
    }

    fun updateCacheDiagnosticsEnabled(value: Boolean) {
        cacheDiagnosticsEnabled = value
        UiPrefs.setCacheDiagnosticsEnabled(value)
    }

    fun updateVisibleBottomPageNames(value: Set<String>) {
        visibleBottomPageNames = value
        UiPrefs.saveVisibleBottomPageNames(value)
    }
}

@Composable
internal fun rememberMainScreenUiPrefsState(
    snapshot: UiPrefsSnapshot,
    appContext: Context,
    mcpServerManager: McpServerManager
): MainScreenUiPrefsState {
    return remember(snapshot, appContext, mcpServerManager) {
        MainScreenUiPrefsState(
            snapshot = snapshot,
            appContext = appContext,
            mcpServerManager = mcpServerManager
        )
    }
}
