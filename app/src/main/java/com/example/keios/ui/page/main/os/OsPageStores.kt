package com.example.keios.ui.page.main

import android.app.ActivityManager
import android.content.Context
import android.content.pm.FeatureInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.LiquidActionBar
import com.example.keios.ui.page.main.widget.LiquidActionItem
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassSearchField
import com.example.keios.ui.page.main.widget.MiuixExpandableSection
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.SnapshotWindowBottomSheet
import com.example.keios.ui.page.main.widget.StatusPill
import com.example.keios.core.system.ShizukuApiUtils
import com.example.keios.core.system.getAllJavaPropString
import com.example.keios.core.system.getAllSystemProperties
import com.rosan.installer.ui.library.effect.getMiuixAppBarColor
import com.rosan.installer.ui.library.effect.rememberMiuixBlurBackdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.theme.MiuixTheme
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object OsInfoCache {
    private const val KV_ID = "os_info_cache"
    private const val LEGACY_KV_ID = "system_info_cache"

    private const val KEY_OS_SYSTEM = "section_os_system_table"
    private const val KEY_OS_SECURE = "section_os_secure_table"
    private const val KEY_OS_GLOBAL = "section_os_global_table"
    private const val KEY_OS_ANDROID = "section_os_android_properties"
    private const val KEY_OS_JAVA = "section_os_java_properties"
    private const val KEY_OS_LINUX = "section_os_linux_environment"

    private const val LEGACY_KEY_SYSTEM = "section_system_table"
    private const val LEGACY_KEY_SECURE = "section_secure_table"
    private const val LEGACY_KEY_GLOBAL = "section_global_table"
    private const val LEGACY_KEY_ANDROID = "section_android_properties"
    private const val LEGACY_KEY_JAVA = "section_java_properties"
    private const val LEGACY_KEY_LINUX = "section_linux_environment"
    private val store: MMKV by lazy { MMKV.mmkvWithID(KV_ID) }
    private val legacyStore: MMKV by lazy { MMKV.mmkvWithID(LEGACY_KV_ID) }

    private fun encodeRows(rows: List<InfoRow>): String {
        return rows.joinToString("\n") { row ->
            "${Uri.encode(row.key)}\t${Uri.encode(row.value)}"
        }
    }

    private fun decodeRows(raw: String?): List<InfoRow> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.lineSequence().mapNotNull { line ->
            val index = line.indexOf('\t')
            if (index <= 0) return@mapNotNull null
            val key = Uri.decode(line.substring(0, index))
            val value = Uri.decode(line.substring(index + 1))
            InfoRow(key, value)
        }.toList()
    }

    private fun readRaw(newKv: MMKV, legacyKv: MMKV, newKey: String, legacyKey: String): String? {
        val newRaw = newKv.decodeString(newKey)
        if (!newRaw.isNullOrBlank()) return newRaw
        return legacyKv.decodeString(legacyKey)
    }

    private fun hasAllKeys(kv: MMKV, keys: List<String>): Boolean {
        return keys.all { kv.containsKey(it) }
    }

    fun read(visibleSections: Set<SectionKind> = SectionKind.entries.toSet()): CachedSections {
        val kv = store
        val legacyKv = legacyStore
        return CachedSections(
            system = if (visibleSections.contains(SectionKind.SYSTEM)) decodeRows(readRaw(kv, legacyKv, KEY_OS_SYSTEM, LEGACY_KEY_SYSTEM)) else emptyList(),
            secure = if (visibleSections.contains(SectionKind.SECURE)) decodeRows(readRaw(kv, legacyKv, KEY_OS_SECURE, LEGACY_KEY_SECURE)) else emptyList(),
            global = if (visibleSections.contains(SectionKind.GLOBAL)) decodeRows(readRaw(kv, legacyKv, KEY_OS_GLOBAL, LEGACY_KEY_GLOBAL)) else emptyList(),
            android = if (visibleSections.contains(SectionKind.ANDROID)) decodeRows(readRaw(kv, legacyKv, KEY_OS_ANDROID, LEGACY_KEY_ANDROID)) else emptyList(),
            java = if (visibleSections.contains(SectionKind.JAVA)) decodeRows(readRaw(kv, legacyKv, KEY_OS_JAVA, LEGACY_KEY_JAVA)) else emptyList(),
            linux = if (visibleSections.contains(SectionKind.LINUX)) decodeRows(readRaw(kv, legacyKv, KEY_OS_LINUX, LEGACY_KEY_LINUX)) else emptyList()
        )
    }

    fun write(section: SectionKind, rows: List<InfoRow>) {
        val kv = store
        when (section) {
            SectionKind.SYSTEM -> kv.encode(KEY_OS_SYSTEM, encodeRows(rows))
            SectionKind.SECURE -> kv.encode(KEY_OS_SECURE, encodeRows(rows))
            SectionKind.GLOBAL -> kv.encode(KEY_OS_GLOBAL, encodeRows(rows))
            SectionKind.ANDROID -> kv.encode(KEY_OS_ANDROID, encodeRows(rows))
            SectionKind.JAVA -> kv.encode(KEY_OS_JAVA, encodeRows(rows))
            SectionKind.LINUX -> kv.encode(KEY_OS_LINUX, encodeRows(rows))
        }
    }

    fun clear(section: SectionKind) {
        val kv = store
        val legacyKv = legacyStore
        when (section) {
            SectionKind.SYSTEM -> {
                kv.removeValueForKey(KEY_OS_SYSTEM)
                legacyKv.removeValueForKey(LEGACY_KEY_SYSTEM)
            }
            SectionKind.SECURE -> {
                kv.removeValueForKey(KEY_OS_SECURE)
                legacyKv.removeValueForKey(LEGACY_KEY_SECURE)
            }
            SectionKind.GLOBAL -> {
                kv.removeValueForKey(KEY_OS_GLOBAL)
                legacyKv.removeValueForKey(LEGACY_KEY_GLOBAL)
            }
            SectionKind.ANDROID -> {
                kv.removeValueForKey(KEY_OS_ANDROID)
                legacyKv.removeValueForKey(LEGACY_KEY_ANDROID)
            }
            SectionKind.JAVA -> {
                kv.removeValueForKey(KEY_OS_JAVA)
                legacyKv.removeValueForKey(LEGACY_KEY_JAVA)
            }
            SectionKind.LINUX -> {
                kv.removeValueForKey(KEY_OS_LINUX)
                legacyKv.removeValueForKey(LEGACY_KEY_LINUX)
            }
        }
    }

    fun readSnapshot(visibleSections: Set<SectionKind> = SectionKind.entries.toSet()): CachedSectionsSnapshot {
        if (visibleSections.isEmpty()) return CachedSectionsSnapshot()
        val cached = read(visibleSections)
        val hasPersistedCache = visibleSections.any { section ->
            when (section) {
                SectionKind.SYSTEM -> cached.system.isNotEmpty()
                SectionKind.SECURE -> cached.secure.isNotEmpty()
                SectionKind.GLOBAL -> cached.global.isNotEmpty()
                SectionKind.ANDROID -> cached.android.isNotEmpty()
                SectionKind.JAVA -> cached.java.isNotEmpty()
                SectionKind.LINUX -> cached.linux.isNotEmpty()
            }
        }
        return CachedSectionsSnapshot(
            cached = cached,
            hasPersistedCache = hasPersistedCache
        )
    }

    fun hasPersistedCache(visibleSections: Set<SectionKind>): Boolean {
        return readSnapshot(visibleSections).hasPersistedCache
    }

    fun clearAll() {
        SectionKind.entries.forEach(::clear)
        store.trim()
        legacyStore.trim()
    }

    fun storageFootprintBytes(): Long = store.totalSize() + legacyStore.totalSize()

    fun actualDataBytes(): Long = store.actualSize() + legacyStore.actualSize()

    fun cacheBytesEstimated(): Long {
        val snapshot = read()
        return snapshot.system.sumOf { (it.key.length + it.value.length).toLong() * 2 + 8L } +
            snapshot.secure.sumOf { (it.key.length + it.value.length).toLong() * 2 + 8L } +
            snapshot.global.sumOf { (it.key.length + it.value.length).toLong() * 2 + 8L } +
            snapshot.android.sumOf { (it.key.length + it.value.length).toLong() * 2 + 8L } +
            snapshot.java.sumOf { (it.key.length + it.value.length).toLong() * 2 + 8L } +
            snapshot.linux.sumOf { (it.key.length + it.value.length).toLong() * 2 + 8L }
    }

    fun cachedSectionCount(visibleCards: Set<OsSectionCard>): Int {
        val visibleSections = buildSet {
            if (visibleCards.contains(OsSectionCard.SYSTEM)) add(SectionKind.SYSTEM)
            if (visibleCards.contains(OsSectionCard.SECURE)) add(SectionKind.SECURE)
            if (visibleCards.contains(OsSectionCard.GLOBAL)) add(SectionKind.GLOBAL)
            if (visibleCards.contains(OsSectionCard.ANDROID)) add(SectionKind.ANDROID)
            if (visibleCards.contains(OsSectionCard.JAVA)) add(SectionKind.JAVA)
            if (visibleCards.contains(OsSectionCard.LINUX)) add(SectionKind.LINUX)
        }
        val cached = read(visibleSections)
        var count = 0
        if (cached.system.isNotEmpty()) count++
        if (cached.secure.isNotEmpty()) count++
        if (cached.global.isNotEmpty()) count++
        if (cached.android.isNotEmpty()) count++
        if (cached.java.isNotEmpty()) count++
        if (cached.linux.isNotEmpty()) count++
        return count
    }

    fun hasPersistedCache(): Boolean {
        val kv = store
        val legacyKv = legacyStore
        val hasNew = hasAllKeys(
            kv,
            listOf(KEY_OS_SYSTEM, KEY_OS_SECURE, KEY_OS_GLOBAL, KEY_OS_ANDROID, KEY_OS_JAVA, KEY_OS_LINUX)
        )
        val hasLegacy = hasAllKeys(
            legacyKv,
            listOf(
                LEGACY_KEY_SYSTEM,
                LEGACY_KEY_SECURE,
                LEGACY_KEY_GLOBAL,
                LEGACY_KEY_ANDROID,
                LEGACY_KEY_JAVA,
                LEGACY_KEY_LINUX
            )
        )
        return hasNew || hasLegacy
    }
}

