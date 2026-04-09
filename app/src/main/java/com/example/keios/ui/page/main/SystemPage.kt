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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.LiquidActionBar
import com.example.keios.ui.page.main.widget.LiquidActionItem
import com.example.keios.ui.page.main.widget.GlassSearchField
import com.example.keios.ui.page.main.widget.MiuixExpandableSection
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.StatusPill
import com.example.keios.ui.utils.ShizukuApiUtils
import com.example.keios.ui.utils.getAllJavaPropString
import com.example.keios.ui.utils.getAllSystemProperties
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
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class InfoRow(
    val key: String,
    val value: String
)

private enum class SectionKind {
    SYSTEM,
    SECURE,
    GLOBAL,
    ANDROID,
    JAVA,
    LINUX
}

private enum class SystemOverviewState {
    Idle,
    Cached,
    Refreshing,
    Completed
}

private data class SectionState(
    val rows: List<InfoRow> = emptyList(),
    val loading: Boolean = false,
    val loadedFresh: Boolean = false
)

private data class CachedSections(
    val system: List<InfoRow> = emptyList(),
    val secure: List<InfoRow> = emptyList(),
    val global: List<InfoRow> = emptyList(),
    val android: List<InfoRow> = emptyList(),
    val java: List<InfoRow> = emptyList(),
    val linux: List<InfoRow> = emptyList()
)

private object TopInfoKeys {
    val system = linkedSetOf(
        "locked_apps",
        "long_press_camera_key",
        "long_press_home_key",
        "long_press_power_key"
    )

    val secure = linkedSetOf(
        "AIRPODS_ADAPTER_JAR_ENABLE",
        "FBO_STATE_OPEN",
        "FBO_UPLOAD_LIST",
        "FBO_UPLOAD_TIME",
        "fbo_app_size",
        "fbo_remaining_minutes",
        "fbo_status",
        "KEY_FBO_DATA",
        "aod_category_name",
        "aod_mode_user_set",
        "aod_show_style",
        "aod_style_state",
        "assistant",
        "auto_download",
        "auto_update",
        "autofill_user_data_max_category_count",
        "backup_transport",
        "bluetooth_name",
        "customize_icon_init_time",
        "enabled_accessibility_services",
        "enabled_input_methods",
        "entity_config_key_voice_assistant",
        "share_camera",
        "share_launcher",
        "share_unlock",
        "theme_rear_widget",
        "voice_interaction_service",
        "voice_recognition_service",
        "package_verifier_state",
        "packageinstaller_first_boot_time",
        "pc_security_center_last_fully_charge_time",
        "autofill_service",
        "autofill_service_search_uri",
        "credential_service",
        "credential_service_primary"
    )

    val global = linkedSetOf(
        "adb_allowrd_connection_time",
        "adb_enabled",
        "adb_notification_shown",
        "adb_wifi_enabled",
        "database_creation_buildid",
        "device_name",
        "effect_implementer",
        "is_auto_update",
        "is_beta",
        "key_last_update_timestamp_0",
        "key_miui_font_weight_scale",
        "lastVersion",
        "lastVersionProjection",
        "last_screenshot_time",
        "lc3Enable",
        "lea_device_status",
        "mi_bt_plugin_consume_traffic_month",
        "mi_cloud_data_fetch_time",
        "mi_fastconnect_consume_traffic_day",
        "mi_fastconnect_consume_traffic_month",
        "mi_fc_last_sync_data_time",
        "mi_flashlight_strength",
        "mi_is_temp_high_close_torch",
        "miui_current_version_branch",
        "miui_is_last_auto_update",
        "miui_memory_size",
        "miui_nearby_download_device_time",
        "miui_phone_rssi_threshold",
        "miui_pre_big_miui_version",
        "miui_pre_codebase",
        "miui_pre_version",
        "miui_process_exit_falg",
        "miui_ram_size",
        "miui_terms_agreed_time",
        "miui_update_ready",
        "miui_version_name",
        "network_watchlist_last_report_time",
        "sc_last_check_dm_time_0",
        "task_stack_view_layout_style",
        "upload_apk_enable",
        "usb_mass_storage_enabled",
        "xiaomi_mi_play_last_playing_package_name",
        "zram_enabled"
    )

