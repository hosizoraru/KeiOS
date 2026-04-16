package com.example.keios.feature.ba.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.keios.BuildConfig
import com.example.keios.KeiOSApp
import okhttp3.Cache
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object GameKeeFetchHelper {
    private const val TAG = "GameKeeFetch"
    private const val ENABLE_LOG = BuildConfig.DEBUG
    private const val BASE_WWW = "https://www.gamekee.com"
    private const val ACCEPT_JSON = "application/json, text/plain, */*"
    private const val ACCEPT_HTML = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
    private const val ACCEPT_IMAGE = "image/*,*/*"
    private const val ACCEPT_LANGUAGE = "zh-CN"
    private const val DEFAULT_MAX_DECODE_EDGE = 2560
    private const val HTTP_CACHE_DIR = "ba_gamekee_http_cache"
    private const val HTTP_CACHE_SIZE_BYTES = 64L * 1024L * 1024L
    private const val REFERER_HOME_PATH = "/"
    private const val REFERER_BA_PATH = "/ba"
    private const val REFERER_ACTIVITY_PATH = "/ba/huodong/15"
    private const val REFERER_POOL_PATH = "/ba/kachi/15"
    private const val FIREFOX_ANDROID_UA =
        "Mozilla/5.0 (Android 15; Mobile; rv:140.0) Gecko/140.0 Firefox/140.0"
    private val REQUEST_UAS = listOf(FIREFOX_ANDROID_UA)

    private class InMemoryCookieJar : CookieJar {
        private val store = ConcurrentHashMap<String, MutableList<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            if (cookies.isEmpty()) return
            val key = cookieKey(url)
            val current = store[key] ?: mutableListOf()
            val merged = current
                .filter { existing -> cookies.none { it.name == existing.name && it.path == existing.path } }
                .toMutableList()
            merged.addAll(cookies)
            store[key] = merged
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val now = System.currentTimeMillis()
            val key = cookieKey(url)
            val existing = (store[key] ?: emptyList()).filter { it.expiresAt > now }
            if (existing.isEmpty()) return emptyList()
            store[key] = existing.toMutableList()
            return existing
        }

        private fun cookieKey(url: HttpUrl): String {
            val host = url.host.removePrefix("www.")
            return host.lowercase()
        }
    }

    private fun logD(msg: String) {
        if (ENABLE_LOG) Log.d(TAG, msg)
    }

    private fun String.compactForLog(limit: Int = 160): String {
        val compact = replace('\n', ' ')
            .replace('\r', ' ')
            .replace('\t', ' ')
            .trim()
        if (compact.length <= limit) return compact
        return compact.take(limit) + "…"
    }

    private fun shortUa(ua: String): String {
        return when (ua) {
            FIREFOX_ANDROID_UA -> "firefox-android"
            else -> "custom"
        }
    }

    private fun Throwable.toCompactLogMessage(): String {
        val root = generateSequence(this) { it.cause }.last()
        val head = "${javaClass.simpleName}:${message.orEmpty()}".compactForLog(120)
        val tail = if (root !== this) {
            " root=${root.javaClass.simpleName}:${root.message.orEmpty()}".compactForLog(90)
        } else {
            ""
        }
        return (head + tail).compactForLog(220)
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(12, TimeUnit.SECONDS)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .fastFallback(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .cookieJar(InMemoryCookieJar())
        .cache(resolveHttpCache())
        .build()

    private fun resolveHttpCache(): Cache? {
        return runCatching {
            val cacheRoot = File(KeiOSApp.appContext.cacheDir, HTTP_CACHE_DIR).apply { mkdirs() }
            Cache(cacheRoot, HTTP_CACHE_SIZE_BYTES)
        }.getOrNull()
    }

    private fun normalizeUrl(base: String, pathOrUrl: String): String {
        val raw = pathOrUrl.trim()
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
        return if (raw.startsWith("/")) "$base$raw" else "$base/$raw"
    }

    private fun extractPathHint(pathOrUrl: String): String {
        val raw = pathOrUrl.trim()
        if (raw.isBlank()) return ""
        val fromUrl = raw.toHttpUrlOrNull()
            ?: raw.takeIf { it.startsWith("//") }?.let { "https:$it".toHttpUrlOrNull() }
        if (fromUrl != null) {
            val query = fromUrl.query
            return buildString {
                append(fromUrl.encodedPath)
                if (!query.isNullOrBlank()) append("?").append(query)
            }
        }
        return if (raw.startsWith("/")) raw else "/$raw"
    }

    private fun extractHostHint(pathOrUrl: String): String {
        val raw = pathOrUrl.trim()
        if (raw.isBlank()) return ""
        val fromUrl = raw.toHttpUrlOrNull()
            ?: raw.takeIf { it.startsWith("//") }?.let { "https:$it".toHttpUrlOrNull() }
        return fromUrl?.host.orEmpty().lowercase()
    }

    private fun extractContentDetailId(pathHint: String): String? {
        val normalized = pathHint.trim()
        if (normalized.isBlank()) return null
        val fromApi = Regex("/v1/content/detail/(\\d+)").find(normalized)?.groupValues?.getOrNull(1)
        if (!fromApi.isNullOrBlank()) return fromApi
        val fromGuidePage = Regex("/ba/tj/(\\d+)\\.html").find(normalized)?.groupValues?.getOrNull(1)
        if (!fromGuidePage.isNullOrBlank()) return fromGuidePage
        return null
    }

    private fun resolveReferer(pathOrUrl: String, refererPath: String): String {
        val refererHint = extractPathHint(refererPath).lowercase()
        val requestHint = extractPathHint(pathOrUrl).lowercase()
        val mergedHint = "$refererHint $requestHint"
        val requestHost = extractHostHint(pathOrUrl)
        val refererHost = extractHostHint(refererPath)
        val effectiveHost = when {
            requestHost.isNotBlank() -> requestHost
            refererHost.isNotBlank() -> refererHost
            else -> "www.gamekee.com"
        }

        // cdn / 非 www 子域名请求统一挂主页 referer，避免跨域资源请求被拒。
        if (effectiveHost.endsWith("gamekee.com") && effectiveHost != "www.gamekee.com") {
            return normalizeUrl(BASE_WWW, REFERER_HOME_PATH)
        }

        val detailId = extractContentDetailId(requestHint) ?: extractContentDetailId(refererHint)
        if (!detailId.isNullOrBlank()) {
            return normalizeUrl(BASE_WWW, "/ba/tj/$detailId.html")
        }

        val selectedPath = when {
            mergedHint.contains("/ba/huodong") -> REFERER_ACTIVITY_PATH
            mergedHint.contains("/ba/kachi") -> REFERER_POOL_PATH
            else -> REFERER_BA_PATH
        }
        return normalizeUrl(BASE_WWW, selectedPath)
    }

    private fun isJsonLike(body: String): Boolean {
        val trimmed = body.trimStart()
        return trimmed.startsWith("{") || trimmed.startsWith("[")
    }

    private fun executeText(
        url: String,
        referer: String,
        ua: String,
        acceptHeader: String,
        extraHeaders: Map<String, String>,
        requireJsonBody: Boolean
    ): String {
        val builder = Request.Builder()
            .url(url)
            .get()
            .header("Accept", acceptHeader)
            .header("Accept-Language", ACCEPT_LANGUAGE)
            .header("Referer", referer)
            .header("User-Agent", ua)
        extraHeaders.forEach { (k, v) -> builder.header(k, v) }

        client.newCall(builder.build()).execute().use { resp ->
            if (!resp.isSuccessful) {
                val contentType = resp.header("Content-Type").orEmpty()
                val bodyPreview = runCatching { resp.peekBody(256).string() }
                    .getOrDefault("")
                    .compactForLog(120)
                throw IOException(
                    "http=${resp.code} ct=${contentType.ifBlank { "-" }} body=${bodyPreview.ifBlank { "-" }}"
                )
            }
            val body = resp.body.string()
            if (requireJsonBody && !isJsonLike(body)) {
                throw IOException("non-json body=${body.compactForLog(120)}")
            }
            return body
        }
    }

    private fun fetchText(
        pathOrUrl: String,
        refererPath: String,
        acceptHeader: String,
        extraHeaders: Map<String, String>,
        requireJsonBody: Boolean
    ): String {
        val traceId = "txt-${System.nanoTime().toString(16)}"
        val url = normalizeUrl(BASE_WWW, pathOrUrl)
        val referer = resolveReferer(pathOrUrl = pathOrUrl, refererPath = refererPath)
        val uas = REQUEST_UAS
        var lastError: Throwable? = null
        val errors = mutableListOf<String>()
        val totalAttempts = uas.size
        logD(
            "text[$traceId] start path=${pathOrUrl.compactForLog(140)} url=${url.compactForLog(120)} " +
                "referer=${referer.compactForLog(80)} json=$requireJsonBody accept=${acceptHeader.compactForLog(36)} " +
                "headers=${extraHeaders.keys.joinToString(",").ifBlank { "-" }}"
        )
        uas.forEachIndexed { index, ua ->
            val attempt = index + 1
            val startMs = System.currentTimeMillis()
            val result = runCatching {
                executeText(
                    url = url,
                    referer = referer,
                    ua = ua,
                    acceptHeader = acceptHeader,
                    extraHeaders = extraHeaders,
                    requireJsonBody = requireJsonBody
                )
            }
            val elapsedMs = System.currentTimeMillis() - startMs
            if (result.isSuccess) {
                logD(
                    "text[$traceId] ok a$attempt/$totalAttempts ${shortUa(ua)} " +
                        "${elapsedMs}ms url=${url.compactForLog(120)} referer=${referer.compactForLog(80)}"
                )
                return result.getOrThrow()
            }
            lastError = result.exceptionOrNull()
            val err = lastError?.toCompactLogMessage().orEmpty()
            errors += "a$attempt:${shortUa(ua)}:${err.compactForLog(140)}"
            logD(
                "text[$traceId] fail a$attempt/$totalAttempts ${shortUa(ua)} " +
                    "${elapsedMs}ms url=${url.compactForLog(120)} referer=${referer.compactForLog(80)} err=$err"
            )
        }

        if (errors.isNotEmpty()) {
            logD("text[$traceId] fail-all summary=${errors.joinToString(" | ").compactForLog(720)}")
        }
        throw (lastError ?: IOException("gamekee fetch failed"))
    }

    fun fetchJson(
        pathOrUrl: String,
        refererPath: String,
        extraHeaders: Map<String, String> = emptyMap()
    ): String {
        return fetchText(
            pathOrUrl = pathOrUrl,
            refererPath = refererPath,
            acceptHeader = ACCEPT_JSON,
            extraHeaders = extraHeaders,
            requireJsonBody = true
        )
    }

    fun fetchHtml(
        pathOrUrl: String,
        refererPath: String,
        extraHeaders: Map<String, String> = emptyMap()
    ): String {
        return fetchText(
            pathOrUrl = pathOrUrl,
            refererPath = refererPath,
            acceptHeader = ACCEPT_HTML,
            extraHeaders = extraHeaders,
            requireJsonBody = false
        )
    }

    private fun decodeBitmap(
        response: Response,
        maxDecodeDimension: Int = DEFAULT_MAX_DECODE_EDGE,
        onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)? = null
    ): Bitmap? {
        if (!response.isSuccessful) return null
        val body = response.body
        val total = body.contentLength()
        body.byteStream().use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var downloaded = 0L
            while (true) {
                val count = input.read(buffer)
                if (count <= 0) break
                output.write(buffer, 0, count)
                downloaded += count
                onProgress?.invoke(downloaded, total)
            }
            val bytes = output.toByteArray()
            if (bytes.isEmpty()) return null
            val safeMax = maxDecodeDimension.coerceAtLeast(512)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val srcWidth = bounds.outWidth
            val srcHeight = bounds.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) {
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            var sample = 1
            while ((srcWidth / sample) > safeMax || (srcHeight / sample) > safeMax) {
                sample *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sample.coerceAtLeast(1)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
        }
    }

    private fun fetchImageInternal(
        imageUrl: String,
        maxDecodeDimension: Int = DEFAULT_MAX_DECODE_EDGE,
        onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)? = null
    ): Bitmap? {
        val normalized = imageUrl.trim()
        if (normalized.isBlank()) return null
        val traceId = "img-${System.nanoTime().toString(16)}"

        val requestUrls = buildList {
            add(normalized)
            if (normalized.startsWith("//")) add("https:$normalized")
        }.distinct()

        var lastError: Throwable? = null
        val uas = REQUEST_UAS
        val totalAttempts = requestUrls.size * uas.size
        var attempt = 0
        val errors = mutableListOf<String>()
        logD(
            "image[$traceId] start url=${normalized.compactForLog(140)} decodeMax=$maxDecodeDimension attempts=$totalAttempts"
        )

        requestUrls.forEach { url ->
            val referer = resolveReferer(pathOrUrl = url, refererPath = "")
            uas.forEach { ua ->
                attempt += 1
                val startMs = System.currentTimeMillis()
                val req = Request.Builder()
                    .url(url)
                    .get()
                    .header("Accept", ACCEPT_IMAGE)
                    .header("Accept-Language", ACCEPT_LANGUAGE)
                    .header("Referer", referer)
                    .header("User-Agent", ua)
                    .build()
                val result = runCatching {
                    client.newCall(req).execute().use { resp: Response ->
                        if (!resp.isSuccessful) {
                            val contentType = resp.header("Content-Type").orEmpty()
                            throw IOException("http=${resp.code} ct=${contentType.ifBlank { "-" }}")
                        }
                        decodeBitmap(
                            response = resp,
                            maxDecodeDimension = maxDecodeDimension,
                            onProgress = onProgress
                        ) ?: throw IOException("bitmap-decode-null")
                    }
                }
                val elapsedMs = System.currentTimeMillis() - startMs
                if (result.isSuccess) {
                    val bitmap = result.getOrNull()
                    if (bitmap != null) {
                        logD(
                            "image[$traceId] ok a$attempt/$totalAttempts ${shortUa(ua)} ${elapsedMs}ms " +
                                "size=${bitmap.width}x${bitmap.height} url=${url.compactForLog(110)} referer=${referer.compactForLog(80)}"
                        )
                        return bitmap
                    }
                }
                if (result.isFailure) {
                    lastError = result.exceptionOrNull()
                    val err = lastError?.toCompactLogMessage().orEmpty()
                    errors += "a$attempt:${shortUa(ua)}:${err.compactForLog(120)}"
                    logD(
                        "image[$traceId] fail a$attempt/$totalAttempts ${shortUa(ua)} ${elapsedMs}ms " +
                            "url=${url.compactForLog(110)} referer=${referer.compactForLog(80)} err=$err"
                    )
                }
            }
        }

        if (errors.isNotEmpty()) {
            logD("image[$traceId] fail-all summary=${errors.joinToString(" | ").compactForLog(720)}")
        }
        if (lastError != null) throw lastError
        return null
    }

    fun fetchImage(
        imageUrl: String,
        maxDecodeDimension: Int = DEFAULT_MAX_DECODE_EDGE
    ): Bitmap? {
        return fetchImageInternal(
            imageUrl = imageUrl,
            maxDecodeDimension = maxDecodeDimension,
            onProgress = null
        )
    }

    fun fetchImageWithProgress(
        imageUrl: String,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
        maxDecodeDimension: Int = DEFAULT_MAX_DECODE_EDGE
    ): Bitmap? {
        return fetchImageInternal(
            imageUrl = imageUrl,
            maxDecodeDimension = maxDecodeDimension,
            onProgress = onProgress
        )
    }

    fun downloadToFile(
        mediaUrl: String,
        targetFile: File,
        onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)? = null
    ): Boolean {
        val normalized = mediaUrl.trim()
        if (normalized.isBlank()) return false
        val traceId = "dl-${System.nanoTime().toString(16)}"

        val requestUrls = buildList {
            add(normalized)
            if (normalized.startsWith("//")) add("https:$normalized")
        }.distinct()

        val uas = REQUEST_UAS
        val tempFile = File(targetFile.parentFile ?: return false, "${targetFile.name}.part")
        targetFile.parentFile?.mkdirs()
        val totalAttempts = requestUrls.size * uas.size
        var attempt = 0
        val errors = mutableListOf<String>()
        logD(
            "download[$traceId] start url=${normalized.compactForLog(140)} " +
                "target=${targetFile.absolutePath.compactForLog(120)} attempts=$totalAttempts"
        )

        var lastError: Throwable? = null
        requestUrls.forEach { url ->
            val referer = resolveReferer(pathOrUrl = url, refererPath = "")
            uas.forEach { ua ->
                attempt += 1
                val startMs = System.currentTimeMillis()
                val req = Request.Builder()
                    .url(url)
                    .get()
                    .header("Accept", ACCEPT_IMAGE)
                    .header("Accept-Language", ACCEPT_LANGUAGE)
                    .header("Referer", referer)
                    .header("User-Agent", ua)
                    .build()

                val result = runCatching {
                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) {
                            val contentType = resp.header("Content-Type").orEmpty()
                            throw IOException("http=${resp.code} ct=${contentType.ifBlank { "-" }}")
                        }
                        val body = resp.body
                        val total = body.contentLength()
                        body.byteStream().use { input ->
                            tempFile.outputStream().use { out ->
                                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                var downloaded = 0L
                                while (true) {
                                    val count = input.read(buffer)
                                    if (count <= 0) break
                                    out.write(buffer, 0, count)
                                    downloaded += count
                                    onProgress?.invoke(downloaded, total)
                                }
                            }
                        }
                        Unit
                    }
                }
                val elapsedMs = System.currentTimeMillis() - startMs
                if (result.isSuccess) {
                    if (targetFile.exists()) {
                        runCatching { targetFile.delete() }
                    }
                    val renamed = tempFile.renameTo(targetFile)
                    if (renamed) {
                        val size = targetFile.length()
                        logD(
                            "download[$traceId] ok a$attempt/$totalAttempts ${shortUa(ua)} " +
                                "${elapsedMs}ms bytes=$size file=${targetFile.name}"
                        )
                        return true
                    }
                    val renameError = IOException("rename-failed ${tempFile.absolutePath} -> ${targetFile.absolutePath}")
                    lastError = renameError
                    errors += "a$attempt:${shortUa(ua)}:${renameError.toCompactLogMessage().compactForLog(120)}"
                    logD(
                        "download[$traceId] fail a$attempt/$totalAttempts ${shortUa(ua)} ${elapsedMs}ms " +
                            "url=${url.compactForLog(110)} err=${renameError.toCompactLogMessage()}"
                    )
                }
                if (result.isFailure) {
                    lastError = result.exceptionOrNull()
                    val err = lastError?.toCompactLogMessage().orEmpty()
                    errors += "a$attempt:${shortUa(ua)}:${err.compactForLog(120)}"
                    logD(
                        "download[$traceId] fail a$attempt/$totalAttempts ${shortUa(ua)} ${elapsedMs}ms " +
                            "url=${url.compactForLog(110)} referer=${referer.compactForLog(80)} err=$err"
                    )
                }
                if (tempFile.exists()) {
                    runCatching { tempFile.delete() }
                }
            }
        }

        if (tempFile.exists()) {
            runCatching { tempFile.delete() }
        }
        if (errors.isNotEmpty()) {
            logD("download[$traceId] fail-all summary=${errors.joinToString(" | ").compactForLog(720)}")
        }
        if (lastError != null) throw lastError
        return false
    }
}
