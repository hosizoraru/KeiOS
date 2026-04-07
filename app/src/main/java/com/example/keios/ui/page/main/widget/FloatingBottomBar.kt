package com.example.keios.ui.page.main.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.model.BottomPage
import com.example.keios.ui.utils.installerXLiquidGlass
import com.example.keios.ui.utils.rememberBottomBarBlurColors
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop

@Composable
fun FloatingBottomBar(
    backdrop: LayerBackdrop,
    currentPage: BottomPage,
    onPageSelected: (BottomPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomBarBlurColors = rememberBottomBarBlurColors()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .installerXLiquidGlass(
                backdrop = backdrop,
                blurColors = bottomBarBlurColors,
                cornerRadiusDp = 999,
                blurRadius = 80f
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BottomBarItem(
            selected = currentPage == BottomPage.Home,
            label = BottomPage.Home.label,
            icon = BottomPage.Home.icon,
            onClick = { onPageSelected(BottomPage.Home) },
            modifier = Modifier.weight(1f)
        )
        BottomBarItem(
            selected = currentPage == BottomPage.About,
            label = BottomPage.About.label,
            icon = BottomPage.About.icon,
            onClick = { onPageSelected(BottomPage.About) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BottomBarItem(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Color(0x1A6F8FFF) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = label)
        Text(text = label, modifier = Modifier.padding(top = 2.dp))
    }
}
