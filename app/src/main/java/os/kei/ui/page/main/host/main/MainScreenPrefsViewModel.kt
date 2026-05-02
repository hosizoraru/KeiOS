package os.kei.ui.page.main.host.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import os.kei.core.prefs.UiPrefsRepository
import os.kei.core.prefs.UiPrefsSnapshot

internal class MainScreenPrefsViewModel : ViewModel() {
    private val repository = UiPrefsRepository()
    val snapshot: StateFlow<UiPrefsSnapshot> = repository.observeSnapshots()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = repository.observeSnapshots().value
        )
    private var loadJob: Job? = null

    fun loadInitialSnapshot() {
        if (loadJob != null) return
        loadJob = viewModelScope.launch {
            repository.refreshSnapshot()
        }
    }

    fun updateLiquidBottomBarEnabled(value: Boolean) {
        launchRepositoryUpdate {
            setLiquidBottomBarEnabled(value)
        }
    }

    fun updateBottomBarScrollEffectReductionEnabled(value: Boolean) {
        launchRepositoryUpdate {
            setBottomBarScrollEffectReductionEnabled(value)
        }
    }

    fun updateLiquidActionBarLayeredStyleEnabled(value: Boolean) {
        launchRepositoryUpdate {
            setLiquidActionBarLayeredStyleEnabled(value)
        }
    }

    fun updateLiquidSwitchEnabled(value: Boolean) {
        launchRepositoryUpdate {
            setLiquidSwitchEnabled(value)
        }
    }

    fun updateTransitionAnimationsEnabled(value: Boolean) {
        launchRepositoryUpdate {
            setTransitionAnimationsEnabled(value)
        }
    }

    fun updatePredictiveBackAnimationsEnabled(value: Boolean) {
        launchRepositoryUpdate {
            setPredictiveBackAnimationsEnabled(value)
        }
    }

    fun updateCardPressFeedbackEnabled(value: Boolean) {
        launchRepositoryUpdate {
            setCardPressFeedbackEnabled(value)
        }
    }

    fun updateHomeIconHdrEnabled(value: Boolean) {
        launchRepositoryUpdate {
            setHomeIconHdrEnabled(value)
        }
    }

    fun updateHomeDynamicFullEffectEnabled(value: Boolean) {
        launchRepositoryUpdate {
            setHomeDynamicFullEffectEnabled(value)
        }
    }

    fun updatePreloadingEnabled(value: Boolean) {
        launchRepositoryUpdate {
            setPreloadingEnabled(value)
        }
    }

    fun updateNonHomeBackgroundEnabled(value: Boolean) {
        launchRepositoryUpdate {
            setNonHomeBackgroundEnabled(value)
        }
    }

    fun updateNonHomeBackgroundUri(value: String) {
        launchRepositoryUpdate {
            setNonHomeBackgroundUri(value)
        }
    }

    fun updateNonHomeBackgroundOpacity(value: Float) {
        launchRepositoryUpdate {
            setNonHomeBackgroundOpacity(value)
        }
    }

    fun updateSuperIslandNotificationEnabled(value: Boolean) {
        launchRepositoryUpdate {
            setSuperIslandNotificationEnabled(value)
        }
    }

    fun updateSuperIslandBypassRestrictionEnabled(value: Boolean) {
        launchRepositoryUpdate {
            setSuperIslandBypassRestrictionEnabled(value)
        }
    }

    fun updateSuperIslandRestoreDelayMs(value: Int) {
        launchRepositoryUpdate {
            setSuperIslandRestoreDelayMs(value)
        }
    }

    fun updateLogDebugEnabled(value: Boolean) {
        launchRepositoryUpdate {
            setLogDebugEnabled(value)
        }
    }

    fun updateTextCopyCapabilityExpanded(value: Boolean) {
        launchRepositoryUpdate {
            setTextCopyCapabilityExpanded(value)
        }
    }

    fun updateCacheDiagnosticsEnabled(value: Boolean) {
        launchRepositoryUpdate {
            setCacheDiagnosticsEnabled(value)
        }
    }

    fun updateVisibleBottomPageNames(value: Set<String>) {
        launchRepositoryUpdate {
            saveVisibleBottomPageNames(value)
        }
    }

    private fun launchRepositoryUpdate(
        persist: suspend UiPrefsRepository.() -> Unit
    ) {
        viewModelScope.launch {
            repository.persist()
        }
    }
}
