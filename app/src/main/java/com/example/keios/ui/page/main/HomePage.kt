package com.example.keios.ui.page.main

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

@Composable
fun HomePage(
    clickCount: Int,
    onPrimaryAction: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "KeiOS", modifier = Modifier.padding(top = 6.dp))
        Text(text = "Miuix Engine Dashboard", modifier = Modifier.padding(top = 4.dp))

        Spacer(modifier = Modifier.height(14.dp))
        FrostedBlock(
            title = "Miuix UI Engine",
            subtitle = "Inspired by InstallerX-Revived settings style",
            body = "当前主页采用 Miuix 风格卡片布局，底部悬浮导航，权限入口已转移到“关于”页。",
            accent = Color(0xFF76A4FF)
        )
        Spacer(modifier = Modifier.height(12.dp))
        FrostedBlock(
            title = "Quick Stats",
            subtitle = "Session preview",
            body = "本次演示点击次数: $clickCount",
            accent = Color(0xFF67B68B)
        )

        Button(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            onClick = onPrimaryAction
        ) {
            Text(text = "Primary Action")
        }
    }
}
