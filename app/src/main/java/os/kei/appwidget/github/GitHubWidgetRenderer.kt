package os.kei.appwidget.github

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import os.kei.MainActivity
import os.kei.R
import java.text.NumberFormat

internal enum class GitHubWidgetLayout { Strip, Banner, Compact, Wide, BoardCompact, Board, Tall, TallExpanded, Detailed, Expanded }

internal object GitHubWidgetRenderer {
    fun render(
        context: Context,
        widgetId: Int,
        snapshot: GitHubWidgetSnapshot,
        configuration: GitHubWidgetConfiguration,
        options: Bundle? = null,
    ): RemoteViews {
        val large = context.resources.configuration.fontScale > 1.3f
        // Some hosts apply updates before reporting a measured view size. Prefer their
        // advertised sizes so a single-size widget does not fall back to the strip.
        val layouts = GitHubWidgetSizing.layouts(options, large).mapValues { (_, variant) ->
            val content = layout(context, widgetId, snapshot, configuration, variant, rootActions = false)
            RemoteViews(context.packageName, R.layout.zzzz_github_widget_shell_v2).apply {
                // Always replace the child: some hosts reuse old trees across package updates.
                removeAllViews(android.R.id.background)
                addView(android.R.id.background, content)
                setContentDescription(android.R.id.background, accessibilitySummary(context, snapshot))
                setOnClickPendingIntent(android.R.id.background, activityIntent(context, widgetId, refresh = false))
            }
        }
        return RemoteViews(layouts)
    }

