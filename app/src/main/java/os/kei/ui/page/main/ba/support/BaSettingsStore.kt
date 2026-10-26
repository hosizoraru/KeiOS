@file:Suppress("ktlint:standard:filename")

package os.kei.ui.page.main.ba.support

import com.tencent.mmkv.MMKV
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import os.kei.core.json.KeiJson
import os.kei.core.prefs.KeiMmkv
import os.kei.ui.page.main.ba.BaReminderCoordinator
import java.util.UUID

internal object BASettingsStore {
    private val store: MMKV by lazy { KeiMmkv.byId(BA_SETTINGS_KV_ID) }

    private fun kv(): MMKV = store

    private fun notifyChanged(notifyHomeOverview: Boolean = true) {
        BASettingsStoreSignals.notifyChanged(notifyHomeOverview = notifyHomeOverview)
    }

    private fun idSettings(): BaIdSettingsAccessor = BaIdSettingsAccessor(MmkvBaSettingsKeyValueStore(kv()))

    private fun accountKeyValueStore(): MmkvBaSettingsKeyValueStore = MmkvBaSettingsKeyValueStore(kv())

    private fun apAcknowledgementRuntimeRepository(): BaApAcknowledgementRuntimeRepository =
        BaApAcknowledgementRuntimeRepository(accountKeyValueStore()) {
            notifyChanged(notifyHomeOverview = false)
        }

    private fun BaPageSnapshot.withLocalApAcknowledgements(accountId: BaAccountId): BaPageSnapshot =
        apAcknowledgementRuntimeRepository().withLocalAcknowledgements(this, accountId)

    private fun accountStore(keyValueStore: MmkvBaSettingsKeyValueStore = accountKeyValueStore()): BaAccountStore =
        BaAccountStore(keyValueStore)

    private fun migratedAccountStore(): BaAccountStore {
        val keyValueStore = accountKeyValueStore()
        val store = accountStore(keyValueStore)
        BaAccountMigration(
            accountStore = store,
            keyValueStore = keyValueStore,
        ).migrateLegacyIfNeeded()
        return store
    }

    private fun cacheStore(): BaSettingsCacheStore = BaSettingsCacheStore(kv()) { notifyChanged() }

    fun loadCalendarCache(serverIndex: Int): Pair<String, Long> = cacheStore().loadCalendarCache(serverIndex)

    fun saveCalendarCache(
        serverIndex: Int,
        encodedEntries: String,
        syncMs: Long,
    ) = cacheStore().saveCalendarCache(serverIndex, encodedEntries, syncMs)

    fun loadCalendarCacheVersion(serverIndex: Int): Int = cacheStore().loadCalendarCacheVersion(serverIndex)

    fun loadPoolCache(serverIndex: Int): Pair<String, Long> = cacheStore().loadPoolCache(serverIndex)

    fun savePoolCache(
        serverIndex: Int,
        encodedEntries: String,
        syncMs: Long,
    ) = cacheStore().savePoolCache(serverIndex, encodedEntries, syncMs)

    fun loadPoolCacheVersion(serverIndex: Int): Int = cacheStore().loadPoolCacheVersion(serverIndex)

    fun loadPoolShowEnded(): Boolean = kv().decodeBool(KEY_POOL_SHOW_ENDED, false)

    fun savePoolShowEnded(enabled: Boolean) {
        kv().encode(KEY_POOL_SHOW_ENDED, enabled)
        notifyChanged()
    }

    fun loadActivityShowEnded(): Boolean = kv().decodeBool(KEY_ACTIVITY_SHOW_ENDED, false)

    fun saveActivityShowEnded(enabled: Boolean) {
        kv().encode(KEY_ACTIVITY_SHOW_ENDED, enabled)
        notifyChanged()
    }

    fun loadCraftCardExpanded(): Boolean = kv().decodeBool(KEY_CRAFT_CARD_EXPANDED, true)

    /**
     * Deliberately silent: no [notifyChanged].
     *
     * The signal is what re-labels the daily-done tiles and shortcuts and what wakes the home
     * overview, and folding a card open changes none of that. The page keeps its own copy in
     * `BaOfficeRuntimeUiState`, so the write only has to survive process death.
     */
    fun saveCraftCardExpanded(expanded: Boolean) {
        kv().encode(KEY_CRAFT_CARD_EXPANDED, expanded)
    }

    fun loadShowCalendarPoolImages(): Boolean = kv().decodeBool(KEY_SHOW_CALENDAR_POOL_IMAGES, true)

    fun saveShowCalendarPoolImages(enabled: Boolean) {
        kv().encode(KEY_SHOW_CALENDAR_POOL_IMAGES, enabled)
        notifyChanged()
    }

    fun loadMediaAdaptiveRotationEnabled(): Boolean = kv().decodeBool(KEY_MEDIA_ADAPTIVE_ROTATION_ENABLED, true)

    fun saveMediaAdaptiveRotationEnabled(enabled: Boolean) {
        kv().encode(KEY_MEDIA_ADAPTIVE_ROTATION_ENABLED, enabled)
        notifyChanged()
    }

    fun loadMediaGuideVideoLoopEnabled(): Boolean = kv().decodeBool(KEY_MEDIA_GUIDE_VIDEO_LOOP_ENABLED, true)

    fun saveMediaGuideVideoLoopEnabled(enabled: Boolean) {
        kv().encode(KEY_MEDIA_GUIDE_VIDEO_LOOP_ENABLED, enabled)
        notifyChanged(notifyHomeOverview = false)
    }

    fun loadMediaSaveCustomEnabled(): Boolean = kv().decodeBool(KEY_MEDIA_SAVE_CUSTOM_ENABLED, false)

    fun saveMediaSaveCustomEnabled(enabled: Boolean) {
        kv().encode(KEY_MEDIA_SAVE_CUSTOM_ENABLED, enabled)
        notifyChanged()
    }

    fun loadMediaSaveFixedTreeUri(): String = kv().decodeString(KEY_MEDIA_SAVE_FIXED_TREE_URI, "").orEmpty().trim()

    fun saveMediaSaveFixedTreeUri(uri: String) {
        kv().encode(KEY_MEDIA_SAVE_FIXED_TREE_URI, uri.trim())
        notifyChanged()
    }

    fun loadNativeBgmMediaNotificationEnabled(): Boolean =
        BaNativeBgmMediaNotificationPrefs(MmkvBaSettingsKeyValueStore(kv())).loadEnabled()

    fun saveNativeBgmMediaNotificationEnabled(enabled: Boolean) {
        BaNativeBgmMediaNotificationPrefs(MmkvBaSettingsKeyValueStore(kv())).saveEnabled(enabled)
        notifyChanged()
    }

