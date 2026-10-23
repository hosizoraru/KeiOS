package os.kei.core.prefs

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.log.AppLogLevel

class UiPrefsRepository(
    private val ioDispatcher: CoroutineDispatcher = AppDispatchers.fileIo,
    initialSnapshot: UiPrefsSnapshot = UiPrefs.defaultSnapshot(),
    private val persistLauncherIconDesign: (LauncherIconDesign) -> Unit = { UiPrefs.setLauncherIconDesign(it) },
) {
    private val snapshots = MutableStateFlow(initialSnapshot)

    fun observeSnapshots(): StateFlow<UiPrefsSnapshot> = snapshots.asStateFlow()

    suspend fun refreshSnapshot() {
        snapshots.value =
            withContext(ioDispatcher) {
                UiPrefs.loadSnapshot()
            }
    }

    suspend fun setLiquidSwitchEnabled(value: Boolean) {
        updateAndPersist({ copy(liquidSwitchEnabled = value) }) {
            UiPrefs.setLiquidSwitchEnabled(value)
        }
    }

    suspend fun setLiquidToastEnabled(value: Boolean) {
        updateAndPersist({ copy(liquidToastEnabled = value) }) {
            UiPrefs.setLiquidToastEnabled(value)
        }
    }

    suspend fun setReduceToastInterruptionEnabled(value: Boolean) {
        updateAndPersist({ copy(reduceToastInterruptionEnabled = value) }) {
            UiPrefs.setReduceToastInterruptionEnabled(value)
        }
    }

    suspend fun setPrivilegeModeId(value: String) {
        updateAndPersist({ copy(privilegeModeId = value) }) {
            UiPrefs.setPrivilegeModeId(value)
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

    suspend fun setSearchAutoFocusEnabled(value: Boolean) {
        updateAndPersist({ copy(searchAutoFocusEnabled = value) }) {
            UiPrefs.setSearchAutoFocusEnabled(value)
        }
    }

    suspend fun setGripAwareFloatingDockEnabled(value: Boolean) {
        updateAndPersist({ copy(gripAwareFloatingDockEnabled = value) }) {
            UiPrefs.setGripAwareFloatingDockEnabled(value)
        }
    }

    suspend fun setHomeIconHdrEnabled(value: Boolean) {
        updateAndPersist({ copy(homeIconHdrEnabled = value) }) {
            UiPrefs.setHomeIconHdrEnabled(value)
        }
    }

    suspend fun setHomeDynamicFullEffectEnabled(value: Boolean) {
        updateAndPersist({ copy(homeDynamicFullEffectEnabled = value) }) {
            UiPrefs.setHomeDynamicFullEffectEnabled(value)
        }
    }

    suspend fun setPreloadingEnabled(value: Boolean) {
        updateAndPersist({ copy(preloadingEnabled = value) }) {
            UiPrefs.setPreloadingEnabled(value)
        }
    }

    suspend fun setLauncherIconDesign(value: LauncherIconDesign) {
        snapshots.update { it.copy(launcherIconDesign = value) }
        // The platform switch is already applied. Finish ordered writes even if settings closes.
        withContext(NonCancellable) {
            launcherIconWriteMutex.withLock {
                withContext(ioDispatcher) {
                    persistLauncherIconDesign(value)
                }
            }
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

    suspend fun setNonHomeBackgroundContentScale(value: NonHomeBackgroundContentScale) {
        updateAndPersist({ copy(nonHomeBackgroundContentScale = value) }) {
            UiPrefs.setNonHomeBackgroundContentScale(value)
        }
    }

    suspend fun setNonHomeBackgroundAlignment(value: NonHomeBackgroundAlignment) {
        updateAndPersist({ copy(nonHomeBackgroundAlignment = value) }) {
            UiPrefs.setNonHomeBackgroundAlignment(value)
        }
    }

    suspend fun setNonHomeBackgroundPageStyle(value: NonHomeBackgroundPageStyle) {
        updateAndPersist({ copy(nonHomeBackgroundPageStyle = value) }) {
            UiPrefs.setNonHomeBackgroundPageStyle(value)
        }
    }

    suspend fun setNonHomeBackgroundScrim(value: Float) {
        updateAndPersist({ copy(nonHomeBackgroundScrim = value) }) {
            UiPrefs.setNonHomeBackgroundScrim(value)
        }
    }

    suspend fun setNonHomeBackgroundDepthEnabled(value: Boolean) {
        updateAndPersist({ copy(nonHomeBackgroundDepthEnabled = value) }) {
            UiPrefs.setNonHomeBackgroundDepthEnabled(value)
        }
    }

    suspend fun setNonHomeBackgroundSaturation(value: Float) {
        updateAndPersist({ copy(nonHomeBackgroundSaturation = value) }) {
            UiPrefs.setNonHomeBackgroundSaturation(value)
        }
    }

    suspend fun resetNonHomeBackgroundRendering() {
        val defaults = UiPrefs.defaultSnapshot()
        updateAndPersist(
            reducer = {
                copy(
                    nonHomeBackgroundOpacity = defaults.nonHomeBackgroundOpacity,
                    nonHomeBackgroundContentScale = defaults.nonHomeBackgroundContentScale,
                    nonHomeBackgroundAlignment = defaults.nonHomeBackgroundAlignment,
                    nonHomeBackgroundPageStyle = defaults.nonHomeBackgroundPageStyle,
                    nonHomeBackgroundScrim = defaults.nonHomeBackgroundScrim,
                    nonHomeBackgroundDepthEnabled = defaults.nonHomeBackgroundDepthEnabled,
                    nonHomeBackgroundSaturation = defaults.nonHomeBackgroundSaturation,
                )
            },
        ) {
            UiPrefs.setNonHomeBackgroundOpacity(defaults.nonHomeBackgroundOpacity)
            UiPrefs.setNonHomeBackgroundContentScale(defaults.nonHomeBackgroundContentScale)
            UiPrefs.setNonHomeBackgroundAlignment(defaults.nonHomeBackgroundAlignment)
            UiPrefs.setNonHomeBackgroundPageStyle(defaults.nonHomeBackgroundPageStyle)
            UiPrefs.setNonHomeBackgroundScrim(defaults.nonHomeBackgroundScrim)
            UiPrefs.setNonHomeBackgroundDepthEnabled(defaults.nonHomeBackgroundDepthEnabled)
            UiPrefs.setNonHomeBackgroundSaturation(defaults.nonHomeBackgroundSaturation)
        }
    }

    suspend fun applyNonHomeBackgroundReadableSuggestion(isDarkTheme: Boolean) {
        val opacity = if (isDarkTheme) 0.18f else 0.14f
        val scrim = if (isDarkTheme) 0.18f else 0.20f
        val saturation = if (isDarkTheme) 0.92f else 0.86f
        updateAndPersist(
            reducer = {
                copy(
                    nonHomeBackgroundOpacity = opacity,
                    nonHomeBackgroundAlignment =
                        if (nonHomeBackgroundContentScale == NonHomeBackgroundContentScale.Fit) {
                            NonHomeBackgroundAlignment.Center
                        } else {
                            nonHomeBackgroundAlignment
                        },
                    nonHomeBackgroundPageStyle = NonHomeBackgroundPageStyle.Readable,
                    nonHomeBackgroundScrim = scrim,
                    nonHomeBackgroundSaturation = saturation,
                )
            },
        ) {
            UiPrefs.setNonHomeBackgroundOpacity(opacity)
            if (UiPrefs.getNonHomeBackgroundContentScale() == NonHomeBackgroundContentScale.Fit) {
                UiPrefs.setNonHomeBackgroundAlignment(NonHomeBackgroundAlignment.Center)
            }
            UiPrefs.setNonHomeBackgroundPageStyle(NonHomeBackgroundPageStyle.Readable)
            UiPrefs.setNonHomeBackgroundScrim(scrim)
            UiPrefs.setNonHomeBackgroundSaturation(saturation)
        }
    }

    suspend fun setSuperIslandNotificationEnabled(value: Boolean) {
        updateAndPersist({ copy(superIslandNotificationEnabled = value) }) {
            UiPrefs.setSuperIslandNotificationEnabled(value)
        }
    }

    suspend fun setSuperIslandFloatBehavior(value: SuperIslandFloatBehavior) {
        updateAndPersist(
            {
                copy(
                    superIslandFloatBehavior = value,
                    superIslandFirstFloatEnabled = value.firstFloatEnabled,
                )
            },
        ) {
            UiPrefs.setSuperIslandFloatBehavior(value)
        }
    }

    suspend fun setSuperIslandAutoClose(value: SuperIslandAutoClose) {
        updateAndPersist({ copy(superIslandAutoClose = value) }) {
            UiPrefs.setSuperIslandAutoClose(value)
        }
    }

    suspend fun setSuperIslandFirstFloatEnabled(value: Boolean) {
        val behavior =
            if (value) {
                SuperIslandFloatBehavior.StartAndFinish
            } else {
                SuperIslandFloatBehavior.SummaryOnly
            }
        updateAndPersist(
            {
                copy(
                    superIslandFloatBehavior = behavior,
                    superIslandFirstFloatEnabled = behavior.firstFloatEnabled,
                )
            },
        ) {
            UiPrefs.setSuperIslandFirstFloatEnabled(value)
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

    suspend fun setLogLevel(value: AppLogLevel) {
        updateAndPersist({ copy(logLevel = value) }) {
            UiPrefs.setLogLevel(value)
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
        persist: () -> Unit,
    ) {
        snapshots.update { snapshot -> snapshot.reducer() }
        withContext(ioDispatcher) {
            persist()
        }
    }

    private companion object {
        val launcherIconWriteMutex = Mutex()
    }
}