internal object OsCardVisibilityStore {
    private const val KV_ID = "os_ui_state"
    private const val KEY_VISIBLE_CARDS = "visible_os_cards"
    private val DEFAULT_VISIBLE = OsSectionCard.entries.map { it.name }.toSet()
    private val store: MMKV by lazy { MMKV.mmkvWithID(KV_ID) }

    private fun resolveVisibleCards(raw: String): Set<OsSectionCard> {
        if (raw.isBlank()) return emptySet()
        val names = raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        val resolved = OsSectionCard.entries.filter { names.contains(it.name) }.toSet()
        return if (resolved.isEmpty() && names == DEFAULT_VISIBLE) OsSectionCard.entries.toSet() else resolved
    }

    fun loadVisibleCards(kv: MMKV = store): Set<OsSectionCard> {
        if (!kv.containsKey(KEY_VISIBLE_CARDS)) return OsSectionCard.entries.toSet()
        return resolveVisibleCards(kv.decodeString(KEY_VISIBLE_CARDS, "").orEmpty())
    }

    fun saveVisibleCards(cards: Set<OsSectionCard>) {
        store.encode(
            KEY_VISIBLE_CARDS,
            cards.map { it.name }.sorted().joinToString(",")
        )
    }
}

internal object OsUiStateStore {
    private const val KV_ID = "os_ui_state"
    private const val LEGACY_KV_ID = "system_ui_state"