    fun loadCalendarRefreshIntervalHours(): Int {
        val raw =
            kv().decodeInt(
                KEY_CALENDAR_REFRESH_INTERVAL_HOURS,
                DEFAULT_CALENDAR_REFRESH_INTERVAL_HOURS,
            )
        return BaCalendarRefreshIntervalOption.fromHours(raw).hours
    }

    fun saveCalendarRefreshIntervalHours(hours: Int) {
        kv().encode(
            KEY_CALENDAR_REFRESH_INTERVAL_HOURS,
            BaCalendarRefreshIntervalOption.fromHours(hours).hours,
        )
        notifyChanged()
    }

    fun loadAccountState(): BaAccountStoreSnapshot {
        return migratedAccountStore().loadState()
    }

    fun buildAccountsSyncExportJson(nowMs: Long = System.currentTimeMillis()): String =
        buildBaAccountsExportJson(
            snapshot = loadAccountState(),
            nowMs = nowMs,
        )

    fun mergeAccountsSyncJson(raw: String) {
        val store = migratedAccountStore()
        val merged =
            mergeBaAccountsForSync(
                local = store.loadState(),
                remote = parseBaAccountsExportJson(raw),
            )
        store.replaceAll(
            accounts = merged.accounts,
            activeAccountId = merged.activeAccountId,
            activeAccountUpdatedAtMs = merged.activeAccountUpdatedAtMs,
        )
        store.saveAllAccountsFollowGlobalNotificationSettingsFromSync(
            merged.allAccountsFollowGlobalNotificationSettings,
            merged.allAccountsFollowGlobalNotificationSettingsUpdatedAtMs,
        )
        store.saveGlobalReminderSettingsFromSync(
            merged.globalReminderSettings,
            merged.globalReminderSettingsUpdatedAtMs,
        )
        notifyChanged()
    }

    fun countAccountsSyncJson(raw: String): Int = countBaAccountsExportJson(raw)

    fun replaceAccountsSyncJson(raw: String) {
        val payload = parseBaAccountsExportJson(raw)
        val store = migratedAccountStore()
        store.replaceAll(payload.accounts, payload.activeAccountId, payload.activeAccountUpdatedAtMs)
        store.saveAllAccountsFollowGlobalNotificationSettingsFromSync(
            payload.allAccountsFollowGlobalNotificationSettings,
            payload.allAccountsFollowGlobalNotificationSettingsUpdatedAtMs,
        )
        store.saveGlobalReminderSettingsFromSync(
            payload.globalReminderSettings, payload.globalReminderSettingsUpdatedAtMs,
        )
        notifyChanged()
    }

    fun selectActiveAccount(accountId: BaAccountId): Boolean {
        val selected = migratedAccountStore().selectActiveAccount(accountId)
        if (selected) {
            notifyChanged()
        }
        return selected
    }

    fun saveAllAccountsFollowGlobalNotificationSettings(enabled: Boolean) {
        migratedAccountStore().saveAllAccountsFollowGlobalNotificationSettings(enabled)
        notifyChanged()
    }

    fun saveGlobalReminderSettings(settings: BaGlobalReminderSettings) {
        migratedAccountStore().saveGlobalReminderSettings(settings)
        notifyChanged()
    }

    fun saveAccountEnabled(
        accountId: BaAccountId,
        enabled: Boolean,
    ): Boolean {
        val store = migratedAccountStore()
        val account = store.loadAccounts().firstOrNull { it.profile.id == accountId } ?: return false
        val updated = store.updateAccount(account.copy(profile = account.profile.copy(enabled = enabled)))
        if (updated) {
            notifyChanged()
        }
        return updated
    }

    fun addAccount(input: BaAccountProfileInput): BaAccountStoreSnapshot {
        val store = migratedAccountStore()
        val serverIndex = input.serverIndex.coerceIn(0, 2)
        val accountId = newManualAccountId(serverIndex)
        val nextNickname = sanitizeBaAccountNickname(input.nickname, serverIndex)
        val updatedAtMs = System.currentTimeMillis().coerceAtLeast(1L)
        val account =
            BaAccountRecord(
                profile =
                    BaAccountProfile(
                        id = accountId,
                        serverIndex = serverIndex,
                        displayName = sanitizeBaAccountDisplayName(input.displayName, nextNickname),
                        nickname = nextNickname,
                        friendCode = sanitizeBaAccountFriendCode(input.friendCode, serverIndex),
                        notificationMode = input.notificationMode,
                        remindersEnabled = input.remindersEnabled,
                        sortOrder = store.loadAccounts().size,
                    ),
                reminderOverride =
                    if (input.notificationMode == BaAccountNotificationMode.Custom) {
                        input.customReminderSettings.toAccountReminderOverride(accountId)
                    } else {
                        null
                    },
                profileUpdatedAtMs = updatedAtMs,
                runtimeUpdatedAtMs = updatedAtMs,
                reminderRuntimeUpdatedAtMs = updatedAtMs,
                reminderOverrideUpdatedAtMs = updatedAtMs,
            )
        store.addAccount(account)
        store.selectActiveAccount(account.profile.id)
        notifyChanged()
        return store.loadState()
    }

    fun updateAccountProfile(
        accountId: BaAccountId,
        input: BaAccountProfileInput,
    ): BaAccountStoreSnapshot {
        val store = migratedAccountStore()
        val account = store.loadAccounts().firstOrNull { it.profile.id == accountId }
        if (account != null) {
            val serverIndex = input.serverIndex.coerceIn(0, 2)
            val nextNickname = sanitizeBaAccountNickname(input.nickname, serverIndex)
            store.updateAccount(
                account.copy(
                    profile =
                        account.profile.copy(
                            serverIndex = serverIndex,
                            displayName = sanitizeBaAccountDisplayName(input.displayName, nextNickname),
                            nickname = nextNickname,
                            friendCode = sanitizeBaAccountFriendCode(input.friendCode, serverIndex),
                            notificationMode = input.notificationMode,
                            remindersEnabled = input.remindersEnabled,
                        ),
                    reminderOverride =
                        if (input.notificationMode == BaAccountNotificationMode.Custom) {
                            input.customReminderSettings.toAccountReminderOverride(accountId)
                        } else {
                            null
                        },
                ),
            )
            notifyChanged()
        }
        return store.loadState()
    }

    fun deleteAccount(accountId: BaAccountId): BaAccountStoreSnapshot {
        val store = migratedAccountStore()
        if (
            deleteBaAccountAndClearAcknowledgements(
                store,
                BaApAcknowledgementStore(accountKeyValueStore()),
                accountId,
            )
        ) {
            notifyChanged()
        }
        return store.loadState()
    }

