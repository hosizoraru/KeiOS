package com.example.keios.feature.github.data.remote

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GitHubAtomReleaseStrategyTest {
    @After
    fun tearDown() {
        GitHubAtomReleaseStrategy.clearCaches()
    }

    @Test
    fun `atom snapshot keeps stable redirect and prerelease entry`() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(sampleAtomFeedXml())
            )
            server.enqueue(
                MockResponse()
                    .setResponseCode(302)
                    .addHeader("Location", "https://github.com/demo/app/releases/tag/v1.1.0")
            )

            val trace = GitHubAtomReleaseStrategy.loadSnapshotTrace(
                owner = "demo",
                repo = "app",
                atomFeedUrl = server.url("/demo/app/releases.atom").toString(),
                latestReleaseUrl = server.url("/demo/app/releases/latest").toString()
            )
            val snapshot = trace.result.getOrThrow()

            assertFalse(trace.fromCache)
            assertEquals("v1.1.0", snapshot.latestStable.rawTag)
            assertNull(snapshot.latestPreRelease)
            assertEquals(2, snapshot.feed.entries.size)
        }
    }

    @Test
    fun `atom snapshot keeps forward prerelease when it outruns stable`() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                            <?xml version="1.0" encoding="utf-8"?>
                            <feed xmlns="http://www.w3.org/2005/Atom">
                              <title>demo/app releases</title>
                              <updated>2026-04-13T10:00:00Z</updated>
                              <entry>
                                <id>tag:github.com,2008:Repository/1/v1.4.7-prerelease3</id>
                                <updated>2026-04-13T09:00:00Z</updated>
                                <title>v1.4.7-prerelease3</title>
                                <link rel="alternate" href="https://github.com/demo/app/releases/tag/v1.4.7-prerelease3" />
                                <content type="html">Preview build</content>
                                <author><name>demo</name></author>
                              </entry>
                              <entry>
                                <id>tag:github.com,2008:Repository/1/v1.4.4-release</id>
                                <updated>2026-04-12T09:00:00Z</updated>
                                <title>v1.4.4-release</title>
                                <link rel="alternate" href="https://github.com/demo/app/releases/tag/v1.4.4-release" />
                                <content type="html">Stable build</content>
                                <author><name>demo</name></author>
                              </entry>
                            </feed>
                        """.trimIndent()
                    )
            )
            server.enqueue(
                MockResponse()
                    .setResponseCode(302)
                    .addHeader("Location", "https://github.com/demo/app/releases/tag/v1.4.4-release")
            )

            val trace = GitHubAtomReleaseStrategy.loadSnapshotTrace(
                owner = "demo",
                repo = "app",
                atomFeedUrl = server.url("/demo/app/releases.atom").toString(),
                latestReleaseUrl = server.url("/demo/app/releases/latest").toString()
            )
            val snapshot = trace.result.getOrThrow()

            assertFalse(trace.fromCache)
            assertEquals("v1.4.4-release", snapshot.latestStable.rawTag)
            assertEquals("v1.4.7-prerelease3", snapshot.latestPreRelease?.rawTag)
        }
    }


    @Test
    fun `atom snapshot falls back to newest prerelease when repo has no stable release yet`() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                            <?xml version="1.0" encoding="utf-8"?>
                            <feed xmlns="http://www.w3.org/2005/Atom">
                              <title>demo/app releases</title>
                              <updated>2026-04-13T10:00:00Z</updated>
                              <entry>
                                <id>tag:github.com,2008:Repository/1/0.0.8</id>
                                <updated>2026-04-13T10:28:20Z</updated>
                                <title>v0.0.8</title>
                                <link rel="alternate" href="https://github.com/demo/app/releases/tag/0.0.8" />
                                <content type="html">Preview build</content>
                                <author><name>demo</name></author>
                              </entry>
                              <entry>
                                <id>tag:github.com,2008:Repository/1/0.0.7</id>
                                <updated>2026-04-11T11:03:39Z</updated>
                                <title>v0.0.7</title>
                                <link rel="alternate" href="https://github.com/demo/app/releases/tag/0.0.7" />
                                <content type="html">Preview build</content>
                                <author><name>demo</name></author>
                              </entry>
                            </feed>
                        """.trimIndent()
                    )
            )
            server.enqueue(
                MockResponse()
                    .setResponseCode(404)
            )

            val trace = GitHubAtomReleaseStrategy.loadSnapshotTrace(
                owner = "demo",
                repo = "app",
                atomFeedUrl = server.url("/demo/app/releases.atom").toString(),
                latestReleaseUrl = server.url("/demo/app/releases/latest").toString()
            )
            val snapshot = trace.result.getOrThrow()

            assertFalse(trace.fromCache)
            assertEquals("0.0.8", snapshot.latestStable.rawTag)
            assertEquals("v0.0.8", snapshot.latestStable.rawName)
            assertNull(snapshot.latestPreRelease)
        }
    }

    @Test
    fun `second atom snapshot hits both caches`() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(sampleAtomFeedXml())
            )
            server.enqueue(
                MockResponse()
                    .setResponseCode(302)
                    .addHeader("Location", "https://github.com/demo/app/releases/tag/v1.1.0")
            )

            val atomFeedUrl = server.url("/demo/app/releases.atom").toString()
            val latestReleaseUrl = server.url("/demo/app/releases/latest").toString()

            val first = GitHubAtomReleaseStrategy.loadSnapshotTrace(
                owner = "demo",
                repo = "app",
                atomFeedUrl = atomFeedUrl,
                latestReleaseUrl = latestReleaseUrl
            )
            val second = GitHubAtomReleaseStrategy.loadSnapshotTrace(
                owner = "demo",
                repo = "app",
                atomFeedUrl = atomFeedUrl,
                latestReleaseUrl = latestReleaseUrl
            )

            assertTrue(first.result.isSuccess)
            assertTrue(second.result.isSuccess)
            assertFalse(first.fromCache)
            assertTrue(second.fromCache)
            assertEquals(2, server.requestCount)
        }
    }

    private fun sampleAtomFeedXml(): String {
        return """
            <?xml version="1.0" encoding="utf-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <title>demo/app releases</title>
              <updated>2026-04-13T10:00:00Z</updated>
              <entry>
                <id>tag:github.com,2008:Repository/1/v1.2.0-beta1</id>
                <updated>2026-04-13T09:00:00Z</updated>
                <title>Version 1.2.0 Beta 1</title>
                <link rel="alternate" href="https://github.com/demo/app/releases/tag/v1.2.0-beta1" />
                <content type="html">Preview build</content>
                <author>
                  <name>demo</name>
                </author>
              </entry>
              <entry>
                <id>tag:github.com,2008:Repository/1/v1.1.0</id>
                <updated>2026-04-12T09:00:00Z</updated>
                <title>Version 1.1.0</title>
                <link rel="alternate" href="https://github.com/demo/app/releases/tag/v1.1.0" />
                <content type="html">Stable build</content>
                <author>
                  <name>demo</name>
                </author>
              </entry>
            </feed>
        """.trimIndent()
    }
}
