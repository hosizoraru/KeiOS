package com.example.keios.ui.page.main

import android.content.pm.PackageInfo
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.FrostedBlock
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop

@Composable
fun AboutPage(
    backdrop: LayerBackdrop,
    appLabel: String,
    packageInfo: PackageInfo,
    shizukuStatus: String,
    onCheckShizuku: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "About", modifier = Modifier.padding(top = 6.dp))
        Text(text = "权限检查与应用详情", modifier = Modifier.padding(top = 4.dp))
        Spacer(modifier = Modifier.height(14.dp))

        FrostedBlock(
            backdrop = backdrop,
            title = "Shizuku",
            subtitle = "Permission Center",
            body = shizukuStatus,
            accent = Color(0xFFFF9F7A)
        )

        Button(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth(),
            onClick = onCheckShizuku
        ) {
            Text(text = "检查 / 申请 Shizuku 权限")
        }

        Spacer(modifier = Modifier.height(12.dp))
        FrostedBlock(
            backdrop = backdrop,
            title = appLabel,
            subtitle = "App Details",
            body = buildString {
                val appInfo = packageInfo.applicationInfo
                appendLine("Package: ${packageInfo.packageName}")
                appendLine("Version: ${packageInfo.versionName ?: "unknown"}")
                appendLine("VersionCode: ${packageInfo.longVersionCode}")
                appendLine("Min SDK: ${appInfo?.minSdkVersion ?: "unknown"}")
                appendLine("Target SDK: ${appInfo?.targetSdkVersion ?: "unknown"}")
                appendLine("Device API: ${Build.VERSION.SDK_INT}")
                append("Miuix: 0.9.0")
            },
            accent = Color(0xFF8C7BFF)
        )
    }
}