    fun layout(
        context: Context,
        widgetId: Int,
        snapshot: GitHubWidgetSnapshot,
        configuration: GitHubWidgetConfiguration,
        variant: GitHubWidgetLayout,
        interactive: Boolean = true,
        rootActions: Boolean = true,
    ): RemoteViews {
        val strip = variant == GitHubWidgetLayout.Strip
        val banner = variant == GitHubWidgetLayout.Banner
        val compact = variant == GitHubWidgetLayout.Compact
        val panel = !strip && !banner && !compact
        val large = context.resources.configuration.fontScale > 1.3f
        val rootId = android.R.id.background
        val views = RemoteViews(
            context.packageName,
            when (variant) {
                GitHubWidgetLayout.Strip -> R.layout.github_widget_strip
                GitHubWidgetLayout.Banner -> R.layout.github_widget_banner
                GitHubWidgetLayout.Compact -> R.layout.github_widget_compact
                GitHubWidgetLayout.Tall, GitHubWidgetLayout.TallExpanded -> R.layout.github_widget_tall
                GitHubWidgetLayout.BoardCompact, GitHubWidgetLayout.Board, GitHubWidgetLayout.Expanded -> R.layout.github_widget_board
                else -> R.layout.github_widget_wide
            },
        )
        val numbers = NumberFormat.getIntegerInstance(context.resources.configuration.locales[0])
        val checked = if (snapshot.lastCheckedAtMillis > 0L) {
            DateUtils.formatDateTime(context, snapshot.lastCheckedAtMillis,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_NUMERIC_DATE)
        } else context.getString(R.string.github_widget_never_checked)
        val day = if (snapshot.lastCheckedAtMillis > 0L) {
            DateUtils.formatDateTime(context, snapshot.lastCheckedAtMillis,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NUMERIC_DATE)
        } else context.getString(R.string.github_widget_never_checked)
        val problems = buildList {
            if (snapshot.failedCount > 0) add(context.getString(R.string.github_widget_failed_short, snapshot.failedCount))
            if (snapshot.uncheckedCount > 0) add(context.getString(R.string.github_widget_unchecked_short, snapshot.uncheckedCount))
            if (snapshot.ignoredCount > 0) add(context.getString(R.string.github_widget_paused_short, snapshot.ignoredCount))
        }
        val status = when {
            !snapshot.available -> context.getString(R.string.github_widget_open_to_load)
            snapshot.trackedCount == 0 -> context.getString(R.string.github_widget_empty)
            problems.isNotEmpty() -> problems.joinToString(" \u00b7 ")
            else -> context.getString(R.string.github_widget_cached_results)
        }
        val candidates = snapshot.projects.ifEmpty {
            snapshot.pendingUpdates.map { GitHubWidgetProject(it.name, GitHubWidgetProjectStatus.Update, it.version, it.preRelease) }
        }
        val shown = if (configuration.showProjectNames && snapshot.available) {
            candidates.take(when (variant) {
                GitHubWidgetLayout.Strip, GitHubWidgetLayout.Banner -> 0
                GitHubWidgetLayout.Compact -> if (large) 0 else 1
                GitHubWidgetLayout.Wide -> 2
                GitHubWidgetLayout.BoardCompact, GitHubWidgetLayout.Board -> 4
                GitHubWidgetLayout.Tall -> if (large) 3 else 4
                GitHubWidgetLayout.TallExpanded -> if (large) 4 else 6
                GitHubWidgetLayout.Detailed -> if (large) 3 else 4
                GitHubWidgetLayout.Expanded -> if (large) 4 else 6
            })
        } else emptyList()
        // Assign labels explicitly: the launcher may inflate in a different language.
        views.setTextViewText(R.id.github_widget_brand_label,
            if (strip && large) problems.firstOrNull() ?: day else context.getString(R.string.github_widget_brand))
        views.setTextViewText(R.id.github_widget_updates_label, context.getString(R.string.github_widget_updates))
        views.setTextViewText(R.id.github_widget_count, if (snapshot.available) numbers.format(snapshot.updateCount) else "\u2014")
        views.setTextColor(R.id.github_widget_count,
            context.getColor(if (snapshot.updateCount > 0) R.color.github_widget_primary else R.color.github_widget_foreground))
        views.setTextViewTextSize(R.id.github_widget_count, TypedValue.COMPLEX_UNIT_SP, when {
            panel -> when {
                large || snapshot.updateCount >= 100 -> 20f
                variant == GitHubWidgetLayout.Tall || variant == GitHubWidgetLayout.TallExpanded -> 24f
                else -> 30f
            }
            banner -> if (large) 16f else 18f
            strip -> if (large) 20f else 24f
            large -> 24f
            else -> 30f
        })
        if ((panel || banner) && large) {
            views.setTextViewTextSize(R.id.github_widget_brand_label, TypedValue.COMPLEX_UNIT_SP, 11f)
            views.setTextViewTextSize(R.id.github_widget_tracked, TypedValue.COMPLEX_UNIT_SP, 10f)
        }
        views.setTextViewText(R.id.github_widget_tracked, when {
            banner -> day
            strip && !large && problems.isNotEmpty() -> problems.first()
            strip && !large && snapshot.lastCheckedAtMillis > 0L ->
                context.getString(R.string.github_widget_compact_metadata, snapshot.trackedCount, day)
            else -> context.getString(R.string.github_widget_total_short, snapshot.trackedCount)
        })
        views.setViewVisibility(R.id.github_widget_tracked,
            if (snapshot.available && !panel) View.VISIBLE else View.GONE)
        views.setContentDescription(R.id.github_widget_tracked,
            if (banner) checked else context.getString(R.string.github_widget_tracked_count, snapshot.trackedCount))
        val tall = variant == GitHubWidgetLayout.Tall || variant == GitHubWidgetLayout.TallExpanded
        val padding = ((if (panel) { if (large || tall) 8f else 10f } else { if (large) 6f else 8f }) *
            context.resources.displayMetrics.density).toInt()
        views.setViewPadding(rootId, padding, padding, padding, padding)
        if (banner) {
            views.setTextViewText(R.id.github_widget_banner_total, if (snapshot.available) numbers.format(snapshot.trackedCount) else "\u2014")
            views.setTextViewText(R.id.github_widget_banner_attention,
                if (snapshot.available) numbers.format(snapshot.failedCount + snapshot.uncheckedCount) else "\u2014")
            views.setTextViewText(R.id.github_widget_banner_total_label, context.getString(R.string.github_widget_summary_tracked))
            views.setTextViewText(R.id.github_widget_banner_attention_label, context.getString(R.string.github_widget_needs_check))
        }
        if (panel) {
            renderProjects(context, views, shown, variant)
            if (!tall) {
                renderProjectSummary(context, views, snapshot, shown, variant, large)
            }
            renderMetrics(context, views, snapshot, numbers, large, variant == GitHubWidgetLayout.Wide)
            views.setViewVisibility(R.id.github_widget_metrics, if (shown.isEmpty()) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.github_widget_names, if (shown.isEmpty()) View.GONE else View.VISIBLE)
            val remaining = (snapshot.trackedCount - shown.size).coerceAtLeast(0)
            val details = buildList {
                if (shown.isNotEmpty()) {
                    add(context.getString(R.string.github_widget_total_short, snapshot.trackedCount))
                    add(when {
                        remaining > 0 -> context.getString(R.string.github_widget_more_projects, remaining)
                        problems.isNotEmpty() -> problems.first()
                        else -> context.getString(R.string.github_widget_no_updates_short, snapshot.noUpdateCount)
                    })
                }
            }
            views.setTextViewText(R.id.github_widget_status,
                if (details.isEmpty()) status else details.joinToString(" \u00b7 "))
            views.setTextViewText(R.id.github_widget_refresh, context.getString(R.string.github_widget_check_short))
            views.setContentDescription(R.id.github_widget_refresh, context.getString(R.string.github_widget_check))
            if (interactive) views.setOnClickPendingIntent(R.id.github_widget_refresh, activityIntent(context, widgetId, refresh = true))
        } else if (compact) {
            views.setTextViewText(R.id.github_widget_progress,
                context.getString(R.string.github_widget_checked_fraction, snapshot.checkedCount, snapshot.trackedCount))
            views.setViewVisibility(R.id.github_widget_progress, if (snapshot.available && snapshot.trackedCount > 0) View.VISIBLE else View.GONE)
            views.setTextViewText(R.id.github_widget_names, shown.firstOrNull()?.let { projectDescription(context, it) }.orEmpty())
            views.setViewVisibility(R.id.github_widget_names, if (shown.isEmpty()) View.GONE else View.VISIBLE)
            views.setTextViewText(R.id.github_widget_status, status)
        }
        if (panel || compact) {
            views.setTextViewText(R.id.github_widget_checked, checked)
            views.setContentDescription(R.id.github_widget_checked,
                if (snapshot.lastCheckedAtMillis > 0L) context.getString(R.string.github_widget_last_checked, checked) else checked)
            views.setTextColor(R.id.github_widget_status,
                context.getColor(if (snapshot.failedCount > 0) R.color.github_widget_warning else R.color.github_widget_secondary))
        } else if (strip) {
            views.setViewVisibility(R.id.github_widget_checked, View.GONE)
        }
        if (rootActions) {
            views.setContentDescription(rootId, buildList {
                add(accessibilitySummary(context, snapshot))
                shown.forEach { add(projectDescription(context, it)) }
            }.joinToString(". "))
            if (interactive) views.setOnClickPendingIntent(rootId, activityIntent(context, widgetId, refresh = false))
        }
        return views
    }

