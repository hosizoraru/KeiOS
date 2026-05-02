package os.kei.ui.page.main.os

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import os.kei.ui.page.main.os.shell.OsShellCommandCard
import os.kei.ui.page.main.os.shortcut.BUILTIN_GOOGLE_SETTINGS_SAMPLE_CARD_ID
import os.kei.ui.page.main.os.shortcut.LEGACY_GOOGLE_SYSTEM_SERVICE_CARD_ID
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCard
import os.kei.ui.page.main.os.shortcut.ShortcutActivityClassOption
import os.kei.ui.page.main.os.shortcut.ShortcutInstalledAppOption
import os.kei.ui.page.main.os.shortcut.ShortcutSuggestionField
import os.kei.ui.page.main.os.shortcut.loadActivityClassOptions
import os.kei.ui.page.main.os.shortcut.loadInstalledAppOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import os.kei.core.ui.snapshot.rememberAppSnapshotFlowManager
import kotlin.collections.plus

@Composable
@OptIn(FlowPreview::class)
internal fun BindOsExpandedStatePersistence(
    ready: Boolean,
    snapshotProvider: () -> OsUiSnapshot
) {
    val currentSnapshotProvider = rememberUpdatedState(snapshotProvider)
    val snapshotFlowManager = rememberAppSnapshotFlowManager()
    LaunchedEffect(ready, snapshotFlowManager) {
        if (!ready) return@LaunchedEffect
        snapshotFlowManager.snapshotFlow {
            currentSnapshotProvider.value()
        }
            .debounce(200)
            .distinctUntilChanged()
            .collectLatest { snapshot ->
                withContext(Dispatchers.IO) {
                    OsUiStateStore.saveExpandedStates(snapshot)
                }
            }
    }
}

@Composable
internal fun BindOsScrollToTopEffect(
    scrollToTopSignal: Int,
    listState: LazyListState
) {
    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) listState.animateScrollToItem(0)
    }
}

