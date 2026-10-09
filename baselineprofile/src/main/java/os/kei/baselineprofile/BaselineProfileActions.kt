package os.kei.baselineprofile

import android.graphics.Rect
import android.util.Log
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.Until
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/** Shared phone/wide catalog path. Remote student and audio data remain optional. */
internal fun MacrobenchmarkScope.exerciseBaCatalogAndReturn(wide: Boolean) {
    openWindowFrom(BA_DOCK_OPEN_GUIDE_CATALOG, BA_GUIDE_CATALOG_PAGE_ROOT)
    if (waitForOptionalTestTag(BA_GUIDE_CATALOG_ENTRY_FIRST, timeoutMs = 8_000)) {
        scrollTestTagIntoReach(BA_GUIDE_CATALOG_ENTRY_FIRST)
        clickTestTag(BA_GUIDE_CATALOG_ENTRY_FIRST)
        if (waitForOptionalTestTag(BA_STUDENT_GUIDE_PAGE_ROOT, timeoutMs = 15_000)) {
            if (wide) {
                exerciseGuideSidebarAndRestore()
            } else {
                flingVisibleScrollable(times = 1)
                clickBottomBarTab(BA_STUDENT_GUIDE_TAB_PROFILE)
                flingVisibleScrollable(times = 1)
                swipeGuidePagerWhileCoasting()
                clickBottomBarTab(BA_STUDENT_GUIDE_TAB_SKILLS)
            }
            device.pressBack()
            waitForTestTag(BA_GUIDE_CATALOG_PAGE_ROOT, timeoutMs = 15_000)
        }
    }
    if (wide) exerciseWideLanes()
    clickBottomBarTab(BA_GUIDE_CATALOG_DOCK_MEMORY_LOBBY)
    if (wide) exerciseWideLanes() else flingVisibleScrollable(times = 1)
    clickBottomBarTab(BA_GUIDE_CATALOG_DOCK_FAVORITE_BGM)
    if (wide) exerciseWideLanes() else flingVisibleScrollable(times = 1)
    // Playback last: playing media replaces collapsed catalog chrome with the mini player.
    clickBottomBarTab(BA_GUIDE_CATALOG_DOCK_STUDENT_BGM)
    if (waitForOptionalTestTag(BA_GUIDE_CATALOG_STUDENT_BGM_FIRST, timeoutMs = 8_000)) {
        scrollTestTagIntoReach(BA_GUIDE_CATALOG_STUDENT_BGM_FIRST)
        playFirstStudentBgm()
        if (!wide) flingVisibleScrollable(times = 1)
    }
    if (wide) exerciseWideLanes()
    device.pressBack()
    waitForTestTag(BA_PAGE_ROOT, timeoutMs = 15_000)
}

/** Opens the owned-coordinate selector without changing the selected theme. */
internal fun MacrobenchmarkScope.exerciseSettingsAndReturn(wide: Boolean) {
    openWindowFrom(HOME_SETTINGS_BUTTON, SETTINGS_PAGE_ROOT)
    clickBottomBarTab(SETTINGS_TAB_INTERFACE)
    waitForTestTag(SETTINGS_THEME_MODE_SELECTOR)
    openMenuAndDismiss(SETTINGS_THEME_MODE_SELECTOR, SNAPSHOT_MENU_PANEL)
    if (wide) exerciseWideLanes() else flingVisibleScrollable(times = 2)
    returnFromPushedRoute(SETTINGS_PAGE_ROOT, HOME_PAGE_ROOT)
}

internal fun MacrobenchmarkScope.navigateAndScrollMainPage(
    tabTag: String,
    pageTag: String,
    settledTag: String,
) {
    navigateToMainPage(tabTag, pageTag, settledTag)
    flingVisibleScrollable(times = 2)
}

internal fun MacrobenchmarkScope.navigateToMainPage(
    tabTag: String,
    pageTag: String,
    settledTag: String,
) {
    clickBottomBarTab(tabTag)
    waitForTestTag(pageTag, timeoutMs = 15_000)
    waitForTestTag(settledTag, timeoutMs = 15_000)
}

