package com.example.keios.ui.page.main.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    showSubtitle: Boolean = false,
    bottomSpacing: Dp = 6.dp,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        TopAppBar(
            modifier = Modifier.fillMaxWidth(),
            title = title,
            color = MiuixTheme.colorScheme.surface,
            navigationIcon = navigationIcon ?: {},
            actions = actions
        )
        if (showSubtitle && !subtitle.isNullOrBlank()) {
            SmallTitle(subtitle)
        }
        Spacer(modifier = Modifier.height(bottomSpacing))
    }
}
