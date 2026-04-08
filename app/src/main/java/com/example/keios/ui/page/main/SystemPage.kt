package com.example.keios.ui.page.main

import android.os.Build
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.MiuixExpandableSection
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.utils.InfoFactory
import com.example.keios.ui.utils.ShizukuApiUtils
import com.example.keios.ui.utils.findJavaPropString
import com.example.keios.ui.utils.findPropString
import com.example.keios.ui.utils.getAllJavaPropString
import com.example.keios.ui.utils.getAllSystemProperties
import com.kyant.backdrop.Backdrop
import java.util.Locale
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

@Composable
fun SystemPage(
    backdrop: Backdrop?,
    scrollToTopSignal: Int,
    shizukuStatus: String,
    shizukuApiUtils: ShizukuApiUtils
) {
    var query by remember { mutableStateOf("") }
    var keyInfoExpanded by remember { mutableStateOf(true) }
    var infoFactoryExpanded by remember { mutableStateOf(true) }
    var lowLevelExpanded by remember { mutableStateOf(false) }
    var systemPropsExpanded by remember { mutableStateOf(false) }
    var javaPropsExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) {
            scrollState.animateScrollTo(0)
        }
    }

    val keyInfoRows = remember {
        listOf(
            InfoRow("Brand", Build.BRAND),
            InfoRow("Model", Build.MODEL),
            InfoRow("Device", Build.DEVICE),
            InfoRow("Release", findPropString("ro.build.version.release")),
            InfoRow("SDK", findPropString("ro.build.version.sdk")),
            InfoRow("Build ID", findPropString("ro.build.id")),
            InfoRow("Display ID", findPropString("ro.build.display.id")),
            InfoRow("Security Patch", findPropString("ro.build.version.security_patch")),
            InfoRow("Locale", findPropString("persist.sys.locale", Locale.getDefault().toLanguageTag())),
            InfoRow("Unicode", findJavaPropString("android.icu.unicode.version")),
            InfoRow("OpenSSL", findJavaPropString("android.openssl.version"))
        )
    }

    val infoFactoryRows = remember {
        listOf(
            InfoRow("procVersion", InfoFactory.procVersion),
            InfoRow("abSlot", InfoFactory.abSlot),
            InfoRow("toyboxVersion", InfoFactory.toyboxVersion),
            InfoRow("vendorBuildSecurityPatch", InfoFactory.vendorBuildSecurityPatch),
            InfoRow("miOSVersionName", InfoFactory.miOSVersionName),
            InfoRow("miuiVersionName", InfoFactory.miuiVersionName),
            InfoRow("miuiVersionCode", InfoFactory.miuiVersionCode),
            InfoRow("deviceName", InfoFactory.deviceName),
            InfoRow("zygote", InfoFactory.zygote),
            InfoRow("unicodeVersion", InfoFactory.unicodeVersion),
            InfoRow("opensslVersion", InfoFactory.opensslVersion),
            InfoRow("selinuxPolicy", InfoFactory.selinuxPolicy),
            InfoRow("backgroundBlurSupported", InfoFactory.backgroundBlurSupported.toString()),
            InfoRow("fileSafStatus", InfoFactory.fileSafStatus.toString())
        )
    }

    val lowLevelRows = remember(shizukuStatus) {
        fun prop(key: String, fallback: String = ""): String = findPropString(key, fallback)
        fun privileged(command: String, fallback: String = ""): String {
            return shizukuApiUtils.execCommand(command)?.lineSequence()?.firstOrNull()?.ifBlank { null } ?: fallback
        }
        listOf(
            InfoRow("Shizuku", shizukuStatus),
            InfoRow("ro.build.ab_update", prop("ro.build.ab_update")),
            InfoRow("ro.virtual_ab.enabled", prop("ro.virtual_ab.enabled")),
            InfoRow("ro.virtual_ab.retrofit", prop("ro.virtual_ab.retrofit")),
            InfoRow("ro.boot.slot_suffix", prop("ro.boot.slot_suffix")),
            InfoRow("ro.boot.dynamic_partitions", prop("ro.boot.dynamic_partitions")),
            InfoRow("ro.boot.dynamic_partitions_retrofit", prop("ro.boot.dynamic_partitions_retrofit")),
            InfoRow("ro.treble.enabled", prop("ro.treble.enabled")),
            InfoRow("ro.vndk.version", prop("ro.vndk.version")),
            InfoRow("ro.vndk.lite", prop("ro.vndk.lite")),
            InfoRow("ro.apex.updatable", prop("ro.apex.updatable")),
            InfoRow("ro.adb.secure", prop("ro.adb.secure")),
            InfoRow("ro.secure", prop("ro.secure")),
            InfoRow("ro.debuggable", prop("ro.debuggable")),
            InfoRow("ro.oem_unlock_supported", prop("ro.oem_unlock_supported")),
            InfoRow("ro.boot.flash.locked", prop("ro.boot.flash.locked")),
            InfoRow("ro.boot.verifiedbootstate", prop("ro.boot.verifiedbootstate")),
            InfoRow("ro.boot.veritymode", prop("ro.boot.veritymode")),
            InfoRow("ro.boot.vbmeta.device_state", prop("ro.boot.vbmeta.device_state")),
            InfoRow("ro.boot.vbmeta.avb_version", prop("ro.boot.vbmeta.avb_version")),
            InfoRow("ro.boot.avb_version", prop("ro.boot.avb_version")),
            InfoRow("ro.boot.secureboot", prop("ro.boot.secureboot")),
            InfoRow("ro.product.first_api_level", prop("ro.product.first_api_level")),
            InfoRow("ro.board.first_api_level", prop("ro.board.first_api_level")),
            InfoRow("getenforce (Shizuku)", privileged("getenforce", "N/A")),
            InfoRow("toybox --version (Shizuku)", privileged("toybox --version", "N/A")),
            InfoRow("uname -a (Shizuku)", privileged("uname -a", "N/A")),
            InfoRow("cat /sys/fs/selinux/policyvers (Shizuku)", privileged("cat /sys/fs/selinux/policyvers", "N/A")),
            InfoRow("ls -l /dev/block/by-name/super (Shizuku)", privileged("ls -l /dev/block/by-name/super", "N/A"))
        )
    }

    val systemPropRows = remember {
        getAllSystemProperties
            .toSortedMap()
            .map { InfoRow(it.key, it.value) }
    }

    val javaPropRows = remember {
        getAllJavaPropString
            .toSortedMap()
            .map { InfoRow(it.key, it.value) }
    }

    val q = query.trim()
    val filteredKeyInfoRows = remember(q, keyInfoRows) { keyInfoRows.filter { matches(it, q) } }
    val filteredInfoFactoryRows = remember(q, infoFactoryRows) { infoFactoryRows.filter { matches(it, q) } }
    val filteredLowLevelRows = remember(q, lowLevelRows) { lowLevelRows.filter { matches(it, q) } }
    val filteredSystemPropRows = remember(q, systemPropRows) { systemPropRows.filter { matches(it, q) } }
    val filteredJavaPropRows = remember(q, javaPropRows) { javaPropRows.filter { matches(it, q) } }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(bottom = 72.dp)
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
                title = "Key Info",
                subtitle = "${filteredKeyInfoRows.size} 条",
                expanded = keyInfoExpanded,
                onExpandedChange = { keyInfoExpanded = it }
            ) {
                if (filteredKeyInfoRows.isEmpty()) {
                    Text(text = "No matched results.")
                } else {
                    filteredKeyInfoRows.forEach { row ->
                        MiuixInfoItem(key = row.key, value = row.value)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            MiuixExpandableSection(
                backdrop = backdrop,
                title = "InfoFactory",
                subtitle = "${filteredInfoFactoryRows.size} 条",
                expanded = infoFactoryExpanded,
                onExpandedChange = { infoFactoryExpanded = it }
            ) {
                if (filteredInfoFactoryRows.isEmpty()) {
                    Text(text = "No matched results.")
                } else {
                    filteredInfoFactoryRows.forEach { row ->
                        MiuixInfoItem(key = row.key, value = row.value)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            MiuixExpandableSection(
                backdrop = backdrop,
                title = "AndroidLowLevelDetector",
                subtitle = "${filteredLowLevelRows.size} 条",
                expanded = lowLevelExpanded,
                onExpandedChange = { lowLevelExpanded = it }
            ) {
                if (filteredLowLevelRows.isEmpty()) {
                    Text(text = "No matched results.")
                } else {
                    filteredLowLevelRows.forEach { row ->
                        MiuixInfoItem(key = row.key, value = row.value)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            MiuixExpandableSection(
                backdrop = backdrop,
                title = "getprop",
                subtitle = "${filteredSystemPropRows.size} 条（默认收纳）",
                expanded = systemPropsExpanded,
                onExpandedChange = { systemPropsExpanded = it }
            ) {
                if (filteredSystemPropRows.isEmpty()) {
                    Text(text = "No matched results.")
                } else {
                    filteredSystemPropRows.forEach { row ->
                        MiuixInfoItem(key = row.key, value = row.value)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            MiuixExpandableSection(
                backdrop = backdrop,
                title = "Java Properties",
                subtitle = "${filteredJavaPropRows.size} 条",
                expanded = javaPropsExpanded,
                onExpandedChange = { javaPropsExpanded = it }
            ) {
                if (filteredJavaPropRows.isEmpty()) {
                    Text(text = "No matched results.")
                } else {
                    filteredJavaPropRows.forEach { row ->
                        MiuixInfoItem(key = row.key, value = row.value)
                    }
                }
            }
        }
    }
}
