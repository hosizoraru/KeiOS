package os.kei.ui.page.main.widget.sheet

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.page.main.widget.glass.BackdropScene
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdropOverridesFallback
import os.kei.ui.page.main.widget.glass.LocalRetainedLiquidMaterial
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = Application::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
/**
 * The one wiring test for SheetSurfaceCard: it is AppSurfaceCard with `exportBackdropToContent = true`, and
 * every other export rule (no parent, disabled effects, window boundary) is AppSurfaceCardBackdropTest's.
 */
class SheetSurfaceCardBackdropTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sheetSurfaceCardExportsItsMaterialToNestedControls() {
        var sheetBackdrop: Backdrop? = null
        var cardBackdrop: Backdrop? = null
        var overridesFallback = false

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val parentBackdrop = rememberLayerBackdrop()
                sheetBackdrop = parentBackdrop
                BackdropScene(parentBackdrop, 240.dp) {
                    CompositionLocalProvider(
                        LocalLiquidParentBackdrop provides parentBackdrop,
                        LocalRetainedLiquidMaterial provides true,
                    ) {
                        SheetSurfaceCard {
                            cardBackdrop = LocalLiquidParentBackdrop.current
                            overridesFallback = LocalLiquidParentBackdropOverridesFallback.current
                            Box(
                                modifier =
                                    Modifier
                                        .size(24.dp)
                                        .testTag("sheet-card-content"),
                            )
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithTag("sheet-card-content").assertExists()
        composeRule.runOnIdle {
            assertNotNull(sheetBackdrop)
            assertNotNull(cardBackdrop)
            assertNotSame(sheetBackdrop, cardBackdrop)
            assertTrue(overridesFallback)
        }
    }
}