/**
 * Changes page the way a finger does, and proves the page actually changed.
 *
 * Every other switch in this journey is a tab tap, and a tap and a swipe do not share their code: a
 * tap runs `animateLoadedPagerPosition` on Miuix's page-navigation spring from rest, while a finger runs
 * `draggable`'s drag detection, `startUserScroll` and `dragBy` once per frame, then `settleAfterDrag`'s
 * velocity spring -- `animateLoadedPagerSettlePosition`, whose one call site no tap reaches.
 *
 * Measured rather than assumed, by capturing this journey twice on the same AVD: without this step
 * its own baseline-prof.txt carries none of `startUserScroll`, `dragBy`, `settleAfterDrag` or
 * `animateLoadedPagerSettlePosition`; with it, all four. What this does *not* claim is new rules in
 * the shipped artifact -- the merged profile already carried that path, incidentally, from some
 * other journey. The point is that the journey named for page switching owns the switch users
 * actually perform, instead of inheriting it from a journey about something else.
 *
 * The arrival is asserted rather than optional on purpose. A horizontal drag a child consumes
 * leaves the pager exactly where it was, and the settled tag is the only thing that separates that
 * from a real page change -- the destination's page root stays composed as a pager neighbour either
 * way. An optional wait here would pass while compiling none of the gesture path.
 */
internal fun MacrobenchmarkScope.swipeMainPagerTo(
    forward: Boolean,
    pageTag: String,
    settledTag: String,
) {
    // Both ends stay well inside the display. A horizontal swipe anchored on either edge is the
    // system's back gesture, which never reaches the pager at all.
    val nearX = (device.displayWidth * PAGER_SWIPE_NEAR_X).toInt()
    val farX = (device.displayWidth * PAGER_SWIPE_FAR_X).toInt()
    val centerY = (device.displayHeight * PAGER_SWIPE_Y).toInt()
    swipeWithInjectionRetry(
        startX = if (forward) farX else nearX,
        startY = centerY,
        endX = if (forward) nearX else farX,
        endY = centerY,
        steps = FLING_STEPS,
        failureMessage = "Unable to swipe the main pager ${if (forward) "forward" else "back"}",
    )
    waitForTestTag(pageTag, timeoutMs = 15_000)
    waitForTestTag(settledTag, timeoutMs = 15_000)
}

/**
 * The guide's own pager, moved by a finger, until it has actually moved from Profile to Voice.
 *
 * Its tabs are otherwise only ever tapped in this profile. Called straight after a fling, so the first
 * swipe can land while the page's list is still coasting; the guide pager's Miuix `pagerGestureOverride` in
 * `CrossAxis` mode takes that swipe over from the list (its Initial-pass takeover) and pages, and a swipe
 * after the list has stopped goes through its Foundation drag node instead. Both are pages moved by a finger,
 * but a swipe can still miss, so the step swipes until the Voice tab reports selected rather than a fixed
 * number of times.
 *
 * Measured on the A17 AVD (2026-09-23), when the guide used `TapToHalt`. The override's pointer loop and the
 * snap fling compile without any swipe: the override sees every pointer event on the pager, vertical flings
 * and tab taps included. What
 * arriving on Voice adds is the pager moving to a neighbour (`PagerCacheWindowScope`'s prefetch) and Gallery
 * composing beside it -- 218 Gallery rules in the journey's own file against 71 in a capture where the
 * swipes happened not to page. Optional, like the guide itself, because the page's content decides whether
 * there is a Voice tab to reach.
 */
private fun MacrobenchmarkScope.swipeGuidePagerWhileCoasting() {
    val nearX = (device.displayWidth * PAGER_SWIPE_NEAR_X).toInt()
    val farX = (device.displayWidth * PAGER_SWIPE_FAR_X).toInt()
    val centerY = (device.displayHeight * PAGER_SWIPE_Y).toInt()
    repeat(GUIDE_PAGER_SWIPE_ATTEMPTS) {
        swipeWithInjectionRetry(
            startX = farX,
            startY = centerY,
            endX = nearX,
            endY = centerY,
            steps = FLING_STEPS,
            failureMessage = "Unable to swipe the guide pager",
        )
        if (device.wait(Until.hasObject(testTagSelector(BA_STUDENT_GUIDE_TAB_VOICE).selected(true)), 3_000)) {
            device.waitForIdle()
            return
        }
    }
}

/**
 * Plays the first student BGM and waits until it is actually playing, which is what loads media3.
 *
 * The tap alone does not: on a fresh install the row resolves its audio over the network first, and moving
 * on straight after the tap left before the player existed -- a capture from a fresh install came back with
 * 9 media3 rules where another gave 4,992. Still optional, as the plan says Media3 is, because a device with
 * no network cannot play; the wait is what makes a device that can actually do it.
 */
