@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package os.kei.ui.page.main.student

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.core.net.toUri
import androidx.media3.session.MediaSession
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.page.main.student.catalog.component.BaGuideBgmMediaArtworkPayloadResolver
import os.kei.ui.page.main.student.media.BgmServiceTestPlayer
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BaGuideBgmMediaArtworkTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()

    @After fun clear() = BaGuideBgmMediaArtworkPayloadResolver.clearMemoryCache()

    @Test fun `session artwork HTTP requests carry GameKee headers and decode the correct cover`() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setHeader("Content-Type", "image/png").setBody(Buffer().write(png(Color.RED))))
            server.enqueue(MockResponse().setHeader("Content-Type", "image/png").setBody(Buffer().write(png(Color.BLUE))))
            val loader = createGameKeeMediaBitmapLoader(context)
            val first = loader.loadBitmap(server.url("/A.png").toString().toUri()).get(10, TimeUnit.SECONDS)
            val second = loader.loadBitmap(server.url("/B.png").toString().toUri()).get(10, TimeUnit.SECONDS)
            assertEquals(Color.RED, first.getPixel(first.width / 2, first.height / 2))
            assertEquals(Color.BLUE, second.getPixel(second.width / 2, second.height / 2))
            assertTrue(first.width > 256, "Session artwork must not reuse the small catalog icon")
            assertTrue(first.width <= MediaSession.getBitmapDimensionLimit(context) * 2 - 1)
            repeat(2) {
                val request = assertNotNull(server.takeRequest(2, TimeUnit.SECONDS))
                assertEquals("https://www.gamekee.com/", request.getHeader("Referer"))
                assertEquals("ba", request.getHeader("game-alias"))
                assertTrue(request.getHeader("User-Agent").orEmpty().contains("Firefox"))
            }
        }
    }

    @Test fun `payload hydration retains system sized artwork instead of a 256 pixel catalog thumbnail`() = runBlocking {
        val file = File.createTempFile("media-cover-", ".png", context.cacheDir)
        try {
            file.writeBytes(png(Color.GREEN))
            val data = assertNotNull(BaGuideBgmMediaArtworkPayloadResolver.resolveUrl(context, file.toURI().toString()))
            val bitmap = assertNotNull(BitmapFactory.decodeByteArray(data, 0, data.size))
            assertEquals(MediaSession.getBitmapDimensionLimit(context), bitmap.width)
            assertEquals(Color.GREEN, bitmap.getPixel(bitmap.width / 2, bitmap.height / 2))
        } finally {
            file.delete()
        }
    }

    @Test fun `non power of two artwork has identical resolution and pixels in URI and hydration paths`() = runBlocking {
        val limit = MediaSession.getBitmapDimensionLimit(context)
        val file = File.createTempFile("media-cover-precision-", ".png", context.cacheDir)
        val player = BgmServiceTestPlayer()
        val session = MediaSession.Builder(context, player)
            .setBitmapLoader(createGameKeeMediaBitmapLoader(context)).build()
        try {
            val edge = limit * 3 / 2
            val pixels = IntArray(edge * edge) { index ->
                val x = index % edge; val y = index / edge
                Color.rgb(x * 255 / edge, y * 255 / edge, if ((x + y) % 17 < 8) 255 else 0)
            }
            val source = Bitmap.createBitmap(pixels, edge, edge, Bitmap.Config.ARGB_8888)
            file.outputStream().use { source.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = file.toURI().toString()
            val fromUri = session.bitmapLoader.loadBitmap(uri.toUri()).get(10, TimeUnit.SECONDS)
            val bytes = assertNotNull(BaGuideBgmMediaArtworkPayloadResolver.resolveUrl(context, uri))
            val hydrated = assertNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            assertEquals(limit, fromUri.width)
            assertEquals(limit, hydrated.width)
            assertEquals(limit, hydrated.height)
            val first = IntArray(limit * limit)
            val second = IntArray(limit * limit)
            fromUri.getPixels(first, 0, limit, 0, 0, limit, limit)
            hydrated.getPixels(second, 0, limit, 0, 0, limit, limit)
            assertContentEquals(first, second)
        } finally {
            session.release()
            player.release()
            file.delete()
        }
    }

    private fun png(color: Int): ByteArray = ByteArrayOutputStream().use { output ->
        val bitmap = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        bitmap.recycle()
        output.toByteArray()
    }
}
