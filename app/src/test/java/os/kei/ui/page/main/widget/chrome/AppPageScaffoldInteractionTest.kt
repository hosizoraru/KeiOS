package os.kei.ui.page.main.widget.chrome

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = Application::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class AppPageScaffoldInteractionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun topBarNavigationWinsHitTestingOverOverlappingBodyContent() {
        var navigationClicks = 0
        var bodyClicks = 0
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                AppPageScaffold(
                    title = "Page title",
                    navigationIcon = {
                        Box(
                            Modifier
                                .size(52.dp)
                                .testTag("navigation")
                                .clickable { navigationClicks++ },
                        )
                    },
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clickable { bodyClicks++ },
                    )
                }
            }
        }

        composeRule.onNodeWithTag("navigation").performTouchInput { click() }
        composeRule.runOnIdle {
            assertEquals(1, navigationClicks)
            assertEquals(0, bodyClicks)
        }
    }
}
