package com.example.keios

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import rikka.shizuku.Shizuku
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class MainActivity : ComponentActivity() {

    private var shizukuStatus by mutableStateOf("Shizuku status: not checked")

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode != REQUEST_SHIZUKU_PERMISSION) return@OnRequestPermissionResultListener
        shizukuStatus = if (grantResult == PackageManager.PERMISSION_GRANTED) {
            "Shizuku permission: granted"
        } else {
            "Shizuku permission: denied"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        setContent {
            KeiOSDemoScreen(
                status = shizukuStatus,
                onCheckOrRequestShizuku = ::checkOrRequestShizukuPermission
            )
        }
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        super.onDestroy()
    }

    private fun checkOrRequestShizukuPermission() {
        if (!Shizuku.pingBinder()) {
            shizukuStatus = "Shizuku service unavailable (start Shizuku app first)"
            return
        }

        if (Shizuku.isPreV11()) {
            shizukuStatus = "Shizuku pre-v11 is unsupported"
            return
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            shizukuStatus = "Shizuku permission: granted"
            return
        }

        if (Shizuku.shouldShowRequestPermissionRationale()) {
            shizukuStatus = "Shizuku permission denied permanently"
            return
        }

        shizukuStatus = "Requesting Shizuku permission..."
        Shizuku.requestPermission(REQUEST_SHIZUKU_PERMISSION)
    }

    companion object {
        private const val REQUEST_SHIZUKU_PERMISSION = 1001
    }
}

@Composable
private fun KeiOSDemoScreen(
    status: String,
    onCheckOrRequestShizuku: () -> Unit
) {
    var clickCount by remember { mutableIntStateOf(0) }
    val controller = remember { ThemeController(ColorSchemeMode.System) }

    MiuixTheme(controller = controller) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "KeiOS Miuix Demo")
            Text(text = "Target API: 37")
            Text(text = "Button clicked: $clickCount")
            Text(text = status, modifier = Modifier.padding(top = 8.dp))

            Button(
                modifier = Modifier.padding(top = 16.dp),
                onClick = { clickCount++ }
            ) {
                Text(text = "Click me")
            }

            Button(
                modifier = Modifier.padding(top = 12.dp),
                onClick = onCheckOrRequestShizuku
            ) {
                Text(text = "Check Shizuku")
            }
        }
    }
}