    private const val KEY_OVERVIEW = "expanded_os_overview"
    private const val KEY_TOP_INFO = "expanded_os_top_info"
    private const val KEY_SHELL_RUNNER = "expanded_os_shell_runner"
    private const val KEY_GOOGLE_SYSTEM_SERVICE = "expanded_os_google_system_service"
    private const val KEY_SYSTEM_TABLE = "expanded_os_system_table"
    private const val KEY_SECURE_TABLE = "expanded_os_secure_table"
    private const val KEY_GLOBAL_TABLE = "expanded_os_global_table"
    private const val KEY_ANDROID_PROPS = "expanded_os_android_props"
    private const val KEY_JAVA_PROPS = "expanded_os_java_props"
    private const val KEY_LINUX_ENV = "expanded_os_linux_env"

    private const val LEGACY_KEY_OVERVIEW = "expanded_overview"
    private const val LEGACY_KEY_TOP_INFO = "expanded_top_info"
    private const val LEGACY_KEY_SHELL_RUNNER = "expanded_shell_runner"
    private const val LEGACY_KEY_GOOGLE_SYSTEM_SERVICE = "expanded_google_system_service"
    private const val LEGACY_KEY_SYSTEM_TABLE = "expanded_system_table"
    private const val LEGACY_KEY_SECURE_TABLE = "expanded_secure_table"
    private const val LEGACY_KEY_GLOBAL_TABLE = "expanded_global_table"
    private const val LEGACY_KEY_ANDROID_PROPS = "expanded_android_props"
    private const val LEGACY_KEY_JAVA_PROPS = "expanded_java_props"
    private const val LEGACY_KEY_LINUX_ENV = "expanded_linux_env"
    private val store: MMKV by lazy { MMKV.mmkvWithID(KV_ID) }
    private val legacyStore: MMKV by lazy { MMKV.mmkvWithID(LEGACY_KV_ID) }

