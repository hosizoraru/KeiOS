package os.kei.feature.webdav.client

import at.bitfire.dav4jvm.ktor.DavCollection
import at.bitfire.dav4jvm.ktor.MultiStatusItem
import at.bitfire.dav4jvm.ktor.Response
import at.bitfire.dav4jvm.property.webdav.WebDAV
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test
import os.kei.feature.webdav.model.WebDavRemoteFile

class WebDavPropfindEncodingTest {
    @Test
    fun `relative hrefs preserve server encoding through PROPFIND and file mapping`() = runBlocking {
        assertEncodedMembers { it }
    }

    @Test
    fun `absolute path hrefs preserve server encoding through PROPFIND and file mapping`() = runBlocking {
        assertEncodedMembers { "$COLLECTION_PATH$it" }
    }

    @Test
    fun `absolute URL hrefs preserve server encoding through PROPFIND and file mapping`() = runBlocking {
        assertEncodedMembers { "$COLLECTION_URL$it" }
    }

    private suspend fun assertEncodedMembers(serverHref: (String) -> String) {
        val entries = listOf(
            "lower%2b.json" to "lower+.json",
            "upper%2B.json" to "upper+.json",
            "space%20name.json" to "space name.json",
            "literal+plus.json" to "literal+plus.json",
            "percent%25.json" to "percent%.json",
            "escaped%2520.json" to "escaped%20.json",
            "%73napshot.json" to "snapshot.json",
            "%e4%b8%ad%E6%96%87.json" to "\u4e2d\u6587.json",
        )
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <d:multistatus xmlns:d="DAV:">
                ${responseXml(COLLECTION_PATH.trimEnd('/'), collection = true)}
                ${responseXml(serverHref("child%2b%20+%25"), collection = true)}
                ${entries.joinToString("\n") { (encodedName, _) -> responseXml(serverHref(encodedName)) }}
            </d:multistatus>
        """.trimIndent().trim()
        var requests = 0
        HttpClient(
            MockEngine { request ->
                requests++
                assertEquals("PROPFIND", request.method.value)
                assertEquals(COLLECTION_URL, request.url.toString())
                assertEquals("1", request.headers[HttpHeaders.Depth])
                respond(
                    content = xml,
                    status = HttpStatusCode.MultiStatus,
                    headers = headersOf(HttpHeaders.ContentType, "application/xml; charset=UTF-8"),
                )
            },
        ).use { client ->
            val items = DavCollection(client, Url(COLLECTION_URL)).propfind(
                1,
                WebDAV.GetETag,
                WebDAV.GetLastModified,
                WebDAV.GetContentLength,
                WebDAV.ResourceType,
            ).toList()
            val responses = items.filterIsInstance<MultiStatusItem.Response>()
            assertEquals(entries.size + 2, responses.size)
            assertEquals(Response.HrefRelation.SELF, responses[0].relation)
            assertEquals(COLLECTION_URL, responses[0].response.href.toString())
            assertNull(responses[0].toWebDavRemoteFileOrNull())
            assertEquals(Response.HrefRelation.MEMBER, responses[1].relation)
            assertEquals(
                "${COLLECTION_URL}child%2b%20+%25/",
                responses[1].response.href.toString(),
            )
            assertNull(responses[1].toWebDavRemoteFileOrNull())

            entries.zip(responses.drop(2)).forEach { (entry, item) ->
                val (encodedName, displayName) = entry
                assertEquals(Response.HrefRelation.MEMBER, item.relation)
                assertEquals("$COLLECTION_URL$encodedName", item.response.href.toString())
                assertEquals(
                    WebDavRemoteFile(
                        href = "$COLLECTION_URL$encodedName",
                        displayName = displayName,
                        lastModified = "2026-10-23T00:00:00Z",
                        contentLength = 42L,
                        etag = "\"etag-42\"",
                    ),
                    assertNotNull(item.toWebDavRemoteFileOrNull()),
                )
            }
            assertEquals(entries.size, items.mapNotNull { it.toWebDavRemoteFileOrNull() }.size)
        }
        assertEquals(1, requests)
    }

    private fun responseXml(href: String, collection: Boolean = false): String = """
        <d:response>
            <d:href>$href</d:href>
            <d:propstat>
                <d:prop>
                    <d:resourcetype>${if (collection) "<d:collection/>" else ""}</d:resourcetype>
                    <d:getetag>"etag-42"</d:getetag>
                    <d:getlastmodified>Fri, 23 Oct 2026 00:00:00 GMT</d:getlastmodified>
                    <d:getcontentlength>42</d:getcontentlength>
                </d:prop>
                <d:status>HTTP/1.1 200 OK</d:status>
            </d:propstat>
        </d:response>
    """.trimIndent()

    private companion object {
        const val COLLECTION_PATH = "/dav/Kei%4fS%20+%25/"
        const val COLLECTION_URL = "https://dav.example.test$COLLECTION_PATH"
    }
}
