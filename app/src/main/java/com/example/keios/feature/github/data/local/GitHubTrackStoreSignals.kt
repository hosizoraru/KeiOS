package com.example.keios.feature.github.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object GitHubTrackStoreSignals {
    private val _version = MutableStateFlow(0L)
    val version: StateFlow<Long> = _version.asStateFlow()

    fun notifyChanged(atMillis: Long = System.currentTimeMillis()) {
        _version.value = atMillis
    }
}
