package os.kei.ui.page.main.widget

import android.app.Application
import android.content.pm.PackageInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import os.kei.ui.page.main.about.section.AboutAppCardSection
import os.kei.ui.page.main.settings.support.SettingsGroupCard
import os.kei.ui.page.main.settings.support.SettingsToggleItem
import os.kei.ui.page.main.student.catalog.BaGuideCatalogEntry
import os.kei.ui.page.main.student.catalog.BaGuideCatalogTab
import os.kei.ui.page.main.student.catalog.component.BaGuideCatalogEntryCard
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.chrome.AppTopBarSearchField
import os.kei.ui.page.main.widget.chrome.AppTopBarSection
import os.kei.ui.page.main.widget.core.AppInfoListBody
import os.kei.ui.page.main.widget.core.AppInfoRow
import os.kei.ui.page.main.widget.core.AppOverviewCard
import os.kei.ui.page.main.widget.core.AppSupportingBlock
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.GlassIconButton
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LiquidDropdownColumn
import os.kei.ui.page.main.widget.glass.LiquidDropdownImpl
import os.kei.ui.page.main.widget.status.StatusPill
import os.kei.ui.page.main.widget.support.LocalTextCopyExpandedOverride
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = AppDesignSystemScreenshotTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi"
)
class AppDesignSystemScreenshotTest {

    private fun currentPackageInfo(): PackageInfo? {
        val context = ApplicationProvider.getApplicationContext<Application>()
        return runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()
    }

