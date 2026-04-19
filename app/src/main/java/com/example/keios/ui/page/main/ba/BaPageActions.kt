package com.example.keios.ui.page.main.ba

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.keios.R
import org.json.JSONArray
import kotlin.math.roundToInt

internal data class BaSettingsPersistenceResult(
    val savedCafeLevel: Int,
    val savedThreshold: Int,
    val cafeVisitNotifyEnabled: Boolean,
    val showEndedPools: Boolean,
    val showEndedActivities: Boolean,
    val showCalendarPoolImages: Boolean,
    val mediaAdaptiveRotationEnabled: Boolean,
    val mediaSaveCustomEnabled: Boolean,
    val mediaSaveFixedTreeUri: String,
    val turningEndedActivitiesOn: Boolean,
    val turningImagesOn: Boolean,
)

internal fun copyBaFriendCodeToClipboard(
    context: Context,
    friendCode: String,
) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(
        ClipData.newPlainText(context.getString(R.string.ba_friend_code_clipboard_label), friendCode)
    )
    Toast.makeText(context, context.getString(R.string.ba_toast_friend_code_copied), Toast.LENGTH_SHORT).show()
}

internal fun openBaExternalLink(
    context: Context,
    url: String,
    failureMessage: String = context.getString(R.string.ba_error_open_activity_link),
) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show()
    }
}

internal fun hasAnyImageInBaCalendarCache(serverIdx: Int): Boolean {
    val (raw, _) = BASettingsStore.loadCalendarCache(serverIdx)
    if (raw.isBlank()) return false
    return runCatching {
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            if (normalizeGameKeeImageLink(obj.optString("imageUrl")).isNotBlank()) return@runCatching true
        }
        false
    }.getOrDefault(false)
}

internal fun hasAnyImageInBaPoolCache(serverIdx: Int): Boolean {
    val (raw, _) = BASettingsStore.loadPoolCache(serverIdx)
    if (raw.isBlank()) return false
    return runCatching {
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            if (normalizeGameKeeImageLink(obj.optString("imageUrl")).isNotBlank()) return@runCatching true
        }
        false
    }.getOrDefault(false)
}

internal fun persistBaSettingsDraft(
    sheetState: BaSettingsSheetState,
    currentCafeLevel: Int,
    currentShowEndedActivities: Boolean,
    currentShowCalendarPoolImages: Boolean,
): BaSettingsPersistenceResult {
    val savedCafeLevel = sheetState.cafeLevel.coerceIn(1, 10)
    val savedThreshold = sheetState.apNotifyThresholdText.toIntOrNull()?.coerceIn(0, BA_AP_MAX) ?: 120

    BASettingsStore.saveCafeLevel(savedCafeLevel)
    BASettingsStore.saveApNotifyEnabled(sheetState.apNotifyEnabled)
    BASettingsStore.saveCafeVisitNotifyEnabled(sheetState.cafeVisitNotifyEnabled)
    BASettingsStore.saveApNotifyThreshold(savedThreshold)
    BASettingsStore.savePoolShowEnded(sheetState.showEndedPools)
    BASettingsStore.saveActivityShowEnded(sheetState.showEndedActivities)
    BASettingsStore.saveShowCalendarPoolImages(sheetState.showCalendarPoolImages)
    BASettingsStore.saveMediaAdaptiveRotationEnabled(sheetState.mediaAdaptiveRotationEnabled)
    BASettingsStore.saveMediaSaveCustomEnabled(sheetState.mediaSaveCustomEnabled)
    BASettingsStore.saveMediaSaveFixedTreeUri(sheetState.mediaSaveFixedTreeUri)

    return BaSettingsPersistenceResult(
        savedCafeLevel = savedCafeLevel,
        savedThreshold = savedThreshold,
        cafeVisitNotifyEnabled = sheetState.cafeVisitNotifyEnabled,
        showEndedPools = sheetState.showEndedPools,
        showEndedActivities = sheetState.showEndedActivities,
        showCalendarPoolImages = sheetState.showCalendarPoolImages,
        mediaAdaptiveRotationEnabled = sheetState.mediaAdaptiveRotationEnabled,
        mediaSaveCustomEnabled = sheetState.mediaSaveCustomEnabled,
        mediaSaveFixedTreeUri = sheetState.mediaSaveFixedTreeUri,
        turningEndedActivitiesOn = !currentShowEndedActivities && sheetState.showEndedActivities,
        turningImagesOn = !currentShowCalendarPoolImages && sheetState.showCalendarPoolImages,
    )
}

