package os.kei.ui.page.main.os

import android.content.Context
import android.widget.Toast
import os.kei.R
import os.kei.core.system.ShizukuApiUtils
import os.kei.ui.page.main.os.shell.OsShellCommandCard
import os.kei.ui.page.main.os.shell.OsShellCommandCardStore
import os.kei.ui.page.main.os.shortcut.OsActivityCardEditMode
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCard
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCardStore
import os.kei.ui.page.main.os.shortcut.ensureEditorActivityShortcutDraft
import os.kei.ui.page.main.os.shortcut.launchGoogleSystemServiceActivity
import os.kei.ui.page.main.os.shortcut.normalizeActivityShortcutConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal suspend fun ensureOsSectionLoaded(
    section: SectionKind,
    forceRefresh: Boolean,
    visibleCardsProvider: () -> Set<OsSectionCard>,
    sectionStatesProvider: () -> Map<SectionKind, SectionState>,
    sectionLoadMutex: Mutex,
    sectionLoadDeferreds: MutableMap<SectionKind, Deferred<List<InfoRow>>>,
    scope: CoroutineScope,
    context: Context,
    shizukuStatus: String,
    shizukuApiUtils: ShizukuApiUtils,
    updateSection: (SectionKind, (SectionState) -> SectionState) -> Unit,
    onCachePersistedChanged: (Boolean) -> Unit
) {
    if (!visibleSectionKinds(visibleCardsProvider()).contains(section)) return
    val current = sectionStatesProvider()[section] ?: SectionState()
    if (!forceRefresh) {
        if (current.loadedFresh) return
        if (current.rows.isNotEmpty()) return
    }
    var isLoadOwner = false
    lateinit var loadDeferred: Deferred<List<InfoRow>>
    sectionLoadMutex.withLock {
        val inFlight = sectionLoadDeferreds[section]
        if (inFlight != null && inFlight.isActive && !forceRefresh) {
            loadDeferred = inFlight
        } else {
            if (forceRefresh) {
                inFlight?.cancel()
            }
            updateSection(section) { it.copy(loading = true) }
            loadDeferred = scope.async(Dispatchers.IO) {
                buildSectionRows(section, context, shizukuStatus, shizukuApiUtils)
            }
            sectionLoadDeferreds[section] = loadDeferred
            isLoadOwner = true
        }
    }
    if (!isLoadOwner) {
        runCatching { loadDeferred.await() }
        return
    }
    try {
        val fresh = loadDeferred.await()
        updateSection(section) { it.copy(rows = fresh, loading = false, loadedFresh = true) }
        val hasPersistedCache = withContext(Dispatchers.IO) {
            OsInfoCache.write(section, fresh)
            OsInfoCache.readSnapshot(visibleSectionKinds(visibleCardsProvider())).hasPersistedCache
        }
        onCachePersistedChanged(hasPersistedCache)
    } catch (throwable: Throwable) {
        if (throwable is CancellationException) throw throwable
        updateSection(section) { it.copy(loading = false) }
    } finally {
        sectionLoadMutex.withLock {
            if (sectionLoadDeferreds[section] === loadDeferred) {
                sectionLoadDeferreds.remove(section)
            }
        }
    }
}

