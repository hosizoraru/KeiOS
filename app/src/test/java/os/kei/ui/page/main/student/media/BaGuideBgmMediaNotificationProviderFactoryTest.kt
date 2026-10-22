package os.kei.ui.page.main.student

import android.app.Application
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import os.kei.R
import os.kei.ui.page.main.student.media.BA_GUIDE_BGM_MEDIA_NOTIFICATION_ID
import os.kei.ui.page.main.student.media.BaGuideBgmMediaNotificationProviderFactory
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(UnstableApi::class)
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
class BaGuideBgmMediaNotificationProviderFactoryTest {
    @Test
    fun `media small icon follows the monochrome contract on every OEM`() {
        assertEquals(R.drawable.ic_launcher_monochrome, BaGuideBgmMediaOemCompat.mediaSmallIconRes())
    }

    @Test
    fun `island sharing requires a HyperOS island generation rather than a brand name`() {
        assertTrue(BaGuideBgmMediaOemCompat.mediaIslandDragShareSupported("OS3.0"))
        assertTrue(BaGuideBgmMediaOemCompat.mediaIslandDragShareSupported("OS4"))
        assertTrue(BaGuideBgmMediaOemCompat.mediaIslandDragShareSupported("", "OS4.0.4.0.XBLCNXM"))
        listOf("", "V816", "OS2.0", "xiaomi", "HyperOS", "OSx").forEach { version ->
            assertFalse(BaGuideBgmMediaOemCompat.mediaIslandDragShareSupported(version), version)
        }
    }

    @Test
    fun `media source icon remains square`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val icon = ContextCompat.getDrawable(
            context,
            BA_GUIDE_BGM_MEDIA_SMALL_ICON_RES
        )

        assertNotNull(icon)
        assertEquals(icon.intrinsicWidth, icon.intrinsicHeight)
    }

    @Test
    fun `provider wraps the default Media3 notification implementation`() {
        val provider = BaGuideBgmMediaNotificationProviderFactory.create(
            ApplicationProvider.getApplicationContext()
        )

        assertEquals(
            DefaultMediaNotificationProvider.DEFAULT_NOTIFICATION_ID,
            BA_GUIDE_BGM_MEDIA_NOTIFICATION_ID
        )
        assertNotNull(provider)
        assertTrue(provider is BaGuideBgmMediaIslandShareNotificationProvider)
    }
}
