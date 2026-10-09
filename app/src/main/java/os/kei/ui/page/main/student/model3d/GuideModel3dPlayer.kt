@file:Suppress("FunctionName")
package os.kei.ui.page.main.student.model3d

import android.annotation.SuppressLint
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.ConsoleMessage
import android.view.ViewConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import os.kei.ui.page.main.student.section.gallery.GuideWebMemoryLobbyLoading
import os.kei.BuildConfig
import os.kei.core.log.AppLogger
import os.kei.ui.page.main.student.rendering.GuideViewerRendering
import os.kei.ui.page.main.student.rendering.GuideWebPresentation
import java.io.ByteArrayInputStream
import kotlin.coroutines.resume

private const val MODEL_ORIGIN = "https://appassets.androidplatform.net"
private const val MODEL_PAGE = "$MODEL_ORIGIN/ba3d/index.html"

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun GuideModel3dPlayer(
    resource: BaModel3dResource,
    model: BaModel3dAsset,
    playing: Boolean,
    actionRequest: Pair<Int, String>,
    resetRequest: Int,
    retry: Int,
    backgroundColor: ComposeColor,
    options: BaModel3dOptions,
    seekRequest: Pair<Int, Float>,
    pollProgress: Boolean,
    rendering: GuideViewerRendering,
    onState: (BaModel3dPlaybackState) -> Unit,
    onError: (Boolean) -> Unit,
    modifier: Modifier,
    onSceneTap: (() -> Unit)? = null,
) {
    val owner = LocalLifecycleOwner.current
    var resumed by remember(owner) { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, _ -> resumed = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val notifyState by rememberUpdatedState(onState)
    val notifyError by rememberUpdatedState(onError)
    val notifyTap by rememberUpdatedState(onSceneTap)
    val shouldPollProgress by rememberUpdatedState(pollProgress && resumed)
    val isResumed by rememberUpdatedState(resumed)
    key(resource.contentId, retry, rendering) {
        var view by remember { mutableStateOf<WebView?>(null) }
        var ready by remember { mutableStateOf(false) }
        var scriptReady by remember { mutableStateOf(false) }
        var failed by remember { mutableStateOf(false) }
        var frameVisible by remember { mutableStateOf(false) }
        val session = remember(model.gitBlob) { BaModel3dCacheSession() }
        val currentSession by rememberUpdatedState(session)
        val currentModel by rememberUpdatedState(model)
        val currentBackground by rememberUpdatedState(backgroundColor)
        DisposableEffect(session) { onDispose { session.close() } }
        LaunchedEffect(view, retry, model) {
            val web = view ?: return@LaunchedEffect
            failed = false
            notifyError(false)
            var waiting = 0
            while (!failed) {
                if (!isResumed) { delay(500); continue }
                val raw = suspendCancellableCoroutine { c ->
                    web.evaluateJavascript("window.keiosModel ? window.keiosModel.state() : null") { if (c.isActive) c.resume(it) }
                }
                val state = runCatching { JSONObject(raw) }.getOrNull()
                if (state != null) {
                    scriptReady = true
                  if (state.optString("url") == "$MODEL_ORIGIN/ba3d/models/${model.gitBlob}.glb") {
                    ready = state.optBoolean("ready") && (rendering == GuideViewerRendering.SystemWebView || frameVisible)
                    if (state.optString("error").isNotBlank()) { failed = true; notifyError(true) }
                    val array = state.optJSONArray("actions")
                    val actions = if (array == null) emptyList() else List(minOf(array.length(), 100)) { array.optString(it) }
                    val durations = state.optJSONArray("durations")
                    notifyState(BaModel3dPlaybackState(actions, List(actions.size) { durations?.optDouble(it, 0.0)?.toFloat() ?: 0f },
                        state.optString("selected"), ready, state.optDouble("time", 0.0).toFloat(),
                        state.optDouble("duration", 0.0).toFloat(), state.optBoolean("ended")))
                  }
                }
                waiting = if (ready) 0 else waiting + 1
                if (waiting >= 300) { failed = true; notifyError(true) }
                delay(if (ready && !shouldPollProgress) 1_000 else 200)
            }
        }
        LaunchedEffect(view, scriptReady, model) {
            if (!scriptReady) return@LaunchedEffect
            ready = false; notifyState(BaModel3dPlaybackState())
            view?.evaluateJavascript("window.keiosModel.load(${JSONObject().put("url", "$MODEL_ORIGIN/ba3d/models/${model.gitBlob}.glb")
                .put("defaultAnimation", resource.defaultAnimation.takeIf { model.file == resource.defaultFile }.orEmpty())})", null)
        }
        LaunchedEffect(view, scriptReady, backgroundColor) {
            view?.setBackgroundColor(backgroundColor.toArgb())
            if (scriptReady) view?.evaluateJavascript(
                "window.keiosModel.setBackground(${JSONObject.quote(model3dBackgroundHex(backgroundColor))})", null)
        }
        LaunchedEffect(view, scriptReady, resumed, playing) {
            view?.let { web ->
                if (resumed) web.onResume() else web.onPause()
                if (scriptReady) web.evaluateJavascript("window.keiosModel.setPlaying($playing);window.keiosModel.setForeground($resumed)", null)
            }
        }
        LaunchedEffect(view, scriptReady, actionRequest) {
            if (scriptReady && actionRequest.first > 0) view?.evaluateJavascript(
                "window.keiosModel.selectAnimation(${JSONObject.quote(actionRequest.second)})", null,
            )
        }
        LaunchedEffect(view, scriptReady, options) {
            if (scriptReady) view?.evaluateJavascript("window.keiosModel.setOptions(${JSONObject()
                .put("speed", options.speed).put("loop", options.loop).put("outline", options.outline)
                .put("outlineWidth", options.outlineWidth).put("scrubbing", options.scrubbing)})", null)
        }
        LaunchedEffect(view, scriptReady, seekRequest) {
            if (scriptReady && seekRequest.first > 0) view?.evaluateJavascript("window.keiosModel.seek(${seekRequest.second})", null)
        }
        LaunchedEffect(view, scriptReady, resetRequest) {
            if (scriptReady && resetRequest > 0) view?.evaluateJavascript("window.keiosModel.resetCamera()", null)
        }
        Box(if (ready && !failed) modifier.testTag(GuideModel3dReadyTag) else modifier) {
            AndroidView(modifier = modifier, factory = { context ->
                GuideWebPresentation(context, rendering, MODEL_ORIGIN,
                    onFrame = { frameVisible = it }, onUnavailable = { failed = true; notifyError(true) }).apply {
                  web.apply {
                    val tap = GuideModel3dTapObserver(ViewConfiguration.get(context).scaledTouchSlop.toFloat())
                    setOnTouchListener { _, event ->
                        if (tap.onTouch(event)) notifyTap?.invoke()
                        false // OrbitControls receives the complete native stream, including pinch and cancel.
                    }
                    if (BuildConfig.DEBUG || BuildConfig.APPLICATION_ID.endsWith(".diag")) WebView.setWebContentsDebuggingEnabled(true)
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                            if (message.messageLevel() in listOf(ConsoleMessage.MessageLevel.ERROR, ConsoleMessage.MessageLevel.WARNING)) {
                                AppLogger.w("BaModel3d", message.message().take(2_000))
                            }
                            return true
                        }
                    }
                    setBackgroundColor(currentBackground.toArgb())
                    settings.apply {
                        javaScriptEnabled = true; domStorageEnabled = false
                        allowFileAccess = false; allowContentAccess = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        setGeolocationEnabled(false); mediaPlaybackRequiresUserGesture = true
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = request.url.toString().substringBefore('#') != MODEL_PAGE
                        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                            val path = request.url.path.orEmpty()
                            if (request.url.scheme != "https" || request.url.host != "appassets.androidplatform.net" || request.method != "GET") return unavailable()
                            return try {
                                if (path.startsWith("/ba3d/models/")) {
                                    val asset = currentModel.takeIf { path == "/ba3d/models/${it.gitBlob}.glb" } ?: return unavailable()
                                    WebResourceResponse("model/gltf-binary", null, BaModel3dCache.get(context).open(asset, currentSession))
                                } else {
                                    if (!path.startsWith("/ba3d/") || ".." in path || '%' in path || '\\' in path) return unavailable()
                                    val mime = when {
                                        path.endsWith(".html") -> "text/html"
                                        path.endsWith(".js") -> "application/javascript"
                                        else -> return unavailable()
                                    }
                                    WebResourceResponse(mime, "UTF-8", context.assets.open(path.removePrefix("/")))
                                }
                            } catch (_: Exception) { unavailable() }
                        }
                        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                            failed = true; notifyError(true); return true
                        }
                    }
                    view = this
                    // The renderer starts with the right color, before the first JS state poll can arrive.
                    loadUrl("$MODEL_PAGE#background=${model3dBackgroundHex(currentBackground).removePrefix("#")}")
                  }
                }
            }, onRelease = { presentation ->
                presentation.web.evaluateJavascript("window.keiosModel?.dispose()", null)
                presentation.close()
                val web = presentation.web
                currentSession.close(); web.stopLoading(); web.onPause(); web.webViewClient = WebViewClient(); web.removeAllViews(); web.destroy()
            })
            if (!failed) GuideWebMemoryLobbyLoading(visible = !ready, textColor = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onBackground)
        }
    }
}

private fun unavailable() = WebResourceResponse("text/plain", "UTF-8", 503, "Model unavailable",
    mapOf("Cache-Control" to "no-store"), ByteArrayInputStream(byteArrayOf()))

internal const val GuideModel3dReadyTag = "guide_model3d_ready"
