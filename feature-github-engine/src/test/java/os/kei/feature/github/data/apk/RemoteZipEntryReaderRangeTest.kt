package os.kei.feature.github.data.apk

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Test
import os.kei.core.download.range.RemoteByteRangeResourceChangedException
import os.kei.feature.github.data.apk.ZipRangeTestFixtures.rangeDispatcher
import os.kei.feature.github.data.apk.ZipRangeTestFixtures.zipWithEntries
import os.kei.feature.github.data.apk.ZipRangeTestFixtures.zipWithStoredEntry
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RemoteZipEntryReaderRangeTest {
    @Test
    fun `reader fetches AndroidManifest entry through explicit byte ranges`() = runBlocking {
        val manifest = "manifest-content".encodeToByteArray()
        val apkBytes = zipWithEntries("AndroidManifest.xml" to manifest)
        MockWebServer().use { server ->
            server.dispatcher = rangeDispatcher(apkBytes)
            val reader = RemoteZipEntryReader(client = OkHttpClient())

            val bytes = reader.readEntry(
                url = server.url("/download/app.apk").toString(),
                entryName = "AndroidManifest.xml"
            ).getOrThrow()
            assertContentEquals(manifest, bytes)
            val ranges = buildList {
                repeat(server.requestCount) {
                    val request = server.takeRequest()
                    assertEquals("no-store", request.getHeader("Cache-Control"))
                    assertEquals("no-cache", request.getHeader("Pragma"))
                    add(request.getHeader("Range").orEmpty())
                }
            }
            assertEquals("bytes=0-0", ranges.first())
            assertTrue(ranges.size <= 4)
            assertTrue(ranges.all { it.matches(Regex("""bytes=\d+-\d+""")) })
        }
    }

    @Test
    fun `reader lists entries and reads selected entries from one central directory`() = runBlocking {
        val manifest = "selected-manifest-content".encodeToByteArray()
        val apkBytes = zipWithEntries("AndroidManifest.xml" to manifest)
        MockWebServer().use { server ->
            server.dispatcher = rangeDispatcher(apkBytes)
            val reader = RemoteZipEntryReader(client = OkHttpClient())

            val selected = reader.readSelectedEntries(
                url = server.url("/download/app.apk").toString(),
                selectEntryNames = { entryNames ->
                    entryNames.filter { it == "AndroidManifest.xml" }
                }
            ).getOrThrow()
            assertContentEquals(manifest, selected.entries.getValue("AndroidManifest.xml"))
            assertEquals(listOf("AndroidManifest.xml"), selected.entryNames)
            assertTrue(server.requestCount <= 4)
        }
    }

    @Test
    fun `reader fails without downloading full body when range is ignored`() = runBlocking {
        val apkBytes = zipWithEntries("AndroidManifest.xml" to "manifest-content".encodeToByteArray())
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(okio.Buffer().write(apkBytes))
            )
            val reader = RemoteZipEntryReader(client = OkHttpClient())

            val result = reader.readEntry(
                url = server.url("/download/app.apk").toString(),
                entryName = "AndroidManifest.xml"
            )

            assertIs<RemoteByteRangeResourceChangedException>(result.exceptionOrNull())
            assertEquals(1, server.requestCount)
            assertEquals("bytes=0-0", server.takeRequest().getHeader("Range"))
        }
    }

    @Test
    fun `reader reuses redirected range url after probe`() = runBlocking {
        val manifest = "redirected-manifest-content".encodeToByteArray()
        val apkBytes = zipWithEntries("AndroidManifest.xml" to manifest)
        MockWebServer().use { server ->
            server.dispatcher = object : Dispatcher() {
                private val assetDispatcher = rangeDispatcher(apkBytes)

                override fun dispatch(request: RecordedRequest): MockResponse {
                    return if (request.path?.startsWith("/download/") == true) {
                        MockResponse()
                            .setResponseCode(302)
                            .addHeader("Location", server.url("/asset/app.apk"))
                    } else {
                        assetDispatcher.dispatch(request)
                    }
                }
            }
            val reader = RemoteZipEntryReader(client = OkHttpClient())

            val bytes = reader.readEntry(
                url = server.url("/download/app.apk").toString(),
                entryName = "AndroidManifest.xml"
            ).getOrThrow()
            assertContentEquals(manifest, bytes)
            val paths = buildList {
                repeat(server.requestCount) {
                    add(server.takeRequest().path.orEmpty())
                }
            }
            assertEquals(1, paths.count { it.startsWith("/download/") })
            assertTrue(paths.drop(1).all { it.startsWith("/asset/") })
        }
    }

    @Test
    fun `reader fetches manifest from nested stored apk entry through ranges`() = runBlocking {
        val manifest = "nested-manifest-content".encodeToByteArray()
        val apkBytes = zipWithEntries("AndroidManifest.xml" to manifest)
        val artifactBytes = zipWithStoredEntry(
            entryName = "outputs/app-universal-release.apk",
            bytes = apkBytes
        )
        MockWebServer().use { server ->
            server.dispatcher = rangeDispatcher(artifactBytes)
            val reader = RemoteZipEntryReader(client = OkHttpClient())

            val bytes = reader.readNestedStoredZipEntry(
                url = server.url("/download/artifact.zip").toString(),
                outerEntryName = "outputs/app-universal-release.apk",
                innerEntryName = "AndroidManifest.xml"
            ).getOrThrow()
            assertContentEquals(manifest, bytes)
            val ranges = buildList {
                repeat(server.requestCount) {
                    add(server.takeRequest().getHeader("Range").orEmpty())
                }
            }
            assertEquals("bytes=0-0", ranges.first())
            assertTrue(ranges.none { it.startsWith("bytes=-") })
            assertTrue(ranges.all { it.matches(Regex("""bytes=\d+-\d+""")) })
        }
    }

}