    private fun readBool(kv: MMKV, legacyKv: MMKV, key: String, legacyKey: String, defaultValue: Boolean): Boolean {
        if (kv.containsKey(key)) return kv.decodeBool(key, defaultValue)
        return legacyKv.decodeBool(legacyKey, defaultValue)
    }

    private fun readBool(key: String, legacyKey: String, defaultValue: Boolean): Boolean {
        val kv = store
        val legacyKv = legacyStore
        return readBool(kv, legacyKv, key, legacyKey, defaultValue)
    }

    fun loadSnapshot(): OsUiSnapshot {
        val kv = store
        val legacyKv = legacyStore
        return OsUiSnapshot(
            topInfoExpanded = readBool(kv, legacyKv, KEY_TOP_INFO, LEGACY_KEY_TOP_INFO, true),
            shellRunnerExpanded = readBool(
                kv,
                legacyKv,
                KEY_SHELL_RUNNER,
                LEGACY_KEY_SHELL_RUNNER,
                false
            ),
            googleSystemServiceExpanded = readBool(
                kv,
                legacyKv,
                KEY_GOOGLE_SYSTEM_SERVICE,
                LEGACY_KEY_GOOGLE_SYSTEM_SERVICE,
                false
            ),
            systemTableExpanded = readBool(kv, legacyKv, KEY_SYSTEM_TABLE, LEGACY_KEY_SYSTEM_TABLE, false),
            secureTableExpanded = readBool(kv, legacyKv, KEY_SECURE_TABLE, LEGACY_KEY_SECURE_TABLE, false),
            globalTableExpanded = readBool(kv, legacyKv, KEY_GLOBAL_TABLE, LEGACY_KEY_GLOBAL_TABLE, false),
            androidPropsExpanded = readBool(kv, legacyKv, KEY_ANDROID_PROPS, LEGACY_KEY_ANDROID_PROPS, false),
            javaPropsExpanded = readBool(kv, legacyKv, KEY_JAVA_PROPS, LEGACY_KEY_JAVA_PROPS, false),
            linuxEnvExpanded = readBool(kv, legacyKv, KEY_LINUX_ENV, LEGACY_KEY_LINUX_ENV, false),
            visibleCards = OsCardVisibilityStore.loadVisibleCards(kv)
        )
    }

