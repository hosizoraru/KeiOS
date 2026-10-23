package os.kei.feature.github.data.apk

import java.net.InetAddress
import java.net.Proxy
import kotlinx.coroutines.runBlocking
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import os.kei.core.download.range.RemoteByteRangeResourceChangedException
import os.kei.feature.github.data.apk.ZipRangeTestFixtures.rangeDispatcher
import os.kei.feature.github.data.apk.ZipRangeTestFixtures.rangeResponse
import os.kei.feature.github.data.apk.ZipRangeTestFixtures.zipWithEntries
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class RemoteZipEntryReaderTest {
    @Test
    fun `reader rejects a zip that changes after probing`() = runBlocking {
        val zipBytes = zipWithEntries("AndroidManifest.xml" to "manifest-content".encodeToByteArray())
        MockWebServer().use { server ->
            var requestIndex = 0
            server.dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse {
                        requestIndex += 1
                        val response = rangeResponse(request, zipBytes)
                        return response.addHeader("ETag", if (requestIndex == 1) "\"v1\"" else "\"v2\"")
                    }
                }
            val reader = RemoteZipEntryReader(OkHttpClient())

            val failure = reader.listEntryNames(server.url("/changing.apk").toString()).exceptionOrNull()

            assertIs<RemoteByteRangeResourceChangedException>(failure)
            assertEquals(null, server.takeRequest().getHeader("If-Range"))
            assertEquals("\"v1\"", server.takeRequest().getHeader("If-Range"))
        }
    }

    @Test
    fun `reader strips github token after a cross host redirect`() = runBlocking {
        val zipBytes = zipWithEntries("AndroidManifest.xml" to "manifest-content".encodeToByteArray())
        MockWebServer().use { origin ->
            MockWebServer().use { asset ->
                asset.dispatcher = rangeDispatcher(zipBytes)
                origin.enqueue(
                    MockResponse()
                        .setResponseCode(302)
                        .addHeader("Location", asset.url("/release.apk")),
                )
                val localhost = InetAddress.getByName("127.0.0.1")
                val client =
                    OkHttpClient.Builder()
                        .proxy(Proxy.NO_PROXY)
                        .dns(
                            Dns { hostname ->
                                if (hostname == "github.com") listOf(localhost) else Dns.SYSTEM.lookup(hostname)
                            },
                        )
                        .build()
                val reader = RemoteZipEntryReader(client)
                val url = "http://github.com:${origin.port}/release.apk"

                val names = reader.listEntryNames(url, apiToken = "secret-token").getOrThrow()

                assertEquals(listOf("AndroidManifest.xml"), names)
                assertEquals("Bearer secret-token", origin.takeRequest().getHeader("Authorization"))
                repeat(asset.requestCount) {
                    assertNull(asset.takeRequest().getHeader("Authorization"))
                }
            }
        }
    }
}
