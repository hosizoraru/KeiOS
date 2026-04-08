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

    val keyInfoLines = remember {
        listOf(
            "Brand = ${Build.BRAND}",
            "Model = ${Build.MODEL}",
            "Device = ${Build.DEVICE}",
            "Release = ${findPropString("ro.build.version.release")}",
            "SDK = ${findPropString("ro.build.version.sdk")}",
            "Build ID = ${findPropString("ro.build.id")}",
            "Display ID = ${findPropString("ro.build.display.id")}",
            "Security Patch = ${findPropString("ro.build.version.security_patch")}",
            "Locale = ${findPropString("persist.sys.locale", Locale.getDefault().toLanguageTag())}",
            "Unicode = ${findJavaPropString("android.icu.unicode.version")}",
            "OpenSSL = ${findJavaPropString("android.openssl.version")}"
        )
    }
    val infoFactoryLines = remember {
        listOf(
            "procVersion = ${InfoFactory.procVersion}",
            "abSlot = ${InfoFactory.abSlot}",
            "toyboxVersion = ${InfoFactory.toyboxVersion}",
            "vendorBuildSecurityPatch = ${InfoFactory.vendorBuildSecurityPatch}",
            "miOSVersionName = ${InfoFactory.miOSVersionName}",
            "miuiVersionName = ${InfoFactory.miuiVersionName}",
            "miuiVersionCode = ${InfoFactory.miuiVersionCode}",
            "deviceName = ${InfoFactory.deviceName}",
            "zygote = ${InfoFactory.zygote}",
            "unicodeVersion = ${InfoFactory.unicodeVersion}",
            "opensslVersion = ${InfoFactory.opensslVersion}",
            "selinuxPolicy = ${InfoFactory.selinuxPolicy}",
            "backgroundBlurSupported = ${InfoFactory.backgroundBlurSupported}",
            "fileSafStatus = ${InfoFactory.fileSafStatus}"
        )
    }
    val lowLevelDetectorLines = remember(shizukuStatus) {
        fun prop(key: String, fallback: String = ""): String = findPropString(key, fallback)
        fun privileged(command: String, fallback: String = ""): String {
            return shizukuApiUtils.execCommand(command)?.lineSequence()?.firstOrNull()?.ifBlank { null }
                ?: fallback
        }
        listOf(
            "Shizuku = $shizukuStatus",
            "ro.build.ab_update = ${prop("ro.build.ab_update")}",
            "ro.virtual_ab.enabled = ${prop("ro.virtual_ab.enabled")}",
            "ro.virtual_ab.retrofit = ${prop("ro.virtual_ab.retrofit")}",
            "ro.boot.slot_suffix = ${prop("ro.boot.slot_suffix")}",
            "ro.boot.dynamic_partitions = ${prop("ro.boot.dynamic_partitions")}",
            "ro.boot.dynamic_partitions_retrofit = ${prop("ro.boot.dynamic_partitions_retrofit")}",
            "ro.treble.enabled = ${prop("ro.treble.enabled")}",
            "ro.vndk.version = ${prop("ro.vndk.version")}",
            "ro.vndk.lite = ${prop("ro.vndk.lite")}",
            "ro.apex.updatable = ${prop("ro.apex.updatable")}",
            "ro.adb.secure = ${prop("ro.adb.secure")}",
            "ro.secure = ${prop("ro.secure")}",
            "ro.debuggable = ${prop("ro.debuggable")}",
            "ro.oem_unlock_supported = ${prop("ro.oem_unlock_supported")}",
            "ro.boot.flash.locked = ${prop("ro.boot.flash.locked")}",
            "ro.boot.verifiedbootstate = ${prop("ro.boot.verifiedbootstate")}",
            "ro.boot.veritymode = ${prop("ro.boot.veritymode")}",
            "ro.boot.vbmeta.device_state = ${prop("ro.boot.vbmeta.device_state")}",
            "ro.boot.vbmeta.avb_version = ${prop("ro.boot.vbmeta.avb_version")}",
            "ro.boot.avb_version = ${prop("ro.boot.avb_version")}",
            "ro.boot.secureboot = ${prop("ro.boot.secureboot")}",
            "ro.product.first_api_level = ${prop("ro.product.first_api_level")}",
            "ro.board.first_api_level = ${prop("ro.board.first_api_level")}",
            "getenforce (Shizuku) = ${privileged("getenforce", "N/A")}",
            "toybox --version (Shizuku) = ${privileged("toybox --version", "N/A")}",
            "uname -a (Shizuku) = ${privileged("uname -a", "N/A")}",
            "cat /sys/fs/selinux/policyvers (Shizuku) = ${privileged("cat /sys/fs/selinux/policyvers", "N/A")}",
            "ls -l /dev/block/by-name/super (Shizuku) = ${privileged("ls -l /dev/block/by-name/super", "N/A")}"
        )
    }

    val systemProps = remember { getAllSystemProperties.toSortedMap() }
    val javaProps = remember { getAllJavaPropString.toSortedMap() }
    val q = query.trim()

    val filteredKeyInfoLines = remember(q, keyInfoLines) {
        keyInfoLines.filter { q.isEmpty() || it.contains(q, true) }
    }
    val filteredInfoFactoryLines = remember(q, infoFactoryLines) {
        infoFactoryLines.filter { q.isEmpty() || it.contains(q, true) }
    }
    val filteredLowLevelLines = remember(q, lowLevelDetectorLines) {
        lowLevelDetectorLines.filter { q.isEmpty() || it.contains(q, true) }
    }
    val filteredSystemProps = remember(q, systemProps) {
        systemProps.entries.filter { q.isEmpty() || it.key.contains(q, true) || it.value.contains(q, true) }
    }
    val filteredJavaProps = remember(q, javaProps) {
        javaProps.entries.filter { q.isEmpty() || it.key.contains(q, true) || it.value.contains(q, true) }
    }

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
                subtitle = "${filteredKeyInfoLines.size} 条",
                expanded = keyInfoExpanded,
                onExpandedChange = { keyInfoExpanded = it }
            ) {
                Text(
                    text = filteredKeyInfoLines.joinToString("\n").ifBlank { "No matched results." }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            MiuixExpandableSection(
                backdrop = backdrop,
                title = "InfoFactory",
                subtitle = "${filteredInfoFactoryLines.size} 条",
                expanded = infoFactoryExpanded,
                onExpandedChange = { infoFactoryExpanded = it }
            ) {
                Text(
                    text = filteredInfoFactoryLines.joinToString("\n").ifBlank { "No matched results." }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            MiuixExpandableSection(
                backdrop = backdrop,
                title = "AndroidLowLevelDetector",
                subtitle = "${filteredLowLevelLines.size} 条",
                expanded = lowLevelExpanded,
                onExpandedChange = { lowLevelExpanded = it }
            ) {
                Text(
                    text = filteredLowLevelLines.joinToString("\n").ifBlank { "No matched results." }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            MiuixExpandableSection(
                backdrop = backdrop,
                title = "getprop",
                subtitle = "${filteredSystemProps.size} 条（默认收纳）",
                expanded = systemPropsExpanded,
                onExpandedChange = { systemPropsExpanded = it }
            ) {
                Text(
                    text = filteredSystemProps.joinToString("\n") { "${it.key} = ${it.value}" }
                        .ifBlank { "No matched results." }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            MiuixExpandableSection(
                backdrop = backdrop,
                title = "Java Properties",
                subtitle = "${filteredJavaProps.size} 条",
                expanded = javaPropsExpanded,
                onExpandedChange = { javaPropsExpanded = it }
            ) {
                Text(
                    text = filteredJavaProps.joinToString("\n") { "${it.key} = ${it.value}" }
                        .ifBlank { "No matched results." }
                )
            }
        }
    }
}
