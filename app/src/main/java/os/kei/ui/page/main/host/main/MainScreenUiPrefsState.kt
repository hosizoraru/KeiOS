package os.kei.ui.page.main.host.main

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import os.kei.R
import os.kei.core.ext.showToast
import os.kei.core.icon.LauncherIconController
import os.kei.core.prefs.LauncherIconDesign
import os.kei.core.log.AppLogLevel
import os.kei.core.log.AppLogger
import os.kei.core.prefs.NonHomeBackgroundAlignment
import os.kei.core.prefs.NonHomeBackgroundContentScale
import os.kei.core.prefs.NonHomeBackgroundPageStyle
import os.kei.core.prefs.SuperIslandFloatBehavior
import os.kei.core.prefs.SuperIslandAutoClose
import os.kei.core.prefs.UiPrefsSnapshot
import os.kei.mcp.notification.McpNotificationHelper
import os.kei.mcp.server.McpServerManager
import os.kei.core.privilege.PrivilegeMode
import os.kei.core.privilege.PrivilegeModeRuntime

@Stable
internal class MainScreenUiPrefsState(
    private val snapshot: UiPrefsSnapshot,
    private val appContext: Context,
    private val mcpServerManager: McpServerManager,
    private val viewModel: MainScreenPrefsViewModel,
) {
    val liquidSwitchEnabled: Boolean get() = snapshot.liquidSwitchEnabled
    val liquidToastEnabled: Boolean get() = snapshot.liquidToastEnabled
    val reduceToastInterruptionEnabled: Boolean get() = snapshot.reduceToastInterruptionEnabled
    val transitionAnimationsEnabled: Boolean get() = snapshot.transitionAnimationsEnabled
    val predictiveBackAnimationsEnabled: Boolean get() = snapshot.predictiveBackAnimationsEnabled
    val searchAutoFocusEnabled: Boolean get() = snapshot.searchAutoFocusEnabled
    val gripAwareFloatingDockEnabled: Boolean get() = snapshot.gripAwareFloatingDockEnabled
    val homeIconHdrEnabled: Boolean get() = snapshot.homeIconHdrEnabled
    val homeDynamicFullEffectEnabled: Boolean get() = snapshot.homeDynamicFullEffectEnabled
    val preloadingEnabled: Boolean get() = snapshot.preloadingEnabled
    val launcherIconDesign: LauncherIconDesign get() = snapshot.launcherIconDesign
    val privilegeMode: PrivilegeMode get() = PrivilegeMode.fromStorageId(snapshot.privilegeModeId)
    val nonHomeBackgroundEnabled: Boolean get() = snapshot.nonHomeBackgroundEnabled
    val nonHomeBackgroundUri: String get() = snapshot.nonHomeBackgroundUri
    val nonHomeBackgroundOpacity: Float get() = snapshot.nonHomeBackgroundOpacity
    val nonHomeBackgroundContentScale: NonHomeBackgroundContentScale get() = snapshot.nonHomeBackgroundContentScale
    val nonHomeBackgroundAlignment: NonHomeBackgroundAlignment get() = snapshot.nonHomeBackgroundAlignment
    val nonHomeBackgroundPageStyle: NonHomeBackgroundPageStyle get() = snapshot.nonHomeBackgroundPageStyle
    val nonHomeBackgroundScrim: Float get() = snapshot.nonHomeBackgroundScrim
    val nonHomeBackgroundDepthEnabled: Boolean get() = snapshot.nonHomeBackgroundDepthEnabled
    val nonHomeBackgroundSaturation: Float get() = snapshot.nonHomeBackgroundSaturation
    val superIslandNotificationEnabled: Boolean get() = snapshot.superIslandNotificationEnabled
    val superIslandFloatBehavior: SuperIslandFloatBehavior get() = snapshot.superIslandFloatBehavior
    val superIslandAutoClose: SuperIslandAutoClose get() = snapshot.superIslandAutoClose
    val superIslandFirstFloatEnabled: Boolean get() = snapshot.superIslandFirstFloatEnabled
    val superIslandBypassRestrictionEnabled: Boolean get() = snapshot.superIslandBypassRestrictionEnabled
    val superIslandRestoreDelayMs: Int get() = snapshot.superIslandRestoreDelayMs
    val logLevel: AppLogLevel get() = snapshot.logLevel
    val textCopyCapabilityExpanded: Boolean get() = snapshot.textCopyCapabilityExpanded
    val cacheDiagnosticsEnabled: Boolean get() = snapshot.cacheDiagnosticsEnabled
    val visibleBottomPageNames: Set<String> get() = snapshot.visibleBottomPageNames

    fun updateLiquidSwitchEnabled(value: Boolean) {
        viewModel.updateLiquidSwitchEnabled(value)
    }

    fun updateLiquidToastEnabled(value: Boolean) {
        viewModel.updateLiquidToastEnabled(value)
    }

    fun updateReduceToastInterruptionEnabled(value: Boolean) {
        viewModel.updateReduceToastInterruptionEnabled(value)
    }

    fun updateTransitionAnimationsEnabled(value: Boolean) {
        viewModel.updateTransitionAnimationsEnabled(value)
    }

    fun updatePredictiveBackAnimationsEnabled(value: Boolean) {
        viewModel.updatePredictiveBackAnimationsEnabled(value)
    }

    fun updateSearchAutoFocusEnabled(value: Boolean) {
        viewModel.updateSearchAutoFocusEnabled(value)
    }

    fun updateGripAwareFloatingDockEnabled(value: Boolean) {
        viewModel.updateGripAwareFloatingDockEnabled(value)
    }

    fun updateHomeIconHdrEnabled(value: Boolean) {
        viewModel.updateHomeIconHdrEnabled(value)
    }

    fun updateHomeDynamicFullEffectEnabled(value: Boolean) {
        viewModel.updateHomeDynamicFullEffectEnabled(value)
    }

    fun updatePreloadingEnabled(value: Boolean) {
        viewModel.updatePreloadingEnabled(value)
    }

    /**
     * Applies the switch to the process-wide runtime first so background privileged entry points
     * follow the new backend even before the preference write lands.
     */
    fun updatePrivilegeMode(value: PrivilegeMode) {
        if (value == privilegeMode) return
        PrivilegeModeRuntime.set(value)
        viewModel.updatePrivilegeMode(value)
    }

    fun updateLauncherIconDesign(value: LauncherIconDesign) {
        val result = LauncherIconController.applyDesign(appContext, value)
        if (result.isFailure) {
            appContext.showToast(R.string.settings_launcher_icon_switch_failed)
            return
        }
        // A rapid second switch can arrive before the UI snapshot reflects the first one.
        viewModel.updateLauncherIconDesign(value)
        if (value != snapshot.launcherIconDesign || result.getOrDefault(false)) {
            McpNotificationHelper.refreshCurrentNotificationStyle(appContext)
        }
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

    fun updateNonHomeBackgroundContentScale(value: NonHomeBackgroundContentScale) {
        viewModel.updateNonHomeBackgroundContentScale(value)
    }

    fun updateNonHomeBackgroundAlignment(value: NonHomeBackgroundAlignment) {
        viewModel.updateNonHomeBackgroundAlignment(value)
    }

    fun updateNonHomeBackgroundPageStyle(value: NonHomeBackgroundPageStyle) {
        viewModel.updateNonHomeBackgroundPageStyle(value)
    }

    fun updateNonHomeBackgroundScrim(value: Float) {
        viewModel.updateNonHomeBackgroundScrim(value)
    }

    fun updateNonHomeBackgroundDepthEnabled(value: Boolean) {
        viewModel.updateNonHomeBackgroundDepthEnabled(value)
    }

    fun updateNonHomeBackgroundSaturation(value: Float) {
        viewModel.updateNonHomeBackgroundSaturation(value)
    }

    fun resetNonHomeBackgroundRendering() {
        viewModel.resetNonHomeBackgroundRendering()
    }

    fun applyNonHomeBackgroundReadableSuggestion(isDarkTheme: Boolean) {
        viewModel.applyNonHomeBackgroundReadableSuggestion(isDarkTheme)
    }

    fun updateSuperIslandNotificationEnabled(value: Boolean) {
        viewModel.updateSuperIslandNotificationEnabled(value)
        mcpServerManager.refreshNotificationNow()
        McpNotificationHelper.refreshCurrentNotificationStyle(appContext)
    }

    fun updateSuperIslandFloatBehavior(value: SuperIslandFloatBehavior) {
        viewModel.updateSuperIslandFloatBehavior(value)
        mcpServerManager.refreshNotificationNow()
        McpNotificationHelper.refreshCurrentNotificationStyle(appContext)
    }

    fun updateSuperIslandAutoClose(value: SuperIslandAutoClose) {
        viewModel.updateSuperIslandAutoClose(value)
        mcpServerManager.refreshNotificationNow()
        McpNotificationHelper.refreshCurrentNotificationStyle(appContext)
    }

    fun updateSuperIslandFirstFloatEnabled(value: Boolean) {
        viewModel.updateSuperIslandFirstFloatEnabled(value)
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

    fun updateLogLevel(value: AppLogLevel) {
        viewModel.updateLogLevel(value)
        AppLogger.setLogLevel(value)
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
    viewModel: MainScreenPrefsViewModel,
): MainScreenUiPrefsState =
    remember(snapshot, appContext, mcpServerManager, viewModel) {
        MainScreenUiPrefsState(
            snapshot = snapshot,
            appContext = appContext,
            mcpServerManager = mcpServerManager,
            viewModel = viewModel,
        )
    }
