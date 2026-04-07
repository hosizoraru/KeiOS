package com.example.keios.ui.page.main.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.model.BottomPage
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun FloatingBottomBar(
    backdrop: Backdrop,
    currentPage: BottomPage,
    onPageSelected: (BottomPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousCapsule },
                effects = {
                    vibrancy()
                    blur(8f.dp.toPx())
                    lens(18f.dp.toPx(), 18f.dp.toPx())
                },
                highlight = { Highlight.Default.copy(alpha = 0.95f) },
                shadow = { Shadow.Default.copy(color = Color.Black.copy(alpha = 0.12f)) },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.42f))
                }
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
            .background(Color.Transparent, ContinuousCapsule)
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
