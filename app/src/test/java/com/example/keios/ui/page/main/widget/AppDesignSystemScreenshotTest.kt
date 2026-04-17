package com.example.keios.ui.page.main.widget

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = AppDesignSystemScreenshotTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi"
)
class AppDesignSystemScreenshotTest {

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
}

class AppDesignSystemScreenshotTestApp : Application()