    fun moveAccount(
        accountId: BaAccountId,
        offset: Int,
    ): BaAccountStoreSnapshot {
        val store = migratedAccountStore()
        if (store.moveAccount(accountId = accountId, offset = offset)) {
            notifyChanged()
        }
        return store.loadState()
    }

    fun migrateAccountsIfNeeded(): BaAccountMigrationResult {
        val keyValueStore = accountKeyValueStore()
        val store = accountStore(keyValueStore)
        return BaAccountMigration(
            accountStore = store,
            keyValueStore = keyValueStore,
        ).migrateLegacyIfNeeded()
    }

    private fun newManualAccountId(serverIndex: Int): BaAccountId =
        BaAccountId(
            value = "manual-${serverIndex.coerceIn(0, 2)}-${UUID.randomUUID()}",
        )

    fun loadSnapshot(): BaPageSnapshot =
        loadSnapshot(accountState = loadAccountState())

    fun loadSnapshot(accountState: BaAccountStoreSnapshot): BaPageSnapshot {
        val snapshot = loadBaSettingsSnapshot(kv()).withActiveBaAccount(accountState)
        val activeAccountId =
            accountState.accounts
                .firstOrNull { it.profile.id == accountState.activeAccountId }
                ?.profile
                ?.id
                ?: return snapshot
        return snapshot.withLocalApAcknowledgements(activeAccountId)
    }

    fun loadCalendarPoolSnapshot(): BaPageSnapshot =
        loadSnapshot().copy(serverIndex = loadCalendarPoolServerIndex())

    fun loadReminderSnapshots(): List<BaAccountReminderSnapshot> {
        return loadReminderSnapshots(includeDisabledAccounts = false)
    }

    private fun loadReminderSnapshots(includeDisabledAccounts: Boolean): List<BaAccountReminderSnapshot> {
        val accountState = loadAccountState()
        val baseSnapshot = loadBaSettingsSnapshot(kv())
        val acknowledgementRepository = apAcknowledgementRuntimeRepository()
        return accountState
            .accounts
            .filter { includeDisabledAccounts || it.profile.enabled }
            .map { account ->
                BaAccountReminderSnapshot(
                    accountId = account.profile.id,
                    displayName = account.profile.displayName,
                    snapshot =
                        baseSnapshot.withBaAccount(
                            accountState = accountState,
                            account = account,
                        ).let { snapshot ->
                            acknowledgementRepository.withLocalAcknowledgements(
                                snapshot = snapshot,
                                accountId = account.profile.id,
                            )
                        },
                )
            }
    }

    fun loadAccountApSuppressionAnchor(
        accountId: BaAccountId,
        kind: BaApReminderKind,
    ): Long =
        apAcknowledgementRuntimeRepository().loadSuppressionAnchor(accountId, kind)

    fun saveAccountApSuppressionAnchor(
        accountId: BaAccountId,
        kind: BaApReminderKind,
        anchorAtMs: Long,
    ): Boolean =
        apAcknowledgementRuntimeRepository()
            .saveSuppressionAnchor(accountId, kind, anchorAtMs)

    fun loadAccountApDismissedUntil(
        accountId: BaAccountId,
        kind: BaApReminderKind,
    ): Long =
        apAcknowledgementRuntimeRepository().loadDismissedUntil(accountId, kind)

    fun saveAccountApDismissedUntil(
        accountId: BaAccountId,
        kind: BaApReminderKind,
        dismissedUntilAtMs: Long,
    ): Boolean =
        apAcknowledgementRuntimeRepository()
            .saveDismissedUntil(accountId, kind, dismissedUntilAtMs)

    fun saveAccountApInteractionState(
        accountId: BaAccountId,
        kind: BaApReminderKind,
        suppressionAnchorAtMs: Long? = null,
        dismissedUntilAtMs: Long? = null,
    ): Boolean =
        apAcknowledgementRuntimeRepository()
            .saveInteractionState(
                accountId = accountId,
                kind = kind,
                suppressionAnchorAtMs = suppressionAnchorAtMs,
                dismissedUntilAtMs = dismissedUntilAtMs,
            )

    fun clearAccountApAcknowledgements(accountId: BaAccountId): Boolean =
        apAcknowledgementRuntimeRepository().clearAccount(accountId)

    fun reconcileApAcknowledgements(nowMs: Long): Boolean {
        val accountStore = migratedAccountStore()
        return apAcknowledgementRuntimeRepository().reconcile(
            accountStore = accountStore,
            baseSnapshot = loadBaSettingsSnapshot(kv()),
            nowMs = nowMs,
        )
    }

    internal fun reconcileApAcknowledgements(
        accountState: BaAccountStoreSnapshot,
        baseSnapshot: BaPageSnapshot,
        accountStore: BaAccountStore,
        acknowledgementStore: BaApAcknowledgementStore,
        nowMs: Long,
    ): Boolean {
        return reconcileBaApAcknowledgements(
            accountState = accountState,
            baseSnapshot = baseSnapshot,
            accountStore = accountStore,
            acknowledgementStore = acknowledgementStore,
            nowMs = nowMs,
        )
    }

    fun loadCalendarCacheSnapshot(serverIndex: Int): BaCacheSnapshot = cacheStore().loadCalendarCacheSnapshot(serverIndex)

    fun loadPoolCacheSnapshot(serverIndex: Int): BaCacheSnapshot = cacheStore().loadPoolCacheSnapshot(serverIndex)

    fun loadServerIndex(): Int {
        val accountState = loadAccountState()
        return accountState
            .accounts
            .firstOrNull { it.profile.id == accountState.activeAccountId }
            ?.profile
            ?.serverIndex
            ?: kv().decodeInt(KEY_SERVER_INDEX, DEFAULT_SERVER_INDEX).coerceIn(0, 2)
    }

    fun saveServerIndex(index: Int) {
        val normalized = index.coerceIn(0, 2)
        kv().encode(KEY_SERVER_INDEX, normalized)
        migratedAccountStore().updateActiveAccountProfile { profile ->
            profile.copy(serverIndex = normalized)
        }
        notifyChanged()
    }

    fun loadCalendarPoolServerIndex(): Int =
        BaCalendarPoolServerSelectionAccessor(accountKeyValueStore())
            .load(legacyServerIndex = loadServerIndex())

    fun loadCalendarPoolSyncServerIndices(): List<Int> =
        loadAccountState()
            .enabledServerIndices()
            .ifEmpty { listOf(loadCalendarPoolServerIndex()) }

    fun saveCalendarPoolServerIndex(index: Int) {
        BaCalendarPoolServerSelectionAccessor(accountKeyValueStore()).save(index)
        pruneCalendarPoolNotifiedKeysForCurrentPolicy()
        notifyChanged()
    }

