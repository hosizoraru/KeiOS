package os.kei.appwidget.github

import android.app.Application
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.core.graphics.ColorUtils
import java.io.File
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.MainActivity
import os.kei.R

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GitHubWidgetRenderingTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val snapshot = GitHubWidgetSnapshot(
        available = true, trackedCount = 12, updateCount = 2,
        uncheckedCount = 3, lastCheckedAtMillis = 1793000000000L,
        pendingUpdates = listOf(GitHubWidgetPendingUpdate("KeiOS", "v1.19.0"), GitHubWidgetPendingUpdate("Miuix", "v0.9.4-rc1", true)),
        projects = listOf(
            GitHubWidgetProject("KeiOS", GitHubWidgetProjectStatus.Update, "v1.19.0", localVersion = "1.18.0"),
            GitHubWidgetProject("Miuix", GitHubWidgetProjectStatus.Update, "v0.9.4-rc1", true, "0.9.3"),
            GitHubWidgetProject("Awaiting check", GitHubWidgetProjectStatus.Unchecked),
            GitHubWidgetProject("A longer project name", GitHubWidgetProjectStatus.Unchecked),
            GitHubWidgetProject("Another project", GitHubWidgetProjectStatus.Unchecked),
            GitHubWidgetProject("Toolbox", GitHubWidgetProjectStatus.NoUpdate),
        ),
    )

    @Test
    fun `responsive layouts keep counts details and the check target within their bounds`() {
        data class Case(val name: String, val width: Int, val height: Int, val variant: GitHubWidgetLayout,
                        val language: String = "zh", val scale: Float = 1f, val dark: Boolean = false,
                        val names: Boolean = true)
        val cases = listOf(
            Case("strip", 160, 72, GitHubWidgetLayout.Strip),
            Case("strip-healthy", 160, 72, GitHubWidgetLayout.Strip, "en"),
            Case("strip-minimum", 110, 72, GitHubWidgetLayout.Strip, "en"),
            Case("strip-large", 160, 72, GitHubWidgetLayout.Strip, "en", 1.5f),
            Case("banner", 320, 72, GitHubWidgetLayout.Banner),
            Case("banner-minimum", 240, 72, GitHubWidgetLayout.Banner, "en"),
            Case("banner-large", 320, 88, GitHubWidgetLayout.Banner, "en", 1.5f),
            Case("compact", 140, 160, GitHubWidgetLayout.Compact),
            Case("compact-minimum", 110, 152, GitHubWidgetLayout.Compact, "en"),
            Case("wide", 320, 160, GitHubWidgetLayout.Board, "en"),
            Case("wide-minimum", 300, 160, GitHubWidgetLayout.Board, "en"),
            Case("wide-medium-font", 320, 160, GitHubWidgetLayout.Board, "zh", 1.15f, true),
            Case("wide-short", 320, 144, GitHubWidgetLayout.BoardCompact, "en"),
            Case("wide-short-large", 320, 176, GitHubWidgetLayout.BoardCompact, "en", 1.5f),
            Case("wide-narrow", 240, 144, GitHubWidgetLayout.Wide, "en"),
            Case("wide-short-private", 240, 144, GitHubWidgetLayout.Wide, "en", names = false),
            Case("wide-short-private-large", 320, 160, GitHubWidgetLayout.Wide, "en", 1.5f, names = false),
            Case("large-font", 320, 192, GitHubWidgetLayout.Board, "zh", 1.5f),
            Case("wide-private", 320, 160, GitHubWidgetLayout.Board, names = false),
            Case("wide-private-large", 320, 192, GitHubWidgetLayout.Board, "en", 1.5f, names = false),
            Case("detailed-narrow", 240, 220, GitHubWidgetLayout.Detailed, "en"),
            Case("detailed-narrow-large", 240, 220, GitHubWidgetLayout.Detailed, "ja", 1.5f),
            Case("compact-large", 160, 152, GitHubWidgetLayout.Compact, "en", 1.5f),
            Case("tall", 160, 336, GitHubWidgetLayout.TallExpanded),
            Case("tall-minimum", 110, 336, GitHubWidgetLayout.TallExpanded, "en"),
            Case("tall-medium", 160, 268, GitHubWidgetLayout.Tall),
            Case("tall-medium-large", 160, 320, GitHubWidgetLayout.Tall, "en", 1.5f),
            Case("tall-large", 160, 384, GitHubWidgetLayout.TallExpanded, "ja", 1.5f),
            Case("tall-private", 160, 336, GitHubWidgetLayout.TallExpanded, "en", names = false),
            Case("wide-dark", 520, 220, GitHubWidgetLayout.Expanded, "ja", dark = true),
            Case("expanded-large", 320, 220, GitHubWidgetLayout.Expanded, "ja", 1.5f, true),
            Case("expanded-private", 320, 220, GitHubWidgetLayout.Expanded, "ja", 1.5f, true, false),
        )
        val output = File("build/reports/github-widget").apply { mkdirs() }
        val failures = mutableListOf<String>()
        cases.forEach { case ->
            try {
                val resources = Configuration(context.resources.configuration).apply {
                    setLocale(Locale.forLanguageTag(case.language))
                    fontScale = case.scale
                    uiMode = if (case.dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
                val themed = context.createConfigurationContext(resources)
                val content = when {
                    case.name == "strip-healthy" -> snapshot.copy(uncheckedCount = 0)
                    else -> snapshot
                }
                val root = GitHubWidgetRenderer.layout(
                    themed, 10, content, GitHubWidgetConfiguration(case.names), case.variant, interactive = false,
                ).apply(themed, FrameLayout(themed))
                root.measure(
                    View.MeasureSpec.makeMeasureSpec(case.width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(case.height, View.MeasureSpec.EXACTLY),
                )
                root.layout(0, 0, case.width, case.height)
                val image = Bitmap.createBitmap(case.width, case.height, Bitmap.Config.ARGB_8888)
                root.draw(Canvas(image))
                File(output, "${case.name}.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
                val count = root.findViewById<TextView>(R.id.github_widget_count)
                assertEquals(content.updateCount.toString(), count.text.toString())
                val rect = Rect()
                assertTrue(count.getLocalVisibleRect(rect), "${case.name}: count is invisible")
                assertTrue(
                    rect.height() >= count.lineHeight,
                    "${case.name}: count is clipped (${rect.height()} < ${count.lineHeight})",
                )
                val label = root.findViewById<TextView>(R.id.github_widget_updates_label)
                assertTrue(label.getLocalVisibleRect(rect) && rect.height() >= label.lineHeight, "${case.name}: update label clipped")
                if (case.variant !in listOf(GitHubWidgetLayout.Strip, GitHubWidgetLayout.Banner, GitHubWidgetLayout.Compact)) {
                    val check = root.findViewById<TextView>(R.id.github_widget_refresh)
                    assertEquals(48, check.height, "${case.name}: minimum check target")
                    assertTrue(check.width >= 48, "${case.name}: check target too narrow")
                    assertTrue(check.getLocalVisibleRect(rect) && rect.height() == check.height, "${case.name}: check target clipped")
                    if (case.names) {
                        val row = root.findViewById<View>(R.id.github_widget_project_row_1)
                        assertTrue(row.getLocalVisibleRect(rect) && rect.height() == row.height, "${case.name}: first project clipped")
                        assertTrue(root.findViewById<TextView>(R.id.github_widget_project_version_1).text.isNotEmpty())
                        val lastId = when (case.variant) {
                            GitHubWidgetLayout.Wide -> R.id.github_widget_project_row_2
                            GitHubWidgetLayout.BoardCompact, GitHubWidgetLayout.Board -> R.id.github_widget_project_row_4
                            GitHubWidgetLayout.Tall, GitHubWidgetLayout.Detailed -> if (case.scale > 1.3f) R.id.github_widget_project_row_3 else R.id.github_widget_project_row_4
                            else -> if (case.scale > 1.3f) R.id.github_widget_project_row_4 else R.id.github_widget_project_row_6
                        }
                        val last = root.findViewById<View>(lastId)
                        assertTrue(last.getLocalVisibleRect(rect) && rect.height() == last.height, "${case.name}: last project clipped")
                        val lastMetadata = (last as ViewGroup).getChildAt(1) as TextView
                        assertTrue(
                            lastMetadata.getLocalVisibleRect(rect) && rect.height() >= lastMetadata.lineHeight,
                            "${case.name}: last project metadata clipped (${rect.height()} < ${lastMetadata.lineHeight})",
                        )
                        if (case.variant in listOf(GitHubWidgetLayout.BoardCompact, GitHubWidgetLayout.Board, GitHubWidgetLayout.Expanded)) {
                            val first = root.findViewById<View>(R.id.github_widget_project_row_1)
                            val second = root.findViewById<View>(R.id.github_widget_project_row_2)
                            assertTrue(second.left >= first.right, "${case.name}: project groups must use separate columns")
                            assertEquals(first.top, second.top, "${case.name}: paired projects share a baseline")
                            val title = root.findViewById<TextView>(R.id.github_widget_project_name_1)
                            assertTrue(title.getLocalVisibleRect(rect) && rect.height() >= title.lineHeight, "${case.name}: project title clipped")
                            if (case.scale <= 1.1f && case.variant != GitHubWidgetLayout.BoardCompact) {
                                val installed = root.findViewById<TextView>(R.id.github_widget_project_detail_1)
                                assertTrue(installed.getLocalVisibleRect(rect) && rect.height() >= installed.lineHeight,
                                    "${case.name}: installed version clipped")
                            }
                        }
                    } else {
                        val value = root.findViewById<TextView>(R.id.github_widget_metric_total)
                        assertEquals("12", value.text.toString())
                        assertTrue(value.getLocalVisibleRect(rect) && rect.height() >= value.lineHeight, "${case.name}: metrics clipped")
                        val lastLabel = root.findViewById<TextView>(R.id.github_widget_metric_label_paused)
                        assertTrue(lastLabel.getLocalVisibleRect(rect) && rect.height() >= lastLabel.lineHeight, "${case.name}: last metric clipped")
                    }
                }
            } catch (error: AssertionError) {
                failures += "${case.name}: ${error.message}"
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun `reapplying privacy settings removes names from an existing content view`() {
        val shown = GitHubWidgetRenderer.layout(context, 10, snapshot, GitHubWidgetConfiguration(true), GitHubWidgetLayout.Expanded)
        val oldRoot = shown.apply(context, FrameLayout(context))
        assertEquals(View.VISIBLE, oldRoot.findViewById<ViewGroup>(R.id.github_widget_names).visibility)
        assertTrue(oldRoot.contentDescription.contains("KeiOS"))
        val hidden = GitHubWidgetRenderer.layout(
            context, 10, snapshot, GitHubWidgetConfiguration(false), GitHubWidgetLayout.Expanded,
        )
        hidden.reapply(context, oldRoot)
        val root = oldRoot
        val names = root.findViewById<ViewGroup>(R.id.github_widget_names)
        assertEquals(View.GONE, names.visibility)
        listOf(
            R.id.github_widget_project_name_1, R.id.github_widget_project_name_2, R.id.github_widget_project_name_3,
            R.id.github_widget_project_name_4, R.id.github_widget_project_name_5, R.id.github_widget_project_name_6,
            R.id.github_widget_project_version_1, R.id.github_widget_project_version_2, R.id.github_widget_project_version_3,
            R.id.github_widget_project_version_4, R.id.github_widget_project_version_5, R.id.github_widget_project_version_6,
            R.id.github_widget_project_detail_1, R.id.github_widget_project_detail_2, R.id.github_widget_project_detail_3,
            R.id.github_widget_project_detail_4, R.id.github_widget_project_detail_5, R.id.github_widget_project_detail_6,
        ).forEach { assertEquals("", root.findViewById<TextView>(it).text.toString()) }
        assertEquals("", root.findViewById<View>(R.id.github_widget_project_row_1).contentDescription)
        assertTrue(!root.contentDescription.contains("Miuix"))
        assertTrue(!root.contentDescription.contains("v1.19.0"))
        assertTrue(!root.contentDescription.contains("1.18.0"))
    }

    @Test
    fun `sparse project panels use their spare row for check statistics`() {
        val content = snapshot.copy(
            trackedCount = 1, updateCount = 0, uncheckedCount = 0, failedCount = 0, ignoredCount = 0,
            projects = listOf(GitHubWidgetProject("KeiOS", GitHubWidgetProjectStatus.NoUpdate, localVersion = "1.19.0")),
        )
        val root = GitHubWidgetRenderer.layout(
            context, 10, content, GitHubWidgetConfiguration(true), GitHubWidgetLayout.Board,
        ).apply(context, FrameLayout(context))
        root.measure(
            View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(160, View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, 320, 160)
        val summary = root.findViewById<View>(R.id.github_widget_project_summary)
        val lastLine = root.findViewById<TextView>(R.id.github_widget_project_summary_attention)
        val rect = Rect()
        assertEquals(View.VISIBLE, summary.visibility)
        assertTrue(lastLine.getLocalVisibleRect(rect) && rect.height() >= lastLine.lineHeight)
        assertEquals(
            context.getString(R.string.github_widget_checked_fraction, 1, 1),
            root.findViewById<TextView>(R.id.github_widget_project_summary_checked).text.toString(),
        )

        GitHubWidgetRenderer.layout(
            context, 10, snapshot, GitHubWidgetConfiguration(true), GitHubWidgetLayout.Board,
        ).reapply(context, root)
        assertEquals(View.GONE, summary.visibility)
    }

    @Test
    fun `unknown data is displayed as unknown rather than zero updates`() {
        val root = GitHubWidgetRenderer.layout(
            context, 10, GitHubWidgetSnapshot(), GitHubWidgetConfiguration(), GitHubWidgetLayout.Compact,
        ).apply(context, FrameLayout(context))
        assertEquals("\u2014", root.findViewById<TextView>(R.id.github_widget_count).text.toString())
        assertEquals(
            context.getString(R.string.github_widget_open_to_load),
            root.findViewById<TextView>(R.id.github_widget_status).text.toString(),
        )
    }

    @Test
    fun `long installed versions cannot displace the update candidate from narrow details`() {
        val local = "1.0.0-a-very-long-development-build"
        val content = snapshot.copy(
            projects = listOf(GitHubWidgetProject("Toolbox", GitHubWidgetProjectStatus.Update, "v2.0", localVersion = local)),
        )
        for (variant in listOf(GitHubWidgetLayout.Wide, GitHubWidgetLayout.Tall)) {
            val root = GitHubWidgetRenderer.layout(context, 10, content, GitHubWidgetConfiguration(true), variant)
                .apply(context, FrameLayout(context))
            assertEquals("v2.0", root.findViewById<TextView>(R.id.github_widget_project_version_1).text.toString())
            assertTrue(root.findViewById<View>(R.id.github_widget_project_row_1).contentDescription.contains(local))
        }
    }

    @Test
    fun `tall expanded panel uses spare row height for installed versions when fewer projects exist`() {
        val content = snapshot.copy(projects = snapshot.projects.take(4))
        val root = GitHubWidgetRenderer.layout(
            context, 10, content, GitHubWidgetConfiguration(true), GitHubWidgetLayout.TallExpanded,
        ).apply(context, FrameLayout(context))
        root.measure(
            View.MeasureSpec.makeMeasureSpec(160, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(336, View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, 160, 336)
        val installed = root.findViewById<TextView>(R.id.github_widget_project_detail_1)
        val rect = Rect()
        assertTrue(installed.getLocalVisibleRect(rect) && rect.height() >= installed.lineHeight)
        assertEquals("v1.19.0", root.findViewById<TextView>(R.id.github_widget_project_version_1).text.toString())
        assertTrue(installed.text.contains("1.18.0"))
        val last = root.findViewById<View>(R.id.github_widget_project_row_4)
        assertTrue(last.getLocalVisibleRect(rect) && rect.height() == last.height)
    }

    @Test
    fun `soft light surfaces preserve small text contrast on bright and dark backgrounds`() {
        for (dark in listOf(false, true)) {
            val themed = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
                uiMode = if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            })
            for (underlay in listOf(android.graphics.Color.WHITE, android.graphics.Color.BLACK)) {
                val root = GitHubWidgetRenderer.layout(
                    themed, 10, snapshot, GitHubWidgetConfiguration(true), GitHubWidgetLayout.Board, interactive = false,
                ).apply(themed, FrameLayout(themed)) as ViewGroup
                root.measure(
                    View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(160, View.MeasureSpec.EXACTLY),
                )
                root.layout(0, 0, 320, 160)
                val pixels = Bitmap.createBitmap(320, 160, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(pixels).apply { drawColor(underlay) }
                fun paintSurfaces(view: View) {
                    if (view.visibility != View.VISIBLE) return
                    val save = canvas.save()
                    canvas.translate(view.left.toFloat(), view.top.toFloat())
                    view.background?.let { drawable ->
                        drawable.setBounds(0, 0, view.width, view.height)
                        drawable.draw(canvas)
                    }
                    if (view is ViewGroup) (0 until view.childCount).forEach { paintSurfaces(view.getChildAt(it)) }
                    canvas.restoreToCount(save)
                }
                paintSurfaces(root)
                val colors = listOf(
                    R.color.github_widget_primary, R.color.github_widget_secondary, R.color.github_widget_warning,
                )
                for (id in listOf(
                    R.id.github_widget_brand_label, R.id.github_widget_refresh,
                    R.id.github_widget_project_row_1, R.id.github_widget_project_row_4,
                )) {
                    val view = root.findViewById<View>(id)
                    val area = Rect(0, 0, view.width, view.height)
                    root.offsetDescendantRectToMyCoords(view, area)
                    area.inset(4, 4)
                    for (color in colors) {
                        var contrast = Double.MAX_VALUE
                        for (y in area.top until area.bottom step 3) {
                            for (x in area.left until area.right step 3) {
                                contrast = minOf(contrast, ColorUtils.calculateContrast(themed.getColor(color), pixels.getPixel(x, y)))
                            }
                        }
                        assertTrue(contrast >= 4.5, "night=$dark underlay=$underlay view=$id color=$color contrast=$contrast")
                    }
                }
            }
        }
    }

    @Test
    fun `host language cannot replace labels in the apps selected language`() {
        val english = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocale(Locale.ENGLISH)
        })
        val chineseHost = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocale(Locale.SIMPLIFIED_CHINESE)
        })
        val remote = GitHubWidgetRenderer.layout(
            english, 10, snapshot, GitHubWidgetConfiguration(), GitHubWidgetLayout.Wide, interactive = false,
        )
        val root = remote.apply(chineseHost, FrameLayout(chineseHost))
        assertEquals("Updates", root.findViewById<TextView>(R.id.github_widget_updates_label).text.toString())
        assertEquals("Check", root.findViewById<TextView>(R.id.github_widget_refresh).text.toString())
    }

    @Test
    fun `widget instances and open versus check have separate immutable activity intents`() {
        val open = GitHubWidgetRenderer.activityIntent(context, 10, false)
        val other = GitHubWidgetRenderer.activityIntent(context, 11, false)
        val check = GitHubWidgetRenderer.activityIntent(context, 10, true)
        assertNotEquals(open, other)
        assertNotEquals(open, check)
        assertTrue(shadowOf(check).isActivity)
        assertTrue(shadowOf(check).isImmutable)
        val intent = shadowOf(check).savedIntent
        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertEquals(
            MainActivity.SHORTCUT_ACTION_GITHUB_REFRESH_TRACKED,
            intent.getStringExtra(MainActivity.EXTRA_SHORTCUT_ACTION),
        )
        assertEquals(
            MainActivity.TARGET_BOTTOM_PAGE_GITHUB,
            intent.getStringExtra(MainActivity.EXTRA_TARGET_BOTTOM_PAGE),
        )
    }
}