    private fun accessibilitySummary(context: Context, snapshot: GitHubWidgetSnapshot): String {
        val numbers = NumberFormat.getIntegerInstance(context.resources.configuration.locales[0])
        return buildList {
            add(context.getString(R.string.github_widget_title))
            if (snapshot.available) {
                add(context.getString(R.string.github_widget_tracked_count, snapshot.trackedCount))
                add("${numbers.format(snapshot.updateCount)} ${context.getString(R.string.github_widget_updates)}")
                add(context.getString(R.string.github_widget_checked_fraction, snapshot.checkedCount, snapshot.trackedCount))
                add("${numbers.format(snapshot.noUpdateCount)} ${context.getString(R.string.github_widget_summary_current)}")
                val problems = buildList {
                    if (snapshot.failedCount > 0) add(context.getString(R.string.github_widget_failed_short, snapshot.failedCount))
                    if (snapshot.uncheckedCount > 0) add(context.getString(R.string.github_widget_unchecked_short, snapshot.uncheckedCount))
                    if (snapshot.ignoredCount > 0) add(context.getString(R.string.github_widget_paused_short, snapshot.ignoredCount))
                }
                add(when {
                    snapshot.trackedCount == 0 -> context.getString(R.string.github_widget_empty)
                    problems.isNotEmpty() -> problems.joinToString(" \u00b7 ")
                    else -> context.getString(R.string.github_widget_cached_results)
                })
            } else {
                add(context.getString(R.string.github_widget_open_to_load))
            }
            if (snapshot.lastCheckedAtMillis > 0L) {
                val checked = DateUtils.formatDateTime(context, snapshot.lastCheckedAtMillis,
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_NUMERIC_DATE)
                add(context.getString(R.string.github_widget_last_checked, checked))
            } else {
                add(context.getString(R.string.github_widget_never_checked))
            }
        }.joinToString(". ")
    }

