package os.kei.core.notification.live.oem.honor

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
class HonorOrderFeedbackTest {
    @Test
    fun clientOrderKeepsNestedObjectsAndStringTimestamps() {
        val json = orderFeedback().toJson()
        assertEquals("1770000000000", json.get("startTime"))
        assertEquals("1770000000000", json.get("createTime"))
        assertFalse(json.has("templateType"))
        assertFalse(json.has("endTime"))
        val detail = json.getJSONObject("orderDetail")
        assertEquals("event_normal", detail.getString("layout"))
        assertEquals("Title", detail.getJSONObject("cardText").getString("title"))
        assertEquals("Ready", detail.getJSONObject("capsuleText").getString("content"))
        assertEquals("os.kei.test", detail.getJSONObject("homePage").getString("packageName"))
        assertIs<String>(detail.get("color"))
        assertEquals(
            "1",
            org.json.JSONObject(detail.getString("color")).getString("templateColor"),
        )
    }

    @Test
    fun lifecycleKeepsOrderIdentityAndSupportsBothDismissalSemantics() {
        val initial = orderFeedback()
        for (status in HonorOrderFeedback.Status.entries) {
            val json = initial.copy(status = status, endTimeMs = 1770000001000).toJson()
            assertEquals(initial.orderNo, json.getString("orderNo"))
            assertEquals(status.code, json.getInt("statusCode"))
            assertEquals("1770000001000", json.get("endTime"))
        }
    }

    @Test
    fun progressSelectsProgressLayoutAndRejectsOutOfRangeValues() {
        val presentation = orderFeedback().presentation
        for (progress in listOf(0, 50, 100)) {
            val detail = presentation.copy(progress = progress).toJson("os.kei.test")
            assertEquals("event_progress", detail.getString("layout"))
            assertEquals(progress, detail.getInt("progress"))
        }
        assertFailsWith<IllegalArgumentException> { presentation.copy(progress = 101) }
        assertFailsWith<IllegalArgumentException> { presentation.copy(progress = -1) }
        assertFailsWith<IllegalArgumentException> { presentation.copy(templateColor = "5") }
        assertFailsWith<IllegalArgumentException> { orderFeedback().copy(intentType = "") }
    }
}

internal fun orderFeedback() =
    HonorOrderFeedback(
        intentType = "test-assigned-scene",
        packageName = "os.kei.test",
        orderNo = "stable-order-id",
        status = HonorOrderFeedback.Status.Show,
        startTimeMs = 1770000000000,
        createTimeMs = 1770000000000,
        presentation =
            HonorOrderPresentation(
                businessType = "Reminder",
                title = "Title",
                content = "Content",
                buttonText = "Open",
                iconUrl = "https://example.com/icon.png",
                deepLink = "test://reminder/stable-order-id",
                capsuleBusinessName = "Task",
                capsuleContent = "Ready",
                capsuleIconUrl = "https://example.com/capsule.png",
                templateColor = "1",
            ),
    )
