package os.kei.appwidget.github

import android.app.Activity
import android.app.Application
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class GitHubWidgetConfigurationContractTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `provider is private and runs in a separate process`() {
        val receiver = context.packageManager.getReceiverInfo(
            GitHubWidgetUpdater.provider(context), PackageManager.GET_META_DATA,
        )
        assertFalse(receiver.exported)
        assertEquals("${context.packageName}:widgetProvider", receiver.processName)
        assertTrue(receiver.metaData.getInt("android.appwidget.provider") != 0)
        assertFalse(receiver.metaData.containsKey("miuiWidget"))
        assertFalse(receiver.metaData.containsKey("vivo_widget"))
    }

    @Test
    fun `missing configuration id returns cancellation without opening a guide`() {
        val activity = Robolectric.buildActivity(
            GitHubWidgetConfigureActivity::class.java,
            Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE),
        ).create().get()
        assertTrue(activity.isFinishing)
        assertEquals(Activity.RESULT_CANCELED, shadowOf(activity).resultCode)
    }

    @Test
    fun `foreign widget cannot write this providers configuration`() {
        val manager = AppWidgetManager.getInstance(context)
        val foreign = ComponentName("example.other", "example.other.Widget")
        shadowOf(manager).addInstalledProvider(AppWidgetProviderInfo().apply { provider = foreign })
        shadowOf(manager).bindAppWidgetId(321, foreign)
        val activity = Robolectric.buildActivity(
            GitHubWidgetConfigureActivity::class.java,
            Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 321),
        ).create().get()
        assertTrue(activity.isFinishing)
        assertEquals(Activity.RESULT_CANCELED, shadowOf(activity).resultCode)
    }
}
