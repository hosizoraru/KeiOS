package os.kei.ui.page.main.widget.glass

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35])
class SheetGlassInvalidationTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun recoloringMaterialAndFrontUpdatesTheirExportsWithoutRebuildingOptics() {
        var surface by mutableStateOf(Color.Red)
        var front by mutableStateOf(Color.Green)
        var configurations = 0
        val source = UniformColorBackdrop(Color.Blue)
        val shape: () -> Shape = { RectangleShape }
        val effects: BackdropEffectScope.() -> Unit = { configurations++ }
        composeRule.setContent {
            val materialExport = rememberLayerBackdrop()
            val frontExport = rememberLayerBackdrop()
            val surfaceColor = surface
            val frontColor = front
            Box(
                Modifier.size(80.dp).testTag("material").drawSheetGlassBackdrop(
                    source, shape, effects, null, materialExport, null, null, null,
                    onDrawSurface = { drawRect(surfaceColor) },
                    onDrawFront = {
                        drawRect(frontColor, size = Size(size.width, size.height / 2f))
                    },
                    exportedFrontBackdrop = frontExport,
                ),
            ) {
                Box(
                    Modifier.align(Alignment.BottomEnd).size(20.dp).testTag("export").drawBackdrop(
                        materialExport, shape = shape, effects = {},
                        highlight = null, shadow = null, innerShadow = null,
                    ),
                )
            }
        }
        val initial = pixels("material")
        assertColor(Color.Green, initial[initial.width / 2, initial.height / 4])
        assertColor(Color.Red, initial[initial.width / 2, initial.height * 3 / 4])
        val configured = configurations
        assertTrue(configured > 0)