    private fun renderProjectSummary(
        context: Context,
        views: RemoteViews,
        snapshot: GitHubWidgetSnapshot,
        shown: List<GitHubWidgetProject>,
        variant: GitHubWidgetLayout,
        large: Boolean,
    ) {
        val grid = variant == GitHubWidgetLayout.BoardCompact ||
            variant == GitHubWidgetLayout.Board || variant == GitHubWidgetLayout.Expanded
        val visible = !large && shown.isNotEmpty() && shown.size <= if (grid) 2 else 1
        val attention = when {
            snapshot.failedCount > 0 -> context.getString(R.string.github_widget_failed_short, snapshot.failedCount)
            snapshot.uncheckedCount > 0 -> context.getString(R.string.github_widget_unchecked_short, snapshot.uncheckedCount)
            else -> context.getString(R.string.github_widget_paused_short, snapshot.ignoredCount)
        }
        views.setViewVisibility(R.id.github_widget_project_summary, if (visible) View.VISIBLE else View.GONE)
        views.setTextViewText(
            R.id.github_widget_project_summary_checked,
            context.getString(R.string.github_widget_checked_fraction, snapshot.checkedCount, snapshot.trackedCount),
        )
        views.setTextViewText(
            R.id.github_widget_project_summary_current,
            context.getString(R.string.github_widget_no_updates_short, snapshot.noUpdateCount),
        )
        views.setTextViewText(R.id.github_widget_project_summary_attention, attention)
        views.setTextColor(
            R.id.github_widget_project_summary_attention,
            context.getColor(if (snapshot.failedCount > 0) R.color.github_widget_warning else R.color.github_widget_secondary),
        )
    }

