package os.kei.appwidget.github

import android.app.Application
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class GitHubWidgetPinningTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val store get() = GitHubWidgetStateStore(context)

    @Test
    fun `system fill in supplies the new id without changing the callback target or request`() {
        val provider = GitHubWidgetUpdater.provider(context)
        val manager = AppWidgetManager.getInstance(context)
        shadowOf(manager).addInstalledProvider(AppWidgetProviderInfo().apply { this.provider = provider })
        shadowOf(manager).bindAppWidgetId(143, provider)
        val token = store.beginPinRequest(GitHubWidgetConfiguration(true), intArrayOf())
        val callback = GitHubWidgetPinning.callback(context, token)
        callback.send(
            context, 0,
            Intent("forged").setComponent(ComponentName("other", "other.Receiver"))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 143),
        )
        val delivered = shadowOf(context).broadcastIntents.last {
            it.action == GitHubWidgetPinning.confirmationAction(context)
        }
        assertEquals(ComponentName(context, GitHubWidgetPinReceiver::class.java), delivered.component)
        assertEquals(token, delivered.data?.lastPathSegment)
        assertEquals(143, delivered.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1))
        assertTrue(GitHubWidgetPinning.complete(context, delivered))
        assertEquals(143, store.pinResult(token))
        assertTrue(store.configuration(143).showProjectNames)
        assertFalse(GitHubWidgetPinning.complete(context, delivered))
        assertFailsWith<PendingIntent.CanceledException> { callback.send() }
    }

    @Test
    fun `foreign ids and missing request identity cannot configure a widget`() {
        val foreign = ComponentName("other", "other.Widget")
        val manager = AppWidgetManager.getInstance(context)
        shadowOf(manager).addInstalledProvider(AppWidgetProviderInfo().apply { provider = foreign })
        shadowOf(manager).bindAppWidgetId(144, foreign)
        val token = store.beginPinRequest(GitHubWidgetConfiguration(true), intArrayOf())
        val intent = shadowOf(GitHubWidgetPinning.callback(context, token)).savedIntent
        assertFalse(GitHubWidgetPinning.complete(context, Intent(intent).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 144)))
        assertFalse(GitHubWidgetPinning.complete(context, Intent(intent).setData(null)))
        assertEquals(null, store.pinResult(token))
        assertFalse(store.configuration(144).showProjectNames)
    }

    @Test
    fun `ordinary widget pinning uses a standard preview without vendor catalog parameters`() {
        val extras = GitHubWidgetPinning.extras(
            context, GitHubWidgetSnapshot(), GitHubWidgetConfiguration(),
        )
        assertFalse(extras.containsKey("addType"))
        assertFalse(extras.containsKey("widgetName"))
        assertTrue(extras.containsKey(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW))
    }

    @Test
    fun `confirmation receiver is private and shares the lightweight provider process`() {
        val receiver = context.packageManager.getReceiverInfo(
            ComponentName(context, GitHubWidgetPinReceiver::class.java), PackageManager.GET_META_DATA,
        )
        assertFalse(receiver.exported)
        assertEquals("${context.packageName}:widgetProvider", receiver.processName)
    }
}
