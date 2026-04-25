package os.kei.core.prefs

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class UiPrefsRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val snapshots = MutableStateFlow(UiPrefs.defaultSnapshot())

    fun observeSnapshots(): StateFlow<UiPrefsSnapshot> = snapshots.asStateFlow()

    suspend fun refreshSnapshot() {
        snapshots.value = withContext(ioDispatcher) {
            UiPrefs.loadSnapshot()
        }
    }

    suspend fun setLiquidBottomBarEnabled(value: Boolean) {
        updateAndPersist({ copy(liquidBottomBarEnabled = value) }) {
            UiPrefs.setLiquidBottomBarEnabled(value)
        }
    }

    suspend fun setLiquidActionBarLayeredStyleEnabled(value: Boolean) {
        updateAndPersist({ copy(liquidActionBarLayeredStyleEnabled = value) }) {
            UiPrefs.setLiquidActionBarLayeredStyleEnabled(value)
        }
    }

    suspend fun setLiquidGlassSwitchEnabled(value: Boolean) {
        updateAndPersist({ copy(liquidGlassSwitchEnabled = value) }) {
            UiPrefs.setLiquidGlassSwitchEnabled(value)
        }
    }

    suspend fun setTransitionAnimationsEnabled(value: Boolean) {
        updateAndPersist({ copy(transitionAnimationsEnabled = value) }) {
            UiPrefs.setTransitionAnimationsEnabled(value)
        }
    }

    suspend fun setPredictiveBackAnimationsEnabled(value: Boolean) {
        updateAndPersist({ copy(predictiveBackAnimationsEnabled = value) }) {
            UiPrefs.setPredictiveBackAnimationsEnabled(value)
        }
    }

    suspend fun setCardPressFeedbackEnabled(value: Boolean) {
        updateAndPersist({ copy(cardPressFeedbackEnabled = value) }) {
            UiPrefs.setCardPressFeedbackEnabled(value)
        }
    }

    suspend fun setHomeIconHdrEnabled(value: Boolean) {
        updateAndPersist({ copy(homeIconHdrEnabled = value) }) {
            UiPrefs.setHomeIconHdrEnabled(value)
        }
    }

    suspend fun setPreloadingEnabled(value: Boolean) {
        updateAndPersist({ copy(preloadingEnabled = value) }) {
            UiPrefs.setPreloadingEnabled(value)
        }
    }

    suspend fun setNonHomeBackgroundEnabled(value: Boolean) {
        updateAndPersist({ copy(nonHomeBackgroundEnabled = value) }) {
            UiPrefs.setNonHomeBackgroundEnabled(value)
        }
    }

    suspend fun setNonHomeBackgroundUri(value: String) {
        val normalized = value.trim()
        updateAndPersist({ copy(nonHomeBackgroundUri = normalized) }) {
            UiPrefs.setNonHomeBackgroundUri(normalized)
        }
    }

    suspend fun setNonHomeBackgroundOpacity(value: Float) {
        updateAndPersist({ copy(nonHomeBackgroundOpacity = value) }) {
            UiPrefs.setNonHomeBackgroundOpacity(value)
        }
    }

    suspend fun setSuperIslandNotificationEnabled(value: Boolean) {
        updateAndPersist({ copy(superIslandNotificationEnabled = value) }) {
            UiPrefs.setSuperIslandNotificationEnabled(value)
        }
    }

    suspend fun setSuperIslandBypassRestrictionEnabled(value: Boolean) {
        updateAndPersist({ copy(superIslandBypassRestrictionEnabled = value) }) {
            UiPrefs.setSuperIslandBypassRestrictionEnabled(value)
        }
    }

    suspend fun setSuperIslandRestoreDelayMs(value: Int) {
        updateAndPersist({ copy(superIslandRestoreDelayMs = value) }) {
            UiPrefs.setSuperIslandRestoreDelayMs(value)
        }
    }

    suspend fun setLogDebugEnabled(value: Boolean) {
        updateAndPersist({ copy(logDebugEnabled = value) }) {
            UiPrefs.setLogDebugEnabled(value)
        }
    }

    suspend fun setTextCopyCapabilityExpanded(value: Boolean) {
        updateAndPersist({ copy(textCopyCapabilityExpanded = value) }) {
            UiPrefs.setTextCopyCapabilityExpanded(value)
        }
    }

    suspend fun setCacheDiagnosticsEnabled(value: Boolean) {
        updateAndPersist({ copy(cacheDiagnosticsEnabled = value) }) {
            UiPrefs.setCacheDiagnosticsEnabled(value)
        }
    }

    suspend fun saveVisibleBottomPageNames(value: Set<String>) {
        updateAndPersist({ copy(visibleBottomPageNames = value) }) {
            UiPrefs.saveVisibleBottomPageNames(value)
        }
    }

    private suspend fun updateAndPersist(
        reducer: UiPrefsSnapshot.() -> UiPrefsSnapshot,
        persist: () -> Unit
    ) {
        snapshots.update { snapshot -> snapshot.reducer() }
        withContext(ioDispatcher) {
            persist()
        }
    }
}