private fun MacrobenchmarkScope.playFirstStudentBgm() {
    clickTestTag(BA_GUIDE_CATALOG_STUDENT_BGM_FIRST)
    waitForOptionalTestTag(BA_GUIDE_CATALOG_BGM_PLAYING, timeoutMs = BGM_PLAYBACK_TIMEOUT_MS)
}

internal fun MacrobenchmarkScope.clickSidebarPage(
    rowTag: String,
    pageTag: String,
    settledTag: String,
) {
    clickTestTag(rowTag)
    waitForTestTag(pageTag, timeoutMs = 15_000)
    waitForTestTag(settledTag, timeoutMs = 15_000)
}

internal fun MacrobenchmarkScope.pushRouteAndReturn(
    entryTag: String,
    pageTag: String,
    returnTag: String,
    flings: Int = 2,
) {
    openWindowFrom(triggerTag = entryTag, arrivalTag = pageTag)
    flingVisibleScrollable(times = flings)
    returnFromPushedRoute(pageTag = pageTag, returnTag = returnTag)
}

internal fun MacrobenchmarkScope.pushWideRouteAndReturn(
    entryTag: String,
    pageTag: String,
    returnTag: String,
) {
    openWindowFrom(triggerTag = entryTag, arrivalTag = pageTag)
    exerciseWideLanes()
    returnFromPushedRoute(pageTag = pageTag, returnTag = returnTag)
}

/**
 * Leaves a pushed route and proves the pushed page actually disappeared.
 *
 * Several route roots stay composed underneath the pushed page, so seeing [returnTag] alone is not
 * proof of a pop. Shell Runner also focuses its editor and opens the IME: its first Back closes the
 * keyboard and its second Back pops the route. Other routes complete on the first attempt.
 */
private fun MacrobenchmarkScope.returnFromPushedRoute(
    pageTag: String,
    returnTag: String,
) {
    repeat(PUSH_ROUTE_MAX_BACK_ATTEMPTS) {
        device.pressBack()
        if (device.wait(Until.gone(testTagSelector(pageTag)), PUSH_ROUTE_GONE_TIMEOUT_MS)) {
            waitForTestTag(returnTag, timeoutMs = 15_000)
            return
        }
    }
    error("Unable to leave pushed route testTag=$pageTag in ${targetAppId()}")
}

internal fun MacrobenchmarkScope.openBaCalendarPoolAndReturn(wide: Boolean = false) {
    openWindowFrom(BA_DOCK_OPEN_CALENDAR_POOL, BA_CALENDAR_POOL_PAGE_ROOT)

    if (wide) {
        // The merged page shows Calendar and Pool side by side at this width and removes the category
        // bar. Driving both lane coordinates covers the two lists; looking for the phone-only Pool tab
        // here would time out on exactly the new UI this journey exists to profile.
        exerciseWideLanes()
    } else {
        flingVisibleScrollable(times = 1)
        clickBottomBarTab(BA_CALENDAR_POOL_TAB_POOL)
        flingVisibleScrollable(times = 1)
    }
    device.pressBack()
    waitForTestTag(BA_PAGE_ROOT, timeoutMs = 15_000)
}

internal fun MacrobenchmarkScope.openAndDismissOverlay(
    triggerTag: String,
    panelTag: String,
    required: Boolean = true,
): Boolean {
    val opened = openWindowFrom(triggerTag, panelTag, required)
    if (opened) dismissTheOpenOverlay(panelTag)
    return opened
}

/**
 * Compiles the shared Sheet paths that opening and pressing Back never reaches.
 *
 * The strategy sheet is deterministic, long and lazy. Expanding through the grabber first lets the
 * following swipes belong to its content; the final grabber drag warms resize, nested-scroll
 * arbitration, layout-height updates and the settle spring without adding another cold start.
 */
internal fun MacrobenchmarkScope.openExerciseAndDismissLiquidSheet(triggerTag: String) {
    openWindowFrom(triggerTag = triggerTag, arrivalTag = LIQUID_SHEET_PANEL)
    waitForTestTag(LIQUID_SHEET_DRAG_REGION, timeoutMs = 12_000)

    dragLiquidSheetRegion(up = true)
    swipeWithinTestTag(LIQUID_SHEET_PANEL, up = true)
    swipeWithinTestTag(LIQUID_SHEET_PANEL, up = false)
    dragLiquidSheetRegion(up = false)

    dismissTheOpenOverlay(LIQUID_SHEET_PANEL)
}

