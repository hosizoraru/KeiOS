package os.kei.core.notification.live.oem.oppo

import androidx.annotation.WorkerThread
import java.util.UUID

data class OppoProgressDelivery(
    val accepted: Boolean,
    /** A provider acknowledgement does not prove a visible card; preserve ordinary delivery. */
    val ordinaryNotificationRequired: Boolean = true,
    val failure: String? = null,
    val rejectionCode: Int? = null,
)

/**
 * Own one instance per stable binding and entity. The caller owns the ordinary
 * notification, persistence and scheduling. Use a new entity ID for a new journey.
 *
 * Images are remote references only. Keep their URLs available until END succeeds
 * (including retries). This adapter never grants URI permissions, downloads,
 * recycles bitmaps or deletes caller-owned images.
 */
class OppoProgressAdapter(
    binding: OppoProgressBinding,
    private val transport: OppoIntentSharingTransport,
    private val clock: () -> Long = System::currentTimeMillis,
    private val identifier: () -> String = { UUID.randomUUID().toString() },
) {
    private val binding = binding.copy(serviceIds = binding.serviceIds.toMap())
    private var lastAccepted: OppoProgressContent? = null
    private var lastAttempt: OppoProgressContent? = null
    private var entityId: String? = null
    private var ending = false
    private var ended = false

    @WorkerThread
    @Synchronized
    fun update(content: OppoProgressContent): OppoProgressDelivery {
        if (ended) return failure("entity_ended")
        if (ending) return failure("entity_ending")
        if (entityId != null && entityId != content.entityId) return failure("entity_mismatch")
        val snapshot = content.copy(progress = content.progress?.let {
            it.copy(nodeLabels = it.nodeLabels.toList())
        })
        val result = send(
            snapshot,
            if (lastAccepted == null) OppoCardAction.CREATE else OppoCardAction.UPDATE,
        )
        // Retain failed attempts too: a lost acknowledgement may still have created a card.
        if (result.failure?.startsWith("invalid_payload:") != true) {
            entityId = snapshot.entityId
            lastAttempt = snapshot
        }
        if (result.accepted) lastAccepted = snapshot
        return result
    }

    /**
     * Supply the latest snapshot after process restoration or an uncertain CREATE.
     * END is safe to retry. It must not be replaced by an UPDATE at 100 percent.
     */
    @WorkerThread
    @Synchronized
    fun end(restoredContent: OppoProgressContent? = null): OppoProgressDelivery {
        if (ended) return OppoProgressDelivery(accepted = true)
        val content = restoredContent ?: lastAttempt ?: return failure("missing_end_snapshot")
        if (entityId != null && entityId != content.entityId) return failure("entity_mismatch")
        val result = send(content, OppoCardAction.END)
        if (result.failure?.startsWith("invalid_payload:") != true) {
            ending = true
            entityId = content.entityId
            lastAttempt = content.copy(progress = content.progress?.let {
                it.copy(nodeLabels = it.nodeLabels.toList())
            })
        }
        if (result.accepted) {
            ended = true
            lastAccepted = null
            lastAttempt = null
        }
        return result
    }

    private fun send(content: OppoProgressContent, action: OppoCardAction): OppoProgressDelivery {
        val mapped = OppoProgressPayloadMapper.map(binding, content, action, identifier(), clock())
        if (mapped is OppoPayloadMapping.Invalid) {
            return failure("invalid_payload:${mapped.reason}")
        }
        mapped as OppoPayloadMapping.Valid
        return when (val result = try {
            transport.share(mapped.json)
        } catch (_: Exception) {
            OppoShareResult.Unavailable("transport_failed")
        }) {
            OppoShareResult.Accepted -> OppoProgressDelivery(accepted = true)
            is OppoShareResult.Rejected -> OppoProgressDelivery(
                accepted = false,
                failure = "provider_rejected",
                rejectionCode = result.code,
            )
            is OppoShareResult.Unavailable -> failure(result.reason)
        }
    }

    private fun failure(reason: String) = OppoProgressDelivery(accepted = false, failure = reason)
}
