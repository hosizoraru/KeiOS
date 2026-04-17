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

internal fun matches(row: InfoRow, query: String): Boolean {
    if (query.isBlank()) return true
    return row.key.contains(query, ignoreCase = true) || row.value.contains(query, ignoreCase = true)
}

internal fun filterRows(rows: List<InfoRow>, query: String): List<InfoRow> {
    if (query.isBlank()) return rows
    return rows.filter { matches(it, query) }
}

internal enum class ValueTypeOrder(val rank: Int) {
    BOOLEAN(0),
    TIME(1),
    NUMBER(2),
    TEXT(3)
}

internal fun detectValueType(value: String): ValueTypeOrder {
    val v = value.trim()
    val lower = v.lowercase()
    if (lower == "true" || lower == "false") return ValueTypeOrder.BOOLEAN
    val looksLikeTime = Regex("^\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}$").matches(v)
    if (looksLikeTime) return ValueTypeOrder.TIME
    if (v.toLongOrNull() != null || v.toDoubleOrNull() != null) return ValueTypeOrder.NUMBER
    return ValueTypeOrder.TEXT
}

internal fun sortRowsByType(rows: List<InfoRow>): List<InfoRow> {
    return rows.sortedWith(
        compareBy<InfoRow>(
            { detectValueType(it.value).rank },
            { it.key.lowercase() }
        )
    )
}

internal fun isInvalidValue(raw: String): Boolean {
    val value = raw.trim()
    if (value.isBlank()) return true
    val normalized = value.lowercase()
    if (normalized == "n/a" || normalized == "na" || normalized == "unknown" || normalized == "null") return true
    if (normalized == "not found" || normalized == "none") return true
    if (normalized.contains("permission denial")) return true
    return false
}

internal fun formatEpochMillis(value: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(value))
}

internal fun tryFormatTimeValue(key: String, value: String): String {
    val k = key.lowercase()
    val likelyTime = k.contains("time") || k.contains("timestamp") || k.contains("build_time")
    if (!likelyTime) return value

    val num = value.toLongOrNull() ?: return value
    val millis = if (num in 1..99_999_999_999L) num * 1000 else num
    if (millis <= 0L) return value
    return runCatching { formatEpochMillis(millis) }.getOrDefault(value)
}

internal fun cleanRows(rows: List<InfoRow>): List<InfoRow> {
    val seen = LinkedHashSet<String>()
    return rows
        .map {
            val formatted = tryFormatTimeValue(it.key, it.value.trim())
            InfoRow(it.key.trim(), formatted.trim())
        }
        .filter { it.key.isNotBlank() && !isInvalidValue(it.value) }
        .filter { seen.add("${it.key}\u0000${it.value}") }
}

internal fun execRuntimeCommand(command: String): String? {
    return runCatching {
        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
        process.inputStream.bufferedReader().use { it.readText() }.trim()
    }.getOrNull()?.ifBlank { null }
}

internal fun parseKeyValueLines(raw: String?): List<InfoRow> {
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

internal fun commandRows(command: String, shizukuApiUtils: ShizukuApiUtils): List<InfoRow> {
    val shizuku = shizukuApiUtils.execCommand(command)
    val runtime = if (shizuku.isNullOrBlank()) execRuntimeCommand(command) else null
    return parseKeyValueLines(shizuku ?: runtime)
}

internal fun decodeVulkanApiVersion(version: Int): String {
    if (version <= 0) return ""
    val major = version shr 22
    val minor = (version shr 12) and 0x3ff
    val patch = version and 0xfff
    return "$major.$minor.$patch"
}

internal fun decodeOpenGlEsVersion(raw: String): String {
    val value = raw.toIntOrNull() ?: return ""
    if (value <= 0) return ""
    val major = (value shr 16) and 0xffff
    val minor = value and 0xffff
    return "$major.$minor"
}

internal fun packageVersionName(context: Context, packageName: String): String {
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

internal fun featureVersion(features: Array<FeatureInfo>?, featureName: String): Int {
    return features?.firstOrNull { it.name == featureName }?.version ?: 0
}

internal fun boolFeatureRow(pm: PackageManager, featureName: String, label: String): InfoRow {
    return InfoRow(label, pm.hasSystemFeature(featureName).toString())
}

internal fun graphicsRows(context: Context): List<InfoRow> {
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

internal fun capabilityRows(context: Context): List<InfoRow> {
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

internal fun buildSectionRows(
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

internal data class ExportSections(
    val topInfo: List<InfoRow>,
    val system: List<InfoRow>,
    val secure: List<InfoRow>,
    val global: List<InfoRow>,
    val android: List<InfoRow>,
    val java: List<InfoRow>,
    val linux: List<InfoRow>
)

internal fun buildExportSections(
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

internal fun escapeMarkdown(text: String): String {
    return text.replace("|", "\\|").replace("\n", "<br>")
}

internal fun appendSectionMarkdown(builder: StringBuilder, title: String, rows: List<InfoRow>) {
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

internal fun buildOsMarkdown(
    generatedAt: String,
    shizukuStatus: String,
    sections: ExportSections
): String {
    return buildString {
        appendLine("# KeiOS OS Export")
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

internal fun buildOsCardMarkdown(
    generatedAt: String,
    shizukuStatus: String,
    cardTitle: String,
    rows: List<InfoRow>
): String {
    return buildString {
        appendLine("# KeiOS OS Export")
        appendLine()
        appendLine("- Generated at: $generatedAt")
        appendLine("- Shizuku status: $shizukuStatus")
        appendLine("- Card: $cardTitle")
        appendLine("- Rows: ${rows.size}")
        appendLine("- Format: Markdown (single-card)")
        appendLine()
        appendSectionMarkdown(this, cardTitle, rows)
    }
}

internal fun buildOsCardJson(
    generatedAt: String,
    shizukuStatus: String,
    cardTitle: String,
    rows: List<InfoRow>
): String {
    return JSONObject().apply {
        put("schema", "keios.os.card.v1")
        put("generatedAt", generatedAt)
        put("shizukuStatus", shizukuStatus)
        put("cardTitle", cardTitle)
        put("rowCount", rows.size)
        put(
            "rows",
            JSONArray().apply {
                rows.forEach { row ->
                    put(
                        JSONObject().apply {
                            put("key", row.key)
                            put("value", row.value)
                        }
                    )
                }
            }
        )
    }.toString(2)
}
