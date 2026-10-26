package os.kei.appwidget.github

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import os.kei.core.concurrency.AppDispatchers

class GitHubWidgetPinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runAsync {
            if (GitHubWidgetPinning.complete(context, intent)) {
                context.sendBroadcast(
                    Intent(GitHubWidgetPinning.confirmationAction(context)).setPackage(context.packageName),
                )
            }
        }
    }
}

internal object GitHubWidgetPinning {
    fun confirmationAction(context: Context): String = "${context.packageName}.GITHUB_WIDGET_PIN_CONFIRMED"

    fun callback(context: Context, token: String): PendingIntent {
        val intent = Intent(context, GitHubWidgetPinReceiver::class.java)
            .setAction(confirmationAction(context))
            .setData(Uri.Builder().scheme("keios-widget").authority(context.packageName)
                .appendPath("pin").appendPath(token).build())
        // The system fills in the new ID. The explicit component and token URI stay fixed.
        return PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE,
        )
    }

    suspend fun request(
        context: Context,
        snapshot: GitHubWidgetSnapshot,
        configuration: GitHubWidgetConfiguration,
    ): String? {
        val manager = AppWidgetManager.getInstance(context)
        val store = GitHubWidgetStateStore(context)
        val token = withContext(AppDispatchers.fileIo) {
            store.beginPinRequest(configuration, GitHubWidgetUpdater.ids(context))
        }
        var accepted = false
        try {
            val extras = extras(context, snapshot, configuration)
            accepted = manager.requestPinAppWidget(GitHubWidgetUpdater.provider(context), extras, callback(context, token))
            return token.takeIf { accepted }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w("GitHubWidget", "Cannot request home screen widget", error)
            return null
        } finally {
            if (!accepted) withContext(NonCancellable + AppDispatchers.fileIo) { store.abandonPinRequest(token) }
        }
    }

    fun extras(
        context: Context,
        snapshot: GitHubWidgetSnapshot,
        configuration: GitHubWidgetConfiguration,
    ): Bundle = Bundle().apply {
        putParcelable(
            AppWidgetManager.EXTRA_APPWIDGET_PREVIEW,
            GitHubWidgetRenderer.layout(context, 0, snapshot, configuration, GitHubWidgetLayout.Strip, interactive = false),
        )
    }

    fun complete(context: Context, intent: Intent): Boolean {
        val uri = intent.data ?: return false
        if (intent.action != confirmationAction(context) || uri.scheme != "keios-widget" ||
            uri.authority != context.packageName || uri.pathSegments.size != 2 ||
            uri.pathSegments[0] != "pin") return false
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val manager = AppWidgetManager.getInstance(context)
        if (id <= 0 || manager.getAppWidgetInfo(id)?.provider != GitHubWidgetUpdater.provider(context)) return false
        if (!GitHubWidgetStateStore(context).completePinRequest(uri.pathSegments[1], id)) return false
        runCatching { GitHubWidgetUpdater.update(context, intArrayOf(id)) }
            .onFailure { Log.w("GitHubWidget", "Cannot render newly pinned widget", it) }
        return true
    }
}
