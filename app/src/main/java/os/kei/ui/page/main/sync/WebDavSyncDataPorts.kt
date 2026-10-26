package os.kei.ui.page.main.sync

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import os.kei.BuildConfig
import os.kei.R
import os.kei.core.background.AppBackgroundScheduler
import os.kei.core.json.KeiJson
import os.kei.core.json.encodeCompact
import os.kei.feature.github.data.local.GitHubTrackedItemsImportPayload
import os.kei.feature.github.domain.GitHubTrackedItemsTransferService
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.GitHubTrackedLocalAppType
import os.kei.feature.github.model.defaultKeiOsTrackedApp
import os.kei.ui.page.main.os.OsGoogleSystemServiceConfig
import os.kei.ui.page.main.os.shell.OsShellCommandCard
import os.kei.ui.page.main.os.shell.OsShellCommandCardStore
import os.kei.ui.page.main.os.shell.buildBuiltInShellCommandCards
import os.kei.ui.page.main.os.shell.rememberBuiltInShellCommandCards
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCard
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCardStore
import os.kei.ui.page.main.os.shortcut.ShortcutIntentExtra
import os.kei.ui.page.main.os.shortcut.buildBuiltInActivityShortcutCards
import os.kei.ui.page.main.os.shortcut.normalizeActivityShortcutConfig
import os.kei.ui.page.main.os.shortcut.osActivityShortcutMergeKey
import os.kei.ui.page.main.os.shortcut.rememberBuiltInActivityShortcutCards
import os.kei.ui.page.main.os.transfer.OsCardTransferService
import os.kei.ui.page.main.os.transfer.parseOsCardImportRoot
import os.kei.ui.page.main.student.GuideBgmFavoriteStore
import os.kei.ui.page.main.student.catalog.BaGuideCatalogStore
import os.kei.ui.page.main.student.catalog.page.buildCatalogFavoritesExportJson
import os.kei.ui.page.main.student.catalog.page.parseCatalogFavoritesExport
import os.kei.ui.page.main.ba.support.BASettingsStore
import os.kei.ui.page.main.ba.support.buildBaAccountsSyncFingerprintJson
import java.util.Locale

internal const val BA_ACCOUNTS_FINGERPRINT_REVISION = 3

/**
 * Builds the [WebDavSyncDataPort] for every [WebDavSyncItem].
 *
 * Two entry points share the same port-building logic:
 *  - [rememberWebDavSyncDataPorts] for the Compose UI (resolves string resources + built-in cards
 *    via [androidx.compose.runtime.Composable] helpers).
 *  - [buildWebDavSyncDataPorts] (Context overload) for background callers such as the auto-sync
 *    coordinator, which cannot enter composition.
 *
 * Every port's `merge` performs a union merge so two-way sync can never drop local-only data.
 */
@Composable
internal fun rememberWebDavSyncDataPorts(): Map<WebDavSyncItem, WebDavSyncDataPort> {
    val context = LocalContext.current
    val defaultIntentFlags = stringResource(R.string.os_google_system_service_default_intent_flags)
    val defaults = remember(defaultIntentFlags) {
        OsGoogleSystemServiceConfig(intentFlags = defaultIntentFlags).normalized()
    }
    val builtInActivityCards = rememberBuiltInActivityShortcutCards(
        defaults = defaults,
        defaultIntentFlags = defaultIntentFlags,
    )
    val builtInShellCards = rememberBuiltInShellCommandCards()
    return remember(context, builtInActivityCards, builtInShellCards, defaults) {
        buildWebDavSyncDataPorts(
            context = context,
            googleSystemServiceDefaults = defaults,
            builtInActivityShortcutCards = builtInActivityCards,
            builtInShellCommandCards = builtInShellCards,
        )
    }
}

/**
 * Non-Compose factory: resolve built-in defaults from string resources via [context] so auto-sync
 * can build ports from application scope.
 */