    fun topInfoExpanded(defaultValue: Boolean = true): Boolean =
        readBool(KEY_TOP_INFO, LEGACY_KEY_TOP_INFO, defaultValue)

    fun overviewExpanded(defaultValue: Boolean = true): Boolean =
        readBool(KEY_OVERVIEW, LEGACY_KEY_OVERVIEW, defaultValue)

    fun shellRunnerExpanded(defaultValue: Boolean = false): Boolean =
        readBool(KEY_SHELL_RUNNER, LEGACY_KEY_SHELL_RUNNER, defaultValue)

    fun osSystemTableExpanded(defaultValue: Boolean = false): Boolean =
        readBool(KEY_SYSTEM_TABLE, LEGACY_KEY_SYSTEM_TABLE, defaultValue)

    fun googleSystemServiceExpanded(defaultValue: Boolean = false): Boolean =
        readBool(KEY_GOOGLE_SYSTEM_SERVICE, LEGACY_KEY_GOOGLE_SYSTEM_SERVICE, defaultValue)

    fun secureTableExpanded(defaultValue: Boolean = false): Boolean =
        readBool(KEY_SECURE_TABLE, LEGACY_KEY_SECURE_TABLE, defaultValue)

    fun globalTableExpanded(defaultValue: Boolean = false): Boolean =
        readBool(KEY_GLOBAL_TABLE, LEGACY_KEY_GLOBAL_TABLE, defaultValue)

    fun androidPropsExpanded(defaultValue: Boolean = false): Boolean =
        readBool(KEY_ANDROID_PROPS, LEGACY_KEY_ANDROID_PROPS, defaultValue)

    fun javaPropsExpanded(defaultValue: Boolean = false): Boolean =
        readBool(KEY_JAVA_PROPS, LEGACY_KEY_JAVA_PROPS, defaultValue)

    fun linuxEnvExpanded(defaultValue: Boolean = false): Boolean =
        readBool(KEY_LINUX_ENV, LEGACY_KEY_LINUX_ENV, defaultValue)

    fun setTopInfoExpanded(value: Boolean) {
        store.encode(KEY_TOP_INFO, value)
    }

    fun setOverviewExpanded(value: Boolean) {
        store.encode(KEY_OVERVIEW, value)
    }

    fun setShellRunnerExpanded(value: Boolean) {
        store.encode(KEY_SHELL_RUNNER, value)
    }

    fun setOsSystemTableExpanded(value: Boolean) {
        store.encode(KEY_SYSTEM_TABLE, value)
    }

    fun setGoogleSystemServiceExpanded(value: Boolean) {
        store.encode(KEY_GOOGLE_SYSTEM_SERVICE, value)
    }

    fun setSecureTableExpanded(value: Boolean) {
        store.encode(KEY_SECURE_TABLE, value)
    }

    fun setGlobalTableExpanded(value: Boolean) {
        store.encode(KEY_GLOBAL_TABLE, value)
    }

    fun setAndroidPropsExpanded(value: Boolean) {
        store.encode(KEY_ANDROID_PROPS, value)
    }

    fun setJavaPropsExpanded(value: Boolean) {
        store.encode(KEY_JAVA_PROPS, value)
    }

    fun setLinuxEnvExpanded(value: Boolean) {
        store.encode(KEY_LINUX_ENV, value)
    }

    fun storageFootprintBytes(): Long = store.totalSize() + legacyStore.totalSize()

    fun actualDataBytes(): Long = store.actualSize() + legacyStore.actualSize()

    fun configBytesEstimated(): Long {
        val snapshot = loadSnapshot()
        val boolBytes = 8L
        val cardBytes = snapshot.visibleCards.sumOf { it.name.length.toLong() * 2 + 4L }
        return boolBytes * 9 + cardBytes
    }
}

internal data class OsShellCommandSnapshot(
    val command: String = "",
    val savedAtMillis: Long = 0L
)

