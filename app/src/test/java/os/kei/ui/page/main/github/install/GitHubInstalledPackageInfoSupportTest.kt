package os.kei.ui.page.main.github.install

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Process
import android.os.UserHandle
import androidx.test.core.app.ApplicationProvider
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class GitHubInstalledPackageInfoSupportTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `install comparison recognizes hidden installed package and its current version`() {
        val packageName = "test.frozen.installcomparison"
        installFixture(packageName, installed = true)
        ReflectionHelpers.callInstanceMethod<Boolean>(
            context.packageManager,
            "setApplicationHiddenSettingAsUser",
            ClassParameter.from(String::class.java, packageName),
            ClassParameter.from(Boolean::class.javaPrimitiveType, true),
            ClassParameter.from(UserHandle::class.java, Process.myUserHandle()),
        )

        val result = assertNotNull(loadInstalledPackageInfo(context, packageName))

        assertEquals(123L, result.versionCode)
        assertEquals("1.2.3", result.versionName)
    }

    @Test
    fun `install comparison excludes packages retained after uninstall`() {
        installFixture("test.frozen.removed", installed = false)

        assertNull(loadInstalledPackageInfo(context, "test.frozen.removed"))
    }

    private fun installFixture(packageName: String, installed: Boolean) {
        shadowOf(context.packageManager).addPackageNoDefaults(
            PackageInfo().apply {
                this.packageName = packageName
                versionName = "1.2.3"
                setLongVersionCode(123)
                applicationInfo = ApplicationInfo().apply {
                    this.packageName = packageName
                    flags = ApplicationInfo.FLAG_HAS_CODE or
                        (if (installed) ApplicationInfo.FLAG_INSTALLED else 0)
                }
            },
        )
    }
}
