package com.example.keios.ui.page.main

import android.os.Build
import java.util.Locale
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.FrostedBlock
import com.example.keios.ui.utils.InfoFactory
import com.example.keios.ui.utils.findJavaPropString
import com.example.keios.ui.utils.findPropString
import com.example.keios.ui.utils.getAllJavaPropString
import com.example.keios.ui.utils.getAllSystemProperties
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField

@Composable
fun SystemPage(
    backdrop: Backdrop?,
    scrollToTopSignal: Int
) {
    var query by remember { mutableStateOf("") }
    var systemPropsExpanded by remember { mutableStateOf(false) }
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
    val systemProps = remember { getAllSystemProperties.toSortedMap() }
    val javaProps = remember { getAllJavaPropString.toSortedMap() }
    val q = query.trim()
    val keyInfoText = remember(q, keyInfoLines) {
        keyInfoLines.filter { q.isEmpty() || it.contains(q, true) }.joinToString("\n")
    }
    val infoFactoryText = remember(q, infoFactoryLines) {
        infoFactoryLines.filter { q.isEmpty() || it.contains(q, true) }.joinToString("\n")
    }
    val filteredSystemProps = remember(q, systemProps) {
        systemProps.entries
            .filter { q.isEmpty() || it.key.contains(q, true) || it.value.contains(q, true) }
    }
    val systemPropsText = remember(filteredSystemProps, systemPropsExpanded) {
        val displayEntries = if (systemPropsExpanded) filteredSystemProps else filteredSystemProps.take(24)
        displayEntries.joinToString("\n") { "${it.key} = ${it.value}" }
    }
    val javaPropsText = remember(q, javaProps) {
        javaProps.entries
            .filter { q.isEmpty() || it.key.contains(q, true) || it.value.contains(q, true) }
            .joinToString("\n") { "${it.key} = ${it.value}" }
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

            FrostedBlock(
                backdrop = backdrop,
                title = "Key Info",
                subtitle = "System / Java properties",
                body = keyInfoText.ifBlank { "No matched results." },
                accent = Color(0xFF5F9DFF)
            )
            Spacer(modifier = Modifier.height(12.dp))
            FrostedBlock(
                backdrop = backdrop,
                title = "InfoFactory",
                subtitle = "Migrated utils snapshot",
                body = infoFactoryText.ifBlank { "No matched results." },
                accent = Color(0xFF7E8CFF)
            )
            Spacer(modifier = Modifier.height(12.dp))
            FrostedBlock(
                backdrop = backdrop,
                title = "getprop",
                subtitle = if (systemPropsExpanded) {
                    "All ${filteredSystemProps.size} entries"
                } else {
                    "Showing ${minOf(24, filteredSystemProps.size)}/${filteredSystemProps.size} entries"
                },
                body = systemPropsText.ifBlank { "No matched results." },
                accent = Color(0xFF6ECF9C)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { systemPropsExpanded = !systemPropsExpanded }
            ) {
                Text(if (systemPropsExpanded) "收起 getprop" else "展开 getprop")
            }
            Spacer(modifier = Modifier.height(12.dp))
            FrostedBlock(
                backdrop = backdrop,
                title = "Java Properties",
                subtitle = "All ${javaProps.size} entries",
                body = javaPropsText.ifBlank { "No matched results." },
                accent = Color(0xFFFFB26B)
            )
        }
    }
}
