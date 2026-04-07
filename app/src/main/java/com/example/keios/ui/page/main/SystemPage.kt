package com.example.keios.ui.page.main

import android.os.Build
import java.util.Locale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.FrostedBlock
import com.example.keios.ui.utils.InfoFactory
import com.example.keios.ui.utils.findJavaPropString
import com.example.keios.ui.utils.findPropString
import com.example.keios.ui.utils.getAllJavaPropString
import com.example.keios.ui.utils.getAllSystemProperties
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun SystemPage(
    backdrop: Backdrop?
) {
    val keyInfoText = remember {
        buildString {
            appendLine("Brand: ${Build.BRAND}")
            appendLine("Model: ${Build.MODEL}")
            appendLine("Device: ${Build.DEVICE}")
            appendLine("Release: ${findPropString("ro.build.version.release")}")
            appendLine("SDK: ${findPropString("ro.build.version.sdk")}")
            appendLine("Build ID: ${findPropString("ro.build.id")}")
            appendLine("Display ID: ${findPropString("ro.build.display.id")}")
            appendLine("Security Patch: ${findPropString("ro.build.version.security_patch")}")
            appendLine("Locale: ${findPropString("persist.sys.locale", Locale.getDefault().toLanguageTag())}")
            appendLine("Unicode: ${findJavaPropString("android.icu.unicode.version")}")
            append("OpenSSL: ${findJavaPropString("android.openssl.version")}")
        }
    }
    val infoFactoryText = remember {
        buildString {
            appendLine("procVersion: ${InfoFactory.procVersion}")
            appendLine("abSlot: ${InfoFactory.abSlot}")
            appendLine("toyboxVersion: ${InfoFactory.toyboxVersion}")
            appendLine("vendorBuildSecurityPatch: ${InfoFactory.vendorBuildSecurityPatch}")
            appendLine("miOSVersionName: ${InfoFactory.miOSVersionName}")
            appendLine("miuiVersionName: ${InfoFactory.miuiVersionName}")
            appendLine("miuiVersionCode: ${InfoFactory.miuiVersionCode}")
            appendLine("deviceName: ${InfoFactory.deviceName}")
            appendLine("zygote: ${InfoFactory.zygote}")
            appendLine("unicodeVersion: ${InfoFactory.unicodeVersion}")
            appendLine("opensslVersion: ${InfoFactory.opensslVersion}")
            appendLine("selinuxPolicy: ${InfoFactory.selinuxPolicy}")
            appendLine("backgroundBlurSupported: ${InfoFactory.backgroundBlurSupported}")
            append("fileSafStatus: ${InfoFactory.fileSafStatus}")
        }
    }
    val systemPropsPreview = remember {
        getAllSystemProperties
            .toSortedMap()
            .entries
            .joinToString("\n") { "${it.key} = ${it.value}" }
    }
    val javaPropsPreview = remember {
        getAllJavaPropString
            .toSortedMap()
            .entries
            .joinToString("\n") { "${it.key} = ${it.value}" }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 12.dp)
    ) {
        Text(text = "System", modifier = Modifier.padding(top = 6.dp))
        Text(text = "系统参数与属性", modifier = Modifier.padding(top = 4.dp))
        Spacer(modifier = Modifier.height(14.dp))

        FrostedBlock(
            backdrop = backdrop,
            title = "Key Info",
            subtitle = "System / Java properties",
            body = keyInfoText,
            accent = Color(0xFF5F9DFF)
        )
        Spacer(modifier = Modifier.height(12.dp))
        FrostedBlock(
            backdrop = backdrop,
            title = "InfoFactory",
            subtitle = "Migrated utils snapshot",
            body = infoFactoryText,
            accent = Color(0xFF7E8CFF)
        )
        Spacer(modifier = Modifier.height(12.dp))
        FrostedBlock(
            backdrop = backdrop,
            title = "getprop",
            subtitle = "All ${getAllSystemProperties.size} entries",
            body = systemPropsPreview.ifBlank { "No system properties available." },
            accent = Color(0xFF6ECF9C)
        )
        Spacer(modifier = Modifier.height(12.dp))
        FrostedBlock(
            backdrop = backdrop,
            title = "Java Properties",
            subtitle = "All ${getAllJavaPropString.size} entries",
            body = javaPropsPreview.ifBlank { "No Java properties available." },
            accent = Color(0xFFFFB26B)
        )
    }
}