    val android = linkedSetOf(
        "graphics.opengl.es",
        "graphics.opengl.es.raw",
        "graphics.opengl.es.decoded",
        "graphics.vulkan.version",
        "graphics.vulkan.version.raw",
        "graphics.vulkan.level",
        "graphics.gpu.driver.package",
        "graphics.gpu.driver.version",
        "graphics.gpu.driver.ro.gfx.driver.0",
        "graphics.gpu.driver.ro.gfx.driver.1",
        "prop.ro.hardware.egl",
        "prop.ro.hardware.vulkan",
        "feature.bluetooth_le",
        "feature.wifi_aware",
        "dalvik.vm.appimageformat",
        "dalvik.vm.background-dex2oat-threads",
        "dalvik.vm.boot-dex2oat-threads",
        "dalvik.vm.dex2oat-Xms",
        "dalvik.vm.dex2oat-Xmx",
        "dalvik.vm.dex2oat-max-image-block-size",
        "dalvik.vm.dex2oat-resolve-startup-strings",
        "dalvik.vm.dex2oat-threads",
        "dalvik.vm.dex2oat64.enabled",
        "dalvik.vm.dexopt.secondary",
        "dalvik.vm.dexopt.thermal-cutoff",
        "dalvik.vm.enable_pr_dexopt",
        "dalvik.vm.isa.arm64.variant",
        "dalvik.vm.usejit",
        "debug.device.battery_level_state",
        "debug.device.usb_state",
        "debug.hwui.skia_atrace_enabled",
        "debug.tracing.screen_brightness",
        "gsm.network.type",
        "gsm.operator.alpha",
        "gsm.operator.orig.alpha",
        "gsm.sim.state",
        "gsm.version.baseband",
        "persist.audio.effect.device_map",
        "persist.radio.modem_build_datetime",
        "persist.radio.modem_release_datetime",
        "persist.sys.computility.cpulevel",
        "persist.sys.computility.gpulevel",
        "persist.sys.computility.version",
        "persist.sys.device_config_gki",
        "persist.sys.grant_version",
        "persist.sys.locale",
        "persist.sys.memory.totalsize",
        "persist.sys.memory.user_freesize",
        "persist.sys.misight.detecttime",
        "persist.sys.miui_art_ota_time",
        "persist.sys.miui_gnss_dc",
        "persist.sys.miui_resolution",
        "persist.sys.updater.version",
        "persist.sys.usb.config",
        "persist.sys.xms.version",
        "persist.sys.xring_extm.enable",
        "persist.sys.zygote.last_pid",
        "persist.sys.zygote.start_pid",
        "persist.vendor.audio.effectimplenter",
        "pm.dexopt.ab-ota",
        "pm.dexopt.baseline",
        "pm.dexopt.bg-dexopt",
        "pm.dexopt.boot-after-mainline-update",
        "pm.dexopt.boot-after-ota",
        "pm.dexopt.boot-after-ota.concurrency",
        "pm.dexopt.cmdline",
        "pm.dexopt.first-boot",
        "pm.dexopt.first-use",
        "pm.dexopt.inactive",
        "pm.dexopt.install",
        "pm.dexopt.install-bulk",
        "pm.dexopt.install-bulk-downgraded",
        "pm.dexopt.install-bulk-secondary",
        "pm.dexopt.install-bulk-secondary-downgraded",
        "pm.dexopt.install-create-dm",
        "pm.dexopt.install-fast",
        "pm.dexopt.post-boot",
        "pm.dexopt.secondary",
        "pm.dexopt.shared",
        "qcom.hw.aac.encoder",
        "ro.adb.secure",
        "ro.ai.os.version.code",
        "ro.ai.os.version.name",
        "ro.apex.updatable",
        "ro.board.api_frozen",
        "ro.board.api_level",
        "ro.board.first_api_level",
        "ro.board.platform",
        "ro.boot.avb_version",
        "ro.boot.baseband",
        "ro.boot.flash.locked",
        "ro.boot.hardware",
        "ro.boot.hardware.cpu.pagesize",
        "ro.boot.hardware.sku",
        "ro.boot.hwc",
        "ro.boot.hwlevel",
        "ro.boot.hwversion",
        "ro.boot.hypervisor.version",
        "ro.boot.vbmeta.avb_version",
        "ro.boot.vbmeta.device_state",
        "ro.boot.verifiedbootstate",
        "ro.boot.veritymode",
        "ro.bootimage.build.version.sdk_full",
        "ro.build.ab_update",
        "ro.build.date",
        "ro.build.description",
        "ro.build.display.id",
        "ro.build.fingerprint",
        "ro.build.hardware.version",
        "ro.build.host",
        "ro.build.id",
        "ro.build.product",
        "ro.build.tags",
        "ro.build.type",
        "ro.build.user",
        "ro.build.version.all_codenames",
        "ro.build.version.codename",
        "ro.build.version.incremental",
        "ro.build.version.min_supported_target_sdk",
        "ro.build.version.preview_sdk",
        "ro.build.version.preview_sdk_fingerprint",
        "ro.build.version.release",
        "ro.build.version.release_or_codename",
        "ro.build.version.release_or_preview_display",
        "ro.build.version.sdk",
        "ro.build.version.sdk_full",
        "ro.build.version.security_patch",
        "ro.com.google.clientidbase",
        "ro.fota.oem",
        "ro.frp.pst",
        "ro.gfx.driver.1",
        "ro.hardware",
        "ro.hardware.egl",
        "ro.hardware.vulkan",
        "ro.kernel.version",
        "ro.llndk.api_level",
        "ro.logd.size",
        "ro.logd.size.stats",
        "ro.mediaserver.64b.enable",
        "ro.mi.os.soc.vendor",
        "ro.mi.os.version.code",
        "ro.mi.os.version.incremental",
        "ro.mi.os.version.name",
        "ro.mi.os.version.publish",
        "ro.mi.xms.version.incremental",
        "ro.millet.netlink",
        "ro.miui.build.region",
        "ro.miui.business.version",
        "ro.miui.has_gmscore",
        "ro.miui.mcc",
        "ro.miui.mnc",
        "ro.miui.product.home",
        "ro.miui.region",
        "ro.miui.support_miui_ime_bottom",
        "ro.miui.ui.font.mi_font_path",
        "ro.miui.ui.font.mi_fonts_customization_xml",
        "ro.miui.ui.version.code",
        "ro.miui.ui.version.name",
        "ro.netflix.bsp_rev",
        "ro.odm.build.date",
        "ro.odm.build.fingerprint",
        "ro.odm.build.media_performance_class",
        "ro.odm.build.version.incremental",
        "ro.opengles.version",
        "ro.product.bootimage.marketname",
        "ro.product.brand",
        "ro.product.build.date",
        "ro.product.build.fingerprint",
        "ro.product.build.id",
        "ro.product.build.version.incremental",
        "ro.product.camera.livephoto.support",
        "ro.product.cert",
        "ro.product.cpu.abi",
        "ro.product.cpu.abilist",
        "ro.product.cpu.abilist64",
        "ro.product.cpu.pagesize.max",
        "ro.product.device",
        "ro.product.first_api_level",
        "ro.product.manufacturer",
        "ro.product.marketname",
        "ro.product.mod_device",
        "ro.product.model",
        "ro.product.name",
        "ro.product.vendor.model",
        "ro.product.vendor.name",
        "ro.secureboot.devicelock",
        "ro.secureboot.lockstate",
        "ro.sf.lcd_density",
        "ro.sf.lcd_sec_density",
        "ro.soc.model",
        "ro.system.build.date",
        "ro.system.build.fingerprint",
        "ro.system.build.id",
        "ro.system.build.version.incremental",
        "ro.system.product.cpu.abilist",
        "ro.vendor.api_level",
        "ro.vendor.audio.dolby.dax.version",
        "ro.vendor.build.date",
        "ro.vendor.build.security_patch",
        "ro.vendor.display.dynamic_refresh_rate",
        "ro.vendor.mi_fake_32bit_support",
        "ro.vendor.mi_sf.new_dynamic_refresh_rate",
        "ro.vendor.mi_support_zygote32_lazyload",
        "ro.zygote",
        "rust.runtime_version",
        "sys.abreuse.size",
        "sys.debug.graphic_buffer_maxsize",
        "sys.ota.type",
        "sys.usb.adb.disabled",
        "sys.usb.config",
        "sys.usb.configfs",
        "sys.usb.controller",
        "sys.usb.mtp.batchcancel",
        "sys.usb.mtp.device_type",
        "tango.debug",
        "tango.enabled",
        "tango.pretrans.apk",
        "tango.pretrans.debug",
        "tango.pretrans.lib",
        "tango.pretrans.max_size",
        "tango.pretrans_on_install",
        "vendor.display.default_resolution",
        "vendor.display.lcd_density",
        "vendor.display.lcd_sec_density",
        "vendor.qvirtmgr.oemvm.status",
        "vendor.qvirtmgr.trustedvm.status",
        "vendor.xiaomi.trustedvm.version"
    )

    val java = linkedSetOf(
        "android.icu.cldr.version",
        "android.icu.library.version",
        "android.icu.unicode.version",
        "android.openssl.version",
        "android.zlib.version",
        "file.encoding",
        "http.agent",
        "java.class.version",
        "java.runtime.version",
        "java.vm.version",
        "os.arch",
        "os.version",
        "user.language",
        "user.locale",
        "user.name",
        "user.region"
    )

    val linux = linkedSetOf(
        "uname-a",
        "getenforce",
        "proc.version",
        "toybox --version",
        "env.ANDROID_SOCKET_zygote"
    )
}

private object SystemInfoCache {
    private const val KV_ID = "system_info_cache"
    private const val KEY_SYSTEM = "section_system_table"
    private const val KEY_SECURE = "section_secure_table"
    private const val KEY_GLOBAL = "section_global_table"
    private const val KEY_ANDROID = "section_android_properties"
    private const val KEY_JAVA = "section_java_properties"
    private const val KEY_LINUX = "section_linux_environment"

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

    fun read(): CachedSections {
        val kv = MMKV.mmkvWithID(KV_ID)
        return CachedSections(
            system = decodeRows(kv.decodeString(KEY_SYSTEM)),
            secure = decodeRows(kv.decodeString(KEY_SECURE)),
            global = decodeRows(kv.decodeString(KEY_GLOBAL)),
            android = decodeRows(kv.decodeString(KEY_ANDROID)),
            java = decodeRows(kv.decodeString(KEY_JAVA)),
            linux = decodeRows(kv.decodeString(KEY_LINUX))
        )
    }

    fun write(section: SectionKind, rows: List<InfoRow>) {
        val kv = MMKV.mmkvWithID(KV_ID)
        when (section) {
            SectionKind.SYSTEM -> kv.encode(KEY_SYSTEM, encodeRows(rows))
            SectionKind.SECURE -> kv.encode(KEY_SECURE, encodeRows(rows))
            SectionKind.GLOBAL -> kv.encode(KEY_GLOBAL, encodeRows(rows))
            SectionKind.ANDROID -> kv.encode(KEY_ANDROID, encodeRows(rows))
            SectionKind.JAVA -> kv.encode(KEY_JAVA, encodeRows(rows))
            SectionKind.LINUX -> kv.encode(KEY_LINUX, encodeRows(rows))
        }
    }

