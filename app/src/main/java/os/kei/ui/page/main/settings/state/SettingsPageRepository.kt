package os.kei.ui.page.main.settings.state

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import os.kei.R
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.export.ExportJobResult
import os.kei.core.log.AppLogStore
import os.kei.feature.keepalive.accessibility.AccessibilityGuardHistoryEntry
import os.kei.feature.keepalive.accessibility.AccessibilityGuardHistoryStore
import os.kei.feature.keepalive.accessibility.AccessibilityGuardCheckReason
import os.kei.feature.keepalive.accessibility.AccessibilityGuardCheckResult
import os.kei.feature.keepalive.accessibility.AccessibilityGuardRuntime
import os.kei.feature.keepalive.service.AccessibilityGuardForegroundService
import os.kei.ui.page.main.settings.section.SettingsAccessibilityGuardHistoryUiItem
import os.kei.ui.page.main.settings.section.SettingsAccessibilityGuardUiState
import os.kei.ui.page.main.settings.cache.CacheEntrySummary
import os.kei.ui.page.main.settings.cache.CacheStores
import os.kei.ui.page.main.settings.page.SettingsSearchTarget
import os.kei.ui.page.main.settings.page.buildSettingsSearchTargets
import os.kei.ui.page.main.settings.page.deriveSettingsSearchTargets
import os.kei.ui.page.main.sync.WebDavSyncStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

private const val NON_HOME_BACKGROUND_CROP_DIR = "non_home_background"
private const val NON_HOME_BACKGROUND_CROP_FILE_PREFIX = "cropped_non_home_"
private const val NON_HOME_BACKGROUND_CROP_TARGET_SHORT_EDGE = 1440
private const val NON_HOME_BACKGROUND_CROP_MAX_WIDTH = 2560
private const val NON_HOME_BACKGROUND_CROP_MAX_HEIGHT = 4096

