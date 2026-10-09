package os.kei.ui.page.main.student.rendering

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GuideRgbaFrameViewTest {
    @Test fun `pixel storage is preserved with vertical orientation, and readiness waits for drawing`() {
        val visible = mutableListOf<Boolean>()
        val view = GuideRgbaFrameView(ApplicationProvider.getApplicationContext(), visible::add)
        view.layout(0, 0, 2, 2)
        view.begin(1)
        view.present(packet(1))
        assertEquals(listOf(false), visible)
        val output = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        // Host Skia uses BGRA for N32 on macOS; Android uses RGBA. Compare storage through
        // the same native bitmap API, and verify Android channel order separately on device.
        val source = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
            copyPixelsFromBuffer(ByteBuffer.wrap(packet(1)).apply { position(16) })
        }
        view.draw(Canvas(output))
        for (x in 0..1) for (y in 0..1) assertEquals(source.getPixel(x, 1-y), output.getPixel(x, y))
        assertEquals(Color.WHITE, output.getPixel(1, 0)); assertEquals(Color.GREEN, output.getPixel(1, 1))
        assertEquals(listOf(false, true), visible)
        view.close(); view.present(packet(1)); view.draw(Canvas(output))
        assertEquals(listOf(false, true), visible)
    }
    @Test fun `old model frames and malformed or oversized packets cannot replace a new generation`() {
        assertNull(guideRgbaFrameHeader(packet(1), 2))
        assertNull(guideRgbaFrameHeader(packet(1).dropLast(1).toByteArray(), 1))
        assertNull(guideRgbaFrameHeader(packet(1).apply { this[0] = 0 }, 1))
        for (dimension in listOf(0, -1, 4097, Int.MAX_VALUE)) {
            val bytes = packet(1)
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(8, dimension)
            assertNull(guideRgbaFrameHeader(bytes, 1))
        }
        assertEquals(GuideRgbaFrameHeader(1, 2, 2), guideRgbaFrameHeader(packet(1), 1))
    }
    @Test fun `cropped model pixels keep their position and erase the previous pose using the same background`() {
        val view = GuideRgbaFrameView(ApplicationProvider.getApplicationContext()) {}
        view.layout(0, 0, 4, 4); view.begin(1)
        fun cropped(x: Int, y: Int) = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(0x4B454932).putInt(1).putInt(4).putInt(4).putInt(x).putInt(y)
            .putInt(1).putInt(1).putInt(Color.WHITE).putInt(0).put(byteArrayOf(0,-1,0,-1)).array()
        val output = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        view.present(cropped(1, 0)); view.draw(Canvas(output))
        assertEquals(Color.GREEN, output.getPixel(1, 3)); assertEquals(Color.WHITE, output.getPixel(0, 0))
        view.present(cropped(2, 1)); view.draw(Canvas(output))
        assertEquals(Color.WHITE, output.getPixel(1, 3)); assertEquals(Color.GREEN, output.getPixel(2, 2))
        assertNull(guideRgbaFrameHeader(cropped(4, 0), 1))
        assertNull(guideRgbaFrameHeader(cropped(0, 0).apply { this[35] = 0 }, 1))
        view.close()
    }
    @Test fun `zooming in and out can repeatedly change crop dimensions without stale pixels or recycling the displayed frame`() {
        val view = GuideRgbaFrameView(ApplicationProvider.getApplicationContext()) {}
        view.layout(0, 0, 4, 4); view.begin(1)
        val output = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        for (size in listOf(4, 4, 2, 1, 4, 2, 2)) {
            val packet = ByteBuffer.allocate(40 + size*size*4).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(0x4B454932).putInt(1).putInt(4).putInt(4).putInt(0).putInt(0)
                .putInt(size).putInt(size).putInt(Color.WHITE).putInt(0)
            repeat(size*size) { packet.put(byteArrayOf(0,-1,0,-1)) }
            view.present(packet.array()); view.draw(Canvas(output))
            for (x in 0..3) for (y in 0..3)
                assertEquals(if (x < size && 3-y < size) Color.GREEN else Color.WHITE, output.getPixel(x,y))
        }
        view.close()
    }
    private fun packet(generation: Int): ByteArray = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
        .putInt(0x4B454931).putInt(generation).putInt(2).putInt(2)
        .put(byteArrayOf(-1, 0, 0, -1, 0, -1, 0, -1, 0, 0, -1, -1, -1, -1, -1, -1)).array()
}