    fun hasPersistedCache(): Boolean {
        val kv = MMKV.mmkvWithID(KV_ID)
        return kv.containsKey(KEY_SYSTEM) &&
            kv.containsKey(KEY_SECURE) &&
            kv.containsKey(KEY_GLOBAL) &&
            kv.containsKey(KEY_ANDROID) &&
            kv.containsKey(KEY_JAVA) &&
            kv.containsKey(KEY_LINUX)
    }
}

private object SystemUiStateStore {
    private const val KV_ID = "system_ui_state"
    private const val KEY_OVERVIEW = "expanded_overview"
    private const val KEY_TOP_INFO = "expanded_top_info"
    private const val KEY_SYSTEM_TABLE = "expanded_system_table"
    private const val KEY_SECURE_TABLE = "expanded_secure_table"
    private const val KEY_GLOBAL_TABLE = "expanded_global_table"
    private const val KEY_ANDROID_PROPS = "expanded_android_props"
    private const val KEY_JAVA_PROPS = "expanded_java_props"
    private const val KEY_LINUX_ENV = "expanded_linux_env"

    fun topInfoExpanded(defaultValue: Boolean = true): Boolean =
        MMKV.mmkvWithID(KV_ID).decodeBool(KEY_TOP_INFO, defaultValue)

    fun overviewExpanded(defaultValue: Boolean = true): Boolean =
        MMKV.mmkvWithID(KV_ID).decodeBool(KEY_OVERVIEW, defaultValue)

    fun systemTableExpanded(defaultValue: Boolean = false): Boolean =
        MMKV.mmkvWithID(KV_ID).decodeBool(KEY_SYSTEM_TABLE, defaultValue)

    fun secureTableExpanded(defaultValue: Boolean = false): Boolean =
        MMKV.mmkvWithID(KV_ID).decodeBool(KEY_SECURE_TABLE, defaultValue)

    fun globalTableExpanded(defaultValue: Boolean = false): Boolean =
        MMKV.mmkvWithID(KV_ID).decodeBool(KEY_GLOBAL_TABLE, defaultValue)

    fun androidPropsExpanded(defaultValue: Boolean = false): Boolean =
        MMKV.mmkvWithID(KV_ID).decodeBool(KEY_ANDROID_PROPS, defaultValue)

    fun javaPropsExpanded(defaultValue: Boolean = false): Boolean =
        MMKV.mmkvWithID(KV_ID).decodeBool(KEY_JAVA_PROPS, defaultValue)

    fun linuxEnvExpanded(defaultValue: Boolean = false): Boolean =
        MMKV.mmkvWithID(KV_ID).decodeBool(KEY_LINUX_ENV, defaultValue)

    fun setTopInfoExpanded(value: Boolean) {
        MMKV.mmkvWithID(KV_ID).encode(KEY_TOP_INFO, value)
    }

    fun setOverviewExpanded(value: Boolean) {
        MMKV.mmkvWithID(KV_ID).encode(KEY_OVERVIEW, value)
    }

    fun setSystemTableExpanded(value: Boolean) {
        MMKV.mmkvWithID(KV_ID).encode(KEY_SYSTEM_TABLE, value)
    }

    fun setSecureTableExpanded(value: Boolean) {
        MMKV.mmkvWithID(KV_ID).encode(KEY_SECURE_TABLE, value)
    }

    fun setGlobalTableExpanded(value: Boolean) {
        MMKV.mmkvWithID(KV_ID).encode(KEY_GLOBAL_TABLE, value)
    }

    fun setAndroidPropsExpanded(value: Boolean) {
        MMKV.mmkvWithID(KV_ID).encode(KEY_ANDROID_PROPS, value)
    }

    fun setJavaPropsExpanded(value: Boolean) {
        MMKV.mmkvWithID(KV_ID).encode(KEY_JAVA_PROPS, value)
    }

    fun setLinuxEnvExpanded(value: Boolean) {
        MMKV.mmkvWithID(KV_ID).encode(KEY_LINUX_ENV, value)
    }
}

private fun matches(row: InfoRow, query: String): Boolean {
    if (query.isBlank()) return true
    return row.key.contains(query, ignoreCase = true) || row.value.contains(query, ignoreCase = true)
}

private fun filterRows(rows: List<InfoRow>, query: String): List<InfoRow> {
    if (query.isBlank()) return rows
    return rows.filter { matches(it, query) }
}

private enum class ValueTypeOrder(val rank: Int) {
    BOOLEAN(0),
    TIME(1),
    NUMBER(2),
    TEXT(3)
}

private fun detectValueType(value: String): ValueTypeOrder {
    val v = value.trim()
    val lower = v.lowercase()
    if (lower == "true" || lower == "false") return ValueTypeOrder.BOOLEAN
    val looksLikeTime = Regex("^\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}$").matches(v)
    if (looksLikeTime) return ValueTypeOrder.TIME
    if (v.toLongOrNull() != null || v.toDoubleOrNull() != null) return ValueTypeOrder.NUMBER
    return ValueTypeOrder.TEXT
}

private fun sortRowsByType(rows: List<InfoRow>): List<InfoRow> {
    return rows.sortedWith(
        compareBy<InfoRow>(
            { detectValueType(it.value).rank },
            { it.key.lowercase() }
        )
    )
}

private data class TopInfoTopic(
    val order: Int,
    val title: String
)

private fun topInfoTopicOf(key: String): TopInfoTopic {
    val k = key.lowercase()
    return when {
        k.startsWith("long_press_") -> TopInfoTopic(0, "按键长按")
        k.contains("fbo") || k == "key_fbo_data" -> TopInfoTopic(1, "FBO")
        k.contains("dex2oat") || k.contains("dexopt") -> TopInfoTopic(2, "Dex 优化")
        k.startsWith("tango.") || k.contains("tango") -> TopInfoTopic(3, "Tango")
        k.contains("aod") -> TopInfoTopic(4, "AOD")
        k.contains("zygote") -> TopInfoTopic(5, "Zygote")
        k.contains("density") -> TopInfoTopic(6, "显示密度")
        k.contains("autofill") || k.contains("credential") -> TopInfoTopic(7, "自动填充与凭据")
        k.startsWith("gsm.") || k.contains("gsm") -> TopInfoTopic(8, "GSM / 蜂窝网络")
        k.contains("level") -> TopInfoTopic(9, "Level / 等级")
        k.startsWith("adb_") || k.contains("adb") -> TopInfoTopic(10, "ADB / 调试")
        k.startsWith("voice_") || k.contains("assistant") || k.contains("recognition") -> TopInfoTopic(11, "语音与助手")
        k.startsWith("share_") -> TopInfoTopic(12, "共享相关")
        k.contains("bluetooth") || k.contains("bt_") || k.contains("lc3") || k.contains("lea_") -> TopInfoTopic(13, "蓝牙音频")
        k.contains("usb") -> TopInfoTopic(14, "USB")
        k.contains("vulkan") || k.contains("opengl") || k.contains("egl") || k.contains("graphics") || k.contains("hwui") -> TopInfoTopic(15, "图形渲染")
        k.startsWith("miui_") || k.startsWith("ro.miui") || k.startsWith("ro.mi.") || k.contains("xiaomi") -> TopInfoTopic(16, "MIUI / Xiaomi")
        k.contains("version") || k.contains("build") || k.contains("fingerprint") || k.contains("security_patch") -> TopInfoTopic(17, "版本与构建")
        k.contains("time") || k.contains("timestamp") -> TopInfoTopic(18, "时间戳")
        k.startsWith("java.") || k.startsWith("android.") || k.startsWith("os.") || k.startsWith("user.") -> TopInfoTopic(19, "Java / 系统属性")
        k.startsWith("env.") || k == "uname-a" || k == "proc.version" || k == "toybox --version" || k == "getenforce" -> TopInfoTopic(20, "Linux 环境")
        else -> TopInfoTopic(99, "其他")
    }
}

