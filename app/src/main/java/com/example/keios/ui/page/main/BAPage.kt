package com.example.keios.ui.page.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keios.mcp.McpNotificationHelper
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassSearchField
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.LiquidActionBar
import com.example.keios.ui.page.main.widget.LiquidActionItem
import com.example.keios.ui.page.main.widget.StatusPill
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowListPopup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

private enum class BAInitState {
    Empty,
    Draft
}

private const val BA_AP_MAX = 999
private const val BA_AP_LIMIT_MAX = 240
private const val BA_AP_REGEN_INTERVAL_MS = 6 * 60 * 1000L
private const val BA_AP_REGEN_TICK_MS = 30_000L
private const val BA_CAFE_HOURLY_INTERVAL_MS = 60 * 60 * 1000L
private const val BA_DEFAULT_NICKNAME = "Kei"
private const val BA_DEFAULT_FRIEND_CODE = "ARISUKEI"
private val BA_CAFE_DAILY_AP_BY_LEVEL = intArrayOf(92, 152, 222, 302, 390, 460, 530, 600, 570, 740)

private fun displayAp(apExact: Double): Int = apExact.coerceAtLeast(0.0).toInt()

private fun normalizeAp(apExact: Double): Double {
    return (apExact.coerceIn(0.0, BA_AP_MAX.toDouble()) * 1000.0).roundToInt() / 1000.0
}

private fun fractionalApPart(apExact: Double): Double {
    val normalized = normalizeAp(apExact)
    val integerPart = displayAp(normalized).toDouble()
    return normalizeAp(normalized - integerPart).coerceIn(0.0, 0.999)
}

private fun cafeHourlyGain(level: Int): Double {
    val safeLevel = level.coerceIn(1, 10)
    return BA_CAFE_DAILY_AP_BY_LEVEL[safeLevel - 1] / 24.0
}

private fun cafeDailyCapacity(level: Int): Int {
    val safeLevel = level.coerceIn(1, 10)
    return BA_CAFE_DAILY_AP_BY_LEVEL[safeLevel - 1]
}

private fun cafeStorageCap(level: Int): Double {
    val safeLevel = level.coerceIn(1, 10)
    return BA_CAFE_DAILY_AP_BY_LEVEL[safeLevel - 1].toDouble()
}

private fun floorToHourMs(epochMs: Long): Long = epochMs - (epochMs % BA_CAFE_HOURLY_INTERVAL_MS)

private fun formatBaDateTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return "未同步"
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(epochMillis))
    }.getOrDefault("未同步")
}

private object BASettingsStore {
    private const val KV_ID = "ba_page_settings"
    private const val KEY_SERVER_INDEX = "server_index"
    private const val KEY_CAFE_LEVEL = "cafe_level"
    private const val KEY_CAFE_STORED_AP = "cafe_stored_ap"
    private const val KEY_CAFE_LAST_HOUR_MS = "cafe_last_hour_ms"
    private const val KEY_ID_NICKNAME = "id_nickname"
    private const val KEY_ID_FRIEND_CODE = "id_friend_code"
    private const val KEY_AP_LIMIT = "ap_limit"
    private const val KEY_AP_NOTIFY_ENABLED = "ap_notify_enabled"
    private const val KEY_AP_NOTIFY_THRESHOLD = "ap_notify_threshold"
    private const val KEY_AP_CURRENT = "ap_current"
    private const val KEY_AP_CURRENT_EXACT = "ap_current_exact"
    private const val KEY_AP_REGEN_BASE_MS = "ap_regen_base_ms"
    private const val KEY_AP_SYNC_MS = "ap_sync_ms"

    private const val DEFAULT_SERVER_INDEX = 2
    private const val DEFAULT_CAFE_LEVEL = 1
    private const val DEFAULT_CAFE_STORED_AP = 0.0
    private const val DEFAULT_ID_NICKNAME = BA_DEFAULT_NICKNAME
    private const val DEFAULT_ID_FRIEND_CODE = BA_DEFAULT_FRIEND_CODE
    private const val DEFAULT_AP_LIMIT = BA_AP_LIMIT_MAX
    private const val DEFAULT_AP_NOTIFY_THRESHOLD = 120
    private const val DEFAULT_AP_CURRENT = 0.0

