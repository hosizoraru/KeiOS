package com.example.keios

import android.content.pm.PackageInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.example.keios.shizuku.ShizukuApiUtils
import com.example.keios.ui.page.main.MainScreen
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class MainActivity : ComponentActivity() {

    private var shizukuStatus = mutableStateOf("Shizuku status: initializing...")
    private val shizukuApiUtils = ShizukuApiUtils()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appLabel = packageManager.getApplicationLabel(applicationInfo).toString()
        val packageInfo = packageManager.getPackageInfoCompat(packageName)
        val controller = ThemeController(ColorSchemeMode.System)

        shizukuApiUtils.attach { status ->
            shizukuStatus.value = status
        }

        setContent {
            MiuixTheme(controller = controller) {
                MainScreen(
                    appLabel = appLabel,
                    packageInfo = packageInfo,
                    shizukuStatus = shizukuStatus.value,
                    onCheckOrRequestShizuku = { shizukuApiUtils.requestPermissionIfNeeded() }
                )
            }
        }
    }

    override fun onDestroy() {
        shizukuApiUtils.detach()
        super.onDestroy()
    }
}

private fun android.content.pm.PackageManager.getPackageInfoCompat(packageName: String): PackageInfo {
    return getPackageInfo(packageName, 0)
}
