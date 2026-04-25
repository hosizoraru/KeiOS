package os.kei.ui.page.main.settings.state

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import os.kei.core.log.AppLogStore
import os.kei.core.prefs.CacheEntrySummary

private const val SETTINGS_LOG_STATS_REFRESH_MS = 1_200L

internal data class SettingsCacheUiState(
    val cacheEntries: List<CacheEntrySummary>? = emptyList(),
    val cacheEntriesLoading: Boolean = false,
    val clearingCacheId: String? = null,
    val clearingAllCaches: Boolean = false
)

internal data class SettingsLogUiState(
    val logStats: AppLogStore.Stats = AppLogStore.Stats.Empty,
    val exportingLogZip: Boolean = false,
    val clearingLogs: Boolean = false,
    val pendingExportFileName: String? = null
)

internal class SettingsPageViewModel : ViewModel() {
    private val repository = SettingsPageRepository()
    private var cacheLoadJob: Job? = null
    private var logStatsJob: Job? = null
    private var boundLogDebugEnabled: Boolean? = null

    private val _cacheState = MutableStateFlow(SettingsCacheUiState())
    val cacheState: StateFlow<SettingsCacheUiState> = _cacheState.asStateFlow()

    private val _logState = MutableStateFlow(SettingsLogUiState())
    val logState: StateFlow<SettingsLogUiState> = _logState.asStateFlow()

    fun bindCacheDiagnostics(
        context: Context,
        enabled: Boolean
    ) {
        val appContext = context.applicationContext
        if (!enabled) {
            cacheLoadJob?.cancel()
            _cacheState.value = SettingsCacheUiState()
            return
        }
        reloadCacheEntries(appContext)
    }

    fun reloadCacheEntries(context: Context) {
        val appContext = context.applicationContext
        cacheLoadJob?.cancel()
        cacheLoadJob = viewModelScope.launch {
            _cacheState.update { state ->
                state.copy(cacheEntriesLoading = state.cacheEntries == null)
            }
            val entries = repository.listCacheEntries(appContext)
            _cacheState.update { state ->
                state.copy(
                    cacheEntries = entries,
                    cacheEntriesLoading = false
                )
            }
        }
    }

    suspend fun clearAllCaches(context: Context): Result<Unit> {
        val appContext = context.applicationContext
        _cacheState.update { state -> state.copy(clearingAllCaches = true) }
        val result = repository.clearAllCaches(appContext)
        _cacheState.update { state -> state.copy(clearingAllCaches = false) }
        reloadCacheEntries(appContext)
        return result
    }

    suspend fun clearCache(
        context: Context,
        cacheId: String
    ): Result<Unit> {
        val appContext = context.applicationContext
        _cacheState.update { state -> state.copy(clearingCacheId = cacheId) }
        val result = repository.clearCache(appContext, cacheId)
        _cacheState.update { state -> state.copy(clearingCacheId = null) }
        reloadCacheEntries(appContext)
        return result
    }

    fun bindLogStats(
        context: Context,
        logDebugEnabled: Boolean
    ) {
        val appContext = context.applicationContext
        if (boundLogDebugEnabled == logDebugEnabled && logStatsJob?.isActive == true) return
        boundLogDebugEnabled = logDebugEnabled
        logStatsJob?.cancel()
        logStatsJob = viewModelScope.launch {
            do {
                _logState.update { state ->
                    state.copy(logStats = repository.loadLogStats(appContext))
                }
                if (!logDebugEnabled) break
                delay(SETTINGS_LOG_STATS_REFRESH_MS)
            } while (true)
        }
    }

    fun reloadLogStats(context: Context) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            _logState.update { state ->
                state.copy(logStats = repository.loadLogStats(appContext))
            }
        }
    }

    fun beginLogExport() {
        if (_logState.value.exportingLogZip || _logState.value.clearingLogs) return
        viewModelScope.launch {
            val fileName = repository.buildLogExportFileName()
            _logState.update { state ->
                state.copy(
                    exportingLogZip = true,
                    pendingExportFileName = fileName
                )
            }
        }
    }

    fun consumePendingExportFileName(): String? {
        val fileName = _logState.value.pendingExportFileName
        _logState.update { state -> state.copy(pendingExportFileName = null) }
        return fileName
    }

    fun finishLogExport() {
        _logState.update { state ->
            state.copy(
                exportingLogZip = false,
                pendingExportFileName = null
            )
        }
    }

    suspend fun exportLogZip(
        context: Context,
        uri: Uri
    ): Result<Unit> {
        return repository.exportLogZip(
            context = context.applicationContext,
            uri = uri
        )
    }

    suspend fun clearLogs(context: Context): Result<Unit> {
        val appContext = context.applicationContext
        if (_logState.value.exportingLogZip || _logState.value.clearingLogs) {
            return Result.success(Unit)
        }
        _logState.update { state -> state.copy(clearingLogs = true) }
        val result = repository.clearLogs(appContext)
        _logState.update { state -> state.copy(clearingLogs = false) }
        reloadLogStats(appContext)
        return result
    }
}
