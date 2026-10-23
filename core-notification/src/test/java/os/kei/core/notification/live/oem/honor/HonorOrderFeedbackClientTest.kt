package os.kei.core.notification.live.oem.honor

import android.app.Application
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.RemoteException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
class HonorOrderFeedbackClientTest {
    @Test
    fun deferredConfigurationChecksUseTheApplicationContext() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        var wrapperReads = 0
        val screenContext = object : ContextWrapper(application) {
            override fun getPackageManager(): PackageManager {
                wrapperReads++
                return super.getPackageManager()
            }
        }
        val client = HonorOrderFeedbackClient(screenContext)
        assertEquals(
            HonorFeedbackSubmission.MissingConfiguration,
            client.submit("request-1", orderFeedback().copy(packageName = application.packageName)),
        )
        assertEquals(0, wrapperReads)
    }

    @Test
    fun missingClientConfigurationDoesNotAttemptProviderDelivery() {
        val client = HonorOrderFeedbackClient("os.kei.test", isConfigured = { false }) { _, _, _ ->
            error("must not call provider")
        }
        assertEquals(
            HonorFeedbackSubmission.MissingConfiguration,
            client.submit("request-1", orderFeedback()),
        )
    }

    @Test
    fun sendsDocumentedProviderCallWithoutCloudCredentialsOrCallbackProvider() {
        var calls = 0
        val client =
            HonorOrderFeedbackClient("os.kei.test") { method, arg, extras ->
                calls++
                assertEquals("feedback", method)
                assertEquals("feedbackOrder", arg)
                assertEquals("request-1", extras.getString("reqId"))
                assertEquals("", extras.getString("uri"))
                val data = JSONObject(extras.getString("data")!!)
                assertEquals("stable-order-id", data.getString("orderNo"))
                assertFalse(data.has("uuids"))
                Bundle().apply { putBoolean("result", true) }
            }
        assertEquals(
            HonorFeedbackSubmission.Submitted,
            client.submit("request-1", orderFeedback()),
        )
        assertEquals(1, calls)
    }

    @Test
    fun updatesAndImmediateDismissalsAreNotBlockedByAnInventedVisibilityRequirement() {
        for (status in listOf(
            HonorOrderFeedback.Status.Update,
            HonorOrderFeedback.Status.DismissUsingLatestEndTime,
            HonorOrderFeedback.Status.DismissUsingFirstEndTime,
        )) {
            var calls = 0
            val client = HonorOrderFeedbackClient("os.kei.test") { _, _, extras ->
                calls++
                val json = JSONObject(extras.getString("data")!!)
                assertEquals(status.code, json.getInt("statusCode"))
                assertFalse(json.has("endTime"))
                Bundle().apply { putBoolean("result", true) }
            }
            assertEquals(
                HonorFeedbackSubmission.Submitted,
                client.submit("request-1", orderFeedback().copy(status = status, endTimeMs = null)),
            )
            assertEquals(1, calls)
        }
    }

    @Test
    fun enforcesMinimumIntervalWithoutSleepingOrAutomaticallyRetrying() {
        var timeMs = 0L
        var calls = 0
        val client =
            HonorOrderFeedbackClient("os.kei.test", elapsedRealtimeMs = { timeMs }) { _, _, _ ->
                calls++
                Bundle().apply { putBoolean("result", true) }
            }
        assertEquals(
            HonorFeedbackSubmission.Submitted,
            client.submit("request-1", orderFeedback()),
        )
        timeMs = 799L
        assertEquals(
            HonorFeedbackSubmission.Throttled,
            client.submit("request-2", orderFeedback()),
        )
        timeMs = 800L
        assertEquals(
            HonorFeedbackSubmission.Submitted,
            client.submit("request-2", orderFeedback()),
        )
        assertEquals(2, calls)
    }

    @Test
    fun rejectionAndMissingReplyNeverClaimSuccessfulSubmission() {
        for ((reply, expected) in listOf(
            null to HonorFeedbackSubmission.Unavailable,
            Bundle() to HonorFeedbackSubmission.Rejected,
            Bundle().apply { putBoolean("result", false) } to HonorFeedbackSubmission.Rejected,
        )) {
            val client = HonorOrderFeedbackClient("os.kei.test") { _, _, _ -> reply }
            assertEquals(expected, client.submit("request-1", orderFeedback()))
        }
    }

    @Test
    fun providerPermissionAndDeathFailuresAreContained() {
        for ((failure, expected) in listOf(
            SecurityException("denied") to HonorFeedbackSubmission.PermissionDenied,
            RemoteException("provider died") to HonorFeedbackSubmission.Unavailable,
            IllegalArgumentException("unknown provider") to HonorFeedbackSubmission.Unavailable,
        )) {
            val client = HonorOrderFeedbackClient("os.kei.test") { _, _, _ -> throw failure }
            assertEquals(expected, client.submit("request-1", orderFeedback()))
        }
    }

    @Test
    fun rejectsForeignPackageAndBlankRequestIdentityBeforeIpc() {
        val client =
            HonorOrderFeedbackClient("os.kei.test") { _, _, _ -> error("must not call provider") }
        assertFailsWith<IllegalArgumentException> {
            client.submit("", orderFeedback())
        }
        assertFailsWith<IllegalArgumentException> {
            client.submit("request-1", orderFeedback().copy(packageName = "other.app"))
        }
    }
}