private fun MacrobenchmarkScope.dragLiquidSheetRegion(up: Boolean) {
    val bounds = device.findObject(testTagSelector(LIQUID_SHEET_DRAG_REGION))?.visibleBounds
        ?: error("Unable to find Liquid Sheet drag region in ${targetAppId()}")
    val distance = (device.displayHeight * LIQUID_SHEET_DRAG_DISTANCE_FRACTION).toInt()
    val startY = bounds.centerY()
    val endY =
        (if (up) startY - distance else startY + distance)
            .coerceIn(1, device.displayHeight - 2)
    swipeWithInjectionRetry(
        startX = bounds.centerX(),
        startY = startY,
        endX = bounds.centerX(),
        endY = endY,
        steps = DRAG_STEPS,
        failureMessage = "Unable to drag Liquid Sheet ${if (up) "up" else "down"}",
    )
}

private fun MacrobenchmarkScope.swipeWithinTestTag(tag: String, up: Boolean) {
    val bounds = device.findObject(testTagSelector(tag))?.visibleBounds
        ?: error("Unable to find testTag=$tag in ${targetAppId()}")
    val upperY = bounds.top + (bounds.height() * LIQUID_SHEET_CONTENT_UPPER_FRACTION).toInt()
    val lowerY = bounds.top + (bounds.height() * LIQUID_SHEET_CONTENT_LOWER_FRACTION).toInt()
    val startY = if (up) lowerY else upperY
    val endY = if (up) upperY else lowerY
    swipeWithInjectionRetry(
        startX = bounds.centerX(),
        startY = startY,
        endX = bounds.centerX(),
        endY = endY,
        steps = FLING_STEPS,
        failureMessage = "Unable to scroll Liquid Sheet content ${if (up) "up" else "down"}",
    )
}

private fun MacrobenchmarkScope.swipeWithInjectionRetry(
    startX: Int,
    startY: Int,
    endX: Int,
    endY: Int,
    steps: Int,
    failureMessage: String,
) {
    repeat(GESTURE_INJECTION_ATTEMPTS) {
        if (device.swipe(startX, startY, endX, endY, steps)) {
            device.waitForIdle()
            return
        }
        device.waitForIdle()
    }
    error(failureMessage)
}

internal fun MacrobenchmarkScope.openMenuAndDismiss(
    triggerTag: String,
    rowTag: String,
) {
    openWindowFrom(triggerTag = triggerTag, arrivalTag = rowTag)
    device.pressBack()
    check(device.wait(Until.gone(testTagSelector(rowTag)), 12_000)) {
        "Timed out waiting for menu row testTag=$rowTag to dismiss in ${targetAppId()}"
    }
    device.waitForIdle()
}

internal fun MacrobenchmarkScope.dismissTheOpenOverlay(panelTag: String) {
    device.pressBack()
    check(device.wait(Until.gone(testTagSelector(panelTag)), 15_000)) {
        "Timed out waiting for testTag=$panelTag to dismiss in ${targetAppId()}"
    }
    device.waitForIdle()
}

internal fun MacrobenchmarkScope.openWindowFrom(
    triggerTag: String,
    arrivalTag: String,
    required: Boolean = true,
): Boolean {
    repeat(OPEN_WINDOW_ATTEMPTS) {
        if (clickVisibleTag(triggerTag, timeoutMs = 6_000) &&
            device.wait(Until.hasObject(testTagSelector(arrivalTag)), 12_000)
        ) {
            device.waitForIdle()
            return true
        }
        Log.i("ProfileJourney", "No arrival at $arrivalTag after $triggerTag (attempt ${it + 1})")
        nudgeVisibleScrollable(forward = true)
    }

    if (required) captureFailedOpenScene(triggerTag, arrivalTag)
    check(!required) {
        "testTag=$triggerTag never opened testTag=$arrivalTag in ${targetAppId()}"
    }
    return false
}

/** Capture before BaselineProfileRule closes its target in failure cleanup. */
private fun MacrobenchmarkScope.captureFailedOpenScene(triggerTag: String, arrivalTag: String) {
    runCatching {
        val output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?.let(::File)
            ?: InstrumentationRegistry.getInstrumentation().context.getExternalFilesDir(null)
            ?: error("No owned diagnostic output directory")
        output.mkdirs()
        val name = "missing-$triggerTag-to-$arrivalTag"
        device.dumpWindowHierarchy(File(output, "$name.xml"))
        device.takeScreenshot(File(output, "$name.png"))
        File(output, "$name-activity.txt").writeText(
            device.executeShellCommand("dumpsys activity activities ${targetAppId()}"),
        )
        Log.i("ProfileJourney", "Captured missing arrival before cleanup: $name, package=${device.currentPackageName}")
    }.onFailure { Log.w("ProfileJourney", "Unable to capture missing arrival", it) }
}