    fun loadCafeLevel(): Int = kv().decodeInt(KEY_CAFE_LEVEL, DEFAULT_CAFE_LEVEL).coerceIn(1, 10)

    fun saveCafeLevel(level: Int) {
        val normalized = level.coerceIn(1, 10)
        kv().encode(KEY_CAFE_LEVEL, normalized)
        migratedAccountStore().updateActiveAccountRuntime { runtime ->
            runtime.copy(cafeLevel = normalized)
        }
        notifyChanged()
    }

    fun loadCafeStoredAp(): Double {
        val raw = kv().decodeString(KEY_CAFE_STORED_AP, DEFAULT_CAFE_STORED_AP.toString())
        return normalizeAp(raw?.toDoubleOrNull() ?: DEFAULT_CAFE_STORED_AP)
    }

    fun saveCafeStoredAp(
        storedAp: Double,
        notifyHomeOverview: Boolean = true,
    ) {
        saveBaRuntimeState(
            cafeStoredAp = storedAp,
            notifyHomeOverview = notifyHomeOverview,
        )
    }

    fun loadCafeLastHourMs(): Long = kv().decodeLong(KEY_CAFE_LAST_HOUR_MS, 0L)

    fun saveCafeLastHourMs(epochMs: Long) {
        saveBaRuntimeState(cafeLastHourMs = epochMs, notifyHomeOverview = false)
    }

    fun loadCafeApNotifyEnabled(): Boolean = kv().decodeBool(KEY_CAFE_AP_NOTIFY_ENABLED, false)

    fun saveCafeApNotifyEnabled(enabled: Boolean) {
        kv().encode(KEY_CAFE_AP_NOTIFY_ENABLED, enabled)
        notifyChanged()
    }

    fun loadCafeApNotifyThreshold(): Int =
        kv()
            .decodeInt(KEY_CAFE_AP_NOTIFY_THRESHOLD, DEFAULT_CAFE_AP_NOTIFY_THRESHOLD)
            .coerceIn(0, BA_AP_MAX)

    fun saveCafeApNotifyThreshold(threshold: Int) {
        kv().encode(KEY_CAFE_AP_NOTIFY_THRESHOLD, threshold.coerceIn(0, BA_AP_MAX))
        notifyChanged()
    }

    fun loadCafeApLastNotifiedLevel(): Int = kv().decodeInt(KEY_CAFE_AP_LAST_NOTIFIED_LEVEL, -1).coerceIn(-1, BA_AP_MAX)

    fun saveCafeApLastNotifiedLevel(level: Int) {
        val normalized = level.coerceIn(-1, BA_AP_MAX)
        kv().encode(KEY_CAFE_AP_LAST_NOTIFIED_LEVEL, normalized)
        migratedAccountStore().updateActiveAccountReminderRuntime { runtime ->
            runtime.copy(cafeApLastNotifiedLevel = normalized)
        }
        notifyChanged(notifyHomeOverview = false)
    }

    fun loadIdNickname(serverIndex: Int? = null): String = idSettings().loadNickname(serverIndex)

    fun saveIdNickname(
        name: String,
        serverIndex: Int? = null,
    ) {
        idSettings().saveNickname(name, serverIndex)
        updateActiveAccountIdentity(
            serverIndex = serverIndex,
            nickname = name,
            friendCode = null,
        )
        notifyChanged()
    }

    fun loadIdFriendCode(serverIndex: Int? = null): String = idSettings().loadFriendCode(serverIndex)

    fun saveIdFriendCode(
        code: String,
        serverIndex: Int? = null,
    ) {
        idSettings().saveFriendCode(code, serverIndex)
        updateActiveAccountIdentity(
            serverIndex = serverIndex,
            nickname = null,
            friendCode = code,
        )
        notifyChanged()
    }

    fun loadApLimit(): Int =
        kv().decodeInt(KEY_AP_LIMIT, DEFAULT_AP_LIMIT).coerceIn(
            0,
            BA_AP_LIMIT_MAX,
        )

    fun saveApLimit(limit: Int) {
        val normalized = limit.coerceIn(0, BA_AP_LIMIT_MAX)
        kv().encode(KEY_AP_LIMIT, normalized)
        migratedAccountStore().updateActiveAccountRuntime { runtime ->
            runtime.copy(apLimit = normalized)
        }
        notifyChanged()
    }

    fun loadApNotifyEnabled(): Boolean = kv().decodeBool(KEY_AP_NOTIFY_ENABLED, false)

    fun saveApNotifyEnabled(enabled: Boolean) {
        kv().encode(KEY_AP_NOTIFY_ENABLED, enabled)
        notifyChanged()
    }

    fun loadApNotifyThreshold(): Int = kv().decodeInt(KEY_AP_NOTIFY_THRESHOLD, DEFAULT_AP_NOTIFY_THRESHOLD).coerceIn(0, BA_AP_MAX)

    fun saveApNotifyThreshold(threshold: Int) {
        kv().encode(KEY_AP_NOTIFY_THRESHOLD, threshold.coerceIn(0, BA_AP_MAX))
        notifyChanged()
    }

    fun loadApLastNotifiedLevel(): Int = kv().decodeInt(KEY_AP_LAST_NOTIFIED_LEVEL, -1).coerceIn(-1, BA_AP_MAX)

    fun saveApLastNotifiedLevel(level: Int) {
        val normalized = level.coerceIn(-1, BA_AP_MAX)
        kv().encode(KEY_AP_LAST_NOTIFIED_LEVEL, normalized)
        migratedAccountStore().updateActiveAccountReminderRuntime { runtime ->
            runtime.copy(apLastNotifiedLevel = normalized)
        }
        notifyChanged(notifyHomeOverview = false)
    }

    fun loadArenaRefreshNotifyEnabled(): Boolean = kv().decodeBool(KEY_ARENA_REFRESH_NOTIFY_ENABLED, false)

    fun saveArenaRefreshNotifyEnabled(enabled: Boolean) {
        kv().encode(KEY_ARENA_REFRESH_NOTIFY_ENABLED, enabled)
        notifyChanged()
    }

    fun loadArenaRefreshLastNotifiedSlotMs(): Long = kv().decodeLong(KEY_ARENA_REFRESH_LAST_NOTIFIED_SLOT_MS, 0L).coerceAtLeast(0L)

    fun saveArenaRefreshLastNotifiedSlotMs(slotMs: Long) {
        val normalized = slotMs.coerceAtLeast(0L)
        kv().encode(KEY_ARENA_REFRESH_LAST_NOTIFIED_SLOT_MS, normalized)
        migratedAccountStore().updateActiveAccountReminderRuntime { runtime ->
            runtime.copy(arenaRefreshLastNotifiedSlotMs = normalized)
        }
        notifyChanged()
    }