internal fun buildWebDavSyncDataPorts(context: Context): Map<WebDavSyncItem, WebDavSyncDataPort> {
    val builtIns = resolveWebDavBuiltInCardSets(context)
    return buildWebDavSyncDataPorts(
        context = context,
        googleSystemServiceDefaults = builtIns.googleSystemServiceDefaults,
        builtInActivityShortcutCards = builtIns.activityShortcutCards,
        builtInShellCommandCards = builtIns.shellCommandCards,
    )
}

private fun buildWebDavSyncDataPorts(
    context: Context,
    googleSystemServiceDefaults: OsGoogleSystemServiceConfig,
    builtInActivityShortcutCards: List<OsActivityShortcutCard>,
    builtInShellCommandCards: List<OsShellCommandCard>,
): Map<WebDavSyncItem, WebDavSyncDataPort> =
    mapOf(
        WebDavSyncItem.GitHubTracked to WebDavSyncDataPort(
            replace = { raw ->
                val payload = GitHubTrackedItemsTransferService.parseImport(raw)
                GitHubTrackedItemsTransferService.replaceFromSync(
                    payload.copy(items = normalizeGitHubTrackedItemsForSync(payload.items)),
                    onRefreshNeeded = { AppBackgroundScheduler.scheduleGitHubRefresh(context) },
                )
            },
            exportJson = {
                val items =
                    normalizeGitHubTrackedItemsForSync(
                        GitHubTrackedItemsTransferService.loadItems(),
                    )
                GitHubTrackedItemsTransferService.buildExportJson(items)
            },
            fingerprintJson = {
                val items =
                    normalizeGitHubTrackedItemsForSync(
                        GitHubTrackedItemsTransferService.loadItems(),
                    )
                GitHubTrackedItemsTransferService.buildExportJson(items, exportedAtMillis = 0L)
            },
            remoteFingerprintJson = { raw ->
                val payload = GitHubTrackedItemsTransferService.parseImport(raw)
                GitHubTrackedItemsTransferService.buildExportJson(
                    normalizeGitHubTrackedItemsForSync(payload.items),
                    exportedAtMillis = 0L,
                )
            },
            merge = { raw ->
                val payload = GitHubTrackedItemsTransferService.parseImport(raw)
                GitHubTrackedItemsTransferService.applyImport(
                    payload = payload,
                    onRefreshNeeded = { AppBackgroundScheduler.scheduleGitHubRefresh(context) },
                    existingItems = GitHubTrackedItemsTransferService.loadItems(),
                )
            },
            localCount = {
                normalizeGitHubTrackedItemsForSync(
                    GitHubTrackedItemsTransferService.loadItems(),
                ).size
            },
            countRemoteItems = { raw ->
                runCatching {
                    normalizeGitHubTrackedItemsForSync(
                        GitHubTrackedItemsTransferService.parseImport(raw).items,
                    ).size
                }
                    .getOrDefault(0)
            },
        ),
        WebDavSyncItem.BaAccounts to WebDavSyncDataPort(
            replace = { raw ->
                BASettingsStore.replaceAccountsSyncJson(raw)
                AppBackgroundScheduler.scheduleBaApThreshold(context)
            },
            exportJson = { BASettingsStore.buildAccountsSyncExportJson() },
            fingerprintJson = {
                BASettingsStore.buildAccountsSyncExportJson(nowMs = 0L)
                    .let(::buildBaAccountsSyncFingerprintJson)
            },
            remoteFingerprintJson = ::buildBaAccountsSyncFingerprintJson,
            merge = { raw ->
                BASettingsStore.mergeAccountsSyncJson(raw)
                AppBackgroundScheduler.scheduleBaApThreshold(context)
            },
            localCount = { BASettingsStore.loadAccountState().accounts.size },
            countRemoteItems = { raw ->
                runCatching { BASettingsStore.countAccountsSyncJson(raw) }.getOrDefault(0)
            },
            mergeRemoteOnAutoConflict = true,
            fingerprintRevision = BA_ACCOUNTS_FINGERPRINT_REVISION,
        ),
        WebDavSyncItem.BaCatalogFavorites to WebDavSyncDataPort(
            replace = { raw -> BaGuideCatalogStore.saveFavorites(parseCatalogFavoritesExport(raw, 1L)) },
            exportJson = {
                buildCatalogFavoritesExportJson(BaGuideCatalogStore.loadFavorites())
            },
            fingerprintJson = {
                buildCatalogFavoritesExportJson(BaGuideCatalogStore.loadFavorites(), nowMs = 0L)
            },
            remoteFingerprintJson = { raw ->
                buildCatalogFavoritesExportJson(parseCatalogFavoritesExport(raw, 1L), nowMs = 0L)
            },
            merge = { raw ->
                val imported = parseCatalogFavoritesExport(raw, 1L)
                val existing = BaGuideCatalogStore.loadFavorites()
                // Union of favorited content; keep the earliest favorited timestamp on conflict.
                val merged = HashMap(existing)
                imported.forEach { (contentId, favoritedAtMs) ->
                    val current = merged[contentId]
                    merged[contentId] = when {
                        current == null -> favoritedAtMs
                        favoritedAtMs <= 0L -> current
                        else -> minOf(current, favoritedAtMs)
                    }
                }
                BaGuideCatalogStore.saveFavorites(merged)
            },
            localCount = { BaGuideCatalogStore.loadFavorites().size },
            countRemoteItems = { raw ->
                runCatching { parseCatalogFavoritesExport(raw).size }.getOrDefault(0)
            },
            mergeRemoteOnAutoConflict = true,
        ),
        WebDavSyncItem.BaBgmFavorites to WebDavSyncDataPort(
            replace = { raw -> GuideBgmFavoriteStore.replaceFavoritesFromSync(raw) },
            exportJson = { GuideBgmFavoriteStore.buildFavoritesExportJson() },
            fingerprintJson = { GuideBgmFavoriteStore.buildFavoritesExportJson(nowMs = 0L) },
            remoteFingerprintJson = { raw -> GuideBgmFavoriteStore.buildFavoritesFingerprintJson(raw) },
            merge = { raw -> GuideBgmFavoriteStore.importFavoritesJsonMerged(raw) },
            localCount = { GuideBgmFavoriteStore.favoritesSnapshot().size },
            countRemoteItems = { raw ->
                runCatching { GuideBgmFavoriteStore.previewFavoritesJsonImport(raw).importedCount }
                    .getOrDefault(0)
            },
            mergeRemoteOnAutoConflict = true,
        ),
        WebDavSyncItem.OsActivityCards to WebDavSyncDataPort(
            replace = { raw ->
                val payload = OsCardTransferService.parseActivityImportPayload(
                    raw, googleSystemServiceDefaults, googleSystemServiceDefaults, builtInActivityShortcutCards,
                )
                OsActivityShortcutCardStore.replaceFromSync(payload.cards, googleSystemServiceDefaults)
            },
            exportJson = {
                val cards = OsActivityShortcutCardStore.loadCards(
                    defaults = googleSystemServiceDefaults,
                    builtInSampleDefaults = googleSystemServiceDefaults,
                    builtInActivityShortcutCards = builtInActivityShortcutCards,
                )
                OsCardTransferService.buildActivityCardsExportJson(
                    cards = cards,
                    defaults = googleSystemServiceDefaults,
                )
            },
            fingerprintJson = {
                val cards = OsActivityShortcutCardStore.loadCards(
                    defaults = googleSystemServiceDefaults,
                    builtInSampleDefaults = googleSystemServiceDefaults,
                    builtInActivityShortcutCards = builtInActivityShortcutCards,
                )
                buildOsActivityCardsSyncFingerprintJson(
                    cards = cards,
                    defaults = googleSystemServiceDefaults,
                )
            },
            remoteFingerprintJson = { raw ->
                buildRemoteOsActivityCardsSyncFingerprintJson(
                    raw = raw,
                    defaults = googleSystemServiceDefaults,
                    builtInSampleDefaults = googleSystemServiceDefaults,
                    builtInActivityShortcutCards = builtInActivityShortcutCards,
                )
            },
            merge = { raw ->
                val payload =
                    OsCardTransferService.parseActivityImportPayload(
                        raw = raw,
                        defaults = googleSystemServiceDefaults,
                        builtInSampleDefaults = googleSystemServiceDefaults,
                        builtInActivityShortcutCards = builtInActivityShortcutCards,
                    )
                OsCardTransferService.applyActivityImport(
                    payload = payload,
                    existingCards =
                        OsActivityShortcutCardStore.loadCards(
                            defaults = googleSystemServiceDefaults,
                            builtInSampleDefaults = googleSystemServiceDefaults,
                            builtInActivityShortcutCards = builtInActivityShortcutCards,
                        ),
                    defaults = googleSystemServiceDefaults,
                    builtInSampleDefaults = googleSystemServiceDefaults,
                    builtInActivityShortcutCards = builtInActivityShortcutCards,
                )
            },
            localCount = {
                OsActivityShortcutCardStore.loadCards(
                    defaults = googleSystemServiceDefaults,
                    builtInSampleDefaults = googleSystemServiceDefaults,
                    builtInActivityShortcutCards = builtInActivityShortcutCards,
                ).size
            },
            countRemoteItems = { raw ->
                runCatching {
                    OsActivityShortcutCardStore.parseCardsImport(
                        root = parseOsCardImportRoot(raw),
                        defaults = googleSystemServiceDefaults,
                        builtInSampleDefaults = googleSystemServiceDefaults,
                        builtInActivityShortcutCards = builtInActivityShortcutCards,
                    ).cards.size
                }.getOrDefault(0)
            },
            mergeRemoteOnAutoConflict = true,
        ),
        WebDavSyncItem.OsShellCards to WebDavSyncDataPort(
            replace = { raw ->
                val payload = OsCardTransferService.parseShellImportPayload(raw)
                OsShellCommandCardStore.replaceFromSync(payload.cards)
            },
            exportJson = {
                val cards = OsShellCommandCardStore.loadCards(
                    builtInShellCommandCards = builtInShellCommandCards,
                )
                OsCardTransferService.buildShellCardsExportJson(cards)
            },
            fingerprintJson = {
                buildOsShellCardsSyncFingerprintJson(
                    OsShellCommandCardStore.loadCards(
                        builtInShellCommandCards = builtInShellCommandCards,
                    ),
                )
            },
            remoteFingerprintJson = { raw -> buildRemoteOsShellCardsSyncFingerprintJson(raw) },
            merge = { raw ->
                val payload = OsCardTransferService.parseShellImportPayload(raw)
                OsCardTransferService.applyShellImport(
                    payload = payload,
                    existingCards =
                        OsShellCommandCardStore.loadCards(
                            builtInShellCommandCards = builtInShellCommandCards,
                        ),
                )
            },
            localCount = {
                OsShellCommandCardStore.loadCards(
                    builtInShellCommandCards = builtInShellCommandCards,
                ).size
            },
            countRemoteItems = { raw ->
                runCatching {
                    OsShellCommandCardStore.parseCardsImport(
                        root = parseOsCardImportRoot(raw),
                    ).cards.size
                }.getOrDefault(0)
            },
            mergeRemoteOnAutoConflict = true,
        ),
    ).mapValues { (item, port) ->
        port.copy(remoteFingerprintJson = { raw ->
            validateWebDavPayload(item, raw)
            port.remoteFingerprintJson(raw)
        })
    }