    @Test
    fun appCardHeaderLight() {
        captureRoboImage(filePath = "src/test/screenshots/design-system/app_card_header_light.png") {
            CompositionLocalProvider(LocalTextCopyExpandedOverride provides false) {
                MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF3F4F6))
                            .padding(16.dp)
                    ) {
                        AppOverviewCard(
                            title = "MCP Logs",
                            subtitle = "8 条日志 · 长按可导出",
                            containerColor = Color.White,
                            borderColor = Color(0xFFD7DFEA),
                            headerEndActions = {
                                StatusPill(
                                    label = "已激活",
                                    color = Color(0xFF22C55E)
                                )
                            }
                        ) {
                            AppSupportingBlock(text = "卡片头部、状态胶囊和正文节奏会在这里一起校验。")
                        }
                    }
                }
            }
        }
    }

    @Test
    @Config(
        application = AppDesignSystemScreenshotTestApp::class,
        sdk = [35],
        qualifiers = "w411dp-h891dp-xxhdpi +night"
    )
    fun appOverviewCardDark() {
        captureRoboImage(filePath = "src/test/screenshots/design-system/app_overview_card_dark.png") {
            CompositionLocalProvider(LocalTextCopyExpandedOverride provides false) {
                MiuixTheme(controller = ThemeController(ColorSchemeMode.Dark)) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF111827))
                            .padding(16.dp)
                    ) {
                        AppOverviewCard(
                            title = "GitHub 项目追踪",
                            subtitle = "点击刷新，长按新增",
                            containerColor = Color(0xFF1F2937),
                            borderColor = Color(0xFF334155),
                            titleColor = Color.White,
                            subtitleColor = Color(0xFFCBD5E1),
                            headerEndActions = {
                                StatusPill(
                                    label = "3m 前",
                                    color = Color(0xFF60A5FA)
                                )
                                StatusPill(
                                    label = "已检查",
                                    color = Color(0xFF4ADE80)
                                )
                            }
                        ) {
                            AppInfoListBody {
                                AppInfoRow(
                                    label = "追踪项目",
                                    value = "18",
                                    labelColor = Color(0xFFCBD5E1),
                                    valueColor = Color.White
                                )
                                AppInfoRow(
                                    label = "可更新",
                                    value = "4",
                                    labelColor = Color(0xFFCBD5E1),
                                    valueColor = Color(0xFF60A5FA)
                                )
                                AppInfoRow(
                                    label = "预发行",
                                    value = "2",
                                    labelColor = Color(0xFFCBD5E1),
                                    valueColor = Color(0xFFFBBF24)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun listBodySkeletonLight() {
        captureRoboImage(filePath = "src/test/screenshots/design-system/app_list_body_light.png") {
            CompositionLocalProvider(LocalTextCopyExpandedOverride provides false) {
                MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF3F4F6))
                            .padding(16.dp)
                    ) {
                        AppOverviewCard(
                            title = "列表骨架",
                            subtitle = "统一正文排布",
                            containerColor = Color.White,
                            borderColor = Color(0xFFD7DFEA),
                            headerEndActions = {
                                StatusPill(
                                    label = "3 项",
                                    color = Color(0xFF2563EB)
                                )
                            }
                        ) {
                            AppInfoListBody {
                                AppInfoRow(label = "当前策略", value = "统一正文骨架")
                                AppInfoRow(label = "说明", value = "支持多行 value，key 与 value 的节奏保持一致。")
                                AppSupportingBlock(text = "后续更多 card 内容区会继续收敛到这套布局。")
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun settingsGroupCardLight() {
        captureRoboImage(filePath = "src/test/screenshots/design-system/settings_group_card_light.png") {
            CompositionLocalProvider(LocalTextCopyExpandedOverride provides false) {
                MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF3F4F6))
                            .padding(16.dp)
                    ) {
                        SettingsGroupCard(
                            header = "视觉样式",
                            title = "操作与反馈",
                            containerColor = Color(0x223B82F6)
                        ) {
                            SettingsToggleItem(
                                title = "ActionBar 分层样式",
                                summary = "保持顶部交互区域的层次和反馈一致。",
                                checked = true,
                                onCheckedChange = {},
                                infoKey = "作用范围",
                                infoValue = "主页面与具备 action bar 的子页面"
                            )
                            SettingsToggleItem(
                                title = "复制能力扩展",
                                summary = "切换完整文本选择能力。",
                                checked = false,
                                onCheckedChange = {},
                                infoKey = "说明",
                                infoValue = "关闭时保留轻量长按复制，开启后支持完整选择拖动。"
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun catalogEntryCardLight() {
        captureRoboImage(filePath = "src/test/screenshots/design-system/catalog_entry_card_light.png") {
            CompositionLocalProvider(LocalTextCopyExpandedOverride provides false) {
                MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF3F4F6))
                            .padding(16.dp)
                    ) {
                        BaGuideCatalogEntryCard(
                            entry = BaGuideCatalogEntry(
                                entryId = 1,
                                pid = 49443,
                                contentId = 46680L,
                                name = "星野（临战）",
                                alias = "hoshino battle",
                                aliasDisplay = "别名：星野 / Hoshino / 对策委员会",
                                iconUrl = "",
                                type = 0,
                                order = 1,
                                createdAtSec = 0L,
                                detailUrl = "https://www.gamekee.com/ba/tj/46680.html",
                                tab = BaGuideCatalogTab.Student
                            ),
                            isFavorite = true,
                            onOpenGuide = {},
                            onToggleFavorite = {}
                        )
                    }
                }
            }
        }
    }

    @Test
    fun aboutAppCardLight() {
        captureRoboImage(filePath = "src/test/screenshots/design-system/about_app_card_light.png") {
            CompositionLocalProvider(LocalTextCopyExpandedOverride provides false) {
                MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF3F4F6))
                            .padding(16.dp)
                    ) {
                        AboutAppCardSection(
                            appLabel = "KeiOS",
                            packageInfo = currentPackageInfo(),
                            cardColor = Color(0x223B82F6),
                            accent = MiuixTheme.colorScheme.primary,
                            subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant,
                            expanded = true,
                            onExpandedChange = {},
                            onOpenDebugActivity = {}
                        )
                    }
                }
            }
        }
    }

    @Test
    fun aboutAppCardCollapsedLight() {
        captureRoboImage(filePath = "src/test/screenshots/design-system/about_app_card_collapsed_light.png") {
            CompositionLocalProvider(LocalTextCopyExpandedOverride provides false) {
                MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF3F4F6))
                            .padding(16.dp)
                    ) {
                        AboutAppCardSection(
                            appLabel = "KeiOS",
                            packageInfo = currentPackageInfo(),
                            cardColor = Color(0x223B82F6),
                            accent = MiuixTheme.colorScheme.primary,
                            subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant,
                            expanded = false,
                            onExpandedChange = {},
                            onOpenDebugActivity = {}
                        )
                    }
                }
            }
        }
    }

    @Test
    fun topBarSearchShellLight() {
        captureRoboImage(filePath = "src/test/screenshots/design-system/topbar_search_shell_light.png") {
            CompositionLocalProvider(LocalTextCopyExpandedOverride provides false) {
                MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF3F4F6))
                            .padding(16.dp)
                    ) {
                        AppTopBarSection(
                            title = "",
                            largeTitle = "图鉴",
                            scrollBehavior = MiuixScrollBehavior(),
                            color = Color.Transparent,
                            searchBarVisible = true,
                            searchBarAnimationLabelPrefix = "screenshotTopBar"
                        ) {
                            AppTopBarSearchField(
                                value = "星野",
                                onValueChange = {},
                                label = "搜索学生 / NPC / 卫星",
                                modifier = Modifier.padding(horizontal = AppChromeTokens.searchFieldHorizontalPadding)
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun controlClusterLight() {
        captureRoboImage(filePath = "src/test/screenshots/design-system/control_cluster_light.png") {
            CompositionLocalProvider(LocalTextCopyExpandedOverride provides false) {
                MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF3F4F6))
                            .padding(16.dp)
                    ) {
                        AppOverviewCard(
                            title = "交互控件",
                            subtitle = "统一尺寸、按压反馈和选项行高",
                            containerColor = Color.White,
                            borderColor = Color(0xFFD7DFEA)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.sectionGap)
                            ) {
                                GlassTextButton(
                                    backdrop = null,
                                    text = "立即刷新",
                                    leadingIcon = MiuixIcons.Regular.Refresh,
                                    onClick = {},
                                    variant = GlassVariant.SheetAction
                                )
                                GlassTextButton(
                                    backdrop = null,
                                    text = "已读",
                                    onClick = {},
                                    variant = GlassVariant.Compact
                                )
                                GlassIconButton(
                                    backdrop = null,
                                    icon = MiuixIcons.Regular.Refresh,
                                    contentDescription = "刷新",
                                    onClick = {},
                                    variant = GlassVariant.Compact
                                )
                                LiquidDropdownColumn {
                                    LiquidDropdownImpl(
                                        text = "默认排序",
                                        optionSize = 3,
                                        isSelected = true,
                                        index = 0,
                                        onSelectedIndexChange = {}
                                    )
                                    LiquidDropdownImpl(
                                        text = "创建条目：新到旧",
                                        optionSize = 3,
                                        isSelected = false,
                                        index = 1,
                                        onSelectedIndexChange = {}
                                    )
                                    LiquidDropdownImpl(
                                        text = "创建条目：旧到新",
                                        optionSize = 3,
                                        isSelected = false,
                                        index = 2,
                                        onSelectedIndexChange = {}
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class AppDesignSystemScreenshotTestApp : Application()
