package os.kei.ui.page.main.ba

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import os.kei.R
import os.kei.ui.page.main.ba.support.BASettingsStore
import os.kei.ui.page.main.ba.support.BA_AP_LIMIT_MAX
import os.kei.ui.page.main.ba.support.BA_AP_MAX
import os.kei.ui.page.main.ba.support.BA_DEFAULT_FRIEND_CODE
import os.kei.ui.page.main.ba.support.BA_DEFAULT_NICKNAME
import os.kei.ui.page.main.ba.support.BaPageSnapshot
import os.kei.ui.page.main.ba.support.cafeStorageCap
import os.kei.ui.page.main.ba.support.currentArenaRefreshSlotMs
import os.kei.ui.page.main.ba.support.currentCafeStudentRefreshSlotMs
import os.kei.ui.page.main.ba.support.displayAp
import os.kei.ui.page.main.ba.support.floorToHourMs
import os.kei.ui.page.main.ba.support.normalizeAp
import kotlin.math.roundToInt

@Stable
internal data class BaOfficeState(
    val cafeLevel: Int,
    val cafeStoredAp: Double,
    val cafeLastHourMs: Long,
    val idNickname: String,
    val idFriendCode: String,
    val apLimit: Int,
    val apCurrent: Double,
    val apRegenBaseMs: Long,
    val apSyncMs: Long,
    val apNotifyEnabled: Boolean,
    val apNotifyThreshold: Int,
    val arenaRefreshNotifyEnabled: Boolean,
    val arenaRefreshLastNotifiedSlotMs: Long,
    val cafeVisitNotifyEnabled: Boolean,
    val cafeVisitLastNotifiedSlotMs: Long,
    val coffeeHeadpatMs: Long,
    val coffeeInvite1UsedMs: Long,
    val coffeeInvite2UsedMs: Long,
    val apCurrentInput: String,
    val apLimitInput: String,
    val idNicknameInput: String,
    val idFriendCodeInput: String,
    val apLastNotifiedLevel: Int,
)

