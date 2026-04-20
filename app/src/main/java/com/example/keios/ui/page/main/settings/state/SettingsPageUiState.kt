package com.example.keios.ui.page.main.settings.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntRect

@Stable
internal class SettingsPageUiState {
    var showThemeModePopup by mutableStateOf(false)
    var themePopupAnchorBounds by mutableStateOf<IntRect?>(null)

    var cacheReloadSignal by mutableIntStateOf(0)
    var clearingCacheId by mutableStateOf<String?>(null)
    var clearingAllCaches by mutableStateOf(false)

    var logReloadSignal by mutableIntStateOf(0)

    fun requestCacheReload() {
        cacheReloadSignal += 1
    }

    fun requestLogReload() {
        logReloadSignal += 1
    }
}

@Composable
internal fun rememberSettingsPageUiState(): SettingsPageUiState {
    return remember { SettingsPageUiState() }
}
