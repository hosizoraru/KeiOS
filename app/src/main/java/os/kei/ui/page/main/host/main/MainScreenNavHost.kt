package os.kei.ui.page.main.host.main

import android.content.pm.PackageInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplayTransitionEffects
import androidx.navigation3.ui.defaultPredictivePopTransitionSpec
import os.kei.core.platform.PredictiveBackOemCompat
import os.kei.core.prefs.AppThemeMode
import os.kei.mcp.server.McpServerManager
import os.kei.ui.navigation.KeiosRoute
import os.kei.ui.navigation.Navigator
import os.kei.ui.page.main.mcp.skill.page.McpSkillPage
import os.kei.ui.page.main.about.page.AboutPage
import os.kei.ui.page.main.host.pager.MainPagerLayout
import os.kei.ui.page.main.settings.page.SettingsPage
import os.kei.ui.page.main.student.catalog.page.BaGuideCatalogPage
import os.kei.ui.page.main.student.page.BaStudentGuidePage
import os.kei.ui.page.main.widget.glass.LocalLiquidControlsEnabled
import os.kei.ui.page.main.widget.motion.LocalPredictiveBackAnimationsEnabled
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled

@Composable
internal fun MainScreenNavHost(
    backStack: MutableList<NavKey>,
    navigator: Navigator,
    pagerCoordinator: MainScreenPagerCoordinator,
    prefsState: MainScreenUiPrefsState,
    appLabel: String,
    packageInfo: PackageInfo?,
    onCheckOrRequestShizuku: () -> Unit,
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    mcpServerManager: McpServerManager,
    appThemeMode: AppThemeMode,
    onAppThemeModeChanged: (AppThemeMode) -> Unit
) {
    val entryProvider = entryProvider<NavKey> {
        entry<KeiosRoute.Main> {
            MainPagerLayout(
                navigator = navigator,
                settingsReturnToken = pagerCoordinator.settingsReturnToken,
                liquidBottomBarEnabled = pagerCoordinator.liquidBottomBarEnabled,
                bottomBarScrollEffectReductionEnabled = pagerCoordinator.bottomBarScrollEffectReductionEnabled,
                liquidActionBarLayeredStyleEnabled = pagerCoordinator.liquidActionBarLayeredStyleEnabled,
                cardPressFeedbackEnabled = pagerCoordinator.cardPressFeedbackEnabled,
                homeIconHdrEnabled = pagerCoordinator.homeIconHdrEnabled,
                preloadingEnabled = pagerCoordinator.preloadingEnabled,
                nonHomeBackgroundEnabled = pagerCoordinator.nonHomeBackgroundEnabled,
                nonHomeBackgroundUri = pagerCoordinator.nonHomeBackgroundUri,
                nonHomeBackgroundOpacity = pagerCoordinator.nonHomeBackgroundOpacity,
                visibleBottomPageNames = pagerCoordinator.visibleBottomPageNames,
                onVisibleBottomPageNamesChange = pagerCoordinator.onVisibleBottomPageNamesChange,
                shizukuStatus = pagerCoordinator.shizukuStatus,
                shizukuApiUtils = pagerCoordinator.shizukuApiUtils,
                mcpServerManager = pagerCoordinator.mcpServerManager,
                onOpenGuideDetail = pagerCoordinator.onOpenGuideDetail,
                requestedBottomPage = pagerCoordinator.requestedBottomPage,
                requestedBottomPageToken = pagerCoordinator.requestedBottomPageToken,
                requestedGitHubRefreshToken = pagerCoordinator.requestedGitHubRefreshToken,
                onRequestedBottomPageConsumed = pagerCoordinator.onRequestedBottomPageConsumed
            )
        }
        entry<KeiosRoute.Settings> {
            SettingsPage(
                notificationPermissionGranted = notificationPermissionGranted,
                onRequestNotificationPermission = onRequestNotificationPermission,
                liquidBottomBarEnabled = prefsState.liquidBottomBarEnabled,
                onLiquidBottomBarChanged = prefsState::updateLiquidBottomBarEnabled,
                bottomBarScrollEffectReductionEnabled = prefsState.bottomBarScrollEffectReductionEnabled,
                onBottomBarScrollEffectReductionChanged = prefsState::updateBottomBarScrollEffectReductionEnabled,
                liquidActionBarLayeredStyleEnabled = prefsState.liquidActionBarLayeredStyleEnabled,
                onLiquidActionBarLayeredStyleChanged = prefsState::updateLiquidActionBarLayeredStyleEnabled,
                liquidSwitchEnabled = prefsState.liquidSwitchEnabled,
                onLiquidSwitchChanged = prefsState::updateLiquidSwitchEnabled,
                transitionAnimationsEnabled = prefsState.transitionAnimationsEnabled,
                onTransitionAnimationsChanged = prefsState::updateTransitionAnimationsEnabled,
                predictiveBackAnimationsEnabled = prefsState.predictiveBackAnimationsEnabled,
                onPredictiveBackAnimationsChanged = prefsState::updatePredictiveBackAnimationsEnabled,
                cardPressFeedbackEnabled = prefsState.cardPressFeedbackEnabled,
                onCardPressFeedbackChanged = prefsState::updateCardPressFeedbackEnabled,
                homeIconHdrEnabled = prefsState.homeIconHdrEnabled,
                onHomeIconHdrChanged = prefsState::updateHomeIconHdrEnabled,
                preloadingEnabled = prefsState.preloadingEnabled,
                onPreloadingEnabledChanged = prefsState::updatePreloadingEnabled,
                nonHomeBackgroundEnabled = prefsState.nonHomeBackgroundEnabled,
                onNonHomeBackgroundEnabledChanged = prefsState::updateNonHomeBackgroundEnabled,
                nonHomeBackgroundUri = prefsState.nonHomeBackgroundUri,
                onNonHomeBackgroundUriChanged = prefsState::updateNonHomeBackgroundUri,
                nonHomeBackgroundOpacity = prefsState.nonHomeBackgroundOpacity,
                onNonHomeBackgroundOpacityChanged = prefsState::updateNonHomeBackgroundOpacity,
                superIslandNotificationEnabled = prefsState.superIslandNotificationEnabled,
                onSuperIslandNotificationChanged = prefsState::updateSuperIslandNotificationEnabled,
                superIslandBypassRestrictionEnabled = prefsState.superIslandBypassRestrictionEnabled,
                onSuperIslandBypassRestrictionChanged = prefsState::updateSuperIslandBypassRestrictionEnabled,
                superIslandRestoreDelayMs = prefsState.superIslandRestoreDelayMs,
                onSuperIslandRestoreDelayMsChanged = prefsState::updateSuperIslandRestoreDelayMs,
                logDebugEnabled = prefsState.logDebugEnabled,
                onLogDebugChanged = prefsState::updateLogDebugEnabled,
                textCopyCapabilityExpanded = prefsState.textCopyCapabilityExpanded,
                onTextCopyCapabilityExpandedChanged = prefsState::updateTextCopyCapabilityExpanded,
                cacheDiagnosticsEnabled = prefsState.cacheDiagnosticsEnabled,
                onCacheDiagnosticsChanged = prefsState::updateCacheDiagnosticsEnabled,
                shizukuStatus = pagerCoordinator.shizukuStatus,
                onCheckOrRequestShizuku = onCheckOrRequestShizuku,
                shizukuApiUtils = pagerCoordinator.shizukuApiUtils,
                appThemeMode = appThemeMode,
                onAppThemeModeChanged = onAppThemeModeChanged,
                onBack = { navigator.pop() }
            )
        }
        entry<KeiosRoute.McpSkill> {
            McpSkillPage(
                mcpServerManager = mcpServerManager,
                onBack = { navigator.pop() }
            )
        }
        entry<KeiosRoute.About> {
            AboutPage(
                appLabel = appLabel,
                packageInfo = packageInfo,
                notificationPermissionGranted = notificationPermissionGranted,
                shizukuStatus = pagerCoordinator.shizukuStatus,
                shizukuApiUtils = pagerCoordinator.shizukuApiUtils,
                onCheckShizuku = onCheckOrRequestShizuku,
                onBack = { navigator.pop() }
            )
        }
        entry<KeiosRoute.BaStudentGuide> {
            BaStudentGuidePage(
                liquidBottomBarEnabled = prefsState.liquidBottomBarEnabled,
                bottomBarScrollEffectReductionEnabled = prefsState.bottomBarScrollEffectReductionEnabled,
                liquidActionBarLayeredStyleEnabled = prefsState.liquidActionBarLayeredStyleEnabled,
                preloadingEnabled = prefsState.preloadingEnabled,
                onBack = { navigator.pop() }
            )
        }
        entry<KeiosRoute.BaGuideCatalog> {
            BaGuideCatalogPage(
                liquidBottomBarEnabled = prefsState.liquidBottomBarEnabled,
                bottomBarScrollEffectReductionEnabled = prefsState.bottomBarScrollEffectReductionEnabled,
                liquidActionBarLayeredStyleEnabled = prefsState.liquidActionBarLayeredStyleEnabled,
                preloadingEnabled = prefsState.preloadingEnabled,
                onBack = { navigator.pop() },
                onOpenGuide = pagerCoordinator.onOpenGuideDetail
            )
        }
    }
    val predictiveBackPolicy = PredictiveBackOemCompat.currentPolicy(
        transitionAnimationsEnabled = prefsState.transitionAnimationsEnabled,
        predictiveBackAnimationsEnabled = prefsState.predictiveBackAnimationsEnabled
    )
    val predictiveBackPreviewEnabled = predictiveBackPolicy.frameworkAnimationsEnabled
    val predictivePopTransitionSpec =
        if (predictiveBackPreviewEnabled) {
            defaultPredictivePopTransitionSpec<NavKey>()
        } else {
            disabledPredictiveBackTransitionSpec<NavKey>()
        }
    val transitionEffects = if (predictiveBackPolicy.popDirectionFollowsSwipeEdge) {
        NavDisplayTransitionEffects.Default.copy(popDirectionFollowsSwipeEdge = true)
    } else {
        NavDisplayTransitionEffects.Default
    }
    val entries = rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        entryProvider = entryProvider,
    )
    CompositionLocalProvider(
        LocalTransitionAnimationsEnabled provides prefsState.transitionAnimationsEnabled,
        LocalPredictiveBackAnimationsEnabled provides predictiveBackPreviewEnabled,
        LocalLiquidControlsEnabled provides prefsState.liquidSwitchEnabled
    ) {
        NavDisplay(
            entries = entries,
            onBack = { navigator.pop() },
            predictivePopTransitionSpec = predictivePopTransitionSpec,
            transitionEffects = transitionEffects,
            modifier = Modifier.fillMaxSize()
        )
        BackHandler(enabled = !predictiveBackPreviewEnabled && backStack.size > 1) {
            navigator.pop()
        }
    }
}

private fun <T : Any> disabledPredictiveBackTransitionSpec():
    AnimatedContentTransitionScope<Scene<T>>.(Int) -> ContentTransform = {
        ContentTransform(
            targetContentEnter = EnterTransition.None,
            initialContentExit = ExitTransition.None
        )
    }