private fun MacrobenchmarkScope.clickVisibleTag(
    tag: String,
    timeoutMs: Long = 8_000,
): Boolean {
    if (!waitForOptionalTestTag(tag, timeoutMs)) {
        // A page fling turns its action bar into a compact dock. Reveal it once before treating a
        // route action as absent; this is the common path into BA calendar/catalog after scrolling.
        val compactDock = findCompactNavigationDock() ?: return false
        val bounds = compactDock.visibleBounds
        if (bounds.isEmpty || !device.click(bounds.centerX(), bounds.centerY())) return false
        device.waitForIdle()
        if (!waitForOptionalTestTag(tag, timeoutMs = 2_000)) return false
    }
    val bounds = waitForStableTagBounds(tag, timeoutMs = timeoutMs) ?: return false
    // Bottom chrome is intentionally near the display edge. Scrolling a visible route action into
    // the list's "safe" area collapses that very action before the tap (notably BA daily Done).
    if (!device.click(bounds.centerX(), bounds.centerY())) return false
    Log.i("ProfileJourney", "Tapped $tag at $bounds")
    device.waitForIdle()
    return true
}

/** Accessibility idleness can precede Compose motion settling; wait on the target's geometry. */
private fun MacrobenchmarkScope.waitForStableTagBounds(tag: String, timeoutMs: Long): Rect? {
    val resources = InstrumentationRegistry.getInstrumentation().context.resources
    val statusBarId = resources.getIdentifier("status_bar_height", "dimen", "android")
    val statusBarBottom = if (statusBarId != 0) resources.getDimensionPixelSize(statusBarId) else 0
    val deadline = System.nanoTime() + timeoutMs * 1_000_000L
    var previous: Rect? = null
    var matchingSamples = 0
    while (System.nanoTime() < deadline) {
        // Compose can replace the semantics node between lookup and bounds read, especially
        // while a catalog switches tabs after a viewport change. Resample only stale nodes.
        val bounds = try {
            device.findObject(testTagSelector(tag))?.takeIf { it.isEnabled }?.visibleBounds
        } catch (_: StaleObjectException) {
            null
        }
        if (bounds != null &&
            bounds.width() >= MIN_TAPPABLE_HEIGHT_PX && bounds.height() >= MIN_TAPPABLE_HEIGHT_PX &&
            bounds.centerY() >= statusBarBottom && bounds.centerY() < device.displayHeight &&
            bounds.centerX() in 0 until device.displayWidth
        ) {
            matchingSamples = if (bounds == previous) matchingSamples + 1 else 1
            if (matchingSamples >= STABLE_TARGET_SAMPLES) return bounds
            previous = bounds
        } else {
            previous = null
            matchingSamples = 0
        }
        Thread.sleep(TARGET_GEOMETRY_SAMPLE_INTERVAL_MS)
    }
    Log.i("ProfileJourney", "No stable visible bounds for $tag")
    return null
}

private fun MacrobenchmarkScope.findCompactNavigationDock(): UiObject2? {
    device.findObject(testTagSelector(COMPACT_BOTTOM_BAR_DOCK).enabled(true))?.let { return it }
    // Main navigation retains the selected page tag on its collapsed dock. Compose exports that
    // caller tag instead of the shared compact tag when both occupy the same semantics node.
    // Unlike an expanded tab, this tagged surface has a clickable descendant, not its own action.
    return listOf(MAIN_BOTTOM_TAB_HOME, MAIN_BOTTOM_TAB_OS, MAIN_BOTTOM_TAB_MCP,
        MAIN_BOTTOM_TAB_GITHUB, MAIN_BOTTOM_TAB_BA)
        .firstNotNullOfOrNull { tag ->
            device.findObject(testTagSelector(tag).enabled(true).clickable(false))
                ?.takeUnless { it.visibleBounds.isEmpty }
        }
}

internal fun MacrobenchmarkScope.clickTestTag(tag: String) {
    val bounds = waitForStableTagBounds(tag, timeoutMs = 8_000)
        ?: error("Unable to find a settled, safe target testTag=$tag in ${targetAppId()}")
    check(device.click(bounds.centerX(), bounds.centerY()))
    Log.i("ProfileJourney", "Tapped $tag at $bounds")
    device.waitForIdle()
}