@Stable
internal class BaOfficeController(
    snapshot: BaPageSnapshot,
) {
    var cafeLevel by mutableIntStateOf(snapshot.cafeLevel)
    var cafeStoredAp by mutableStateOf(snapshot.cafeStoredAp)
    var cafeLastHourMs by mutableLongStateOf(snapshot.cafeLastHourMs)
    var idNickname by mutableStateOf(snapshot.idNickname)
    var idFriendCode by mutableStateOf(snapshot.idFriendCode)
    var apLimit by mutableIntStateOf(snapshot.apLimit)
    var apCurrent by mutableStateOf(snapshot.apCurrent.coerceAtLeast(0.0))
    var apRegenBaseMs by mutableLongStateOf(snapshot.apRegenBaseMs)
    var apSyncMs by mutableLongStateOf(snapshot.apSyncMs)
    var apNotifyEnabled by mutableStateOf(snapshot.apNotifyEnabled)
    var apNotifyThreshold by mutableIntStateOf(snapshot.apNotifyThreshold)
    var arenaRefreshNotifyEnabled by mutableStateOf(snapshot.arenaRefreshNotifyEnabled)
    var arenaRefreshLastNotifiedSlotMs by mutableLongStateOf(snapshot.arenaRefreshLastNotifiedSlotMs)
    var cafeVisitNotifyEnabled by mutableStateOf(snapshot.cafeVisitNotifyEnabled)
    var cafeVisitLastNotifiedSlotMs by mutableLongStateOf(snapshot.cafeVisitLastNotifiedSlotMs)
    var coffeeHeadpatMs by mutableLongStateOf(snapshot.coffeeHeadpatMs)
    var coffeeInvite1UsedMs by mutableLongStateOf(snapshot.coffeeInvite1UsedMs)
    var coffeeInvite2UsedMs by mutableLongStateOf(snapshot.coffeeInvite2UsedMs)

    var apCurrentInput by mutableStateOf(displayAp(apCurrent).toString())
    var apLimitInput by mutableStateOf(apLimit.toString())
    var idNicknameInput by mutableStateOf(idNickname)
    var idFriendCodeInput by mutableStateOf(idFriendCode)
    var apLastNotifiedLevel by mutableIntStateOf(snapshot.apLastNotifiedLevel)

    fun displayApInputText(): String = displayAp(apCurrent).toString()

    fun state(): BaOfficeState {
        return BaOfficeState(
            cafeLevel = cafeLevel,
            cafeStoredAp = cafeStoredAp,
            cafeLastHourMs = cafeLastHourMs,
            idNickname = idNickname,
            idFriendCode = idFriendCode,
            apLimit = apLimit,
            apCurrent = apCurrent,
            apRegenBaseMs = apRegenBaseMs,
            apSyncMs = apSyncMs,
            apNotifyEnabled = apNotifyEnabled,
            apNotifyThreshold = apNotifyThreshold,
            arenaRefreshNotifyEnabled = arenaRefreshNotifyEnabled,
            arenaRefreshLastNotifiedSlotMs = arenaRefreshLastNotifiedSlotMs,
            cafeVisitNotifyEnabled = cafeVisitNotifyEnabled,
            cafeVisitLastNotifiedSlotMs = cafeVisitLastNotifiedSlotMs,
            coffeeHeadpatMs = coffeeHeadpatMs,
            coffeeInvite1UsedMs = coffeeInvite1UsedMs,
            coffeeInvite2UsedMs = coffeeInvite2UsedMs,
            apCurrentInput = apCurrentInput,
            apLimitInput = apLimitInput,
            idNicknameInput = idNicknameInput,
            idFriendCodeInput = idFriendCodeInput,
            apLastNotifiedLevel = apLastNotifiedLevel,
        )
    }

    fun ensureRegenBase(nowMs: Long = System.currentTimeMillis()) {
        if (apRegenBaseMs <= 0L) {
            apRegenBaseMs = nowMs
            BASettingsStore.saveApRegenBaseMs(nowMs)
        }
    }

    fun ensureCafeHourBase(nowMs: Long = System.currentTimeMillis()) {
        val currentHour = floorToHourMs(nowMs)
        if (cafeLastHourMs <= 0L || cafeLastHourMs > currentHour) {
            cafeLastHourMs = currentHour
            BASettingsStore.saveCafeLastHourMs(currentHour)
        }
    }

    fun clampCafeStoredToCap() {
        val cap = cafeStorageCap(cafeLevel)
        val clamped = normalizeAp(cafeStoredAp.coerceIn(0.0, cap))
        if (clamped != cafeStoredAp) {
            cafeStoredAp = clamped
            BASettingsStore.saveCafeStoredAp(clamped)
        }
    }

    fun saveIdNicknameFromInput() {
        val sanitized = idNicknameInput.take(10).ifEmpty { BA_DEFAULT_NICKNAME }
        idNickname = sanitized
        idNicknameInput = sanitized
        BASettingsStore.saveIdNickname(sanitized)
    }

    fun saveIdFriendCodeFromInput(context: Context) {
        val sanitized = sanitizeBaFriendCodeInput(idFriendCodeInput)
        if (sanitized.length != 8) {
            Toast.makeText(context, context.getString(R.string.ba_toast_friend_code_invalid), Toast.LENGTH_SHORT).show()
            idFriendCodeInput = idFriendCode
            return
        }
        idFriendCode = sanitized
        idFriendCodeInput = sanitized
        BASettingsStore.saveIdFriendCode(sanitized)
    }

    fun updateCurrentAp(newValue: Int, markSync: Boolean) {
        val (next, nowMs) = applyBaCurrentApUpdate(
            currentAp = apCurrent,
            newValue = newValue,
        )
        apCurrent = next
        BASettingsStore.saveApCurrent(next)
        apRegenBaseMs = nowMs
        BASettingsStore.saveApRegenBaseMs(nowMs)
        if (markSync) {
            apSyncMs = nowMs
            BASettingsStore.saveApSyncMs(nowMs)
        }
    }

    fun addCurrentAp(delta: Double, markSync: Boolean) {
        val result = applyBaCurrentApDelta(
            currentAp = apCurrent,
            delta = delta,
        ) ?: return
        val (next, nowMs) = result
        apCurrent = next
        BASettingsStore.saveApCurrent(next)
        apRegenBaseMs = nowMs
        BASettingsStore.saveApRegenBaseMs(nowMs)
        if (markSync) {
            apSyncMs = nowMs
            BASettingsStore.saveApSyncMs(nowMs)
        }
    }

    fun updateApLimit(newLimit: Int) {
        val clamped = coerceBaApLimit(newLimit)
        apLimit = clamped
        BASettingsStore.saveApLimit(clamped)
        ensureRegenBase()
    }

    fun applyApRegen(nowMs: Long = System.currentTimeMillis()) {
        if (apLimit.coerceIn(0, BA_AP_LIMIT_MAX) <= 0) {
            apRegenBaseMs = nowMs
            BASettingsStore.saveApRegenBaseMs(nowMs)
            if (apCurrent < 0.0) {
                apCurrent = 0.0
                BASettingsStore.saveApCurrent(0.0)
            }
            return
        }
        val (nextAp, nextBase) = applyBaApRegenTick(
            apLimit = apLimit,
            apCurrent = apCurrent,
            apRegenBaseMs = apRegenBaseMs,
            nowMs = nowMs,
        )
        if (nextAp != apCurrent) {
            apCurrent = nextAp
            BASettingsStore.saveApCurrent(nextAp)
        }
        if (nextBase != apRegenBaseMs) {
            apRegenBaseMs = nextBase
            BASettingsStore.saveApRegenBaseMs(nextBase)
        }
    }

    fun applyCafeStorage(nowMs: Long = System.currentTimeMillis()) {
        val (nextStoredAp, nextHour) = applyBaCafeStorageTick(
            cafeStoredAp = cafeStoredAp,
            cafeLevel = cafeLevel,
            cafeLastHourMs = cafeLastHourMs,
            nowMs = nowMs,
        )
        if (nextStoredAp != cafeStoredAp) {
            cafeStoredAp = nextStoredAp
            BASettingsStore.saveCafeStoredAp(nextStoredAp)
        }
        if (nextHour != cafeLastHourMs) {
            cafeLastHourMs = nextHour
            BASettingsStore.saveCafeLastHourMs(nextHour)
        }
    }

    fun claimCafeStoredAp(context: Context) {
        applyCafeStorage()
        val claim = applyBaCafeClaim(cafeStoredAp)
        if (claim <= 0.0) {
            Toast.makeText(context, context.getString(R.string.ba_toast_cafe_no_ap), Toast.LENGTH_SHORT).show()
            return
        }
        addCurrentAp(claim, markSync = true)
        cafeStoredAp = 0.0
        BASettingsStore.saveCafeStoredAp(0.0)
        Toast.makeText(
            context,
            context.getString(R.string.ba_toast_cafe_claimed_ap, claim.roundToInt()),
            Toast.LENGTH_SHORT
        ).show()
    }

    fun testCafePlus3Hours(context: Context) {
        applyCafeStorage()
        val (nextStoredAp, gainedInt) = applyBaCafeDebugGain(
            cafeStoredAp = cafeStoredAp,
            cafeLevel = cafeLevel,
        )
        cafeStoredAp = nextStoredAp
        BASettingsStore.saveCafeStoredAp(cafeStoredAp)
        Toast.makeText(
            context,
            context.getString(R.string.ba_toast_cafe_debug_added, gainedInt),
            Toast.LENGTH_SHORT
        ).show()
    }

    fun copyFriendCodeToClipboard(context: Context) {
        copyBaFriendCodeToClipboard(
            context = context,
            friendCode = idFriendCode.ifBlank { BA_DEFAULT_FRIEND_CODE },
        )
    }

    fun touchHead(serverIndex: Int) {
        val consumedAt = consumeBaHeadpat(
            coffeeHeadpatMs = coffeeHeadpatMs,
            serverIndex = serverIndex,
        ) ?: return
        coffeeHeadpatMs = consumedAt
        BASettingsStore.saveCoffeeHeadpatMs(consumedAt)
    }

    fun forceResetHeadpatCooldown() {
        coffeeHeadpatMs = 0L
        BASettingsStore.saveCoffeeHeadpatMs(0L)
    }

    fun useInviteTicket1() {
        val consumedAt = consumeBaInviteTicket(coffeeInvite1UsedMs) ?: return
        coffeeInvite1UsedMs = consumedAt
        BASettingsStore.saveCoffeeInvite1UsedMs(consumedAt)
    }

    fun forceResetInviteTicket1Cooldown() {
        coffeeInvite1UsedMs = 0L
        BASettingsStore.saveCoffeeInvite1UsedMs(0L)
    }

    fun useInviteTicket2() {
        val consumedAt = consumeBaInviteTicket(coffeeInvite2UsedMs) ?: return
        coffeeInvite2UsedMs = consumedAt
        BASettingsStore.saveCoffeeInvite2UsedMs(consumedAt)
    }

    fun forceResetInviteTicket2Cooldown() {
        coffeeInvite2UsedMs = 0L
        BASettingsStore.saveCoffeeInvite2UsedMs(0L)
    }

    fun sendApTestNotification(
        context: Context,
        showToast: Boolean = true,
        thresholdTriggered: Boolean = false,
    ): Boolean {
        val currentDisplay = displayAp(apCurrent)
        val limitDisplay = apLimit.coerceIn(0, BA_AP_MAX)
        val thresholdDisplay = apNotifyThreshold.coerceIn(0, BA_AP_MAX)
        val sent = BaApNotificationDispatcher.send(
            context = context,
            currentDisplay = currentDisplay,
            limitDisplay = limitDisplay,
            thresholdDisplay = thresholdDisplay
        )
        if (!sent) {
            if (showToast) {
                Toast.makeText(
                    context,
                    context.getString(R.string.ba_toast_notification_permission_required),
                    Toast.LENGTH_SHORT
                ).show()
            }
            return false
        }
        if (showToast) {
            val notifyText = if (thresholdTriggered) "已发送AP阈值提醒" else "已发送AP通知"
            Toast.makeText(context, notifyText, Toast.LENGTH_SHORT).show()
        }
        return true
    }

    fun sendCafeVisitTestNotification(
        context: Context,
        serverIndex: Int,
        showToast: Boolean = true,
    ): Boolean {
        val slotMs = currentCafeStudentRefreshSlotMs(
            nowMs = System.currentTimeMillis(),
            serverIndex = serverIndex
        )
        val sent = BaCafeVisitNotificationDispatcher.send(
            context = context,
            serverIndex = serverIndex,
            slotMs = slotMs
        )
        if (!sent) {
            if (showToast) {
                Toast.makeText(
                    context,
                    context.getString(R.string.ba_toast_notification_permission_required),
                    Toast.LENGTH_SHORT
                ).show()
            }
            return false
        }
        if (showToast) {
            Toast.makeText(
                context,
                context.getString(R.string.ba_toast_cafe_visit_notification_sent),
                Toast.LENGTH_SHORT
            ).show()
        }
        return true
    }

    fun sendArenaRefreshTestNotification(
        context: Context,
        serverIndex: Int,
        showToast: Boolean = true,
    ): Boolean {
        val slotMs = currentArenaRefreshSlotMs(
            nowMs = System.currentTimeMillis(),
            serverIndex = serverIndex
        )
        val sent = BaArenaRefreshNotificationDispatcher.send(
            context = context,
            serverIndex = serverIndex,
            slotMs = slotMs
        )
        if (!sent) {
            if (showToast) {
                Toast.makeText(
                    context,
                    context.getString(R.string.ba_toast_notification_permission_required),
                    Toast.LENGTH_SHORT
                ).show()
            }
            return false
        }
        if (showToast) {
            Toast.makeText(
                context,
                context.getString(R.string.ba_toast_arena_refresh_notification_sent),
                Toast.LENGTH_SHORT
            ).show()
        }
        return true
    }

    fun tryApThresholdNotification(context: Context) {
        if (!apNotifyEnabled) {
            apLastNotifiedLevel = -1
            BASettingsStore.saveApLastNotifiedLevel(-1)
            return
        }
        val threshold = apNotifyThreshold.coerceIn(0, BA_AP_MAX)
        val currentDisplay = displayAp(apCurrent)
        if (currentDisplay < threshold) {
            apLastNotifiedLevel = -1
            BASettingsStore.saveApLastNotifiedLevel(-1)
            return
        }
        if (currentDisplay == apLastNotifiedLevel) return
        if (sendApTestNotification(context = context, showToast = false, thresholdTriggered = true)) {
            apLastNotifiedLevel = currentDisplay
            BASettingsStore.saveApLastNotifiedLevel(currentDisplay)
        }
    }
}

@Composable
internal fun rememberBaOfficeController(
    snapshot: BaPageSnapshot,
): BaOfficeController {
    return remember(snapshot) { BaOfficeController(snapshot) }
}
