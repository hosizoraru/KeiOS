package os.kei.ui.testing

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import os.kei.ui.page.main.student.GuideBottomTab
import os.kei.ui.page.main.student.page.component.GuideSidebarToggleTestTag
import os.kei.ui.page.main.student.page.component.guideSidebarRowTestTag
import os.kei.ui.page.main.widget.chrome.tabbedPageCategoryTabTestTag

/**
 * The macrobenchmark module cannot depend on the app's source set, so its profile and measurement
 * sources re-spell every test tag as a string constant. A drift between the two spellings fails
 * only on a device, minutes into a profile run, as `Timed out waiting for testTag=…`. This walks
 * the same ground in a second.
 */
class BaselineProfileTestTagContractTest {
    @Test
    fun everyTagTheProfileSourcesWaitForIsDeclaredInTheApp() {
        val declared = keiOsTestTagValues() + componentOwnedTagValues()
        val profileTags = profileTagConstants()

        assertTrue(profileTags.isNotEmpty(), "Unable to parse tag constants out of the profile sources")
        profileTags.forEach { (constant, value) ->
            assertTrue(
                value in declared,
                "$constant = \"$value\" matches no declared tag; a profile task would time out",
            )
        }
    }

    @Test
    fun theOverlayLayerStillPublishesTagsAsResourceIds() {
        // The overlay layer is a sibling of the page content, so it inherits nothing from a page root.
        // Without this line every tag inside a sheet, alert, action sheet or menu is invisible to
        // UiAutomator — which is how LiquidSheetPanelTestTag came to exist for the baseline profile and
        // never resolve. Removing it would put every presentation journey back to timing out.
        val source = repoSource(SCENE_BACKDROP_HOST)

        assertTrue(
            "testTagsAsResourceId = true" in source,
            "$SCENE_BACKDROP_HOST must publish overlay tags or every sheet journey goes blind",
        )
    }

    @Test
    fun everyPageRootTagIsAppliedThroughTheSharedModifier() {
        // testTagsAsResourceId is what publishes a tag to UiAutomator, and pageRootTestTag is the
        // only place that pairs the two. A page root that reaches for a bare testTag is invisible.
        PAGE_ROOT_SOURCES.forEach { (relativePath, tag) ->
            val source = repoSource(relativePath)

            assertTrue(
                "pageRootTestTag(KeiOsTestTags.$tag)" in source,
                "$relativePath must tag its root through pageRootTestTag",
            )
        }
    }

    @Test
    fun theSharedModifierStillPublishesTagsAsResourceIds() {
        val source = repoSource("app/src/main/java/os/kei/ui/testing/PageRootTestTag.kt")

        assertTrue(
            "testTagsAsResourceId = true" in source,
            "pageRootTestTag must keep setting testTagsAsResourceId or every journey goes blind",
        )
    }

    /**
     * A journey helper that nothing calls collects nothing, and says nothing about it.
     *
     * `launchDailyTemplateFromTileLongPress` and `discardTheOpenSheetsEdit` were written, documented as
     * covering the tile template editor, and never wired into a `@Test`. They sat uncalled through two
     * profile captures. Kotlin warns about an unused private function; the build does not fail on
     * warnings, and a profile run cannot fail on a journey that does not exist.
     *
     * Comments are stripped before counting, because a KDoc reference to the helper is exactly what made
     * the gap look wired.
     */
    @Test
    fun theGeneratorCallsEveryHelperItDeclares() {
        val source = generatorSourceWithoutComments()
        val helpers = SCOPED_HELPER.findAll(source).map { match -> match.groupValues[1] }.toList()

        assertTrue(helpers.isNotEmpty(), "Unable to parse journey helpers out of the generator")
        helpers.forEach { helper ->
            val calls = Regex("""\b$helper\s*\(""").findAll(source).count()
            assertTrue(
                calls > 1,
                "$helper is declared and never called, so whatever it was meant to collect is missing",
            )
        }
    }