private fun sortTopInfoRows(rows: List<InfoRow>): List<InfoRow> {
    return rows.sortedWith(
        compareBy<InfoRow>(
            { topInfoTopicOf(it.key).order },
            { detectValueType(it.value).rank },
            { it.key.lowercase() }
        )
    )
}

private fun groupTopInfoRows(rows: List<InfoRow>): List<Pair<String, List<InfoRow>>> {
    val grouped = rows.groupBy { topInfoTopicOf(it.key) }
    return grouped.entries
        .sortedBy { it.key.order }
        .map { entry -> entry.key.title to entry.value }
}

private fun isInvalidValue(raw: String): Boolean {
    val value = raw.trim()
    if (value.isBlank()) return true
    val normalized = value.lowercase()
    if (normalized == "n/a" || normalized == "na" || normalized == "unknown" || normalized == "null") return true
    if (normalized == "not found" || normalized == "none") return true
    if (normalized.contains("permission denial")) return true
    return false
}

private fun formatEpochMillis(value: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(value))
}

private fun tryFormatTimeValue(key: String, value: String): String {
    val k = key.lowercase()
    val likelyTime = k.contains("time") || k.contains("timestamp") || k.contains("build_time")
    if (!likelyTime) return value

    val num = value.toLongOrNull() ?: return value
    val millis = if (num in 1..99_999_999_999L) num * 1000 else num
    if (millis <= 0L) return value
    return runCatching { formatEpochMillis(millis) }.getOrDefault(value)
}

private fun cleanRows(rows: List<InfoRow>): List<InfoRow> {
    val seen = LinkedHashSet<String>()
    return rows
        .map {
            val formatted = tryFormatTimeValue(it.key, it.value.trim())
            InfoRow(it.key.trim(), formatted.trim())
        }
        .filter { it.key.isNotBlank() && !isInvalidValue(it.value) }
        .filter { seen.add("${it.key}\u0000${it.value}") }
}

private fun execRuntimeCommand(command: String): String? {
    return runCatching {
        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
        process.inputStream.bufferedReader().use { it.readText() }.trim()
    }.getOrNull()?.ifBlank { null }
}

private fun parseKeyValueLines(raw: String?): List<InfoRow> {
    if (raw.isNullOrBlank()) return emptyList()
    return raw.lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() && it.contains("=") }
        .mapNotNull { line ->
            val index = line.indexOf('=')
            if (index <= 0) return@mapNotNull null
            InfoRow(
                key = line.substring(0, index).trim(),
                value = line.substring(index + 1).trim()
            )
        }
        .toList()
}

private fun commandRows(command: String, shizukuApiUtils: ShizukuApiUtils): List<InfoRow> {
    val shizuku = shizukuApiUtils.execCommand(command)
    val runtime = if (shizuku.isNullOrBlank()) execRuntimeCommand(command) else null
    return parseKeyValueLines(shizuku ?: runtime)
}

private fun decodeVulkanApiVersion(version: Int): String {
    if (version <= 0) return ""
    val major = version shr 22
    val minor = (version shr 12) and 0x3ff
    val patch = version and 0xfff
    return "$major.$minor.$patch"
}

private fun decodeOpenGlEsVersion(raw: String): String {
    val value = raw.toIntOrNull() ?: return ""
    if (value <= 0) return ""
    val major = (value shr 16) and 0xffff
    val minor = value and 0xffff
    return "$major.$minor"
}

private fun packageVersionName(context: Context, packageName: String): String {
    return runCatching {
        val info = if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, 0)
        }
        val versionName = info.versionName ?: ""
        val versionCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        buildString {
            append(versionName.ifBlank { "unknown" })
            append(" (")
            append(versionCode)
            append(")")
        }
    }.getOrDefault("")
}

private fun featureVersion(features: Array<FeatureInfo>?, featureName: String): Int {
    return features?.firstOrNull { it.name == featureName }?.version ?: 0
}

private fun boolFeatureRow(pm: PackageManager, featureName: String, label: String): InfoRow {
    return InfoRow(label, pm.hasSystemFeature(featureName).toString())
}

private fun graphicsRows(context: Context): List<InfoRow> {
    val pm = context.packageManager
    val features = pm.systemAvailableFeatures
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val glEsVersion = activityManager?.deviceConfigurationInfo?.glEsVersion.orEmpty()
    val glEsRaw = execRuntimeCommand("getprop ro.opengles.version").orEmpty()
    val glEsDecoded = decodeOpenGlEsVersion(glEsRaw)

    val vulkanVersionEncoded = featureVersion(features, "android.hardware.vulkan.version")
    val vulkanLevel = featureVersion(features, "android.hardware.vulkan.level")
    val vulkanVersionText = decodeVulkanApiVersion(vulkanVersionEncoded)
    val vulkanHardware = execRuntimeCommand("getprop ro.hardware.vulkan").orEmpty()
    val eglHardware = execRuntimeCommand("getprop ro.hardware.egl").orEmpty()
    val gfxDriver0 = execRuntimeCommand("getprop ro.gfx.driver.0").orEmpty()
    val gfxDriver1 = execRuntimeCommand("getprop ro.gfx.driver.1").orEmpty()
    val gpuDriverPackage = listOf(gfxDriver1, gfxDriver0).firstOrNull { it.contains('.') }.orEmpty()
    val gpuDriverVersion = if (gpuDriverPackage.isBlank()) "" else packageVersionName(context, gpuDriverPackage)

    return listOf(
        InfoRow("graphics.opengl.es", glEsVersion),
        InfoRow("graphics.opengl.es.raw", glEsRaw),
        InfoRow("graphics.opengl.es.decoded", glEsDecoded),
        InfoRow("graphics.vulkan.version", vulkanVersionText),
        InfoRow("graphics.vulkan.version.raw", if (vulkanVersionEncoded > 0) vulkanVersionEncoded.toString() else ""),
        InfoRow("graphics.vulkan.level", if (vulkanLevel > 0) vulkanLevel.toString() else ""),
        InfoRow("graphics.gpu.driver.package", gpuDriverPackage),
        InfoRow("graphics.gpu.driver.version", gpuDriverVersion),
        InfoRow("graphics.gpu.driver.ro.gfx.driver.0", gfxDriver0),
        InfoRow("graphics.gpu.driver.ro.gfx.driver.1", gfxDriver1),
        boolFeatureRow(pm, "android.hardware.vulkan.version", "feature.vulkan.version"),
        boolFeatureRow(pm, "android.hardware.vulkan.level", "feature.vulkan.level"),
        boolFeatureRow(pm, "android.hardware.opengles.aep", "feature.opengl.aep"),
        boolFeatureRow(pm, PackageManager.FEATURE_OPENGLES_EXTENSION_PACK, "feature.opengles.extension_pack"),
        InfoRow("prop.ro.hardware.egl", eglHardware),
        InfoRow("prop.ro.hardware.vulkan", vulkanHardware)
    )
}

