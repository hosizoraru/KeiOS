package com.example.keios.ui.page.main

import android.app.ActivityManager
import android.content.Context
import android.content.pm.FeatureInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.MiuixExpandableSection
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.utils.ShizukuApiUtils
import com.example.keios.ui.utils.getAllJavaPropString
import com.example.keios.ui.utils.getAllSystemProperties
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField

private data class InfoRow(
    val key: String,
    val value: String
)

private fun matches(row: InfoRow, query: String): Boolean {
    if (query.isBlank()) return true
    return row.key.contains(query, ignoreCase = true) || row.value.contains(query, ignoreCase = true)
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

private fun cleanRows(rows: List<InfoRow>): List<InfoRow> {
    val seen = LinkedHashSet<String>()
    return rows
        .map { InfoRow(it.key.trim(), it.value.trim()) }
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

    val vulkanVersionEncoded = featureVersion(features, "android.hardware.vulkan.version")
    val vulkanLevel = featureVersion(features, "android.hardware.vulkan.level")
    val vulkanVersionText = decodeVulkanApiVersion(vulkanVersionEncoded)

    return listOf(
        InfoRow("graphics.opengl.es", glEsVersion),
        InfoRow("graphics.vulkan.version", vulkanVersionText),
        InfoRow("graphics.vulkan.level", if (vulkanLevel > 0) vulkanLevel.toString() else ""),
        boolFeatureRow(pm, "android.hardware.vulkan.version", "feature.vulkan.version"),
        boolFeatureRow(pm, "android.hardware.vulkan.level", "feature.vulkan.level"),
        boolFeatureRow(pm, "android.hardware.opengles.aep", "feature.opengl.aep"),
        boolFeatureRow(pm, PackageManager.FEATURE_OPENGLES_EXTENSION_PACK, "feature.opengles.extension_pack"),
        InfoRow("prop.ro.hardware.egl", execRuntimeCommand("getprop ro.hardware.egl").orEmpty()),
        InfoRow("prop.ro.hardware.vulkan", execRuntimeCommand("getprop ro.hardware.vulkan").orEmpty())
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

@Composable
fun SystemPage(
    backdrop: Backdrop?,
    scrollToTopSignal: Int,
    shizukuStatus: String,
    shizukuApiUtils: ShizukuApiUtils,
    contentBottomPadding: Dp = 72.dp
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var systemTableExpanded by remember { mutableStateOf(true) }
    var secureTableExpanded by remember { mutableStateOf(false) }
    var globalTableExpanded by remember { mutableStateOf(false) }
    var androidPropsExpanded by remember { mutableStateOf(false) }
    var javaPropsExpanded by remember { mutableStateOf(false) }
    var linuxEnvExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) {
            scrollState.animateScrollTo(0)
        }
    }

    val systemTableRows = remember(shizukuStatus) {
        cleanRows(commandRows("settings list system", shizukuApiUtils))
    }
    val secureTableRows = remember(shizukuStatus) {
        cleanRows(commandRows("settings list secure", shizukuApiUtils))
    }
    val globalTableRows = remember(shizukuStatus) {
        cleanRows(commandRows("settings list global", shizukuApiUtils))
    }

    val androidPropertiesRows = remember(context) {
        cleanRows(
            graphicsRows(context) +
                capabilityRows(context) +
                getAllSystemProperties
                    .toSortedMap()
                    .map { InfoRow(it.key, it.value) }
        )
    }

    val javaPropertiesRows = remember {
        cleanRows(
            getAllJavaPropString
                .toSortedMap()
                .map { InfoRow(it.key, it.value) }
        )
    }

    val linuxEnvironmentRows = remember(shizukuStatus) {
        val runtimeUname = execRuntimeCommand("uname -a")
        val runtimeGetenforce = execRuntimeCommand("getenforce")
        val runtimeProcVersion = execRuntimeCommand("cat /proc/version")
        val runtimeToybox = execRuntimeCommand("toybox --version")

        val shizukuUname = shizukuApiUtils.execCommand("uname -a")
        val shizukuGetenforce = shizukuApiUtils.execCommand("getenforce")
        val shizukuProcVersion = shizukuApiUtils.execCommand("cat /proc/version")
        val shizukuToybox = shizukuApiUtils.execCommand("toybox --version")

        val envRows = System.getenv()
            .toSortedMap()
            .map { InfoRow("env.${it.key}", it.value) }

        cleanRows(
            listOf(
                InfoRow("Shizuku Status", shizukuStatus),
                InfoRow("uname -a", shizukuUname?.lineSequence()?.firstOrNull() ?: runtimeUname.orEmpty()),
                InfoRow("getenforce", shizukuGetenforce?.lineSequence()?.firstOrNull() ?: runtimeGetenforce.orEmpty()),
                InfoRow("proc.version", shizukuProcVersion?.lineSequence()?.firstOrNull() ?: runtimeProcVersion.orEmpty()),
                InfoRow("toybox --version", shizukuToybox?.lineSequence()?.firstOrNull() ?: runtimeToybox.orEmpty())
            ) + envRows
        )
    }

    val q = query.trim()
    val filteredSystemTableRows = remember(q, systemTableRows) { systemTableRows.filter { matches(it, q) } }
    val filteredSecureTableRows = remember(q, secureTableRows) { secureTableRows.filter { matches(it, q) } }
    val filteredGlobalTableRows = remember(q, globalTableRows) { globalTableRows.filter { matches(it, q) } }
    val filteredAndroidPropertiesRows = remember(q, androidPropertiesRows) { androidPropertiesRows.filter { matches(it, q) } }
    val filteredJavaPropertiesRows = remember(q, javaPropertiesRows) { javaPropertiesRows.filter { matches(it, q) } }
    val filteredLinuxEnvironmentRows = remember(q, linuxEnvironmentRows) { linuxEnvironmentRows.filter { matches(it, q) } }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(bottom = contentBottomPadding)
        ) {
            Text(text = "System", modifier = Modifier.padding(top = 6.dp))
            Text(text = "系统参数与属性", modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.height(10.dp))
            TextField(
                value = query,
                onValueChange = { query = it },
                label = "搜索系统参数",
                useLabelAsPlaceholder = true,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(14.dp))

            MiuixExpandableSection(
                backdrop = backdrop,
                title = "System Table",
                subtitle = "${filteredSystemTableRows.size} 条",
                expanded = systemTableExpanded,
                onExpandedChange = { systemTableExpanded = it }
            ) {
                if (filteredSystemTableRows.isEmpty()) {
                    Text(text = "No matched results.")
                } else {
                    filteredSystemTableRows.forEach { row -> MiuixInfoItem(row.key, row.value) }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            MiuixExpandableSection(
                backdrop = backdrop,
                title = "Secure Table",
                subtitle = "${filteredSecureTableRows.size} 条",
                expanded = secureTableExpanded,
                onExpandedChange = { secureTableExpanded = it }
            ) {
                if (filteredSecureTableRows.isEmpty()) {
                    Text(text = "No matched results.")
                } else {
                    filteredSecureTableRows.forEach { row -> MiuixInfoItem(row.key, row.value) }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            MiuixExpandableSection(
                backdrop = backdrop,
                title = "Global Table",
                subtitle = "${filteredGlobalTableRows.size} 条",
                expanded = globalTableExpanded,
                onExpandedChange = { globalTableExpanded = it }
            ) {
                if (filteredGlobalTableRows.isEmpty()) {
                    Text(text = "No matched results.")
                } else {
                    filteredGlobalTableRows.forEach { row -> MiuixInfoItem(row.key, row.value) }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            MiuixExpandableSection(
                backdrop = backdrop,
                title = "Android Properties",
                subtitle = "${filteredAndroidPropertiesRows.size} 条",
                expanded = androidPropsExpanded,
                onExpandedChange = { androidPropsExpanded = it }
            ) {
                if (filteredAndroidPropertiesRows.isEmpty()) {
                    Text(text = "No matched results.")
                } else {
                    filteredAndroidPropertiesRows.forEach { row -> MiuixInfoItem(row.key, row.value) }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            MiuixExpandableSection(
                backdrop = backdrop,
                title = "Java Properties",
                subtitle = "${filteredJavaPropertiesRows.size} 条",
                expanded = javaPropsExpanded,
                onExpandedChange = { javaPropsExpanded = it }
            ) {
                if (filteredJavaPropertiesRows.isEmpty()) {
                    Text(text = "No matched results.")
                } else {
                    filteredJavaPropertiesRows.forEach { row -> MiuixInfoItem(row.key, row.value) }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            MiuixExpandableSection(
                backdrop = backdrop,
                title = "Linux environment",
                subtitle = "${filteredLinuxEnvironmentRows.size} 条",
                expanded = linuxEnvExpanded,
                onExpandedChange = { linuxEnvExpanded = it }
            ) {
                if (filteredLinuxEnvironmentRows.isEmpty()) {
                    Text(text = "No matched results.")
                } else {
                    filteredLinuxEnvironmentRows.forEach { row -> MiuixInfoItem(row.key, row.value) }
                }
            }
        }
    }
}
