package os.kei.core.intent

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

object UriGrantCompat {
    fun grantToIntentTargets(
        context: Context,
        intent: Intent,
        uris: List<Uri>,
        flags: Int
    ) {
        if (uris.isEmpty() || flags == 0) return
        val packageNames = buildSet {
            add(context.packageName)
            intent.`package`?.takeIf { it.isNotBlank() }?.let(::add)
            runCatching {
                context.packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0)
                )
            }.getOrDefault(emptyList()).forEach { resolveInfo ->
                resolveInfo.activityInfo?.packageName
                    ?.takeIf { it.isNotBlank() }
                    ?.let(::add)
            }
        }
        packageNames.forEach { packageName ->
            uris.forEach { uri ->
                runCatching {
                    context.grantUriPermission(packageName, uri, flags)
                }
            }
        }
    }
}
