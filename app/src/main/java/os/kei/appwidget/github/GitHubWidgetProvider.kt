package os.kei.appwidget.github

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import java.util.concurrent.Executors

internal fun isGitHubWidgetProcess(processName: String?, packageName: String): Boolean =
    processName == "$packageName:widgetProvider"

class GitHubWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        runAsync { GitHubWidgetUpdater.update(context, appWidgetIds) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        runAsync { GitHubWidgetUpdater.update(context, intArrayOf(appWidgetId)) }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        runAsync { GitHubWidgetStateStore(context).delete(appWidgetIds) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_RESTORED -> {
                val oldIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_OLD_IDS)
                val newIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                if (oldIds != null && newIds != null && oldIds.size == newIds.size) {
                    // The base implementation invokes two callbacks; use one async result for both.
                    runAsync {
                        GitHubWidgetStateStore(context).restore(oldIds, newIds)
                        GitHubWidgetUpdater.update(context, newIds)
                    }
                }
            }
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> runAsync { GitHubWidgetUpdater.updateAll(context) }
            else -> super.onReceive(context, intent)
        }
    }

}

internal fun BroadcastReceiver.runAsync(action: () -> Unit) {
    val pending = goAsync()
    GitHubWidgetBroadcastExecutor.executor.execute {
        try {
            action()
        } catch (error: Exception) {
            Log.w("GitHubWidget", "Widget update failed", error)
        } finally {
            pending?.finish()
        }
    }
}

private object GitHubWidgetBroadcastExecutor {
    val executor by lazy {
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "github-widget").apply { isDaemon = true }
        }
    }
}

internal object GitHubWidgetUpdater {
    fun provider(context: Context): ComponentName =
        ComponentName(context, GitHubWidgetProvider::class.java)

    fun ids(context: Context): IntArray =
        AppWidgetManager.getInstance(context).getAppWidgetIds(provider(context))

    fun updateAll(context: Context) = update(context, ids(context))

    fun update(context: Context, requestedIds: IntArray) {
        val manager = AppWidgetManager.getInstance(context)
        val ownedIds = manager.getAppWidgetIds(provider(context)).toSet()
        val store = GitHubWidgetStateStore(context)
        val snapshot = store.readSnapshot()
        requestedIds.distinct().filter { it in ownedIds }.forEach { id ->
            val views = GitHubWidgetRenderer.render(
                context, id, snapshot, store.configuration(id), manager.getAppWidgetOptions(id),
            )
            try {
                manager.updateAppWidget(id, views)
            } catch (error: IllegalArgumentException) {
                Log.w("GitHubWidget", "Host rejected widget update; using fallback", error)
                manager.updateAppWidget(
                    id,
                    GitHubWidgetRenderer.layout(
                        context, id, GitHubWidgetSnapshot(), GitHubWidgetConfiguration(), variant = GitHubWidgetLayout.Strip,
                    ),
                )
            }
        }
    }
}
