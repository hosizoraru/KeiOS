package os.kei.ui.page.main.ba.support

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import os.kei.core.json.KeiJson

private const val BA_ACCOUNTS_TRANSFER_VERSION = 1

@Serializable
internal data class BaAccountsTransferPayload(
    val version: Int = BA_ACCOUNTS_TRANSFER_VERSION,
    val exportedAtMs: Long = 0L,
    val accounts: List<BaAccountRecord> = emptyList(),
    val activeAccountId: BaAccountId? = null,
    val allAccountsFollowGlobalNotificationSettings: Boolean = true,
    val globalReminderSettings: BaGlobalReminderSettings = BaGlobalReminderSettings(),
    val activeAccountUpdatedAtMs: Long = 0L,
    val allAccountsFollowGlobalNotificationSettingsUpdatedAtMs: Long = 0L,
    val globalReminderSettingsUpdatedAtMs: Long = 0L,
)

internal fun buildBaAccountsExportJson(
    snapshot: BaAccountStoreSnapshot,
    nowMs: Long = System.currentTimeMillis(),
): String =
    KeiJson.pretty.encodeToString(
        BaAccountsTransferPayload(
            version = BA_ACCOUNTS_TRANSFER_VERSION,
            exportedAtMs = nowMs.coerceAtLeast(0L),
            accounts = snapshot.accounts,
            activeAccountId = snapshot.activeAccountId,
            allAccountsFollowGlobalNotificationSettings =
                snapshot.allAccountsFollowGlobalNotificationSettings,
            globalReminderSettings = snapshot.globalReminderSettings,
            activeAccountUpdatedAtMs = snapshot.activeAccountUpdatedAtMs,
            allAccountsFollowGlobalNotificationSettingsUpdatedAtMs =
                snapshot.allAccountsFollowGlobalNotificationSettingsUpdatedAtMs,
            globalReminderSettingsUpdatedAtMs = snapshot.globalReminderSettingsUpdatedAtMs,
        ).normalized(),
    )

internal fun parseBaAccountsExportJson(raw: String): BaAccountsTransferPayload =
    KeiJson.lenient
        .decodeFromString<BaAccountsTransferPayload>(raw)
        .normalized()

internal fun countBaAccountsExportJson(raw: String): Int =
    parseBaAccountsExportJson(raw).accounts.size

internal fun buildBaAccountsSyncFingerprintJson(raw: String): String {
    val payload = parseBaAccountsExportJson(raw)
    // Match replaceAll's active-account repair without changing timestamp-based merge policy.
    val active = payload.activeAccountId ?: payload.accounts.firstOrNull()?.profile?.id
    return KeiJson.pretty.encodeToString(
        payload.copy(
            exportedAtMs = 0L,
            activeAccountId = active,
            activeAccountUpdatedAtMs = if (active == null) 0L else payload.activeAccountUpdatedAtMs,
        ),
    )
}

