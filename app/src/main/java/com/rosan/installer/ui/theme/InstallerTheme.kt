package com.rosan.installer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

object InstallerTheme {
    val isDark: Boolean
        @Composable get() = isSystemInDarkTheme()
}
