package os.kei.ui.page.main.widget.glass

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.test.assertTrue

/**
 * Native software rendering checks clipping, decoration order and layer reuse. RenderEffect optics
 * require the separate hardware/device comparison; Robolectric does not execute those shaders.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35])
class SheetGlassBackdropTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun effectsAreOnlyConfiguredWithMeasuredGeometry() {
        val configuredSizes = mutableListOf<Size>()
        composeRule.setContent {
            Box(
                Modifier.size(100.dp).testTag("measured").drawSheetGlassBackdrop(
                    backdrop = UniformColorBackdrop(Color.Blue),
                    shape = { RoundedRectangle(16.dp) },
                    effects = { configuredSizes += size },
                    layerBlock = null, exportedBackdrop = null,
                    highlight = null, shadow = null, innerShadow = null,
                    onDrawSurface = {}, onDrawFront = {},
                ),
            )
        }
        val image = composeRule.onNodeWithTag("measured").captureToImage()
        composeRule.runOnIdle {
            assertTrue(configuredSizes.isNotEmpty())
            assertTrue(
                configuredSizes.all { it == Size(image.width.toFloat(), image.height.toFloat()) },
                "native filters must not be built with unmeasured geometry: $configuredSizes",
            )
        }
    }

    @Test
    fun outerCropPreservesGroupedOpacityWithoutAnInnerTarget() {
        var mergeClip by mutableStateOf(false)
        var alpha by mutableStateOf(0.37f)
        var crop by mutableStateOf(53.25.dp)
        var color by mutableStateOf(Color.Blue)
        composeRule.setContent {
            val field = remember {
                object : Backdrop {
                    override val isCoordinatesDependent = false
                    override fun DrawScope.drawBackdrop(
                        density: Density,
                        coordinates: LayoutCoordinates?,
                        layerBlock: (GraphicsLayerScope.() -> Unit)?,
                    ) {
                        drawRect(color)
                        drawRect(Color.Yellow, Offset(size.width / 2f, 0f), Size(size.width / 2f, size.height))
                    }
                }
            }
            Box(Modifier.size(120.dp).background(Color.Magenta).testTag("crop")) {
                Box(
                    Modifier.size(100.dp).graphicsLayer {
                        compositingStrategy =
                            if (mergeClip) androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                            else androidx.compose.ui.graphics.CompositingStrategy.Auto
                        this.alpha = alpha
                        clip = true
                        shape = object : Shape {
                            override fun createOutline(size: Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: Density) =
                                Outline.Rectangle(Rect(0f, 0f, size.width, with(density) { crop.toPx() }))
                        }
                    }.drawSheetGlassBackdrop(
                        backdrop = field, shape = { RectangleShape }, effects = {}, layerBlock = null,
                        exportedBackdrop = null, highlight = null, shadow = null, innerShadow = null,
                        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.06f)) },
                        onDrawFront = {},
                        clip = !mergeClip,
                    ),
                ) {
                    Box(Modifier.size(30.dp).background(Color.Green.copy(alpha = 0.4f)))
                }
            }
        }
        val poses = listOf(
            Triple(0.37f, 53.25.dp, Color.Blue),
            Triple(1f, 100.dp, Color.Blue),
            Triple(0.17f, 29.7.dp, Color.Red),
            Triple(0f, 0.dp, Color.Red),
            Triple(0.81f, 100.dp, Color.Blue),
        )
        fun pose(value: Triple<Float, androidx.compose.ui.unit.Dp, Color>) {
            composeRule.runOnIdle { alpha = value.first; crop = value.second; color = value.third }
        }
        val expected = poses.map {
            pose(it)
            composeRule.onNodeWithTag("crop").captureToImage().toPixelMap()
        }
        composeRule.runOnIdle { mergeClip = true }
        poses.forEachIndexed { index, value ->
            pose(value)
            assertClose(expected[index], composeRule.onNodeWithTag("crop").captureToImage().toPixelMap())
        }
    }

    @Test
    fun unrelatedCompositionDoesNotReconfigureTheMaterial() {
        var revision by mutableIntStateOf(0)
        var effectApplications = 0
        val field = UniformColorBackdrop(Color.Blue)
        val shape: () -> Shape = { RoundedRectangle(16.dp) }
        val effects: BackdropEffectScope.() -> Unit = { effectApplications++ }
        val transform: GraphicsLayerScope.() -> Unit = {}
        val surface: DrawScope.() -> Unit = {}
        val front: DrawScope.() -> Unit = {}
        composeRule.setContent {
            val label = revision.toString()
            val export = rememberLayerBackdrop()
            Box(
                Modifier.size(100.dp).testTag("surface").drawSheetGlassBackdrop(
                    field, shape, effects, transform, export, null, null, null, surface, front,
                ),
            ) {
                BasicText(label)
            }
        }
        composeRule.onNodeWithTag("surface").captureToImage()
        val initial = effectApplications
        composeRule.runOnIdle { revision++ }
        composeRule.onNodeWithTag("surface").captureToImage()
        assertTrue(initial > 0)
        assertTrue(effectApplications == initial, "unchanged material was configured $initial -> $effectApplications times")
    }

    @Test
    fun materialAndDecorationsMatchBeforeAndAfterMotionAndResize() {
        var shifted by mutableStateOf(false)
        var dark by mutableStateOf(false)
        var optimized by mutableStateOf(false)
        var compact by mutableStateOf(false)
        composeRule.setContent {
            // A fresh library reference supplies the requested colors. Its combined layout/draw
            // node can retain old surface callbacks on a theme-only change. Keep our node alive
            // throughout the replay to check that moving, recoloring and regrowing all invalidate it.
            key(if (optimized) null else Triple(shifted, dark, compact)) {
                Scene("surface", optimized, shifted, dark, compact)
            }
        }
        val poses = listOf(
            Triple(false, false, false), Triple(true, false, false), Triple(true, true, false),
            Triple(true, true, true), Triple(false, false, false),
        )
        fun pose(value: Triple<Boolean, Boolean, Boolean>) {
            composeRule.runOnIdle { shifted = value.first; dark = value.second; compact = value.third }
        }
        val expected = poses.map { value ->
            pose(value)
            composeRule.onNodeWithTag("surface").captureToImage().toPixelMap()
        }
        composeRule.runOnIdle { optimized = true }
        poses.forEachIndexed { index, value ->
            pose(value)
            val actual = composeRule.onNodeWithTag("surface").captureToImage().toPixelMap()
            try {
                assertClose(expected[index], actual)
            } catch (failure: AssertionError) {
                throw AssertionError("pose $index ($value): ${failure.message}", failure)
            }
        }
    }

    @Test
    fun recycledProgramsPreserveIndependentMaterialsAcrossRemounts() {
        var epoch by mutableIntStateOf(0)
        var optimized by mutableStateOf(false)
        var dark by mutableStateOf(false)
        var compact by mutableStateOf(false)
        composeRule.setContent {
            androidx.compose.foundation.layout.Row {
                key(epoch, optimized, dark, compact) {
                    Scene("first", optimized, shifted = false, dark = dark, compact = compact)
                    Scene("second", optimized, shifted = true, dark = !dark, compact = !compact)
                }
            }
        }
        val poses = listOf(false to false, true to true, false to true, true to false)
        fun pose(value: Pair<Boolean, Boolean>) {
            composeRule.runOnIdle {
                dark = value.first
                compact = value.second
                epoch++
            }
        }
        val expected = poses.map { value ->
            pose(value)
            listOf("first", "second").map {
                composeRule.onNodeWithTag(it).captureToImage().toPixelMap()
            }
        }
        composeRule.runOnIdle { optimized = true }
        repeat(2) {
            poses.forEachIndexed { index, value ->
                pose(value)
                listOf("first", "second").forEachIndexed { material, tag ->
                    assertClose(expected[index][material], composeRule.onNodeWithTag(tag).captureToImage().toPixelMap())
                }
            }
        }
    }

    @Test
    fun movingSurfaceKeepsItsSampleAnchoredToTheScene() {
        var optimized by mutableStateOf(false)
        var position by mutableStateOf(0.dp)
        composeRule.setContent {
            val scene = rememberLayerBackdrop {
                drawRect(Color.Blue)
                drawRect(Color.Green, Offset(0f, size.height / 2f), Size(size.width, size.height / 2f))
                drawContent()
            }
            val export = rememberLayerBackdrop()
            Box(Modifier.width(160.dp).height(320.dp).testTag("scene-motion")) {
                Box(Modifier.matchParentSize().layerBackdrop(scene))
                Box(
                    Modifier.width(100.dp).height(180.dp).drawLiquidSurfaceBackdrop(
                        cacheRecording = optimized,
                        backdrop = scene,
                        shape = { RoundedRectangle(24.dp) },
                        effects = {},
                        highlight = null,
                        shadow = null,
                        innerShadow = null,
                        layerBlock = { translationY = position.toPx() },
                        exportedBackdrop = export,
                        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.2f)) },
                    ),
                )
            }
        }
        val offsets = listOf(0.dp, 6.25.dp, 38.7.dp, 93.1.dp, 0.dp)
        val expected = offsets.map { offset ->
            composeRule.runOnIdle { position = offset }
            composeRule.onNodeWithTag("scene-motion").captureToImage().toPixelMap()
        }
        composeRule.runOnIdle { optimized = true }
        offsets.forEachIndexed { index, offset ->
            composeRule.runOnIdle { position = offset }
            val actual = composeRule.onNodeWithTag("scene-motion").captureToImage().toPixelMap()
            assertClose(expected[index], actual)
        }
    }

    @Test
    fun sourceIsRecordedOncePerDrawAndSurvivesReattachment() {
        var attached by mutableStateOf(true)
        var color by mutableStateOf(Color.Blue)
        var sourceDraws = 0
        var contentDraws = 0
        val field = object : Backdrop {
            override val isCoordinatesDependent = false
            override fun DrawScope.drawBackdrop(
                density: Density,
                coordinates: LayoutCoordinates?,
                layerBlock: (GraphicsLayerScope.() -> Unit)?,
            ) {
                sourceDraws++
                drawRect(color)
            }
        }
        composeRule.setContent {
            val export = rememberLayerBackdrop()
            if (attached) {
                Box(
                    Modifier.size(100.dp).testTag("surface").drawSheetGlassBackdrop(
                        backdrop = field,
                        shape = { RoundedRectangle(16.dp) },
                        effects = {},
                        layerBlock = {},
                        exportedBackdrop = export,
                        highlight = null,
                        shadow = null,
                        innerShadow = null,
                        onDrawSurface = {},
                        onDrawFront = {},
                    ).drawWithContent { contentDraws++; drawContent() },
                )
            }
        }
        fun captureAndCheck() {
            composeRule.onNodeWithTag("surface").captureToImage()
            composeRule.runOnIdle {
                assertTrue(sourceDraws > 0)
                assertTrue(contentDraws == sourceDraws, "$sourceDraws source / $contentDraws content")
            }
        }
        captureAndCheck()
        composeRule.runOnIdle { color = Color.Red }
        captureAndCheck()
        composeRule.runOnIdle { attached = false }
        composeRule.runOnIdle { attached = true }
        captureAndCheck()
    }

    @Test
    fun aCanvasBackdropStillUpdatesWhenItsDrawStateChanges() {
        var background by mutableStateOf(Color.Blue)
        var foreground by mutableStateOf(Color.Red)
        var sourceDraws = 0
        val field = object : Backdrop {
            override val isCoordinatesDependent = false
            override fun DrawScope.drawBackdrop(
                density: Density,
                coordinates: LayoutCoordinates?,
                layerBlock: (GraphicsLayerScope.() -> Unit)?,
            ) {
                sourceDraws++
                drawRect(background)
            }
        }
        composeRule.setContent {
            val export = rememberLayerBackdrop()
            Box(
                Modifier.size(100.dp).testTag("surface").drawSheetGlassBackdrop(
                    field, { RoundedRectangle(16.dp) }, {}, {}, export, null, null, null, {},
                    { drawRect(foreground, Offset(20f, 20f), Size(10f, 10f)) },
                ),
            )
        }
        composeRule.onNodeWithTag("surface").captureToImage()
        val first = sourceDraws
        composeRule.runOnIdle { foreground = Color.Yellow }
        val changedFront = composeRule.onNodeWithTag("surface").captureToImage().toPixelMap()[25, 25]
        assertTrue(changedFront.red > 0.98f && changedFront.green > 0.98f && changedFront.blue < 0.02f)
        composeRule.runOnIdle { background = Color.Green }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        val pixels = composeRule.onNodeWithTag("surface").captureToImage().toPixelMap()
        assertTrue(sourceDraws > first, "source draws: $first -> $sourceDraws")
        val center = pixels[pixels.width / 2, pixels.height / 2]
        assertTrue(center.green > 0.98f && center.blue < 0.02f && center.red < 0.02f, "$center")
    }

    @Test
    fun aLiveSceneLayerStillUpdatesThroughTheRetainedOpticalRecording() {
        var color by mutableStateOf(Color.Blue)
        composeRule.setContent {
            val scene = rememberLayerBackdrop {
                drawRect(color)
                drawContent()
            }
            val export = rememberLayerBackdrop()
            Box(Modifier.size(160.dp)) {
                Box(Modifier.matchParentSize().layerBackdrop(scene))
                Box(
                    Modifier.size(100.dp).testTag("surface").drawSheetGlassBackdrop(
                        scene, { RoundedRectangle(16.dp) }, {}, {}, export, null, null, null, {}, {},
                    ),
                ) {
                    Box(
                        Modifier.align(Alignment.Center).size(30.dp).drawBackdrop(
                            export, { androidx.compose.ui.graphics.RectangleShape }, {},
                            highlight = null, shadow = null, innerShadow = null,
                        ),
                    )
                }
            }
        }
        val first = composeRule.onNodeWithTag("surface").captureToImage().toPixelMap()
        assertTrue(first[first.width / 2, first.height / 2].blue > 0.98f)
        composeRule.runOnIdle { color = Color.Green }
        val next = composeRule.onNodeWithTag("surface").captureToImage().toPixelMap()
        val center = next[next.width / 2, next.height / 2]
        assertTrue(center.green > 0.98f && center.blue < 0.02f, "live layer changes must not freeze: $center")
    }

    @Test
    fun exportedMaterialExcludesChildrenAndUpdatesItsSurfaceColor() {
        var tint by mutableStateOf(Color.White.copy(alpha = 0.2f))
        var height by mutableStateOf(100.dp)
        composeRule.setContent {
            val export = rememberLayerBackdrop()
            val front = rememberLayerBackdrop()
            val material = rememberCombinedBackdrop(export, front)
            Box(
                Modifier.width(100.dp).height(height).testTag("surface").drawSheetGlassBackdrop(
                    UniformColorBackdrop(Color.Blue), { RoundedRectangle(16.dp) }, {}, {}, export,
                    null, null, null, { drawRect(tint) },
                    {
                        drawLine(
                            Color.White, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f),
                            strokeWidth = 2f,
                        )
                    },
                    exportedFrontBackdrop = front,
                ),
            ) {
                Box(Modifier.matchParentSize().background(Color.Magenta))
                Box(
                    Modifier.align(Alignment.Center).size(40.dp).drawBackdrop(
                        material, { androidx.compose.ui.graphics.RectangleShape }, {},
                        highlight = null, shadow = null, innerShadow = null,
                    ),
                )
            }
        }
        fun sample() = composeRule.onNodeWithTag("surface").captureToImage().toPixelMap().let {
            val line = it[it.width / 2, it.height / 2]
            assertTrue(line.red > 0.98f && line.green > 0.98f, "export callbacks must use logical sheet height: $line")
            it[it.width / 2, it.height / 2 - 5]
        }
        val first = sample()
        assertTrue(abs(first.red - 0.2f) < 0.01f && first.blue > 0.98f, "$first")
        composeRule.runOnIdle { tint = Color.Green.copy(alpha = 0.5f) }
        val next = sample()
        assertTrue(next.red < 0.02f && abs(next.green - 0.5f) < 0.01f && abs(next.blue - 0.5f) < 0.01f, "$next")
        composeRule.runOnIdle { height = 80.dp }
        val resized = sample()
        assertTrue(resized.red < 0.02f && abs(resized.green - 0.5f) < 0.01f, "$resized")
    }

    @Composable
    private fun Scene(tag: String, optimized: Boolean, shifted: Boolean, dark: Boolean, compact: Boolean = false) {
        val export = rememberLayerBackdrop()
        val frontBackdrop = rememberLayerBackdrop()
        val field = remember {
            object : Backdrop {
                override val isCoordinatesDependent = false
                override fun DrawScope.drawBackdrop(
                    density: Density,
                    coordinates: LayoutCoordinates?,
                    layerBlock: (GraphicsLayerScope.() -> Unit)?,
                ) {
                    drawRect(Color(0xFF8CAFE5))
                    drawRect(Color(0xFFE2B86D), Offset(size.width / 2, 0f), Size(size.width / 2, size.height))
                }
            }
        }
        val shape: Shape = RoundedRectangle(if (shifted) 20.dp else 24.dp)
        val transform: GraphicsLayerScope.() -> Unit = {
            translationY = if (shifted) 6.25.dp.toPx() else 0f
            scaleY = if (shifted) 0.97f else 1f
            scaleX = if (shifted) 0.95f else 1f
            rotationZ = if (shifted) 2.3f else 0f
            alpha = if (shifted) 0.93f else 1f
        }
        val highlight = { Highlight.Default.copy(alpha = if (dark) 0.74f else 0.88f) }
        val shadow = { Shadow.Default.copy(color = Color.Black.copy(alpha = if (dark) 0.22f else 0.14f)) }
        val inner = { InnerShadow(radius = 8.dp, alpha = if (dark) 0.18f else 0.11f) }
        val surface: DrawScope.() -> Unit = { drawRect(Color.White.copy(alpha = if (dark) 0.12f else 0.24f)) }
        val front: DrawScope.() -> Unit = {
            drawLine(Color.White, Offset(24.dp.toPx(), 1.dp.toPx()), Offset(size.width - 24.dp.toPx(), 1.dp.toPx()))
        }
        val material = Modifier.drawLiquidSurfaceBackdrop(
            cacheRecording = optimized,
            backdrop = field, shape = { shape }, effects = {}, layerBlock = transform,
            exportedBackdrop = export, highlight = highlight, shadow = shadow, innerShadow = inner,
            onDrawSurface = surface, onDrawFront = front,
            exportedFrontBackdrop = if (optimized) frontBackdrop else null,
        )
        Box(Modifier.testTag(tag).width(160.dp).height(420.dp).background(if (dark) Color(0xFF242428) else Color.White).padding(20.dp)) {
            Box(material.width(if (shifted) 108.dp else 120.dp).height(if (compact) 60.dp else if (shifted) 340.dp else 360.dp).background(Color(0x55FF4D8D)))
        }
    }

    private fun assertClose(expected: PixelMap, actual: PixelMap) {
        var worst = 0f
        var worstAt = ""
        var changed = 0
        for (y in 0 until expected.height) {
            for (x in 0 until expected.width) {
                val e = expected[x, y]
                val a = actual[x, y]
                val delta = maxOf(abs(e.red - a.red), abs(e.green - a.green), abs(e.blue - a.blue), abs(e.alpha - a.alpha))
                if (delta > worst) {
                    worst = delta
                    worstAt = "$x,$y: $e -> $a"
                }
                if (delta > 2f / 255f) changed++
            }
        }
        assertTrue(worst <= 2f / 255f, "worst channel difference ${worst * 255f}/255 at $worstAt; $changed pixels")
    }
}
