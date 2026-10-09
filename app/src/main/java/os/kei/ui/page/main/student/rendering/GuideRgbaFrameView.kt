package os.kei.ui.page.main.student.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal data class GuideRgbaFrameHeader(
    val generation: Int, val width: Int, val height: Int,
    val x: Int = 0, val y: Int = 0, val frameWidth: Int = width, val frameHeight: Int = height,
    val background: Int? = null, val offset: Int = 16,
)

/** Keep the original drawing-buffer resolution, bounded to 4096 pixels and 64 MiB per frame. */
internal fun guideRgbaFrameHeader(bytes: ByteArray, generation: Int): GuideRgbaFrameHeader? {
    if (bytes.size < 16 || generation < 0) return null
    val packet = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    val magic = packet.int
    if ((magic != 0x4B454931 && magic != 0x4B454932) || packet.int != generation) return null
    val fw = packet.int; val fh = packet.int
    if (fw !in 1..4096 || fh !in 1..4096) return null
    val header = if (magic == 0x4B454932) {
        if (bytes.size < 40) return null
        val x = packet.int; val y = packet.int; val w = packet.int; val h = packet.int; val background = packet.int
        if (packet.int != 0 || background ushr 24 != 255 || x < 0 || y < 0 || w <= 0 || h <= 0 ||
            x.toLong() + w > fw || y.toLong() + h > fh) return null
        GuideRgbaFrameHeader(generation, w, h, x, y, fw, fh, background, 40)
    } else GuideRgbaFrameHeader(generation, fw, fh)
    if (bytes.size.toLong() != header.offset.toLong() + header.width.toLong() * header.height * 4) return null
    return header
}

/** Canvas presents the WebGL framebuffer without WebView's texture-import draw functor.
 * Two reusable bitmaps and the JS acknowledgement keep storage/queued work bounded. */
internal class GuideRgbaFrameView(context: Context, private val onFrame: (Boolean) -> Unit) : View(context) {
    private val buffers = arrayOfNulls<Bitmap>(2)
    private var current: Bitmap? = null
    private var currentHeader: GuideRgbaFrameHeader? = null
    private var next = 0
    private var generation = -1
    private var closed = false
    private var announced = false
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val destination = RectF()
    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    fun begin(value: Int) {
        if (closed || value < 0 || value < generation) return
        generation = value; current = null; currentHeader = null; announced = false; onFrame(false); invalidate()
    }
    fun present(bytes: ByteArray) {
        if (closed) return
        val header = guideRgbaFrameHeader(bytes, generation) ?: return
        var buffer = buffers[next]
        if (buffer == null || buffer.allocationByteCount < header.width * header.height * 4) {
            buffer?.recycle()
            buffer = Bitmap.createBitmap(header.width, header.height, Bitmap.Config.ARGB_8888)
            buffers[next] = buffer
        } else if (buffer.width != header.width || buffer.height != header.height) {
            // A smaller crop reuses its allocation. Never reconfigure the currently displayed buffer.
            buffer.reconfigure(header.width, header.height, Bitmap.Config.ARGB_8888)
        }
        currentHeader = header
        current = buffer.apply {
            copyPixelsFromBuffer(ByteBuffer.wrap(bytes).apply { position(header.offset) })
        }
        next = 1 - next; invalidate()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        current?.let { bitmap ->
            val header = currentHeader ?: return
            header.background?.let(canvas::drawColor)
            val save = canvas.save()
            canvas.translate(0f, height.toFloat()); canvas.scale(1f, -1f)
            val sx = width.toFloat() / header.frameWidth; val sy = height.toFloat() / header.frameHeight
            destination.set(header.x * sx, header.y * sy, (header.x + header.width) * sx, (header.y + header.height) * sy)
            canvas.drawBitmap(bitmap, null, destination, paint); canvas.restoreToCount(save)
            if (!announced) { announced = true; onFrame(true) }
        }
    }
    fun close() {
        if (closed) return
        closed = true; current = null; currentHeader = null; buffers.forEach { it?.recycle() }; buffers.fill(null)
    }
}
