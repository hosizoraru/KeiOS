package os.kei.mcp.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import os.kei.core.intent.PendingIntentLaunchOptionsCompat

internal enum class McpNotificationOpenEntry(val requestCodeBase: Int) {
    Body(110_100),
    Focus(410_100),
    ForegroundBootstrap(310_100),
}

internal object McpNotificationOpenIntents {
    fun create(
        context: Context,
        notificationId: Int,
        intent: Intent,
        entry: McpNotificationOpenEntry,
    ): PendingIntent {
        // Account/slot ids can make the numeric request-code ranges overlap. Extras do not
        // distinguish PendingIntents, so retain the producer, entry and id in filterEquals.
        val identifiedIntent = Intent(intent).apply {
            identifier = "os.kei.mcp.notification:${entry.name}:$notificationId"
        }
        val requestCode = entry.requestCodeBase + notificationId
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (entry == McpNotificationOpenEntry.Focus) {
            PendingIntent.getActivity(context, requestCode, identifiedIntent, flags)
        } else {
            PendingIntentLaunchOptionsCompat.getUserVisibleActivity(
                context,
                requestCode,
                identifiedIntent,
                flags,
            )
        }
    }
}