    /** The shared helpers, the same way: declared once for both callers, so one of them must call each. */
    @Test
    fun everySharedJourneyHelperIsCalled() {
        val callers = PROFILE_CALLER_SOURCES.joinToString("\n") { sourceWithoutComments(it) }
        val helpers =
            SHARED_HELPER.findAll(sourceWithoutComments(JOURNEY_SUPPORT_SOURCE))
                .map { match -> match.groupValues[1] }
                .toList()

        assertTrue(helpers.isNotEmpty(), "Unable to parse helpers out of $JOURNEY_SUPPORT_SOURCE")
        helpers.forEach { helper ->
            // More than one: the declaration itself is in these sources and matches too.
            assertTrue(
                Regex("""\b$helper\s*\(""").findAll(callers).count() > 1,
                "$helper is shared and nothing calls it",
            )
        }
    }

    /** The same silence, one step earlier: a tag no journey or benchmark ever waits for. */
    @Test
    fun everySharedTagIsUsed() {
        val callers = PROFILE_CALLER_SOURCES.joinToString("\n") { sourceWithoutComments(it) }
        val tags =
            CONST_DECLARATION.findAll(repoSource(PROFILE_TAGS_SOURCE)).map { match -> match.groupValues[1] }.toList()

        assertTrue(tags.isNotEmpty(), "Unable to parse tags out of $PROFILE_TAGS_SOURCE")
        tags.forEach { tag ->
            assertTrue(
                Regex("""\b$tag\b""").containsMatchIn(callers),
                "$tag is declared and never used, so the path it names is uncovered",
            )
        }
    }

    @Test
    fun theGeneratorUsesEveryConstantItDeclares() {
        val source = generatorSourceWithoutComments()
        val constants = generatorConstants().map { (constant, _) -> constant }

        assertTrue(constants.isNotEmpty(), "Unable to parse constants out of the generator")
        constants.forEach { constant ->
            val references = Regex("""\b$constant\b""").findAll(source).count()
            assertTrue(
                references > 1,
                "$constant is declared and never used, so the path it names is uncovered",
            )
        }
    }

    @Test
    fun theDefaultProfileStaysDeterministic() {
        val source = generatorSourceWithoutComments()

        FORBIDDEN_PROFILE_FIXTURES.forEach { token ->
            assertTrue(
                token !in source,
                "The default profile contains $token; keep network and package-state fixtures in tests",
            )
        }
    }

    @Test
    fun startupOwnsTheStartupProfileAndIncludesAFirstScroll() {
        val source = generatorSourceWithoutComments()
        val startupJourney =
            requireNotNull(
                Regex(
                    """fun startupAndFirstScroll\(\).*?(?=\n\s*@Test|\n})""",
                    RegexOption.DOT_MATCHES_ALL,
                ).find(source),
            ).value

        assertEquals(1, Regex("""includeInStartupProfile\s*=\s*true""").findAll(source).count())
        assertTrue("flingVisibleScrollable" in startupJourney)
    }

    @Test
    fun theDefaultCollectionKeepsSixJourneysAndSixteenReplays() {
        val source = generatorSourceWithoutComments()
        val limits = generatorConstants().associate { (name, value) -> name to value.toIntOrNull() }
        val journeys = Regex(
            """@Test\s+fun (\w+)\(\)\s*\{\s*rule\.collect\((.*?)\)\s*\{""",
            RegexOption.DOT_MATCHES_ALL,
        ).findAll(source).toList()
        assertEquals(
            setOf("startupAndFirstScroll", "mainPagesAndNavigation", "commonRoutesAndChrome",
                "gitHubTrackingCore", "baOfficeAndCatalogCore", "adaptiveLargeScreenCore"),
            journeys.map { it.groupValues[1] }.toSet(),
        )
        assertEquals(6, journeys.size)
        var totalMaxReplays = 0
        journeys.forEach { journey ->
            val arguments = journey.groupValues[2]
            fun limit(argument: String): Int {
                val constant = requireNotNull(Regex("$argument\\s*=\\s*(\\w+)").find(arguments))
                    .groupValues[1]
                return requireNotNull(limits[constant]) { "Missing numeric replay limit: $constant" }
            }
            val max = limit("maxIterations")
            val stable = limit("stableIterations")
            assertTrue(stable in 2..max, "Invalid replay limits for ${journey.groupValues[1]}")
            totalMaxReplays += max
        }
        assertEquals(16, totalMaxReplays)
    }

