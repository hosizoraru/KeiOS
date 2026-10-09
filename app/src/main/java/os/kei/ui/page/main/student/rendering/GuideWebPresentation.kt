package os.kei.ui.page.main.student.rendering

import android.content.Context
import android.graphics.Canvas
import android.webkit.WebView
import android.view.View
import android.widget.FrameLayout
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject

/** The bridge only accepts pixels from the selected main-frame origin; it exposes no app services. */
internal class GuideWebPresentation(
    context: Context,
    mode: GuideViewerRendering,
    private val origin: String,
    onFrame: (Boolean) -> Unit,
    private val onUnavailable: () -> Unit,
) : FrameLayout(context) {
    private val compatible = mode == GuideViewerRendering.CompatibleFrames
    private val binary = compatible && WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) &&
        WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_ARRAY_BUFFER)
    private val frame = if (compatible && binary) GuideRgbaFrameView(context, onFrame) else null
    val web = object : WebView(context) {
        override fun onDraw(canvas: Canvas) { if (!compatible) super.onDraw(canvas) }
    }
    init {
        // Only direct presentation needs to cache WebView's draw functor for multiple Liquid samples.
        if (!compatible) web.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        addView(web, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        frame?.let { pixels ->
            addView(pixels, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            WebViewCompat.addWebMessageListener(web, "KeiViewerFrames", setOf(origin)) { _, message, source, main, reply ->
                if (main && source.toString().trimEnd('/') == origin) {
                    if (message.type == WebMessageCompat.TYPE_ARRAY_BUFFER) {
                        pixels.present(message.arrayBuffer)
                        reply.postMessage("ack")
                    } else runCatching { JSONObject(message.data.orEmpty()) }.getOrNull()?.let {
                        pixels.begin(it.optInt("generation", -1))
                    }
                }
            }
        }
        if (compatible && !binary) post(onUnavailable)
    }
    fun close() {
        frame?.close()
        if (frame != null) WebViewCompat.removeWebMessageListener(web, "KeiViewerFrames")
    }
}
