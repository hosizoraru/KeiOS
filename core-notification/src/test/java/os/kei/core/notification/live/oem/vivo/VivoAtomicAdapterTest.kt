package os.kei.core.notification.live.oem.vivo

import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
class VivoAtomicAdapterTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val adapter = VivoAtomicAdapter()
    // A protocol test uses an actual documented scene, not a made-up DOWNLOAD scene.
    private val binding = VivoAtomicSceneBinding("travel", "TRAIN")
    private val enabled = VivoAtomicCapability(VivoAtomicSignal.ENABLED, channelExists = true)
    private val content = VivoAtomicContent(
        title = "Train",
        content = "Boarding",
        shortDescription = "Platform",
        shortCoreInfo = "3",
        capsuleText = "Boarding",
        icon = Icon.createWithResource(context, android.R.drawable.ic_dialog_info),
        openIntent = PendingIntent.getActivity(
            context, 1, Intent("os.kei.test.VIVO_OPEN").setPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE,
        ),
        progress = 20,
    )

    private fun request(time: Long = 0, ended: Boolean = false) =
        VivoAtomicRequest("trip-1", 41, "travel", "boot-1", time, content, ended)

    private fun create() = adapter.plan(request(), binding, enabled, null).nextRecord!!

    @Test
    fun `unconfigured and ambiguous businesses have no invented default scene`() {
        assertNull(VivoAtomicSceneSelector.select("github", emptyList()))
        assertNull(VivoAtomicSceneSelector.select("travel", listOf(binding, binding)))
        assertEquals(binding, VivoAtomicSceneSelector.select("travel", listOf(binding)))
        val plan = adapter.plan(request(), null, enabled, null)
        assertEquals(VivoAtomicAction.PublishOrdinary(false), plan.action)
    }

    @Test
    fun `missing disabled and blocked scene queries fall back`() {
        listOf(VivoAtomicSignal.UNKNOWN, VivoAtomicSignal.DISABLED).forEach { signal ->
            val plan = adapter.plan(request(), binding, enabled.copy(sceneStatus = signal), null)
            assertIs<VivoAtomicAction.PublishOrdinary>(plan.action)
        }
        val missingChannel = adapter.plan(request(), binding, enabled.copy(channelExists = false), null)
        assertIs<VivoAtomicAction.PublishOrdinary>(missingChannel.action)
        val blocked = VivoAtomicCapabilityProbe { _, _, _ -> throw SecurityException() }
            .inspect(context.packageName, "TRAIN", true)
        assertEquals(VivoAtomicSignal.UNKNOWN, blocked.sceneStatus)
        assertFalse(blocked.canAttemptLocalAtomic)
    }

    @Test
    fun `probe uses package and scene on both documented methods without assuming custom support`() {
        val calls = mutableListOf<Triple<String, String, String>>()
        val probe = VivoAtomicCapabilityProbe { method, pkg, scene ->
            calls += Triple(method, pkg, scene)
            if (method == "getSceneStatus") true else null
        }
        val result = probe.inspect(context.packageName, "TRAIN", true)
        assertEquals(listOf("getSceneStatus", "isSupportCustomFun"), calls.map { it.first })
        assertTrue(calls.all { it.second == context.packageName && it.third == "TRAIN" })
        assertTrue(result.canAttemptLocalAtomic)
        assertEquals(VivoAtomicSignal.UNKNOWN, result.customFunctionSupport)
    }

    @Test
    fun `create and update use stable identity and increasing wire sequence`() {
        val first = adapter.plan(request(), binding, enabled, null)
        val create = assertIs<VivoAtomicAction.PublishAtomic>(first.action)
        assertEquals(VivoAtomicOperation.CREATE, create.operation)
        assertEquals(0, create.extras.getInt("notification.superx.operation"))
        val second = adapter.plan(request(10_000), binding, enabled, first.nextRecord)
        val update = assertIs<VivoAtomicAction.PublishAtomic>(second.action)
        assertEquals(VivoAtomicOperation.UPDATE, update.operation)
        assertEquals(1, update.extras.getInt("notification.superx.changedRecord"))
        assertEquals(41, second.nextRecord!!.notificationId)
        assertEquals(0L, second.nextRecord.createdAtMs)
    }

    @Test
    fun `frequent updates defer without renewing idle timeout or incrementing sequence`() {
        val old = create()
        val result = adapter.plan(request(9_999), binding, enabled, old)
        assertEquals(VivoAtomicAction.Defer(10_000), result.action)
        assertEquals(old, result.nextRecord)
    }

    @Test
    fun `end is minimal and immediate cancellation avoids refresh limit`() {
        val old = create()
        val immediate = adapter.plan(request(1, ended = true), binding, enabled, old)
        assertIs<VivoAtomicAction.Cancel>(immediate.action)
        val result = adapter.plan(request(10_000, ended = true), binding, enabled, old)
        val end = assertIs<VivoAtomicAction.PublishAtomic>(result.action)
        assertEquals(setOf("notification.superx.operation"), end.extras.keySet())
        assertEquals(2, end.extras.getInt("notification.superx.operation"))
        assertEquals(VivoAtomicPhase.CLOSED, result.nextRecord!!.phase)
        assertIs<VivoAtomicAction.Suppress>(
            adapter.plan(request(30_000), binding, enabled, result.nextRecord).action,
        )
    }

    @Test
    fun `idle limit expires without automatic recreation`() {
        val result = adapter.plan(request(VivoAtomicAdapter.IDLE_LIMIT_MS), binding, enabled, create())
        assertIs<VivoAtomicAction.Cancel>(result.action)
        assertEquals(VivoAtomicPhase.CLOSED, result.nextRecord!!.phase)
        assertIs<VivoAtomicAction.Suppress>(
            adapter.plan(request(VivoAtomicAdapter.IDLE_LIMIT_MS + 1), binding, enabled, result.nextRecord).action,
        )
    }

    @Test
    fun `eight hour limit applies despite recent updates`() {
        val old = create().copy(lastPublishedAtMs = VivoAtomicAdapter.LIFETIME_LIMIT_MS - 20_000)
        val result = adapter.plan(request(VivoAtomicAdapter.LIFETIME_LIMIT_MS), binding, enabled, old)
        assertIs<VivoAtomicAction.Cancel>(result.action)
    }

    @Test
    fun `removal tombstone survives capability loss and suppresses ordinary fallback too`() {
        val removed = adapter.onUserRemoved(create(), VivoAtomicRemoval.NOTIFICATION_CENTER_OR_LOCK_SCREEN)
        val result = adapter.plan(
            request(20_000), null, enabled.copy(sceneStatus = VivoAtomicSignal.UNKNOWN), removed,
        )
        assertIs<VivoAtomicAction.Suppress>(result.action)
        assertEquals(VivoAtomicPhase.REMOVED, result.nextRecord!!.phase)
    }

    @Test
    fun `status bar hide preserves other surfaces and key node update`() {
        val old = create()
        assertEquals(old, adapter.onUserRemoved(old, VivoAtomicRemoval.STATUS_BAR_ONLY))
        val result = adapter.plan(
            request(10_000).copy(content = content.copy(newNode = 1)), binding, enabled, old,
        )
        assertEquals(1, assertIs<VivoAtomicAction.PublishAtomic>(result.action)
            .extras.getInt("notification.superx.newNode"))
        assertFailsWith<IllegalArgumentException> {
            adapter.plan(request(20_000), binding, enabled, result.nextRecord)
        }
    }

    @Test
    fun `capability loss transitions once and does not later promote the same activity`() {
        val fallback = adapter.plan(request(10_000), null, enabled, create())
        assertEquals(VivoAtomicAction.PublishOrdinary(true), fallback.action)
        val later = adapter.plan(request(20_000), binding, enabled, fallback.nextRecord)
        assertEquals(VivoAtomicAction.PublishOrdinary(false), later.action)
        val failure = adapter.onDispatchFailed(create())
        assertEquals(VivoAtomicAction.PublishOrdinary(true), failure.action)
    }

    @Test
    fun `boot change closes activity and slot reuse requires explicit retirement`() {
        val old = create()
        assertIs<VivoAtomicAction.Cancel>(
            adapter.plan(request().copy(bootId = "boot-2"), binding, enabled, old).action,
        )
        assertFailsWith<IllegalArgumentException> {
            adapter.plan(request().copy(activityId = "trip-2"), binding, enabled, old)
        }
    }

    @Test
    fun `basic progress payload includes short card both status surfaces and local field names`() {
        val extras = assertIs<VivoAtomicAction.PublishAtomic>(
            adapter.plan(request(), binding, enabled, null).action,
        ).extras
        assertEquals(4, extras.getInt("notification.superx.template"))
        assertEquals("TRAIN", extras.getString("notification.superx.scene"))
        assertTrue(extras.getBoolean("notification.superx.showNotify"))
        assertEquals(0x111, extras.getInt("notification.superx.displays"))
        val base = extras.getBundle("notification.superx.baseInfos")!!
        assertEquals(5, base.getInt("notification.superx.baseInfos.subInfo"))
        assertEquals(20, base.getInt("notification.superx.baseInfos.subProgress"))
        assertTrue(extras.getBundle("notification.superx.shortInfos")!!
            .containsKey("notification.superx.shortInfos.imageClickResp"))
        assertEquals(1, extras.getBundle("notification.superx.capsule")!!
            .getInt("notification.superx.capsule.state"))
        val island = extras.getBundle("notification.superx.island")!!
        assertEquals(2, island.getInt("island.superx.rightTemplate"))
        assertEquals(20, island.getBundle("island.superx.rightInfo")!!
            .getInt("island.superx.rightInfo.progressValue"))
        assertFalse(extras.containsKey("liveMessage"))
        assertFalse(extras.containsKey("regId"))
    }

    @Test
    fun `foreground payload omits status touchpoint while retaining notification and lock screen`() {
        val extras = VivoAtomicPayloadMapper.extras(
            "TRAIN", VivoAtomicOperation.CREATE,
            content.copy(progress = null, landingPageVisible = true), 0,
        )
        assertEquals(0x011, extras.getInt("notification.superx.displays"))
        assertEquals(0, extras.getBundle("notification.superx.capsule")!!
            .getInt("notification.superx.capsule.state"))
        assertEquals(1, extras.getBundle("notification.superx.baseInfos")!!
            .getInt("notification.superx.baseInfos.subInfo"))
    }

    @Test
    fun `decorator preserves standard data disables auto cancel and cleans fallback without mutation`() {
        val original = Notification.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Standard title")
            .setAutoCancel(true)
            .build()
        val action = assertIs<VivoAtomicAction.PublishAtomic>(
            adapter.plan(request(), binding, enabled, null).action,
        )
        val atomic = VivoAtomicNotificationDecorator.atomic(original, action)
        assertEquals("Standard title", atomic.extras.getCharSequence(Notification.EXTRA_TITLE))
        assertEquals(0, atomic.flags and Notification.FLAG_AUTO_CANCEL)
        assertTrue(original.flags and Notification.FLAG_AUTO_CANCEL != 0)
        val ordinary = VivoAtomicNotificationDecorator.ordinary(atomic)
        assertFalse(ordinary.extras.keySet().any { it.startsWith("notification.superx.") })
        assertEquals("Standard title", ordinary.extras.getCharSequence(Notification.EXTRA_TITLE))
        assertTrue(atomic.extras.containsKey("notification.superx.scene"))
    }

    @Test
    fun `media session transport is never decorated as atomic`() {
        val media = Notification.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .build()
        val action = assertIs<VivoAtomicAction.PublishAtomic>(
            adapter.plan(request(), binding, enabled, null).action,
        )
        assertFailsWith<IllegalArgumentException> { VivoAtomicNotificationDecorator.atomic(media, action) }
    }

    @Test
    fun `invalid progress and blank scene fail before creating wire payload`() {
        assertFailsWith<IllegalArgumentException> { content.copy(progress = 101) }
        assertFailsWith<IllegalArgumentException> { VivoAtomicSceneBinding("travel", " ") }
    }
}
