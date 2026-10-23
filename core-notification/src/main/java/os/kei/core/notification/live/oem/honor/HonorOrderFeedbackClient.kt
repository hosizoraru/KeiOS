package os.kei.core.notification.live.oem.honor

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.annotation.WorkerThread

/**
 * Public ContentProvider transport from https://developer.honor.com/cn/doc/guides/101454.
 *
 * Consumers must declare KIT_SERVICE_ACCESS, their real com.hihonor.mcs.client.appid and
 * com.hihonor.intelligence.suggestion-kit.enableFlag=true in their application manifest.
 * Package visibility must allow querying com.hihonor.brain. The user must be logged into HONOR.
 * Reuse one app-scoped client so its 800ms minimum interval covers all local order feedback.
 * This independent client does not enable a scene or replace the standard notification path.
 */
class HonorOrderFeedbackClient internal constructor(
    private val packageName: String,
    private val elapsedRealtimeMs: () -> Long = SystemClock::elapsedRealtime,
    private val isConfigured: () -> Boolean = { true },
    private val transport: HonorFeedbackTransport,
) {
    constructor(context: Context) : this(
        packageName = context.packageName,
        transport = HonorProviderFeedbackTransport(context.applicationContext.contentResolver),
        isConfigured = applicationConfigurationCheck(context),
    )

    private var lastAttemptMs: Long? = null

    /** The journey owner controls user initiation; background update/end attempts are not blocked. */
    @WorkerThread
    @Synchronized
    fun submit(
        requestId: String,
        feedback: HonorOrderFeedback,
    ): HonorFeedbackSubmission {
        require(requestId.isNotBlank())
        require(feedback.packageName == packageName)
        if (!isConfigured()) return HonorFeedbackSubmission.MissingConfiguration
        val now = elapsedRealtimeMs()
        if (lastAttemptMs?.let { now - it < MIN_FEEDBACK_INTERVAL_MS } == true) {
            return HonorFeedbackSubmission.Throttled
        }
        val extras =
            Bundle().apply {
                putString("reqId", requestId)
                putString("data", feedback.toJson().toString())
                putString("uri", "")
            }
        return try {
            lastAttemptMs = now
            val response = transport.call("feedback", "feedbackOrder", extras)
            when {
                response == null -> HonorFeedbackSubmission.Unavailable
                response.getBoolean("result", false) -> HonorFeedbackSubmission.Submitted
                else -> HonorFeedbackSubmission.Rejected
            }
        } catch (_: SecurityException) {
            HonorFeedbackSubmission.PermissionDenied
        } catch (_: Exception) {
            HonorFeedbackSubmission.Unavailable
        }
    }

    companion object {
        private const val MIN_FEEDBACK_INTERVAL_MS = 800L
        const val REQUIRED_PERMISSION = "com.hihonor.brain.permission.KIT_SERVICE_ACCESS"
        const val PROVIDER_URI = "content://com.hihonor.brain.kitservice.provider.KitProvider"

        private fun applicationConfigurationCheck(context: Context): () -> Boolean {
            val application = context.applicationContext
            return { hasClientConfiguration(application) }
        }

        fun hasClientConfiguration(context: Context): Boolean =
            runCatching {
                val metadata = context.packageManager
                    .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA).metaData
                metadata?.getBoolean("com.hihonor.intelligence.suggestion-kit.enableFlag", false) == true &&
                    metadata.get("com.hihonor.mcs.client.appid")?.toString()?.isNotBlank() == true
            }.getOrDefault(false)

        @WorkerThread
        fun supportsOrderFeedback(context: Context): Boolean =
            try {
                context.packageManager
                    .getApplicationInfo("com.hihonor.brain", PackageManager.GET_META_DATA)
                    .metaData
                    ?.getBoolean("com.hihonor.brain.supportFeedbackOrder.provider", false) == true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            } catch (_: SecurityException) {
                false
            }
    }
}

enum class HonorFeedbackSubmission {
    MissingConfiguration,
    /** KitProvider accepted the handoff; this does not mean a card was rendered or authorized. */
    Submitted,
    Rejected,
    PermissionDenied,
    Unavailable,
    Throttled,
}

internal fun interface HonorFeedbackTransport {
    fun call(method: String, arg: String, extras: Bundle): Bundle?
}

internal class HonorProviderFeedbackTransport(
    private val resolver: ContentResolver,
) : HonorFeedbackTransport {
    override fun call(method: String, arg: String, extras: Bundle): Bundle? =
        resolver.acquireUnstableContentProviderClient(Uri.parse(HonorOrderFeedbackClient.PROVIDER_URI))
            ?.use { client -> client.call(method, arg, extras) }
}