    fun loadCafeVisitNotifyEnabled(): Boolean = kv().decodeBool(KEY_CAFE_VISIT_NOTIFY_ENABLED, false)

    fun saveCafeVisitNotifyEnabled(enabled: Boolean) {
        kv().encode(KEY_CAFE_VISIT_NOTIFY_ENABLED, enabled)
        notifyChanged()
    }

    fun loadCafeVisitLastNotifiedSlotMs(): Long = kv().decodeLong(KEY_CAFE_VISIT_LAST_NOTIFIED_SLOT_MS, 0L).coerceAtLeast(0L)

    fun saveCafeVisitLastNotifiedSlotMs(slotMs: Long) {
        val normalized = slotMs.coerceAtLeast(0L)
        kv().encode(KEY_CAFE_VISIT_LAST_NOTIFIED_SLOT_MS, normalized)
        migratedAccountStore().updateActiveAccountReminderRuntime { runtime ->
            runtime.copy(cafeVisitLastNotifiedSlotMs = normalized)
        }
        notifyChanged()
    }

    fun resetReminderRuntimeForAccounts(accountIds: List<BaAccountId>) {
        val store = migratedAccountStore()
        accountIds.forEach { accountId ->
            store.updateAccountReminderRuntime(accountId) {
                BaAccountReminderRuntime()
            }
        }
        notifyChanged(notifyHomeOverview = false)
    }

    fun loadCalendarUpcomingNotifyEnabled(): Boolean = kv().decodeBool(KEY_CALENDAR_UPCOMING_NOTIFY_ENABLED, false)

    fun saveCalendarUpcomingNotifyEnabled(enabled: Boolean) {
        kv().encode(KEY_CALENDAR_UPCOMING_NOTIFY_ENABLED, enabled)
        notifyChanged()
    }

    fun loadCalendarEndingNotifyEnabled(): Boolean = kv().decodeBool(KEY_CALENDAR_ENDING_NOTIFY_ENABLED, false)

    fun saveCalendarEndingNotifyEnabled(enabled: Boolean) {
        kv().encode(KEY_CALENDAR_ENDING_NOTIFY_ENABLED, enabled)
        notifyChanged()
    }

    fun loadPoolUpcomingNotifyEnabled(): Boolean = kv().decodeBool(KEY_POOL_UPCOMING_NOTIFY_ENABLED, false)

    fun savePoolUpcomingNotifyEnabled(enabled: Boolean) {
        kv().encode(KEY_POOL_UPCOMING_NOTIFY_ENABLED, enabled)
        notifyChanged()
    }

    fun loadPoolEndingNotifyEnabled(): Boolean = kv().decodeBool(KEY_POOL_ENDING_NOTIFY_ENABLED, false)

    fun savePoolEndingNotifyEnabled(enabled: Boolean) {
        kv().encode(KEY_POOL_ENDING_NOTIFY_ENABLED, enabled)
        notifyChanged()
    }

    fun loadCalendarPoolChangeNotifyEnabled(): Boolean = kv().decodeBool(KEY_CALENDAR_POOL_CHANGE_NOTIFY_ENABLED, false)

    fun saveCalendarPoolChangeNotifyEnabled(enabled: Boolean) {
        kv().encode(KEY_CALENDAR_POOL_CHANGE_NOTIFY_ENABLED, enabled)
        notifyChanged()
    }

    fun loadCalendarPoolNotifyLeadHours(): Int =
        BaCalendarPoolNotifyLeadOption
            .fromHours(
                kv().decodeInt(KEY_CALENDAR_POOL_NOTIFY_LEAD_HOURS, 24),
            ).hours

    fun saveCalendarPoolNotifyLeadHours(hours: Int) {
        kv().encode(
            KEY_CALENDAR_POOL_NOTIFY_LEAD_HOURS,
            BaCalendarPoolNotifyLeadOption.fromHours(hours).hours,
        )
        notifyChanged()
    }

    fun loadCalendarPoolNotifiedKeys(): Set<String> =
        kv()
            .decodeString(KEY_CALENDAR_POOL_NOTIFIED_KEYS, "")
            .orEmpty()
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()

    fun markCalendarPoolNotified(key: String) {
        val normalized = key.trim()
        if (normalized.isBlank()) return
        val keys = (loadCalendarPoolNotifiedKeys() + normalized).toList().takeLast(500)
        kv().encode(KEY_CALENDAR_POOL_NOTIFIED_KEYS, keys.joinToString(separator = "\n"))
        notifyChanged()
    }

    fun replaceCalendarPoolNotifiedKeys(keys: Set<String>) {
        val normalized =
            keys
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .takeLast(500)
        if (normalized.isEmpty()) {
            kv().removeValueForKey(KEY_CALENDAR_POOL_NOTIFIED_KEYS)
            notifyChanged()
            return
        }
        kv().encode(KEY_CALENDAR_POOL_NOTIFIED_KEYS, normalized.joinToString(separator = "\n"))
        notifyChanged()
    }

    fun pruneCalendarPoolNotifiedKeysForCurrentPolicy() {
        val snapshot = loadSnapshot()
        pruneCalendarPoolNotifiedKeysForPolicy(
            serverIndex = loadCalendarPoolServerIndex(),
            leadHours = snapshot.calendarPoolNotifyLeadHours,
            calendarUpcomingEnabled = snapshot.calendarUpcomingNotifyEnabled,
            calendarEndingEnabled = snapshot.calendarEndingNotifyEnabled,
            poolUpcomingEnabled = snapshot.poolUpcomingNotifyEnabled,
            poolEndingEnabled = snapshot.poolEndingNotifyEnabled,
            calendarPoolChangeEnabled = snapshot.calendarPoolChangeNotifyEnabled,
        )
    }

    fun pruneCalendarPoolNotifiedKeysForPolicy(
        serverIndex: Int,
        leadHours: Int,
        calendarUpcomingEnabled: Boolean,
        calendarEndingEnabled: Boolean,
        poolUpcomingEnabled: Boolean,
        poolEndingEnabled: Boolean,
        calendarPoolChangeEnabled: Boolean,
    ) {
        val current = loadCalendarPoolNotifiedKeys()
        val retained =
            BaReminderCoordinator.retainNotifiedKeysForPolicy(
                keys = current,
                serverIndex = serverIndex,
                leadHours = leadHours,
                calendarUpcomingEnabled = calendarUpcomingEnabled,
                calendarEndingEnabled = calendarEndingEnabled,
                poolUpcomingEnabled = poolUpcomingEnabled,
                poolEndingEnabled = poolEndingEnabled,
                calendarPoolChangeEnabled = calendarPoolChangeEnabled,
            )
        if (retained != current) {
            replaceCalendarPoolNotifiedKeys(retained)
        }
    }