internal fun resolveWebDavBuiltInCardSets(context: Context): WebDavBuiltInCardSets {
    val defaultIntentFlags =
        context.getString(R.string.os_google_system_service_default_intent_flags)
    val defaults = OsGoogleSystemServiceConfig(intentFlags = defaultIntentFlags).normalized()
    return WebDavBuiltInCardSets(
        googleSystemServiceDefaults = defaults,
        activityShortcutCards =
            buildBuiltInActivityShortcutCards(
                context = context,
                defaults = defaults,
                defaultIntentFlags = defaultIntentFlags,
            ),
        shellCommandCards = buildBuiltInShellCommandCards(context),
    )
}

internal data class WebDavBuiltInCardSets(
    val googleSystemServiceDefaults: OsGoogleSystemServiceConfig,
    val activityShortcutCards: List<OsActivityShortcutCard>,
    val shellCommandCards: List<OsShellCommandCard>,
)

internal fun buildOsActivityCardsSyncFingerprintJson(
    cards: List<OsActivityShortcutCard>,
    defaults: OsGoogleSystemServiceConfig,
): String =
    buildJsonObject {
        put("schema", "keios.os.activity.sync_fingerprint")
        put("schemaVersion", 1)
        put(
            "items",
            buildJsonArray {
                cards
                    .asActivitySyncFingerprintItems(defaults)
                    .forEach { item ->
                        add(
                            buildJsonObject {
                                put("identity", item.identity)
                                put("builtIn", item.builtIn)
                                put("visible", item.visible)
                                item.config?.let { config ->
                                    put("title", config.title)
                                    put("subtitle", config.subtitle)
                                    put("appName", config.appName)
                                    put("packageName", config.packageName)
                                    put("className", config.className)
                                    put("intentAction", config.intentAction)
                                    put("intentCategory", config.intentCategory)
                                    put("intentFlags", config.intentFlags)
                                    put("intentUriData", config.intentUriData)
                                    put("intentMimeType", config.intentMimeType)
                                    put(
                                        "intentExtras",
                                        buildJsonArray {
                                            config.intentExtras.forEach { extra ->
                                                add(
                                                    buildJsonObject {
                                                        put("key", extra.key)
                                                        put("type", extra.type.rawValue)
                                                        put("value", extra.value)
                                                    },
                                                )
                                            }
                                        },
                                    )
                                }
                            },
                        )
                    }
            },
        )
    }.encodeCompact(KeiJson.pretty)

