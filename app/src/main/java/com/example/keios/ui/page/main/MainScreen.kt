package com.example.keios.ui.page.main

import android.content.pm.PackageInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.model.BottomPage
import com.example.keios.ui.page.main.widget.FloatingBottomBar
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Composable
fun MainScreen(
    appLabel: String,
    packageInfo: PackageInfo?,
    shizukuStatus: String,
    onCheckOrRequestShizuku: () -> Unit,
) {
    var currentPage by remember { mutableStateOf(BottomPage.Home) }
    var clickCount by remember { mutableIntStateOf(0) }
    val backdrop = rememberLayerBackdrop()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .layerBackdrop(backdrop)
                .padding(WindowInsets.safeDrawing.union(WindowInsets.navigationBars).asPaddingValues())
        ) {
            Spacer(modifier = Modifier.height(14.dp))
            if (currentPage == BottomPage.Home) {
                HomePage(
                    clickCount = clickCount,
                    onPrimaryAction = { clickCount++ }
                )
            } else {
                AboutPage(
                    appLabel = appLabel,
                    packageInfo = packageInfo,
                    shizukuStatus = shizukuStatus,
                    onCheckShizuku = onCheckOrRequestShizuku
                )
            }
            Spacer(modifier = Modifier.height(110.dp))
        }

        FloatingBottomBar(
            backdrop = backdrop,
            currentPage = currentPage,
            onPageSelected = { currentPage = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 22.dp, vertical = 18.dp)
        )
    }
}
