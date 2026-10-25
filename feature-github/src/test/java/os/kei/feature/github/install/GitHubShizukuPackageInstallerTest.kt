package os.kei.feature.github.install

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageInstaller
import android.os.Parcel
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import os.kei.feature.github.data.remote.GitHubReleaseAssetFile
import os.kei.feature.github.model.GitHubLookupConfig
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
class GitHubShizukuPackageInstallerTest {
    @Test
    fun `install session preserves the disabled setting across framework serialization`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        ShizukuPackageInstallerBridge().applySessionParams(params, context, "demo.app")

        val parcel = Parcel.obtain()
        try {
            params.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            val restored = PackageInstaller.SessionParams.CREATOR.createFromParcel(parcel)

            assertTrue(ReflectionHelpers.getField(restored, "applicationEnabledSettingPersistent"))
            assertEquals("demo.app", ReflectionHelpers.getField(restored, "appPackageName"))
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun `installer fails before session when shizuku is unavailable`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val installer = GitHubShizukuPackageInstaller(
            bridge = FakeBridge(
                ShizukuPackageInstallerCapability(
                    available = false,
                    failureReason = GitHubApkInstallFailureReason.ShizukuUnavailable,
                    message = "No Shizuku"
                )
            ),
            client = OkHttpClient()
        )

        val result = installer.install(context, request())

        val failed = assertIs<GitHubApkInstallResult.Failed>(result)
        assertEquals(GitHubApkInstallFailureReason.ShizukuUnavailable, failed.reason)
        assertEquals("No Shizuku", failed.message)
    }

    @Test
    fun `installer rejects non https resolved url before creating session`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val installer = GitHubShizukuPackageInstaller(
            bridge = FakeBridge(ShizukuPackageInstallerCapability(available = true)),
            client = OkHttpClient()
        )

        val result = installer.install(
            context = context,
            request = request(resolvedDownloadUrl = "http://example.com/app.apk")
        )

        val failed = assertIs<GitHubApkInstallResult.Failed>(result)
        assertEquals(GitHubApkInstallFailureReason.DownloadUrlInvalid, failed.reason)
    }

    @Test
    fun `request id can be scoped by runtime package name`() {
        val requestId = GitHubApkInstallRequestIds.newId("os.kei.debug")

        assertEquals(true, requestId.startsWith("os.kei.debug-github-apk-"))
    }

    @Test
    fun `install result action is scoped by runtime package name`() {
        assertEquals(
            "os.kei.debug.github.install.action.SHIZUKU_INSTALL_RESULT",
            GitHubShizukuInstallCommitRegistry.installResultAction("os.kei.debug")
        )
        assertEquals(
            "os.kei.benchmark.github.install.action.SHIZUKU_INSTALL_RESULT",
            GitHubShizukuInstallCommitRegistry.installResultAction("os.kei.benchmark")
        )
    }

    @Test
    fun `commit result pending intent is mutable for package installer fill in result`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val pendingIntent = GitHubShizukuInstallCommitRegistry.buildPendingIntent(
            context = context,
            requestId = "request-1",
            sessionId = 42
        )
        val shadow = shadowOf(pendingIntent)

        assertTrue(shadow.flags and PendingIntent.FLAG_MUTABLE != 0)
        assertTrue(shadow.flags and PendingIntent.FLAG_UPDATE_CURRENT != 0)
    }

    private fun request(
        resolvedDownloadUrl: String = "https://example.com/app.apk"
    ): GitHubApkInstallRequest {
        return GitHubApkInstallRequest(
            owner = "owner",
            repo = "repo",
            releaseTag = "v1.0.0",
            projectUrl = "https://github.com/owner/repo",
            asset = GitHubReleaseAssetFile(
                name = "app.apk",
                downloadUrl = "https://example.com/app.apk",
                sizeBytes = 128L,
                downloadCount = 0
            ),
            lookupConfig = GitHubLookupConfig(appManagedShareInstallEnabled = true),
            scannedPackageName = "demo.app",
            resolvedDownloadUrl = resolvedDownloadUrl
        )
    }

    private class FakeBridge(
        private val capability: ShizukuPackageInstallerCapability
    ) : ShizukuPackageInstallerBridge() {
        override fun checkCapability(): ShizukuPackageInstallerCapability = capability

        override fun packageInstaller(context: Context): android.content.pm.PackageInstaller {
            error("PackageInstaller should not be requested")
        }
    }
}
