package os.kei.feature.notification

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import os.kei.MainActivity
import os.kei.feature.github.model.GitHubActionsRecommendedRunSnapshot
import os.kei.feature.github.notification.GitHubActionsUpdateNotificationHelper
import os.kei.ui.page.main.ba.BaCalendarPoolNotificationDestination
import os.kei.ui.page.main.ba.BaCalendarPoolNotificationDispatcher
import os.kei.ui.page.main.ba.baCalendarPoolServerIndexOrNull
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
class NotificationPendingIntentIsolationTest {
    private val context: Application
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `actions and calendar entries keep their own targets when request codes overlap`() {
        val calendar = calendarPendingIntent(
            focus = false,
            notificationId = 389_399_000,
            serverIndex = 0,
        )
        val actions = actionsPendingIntent(notificationId = shadowOf(calendar).requestCode)

        assertNotEquals(calendar, actions)
        assertEquals(
            MainActivity.TARGET_ROUTE_BA_ACTIVITY_CALENDAR,
            shadowOf(calendar).savedIntent.getStringExtra(MainActivity.EXTRA_TARGET_ROUTE),
        )
        assertEquals(
            MainActivity.TARGET_BOTTOM_PAGE_GITHUB,
            shadowOf(actions).savedIntent.getStringExtra(MainActivity.EXTRA_TARGET_BOTTOM_PAGE),
        )
    }

    @Test
    fun `calendar focus and body entries cannot overwrite another server`() {
        val focus = calendarPendingIntent(
            focus = true,
            notificationId = 389_399_000,
            serverIndex = 0,
        )
        val body = calendarPendingIntent(
            focus = false,
            notificationId = 389_401_000,
            serverIndex = 1,
        )

        assertEquals(shadowOf(focus).requestCode, shadowOf(body).requestCode)
        assertNotEquals(focus, body)
        assertEquals(0, shadowOf(focus).savedIntent.baCalendarPoolServerIndexOrNull())
        assertEquals(1, shadowOf(body).savedIntent.baCalendarPoolServerIndexOrNull())
    }

    @Test
    fun `rebuilding the same calendar entry updates its existing token`() {
        val first = calendarPendingIntent(focus = false, notificationId = 389_399_000, serverIndex = 0)
        val updated = calendarPendingIntent(focus = false, notificationId = 389_399_000, serverIndex = 2)

        assertEquals(first, updated)
        assertEquals(2, shadowOf(first).savedIntent.baCalendarPoolServerIndexOrNull())
    }

    private fun calendarPendingIntent(
        focus: Boolean,
        notificationId: Int,
        serverIndex: Int,
    ): PendingIntent {
        val method = BaCalendarPoolNotificationDispatcher::class.java.getDeclaredMethod(
            if (focus) "focusOpenBaPendingIntent" else "openBaPendingIntent",
            Context::class.java,
            Int::class.javaPrimitiveType,
            BaCalendarPoolNotificationDestination::class.java,
            Int::class.javaPrimitiveType,
        )
        method.isAccessible = true
        return method.invoke(
            BaCalendarPoolNotificationDispatcher,
            context,
            notificationId,
            BaCalendarPoolNotificationDestination.Calendar,
            serverIndex,
        ) as PendingIntent
    }

    private fun actionsPendingIntent(notificationId: Int): PendingIntent {
        val snapshot = GitHubActionsRecommendedRunSnapshot(
            trackId = "hosizoraru/KeiOS|os.kei",
            owner = "hosizoraru",
            repo = "KeiOS",
            appLabel = "KeiOS",
            workflowId = 1L,
            workflowName = "Build",
            workflowPath = ".github/workflows/build.yml",
            runId = 1L,
            runNumber = 1L,
            runAttempt = 1,
            runDisplayName = "Build",
            headBranch = "master",
            headSha = "",
            event = "push",
            status = "completed",
            conclusion = "success",
            htmlUrl = "",
            artifactCount = 1,
            androidArtifactCount = 1,
            createdAtMillis = 1L,
            updatedAtMillis = 1L,
            checkedAtMillis = 1L,
        )
        val method = GitHubActionsUpdateNotificationHelper::class.java.getDeclaredMethod(
            "buildOpenPendingIntent",
            Context::class.java,
            GitHubActionsRecommendedRunSnapshot::class.java,
            Int::class.javaPrimitiveType,
        )
        method.isAccessible = true
        return method.invoke(GitHubActionsUpdateNotificationHelper, context, snapshot, notificationId) as PendingIntent
    }
}