    fun loadApCurrent(): Double {
        val store = kv()
        val value =
            if (store.containsKey(KEY_AP_CURRENT_EXACT)) {
                store.decodeString(KEY_AP_CURRENT_EXACT, DEFAULT_AP_CURRENT.toString())?.toDoubleOrNull() ?: DEFAULT_AP_CURRENT
            } else {
                store.decodeInt(KEY_AP_CURRENT, DEFAULT_AP_CURRENT.toInt()).toDouble()
            }
        return normalizeAp(value.coerceIn(0.0, BA_AP_MAX.toDouble()))
    }

    fun saveApCurrent(
        current: Double,
        notifyHomeOverview: Boolean = true,
    ) {
        saveBaRuntimeState(
            apCurrent = current,
            notifyHomeOverview = notifyHomeOverview,
        )
    }

    fun loadApRegenBaseMs(): Long = kv().decodeLong(KEY_AP_REGEN_BASE_MS, 0L)

    fun saveApRegenBaseMs(epochMs: Long) {
        saveBaRuntimeState(apRegenBaseMs = epochMs, notifyHomeOverview = false)
    }

    fun loadApSyncMs(): Long = kv().decodeLong(KEY_AP_SYNC_MS, 0L)

    fun saveApSyncMs(epochMs: Long) {
        saveBaRuntimeState(apSyncMs = epochMs, notifyHomeOverview = false)
    }

    fun saveBaRuntimeState(
        apCurrent: Double? = null,
        apRegenBaseMs: Long? = null,
        apSyncMs: Long? = null,
        cafeStoredAp: Double? = null,
        cafeLastHourMs: Long? = null,
        notifyHomeOverview: Boolean = true,
    ) {
        val store = kv()
        apCurrent?.let { current ->
            val normalized = normalizeAp(current)
            store.encode(KEY_AP_CURRENT_EXACT, normalized.toString())
            store.encode(KEY_AP_CURRENT, displayAp(normalized))
        }
        apRegenBaseMs?.let { epochMs ->
            store.encode(KEY_AP_REGEN_BASE_MS, epochMs.coerceAtLeast(0L))
        }
        apSyncMs?.let { epochMs ->
            store.encode(KEY_AP_SYNC_MS, epochMs.coerceAtLeast(0L))
        }
        cafeStoredAp?.let { storedAp ->
            store.encode(KEY_CAFE_STORED_AP, normalizeAp(storedAp).toString())
        }
        cafeLastHourMs?.let { epochMs ->
            store.encode(KEY_CAFE_LAST_HOUR_MS, floorToHourMs(epochMs.coerceAtLeast(0L)))
        }
        migratedAccountStore().updateActiveAccountRuntime { runtime ->
            runtime.copy(
                apCurrent = apCurrent?.let(::normalizeAp) ?: runtime.apCurrent,
                apRegenBaseMs = apRegenBaseMs?.coerceAtLeast(0L) ?: runtime.apRegenBaseMs,
                apSyncMs = apSyncMs?.coerceAtLeast(0L) ?: runtime.apSyncMs,
                cafeStoredAp = cafeStoredAp?.let(::normalizeAp) ?: runtime.cafeStoredAp,
                cafeLastHourMs =
                    cafeLastHourMs
                        ?.coerceAtLeast(0L)
                        ?.let(::floorToHourMs)
                        ?: runtime.cafeLastHourMs,
            )
        }
        notifyChanged(notifyHomeOverview = notifyHomeOverview)
    }

    fun saveAccountBaRuntimeState(
        accountId: BaAccountId,
        apCurrent: Double? = null,
        apRegenBaseMs: Long? = null,
        apSyncMs: Long? = null,
        cafeStoredAp: Double? = null,
        cafeLastHourMs: Long? = null,
        notifyHomeOverview: Boolean = false,
    ) {
        migratedAccountStore().updateAccountRuntime(accountId) { runtime ->
            runtime.copy(
                apCurrent = apCurrent?.let(::normalizeAp) ?: runtime.apCurrent,
                apRegenBaseMs = apRegenBaseMs?.coerceAtLeast(0L) ?: runtime.apRegenBaseMs,
                apSyncMs = apSyncMs?.coerceAtLeast(0L) ?: runtime.apSyncMs,
                cafeStoredAp = cafeStoredAp?.let(::normalizeAp) ?: runtime.cafeStoredAp,
                cafeLastHourMs =
                    cafeLastHourMs
                        ?.coerceAtLeast(0L)
                        ?.let(::floorToHourMs)
                        ?: runtime.cafeLastHourMs,
            )
        }
        notifyChanged(notifyHomeOverview = notifyHomeOverview)
    }

    fun loadDailyTileState(): BaDailyTileState {
        val raw = kv().decodeString(KEY_BA_DAILY_TILE_STATE, "").orEmpty()
        if (raw.isBlank()) return BaDailyTileState().normalized()
        return runCatching { KeiJson.lenient.decodeFromString<BaDailyTileState>(raw) }
            .getOrElse { BaDailyTileState() }
            .normalized()
    }

    fun saveDailyTileState(state: BaDailyTileState) {
        kv().encode(KEY_BA_DAILY_TILE_STATE, KeiJson.lenient.encodeToString(state.normalized()))
        notifyChanged(notifyHomeOverview = false)
    }

    /**
     * The daily-done template, shared by every trigger.
     *
     * One record for all of them rather than one per tile: the tile, the launcher shortcut and the MCP
     * tool are three ways to say the same sentence, and a per-trigger copy would make "my dailies" mean
     * something different depending on which one the teacher reached for. It is also not per account, so
     * the all-accounts tile has one answer to apply — the price is that a teacher whose accounts end the
     * day at different AP has to pick a number, which the long-press editor makes a two-tap correction.
     *
     * A malformed value decodes to the defaults, which are the pre-configurable template, so a corrupt
     * record degrades to the behaviour the tile used to have rather than to nothing at all.
     */
    fun loadDailyDoneConfig(): BaDailyDoneConfig {
        val raw = kv().decodeString(KEY_BA_DAILY_DONE_CONFIG, "").orEmpty()
        if (raw.isBlank()) return BaDailyDoneConfig().normalized()
        return runCatching { KeiJson.lenient.decodeFromString<BaDailyDoneConfig>(raw) }
            .getOrElse { BaDailyDoneConfig() }
            .normalized()
    }

