@file:Suppress("FunctionName")

package os.kei.ui.page.main.host.main

import android.os.SystemClock
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import os.kei.core.platform.PredictiveBackOemCompat
import os.kei.core.prefs.AppThemeMode
import os.kei.mcp.server.McpServerManager
import os.kei.ui.navigation.KeiosRoute
import os.kei.ui.navigation.Navigator
import os.kei.ui.page.main.about.page.AboutPage
import os.kei.ui.page.main.back.BackNavigationRuntimeController
import os.kei.ui.page.main.back.LocalBackNavigationRuntimeController
import os.kei.ui.page.main.back.LocalBackNavigationRuntimeState
import os.kei.ui.page.main.ba.BaCalendarPoolPage
import os.kei.ui.page.main.ba.BaCalendarPoolTab
import os.kei.ui.page.main.ba.toInitialServerSelection
import os.kei.ui.page.main.github.history.GitHubActionsNotificationHistoryPage
import os.kei.ui.page.main.github.fdroid.FdroidVersionListPage
import os.kei.ui.page.main.github.release.GitHubReleaseListPage
import os.kei.ui.page.main.host.pager.MainPagerLayout
import os.kei.ui.page.main.mcp.skill.page.McpSkillPage
import os.kei.ui.page.main.os.shell.page.OsShellRunnerPage
import os.kei.ui.page.main.settings.page.SettingsPage
import os.kei.ui.page.main.student.catalog.page.BaGuideCatalogPage
import os.kei.ui.page.main.student.page.BaStudentGuidePage
import os.kei.ui.page.main.sync.WebDavSyncPage
import os.kei.ui.page.main.sync.rememberWebDavSyncDataPorts
import os.kei.ui.page.main.widget.chrome.AppManagedBackgroundHost
import os.kei.ui.page.main.widget.chrome.AppManagedBackgroundStyle
import os.kei.ui.page.main.widget.chrome.AppManagedBackgroundStyles
import os.kei.ui.page.main.widget.chrome.LocalSearchAutoFocusEnabled
import os.kei.ui.page.main.widget.glass.BindLiquidToastBridge
import os.kei.ui.page.main.widget.glass.LiquidToastHost
import os.kei.ui.page.main.widget.glass.LocalLiquidControlsEnabled
import os.kei.ui.page.main.widget.glass.LocalLiquidPresentationOwnerActive
import os.kei.ui.page.main.widget.glass.rememberLiquidToastState
import os.kei.ui.page.main.widget.isAppInDarkTheme
import os.kei.ui.page.main.widget.motion.LocalPredictiveBackAnimationsEnabled
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.support.LocalTextCopyExpandedOverride
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.transition.NavMotion
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.nav.transition.navGraphicsTransition
import kotlin.math.roundToInt

