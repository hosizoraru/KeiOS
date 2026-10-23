package os.kei.ui.page.main.settings.state

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
class SettingsPageRepositoryTest {
    private val context: Application
        get() = ApplicationProvider.getApplicationContext()

    private val backgroundDir: File
        get() = File(context.filesDir, "non_home_background")

    @Before
    fun setUp() {
        backgroundDir.deleteRecursively()
        backgroundDir.mkdirs()
    }

    @After
    fun tearDown() {
        backgroundDir.deleteRecursively()
    }

    @Test
    fun `trim keeps current managed background and removes orphan crops`() {
        val keep = managedBackgroundFile("cropped_non_home_keep.jpg")
        val orphan = managedBackgroundFile("cropped_non_home_orphan.jpg")
        val unrelated = managedBackgroundFile("manual_background.jpg")

        trimManagedNonHomeBackgroundFilesSync(context, managedContentUri(keep).toString())

        assertTrue(keep.exists())
        assertFalse(orphan.exists())
        assertTrue(unrelated.exists())
    }

    @Test
    fun `blank keep clears managed crop files only`() {
        val crop = managedBackgroundFile("cropped_non_home_clear.jpg")
        val unrelated = managedBackgroundFile("manual_background.jpg")

        trimManagedNonHomeBackgroundFilesSync(context, keepUriText = "")

        assertFalse(crop.exists())
        assertTrue(unrelated.exists())
    }

    @Test
    fun `crop keeps source read access scoped and writes to an app owned output`() = runTest {
        val source = Uri.parse("content://readonly.documents/document/portrait")
        val intent = SettingsPageRepository().buildNonHomeBackgroundCropIntent(context, source).getOrThrow()

        assertEquals(context.packageName, intent.component?.packageName)
        assertEquals(source, intent.getParcelableExtra(UCrop.EXTRA_INPUT_URI, Uri::class.java))
        val output = requireNotNull(intent.getParcelableExtra(UCrop.EXTRA_OUTPUT_URI, Uri::class.java))
        assertEquals("${context.packageName}.fileprovider", output.authority)
        assertEquals(0, intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        assertFalse(context.contentResolver.persistedUriPermissions.any { it.uri == source })
    }

    private fun managedBackgroundFile(name: String): File =
        File(backgroundDir, name).also { file ->
            file.writeText(name)
        }

    private fun managedContentUri(file: File): Uri =
        Uri
            .Builder()
            .scheme("content")
            .authority("${context.packageName}.fileprovider")
            .appendEncodedPath("non_home_background/${file.name}")
            .build()
}
