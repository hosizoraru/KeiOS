@file:Suppress("FunctionName")

package os.kei.ui.page.main.student.section.gallery

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import os.kei.R
import os.kei.ui.page.main.student.gameKeeMemoryLobbyViewerUrl
import os.kei.ui.page.main.student.BaGuideWebMemoryLobby
import os.kei.ui.page.main.student.BaGuideSpineWebCache
import os.kei.ui.page.main.student.BaGuideSpineWebCacheSession
import os.kei.ui.page.main.widget.motion.AppMotionTokens
import os.kei.ui.page.main.widget.motion.appMotionFloatState
import os.kei.BuildConfig
import os.kei.ui.page.main.student.rendering.GuideViewerRendering
import os.kei.ui.page.main.student.rendering.GuideWebPresentation
import kotlin.coroutines.resume

/** Reuses the Wiki renderer through a narrow playback controller. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun GuideWebMemoryLobbyPlayer(
    resource: BaGuideWebMemoryLobby,
    playing: Boolean,
    retryToken: Int,
    stateRequest: Int,
    actionRequest: Int,
    selectedAction: String,
    camera: GuideWebMemoryLobbyCamera,
    onActionsAvailable: (List<String>, String) -> Unit,
    rendering: GuideViewerRendering = GuideViewerRendering.SystemWebView,
    modifier: Modifier = Modifier,
) {
    val viewerUrl = resource.viewerUrl
    val owner = LocalLifecycleOwner.current
    var resumed by remember(owner) { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val notifyActions by rememberUpdatedState(onActionsAvailable)
    key(resource, retryToken, rendering) {
        var view by remember { mutableStateOf<WebView?>(null) }
        var ready by remember { mutableStateOf(false) }
        var failed by remember { mutableStateOf(false) }
        var frameVisible by remember { mutableStateOf(false) }
        var frameScript by remember { mutableStateOf("") }
        val playerAlpha = appMotionFloatState(
            targetValue = if (ready) 1f else 0f,
            durationMillis = AppMotionTokens.floatingFadeInMs,
            label = "lobbyPlayerReady",
        )
        var cacheSession by remember { mutableStateOf<BaGuideSpineWebCacheSession?>(null) }
        LaunchedEffect(view, failed) {
            notifyActions(emptyList(), "")
            val web = view ?: return@LaunchedEffect
            if (failed) return@LaunchedEffect
            repeat(90) {
                val state = suspendCancellableCoroutine { continuation ->
                    web.evaluateJavascript(frameScript + "\n" + GameKeeLobbyFocusScript) { result ->
                        if (continuation.isActive) continuation.resume(result.orEmpty())
                    }
                }
                if (state.startsWith("\"ready")) {
                    ready = rendering == GuideViewerRendering.SystemWebView || frameVisible
                    if (!ready) { delay(100); return@repeat }
                    return@LaunchedEffect
                }
                delay(1_000)
            }
            failed = true
        }
        // Refresh on opening the picker and returning to the foreground: the Wiki can transition
        // from its introduction to Idle without a native action request.
        LaunchedEffect(view, ready, resumed, stateRequest) {
            if (ready && resumed) view?.evaluateJavascript("window.keiosLobby.state()") { result ->
                val actions = runCatching {
                    val state = JSONObject(result.orEmpty())
                    val array = state.getJSONArray("actions")
                    val names = (0 until minOf(array.length(), 100)).map { array.optString(it).trim() }
                        .filter(String::isNotBlank).distinct()
                    names to state.optString("action")
                }.getOrDefault(emptyList<String>() to "")
                notifyActions(actions.first, actions.second)
            }
        }
        LaunchedEffect(view, ready, resumed, playing) {
            view?.let { web ->
                if (resumed) web.onResume() else web.onPause()
                if (ready) web.evaluateJavascript("window.keiosLobby.setPlaying(${playing && resumed})", null)
                web.evaluateJavascript("window.keiosLobby?.setForeground($resumed)", null)
            }
        }
        LaunchedEffect(view, ready, resumed, camera) {
            if (!ready || !resumed) return@LaunchedEffect
            camera.transform.collect { transform ->
                // Await one evaluation so rapid input conflates to the newest absolute view.
                // No animation selection, lifecycle change or WebView recreation is involved.
                suspendCancellableCoroutine { continuation ->
                    view?.evaluateJavascript(gameKeeLobbyCameraScript(transform)) {
                        if (continuation.isActive) continuation.resume(Unit)
                    } ?: continuation.resume(Unit)
                }
            }
        }
        // The page controller only exposes playback; no native app services are exposed to JS.
        LaunchedEffect(actionRequest) {
            if (actionRequest > 0 && selectedAction.isNotBlank()) {
                view?.evaluateJavascript(gameKeeLobbySelectActionScript(selectedAction), null)
            }
        }
        Box(modifier, contentAlignment = Alignment.Center) {
            if (!failed) {
                AndroidView(
                    // Compatibility readiness is acknowledged by Canvas's first actual draw.
                    // An alpha-zero parent would prevent that draw and deadlock the loading gate.
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        alpha = if (rendering == GuideViewerRendering.CompatibleFrames) 1f else playerAlpha.value
                    },
                    factory = { context ->
                        GuideWebPresentation(context, rendering, "https://www.gamekee.com",
                            onFrame = { frameVisible = it }, onUnavailable = { failed = true }).apply {
                          web.apply {
                            if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
                            if (rendering == GuideViewerRendering.CompatibleFrames) {
                                frameScript = context.assets.open("ba3d/frame-presentation.js").bufferedReader().use { it.readText() }
                            }
                            setBackgroundColor(Color.TRANSPARENT)
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                allowFileAccess = false
                                allowContentAccess = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                setGeolocationEnabled(false)
                                mediaPlaybackRequiresUserGesture = true
                                // The desktop SPA owns BA's interactive background player.
                                userAgentString = userAgentString
                                    .replaceFirst(Regex("\\([^)]*\\)"), "(X11; Linux x86_64)")
                                    .replace(" Mobile", "").replace("Version/4.0 ", "")
                            }
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                            val session = BaGuideSpineWebCacheSession(
                                resource, { BaGuideSpineWebCache.get(context.applicationContext) },
                                settings.userAgentString, revalidate = retryToken > 0,
                            )
                            cacheSession = session
                            webViewClient = object : WebViewClient() {
                                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                                    session.intercept(request)

                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                                    gameKeeMemoryLobbyViewerUrl(request.url.toString()) != viewerUrl

                                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                                    if (request.isForMainFrame) failed = true
                                }

                                override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                                    if (request.isForMainFrame) failed = true
                                }

                                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                                    failed = true
                                    return true
                                }
                            }
                            view = this
                            loadUrl(viewerUrl)
                          }
                        }
                    },
                    onRelease = { presentation ->
                        presentation.web.evaluateJavascript("window.keiosLobbyLoop?.dispose();window.keiosLobbyFrames?.dispose()", null)
                        presentation.close()
                        val web = presentation.web
                        web.stopLoading()
                        cacheSession?.close()
                        web.onPause()
                        web.webViewClient = WebViewClient()
                        web.removeAllViews()
                        web.destroy()
                        notifyActions(emptyList(), "")
                    },
                )
            }
            if (failed) {
                GuideWebMemoryLobbyStatus(
                    stringResource(R.string.guide_gallery_dynamic_lobby_failed),
                )
            } else {
                GuideWebMemoryLobbyLoading(visible = !ready)
            }
        }
    }
}