    private fun renderProjects(
        context: Context, views: RemoteViews, shown: List<GitHubWidgetProject>,
        variant: GitHubWidgetLayout,
    ) {
        val grid = variant == GitHubWidgetLayout.BoardCompact || variant == GitHubWidgetLayout.Board || variant == GitHubWidgetLayout.Expanded
        val icons = listOf(
            R.id.github_widget_project_icon_1, R.id.github_widget_project_icon_2, R.id.github_widget_project_icon_3,
            R.id.github_widget_project_icon_4, R.id.github_widget_project_icon_5, R.id.github_widget_project_icon_6,
        )
        val details = listOf(
            R.id.github_widget_project_detail_1, R.id.github_widget_project_detail_2, R.id.github_widget_project_detail_3,
            R.id.github_widget_project_detail_4, R.id.github_widget_project_detail_5, R.id.github_widget_project_detail_6,
        )
        val rows = listOf(
            Triple(R.id.github_widget_project_row_1, R.id.github_widget_project_name_1, R.id.github_widget_project_version_1),
            Triple(R.id.github_widget_project_row_2, R.id.github_widget_project_name_2, R.id.github_widget_project_version_2),
            Triple(R.id.github_widget_project_row_3, R.id.github_widget_project_name_3, R.id.github_widget_project_version_3),
            Triple(R.id.github_widget_project_row_4, R.id.github_widget_project_name_4, R.id.github_widget_project_version_4),
            Triple(R.id.github_widget_project_row_5, R.id.github_widget_project_name_5, R.id.github_widget_project_version_5),
            Triple(R.id.github_widget_project_row_6, R.id.github_widget_project_name_6, R.id.github_widget_project_version_6),
        )
        rows.forEachIndexed { index, (row, name, metadata) ->
            val item = shown.getOrNull(index)
            val detail = item?.localVersion?.takeIf { it.isNotBlank() }
                ?.let { context.getString(R.string.github_widget_local_version, it) }.orEmpty()
            val separateLocalVersion = detail.isNotEmpty() && context.resources.configuration.fontScale <= 1.1f &&
                (grid && variant != GitHubWidgetLayout.BoardCompact ||
                    variant == GitHubWidgetLayout.TallExpanded && shown.size <= 4)
            views.setViewVisibility(row, if (item == null) View.GONE else View.VISIBLE)
            views.setContentDescription(row, item?.let { projectDescription(context, it) }.orEmpty())
            views.setTextViewText(name, item?.name.orEmpty())
            views.setTextViewText(metadata, item?.let {
                val change = context.getString(R.string.github_widget_version_change, it.localVersion, it.version)
                val budget = if (variant == GitHubWidgetLayout.Tall || variant == GitHubWidgetLayout.TallExpanded) 14 else 20
                if (!grid && !separateLocalVersion && context.resources.configuration.fontScale <= 1.1f &&
                    it.status == GitHubWidgetProjectStatus.Update &&
                    !it.preRelease && it.version.isNotBlank() && it.localVersion.isNotBlank() && change.length <= budget
                ) change
                else projectMetadata(context, it)
            }.orEmpty())
            views.setTextViewText(details[index], detail)
            views.setViewVisibility(details[index],
                if (separateLocalVersion) View.VISIBLE else View.GONE)
            views.setImageViewResource(icons[index], when (item?.status) {
                GitHubWidgetProjectStatus.Update -> R.drawable.github_widget_update
                GitHubWidgetProjectStatus.Failed -> R.drawable.github_widget_failed
                GitHubWidgetProjectStatus.NoUpdate -> R.drawable.github_widget_current
                GitHubWidgetProjectStatus.Paused -> R.drawable.github_widget_paused
                else -> R.drawable.github_widget_unchecked
            })
            views.setViewVisibility(icons[index],
                if (item == null || item.status == GitHubWidgetProjectStatus.NoUpdate) View.GONE else View.VISIBLE)
            if (!grid) {
                views.setViewLayoutMargin(row, RemoteViews.MARGIN_BOTTOM,
                    if (index < shown.lastIndex) 2f else 0f, TypedValue.COMPLEX_UNIT_DIP)
            }
            views.setTextColor(metadata, context.getColor(when (item?.status) {
                GitHubWidgetProjectStatus.Update -> R.color.github_widget_primary
                GitHubWidgetProjectStatus.Failed -> R.color.github_widget_warning
                else -> R.color.github_widget_secondary
            }))
        }
        if (grid) {
            listOf(R.id.github_widget_project_group_1, R.id.github_widget_project_group_2, R.id.github_widget_project_group_3)
                .forEachIndexed { index, id ->
                    views.setViewVisibility(id, if (index * 2 < shown.size) View.VISIBLE else View.GONE)
                    views.setViewLayoutMargin(id, RemoteViews.MARGIN_BOTTOM,
                        if ((index + 1) * 2 < shown.size) 6f else 0f, TypedValue.COMPLEX_UNIT_DIP)
                }
        }
    }