private fun capabilityRows(context: Context): List<InfoRow> {
    val pm = context.packageManager
    return listOf(
        boolFeatureRow(pm, PackageManager.FEATURE_BLUETOOTH, "feature.bluetooth"),
        boolFeatureRow(pm, PackageManager.FEATURE_BLUETOOTH_LE, "feature.bluetooth_le"),
        boolFeatureRow(pm, PackageManager.FEATURE_WIFI, "feature.wifi"),
        boolFeatureRow(pm, PackageManager.FEATURE_WIFI_AWARE, "feature.wifi_aware"),
        boolFeatureRow(pm, PackageManager.FEATURE_NFC, "feature.nfc"),
        boolFeatureRow(pm, PackageManager.FEATURE_USB_HOST, "feature.usb_host"),
        boolFeatureRow(pm, PackageManager.FEATURE_CAMERA, "feature.camera"),
        boolFeatureRow(pm, PackageManager.FEATURE_CAMERA_FRONT, "feature.camera_front"),
        boolFeatureRow(pm, PackageManager.FEATURE_CAMERA_FLASH, "feature.camera_flash"),
        boolFeatureRow(pm, PackageManager.FEATURE_SENSOR_ACCELEROMETER, "feature.sensor.accelerometer"),
        boolFeatureRow(pm, PackageManager.FEATURE_SENSOR_GYROSCOPE, "feature.sensor.gyroscope"),
        boolFeatureRow(pm, PackageManager.FEATURE_SENSOR_BAROMETER, "feature.sensor.barometer"),
        boolFeatureRow(pm, PackageManager.FEATURE_FINGERPRINT, "feature.fingerprint"),
        boolFeatureRow(pm, PackageManager.FEATURE_LOCATION_GPS, "feature.location.gps"),
        boolFeatureRow(pm, PackageManager.FEATURE_TELEPHONY, "feature.telephony")
    )
}

private fun buildSectionRows(
    section: SectionKind,
    context: Context,
    shizukuStatus: String,
    shizukuApiUtils: ShizukuApiUtils
): List<InfoRow> {
    return when (section) {
        SectionKind.SYSTEM -> cleanRows(commandRows("settings list system", shizukuApiUtils))
        SectionKind.SECURE -> cleanRows(commandRows("settings list secure", shizukuApiUtils))
        SectionKind.GLOBAL -> cleanRows(commandRows("settings list global", shizukuApiUtils))
        SectionKind.ANDROID -> cleanRows(
            graphicsRows(context) +
                capabilityRows(context) +
                getAllSystemProperties.toSortedMap().map { InfoRow(it.key, it.value) }
        )
        SectionKind.JAVA -> cleanRows(
            getAllJavaPropString.toSortedMap().map { InfoRow(it.key, it.value) }
        )
        SectionKind.LINUX -> {
            val runtimeUname = execRuntimeCommand("uname -a")
            val runtimeGetenforce = execRuntimeCommand("getenforce")
            val runtimeProcVersion = execRuntimeCommand("cat /proc/version")
            val runtimeToybox = execRuntimeCommand("toybox --version")

            val shizukuUname = shizukuApiUtils.execCommand("uname -a")
            val shizukuGetenforce = shizukuApiUtils.execCommand("getenforce")
            val shizukuProcVersion = shizukuApiUtils.execCommand("cat /proc/version")
            val shizukuToybox = shizukuApiUtils.execCommand("toybox --version")

            val envRows = System.getenv().toSortedMap().map { InfoRow("env.${it.key}", it.value) }
            cleanRows(
                listOf(
                    InfoRow("Shizuku Status", shizukuStatus),
                    InfoRow("uname-a", shizukuUname?.lineSequence()?.firstOrNull() ?: runtimeUname.orEmpty()),
                    InfoRow("getenforce", shizukuGetenforce?.lineSequence()?.firstOrNull() ?: runtimeGetenforce.orEmpty()),
                    InfoRow("proc.version", shizukuProcVersion?.lineSequence()?.firstOrNull() ?: runtimeProcVersion.orEmpty()),
                    InfoRow("toybox --version", shizukuToybox?.lineSequence()?.firstOrNull() ?: runtimeToybox.orEmpty())
                ) + envRows
            )
        }
    }
}

private fun removeTopInfoRows(section: SectionKind, rows: List<InfoRow>): List<InfoRow> {
    val keySet = when (section) {
        SectionKind.SYSTEM -> TopInfoKeys.system
        SectionKind.SECURE -> TopInfoKeys.secure
        SectionKind.GLOBAL -> TopInfoKeys.global
        SectionKind.ANDROID -> TopInfoKeys.android
        SectionKind.JAVA -> TopInfoKeys.java
        SectionKind.LINUX -> TopInfoKeys.linux
    }
    return rows.filterNot { keySet.contains(it.key) }
}

private fun mapRows(rows: List<InfoRow>): Map<String, String> = rows.associate { it.key to it.value }

private fun buildTopInfoRows(
    systemRows: List<InfoRow>,
    secureRows: List<InfoRow>,
    globalRows: List<InfoRow>,
    androidRows: List<InfoRow>,
    javaRows: List<InfoRow>,
    linuxRows: List<InfoRow>
): List<InfoRow> {
    val systemMap = mapRows(systemRows)
    val secureMap = mapRows(secureRows)
    val globalMap = mapRows(globalRows)
    val androidMap = mapRows(androidRows)
    val javaMap = mapRows(javaRows)
    val linuxMap = mapRows(linuxRows)

    val rows = mutableListOf<InfoRow>()
    TopInfoKeys.system.forEach { key -> systemMap[key]?.let { rows += InfoRow(key, it) } }
    TopInfoKeys.secure.forEach { key -> secureMap[key]?.let { rows += InfoRow(key, it) } }
    TopInfoKeys.global.forEach { key -> globalMap[key]?.let { rows += InfoRow(key, it) } }
    TopInfoKeys.android.forEach { key -> androidMap[key]?.let { rows += InfoRow(key, it) } }
    TopInfoKeys.java.forEach { key -> javaMap[key]?.let { rows += InfoRow(key, it) } }
    TopInfoKeys.linux.forEach { key -> linuxMap[key]?.let { rows += InfoRow(key, it) } }
    return sortTopInfoRows(cleanRows(rows))
}

private data class ExportSections(
    val topInfo: List<InfoRow>,
    val system: List<InfoRow>,
    val secure: List<InfoRow>,
    val global: List<InfoRow>,
    val android: List<InfoRow>,
    val java: List<InfoRow>,
    val linux: List<InfoRow>
)

private fun buildExportSections(
    context: Context,
    shizukuStatus: String,
    shizukuApiUtils: ShizukuApiUtils
): ExportSections {
    val system = buildSectionRows(SectionKind.SYSTEM, context, shizukuStatus, shizukuApiUtils)
    val secure = buildSectionRows(SectionKind.SECURE, context, shizukuStatus, shizukuApiUtils)
    val global = buildSectionRows(SectionKind.GLOBAL, context, shizukuStatus, shizukuApiUtils)
    val android = buildSectionRows(SectionKind.ANDROID, context, shizukuStatus, shizukuApiUtils)
    val java = buildSectionRows(SectionKind.JAVA, context, shizukuStatus, shizukuApiUtils)
    val linux = buildSectionRows(SectionKind.LINUX, context, shizukuStatus, shizukuApiUtils)
    val topInfo = buildTopInfoRows(system, secure, global, android, java, linux)

    return ExportSections(
        topInfo = topInfo,
        system = removeTopInfoRows(SectionKind.SYSTEM, system),
        secure = removeTopInfoRows(SectionKind.SECURE, secure),
        global = removeTopInfoRows(SectionKind.GLOBAL, global),
        android = removeTopInfoRows(SectionKind.ANDROID, android),
        java = removeTopInfoRows(SectionKind.JAVA, java),
        linux = removeTopInfoRows(SectionKind.LINUX, linux)
    )
}

