package os.kei.core.notification.live.oem.oppo

import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OppoProgressAdapterTest {
    // Official debug template values; these are not production configuration.
    private val binding = OppoProgressBinding(
        intentName = "Example.Progress",
        serviceIds = mapOf("fluidCloud" to "999900001", "launcher" to "999800001"),
        entityName = "TAXI",
    )
    private val content = OppoProgressContent(
        entityId = "journey-1",
        milestoneCode = 30,
        milestoneText = "vehicle_coming",
        capsuleLeftText = "Driver",
        capsuleRightText = "Coming",
        title = "Driver is coming",
        content = "Vehicle",
        clickAction = "keios://journey/1",
        progress = OppoProgressBar(
            percent = 25,
            indicatorImageUrl = "https://example.com/indicator.png",
            nodeLabels = listOf("Accepted", "Pickup", "Destination"),
        ),
        rightImageUrl = "https://example.com/artwork.png",
    )

    @Test
    fun `progress envelope carries stable entity and independent request identity`() {
        val sent = mutableListOf<JSONObject>()
        val adapter = adapter(sent)
        assertTrue(adapter.update(content).accepted)
        assertTrue(adapter.update(content.copy(progress = content.progress!!.copy(percent = 70))).accepted)
        assertTrue(adapter.end().accepted)
        assertEquals(listOf(0, 1, 2), sent.map { it.getJSONObject("intentAction").getInt("actionStatus") })
        assertEquals(listOf("journey-1", "journey-1", "journey-1"),
            sent.map { it.getJSONObject("intentEntity").getString("entityId") })
        assertEquals(3, sent.map { it.getString("identifier") }.distinct().size)
        val entity = sent.first().getJSONObject("intentEntity")
        assertEquals(25, entity.getJSONObject("secondaryData").getInt("progress"))
        assertEquals("PROGRESS", entity.getJSONObject("secondaryData").getString("type"))
        assertEquals("vehicle_coming", entity.getJSONObject("milestone").getString("text"))
        assertEquals("keios://journey/1", entity.getJSONObject("primary").getString("clickAction"))
        assertFalse(sent.first().has("seedlingCardOptions"))
        assertFalse(sent.first().has("param_v2"))
    }

    @Test
    fun `rejected create preserves fallback and can end an uncertain delivery`() {
        val sent = mutableListOf<JSONObject>()
        var succeed = false
        val adapter = adapter(sent) {
            if (succeed) OppoShareResult.Accepted else OppoShareResult.Rejected(10101001)
        }
        val rejected = adapter.update(content)
        assertFalse(rejected.accepted)
        assertTrue(rejected.ordinaryNotificationRequired)
        assertEquals(10101001, rejected.rejectionCode)
        succeed = true
        assertTrue(adapter.end().accepted)
        assertEquals(listOf(0, 2), sent.map { it.getJSONObject("intentAction").getInt("actionStatus") })
    }

    @Test
    fun `failed end retains snapshot and images for retry then prevents resurrection`() {
        val sent = mutableListOf<JSONObject>()
        var failEnd = true
        val adapter = adapter(sent) {
            if (sent.last().getJSONObject("intentAction").getInt("actionStatus") == 2 && failEnd) {
                OppoShareResult.Unavailable("provider_missing")
            } else OppoShareResult.Accepted
        }
        adapter.update(content)
        assertFalse(adapter.end().accepted)
        assertEquals("entity_ending", adapter.update(content).failure)
        failEnd = false
        assertTrue(adapter.end().accepted)
        assertEquals(
            "https://example.com/artwork.png",
            sent.last().getJSONObject("intentEntity").getJSONObject("primary").getString("rightImg"),
        )
        assertEquals("entity_ended", adapter.update(content).failure)
        assertTrue(adapter.end().accepted)
        assertEquals(3, sent.size)
    }

    @Test
    fun `invalid payload does not bind adapter or call provider`() {
        val sent = mutableListOf<JSONObject>()
        val adapter = adapter(sent)
        val result = adapter.update(content.copy(entityId = "", title = ""))
        assertFalse(result.accepted)
        assertTrue(result.ordinaryNotificationRequired)
        assertTrue(sent.isEmpty())
        assertTrue(adapter.update(content.copy(entityId = "another-journey")).accepted)
    }

    @Test
    fun `adapter rejects switching entities and supports explicit restored end`() {
        val sent = mutableListOf<JSONObject>()
        val adapter = adapter(sent)
        adapter.update(content)
        assertEquals("entity_mismatch", adapter.update(content.copy(entityId = "other")).failure)
        assertEquals("entity_mismatch", adapter.end(content.copy(entityId = "other")).failure)
        val restored = adapter(sent)
        assertTrue(restored.end(content).accepted)
        assertEquals(2, sent.last().getJSONObject("intentAction").getInt("actionStatus"))
    }

    @Test
    fun `restored end retains retry snapshot even when first call is unavailable`() {
        val sent = mutableListOf<JSONObject>()
        var available = false
        val restored = adapter(sent) {
            if (available) OppoShareResult.Accepted else OppoShareResult.Unavailable("provider_missing")
        }
        assertFalse(restored.end(content).accepted)
        available = true
        assertTrue(restored.end().accepted)
        assertEquals(listOf(2, 2), sent.map { it.getJSONObject("intentAction").getInt("actionStatus") })
    }

    @Test
    fun `unavailable and throwing transport preserve ordinary notification`() {
        for (transport in listOf<OppoIntentSharingTransport>(
            OppoIntentSharingTransport { OppoShareResult.Unavailable("provider_missing") },
            OppoIntentSharingTransport { throw SecurityException("denied") },
        )) {
            val delivery = OppoProgressAdapter(binding, transport).update(content)
            assertFalse(delivery.accepted)
            assertTrue(delivery.ordinaryNotificationRequired)
        }
        assertTrue(adapter(mutableListOf()).update(content).ordinaryNotificationRequired)
    }

    @Test
    fun `local images and invalid progress fail before IPC while text only needs no image lease`() {
        val bad = listOf(
            content.copy(rightImageUrl = "content://os.kei/artwork/1"),
            content.copy(rightImageUrl = "file:///cache/artwork.png"),
            content.copy(rightImageUrl = "https://user:password@example.com/artwork.png"),
            content.copy(progress = content.progress!!.copy(percent = 101)),
            content.copy(progress = content.progress!!.copy(nodeLabels = listOf("Only"))),
            content.copy(progress = content.progress!!.copy(indicatorImageUrl = "content://icons/1")),
            content.copy(clickAction = "javascript:alert(1)"),
        )
        bad.forEach {
            assertIs<OppoPayloadMapping.Invalid>(
                OppoProgressPayloadMapper.map(binding, it, OppoCardAction.CREATE, "request-1", 1L),
            )
        }
        val mapped = assertIs<OppoPayloadMapping.Valid>(
            OppoProgressPayloadMapper.map(
                binding, content.copy(progress = null, rightImageUrl = null),
                OppoCardAction.CREATE, "request-1", 1L,
            ),
        )
        val entity = JSONObject(mapped.json).getJSONObject("intentEntity")
        assertFalse(entity.has("secondaryData"))
        assertFalse(entity.getJSONObject("primary").has("rightImg"))
    }

    @Test
    fun `reply decoding never treats missing or malformed code as success`() {
        listOf(null, "", "not json", "{}", """{"code":"0"}""", """{"code":0.5}""").forEach {
            assertIs<OppoShareResult.Unavailable>(OppoIntentSharingClient.decode(it))
        }
        assertEquals(OppoShareResult.Accepted, OppoIntentSharingClient.decode("""{"code":0,"message":"ok"}"""))
        for (code in listOf(10101001, 10101002, 10102001, 10103001, 10103002, 10103003, 10103004, 10103005)) {
            assertEquals(OppoShareResult.Rejected(code),
                OppoIntentSharingClient.decode("""{"code":$code,"message":"rejected"}"""))
        }
    }

    private fun adapter(
        sent: MutableList<JSONObject>,
        response: () -> OppoShareResult = { OppoShareResult.Accepted },
    ): OppoProgressAdapter {
        var request = 0
        return OppoProgressAdapter(
            binding = binding,
            transport = {
                sent += JSONObject(it)
                response()
            },
            clock = { 1_000L },
            identifier = { "request-${++request}" },
        )
    }
}