    @Test
    fun startupHasAnExplicitFullyDrawnSignal() {
        val source = repoSource(MAIN_PAGER_PAGE_HOST)

        assertTrue("import androidx.activity.compose.ReportDrawn" in source)
        assertTrue("ReportDrawn()" in source)
    }

    /**
     * The derived tab tags have to spell what the declared constants say.
     *
     * `TabbedPageBottomChrome` is generic over its categories, so it builds each tab's tag from the
     * page's `labelPrefix` and the tab index rather than taking a list of tags it cannot see. That
     * means the literals in [KeiOsTestTags] are a second spelling of a format defined elsewhere, and
     * changing the format would leave them pointing at nothing — failing, as ever, minutes into a
     * profile run rather than here.
     */
    @Test
    fun theDerivedTabbedPageTagsMatchTheDeclaredOnes() {
        assertEquals(KeiOsTestTags.SettingsTabInterface, tabbedPageCategoryTabTestTag("settings", 2))
        assertEquals(KeiOsTestTags.GitHubHistoryTabRefresh, tabbedPageCategoryTabTestTag("github_history", 0))
        assertEquals(KeiOsTestTags.GitHubHistoryTabActions, tabbedPageCategoryTabTestTag("github_history", 1))
        assertEquals(KeiOsTestTags.GitHubHistoryTabTracking, tabbedPageCategoryTabTestTag("github_history", 2))
        assertEquals(KeiOsTestTags.GitHubHistoryTabApps, tabbedPageCategoryTabTestTag("github_history", 3))
        assertEquals(KeiOsTestTags.AboutTabLab, tabbedPageCategoryTabTestTag("about", 3))
        assertEquals(
            KeiOsTestTags.BaCalendarPoolTabCalendar,
            tabbedPageCategoryTabTestTag("ba_calendar_pool", 0),
        )
        assertEquals(
            KeiOsTestTags.BaCalendarPoolTabPool,
            tabbedPageCategoryTabTestTag("ba_calendar_pool", 1),
        )
    }

    /**
     * And the guide's rail rows, which derive from the bottom bar's tags rather than being declared.
     *
     * The adaptive journey taps one to prove the conversion happened -- the toggle keeps one tag in both
     * shapes, so it cannot tell them apart. A drift here would make that journey silently skip the rail
     * and quietly stop compiling it.
     */
    @Test
    fun theDerivedGuideSidebarRowTagsMatchTheDeclaredOnes() {
        assertEquals(
            KeiOsTestTags.BaStudentGuideSidebarRowSkills,
            guideSidebarRowTestTag(GuideBottomTab.Skills),
        )
        assertEquals(KeiOsTestTags.BaStudentGuideSidebarToggle, GuideSidebarToggleTestTag)
    }

    /**
     * And the prefixes have to be the ones those pages actually pass.
     *
     * A tag derived from "about" is worthless if the About page stops calling itself that, and the
     * equality above would still hold.
     */
    @Test
    fun thePagesStillPassThePrefixesThoseTagsAssume() {
        assertTrue(
            """labelPrefix = "about"""" in repoSource(ABOUT_BOTTOM_CHROME),
            "$ABOUT_BOTTOM_CHROME must keep the prefix KeiOsTestTags.AboutTabLab is spelled from",
        )
        assertTrue(
            """labelPrefix = "github_history"""" in repoSource(GITHUB_HISTORY_PAGE),
            "$GITHUB_HISTORY_PAGE must keep the prefix the GitHubHistoryTab* tags are spelled from",
        )
        assertTrue(
            """labelPrefix = "ba_calendar_pool"""" in repoSource(BA_CALENDAR_POOL_PAGE),
            "$BA_CALENDAR_POOL_PAGE must keep the prefix the BaCalendarPoolTab* tags are spelled from",
        )
    }

