package com.example.keios.ui.page.main.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun FrostedBlock(
    title: String,
    subtitle: String,
    body: String,
    accent: Color,
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.62f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent.copy(alpha = 0.22f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(text = title)
            }
        }
        Text(text = subtitle, modifier = Modifier.padding(top = 8.dp))
        Text(text = body, modifier = Modifier.padding(top = 8.dp))
    }
}