internal object OsShellCommandStore {
    private const val KV_ID = "os_ui_state"
    private const val KEY_SHELL_COMMAND = "shell_runner_saved_command"
    private const val KEY_SHELL_COMMAND_SAVED_AT = "shell_runner_saved_command_saved_at"
    private val store: MMKV by lazy { MMKV.mmkvWithID(KV_ID) }

    fun loadSnapshot(): OsShellCommandSnapshot {
        val command = store.decodeString(KEY_SHELL_COMMAND).orEmpty().trim()
        val savedAtMillis = store.decodeLong(KEY_SHELL_COMMAND_SAVED_AT, 0L)
        if (command.isBlank()) return OsShellCommandSnapshot()
        return OsShellCommandSnapshot(
            command = command,
            savedAtMillis = savedAtMillis
        )
    }

    fun saveCommand(command: String): OsShellCommandSnapshot {
        val normalized = command.trim()
        if (normalized.isBlank()) {
            clear()
            return OsShellCommandSnapshot()
        }
        val savedAtMillis = System.currentTimeMillis()
        store.encode(KEY_SHELL_COMMAND, normalized)
        store.encode(KEY_SHELL_COMMAND_SAVED_AT, savedAtMillis)
        return OsShellCommandSnapshot(
            command = normalized,
            savedAtMillis = savedAtMillis
        )
    }

    fun clear() {
        store.removeValueForKey(KEY_SHELL_COMMAND)
        store.removeValueForKey(KEY_SHELL_COMMAND_SAVED_AT)
    }
}

internal object OsShortcutCardStore {
    private const val KV_ID = "os_ui_state"
    private const val KEY_GOOGLE_SYSTEM_SERVICE_TITLE = "google_system_service_title"
    private const val KEY_GOOGLE_SYSTEM_SERVICE_SUBTITLE = "google_system_service_subtitle"
    private const val KEY_GOOGLE_SYSTEM_SERVICE_APP_NAME = "google_system_service_app_name"
    private const val KEY_GOOGLE_SYSTEM_SERVICE_PACKAGE = "google_system_service_package"
    private const val KEY_GOOGLE_SYSTEM_SERVICE_CLASS = "google_system_service_class"
    private const val KEY_GOOGLE_SYSTEM_SERVICE_ACTION = "google_system_service_action"
    private const val KEY_GOOGLE_SYSTEM_SERVICE_CATEGORY = "google_system_service_category"
    private const val KEY_GOOGLE_SYSTEM_SERVICE_FLAGS = "google_system_service_flags"
    private const val KEY_GOOGLE_SYSTEM_SERVICE_URI_DATA = "google_system_service_uri_data"
    private const val KEY_GOOGLE_SYSTEM_SERVICE_DATA = "google_system_service_data"
    private const val KEY_GOOGLE_SYSTEM_SERVICE_MIME_TYPE = "google_system_service_mime_type"
    private const val KEY_GOOGLE_SYSTEM_SERVICE_INTENT_EXTRAS = "google_system_service_intent_extras"
    private const val KEY_EXTRA_KEY = "key"
    private const val KEY_EXTRA_TYPE = "type"
    private const val KEY_EXTRA_VALUE = "value"
    private val store: MMKV by lazy { MMKV.mmkvWithID(KV_ID) }

