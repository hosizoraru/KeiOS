package os.kei.core.notification.live.oem.vivo

import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.test.core.app.ApplicationProvider
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class VivoAtomicDispatcherTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val binding = VivoAtomicSceneBinding("fixture", "TRAIN")
    private val capability = VivoAtomicCapability(VivoAtomicSignal.ENABLED, channelExists = true)

    @Test
    fun oneAtomicAttemptIsSentAndFrequentUpdatesAreReturnedToOwnerWithoutPosting() {
        val transport = Transport()
        val dispatcher = VivoAtomicDispatcher(transport, Records())
        assertEquals(VivoAtomicDelivery.ATOMIC_ATTEMPTED, send(dispatcher, 10_000).delivery)
        assertEquals(1, transport.notifications.size)
        val deferred = send(dispatcher, 10_001)
        assertEquals(VivoAtomicDelivery.DEFERRED, deferred.delivery)
        assertEquals(20_000L, deferred.retryAtElapsedMs)
        assertEquals(1, transport.notifications.size)
        assertEquals(listOf<Pair<String?, Int>>(null to 45), transport.cancellations)
    }

    @Test
    fun transportFailureFallsBackOnceWithCleanExtrasAndTheSameNotificationId() {
        val transport = Transport().apply { failAtomic = true }
        val records = Records()
        val dispatcher = VivoAtomicDispatcher(transport, records)
        assertEquals(VivoAtomicDelivery.ORDINARY_ATTEMPTED, send(dispatcher, 10_000).delivery)
        assertEquals(1, transport.notifications.size)
        assertEquals(45, transport.notifications.single().first)
        assertFalse(transport.notifications.single().second.extras.containsKey("notification.superx.operation"))
        assertTrue(transport.cancellations.contains(VivoAtomicPayloadMapper.CANCEL_TAG to 45))
        assertEquals(VivoAtomicPhase.ORDINARY, records.load("activity")?.phase)
    }

    @Test
    fun removedTombstoneSurvivesNewDispatcherAndPreventsOrdinaryFallbackToo() {
        val records = Records()
        val first = VivoAtomicDispatcher(Transport(), records)
        send(first, 10_000)
        first.onUserRemoved("activity")
        val transport = Transport()
        val restored = VivoAtomicDispatcher(transport, records)
        assertEquals(VivoAtomicDelivery.SUPPRESSED, send(restored, 30_000).delivery)
        assertTrue(transport.notifications.isEmpty())
        assertEquals(VivoAtomicPhase.REMOVED, records.load("activity")?.phase)
    }

    @Test
    fun failedEndCancelsInsteadOfPostingAnOrdinaryCompletionAlert() {
        val transport = Transport()
        val records = Records()
        val dispatcher = VivoAtomicDispatcher(transport, records)
        send(dispatcher, 10_000)
        transport.failAtomic = true
        val ended = dispatcher.dispatch(base(), request(30_000).copy(ended = true), binding, capability)
        assertEquals(VivoAtomicDelivery.CANCELLED, ended.delivery)
        assertEquals(1, transport.notifications.size)
        assertEquals(VivoAtomicPhase.CLOSED, records.load("activity")?.phase)
    }

    @Test
    fun persistenceFailureDoesNotProduceAnotherNotificationOrForgetRemovalInThisProcess() {
        val transport = Transport()
        val records = Records().apply { failWrites = true }
        val dispatcher = VivoAtomicDispatcher(transport, records)
        assertFalse(send(dispatcher, 10_000).stateSaved)
        assertEquals(1, transport.notifications.size)
        assertFailsWith<IllegalStateException> { dispatcher.onUserRemoved("activity") }
        assertEquals(VivoAtomicDelivery.SUPPRESSED, send(dispatcher, 30_000).delivery)
        assertEquals(1, transport.notifications.size)
    }

    @Test
    fun mediaSessionsAreRejectedBeforeAnyLedgerOrNotificationMutation() {
        val transport = Transport()
        val dispatcher = VivoAtomicDispatcher(transport, Records())
        val media = base().apply { category = Notification.CATEGORY_TRANSPORT }
        assertFailsWith<IllegalArgumentException> {
            dispatcher.dispatch(media, request(10_000), binding, capability)
        }
        assertTrue(transport.notifications.isEmpty())
        assertTrue(transport.cancellations.isEmpty())
    }

    private fun send(dispatcher: VivoAtomicDispatcher, time: Long) =
        dispatcher.dispatch(base(), request(time), binding, capability)

    private fun base() = Notification.Builder(context, "fixture")
        .setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("Task").build()

    private fun request(time: Long) = VivoAtomicRequest(
        activityId = "activity", notificationId = 45, business = "fixture",
        bootId = "boot", nowElapsedMs = time,
        content = VivoAtomicContent(
            "Task", "Working", "Step", "50%", "Working",
            Icon.createWithResource(context, android.R.drawable.ic_media_play),
            PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE),
            progress = 50,
        ),
    )

    private class Records : VivoAtomicRecordStore {
        val encoded = mutableMapOf<String, String>()
        var failWrites = false
        override fun load(activityId: String) = encoded[activityId]?.let(VivoAtomicRecordCodec::decode)
        override fun save(record: VivoAtomicRecord) {
            check(!failWrites)
            encoded[record.activityId] = VivoAtomicRecordCodec.encode(record)
        }
    }

    private class Transport : VivoAtomicNotificationTransport {
        val notifications = mutableListOf<Pair<Int, Notification>>()
        val cancellations = mutableListOf<Pair<String?, Int>>()
        var failAtomic = false
        override fun notify(notificationId: Int, notification: Notification) {
            if (failAtomic && notification.extras.containsKey("notification.superx.operation")) {
                error("transport unavailable")
            }
            notifications += notificationId to notification
        }
        override fun cancel(tag: String?, notificationId: Int) {
            cancellations += tag to notificationId
        }
    }
}
