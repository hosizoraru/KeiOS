package os.kei.feature.webdav.client

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Test
import os.kei.feature.webdav.model.WebDavConfig
import kotlin.test.*

class WebDavSyncClientTest {
    private val config = WebDavConfig("https://dav.example.test/dav/", "user", "application-password", "KeiOS/")
    private fun client(engine: MockEngine, policy: WebDavRequestPolicy = WebDavRequestPolicy(), waits: MutableList<Long> = mutableListOf()) =
        WebDavSyncClient(config, engine = engine, requestPolicy = policy, retryDelay = { waits += it })

    @Test fun `put is read back and returns the readback validator without directory listing`(): Unit = runBlocking {
        val methods = mutableListOf<String>()
        val content = """{"name":"\u4e2d\u6587"}"""
        val c = client(MockEngine { request ->
            methods += request.method.value
            assertEquals("https://dav.example.test/dav/KeiOS/data.json", request.url.toString())
            assertTrue(request.headers[HttpHeaders.Authorization]?.startsWith("Basic ") == true)
            when (request.method.value) {
                "PUT" -> {
                    assertEquals("\"old\"", request.headers[HttpHeaders.IfMatch])
                    assertEquals(content, (request.body as OutgoingContent.ByteArrayContent).bytes().toString(Charsets.UTF_8))
                    respond("", HttpStatusCode.NoContent)
                }
                else -> respond(content, headers = headersOf(HttpHeaders.ETag, "\"readback\""))
            }
        })
        try { assertEquals(WebDavUploadResult.Success("\"readback\""), c.upload("data.json", content, "old")) }
        finally { c.close() }
        assertEquals(listOf("PUT", "GET"), methods)
    }

    @Test fun `create only precondition is preserved and a 412 never falls back`(): Unit = runBlocking {
        var calls = 0
        val c = client(MockEngine { request ->
            calls++
            assertEquals("*", request.headers[HttpHeaders.IfNoneMatch])
            respond("", HttpStatusCode.PreconditionFailed)
        })
        try { assertEquals(WebDavUploadResult.Conflict, c.uploadIfAbsent("data.json", "{}")) }
        finally { c.close() }
        assertEquals(1, calls)
    }

    @Test fun `ambiguous committed PUT is recovered by readback instead of replay`(): Unit = runBlocking {
        val methods = mutableListOf<String>()
        val c = client(MockEngine { request ->
            methods += request.method.value
            if (request.method.value == "PUT") throw IOException("connection lost after commit")
            respond("{}", headers = headersOf(HttpHeaders.ETag, "\"committed\""))
        })
        try { assertEquals(WebDavUploadResult.Success("\"committed\""), c.upload("data.json", "{}")) }
        finally { c.close() }
        assertEquals(listOf("PUT", "GET"), methods)
    }

    @Test fun `mismatching readback cannot be recorded as successful upload`(): Unit = runBlocking {
        val c = client(MockEngine { request ->
            if (request.method.value == "PUT") respond("", HttpStatusCode.Created) else respond("{\"other\":true}")
        })
        try { assertEquals(WebDavUploadResult.Conflict, c.upload("data.json", "{}")) }
        finally { c.close() }
    }

    @Test fun `only 404 means absent - empty HTML and unauthorized responses are failures`(): Unit = runBlocking {
        for ((status, text) in listOf(404 to "", 200 to "", 200 to "<html>login</html>", 401 to "", 403 to "")) {
            val c = client(MockEngine { respond(text, HttpStatusCode.fromValue(status)) })
            try {
                val result = c.download("data.json")
                if (status == 404) assertEquals(WebDavDownloadResult.Empty, result)
                else assertIs<WebDavDownloadResult.Error>(result)
            } finally { c.close() }
        }
    }

    @Test fun `invalid UTF8 is corrupt content rather than a network outage`(): Unit = runBlocking {
        val c = client(MockEngine { respond(byteArrayOf(0xc3.toByte(), 0x28)) })
        try {
            val result = assertIs<WebDavDownloadResult.Error>(c.download("data.json"))
            assertEquals(422, assertIs<WebDavError.Unknown>(result.error).code)
        } finally { c.close() }
    }

    @Test fun `rejected credentials stop a six-file batch across clients and a new password can recover`(): Unit = runBlocking {
        var requests = 0
        val policy = WebDavRequestPolicy()
        val engine = MockEngine { request ->
            requests++
            val valid = "Basic " + java.util.Base64.getEncoder().encodeToString("${config.username}:fixed".toByteArray())
            if (request.headers[HttpHeaders.Authorization] == valid) respond("{}")
            else respond("", HttpStatusCode.Unauthorized)
        }
        val first = client(engine, policy); val second = client(engine, policy)
        val fixed = WebDavSyncClient(config.copy(appPassword = "fixed"), engine = engine, requestPolicy = policy)
        try {
            repeat(6) {
                val read = (if (it % 2 == 0) first else second).download("data-$it.json")
                assertEquals(WebDavError.AuthFailed, assertIs<WebDavDownloadResult.Error>(read).error)
            }
            assertEquals(1, requests)
            assertIs<WebDavDownloadResult.Success>(fixed.download("data.json"))
            assertEquals(2, requests)
        } finally { first.close(); second.close(); fixed.close() }
    }

