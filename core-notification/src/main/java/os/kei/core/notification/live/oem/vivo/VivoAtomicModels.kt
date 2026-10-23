package os.kei.core.notification.live.oem.vivo

import android.app.PendingIntent
import android.graphics.drawable.Icon

/**
 * A real app/business-to-scene assignment supplied by the integration owner after vivo approval.
 * There is deliberately no default DOWNLOAD, BA, MEDIA or KEEPALIVE scene.
 */
data class VivoAtomicSceneBinding(
    val business: String,
    val scene: String,
) {
    init {
        require(business.isNotBlank())
        require(scene.isNotBlank() && scene == scene.trim())
    }
}

object VivoAtomicSceneSelector {
    fun select(business: String, bindings: List<VivoAtomicSceneBinding>): VivoAtomicSceneBinding? =
        bindings.singleOrNull { it.business == business }
}

enum class VivoAtomicSignal { ENABLED, DISABLED, UNKNOWN }

data class VivoAtomicCapability(
    val sceneStatus: VivoAtomicSignal,
    // Doc 896 describes compatibility for custom functions, not an atomic-notification entitlement.
    val customFunctionSupport: VivoAtomicSignal = VivoAtomicSignal.UNKNOWN,
    val channelExists: Boolean,
) {
    val canAttemptLocalAtomic: Boolean
        get() = sceneStatus == VivoAtomicSignal.ENABLED && channelExists
}

enum class VivoAtomicOperation(val wireValue: Int) { CREATE(0), UPDATE(1), END(2) }

/** Typed subset of doc 896: basic card (4), with text or a progress ring, plus both status surfaces. */
data class VivoAtomicContent(
    val title: String,
    val content: String,
    val shortDescription: String,
    val shortCoreInfo: String,
    val capsuleText: String,
    val icon: Icon,
    val openIntent: PendingIntent,
    val progress: Int? = null,
    val progressState: Int = 0,
    val newNode: Int = 0,
    val sound: Boolean = false,
    val dismissWhenKill: Boolean = false,
    val landingPageVisible: Boolean = false,
) {
    init {
        require(title.isNotBlank() && content.isNotBlank())
        require(shortDescription.isNotBlank() && shortCoreInfo.isNotBlank())
        require(capsuleText.isNotBlank())
        require(progress == null || progress in 0..100)
        require(progressState in -1..1)
        require(newNode >= 0)
    }
}

/**
 * Persist per activity, including CLOSED/REMOVED tombstones. elapsedRealtime values are valid only
 * within bootId; the owner must supply a stable identifier for the current boot.
 */
data class VivoAtomicRecord(
    val activityId: String,
    val notificationId: Int,
    val business: String,
    val bootId: String,
    val scene: String?,
    val phase: VivoAtomicPhase,
    val createdAtMs: Long,
    val lastPublishedAtMs: Long,
    val sequence: Int,
    val newNode: Int,
)

enum class VivoAtomicPhase { ATOMIC, ORDINARY, CLOSED, REMOVED }
enum class VivoAtomicRemoval { NOTIFICATION_CENTER_OR_LOCK_SCREEN, STATUS_BAR_ONLY }

data class VivoAtomicRequest(
    val activityId: String,
    val notificationId: Int,
    val business: String,
    val bootId: String,
    val nowElapsedMs: Long,
    val content: VivoAtomicContent,
    val ended: Boolean = false,
) {
    init {
        require(activityId.isNotBlank() && bootId.isNotBlank() && business.isNotBlank())
        require(nowElapsedMs >= 0)
    }
}
