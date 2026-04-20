package com.example.keios.ui.page.main.os.state

import android.content.Context
import com.example.keios.R
import com.example.keios.core.system.ShizukuApiUtils
import com.example.keios.ui.page.main.os.InfoRow
import com.example.keios.ui.page.main.os.OsGoogleSystemServiceConfig
import com.example.keios.ui.page.main.os.OsSectionCard
import com.example.keios.ui.page.main.os.SectionKind
import com.example.keios.ui.page.main.os.SectionState
import com.example.keios.ui.page.main.os.applyOsActivityCardVisibility
import com.example.keios.ui.page.main.os.applyOsCardVisibility
import com.example.keios.ui.page.main.os.applyOsShellCommandCardVisibility
import com.example.keios.ui.page.main.os.ensureOsSectionLoaded
import com.example.keios.ui.page.main.os.refreshAllOsSections
import com.example.keios.ui.page.main.os.runOsShellCommandCard
import com.example.keios.ui.page.main.os.shell.OsShellCommandCard
import com.example.keios.ui.page.main.os.shell.OsShellCommandCardStore
import com.example.keios.ui.page.main.os.shortcut.OsActivityShortcutCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex

internal data class OsPageActionState(
    val ensureLoad: suspend (SectionKind, Boolean) -> Unit,
    val applyCardVisibility: suspend (OsSectionCard, Boolean) -> Unit,
    val applyActivityCardVisibility: suspend (String, Boolean) -> Unit,
    val applyShellCommandCardVisibility: suspend (String, Boolean) -> Unit,
    val runShellCommandCard: suspend (OsShellCommandCard) -> Unit,
    val refreshAllSections: suspend () -> Unit
)

internal fun createOsPageActionState(
    context: Context,
    scope: CoroutineScope,
    shizukuStatus: String,
    shizukuApiUtils: ShizukuApiUtils,
    sectionLoadMutex: Mutex,
    sectionLoadDeferreds: MutableMap<SectionKind, Deferred<List<InfoRow>>>,
    visibleCardsProvider: () -> Set<OsSectionCard>,
    sectionStatesProvider: () -> Map<SectionKind, SectionState>,
    updateSection: (SectionKind, (SectionState) -> SectionState) -> Unit,
    onCachePersistedChanged: (Boolean) -> Unit,
    updateVisibleCards: (Set<OsSectionCard>) -> Unit,
    setTopInfoExpanded: (Boolean) -> Unit,
    setShellRunnerExpanded: (Boolean) -> Unit,
    setSystemTableExpanded: (Boolean) -> Unit,
    setSecureTableExpanded: (Boolean) -> Unit,
    setGlobalTableExpanded: (Boolean) -> Unit,
    setAndroidPropsExpanded: (Boolean) -> Unit,
    setJavaPropsExpanded: (Boolean) -> Unit,
    setLinuxEnvExpanded: (Boolean) -> Unit,
    activityShortcutCardsProvider: () -> List<OsActivityShortcutCard>,
    updateActivityShortcutCards: (List<OsActivityShortcutCard>) -> Unit,
    googleSystemServiceDefaults: OsGoogleSystemServiceConfig,
    updateShellCommandCards: (List<OsShellCommandCard>) -> Unit,
    runningShellCommandCardIdsProvider: () -> Set<String>,
    onRunningShellCommandCardIdsChange: (Set<String>) -> Unit,
    onRefreshingChange: (Boolean) -> Unit,
    onRefreshProgressChange: (Float) -> Unit,
    shellCardCommandRequiredToast: String,
    shellRunNoPermissionText: String,
    shellRunNoOutputText: String,
    noRefreshableCardText: String,
    refreshCompletedText: String
): OsPageActionState {
    val ensureLoad: suspend (SectionKind, Boolean) -> Unit = { section, forceRefresh ->
        ensureOsSectionLoaded(
            section = section,
            forceRefresh = forceRefresh,
            visibleCardsProvider = visibleCardsProvider,
            sectionStatesProvider = sectionStatesProvider,
            sectionLoadMutex = sectionLoadMutex,
            sectionLoadDeferreds = sectionLoadDeferreds,
            scope = scope,
            context = context,
            shizukuStatus = shizukuStatus,
            shizukuApiUtils = shizukuApiUtils,
            updateSection = updateSection,
            onCachePersistedChanged = onCachePersistedChanged
        )
    }

    val applyCardVisibility: suspend (OsSectionCard, Boolean) -> Unit = { card, visible ->
        applyOsCardVisibility(
            card = card,
            visible = visible,
            currentVisibleCards = visibleCardsProvider(),
            updateVisibleCards = updateVisibleCards,
            setTopInfoExpanded = setTopInfoExpanded,
            setShellRunnerExpanded = setShellRunnerExpanded,
            setSystemTableExpanded = setSystemTableExpanded,
            setSecureTableExpanded = setSecureTableExpanded,
            setGlobalTableExpanded = setGlobalTableExpanded,
            setAndroidPropsExpanded = setAndroidPropsExpanded,
            setJavaPropsExpanded = setJavaPropsExpanded,
            setLinuxEnvExpanded = setLinuxEnvExpanded,
            updateSection = updateSection,
            ensureLoad = ensureLoad,
            visibleCardsProvider = visibleCardsProvider,
            onCachePersistedChanged = onCachePersistedChanged
        )
    }

    val applyActivityCardVisibility: suspend (String, Boolean) -> Unit = { cardId, visible ->
        applyOsActivityCardVisibility(
            cardId = cardId,
            visible = visible,
            currentCards = activityShortcutCardsProvider(),
            defaults = googleSystemServiceDefaults,
            updateCards = updateActivityShortcutCards
        )
    }

    val applyShellCommandCardVisibility: suspend (String, Boolean) -> Unit = { cardId, visible ->
        applyOsShellCommandCardVisibility(
            cardId = cardId,
            visible = visible,
            updateCards = updateShellCommandCards
        )
    }

    val runShellCommandCard: suspend (OsShellCommandCard) -> Unit = { card ->
        runOsShellCommandCard(
            card = card,
            context = context,
            shizukuApiUtils = shizukuApiUtils,
            shellCardCommandRequiredToast = shellCardCommandRequiredToast,
            shellRunNoPermissionToast = shellRunNoPermissionText,
            shellRunNoOutputText = shellRunNoOutputText,
            runningCardIdsProvider = runningShellCommandCardIdsProvider,
            updateRunningCardIds = onRunningShellCommandCardIdsChange,
            onCardsReload = { updateShellCommandCards(OsShellCommandCardStore.loadCards()) },
            runFailedMessage = { throwable ->
                context.getString(
                    R.string.os_shell_card_toast_run_failed,
                    throwable.javaClass.simpleName
                )
            }
        )
    }

    val refreshAllSections: suspend () -> Unit = {
        refreshAllOsSections(
            context = context,
            visibleCardsProvider = visibleCardsProvider,
            setRefreshing = onRefreshingChange,
            setRefreshProgress = onRefreshProgressChange,
            ensureLoad = ensureLoad,
            noRefreshableCardText = noRefreshableCardText,
            refreshCompletedText = refreshCompletedText
        )
    }

    return OsPageActionState(
        ensureLoad = ensureLoad,
        applyCardVisibility = applyCardVisibility,
        applyActivityCardVisibility = applyActivityCardVisibility,
        applyShellCommandCardVisibility = applyShellCommandCardVisibility,
        runShellCommandCard = runShellCommandCard,
        refreshAllSections = refreshAllSections
    )
}