@Composable
internal fun BindOsShellCardReloadOnResume(
    lifecycleOwner: LifecycleOwner,
    reloadCards: () -> Unit
) {
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                reloadCards()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
internal fun BindOsInitialCacheLoad(
    ready: Boolean,
    visibleCards: Set<OsSectionCard>,
    onVisibleCardsChange: (Set<OsSectionCard>) -> Unit,
    onSectionStatesChange: (Map<SectionKind, SectionState>) -> Unit,
    onCachePersistedChange: (Boolean) -> Unit,
    onCacheLoadedChange: (Boolean) -> Unit,
    onUiStatePersistenceReadyChange: (Boolean) -> Unit,
    isPageActive: Boolean,
    ensureLoad: suspend (SectionKind, Boolean) -> Unit
) {
    LaunchedEffect(ready) {
        if (!ready) return@LaunchedEffect
        var ensuredVisibleCards = visibleCards
        if (!ensuredVisibleCards.contains(OsSectionCard.GOOGLE_SYSTEM_SERVICE)) {
            ensuredVisibleCards = ensuredVisibleCards + OsSectionCard.GOOGLE_SYSTEM_SERVICE
        }
        if (!ensuredVisibleCards.contains(OsSectionCard.SHELL_RUNNER)) {
            ensuredVisibleCards = ensuredVisibleCards + OsSectionCard.SHELL_RUNNER
        }
        if (ensuredVisibleCards != visibleCards) {
            onVisibleCardsChange(ensuredVisibleCards)
            withContext(Dispatchers.IO) {
                OsCardVisibilityStore.saveVisibleCards(ensuredVisibleCards)
            }
        }
        val visibleSections = visibleSectionKinds(ensuredVisibleCards)
        val snapshot = withContext(Dispatchers.IO) {
            OsInfoCache.readSnapshot(visibleSections)
        }
        val cached = snapshot.cached
        onSectionStatesChange(
            mapOf(
                SectionKind.SYSTEM to SectionState(rows = if (visibleSections.contains(SectionKind.SYSTEM)) cached.system else emptyList()),
                SectionKind.SECURE to SectionState(rows = if (visibleSections.contains(SectionKind.SECURE)) cached.secure else emptyList()),
                SectionKind.GLOBAL to SectionState(rows = if (visibleSections.contains(SectionKind.GLOBAL)) cached.global else emptyList()),
                SectionKind.ANDROID to SectionState(rows = if (visibleSections.contains(SectionKind.ANDROID)) cached.android else emptyList()),
                SectionKind.JAVA to SectionState(rows = if (visibleSections.contains(SectionKind.JAVA)) cached.java else emptyList()),
                SectionKind.LINUX to SectionState(rows = if (visibleSections.contains(SectionKind.LINUX)) cached.linux else emptyList())
            )
        )
        onCachePersistedChange(snapshot.hasPersistedCache)
        onCacheLoadedChange(true)
        onUiStatePersistenceReadyChange(true)
        if (isPageActive) {
            visibleSections.forEach { section ->
                ensureLoad(section, false)
            }
        }
    }
}

@Composable
internal fun BindOsShizukuInvalidation(
    shizukuReady: Boolean,
    updateSection: (SectionKind, (SectionState) -> SectionState) -> Unit
) {
    LaunchedEffect(shizukuReady) {
        updateSection(SectionKind.SYSTEM) { it.copy(loadedFresh = false) }
        updateSection(SectionKind.SECURE) { it.copy(loadedFresh = false) }
        updateSection(SectionKind.GLOBAL) { it.copy(loadedFresh = false) }
        updateSection(SectionKind.LINUX) { it.copy(loadedFresh = false) }
    }
}

@Composable
internal fun BindOsVisibleSectionLoadEffects(
    cacheLoaded: Boolean,
    isDataActive: Boolean,
    visibleCards: Set<OsSectionCard>,
    systemTableExpanded: Boolean,
    secureTableExpanded: Boolean,
    globalTableExpanded: Boolean,
    androidPropsExpanded: Boolean,
    javaPropsExpanded: Boolean,
    linuxEnvExpanded: Boolean,
    ensureLoad: suspend (SectionKind) -> Unit
) {
    BindOsSectionLoadEffect(
        expanded = systemTableExpanded,
        cacheLoaded = cacheLoaded,
        isDataActive = isDataActive,
        visibleCards = visibleCards,
        card = OsSectionCard.SYSTEM,
        section = SectionKind.SYSTEM,
        ensureLoad = ensureLoad
    )
    BindOsSectionLoadEffect(
        expanded = secureTableExpanded,
        cacheLoaded = cacheLoaded,
        isDataActive = isDataActive,
        visibleCards = visibleCards,
        card = OsSectionCard.SECURE,
        section = SectionKind.SECURE,
        ensureLoad = ensureLoad
    )
    BindOsSectionLoadEffect(
        expanded = globalTableExpanded,
        cacheLoaded = cacheLoaded,
        isDataActive = isDataActive,
        visibleCards = visibleCards,
        card = OsSectionCard.GLOBAL,
        section = SectionKind.GLOBAL,
        ensureLoad = ensureLoad
    )
    BindOsSectionLoadEffect(
        expanded = androidPropsExpanded,
        cacheLoaded = cacheLoaded,
        isDataActive = isDataActive,
        visibleCards = visibleCards,
        card = OsSectionCard.ANDROID,
        section = SectionKind.ANDROID,
        ensureLoad = ensureLoad
    )
    BindOsSectionLoadEffect(
        expanded = javaPropsExpanded,
        cacheLoaded = cacheLoaded,
        isDataActive = isDataActive,
        visibleCards = visibleCards,
        card = OsSectionCard.JAVA,
        section = SectionKind.JAVA,
        ensureLoad = ensureLoad
    )
    BindOsSectionLoadEffect(
        expanded = linuxEnvExpanded,
        cacheLoaded = cacheLoaded,
        isDataActive = isDataActive,
        visibleCards = visibleCards,
        card = OsSectionCard.LINUX,
        section = SectionKind.LINUX,
        ensureLoad = ensureLoad
    )
}

@Composable
private fun BindOsSectionLoadEffect(
    expanded: Boolean,
    cacheLoaded: Boolean,
    isDataActive: Boolean,
    visibleCards: Set<OsSectionCard>,
    card: OsSectionCard,
    section: SectionKind,
    ensureLoad: suspend (SectionKind) -> Unit
) {
    LaunchedEffect(expanded, visibleCards, cacheLoaded, isDataActive) {
        if (!cacheLoaded) return@LaunchedEffect
        if (isDataActive && expanded && isCardVisible(visibleCards, card)) {
            ensureLoad(section)
        }
    }
}

@Composable
internal fun BindOsActivitySuggestionLoadEffect(
    showActivitySuggestionSheet: Boolean,
    googleSystemServiceSuggestionTarget: ShortcutSuggestionField,
    activityShortcutDraftPackageName: String,
    context: Context,
    onPackageSuggestionsLoadingChange: (Boolean) -> Unit,
    onPackageSuggestionsChange: (List<ShortcutInstalledAppOption>) -> Unit,
    onClassSuggestionsLoadingChange: (Boolean) -> Unit,
    onClassSuggestionsChange: (List<ShortcutActivityClassOption>) -> Unit
) {
    LaunchedEffect(
        showActivitySuggestionSheet,
        googleSystemServiceSuggestionTarget,
        activityShortcutDraftPackageName
    ) {
        if (!showActivitySuggestionSheet) return@LaunchedEffect
        when (googleSystemServiceSuggestionTarget) {
            ShortcutSuggestionField.PackageName -> {
                onPackageSuggestionsLoadingChange(true)
                runCatching {
                    withContext(Dispatchers.IO) { loadInstalledAppOptions(context) }
                }.onSuccess(onPackageSuggestionsChange)
                    .onFailure { onPackageSuggestionsChange(emptyList()) }
                onPackageSuggestionsLoadingChange(false)
            }

            ShortcutSuggestionField.ClassName -> {
                val targetPackageName = activityShortcutDraftPackageName.trim()
                if (targetPackageName.isBlank()) {
                    onClassSuggestionsChange(emptyList())
                    return@LaunchedEffect
                }
                onClassSuggestionsLoadingChange(true)
                runCatching {
                    withContext(Dispatchers.IO) {
                        loadActivityClassOptions(
                            context = context,
                            packageName = targetPackageName
                        )
                    }
                }.onSuccess(onClassSuggestionsChange)
                    .onFailure { onClassSuggestionsChange(emptyList()) }
                onClassSuggestionsLoadingChange(false)
            }

            else -> Unit
        }
    }
}

@Composable
internal fun BindOsCardExpandedStateMaps(
    activityShortcutCards: List<OsActivityShortcutCard>,
    activityCardExpanded: MutableMap<String, Boolean>,
    initialGoogleSystemServiceExpanded: Boolean,
    shellCommandCards: List<OsShellCommandCard>,
    shellCommandCardExpanded: MutableMap<String, Boolean>
) {
    LaunchedEffect(activityShortcutCards, initialGoogleSystemServiceExpanded) {
        val currentIds = activityShortcutCards.map { it.id }.toSet()
        activityCardExpanded.keys.toList().forEach { id ->
            if (!currentIds.contains(id)) {
                activityCardExpanded.remove(id)
            }
        }
        activityShortcutCards.forEachIndexed { index, card ->
            val usesStoredDefaultExpansion = index == 0 && (
                card.id == LEGACY_GOOGLE_SYSTEM_SERVICE_CARD_ID ||
                    card.id == BUILTIN_GOOGLE_SETTINGS_SAMPLE_CARD_ID
                )
            if (usesStoredDefaultExpansion) {
                activityCardExpanded[card.id] = initialGoogleSystemServiceExpanded
            } else if (!activityCardExpanded.containsKey(card.id)) {
                activityCardExpanded[card.id] = false
            }
        }
    }

    LaunchedEffect(shellCommandCards) {
        val currentIds = shellCommandCards.map { it.id }.toSet()
        shellCommandCardExpanded.keys.toList().forEach { id ->
            if (!currentIds.contains(id)) {
                shellCommandCardExpanded.remove(id)
            }
        }
        shellCommandCards.forEach { card ->
            if (!shellCommandCardExpanded.containsKey(card.id)) {
                shellCommandCardExpanded[card.id] = false
            }
        }
    }
}