internal fun mergeBaAccountsForSync(
    local: BaAccountStoreSnapshot,
    remote: BaAccountsTransferPayload,
    nowMs: Long = System.currentTimeMillis(),
): BaAccountsTransferPayload {
    val localAccounts = local.accounts.normalizedAccounts()
    val remotePayload = remote.normalized()
    val localById = localAccounts.associateBy { it.profile.id.value }
    val remoteById = remotePayload.accounts.associateBy { it.profile.id.value }
    val localOrderUpdatedAtMs = localAccounts.maxOfOrNull { it.profileUpdatedAtMs } ?: 0L
    val remoteOrderUpdatedAtMs = remotePayload.accounts.maxOfOrNull { it.profileUpdatedAtMs } ?: 0L
    val primaryOrder =
        if (remoteOrderUpdatedAtMs > localOrderUpdatedAtMs) {
            remotePayload.accounts.map { it.profile.id.value }
        } else {
            localAccounts.map { it.profile.id.value }
        }
    val orderedIds =
        (primaryOrder + localAccounts.map { it.profile.id.value } + remotePayload.accounts.map { it.profile.id.value })
            .distinct()
    val mergedAccounts =
        orderedIds
            .mapNotNull { id -> mergeBaAccountRecord(localById[id], remoteById[id]) }
            .mapIndexed { index, account ->
                account.copy(profile = account.profile.copy(sortOrder = index))
            }
            .normalizedAccounts()
    val activeAccountId =
        chooseActiveAccountId(
            mergedAccounts = mergedAccounts,
            localActiveAccountId = local.activeAccountId,
            localUpdatedAtMs = local.activeAccountUpdatedAtMs,
            remoteActiveAccountId = remotePayload.activeAccountId,
            remoteUpdatedAtMs = remotePayload.activeAccountUpdatedAtMs,
        )
    val allFollowGlobal =
        chooseNewer(
            localValue = local.allAccountsFollowGlobalNotificationSettings,
            localUpdatedAtMs = local.allAccountsFollowGlobalNotificationSettingsUpdatedAtMs,
            remoteValue = remotePayload.allAccountsFollowGlobalNotificationSettings,
            remoteUpdatedAtMs = remotePayload.allAccountsFollowGlobalNotificationSettingsUpdatedAtMs,
        )
    val globalReminderSettings =
        chooseNewer(
            localValue = local.globalReminderSettings.normalized(),
            localUpdatedAtMs = local.globalReminderSettingsUpdatedAtMs,
            remoteValue = remotePayload.globalReminderSettings.normalized(),
            remoteUpdatedAtMs = remotePayload.globalReminderSettingsUpdatedAtMs,
        )

    return BaAccountsTransferPayload(
        version = BA_ACCOUNTS_TRANSFER_VERSION,
        exportedAtMs = nowMs.coerceAtLeast(remotePayload.exportedAtMs.coerceAtLeast(0L)),
        accounts = mergedAccounts,
        activeAccountId = activeAccountId,
        allAccountsFollowGlobalNotificationSettings = allFollowGlobal,
        globalReminderSettings = globalReminderSettings,
        activeAccountUpdatedAtMs =
            maxOf(local.activeAccountUpdatedAtMs, remotePayload.activeAccountUpdatedAtMs),
        allAccountsFollowGlobalNotificationSettingsUpdatedAtMs =
            maxOf(
                local.allAccountsFollowGlobalNotificationSettingsUpdatedAtMs,
                remotePayload.allAccountsFollowGlobalNotificationSettingsUpdatedAtMs,
            ),
        globalReminderSettingsUpdatedAtMs =
            maxOf(local.globalReminderSettingsUpdatedAtMs, remotePayload.globalReminderSettingsUpdatedAtMs),
    ).normalized()
}

private fun BaAccountsTransferPayload.normalized(): BaAccountsTransferPayload {
    val normalizedAccounts = accounts.normalizedAccounts()
    val resolvedActiveAccountId =
        activeAccountId?.takeIf { candidate ->
            normalizedAccounts.any { it.profile.id == candidate }
        }
    return copy(
        version = version.coerceAtLeast(BA_ACCOUNTS_TRANSFER_VERSION),
        exportedAtMs = exportedAtMs.coerceAtLeast(0L),
        accounts = normalizedAccounts,
        activeAccountId = resolvedActiveAccountId,
        globalReminderSettings = globalReminderSettings.normalized(),
        activeAccountUpdatedAtMs = activeAccountUpdatedAtMs.coerceAtLeast(0L),
        allAccountsFollowGlobalNotificationSettingsUpdatedAtMs =
            allAccountsFollowGlobalNotificationSettingsUpdatedAtMs.coerceAtLeast(0L),
        globalReminderSettingsUpdatedAtMs = globalReminderSettingsUpdatedAtMs.coerceAtLeast(0L),
    )
}