internal suspend fun applyOsCardVisibility(
    card: OsSectionCard,
    visible: Boolean,
    currentVisibleCards: Set<OsSectionCard>,
    updateVisibleCards: (Set<OsSectionCard>) -> Unit,
    setTopInfoExpanded: (Boolean) -> Unit,
    setShellRunnerExpanded: (Boolean) -> Unit,
    setSystemTableExpanded: (Boolean) -> Unit,
    setSecureTableExpanded: (Boolean) -> Unit,
    setGlobalTableExpanded: (Boolean) -> Unit,
    setAndroidPropsExpanded: (Boolean) -> Unit,
    setJavaPropsExpanded: (Boolean) -> Unit,
    setLinuxEnvExpanded: (Boolean) -> Unit,
    updateSection: (SectionKind, (SectionState) -> SectionState) -> Unit,
    ensureLoad: suspend (SectionKind, Boolean) -> Unit,
    visibleCardsProvider: () -> Set<OsSectionCard>,
    onCachePersistedChanged: (Boolean) -> Unit
) {
    val updated = currentVisibleCards.toMutableSet().apply {
        if (visible) add(card) else remove(card)
    }.toSet()
    updateVisibleCards(updated)
    withContext(Dispatchers.IO) { OsCardVisibilityStore.saveVisibleCards(updated) }
    when (card) {
        OsSectionCard.TOP_INFO -> {
            if (!visible) setTopInfoExpanded(false)
        }
        OsSectionCard.SHELL_RUNNER -> {
            if (!visible) setShellRunnerExpanded(false)
        }
        OsSectionCard.GOOGLE_SYSTEM_SERVICE -> Unit
        OsSectionCard.SYSTEM -> {
            if (!visible) {
                setSystemTableExpanded(false)
                updateSection(SectionKind.SYSTEM) { SectionState() }
                withContext(Dispatchers.IO) { OsInfoCache.clear(SectionKind.SYSTEM) }
            } else {
                ensureLoad(SectionKind.SYSTEM, true)
            }
        }
        OsSectionCard.SECURE -> {
            if (!visible) {
                setSecureTableExpanded(false)
                updateSection(SectionKind.SECURE) { SectionState() }
                withContext(Dispatchers.IO) { OsInfoCache.clear(SectionKind.SECURE) }
            } else {
                ensureLoad(SectionKind.SECURE, true)
            }
        }
        OsSectionCard.GLOBAL -> {
            if (!visible) {
                setGlobalTableExpanded(false)
                updateSection(SectionKind.GLOBAL) { SectionState() }
                withContext(Dispatchers.IO) { OsInfoCache.clear(SectionKind.GLOBAL) }
            } else {
                ensureLoad(SectionKind.GLOBAL, true)
            }
        }
        OsSectionCard.ANDROID -> {
            if (!visible) {
                setAndroidPropsExpanded(false)
                updateSection(SectionKind.ANDROID) { SectionState() }
                withContext(Dispatchers.IO) { OsInfoCache.clear(SectionKind.ANDROID) }
            } else {
                ensureLoad(SectionKind.ANDROID, true)
            }
        }
        OsSectionCard.JAVA -> {
            if (!visible) {
                setJavaPropsExpanded(false)
                updateSection(SectionKind.JAVA) { SectionState() }
                withContext(Dispatchers.IO) { OsInfoCache.clear(SectionKind.JAVA) }
            } else {
                ensureLoad(SectionKind.JAVA, true)
            }
        }
        OsSectionCard.LINUX -> {
            if (!visible) {
                setLinuxEnvExpanded(false)
                updateSection(SectionKind.LINUX) { SectionState() }
                withContext(Dispatchers.IO) { OsInfoCache.clear(SectionKind.LINUX) }
            } else {
                ensureLoad(SectionKind.LINUX, true)
            }
        }
    }
    val hasPersistedCache = withContext(Dispatchers.IO) {
        OsInfoCache.readSnapshot(visibleSectionKinds(visibleCardsProvider())).hasPersistedCache
    }
    onCachePersistedChanged(hasPersistedCache)
}

internal suspend fun applyOsActivityCardVisibility(
    cardId: String,
    visible: Boolean,
    currentCards: List<OsActivityShortcutCard>,
    defaults: OsGoogleSystemServiceConfig,
    updateCards: (List<OsActivityShortcutCard>) -> Unit
) {
    val updatedCards = currentCards.map { card ->
        if (card.id == cardId) card.copy(visible = visible) else card
    }
    updateCards(updatedCards)
    withContext(Dispatchers.IO) {
        OsActivityShortcutCardStore.saveCards(
            cards = updatedCards,
            defaults = defaults
        )
    }
}

internal suspend fun applyOsShellCommandCardVisibility(
    cardId: String,
    visible: Boolean,
    updateCards: (List<OsShellCommandCard>) -> Unit
) {
    val updatedCards = withContext(Dispatchers.IO) {
        OsShellCommandCardStore.setCardVisible(cardId = cardId, visible = visible)
    }
    updateCards(updatedCards)
}