private fun escapeMarkdown(text: String): String {
    return text.replace("|", "\\|").replace("\n", "<br>")
}

private fun appendSectionMarkdown(builder: StringBuilder, title: String, rows: List<InfoRow>) {
    builder.appendLine("## $title")
    if (rows.isEmpty()) {
        builder.appendLine("_No data_")
        builder.appendLine()
        return
    }
    builder.appendLine("| Key | Value |")
    builder.appendLine("| --- | --- |")
    rows.forEach { row ->
        builder.appendLine("| ${escapeMarkdown(row.key)} | ${escapeMarkdown(row.value)} |")
    }
    builder.appendLine()
}

private fun buildSystemMarkdown(
    generatedAt: String,
    shizukuStatus: String,
    sections: ExportSections
): String {
    return buildString {
        appendLine("# KeiOS System Export")
        appendLine()
        appendLine("- Generated at: $generatedAt")
        appendLine("- Shizuku status: $shizukuStatus")
        appendLine("- Format: Markdown")
        appendLine()
        appendSectionMarkdown(this, "TopInfo", sections.topInfo)
        appendSectionMarkdown(this, "System Table", sections.system)
        appendSectionMarkdown(this, "Secure Table", sections.secure)
        appendSectionMarkdown(this, "Global Table", sections.global)
        appendSectionMarkdown(this, "Android Properties", sections.android)
        appendSectionMarkdown(this, "Java Properties", sections.java)
        appendSectionMarkdown(this, "Linux environment", sections.linux)
    }
}

