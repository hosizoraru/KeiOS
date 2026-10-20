package os.kei.ui.page.main.widget.sheet

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.page.main.widget.glass.UniformColorBackdrop
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35])
class LiquidSheetScrimDrawingTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun plateTracksItsVisibleHeightAndPresentationWithoutRecomposition() {
        var height by mutableIntStateOf(40)
        var presentation by mutableFloatStateOf(1f)
        var compositions = 0
        val backdrop = UniformColorBackdrop(Color.Blue)
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                SideEffect { compositions++ }
                Box(Modifier.size(100.dp).background(Color.White).testTag("scrim")) {
                    LiquidSheetScrim(backdrop, 8.dp, { presentation }, { height })
                }
            }
        }
        fun pixels() = composeRule.onNodeWithTag("scrim").captureToImage().toPixelMap()
        fun assertPlateAt(y: Int, visible: Boolean) {
            val p = pixels()
            val color = p[p.width / 2, y]
            if (visible) {
                assertTrue(color.blue > 0.95f && color.red < 0.15f, "$y: $color")
            } else {
                assertTrue(abs(color.red - color.blue) < 0.01f, "$y: $color")
            }
        }
        assertPlateAt(20, true)
        assertPlateAt(60, false)
        val initial = compositions
        composeRule.runOnIdle { height = 70 }
        assertPlateAt(60, true)
        assertPlateAt(85, false)
        composeRule.runOnIdle { presentation = 0f }
        val gone = pixels()[50, 20]
        assertTrue(gone.red > 0.98f && gone.green > 0.98f && gone.blue > 0.98f, "$gone")
        assertEquals(initial, compositions)
    }
}
