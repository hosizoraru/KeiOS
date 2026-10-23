package os.kei.ui.document

import android.app.Activity
import android.app.Application
import android.content.ClipDescription
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
class OpenJsonDocumentTest {
    private val contract = OpenJsonDocument()

    @Test
    fun `background image picker requires stream-readable images without a provider pin`() {
        val intent = OpenReadableDocument().createIntent(
            ApplicationProvider.getApplicationContext(),
            arrayOf("image/*"),
        )

        assertEquals(Intent.ACTION_OPEN_DOCUMENT, intent.action)
        assertTrue(intent.hasCategory(Intent.CATEGORY_OPENABLE))
        assertEquals(listOf("image/*"), intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)?.toList())
        assertNull(intent.component)
        assertNull(intent.`package`)
    }

    @Test
    fun `system owns a single openable document request without broad permission or package pin`() {
        val intent = contract.createIntent(ApplicationProvider.getApplicationContext(), Unit)

        assertEquals(Intent.ACTION_OPEN_DOCUMENT, intent.action)
        assertTrue(intent.hasCategory(Intent.CATEGORY_OPENABLE))
        assertEquals("*/*", intent.type)
        assertTrue(intent.hasExtra(Intent.EXTRA_ALLOW_MULTIPLE))
        assertFalse(intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false))
        assertNull(intent.component)
        assertNull(intent.`package`)
        assertEquals(0, intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    @Test
    fun `provider filters include JSON text and generic binary but exclude known unrelated files`() {
        val intent = contract.createIntent(ApplicationProvider.getApplicationContext(), Unit)
        val filters = requireNotNull(intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES))
        fun accepts(mimeType: String) = filters.any { ClipDescription.compareMimeTypes(mimeType, it) }

        assertTrue(accepts("application/json"))
        assertTrue(accepts("text/plain"))
        assertTrue(accepts("text/json"))
        assertTrue(accepts("application/octet-stream"))
        assertFalse(accepts("application/pdf"))
        assertFalse(accepts("image/jpeg"))
        assertFalse(accepts("application/zip"))
    }

    @Test
    fun `successful selection returns the provider URI without converting it to a file path`() {
        val uri = Uri.parse("content://documents.example/document/extensionless%3A42")

        assertEquals(uri, contract.parseResult(Activity.RESULT_OK, Intent().setData(uri)))
    }

    @Test
    fun `cancelled or missing results never import a stale URI`() {
        val stale = Intent().setData(Uri.parse("content://documents.example/old"))

        assertNull(contract.parseResult(Activity.RESULT_CANCELED, stale))
        assertNull(contract.parseResult(Activity.RESULT_OK, null))
        assertNull(contract.parseResult(Activity.RESULT_OK, Intent()))
    }
}
