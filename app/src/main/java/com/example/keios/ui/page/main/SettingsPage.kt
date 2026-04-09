package com.example.keios.ui.page.main

import android.R.attr.contentDescription
import android.R.attr.iconTint
import android.R.attr.onClick
import androidx.compose.foundation.background
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.FrostedBlock
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
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
    val accent = MiuixTheme.colorScheme.primary
    val titleColor = MiuixTheme.colorScheme.onBackground
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant
    val surfaceColor = MiuixTheme.colorScheme.surface

    val backdrop: LayerBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    // Initialize the scroll behavior to enable nested scrolling with the top app bar
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "Settings",
                scrollBehavior = scrollBehavior,
                color =  MiuixTheme.colorScheme.surface,
                navigationIcon = {
                    IconButton(onClick = onBack,) {
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
            item {
                SmallTitle("界面与样式")
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }

            item {
                FrostedBlock(
                    backdrop = backdrop,
                    title = "Bottom Bar",
                    subtitle = "底栏样式",
                    accent = accent,
                    content = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLiquidBottomBarChanged(!liquidBottomBarEnabled) },
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("液态玻璃底栏", color = titleColor)
                                Text(
                                    text = if (liquidBottomBarEnabled) "已启用玻璃模糊与高光" else "使用纯色底栏",
                                    color = subtitleColor,
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
    }
}