internal suspend fun runOsShellCommandCard(
    card: OsShellCommandCard,
    context: Context,
    shizukuApiUtils: ShizukuApiUtils,
    shellCardCommandRequiredToast: String,
    shellRunNoPermissionToast: String,
    shellRunNoOutputText: String,
    runningCardIdsProvider: () -> Set<String>,
    updateRunningCardIds: (Set<String>) -> Unit,
    onCardsReload: () -> Unit,
    runFailedMessage: (Throwable) -> String
) {
    val command = card.command.trim()
    if (command.isBlank()) {
        Toast.makeText(context, shellCardCommandRequiredToast, Toast.LENGTH_SHORT).show()
        return
    }
    if (runningCardIdsProvider().contains(card.id)) return
    if (!shizukuApiUtils.canUseCommand()) {
        shizukuApiUtils.requestPermissionIfNeeded()
        Toast.makeText(context, shellRunNoPermissionToast, Toast.LENGTH_SHORT).show()
        return
    }
    updateRunningCardIds(runningCardIdsProvider() + card.id)
    try {
        val output = withContext(Dispatchers.IO) {
            shizukuApiUtils.execCommandCancellable(
                command = command,
                timeoutMs = 300_000L
            )
        }.orEmpty().trim().ifBlank { shellRunNoOutputText }
        withContext(Dispatchers.IO) {
            OsShellCommandCardStore.updateCardRunResult(
                cardId = card.id,
                runOutput = output
            )
        }
        onCardsReload()
    } catch (throwable: CancellationException) {
        throw throwable
    } catch (throwable: Throwable) {
        Toast.makeText(
            context,
            runFailedMessage(throwable),
            Toast.LENGTH_SHORT
        ).show()
    } finally {
        updateRunningCardIds(runningCardIdsProvider() - card.id)
    }
}

internal suspend fun refreshAllOsSections(
    context: Context,
    visibleCardsProvider: () -> Set<OsSectionCard>,
    setRefreshing: (Boolean) -> Unit,
    setRefreshProgress: (Float) -> Unit,
    ensureLoad: suspend (SectionKind, Boolean) -> Unit,
    noRefreshableCardText: String,
    refreshCompletedText: String
) {
    setRefreshing(true)
    setRefreshProgress(0f)
    try {
        val targets = SectionKind.entries.filter { visibleSectionKinds(visibleCardsProvider()).contains(it) }
        val sectionCount = targets.size.coerceAtLeast(1)
        targets.forEachIndexed { index, section ->
            ensureLoad(section, true)
            setRefreshProgress((index + 1).toFloat() / sectionCount.toFloat())
        }
        Toast.makeText(
            context,
            if (targets.isEmpty()) noRefreshableCardText else refreshCompletedText,
            Toast.LENGTH_SHORT
        ).show()
    } finally {
        setRefreshing(false)
    }
}

internal suspend fun exportOsSectionCard(
    card: OsSectionCard,
    currentExportingCard: OsSectionCard?,
    updateExportingCard: (OsSectionCard?) -> Unit,
    visibleCardsProvider: () -> Set<OsSectionCard>,
    ensureLoad: suspend (SectionKind, Boolean) -> Unit,
    sectionStatesProvider: () -> Map<SectionKind, SectionState>,
    activityShortcutCardsProvider: () -> List<OsActivityShortcutCard>,
    googleSystemServiceDefaults: OsGoogleSystemServiceConfig,
    context: Context,
    shizukuStatus: String,
    launchExport: (fileName: String, payload: String) -> Unit,
    onExportFailed: (Throwable) -> Unit
) {
    if (currentExportingCard != null) return
    updateExportingCard(card)
    try {
        when (card) {
            OsSectionCard.TOP_INFO -> {
                visibleSectionKinds(visibleCardsProvider()).forEach { section ->
                    ensureLoad(section, false)
                }
            }
            else -> {
                sectionKindByCard(card)?.let { section ->
                    ensureLoad(section, false)
                }
            }
        }
        val rows = currentRowsForCard(
            card = card,
            sectionStates = sectionStatesProvider(),
            googleSystemServiceConfig = activityShortcutCardsProvider().firstOrNull()?.config
                ?: googleSystemServiceDefaults,
            googleSystemServiceDefaults = googleSystemServiceDefaults,
            context = context
        )
        val generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val payload = buildOsCardJson(
            generatedAt = generatedAt,
            shizukuStatus = shizukuStatus,
            cardTitle = card.title(context),
            rows = rows
        )
        val exportStamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.getDefault()).format(Date())
        val fileName = "keios-os-${exportSlug(card)}-$exportStamp.json"
        launchExport(fileName, payload)
    } catch (throwable: Throwable) {
        onExportFailed(throwable)
    } finally {
        updateExportingCard(null)
    }
}

