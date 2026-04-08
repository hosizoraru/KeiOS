package com.example.keios.ui.page.main.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun StatusPill(
    label: String,
    color: Color
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(color.copy(alpha = 0.18f))
            .border(width = 0.6.dp, color = color.copy(alpha = 0.35f), shape = shape)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text = label, color = color)
    }
}
