package os.kei.core.download

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import os.kei.core.intent.SafeExternalIntents

object AppPrivateDownloadManager {
    fun enqueueHttpsDownload(
        context: Context,
        url: String,
        fileName: String,
        mimeType: String = ""
    ): Long {
        val safeUrl = SafeExternalIntents.httpsExternalUrlOrNull(url)
            ?: throw IllegalArgumentException("download url must be https")
        val safeFileName = sanitizeDownloadFileName(fileName)
        val request = DownloadManager.Request(safeUrl.toUri()).apply {
            setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
            )
            setTitle(safeFileName)
            setDescription(safeFileName)
            if (mimeType.isNotBlank()) {
                setMimeType(mimeType)
            }
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, safeFileName)
        }
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: throw IllegalStateException("download manager unavailable")
        return manager.enqueue(request)
    }

    internal fun sanitizeDownloadFileName(raw: String): String {
        val cleaned = raw
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .replace(Regex("""[\p{Cntrl}]"""), "")
            .replace(Regex("""[:*?"<>|]+"""), "_")
            .replace(Regex("""_+"""), "_")
            .replace(Regex("""_+\."""), ".")
            .trim()
            .trim('.')
        return cleaned.ifBlank { "download.bin" }.take(MAX_FILE_NAME_LENGTH)
    }

    private const val MAX_FILE_NAME_LENGTH = 160
}