    fun loadGoogleSystemServiceConfig(
        defaults: OsGoogleSystemServiceConfig = OsGoogleSystemServiceConfig()
    ): OsGoogleSystemServiceConfig {
        return OsGoogleSystemServiceConfig(
            title = store.decodeString(KEY_GOOGLE_SYSTEM_SERVICE_TITLE, defaults.title).orEmpty(),
            subtitle = store.decodeString(KEY_GOOGLE_SYSTEM_SERVICE_SUBTITLE, defaults.subtitle).orEmpty(),
            appName = store.decodeString(KEY_GOOGLE_SYSTEM_SERVICE_APP_NAME, defaults.appName).orEmpty(),
            packageName = store.decodeString(KEY_GOOGLE_SYSTEM_SERVICE_PACKAGE, defaults.packageName).orEmpty(),
            className = store.decodeString(KEY_GOOGLE_SYSTEM_SERVICE_CLASS, defaults.className).orEmpty(),
            intentAction = store.decodeString(KEY_GOOGLE_SYSTEM_SERVICE_ACTION, defaults.intentAction).orEmpty(),
            intentCategory = store.decodeString(
                KEY_GOOGLE_SYSTEM_SERVICE_CATEGORY,
                defaults.intentCategory
            ).orEmpty(),
            intentFlags = store.decodeString(KEY_GOOGLE_SYSTEM_SERVICE_FLAGS, defaults.intentFlags).orEmpty(),
            intentUriData = store.decodeString(KEY_GOOGLE_SYSTEM_SERVICE_URI_DATA)
                ?: store.decodeString(KEY_GOOGLE_SYSTEM_SERVICE_DATA, defaults.intentUriData).orEmpty(),
            intentMimeType = store.decodeString(
                KEY_GOOGLE_SYSTEM_SERVICE_MIME_TYPE,
                defaults.intentMimeType
            ).orEmpty(),
            intentExtras = decodeIntentExtras(
                store.decodeString(KEY_GOOGLE_SYSTEM_SERVICE_INTENT_EXTRAS)
            )
        ).normalized(defaults)
    }

    fun saveGoogleSystemServiceConfig(
        config: OsGoogleSystemServiceConfig,
        defaults: OsGoogleSystemServiceConfig = OsGoogleSystemServiceConfig()
    ) {
        val normalized = config.normalized(defaults)
        store.encode(KEY_GOOGLE_SYSTEM_SERVICE_TITLE, normalized.title)
        store.encode(KEY_GOOGLE_SYSTEM_SERVICE_SUBTITLE, normalized.subtitle)
        store.encode(KEY_GOOGLE_SYSTEM_SERVICE_APP_NAME, normalized.appName)
        store.encode(KEY_GOOGLE_SYSTEM_SERVICE_PACKAGE, normalized.packageName)
        store.encode(KEY_GOOGLE_SYSTEM_SERVICE_CLASS, normalized.className)
        store.encode(KEY_GOOGLE_SYSTEM_SERVICE_ACTION, normalized.intentAction)
        store.encode(KEY_GOOGLE_SYSTEM_SERVICE_CATEGORY, normalized.intentCategory)
        store.encode(KEY_GOOGLE_SYSTEM_SERVICE_FLAGS, normalized.intentFlags)
        store.encode(KEY_GOOGLE_SYSTEM_SERVICE_URI_DATA, normalized.intentUriData)
        store.encode(KEY_GOOGLE_SYSTEM_SERVICE_DATA, normalized.intentUriData)
        store.encode(KEY_GOOGLE_SYSTEM_SERVICE_MIME_TYPE, normalized.intentMimeType)
        store.encode(
            KEY_GOOGLE_SYSTEM_SERVICE_INTENT_EXTRAS,
            encodeIntentExtras(normalized.intentExtras)
        )
    }

    private fun encodeIntentExtras(extras: List<ShortcutIntentExtra>): String {
        val normalized = normalizeShortcutIntentExtras(extras)
        val array = JSONArray()
        normalized.forEach { extra ->
            array.put(
                JSONObject().apply {
                    put(KEY_EXTRA_KEY, extra.key)
                    put(KEY_EXTRA_TYPE, extra.type.rawValue)
                    put(KEY_EXTRA_VALUE, extra.value)
                }
            )
        }
        return array.toString()
    }

    private fun decodeIntentExtras(raw: String?): List<ShortcutIntentExtra> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(
                        ShortcutIntentExtra(
                            key = item.optString(KEY_EXTRA_KEY),
                            type = ShortcutIntentExtraType.fromRaw(item.optString(KEY_EXTRA_TYPE)),
                            value = item.optString(KEY_EXTRA_VALUE)
                        )
                    )
                }
            }.let(::normalizeShortcutIntentExtras)
        }.getOrDefault(emptyList())
    }
}