internal fun MacrobenchmarkScope.clickTaggedCardHeader(tag: String) {
    val bounds = device.findObject(testTagSelector(tag))?.visibleBounds
        ?: error("Unable to find card testTag=$tag in ${targetAppId()}")
    val inset = minOf(bounds.height() / 4, MAX_HEADER_TAP_INSET_PX)
    device.click(bounds.centerX(), bounds.top + inset)
    device.waitForIdle()
}

internal fun MacrobenchmarkScope.scrollTestTagIntoReach(
    tag: String,
    forwardWhenAbsent: Boolean = true,
) {
    val safeTop = (device.displayHeight * SCROLL_SAFE_TOP_FRACTION).toInt()
    val safeBottom = (device.displayHeight * SCROLL_SAFE_BOTTOM_FRACTION).toInt()
    repeat(SCROLL_INTO_REACH_ATTEMPTS) {
        val bounds = device.findObject(testTagSelector(tag))?.visibleBounds
        if (bounds != null &&
            bounds.height() >= MIN_TAPPABLE_HEIGHT_PX &&
            bounds.centerY() in safeTop..safeBottom
        ) {
            device.waitForIdle()
            return
        }
        nudgeVisibleScrollable(forward = if (bounds == null) forwardWhenAbsent else bounds.centerY() > safeBottom)
    }
    error("Unable to bring testTag=$tag into reach in ${targetAppId()}")
}

internal fun MacrobenchmarkScope.clickBottomBarTab(tag: String) {
    repeat(BOTTOM_BAR_REEXPAND_ATTEMPTS) {
        val tab = device.findObject(testTagSelector(tag))
        if (tab != null) {
            val bounds = waitForStableTagBounds(tag, timeoutMs = 3_000)
            if (bounds != null && device.click(bounds.centerX(), bounds.centerY())) {
                Log.i("ProfileJourney", "Tapped navigation $tag at $bounds")
                device.waitForIdle()
                return
            }
        }

        val compactDock = findCompactNavigationDock()
        if (compactDock != null) {
            // The shared tag sits on the visual Liquid surface while its clickable semantics can
            // belong to a descendant. UiObject2.click() therefore warns that the tagged node is
            // non-clickable even though tapping its bounds is the correct user interaction. A
            // compact-to-expanded transition also keeps stale semantics around for a few frames;
            // wait for the requested tab instead of burning through every retry during animation.
            val bounds = compactDock.visibleBounds
            device.click(bounds.centerX(), bounds.centerY())
            device.wait(Until.hasObject(testTagSelector(tag)), BOTTOM_BAR_EXPAND_TIMEOUT_MS)
            device.waitForIdle()
        } else {
            nudgeVisibleScrollable(forward = false)
            // A reverse scroll asks the shared chrome controller to expand the bar. Compose can be
            // idle before the expand transition publishes fresh tab semantics, so wait for this
            // exact destination before the next lookup.
            device.wait(Until.hasObject(testTagSelector(tag)), BOTTOM_BAR_EXPAND_TIMEOUT_MS)
        }
    }
    error("Unable to bring navigation tab testTag=$tag into view in ${targetAppId()}")
}

/**
 * Converts the guide to its rail, exercises it, and converts it back.
 *
 * The conversion writes a *persisted* preference, which makes this the one journey that changes how the
 * app looks after the capture ends. So it restores what it found, in a `finally`: a failure between the
 * two taps would otherwise hand every later journey -- and whoever picks the device up next -- a shape
 * nobody asked for. See the plan's note on journeys leaving device state behind.
 *
 * The toggle keeps one tag in both shapes, so the *row* tag is what confirms the conversion actually
 * happened; waiting on the toggle again would pass either way.
 */
