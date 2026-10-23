package os.kei.ui.page.main.student.catalog.component

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import os.kei.ui.page.main.student.BA_GUIDE_BGM_MEDIA_METADATA_AUDIO_URL
import os.kei.ui.page.main.student.BA_GUIDE_BGM_MEDIA_METADATA_SOURCE_URL
import os.kei.ui.page.main.student.BA_GUIDE_BGM_MEDIA_METADATA_STUDENT_TITLE
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.catalog.testBgmFavorite
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
class BaGuideBgmMediaItemMapperTest {
    @After
    fun tearDown() {
        BaGuideBgmMediaArtworkPayloadResolver.clearMemoryCache()
    }

    @Test
    fun `media item preserves queue metadata`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val artworkData = byteArrayOf(1, 2, 3, 4)
        val item = track(
            id = "https://example.com/bgm.mp3?token=1",
            title = "Theme",
            student = "Hoshino",
            image = "https://example.com/hoshino.png"
        ).toBaGuideBgmMediaItem(
            context = context,
            playbackUrlResolver = { _, favorite -> favorite.audioUrl },
            artworkUrlResolver = { favorite -> favorite.studentImageUrl },
            artworkData = artworkData
        )

        assertNotNull(item)
        assertEquals("https://example.com/bgm.mp3?token=1", item.mediaId)
        assertEquals("https://example.com/bgm.mp3?token=1", item.localConfiguration?.uri.toString())
        assertEquals("Hoshino", item.mediaMetadata.title.toString())
        assertEquals("Hoshino", item.mediaMetadata.displayTitle.toString())
        assertEquals("Memorial Lobby BGM", item.mediaMetadata.artist.toString())
        assertEquals("Memorial Lobby BGM", item.mediaMetadata.subtitle.toString())
        assertEquals("https://example.com/hoshino.png", item.mediaMetadata.artworkUri.toString())
        assertEquals(
            "https://example.com/bgm.mp3?token=1",
            item.mediaMetadata.extras?.getString(BA_GUIDE_BGM_MEDIA_METADATA_AUDIO_URL)
        )
        assertEquals(
            "https://www.gamekee.com/ba/tj/1.html",
            item.mediaMetadata.extras?.getString(BA_GUIDE_BGM_MEDIA_METADATA_SOURCE_URL)
        )
        assertEquals(
            "Hoshino",
            item.mediaMetadata.extras?.getString(BA_GUIDE_BGM_MEDIA_METADATA_STUDENT_TITLE)
        )
        assertContentEquals(artworkData, item.mediaMetadata.artworkData)
        assertEquals(MediaMetadata.PICTURE_TYPE_FRONT_COVER, item.mediaMetadata.artworkDataType)
    }

    @Test
    fun `media item uses stable fallback metadata`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val item = track(
            id = "https://example.com/fallback.ogg",
            title = "",
            student = "",
            image = ""
        ).toBaGuideBgmMediaItem(
            context = context,
            playbackUrlResolver = { _, favorite -> favorite.audioUrl },
            artworkUrlResolver = { favorite -> favorite.studentImageUrl }
        )

        assertNotNull(item)
        assertEquals("https://example.com/fallback.ogg", item.mediaId)
        assertEquals("Unknown student", item.mediaMetadata.title.toString())
        assertEquals("Memorial Lobby BGM", item.mediaMetadata.artist.toString())
    }

    @Test
    fun `media item restores exact original metadata as a transient track`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val favorite = track(
            id = "https://example.com/bgm.mp3?token=original",
            title = "Memorial Theme",
            student = "Hoshino",
            image = "https://example.com/hoshino.png?token=portrait%2Fsignature"
        ).copy(
            imageUrl = "https://example.com/lobby.jpg?token=scene%2Bsignature",
            note = "Original recording\nMemorial lobby",
            favoritedAtMs = 123_456L
        )
        val item = favorite.toBaGuideBgmMediaItem(
            context = context,
            playbackUrlResolver = { _, track -> track.audioUrl }
        )

        assertNotNull(item)
        assertEquals("Hoshino", item.mediaMetadata.title.toString())
        assertEquals(favorite.copy(favoritedAtMs = 0L), item.toBaGuideBgmFavoriteItem())
    }

    @Test
    fun `original blank metadata survives notification display fallbacks`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val favorites = listOf(
            track("https://example.com/theme.mp3", "Theme", "", ""),
            track("https://example.com/unknown.mp3", "", "", "")
        )

        favorites.forEach { favorite ->
            val item = favorite.toBaGuideBgmMediaItem(
                context = context,
                playbackUrlResolver = { _, track -> track.audioUrl }
            )

            assertNotNull(item)
            assertEquals(favorite.copy(favoritedAtMs = 0L), item.toBaGuideBgmFavoriteItem())
        }
    }

    @Test
    fun `legacy extras-only item restores metadata without local configuration`() {
        val audioUrl = "https://example.com/legacy.mp3?token=original"
        val artworkUrl = "https://example.com/legacy.png?token=portrait%2Fsignature"
        val item = MediaItem.Builder()
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Legacy Theme")
                    .setDisplayTitle("Legacy display title")
                    .setArtworkUri(artworkUrl.toUri())
                    .setExtras(
                        Bundle().apply {
                            putString(BA_GUIDE_BGM_MEDIA_METADATA_AUDIO_URL, audioUrl)
                            putString(
                                BA_GUIDE_BGM_MEDIA_METADATA_SOURCE_URL,
                                "https://www.gamekee.com/ba/tj/1.html"
                            )
                            putString(BA_GUIDE_BGM_MEDIA_METADATA_STUDENT_TITLE, "Hoshino")
                        }
                    )
                    .build()
            )
            .build()

        assertEquals("", item.mediaId)
        assertNull(item.localConfiguration)
        assertEquals(
            testBgmFavorite(
                audioUrl = audioUrl,
                title = "Legacy Theme",
                studentTitle = "Hoshino",
                studentImageUrl = artworkUrl,
                imageUrl = artworkUrl,
                sourceUrl = "https://www.gamekee.com/ba/tj/1.html",
                favoritedAtMs = 0L
            ),
            item.toBaGuideBgmFavoriteItem()
        )
    }

    @Test
    fun `stable media id restores standard metadata when audio extra is blank`() {
        val audioUrl = "https://example.com/stable.mp3?token=original"
        val artworkUrl = "https://example.com/stable.png?token=portrait%2Bsignature"
        val item = MediaItem.Builder()
            .setMediaId(audioUrl)
            .setUri("file:///data/user/0/os.kei/cache/playback.mp3")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Hoshino")
                    .setArtworkUri(artworkUrl.toUri())
                    .setExtras(
                        Bundle().apply {
                            putString(BA_GUIDE_BGM_MEDIA_METADATA_AUDIO_URL, " \t")
                            putString(BA_GUIDE_BGM_MEDIA_METADATA_STUDENT_TITLE, " ")
                        }
                    )
                    .build()
            )
            .build()

        assertEquals(
            testBgmFavorite(
                audioUrl = audioUrl,
                title = "Hoshino",
                studentTitle = "Hoshino",
                studentImageUrl = artworkUrl,
                imageUrl = artworkUrl,
                favoritedAtMs = 0L
            ),
            item.toBaGuideBgmFavoriteItem()
        )
    }

    @Test
    fun `legacy metadata falls back to display title and student extra`() {
        val metadata = listOf(
            MediaMetadata.Builder()
                .setTitle(" ")
                .setDisplayTitle("Hoshino")
                .build(),
            MediaMetadata.Builder()
                .setExtras(
                    Bundle().apply {
                        putString(BA_GUIDE_BGM_MEDIA_METADATA_STUDENT_TITLE, "Hoshino")
                    }
                )
                .build()
        )

        metadata.forEach { value ->
            val item = MediaItem.Builder()
                .setMediaId("https://example.com/legacy.mp3")
                .setMediaMetadata(value)
                .build()

            val restored = item.toBaGuideBgmFavoriteItem()

            assertNotNull(restored)
            assertEquals("Hoshino", restored.title)
            assertEquals("Hoshino", restored.studentTitle)
            assertEquals(0L, restored.favoritedAtMs)
        }
    }

    @Test
    fun `explicit original audio extra takes precedence over media id`() {
        val item = MediaItem.Builder()
            .setMediaId("session-track")
            .setUri("file:///data/user/0/os.kei/cache/playback.mp3")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setExtras(
                        Bundle().apply {
                            putString(
                                BA_GUIDE_BGM_MEDIA_METADATA_AUDIO_URL,
                                "https://example.com/original.mp3?token=original"
                            )
                        }
                    )
                    .build()
            )
            .build()

        val restored = item.toBaGuideBgmFavoriteItem()

        assertNotNull(restored)
        assertEquals("https://example.com/original.mp3?token=original", restored.audioUrl)
    }

    @Test
    fun `blank audio identity never falls back to playback uri`() {
        val playbackUrls = listOf(
            "file:///data/user/0/os.kei/cache/playback.mp3",
            "https://example.com/playback.mp3?token=transport"
        )

        assertNull(MediaItem.Builder().build().toBaGuideBgmFavoriteItem())
        playbackUrls.forEach { playbackUrl ->
            val item = MediaItem.Builder()
                .setMediaId(" \t")
                .setUri(playbackUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Theme")
                        .setExtras(
                            Bundle().apply {
                                putString(BA_GUIDE_BGM_MEDIA_METADATA_AUDIO_URL, " \n")
                            }
                        )
                        .build()
                )
                .build()

            assertNull(item.toBaGuideBgmFavoriteItem())
        }
    }

    @Test
    fun `round trip keeps original audio distinct from cached or signed playback uri`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val favorite = track(
            id = "https://example.com/original.mp3?token=original",
            title = "Theme",
            student = "Hoshino",
            image = ""
        )
        val playbackUrls = listOf(
            "file:///data/user/0/os.kei/cache/playback.mp3",
            "https://example.com/playback.mp3?token=transport"
        )

        playbackUrls.forEach { playbackUrl ->
            val item = favorite.toBaGuideBgmMediaItem(
                context = context,
                playbackUrlResolver = { _, _ -> playbackUrl }
            )

            assertNotNull(item)
            assertEquals(playbackUrl, item.localConfiguration?.uri.toString())
            assertEquals(favorite.audioUrl, item.mediaId)
            assertEquals(favorite.copy(favoritedAtMs = 0L), item.toBaGuideBgmFavoriteItem())
        }
    }

    @Test
    fun `cached artwork uri does not replace original artwork urls`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val favorite = track(
            id = "https://example.com/theme.mp3",
            title = "Theme",
            student = "Hoshino",
            image = "https://example.com/portrait.png?token=portrait%2Fsignature&size=full"
        ).copy(imageUrl = "https://example.com/lobby.png?token=scene%2Bsignature&size=full")
        val cachedArtworkUrl = "file:///data/user/0/os.kei/cache/artwork.png"
        val item = favorite.toBaGuideBgmMediaItem(
            context = context,
            playbackUrlResolver = { _, track -> track.audioUrl },
            artworkUrlResolver = { cachedArtworkUrl }
        )

        assertNotNull(item)
        assertEquals(cachedArtworkUrl, item.mediaMetadata.artworkUri.toString())
        val restored = item.toBaGuideBgmFavoriteItem()

        assertEquals(favorite.copy(favoritedAtMs = 0L), restored)
        assertNotNull(restored)
        assertEquals(favorite.studentImageUrl, restored.resolvePlaybackArtworkImageUrl())
    }

    @Test
    fun `round trip preserves missing artwork fields and the selected artwork source`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val favorite = track(
            id = "https://example.com/theme.mp3",
            title = "Theme",
            student = "Hoshino",
            image = "https://example.com/portrait.png?token=portrait%2Fsignature"
        ).copy(imageUrl = "https://example.com/lobby.png?token=scene%2Bsignature")
        val favorites = listOf(
            favorite.copy(studentImageUrl = ""),
            favorite.copy(imageUrl = "")
        )

        favorites.forEach { original ->
            val item = original.toBaGuideBgmMediaItem(
                context = context,
                playbackUrlResolver = { _, track -> track.audioUrl }
            )

            assertNotNull(item)
            val restored = item.toBaGuideBgmFavoriteItem()

            assertEquals(original.copy(favoritedAtMs = 0L), restored)
            assertNotNull(restored)
            assertEquals(
                original.resolvePlaybackArtworkImageUrl(),
                restored.resolvePlaybackArtworkImageUrl()
            )
        }
    }

    @Test
    fun `artwork resolver produces cached png payload`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val artworkUrl = "https://example.com/artwork-${System.nanoTime()}.png"
        var loadCount = 0
        val loader: (Context, String) -> Bitmap? = { _, url ->
            loadCount += 1
            assertEquals(artworkUrl, url)
            Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.RED)
            }
        }

        val first = BaGuideBgmMediaArtworkPayloadResolver.resolveUrl(
            context = context,
            artworkUrl = artworkUrl,
            bitmapLoader = loader
        )
        val second = BaGuideBgmMediaArtworkPayloadResolver.resolveUrl(
            context = context,
            artworkUrl = artworkUrl,
            bitmapLoader = loader
        )

        assertNotNull(first)
        assertContentEquals(first, second)
        assertEquals(1, loadCount)
    }

    @Test
    fun `artwork hydration targets start from selected track and stay bounded`() {
        val queue = listOf(
            track("https://example.com/1.mp3", "One", "One", ""),
            track("https://example.com/2.mp3", "Two", "Two", ""),
            track("https://example.com/3.mp3", "Three", "Three", ""),
            track("https://example.com/4.mp3", "Four", "Four", "")
        )

        val targets = queue.orderedArtworkHydrationTargets(
            selectedAudioUrl = "https://example.com/3.mp3",
            maxCount = 3
        )

        assertEquals(
            listOf(
                "https://example.com/3.mp3",
                "https://example.com/4.mp3",
                "https://example.com/1.mp3"
            ),
            targets.map { it.audioUrl }
        )
    }

    private fun track(
        id: String,
        title: String,
        student: String,
        image: String
    ): GuideBgmFavoriteItem =
        testBgmFavorite(
            audioUrl = id,
            sourceUrl = "https://www.gamekee.com/ba/tj/1.html",
            title = title,
            studentTitle = student,
            studentImageUrl = image,
        )
}
