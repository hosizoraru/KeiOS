package os.kei.mcp.notification

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
class McpNotificationOpenIntentsTest {
    private val context: Application
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `different entries cannot overwrite another accounts body intent`() {
        val cases = listOf(
            McpNotificationOpenEntry.Focus to 43_000,
            McpNotificationOpenEntry.ForegroundBootstrap to 143_000,
        )
        cases.forEach { (entry, id) ->
            val first = create(id, "account-a", entry)
            val body = create(343_000, "account-b", McpNotificationOpenEntry.Body)

            assertEquals(shadowOf(first).requestCode, shadowOf(body).requestCode)
            assertNotEquals(first, body)
            assertEquals("account-a", shadowOf(first).savedIntent.getStringExtra(McpAppIntentContract.EXTRA_BA_ACCOUNT_ID))
            assertEquals("account-b", shadowOf(body).savedIntent.getStringExtra(McpAppIntentContract.EXTRA_BA_ACCOUNT_ID))
        }
    }

    @Test
    fun `rebuilding the same entry updates its existing token`() {
        val first = create(43_000, "account-a", McpNotificationOpenEntry.Body)
        val updated = create(43_000, "account-b", McpNotificationOpenEntry.Body)

        assertEquals(first, updated)
        assertEquals("account-b", shadowOf(first).savedIntent.getStringExtra(McpAppIntentContract.EXTRA_BA_ACCOUNT_ID))
    }

    private fun create(id: Int, accountId: String, entry: McpNotificationOpenEntry) =
        McpNotificationOpenIntents.create(
            context = context,
            notificationId = id,
            intent = McpNotificationHelper.buildOpenIntent(
                context = context,
                serverName = McpNotificationPayload.BA_AP_SERVER_NAME,
                targetBaAccountId = accountId,
            ),
            entry = entry,
        )
}
