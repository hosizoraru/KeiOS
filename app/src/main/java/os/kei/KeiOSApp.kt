package os.kei

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.StatFs
import coil3.ImageLoader
import coil3.SingletonImageLoader
import os.kei.memory.AppMemoryRelease
import os.kei.platform.Android17StrictModeProbes
import os.kei.memory.AppMemoryReleaseLevel
import os.kei.memory.ItgsaFairMemoryReceiver
import coil3.disk.DiskCache
import coil3.gif.AnimatedImageDecoder
import coil3.memory.MemoryCache
import coil3.request.allowHardware
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path.Companion.toOkioPath
import os.kei.core.background.AppBackgroundScheduler
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.log.AppLogLevel
import os.kei.core.log.AppLogger
import os.kei.core.perf.Android17AnomalyProfiler
import os.kei.core.prefs.UiPrefs
import os.kei.core.system.AppBuildEnv
import os.kei.core.system.AppPackageChangedEvent
import os.kei.core.system.AppPackageChangedEvents
import os.kei.feature.github.data.local.GitHubInstalledAppRepository
import os.kei.feature.github.domain.GitHubAppInstallHistoryService
import os.kei.ui.page.main.github.share.GitHubShareImportFlowCoordinator
import os.kei.ui.page.main.github.share.GitHubShareImportPendingScheduler
import os.kei.ui.page.main.student.BaStudentGuideStore
import os.kei.ui.page.main.sync.WebDavAutoSync
import os.kei.core.privilege.PrivilegeMode
import os.kei.core.privilege.PrivilegeModeRuntime

private const val COIL_MEMORY_CACHE_PERCENT = 0.25
private const val COIL_DISK_CACHE_DEFAULT_BYTES = 96L * 1024L * 1024L
private const val COIL_DISK_CACHE_MAX_BYTES = 192L * 1024L * 1024L
private const val COIL_DISK_CACHE_FREE_SPACE_RATIO = 0.02
private const val COIL_DISK_CACHE_DIR = "coil_image_cache"
private const val DEFERRED_STARTUP_WORK_DELAY_MS = 2_000L

class KeiOSApp : Application() {
    companion object {
        @Volatile
        private lateinit var instance: KeiOSApp

        val appContext: Application
            get() = instance
    }

    /**
     * Application-scoped supervisor for non-UI background work that should outlive any single
     * Activity but must still cancel when the process is torn down. Use [Dispatchers.Default] so
     * launching warm-up work does not steal the IO pool from in-flight network calls.
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val appInstallHistoryService by lazy { GitHubAppInstallHistoryService() }

    private val packageChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_REPLACED,
                Intent.ACTION_PACKAGE_CHANGED,
                Intent.ACTION_PACKAGE_FULLY_REMOVED -> {
                    val pkg = intent.data?.schemeSpecificPart?.trim().orEmpty()
                    if (pkg.isNotBlank()) {
                        val event = AppPackageChangedEvent(
                            packageName = pkg,
                            action = intent.action.orEmpty(),
                            replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false),
                            uid = intent.getIntExtra(Intent.EXTRA_UID, -1),
                            dataRemoved = intent.getBooleanExtra(Intent.EXTRA_DATA_REMOVED, false),
                            userInitiated = intent.getBooleanExtra(Intent.EXTRA_USER_INITIATED, false),
                            archival = intent.getBooleanExtra(Intent.EXTRA_ARCHIVAL, false),
                        )
                        AppPackageChangedEvents.publish(event)
                        GitHubShareImportFlowCoordinator.handlePackageChangedAsync(
                            this@KeiOSApp,
                            event
                        )
                        applicationScope.launch {
                            appInstallHistoryService.recordPackageChanged(
                                context = this@KeiOSApp,
                                packageName = event.packageName,
                                action = event.action,
                                replacing = event.replacing,
                                broadcastUid = event.uid,
                                broadcastDataRemoved = event.dataRemoved,
                                broadcastUserInitiated = event.userInitiated,
                                broadcastArchival = event.archival,
                                changedAtMillis = event.atMillis,
                            )
                        }
                    }
                    GitHubInstalledAppRepository.invalidateCache()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // First-frame critical path: keep this list as small as possible. Anything that touches
        // MMKV, AlarmManager, or scans tracked-app state should run via [applicationScope] below.
        MMKV.initialize(this)
        BaStudentGuideStore.configure(this)
        AppBuildEnv.configure(
            buildType = BuildConfig.BUILD_TYPE,
            isDebugBuild = BuildConfig.DEBUG,
            applicationId = BuildConfig.APPLICATION_ID,
        )
        PrivilegeModeRuntime.configure(PrivilegeMode.fromStorageId(UiPrefs.getPrivilegeModeId()))
        UiPrefs.configureRuntimeDefaults(
            buildType = BuildConfig.BUILD_TYPE,
            defaultLogLevelId = BuildConfig.DEFAULT_LOG_LEVEL_ID,
        )
        AppLogger.initialize(this, BuildConfig.DEFAULT_LOG_LEVEL_ID)
        AppLogger.setLogLevel(
            UiPrefs.getLogLevel(
                defaultValue = AppLogLevel.fromStorageId(BuildConfig.DEFAULT_LOG_LEVEL_ID),
            ),
        )
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizePercent(context, COIL_MEMORY_CACHE_PERCENT)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.cacheDir.resolve(COIL_DISK_CACHE_DIR).toOkioPath())
                        .maxSizeBytes(resolveCoilDiskCacheBytes(context))
                        .build()
                }
                .allowHardware(true)
                .components {
                    add(AnimatedImageDecoder.Factory())
                }
                .build()
        }
        Android17AnomalyProfiler.install(this)
        registerPackageChangedReceiver()
        registerMemoryPressureHandling()
        Android17StrictModeProbes.install(isDebugBuild = BuildConfig.DEBUG)
        WebDavAutoSync.init(this)
        scheduleDeferredStartupWork()
    }

    /**
     * Answers memory pressure, from Android and from HyperOS's fair-memory mechanism.
     *
     * The app had no answer at all before this: nothing overrode [onTrimMemory] or [onLowMemory], so every
     * cache it held survived until the process died. Registered here rather than deferred with the other
     * startup work because pressure can arrive before a two-second delay elapses, and the caches are already
     * filling by then.
     */
    private fun registerMemoryPressureHandling() {
        AppMemoryRelease.registerAppCaches()
        ItgsaFairMemoryReceiver.register(context = this) {
            // Input callbacks prepare the snapshots; this background callback never reads UI state.
            os.kei.core.state.EditorRecoveryStore.checkpointIfInitialized()
        }
    }