internal fun buildRemoteOsActivityCardsSyncFingerprintJson(
    raw: String,
    defaults: OsGoogleSystemServiceConfig,
    builtInSampleDefaults: OsGoogleSystemServiceConfig,
    builtInActivityShortcutCards: List<OsActivityShortcutCard>,
): String {
    val payload =
        OsCardTransferService.parseActivityImportPayload(
            raw = raw,
            defaults = defaults,
            builtInSampleDefaults = builtInSampleDefaults,
            builtInActivityShortcutCards = builtInActivityShortcutCards,
        )
    require(payload.invalidCount == 0) { "Invalid activity card backup" }
    return buildOsActivityCardsSyncFingerprintJson(
        cards = payload.cards,
        defaults = defaults,
    )
}

internal fun buildOsShellCardsSyncFingerprintJson(cards: List<OsShellCommandCard>): String =
    OsShellCommandCardStore.buildCardsExportJson(
        cards = cards.map(OsShellCommandCard::asSyncFingerprintCard),
        exportedAtMillis = 0L,
    )

internal fun buildRemoteOsShellCardsSyncFingerprintJson(raw: String): String {
    val payload = OsCardTransferService.parseShellImportPayload(raw)
    require(payload.invalidCount == 0) { "Invalid shell card backup" }
    return buildOsShellCardsSyncFingerprintJson(payload.cards)
}

