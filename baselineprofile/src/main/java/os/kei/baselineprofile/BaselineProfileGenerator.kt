package os.kei.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.BeforeClass
import org.junit.AfterClass
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The default profile is intentionally a small set of user journeys.
 *
 * A Baseline Profile is an install-time compilation budget. It should make startup, the main page
 * switch, first scrolls and the app's common routes warm. Feature diagnostics, debug catalogues and
 * network-dependent edge cases belong to functional tests: collecting them here enlarged the profile,
 * made the run depend on remote state and stretched a capture to 23 journeys.
 *
 * The maximum replay budget is 16:
 *
 *  - startup: 5
 *  - main pages: 3
 *  - common routes: 2
 *  - GitHub core: 2
 *  - BA core: 2
 *  - adaptive layouts: 2
 *
 * Each non-startup journey groups adjacent interactions behind one cold start. This preserves distinct
 * failure names while avoiding a new process launch for every screen.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {
    companion object {
        @BeforeClass
        @JvmStatic
        fun preparePermissions() {
            prepareProfileCapturePermissions()
            ProfileCaptureInterruptions.begin()
        }

        @AfterClass
        @JvmStatic
        fun restoreInterruptions() = ProfileCaptureInterruptions.restore()
    }

    @get:Rule
    val rule = BaselineProfileRule()

    /**
     * Startup plus the first user gesture. This is the only journey included in startup-prof.txt.
     */
    @Test
    fun startupAndFirstScroll() {
        rule.collect(
            packageName = profileCaptureAppId(),
            maxIterations = STARTUP_MAX_ITERATIONS,
            stableIterations = STARTUP_STABLE_ITERATIONS,
            includeInStartupProfile = true,
        ) {
            launchHomeFromColdStart()
            flingVisibleScrollable(times = 2)
        }
    }

    /**
     * Every primary destination, its enter transition and its first list movement.
     */
    @Test
    fun mainPagesAndNavigation() {
        rule.collect(
            packageName = profileCaptureAppId(),
            maxIterations = CORE_MAX_ITERATIONS,
            stableIterations = CORE_STABLE_ITERATIONS,
            includeInStartupProfile = false,
        ) {
            launchHomeFromColdStart()
            flingVisibleScrollable(times = 2)

            navigateAndScrollMainPage(
                tabTag = MAIN_BOTTOM_TAB_OS,
                pageTag = OS_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_OS,
            )
            navigateAndScrollMainPage(
                tabTag = MAIN_BOTTOM_TAB_MCP,
                pageTag = MCP_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_MCP,
            )
            navigateAndScrollMainPage(
                tabTag = MAIN_BOTTOM_TAB_GITHUB,
                pageTag = GITHUB_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_GITHUB,
            )
            navigateAndScrollMainPage(
                tabTag = MAIN_BOTTOM_TAB_BA,
                pageTag = BA_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_BA,
            )
            navigateToMainPage(
                tabTag = MAIN_BOTTOM_TAB_HOME,
                pageTag = HOME_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_HOME,
            )

            swipeMainPagerTo(
                forward = true,
                pageTag = OS_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_OS,
            )
            swipeMainPagerTo(
                forward = false,
                pageTag = HOME_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_HOME,
            )
        }
    }

    /**
     * Common route pushes and the shared presentation layer.
     */
    @Test
    fun commonRoutesAndChrome() {
        rule.collect(
            packageName = profileCaptureAppId(),
            maxIterations = FEATURE_MAX_ITERATIONS,
            stableIterations = FEATURE_STABLE_ITERATIONS,
            includeInStartupProfile = false,
        ) {
            launchHomeFromColdStart()

            exerciseSettingsAndReturn(wide = profileWindowUsesTwoColumns())
            pushRouteAndReturn(
                entryTag = HOME_ABOUT_BUTTON,
                pageTag = ABOUT_PAGE_ROOT,
                returnTag = HOME_PAGE_ROOT,
            )
            pushRouteAndReturn(
                entryTag = HOME_WEBDAV_CARD,
                pageTag = WEBDAV_SYNC_PAGE_ROOT,
                returnTag = HOME_PAGE_ROOT,
                flings = 1,
            )

            navigateToMainPage(
                tabTag = MAIN_BOTTOM_TAB_OS,
                pageTag = OS_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_OS,
            )
            pushRouteAndReturn(
                entryTag = OS_SHELL_RUNNER_BUTTON,
                pageTag = OS_SHELL_RUNNER_PAGE_ROOT,
                returnTag = OS_PAGE_ROOT,
                flings = 1,
            )

            navigateToMainPage(
                tabTag = MAIN_BOTTOM_TAB_MCP,
                pageTag = MCP_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_MCP,
            )
            pushRouteAndReturn(
                entryTag = MCP_SKILL_BUTTON,
                pageTag = MCP_SKILL_PAGE_ROOT,
                returnTag = MCP_PAGE_ROOT,
            )

            navigateToMainPage(
                tabTag = MAIN_BOTTOM_TAB_GITHUB,
                pageTag = GITHUB_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_GITHUB,
            )
            openMenuAndDismiss(
                triggerTag = GITHUB_IMPORT_MENU_BUTTON,
                rowTag = GITHUB_IMPORT_TRACKS,
            )
        }
    }

    /**
     * The frequent GitHub work: inspect a tracked app and open the two primary editing sheets.
     *
     * A fresh install can legitimately have no tracked card. The page, navigation and edit surfaces
     * still collect deterministically; the card branch becomes available once user state exists.
     */
    @Test
    fun gitHubTrackingCore() {
        rule.collect(
            packageName = profileCaptureAppId(),
            maxIterations = FEATURE_MAX_ITERATIONS,
            stableIterations = FEATURE_STABLE_ITERATIONS,
            includeInStartupProfile = false,
        ) {
            launchHomeFromColdStart()
            navigateToMainPage(
                tabTag = MAIN_BOTTOM_TAB_GITHUB,
                pageTag = GITHUB_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_GITHUB,
            )

            if (waitForOptionalTestTag(GITHUB_TRACKED_ITEM_CARD_FIRST, timeoutMs = 8_000)) {
                scrollTestTagIntoReach(GITHUB_TRACKED_ITEM_CARD_FIRST)
                clickTaggedCardHeader(GITHUB_TRACKED_ITEM_CARD_FIRST)
                flingVisibleScrollable(times = 1)

                // The preceding fling can unload this header above the viewport; seek back toward
                // it instead of continuing down the list when LazyColumn no longer exposes it.
                scrollTestTagIntoReach(GITHUB_TRACKED_ITEM_MORE_BUTTON, forwardWhenAbsent = false)
                clickTestTag(GITHUB_TRACKED_ITEM_MORE_BUTTON)
                if (waitForOptionalTestTag(GITHUB_ACTIONS_MENU_ITEM, timeoutMs = 5_000)) {
                    clickTestTag(GITHUB_ACTIONS_MENU_ITEM)
                    waitForTestTag(LIQUID_SHEET_PANEL, timeoutMs = 12_000)
                    dismissTheOpenOverlay(LIQUID_SHEET_PANEL)
                } else {
                    device.pressBack()
                    device.waitForIdle()
                }
            }

            openAndDismissOverlay(
                triggerTag = GITHUB_ADD_TRACKED_BUTTON,
                panelTag = LIQUID_SHEET_PANEL,
            )
            openExerciseAndDismissLiquidSheet(GITHUB_STRATEGY_SHEET_BUTTON)
        }
    }

    /**
     * BA office cards, the merged calendar route, the daily sheet and the media catalogue.
     *
     * Two representative student-detail tabs replace the former six-tab sweep. The catalogue still
     * includes Students, Lobby, Music and Play because each is a distinct high-level experience and
     * playing one row is the only path that warms Media3.
     */
    @Test
    fun baOfficeAndCatalogCore() {
        rule.collect(
            packageName = profileCaptureAppId(),
            maxIterations = FEATURE_MAX_ITERATIONS,
            stableIterations = FEATURE_STABLE_ITERATIONS,
            includeInStartupProfile = false,
        ) {
            launchHomeFromColdStart()
            navigateToMainPage(
                tabTag = MAIN_BOTTOM_TAB_BA,
                pageTag = BA_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_BA,
            )
            flingVisibleScrollable(times = 2)
            dragVisibleScrollable(times = 1)

            if (waitForOptionalTestTag(BA_COOLDOWN_CARD_FIRST, timeoutMs = 5_000)) {
                scrollTestTagIntoReach(BA_COOLDOWN_CARD_FIRST)
                clickTaggedCardHeader(BA_COOLDOWN_CARD_FIRST)
                if (waitForOptionalTestTag(BA_COOLDOWN_ADJUST_BUTTON, timeoutMs = 5_000)) {
                    openAndDismissOverlay(
                        triggerTag = BA_COOLDOWN_ADJUST_BUTTON,
                        panelTag = LIQUID_SHEET_PANEL,
                        required = false,
                    )
                }
            }

            val wide = profileWindowUsesTwoColumns()
            openBaCalendarPoolAndReturn(wide = wide)
            openAndDismissOverlay(
                triggerTag = BA_DOCK_DAILY_DONE,
                panelTag = LIQUID_SHEET_PANEL,
            )

            exerciseBaCatalogAndReturn(wide = wide)
        }
    }

    /**
     * The wide branches added by the recent two-lane UI work, followed by a live fold transition.
     *
     * One forced 1000x800dp window exercises Settings, About, OS, Shell, MCP, Skill, BA, Calendar,
     * Catalogue and GitHub in their wide forms. Both lane coordinates are scrolled independently.
     * The final 775dp -> 500dp transition covers sidebar-to-bottom-bar reflow without another cold start.
     */
    @Test
    fun adaptiveLargeScreenCore() {
        rule.collect(
            packageName = profileCaptureAppId(),
            maxIterations = ADAPTIVE_MAX_ITERATIONS,
            stableIterations = ADAPTIVE_STABLE_ITERATIONS,
            includeInStartupProfile = false,
            // The fixed launch harness exists only in the disposable collector APK.
            filterPredicate = { rule -> "Los/kei/profilecapture/" !in rule },
        ) {
            val originalWindowSize = readWindowSizeOverride()
            try {
                forceWindowSizeDp(widthDp = 1000, heightDp = 800)
                launchHomeFromColdStart()

                exerciseSettingsAndReturn(wide = true)
                pushWideRouteAndReturn(
                    entryTag = HOME_ABOUT_BUTTON,
                    pageTag = ABOUT_PAGE_ROOT,
                    returnTag = HOME_PAGE_ROOT,
                )

                navigateToMainPage(
                    tabTag = MAIN_BOTTOM_TAB_OS,
                    pageTag = OS_PAGE_ROOT,
                    settledTag = MAIN_PAGER_SETTLED_OS,
                )
                exerciseWideLanes()
                pushWideRouteAndReturn(
                    entryTag = OS_SHELL_RUNNER_BUTTON,
                    pageTag = OS_SHELL_RUNNER_PAGE_ROOT,
                    returnTag = OS_PAGE_ROOT,
                )

                // Scrolling both wide lanes can briefly rebuild the top chrome while the shared
                // pager settles. Wait for the adaptable-navigation control to re-enter the
                // accessibility tree before converting the tab bar to a sidebar.
                waitForTestTag(MAIN_SIDEBAR_TOGGLE, timeoutMs = 15_000)
                clickTestTag(MAIN_SIDEBAR_TOGGLE)
                waitForTestTag(MAIN_SIDEBAR_ROW_MCP)
                clickSidebarPage(
                    rowTag = MAIN_SIDEBAR_ROW_MCP,
                    pageTag = MCP_PAGE_ROOT,
                    settledTag = MAIN_PAGER_SETTLED_MCP,
                )
                exerciseWideLanes()
                pushWideRouteAndReturn(
                    entryTag = MCP_SKILL_BUTTON,
                    pageTag = MCP_SKILL_PAGE_ROOT,
                    returnTag = MCP_PAGE_ROOT,
                )

                clickSidebarPage(
                    rowTag = MAIN_SIDEBAR_ROW_BA,
                    pageTag = BA_PAGE_ROOT,
                    settledTag = MAIN_PAGER_SETTLED_BA,
                )
                exerciseWideLanes()
                openBaCalendarPoolAndReturn(wide = true)

                exerciseBaCatalogAndReturn(wide = true)
                exerciseGuideViewersAndReturn()

                clickSidebarPage(
                    rowTag = MAIN_SIDEBAR_ROW_GITHUB,
                    pageTag = GITHUB_PAGE_ROOT,
                    settledTag = MAIN_PAGER_SETTLED_GITHUB,
                )
                exerciseWideLanes()
                if (openWindowFrom(
                        triggerTag = GITHUB_ACTIONS_HISTORY_BUTTON,
                        arrivalTag = GITHUB_ACTIONS_HISTORY_PAGE_ROOT,
                        required = false,
                    )
                ) {
                    clickBottomBarTab(GITHUB_HISTORY_TAB_ACTIONS)
                    exerciseWideLanes()
                    clickBottomBarTab(GITHUB_HISTORY_TAB_TRACKING)
                    exerciseWideLanes()
                    device.pressBack()
                    waitForTestTag(GITHUB_PAGE_ROOT, timeoutMs = 15_000)
                }

                // The sidebar row selection above can still be settling its chrome layer. Keep
                // this second shape conversion deterministic for the following window reflow.
                waitForTestTag(MAIN_SIDEBAR_TOGGLE, timeoutMs = 15_000)
                clickTestTag(MAIN_SIDEBAR_TOGGLE)
                forceWindowSizeDp(widthDp = 775, heightDp = 800)
                forceWindowSizeDp(widthDp = 500, heightDp = 800)
                // Recollect compact guide tabs and finger paging on this source instead of
                // carrying forward an older Phone profile when only the Pad is available.
                navigateToMainPage(
                    tabTag = MAIN_BOTTOM_TAB_BA,
                    pageTag = BA_PAGE_ROOT,
                    settledTag = MAIN_PAGER_SETTLED_BA,
                )
                exerciseBaCatalogAndReturn(wide = false)
                navigateToMainPage(
                    tabTag = MAIN_BOTTOM_TAB_HOME,
                    pageTag = HOME_PAGE_ROOT,
                    settledTag = MAIN_PAGER_SETTLED_HOME,
                )
            } finally {
                restoreWindowSize(originalWindowSize)
            }
        }
    }
}

private const val STARTUP_MAX_ITERATIONS = 5
private const val STARTUP_STABLE_ITERATIONS = 2
private const val CORE_MAX_ITERATIONS = 3
private const val CORE_STABLE_ITERATIONS = 2
private const val FEATURE_MAX_ITERATIONS = 2
private const val FEATURE_STABLE_ITERATIONS = 2
private const val ADAPTIVE_MAX_ITERATIONS = 2
private const val ADAPTIVE_STABLE_ITERATIONS = 2
