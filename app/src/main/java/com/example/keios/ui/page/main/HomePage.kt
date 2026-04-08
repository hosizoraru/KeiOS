package com.example.keios.ui.page.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.FrostedBlock
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.StatusPill
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HomePage(
    backdrop: Backdrop?,
    shizukuStatus: String,
    mcpRunning: Boolean,
    mcpPort: Int,
    shizukuApiVersion: String,
    mcpConnectedClients: Int,
    onOpenSettings: () -> Unit
) {
    val shizukuGranted = shizukuStatus.contains("granted", ignoreCase = true)
    val accent = MiuixTheme.colorScheme.primary
    val runningColor = MiuixTheme.colorScheme.primary
    val inactiveColor = MiuixTheme.colorScheme.onBackgroundVariant
    val infoColor = MiuixTheme.colorScheme.secondary
    val titleColor = MiuixTheme.colorScheme.onBackground
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant
    val shizukuState = when {
        shizukuGranted -> "已授权"
        else -> "待检查"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "KeiOS", color = titleColor)
                Text(text = "Dashboard", color = subtitleColor, modifier = Modifier.padding(top = 4.dp))
            }
            Button(onClick = onOpenSettings) {
                Icon(
                    imageVector = MiuixIcons.Regular.Settings,
                    contentDescription = "设置",
                    tint = MiuixTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        FrostedBlock(
            backdrop = backdrop,
            title = "Runtime Status",
            subtitle = "MCP / Shizuku 实时状态",
            accent = accent,
            content = {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatusPill(
                        label = "MCP ${if (mcpRunning) "Running" else "Stopped"}",
                        color = if (mcpRunning) runningColor else inactiveColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusPill(
                        label = "Shizuku $shizukuState",
                        color = if (shizukuGranted) infoColor else inactiveColor
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                MiuixInfoItem(
                    "MCP Server",
                    "${if (mcpRunning) "运行中" else "未运行"} · 在线 $mcpConnectedClients · $mcpPort 端口 · MCP 协议"
                )
                MiuixInfoItem("Shizuku 授权", "$shizukuState · API $shizukuApiVersion")
            }
        )
    }
}