@Composable
fun SystemPage(
    scrollToTopSignal: Int,
    shizukuStatus: String,
    shizukuApiUtils: ShizukuApiUtils,
    contentBottomPadding: Dp = 72.dp,
    onActionBarInteractingChanged: (Boolean) -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val inactive = MiuixTheme.colorScheme.onBackgroundVariant
    val titleColor = MiuixTheme.colorScheme.onBackground
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant
    val cachedColor = Color(0xFFF59E0B)
    val refreshingColor = Color(0xFF3B82F6)
    val syncedColor = Color(0xFF22C55E)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shizukuReady = shizukuStatus.contains("granted", ignoreCase = true)
    var cacheLoaded by remember { mutableStateOf(false) }
    var cachePersisted by remember { mutableStateOf(false) }
    var queryInput by remember { mutableStateOf("") }
    var queryApplied by remember { mutableStateOf("") }
    var topInfoExpanded by remember { mutableStateOf(SystemUiStateStore.topInfoExpanded(defaultValue = true)) }
    var systemTableExpanded by remember { mutableStateOf(SystemUiStateStore.systemTableExpanded(defaultValue = false)) }
    var secureTableExpanded by remember { mutableStateOf(SystemUiStateStore.secureTableExpanded(defaultValue = false)) }
    var globalTableExpanded by remember { mutableStateOf(SystemUiStateStore.globalTableExpanded(defaultValue = false)) }
    var androidPropsExpanded by remember { mutableStateOf(SystemUiStateStore.androidPropsExpanded(defaultValue = false)) }
    var javaPropsExpanded by remember { mutableStateOf(SystemUiStateStore.javaPropsExpanded(defaultValue = false)) }
    var linuxEnvExpanded by remember { mutableStateOf(SystemUiStateStore.linuxEnvExpanded(defaultValue = false)) }
    val listState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    var pendingExportContent by remember { mutableStateOf<String?>(null) }
    var exportPreparing by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var refreshProgress by remember { mutableStateOf(0f) }
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop: LayerBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    DisposableEffect(Unit) {
        onDispose { onActionBarInteractingChanged(false) }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        val content = pendingExportContent
        if (uri == null || content.isNullOrBlank()) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter().use { writer ->
                writer?.write(content)
            }
        }.onSuccess {
            Toast.makeText(context, "导出成功", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "导出失败: ${it.javaClass.simpleName}", Toast.LENGTH_SHORT).show()
        }
    }

    var sectionStates by remember {
        mutableStateOf(
            mapOf(
                SectionKind.SYSTEM to SectionState(),
                SectionKind.SECURE to SectionState(),
                SectionKind.GLOBAL to SectionState(),
                SectionKind.ANDROID to SectionState(),
                SectionKind.JAVA to SectionState(),
                SectionKind.LINUX to SectionState()
            )
        )
    }

    fun updateSection(section: SectionKind, transform: (SectionState) -> SectionState) {
        sectionStates = sectionStates.toMutableMap().also { map ->
            val old = map[section] ?: SectionState()
            map[section] = transform(old)
        }
    }

    suspend fun ensureLoad(section: SectionKind, forceRefresh: Boolean = false) {
        val current = sectionStates[section] ?: SectionState()
        if (current.loading) return
        if (!forceRefresh) {
            if (current.loadedFresh) return
            if (current.rows.isNotEmpty()) return
        }
        updateSection(section) { it.copy(loading = true) }
        val fresh = withContext(Dispatchers.IO) {
            buildSectionRows(section, context, shizukuStatus, shizukuApiUtils)
        }
        updateSection(section) { it.copy(rows = fresh, loading = false, loadedFresh = true) }
        cachePersisted = withContext(Dispatchers.IO) {
            SystemInfoCache.write(section, fresh)
            SystemInfoCache.hasPersistedCache()
        }
    }

    suspend fun refreshAllSections() {
        refreshing = true
        refreshProgress = 0f
        try {
            val sectionCount = SectionKind.entries.size
            SectionKind.entries.forEachIndexed { index, section ->
                ensureLoad(section, forceRefresh = true)
                refreshProgress = (index + 1).toFloat() / sectionCount.toFloat()
            }
            Toast.makeText(context, "系统参数已刷新并缓存", Toast.LENGTH_SHORT).show()
        } finally {
            refreshing = false
        }
    }

    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) listState.animateScrollToItem(0)
    }

    LaunchedEffect(queryInput) {
        delay(180)
        queryApplied = queryInput
    }

    LaunchedEffect(Unit) {
        val (cached, persisted) = withContext(Dispatchers.IO) {
            SystemInfoCache.read() to SystemInfoCache.hasPersistedCache()
        }
        sectionStates = mapOf(
            SectionKind.SYSTEM to SectionState(rows = cached.system),
            SectionKind.SECURE to SectionState(rows = cached.secure),
            SectionKind.GLOBAL to SectionState(rows = cached.global),
            SectionKind.ANDROID to SectionState(rows = cached.android),
            SectionKind.JAVA to SectionState(rows = cached.java),
            SectionKind.LINUX to SectionState(rows = cached.linux)
        )
        cachePersisted = persisted
        cacheLoaded = true
    }

    LaunchedEffect(shizukuReady) {
        updateSection(SectionKind.SYSTEM) { it.copy(loadedFresh = false) }
        updateSection(SectionKind.SECURE) { it.copy(loadedFresh = false) }
        updateSection(SectionKind.GLOBAL) { it.copy(loadedFresh = false) }
        updateSection(SectionKind.LINUX) { it.copy(loadedFresh = false) }
    }

    LaunchedEffect(topInfoExpanded) {
        withContext(Dispatchers.IO) { SystemUiStateStore.setTopInfoExpanded(topInfoExpanded) }
    }
    LaunchedEffect(systemTableExpanded) {
        withContext(Dispatchers.IO) { SystemUiStateStore.setSystemTableExpanded(systemTableExpanded) }
    }
    LaunchedEffect(secureTableExpanded) {
        withContext(Dispatchers.IO) { SystemUiStateStore.setSecureTableExpanded(secureTableExpanded) }
    }
    LaunchedEffect(globalTableExpanded) {
        withContext(Dispatchers.IO) { SystemUiStateStore.setGlobalTableExpanded(globalTableExpanded) }
    }
    LaunchedEffect(androidPropsExpanded) {
        withContext(Dispatchers.IO) { SystemUiStateStore.setAndroidPropsExpanded(androidPropsExpanded) }
    }
    LaunchedEffect(javaPropsExpanded) {
        withContext(Dispatchers.IO) { SystemUiStateStore.setJavaPropsExpanded(javaPropsExpanded) }
    }
    LaunchedEffect(linuxEnvExpanded) {
        withContext(Dispatchers.IO) { SystemUiStateStore.setLinuxEnvExpanded(linuxEnvExpanded) }
    }

    LaunchedEffect(systemTableExpanded) {
        if (systemTableExpanded) ensureLoad(SectionKind.SYSTEM)
    }
    LaunchedEffect(secureTableExpanded) {
        if (secureTableExpanded) ensureLoad(SectionKind.SECURE)
    }
    LaunchedEffect(globalTableExpanded) {
        if (globalTableExpanded) ensureLoad(SectionKind.GLOBAL)
    }
    LaunchedEffect(androidPropsExpanded) {
        if (androidPropsExpanded) ensureLoad(SectionKind.ANDROID)
    }
    LaunchedEffect(javaPropsExpanded) {
        if (javaPropsExpanded) ensureLoad(SectionKind.JAVA)
    }
    LaunchedEffect(linuxEnvExpanded) {
        if (linuxEnvExpanded) ensureLoad(SectionKind.LINUX)
    }

    val systemRows = sectionStates[SectionKind.SYSTEM]?.rows ?: emptyList()
    val secureRows = sectionStates[SectionKind.SECURE]?.rows ?: emptyList()
    val globalRows = sectionStates[SectionKind.GLOBAL]?.rows ?: emptyList()
    val androidRows = sectionStates[SectionKind.ANDROID]?.rows ?: emptyList()
    val javaRows = sectionStates[SectionKind.JAVA]?.rows ?: emptyList()
    val linuxRows = sectionStates[SectionKind.LINUX]?.rows ?: emptyList()

    val topInfoRows = remember(systemRows, secureRows, globalRows, androidRows, javaRows, linuxRows) {
        buildTopInfoRows(systemRows, secureRows, globalRows, androidRows, javaRows, linuxRows)
    }

    val prunedSystemRows = remember(systemRows) { removeTopInfoRows(SectionKind.SYSTEM, systemRows) }
    val prunedSecureRows = remember(secureRows) { removeTopInfoRows(SectionKind.SECURE, secureRows) }
    val prunedGlobalRows = remember(globalRows) { removeTopInfoRows(SectionKind.GLOBAL, globalRows) }
    val prunedAndroidRows = remember(androidRows) { removeTopInfoRows(SectionKind.ANDROID, androidRows) }
    val prunedJavaRows = remember(javaRows) { removeTopInfoRows(SectionKind.JAVA, javaRows) }
    val prunedLinuxRows = remember(linuxRows) { removeTopInfoRows(SectionKind.LINUX, linuxRows) }

    val q = queryApplied.trim()
    val displayedTopInfoRows = remember(q, topInfoRows, topInfoExpanded) {
        if (q.isBlank() && !topInfoExpanded) topInfoRows else sortRowsByType(filterRows(topInfoRows, q))
    }
    val displayedSystemRows = remember(q, prunedSystemRows, systemTableExpanded) {
        if (q.isBlank() && !systemTableExpanded) prunedSystemRows else sortRowsByType(filterRows(prunedSystemRows, q))
    }
    val displayedSecureRows = remember(q, prunedSecureRows, secureTableExpanded) {
        if (q.isBlank() && !secureTableExpanded) prunedSecureRows else sortRowsByType(filterRows(prunedSecureRows, q))
    }
    val displayedGlobalRows = remember(q, prunedGlobalRows, globalTableExpanded) {
        if (q.isBlank() && !globalTableExpanded) prunedGlobalRows else sortRowsByType(filterRows(prunedGlobalRows, q))
    }
    val displayedAndroidRows = remember(q, prunedAndroidRows, androidPropsExpanded) {
        if (q.isBlank() && !androidPropsExpanded) prunedAndroidRows else sortRowsByType(filterRows(prunedAndroidRows, q))
    }
    val displayedJavaRows = remember(q, prunedJavaRows, javaPropsExpanded) {
        if (q.isBlank() && !javaPropsExpanded) prunedJavaRows else sortRowsByType(filterRows(prunedJavaRows, q))
    }
    val displayedLinuxRows = remember(q, prunedLinuxRows, linuxEnvExpanded) {
        if (q.isBlank() && !linuxEnvExpanded) prunedLinuxRows else sortRowsByType(filterRows(prunedLinuxRows, q))
    }
    val groupedTopInfoRows = remember(displayedTopInfoRows, topInfoExpanded, q) {
        if (q.isBlank() && !topInfoExpanded) emptyList() else groupTopInfoRows(displayedTopInfoRows)
    }

    fun sectionSubtitle(section: SectionKind, size: Int): String {
        val state = sectionStates[section] ?: SectionState()
        return when {
            state.loading -> "加载中..."
            !state.loadedFresh && state.rows.isNotEmpty() -> "$size 条（缓存）"
            !state.loadedFresh && state.rows.isEmpty() -> "未加载"
            else -> "$size 条"
        }
    }
    val loadedFreshCount = sectionStates.values.count { it.loadedFresh }
    val cachedSectionCount = sectionStates.values.count { !it.loadedFresh && it.rows.isNotEmpty() }
    val sectionCount = SectionKind.entries.size
    val overviewState = when {
        refreshing -> SystemOverviewState.Refreshing
        loadedFreshCount == sectionCount && sectionCount > 0 -> SystemOverviewState.Completed
        cachePersisted || cachedSectionCount > 0 -> SystemOverviewState.Cached
        else -> SystemOverviewState.Idle
    }
    val statusLabel = when (overviewState) {
        SystemOverviewState.Cached -> "Cached"
        SystemOverviewState.Refreshing -> "Refreshing"
        SystemOverviewState.Completed -> "Synced"
        SystemOverviewState.Idle -> "Idle"
    }
    val statusColor = when (overviewState) {
        SystemOverviewState.Cached -> cachedColor
        SystemOverviewState.Refreshing -> refreshingColor
        SystemOverviewState.Completed -> syncedColor
        SystemOverviewState.Idle -> inactive
    }
    val indicatorProgress = when (overviewState) {
        SystemOverviewState.Refreshing -> refreshProgress.coerceIn(0f, 1f)
        SystemOverviewState.Completed,
        SystemOverviewState.Cached -> 1f
        SystemOverviewState.Idle -> 0f
    }
    val indicatorBg = when (overviewState) {
        SystemOverviewState.Refreshing -> Color(0x553B82F6)
        SystemOverviewState.Completed -> Color(0x5522C55E)
        SystemOverviewState.Cached -> Color(0x55F59E0B)
        SystemOverviewState.Idle -> MiuixTheme.colorScheme.surface
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = "System",
                    scrollBehavior = scrollBehavior,
                    color = MiuixTheme.colorScheme.surface,
                    actions = {
                        LiquidActionBar(
                            backdrop = backdrop,
                            items = listOf(
                                LiquidActionItem(
                                    icon = MiuixIcons.Regular.Refresh,
                                    contentDescription = "刷新系统参数",
                                    onClick = {
                                        if (refreshing) return@LiquidActionItem
                                        scope.launch { refreshAllSections() }
                                    }
                                ),
                                LiquidActionItem(
                                    icon = MiuixIcons.Regular.Download,
                                    contentDescription = if (exportPreparing) "准备导出中" else "导出",
                                    onClick = {
                                        if (exportPreparing) return@LiquidActionItem
                                        exportPreparing = true
                                        scope.launch {
                                            val generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                                            val markdown = withContext(Dispatchers.IO) {
                                                val exportSections = buildExportSections(context, shizukuStatus, shizukuApiUtils)
                                                buildSystemMarkdown(generatedAt, shizukuStatus, exportSections)
                                            }
                                            val fileName = "keios-system-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())}.md"
                                            pendingExportContent = markdown
                                            exportPreparing = false
                                            exportLauncher.launch(fileName)
                                        }
                                    }
                                )
                            ),
                            onInteractionChanged = onActionBarInteractingChanged
                        )
                    }
                )
                GlassSearchField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    value = queryInput,
                    onValueChange = { queryInput = it },
                    label = "搜索系统参数",
                    backdrop = backdrop,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
                start = 12.dp,
                end = 12.dp
            )
        ) {
            item { SmallTitle("系统参数与属性") }
            item { Spacer(modifier = Modifier.height(10.dp)) }
            item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.defaultColors(
                    color = when (overviewState) {
                        SystemOverviewState.Cached -> if (isDark) Color(0x55F59E0B) else Color(0x33F59E0B)
                        SystemOverviewState.Refreshing -> if (isDark) Color(0x553B82F6) else Color(0x333B82F6)
                        SystemOverviewState.Completed -> if (isDark) Color(0x5522C55E) else Color(0x3322C55E)
                        SystemOverviewState.Idle -> MiuixTheme.colorScheme.surface.copy(alpha = 0.66f)
                    },
                    contentColor = titleColor
                ),
                showIndication = true,
                onClick = {
                    if (refreshing) return@Card
                    scope.launch { refreshAllSections() }
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Overview", color = titleColor)
                        Spacer(modifier = Modifier.weight(1f))
                        StatusPill(label = statusLabel, color = statusColor)
                        StatusPill(label = "Shizuku", color = if (shizukuReady) syncedColor else inactive)
                    }
                    Text(
                        text = "TopInfo ${topInfoRows.size} 条 · 查询 ${if (q.isBlank()) "（空）" else q}",
                        color = subtitleColor
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (!cacheLoaded) {
                                "读取缓存中... · Fresh $loadedFreshCount / $sectionCount"
                            } else {
                                "缓存分区 $cachedSectionCount · Fresh $loadedFreshCount / $sectionCount"
                            },
                            color = subtitleColor
                        )
                        if (overviewState != SystemOverviewState.Idle) {
                            CircularProgressIndicator(
                                progress = indicatorProgress,
                                size = 18.dp,
                                strokeWidth = 2.dp,
                                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                                    foregroundColor = statusColor,
                                    backgroundColor = indicatorBg
                                )
                            )
                        }
                    }
                }
            }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
            MiuixExpandableSection(
                backdrop = backdrop,
                title = "TopInfo",
                subtitle = "${displayedTopInfoRows.size} 条",
                expanded = topInfoExpanded,
                onExpandedChange = { topInfoExpanded = it }
            ) {
                if (displayedTopInfoRows.isEmpty()) {
                    Text(text = "No matched results.", color = MiuixTheme.colorScheme.onBackgroundVariant)
                } else {
                    if (q.isBlank() && !topInfoExpanded) {
                        displayedTopInfoRows.forEach { row ->
                            MiuixInfoItem(row.key, row.value)
                        }
                    } else {
                        groupedTopInfoRows.forEachIndexed { index, (type, rows) ->
                            Text(
                                text = type,
                                color = MiuixTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = if (index == 0) 0.dp else 8.dp, bottom = 2.dp)
                            )
                            rows.forEach { row ->
                                MiuixInfoItem(row.key, row.value)
                            }
                        }
                    }
                }
            }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
            MiuixExpandableSection(
                backdrop = backdrop,
                title = "System Table",
                subtitle = sectionSubtitle(SectionKind.SYSTEM, if (q.isBlank()) prunedSystemRows.size else displayedSystemRows.size),
                expanded = systemTableExpanded,
                onExpandedChange = { systemTableExpanded = it }
            ) {
                if (displayedSystemRows.isEmpty()) Text(text = "No matched results.", color = MiuixTheme.colorScheme.onBackgroundVariant)
                else displayedSystemRows.forEach { row -> MiuixInfoItem(row.key, row.value) }
            }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
            MiuixExpandableSection(
                backdrop = backdrop,
                title = "Secure Table",
                subtitle = sectionSubtitle(SectionKind.SECURE, if (q.isBlank()) prunedSecureRows.size else displayedSecureRows.size),
                expanded = secureTableExpanded,
                onExpandedChange = { secureTableExpanded = it }
            ) {
                if (displayedSecureRows.isEmpty()) Text(text = "No matched results.", color = MiuixTheme.colorScheme.onBackgroundVariant)
                else displayedSecureRows.forEach { row -> MiuixInfoItem(row.key, row.value) }
            }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
            MiuixExpandableSection(
                backdrop = backdrop,
                title = "Global Table",
                subtitle = sectionSubtitle(SectionKind.GLOBAL, if (q.isBlank()) prunedGlobalRows.size else displayedGlobalRows.size),
                expanded = globalTableExpanded,
                onExpandedChange = { globalTableExpanded = it }
            ) {
                if (displayedGlobalRows.isEmpty()) Text(text = "No matched results.", color = MiuixTheme.colorScheme.onBackgroundVariant)
                else displayedGlobalRows.forEach { row -> MiuixInfoItem(row.key, row.value) }
            }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
            MiuixExpandableSection(
                backdrop = backdrop,
                title = "Android Properties",
                subtitle = sectionSubtitle(SectionKind.ANDROID, if (q.isBlank()) prunedAndroidRows.size else displayedAndroidRows.size),
                expanded = androidPropsExpanded,
                onExpandedChange = { androidPropsExpanded = it }
            ) {
                if (displayedAndroidRows.isEmpty()) Text(text = "No matched results.", color = MiuixTheme.colorScheme.onBackgroundVariant)
                else displayedAndroidRows.forEach { row -> MiuixInfoItem(row.key, row.value) }
            }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
            MiuixExpandableSection(
                backdrop = backdrop,
                title = "Java Properties",
                subtitle = sectionSubtitle(SectionKind.JAVA, if (q.isBlank()) prunedJavaRows.size else displayedJavaRows.size),
                expanded = javaPropsExpanded,
                onExpandedChange = { javaPropsExpanded = it }
            ) {
                if (displayedJavaRows.isEmpty()) Text(text = "No matched results.", color = MiuixTheme.colorScheme.onBackgroundVariant)
                else displayedJavaRows.forEach { row -> MiuixInfoItem(row.key, row.value) }
            }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
            MiuixExpandableSection(
                backdrop = backdrop,
                title = "Linux environment",
                subtitle = sectionSubtitle(SectionKind.LINUX, if (q.isBlank()) prunedLinuxRows.size else displayedLinuxRows.size),
                expanded = linuxEnvExpanded,
                onExpandedChange = { linuxEnvExpanded = it }
            ) {
                if (displayedLinuxRows.isEmpty()) Text(text = "No matched results.", color = MiuixTheme.colorScheme.onBackgroundVariant)
                else displayedLinuxRows.forEach { row -> MiuixInfoItem(row.key, row.value) }
            }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}
