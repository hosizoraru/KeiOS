package com.example.keios.ui.page.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.FrostedBlock
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun SettingsPage(
    backdrop: Backdrop?,
    liquidBottomBarEnabled: Boolean,
    onLiquidBottomBarChanged: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Settings", modifier = Modifier.padding(top = 6.dp))
        Text(text = "界面与样式", modifier = Modifier.padding(top = 4.dp))

        Spacer(modifier = Modifier.height(14.dp))
        FrostedBlock(
            backdrop = backdrop,
            title = "Bottom Bar",
            subtitle = "底栏样式",
            accent = Color(0xFF6E9EFF),
            content = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLiquidBottomBarChanged(!liquidBottomBarEnabled) },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("液态玻璃底栏")
                        Text(
                            text = if (liquidBottomBarEnabled) "已启用玻璃模糊与高光" else "使用纯色底栏",
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Checkbox(
                        state = if (liquidBottomBarEnabled) ToggleableState.On else ToggleableState.Off,
                        onClick = { onLiquidBottomBarChanged(!liquidBottomBarEnabled) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                MiuixInfoItem("样式说明", "参考 InstallerX-Revived / KernelSU：胶囊底栏 + 选中态高亮")
            }
        )
    }
}