private fun mergeBaAccountRecord(
    local: BaAccountRecord?,
    remote: BaAccountRecord?,
): BaAccountRecord? {
    if (local == null) return remote
    if (remote == null) return local
    val profileChoice =
        chooseNewerWithTimestamp(
            localValue = local.profile,
            localUpdatedAtMs = local.profileUpdatedAtMs,
            remoteValue = remote.profile,
            remoteUpdatedAtMs = remote.profileUpdatedAtMs,
        )
    val runtimeChoice =
        chooseNewerWithTimestamp(
            localValue = local.runtime,
            localUpdatedAtMs = local.runtimeUpdatedAtMs,
            remoteValue = remote.runtime,
            remoteUpdatedAtMs = remote.runtimeUpdatedAtMs,
        )
    val reminderRuntimeChoice =
        chooseNewerWithTimestamp(
            localValue = local.reminderRuntime,
            localUpdatedAtMs = local.reminderRuntimeUpdatedAtMs,
            remoteValue = remote.reminderRuntime,
            remoteUpdatedAtMs = remote.reminderRuntimeUpdatedAtMs,
        )
    val reminderOverrideChoice =
        chooseNewerWithTimestamp(
            localValue = local.reminderOverride,
            localUpdatedAtMs = local.reminderOverrideUpdatedAtMs,
            remoteValue = remote.reminderOverride,
            remoteUpdatedAtMs = remote.reminderOverrideUpdatedAtMs,
        )
    return local.copy(
        profile = profileChoice.value,
        runtime = runtimeChoice.value,
        reminderRuntime = reminderRuntimeChoice.value,
        reminderOverride = reminderOverrideChoice.value,
        profileUpdatedAtMs = profileChoice.updatedAtMs,
        runtimeUpdatedAtMs = runtimeChoice.updatedAtMs,
        reminderRuntimeUpdatedAtMs = reminderRuntimeChoice.updatedAtMs,
        reminderOverrideUpdatedAtMs = reminderOverrideChoice.updatedAtMs,
    )
}

private fun chooseActiveAccountId(
    mergedAccounts: List<BaAccountRecord>,
    localActiveAccountId: BaAccountId?,
    localUpdatedAtMs: Long,
    remoteActiveAccountId: BaAccountId?,
    remoteUpdatedAtMs: Long,
): BaAccountId? {
    val accountIds = mergedAccounts.mapTo(HashSet()) { it.profile.id }
    val newerFirst =
        if (remoteUpdatedAtMs > localUpdatedAtMs) {
            listOf(remoteActiveAccountId, localActiveAccountId)
        } else {
            listOf(localActiveAccountId, remoteActiveAccountId)
        }
    return newerFirst
        .firstOrNull { candidate -> candidate != null && candidate in accountIds }
        ?: mergedAccounts.firstOrNull()?.profile?.id
}

private data class TimestampedChoice<T>(
    val value: T,
    val updatedAtMs: Long,
)

private fun <T> chooseNewer(
    localValue: T,
    localUpdatedAtMs: Long,
    remoteValue: T,
    remoteUpdatedAtMs: Long,
): T =
    chooseNewerWithTimestamp(
        localValue = localValue,
        localUpdatedAtMs = localUpdatedAtMs,
        remoteValue = remoteValue,
        remoteUpdatedAtMs = remoteUpdatedAtMs,
    ).value

private fun <T> chooseNewerWithTimestamp(
    localValue: T,
    localUpdatedAtMs: Long,
    remoteValue: T,
    remoteUpdatedAtMs: Long,
): TimestampedChoice<T> =
    if (remoteUpdatedAtMs > localUpdatedAtMs) {
        TimestampedChoice(remoteValue, remoteUpdatedAtMs.coerceAtLeast(0L))
    } else {
        TimestampedChoice(localValue, localUpdatedAtMs.coerceAtLeast(0L))
    }
