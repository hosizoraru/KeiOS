package os.kei.core.notification.live.oem.vivo

import android.app.NotificationManager
import android.content.Context

/** Publicly documented OEM extensions; absent/blocked methods remain UNKNOWN, never "supported". */
class VivoAtomicCapabilityProbe(
    private val query: (String, String, String) -> Boolean?,
) {
    constructor(context: Context) : this(
        { method, packageName, scene ->
            val manager = context.getSystemService(NotificationManager::class.java)
            runCatching {
                NotificationManager::class.java
                    .getDeclaredMethod(method, String::class.java, String::class.java)
                    .apply { isAccessible = true }
                    .invoke(manager, packageName, scene) as? Boolean
            }.getOrNull()
        },
    )

    fun inspect(packageName: String, scene: String, channelExists: Boolean): VivoAtomicCapability {
        require(packageName.isNotBlank() && scene.isNotBlank())
        fun signal(method: String): VivoAtomicSignal =
            when (runCatching { query(method, packageName, scene) }.getOrNull()) {
                true -> VivoAtomicSignal.ENABLED
                false -> VivoAtomicSignal.DISABLED
                null -> VivoAtomicSignal.UNKNOWN
            }
        return VivoAtomicCapability(
            sceneStatus = signal("getSceneStatus"),
            customFunctionSupport = signal("isSupportCustomFun"),
            channelExists = channelExists,
        )
    }
}
