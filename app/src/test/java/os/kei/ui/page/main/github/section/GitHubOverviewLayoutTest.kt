package os.kei.ui.page.main.github.section

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.R
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubLookupStrategyOption
import os.kei.ui.page.main.github.OverviewRefreshState
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = Application::class,
    sdk = [35],
    qualifiers = "zh-rCN-w411dp-h891dp-xxhdpi",
)
class GitHubOverviewLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Application
        get() = ApplicationProvider.getApplicationContext()

    @Test
    @Config(
        application = Application::class,
        sdk = [35],
        qualifiers = "zh-rCN-w375dp-h817dp-520dpi",
    )
    fun overviewPillsStayOnTwoInformationRowsAtPhysicalDeviceWidth() {
        assertOverviewPillsStayOnTwoRows()
    }

    @Test
    @Config(
        application = Application::class,
        sdk = [35],
        qualifiers = "zh-rCN-w375dp-h817dp-520dpi",
    )
    fun atomRefreshingHeaderStaysOnOneLineAtPhysicalDeviceWidth() {
        assertOverviewPillsStayOnTwoRows(
            lookupStrategy = GitHubLookupStrategyOption.AtomFeed,
            refreshState = OverviewRefreshState.Refreshing,
            lastRefreshMs = 0L,
        )
    }

    @Test
    @Config(qualifiers = "en-rUS-w375dp-h817dp-520dpi")
    fun englishOverviewKeepsItsTitleAndMetricsOnTwoRows() {
        val checkedAt = System.currentTimeMillis() - 94 * 60_000L - 30_000L
        assertOverviewPillsStayOnTwoRows(
            lookupStrategy = GitHubLookupStrategyOption.AtomFeed,
            lastRefreshMs = checkedAt,
            incrementalRefreshMs = checkedAt,
        )
    }

    @Test
    @Config(qualifiers = "ja-rJP-w375dp-h817dp-520dpi")
    fun japaneseOverviewKeepsItsTitleAndMetricsOnTwoRows() {
        val checkedAt = System.currentTimeMillis() - 94 * 60_000L - 30_000L
        assertOverviewPillsStayOnTwoRows(
            lookupStrategy = GitHubLookupStrategyOption.AtomFeed,
            lastRefreshMs = checkedAt,
            incrementalRefreshMs = checkedAt,
        )
    }

    private fun assertOverviewPillsStayOnTwoRows(
        lookupStrategy: GitHubLookupStrategyOption = GitHubLookupStrategyOption.GitHubApiToken,
        refreshState: OverviewRefreshState = OverviewRefreshState.Completed,
        lastRefreshMs: Long = System.currentTimeMillis() - 3_600_000L,
        incrementalRefreshMs: Long = System.currentTimeMillis() - 30 * 60_000L,
    ) {
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    GitHubOverviewCard(
                        isDark = false,
                        lookupConfig =
                            GitHubLookupConfig(
                                selectedStrategy = lookupStrategy,
                                apiToken = "github_pat_example_token",
                            ),
                        overviewRefreshState = refreshState,
                        refreshProgress = 1f,
                        lastRefreshMs = lastRefreshMs,
                        metrics =
                            GitHubOverviewMetrics(
                                trackedCount = 77,
                                stableUpdateCount = 36,
                                totalUpdatableCount = 44,
                                stableLatestCount = 36,
                                preReleaseCount = 8,
                                preReleaseUpdateCount = 8,
                                failedCount = 0,
                                latestCheckedAtMillis = incrementalRefreshMs,
                            ),
                        failedFilterActive = false,
                        onRetryFailedTracked = {},
                        onFailedFilterToggle = {},
                        onAddTracked = {},
                    )
                }
            }
        }

        composeRule.waitForIdle()

        val title = boundsFor(context.getString(R.string.github_overview_title))
        val tracked = boundsFor("77")
        val mode =
            boundsFor(
                context.getString(
                    when (lookupStrategy) {
                        GitHubLookupStrategyOption.AtomFeed -> R.string.github_overview_strategy_atom
                        GitHubLookupStrategyOption.GitHubApiToken -> R.string.github_overview_strategy_api
                    },
                ),
            )
        val stable = boundsFor(context.getString(R.string.github_overview_pill_stable_pair, 36, 72))
        val preRelease = boundsFor(context.getString(R.string.github_overview_pill_prerelease_pair, 8, 8))
        val failed = boundsFor(context.getString(R.string.github_overview_pill_failed, 0))
        val incrementalTime = boundsForContentDescription(
            context.getString(
                R.string.github_overview_incremental_refresh_time,
                os.kei.ui.page.main.github.formatRefreshAgo(context, incrementalRefreshMs),
            ),
        )
        val fullTime = boundsForContentDescription(
            context.getString(
                R.string.github_overview_full_refresh_time,
                os.kei.ui.page.main.github.formatRefreshAgo(context, lastRefreshMs),
            ),
        )
        val tolerancePx = with(composeRule.density) { 2.dp.toPx() }
        val maxSingleLineTitleHeightPx = with(composeRule.density) { 28.dp.toPx() }

        assertVerticallyCentered(title, mode, tolerancePx)
        assertVerticallyCentered(title, incrementalTime, tolerancePx)
        assertVerticallyCentered(title, fullTime, tolerancePx)
        assertSameRow(tracked, stable, tolerancePx)
        assertSameRow(stable, preRelease, tolerancePx)
        assertSameRow(stable, failed, tolerancePx)
        assertTrue(
            actual = tracked.top > title.bottom,
            message = "Expected tracked-count and metric pills below the title and mode pill",
        )
        assertTrue(
            actual = title.height <= maxSingleLineTitleHeightPx,
            message = "Expected a single-line title, title=$title",
        )

        val rowCenters =
            listOf(title, mode, incrementalTime, fullTime, tracked, stable, preRelease, failed)
                .map { bounds -> bounds.center.y }
        val distinctRows =
            rowCenters.fold(mutableListOf<Float>()) { rows, centerY ->
                if (rows.none { existing -> abs(existing - centerY) <= tolerancePx }) rows += centerY
                rows
            }
        assertEquals(2, distinctRows.size)
    }

    private fun boundsFor(text: String, substring: Boolean = false): Rect =
        composeRule
            .onNodeWithText(text, substring = substring, useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot

    private fun boundsForContentDescription(description: String): Rect =
        composeRule
            .onNodeWithContentDescription(description, useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot

    private fun assertSameRow(first: Rect, second: Rect, tolerancePx: Float) {
        assertTrue(
            actual = abs(first.top - second.top) <= tolerancePx,
            message = "Expected the pills on the same row, first=$first second=$second",
        )
    }

    private fun assertVerticallyCentered(first: Rect, second: Rect, tolerancePx: Float) {
        assertTrue(
            actual = abs(first.center.y - second.center.y) <= tolerancePx,
            message = "Expected elements on the same center line, first=$first second=$second",
        )
    }
}