    private fun kv(): MMKV = MMKV.mmkvWithID(KV_ID)

    fun loadServerIndex(): Int = kv().decodeInt(KEY_SERVER_INDEX, DEFAULT_SERVER_INDEX).coerceIn(0, 2)

    fun saveServerIndex(index: Int) {
        kv().encode(KEY_SERVER_INDEX, index.coerceIn(0, 2))
    }

    fun loadCafeLevel(): Int = kv().decodeInt(KEY_CAFE_LEVEL, DEFAULT_CAFE_LEVEL).coerceIn(1, 10)

    fun saveCafeLevel(level: Int) {
        kv().encode(KEY_CAFE_LEVEL, level.coerceIn(1, 10))
    }

    fun loadCafeStoredAp(): Double {
        val raw = kv().decodeString(KEY_CAFE_STORED_AP, DEFAULT_CAFE_STORED_AP.toString())
        return normalizeAp(raw?.toDoubleOrNull() ?: DEFAULT_CAFE_STORED_AP)
    }

    fun saveCafeStoredAp(storedAp: Double) {
        kv().encode(KEY_CAFE_STORED_AP, normalizeAp(storedAp).toString())
    }

    fun loadCafeLastHourMs(): Long = kv().decodeLong(KEY_CAFE_LAST_HOUR_MS, 0L)

    fun saveCafeLastHourMs(epochMs: Long) {
        kv().encode(KEY_CAFE_LAST_HOUR_MS, floorToHourMs(epochMs.coerceAtLeast(0L)))
    }

    fun loadIdNickname(): String {
        val raw = kv().decodeString(KEY_ID_NICKNAME, DEFAULT_ID_NICKNAME).orEmpty().take(10)
        return raw.ifEmpty { DEFAULT_ID_NICKNAME }
    }

    fun saveIdNickname(name: String) {
        val sanitized = name.take(10).ifEmpty { DEFAULT_ID_NICKNAME }
        kv().encode(KEY_ID_NICKNAME, sanitized)
    }

    fun loadIdFriendCode(): String {
        val normalized = kv().decodeString(KEY_ID_FRIEND_CODE, DEFAULT_ID_FRIEND_CODE)
            .orEmpty()
            .uppercase(Locale.ROOT)
            .filter { it in 'A'..'Z' }
            .take(8)
        return if (normalized.length == 8) normalized else DEFAULT_ID_FRIEND_CODE
    }

    fun saveIdFriendCode(code: String) {
        val normalized = code.uppercase(Locale.ROOT).filter { it in 'A'..'Z' }.take(8)
        kv().encode(
            KEY_ID_FRIEND_CODE,
            if (normalized.length == 8) normalized else DEFAULT_ID_FRIEND_CODE
        )
    }

    fun loadApLimit(): Int = kv().decodeInt(KEY_AP_LIMIT, DEFAULT_AP_LIMIT).coerceIn(0, BA_AP_LIMIT_MAX)

    fun saveApLimit(limit: Int) {
        kv().encode(KEY_AP_LIMIT, limit.coerceIn(0, BA_AP_LIMIT_MAX))
    }

    fun loadApNotifyEnabled(): Boolean = kv().decodeBool(KEY_AP_NOTIFY_ENABLED, false)

    fun saveApNotifyEnabled(enabled: Boolean) {
        kv().encode(KEY_AP_NOTIFY_ENABLED, enabled)
    }

    fun loadApNotifyThreshold(): Int =
        kv().decodeInt(KEY_AP_NOTIFY_THRESHOLD, DEFAULT_AP_NOTIFY_THRESHOLD).coerceIn(0, BA_AP_MAX)

    fun saveApNotifyThreshold(threshold: Int) {
        kv().encode(KEY_AP_NOTIFY_THRESHOLD, threshold.coerceIn(0, BA_AP_MAX))
    }

    fun loadApCurrent(): Double {
        val store = kv()
        val value = if (store.containsKey(KEY_AP_CURRENT_EXACT)) {
            store.decodeString(KEY_AP_CURRENT_EXACT, DEFAULT_AP_CURRENT.toString())?.toDoubleOrNull() ?: DEFAULT_AP_CURRENT
        } else {
            store.decodeInt(KEY_AP_CURRENT, DEFAULT_AP_CURRENT.toInt()).toDouble()
        }
        return normalizeAp(value.coerceIn(0.0, BA_AP_MAX.toDouble()))
    }

