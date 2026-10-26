package os.kei.appwidget.github

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.R
import os.kei.ui.page.main.widget.glass.LocalLiquidControlsEnabled
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35], qualifiers = "en-rUS-w360dp-h800dp-xxhdpi")
class GitHubWidgetConfigurationScreenTest {
    @get:Rule val composeRule = createComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `large font guide keeps the add action visible and exposes labeled privacy and preview`() {
        var confirmations = 0
        composeRule.setContent {
            val density = LocalDensity.current
            var names by remember { mutableStateOf(false) }
            MiuixTheme(controller = remember { ThemeController(ColorSchemeMode.Light) }) {
                CompositionLocalProvider(
                    LocalLiquidControlsEnabled provides false,
                    LocalDensity provides Density(density.density, 1.5f),
                ) {
                    GitHubWidgetConfigurationScreen(
                        snapshot = GitHubWidgetSnapshot(available = true, trackedCount = 12, updateCount = 2),
                        showProjectNames = names, isGuide = true, ready = true, saving = false,
                        pinSupported = true, message = null,
                        onShowProjectNamesChange = { names = it },
                        onConfirm = { confirmations++ }, onClose = {},
                    )
                }
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.github_widget_add))
            .assertIsDisplayed().performClick()
        assertEquals(1, confirmations)
        composeRule.onNodeWithContentDescription(context.getString(R.string.github_widget_preview, 12, 2))
            .assertExists()
        composeRule.onNodeWithContentDescription(context.getString(R.string.github_widget_show_names))
            .performScrollTo().performClick().assertIsOn()
    }

    @Test
    fun `accepted request reveals the manual fallback without hiding the retry action`() {
        val message = context.getString(R.string.github_widget_pin_requested)
        composeRule.setContent {
            val density = LocalDensity.current
            MiuixTheme(controller = remember { ThemeController(ColorSchemeMode.Light) }) {
                CompositionLocalProvider(
                    LocalLiquidControlsEnabled provides false,
                    LocalDensity provides Density(density.density, 1.5f),
                ) {
                    GitHubWidgetConfigurationScreen(
                        snapshot = GitHubWidgetSnapshot(available = true),
                        showProjectNames = false, isGuide = true, ready = true, saving = false,
                        pinSupported = true, pinRequested = true, message = message,
                        onShowProjectNamesChange = {}, onConfirm = {}, onClose = {},
                    )
                }
            }
        }
        composeRule.onNodeWithText(message).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.github_widget_manual_hint)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.github_widget_retry_pin)).assertIsDisplayed()
    }
}
