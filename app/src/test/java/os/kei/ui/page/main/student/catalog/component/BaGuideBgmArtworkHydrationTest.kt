package os.kei.ui.page.main.student.catalog.component

import android.app.Application
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import os.kei.ui.page.main.student.catalog.testBgmFavorite
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
class BaGuideBgmArtworkHydrationTest {
    @Test
    fun `late metadata preserves prepared audio but replaces stale artwork identity`() {
        val current = item("old", byteArrayOf(1))
        val replacement = current.withUpdatedBaGuideBgmMetadata(
            item("new", null).buildUpon().setUri("https://example.com/other-source").build(),
        )
        assertEquals(current.localConfiguration, replacement.localConfiguration)
        assertEquals("https://example.com/new.png", replacement.mediaMetadata.artworkUri.toString())
        assertNull(replacement.mediaMetadata.artworkData)
        val sameCover = current.withUpdatedBaGuideBgmMetadata(item("old", null))
        assertContentEquals(byteArrayOf(1), sameCover.mediaMetadata.artworkData)
    }

    @Test
    fun `transient artwork failure retries without redownloading successes`() = runTest {
        val first = testBgmFavorite(audioUrl = "a")
        val second = testBgmFavorite(audioUrl = "b")
        val attempts = mutableMapOf<String, Int>()
        val published = mutableListOf<String>()
        hydrateBaGuideBgmArtwork(
            targets = listOf(first, second),
            isCurrent = { true },
            resolve = {
                val attempt = attempts.getOrDefault(it.audioUrl, 0) + 1
                attempts[it.audioUrl] = attempt
                if (it.audioUrl == "a" && attempt == 1) null else byteArrayOf(7)
            },
            publish = { favorite, _ -> published += favorite.audioUrl },
        )
        assertEquals(mapOf("a" to 2, "b" to 1), attempts)
        assertEquals(listOf("b", "a"), published)
    }

    @Test
    fun `failed artwork is bounded and never published`() = runTest {
        var attempts = 0
        hydrateBaGuideBgmArtwork(
            listOf(testBgmFavorite(audioUrl = "a")),
            isCurrent = { true },
            resolve = { attempts++; null },
            publish = { _, _ -> error("Missing artwork must not publish a fallback cover") },
        )
        assertEquals(3, attempts)
    }

    @Test
    fun `superseded artwork does not reach a different queue`() = runTest {
        var current = true
        hydrateBaGuideBgmArtwork(
            listOf(testBgmFavorite(audioUrl = "a")),
            isCurrent = { current },
            resolve = { current = false; byteArrayOf(1) },
            publish = { _, _ -> error("Stale result reached the player") },
        )
    }

    @Test
    fun `cancellation aborts instead of retrying`() = runTest {
        assertFailsWith<CancellationException> {
            hydrateBaGuideBgmArtwork(
                listOf(testBgmFavorite(audioUrl = "a")),
                isCurrent = { true },
                resolve = { throw CancellationException() },
                publish = { _, _ -> error("Cancelled result reached the player") },
            )
        }
    }

    private fun item(cover: String, data: ByteArray?): MediaItem =
        MediaItem.Builder()
            .setMediaId("same")
            .setUri("https://example.com/prepared-audio")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(cover)
                    .setArtworkUri("https://example.com/$cover.png".toUri())
                    .setArtworkData(data, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    .build(),
            ).build()
}
