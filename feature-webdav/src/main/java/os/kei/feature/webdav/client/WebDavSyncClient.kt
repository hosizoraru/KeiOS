package os.kei.feature.webdav.client

import at.bitfire.dav4jvm.ktor.DavCollection
import at.bitfire.dav4jvm.property.webdav.WebDAV
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.http.content.ByteArrayContent
import io.ktor.utils.io.readAvailable
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import os.kei.core.concurrency.AppDispatchers
import os.kei.feature.webdav.model.WebDavConfig
import os.kei.feature.webdav.model.WebDavRemoteFile

/** Known-file sync uses GET/PUT; directory enumeration is never required to find a backup. */
class WebDavSyncClient(
    private val config: WebDavConfig,
    private val ioDispatcher: CoroutineDispatcher = AppDispatchers.webDavNetwork,
    engine: HttpClientEngine? = null,
    private val requestPolicy: WebDavRequestPolicy = WebDavRequestPolicy.forConfig(config),
    private val retryDelay: suspend (Long) -> Unit = { delay(it) },
) : WebDavRemoteClient {
    private val endpoint = WebDavEndpoint(config)
    private val credentialIdentity = os.kei.feature.webdav.sync.contentHash("${config.username}\n${config.appPassword}")
    private val httpClient = if (engine == null) {
        HttpClient(CIO) { configure() }
    } else {
        HttpClient(engine) { configure() }
    }

    private fun HttpClientConfig<*>.configure() {
        followRedirects = false
        expectSuccess = false
        install(UserAgent) { agent = USER_AGENT }
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 60_000
            socketTimeoutMillis = 30_000
        }
        install(Auth) { configureWebDavAuth(config, endpoint.base.host) }
    }

    override fun close() = httpClient.close()

    override suspend fun testConnection(): WebDavTestConnectionResult = withContext(ioDispatcher) {
        try {
            requestPolicy.retryCredentials(credentialIdentity)
            var created = false
            var response = request("PROPFIND", endpoint.collection, PROPFIND_BODY, xml = true)
            if (response.code in setOf(404, 409)) {
                createDirectories()
                created = true
                response = request("PROPFIND", endpoint.collection, PROPFIND_BODY, xml = true)
            }
            requireCollection(response)
            WebDavTestConnectionResult.Success(created)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            when (val failure = classify(error)) {
                WebDavError.AuthFailed -> WebDavTestConnectionResult.AuthFailed
                WebDavError.PermissionDenied -> WebDavTestConnectionResult.PermissionDenied
                WebDavError.NetworkUnreachable -> WebDavTestConnectionResult.NetworkError("WebDAV network request failed")
                is WebDavError.Unknown -> WebDavTestConnectionResult.Error("HTTP ${failure.code}: ${failure.message}")
            }
        }
    }

    override suspend fun download(fileName: String): WebDavDownloadResult = withContext(ioDispatcher) {
        try {
            val response = request("GET", endpoint.file(fileName))
            if (response.code == 404) return@withContext WebDavDownloadResult.Empty
            requireStatus(response, 200)
            val text = try {
                response.bytes.decodeToString(throwOnInvalidSequence = true).removePrefix("\uFEFF")
            } catch (_: java.nio.charset.CharacterCodingException) {
                throw WebDavRequestException(WebDavError.Unknown(422, "Remote backup is not valid UTF-8"))
            }
            if (text.isBlank() || text.trimStart().startsWith("<")) {
                throw WebDavRequestException(WebDavError.Unknown(422, "Remote backup is empty or is not a JSON document"))
            }
            WebDavDownloadResult.Success(text, response.headers[HttpHeaders.ETag])
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            WebDavDownloadResult.Error(classify(error))
        }
    }

    override suspend fun upload(fileName: String, content: String, etag: String?): WebDavUploadResult =
        write(fileName, content, etag?.let { HttpHeaders.IfMatch to normalizeEtag(it) })

    override suspend fun uploadIfAbsent(fileName: String, content: String): WebDavUploadResult =
        write(fileName, content, HttpHeaders.IfNoneMatch to "*")

    private suspend fun write(
        fileName: String,
        content: String,
        condition: Pair<String, String>?,
    ): WebDavUploadResult = withContext(ioDispatcher) {
        try {
            val bytes = content.toByteArray(Charsets.UTF_8)
            if (bytes.isEmpty() || bytes.size > MAX_DOCUMENT_BYTES) throw WebDavRequestException(
                WebDavError.Unknown(413, "Backup must be nonempty and no larger than 32 MiB"),
            )
            val target = endpoint.file(fileName)
            val response = try {
                var put = request("PUT", target, bytes, condition = condition)
                if (put.code in setOf(404, 409)) {
                    createDirectories()
                    put = request("PUT", target, bytes, condition = condition)
                }
                put
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                // A disconnected PUT may already have committed. Read back instead of replaying it.
                return@withContext verifyWrite(fileName, content, classify(error))
            }
            if (response.code == 412) return@withContext WebDavUploadResult.Conflict
            requireStatus(response, 200, 201, 204)
            verifyWrite(fileName, content)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            WebDavUploadResult.Error(classify(error))
        }
    }

    private suspend fun verifyWrite(
        fileName: String,
        content: String,
        originalFailure: WebDavError? = null,
    ): WebDavUploadResult = when (val readback = download(fileName)) {
        is WebDavDownloadResult.Success -> if (readback.content == content.removePrefix("\uFEFF")) {
            WebDavUploadResult.Success(readback.etag)
        } else {
            WebDavUploadResult.Conflict
        }
        WebDavDownloadResult.Empty -> WebDavUploadResult.Error(
            originalFailure ?: WebDavError.Unknown(502, "Uploaded backup could not be read back"),
        )
        is WebDavDownloadResult.Error -> WebDavUploadResult.Error(originalFailure ?: readback.error)
    }

    private suspend fun createDirectories() {
        for (directory in endpoint.directories) {
            val response = request("MKCOL", directory)
            if (response.code == 405) {
                requireCollection(request("PROPFIND", directory, PROPFIND_BODY, xml = true))
            } else {
                requireStatus(response, 200, 201, 204)
            }
        }
    }

    private fun requireCollection(response: WireResponse) {
        requireStatus(response, 200, 207)
        val xml = response.bytes.toString(Charsets.UTF_8)
        require(!xml.contains("<!DOCTYPE", ignoreCase = true)) { "Invalid WebDAV XML" }
        val parser = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }.newPullParser()
        parser.setInput(xml.reader())
        var collection = false
        var propertyIsCollection = false
        var propertySucceeded = false
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.namespace == "DAV:") {
                when (parser.name) {
                    "propstat" -> { propertyIsCollection = false; propertySucceeded = false }
                    "collection" -> propertyIsCollection = true
                    "status" -> propertySucceeded = parser.nextText().trim().split(' ').getOrNull(1) == "200"
                }
            }
            if (parser.eventType == XmlPullParser.END_TAG && parser.name == "propstat" &&
                propertyIsCollection && propertySucceeded) collection = true
            parser.next()
        }
        require(collection) { "The WebDAV path is not an accessible collection" }
    }

    /** Legacy browsing API. Failed/truncated listings must never masquerade as an empty cloud. */
    suspend fun listFiles(): List<WebDavRemoteFile> = withContext(ioDispatcher) {
        requestPolicy.acquire(credentialIdentity)
        val files = mutableListOf<WebDavRemoteFile>()
        DavCollection(httpClient, endpoint.collection).propfind(
            1, WebDAV.GetETag, WebDAV.GetLastModified, WebDAV.GetContentLength, WebDAV.ResourceType,
        ).collect { it.toWebDavRemoteFileOrNull()?.let(files::add) }
        check(!endpoint.base.host.equals("dav.jianguoyun.com", true) || files.size < 749) {
            "The provider may have truncated this listing; known backup files can still be synced directly"
        }
        files
    }

    private suspend fun request(
        method: String,
        originalUrl: Url,
        body: ByteArray? = null,
        xml: Boolean = false,
        condition: Pair<String, String>? = null,
    ): WireResponse {
        var target = originalUrl
        var redirects = 0
        var retries = 0
        while (true) {
            currentCoroutineContext().ensureActive()
            requestPolicy.acquire(credentialIdentity)
            val response = try {
                httpClient.prepareRequest(target) {
                    this.method = HttpMethod(method)
                    headers.append(HttpHeaders.CacheControl, "no-cache, no-store")
                    headers.append(HttpHeaders.Accept, if (xml) "application/xml" else "application/json")
                    if (method == "PROPFIND") headers.append("Depth", "0")
                    condition?.let { headers.append(it.first, it.second) }
                    body?.let { setBody(ByteArrayContent(it, if (xml) ContentType.Application.Xml else ContentType.Application.Json)) }
                }.execute { incoming ->
                    val channel = incoming.bodyAsChannel()
                    val buffer = ByteArray(8192)
                    val bytes = ByteArrayOutputStream()
                    try {
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = channel.readAvailable(buffer, 0, buffer.size)
                            if (count < 0) break
                            if (bytes.size() + count > MAX_DOCUMENT_BYTES) {
                                throw WebDavRequestException(WebDavError.Unknown(413, "Remote backup exceeds the 32 MiB sync limit"))
                            }
                            bytes.write(buffer, 0, count)
                        }
                        WireResponse(incoming.status.value, incoming.headers, bytes.toByteArray())
                    } finally {
                        channel.cancel(null)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                if (method !in setOf("GET", "PROPFIND") || retries >= 2) throw error
                retryDelay(500L shl retries++)
                continue
            }
            if (response.code in setOf(301, 302, 307, 308)) {
                require(++redirects <= 3) { "Too many WebDAV redirects" }
                target = endpoint.redirect(target, response.headers[HttpHeaders.Location] ?: error("Missing redirect target"))
                continue
            }
            if (response.code == 429 || response.code == 503) {
                requestPolicy.coolDown(response.headers[HttpHeaders.RetryAfter])
                throw WebDavRequestException(WebDavError.Unknown(response.code, "WebDAV temporarily unavailable or rate limited; wait before retrying"))
            }
            if (response.code == 401) requestPolicy.rejectCredentials(credentialIdentity)
            if (response.code in setOf(502, 504) && method in setOf("GET", "PROPFIND") && retries < 2) {
                retryDelay(500L shl retries++)
                continue
            }
            return response
        }
    }

    private fun requireStatus(response: WireResponse, vararg accepted: Int) {
        if (response.code in accepted) return
        throw WebDavRequestException(when (response.code) {
            401 -> WebDavError.AuthFailed
            403 -> WebDavError.PermissionDenied
            else -> WebDavError.Unknown(response.code, "WebDAV request rejected (HTTP ${response.code})")
        })
    }

    private fun classify(error: Exception): WebDavError = when (error) {
        is WebDavRequestException -> error.error
        is IOException -> WebDavError.NetworkUnreachable
        else -> WebDavError.Unknown(0, "Invalid WebDAV response or configuration (${error.javaClass.simpleName})")
    }

    private fun normalizeEtag(etag: String): String {
        val trimmed = etag.trim()
        require(os.kei.feature.webdav.sync.WebDavSyncSession.strongEtag(trimmed) != null) {
            "Conditional writes require a strong ETag"
        }
        return if (trimmed.startsWith('"') && trimmed.endsWith('"')) trimmed else "\"$trimmed\""
    }

    private data class WireResponse(val code: Int, val headers: Headers, val bytes: ByteArray)

    companion object {
        const val USER_AGENT = "KeiOS-WebDAV/2 (+https://github.com/hosizoraru/KeiOS)"
        private const val MAX_DOCUMENT_BYTES = 32 * 1024 * 1024
        private val PROPFIND_BODY = """<?xml version="1.0" encoding="utf-8"?><d:propfind xmlns:d="DAV:"><d:prop><d:resourcetype/></d:prop></d:propfind>""".toByteArray()
    }
}
