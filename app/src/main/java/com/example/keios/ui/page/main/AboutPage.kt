package com.example.keios.ui.page.main

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.MiuixExpandableSection
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.kyant.backdrop.Backdrop
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text

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
    onCheckShizuku: () -> Unit
) {
    var identityExpanded by remember { mutableStateOf(true) }
    var buildExpanded by remember { mutableStateOf(true) }
    var installExpanded by remember { mutableStateOf(true) }
    var runtimeExpanded by remember { mutableStateOf(false) }
    var permissionExpanded by remember { mutableStateOf(false) }

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
            AboutRow("Shizuku API", "13.1.5")
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
        listOf(
            AboutRow("Shizuku Status", shizukuStatus),
            AboutRow("Shizuku Ready", shizukuStatus.contains("granted", ignoreCase = true).toString())
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = "About", modifier = Modifier.padding(top = 6.dp))
        Text(text = "权限检查与应用详情", modifier = Modifier.padding(top = 4.dp))
        Spacer(modifier = Modifier.height(14.dp))

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
            subtitle = "${permissionRows.size} 条",
            expanded = permissionExpanded,
            onExpandedChange = { permissionExpanded = it }
        ) {
            permissionRows.forEach { row -> MiuixInfoItem(row.key, row.value) }
        }
    }
}
