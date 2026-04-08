package com.example.keios.ui.page.main.widget

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponent

@Composable
fun MiuixInfoItem(
    key: String,
    value: String
) {
    BasicComponent(
        title = key,
        summary = value.ifBlank { "N/A" },
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
