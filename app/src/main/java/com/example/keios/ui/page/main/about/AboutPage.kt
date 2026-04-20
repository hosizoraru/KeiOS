package com.example.keios.ui.page.main.about

import android.content.pm.PackageInfo
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.core.system.ShizukuApiUtils
import com.example.keios.ui.page.main.about.model.buildComponentEntries
import com.example.keios.ui.page.main.about.model.buildPermissionEntries
import com.example.keios.ui.page.main.about.model.loadPackageDetailInfo
import com.example.keios.ui.page.main.about.section.AboutAppCardSection
import com.example.keios.ui.page.main.about.section.AboutBuildSdkCardSection
import com.example.keios.ui.page.main.about.section.AboutComponentCardSection
import com.example.keios.ui.page.main.about.section.AboutGitHubCardSection
import com.example.keios.ui.page.main.about.section.AboutLicenseCardSection
import com.example.keios.ui.page.main.about.section.AboutMediaStorageCardSection
import com.example.keios.ui.page.main.about.section.AboutNetworkServiceCardSection
import com.example.keios.ui.page.main.about.section.AboutPermissionCardSection
import com.example.keios.ui.page.main.about.section.AboutProjectLicenseCardSection
import com.example.keios.ui.page.main.about.section.AboutRuntimeStatusCardSection
import com.example.keios.ui.page.main.about.section.AboutUiFrameworkCardSection
import com.example.keios.ui.page.main.about.util.openExternalUrl
import com.example.keios.ui.page.main.os.appLucideBackIcon
import com.example.keios.ui.page.main.widget.chrome.AppPageLazyColumn
import com.example.keios.ui.page.main.widget.chrome.AppPageScaffold
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    val context = LocalContext.current
    val accent = MiuixTheme.colorScheme.primary
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.90f)
    val readyColor = Color(0xFF2E7D32)
    val notReadyColor = Color(0xFFC62828)
    val infoCardColor = Color(0x223B82F6)
    val buildCardColor = Color(0x223B82F6)
    val uiFrameworkCardColor = Color(0x2233A1F4)
    val networkServiceCardColor = Color(0x2222C55E)
    val mediaStorageCardColor = Color(0x2260A5FA)
    val projectLicenseCardColor = Color(0x2243A047)
    val licenseCardColor = Color(0x2243A047)
    val runtimeCardColor = if (shizukuStatus.contains("granted", ignoreCase = true)) {
        Color(0x2222C55E)
    } else {
        Color(0x22EF4444)
    }

    val listState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    var appExpanded by rememberSaveable { mutableStateOf(true) }
    var runtimeExpanded by rememberSaveable { mutableStateOf(false) }
    var permissionExpanded by rememberSaveable { mutableStateOf(false) }
    var componentExpanded by rememberSaveable { mutableStateOf(false) }
    var buildExpanded by rememberSaveable { mutableStateOf(false) }
    var uiFrameworkExpanded by rememberSaveable { mutableStateOf(false) }
    var githubExpanded by rememberSaveable { mutableStateOf(false) }
    var networkExpanded by rememberSaveable { mutableStateOf(false) }
    var mediaExpanded by rememberSaveable { mutableStateOf(false) }
    var projectLicenseExpanded by rememberSaveable { mutableStateOf(false) }
    var licenseExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) listState.animateScrollToItem(0)
    }

    val packageDetailInfo = remember(context) {
        loadPackageDetailInfo(context)
    }
    val permissionEntries = remember(packageDetailInfo, notificationPermissionGranted) {
        buildPermissionEntries(context, packageDetailInfo, notificationPermissionGranted)
    }
    val componentEntries = remember(packageDetailInfo) {
        buildComponentEntries(context, packageDetailInfo)
    }
    val shizukuDetailMap = remember(shizukuStatus) {
        shizukuApiUtils.detailedRows().toMap()
    }
    val shizukuReady = shizukuStatus.contains("granted", ignoreCase = true)
    val openLinkFailed = stringResource(R.string.common_open_link_failed)

    AppPageScaffold(
        title = stringResource(R.string.about_page_title),
        modifier = Modifier.fillMaxSize(),
        scrollBehavior = scrollBehavior,
        topBarColor = Color.Transparent,
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = appLucideBackIcon(),
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurface
                    )
                }
            }
        }
    ) { innerPadding ->
        AppPageLazyColumn(
            innerPadding = innerPadding,
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            bottomExtra = contentBottomPadding,
            sectionSpacing = 14.dp
        ) {
            item {
                AboutAppCardSection(
                    appLabel = appLabel,
                    packageInfo = packageInfo,
                    cardColor = infoCardColor,
                    accent = accent,
                    subtitleColor = subtitleColor,
                    expanded = appExpanded,
                    onExpandedChange = { appExpanded = it }
                )
            }
            item {
                AboutGitHubCardSection(
                    cardColor = Color(0x2248A6FF),
                    accent = accent,
                    subtitleColor = subtitleColor,
                    expanded = githubExpanded,
                    onExpandedChange = { githubExpanded = it },
                    onOpenProjectUrl = { url ->
                        if (!openExternalUrl(context, url)) {
                            Toast.makeText(context, openLinkFailed, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            item {
                AboutRuntimeStatusCardSection(
                    cardColor = runtimeCardColor,
                    accent = accent,
                    shizukuReady = shizukuReady,
                    readyColor = readyColor,
                    notReadyColor = notReadyColor,
                    subtitleColor = subtitleColor,
                    notificationPermissionGranted = notificationPermissionGranted,
                    shizukuDetailMap = shizukuDetailMap,
                    permissionCount = permissionEntries.size,
                    componentCount = componentEntries.size,
                    expanded = runtimeExpanded,
                    onExpandedChange = { runtimeExpanded = it },
                    onCheckShizuku = onCheckShizuku
                )
            }
            item {
                AboutPermissionCardSection(
                    cardColor = Color(0x2248A6FF),
                    accent = accent,
                    subtitleColor = subtitleColor,
                    readyColor = readyColor,
                    notReadyColor = notReadyColor,
                    entries = permissionEntries,
                    expanded = permissionExpanded,
                    onExpandedChange = { permissionExpanded = it }
                )
            }
            item {
                AboutComponentCardSection(
                    cardColor = Color(0x2234D399),
                    titleColor = readyColor,
                    subtitleColor = subtitleColor,
                    accent = accent,
                    entries = componentEntries,
                    expanded = componentExpanded,
                    onExpandedChange = { componentExpanded = it }
                )
            }
            item {
                AboutBuildSdkCardSection(
                    cardColor = buildCardColor,
                    accent = accent,
                    subtitleColor = subtitleColor,
                    expanded = buildExpanded,
                    onExpandedChange = { buildExpanded = it }
                )
            }
            item {
                AboutUiFrameworkCardSection(
                    cardColor = uiFrameworkCardColor,
                    accent = accent,
                    subtitleColor = subtitleColor,
                    expanded = uiFrameworkExpanded,
                    onExpandedChange = { uiFrameworkExpanded = it }
                )
            }
            item {
                AboutNetworkServiceCardSection(
                    cardColor = networkServiceCardColor,
                    titleColor = readyColor,
                    subtitleColor = subtitleColor,
                    expanded = networkExpanded,
                    onExpandedChange = { networkExpanded = it }
                )
            }
            item {
                AboutMediaStorageCardSection(
                    cardColor = mediaStorageCardColor,
                    accent = accent,
                    subtitleColor = subtitleColor,
                    expanded = mediaExpanded,
                    onExpandedChange = { mediaExpanded = it }
                )
            }
            item {
                AboutProjectLicenseCardSection(
                    cardColor = projectLicenseCardColor,
                    accent = accent,
                    subtitleColor = subtitleColor,
                    expanded = projectLicenseExpanded,
                    onExpandedChange = { projectLicenseExpanded = it },
                    onOpenLicenseUrl = { url ->
                        if (!openExternalUrl(context, url)) {
                            Toast.makeText(context, openLinkFailed, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            item {
                AboutLicenseCardSection(
                    cardColor = licenseCardColor,
                    accent = accent,
                    subtitleColor = subtitleColor,
                    expanded = licenseExpanded,
                    onExpandedChange = { licenseExpanded = it },
                    onOpenSourceUrl = { url ->
                        if (!openExternalUrl(context, url)) {
                            Toast.makeText(context, openLinkFailed, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}
