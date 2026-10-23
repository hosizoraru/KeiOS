package os.kei.ui.page.main.ba

import kotlinx.coroutines.test.runTest
import org.junit.Test
import os.kei.feature.notification.MiFocusNotificationActionReceiver
import os.kei.feature.notification.resolveBaApNotificationInteractionWrite
import os.kei.ui.page.main.ba.support.BaAccountId
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BaApNotificationInteractionRegressionTest {
    @Test
    fun `swipe snoozes an active reminder and expiry allows the same level again`() = runTest {
        val delivery = RecordingDelivery()
        val initial = request()
        val delivered = BaApNotificationSyncCoordinator.sync(initial, NOW_MS, delivery)
        val active = initial.copy(lastNotifiedLevel = assertNotNull(delivered.lastNotifiedLevel))

        BaApNotificationSyncCoordinator.sync(active, NOW_MS + 1, delivery)
        assertEquals(listOf("send", "refresh"), delivery.events)

        val swipe = assertNotNull(
            resolveBaApNotificationInteractionWrite(MiFocusNotificationActionReceiver.ACTION_DISMISS, NOW_MS + 2),
        )
        assertNull(swipe.suppressionAnchorAtMs)
        val snoozed = active.copy(dismissedUntilAtMs = assertNotNull(swipe.dismissedUntilAtMs))
        delivery.events.clear()

        BaApNotificationSyncCoordinator.sync(snoozed, snoozed.dismissedUntilAtMs - 1, delivery)
        assertEquals(emptyList(), delivery.events)

        val repeated = BaApNotificationSyncCoordinator.sync(snoozed, snoozed.dismissedUntilAtMs, delivery)
        assertEquals(listOf("send"), delivery.events)
        assertEquals(initial.currentDisplay, repeated.lastNotifiedLevel)
        assertEquals(0L, repeated.dismissedUntilAtMs)
        assertEquals(initial.accountId, delivery.lastAccountId)
    }

    @Test
    fun `mark read prevents refresh and regenerated AP until the configured reset`() = runTest {
        val read = assertNotNull(
            resolveBaApNotificationInteractionWrite(MiFocusNotificationActionReceiver.ACTION_MARK_READ, NOW_MS),
        )
        val acknowledged = request().copy(
            currentDisplay = 121,
            lastNotifiedLevel = 120,
            suppressionAnchorAtMs = assertNotNull(read.suppressionAnchorAtMs),
            dismissedUntilAtMs = assertNotNull(read.dismissedUntilAtMs),
        )
        val delivery = RecordingDelivery()

        BaApNotificationSyncCoordinator.sync(acknowledged, NOW_MS + BA_AP_READ_REPEAT_INTERVAL_MS, delivery)
        assertEquals(emptyList(), delivery.events)

        val recovered = BaApNotificationSyncCoordinator.sync(
            acknowledged.copy(currentDisplay = 119), NOW_MS + BA_AP_READ_REPEAT_INTERVAL_MS + 1, delivery,
        )
        assertEquals(0L, recovered.suppressionAnchorAtMs)
        assertEquals(-1, recovered.lastNotifiedLevel)
        assertFalse(delivery.events.contains("send"))
    }

    private fun request() = BaApNotificationSyncRequest(
        currentDisplay = 120,
        limitDisplay = 240,
        thresholdDisplay = 120,
        notifyEnabled = true,
        lastNotifiedLevel = -1,
        accountId = BaAccountId("cn-main"),
        keepReadUntilBelowThreshold = true,
    )

    private class RecordingDelivery : BaApNotificationDelivery {
        val events = mutableListOf<String>()
        var lastAccountId: BaAccountId? = null

        override suspend fun sendThreshold(request: BaApNotificationSyncRequest): Boolean {
            events += "send"
            lastAccountId = request.accountId
            return true
        }

        override suspend fun refreshActive(request: BaApNotificationSyncRequest): Boolean {
            events += "refresh"
            lastAccountId = request.accountId
            return true
        }
    }

    private companion object {
        const val NOW_MS = 1_000_000L
    }
}
