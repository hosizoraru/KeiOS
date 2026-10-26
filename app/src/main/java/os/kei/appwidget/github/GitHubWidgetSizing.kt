package os.kei.appwidget.github

import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.util.SizeF
import kotlin.math.ceil

internal object GitHubWidgetSizing {
    private const val MAX_SIZED_VIEWS = 16

    fun requirements(large: Boolean): Map<SizeF, GitHubWidgetLayout> = linkedMapOf(
        SizeF(110f, 72f) to GitHubWidgetLayout.Strip,
        SizeF(240f, if (large) 88f else 72f) to GitHubWidgetLayout.Banner,
        SizeF(110f, 152f) to GitHubWidgetLayout.Compact,
        SizeF(240f, if (large) 160f else 144f) to GitHubWidgetLayout.Wide,
        SizeF(300f, if (large) 176f else 144f) to GitHubWidgetLayout.BoardCompact,
        SizeF(300f, if (large) 192f else 160f) to GitHubWidgetLayout.Board,
        SizeF(110f, if (large) 320f else 268f) to GitHubWidgetLayout.Tall,
        SizeF(110f, if (large) 384f else 336f) to GitHubWidgetLayout.TallExpanded,
        SizeF(240f, 220f) to GitHubWidgetLayout.Detailed,
        SizeF(300f, 220f) to GitHubWidgetLayout.Expanded,
    )

    fun layouts(options: Bundle?, large: Boolean): Map<SizeF, GitHubWidgetLayout> {
        val requirements = requirements(large)
        val sizes = reportedSizes(options)
        if (sizes.isEmpty() || sizes.size > MAX_SIZED_VIEWS) return requirements
        return sizes.associateWith { size -> bestFit(size, requirements) }
    }

    private fun reportedSizes(options: Bundle?): List<SizeF> {
        if (options == null) return emptyList()
        val sizes = runCatching {
            options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java)
                .orEmpty()
                .filter { it.width.isFinite() && it.height.isFinite() && it.width > 0f && it.height > 0f }
                .distinct()
        }.getOrDefault(emptyList())
        if (sizes.isNotEmpty()) return sizes

        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        if (minWidth <= 0 || minHeight <= 0 || maxWidth < minWidth || maxHeight < minHeight) return emptyList()
        return listOf(
            SizeF(minWidth.toFloat(), maxHeight.toFloat()),
            SizeF(maxWidth.toFloat(), minHeight.toFloat()),
        ).distinct()
    }

    private fun bestFit(size: SizeF, requirements: Map<SizeF, GitHubWidgetLayout>): GitHubWidgetLayout =
        requirements.entries
            .filter { (minimum, _) ->
                ceil(size.width) + 1 > minimum.width && ceil(size.height) + 1 > minimum.height
            }
            .minByOrNull { (minimum, _) ->
                val dx = size.width - minimum.width
                val dy = size.height - minimum.height
                dx * dx + dy * dy
            }
            ?.value ?: GitHubWidgetLayout.Strip
}