@Composable
internal fun MainScreenNavHost(
    backStack: NavBackStack,
    navigator: Navigator,
    pagerCoordinator: MainScreenPagerCoordinator,
    externalNavigationToken: Int,
    prefsState: MainScreenUiPrefsState,
    appLabel: String,
    onCheckOrRequestPrivilege: () -> Unit,
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    mcpServerManager: McpServerManager,
    appThemeMode: AppThemeMode,
    transientExternalLaunchActive: Boolean,
    onAppThemeModeChanged: (AppThemeMode) -> Unit,
    onOpenGitHubActionsTrackFromHistory: (String) -> Unit,
    onRetryGitHubRefreshTargetsFromHistory: (List<String>) -> Unit,
) {
    val backCoordinator =
        rememberMainScreenBackCoordinator(
            backStack = backStack,
            navigator = navigator,
            pagerCoordinator = pagerCoordinator,
        )
    val onRouteBack =
        remember(backCoordinator) {
            { backCoordinator.onRouteBack() }
        }
    val predictiveBackPolicy =
        PredictiveBackOemCompat.currentPolicy(
            transitionAnimationsEnabled = prefsState.transitionAnimationsEnabled,
            predictiveBackAnimationsEnabled = prefsState.predictiveBackAnimationsEnabled,
        )
    val backRuntimeController = remember { BackNavigationRuntimeController() }
    SideEffect {
        backRuntimeController.updatePolicy(predictiveBackPolicy)
    }
    // Initial intent routing is covered by the system splash; subsequent navigation keeps its
    // normal Miuix motion. This avoids revealing Home halfway through a cold shortcut push.
    val routeAnimationsEnabled = prefsState.transitionAnimationsEnabled &&
        (os.kei.LocalMainStartupTransition.current?.hasPresented != false)
    val isDarkTheme = isAppInDarkTheme()
    // miuix-nav keeps one visual contract for push/pop/predictive back (visual = f(depth)), so the
    // former transitionSpec/popTransitionSpec/predictivePopTransitionSpec trio collapses into a
    // single NavTransition: MiuixDefault's geometry with our own pacing when route animations are
    // on, None for an instant swap. See keiosNavTransition for why the pacing differs.
    val navTransition =
        remember(routeAnimationsEnabled) {
            if (routeAnimationsEnabled) keiosNavTransition() else NavTransitions.None
        }
    val catalogTransition =
        remember(routeAnimationsEnabled) {
            if (routeAnimationsEnabled) baGuideCatalogNavTransition() else null
        }
    val navEffects =
        remember(routeAnimationsEnabled, isDarkTheme) {
            if (routeAnimationsEnabled) {
                NavDisplayEffects(
                    enableCornerClip = true,
                    dimAmount = if (isDarkTheme) 0.54f else 0.34f,
                    blockInputDuringTransition = true,
                )
            } else {
                NavDisplayEffects.None
            }
        }
    // No route opts into navSwipeDismiss. The original reason (issue #21) is gone: through
    // 0.9.3 Miuix engaged that gesture on PointerEventPass.Initial, which dispatches parent-first,
    // so the display container claimed any horizontal drag past touch slop before a descendant saw
    // it and sliders, AppSwitch, the bottom-bar tab drag and text fields all lost their gesture.
    // 0.9.4 moved engagement to PointerEventPass.Final and arbitrates ownership from child
    // *consumption* (NavSwipeArbitrator) — the "honour a consumed drag" fix the gap doc asked for.
    //
    // It stays off anyway, on a different and unresolved observation: with dismissDirection =
    // LeftToRight confirmed live on this transition, no synthesised drag engaged the gesture at all
    // on the API 37 AVD — not from page content, not from the inert top-bar band, at 83% of screen
    // width, fast or slow. Enabling would ship a gesture that is either dead or untested here.
    // See docs/planning/miuix-nav-swipe-dismiss-gap.md before enabling it again.

    CompositionLocalProvider(
        LocalBackNavigationRuntimeController provides backRuntimeController,
        LocalBackNavigationRuntimeState provides backRuntimeController.state,
        LocalTransitionAnimationsEnabled provides prefsState.transitionAnimationsEnabled,
        LocalPredictiveBackAnimationsEnabled provides predictiveBackPolicy.localPredictiveBackEnabled,
        LocalSearchAutoFocusEnabled provides prefsState.searchAutoFocusEnabled,
        LocalLiquidControlsEnabled provides prefsState.liquidSwitchEnabled,
        LocalTextCopyExpandedOverride provides prefsState.textCopyCapabilityExpanded,
    ) {
        val liquidToastState = rememberLiquidToastState()
        BindLiquidToastBridge(
            state = liquidToastState,
            liquidToastEnabled = prefsState.liquidToastEnabled,
            reduceToastInterruptionEnabled = prefsState.reduceToastInterruptionEnabled,
        )
        // The toast used to own a second `layerBackdrop` producer wrapping NavDisplay, gated on
        // visibility to keep the offscreen layer off the idle path. It no longer needs one: it portals
        // into the overlay host's notification layer and samples `LocalSceneBackdrop`, which already
        // captures this very content — and captures it over an opaque base rect, which the private
        // producer never did.
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                NavDisplay(
                    backStack = backStack,
                    modifier = Modifier.fillMaxSize(),
                    onBack = onRouteBack,
                    transition = navTransition,
                    effects = navEffects,
                ) {
                    entry<KeiosRoute.Main> {
                        MainPagerLayout(
                            rootBackHandlersEnabled = backStack.lastOrNull() is KeiosRoute.Main,
                            navigator = navigator,
                            settingsReturnToken = pagerCoordinator.settingsReturnToken,
                            gripAwareFloatingDockEnabled = pagerCoordinator.gripAwareFloatingDockEnabled,
                            homeIconHdrEnabled = pagerCoordinator.homeIconHdrEnabled,
                            homeDynamicFullEffectEnabled = pagerCoordinator.homeDynamicFullEffectEnabled,
                            preloadingEnabled = pagerCoordinator.preloadingEnabled,
                            nonHomeBackgroundEnabled = pagerCoordinator.nonHomeBackgroundEnabled,
                            nonHomeBackgroundUri = pagerCoordinator.nonHomeBackgroundUri,
                            nonHomeBackgroundOpacity = pagerCoordinator.nonHomeBackgroundOpacity,
                            nonHomeBackgroundContentScale = pagerCoordinator.nonHomeBackgroundContentScale,
                            nonHomeBackgroundAlignment = pagerCoordinator.nonHomeBackgroundAlignment,
                            nonHomeBackgroundPageStyle = pagerCoordinator.nonHomeBackgroundPageStyle,
                            nonHomeBackgroundScrim = pagerCoordinator.nonHomeBackgroundScrim,
                            nonHomeBackgroundDepthEnabled = pagerCoordinator.nonHomeBackgroundDepthEnabled,
                            nonHomeBackgroundSaturation = pagerCoordinator.nonHomeBackgroundSaturation,
                            visibleBottomPageNames = pagerCoordinator.visibleBottomPageNames,
                            onVisibleBottomPageNamesChange = pagerCoordinator.onVisibleBottomPageNamesChange,
                            privilegeStatus = pagerCoordinator.privilegeStatus,
                            privilegedShell = pagerCoordinator.privilegedShell,
                            mcpServerManager = pagerCoordinator.mcpServerManager,
                            onOpenGuideDetail = pagerCoordinator.onOpenGuideDetail,
                            onOpenBaGuideCatalog = pagerCoordinator.onBaGuideCatalogOpen,
                            routeAtTop = rememberNavEntryAtTop(),
                            onOpenBaCalendarPool = pagerCoordinator.onOpenBaCalendarPool,
                            requestedBottomPage = pagerCoordinator.requestedBottomPage,
                            requestedBottomPageToken = pagerCoordinator.requestedBottomPageToken,
                            externalNavigationToken = externalNavigationToken,
                            requestedGitHubRefreshToken = pagerCoordinator.requestedGitHubRefreshToken,
                            requestedGitHubActionsTrackId = pagerCoordinator.requestedGitHubActionsTrackId,
                            requestedGitHubActionsSheetToken = pagerCoordinator.requestedGitHubActionsSheetToken,
                            requestedBaAccountId = pagerCoordinator.requestedBaAccountId,
                            requestedBaAccountToken = pagerCoordinator.requestedBaAccountToken,
                            transientExternalLaunchActive = transientExternalLaunchActive,
                            onRequestedBottomPageConsumed = pagerCoordinator.onRequestedBottomPageConsumed,
                        )
                    }
                    entry<KeiosRoute.Settings> {
                        MainScreenRouteBackgroundHost(
                            prefsState = prefsState,
                            exportBackdropToContent = true,
                        ) {
                            SettingsPage(
                                notificationPermissionGranted = notificationPermissionGranted,
                                onRequestNotificationPermission = onRequestNotificationPermission,
                                liquidSwitchEnabled = prefsState.liquidSwitchEnabled,
                                onLiquidSwitchChanged = prefsState::updateLiquidSwitchEnabled,
                                liquidToastEnabled = prefsState.liquidToastEnabled,
                                onLiquidToastChanged = prefsState::updateLiquidToastEnabled,
                                reduceToastInterruptionEnabled = prefsState.reduceToastInterruptionEnabled,
                                onReduceToastInterruptionChanged = prefsState::updateReduceToastInterruptionEnabled,
                                transitionAnimationsEnabled = prefsState.transitionAnimationsEnabled,
                                onTransitionAnimationsChanged = prefsState::updateTransitionAnimationsEnabled,
                                predictiveBackAnimationsEnabled = prefsState.predictiveBackAnimationsEnabled,
                                onPredictiveBackAnimationsChanged = prefsState::updatePredictiveBackAnimationsEnabled,
                                searchAutoFocusEnabled = prefsState.searchAutoFocusEnabled,
                                onSearchAutoFocusChanged = prefsState::updateSearchAutoFocusEnabled,
                                gripAwareFloatingDockEnabled = prefsState.gripAwareFloatingDockEnabled,
                                onGripAwareFloatingDockChanged = prefsState::updateGripAwareFloatingDockEnabled,
                                homeIconHdrEnabled = prefsState.homeIconHdrEnabled,
                                onHomeIconHdrChanged = prefsState::updateHomeIconHdrEnabled,
                                homeDynamicFullEffectEnabled = prefsState.homeDynamicFullEffectEnabled,
                                onHomeDynamicFullEffectChanged = prefsState::updateHomeDynamicFullEffectEnabled,
                                preloadingEnabled = prefsState.preloadingEnabled,
                                onPreloadingEnabledChanged = prefsState::updatePreloadingEnabled,
                                launcherIconDesign = prefsState.launcherIconDesign,
                                onLauncherIconDesignChanged = prefsState::updateLauncherIconDesign,
                                nonHomeBackgroundEnabled = prefsState.nonHomeBackgroundEnabled,
                                onNonHomeBackgroundEnabledChanged = prefsState::updateNonHomeBackgroundEnabled,
                                nonHomeBackgroundUri = prefsState.nonHomeBackgroundUri,
                                onNonHomeBackgroundUriChanged = prefsState::updateNonHomeBackgroundUri,
                                nonHomeBackgroundOpacity = prefsState.nonHomeBackgroundOpacity,
                                onNonHomeBackgroundOpacityChanged = prefsState::updateNonHomeBackgroundOpacity,
                                nonHomeBackgroundContentScale = prefsState.nonHomeBackgroundContentScale,
                                onNonHomeBackgroundContentScaleChanged = prefsState::updateNonHomeBackgroundContentScale,
                                nonHomeBackgroundAlignment = prefsState.nonHomeBackgroundAlignment,
                                onNonHomeBackgroundAlignmentChanged = prefsState::updateNonHomeBackgroundAlignment,
                                nonHomeBackgroundPageStyle = prefsState.nonHomeBackgroundPageStyle,
                                onNonHomeBackgroundPageStyleChanged = prefsState::updateNonHomeBackgroundPageStyle,
                                nonHomeBackgroundScrim = prefsState.nonHomeBackgroundScrim,
                                onNonHomeBackgroundScrimChanged = prefsState::updateNonHomeBackgroundScrim,
                                nonHomeBackgroundDepthEnabled = prefsState.nonHomeBackgroundDepthEnabled,
                                onNonHomeBackgroundDepthEnabledChanged = prefsState::updateNonHomeBackgroundDepthEnabled,
                                nonHomeBackgroundSaturation = prefsState.nonHomeBackgroundSaturation,
                                onNonHomeBackgroundSaturationChanged = prefsState::updateNonHomeBackgroundSaturation,
                                onResetNonHomeBackgroundRendering = prefsState::resetNonHomeBackgroundRendering,
                                onApplyNonHomeBackgroundReadableSuggestion = prefsState::applyNonHomeBackgroundReadableSuggestion,
                                superIslandNotificationEnabled = prefsState.superIslandNotificationEnabled,
                                onSuperIslandNotificationChanged = prefsState::updateSuperIslandNotificationEnabled,
                                superIslandFloatBehavior = prefsState.superIslandFloatBehavior,
                                superIslandAutoClose = prefsState.superIslandAutoClose,
                                onSuperIslandFloatBehaviorChanged = prefsState::updateSuperIslandFloatBehavior,
                                onSuperIslandAutoCloseChanged = prefsState::updateSuperIslandAutoClose,
                                superIslandBypassRestrictionEnabled = prefsState.superIslandBypassRestrictionEnabled,
                                onSuperIslandBypassRestrictionChanged = prefsState::updateSuperIslandBypassRestrictionEnabled,
                                superIslandRestoreDelayMs = prefsState.superIslandRestoreDelayMs,
                                onSuperIslandRestoreDelayMsChanged = prefsState::updateSuperIslandRestoreDelayMs,
                                logLevel = prefsState.logLevel,
                                onLogLevelChanged = prefsState::updateLogLevel,
                                textCopyCapabilityExpanded = prefsState.textCopyCapabilityExpanded,
                                onTextCopyCapabilityExpandedChanged = prefsState::updateTextCopyCapabilityExpanded,
                                cacheDiagnosticsEnabled = prefsState.cacheDiagnosticsEnabled,
                                onCacheDiagnosticsChanged = prefsState::updateCacheDiagnosticsEnabled,
                                privilegeStatus = pagerCoordinator.privilegeStatus,
                                onCheckOrRequestPrivilege = onCheckOrRequestPrivilege,
                                privilegeMode = prefsState.privilegeMode,
                                onPrivilegeModeChanged = prefsState::updatePrivilegeMode,
                                privilegedShell = pagerCoordinator.privilegedShell,
                                appThemeMode = appThemeMode,
                                onAppThemeModeChanged = onAppThemeModeChanged,
                                onBack = onRouteBack,
                                onOpenWebDavSync = { navigator.pushSingleTop(KeiosRoute.WebDavSync) },
                            )
                        }
                    }
                    entry<KeiosRoute.McpSkill> {
                        MainScreenRouteBackgroundHost(
                            prefsState = prefsState,
                            exportBackdropToContent = true,
                        ) {
                            McpSkillPage(
                                mcpServerManager = mcpServerManager,
                                onBack = onRouteBack,
                            )
                        }
                    }
                    entry<KeiosRoute.GitHubActionsNotificationHistory> {
                        MainScreenRouteBackgroundHost(
                            prefsState = prefsState,
                            exportBackdropToContent = true,
                        ) {
                            GitHubActionsNotificationHistoryPage(
                                onBack = onRouteBack,
                                onOpenTrackActions = onOpenGitHubActionsTrackFromHistory,
                                onRetryRefreshTargets = onRetryGitHubRefreshTargetsFromHistory,
                            )
                        }
                    }
                    entry<KeiosRoute.GitHubReleaseList> { route ->
                        MainScreenRouteBackgroundHost(
                            prefsState = prefsState,
                            exportBackdropToContent = true,
                        ) {
                            GitHubReleaseListPage(
                                trackId = route.trackId,
                                onBack = onRouteBack,
                            )
                        }
                    }
                    entry<KeiosRoute.FdroidVersionList> { route ->
                        MainScreenRouteBackgroundHost(
                            prefsState = prefsState,
                            exportBackdropToContent = true,
                        ) {
                            FdroidVersionListPage(
                                trackId = route.trackId,
                                onBack = onRouteBack,
                            )
                        }
                    }
                    entry<KeiosRoute.About> {
                        MainScreenRouteBackgroundHost(prefsState = prefsState) {
                            AboutPage(
                                appLabel = appLabel,
                                notificationPermissionGranted = notificationPermissionGranted,
                                privilegeStatus = pagerCoordinator.privilegeStatus,
                                privilegedShell = pagerCoordinator.privilegedShell,
                                onCheckPrivilege = onCheckOrRequestPrivilege,
                                onBack = onRouteBack,
                            )
                        }
                    }
                    entry<KeiosRoute.BaStudentGuide> { route ->
                        MainScreenRouteBackgroundHost(prefsState = prefsState) {
                            BaStudentGuidePage(
                                warmStartId = route.nonce,
                                preloadingEnabled = prefsState.preloadingEnabled,
                                onBack = onRouteBack,
                            )
                        }
                    }
                    entry<KeiosRoute.BaGuideCatalog>(transition = catalogTransition) { route ->
                        MainScreenRouteBackgroundHost(prefsState = prefsState) {
                            BaGuideCatalogPage(
                                preloadingEnabled = prefsState.preloadingEnabled,
                                notificationPermissionGranted = notificationPermissionGranted,
                                onRequestNotificationPermission = onRequestNotificationPermission,
                                openBgmPlaybackToken = route.openBgmPlaybackToken,
                                onBack = onRouteBack,
                                onOpenGuide = pagerCoordinator.onOpenGuideDetail,
                            )
                        }
                    }
                    entry<KeiosRoute.BaCalendarPool> { route ->
                        MainScreenRouteBackgroundHost(prefsState = prefsState) {
                            BaCalendarPoolPage(
                                targetServerSelection = route.toInitialServerSelection(),
                                initialTab =
                                    if (route.showPool) {
                                        BaCalendarPoolTab.Pool
                                    } else {
                                        BaCalendarPoolTab.Calendar
                                    },
                                onClose = onRouteBack,
                                // The banner half used to swap in the guide behind a boolean with its own
                                // back handler; it is a back-stack push like every other detail page
                                // now. The URL is already saved by preparePoolGuideOpen, so the guide
                                // reads it back the same way it does on the canonical path, and the
                                // nonce only has to keep the content key unique.
                                onOpenGuide = {
                                    navigator.push(
                                        KeiosRoute.BaStudentGuide(nonce = SystemClock.elapsedRealtimeNanos()),
                                    )
                                },
                            )
                        }
                    }
                    entry<KeiosRoute.WebDavSync> {
                        val dataPorts = rememberWebDavSyncDataPorts()
                        MainScreenRouteBackgroundHost(prefsState = prefsState) {
                            WebDavSyncPage(
                                onBack = onRouteBack,
                                dataPorts = dataPorts,
                            )
                        }
                    }
                    entry<KeiosRoute.OsShellRunner> {
                        val privilegedShell = pagerCoordinator.privilegedShell
                        // As an activity this page owned a second PrivilegedShell and attached its
                        // own status callback. PrivilegedShell keeps exactly one callback, so a
                        // shared instance could not have carried both; the private one existed to
                        // avoid displacing MainActivity's. Reading the status MainActivity already
                        // publishes needs no callback at all, so the route can use the shared shell
                        // and privilege lifetime stays exactly where it was.
                        val canRunShellCommand =
                            remember(privilegedShell, pagerCoordinator.privilegeStatus) {
                                privilegedShell.canUseCommand()
                            }
                        MainScreenRouteBackgroundHost(
                            prefsState = prefsState,
                            style = AppManagedBackgroundStyles.FocusedTask,
                            exportBackdropToContent = true,
                        ) {
                            OsShellRunnerPage(
                                canRunShellCommand = canRunShellCommand,
                                onRequestPrivilegeAccess = onCheckOrRequestPrivilege,
                                onRunShellCommand = { command, timeoutMs, onOutput ->
                                    privilegedShell.execCommandCancellableStreaming(
                                        command = command,
                                        timeoutMs = timeoutMs,
                                    ) { output -> onOutput(output) }
                                },
                                onClose = onRouteBack,
                            )
                        }
                    }
                }
            }
            LiquidToastHost(state = liquidToastState)
        }
    }
}

