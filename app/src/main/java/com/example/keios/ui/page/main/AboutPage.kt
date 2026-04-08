package com.example.keios.ui.page.main

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.FrostedBlock
import com.example.keios.ui.page.main.widget.MiuixExpandableSection
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.StatusPill
import com.example.keios.ui.utils.ShizukuApiUtils
import com.kyant.backdrop.Backdrop
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class AboutRow(
    val key: String,
    val value: String
)

private fun formatTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return "N/A"
    return runCatching {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        formatter.format(Date(epochMillis))
    }.getOrDefault("N/A")
}

@Composable
fun AboutPage(
    backdrop: Backdrop?,
    appLabel: String,
    packageInfo: PackageInfo?,
    shizukuStatus: String,
    shizukuApiUtils: ShizukuApiUtils,
    onCheckShizuku: () -> Unit,
    contentBottomPadding: Dp = 72.dp,
    scrollToTopSignal: Int = 0
) {
    val primary = MiuixTheme.colorScheme.primary
    val success = MiuixTheme.colorScheme.secondary
    val inactive = MiuixTheme.colorScheme.onBackgroundVariant
    val titleColor = MiuixTheme.colorScheme.onBackground
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant

    var identityExpanded by remember { mutableStateOf(false) }
    var buildExpanded by remember { mutableStateOf(false) }
    var installExpanded by remember { mutableStateOf(false) }
    var runtimeExpanded by remember { mutableStateOf(false) }
    var permissionExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) {
            scrollState.animateScrollTo(0)
        }
    }

    val appInfo: ApplicationInfo? = packageInfo?.applicationInfo

    val identityRows = remember(packageInfo, appInfo, appLabel) {
        listOf(
            AboutRow("App Label", appLabel),
            AboutRow("Package Name", packageInfo?.packageName ?: "unknown"),
            AboutRow("Process Name", appInfo?.processName ?: "unknown"),
            AboutRow("UID", appInfo?.uid?.toString() ?: "unknown"),
            AboutRow("Enabled", (appInfo?.enabled ?: false).toString()),
            AboutRow("System App", ((appInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0).toString()),
            AboutRow("Debuggable", ((appInfo?.flags ?: 0) and ApplicationInfo.FLAG_DEBUGGABLE != 0).toString())
        )
    }

    val buildRows = remember(packageInfo, appInfo) {
        listOf(
            AboutRow("Version Name", packageInfo?.versionName ?: "unknown"),
            AboutRow("Version Code", packageInfo?.longVersionCode?.toString() ?: "unknown"),
            AboutRow("Package (build id)", packageInfo?.packageName ?: "unknown"),
            AboutRow("Target SDK", appInfo?.targetSdkVersion?.toString() ?: "unknown"),
            AboutRow("Min SDK", appInfo?.minSdkVersion?.toString() ?: "unknown"),
            AboutRow("Build Fingerprint", Build.FINGERPRINT),
            AboutRow("Build Type", Build.TYPE),
            AboutRow("Build Tags", Build.TAGS),
            AboutRow("Miuix UI", "0.9.0"),
            AboutRow("Shizuku API", ShizukuApiUtils.API_VERSION)
        )
    }

    val installRows = remember(packageInfo, appInfo) {
        val splitCount = appInfo?.splitSourceDirs?.size ?: 0
        val splitPaths = appInfo?.splitSourceDirs?.joinToString("\n") ?: "N/A"
        listOf(
            AboutRow("First Install Time", packageInfo?.firstInstallTime?.let(::formatTime) ?: "unknown"),
            AboutRow("Last Update Time", packageInfo?.lastUpdateTime?.let(::formatTime) ?: "unknown"),
            AboutRow("Source Dir", appInfo?.sourceDir ?: "unknown"),
            AboutRow("Public Source Dir", appInfo?.publicSourceDir ?: "unknown"),
            AboutRow("Native Library Dir", appInfo?.nativeLibraryDir ?: "unknown"),
            AboutRow("Data Dir", appInfo?.dataDir ?: "unknown"),
            AboutRow("Split APK Count", splitCount.toString()),
            AboutRow("Split APK Paths", splitPaths)
        )
    }

    val runtimeRows = remember {
        listOf(
            AboutRow("Device API", Build.VERSION.SDK_INT.toString()),
            AboutRow("Release", Build.VERSION.RELEASE ?: "unknown"),
            AboutRow("Codename", Build.VERSION.CODENAME ?: "unknown"),
            AboutRow("Security Patch", Build.VERSION.SECURITY_PATCH ?: "unknown"),
            AboutRow("Brand", Build.BRAND),
            AboutRow("Manufacturer", Build.MANUFACTURER),
            AboutRow("Model", Build.MODEL),
            AboutRow("Device", Build.DEVICE),
            AboutRow("Product", Build.PRODUCT),
            AboutRow("Supported ABIs", Build.SUPPORTED_ABIS.joinToString(", "))
        )
    }

    val permissionRows = remember(shizukuStatus) {
        mutableListOf(
            AboutRow("Shizuku Status", shizukuStatus),
            AboutRow("Shizuku Ready", shizukuStatus.contains("granted", ignoreCase = true).toString())
        )
    }

    val shizukuDetailRows = remember(shizukuStatus) {
        shizukuApiUtils.detailedRows()
            .map { AboutRow(it.first, it.second) }
    }

    val permissionSectionRows = remember(permissionRows, shizukuDetailRows) {
        val activated = permissionRows.any {
            it.key == "Shizuku Ready" && it.value.equals("true", ignoreCase = true)
        }
        if (activated) {
            permissionRows + shizukuDetailRows
        } else {
            permissionRows + listOf(AboutRow("Shizuku Detail", "激活并授权后显示更多信息"))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = contentBottomPadding)
    ) {
        val shizukuReady = shizukuStatus.contains("granted", ignoreCase = true)
        Text(text = "About", color = titleColor, modifier = Modifier.padding(top = 6.dp))
        Text(text = "权限检查与应用详情", color = subtitleColor, modifier = Modifier.padding(top = 4.dp))
        Spacer(modifier = Modifier.height(14.dp))

        FrostedBlock(
            backdrop = backdrop,
            title = "Overview",
            subtitle = "应用与权限状态总览",
            accent = primary,
            content = {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatusPill(
                        label = "Build",
                        color = primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusPill(
                        label = if (shizukuReady) "Shizuku Ready" else "Shizuku Limited",
                        color = if (shizukuReady) success else inactive
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                MiuixInfoItem("Build Version", packageInfo?.versionName ?: "unknown")
                MiuixInfoItem("Package", packageInfo?.packageName ?: "unknown")
                MiuixInfoItem("Version", "${packageInfo?.versionName ?: "unknown"} (${packageInfo?.longVersionCode ?: -1})")
                MiuixInfoItem("Target SDK", appInfo?.targetSdkVersion?.toString() ?: "unknown")
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onCheckShizuku
        ) {
            Text(text = "检查 / 申请 Shizuku 权限")
        }

        Spacer(modifier = Modifier.height(12.dp))

        MiuixExpandableSection(
            backdrop = backdrop,
            title = "应用身份信息",
            subtitle = "${identityRows.size} 条",
            expanded = identityExpanded,
            onExpandedChange = { identityExpanded = it }
        ) {
            identityRows.forEach { row -> MiuixInfoItem(row.key, row.value) }
        }

        Spacer(modifier = Modifier.height(12.dp))

        MiuixExpandableSection(
            backdrop = backdrop,
            title = "构建与版本信息",
            subtitle = "${buildRows.size} 条",
            expanded = buildExpanded,
            onExpandedChange = { buildExpanded = it }
        ) {
            buildRows.forEach { row -> MiuixInfoItem(row.key, row.value) }
        }

        Spacer(modifier = Modifier.height(12.dp))

        MiuixExpandableSection(
            backdrop = backdrop,
            title = "安装与路径信息",
            subtitle = "${installRows.size} 条",
            expanded = installExpanded,
            onExpandedChange = { installExpanded = it }
        ) {
            installRows.forEach { row -> MiuixInfoItem(row.key, row.value) }
        }

        Spacer(modifier = Modifier.height(12.dp))

        MiuixExpandableSection(
            backdrop = backdrop,
            title = "运行环境信息",
            subtitle = "${runtimeRows.size} 条",
            expanded = runtimeExpanded,
            onExpandedChange = { runtimeExpanded = it }
        ) {
            runtimeRows.forEach { row -> MiuixInfoItem(row.key, row.value) }
        }

        Spacer(modifier = Modifier.height(12.dp))

        MiuixExpandableSection(
            backdrop = backdrop,
            title = "权限与服务状态",
            subtitle = "${permissionSectionRows.size} 条",
            expanded = permissionExpanded,
            onExpandedChange = { permissionExpanded = it }
        ) {
            permissionSectionRows.forEach { row -> MiuixInfoItem(row.key, row.value) }
        }
    }
}