    @Test
    fun tagValuesAreUnique() {
        val values = keiOsTestTagValues()

        assertEquals(values.size, values.toSet().size, "Two KeiOsTestTags entries share a value")
    }
}

private fun keiOsTestTagValues(): List<String> =
    CONST_DECLARATION
        .findAll(repoSource("app/src/main/java/os/kei/ui/testing/KeiOsTestTags.kt"))
        .map { match -> match.groupValues[2] }
        .toList()

/**
 * Tags a ui-liquid-glass component owns rather than a page.
 *
 * A journey waiting for "any sheet" or "any menu" cannot name a page's tag, and copying these into
 * [KeiOsTestTags] would leave two spellings to drift apart — the exact failure this file exists to
 * prevent.
 */
private fun componentOwnedTagValues(): List<String> =
    COMPONENT_TAG_SOURCES.flatMap { relativePath ->
        CONST_DECLARATION.findAll(repoSource(relativePath)).map { match -> match.groupValues[2] }
    }

private val COMPONENT_TAG_SOURCES =
    listOf(
        "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/sheet/LiquidSheet.kt",
        "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/sheet/MiuixSnapshotAdapters.kt",
        "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/sheet/UnsavedSheetDismiss.kt",
        "app/src/main/java/os/kei/ui/page/main/student/model3d/GuideModel3dPlayer.kt",
        "app/src/main/java/os/kei/ui/page/main/student/section/gallery/GuideWebMemoryLobbyChrome.kt",
    )

private const val BA_CALENDAR_POOL_PAGE =
    "app/src/main/java/os/kei/ui/page/main/ba/BaCalendarPoolPage.kt"

private const val SCENE_BACKDROP_HOST =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/sheet/SceneBackdropScope.kt"

/**
 * The tag constants in the profile sources.
 *
 * Filtered to tag-shaped values (snake_case identifiers), so a string constant that is not a tag -- a
 * platform class name, a shell argument -- is not mistaken for one and failed against the app's tags.
 */
private fun profileTagConstants(): List<Pair<String, String>> =
    PROFILE_SOURCE_FILES.flatMap { relativePath ->
        CONST_DECLARATION
            .findAll(repoSource(relativePath))
            .map { match -> match.groupValues[1] to match.groupValues[2] }
            .filter { (_, value) -> TAG_SHAPED.matches(value) }
    }

/** Every constant the generator declares for itself: gesture fractions, step counts and timeouts. */
private fun generatorConstants(): List<Pair<String, String>> =
    Regex("""const val (\w+)\s*=\s*([^\n]+)""")
        .findAll(GENERATOR_SOURCE_FILES.joinToString("\n") { repoSource(it) })
        .map { match -> match.groupValues[1] to match.groupValues[2] }
        .toList()

private val TAG_SHAPED = Regex("""[a-z0-9_]+""")

private val SCOPED_HELPER = Regex("""(?:private|internal) fun MacrobenchmarkScope\.(\w+)\s*\(""")

/**
 * The generator with comments removed.
 *
 * A KDoc mention reads as a use to any plain text search, which is how an uncalled helper stayed
 * plausible through two captures.
 */
private fun generatorSourceWithoutComments(): String =
    GENERATOR_SOURCE_FILES.joinToString("\n") { sourceWithoutComments(it) }

private fun sourceWithoutComments(relativePath: String): String =
    repoSource(relativePath)
        .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("""//[^\n]*"""), "")

private val SHARED_HELPER = Regex("""internal fun (?:MacrobenchmarkScope\.)?(\w+)\s*\(""")

private const val GENERATOR_SOURCE =
    "baselineprofile/src/main/java/os/kei/baselineprofile/BaselineProfileGenerator.kt"

