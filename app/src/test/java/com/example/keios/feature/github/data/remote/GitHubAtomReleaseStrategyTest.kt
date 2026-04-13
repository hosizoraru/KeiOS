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
            assertEquals("v1.2.0-beta1", snapshot.latestPreRelease?.rawTag)
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
    fun `atom latest redirect matches exact stable tag instead of newer alpha entry`() {
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
                                <id>tag:github.com,2008:Repository/1/Version.26.4.Alpha2_C384</id>
                                <updated>2026-04-13T09:00:00Z</updated>
                                <title>Version.26.4.Alpha2_C384</title>
                                <link rel="alternate" href="https://github.com/demo/app/releases/tag/Version.26.4.Alpha2_C384" />
                                <content type="html">Alpha preview build</content>
                                <author><name>demo</name></author>
                              </entry>
                              <entry>
                                <id>tag:github.com,2008:Repository/1/Canary.Version_C384</id>
                                <updated>2026-04-13T08:59:00Z</updated>
                                <title>Canary Build Version.26.4.Canary_C384</title>
                                <link rel="alternate" href="https://github.com/demo/app/releases/tag/Canary.Version_C384" />
                                <content type="html">Canary build</content>
                                <author><name>demo</name></author>
                              </entry>
                              <entry>
                                <id>tag:github.com,2008:Repository/1/Version.1.3.Fix2_C359</id>
                                <updated>2026-04-12T09:00:00Z</updated>
                                <title>Version.1.3.Fix2_C359</title>
                                <link rel="alternate" href="https://github.com/demo/app/releases/tag/Version.1.3.Fix2_C359" />
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
                    .addHeader("Location", "https://github.com/demo/app/releases/tag/Version.1.3.Fix2_C359")
            )

            val trace = GitHubAtomReleaseStrategy.loadSnapshotTrace(
                owner = "demo",
                repo = "app",
                atomFeedUrl = server.url("/demo/app/releases.atom").toString(),
                latestReleaseUrl = server.url("/demo/app/releases/latest").toString()
            )
            val snapshot = trace.result.getOrThrow()

            assertTrue(snapshot.hasStableRelease)
            assertEquals("Version.1.3.Fix2_C359", snapshot.latestStable.rawTag)
            assertEquals("Version.26.4.Alpha2_C384", snapshot.latestPreRelease?.rawTag)
        }
    }

    @Test
    fun `atom keeps rc prerelease visible when stable redirect points to same base final release`() {
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
                                <id>tag:github.com,2008:Repository/1/3.8.0</id>
                                <updated>2026-04-12T09:00:00Z</updated>
                                <title>3.8.0</title>
                                <link rel="alternate" href="https://github.com/demo/app/releases/tag/3.8.0" />
                                <content type="html">Full Changelog 3.7.2-alpha02...3.8.0</content>
                                <author><name>demo</name></author>
                              </entry>
                              <entry>
                                <id>tag:github.com,2008:Repository/1/3.8.0-rc04</id>
                                <updated>2026-04-13T09:00:00Z</updated>
                                <title>3.8.0-rc04</title>
                                <link rel="alternate" href="https://github.com/demo/app/releases/tag/3.8.0-rc04" />
                                <content type="html">Full Changelog 3.8.0-rc03...3.8.0-rc04</content>
                                <author><name>demo</name></author>
                              </entry>
                            </feed>
                        """.trimIndent()
                    )
            )
            server.enqueue(
                MockResponse()
                    .setResponseCode(302)
                    .addHeader("Location", "https://github.com/demo/app/releases/tag/3.8.0")
            )

            val trace = GitHubAtomReleaseStrategy.loadSnapshotTrace(
                owner = "demo",
                repo = "app",
                atomFeedUrl = server.url("/demo/app/releases.atom").toString(),
                latestReleaseUrl = server.url("/demo/app/releases/latest").toString()
            )
            val snapshot = trace.result.getOrThrow()

            assertTrue(snapshot.hasStableRelease)
            assertEquals("3.8.0", snapshot.latestStable.rawTag)
            assertEquals("3.8.0-rc04", snapshot.latestPreRelease?.rawTag)
        }
    }


    @Test
    fun `atom snapshot keeps prerelease only repos explicit instead of faking stable channel`() {
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
            assertFalse(snapshot.hasStableRelease)
            assertEquals("0.0.8", snapshot.latestStable.rawTag)
            assertEquals("v0.0.8", snapshot.latestStable.rawName)
            assertEquals("0.0.8", snapshot.latestPreRelease?.rawTag)
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