internal open class SettingsPageRepository(
    private val ioDispatcher: CoroutineDispatcher = AppDispatchers.fileIo,
    private val defaultDispatcher: CoroutineDispatcher = AppDispatchers.uiDerivation,
) {
    suspend fun listCacheEntries(context: Context): List<CacheEntrySummary> =
        withContext(ioDispatcher) {
            runCatching { CacheStores.list(context) }.getOrDefault(emptyList())
        }

    suspend fun clearAllCaches(context: Context): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching { CacheStores.clearAll(context) }
        }

    suspend fun clearCache(
        context: Context,
        cacheId: String,
    ): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching { CacheStores.clear(context, cacheId) }
        }

    suspend fun loadLogStats(context: Context): AppLogStore.Stats =
        withContext(ioDispatcher) {
            runCatching { AppLogStore.stats(context) }.getOrDefault(AppLogStore.Stats.Empty)
        }

    fun buildWebDavSyncState(): SettingsWebDavSyncUiState {
        val config = WebDavSyncStore.loadConfig()
        return SettingsWebDavSyncUiState(
            configured = config != null,
            username = config?.username.orEmpty(),
            autoSyncEnabled = WebDavSyncStore.isAutoSyncEnabled(),
            autoSyncIntervalHours = WebDavSyncStore.getAutoSyncIntervalHours(),
            lastFullSyncTimeMs = WebDavSyncStore.getLastFullSyncTime(),
        )
    }

    open suspend fun loadWebDavSyncState(): SettingsWebDavSyncUiState =
        withContext(ioDispatcher) {
            buildWebDavSyncState()
        }

    open suspend fun loadAccessibilityGuardState(context: Context): SettingsAccessibilityGuardUiState =
        withContext(ioDispatcher) {
            val appContext = context.applicationContext
            val stateStore = AccessibilityGuardRuntime.newStateStore()
            val settings = stateStore.loadSettings()
            val snapshot =
                AccessibilityGuardRuntime
                    .coordinator(stateStore)
                    .loadSnapshot()
            val history = AccessibilityGuardHistoryStore.forContext(appContext).latest(5)
            val capability = snapshot.capability
            SettingsAccessibilityGuardUiState(
                daemonEnabled = settings.daemonEnabled,
                bootCheckEnabled = settings.bootCheckEnabled,
                screenOnCheckEnabled = settings.screenOnCheckEnabled,
                secureSettingsReadable = capability.canReadSecureSettings,
                privilegeStatus = capability.privilegeStatus,
                activePolicyCount = settings.activePolicyCount,
                historyCount = history.size,
                latestHistory = history.firstOrNull()?.toSettingsUiItem(),
            )
        }

    suspend fun setAccessibilityGuardDaemonEnabled(
        context: Context,
        enabled: Boolean,
    ): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                AccessibilityGuardRuntime.coordinator().setDaemonEnabled(enabled)
                if (enabled) {
                    AccessibilityGuardForegroundService.start(context.applicationContext)
                } else {
                    AccessibilityGuardForegroundService.stop(context.applicationContext)
                }
                Unit
            }
        }

    suspend fun setAccessibilityGuardBootCheckEnabled(enabled: Boolean): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                AccessibilityGuardRuntime.coordinator().setBootCheckEnabled(enabled)
                Unit
            }
        }

    suspend fun setAccessibilityGuardScreenOnEnabled(
        context: Context,
        enabled: Boolean,
    ): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val settings =
                    AccessibilityGuardRuntime
                        .coordinator()
                        .setScreenOnCheckEnabled(enabled)
                if (settings.daemonEnabled) {
                    AccessibilityGuardForegroundService.start(context.applicationContext)
                }
                Unit
            }
        }

    suspend fun runAccessibilityGuardManualCheck(context: Context): Result<AccessibilityGuardCheckResult> =
        withContext(ioDispatcher) {
            runCatching {
                val appContext = context.applicationContext
                val stateStore = AccessibilityGuardRuntime.newStateStore()
                AccessibilityGuardRuntime
                    .checkRunner(
                        context = appContext,
                        stateStore = stateStore,
                    )
                    .checkAndRecord(
                        reason = AccessibilityGuardCheckReason.Manual,
                        triggerAction = "settings_manual_check",
                    )
            }
        }

    suspend fun clearLogs(context: Context): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching { AppLogStore.clear(context) }
        }

    suspend fun exportLogZip(
        context: Context,
        uri: Uri,
    ): ExportJobResult =
        withContext(ioDispatcher) {
            val fileName =
                uri.lastPathSegment
                    ?.substringAfterLast('/')
                    ?.trim()
                    .orEmpty()
                    .ifBlank { "keios-logs.zip" }
            AppLogStore
                .exportZipToUri(context, uri)
                .fold(
                    onSuccess = { ExportJobResult.success(fileName = fileName) },
                    onFailure = { error ->
                        ExportJobResult.failure(
                            fileName = fileName,
                            error = error,
                        )
                    },
                )
        }

    suspend fun buildLogExportFileName(): String =
        withContext(defaultDispatcher) {
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
            "keios-logs-$stamp.zip"
        }

    suspend fun exportAccessibilityGuardHistory(
        context: Context,
        uri: Uri,
    ): ExportJobResult =
        withContext(ioDispatcher) {
            val fileName =
                uri.lastPathSegment
                    ?.substringAfterLast('/')
                    ?.trim()
                    .orEmpty()
                    .ifBlank { "keios-accessibility-guard-history.json" }
            AccessibilityGuardHistoryStore
                .forContext(context.applicationContext)
                .exportToUri(context.applicationContext, uri)
                .fold(
                    onSuccess = { result ->
                        ExportJobResult.success(
                            fileName = fileName,
                            attempted = result.exportedCount.coerceAtLeast(1),
                        )
                    },
                    onFailure = { error ->
                        ExportJobResult.failure(
                            fileName = fileName,
                            error = error,
                        )
                    },
                )
        }

    suspend fun buildAccessibilityGuardHistoryExportFileName(): String =
        withContext(defaultDispatcher) {
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
            "keios-accessibility-guard-history-$stamp.json"
        }

    suspend fun buildSearchTargets(context: Context): List<SettingsSearchTarget> {
        val appContext = context.applicationContext
        return withContext(defaultDispatcher) {
            buildSettingsSearchTargets(appContext::getString)
        }
    }

    suspend fun deriveSearchState(
        targets: List<SettingsSearchTarget>,
        query: String,
    ): SettingsSearchUiState =
        withContext(defaultDispatcher) {
            SettingsSearchUiState(
                matchingTargets =
                    deriveSettingsSearchTargets(
                        targets = targets,
                        query = query,
                    ),
            )
        }

    suspend fun buildNonHomeBackgroundCropIntent(
        context: Context,
        sourceUri: Uri,
    ): Result<Intent> {
        val appContext = context.applicationContext
        return withContext(ioDispatcher) {
            runCatching {
                val outputUri = createNonHomeBackgroundCropOutputUri(appContext)
                val (aspectRatioX, aspectRatioY) = resolveNonHomeBackgroundAspectRatio(appContext)
                val (maxResultWidth, maxResultHeight) = resolveNonHomeBackgroundCropSize(appContext)
                val cropOptions =
                    UCrop.Options().apply {
                        setToolbarTitle(appContext.getString(R.string.settings_non_home_background_crop_title))
                        setCompressionFormat(Bitmap.CompressFormat.JPEG)
                        setCompressionQuality(92)
                        setFreeStyleCropEnabled(false)
                        setHideBottomControls(false)
                        setShowCropFrame(true)
                        setShowCropGrid(true)
                    }
                // UCrop runs in our UID; the selected source only needs its current read grant.
                UCrop
                    .of(sourceUri, outputUri)
                    .withAspectRatio(aspectRatioX, aspectRatioY)
                    .withMaxResultSize(maxResultWidth, maxResultHeight)
                    .withOptions(cropOptions)
                    .getIntent(appContext)
            }
        }
    }

    suspend fun trimManagedNonHomeBackgroundFiles(
        context: Context,
        keepUriText: String,
    ) {
        val appContext = context.applicationContext
        withContext(ioDispatcher) {
            trimManagedNonHomeBackgroundFilesSync(appContext, keepUriText)
        }
    }
}

