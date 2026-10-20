package os.kei.ui.page.main.widget.sheet

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.page.main.widget.glass.AppLiquidWindowBoundary
import os.kei.ui.page.main.widget.glass.LocalLiquidDialogBackdrop
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdropOverridesFallback
import os.kei.ui.page.main.widget.glass.preferredLiquidBackdrop
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class WindowBackdropBoundaryContractTest {
    /**
     * The sheet and modal surfaces sample the scene through `drawBackdrop` and export their own surface.
     * A second `layerBackdrop` producer on the same node records the element into the layer it samples,
     * which the library documents as a draw loop and a RenderThread SIGSEGV — not something a Robolectric
     * render reproduces. The gating and export themselves are behaviour, covered by
     * LiquidGlassBottomSheetTest (no overlay host -> null backdrop; in the host -> the sheet's own) and
     * LiquidGlassDialogTest.dialogReplacesInheritedParentBackdropWithItsOwnSurface.
     */
    @Test
    fun sampledSurfacesNeverAlsoProduceABackdrop() {
        val samplers = mapOf(
            LIQUID_SHEET_SURFACE_SOURCE to ".drawSheetGlassBackdrop(",
            LIQUID_MODAL_SURFACE_SOURCE to ".drawBackdrop(",
            LIQUID_SHEET_RENDERER_SOURCE to "drawBackdrop(density = effectScope,",
        )
        for ((relativePath, sampleCall) in samplers) {
            val surface = windowBoundarySource(relativePath)
            assertTrue(sampleCall in surface, "$relativePath no longer samples; update this guard")
            assertFalse(".layerBackdrop(" in surface, "$relativePath: a second layerBackdrop after drawBackdrop is the documented draw loop")
        }
    }

    /**
     * Every presentation republishes its own surface so controls inside cannot sample the page they float
     * over. The alert is proved at runtime by LiquidGlassDialogTest; this is the only guard for the action
     * sheet. Not going back to a Dialog window is the repo-wide ban in app's PlatformWindowSourceContractTest.
     */
    @Test
    fun modalPresentationsRepublishTheirOwnSurface() {
        for (relativePath in LIQUID_MODAL_CONSUMER_SOURCES) {
            val consumer = windowBoundarySource(relativePath)
            assertTrue(
                "LocalLiquidParentBackdrop provides surface.exportedBackdrop," in consumer,
                "$relativePath must republish its own surface to its content",
            )
        }
    }

    /**
     * Inverted from the contract it replaces, which checked that the anchored panel installed a window
     * boundary before composing caller content.
     *
     * That was the right invariant for a panel in a `Popup`: content there must not inherit a producer
     * from the activity window, because a `LayerBackdrop` resolves its offset through shared
     * `LayoutCoordinates` and two windows have none. But obeying it meant the panel had no backdrop at
     * all — `activeGlassBackdrop` resolved to null and every menu in the app drew a flat filled card
     * while carrying twenty configured optical values that never reached a shader.
     *
     * So the panel moved into the activity window, and the invariant flips: it must *not* install a
     * boundary, and it must go through the overlay portal.
     */
    @Test
    fun theAnchoredPanelIsHostedInWindowRatherThanBehindAWindowBoundary() {
        val panel = windowBoundarySource(SNAPSHOT_POPUP_SOURCE)

        // Not hosting it in a Popup window, which is what blanked its backdrop, is the repo-wide ban in
        // app's PlatformWindowSourceContractTest.
        assertTrue(
            "LiquidMenuPresentation(" in panel,
            "the panel is hosted by LiquidMenuPresentation, which portals it into the activity window",
        )

        val presentation = windowBoundarySource(LIQUID_MENU_PRESENTATION_SOURCE)
        assertTrue(
            "LiquidOverlayPortal {" in presentation,
            "in-window hosting is what makes the scene backdrop reachable",
        )
        // Matched on the call, not the bare name: the presentation's own KDoc names the boundary while
        // explaining why the panel no longer sits behind one.
        assertFalse(
            "LiquidBackdropWindowBoundary {" in presentation ||
                "LiquidBackdropWindowBoundary(" in presentation,
            "installing a boundary here would blank the very backdrop the panel now samples",
        )
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = Application::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class WindowBackdropBoundaryRuntimeTest {
    @get:Rule
    val composeRule = createComposeRule()

    /**
     * A `LayerBackdrop` resolves its offset through shared `LayoutCoordinates`, and two windows have none,
     * so nothing produced outside a window boundary may reach inside it: the page's scene, parent and dialog
     * backdrops, and the "overrides fallback" flag, all arrive here set and must be cleared. Backdrops
     * created inside the boundary are window-local and still resolve.
     */
    @Test
    fun boundaryClearsOuterBackdropsAndAcceptsWindowLocalProviders() {
        var observedInside = false
        var outerScene: Backdrop? = null
        var innerScene: Backdrop? = null
        var innerParent: Backdrop? = null
        var innerDialog: Backdrop? = null
        var innerOverridesFallback = true
        var explicitOnly: Backdrop? = null
        var expectedParent: Backdrop? = null
        var expectedDialog: Backdrop? = null
        var resolvedParent: Backdrop? = null
        var resolvedDialog: Backdrop? = null

        composeRule.setContent {
            val capturedPageBackdrop = rememberLayerBackdrop()
            val windowParentBackdrop = rememberLayerBackdrop()
            val windowDialogBackdrop = rememberLayerBackdrop()
            outerScene = capturedPageBackdrop

            CompositionLocalProvider(
                LocalSceneBackdrop provides capturedPageBackdrop,
                LocalLiquidParentBackdrop provides capturedPageBackdrop,
                LocalLiquidDialogBackdrop provides capturedPageBackdrop,
                LocalLiquidParentBackdropOverridesFallback provides true,
            ) {
                AppLiquidWindowBoundary {
                    val scene = LocalSceneBackdrop.current
                    val parent = LocalLiquidParentBackdrop.current
                    val dialog = LocalLiquidDialogBackdrop.current
                    val overrides = LocalLiquidParentBackdropOverridesFallback.current
                    val explicitResolution = preferredLiquidBackdrop(capturedPageBackdrop)
                    SideEffect {
                        observedInside = true
                        innerScene = scene
                        innerParent = parent
                        innerDialog = dialog
                        innerOverridesFallback = overrides
                        explicitOnly = explicitResolution
                        expectedParent = windowParentBackdrop
                        expectedDialog = windowDialogBackdrop
                    }
                    CompositionLocalProvider(LocalLiquidParentBackdrop provides windowParentBackdrop) {
                        val resolution = preferredLiquidBackdrop(capturedPageBackdrop)
                        SideEffect { resolvedParent = resolution }
                    }
                    CompositionLocalProvider(LocalLiquidDialogBackdrop provides windowDialogBackdrop) {
                        val resolution = preferredLiquidBackdrop(capturedPageBackdrop)
                        SideEffect { resolvedDialog = resolution }
                    }
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertTrue(observedInside)
            assertNotSame(outerScene, innerScene, "the page's scene backdrop must not reach another window")
            assertNull(innerParent)
            assertNull(innerDialog)
            assertFalse(innerOverridesFallback)
            // An explicit backdrop captured outside is rejected too.
            assertNull(explicitOnly)
            assertSame(expectedParent, resolvedParent)
            assertSame(expectedDialog, resolvedDialog)
        }
    }
}

private fun windowBoundarySource(relativePath: String): String {
    val roots = generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { it.parentFile }
    val source = roots.map { File(it, relativePath) }.firstOrNull(File::isFile)
    return requireNotNull(source) {
        "Unable to locate $relativePath from ${System.getProperty("user.dir")}"
    }.readText()
}

private const val LIQUID_SHEET_SURFACE_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/sheet/LiquidSheetSurface.kt"
private const val LIQUID_SHEET_RENDERER_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/glass/SheetGlassBackdrop.kt"
private const val LIQUID_MODAL_SURFACE_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/dialog/LiquidModalSurface.kt"
private val LIQUID_MODAL_CONSUMER_SOURCES =
    listOf(
        // LiquidGlassDialog is a deprecated alias that delegates to LiquidAlert, so it has no
        // material of its own to check.
        "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/dialog/LiquidAlert.kt",
        "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/dialog/LiquidActionSheet.kt",
    )
private const val SNAPSHOT_POPUP_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/sheet/MiuixSnapshotAdapters.kt"
private const val LIQUID_MENU_PRESENTATION_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/glass/LiquidMenuPresentation.kt"