    fun saveApCurrent(current: Double) {
        val normalized = normalizeAp(current)
        kv().encode(KEY_AP_CURRENT_EXACT, normalized.toString())
        kv().encode(KEY_AP_CURRENT, displayAp(normalized))
    }

    fun loadApRegenBaseMs(): Long = kv().decodeLong(KEY_AP_REGEN_BASE_MS, 0L)

    fun saveApRegenBaseMs(epochMs: Long) {
        kv().encode(KEY_AP_REGEN_BASE_MS, epochMs.coerceAtLeast(0L))
    }

    fun loadApSyncMs(): Long = kv().decodeLong(KEY_AP_SYNC_MS, 0L)

    fun saveApSyncMs(epochMs: Long) {
        kv().encode(KEY_AP_SYNC_MS, epochMs.coerceAtLeast(0L))
    }
}

@Composable
fun BAPage(
    contentBottomPadding: Dp = 72.dp,
    scrollToTopSignal: Int = 0,
    onActionBarInteractingChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop: LayerBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val serverOptions = remember { listOf("国服", "国际服", "日服") }
    val cafeLevelOptions = remember { (1..10).toList() }

    var initState by remember { mutableStateOf(BAInitState.Empty) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showOverviewServerPopup by remember { mutableStateOf(false) }
    var showCafeLevelPopup by remember { mutableStateOf(false) }

    var serverIndex by remember { mutableIntStateOf(BASettingsStore.loadServerIndex()) }
    var cafeLevel by remember { mutableIntStateOf(BASettingsStore.loadCafeLevel()) }
    var cafeStoredAp by remember { mutableStateOf(BASettingsStore.loadCafeStoredAp()) }
    var cafeLastHourMs by remember { mutableLongStateOf(BASettingsStore.loadCafeLastHourMs()) }
    var idNickname by remember { mutableStateOf(BASettingsStore.loadIdNickname()) }
    var idFriendCode by remember { mutableStateOf(BASettingsStore.loadIdFriendCode()) }
    var apLimit by remember { mutableIntStateOf(BASettingsStore.loadApLimit()) }
    var apCurrent by remember {
        mutableStateOf(BASettingsStore.loadApCurrent().coerceAtLeast(0.0))
    }
    var apRegenBaseMs by remember { mutableLongStateOf(BASettingsStore.loadApRegenBaseMs()) }
    var apSyncMs by remember { mutableLongStateOf(BASettingsStore.loadApSyncMs()) }
    var apNotifyEnabled by remember { mutableStateOf(BASettingsStore.loadApNotifyEnabled()) }
    var apNotifyThreshold by remember { mutableIntStateOf(BASettingsStore.loadApNotifyThreshold()) }

    var sheetCafeLevel by remember { mutableIntStateOf(cafeLevel) }
    var sheetApNotifyEnabled by remember { mutableStateOf(apNotifyEnabled) }
    var sheetApNotifyThresholdText by remember { mutableStateOf(apNotifyThreshold.toString()) }

    var apCurrentInput by remember { mutableStateOf(displayAp(apCurrent).toString()) }
    var apLimitInput by remember { mutableStateOf(apLimit.toString()) }
    var idNicknameInput by remember { mutableStateOf(idNickname) }
    var idFriendCodeInput by remember { mutableStateOf(idFriendCode) }
    var apLastNotifiedLevel by remember { mutableIntStateOf(-1) }

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

    fun saveIdFriendCodeFromInput() {
        val sanitized = idFriendCodeInput
            .uppercase(Locale.ROOT)
            .filter { it in 'A'..'Z' }
            .take(8)
        if (sanitized.length != 8) {
            Toast.makeText(context, "好友码需为8位大写字母", Toast.LENGTH_SHORT).show()
            idFriendCodeInput = idFriendCode
            return
        }
        idFriendCode = sanitized
        idFriendCodeInput = sanitized
        BASettingsStore.saveIdFriendCode(sanitized)
    }

    fun updateCurrentAp(newValue: Int, markSync: Boolean) {
        val nowMs = System.currentTimeMillis()
        val integerPart = newValue.coerceIn(0, BA_AP_MAX)
        val fractionPart = fractionalApPart(apCurrent)
        val next = normalizeAp(integerPart.toDouble() + fractionPart)
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
        if (delta <= 0.0) return
        val nowMs = System.currentTimeMillis()
        val next = normalizeAp(apCurrent + delta)
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
        val clamped = newLimit.coerceIn(0, BA_AP_LIMIT_MAX)
        apLimit = clamped
        BASettingsStore.saveApLimit(clamped)
        ensureRegenBase()
    }

    fun applyApRegen(nowMs: Long = System.currentTimeMillis()) {
        val limit = apLimit.coerceIn(0, BA_AP_LIMIT_MAX)
        if (limit <= 0) {
            apRegenBaseMs = nowMs
            BASettingsStore.saveApRegenBaseMs(nowMs)
            return
        }
        if (apCurrent < 0.0) {
            apCurrent = 0.0
            BASettingsStore.saveApCurrent(0.0)
        }

        ensureRegenBase(nowMs)

        if (apCurrent >= limit.toDouble()) {
            if (apRegenBaseMs != nowMs) {
                apRegenBaseMs = nowMs
                BASettingsStore.saveApRegenBaseMs(nowMs)
            }
            return
        }

        val elapsed = (nowMs - apRegenBaseMs).coerceAtLeast(0L)
        val gained = (elapsed / BA_AP_REGEN_INTERVAL_MS).toInt()
        if (gained <= 0) return

        val pointsUntilStop = ceil(limit.toDouble() - apCurrent).toInt().coerceAtLeast(0)
        val pointsApplied = gained.coerceAtMost(pointsUntilStop)
        if (pointsApplied <= 0) return

        val nextAp = normalizeAp(apCurrent + pointsApplied.toDouble())
        apCurrent = nextAp
        BASettingsStore.saveApCurrent(nextAp)

        apRegenBaseMs = if (nextAp >= limit.toDouble()) {
            nowMs
        } else {
            apRegenBaseMs + pointsApplied * BA_AP_REGEN_INTERVAL_MS
        }
        BASettingsStore.saveApRegenBaseMs(apRegenBaseMs)
    }

    fun calculateApFullAtMs(nowMs: Long = System.currentTimeMillis()): Long {
        val limit = apLimit.coerceIn(0, BA_AP_LIMIT_MAX)
        if (limit <= 0) return nowMs
        val current = apCurrent.coerceAtLeast(0.0)
        if (current >= limit.toDouble()) return nowMs

        val base = apRegenBaseMs.takeIf { it > 0L } ?: nowMs
        val elapsed = (nowMs - base).coerceAtLeast(0L)
        val remainder = elapsed % BA_AP_REGEN_INTERVAL_MS
        val pointsNeeded = ceil(limit.toDouble() - current).toLong().coerceAtLeast(0L)
        if (pointsNeeded <= 0L) return nowMs

        val untilNextPoint = if (remainder == 0L) BA_AP_REGEN_INTERVAL_MS else BA_AP_REGEN_INTERVAL_MS - remainder
        return nowMs + untilNextPoint + (pointsNeeded - 1L) * BA_AP_REGEN_INTERVAL_MS
    }

    fun applyCafeStorage(nowMs: Long = System.currentTimeMillis()) {
        ensureCafeHourBase(nowMs)
        val currentHour = floorToHourMs(nowMs)
        if (currentHour <= cafeLastHourMs) return
        val hoursPassed = ((currentHour - cafeLastHourMs) / BA_CAFE_HOURLY_INTERVAL_MS).toInt()
        if (hoursPassed <= 0) return
        val gained = hoursPassed * cafeHourlyGain(cafeLevel)
        val cap = cafeStorageCap(cafeLevel)
        cafeStoredAp = normalizeAp((cafeStoredAp + gained).coerceAtMost(cap))
        BASettingsStore.saveCafeStoredAp(cafeStoredAp)
        cafeLastHourMs = currentHour
        BASettingsStore.saveCafeLastHourMs(currentHour)
    }

    fun claimCafeStoredAp() {
        applyCafeStorage()
        val claim = normalizeAp(cafeStoredAp)
        if (claim <= 0.0) {
            Toast.makeText(context, "咖啡厅暂无可领取体力", Toast.LENGTH_SHORT).show()
            return
        }
        addCurrentAp(claim, markSync = true)
        cafeStoredAp = 0.0
        BASettingsStore.saveCafeStoredAp(0.0)
        Toast.makeText(context, "已领取 ${claim.roundToInt()} 体力", Toast.LENGTH_SHORT).show()
    }

    fun testCafePlus3Hours() {
        applyCafeStorage()
        val gained = normalizeAp(cafeHourlyGain(cafeLevel) * 3.0)
        val cap = cafeStorageCap(cafeLevel)
        cafeStoredAp = normalizeAp((cafeStoredAp + gained).coerceAtMost(cap))
        BASettingsStore.saveCafeStoredAp(cafeStoredAp)
        Toast.makeText(context, "已增加 +3h 咖啡厅体力（+${gained.roundToInt()}）", Toast.LENGTH_SHORT).show()
    }

    fun copyFriendCodeToClipboard() {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        val friendCode = idFriendCode.ifBlank { BA_DEFAULT_FRIEND_CODE }
        clipboard.setPrimaryClip(ClipData.newPlainText("BA Friend Code", friendCode))
        Toast.makeText(context, "好友码已复制", Toast.LENGTH_SHORT).show()
    }

    fun sendApTestNotification(showToast: Boolean = true): Boolean {
        val notificationsGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (!notificationsGranted) {
            if (showToast) Toast.makeText(context, "请先授予通知权限", Toast.LENGTH_SHORT).show()
            return false
        }
        McpNotificationHelper.notifyTest(
            context = context,
            serverName = "BlueArchive AP",
            running = true,
            port = displayAp(apCurrent),
            path = "阈值:$apNotifyThreshold",
            clients = apLimit
        )
        if (showToast) Toast.makeText(context, "已发送AP测试通知", Toast.LENGTH_SHORT).show()
        return true
    }

    fun tryApThresholdNotification() {
        if (!apNotifyEnabled) {
            apLastNotifiedLevel = -1
            return
        }
        val threshold = apNotifyThreshold.coerceIn(0, BA_AP_MAX)
        val currentDisplay = displayAp(apCurrent)
        if (currentDisplay < threshold) {
            apLastNotifiedLevel = -1
            return
        }
        if (currentDisplay == apLastNotifiedLevel) return
        if (sendApTestNotification(showToast = false)) {
            apLastNotifiedLevel = currentDisplay
        }
    }

    fun openSettingsSheet() {
        showOverviewServerPopup = false
        showCafeLevelPopup = false
        sheetCafeLevel = cafeLevel
        sheetApNotifyEnabled = apNotifyEnabled
        sheetApNotifyThresholdText = apNotifyThreshold.toString()
        showSettingsSheet = true
    }

    fun closeSettingsSheet() {
        showSettingsSheet = false
        showCafeLevelPopup = false
        sheetCafeLevel = cafeLevel
        sheetApNotifyEnabled = apNotifyEnabled
        sheetApNotifyThresholdText = apNotifyThreshold.toString()
    }

    fun saveSettings() {
        val savedCafeLevel = sheetCafeLevel.coerceIn(1, 10)
        val savedThreshold = sheetApNotifyThresholdText.toIntOrNull()?.coerceIn(0, BA_AP_MAX) ?: 120

        applyCafeStorage()

        BASettingsStore.saveCafeLevel(savedCafeLevel)
        cafeLevel = savedCafeLevel
        clampCafeStoredToCap()

        BASettingsStore.saveApNotifyEnabled(sheetApNotifyEnabled)
        BASettingsStore.saveApNotifyThreshold(savedThreshold)
        apNotifyEnabled = sheetApNotifyEnabled
        apNotifyThreshold = savedThreshold

        applyApRegen()
        showSettingsSheet = false
        showCafeLevelPopup = false
    }

    DisposableEffect(Unit) {
        onDispose { onActionBarInteractingChanged(false) }
    }

    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) listState.animateScrollToItem(0)
    }

    LaunchedEffect(Unit) {
        ensureRegenBase()
        ensureCafeHourBase()
        clampCafeStoredToCap()
        applyCafeStorage()
        applyApRegen()
        while (true) {
            delay(BA_AP_REGEN_TICK_MS)
            applyCafeStorage()
            applyApRegen()
        }
    }

    LaunchedEffect(apCurrent) {
        val target = displayAp(apCurrent).toString()
        if (apCurrentInput != target) apCurrentInput = target
    }

    LaunchedEffect(apLimit) {
        val target = apLimit.toString()
        if (apLimitInput != target) apLimitInput = target
    }

    LaunchedEffect(idNickname) {
        if (idNicknameInput != idNickname) idNicknameInput = idNickname
    }

    LaunchedEffect(idFriendCode) {
        if (idFriendCodeInput != idFriendCode) idFriendCodeInput = idFriendCode
    }

    LaunchedEffect(apCurrent, apNotifyEnabled, apNotifyThreshold) {
        tryApThresholdNotification()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = "BlueArchive",
                    scrollBehavior = scrollBehavior,
                    color = MiuixTheme.colorScheme.surface,
                    actions = {
                        Box {
                            LiquidActionBar(
                                backdrop = backdrop,
                                items = listOf(
                                    LiquidActionItem(
                                        icon = MiuixIcons.Regular.Refresh,
                                        contentDescription = "刷新",
                                        onClick = {
                                            applyCafeStorage()
                                            applyApRegen()
                                        }
                                    ),
                                    LiquidActionItem(
                                        icon = MiuixIcons.Regular.Edit,
                                        contentDescription = "编辑",
                                        onClick = { openSettingsSheet() }
                                    ),
                                    LiquidActionItem(
                                        icon = MiuixIcons.Regular.Copy,
                                        contentDescription = "复制好友码",
                                        onClick = { copyFriendCodeToClipboard() }
                                    )
                                ),
                                onInteractionChanged = onActionBarInteractingChanged
                            )
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            state = listState,
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + contentBottomPadding + 16.dp,
                start = 12.dp,
                end = 12.dp
            )
        ) {
            item { SmallTitle("夏莱办公室") }
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                val isWorkActivated = idFriendCode != BA_DEFAULT_FRIEND_CODE
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                if (initState == BAInitState.Empty) {
                                    initState = BAInitState.Draft
                                }
                            },
                            onLongClick = { initState = BAInitState.Empty }
                        ),
                    colors = CardDefaults.defaultColors(
                        color = if (isWorkActivated) Color(0x333B82F6) else Color(0x33F59E0B),
                        contentColor = MiuixTheme.colorScheme.onBackground
                    ),
                    showIndication = true,
                    onClick = {
                        if (initState == BAInitState.Empty) {
                            initState = BAInitState.Draft
                        }
                    }
                ) {
                    val accentBlue = Color(0xFF3B82F6)
                    val accentGreen = Color(0xFF22C55E)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("AP", color = MiuixTheme.colorScheme.onBackground)
                            StatusPill(
                                label = if (isWorkActivated) "Work" else "Init",
                                color = if (isWorkActivated) Color(0xFF3B82F6) else Color(0xFFF59E0B)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.heightIn(min = 40.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text("服务器", color = MiuixTheme.colorScheme.onBackground)
                            }
                            Box {
                                GlassTextButton(
                                    backdrop = backdrop,
                                    text = serverOptions[serverIndex],
                                    onClick = { showOverviewServerPopup = !showOverviewServerPopup }
                                )
                                if (showOverviewServerPopup) {
                                    WindowListPopup(
                                        show = showOverviewServerPopup,
                                        alignment = PopupPositionProvider.Align.BottomEnd,
                                        onDismissRequest = { showOverviewServerPopup = false },
                                        enableWindowDim = false
                                    ) {
                                        ListPopupColumn {
                                            serverOptions.forEachIndexed { index, server ->
                                                DropdownImpl(
                                                    text = server,
                                                    optionSize = serverOptions.size,
                                                    isSelected = serverIndex == index,
                                                    index = index,
                                                    onSelectedIndexChange = { selected ->
                                                        serverIndex = selected
                                                        BASettingsStore.saveServerIndex(selected)
                                                        showOverviewServerPopup = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.heightIn(min = 44.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text("AP", color = MiuixTheme.colorScheme.onBackground)
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GlassSearchField(
                                    modifier = Modifier.width(72.dp),
                                    value = apCurrentInput,
                                    onValueChange = { input ->
                                        apCurrentInput = input.filter { it.isDigit() }.take(3)
                                    },
                                    onImeActionDone = {
                                        val finalValue = apCurrentInput.toIntOrNull()?.coerceIn(0, BA_AP_MAX) ?: 0
                                        updateCurrentAp(finalValue, markSync = true)
                                        apCurrentInput = finalValue.toString()
                                    },
                                    label = "0",
                                    backdrop = backdrop,
                                    singleLine = true,
                                    textAlign = TextAlign.Center,
                                    fontSize = 18.sp,
                                    textColor = accentGreen
                                )
                                Text("/", color = MiuixTheme.colorScheme.onBackgroundVariant)
                                GlassSearchField(
                                    modifier = Modifier.width(72.dp),
                                    value = apLimitInput,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }.take(3)
                                        if (digits.isBlank()) {
                                            apLimitInput = ""
                                        } else {
                                            val normalized = digits.toIntOrNull()?.coerceIn(0, BA_AP_LIMIT_MAX)
                                            if (normalized != null) {
                                                apLimitInput = normalized.toString()
                                            }
                                        }
                                    },
                                    onImeActionDone = {
                                        val finalValue = apLimitInput.toIntOrNull()?.coerceIn(0, BA_AP_LIMIT_MAX) ?: BA_AP_LIMIT_MAX
                                        updateApLimit(finalValue)
                                        applyApRegen()
                                        apLimitInput = finalValue.toString()
                                    },
                                    label = "240",
                                    backdrop = backdrop,
                                    singleLine = true,
                                    textAlign = TextAlign.Center,
                                    fontSize = 18.sp,
                                    textColor = accentGreen
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.heightIn(min = 40.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text("咖啡厅AP", color = MiuixTheme.colorScheme.onBackground)
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GlassTextButton(
                                    backdrop = backdrop,
                                    text = "领取",
                                    textColor = Color(0xFF22C55E),
                                    onClick = { claimCafeStoredAp() }
                                )
                                GlassTextButton(
                                    backdrop = backdrop,
                                    text = "${cafeStoredAp.roundToInt()}/${cafeDailyCapacity(cafeLevel)}",
                                    textColor = accentGreen,
                                    onClick = {}
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.heightIn(min = 40.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text("AP Sync", color = MiuixTheme.colorScheme.onBackground)
                            }
                            Text(
                                text = formatBaDateTime(apSyncMs),
                                color = accentBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.heightIn(min = 40.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text("AP Full", color = MiuixTheme.colorScheme.onBackground)
                            }
                            Text(
                                text = formatBaDateTime(calculateApFullAtMs()),
                                color = accentBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }

            item {
                val nicknameLengthForWidth = idNicknameInput.ifEmpty { BA_DEFAULT_NICKNAME }.length.coerceIn(1, 10)
                val nicknameFieldWidth = (nicknameLengthForWidth * 11 + 34).coerceIn(72, 124).dp
                val friendCodeLengthForWidth = idFriendCodeInput.ifEmpty { BA_DEFAULT_FRIEND_CODE }.length.coerceIn(1, 8)
                val friendCodeFieldWidth = (friendCodeLengthForWidth * 11 + 34).coerceIn(92, 128).dp
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = Color(0x223B82F6),
                        contentColor = MiuixTheme.colorScheme.onBackground
                    ),
                    pressFeedbackType = PressFeedbackType.Tilt,
                    showIndication = true,
                    onClick = {}
                ) {
                    val accentBlue = Color(0xFF3B82F6)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("ID卡", color = MiuixTheme.colorScheme.onBackground)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.heightIn(min = 44.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text("昵称", color = MiuixTheme.colorScheme.onBackground)
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GlassSearchField(
                                    modifier = Modifier.width(nicknameFieldWidth),
                                    value = idNicknameInput,
                                    onValueChange = { input ->
                                        idNicknameInput = input.take(10)
                                    },
                                    onImeActionDone = { saveIdNicknameFromInput() },
                                    label = "Kei",
                                    backdrop = backdrop,
                                    singleLine = true,
                                    textAlign = TextAlign.Center
                                )
                                Text("老师", color = accentBlue)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.heightIn(min = 44.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text("好友码", color = MiuixTheme.colorScheme.onBackground)
                            }
                            GlassSearchField(
                                modifier = Modifier.width(friendCodeFieldWidth),
                                value = idFriendCodeInput,
                                onValueChange = { input ->
                                    idFriendCodeInput = input
                                        .uppercase(Locale.ROOT)
                                        .filter { it in 'A'..'Z' }
                                        .take(8)
                                },
                                onImeActionDone = { saveIdFriendCodeFromInput() },
                                label = "ARISUKEI",
                                backdrop = backdrop,
                                singleLine = true,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = Color(0x223B82F6),
                        contentColor = MiuixTheme.colorScheme.onBackground
                    ),
                    pressFeedbackType = PressFeedbackType.Tilt,
                    showIndication = true,
                    onClick = {}
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Test", color = MiuixTheme.colorScheme.onBackground)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassTextButton(
                                backdrop = backdrop,
                                text = "AP通知",
                                onClick = { sendApTestNotification(showToast = true) }
                            )
                            GlassTextButton(
                                backdrop = backdrop,
                                text = "咖啡厅3h AP",
                                onClick = { testCafePlus3Hours() }
                            )
                        }
                    }
                }
            }
        }
    }

    WindowBottomSheet(
        show = showSettingsSheet,
        title = "BA 配置",
        onDismissRequest = { closeSettingsSheet() },
        startAction = {
            GlassIconButton(
                backdrop = backdrop,
                icon = MiuixIcons.Regular.Close,
                contentDescription = "关闭",
                onClick = { closeSettingsSheet() }
            )
        },
        endAction = {
            GlassIconButton(
                backdrop = backdrop,
                icon = MiuixIcons.Regular.Ok,
                contentDescription = "保存",
                onClick = { saveSettings() }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "咖啡厅等级",
                    color = MiuixTheme.colorScheme.onBackground
                )
                Box {
                    GlassTextButton(
                        backdrop = backdrop,
                        text = "${sheetCafeLevel}级",
                        onClick = { showCafeLevelPopup = !showCafeLevelPopup }
                    )
                    if (showCafeLevelPopup) {
                        WindowListPopup(
                            show = showCafeLevelPopup,
                            alignment = PopupPositionProvider.Align.BottomEnd,
                            onDismissRequest = { showCafeLevelPopup = false },
                            enableWindowDim = false
                        ) {
                            ListPopupColumn {
                                cafeLevelOptions.forEachIndexed { index, level ->
                                    DropdownImpl(
                                        text = "${level}级",
                                        optionSize = cafeLevelOptions.size,
                                        isSelected = sheetCafeLevel == level,
                                        index = index,
                                        onSelectedIndexChange = { selected ->
                                            sheetCafeLevel = cafeLevelOptions[selected]
                                            showCafeLevelPopup = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AP通知",
                    color = MiuixTheme.colorScheme.onBackground
                )
                Switch(
                    checked = sheetApNotifyEnabled,
                    onCheckedChange = { checked -> sheetApNotifyEnabled = checked }
                )
            }

            if (sheetApNotifyEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "提醒阈值",
                        color = MiuixTheme.colorScheme.onBackground
                    )
                    GlassSearchField(
                        modifier = Modifier.width(70.dp),
                        value = sheetApNotifyThresholdText,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }.take(3)
                            if (digits.isBlank()) {
                                sheetApNotifyThresholdText = ""
                            } else {
                                val normalized = digits.toIntOrNull()?.coerceIn(0, BA_AP_MAX)
                                sheetApNotifyThresholdText = normalized?.toString() ?: ""
                            }
                        },
                        onImeActionDone = {
                            val normalized = sheetApNotifyThresholdText.toIntOrNull()?.coerceIn(0, BA_AP_MAX) ?: 120
                            sheetApNotifyThresholdText = normalized.toString()
                        },
                        label = "120",
                        backdrop = backdrop,
                        singleLine = true,
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                        textColor = Color(0xFF22C55E)
                    )
                }
            }

            Text(
                text = "不同服务器时区不同，建议按实际游玩服务器设置。",
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
