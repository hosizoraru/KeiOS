package os.kei.appwidget.github

import android.app.Application
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.util.SizeF
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import os.kei.R

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class GitHubWidgetSizingTest {
    @Test
    fun `single advertised size renders its board even without a host measurement`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val size = SizeF(327.94f, 167.69f)
        val options = options(size)
        assertEquals(mapOf(size to GitHubWidgetLayout.Board), GitHubWidgetSizing.layouts(options, large = false))

        val snapshot = GitHubWidgetSnapshot(
            available = true,
            trackedCount = 1,
            projects = listOf(GitHubWidgetProject("KeiOS", GitHubWidgetProjectStatus.Unchecked)),
        )
        val root = GitHubWidgetRenderer.render(
            context, 37, snapshot, GitHubWidgetConfiguration(showProjectNames = true), options,
        ).apply(context, FrameLayout(context))
        assertEquals("KeiOS", root.findViewById<TextView>(R.id.github_widget_project_name_1).text.toString())
        assertNotNull(root.findViewById<TextView>(R.id.github_widget_refresh))
    }

    @Test
    fun `reported sizes retain separate responsive layouts and large font headroom`() {
        val compact = SizeF(160f, 160f)
        val board = SizeF(327.94f, 167.69f)
        val sizes = options(compact, board)
        assertEquals(
            mapOf(compact to GitHubWidgetLayout.Compact, board to GitHubWidgetLayout.Board),
            GitHubWidgetSizing.layouts(sizes, large = false),
        )
        assertEquals(GitHubWidgetLayout.Wide, GitHubWidgetSizing.layouts(sizes, large = true)[board])
    }

    @Test
    fun `updating a reused host shell replaces content and clears private descriptions`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val options = options(SizeF(327.94f, 167.69f))
        val snapshot = GitHubWidgetSnapshot(
            available = true,
            trackedCount = 1,
            projects = listOf(GitHubWidgetProject("KeiOS", GitHubWidgetProjectStatus.Unchecked)),
        )
        fun render(showNames: Boolean) = GitHubWidgetRenderer.render(
            context, 37, snapshot, GitHubWidgetConfiguration(showNames), options,
        )

        val shell = render(true).apply(context, FrameLayout(context)) as ViewGroup
        val publicRoot = shell.getChildAt(0)
        assertTrue(shell.findViewById<android.view.View>(R.id.github_widget_project_row_1)
            .contentDescription.contains("KeiOS"))
        assertTrue(!shell.contentDescription.contains("KeiOS"))
        render(false).reapply(context, shell)
        assertEquals(1, shell.childCount)
        val privateRoot = shell.getChildAt(0)
        assertNotNull(privateRoot)
        assertNotSame(publicRoot, privateRoot)
        assertTrue(!shell.contentDescription.contains("KeiOS"))
        assertEquals("", shell.findViewById<TextView>(R.id.github_widget_project_name_1).text.toString())

        render(true).reapply(context, shell)
        assertEquals(1, shell.childCount)
        assertTrue(shell.findViewById<android.view.View>(R.id.github_widget_project_row_1)
            .contentDescription.contains("KeiOS"))
    }

    @Test
    fun `legacy bounds preserve portrait and landscape size choices`() {
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 327)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 268)
        }
        assertEquals(
            mapOf(
                SizeF(160f, 268f) to GitHubWidgetLayout.Tall,
                SizeF(327f, 160f) to GitHubWidgetLayout.Board,
            ),
            GitHubWidgetSizing.layouts(options, large = false),
        )
    }

    @Test
    fun `missing invalid or excessive sizes use the standard responsive layouts`() {
        val fallback = GitHubWidgetSizing.requirements(large = false)
        assertEquals(fallback, GitHubWidgetSizing.layouts(null, large = false))
        assertEquals(fallback, GitHubWidgetSizing.layouts(options(SizeF(0f, 120f)), large = false))
        val excessive = (1..17).map { SizeF(110f + it, 72f) }.toTypedArray()
        assertEquals(fallback, GitHubWidgetSizing.layouts(options(*excessive), large = false))
    }

    private fun options(vararg sizes: SizeF) = Bundle().apply {
        putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, ArrayList(sizes.toList()))
    }
}
