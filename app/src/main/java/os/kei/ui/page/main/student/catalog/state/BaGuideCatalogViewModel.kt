package os.kei.ui.page.main.student.catalog.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import os.kei.ui.page.main.student.catalog.BaGuideCatalogBundle

internal data class BaGuideCatalogDataUiState(
    val catalog: BaGuideCatalogBundle = BaGuideCatalogBundle.EMPTY,
    val loading: Boolean = false,
    val error: String? = null
)

private data class BaGuideCatalogBinding(
    val transitionAnimationsEnabled: Boolean,
    val initialFetchDelayMs: Int,
    val loadFailedText: String,
    val refreshFailedKeepCacheText: String
)

internal class BaGuideCatalogViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val repository = BaGuideCatalogRepository()
    private var binding: BaGuideCatalogBinding? = null
    private var loadJob: Job? = null
    private var hydrateJob: Job? = null
    private var hydratedSyncedAtMs: Long = -1L

    private val _dataState = MutableStateFlow(BaGuideCatalogDataUiState())
    val dataState: StateFlow<BaGuideCatalogDataUiState> = _dataState.asStateFlow()

    fun bind(
        transitionAnimationsEnabled: Boolean,
        initialFetchDelayMs: Int,
        loadFailedText: String,
        refreshFailedKeepCacheText: String
    ) {
        val next = BaGuideCatalogBinding(
            transitionAnimationsEnabled = transitionAnimationsEnabled,
            initialFetchDelayMs = initialFetchDelayMs,
            loadFailedText = loadFailedText,
            refreshFailedKeepCacheText = refreshFailedKeepCacheText
        )
        if (binding == next && _dataState.value.catalog.entriesByTab.values.any { it.isNotEmpty() }) return
        binding = next
        loadCatalog(manualRefresh = false, allowInitialDelay = true)
    }

    fun requestRefresh() {
        loadCatalog(manualRefresh = true, allowInitialDelay = false)
    }

    private fun loadCatalog(
        manualRefresh: Boolean,
        allowInitialDelay: Boolean
    ) {
        val currentBinding = binding ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (
                allowInitialDelay &&
                currentBinding.transitionAnimationsEnabled &&
                currentBinding.initialFetchDelayMs > 0
            ) {
                delay(currentBinding.initialFetchDelayMs.toLong())
            }
            _dataState.update { state -> state.copy(loading = true) }
            val result = repository.loadCatalog(
                context = appContext,
                currentCatalog = _dataState.value.catalog,
                manualRefresh = manualRefresh,
                loadFailedText = currentBinding.loadFailedText,
                refreshFailedKeepCacheText = currentBinding.refreshFailedKeepCacheText
            )
            _dataState.value = BaGuideCatalogDataUiState(
                catalog = result.catalog,
                loading = false,
                error = result.error
            )
            hydrateReleaseDatesIfNeeded(result.catalog)
        }
    }

    private fun hydrateReleaseDatesIfNeeded(catalog: BaGuideCatalogBundle) {
        if (catalog.entriesByTab.values.all { it.isEmpty() }) return
        if (catalog.syncedAtMs <= 0L || catalog.syncedAtMs == hydratedSyncedAtMs) return
        hydratedSyncedAtMs = catalog.syncedAtMs
        hydrateJob?.cancel()
        hydrateJob = viewModelScope.launch {
            repository.hydrateReleaseDateIndex(
                source = catalog,
                onBundleUpdated = { updated ->
                    _dataState.update { state ->
                        if (state.catalog.syncedAtMs == catalog.syncedAtMs) {
                            state.copy(catalog = updated)
                        } else {
                            state
                        }
                    }
                }
            )
        }
    }
}