    fun saveDailyDoneConfig(config: BaDailyDoneConfig) {
        kv().encode(KEY_BA_DAILY_DONE_CONFIG, KeiJson.lenient.encodeToString(config.normalized()))
        notifyChanged(notifyHomeOverview = false)
    }

    /**
     * Applies a whole daily-done template to one account.
     *
     * One function rather than a chain of the per-field savers above, for two reasons. Correctness
     * first: [BaOfficeCooldownPersistenceUpdate] and the cooldown savers it drives are active-account
     * only and carry no id, so composing a per-account template out of them would silently write
     * somebody else's cooldowns. Cost second: each saver is its own whole-account-list JSON re-encode
     * plus its own change signal, so the field-by-field route would be five of each per account.
     *
     * Still two record updates, because game state and reminder state are deliberately separate —
     * `runtimeUpdatedAtMs` arbitrates WebDAV merge and the notified levels must not touch it. [notify]
     * lets an all-accounts pass fold every account into a single signal at the end.
     */
    fun saveAccountDailyDone(
        accountId: BaAccountId,
        plan: BaDailyDonePlan,
        notify: Boolean = true,
    ) {
        val store = migratedAccountStore()
        store.updateAccountRuntime(accountId) { runtime ->
            runtime.copy(
                apCurrent = plan.apCurrent,
                apRegenBaseMs = plan.apRegenBaseMs,
                apSyncMs = plan.apSyncMs,
                cafeStoredAp = plan.cafeStoredAp,
                cafeLastHourMs = plan.cafeLastHourMs,
                coffeeHeadpatMs = plan.coffeeHeadpatMs,
                coffeeInvite1UsedMs = plan.coffeeInvite1UsedMs,
                coffeeInvite2UsedMs = plan.coffeeInvite2UsedMs,
                craft = plan.craft,
            )
        }
        store.updateAccountReminderRuntime(accountId) { runtime ->
            runtime.copy(
                apLastNotifiedLevel = plan.apLastNotifiedLevel,
                cafeApLastNotifiedLevel = plan.cafeApLastNotifiedLevel,
            )
        }
        if (notify) notifyChanged(notifyHomeOverview = true)
    }

    /**
     * Runs the daily-done template across every enabled account, in one signal.
     *
     * The template itself is [loadDailyDoneConfig], not a parameter: every trigger applies the teacher's
     * one template, and the long-press editor persists before it applies. Returns the per-account
     * outcomes so the caller can say what actually happened rather than claiming success. An account
     * whose dailies were already done contributes an outcome with `changedAnything == false`.
     */
    fun applyDailyDone(
        accountIds: List<BaAccountId>? = null,
        nowMs: Long = System.currentTimeMillis(),
    ): Map<BaAccountId, BaDailyDoneOutcome> = runDailyDone(accountIds = accountIds, nowMs = nowMs, persist = true)

    /**
     * What [applyDailyDone] would do, without writing anything.
     *
     * Shares [runDailyDone] with the real thing rather than re-deriving, so a preview cannot promise an
     * outcome the write would not produce. Used by the MCP tool, where a bare call has to be safe.
     */
    fun previewDailyDone(
        accountIds: List<BaAccountId>? = null,
        nowMs: Long = System.currentTimeMillis(),
    ): Map<BaAccountId, BaDailyDoneOutcome> = runDailyDone(accountIds = accountIds, nowMs = nowMs, persist = false)

    /** Enabled accounts the template would touch, in list order. Empty when nothing matches. */
    fun dailyDoneTargets(accountIds: List<BaAccountId>? = null): List<BaAccountRecord> =
        loadAccountState()
            .accounts
            .filter { it.profile.enabled }
            .filter { accountIds == null || it.profile.id in accountIds }

    private fun runDailyDone(
        accountIds: List<BaAccountId>?,
        nowMs: Long,
        persist: Boolean,
    ): Map<BaAccountId, BaDailyDoneOutcome> {
        val accountState = loadAccountState()
        val targets =
            accountState.accounts
                .filter { it.profile.enabled }
                .filter { accountIds == null || it.profile.id in accountIds }
        if (targets.isEmpty()) return emptyMap()
        // Read once for the whole pass: an all-accounts run must apply one template, and re-reading per
        // account would let a concurrent edit split the run across two of them.
        val config = loadDailyDoneConfig()
        val outcomes = LinkedHashMap<BaAccountId, BaDailyDoneOutcome>(targets.size)
        targets.forEach { account ->
            val snapshot = BaPageSnapshot().withBaAccount(accountState = accountState, account = account)
            val plan = planBaDailyDone(snapshot = snapshot, config = config, nowMs = nowMs)
            if (persist) saveAccountDailyDone(accountId = account.profile.id, plan = plan, notify = false)
            outcomes[account.profile.id] = plan.outcome
        }
        if (persist) notifyChanged(notifyHomeOverview = true)
        return outcomes
    }

    /**
     * A full snapshot for one account, globals included.
     *
     * [loadSnapshot] answers only for the active account. Building the difference by hand is the trap
     * this exists to close: `BaPageSnapshot().withBaAccount(...)` looks complete but leaves every global
     * preference — the display toggles, the refresh interval, the craft card's expansion — at its
     * compiled-in default, so a reader would report defaults as if they were the teacher's settings.
     */
    fun loadSnapshotForAccount(accountId: BaAccountId): BaPageSnapshot? {
        val accountState = loadAccountState()
        val account = accountState.accounts.firstOrNull { it.profile.id == accountId } ?: return null
        return loadBaSettingsSnapshot(kv())
            .withBaAccount(accountState = accountState, account = account)
            .withLocalApAcknowledgements(account.profile.id)
    }

    fun saveAccountApLastNotifiedLevel(
        accountId: BaAccountId,
        level: Int,
    ) {
        val normalized = level.coerceIn(-1, BA_AP_MAX)
        migratedAccountStore().updateAccountReminderRuntime(accountId) { runtime ->
            runtime.copy(apLastNotifiedLevel = normalized)
        }
        notifyChanged(notifyHomeOverview = false)
    }

    fun saveAccountCafeApLastNotifiedLevel(
        accountId: BaAccountId,
        level: Int,
    ) {
        val normalized = level.coerceIn(-1, BA_AP_MAX)
        migratedAccountStore().updateAccountReminderRuntime(accountId) { runtime ->
            runtime.copy(cafeApLastNotifiedLevel = normalized)
        }
        notifyChanged(notifyHomeOverview = false)
    }

    fun saveAccountArenaRefreshLastNotifiedSlotMs(
        accountId: BaAccountId,
        slotMs: Long,
    ) {
        val normalized = slotMs.coerceAtLeast(0L)
        migratedAccountStore().updateAccountReminderRuntime(accountId) { runtime ->
            runtime.copy(arenaRefreshLastNotifiedSlotMs = normalized)
        }
        notifyChanged(notifyHomeOverview = false)
    }

