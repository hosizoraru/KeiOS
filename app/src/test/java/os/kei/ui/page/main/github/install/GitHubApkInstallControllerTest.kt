package os.kei.ui.page.main.github.install

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import os.kei.R
import os.kei.feature.github.data.remote.GitHubApkInfoRepository
import os.kei.feature.github.data.remote.GitHubApkManifestInfoCache
import os.kei.feature.github.data.remote.GitHubReleaseAssetFile
import os.kei.feature.github.install.GitHubApkInstallProgress
import os.kei.feature.github.install.GitHubApkInstallRequest
import os.kei.feature.github.install.GitHubApkInstallResult
import os.kei.feature.github.install.GitHubManagedApkInstaller
import os.kei.feature.github.model.GitHubApkManifestInfo
import os.kei.feature.github.model.GitHubInstalledPackageInfo
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.ui.page.main.github.page.GitHubApkInfoDetailRequest
import os.kei.ui.page.main.github.page.GitHubManagedInstallConfirmRequest
import os.kei.ui.page.main.github.page.githubApkInfoKey
import os.kei.ui.page.main.github.page.githubManagedInstallKey

@Config(application = Application::class, sdk = [35])
@RunWith(AndroidJUnit4::class)
class GitHubApkInstallControllerTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val pageJob = SupervisorJob()
    private val installJob = SupervisorJob()
    private val controllers = mutableListOf<GitHubApkInstallController>()

    @After
    fun tearDown() {
        controllers.forEach(GitHubApkInstallController::dispose)
        pageJob.cancel()
        installJob.cancel()
        GitHubManagedInstallRuns.running.clear()
    }

    @Test
    fun infoOpensTheSheetForTheRowsFileAndInspectsIt() {
        val asset = apkAsset("info")
        val controller = controller()

        controller.openApkInfo(TRACK, asset)
        pageJob.awaitChildren()

        assertEquals(GitHubApkInfoDetailRequest(TRACK, asset), controller.state.apkInfoDetailRequest)
        assertEquals(INSPECTED, controller.state.apkInfoResults[asset.githubApkInfoKey()])
        controller.dismissApkInfo()
        assertNull(controller.state.apkInfoDetailRequest)
    }

    @Test
    fun installOpensTheConfirmWhenKeiOsInstallsTheFileItself() {
        val asset = apkAsset("confirm")
        val host = RecordingHost()
        val controller = controller(host = host, lookupConfig = GitHubLookupConfig(appManagedShareInstallEnabled = true))

        controller.installOrFallback(TRACK, asset)
        pageJob.awaitChildren()

        assertEquals(GitHubManagedInstallConfirmRequest(TRACK, asset), controller.state.managedInstallConfirmRequest)
        assertTrue(host.downloads.isEmpty(), "A file KeiOS installs is not also handed to the downloader")
    }

    @Test
    fun installHandsTheFileToTheDownloaderWhenKeiOsWillNot() {
        val asset = apkAsset("fallback")
        val host = RecordingHost()
        val controller = controller(host = host, lookupConfig = GitHubLookupConfig(appManagedShareInstallEnabled = false))

        controller.installOrFallback(TRACK, asset)
        pageJob.awaitChildren()

        assertEquals(listOf(asset), host.downloads)
        assertNull(controller.state.managedInstallConfirmRequest)
    }

    @Test
    fun aConfirmedInstallRunsOnceAndMovesWhatThePageSaysIsInstalled() {
        installPackage(versionName = "1.4.1", versionCode = 229L)
        assertNotNull(loadInstalledPackageInfo(application, TRACK.packageName))
        val asset = apkAsset("rollback")
        val host = RecordingHost()
        val installer = FakeInstaller(GitHubApkInstallResult.Succeeded("request", 7, TRACK.packageName))
        val controller =
            controller(
                host = host,
                lookupConfig = GitHubLookupConfig(appManagedShareInstallEnabled = true),
                installer = installer,
            )

        controller.openManagedInstallConfirm(TRACK, asset)
        controller.confirmManagedInstall()
        installJob.awaitChildren()

        assertEquals(1, installer.requests.size)
        assertEquals(asset.downloadUrl, installer.requests.single().resolvedDownloadUrl)
        assertNull(controller.state.managedInstallConfirmRequest, "A confirm is used up by confirming it")
        assertEquals("1.4.1", host.installed.single().versionName)
        assertEquals(229L, host.installed.single().versionCode)
        assertTrue(controller.state.managedInstallLoading.isEmpty(), "Nothing is still marked as installing")
    }

    @Test
    fun aRunningInstallShowsOnEverySurfaceThatHasTheFile() {
        val asset = apkAsset("running")
        val release = CompletableDeferred<GitHubApkInstallResult>()
        val historyPage = controller(installer = FakeInstaller(release), lookupConfig = MANAGED)
        val trackedCard = controller(lookupConfig = MANAGED)

        historyPage.openManagedInstallConfirm(TRACK, asset)
        historyPage.confirmManagedInstall()
        awaitUntil { trackedCard.managedInstallRunning(TRACK, asset) }

        // Started from a history page, running on the tracked card too: the two pages hold their own state,
        // but whether a file is being installed is one fact for the whole app.
        assertTrue(trackedCard.managedInstallRunning(TRACK, asset))
        assertTrue(trackedCard.state.managedInstallLoading[TRACK.githubManagedInstallKey(asset)] == true)
        release.complete(GitHubApkInstallResult.Cancelled(requestId = "request", sessionId = 0))
        installJob.awaitChildren()
        assertTrue(!trackedCard.managedInstallRunning(TRACK, asset))
    }

    @Test
    fun anInstallOutlivesThePageThatConfirmedIt() {
        val asset = apkAsset("outlives")
        val release = CompletableDeferred<GitHubApkInstallResult>()
        val installer = FakeInstaller(release)
        val host = RecordingHost()
        val controller = controller(host = host, installer = installer, lookupConfig = MANAGED)

        controller.openManagedInstallConfirm(TRACK, asset)
        controller.confirmManagedInstall()
        awaitUntil { installer.requests.isNotEmpty() }
        // The history page is closed: its composition scope goes, the install does not.
        controller.dispose()
        pageJob.cancel()
        release.complete(GitHubApkInstallResult.Succeeded("request", 7, TRACK.packageName))
        installJob.awaitChildren()

        assertTrue(
            application.getString(R.string.github_toast_page_install_completed, TRACK.appLabel) in host.toasts,
            "The install finished and said so, after the page that started it had gone: ${host.toasts}",
        )
        assertTrue(!controller.managedInstallRunning(TRACK, asset))
    }

    @Test
    fun aDismissedConfirmCannotBeConfirmedLater() {
        val asset = apkAsset("dismissed")
        val host = RecordingHost()
        val installer = FakeInstaller(GitHubApkInstallResult.Succeeded("request", 7, TRACK.packageName))
        val controller = controller(host = host, lookupConfig = MANAGED, installer = installer)

        controller.openManagedInstallConfirm(TRACK, asset)
        controller.dismissManagedInstallConfirm()
        controller.confirmManagedInstall()
        installJob.awaitChildren()

        assertNull(controller.state.managedInstallConfirmRequest)
        assertTrue(installer.requests.isEmpty())
        assertEquals(listOf(application.getString(R.string.github_page_install_confirm_expired)), host.toasts)
    }

    private fun controller(
        host: RecordingHost = RecordingHost(),
        lookupConfig: GitHubLookupConfig = GitHubLookupConfig(),
        installer: GitHubManagedApkInstaller = FakeInstaller(GitHubApkInstallResult.Cancelled("request", 0)),
    ): GitHubApkInstallController {
        val repository = GitHubApkInfoRepository(manifestCache = ManifestForEveryAsset)
        return GitHubApkInstallController(
            context = application,
            scope = CoroutineScope(pageJob + Dispatchers.Main.immediate),
            state = GitHubApkInstallState(),
            lookupConfig = { lookupConfig },
            host = host,
            apkInfoRepository = repository,
            runner = GitHubManagedInstallRunner(repository, installer),
            installScope = CoroutineScope(installJob + Dispatchers.Main.immediate),
        ).also(controllers::add)
    }

    private fun installPackage(
        versionName: String,
        versionCode: Long,
    ) {
        val info =
            PackageInfo().apply {
                packageName = TRACK.packageName
                this.versionName = versionName
                longVersionCode = versionCode
                applicationInfo =
                    ApplicationInfo().apply {
                        packageName = TRACK.packageName
                        flags = ApplicationInfo.FLAG_INSTALLED or ApplicationInfo.FLAG_HAS_CODE
                    }
            }
        shadowOf(application.packageManager).installPackage(info)
    }

    /** Waits for what this job launched. */
    private fun Job.awaitChildren() {
        awaitUntil { children.none { it.isActive } }
    }

    /**
     * Polls [condition], running the main looper in between: Robolectric keeps it paused, and every
     * continuation that comes back from the network dispatcher to Main is queued on it.
     */
    private fun awaitUntil(condition: () -> Boolean) {
        val looper = shadowOf(android.os.Looper.getMainLooper())
        runBlocking {
            withTimeout(5_000) {
                while (true) {
                    looper.idle()
                    if (condition()) break
                    delay(10)
                }
            }
        }
        looper.idle()
    }

    private class RecordingHost : GitHubApkInstallHost {
        val toasts = mutableListOf<String>()
        val downloads = mutableListOf<GitHubReleaseAssetFile>()
        val installed = mutableListOf<GitHubInstalledPackageInfo>()

        override fun toast(message: String) {
            toasts += message
        }

        override suspend fun downloadInstead(asset: GitHubReleaseAssetFile) {
            downloads += asset
        }

        override fun onInstalled(
            item: GitHubTrackedApp,
            installedInfo: GitHubInstalledPackageInfo,
            appLabel: String,
        ) {
            installed += installedInfo
        }
    }

    private class FakeInstaller(
        private val result: CompletableDeferred<GitHubApkInstallResult>,
    ) : GitHubManagedApkInstaller {
        constructor(result: GitHubApkInstallResult) : this(CompletableDeferred(result))

        val requests = mutableListOf<GitHubApkInstallRequest>()

        override suspend fun stage(
            context: Context,
            request: GitHubApkInstallRequest,
            onProgress: suspend (GitHubApkInstallProgress) -> Unit,
        ): GitHubApkInstallResult = error("The runner installs in one step")

        override suspend fun commit(
            context: Context,
            request: GitHubApkInstallRequest,
            sessionId: Int,
            downloadedBytes: Long,
            totalBytes: Long,
            onProgress: suspend (GitHubApkInstallProgress) -> Unit,
        ): GitHubApkInstallResult = error("The runner installs in one step")

        override suspend fun install(
            context: Context,
            request: GitHubApkInstallRequest,
            onProgress: suspend (GitHubApkInstallProgress) -> Unit,
        ): GitHubApkInstallResult {
            requests += request
            return result.await()
        }
    }

    /** Answers every inspection from "cache", so nothing here reads a real APK over the network. */
    private object ManifestForEveryAsset : GitHubApkManifestInfoCache {
        override fun load(
            cacheKey: String,
            refreshIntervalHours: Int,
        ): GitHubApkManifestInfo = INSPECTED

        override fun save(
            cacheKey: String,
            info: GitHubApkManifestInfo,
        ) = Unit

        override fun remove(cacheKey: String) = Unit
    }

    private companion object {
        val TRACK =
            GitHubTrackedApp(
                repoUrl = "https://github.com/MatsuriDayo/NekoBoxForAndroid",
                owner = "MatsuriDayo",
                repo = "NekoBoxForAndroid",
                packageName = "moe.nb4a",
                appLabel = "NekoBox",
            )
        val MANAGED = GitHubLookupConfig(appManagedShareInstallEnabled = true)

        /** A distinct file per test: the repository keeps a process-wide cache of what it inspected. */
        fun apkAsset(tag: String): GitHubReleaseAssetFile =
            GitHubReleaseAssetFile(
                name = "NekoBox-1.4.1-$tag-arm64-v8a.apk",
                downloadUrl = "https://github.com/MatsuriDayo/NekoBoxForAndroid/releases/download/1.4.1/NekoBox-1.4.1-$tag-arm64-v8a.apk",
                sizeBytes = 14_500_000L,
                downloadCount = 0,
            )

        val INSPECTED = GitHubApkManifestInfo(assetName = "NekoBox", packageName = "moe.nb4a", versionName = "1.4.1")
    }
}
