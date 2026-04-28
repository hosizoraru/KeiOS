package os.kei.core.intent

import android.content.Context
import android.content.Intent
import android.net.Uri

object SafeExternalIntents {
    private const val MIME_TEXT_PLAIN = "text/plain"

    fun isPlainTextSend(intent: Intent?): Boolean {
        if (intent?.action != Intent.ACTION_SEND) return false
        val type = intent.type?.lowercase()?.substringBefore(';')?.trim()
        return type == MIME_TEXT_PLAIN
    }

    fun textShareIntent(
        text: String,
        subject: String = "",
        targetPackage: String = "",
        extras: Map<String, Any> = emptyMap()
    ): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = MIME_TEXT_PLAIN
            if (subject.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
            if (targetPackage.isNotBlank()) `package` = targetPackage
            extras.forEach { (key, value) ->
                when (value) {
                    is Boolean -> putExtra(key, value)
                    is String -> putExtra(key, value)
                    is Int -> putExtra(key, value)
                    is Long -> putExtra(key, value)
                }
            }
        }
    }

    fun browsableViewIntent(
        url: String,
        targetPackage: String = "",
        newTask: Boolean = false
    ): Intent? {
        val uri = parseHttpUriOrNull(url) ?: return null
        return Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            if (targetPackage.isNotBlank()) setPackage(targetPackage)
            if (newTask) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun parseHttpUriOrNull(url: String): Uri? {
        val uri = runCatching { Uri.parse(url.trim()) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase().orEmpty()
        val host = uri.host.orEmpty()
        if (scheme != "https" && scheme != "http") return null
        if (host.isBlank()) return null
        return uri
    }

    fun httpsExternalUrlOrNull(url: String): String? {
        val uri = parseHttpUriOrNull(url) ?: return null
        if (uri.scheme?.lowercase() != "https") return null
        return uri.toString()
    }

    fun startBrowsableUrl(
        context: Context,
        url: String,
        targetPackage: String = "",
        newTask: Boolean = false
    ): Boolean {
        val intent = browsableViewIntent(
            url = url,
            targetPackage = targetPackage,
            newTask = newTask
        ) ?: return false
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
