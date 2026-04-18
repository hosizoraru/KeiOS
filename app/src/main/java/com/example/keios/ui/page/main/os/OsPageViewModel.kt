package com.example.keios.ui.page.main.os

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
internal class OsPageViewModel : ViewModel() {
    private val _queryInput = MutableStateFlow("")
    val queryInput: StateFlow<String> = _queryInput.asStateFlow()

    private val _queryApplied = MutableStateFlow("")
    val queryApplied: StateFlow<String> = _queryApplied.asStateFlow()

    init {
        viewModelScope.launch {
            _queryInput
                .debounce(180)
                .distinctUntilChanged()
                .collect { q ->
                    _queryApplied.value = q
                }
        }
    }

    fun updateQueryInput(value: String) {
        _queryInput.value = value
    }
}
