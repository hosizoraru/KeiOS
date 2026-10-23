package os.kei.core.notification.live.oem.vivo

import android.app.Notification
import androidx.annotation.WorkerThread

interface VivoAtomicRecordStore {
    fun load(activityId: String): VivoAtomicRecord?
    fun save(record: VivoAtomicRecord)
}

interface VivoAtomicNotificationTransport {
    fun notify(notificationId: Int, notification: Notification)
    fun cancel(tag: String?, notificationId: Int)
}

enum class VivoAtomicDelivery { ATOMIC_ATTEMPTED, ORDINARY_ATTEMPTED, CANCELLED, DEFERRED, SUPPRESSED, FAILED }

data class VivoAtomicDispatchResult(
    val delivery: VivoAtomicDelivery,
    val retryAtElapsedMs: Long? = null,
    val stateSaved: Boolean = true,
)

/** Executes the planner against one notification identity; deferred content stays with the owner. */
class VivoAtomicDispatcher(
    private val transport: VivoAtomicNotificationTransport,
    private val records: VivoAtomicRecordStore,
    private val planner: VivoAtomicAdapter = VivoAtomicAdapter(),
) {
    private val currentRecords = mutableMapOf<String, VivoAtomicRecord>()
    @WorkerThread
    @Synchronized
    fun dispatch(
        ordinary: Notification,
        request: VivoAtomicRequest,
        binding: VivoAtomicSceneBinding?,
        capability: VivoAtomicCapability,
    ): VivoAtomicDispatchResult {
        require(ordinary.category != Notification.CATEGORY_TRANSPORT &&
            !ordinary.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
        ) { "MediaSession notifications must bypass the atomic dispatcher" }
        val previous = currentRecords[request.activityId] ?: records.load(request.activityId)
        val plan = planner.plan(request, binding, capability, previous)
        return execute(ordinary, request, plan)
    }

    @WorkerThread
    @Synchronized
    fun onUserRemoved(activityId: String) {
        (currentRecords[activityId] ?: records.load(activityId))?.let { old ->
            val removed = planner.onUserRemoved(old, VivoAtomicRemoval.NOTIFICATION_CENTER_OR_LOCK_SCREEN)
            currentRecords[activityId] = removed
            records.save(removed)
            currentRecords.remove(activityId)
        }
    }

    private fun execute(
        ordinary: Notification,
        request: VivoAtomicRequest,
        plan: VivoAtomicPlan,
    ): VivoAtomicDispatchResult {
        val id = request.notificationId
        val action = plan.action
        if (action is VivoAtomicAction.Defer) {
            return VivoAtomicDispatchResult(VivoAtomicDelivery.DEFERRED, action.retryAtElapsedMs)
        }
        if (action is VivoAtomicAction.Suppress) {
            return VivoAtomicDispatchResult(VivoAtomicDelivery.SUPPRESSED)
        }
        val delivery = try {
            when (action) {
                is VivoAtomicAction.PublishAtomic -> {
                    if (action.cancelOrdinaryFirst) transport.cancel(null, id)
                    transport.notify(id, VivoAtomicNotificationDecorator.atomic(ordinary, action))
                    VivoAtomicDelivery.ATOMIC_ATTEMPTED
                }
                is VivoAtomicAction.PublishOrdinary -> {
                    if (action.cancelAtomicFirst) transport.cancel(VivoAtomicPayloadMapper.CANCEL_TAG, id)
                    transport.notify(id, VivoAtomicNotificationDecorator.ordinary(ordinary))
                    VivoAtomicDelivery.ORDINARY_ATTEMPTED
                }
                VivoAtomicAction.Cancel -> {
                    cancel(id)
                    VivoAtomicDelivery.CANCELLED
                }
                else -> error("Non-dispatch action")
            }
        } catch (_: Exception) {
            if (action is VivoAtomicAction.PublishAtomic && action.operation == VivoAtomicOperation.END) {
                // A failed END cannot become a fresh ordinary alert.
                return try {
                    cancel(id)
                    commit(plan, VivoAtomicDelivery.CANCELLED)
                } catch (_: Exception) {
                    VivoAtomicDispatchResult(VivoAtomicDelivery.FAILED)
                }
            }
            if (action is VivoAtomicAction.PublishAtomic) {
                return execute(ordinary, request, planner.onDispatchFailed(requireNotNull(plan.nextRecord)))
            }
            return VivoAtomicDispatchResult(VivoAtomicDelivery.FAILED)
        }
        return commit(plan, delivery)
    }

    private fun cancel(id: Int) {
        transport.cancel(VivoAtomicPayloadMapper.CANCEL_TAG, id)
        transport.cancel(null, id)
    }

    private fun commit(plan: VivoAtomicPlan, delivery: VivoAtomicDelivery): VivoAtomicDispatchResult {
        val saved = runCatching { plan.nextRecord?.let(records::save) }.isSuccess
        plan.nextRecord?.let {
            if (saved) currentRecords.remove(it.activityId) else currentRecords[it.activityId] = it
        }
        // Storage failure after notify is not transport failure: never post a duplicate fallback.
        return VivoAtomicDispatchResult(delivery, stateSaved = saved)
    }
}
