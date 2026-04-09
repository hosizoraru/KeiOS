package com.example.keios.ui.page.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import com.example.keios.ui.page.main.widget.AppTopBar
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.StatusPill
import com.example.keios.ui.utils.rememberCardBlurColors
import com.kyant.shapes.RoundedRectangle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.isRenderEffectSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HomePage(
    shizukuStatus: String,
    mcpRunning: Boolean,
    mcpPort: Int,
    shizukuApiVersion: String,
    mcpConnectedClients: Int,
    onOpenSettings: () -> Unit,
    contentTopPadding: Dp = 0.dp,
    contentBottomPadding: Dp = 0.dp
) {
    val shizukuGranted = shizukuStatus.contains("granted", ignoreCase = true)
    val runningColor = MiuixTheme.colorScheme.primary
    val inactiveColor = MiuixTheme.colorScheme.onBackgroundVariant
    val infoColor = MiuixTheme.colorScheme.secondary
    val titleColor = MiuixTheme.colorScheme.onBackground
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant
    val isDark = isSystemInDarkTheme()
    val foregroundBackdrop = rememberLayerBackdrop()
    val blurEnabled = isRenderEffectSupported()
    val cardBlurColors = rememberCardBlurColors()
    val shizukuState = when {
        shizukuGranted -> "已授权"
        else -> "待检查"
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(foregroundBackdrop)
                    .background(
                        brush = if (isDark) {
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF5C1D5E),
                                    Color(0xFF2A2A7A),
                                    Color(0xFF191C24)
                                )
                            )
                        } else {
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFFFFE3F1),
                                    Color(0xFFE6E8FF),
                                    Color(0xFFF4F7FF)
                                )
                            )
                        }
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 44.dp)
                    .size(140.dp)
                    .background(
                        if (isDark) Color(0x55D748A3) else Color(0x66FF8CC7),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 52.dp)
                    .size(160.dp)
                    .background(
                        if (isDark) Color(0x44FF8C3A) else Color(0x66FFC18B),
                        shape = CircleShape
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 2.dp,
                        end = 2.dp,
                        top = contentTopPadding + 6.dp,
                        bottom = contentBottomPadding + 6.dp
                    )
            ) {
                AppTopBar(
                    title = "KeiOS",
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = MiuixIcons.Regular.Settings,
                                contentDescription = "设置",
                                tint = MiuixTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(82.dp)
                            .background(
                                color = if (isDark) Color(0x55FF7CC9) else Color(0x66FF95D4),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("K", color = Color.White)
                    }
                    Text(
                        text = "KeiOS",
                        color = titleColor,
                        modifier = Modifier.padding(top = 14.dp)
                    )
                    Text(
                        text = "MCP / Shizuku Runtime",
                        color = subtitleColor,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                        .textureBlur(
                            backdrop = foregroundBackdrop,
                            shape = RoundedRectangle(18.dp),
                            blurRadius = 52f,
                            noiseCoefficient = BlurDefaults.NoiseCoefficient,
                            colors = cardBlurColors,
                            enabled = blurEnabled
                        )
                        .background(
                            color = if (blurEnabled) {
                                Color.White.copy(alpha = if (isDark) 0.10f else 0.12f)
                            } else if (isDark) {
                                MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.86f)
                            } else {
                                Color.White.copy(alpha = 0.74f)
                            },
                            shape = RoundedRectangle(18.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = if (isDark) 0.14f else 0.20f),
                            shape = RoundedRectangle(18.dp)
                        )
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Runtime Status",
                        color = titleColor
                    )
                    Text(
                        text = "MCP / Shizuku 实时状态",
                        color = subtitleColor,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
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
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
