package os.kei.ui.page.main.student.catalog.component.bgm

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.page.main.student.catalog.component.LocalBaGuideCatalogImageBitmaps
import os.kei.ui.page.main.student.catalog.component.LocalBaGuideCatalogImageRequester

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35])
class BaGuideBgmArtworkImageTest {
    private val scheduler = TestCoroutineScheduler()

    @get:Rule
    val composeRule = createComposeRule(effectContext = StandardTestDispatcher(scheduler))

    private val owner = ArtworkLifecycleOwner()
    private val bitmaps = mutableStateOf<Map<String, Bitmap>>(emptyMap())
    private val url = mutableStateOf(ARTWORK_URL)
    private val visible = mutableStateOf(true)
    private val requests = mutableListOf<List<String>>()

    @Test
    fun transientMissRecoversRenderedBitmapAndStopsRetries() {
        val recovered = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.RED)
        }
        showArtwork { urls ->
            requests += urls
            if (requests.size == 2) bitmaps.value = mapOf(ARTWORK_URL to recovered)
        }

        composeRule.onNodeWithTag(ARTWORK_TAG).assertDoesNotExist()
        assertEquals(listOf(listOf(ARTWORK_URL)), requests)
        advance(1_100)

        val image = composeRule.onNodeWithTag(ARTWORK_TAG).assertExists().captureToImage()
        assertEquals(Color.Red, image.toPixelMap()[image.width / 2, image.height / 2])
        advance(60_000)
        assertEquals(2, requests.size, "A real bitmap must cancel all remaining retries")
    }

    @Test
    fun persistentMissHasFiniteBudgetAndUnrelatedImagesDoNotRestartIt() {
        showArtwork()
        advance(20_000)
        assertEquals(4, requests.size)

        composeRule.runOnIdle {
            bitmaps.value = mapOf(
                "https://example.test/other.png" to Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
            )
        }
        advance(60_000)
        assertEquals(4, requests.size, "Cache updates for other URLs must not create a retry loop")
        composeRule.onNodeWithTag(ARTWORK_TAG).assertDoesNotExist()
    }

    @Test
    fun backgroundCancelsRetriesAndForegroundStartsNewBudget() {
        owner.registry.currentState = Lifecycle.State.CREATED
        showArtwork()
        advance(20_000)
        assertEquals(0, requests.size)

        composeRule.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        composeRule.waitForIdle()
        assertEquals(1, requests.size)
        composeRule.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        advance(20_000)
        assertEquals(1, requests.size, "Background must not perform delayed retries")

        composeRule.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        composeRule.waitForIdle()
        assertEquals(2, requests.size, "A foreground entry retries the still-missing image immediately")
        advance(1_100)
        assertEquals(3, requests.size)
        composeRule.onNodeWithTag(ARTWORK_SLOT_TAG).assertExists()
        composeRule.runOnIdle { visible.value = false }
        // Apply removal before advancing the retry scheduler past its next deadline.
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ARTWORK_SLOT_TAG).assertDoesNotExist()
        assertEquals(3, requests.size, "Removing the artwork must not trigger another request")
        advance(60_000)
        assertEquals(3, requests.size, "Leaving composition must cancel retries")
    }

    @Test
    fun blankUrlNeverRequestsAnImage() {
        url.value = "  "
        showArtwork()
        advance(60_000)
        assertEquals(emptyList(), requests)
        composeRule.onNodeWithTag(ARTWORK_TAG).assertDoesNotExist()
    }

    @Test
    fun changingUrlCancelsOldImageRetries() {
        showArtwork()
        val replacement = "https://example.test/replacement.png"
        composeRule.runOnIdle { url.value = " $replacement " }
        composeRule.waitForIdle()
        advance(20_000)
        assertEquals(
            listOf(listOf(ARTWORK_URL)) + List(4) { listOf(replacement) },
            requests,
            "Only the new normalized URL may be retried after switching tracks",
        )
    }

    private fun showArtwork(request: (List<String>) -> Unit = { requests += it }) {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalLifecycleOwner provides owner,
                LocalBaGuideCatalogImageBitmaps provides bitmaps.value,
                LocalBaGuideCatalogImageRequester provides request,
            ) {
                Box(Modifier.size(32.dp)) {
                    if (visible.value) {
                        Box(Modifier.testTag(ARTWORK_SLOT_TAG)) {
                            BaGuideBgmArtworkImage(
                                imageUrl = url.value,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(32.dp).testTag(ARTWORK_TAG),
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun advance(milliseconds: Long) {
        composeRule.mainClock.advanceTimeBy(milliseconds)
        composeRule.waitForIdle()
    }

    private class ArtworkLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this).apply {
            currentState = Lifecycle.State.STARTED
        }
        override val lifecycle: Lifecycle = registry
    }

    private companion object {
        const val ARTWORK_URL = "https://example.test/artwork.png"
        const val ARTWORK_TAG = "bgm-artwork"
        const val ARTWORK_SLOT_TAG = "bgm-artwork-slot"
    }
}
