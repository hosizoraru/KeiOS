package com.example.keios.ui.page.main.ba

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class BaCalendarUiState(
    val entries: List<BaCalendarEntry> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val lastSyncMs: Long = 0L
)

internal data class BaPoolUiState(
    val entries: List<BaPoolEntry> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val lastSyncMs: Long = 0L
)

private data class BaCalendarRequestKey(
    val isPageActive: Boolean,
    val serverIndex: Int,
    val reloadSignal: Int,
    val calendarRefreshIntervalHours: Int,
    val hydrationReady: Boolean
)

private data class BaPoolRequestKey(
    val isPageActive: Boolean,
    val serverIndex: Int,
    val reloadSignal: Int,
    val calendarRefreshIntervalHours: Int,
    val hydrationReady: Boolean
)

internal class BaCalendarPoolViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext

    private val _calendarUiState = MutableStateFlow(BaCalendarUiState())
    val calendarUiState: StateFlow<BaCalendarUiState> = _calendarUiState.asStateFlow()

    private val _poolUiState = MutableStateFlow(BaPoolUiState())
    val poolUiState: StateFlow<BaPoolUiState> = _poolUiState.asStateFlow()

    private var calendarJob: Job? = null
    private var poolJob: Job? = null
    private var lastCalendarRequestKey: BaCalendarRequestKey? = null
    private var lastPoolRequestKey: BaPoolRequestKey? = null

    fun syncCalendar(
        isPageActive: Boolean,
        serverIndex: Int,
        reloadSignal: Int,
        calendarRefreshIntervalHours: Int,
        hydrationReady: Boolean
    ) {
        val key = BaCalendarRequestKey(
            isPageActive = isPageActive,
            serverIndex = serverIndex,
            reloadSignal = reloadSignal,
            calendarRefreshIntervalHours = calendarRefreshIntervalHours,
            hydrationReady = hydrationReady
        )
        if (key == lastCalendarRequestKey) return
        lastCalendarRequestKey = key
        calendarJob?.cancel()
        calendarJob = viewModelScope.launch {
            _calendarUiState.value = _calendarUiState.value.copy(loading = true, error = null)
            val snapshot = BaCalendarPoolRepository.syncCalendar(
                context = appContext,
                isPageActive = isPageActive,
                serverIndex = serverIndex,
                reloadSignal = reloadSignal,
                calendarRefreshIntervalHours = calendarRefreshIntervalHours,
                hydrationReady = hydrationReady
            )
            _calendarUiState.value = BaCalendarUiState(
                entries = snapshot.entries,
                loading = snapshot.loading,
                error = snapshot.error,
                lastSyncMs = snapshot.lastSyncMs
            )
        }
    }

    fun syncPool(
        isPageActive: Boolean,
        serverIndex: Int,
        reloadSignal: Int,
        calendarRefreshIntervalHours: Int,
        hydrationReady: Boolean
    ) {
        val key = BaPoolRequestKey(
            isPageActive = isPageActive,
            serverIndex = serverIndex,
            reloadSignal = reloadSignal,
            calendarRefreshIntervalHours = calendarRefreshIntervalHours,
            hydrationReady = hydrationReady
        )
        if (key == lastPoolRequestKey) return
        lastPoolRequestKey = key
        poolJob?.cancel()
        poolJob = viewModelScope.launch {
            _poolUiState.value = _poolUiState.value.copy(loading = true, error = null)
            val snapshot = BaCalendarPoolRepository.syncPool(
                context = appContext,
                isPageActive = isPageActive,
                serverIndex = serverIndex,
                reloadSignal = reloadSignal,
                calendarRefreshIntervalHours = calendarRefreshIntervalHours,
                hydrationReady = hydrationReady
            )
            _poolUiState.value = BaPoolUiState(
                entries = snapshot.entries,
                loading = snapshot.loading,
                error = snapshot.error,
                lastSyncMs = snapshot.lastSyncMs
            )
        }
    }
}
