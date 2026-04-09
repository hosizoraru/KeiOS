package com.example.keios.ui.page.main

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.StatusPill
import com.example.keios.ui.utils.ShizukuApiUtils
import com.kyant.backdrop.Backdrop
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private fun formatTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return "N/A"
    return runCatching {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        formatter.format(Date(epochMillis))
    }.getOrDefault("N/A")
}

@Composable
fun AboutPage(
    appLabel: String,
    packageInfo: PackageInfo?,
    notificationPermissionGranted: Boolean,
    shizukuStatus: String,
    shizukuApiUtils: ShizukuApiUtils,
    onCheckShizuku: () -> Unit,
    contentBottomPadding: Dp = 72.dp,
    scrollToTopSignal: Int = 0
) {
    val readyColor = Color(0xFF2E7D32)
    val notReadyColor = Color(0xFFC62828)
    val titleColor = MiuixTheme.colorScheme.onBackground
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) {
            scrollState.animateScrollTo(0)
        }
    }

    val appInfo: ApplicationInfo? = packageInfo?.applicationInfo

    val shizukuDetailMap = remember(shizukuStatus) {
        shizukuApiUtils.detailedRows()
            .toMap()
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.defaultColors(
                color = if (shizukuReady) readyColor.copy(alpha = 0.16f) else notReadyColor.copy(alpha = 0.16f),
                contentColor = titleColor
            ),
            showIndication = true,
            onClick = onCheckShizuku
        ) {
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
                    Text("Overview", color = titleColor)
                    StatusPill(
                        label = if (shizukuReady) "Shizuku Ready" else "Shizuku Limited",
                        color = if (shizukuReady) readyColor else notReadyColor
                    )
                }
                MiuixInfoItem(
                    "App",
                    buildString {
                        append(appLabel)
                        append(" · ")
                        append(packageInfo?.packageName ?: "unknown")
                        append(" · Debuggable ")
                        append(((appInfo?.flags ?: 0) and ApplicationInfo.FLAG_DEBUGGABLE != 0))
                    }
                )
                MiuixInfoItem(
                    "Version",
                    "${packageInfo?.versionName ?: "unknown"} (${packageInfo?.longVersionCode ?: -1})"
                )
                MiuixInfoItem(
                    "Runtime",
                    buildString {
                        append("API ")
                        append(Build.VERSION.SDK_INT)
                        append(" · Security Patch ")
                        append(Build.VERSION.SECURITY_PATCH ?: "unknown")
                        append(" · Last Update ")
                        append(packageInfo?.lastUpdateTime?.let(::formatTime) ?: "unknown")
                    }
                )
                MiuixInfoItem(
                    "Framework",
                    "Miuix UI 0.9.0 · Shizuku API ${ShizukuApiUtils.API_VERSION}"
                )
                MiuixInfoItem(
                    "Permissions & Service",
                    buildString {
                        append("Notification ")
                        append(if (notificationPermissionGranted) "granted" else "not granted")
                        append(" · uname ")
                        append(shizukuDetailMap["Shizuku uname"] ?: "N/A")
                        append(" · SELinux ")
                        append(shizukuDetailMap["Shizuku getenforce"] ?: "N/A")
                    }
                )
            }
        }
    }
}