    @Test fun `503 Retry-After cooldown spans clients and does not flood the provider`(): Unit = runBlocking {
        var now = 10_000L
        var requests = 0
        val policy = WebDavRequestPolicy(nowMs = { now })
        val engine = MockEngine {
            requests++
            if (requests == 1) respond("busy", HttpStatusCode.ServiceUnavailable, headersOf(HttpHeaders.RetryAfter, "120"))
            else respond("{}")
        }
        val first = client(engine, policy)
        val second = client(engine, policy)
        try {
            assertIs<WebDavDownloadResult.Error>(first.download("data.json"))
            assertIs<WebDavDownloadResult.Error>(second.download("data.json"))
            assertEquals(1, requests)
            now += 120_001
            assertIs<WebDavDownloadResult.Success>(second.download("data.json"))
            assertEquals(2, requests)
        } finally { first.close(); second.close() }
    }

    @Test fun `safe transient reads retry with bounded backoff`(): Unit = runBlocking {
        var requests = 0
        val waits = mutableListOf<Long>()
        val c = client(MockEngine {
            requests++
            if (requests < 3) respond("busy", HttpStatusCode.BadGateway) else respond("{}")
        }, waits = waits)
        try { assertIs<WebDavDownloadResult.Success>(c.download("data.json")) }
        finally { c.close() }
        assertEquals(listOf(500L, 1000L), waits)
        assertEquals(3, requests)
    }

    @Test fun `cross-origin redirects are refused before sending credentials`(): Unit = runBlocking {
        val targets = mutableListOf<String>()
        val c = client(MockEngine { request ->
            targets += request.url.toString()
            respond("", HttpStatusCode.TemporaryRedirect, headersOf(HttpHeaders.Location, "https://elsewhere.test/secret"))
        })
        try { assertIs<WebDavDownloadResult.Error>(c.download("data.json")) }
        finally { c.close() }
        assertEquals(1, targets.size)
    }

    @Test fun `directory segments and file names keep percent plus unicode and reserved characters`(): Unit = runBlocking {
        var actual = ""
        val special = config.copy(serverUrl = "https://dav.example.test/dav%20root/", remoteDir = "\u4e2d\u6587 +%/nested#?/")
        val c = WebDavSyncClient(special, engine = MockEngine { request ->
            actual = request.url.toString(); respond("{}")
        }, requestPolicy = WebDavRequestPolicy())
        try { assertIs<WebDavDownloadResult.Success>(c.download("a +%.json")) }
        finally { c.close() }
        assertEquals("https://dav.example.test/dav%20root/%E4%B8%AD%E6%96%87%20%2B%25/nested%23%3F/a%20%2B%25.json", actual)
    }

    @Test fun `missing parents are created recursively and an existing directory is verified`(): Unit = runBlocking {
        val methods = mutableListOf<String>()
        var puts = 0
        val c = client(MockEngine { request ->
            methods += request.method.value
            when (request.method.value) {
                "PUT" -> respond("", if (++puts == 1) HttpStatusCode.Conflict else HttpStatusCode.Created)
                "MKCOL" -> respond("", HttpStatusCode.MethodNotAllowed)
                "PROPFIND" -> respond(COLLECTION, HttpStatusCode.MultiStatus, headersOf(HttpHeaders.ContentType, "application/xml"))
                else -> respond("{}")
            }
        })
        try { assertIs<WebDavUploadResult.Success>(c.upload("data.json", "{}")) }
        finally { c.close() }
        assertEquals(listOf("PUT", "MKCOL", "PROPFIND", "PUT", "GET"), methods)
    }

    @Test fun `an existing non-directory cannot pass the collection check`(): Unit = runBlocking {
        val c = client(MockEngine { respond(COLLECTION.replace("<d:collection/>", ""), HttpStatusCode.MultiStatus) })
        try { assertIs<WebDavTestConnectionResult.Error>(c.testConnection()) }
        finally { c.close() }
    }

    companion object {
        private const val COLLECTION = """<d:multistatus xmlns:d="DAV:"><d:response><d:href>/dav/KeiOS/</d:href><d:propstat><d:prop><d:resourcetype><d:collection/></d:resourcetype></d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response></d:multistatus>"""
    }
}