internal suspend fun exportOsPageCard(
    card: OsSectionCard,
    currentExportingCard: OsSectionCard?,
    updateExportingCard: (OsSectionCard?) -> Unit,
    visibleCardsProvider: () -> Set<OsSectionCard>,
    ensureLoad: suspend (SectionKind, Boolean) -> Unit,
    sectionStatesProvider: () -> Map<SectionKind, SectionState>,
    activityShortcutCardsProvider: () -> List<OsActivityShortcutCard>,
    googleSystemServiceDefaults: OsGoogleSystemServiceConfig,
    context: Context,
    shizukuStatus: String,
    launchExport: (fileName: String, payload: String) -> Unit
) {
    exportOsSectionCard(
        card = card,
        currentExportingCard = currentExportingCard,
        updateExportingCard = updateExportingCard,
        visibleCardsProvider = visibleCardsProvider,
        ensureLoad = ensureLoad,
        sectionStatesProvider = sectionStatesProvider,
        activityShortcutCardsProvider = activityShortcutCardsProvider,
        googleSystemServiceDefaults = googleSystemServiceDefaults,
        context = context,
        shizukuStatus = shizukuStatus,
        launchExport = launchExport,
        onExportFailed = { throwable ->
            Toast.makeText(
                context,
                context.getString(R.string.common_export_failed_with_reason, throwable.javaClass.simpleName),
                Toast.LENGTH_SHORT
            ).show()
        }
    )
}

internal fun openOsActivityShortcutCard(
    context: Context,
    card: OsActivityShortcutCard,
    defaults: OsGoogleSystemServiceConfig,
    invalidTargetMessage: String,
    openFailedMessage: (Throwable) -> String
) {
    val normalized = normalizeActivityShortcutConfig(
        config = card.config,
        defaults = defaults
    )
    if (normalized.packageName.isBlank()) {
        Toast.makeText(context, invalidTargetMessage, Toast.LENGTH_SHORT).show()
        return
    }
    runCatching {
        launchGoogleSystemServiceActivity(
            context = context,
            config = normalized,
            defaults = defaults
        )
    }.onFailure { error ->
        Toast.makeText(
            context,
            openFailedMessage(error),
            Toast.LENGTH_SHORT
        ).show()
    }
}

internal fun beginEditingOsActivityShortcutCard(
    card: OsActivityShortcutCard,
    defaults: OsGoogleSystemServiceConfig,
    onEditModeChange: (OsActivityCardEditMode) -> Unit,
    onEditingCardIdChange: (String?) -> Unit,
    onEditingBuiltInChange: (Boolean) -> Unit,
    onDraftChange: (OsGoogleSystemServiceConfig) -> Unit,
    onShowEditorChange: (Boolean) -> Unit
) {
    onEditModeChange(OsActivityCardEditMode.Edit)
    onEditingCardIdChange(card.id)
    onEditingBuiltInChange(card.isBuiltInSample)
    onDraftChange(
        ensureEditorActivityShortcutDraft(
            normalizeActivityShortcutConfig(
                config = card.config,
                defaults = defaults
            )
        )
    )
    onShowEditorChange(true)
}
