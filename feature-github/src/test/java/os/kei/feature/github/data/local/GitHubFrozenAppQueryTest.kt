package os.kei.feature.github.data.local

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Process
import android.os.UserHandle
import androidx.test.core.app.ApplicationProvider
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import os.kei.core.system.queryLaunchablePackageInfos

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class GitHubFrozenAppQueryTest {
    private lateinit var context: Context
    private val packageManager: PackageManager get() = context.packageManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        clearCaches()
    }

    @After
    fun clearCaches() {
        GitHubInstalledAppRepository.invalidateCache()
        AppIconCache.clear()
    }

    @Test
    fun `disabled app remains selectable with its local version without enabling it`() {
        installFixture("test.frozen.disabled")
        packageManager.setApplicationEnabledSetting(
            "test.frozen.disabled",
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
            PackageManager.DONT_KILL_APP,
        )

        val app = scannedApp("test.frozen.disabled")

        assertTrue(app.isDisabledOrFrozen)
        assertEquals("Frozen fixture", app.label)
        assertEquals(42L, GitHubInstalledAppRepository.localVersionInfoOrNull(context, app.packageName)?.versionCode)
        assertEquals(
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
            packageManager.getApplicationEnabledSetting(app.packageName),
        )
    }

    @Test
    fun `hidden installed app retains metadata version and icon without unhiding it`() {
        val packageName = "test.frozen.hidden"
        installFixture(packageName)
        shadowOf(packageManager).setApplicationIcon(packageName, ColorDrawable(Color.RED))
        setHidden(packageName)
        assertFailsWith<PackageManager.NameNotFoundException> {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        }

        val app = scannedApp(packageName)

        assertTrue(app.isDisabledOrFrozen)
        assertEquals("Frozen fixture", app.label)
        assertEquals("1.2.3", GitHubInstalledAppRepository.localVersionName(context, packageName))
        val icon = assertNotNull(AppIconCache.getOrLoad(context, packageName))
        assertEquals(Color.RED, icon.getPixel(0, 0))
        assertFailsWith<PackageManager.NameNotFoundException> {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        }
    }

    @Test
    fun `suspended app is classified as frozen while force stopped app stays ordinary`() {
        installFixture("test.frozen.suspended", extraFlags = ApplicationInfo.FLAG_SUSPENDED)
        installFixture("test.frozen.stopped", extraFlags = ApplicationInfo.FLAG_STOPPED)

        assertTrue(scannedApp("test.frozen.suspended").isDisabledOrFrozen)
        assertFalse(scannedApp("test.frozen.stopped").isDisabledOrFrozen)
        assertEquals(42L, GitHubInstalledAppRepository.localVersionCode(context, "test.frozen.suspended"))
    }

    @Test
    fun `retained data and archived entries do not become installed tracking candidates`() {
        val packageName = "test.frozen.removed"
        installFixture(packageName, installed = false)

        assertTrue(
            GitHubInstalledAppRepository.queryInstalledLaunchableApps(context, forceRefresh = true)
                .none { it.packageName == packageName },
        )
        assertNull(GitHubInstalledAppRepository.localVersionInfoOrNull(context, packageName))
        assertNull(AppIconCache.getOrLoad(context, packageName))
    }

    @Test
    fun `system app scope and pinning still apply to disabled apps`() {
        installFixture("test.frozen.system", extraFlags = ApplicationInfo.FLAG_SYSTEM, enabled = false)

        assertTrue(
            GitHubInstalledAppRepository.queryInstalledLaunchableApps(context, includeSystemApps = false)
                .none { it.packageName == "test.frozen.system" },
        )
        assertTrue(
            GitHubInstalledAppRepository.queryInstalledLaunchableApps(
                context,
                includeSystemApps = false,
                pinnedSystemPackageNames = setOf("test.frozen.system"),
            ).single { it.packageName == "test.frozen.system" }.isDisabledOrFrozen,
        )
    }

    @Test
    fun `resource only and test only packages remain excluded`() {
        installFixture("test.frozen.resources", hasCode = false)
        installFixture("test.frozen.testonly", extraFlags = ApplicationInfo.FLAG_TEST_ONLY)

        val packages = GitHubInstalledAppRepository.queryInstalledLaunchableApps(context)
            .map { it.packageName }
        assertFalse("test.frozen.resources" in packages)
        assertFalse("test.frozen.testonly" in packages)
        assertNull(GitHubInstalledAppRepository.localVersionInfoOrNull(context, "test.frozen.testonly"))
    }

    @Test
    fun `launcher fallback retains disabled launchers when explicitly requested`() {
        val packageName = "test.frozen.launcher"
        val info = installFixture(packageName, enabled = false).applicationInfo
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        shadowOf(packageManager).addResolveInfoForIntent(
            intent,
            ResolveInfo().apply {
                activityInfo = ActivityInfo().apply {
                    this.packageName = packageName
                    name = "$packageName.MainActivity"
                    applicationInfo = info
                    enabled = false
                }
            },
        )

        val packages = packageManager.queryLaunchablePackageInfos(
            PackageManager.PackageInfoFlags.of(gitHubInstalledAppQueryFlags),
        )

        assertTrue(packages.any { it.packageName == packageName })
    }

    private fun scannedApp(packageName: String) =
        GitHubInstalledAppRepository.queryInstalledLaunchableApps(context, forceRefresh = true)
            .single { it.packageName == packageName }

    private fun installFixture(
        packageName: String,
        installed: Boolean = true,
        enabled: Boolean = true,
        hasCode: Boolean = true,
        extraFlags: Int = 0,
    ): PackageInfo {
        val info = PackageInfo().apply {
            this.packageName = packageName
            versionName = "1.2.3"
            setLongVersionCode(42L)
            applicationInfo = ApplicationInfo().apply {
                this.packageName = packageName
                this.enabled = enabled
                nonLocalizedLabel = "Frozen fixture"
                flags = extraFlags or
                    (if (installed) ApplicationInfo.FLAG_INSTALLED else 0) or
                    (if (hasCode) ApplicationInfo.FLAG_HAS_CODE else 0)
            }
        }
        shadowOf(packageManager).addPackageNoDefaults(info)
        return info
    }

    private fun setHidden(packageName: String) {
        assertTrue(
            ReflectionHelpers.callInstanceMethod(
                packageManager,
                "setApplicationHiddenSettingAsUser",
                ClassParameter.from(String::class.java, packageName),
                ClassParameter.from(Boolean::class.javaPrimitiveType, true),
                ClassParameter.from(UserHandle::class.java, Process.myUserHandle()),
            ),
        )
    }
}