        composeRule.runOnIdle {
            surface = Color.Magenta
            front = Color.Cyan
        }
        val changed = pixels("material")
        assertColor(Color.Cyan, changed[changed.width / 2, changed.height / 4])
        assertColor(Color.Magenta, changed[changed.width / 2, changed.height * 3 / 4])
        val exported = pixels("export")
        assertColor(Color.Magenta, exported[exported.width / 2, exported.height / 4])
        assertEquals(configured, configurations, "recoloring must not recreate optical filters")
    }

    @Test
    fun replacingTheSamplingTransformKeepsTheSourceAnchoredWithoutRebuildingOptics() {
        var scale by mutableFloatStateOf(1f)
        var configurations = 0
        val effects: BackdropEffectScope.() -> Unit = { configurations++ }
        composeRule.setContent {
            val currentScale = scale
            val transform: GraphicsLayerScope.() -> Unit =
                remember(currentScale) {
                    {
                        scaleX = currentScale
                        scaleY = currentScale
                    }
                }
            Column {
                SamplingScene("retained", transform, effects, retained = true)
                SamplingScene("reference", transform, effects = {}, retained = false)
            }
        }
        assertClose(pixels("reference"), pixels("retained"))
        val configured = configurations
        for (value in listOf(0.8f, 1.15f, 0.9f, 1f)) {
            composeRule.runOnIdle { scale = value }
            assertClose(pixels("reference"), pixels("retained"))
            assertEquals(configured, configurations, "a transform changes the sample, not the optical filter")
        }
    }

    @Test
    fun observedFrontAnimationDoesNotRecordTheMaterialAgain() {
        val front = mutableStateOf(Color.Green)
        var materialRecordings = 0
        var frontRecordings = 0
        var configurations = 0
        val source = UniformColorBackdrop(Color.Blue)
        val shape: () -> Shape = { RectangleShape }
        val surface: DrawScope.() -> Unit = {
            materialRecordings++
            drawRect(Color.Red)
        }
        val foreground: DrawScope.() -> Unit = {
            frontRecordings++
            drawRect(front.value, size = Size(size.width, size.height / 2f))
        }
        val effects: BackdropEffectScope.() -> Unit = { configurations++ }
        composeRule.setContent {
            val export = rememberLayerBackdrop()
            val frontExport = rememberLayerBackdrop()
            Box(
                Modifier.size(80.dp).testTag("material").drawSheetGlassBackdrop(
                    source, shape, effects, null, export, null, null, null, surface, foreground, frontExport,
                ),
            )
        }
        pixels("material")
        val materialBefore = materialRecordings
        val frontBefore = frontRecordings
        val configured = configurations
        composeRule.runOnIdle { front.value = Color.Cyan }
        val changed = pixels("material")
        assertColor(Color.Cyan, changed[changed.width / 2, changed.height / 4])
        assertColor(Color.Red, changed[changed.width / 2, changed.height * 3 / 4])
        assertTrue(frontRecordings > frontBefore)
        assertEquals(materialBefore, materialRecordings, "foreground changes must retain the sampled material")
        assertEquals(configured, configurations)
    }

    @Test
    fun effectCallbackChangesAndObservedOpticalStateBothRebuildTheFilter() {
        var amount by mutableFloatStateOf(0f)
        val observedAmount = mutableFloatStateOf(0f)
        var configurations = 0
        val applied = mutableListOf<Float>()
        val source = UniformColorBackdrop(Color.Blue)
        val shape: () -> Shape = { RectangleShape }
        composeRule.setContent {
            val currentAmount = amount
            Box(
                Modifier.size(80.dp).testTag("material").drawSheetGlassBackdrop(
                    source, shape,
                    effects = {
                        configurations++
                        applied += currentAmount + observedAmount.floatValue
                    },
                    layerBlock = null, exportedBackdrop = null,
                    highlight = null, shadow = null, innerShadow = null,
                    onDrawSurface = {}, onDrawFront = {},
                ),
            )
        }
        pixels("material")
        val initial = configurations
        composeRule.runOnIdle { amount = 1f }
        pixels("material")
        assertTrue(configurations > initial)
        assertEquals(1f, applied.last())
        val changedCallback = configurations
        composeRule.runOnIdle { observedAmount.floatValue = 2f }
        pixels("material")
        assertTrue(configurations > changedCallback)
        assertEquals(3f, applied.last())
    }

    private fun pixels(tag: String) =
        composeRule.onNodeWithTag(tag).captureToImage().toPixelMap()

    @Composable
    private fun SamplingScene(
        tag: String,
        transform: GraphicsLayerScope.() -> Unit,
        effects: BackdropEffectScope.() -> Unit,
        retained: Boolean,
    ) {
        val source = rememberLayerBackdrop()
        val shape: () -> Shape = { RectangleShape }
        Box(Modifier.size(100.dp)) {
            Box(
                Modifier.size(100.dp).layerBackdrop(source).drawBehind {
                    drawRect(Color.Blue)
                    drawRect(Color.Red, topLeft = Offset(size.width * 0.37f, 0f))
                },
            )
            Box(
                Modifier.size(80.dp).testTag(tag).then(
                    if (retained) {
                        Modifier.drawSheetGlassBackdrop(
                            source, shape, effects, transform, null, null, null, null, {}, {},
                        )
                    } else {
                        Modifier.drawBackdrop(
                            source, shape, effects = {}, layerBlock = transform,
                            highlight = null, shadow = null, innerShadow = null,
                        )
                    },
                ),
            )
        }
    }

    private fun assertColor(expected: Color, actual: Color) {
        assertTrue(
            abs(expected.red - actual.red) < 0.01f &&
                abs(expected.green - actual.green) < 0.01f &&
                abs(expected.blue - actual.blue) < 0.01f &&
                abs(expected.alpha - actual.alpha) < 0.01f,
            "$expected != $actual",
        )
    }

    private fun assertClose(expected: PixelMap, actual: PixelMap) {
        assertEquals(expected.width, actual.width)
        assertEquals(expected.height, actual.height)
        for (y in 0 until actual.height) {
            for (x in 0 until actual.width) {
                assertColor(expected[x, y], actual[x, y])
            }
        }
    }
}