    fun saveAccountCafeVisitLastNotifiedSlotMs(
        accountId: BaAccountId,
        slotMs: Long,
    ) {
        val normalized = slotMs.coerceAtLeast(0L)
        migratedAccountStore().updateAccountReminderRuntime(accountId) { runtime ->
            runtime.copy(cafeVisitLastNotifiedSlotMs = normalized)
        }
        notifyChanged(notifyHomeOverview = false)
    }

    /**
     * Marks the given craft completions as announced, in one write.
     *
     * Batched on purpose. Each [os.kei.ui.page.main.ba.support.BaAccountStore.updateAccountReminderRuntime]
     * ends in a whole-account-list JSON re-encode, and one sweep can complete up to
     * `2 * BA_CRAFT_SLOT_COUNT` slots for a single account — six re-encodes inside the receiver's 12s
     * budget. Folding them keeps that at one without giving up per-notification precision: the caller
     * still passes only the completions whose notification actually posted.
     *
     * [BaCraftCompletion.endAtMs] goes in unmodified. Dedup is an exact inequality against the marker,
     * so any rounding here would make the same completion re-fire on every tick forever.
     */
    fun saveAccountCraftNotifiedMarkers(
        accountId: BaAccountId,
        completions: List<BaCraftCompletion>,
    ) {
        if (completions.isEmpty()) return
        migratedAccountStore().updateAccountReminderRuntime(accountId) { runtime ->
            runtime.copy(
                craftNotified =
                    completions.fold(runtime.craftNotified) { markers, completion ->
                        markers.withMarkerAt(
                            function = completion.function,
                            index = completion.index,
                            endAtMs = completion.endAtMs,
                        )
                    },
            )
        }
        notifyChanged(notifyHomeOverview = false)
    }

    fun loadCoffeeHeadpatMs(): Long = kv().decodeLong(KEY_COFFEE_HEADPAT_MS, 0L)

    fun saveCoffeeHeadpatMs(epochMs: Long) {
        val normalized = epochMs.coerceAtLeast(0L)
        kv().encode(KEY_COFFEE_HEADPAT_MS, normalized)
        migratedAccountStore().updateActiveAccountRuntime { runtime ->
            runtime.copy(coffeeHeadpatMs = normalized)
        }
        notifyChanged(notifyHomeOverview = false)
    }

    fun loadCoffeeInvite1UsedMs(): Long = kv().decodeLong(KEY_COFFEE_INVITE1_USED_MS, 0L)

    fun saveCoffeeInvite1UsedMs(epochMs: Long) {
        val normalized = epochMs.coerceAtLeast(0L)
        kv().encode(KEY_COFFEE_INVITE1_USED_MS, normalized)
        migratedAccountStore().updateActiveAccountRuntime { runtime ->
            runtime.copy(coffeeInvite1UsedMs = normalized)
        }
        notifyChanged(notifyHomeOverview = false)
    }

    fun loadCoffeeInvite2UsedMs(): Long = kv().decodeLong(KEY_COFFEE_INVITE2_USED_MS, 0L)

    fun saveCoffeeInvite2UsedMs(epochMs: Long) {
        val normalized = epochMs.coerceAtLeast(0L)
        kv().encode(KEY_COFFEE_INVITE2_USED_MS, normalized)
        migratedAccountStore().updateActiveAccountRuntime { runtime ->
            runtime.copy(coffeeInvite2UsedMs = normalized)
        }
        notifyChanged(notifyHomeOverview = false)
    }

    /**
     * Persists one account's craft slots.
     *
     * No `kv().encode` legacy mirror, unlike the coffee cooldowns above: craft has never had a global
     * key and [BaSettingsSnapshotLoader] has no line for it — it reaches a snapshot only through the
     * account record, via [withBaAccount]. Writing a global mirror would put the active account's
     * slots on every other account.
     *
     * Goes through `updateAccountRuntime`, not `updateAccountReminderRuntime`: these are game state and
     * belong to `runtimeUpdatedAtMs`. Only the announced-markers write uses the reminder runtime.
     */
    fun saveCraft(
        accountId: BaAccountId?,
        craft: BaCraftState,
    ) {
        val normalized = craft.normalized()
        val store = migratedAccountStore()
        if (accountId != null) {
            store.updateAccountRuntime(accountId) { runtime -> runtime.copy(craft = normalized) }
        } else {
            store.updateActiveAccountRuntime { runtime -> runtime.copy(craft = normalized) }
        }
        notifyChanged(notifyHomeOverview = false)
    }

    fun clearCalendarAndPoolCaches() = cacheStore().clearCalendarAndPoolCaches()

    fun storageFootprintBytes(): Long = kv().totalSize()

    fun actualDataBytes(): Long = kv().actualSize()

    fun cacheBytesEstimated(): Long = cacheStore().cacheBytesEstimated()

    fun configBytesEstimated(): Long {
        val snapshot = loadSnapshot()
        val accountBytes =
            loadAccountState().accounts.sumOf { account ->
                listOf(
                    account.profile.id.value,
                    account.profile.displayName,
                    account.profile.nickname,
                    account.profile.friendCode,
                ).sumOf { it.length.toLong() * 2 } + 120L
            }
        return accountBytes +
            listOf(
                snapshot.idNickname,
                snapshot.idFriendCode,
            ).sumOf { it.length.toLong() * 2 } + 160L
    }

    fun clearListScrollState() {
        val store = kv()
        store.removeValueForKey(KEY_LIST_SCROLL_INDEX)
        store.removeValueForKey(KEY_LIST_SCROLL_OFFSET)
    }

    private fun updateActiveAccountIdentity(
        serverIndex: Int?,
        nickname: String?,
        friendCode: String?,
    ) {
        migratedAccountStore().updateActiveAccountProfile { profile ->
            if (serverIndex != null && serverIndex.coerceIn(0, 2) != profile.serverIndex) {
                return@updateActiveAccountProfile profile
            }
            val normalizedServerIndex = profile.serverIndex.coerceIn(0, 2)
            val nextNickname =
                nickname?.let { sanitizeBaAccountNickname(it, normalizedServerIndex) } ?: profile.nickname
            val nextFriendCode =
                friendCode?.let { sanitizeBaAccountFriendCode(it, normalizedServerIndex) } ?: profile.friendCode
            profile.copy(
                displayName =
                    if (nickname != null && profile.displayName == profile.nickname) {
                        nextNickname
                    } else {
                        profile.displayName
                    },
                nickname = nextNickname,
                friendCode = nextFriendCode,
            )
        }
    }
}
