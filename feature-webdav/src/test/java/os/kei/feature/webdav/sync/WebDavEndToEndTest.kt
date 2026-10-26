package os.kei.feature.webdav.sync

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import os.kei.feature.webdav.client.WebDavRequestPolicy
import os.kei.feature.webdav.client.WebDavSyncClient
import os.kei.feature.webdav.model.WebDavConfig
import kotlin.test.*

/** A real local HTTP server catches wire/framing issues that MockEngine cannot. */
class WebDavEndToEndTest {
    @Test fun `six known backups upload and restore through real HTTP with fresh validators`(): Unit = runBlocking {
        val cloud = LocalDav()
        val client = WebDavSyncClient(cloud.config, requestPolicy = WebDavRequestPolicy())
        try {
            withTimeout(30_000) {
                val session = WebDavSyncSession(client)
                for (file in FILES) {
                    val backup = """{"items":["backup-for-$file"]}"""
                    var local = backup
                    val port = WebDavDocumentPort(
                        exportJson = { local }, merge = { error("Restore must not merge") },
                        replace = { local = it }, localCount = { 1 }, countRemoteItems = { 1 },
                    )
                    val uploaded = session.execute(WebDavSyncAction.Upload, file, port, confirmedOverwrite = true)
                    assertEquals(WebDavSyncStatus.Uploaded, uploaded.status, "$file: $uploaded")
                    local = """{"items":["broken","local-only"]}"""
                    val restored = session.execute(WebDavSyncAction.Restore, file, port)
                    assertEquals(WebDavSyncStatus.Downloaded, restored.status, "$file: $restored")
                    assertEquals(backup, local)
                    assertEquals(contentHash(backup), restored.checkpoint?.hash)
                    assertNotNull(restored.etag)
                }
                assertEquals(FILES.size, cloud.documents.size)
                assertTrue(cloud.contentLengths.all { it > 0 })
            }
        } finally { client.close(); cloud.close() }
    }

    @Test fun `a provider acknowledging but discarding the PUT cannot establish a baseline`(): Unit = runBlocking {
        val cloud = LocalDav()
        cloud.discardWrites = true
        val client = WebDavSyncClient(cloud.config, requestPolicy = WebDavRequestPolicy())
        try {
            val port = WebDavDocumentPort({ "{}" }, {}, { 0 }, { 0 })
            val result = WebDavSyncSession(client).execute(WebDavSyncAction.Upload, FILES.first(), port, confirmedOverwrite = true)
            assertEquals(WebDavSyncStatus.Error, result.status)
            assertNull(result.checkpoint)
        } finally { client.close(); cloud.close() }
    }

    private class LocalDav : AutoCloseable {
        data class Blob(val bytes: ByteArray, val etag: String)
        val documents = ConcurrentHashMap<String, Blob>()
        val contentLengths = mutableListOf<Int>()
        @Volatile var discardWrites = false
        private val directories = ConcurrentHashMap.newKeySet<String>().apply { add("/dav/") }
        private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/dav/") { exchange -> handle(exchange) }
            start()
        }
        val config = WebDavConfig("http://127.0.0.1:${server.address.port}/dav/", "fixture", "password", "KeiOS +%/")

        private fun handle(exchange: HttpExchange) {
            exchange.use {
                val authorization = "Basic " + Base64.getEncoder().encodeToString("fixture:password".toByteArray())
                if (it.requestHeaders.getFirst("Authorization") != authorization) return reply(it, 401)
                val path = it.requestURI.path
                when (it.requestMethod) {
                    "MKCOL" -> reply(it, if (directories.add(path)) 201 else 405)
                    "PROPFIND" -> {
                        if (path !in directories) return reply(it, 404)
                        val xml = """<d:multistatus xmlns:d="DAV:"><d:response><d:href>$path</d:href><d:propstat><d:prop><d:resourcetype><d:collection/></d:resourcetype></d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response></d:multistatus>"""
                        reply(it, 207, xml.toByteArray())
                    }
                    "PUT" -> {
                        val parent = path.substringBeforeLast('/') + "/"
                        if (parent !in directories) return reply(it, 409)
                        val previous = documents[path]
                        if (it.requestHeaders.getFirst("If-None-Match") == "*" && previous != null) return reply(it, 412)
                        val expected = it.requestHeaders.getFirst("If-Match")
                        if (expected != null && expected != previous?.etag) return reply(it, 412)
                        contentLengths += it.requestHeaders.getFirst("Content-length")?.toIntOrNull() ?: -1
                        val bytes = it.requestBody.readBytes()
                        if (!discardWrites) documents[path] = Blob(bytes, "\"${contentHash(bytes.toString(Charsets.UTF_8))}\"")
                        reply(it, 201)
                    }
                    "GET" -> {
                        val blob = documents[path] ?: return reply(it, 404)
                        it.responseHeaders.set("ETag", blob.etag)
                        reply(it, 200, blob.bytes)
                    }
                    else -> reply(it, 405)
                }
            }
        }

        private fun reply(exchange: HttpExchange, status: Int, bytes: ByteArray = byteArrayOf()) {
            exchange.sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
            if (bytes.isNotEmpty()) exchange.responseBody.write(bytes)
        }
        override fun close() = server.stop(0)
    }

    companion object {
        private val FILES = listOf("github_tracked.json", "ba_accounts.json", "ba_catalog_favorites.json", "ba_bgm_favorites.json", "os_activity_cards.json", "os_shell_cards.json")
    }
}
