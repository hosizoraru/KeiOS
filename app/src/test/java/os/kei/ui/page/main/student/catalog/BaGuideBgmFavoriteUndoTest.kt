package os.kei.ui.page.main.student.catalog

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import os.kei.R
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.catalog.component.BaGuideBgmPlaybackCoordinator
import os.kei.ui.page.main.student.catalog.component.BaGuideBgmPlaybackUiState
import os.kei.ui.page.main.student.catalog.component.BaGuideBgmNativeSessionSnapshot
import os.kei.ui.page.main.student.catalog.component.BaGuideBgmQueueMode
import os.kei.ui.page.main.student.catalog.component.withNativeSession
import os.kei.ui.page.main.student.catalog.page.BaGuideFavoriteBgmMusicContent
import os.kei.ui.page.main.student.catalog.state.BaGuideFavoriteBgmListDerivedState
import os.kei.ui.page.main.student.catalog.state.BaGuideFavoriteBgmOfflineCacheUiState
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * The favourites list shows the undo card while an offer is pending.
 *
 * `BaGuideBgmUndoBlock` was first written with its own strings and an Undo action and then **never called**:
 * an affordance that compiled and rendered nowhere. This renders the list itself, so it fails if the list
 * stops placing the card, and checks the card's Undo reaches the list's undo callback. The removal, restore
 * and expiry are in `BaGuideBgmFavoriteUndoControllerTest`.
 */
@RunWith(AndroidJUnit4::class)
@Config(
    application = Application::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class BaGuideBgmFavoriteUndoTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `the favourites list shows the undo card for a pending removal`() {
        var undoClicks = 0
        setFavoritesList(pendingUndoFavorite = Removed, onUndo = { undoClicks += 1 })

        composeRule
            .onNodeWithText(Removed.studentTitle, substring = true, useUnmergedTree = true)
            .fetchSemanticsNode()
        composeRule.onNodeWithText(context.getString(R.string.ba_catalog_bgm_action_undo)).performClick()

        assertEquals(1, undoClicks, "the card's Undo must reach the list's undo callback")
    }

    @Test
    fun `playback page shows the native unfavorited track and requests its artwork`() {
        val playing = testBgmFavorite(
            audioUrl = "https://example.invalid/unfavorited.ogg",
            title = "Lobby Theme",
            studentTitle = "Hina",
            studentImageUrl = "https://example.invalid/hina.png",
        )
        val restored = BaGuideBgmPlaybackUiState().withNativeSession(
            BaGuideBgmNativeSessionSnapshot(
                queue = listOf(playing),
                selectedAudioUrl = playing.audioUrl,
                queueMode = BaGuideBgmQueueMode.Continuous,
            ),
            restoreQueue = true,
        )
        val requested = mutableListOf<String>()
        setFavoritesList(
            pendingUndoFavorite = null,
            onUndo = {},
            playbackState = restored,
            onRequestVisibleImages = { requested.addAll(it) },
        )

        composeRule.onNodeWithText(playing.studentTitle).assertIsDisplayed()
        assertTrue(playing.studentImageUrl in requested)
        assertTrue(restored.favorites.isEmpty())
    }

    private fun setFavoritesList(
        pendingUndoFavorite: GuideBgmFavoriteItem?,
        onUndo: () -> Unit,
        playbackState: BaGuideBgmPlaybackUiState = BaGuideBgmPlaybackUiState(),
        onRequestVisibleImages: (List<String>) -> Unit = {},
    ) {
        val coordinator = BaGuideBgmPlaybackCoordinator(context)
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                BaGuideFavoriteBgmMusicContent(
                    catalog = BaGuideCatalogBundle.EMPTY,
                    favorites = emptyList(),
                    derivedState = BaGuideFavoriteBgmListDerivedState(),
                    offlineCacheState = BaGuideFavoriteBgmOfflineCacheUiState(),
                    playbackCoordinator = coordinator,
                    playbackState = playbackState,
                    volumeControlVisible = false,
                    lastAudibleVolume = 1f,
                    accent = Color(0xFF3B82F6),
                    bottomBarScrollConnection = object : NestedScrollConnection {},
                    topPadding = 0.dp,
                    bottomPadding = 0.dp,
                    // Inactive keeps the playback backends from starting; the card does not depend on it.
                    isPageActive = false,
                    onSliderInteractionChanged = {},
                    onVolumeControlVisibleChange = {},
                    onLastAudibleVolumeChange = {},
                    onScrollBoundsChange = { _, _ -> },
                    onRemoveBgmFavorite = {},
                    pendingUndoFavorite = pendingUndoFavorite,
                    onUndoRemoveBgmFavorite = onUndo,
                    onRequestOfflineCache = { _, _, _ -> },
                    onToggleFavoriteCache = { _, _ -> },
                    onRequestVisibleImages = onRequestVisibleImages,
                    onOpenGuide = {},
                    onRequestGuideDetailTab = { _, _ -> },
                )
            }
        }
        composeRule.waitForIdle()
    }
}

private val Removed =
    testBgmFavorite(
        audioUrl = "https://example.invalid/bgm/removed.ogg",
        sourceUrl = "https://example.invalid/student/hoshino",
        title = "Removed Track",
        studentTitle = "Hoshino",
        favoritedAtMs = 2_000L,
    )
