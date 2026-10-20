@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.sheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import os.kei.ui.page.main.widget.glass.LiquidOverlayHostState
import os.kei.ui.page.main.widget.glass.LocalLiquidOverlayHost
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * The scene-level backdrop that on-top glass surfaces (notably [LiquidGlassBottomSheet]) sample.
 *
 * Per the kyant.backdrop tutorial: a glass overlay needs a real `LayerBackdrop` in its draw path
 * with the entire app content rendered behind it. We wire that backdrop once at the activity
 * root and expose it through this [staticCompositionLocalOf] so any composable lower in the tree
 * can reach it without prop-drilling. The default value is the library's [emptyBackdrop] no-op
 * so consumers that want graceful degradation outside of the activity tree (previews, tests,
 * isolated harnesses) don't crash — they just render without backdrop sampling.
 */
val LocalSceneBackdrop = staticCompositionLocalOf<Backdrop> { emptyBackdrop() }

/**
 * Hosts the scene backdrop. Wraps [content] with a `Modifier.layerBackdrop(...)` so the entire
 * subtree (the actual MainScreen) becomes the source any `Modifier.drawBackdrop(...)` consumer
 * samples. The backdrop pre-paints [backgroundColor] underneath the content so glass surfaces
 * see a stable opaque background instead of a transparent compose surface.
 *
 * Also installs the [LiquidOverlayHostState] that sheets and other on-top glass render into. The
 * overlay is a **sibling** of the captured content, never a child: content inside
 * `Modifier.layerBackdrop` is recorded into the backdrop, so an overlay placed there would sample
 * the layer it is being drawn into. The library documents that loop as a RenderThread SIGSEGV.
 *
 * Usage at the activity root:
 *
 * ```kotlin
 * SceneBackdropHost(backgroundColor = MiuixTheme.colorScheme.background) {
 *     MainScreen(...)
 * }
 * ```
 */
@Composable
fun SceneBackdropHost(
    backgroundColor: Color = MiuixTheme.colorScheme.background,
    content: @Composable () -> Unit,
) {
    // Pre-multiply onto black so even a translucent theme background still produces an opaque
    // source for the blur. The compose Color stays in the graph; the blend happens inside the
    // backdrop drawer. compositeOver keeps theme transparency math correct.
    val solidBackground = backgroundColor.compositeOver(Color.Black)
    val sceneBackdrop = rememberLayerBackdrop {
        drawRect(solidBackground)
        drawContent()
    }
    val overlayHost = remember { LiquidOverlayHostState() }
    SideEffect {
        // The captured display list points at the same retained page used on screen. A second
        // full-window target here would duplicate that page. Preserve its former Offscreen bounds
        // for blur samples at window edges without forcing another texture.
        sceneBackdrop.graphicsLayer.compositingStrategy = CompositingStrategy.Auto
        sceneBackdrop.graphicsLayer.clip = true
    }
    CompositionLocalProvider(
        LocalSceneBackdrop provides sceneBackdrop,
        LocalLiquidOverlayHost provides overlayHost,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Isolate the capture's display list from overlay sibling redraws.
                    .graphicsLayer()
                    .layerBackdrop(sceneBackdrop)
                    // Both the visible page and the exported backdrop reference this producer.
                    // Retain the producer before a modal opens too: switching its target during
                    // entry would move a full-window allocation onto the first sheet frame.
                    // Child animations and page changes continue to invalidate the producer.
                    .graphicsLayer {
                        compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                    },
            ) {
                content()
            }
            // The overlay layer is a *sibling* of the page content, so it inherits nothing from the
            // page root — including its `testTagsAsResourceId`. Without this, a test tag inside a
            // sheet, alert, action sheet or menu never reaches the view hierarchy's resource id and
            // is invisible to UiAutomator, which is what kept every presentation out of the baseline
            // profile. Two surfaces had already worked around it one at a time
            // (`LiquidGlassActionMenu`, `Modifier.shareImportSheetTags`); doing it here covers the
            // whole layer once. It only names nodes for tooling — no behaviour, and TalkBack ignores it.
            Box(modifier = Modifier.semantics { testTagsAsResourceId = true }) {
                overlayHost.Content()
            }
        }
    }
}