/**
 * Preserves the pre-migration BaGuideCatalog feel — a light fade + shallow slide instead of the
 * full-width Miuix slide. The same depth function drives push, pop, and predictive back: the
 * entering/leaving top fades over a width/7 offset, the covered layer parallaxes width/18.
 */
/**
 * Route push/pop pacing.
 *
 * Miuix's own [NavProgrammaticEasing] bakes an underdamped spring into the tween: it reaches ~48%
 * of the travel in a fifth of the duration and spends the rest on a tail that is barely visible.
 * The page therefore reads as a snap even at the stock 500ms, which is what "too fast" describes.
 * A symmetric emphasized curve spends the duration on the part you can actually see, so the same
 * ballpark length feels deliberate instead of abrupt.
 *
 * Only the programmatic phase is overridden. Gesture commit and cancel stay on the shared spring,
 * which has to seed the release velocity a tween cannot carry.
 */
/** Route push/pop length. Raise for a heavier feel, lower for a snappier one. */
private const val RouteSwitchDurationMillis = 560

private val RouteSwitchMotion =
    NavMotion(
        programmatic =
            NavSettleSpec.Tween(
                durationMillis = RouteSwitchDurationMillis,
                easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
            ),
    )

private fun keiosNavTransition(): NavTransition =
    navGraphicsTransition(opaqueDepth = 1f, motion = RouteSwitchMotion) { scope ->
        // Geometry copied from NavTransitions.MiuixDefault so only the pacing differs: the entering
        // page slides full width from the trailing edge, the covered one parallaxes a quarter width
        // with a light alpha falloff, RTL mirrored, entering offset pixel-snapped.
        val width = scope.layoutSize.width.toFloat()
        val d = scope.relativeDepth
        val rtl = scope.layoutDirection == LayoutDirection.Rtl
        if (d <= 0f) {
            translationX = ((if (rtl) -1f else 1f) * (-d).coerceIn(0f, 1f) * width).roundToInt().toFloat()
        } else {
            val progress = d.coerceIn(0f, 1f)
            translationX = (if (rtl) 1f else -1f) * progress * width * 0.25f
            alpha = 1f - 0.1f * progress
        }
    }