private fun List<OsActivityShortcutCard>.asActivitySyncFingerprintItems(
    defaults: OsGoogleSystemServiceConfig,
): List<OsActivitySyncFingerprintItem> =
    map { card ->
        val normalizedConfig =
            normalizeActivityShortcutConfig(card.config, defaults)
                .copy(intentExtras = card.config.intentExtras.asActivitySyncFingerprintExtras())
                .let { normalizeActivityShortcutConfig(it, defaults) }
        val normalizedCard = card.copy(config = normalizedConfig)
        if (normalizedCard.isBuiltInSample) {
            OsActivitySyncFingerprintItem(
                identity = "built-in:${normalizedCard.id.trim()}",
                builtIn = true,
                visible = normalizedCard.visible,
                config = null,
            )
        } else {
            OsActivitySyncFingerprintItem(
                identity = "custom:${osActivityShortcutMergeKey(normalizedCard)}",
                builtIn = false,
                visible = normalizedCard.visible,
                config = normalizedConfig,
            )
        }
    }.sortedWith(
        compareBy<OsActivitySyncFingerprintItem> { it.identity }
            .thenBy { it.visible.toString() },
    )

private data class OsActivitySyncFingerprintItem(
    val identity: String,
    val builtIn: Boolean,
    val visible: Boolean,
    val config: OsGoogleSystemServiceConfig?,
)