private fun MacrobenchmarkScope.exerciseGuideSidebarAndRestore() {
    if (!waitForOptionalTestTag(BA_STUDENT_GUIDE_SIDEBAR_TOGGLE, timeoutMs = 8_000)) return
    var converted = false
    try {
        clickTestTag(BA_STUDENT_GUIDE_SIDEBAR_TOGGLE)
        converted = waitForOptionalTestTag(BA_STUDENT_GUIDE_SIDEBAR_ROW_SKILLS, timeoutMs = 8_000)
        if (!converted) return
        // A row tap, so the rail's selection change and the content beside it both compose, and a fling
        // so the inset content scrolls under the rail rather than only appearing beside it.
        clickTestTag(BA_STUDENT_GUIDE_SIDEBAR_ROW_SKILLS)
        device.waitForIdle()
        flingVisibleScrollable(times = 1, horizontalFraction = WIDE_SECONDARY_LANE_X)
    } finally {
        if (converted && waitForOptionalTestTag(BA_STUDENT_GUIDE_SIDEBAR_TOGGLE, timeoutMs = 6_000)) {
            clickTestTag(BA_STUDENT_GUIDE_SIDEBAR_TOGGLE)
            // Confirmed by the row going away, for the same reason the conversion is: the toggle is
            // present either way.
            device.wait(Until.gone(testTagSelector(BA_STUDENT_GUIDE_SIDEBAR_ROW_SKILLS)), 6_000)
            device.waitForIdle()
        }
    }
}

internal fun MacrobenchmarkScope.exerciseWideLanes() {
    flingVisibleScrollable(times = 1, horizontalFraction = WIDE_PRIMARY_LANE_X)
    flingVisibleScrollable(times = 1, horizontalFraction = WIDE_SECONDARY_LANE_X)
}

internal fun MacrobenchmarkScope.flingVisibleScrollable(
    times: Int,
    horizontalFraction: Float = DEFAULT_SCROLL_X,
) {
    val centerX = (device.displayWidth * horizontalFraction).toInt()
    val startY = (device.displayHeight * 0.74f).toInt()
    val endY = (device.displayHeight * 0.34f).toInt()
    repeat(times) {
        device.swipe(centerX, startY, centerX, endY, FLING_STEPS)
        device.waitForIdle()
    }
}

internal fun MacrobenchmarkScope.dragVisibleScrollable(times: Int) {
    val centerX = (device.displayWidth * DEFAULT_SCROLL_X).toInt()
    val startY = (device.displayHeight * 0.68f).toInt()
    val endY = (device.displayHeight * 0.47f).toInt()
    repeat(times) {
        device.swipe(centerX, startY, centerX, endY, DRAG_STEPS)
        device.waitForIdle()
    }
}

private fun MacrobenchmarkScope.nudgeVisibleScrollable(forward: Boolean) {
    val centerX = (device.displayWidth * DEFAULT_SCROLL_X).toInt()
    val upperY = (device.displayHeight * 0.40f).toInt()
    val lowerY = (device.displayHeight * 0.66f).toInt()
    val startY = if (forward) lowerY else upperY
    val endY = if (forward) upperY else lowerY
    device.swipe(centerX, startY, centerX, endY, NUDGE_STEPS)
    device.waitForIdle()
}

internal fun MacrobenchmarkScope.forceWindowSizeDp(
    widthDp: Int,
    heightDp: Int,
) {
    val densityDpi = deviceDensityDpi()
    forceWindowSize(
        widthPx = widthDp * densityDpi / 160,
        heightPx = heightDp * densityDpi / 160,
    )
}

/** Mirrors the large-device (600dp) and two-pane (2 * 380dp) gates for full-screen capture. */
internal fun MacrobenchmarkScope.profileWindowUsesTwoColumns(): Boolean {
    val densityDpi = deviceDensityDpi()
    val widthDp = device.displayWidth * 160 / densityDpi
    val heightDp = device.displayHeight * 160 / densityDpi
    return minOf(widthDp, heightDp) >= LARGE_SCREEN_SMALLEST_WIDTH_DP &&
        widthDp >= TWO_COLUMN_MIN_WIDTH_DP
}

private fun MacrobenchmarkScope.forceWindowSize(
    widthPx: Int,
    heightPx: Int,
) {
    // wm size uses the display's natural orientation, while journey widths are current viewport
    // widths. Native landscape Pads rotate the physical override before reporting window bounds.
    val rotated = device.displayRotation % 2 != 0
    val naturalWidth = if (rotated) heightPx else widthPx
    val naturalHeight = if (rotated) widthPx else heightPx
    device.executeShellCommand("wm size ${naturalWidth}x$naturalHeight")
    device.waitForIdle()
    val deadline = android.os.SystemClock.uptimeMillis() + 5_000
    while ((device.displayWidth != widthPx || device.displayHeight != heightPx) &&
        android.os.SystemClock.uptimeMillis() < deadline
    ) {
        android.os.SystemClock.sleep(100)
    }
    check(device.displayWidth == widthPx && device.displayHeight == heightPx) {
        "Expected ${widthPx}x$heightPx viewport, got ${device.displayWidth}x${device.displayHeight}"
    }
}