private fun baGuideCatalogNavTransition(): NavTransition =
    navGraphicsTransition(opaqueDepth = 1f, motion = RouteSwitchMotion) { scope ->
        val width = scope.layoutSize.width.toFloat()
        val d = scope.relativeDepth
        val direction = if (scope.layoutDirection == LayoutDirection.Rtl) -1f else 1f
        if (d <= 0f) {
            val progress = (-d).coerceIn(0f, 1f)
            translationX =
                (direction * progress * width / CatalogRouteTopOffsetDivisor).roundToInt().toFloat()
            alpha = 1f - progress
        } else {
            val progress = d.coerceIn(0f, 1f)
            translationX = -direction * progress * width / CatalogRouteCoverOffsetDivisor
            alpha = 1f - CatalogRouteCoverAlphaFalloff * progress
        }
    }

@Composable
private fun MainScreenRouteBackgroundHost(
    prefsState: MainScreenUiPrefsState,
    style: AppManagedBackgroundStyle = AppManagedBackgroundStyles.Standard,
    exportBackdropToContent: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalLiquidPresentationOwnerActive provides rememberNavEntryAtTop(),
    ) {
        AppManagedBackgroundHost(
            enabled = prefsState.nonHomeBackgroundEnabled,
            imageUri = prefsState.nonHomeBackgroundUri,
            opacity = prefsState.nonHomeBackgroundOpacity,
            saturation = prefsState.nonHomeBackgroundSaturation,
            contentScale = prefsState.nonHomeBackgroundContentScale,
            alignment = prefsState.nonHomeBackgroundAlignment,
            pageStyle = prefsState.nonHomeBackgroundPageStyle,
            scrim = prefsState.nonHomeBackgroundScrim,
            style = style,
            exportBackdropToContent = exportBackdropToContent,
            content = content,
        )
    }
}

private const val CatalogRouteTopOffsetDivisor = 7
private const val CatalogRouteCoverOffsetDivisor = 18
private const val CatalogRouteCoverAlphaFalloff = 0.1f