private fun createNonHomeBackgroundCropOutputUri(context: Context): Uri {
    val dir = File(context.filesDir, NON_HOME_BACKGROUND_CROP_DIR)
    if (!dir.exists()) {
        dir.mkdirs()
    }
    val output =
        File(
            dir,
            "$NON_HOME_BACKGROUND_CROP_FILE_PREFIX${System.currentTimeMillis()}.jpg",
        )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        output,
    )
}

private fun resolveNonHomeBackgroundAspectRatio(context: Context): Pair<Float, Float> {
    val metrics = context.resources.displayMetrics
    val widthPx = metrics.widthPixels.coerceAtLeast(1)
    val heightPx = metrics.heightPixels.coerceAtLeast(1)
    return widthPx.toFloat() to heightPx.toFloat()
}

private fun resolveNonHomeBackgroundCropSize(context: Context): Pair<Int, Int> {
    val metrics = context.resources.displayMetrics
    val widthPx = metrics.widthPixels.coerceAtLeast(1)
    val heightPx = metrics.heightPixels.coerceAtLeast(1)
    val shortEdge = min(widthPx, heightPx).coerceAtLeast(1)
    val upscale =
        (NON_HOME_BACKGROUND_CROP_TARGET_SHORT_EDGE.toFloat() / shortEdge.toFloat())
            .coerceAtLeast(1f)
    val width = (widthPx * upscale).roundToInt().coerceIn(widthPx, NON_HOME_BACKGROUND_CROP_MAX_WIDTH)
    val height = (heightPx * upscale).roundToInt().coerceIn(heightPx, NON_HOME_BACKGROUND_CROP_MAX_HEIGHT)
    return width to height
}

internal fun trimManagedNonHomeBackgroundFilesSync(
    context: Context,
    keepUriText: String,
) {
    val dir = File(context.filesDir, NON_HOME_BACKGROUND_CROP_DIR)
    val keepFile = resolveManagedNonHomeBackgroundFile(context, keepUriText)
    val keepPath = keepFile?.safeCanonicalPath()
    dir
        .listFiles()
        .orEmpty()
        .asSequence()
        .filter { it.isFile }
        .filter { it.name.startsWith(NON_HOME_BACKGROUND_CROP_FILE_PREFIX) }
        .filter { keepPath == null || it.safeCanonicalPath() != keepPath }
        .forEach { file ->
            runCatching { file.delete() }
        }
}

private fun resolveManagedNonHomeBackgroundFile(
    context: Context,
    uriText: String,
): File? {
    if (uriText.isBlank()) return null
    val uri = runCatching { uriText.toUri() }.getOrNull() ?: return null
    return when (uri.scheme) {
        "file" -> File(uri.path ?: return null).takeIf(::isManagedNonHomeBackgroundFile)
        "content" -> resolveManagedNonHomeBackgroundFileByContentUri(context, uri)
        else -> null
    }
}

private fun isManagedNonHomeBackgroundFile(file: File): Boolean {
    if (!file.name.startsWith(NON_HOME_BACKGROUND_CROP_FILE_PREFIX)) return false
    if (file.parentFile?.name != NON_HOME_BACKGROUND_CROP_DIR) return false
    return true
}

private fun AccessibilityGuardHistoryEntry.toSettingsUiItem(): SettingsAccessibilityGuardHistoryUiItem =
    SettingsAccessibilityGuardHistoryUiItem(
        timestampMs = timestampMs,
        reason = reason,
        status = status,
        triggerAction = triggerAction,
        checkCount = checkCount,
        healthyCount = healthyCount,
        warningCount = warningCount,
        elapsedMs = elapsedMs,
        failureReason = failureReason,
    )

private val os.kei.feature.keepalive.accessibility.AccessibilityGuardSettings.activePolicyCount: Int
    get() =
        listOf(
            daemonEnabled,
            bootCheckEnabled,
            screenOnCheckEnabled,
        ).count { it }

private fun File.safeCanonicalPath(): String =
    runCatching { canonicalPath }.getOrDefault(absolutePath)

private fun resolveManagedNonHomeBackgroundFileByContentUri(
    context: Context,
    uri: Uri,
): File? {
    val expectedAuthority = "${context.packageName}.fileprovider"
    if (uri.authority != expectedAuthority) return null
    val fileName =
        uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.takeIf { it.startsWith(NON_HOME_BACKGROUND_CROP_FILE_PREFIX) }
            ?: return null
    val dir = File(context.filesDir, NON_HOME_BACKGROUND_CROP_DIR)
    return File(dir, fileName)
}