private fun MacrobenchmarkScope.deviceDensityDpi(): Int {
    val output = device.executeShellCommand("wm density")
    val override = Regex("Override density: (\\d+)").find(output)?.groupValues?.get(1)?.toIntOrNull()
    val physical = Regex("Physical density: (\\d+)").find(output)?.groupValues?.get(1)?.toIntOrNull()
    return override ?: physical
        ?: error("Could not read the device density from wm density: $output")
}

internal fun MacrobenchmarkScope.readWindowSizeOverride(): String? =
    Regex("""Override size: (\d+x\d+)""")
        .find(device.executeShellCommand("wm size"))?.groupValues?.get(1)

internal fun MacrobenchmarkScope.restoreWindowSize(overrideSize: String?) {
    device.executeShellCommand("wm size ${overrideSize ?: "reset"}")
    device.waitForIdle()
}

internal fun MacrobenchmarkScope.launchHomeFromColdStart() {
    pressHome()
    grantRuntimePermissions()
    val launcherComponent = resolveLauncherComponent()
    device.executeShellCommand("am force-stop ${targetAppId()}")
    device.executeShellCommand(
        "am start -W -a android.intent.action.MAIN " +
            "-c android.intent.category.LAUNCHER -n $launcherComponent",
    )
    waitForTestTag(HOME_PAGE_ROOT, timeoutMs = 15_000)
    // Adaptive captures and an interrupted run can leave the collector's navigation rail saved.
    // Start every replay in the same tab mode before individual journeys choose their own shape.
    if (waitForOptionalTestTag(MAIN_SIDEBAR_ROW_HOME, timeoutMs = 500)) {
        clickTestTag(MAIN_SIDEBAR_TOGGLE)
        waitForTestTag(MAIN_BOTTOM_TAB_HOME, timeoutMs = 15_000)
    }
}

private fun MacrobenchmarkScope.resolveLauncherComponent(): String {
    val output = device.executeShellCommand("cmd package resolve-activity --brief ${targetAppId()}")
    return output
        .lineSequence()
        .map(String::trim)
        .lastOrNull { line -> "/" in line }
        ?: error("Unable to resolve launcher activity for ${targetAppId()}: $output")
}



private const val OPEN_WINDOW_ATTEMPTS = 3
private const val STABLE_TARGET_SAMPLES = 3
private const val TARGET_GEOMETRY_SAMPLE_INTERVAL_MS = 100L
private const val LARGE_SCREEN_SMALLEST_WIDTH_DP = 600
private const val TWO_COLUMN_MIN_WIDTH_DP = 760
private const val GUIDE_PAGER_SWIPE_ATTEMPTS = 3
private const val BGM_PLAYBACK_TIMEOUT_MS = 25_000L
private const val BOTTOM_BAR_REEXPAND_ATTEMPTS = 8
private const val BOTTOM_BAR_EXPAND_TIMEOUT_MS = 3_000L
private const val GESTURE_INJECTION_ATTEMPTS = 2
private const val SCROLL_INTO_REACH_ATTEMPTS = 12
private const val MIN_TAPPABLE_HEIGHT_PX = 40
private const val MAX_HEADER_TAP_INSET_PX = 100
private const val FLING_STEPS = 24
private const val PUSH_ROUTE_MAX_BACK_ATTEMPTS = 2
private const val PUSH_ROUTE_GONE_TIMEOUT_MS = 3_000L
private const val DRAG_STEPS = 72
private const val NUDGE_STEPS = 36
private const val SCROLL_SAFE_TOP_FRACTION = 0.18f
private const val SCROLL_SAFE_BOTTOM_FRACTION = 0.82f
private const val DEFAULT_SCROLL_X = 0.50f
private const val WIDE_PRIMARY_LANE_X = 0.32f
private const val WIDE_SECONDARY_LANE_X = 0.74f
private const val PAGER_SWIPE_NEAR_X = 0.22f
private const val PAGER_SWIPE_FAR_X = 0.78f
private const val PAGER_SWIPE_Y = 0.55f
private const val LIQUID_SHEET_DRAG_DISTANCE_FRACTION = 0.14f
private const val LIQUID_SHEET_CONTENT_UPPER_FRACTION = 0.34f
private const val LIQUID_SHEET_CONTENT_LOWER_FRACTION = 0.82f
