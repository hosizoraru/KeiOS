package com.example.keios.ui.page.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.FrostedBlock
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.StatusPill
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun HomePage(
    backdrop: Backdrop?,
    shizukuStatus: String,
    mcpRunning: Boolean,
    mcpPort: Int,
    shizukuApiVersion: String,
    mcpConnectedClients: Int
) {
    val shizukuGranted = shizukuStatus.contains("granted", ignoreCase = true)
    val shizukuState = when {
        shizukuGranted -> "已授权"
        shizukuStatus.contains("denied", ignoreCase = true) -> "已拒绝"
        shizukuStatus.contains("unavailable", ignoreCase = true) -> "服务不可用"
        else -> "待检查"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "KeiOS", modifier = Modifier.padding(top = 6.dp))
        Text(text = "Dashboard", modifier = Modifier.padding(top = 4.dp))

        Spacer(modifier = Modifier.height(14.dp))
        FrostedBlock(
            backdrop = backdrop,
            title = "Runtime Status",
            subtitle = "MCP / Shizuku 实时状态",
            accent = Color(0xFF67B68B),
            content = {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatusPill(
                        label = "MCP ${if (mcpRunning) "Running" else "Stopped"}",
                        color = if (mcpRunning) Color(0xFF2E7D32) else Color(0xFF9E9E9E)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusPill(
                        label = "Shizuku $shizukuState",
                        color = if (shizukuGranted) Color(0xFF1565C0) else Color(0xFF9E9E9E)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                MiuixInfoItem("MCP Server", "${if (mcpRunning) "运行中" else "未运行"} · 在线 $mcpConnectedClients")
                MiuixInfoItem("MCP Endpoint", "$mcpPort 端口 · MCP 协议")
                MiuixInfoItem("Shizuku 授权", "$shizukuState · API $shizukuApiVersion")
                MiuixInfoItem("Shizuku 详情", "$shizukuStatus（API $shizukuApiVersion）")
            }
        )
    }
}
