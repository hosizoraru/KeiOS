package com.example.keios.ui.page.main

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

@Composable
internal fun BindOsExpandedStatePersistence(
    ready: Boolean,
    snapshotProvider: () -> OsUiSnapshot
) {
    LaunchedEffect(ready) {
        if (!ready) return@LaunchedEffect
        snapshotFlow {
            snapshotProvider()
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
    visibleCards: Set<OsSectionCard>,
    onVisibleCardsChange: (Set<OsSectionCard>) -> Unit,
    onSectionStatesChange: (Map<SectionKind, SectionState>) -> Unit,
    onCachePersistedChange: (Boolean) -> Unit,
    onCacheLoadedChange: (Boolean) -> Unit,
    onUiStatePersistenceReadyChange: (Boolean) -> Unit,
    isPageActive: Boolean,
    ensureLoad: suspend (SectionKind, Boolean) -> Unit
) {
    LaunchedEffect(Unit) {
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
internal fun BindOsCardExpandedStateMaps(
    activityShortcutCards: List<OsActivityShortcutCard>,
    activityCardExpanded: MutableMap<String, Boolean>,
    initialGoogleSystemServiceExpanded: Boolean,
    shellCommandCards: List<OsShellCommandCard>,
    shellCommandCardExpanded: MutableMap<String, Boolean>
) {
    LaunchedEffect(activityShortcutCards) {
        val currentIds = activityShortcutCards.map { it.id }.toSet()
        activityCardExpanded.keys.toList().forEach { id ->
            if (!currentIds.contains(id)) {
                activityCardExpanded.remove(id)
            }
        }
        activityShortcutCards.forEachIndexed { index, card ->
            if (!activityCardExpanded.containsKey(card.id)) {
                activityCardExpanded[card.id] =
                    if (
                        index == 0 && (
                            card.id == LEGACY_GOOGLE_SYSTEM_SERVICE_CARD_ID ||
                                card.id == BUILTIN_GOOGLE_SETTINGS_SAMPLE_CARD_ID
                            )
                    ) {
                        initialGoogleSystemServiceExpanded
                    } else {
                        false
                    }
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