private fun List<ShortcutIntentExtra>.asActivitySyncFingerprintExtras(): List<ShortcutIntentExtra> =
    map { extra ->
        extra.copy(
            key = extra.key.trim(),
            value = extra.value.trim(),
        )
    }
        .filter { it.key.isNotBlank() }
        .sortedWith(
            compareBy<ShortcutIntentExtra> { it.key.lowercase(Locale.ROOT) }
                .thenBy { it.type.rawValue }
                .thenBy { it.value },
        )

private fun OsShellCommandCard.asSyncFingerprintCard(): OsShellCommandCard =
    copy(
        runOutput = "",
        lastRunAtMillis = 0L,
        createdAtMillis = OS_SHELL_CARD_SYNC_FINGERPRINT_TIMESTAMP_MS,
        updatedAtMillis = OS_SHELL_CARD_SYNC_FINGERPRINT_TIMESTAMP_MS,
    )

private const val OS_SHELL_CARD_SYNC_FINGERPRINT_TIMESTAMP_MS = 1L

internal fun normalizeGitHubTrackedItemsForSync(items: List<GitHubTrackedApp>): List<GitHubTrackedApp> {
    val deduplicated = LinkedHashMap<String, GitHubTrackedApp>()
    items.forEach { item ->
        deduplicated[item.id] = item
    }
    val selfTrack = defaultKeiOsTrackedApp(packageName = BuildConfig.APPLICATION_ID)
    if (deduplicated.containsKey(selfTrack.id)) {
        return deduplicated.values.toList()
    }
    return listOf(selfTrack) + deduplicated.values
}

internal fun mergeGitHubTrackedItemsForSync(
    existingItems: List<GitHubTrackedApp>,
    payload: GitHubTrackedItemsImportPayload,
): List<GitHubTrackedApp> = mergeGitHubTrackedItemsForSync(existingItems, payload.items)

internal fun mergeGitHubTrackedItemsForSync(
    existingItems: List<GitHubTrackedApp>,
    importedItems: List<GitHubTrackedApp>,
): List<GitHubTrackedApp> {
    if (importedItems.isEmpty()) {
        return normalizeGitHubTrackedItemsForSync(existingItems)
    }
    val mergedItems = existingItems.toMutableList()
    val indexById =
        mergedItems
            .withIndex()
            .associate { it.value.id to it.index }
            .toMutableMap()
    importedItems.forEach { item ->
        val existingIndex = indexById[item.id]
        if (existingIndex == null) {
            mergedItems += item
            indexById[item.id] = mergedItems.lastIndex
        } else {
            val existingItem = mergedItems[existingIndex]
            mergedItems[existingIndex] = item.withTrackedLocalAppTypeFallback(existingItem)
        }
    }
    return normalizeGitHubTrackedItemsForSync(mergedItems)
}

private fun GitHubTrackedApp.withTrackedLocalAppTypeFallback(existingItem: GitHubTrackedApp): GitHubTrackedApp =
    if (localAppType == GitHubTrackedLocalAppType.Unknown) {
        copy(localAppType = existingItem.localAppType)
    } else {
        this
    }