private const val GENERATOR_ACTIONS_SOURCE =
    "baselineprofile/src/main/java/os/kei/baselineprofile/BaselineProfileActions.kt"

private val GENERATOR_SOURCE_FILES = listOf(GENERATOR_SOURCE, GENERATOR_ACTIONS_SOURCE)

private const val MAIN_NAVIGATION_BENCHMARK_SOURCE =
    "baselineprofile/src/main/java/os/kei/baselineprofile/MainNavigationFrameBenchmarks.kt"

private const val STARTUP_BENCHMARK_SOURCE =
    "baselineprofile/src/main/java/os/kei/baselineprofile/StartupBenchmarks.kt"

/** Every tag either caller waits for, spelled once for both. */
private const val PROFILE_TAGS_SOURCE =
    "baselineprofile/src/main/java/os/kei/baselineprofile/ProfileTags.kt"

private const val JOURNEY_SUPPORT_SOURCE =
    "baselineprofile/src/main/java/os/kei/baselineprofile/ProfileJourneySupport.kt"

/** The files that drive the app; the shared tags and helpers must each be used by one of them. */
private val PROFILE_CALLER_SOURCES =
    listOf(
        GENERATOR_SOURCE,
        GENERATOR_ACTIONS_SOURCE,
        "baselineprofile/src/main/java/os/kei/baselineprofile/GuideViewerProfileActions.kt",
        MAIN_NAVIGATION_BENCHMARK_SOURCE,
        STARTUP_BENCHMARK_SOURCE,
        JOURNEY_SUPPORT_SOURCE,
    )

private val PROFILE_SOURCE_FILES = PROFILE_CALLER_SOURCES + PROFILE_TAGS_SOURCE

private val CONST_DECLARATION = Regex("""const val (\w+)\s*(?:=\s*)?\n?\s*"([^"]+)"""")
private val FORBIDDEN_PROFILE_FIXTURES =
    listOf(
        "ServerSocket",
        "http://",
        "https://",
        "pm hide",
        "pm unhide",
        "ACTION_SEND",
    )

private val PAGE_ROOT_SOURCES =
    listOf(
        "app/src/main/java/os/kei/ui/page/main/settings/page/SettingsPage.kt" to "SettingsPageRoot",
        "app/src/main/java/os/kei/ui/page/main/about/page/AboutPage.kt" to "AboutPageRoot",
        "app/src/main/java/os/kei/ui/page/main/sync/WebDavSyncPage.kt" to "WebDavSyncPageRoot",
        "app/src/main/java/os/kei/ui/page/main/mcp/skill/page/McpSkillPage.kt" to "McpSkillPageRoot",
        "app/src/main/java/os/kei/ui/page/main/os/shell/page/OsShellRunnerContent.kt" to "OsShellRunnerPageRoot",
        "app/src/main/java/os/kei/ui/page/main/github/history/GitHubActionsNotificationHistoryPage.kt"
            to "GitHubActionsHistoryPageRoot",
        "app/src/main/java/os/kei/ui/page/main/student/catalog/page/BaGuideCatalogPageContent.kt"
            to "BaGuideCatalogPageRoot",
        "app/src/main/java/os/kei/ui/page/main/student/page/BaStudentGuidePage.kt"
            to "BaStudentGuidePageRoot",
        "app/src/main/java/os/kei/ui/page/main/jsonimport/KeiOSJsonImportPage.kt"
            to "JsonImportPageRoot",
    )

private const val ABOUT_BOTTOM_CHROME =
    "app/src/main/java/os/kei/ui/page/main/about/page/AboutBottomChrome.kt"

private const val GITHUB_HISTORY_PAGE =
    "app/src/main/java/os/kei/ui/page/main/github/history/GitHubActionsNotificationHistoryPage.kt"

private const val MAIN_PAGER_PAGE_HOST =
    "app/src/main/java/os/kei/ui/page/main/host/pager/MainPagerPageHost.kt"
