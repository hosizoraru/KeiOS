package com.example.keios

import android.content.pm.PackageInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.keios.shizuku.ShizukuApiUtils
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.isRenderEffectSupported
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Tasks
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape

class MainActivity : ComponentActivity() {

    private var shizukuStatus by mutableStateOf("Shizuku status: initializing...")
    private val shizukuApiUtils = ShizukuApiUtils()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shizukuApiUtils.attach { status ->
            shizukuStatus = status
        }
        setContent {
            KeiOSDemoScreen(
                status = shizukuStatus,
                onCheckOrRequestShizuku = { shizukuApiUtils.requestPermissionIfNeeded() }
            )
        }
    }

    override fun onDestroy() {
        shizukuApiUtils.detach()
        super.onDestroy()
    }
}

@Composable
private fun KeiOSDemoScreen(
    status: String,
    onCheckOrRequestShizuku: () -> Unit
) {
    var currentPage by remember { mutableStateOf(BottomPage.Home) }
    var clickCount by remember { mutableIntStateOf(0) }
    val controller = remember { ThemeController(ColorSchemeMode.System) }
    val context = LocalContext.current
    val packageInfo = remember { context.packageManager.getPackageInfoCompat(context.packageName) }
    val backdrop = rememberLayerBackdrop()
    val appLabel = remember {
        context.packageManager.getApplicationLabel(context.applicationInfo).toString()
    }

    MiuixTheme(controller = controller) {
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
                        shizukuStatus = status,
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
}

@Composable
private fun HomePage(
    clickCount: Int,
    onPrimaryAction: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "KeiOS", modifier = Modifier.padding(top = 6.dp))
        Text(text = "Miuix Engine Dashboard", modifier = Modifier.padding(top = 4.dp))

        Spacer(modifier = Modifier.height(14.dp))
        FrostedBlock(
            title = "Miuix UI Engine",
            subtitle = "Inspired by InstallerX-Revived settings style",
            body = "当前主页采用 Miuix 风格卡片布局，底部悬浮导航，权限入口已转移到“关于”页。",
            accent = Color(0xFF76A4FF)
        )
        Spacer(modifier = Modifier.height(12.dp))
        FrostedBlock(
            title = "Quick Stats",
            subtitle = "Session preview",
            body = "本次演示点击次数: $clickCount",
            accent = Color(0xFF67B68B)
        )

        Button(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            onClick = onPrimaryAction
        ) {
            Text(text = "Primary Action")
        }
    }
}

@Composable
private fun AboutPage(
    appLabel: String,
    packageInfo: PackageInfo,
    shizukuStatus: String,
    onCheckShizuku: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "About", modifier = Modifier.padding(top = 6.dp))
        Text(text = "权限检查与应用详情", modifier = Modifier.padding(top = 4.dp))
        Spacer(modifier = Modifier.height(14.dp))

        FrostedBlock(
            title = "Shizuku",
            subtitle = "Permission Center",
            body = shizukuStatus,
            accent = Color(0xFFFF9F7A)
        )

        Button(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth(),
            onClick = onCheckShizuku
        ) {
            Text(text = "检查 / 申请 Shizuku 权限")
        }

        Spacer(modifier = Modifier.height(12.dp))
        FrostedBlock(
            title = appLabel,
            subtitle = "App Details",
            body = buildString {
                val appInfo = packageInfo.applicationInfo
                appendLine("Package: ${packageInfo.packageName}")
                appendLine("Version: ${packageInfo.versionName ?: "unknown"}")
                appendLine("VersionCode: ${packageInfo.longVersionCode}")
                appendLine("Min SDK: ${appInfo?.minSdkVersion ?: "unknown"}")
                appendLine("Target SDK: ${appInfo?.targetSdkVersion ?: "unknown"}")
                appendLine("Device API: ${Build.VERSION.SDK_INT}")
                append("Miuix: 0.9.0")
            },
            accent = Color(0xFF8C7BFF)
        )
    }
}

@Composable
private fun FrostedBlock(
    title: String,
    subtitle: String,
    body: String,
    accent: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.95f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
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

@Composable
private fun FloatingBottomBar(
    backdrop: top.yukonga.miuix.kmp.blur.LayerBackdrop,
    currentPage: BottomPage,
    onPageSelected: (BottomPage) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val blurEnabled = isRenderEffectSupported() || isRuntimeShaderSupported()
    val blurColors = remember(isDark) {
        if (isDark) {
            BlurColors(
                blendColors = listOf(
                    BlendColorEntry(Color(0x667A7A7A), BlurBlendMode.ColorBurn),
                    BlendColorEntry(Color(0x33747474), BlurBlendMode.Overlay)
                )
            )
        } else {
            BlurColors(
                blendColors = listOf(
                    BlendColorEntry(Color(0x7F040404), BlurBlendMode.Overlay),
                    BlendColorEntry(Color(0x26F1F1F1), BlurBlendMode.ColorDodge)
                )
            )
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .textureBlur(
                backdrop = backdrop,
                shape = RoundedCornerShape(999.dp),
                blurRadius = 80f,
                noiseCoefficient = BlurDefaults.NoiseCoefficient,
                colors = blurColors,
                contentBlendMode = BlendMode.SrcOver,
                enabled = blurEnabled
            )
            .background(
                if (isDark) Color(0xCC1A1B22) else Color(0xB3FFFFFF)
            )
            .border(
                BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x55FFFFFF)),
                RoundedCornerShape(999.dp)
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
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (selected) Color(0x1A6F8FFF) else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label
        )
        Text(text = label, modifier = Modifier.padding(top = 2.dp))
    }
}

private enum class BottomPage(
    val label: String,
    val icon: ImageVector
) {
    Home("主页", MiuixIcons.Regular.Tasks),
    About("关于", MiuixIcons.Regular.Info)
}

private fun android.content.pm.PackageManager.getPackageInfoCompat(packageName: String): PackageInfo {
    return getPackageInfo(packageName, 0)
}