    private fun renderMetrics(
        context: Context, views: RemoteViews, snapshot: GitHubWidgetSnapshot,
        numbers: NumberFormat, large: Boolean, short: Boolean,
    ) {
        val metrics = listOf(
            Triple(R.id.github_widget_metric_total, R.id.github_widget_metric_label_total, R.string.github_widget_summary_tracked) to snapshot.trackedCount,
            Triple(R.id.github_widget_metric_checked, R.id.github_widget_metric_label_checked, R.string.github_widget_summary_checked) to snapshot.checkedCount,
            Triple(R.id.github_widget_metric_current, R.id.github_widget_metric_label_current, R.string.github_widget_summary_current) to snapshot.noUpdateCount,
            Triple(R.id.github_widget_metric_unchecked, R.id.github_widget_metric_label_unchecked, R.string.github_widget_summary_unchecked) to snapshot.uncheckedCount,
            Triple(R.id.github_widget_metric_failed, R.id.github_widget_metric_label_failed, R.string.github_widget_summary_failed) to snapshot.failedCount,
            Triple(R.id.github_widget_metric_paused, R.id.github_widget_metric_label_paused, R.string.github_widget_summary_paused) to snapshot.ignoredCount,
        )
        metrics.forEach { (ids, value) ->
            views.setTextViewText(ids.first, if (snapshot.available) numbers.format(value) else "\u2014")
            views.setTextViewText(ids.second, context.getString(ids.third))
            val color = when {
                ids.first == R.id.github_widget_metric_failed && value > 0 -> R.color.github_widget_warning
                value == 0 -> R.color.github_widget_secondary
                else -> R.color.github_widget_foreground
            }
            views.setTextColor(ids.first, context.getColor(color))
            views.setTextViewTextSize(ids.first, TypedValue.COMPLEX_UNIT_SP,
                if (short) { if (large) 11f else 16f } else { if (large) 14f else 18f })
            views.setTextViewTextSize(ids.second, TypedValue.COMPLEX_UNIT_SP,
                if (short) { if (large) 9f else 10f } else { if (large) 10f else 11f })
        }
    }

    private fun projectMetadata(context: Context, project: GitHubWidgetProject): String = when (project.status) {
        GitHubWidgetProjectStatus.Update -> when {
            project.version.isBlank() -> context.getString(R.string.github_widget_updates)
            project.preRelease -> context.getString(R.string.github_widget_preview_version, project.version)
            else -> project.version
        }
        GitHubWidgetProjectStatus.NoUpdate -> context.getString(R.string.github_widget_row_current)
        GitHubWidgetProjectStatus.Failed -> context.getString(R.string.github_widget_row_failed)
        GitHubWidgetProjectStatus.Unchecked -> context.getString(R.string.github_widget_row_unchecked)
        GitHubWidgetProjectStatus.Paused -> context.getString(R.string.github_widget_row_paused)
    }

    private fun projectDescription(context: Context, project: GitHubWidgetProject): String =
        buildList {
            add(project.name)
            add(projectMetadata(context, project))
            if (project.localVersion.isNotBlank()) add(context.getString(R.string.github_widget_local_version, project.localVersion))
        }.joinToString(", ")

    fun activityIntent(context: Context, widgetId: Int, refresh: Boolean): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            data = Uri.Builder().scheme("keios-widget").authority(context.packageName)
                .appendPath("github").appendPath(widgetId.toString())
                .appendPath(if (refresh) "check" else "open").build()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.EXTRA_TARGET_BOTTOM_PAGE, MainActivity.TARGET_BOTTOM_PAGE_GITHUB)
            if (refresh) {
                putExtra(
                    MainActivity.EXTRA_SHORTCUT_ACTION,
                    MainActivity.SHORTCUT_ACTION_GITHUB_REFRESH_TRACKED,
                )
            }
        }
        return PendingIntent.getActivity(
            context, widgetId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
