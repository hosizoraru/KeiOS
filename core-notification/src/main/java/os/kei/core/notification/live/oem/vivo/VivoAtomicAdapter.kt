package os.kei.core.notification.live.oem.vivo

import android.os.Bundle

sealed interface VivoAtomicAction {
    data class PublishAtomic(
        val operation: VivoAtomicOperation,
        val extras: Bundle,
        val cancelOrdinaryFirst: Boolean = operation == VivoAtomicOperation.CREATE,
    ) : VivoAtomicAction
    /** Cancel the OEM-owned entry first when transitioning out of an atomic attempt. */
    data class PublishOrdinary(val cancelAtomicFirst: Boolean) : VivoAtomicAction
    data object Cancel : VivoAtomicAction
    data class Defer(val retryAtElapsedMs: Long) : VivoAtomicAction
    data class Suppress(val reason: String) : VivoAtomicAction
}

data class VivoAtomicPlan(
    val action: VivoAtomicAction,
    /** Commit only after executing the plan successfully. Defer/suppress never renew the lease. */
    val nextRecord: VivoAtomicRecord?,
)

/**
 * Stateless, local-only planner. The parent owns serialized dispatch, persistence and retry scheduling.
 * A notify() return is a delivery attempt, not confirmation that OriginOS displayed an atomic card.
 */
class VivoAtomicAdapter {
    companion object {
        const val MIN_UPDATE_MS = 10_000L
        const val IDLE_LIMIT_MS = 2 * 60 * 60 * 1000L
        const val LIFETIME_LIMIT_MS = 8 * 60 * 60 * 1000L
    }

    fun plan(
        request: VivoAtomicRequest,
        binding: VivoAtomicSceneBinding?,
        capability: VivoAtomicCapability,
        previous: VivoAtomicRecord?,
    ): VivoAtomicPlan {
        val old = previous?.takeIf { it.activityId == request.activityId }
        if (old != null) {
            require(old.notificationId == request.notificationId) { "An activity must retain its notificationId" }
            require(old.business == request.business) { "An activity must retain its business identity" }
            if (old.phase == VivoAtomicPhase.REMOVED || old.phase == VivoAtomicPhase.CLOSED) {
                return VivoAtomicPlan(VivoAtomicAction.Suppress("activity already closed or removed"), old)
            }
            if (old.bootId != request.bootId || request.nowElapsedMs < old.lastPublishedAtMs) {
                return close(request, old)
            }
        }
        // A caller must retire the old activity before reusing its notification slot.
        require(previous == null || old != null) { "Retire the previous activity before reusing its slot" }
        if (request.ended) {
            if (old?.phase != VivoAtomicPhase.ATOMIC) {
                return VivoAtomicPlan(VivoAtomicAction.Cancel, record(request, old, VivoAtomicPhase.CLOSED))
            }
            // cancel(tag, id) ends immediately without making an over-frequency notify update.
            if (request.nowElapsedMs - old.lastPublishedAtMs < MIN_UPDATE_MS) {
                return VivoAtomicPlan(VivoAtomicAction.Cancel, old.copy(phase = VivoAtomicPhase.CLOSED))
            }
            return VivoAtomicPlan(
                VivoAtomicAction.PublishAtomic(
                    VivoAtomicOperation.END,
                    VivoAtomicPayloadMapper.extras(
                        old.scene!!, VivoAtomicOperation.END, request.content, old.sequence,
                    ),
                ),
                old.copy(phase = VivoAtomicPhase.CLOSED),
            )
        }
        if (old?.phase == VivoAtomicPhase.ATOMIC &&
            (request.nowElapsedMs - old.createdAtMs >= LIFETIME_LIMIT_MS ||
                request.nowElapsedMs - old.lastPublishedAtMs >= IDLE_LIMIT_MS)
        ) {
            return close(request, old)
        }
        val scene = binding?.takeIf { it.business == request.business }?.scene
        // Once degraded, keep this activity ordinary rather than repeatedly replacing its surface.
        if (old?.phase == VivoAtomicPhase.ORDINARY || scene == null ||
            !capability.canAttemptLocalAtomic || (old != null && old.scene != scene)
        ) {
            return VivoAtomicPlan(
                VivoAtomicAction.PublishOrdinary(old?.phase == VivoAtomicPhase.ATOMIC),
                record(request, old, VivoAtomicPhase.ORDINARY),
            )
        }
        if (old != null) {
            require(request.content.newNode >= old.newNode) { "Key nodes must not move backwards" }
            if (request.nowElapsedMs - old.lastPublishedAtMs < MIN_UPDATE_MS) {
                return VivoAtomicPlan(
                    VivoAtomicAction.Defer(old.lastPublishedAtMs + MIN_UPDATE_MS), old,
                )
            }
            if (old.sequence == Int.MAX_VALUE) return close(request, old)
        }
        val next = record(request, old, VivoAtomicPhase.ATOMIC).copy(scene = scene)
        val operation = if (old == null) VivoAtomicOperation.CREATE else VivoAtomicOperation.UPDATE
        return VivoAtomicPlan(
            VivoAtomicAction.PublishAtomic(
                operation, VivoAtomicPayloadMapper.extras(scene, operation, request.content, next.sequence),
            ),
            next,
        )
    }

    /**
     * The public docs define no local display-result callback or removal broadcast. The owner supplies
     * reliable removal evidence; do not infer user deletion from absence alone or from cancel().
     */
    fun onUserRemoved(record: VivoAtomicRecord, surface: VivoAtomicRemoval): VivoAtomicRecord =
        when (surface) {
            VivoAtomicRemoval.NOTIFICATION_CENTER_OR_LOCK_SCREEN -> record.copy(phase = VivoAtomicPhase.REMOVED)
            // OS owns hiding/reappearance of the status surface at subsequent newNode values.
            VivoAtomicRemoval.STATUS_BAR_ONLY -> record
        }

    /** Explicit local dispatch failure: retry ordinary with the same id, never post a second id. */
    fun onDispatchFailed(record: VivoAtomicRecord): VivoAtomicPlan =
        if (record.phase == VivoAtomicPhase.ATOMIC) {
            VivoAtomicPlan(VivoAtomicAction.PublishOrdinary(true), record.copy(phase = VivoAtomicPhase.ORDINARY))
        } else {
            VivoAtomicPlan(VivoAtomicAction.Suppress("no active atomic attempt"), record)
        }

    private fun record(
        request: VivoAtomicRequest,
        old: VivoAtomicRecord?,
        phase: VivoAtomicPhase,
    ) = VivoAtomicRecord(
        activityId = request.activityId,
        notificationId = request.notificationId,
        business = request.business,
        bootId = request.bootId,
        scene = old?.scene,
        phase = phase,
        createdAtMs = old?.createdAtMs ?: request.nowElapsedMs,
        lastPublishedAtMs = request.nowElapsedMs,
        sequence = old?.let { if (it.sequence < Int.MAX_VALUE) it.sequence + 1 else it.sequence } ?: 0,
        newNode = request.content.newNode,
    )

    private fun close(request: VivoAtomicRequest, old: VivoAtomicRecord): VivoAtomicPlan =
        VivoAtomicPlan(
            VivoAtomicAction.Cancel,
            record(request, old, VivoAtomicPhase.CLOSED),
        )
}