    /**
     * Android's own memory-pressure callback, routed to the shared release path.
     *
     * The two `RUNNING_*` levels that arrive while the app is visible are deliberately ignored by
     * [AppMemoryRelease.levelForTrimMemory]; see its note for why emptying the caches under the user's eyes
     * is the wrong answer to those.
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        val releaseLevel = AppMemoryRelease.levelForTrimMemory(level) ?: return
        applicationScope.launch {
            runCatching { AppMemoryRelease.release(this@KeiOSApp, releaseLevel) }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        applicationScope.launch {
            runCatching { AppMemoryRelease.release(this@KeiOSApp, AppMemoryReleaseLevel.Critical) }
        }
    }

    private fun scheduleDeferredStartupWork() {
        // AlarmManager wiring and pending share-import scheduling both read from MMKV and only feed
        // background notification flows. Push them off the critical path so the first Compose frame
        // is not delayed by tracked-app fan-out work.
        applicationScope.launch {
            delay(DEFERRED_STARTUP_WORK_DELAY_MS)
            runCatching { AppBackgroundScheduler.scheduleAll(this@KeiOSApp) }
            runCatching { GitHubShareImportPendingScheduler.scheduleNext(this@KeiOSApp) }
            runCatching {
                appInstallHistoryService.refreshTrackedInstallSnapshots(this@KeiOSApp)
            }
            runCatching {
                withContext(AppDispatchers.fileIo) {
                    BaStudentGuideStore.migratePayloadsToFileStoreIfNeeded(this@KeiOSApp)
                }
            }
        }
    }

    private fun registerPackageChangedReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
            addDataScheme("package")
        }
        registerReceiver(packageChangedReceiver, filter, RECEIVER_NOT_EXPORTED)
    }

    /**
     * Adaptive Coil 3 disk-cache budget. Picks ~2% of free cache-partition space and clamps to
     * [COIL_DISK_CACHE_DEFAULT_BYTES]…[COIL_DISK_CACHE_MAX_BYTES] so low-storage devices never
     * thrash on cache trim while high-storage devices still get the full Coil benefits.
     */
    private fun resolveCoilDiskCacheBytes(context: Context): Long {
        val available = runCatching {
            val stat = StatFs(context.cacheDir.absolutePath)
            (stat.availableBytes * COIL_DISK_CACHE_FREE_SPACE_RATIO).toLong()
        }.getOrDefault(COIL_DISK_CACHE_DEFAULT_BYTES)
        return available.coerceIn(COIL_DISK_CACHE_DEFAULT_BYTES, COIL_DISK_CACHE_MAX_BYTES)
    }
}
