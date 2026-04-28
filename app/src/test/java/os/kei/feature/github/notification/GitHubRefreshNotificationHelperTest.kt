package os.kei.feature.github.notification

import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(
    application = GitHubRefreshNotificationHelperTestApp::class,
    sdk = [35]
)
class GitHubRefreshNotificationHelperTest {
    @Test
    fun `mi island open action uses focus pending intent`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val state = createRefreshState(running = false)
        val notification = invokeMiIslandNotification(context, state)
        val notificationOpenPendingIntent = invokePendingIntentMethod("buildOpenPendingIntent", context)
        val focusOpenPendingIntent = invokePendingIntentMethod("buildFocusOpenPendingIntent", context)
        val focusOpenAction = notification.focusAction("github_action_open")
        val focusParam = notification.extras.getString("miui.focus.param").orEmpty()

        assertEquals(notificationOpenPendingIntent, notification.contentIntent)
        assertEquals(focusOpenPendingIntent, focusOpenAction.actionIntent)
        assertTrue(focusParam.contains("github_action_open"))
    }

    private fun createRefreshState(running: Boolean): Any {
        val stateClass = refreshStateClass()
        return stateClass.getDeclaredConstructor(
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        ).apply {
            isAccessible = true
        }.newInstance(
            4,
            4,
            1,
            2,
            0,
            running,
            false,
            100
        )
    }

    private fun invokeMiIslandNotification(
        context: Context,
        state: Any
    ): Notification {
        val method = GitHubRefreshNotificationHelper::class.java.getDeclaredMethod(
            "buildMiIslandNotification",
            Context::class.java,
            refreshStateClass(),
            Boolean::class.javaPrimitiveType
        ).apply {
            isAccessible = true
        }
        return method.invoke(
            GitHubRefreshNotificationHelper,
            context,
            state,
            true
        ) as Notification
    }

    private fun invokePendingIntentMethod(
        methodName: String,
        context: Context
    ): PendingIntent {
        val method = GitHubRefreshNotificationHelper::class.java.getDeclaredMethod(
            methodName,
            Context::class.java
        ).apply {
            isAccessible = true
        }
        return method.invoke(GitHubRefreshNotificationHelper, context) as PendingIntent
    }

    private fun refreshStateClass(): Class<*> {
        return Class.forName(
            "os.kei.feature.github.notification.GitHubRefreshNotificationHelper\$RefreshState"
        )
    }

    private fun Notification.focusAction(key: String): Notification.Action {
        val actions = extras.getBundle("miui.focus.actions")
        assertNotNull(actions, "Focus actions bundle should be present")
        return actions.getActionCompat(key)
    }

    @Suppress("DEPRECATION")
    private fun Bundle.getActionCompat(key: String): Notification.Action {
        return getParcelable<Notification.Action>(key)
            ?: error("Missing focus action: $key")
    }
}

class GitHubRefreshNotificationHelperTestApp : Application()
