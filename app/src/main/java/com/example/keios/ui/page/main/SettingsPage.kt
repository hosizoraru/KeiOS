package com.example.keios.ui.page.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsPage(
    liquidBottomBarEnabled: Boolean,
    onLiquidBottomBarChanged: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val titleColor = MiuixTheme.colorScheme.onBackground
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant

    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "Settings",
                scrollBehavior = scrollBehavior,
                color = MiuixTheme.colorScheme.surface,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Back,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp
            )
        ) {
            item { SmallTitle("界面与样式") }
            item { Spacer(modifier = Modifier.height(14.dp)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = Color(0x223B82F6),
                        contentColor = MiuixTheme.colorScheme.onBackground
                    ),
                    onClick = {}
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Bottom Bar", color = titleColor)
                            GlassTextButton(
                                backdrop = null,
                                text = if (liquidBottomBarEnabled) "ON" else "OFF",
                                enabled = false,
                                textColor = if (liquidBottomBarEnabled) Color(0xFF34C759) else Color(0xFF60A5FA),
                                bottomBarStyle = true,
                                onClick = {}
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLiquidBottomBarChanged(!liquidBottomBarEnabled) },
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("液态玻璃底栏", color = titleColor)
                                Text(
                                    text = if (liquidBottomBarEnabled) "已启用玻璃模糊与高光" else "使用纯色底栏",
                                    color = subtitleColor,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Switch(
                                checked = liquidBottomBarEnabled,
                                onCheckedChange = { checked -> onLiquidBottomBarChanged(checked) }
                            )
                        }

                        MiuixInfoItem(
                            key = "样式说明",
                            value = "胶囊底栏 + 选中态高亮 + 轻量玻璃质感"
                        )
                    }
                }
            }
        }
    }
}
