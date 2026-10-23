package os.kei.core.notification.live.oem.oppo

import android.content.ContentResolver
import android.os.Bundle
import androidx.annotation.WorkerThread
import org.json.JSONObject

fun interface OppoIntentSharingTransport {
    fun share(json: String): OppoShareResult
}

sealed interface OppoShareResult {
    data object Accepted : OppoShareResult
    data class Rejected(val code: Int) : OppoShareResult
    data class Unavailable(val reason: String) : OppoShareResult
}

/** Official document 13558; all IPC stays outside notification construction. */
class OppoIntentSharingClient(
    private val resolver: ContentResolver,
) : OppoIntentSharingTransport {
    @WorkerThread
    override fun share(json: String): OppoShareResult =
        try {
            val client = resolver.acquireUnstableContentProviderClient("IntelligentIntent")
            if (client == null) {
                OppoShareResult.Unavailable("provider_missing")
            } else {
                client.use {
                    val response = it.call("shareIntent", null, Bundle().apply {
                        putString("intentData", json)
                    })
                    decode(response?.getString("result"))
                }
            }
        } catch (_: Exception) {
            OppoShareResult.Unavailable("provider_call_failed")
        }

    companion object {
        internal fun decode(json: String?): OppoShareResult {
            if (json.isNullOrBlank()) return OppoShareResult.Unavailable("missing_result")
            return try {
                val result = JSONObject(json)
                // Do not coerce strings or fractional numbers into a success code.
                val code = result.opt("code")
                if (code !is Int) {
                    OppoShareResult.Unavailable("invalid_result_code")
                } else if (code == 0) {
                    OppoShareResult.Accepted
                } else {
                    OppoShareResult.Rejected(code)
                }
            } catch (_: Exception) {
                OppoShareResult.Unavailable("malformed_result")
            }
        }
    }
}