internal fun sanitizeBaFriendCodeInput(raw: String): String {
    return raw.uppercase().filter { it in 'A'..'Z' }.take(8)
}

internal fun applyBaCurrentApUpdate(
    currentAp: Double,
    newValue: Int,
): Pair<Double, Long> {
    val nowMs = System.currentTimeMillis()
    val integerPart = newValue.coerceIn(0, BA_AP_MAX)
    val fractionPart = fractionalApPart(currentAp)
    val next = normalizeAp(integerPart.toDouble() + fractionPart)
    return next to nowMs
}

internal fun applyBaCurrentApDelta(
    currentAp: Double,
    delta: Double,
): Pair<Double, Long>? {
    if (delta <= 0.0) return null
    val nowMs = System.currentTimeMillis()
    return normalizeAp(currentAp + delta) to nowMs
}

internal fun coerceBaApLimit(newLimit: Int): Int {
    return newLimit.coerceIn(0, BA_AP_LIMIT_MAX)
}

internal fun applyBaApRegenTick(
    apLimit: Int,
    apCurrent: Double,
    apRegenBaseMs: Long,
    nowMs: Long = System.currentTimeMillis(),
): Pair<Double, Long> {
    val limit = apLimit.coerceIn(0, BA_AP_LIMIT_MAX)
    if (limit <= 0) return apCurrent.coerceAtLeast(0.0) to nowMs

    val current = apCurrent.coerceAtLeast(0.0)
    val ensuredBase = if (apRegenBaseMs <= 0L) nowMs else apRegenBaseMs
    if (current >= limit.toDouble()) return current to nowMs

    val elapsed = (nowMs - ensuredBase).coerceAtLeast(0L)
    val gained = (elapsed / BA_AP_REGEN_INTERVAL_MS).toInt()
    if (gained <= 0) return current to ensuredBase

    val pointsUntilStop = kotlin.math.ceil(limit.toDouble() - current).toInt().coerceAtLeast(0)
    val pointsApplied = gained.coerceAtMost(pointsUntilStop)
    if (pointsApplied <= 0) return current to ensuredBase

    val nextAp = normalizeAp(current + pointsApplied.toDouble())
    val nextBase = if (nextAp >= limit.toDouble()) {
        nowMs
    } else {
        ensuredBase + pointsApplied * BA_AP_REGEN_INTERVAL_MS
    }
    return nextAp to nextBase
}

internal fun applyBaCafeStorageTick(
    cafeStoredAp: Double,
    cafeLevel: Int,
    cafeLastHourMs: Long,
    nowMs: Long = System.currentTimeMillis(),
): Pair<Double, Long> {
    val currentHour = floorToHourMs(nowMs)
    val baseHour = if (cafeLastHourMs <= 0L || cafeLastHourMs > currentHour) currentHour else cafeLastHourMs
    if (currentHour <= baseHour) return cafeStoredAp to baseHour
    val hoursPassed = ((currentHour - baseHour) / BA_CAFE_HOURLY_INTERVAL_MS).toInt()
    if (hoursPassed <= 0) return cafeStoredAp to baseHour
    val gained = hoursPassed * cafeHourlyGain(cafeLevel)
    val cap = cafeStorageCap(cafeLevel)
    return normalizeAp((cafeStoredAp + gained).coerceAtMost(cap)) to currentHour
}

internal fun applyBaCafeClaim(
    cafeStoredAp: Double,
): Double {
    return normalizeAp(cafeStoredAp)
}

internal fun applyBaCafeDebugGain(
    cafeStoredAp: Double,
    cafeLevel: Int,
): Pair<Double, Int> {
    val gained = normalizeAp(cafeHourlyGain(cafeLevel) * 3.0)
    val cap = cafeStorageCap(cafeLevel)
    val next = normalizeAp((cafeStoredAp + gained).coerceAtMost(cap))
    return next to gained.roundToInt()
}

internal fun consumeBaHeadpat(
    coffeeHeadpatMs: Long,
    serverIndex: Int,
): Long? {
    val nowMs = System.currentTimeMillis()
    val availableAt = calculateNextHeadpatAvailableMs(coffeeHeadpatMs, serverIndex)
    if (coffeeHeadpatMs > 0L && availableAt > nowMs) return null
    return nowMs
}

internal fun consumeBaInviteTicket(
    usedMs: Long,
): Long? {
    val nowMs = System.currentTimeMillis()
    val availableAt = calculateInviteTicketAvailableMs(usedMs)
    if (usedMs > 0L && availableAt > nowMs) return null
    return nowMs
}
