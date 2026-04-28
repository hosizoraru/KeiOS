package os.kei.ui.page.main.about.util

import android.content.Context
import os.kei.core.intent.SafeExternalIntents
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return ""
    return runCatching {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        formatter.format(Date(epochMillis))
    }.getOrDefault("")
}

fun openExternalUrl(context: Context, url: String): Boolean {
    return SafeExternalIntents.startBrowsableUrl(context, url, newTask = true)
}
