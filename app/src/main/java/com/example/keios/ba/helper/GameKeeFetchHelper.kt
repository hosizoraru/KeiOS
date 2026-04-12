package com.example.keios.ba.helper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.dnsoverhttps.DnsOverHttps
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.URI
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object GameKeeFetchHelper {
    private const val TAG = "GameKeeFetch"
    private val ENABLE_LOG = runCatching {
        Class.forName("com.example.keios.BuildConfig").getField("DEBUG").getBoolean(null)
    }.getOrDefault(false)
    private const val BASE_WWW = "https://www.gamekee.com"
    private const val BASE_BARE = "https://gamekee.com"
    private const val ACCEPT_JSON = "application/json, text/plain, */*"
    private const val ACCEPT_HTML = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
    private const val ACCEPT_IMAGE = "image/*,*/*"
    private const val ACCEPT_LANGUAGE = "zh-CN"
    private const val DNS_CACHE_TTL_MS = 10 * 60 * 1000L
    private val GAMEKEE_RESOLVE_IPS = listOf(
        "39.96.244.149"
    )
    private const val DESKTOP_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36"
    private const val ANDROID_UA =
        "Mozilla/5.0 (Linux; Android 15; 23127PN0CC Build/AP3A.240905.015.A2; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/126.0.6478.122 Mobile Safari/537.36"

    private data class CachedDns(
        val addresses: List<InetAddress>,
        val expiresAtMs: Long
    )

    private val dnsCache = ConcurrentHashMap<String, CachedDns>()

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

    private val bootstrapClient: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(2500, TimeUnit.MILLISECONDS)
        .connectTimeout(1500, TimeUnit.MILLISECONDS)
        .readTimeout(1500, TimeUnit.MILLISECONDS)
        .writeTimeout(1500, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(false)
        .fastFallback(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val dohGoogle: DnsOverHttps by lazy {
        DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url("https://dns.google/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                InetAddress.getByName("8.8.8.8"),
                InetAddress.getByName("8.8.4.4")
            )
            .includeIPv6(true)
            .resolvePrivateAddresses(true)
            .build()
    }

    private val dohCloudflare: DnsOverHttps by lazy {
        DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                InetAddress.getByName("1.1.1.1"),
                InetAddress.getByName("1.0.0.1")
            )
            .includeIPv6(true)
            .resolvePrivateAddresses(true)
            .build()
    }

    private val dohAliDns: DnsOverHttps by lazy {
        DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url("https://dns.alidns.com/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                InetAddress.getByName("223.5.5.5"),
                InetAddress.getByName("223.6.6.6")
            )
            .includeIPv6(true)
            .resolvePrivateAddresses(true)
            .build()
    }

    private val dohDnsPod: DnsOverHttps by lazy {
        DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url("https://doh.pub/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                InetAddress.getByName("119.29.29.29"),
                InetAddress.getByName("1.12.12.12")
            )
            .includeIPv6(true)
            .resolvePrivateAddresses(true)
            .build()
    }

    private fun logD(msg: String) {
        if (ENABLE_LOG) Log.d(TAG, msg)
    }

    private fun normalizeAddresses(addresses: List<InetAddress>): List<InetAddress> {
        return addresses
            .asSequence()
            .distinctBy { it.hostAddress }
            .toList()
    }

    private fun loadDnsCache(hostname: String): List<InetAddress>? {
        val key = hostname.lowercase()
        val cached = dnsCache[key] ?: return null
        if (cached.expiresAtMs <= System.currentTimeMillis()) {
            dnsCache.remove(key)
            return null
        }
        return cached.addresses
    }

    private fun saveDnsCache(hostname: String, addresses: List<InetAddress>) {
        if (addresses.isEmpty()) return
        dnsCache[hostname.lowercase()] = CachedDns(
            addresses = addresses,
            expiresAtMs = System.currentTimeMillis() + DNS_CACHE_TTL_MS
        )
    }

    private val smartDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val host = hostname.lowercase()
            val isGameKee = host.contains("gamekee.com")
            if (!isGameKee) return Dns.SYSTEM.lookup(hostname)
            val isApiHost = host == "www.gamekee.com" || host == "gamekee.com"
            if (!isApiHost) {
                // 图片域名不走 DoH，避免大量并发图片请求把 DNS 重试链路拖垮主数据同步。
                return Dns.SYSTEM.lookup(hostname)
            }

            loadDnsCache(hostname)?.let { cached ->
                logD("dns cache hit: $hostname -> $cached")
                return cached
            }

            var lastError: Throwable? = null
            val collected = LinkedHashMap<String, InetAddress>()

            fun collectFrom(name: String, resolver: () -> List<InetAddress>) {
                val startMs = System.currentTimeMillis()
                val result = runCatching { normalizeAddresses(resolver()) }
                val costMs = System.currentTimeMillis() - startMs
                val addresses = result.getOrNull().orEmpty()
                if (addresses.isNotEmpty()) {
                    addresses.forEach { addr ->
                        val key = addr.hostAddress ?: addr.hostName ?: return@forEach
                        collected.putIfAbsent(key, addr)
                    }
                    logD("dns $name ok (${costMs}ms): $hostname -> $addresses")
                } else {
                    val error = result.exceptionOrNull()
                    if (error != null) {
                        lastError = error
                        logD("dns $name failed (${costMs}ms): $hostname err=${error.message}")
                    }
                }
            }

            collectFrom("system") { Dns.SYSTEM.lookup(hostname) }
            val hasIpv6 = collected.values.any { it.hostAddress?.contains(":") == true }
            if (!hasIpv6) {
                // 某些网络下 IPv4 通路不稳定，补充 DoH 以拿到 IPv6 地址。
                collectFrom("alidns-doh") { dohAliDns.lookup(hostname) }
                collectFrom("dnspod-doh") { dohDnsPod.lookup(hostname) }
            }
            if (collected.isEmpty()) {
                collectFrom("google-doh") { dohGoogle.lookup(hostname) }
                collectFrom("cloudflare-doh") { dohCloudflare.lookup(hostname) }
            }

            val finalAddresses = collected.values.toList()
            if (finalAddresses.isNotEmpty()) {
                saveDnsCache(hostname, finalAddresses)
                return finalAddresses
            }

            throw UnknownHostException("Unable to resolve $hostname").also { ex ->
                lastError?.let { ex.addSuppressed(it) }
            }
        }
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
        .dns(smartDns)
        .cookieJar(InMemoryCookieJar())
        .build()

    private fun normalizeUrl(base: String, pathOrUrl: String): String {
        val raw = pathOrUrl.trim()
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
        return if (raw.startsWith("/")) "$base$raw" else "$base/$raw"
    }

    private fun normalizeReferer(base: String, refererPath: String): String {
        val ref = refererPath.trim().ifBlank { "/" }
        return normalizeUrl(base, ref)
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
            .header("Cache-Control", "no-cache")
        extraHeaders.forEach { (k, v) -> builder.header(k, v) }

        client.newCall(builder.build()).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            val body = resp.body.string()
            if (requireJsonBody && !isJsonLike(body)) {
                throw IOException("non-json body")
            }
            return body
        }
    }

    private fun executeTextBySystemCurl(
        url: String,
        referer: String,
        ua: String,
        acceptHeader: String,
        extraHeaders: Map<String, String>,
        requireJsonBody: Boolean
    ): String {
        val host = runCatching { URI(url).host.orEmpty() }.getOrDefault("")
        val useResolveByIp = host.equals("www.gamekee.com", ignoreCase = true) ||
            host.equals("gamekee.com", ignoreCase = true)
        val cmd = mutableListOf(
            "/system/bin/curl",
            "-sS",
            "-L",
            "--max-time",
            "12",
            "--connect-timeout",
            "8",
            "--retry",
            "1",
            "--retry-delay",
            "1",
            "-H",
            "Accept: $acceptHeader",
            "-H",
            "Accept-Language: $ACCEPT_LANGUAGE",
            "-H",
            "Referer: $referer",
            "-H",
            "User-Agent: $ua",
            "-H",
            "Cache-Control: no-cache"
        )
        if (useResolveByIp) {
            val resolveHost = if (host.equals("gamekee.com", ignoreCase = true)) "gamekee.com" else "www.gamekee.com"
            GAMEKEE_RESOLVE_IPS.forEach { ip ->
                cmd += "--resolve"
                cmd += "$resolveHost:443:$ip"
            }
        }
        extraHeaders.forEach { (k, v) ->
            cmd += "-H"
            cmd += "$k: $v"
        }
        cmd += url

        val process = ProcessBuilder(cmd)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val finished = process.waitFor(14, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            throw IOException("system curl timeout")
        }
        if (process.exitValue() != 0) {
            throw IOException("system curl exit=${process.exitValue()} body=${output.take(120)}")
        }
        if (requireJsonBody && !isJsonLike(output)) {
            throw IOException("system curl non-json body")
        }
        return output
    }

    private fun fetchText(
        pathOrUrl: String,
        refererPath: String,
        acceptHeader: String,
        extraHeaders: Map<String, String>,
        requireJsonBody: Boolean
    ): String {
        val candidates = listOf(
            Triple(BASE_WWW, DESKTOP_UA, normalizeReferer(BASE_WWW, refererPath)),
            Triple(BASE_WWW, ANDROID_UA, normalizeReferer(BASE_WWW, refererPath))
        )

        var lastError: Throwable? = null
        candidates.forEach { (base, ua, referer) ->
            val url = normalizeUrl(base, pathOrUrl)
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
            if (result.isSuccess) return result.getOrThrow()
            lastError = result.exceptionOrNull()
            logD("fetch failed: url=$url referer=$referer ua=${ua.take(24)} err=${lastError?.message}")
        }

        val fallbackCandidates = listOf(
            Triple(BASE_WWW, DESKTOP_UA, normalizeReferer(BASE_WWW, refererPath)),
            Triple(BASE_BARE, DESKTOP_UA, normalizeReferer(BASE_BARE, refererPath))
        )
        fallbackCandidates.forEach { (base, ua, referer) ->
            val fallbackUrl = normalizeUrl(base, pathOrUrl)
            val fallback = runCatching {
                logD("fallback to system curl: $fallbackUrl")
                executeTextBySystemCurl(
                    url = fallbackUrl,
                    referer = referer,
                    ua = ua,
                    acceptHeader = acceptHeader,
                    extraHeaders = extraHeaders,
                    requireJsonBody = requireJsonBody
                )
            }
            if (fallback.isSuccess) return fallback.getOrThrow()
            lastError = fallback.exceptionOrNull() ?: lastError
            val errName = lastError?.javaClass?.simpleName ?: "UnknownErr"
            logD("system curl failed: url=$fallbackUrl err=$errName:${lastError?.message}")
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
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }

    private fun fetchImageInternal(
        imageUrl: String,
        onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)? = null
    ): Bitmap? {
        val normalized = imageUrl.trim()
        if (normalized.isBlank()) return null

        val requestUrls = buildList {
            add(normalized)
            if (normalized.startsWith("//")) add("https:$normalized")
        }.distinct()

        var lastError: Throwable? = null
        val referers = listOf(
            "https://www.gamekee.com/",
            "https://www.gamekee.com/ba/huodong/15",
            "https://www.gamekee.com/ba/kachi/15"
        )
        val uas = listOf(DESKTOP_UA, ANDROID_UA)

        requestUrls.forEach { url ->
            referers.forEach { referer ->
                uas.forEach { ua ->
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
                            decodeBitmap(resp, onProgress)
                        }
                    }
                    if (result.isSuccess && result.getOrNull() != null) return result.getOrNull()
                    if (result.isFailure) lastError = result.exceptionOrNull()
                }
            }
        }

        if (lastError != null) throw lastError
        return null
    }

    fun fetchImage(imageUrl: String): Bitmap? {
        return fetchImageInternal(imageUrl, onProgress = null)
    }

    fun fetchImageWithProgress(
        imageUrl: String,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Bitmap? {
        return fetchImageInternal(imageUrl, onProgress = onProgress)
    }

    fun downloadToFile(
        mediaUrl: String,
        targetFile: File,
        onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)? = null
    ): Boolean {
        val normalized = mediaUrl.trim()
        if (normalized.isBlank()) return false

        val requestUrls = buildList {
            add(normalized)
            if (normalized.startsWith("//")) add("https:$normalized")
        }.distinct()

        val referers = listOf(
            "https://www.gamekee.com/",
            "https://www.gamekee.com/ba/huodong/15",
            "https://www.gamekee.com/ba/kachi/15"
        )
        val uas = listOf(DESKTOP_UA, ANDROID_UA)
        val tempFile = File(targetFile.parentFile ?: return false, "${targetFile.name}.part")
        targetFile.parentFile?.mkdirs()

        var lastError: Throwable? = null
        requestUrls.forEach { url ->
            referers.forEach { referer ->
                uas.forEach { ua ->
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
                            if (!resp.isSuccessful) return@use false
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
                            true
                        }
                    }
                    if (result.isSuccess && result.getOrDefault(false)) {
                        if (targetFile.exists()) {
                            runCatching { targetFile.delete() }
                        }
                        return tempFile.renameTo(targetFile)
                    }
                    if (result.isFailure) {
                        lastError = result.exceptionOrNull()
                    }
                    if (tempFile.exists()) {
                        runCatching { tempFile.delete() }
                    }
                }
            }
        }

        if (tempFile.exists()) {
            runCatching { tempFile.delete() }
        }
        if (lastError != null) throw lastError
        return false
    }
}
