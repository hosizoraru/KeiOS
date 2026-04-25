package os.kei.ui.page.main.settings.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntRect

@Stable
internal class SettingsPageUiState {
    var showThemeModePopup by mutableStateOf(false)
    var themePopupAnchorBounds by mutableStateOf<IntRect?>(null)
}

@Composable
internal fun rememberSettingsPageUiState(): SettingsPageUiState {
    return remember { SettingsPageUiState() }
}
