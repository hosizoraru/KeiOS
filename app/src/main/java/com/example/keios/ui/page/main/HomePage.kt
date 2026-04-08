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
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun HomePage(
    backdrop: Backdrop?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "KeiOS", modifier = Modifier.padding(top = 6.dp))
        Text(text = "Miuix Engine Dashboard", modifier = Modifier.padding(top = 4.dp))

        Spacer(modifier = Modifier.height(14.dp))
        FrostedBlock(
            backdrop = backdrop,
            title = "Miuix UI Engine",
            subtitle = "Inspired by InstallerX-Revived settings style",
            body = "当前主页采用 Miuix 风格卡片布局，底部悬浮导航，权限入口已转移到“关于”页。",
            accent = Color(0xFF76A4FF)
        )
    }
}
