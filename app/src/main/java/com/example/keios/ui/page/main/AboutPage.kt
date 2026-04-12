package com.example.keios.ui.page.main

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow.Companion.Clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.keios.BuildConfig
import com.example.keios.core.system.ShizukuApiUtils
import com.example.keios.ui.page.main.widget.StatusPill
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

private fun formatTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return "N/A"
    return runCatching {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        formatter.format(Date(epochMillis))
    }.getOrDefault("N/A")
}

@Composable
private fun AboutCompactRow(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    valueContent: @Composable RowScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.width(104.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = title,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                maxLines = Int.MAX_VALUE,
                overflow = Clip
            )
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            content = valueContent
        )
    }
}

@Composable
private fun AboutCompactInfoRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MiuixTheme.colorScheme.onBackground,
    onClick: (() -> Unit)? = null
) {
    AboutCompactRow(
        title = title,
        modifier = modifier,
        onClick = onClick
    ) {
        Text(
            text = value,
            color = valueColor,
            maxLines = Int.MAX_VALUE,
            overflow = Clip,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun AboutCompactPillRow(
    title: String,
    label: String,
    color: Color,
    onClick: (() -> Unit)? = null
) {
    AboutCompactRow(
        title = title,
        onClick = onClick
    ) {
        StatusPill(
            label = label,
            color = color
        )
    }
}

private fun normalizeLower(value: String): String = value.trim().lowercase(Locale.ROOT)

private fun selinuxStatusColor(selinuxState: String): Color {
    return when (normalizeLower(selinuxState)) {
        "enforcing" -> Color(0xFF2E7D32)
        "permissive" -> Color(0xFFB26A00)
        "disabled" -> Color(0xFFC62828)
        "n/a", "", "unknown" -> Color(0xFF6B7280)
        else -> Color(0xFF2563EB)
    }
}

@Composable
fun AboutPage(
    appLabel: String,
    packageInfo: PackageInfo?,
    notificationPermissionGranted: Boolean,
    shizukuStatus: String,
    shizukuApiUtils: ShizukuApiUtils,
    onCheckShizuku: () -> Unit,
    contentBottomPadding: Dp = 72.dp,
    scrollToTopSignal: Int = 0,
    onBack: (() -> Unit)? = null
) {
    val accent = MiuixTheme.colorScheme.primary
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant
    val readyColor = Color(0xFF2E7D32)
    val notReadyColor = Color(0xFFC62828)
    val infoCardColor = Color(0x223B82F6)
    val frameworkCardColor = Color(0x223B82F6)
    val listState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()

    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) listState.animateScrollToItem(0)
    }

    val appInfo: ApplicationInfo? = packageInfo?.applicationInfo
    val shizukuDetailMap = remember(shizukuStatus) {
        shizukuApiUtils.detailedRows().toMap()
    }
    val frameworkRows = remember {
        listOf(
            "UI 框架" to "Miuix UI ${BuildConfig.MIUIX_VERSION}",
            "声明式界面" to "Jetpack Compose ${BuildConfig.COMPOSE_VERSION}",
            "页面导航" to "Navigation3 ${BuildConfig.NAVIGATION3_VERSION}",
            "玻璃材质" to "Backdrop ${BuildConfig.BACKDROP_VERSION} · Capsule ${BuildConfig.CAPSULE_VERSION}",
            "权限桥接" to "Shizuku API ${ShizukuApiUtils.API_VERSION}",
            "MCP 服务栈" to "kotlin-sdk ${BuildConfig.MCP_KOTLIN_SDK_VERSION} · Ktor ${BuildConfig.KTOR_VERSION}",
            "网络请求" to "OkHttp ${BuildConfig.OKHTTP_VERSION}",
            "多媒体" to "Media3 ${BuildConfig.MEDIA3_VERSION} · ZoomImage ${BuildConfig.ZOOMIMAGE_VERSION}",
            "本地存储" to "MMKV ${BuildConfig.MMKV_VERSION}"
        )
    }
    val shizukuReady = shizukuStatus.contains("granted", ignoreCase = true)
    val permissionCardColor = if (shizukuReady) Color(0x2222C55E) else Color(0x22EF4444)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "About",
                scrollBehavior = scrollBehavior,
                color = MiuixTheme.colorScheme.surface,
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Regular.Back,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            state = listState,
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + contentBottomPadding + 16.dp,
                start = 16.dp,
                end = 16.dp
            )
        ) {
            item {
                SmallTitle("应用信息")
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = infoCardColor,
                        contentColor = MiuixTheme.colorScheme.onBackground
                    ),
                    onClick = {}
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "App",
                            color = accent
                        )
                        Text(
                            text = "应用身份与运行时信息",
                            color = subtitleColor
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            AboutCompactInfoRow("名称", appLabel)
                            AboutCompactInfoRow("包名", packageInfo?.packageName ?: "unknown")
                            AboutCompactInfoRow(
                                "版本",
                                "${packageInfo?.versionName ?: "unknown"} (${packageInfo?.longVersionCode ?: -1})"
                            )
                            AboutCompactInfoRow(
                                "最后更新",
                                packageInfo?.lastUpdateTime?.let(::formatTime) ?: "unknown"
                            )
                            AboutCompactInfoRow(
                                "Debug",
                                (((appInfo?.flags ?: 0) and ApplicationInfo.FLAG_DEBUGGABLE) != 0).toString()
                            )
                            AboutCompactInfoRow(
                                "API Level",
                                Build.VERSION.SDK_INT.toString()
                            )
                            AboutCompactInfoRow(
                                "安全补丁",
                                Build.VERSION.SECURITY_PATCH ?: "unknown"
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = frameworkCardColor,
                        contentColor = MiuixTheme.colorScheme.onBackground
                    ),
                    onClick = {}
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "框架",
                            color = accent
                        )
                        Text(
                            text = "项目依赖与技术栈",
                            color = subtitleColor
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            frameworkRows.forEach { (title, value) ->
                                AboutCompactInfoRow(
                                    title = title,
                                    value = value
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = permissionCardColor,
                        contentColor = MiuixTheme.colorScheme.onBackground
                    ),
                    onClick = {}
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Permissions",
                                color = if (shizukuReady) readyColor else notReadyColor
                            )
                            StatusPill(
                                label = "Shizuku",
                                color = if (shizukuReady) readyColor else notReadyColor
                            )
                        }
                        Text(
                            text = "权限与服务状态",
                            color = subtitleColor
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            AboutCompactPillRow(
                                "通知权限",
                                label = if (notificationPermissionGranted) "Granted" else "Not Granted",
                                color = if (notificationPermissionGranted) readyColor else notReadyColor
                            )
                            AboutCompactPillRow(
                                "SELinux",
                                label = shizukuDetailMap["Shizuku getenforce"] ?: "N/A",
                                color = selinuxStatusColor(shizukuDetailMap["Shizuku getenforce"] ?: "N/A"),
                                onClick = onCheckShizuku
                            )
                            AboutCompactInfoRow(
                                "uname",
                                shizukuDetailMap["Shizuku uname"] ?: "N/A",
                                valueColor = accent,
                                onClick = onCheckShizuku
                            )
                        }
                    }
                }
            }
        }
    }
}
