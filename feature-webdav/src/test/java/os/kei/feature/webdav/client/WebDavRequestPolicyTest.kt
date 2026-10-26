package os.kei.feature.webdav.client

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.runBlocking
import org.junit.Test
import os.kei.feature.webdav.model.WebDavConfig
import os.kei.feature.webdav.model.workspaceIdentity
import kotlin.test.*

class WebDavRequestPolicyTest {
    @Test fun `Nutstore pacing and local budget are bounded across the half hour`(): Unit = runBlocking {
        var now = 1_000L
        val waits = mutableListOf<Long>()
        val policy = WebDavRequestPolicy(1_000, nowMs = { now }, wait = { waits += it; now += it })
        repeat(120) { policy.acquire() }
        assertEquals(119, waits.size)
        assertTrue(waits.all { it == 1_000L })
        assertEquals(429, assertIs<WebDavError.Unknown>(assertFailsWith<WebDavRequestException> { policy.acquire() }.error).code)
        now = 30 * 60 * 1_000 + 1_001L
        policy.acquire()
    }

    @Test fun `HTTP date Retry-After blocks without sleeping and expires at its deadline`(): Unit = runBlocking {
        var now = 1_800_000_000_000L
        var requestsWaited = 0
        val policy = WebDavRequestPolicy(nowMs = { now }, wait = { requestsWaited++ })
        policy.coolDown(DateTimeFormatter.RFC_1123_DATE_TIME.format(Instant.ofEpochMilli(now + 120_000).atZone(ZoneOffset.UTC)))
        assertFailsWith<WebDavRequestException> { policy.acquire() }
        assertEquals(0, requestsWaited)
        now += 120_001
        policy.acquire()
    }

    @Test fun `workspace identity separates account path and server but tolerates password rotation`() {
        val config = WebDavConfig("https://example.test/dav", "user", "old", "KeiOS/")
        assertEquals(config.workspaceIdentity(), config.copy(appPassword = "rotated").workspaceIdentity())
        assertEquals(config.workspaceIdentity(), config.copy(serverUrl = "https://example.test/dav/", remoteDir = "/KeiOS/").workspaceIdentity())
        assertNotEquals(config.workspaceIdentity(), config.copy(username = "someone-else").workspaceIdentity())
        assertNotEquals(config.workspaceIdentity(), config.copy(remoteDir = "Another/").workspaceIdentity())
        assertNotEquals(config.workspaceIdentity(), config.copy(serverUrl = "https://other.test/dav/").workspaceIdentity())
        for (invalid in listOf("file:///tmp/data", "https://user:secret@example.test", "https://example.test/?q=data")) {
            assertFails { config.copy(serverUrl = invalid).workspaceIdentity() }
        }
        assertFails { config.copy(remoteDir = "../other/").workspaceIdentity() }
    }

    @Test fun `explicit connection testing can retry a previously rejected credential`(): Unit = runBlocking {
        val policy = WebDavRequestPolicy()
        policy.rejectCredentials("credential-hash")
        assertEquals(WebDavError.AuthFailed, assertFailsWith<WebDavRequestException> { policy.acquire("credential-hash") }.error)
        policy.retryCredentials("credential-hash")
        policy.acquire("credential-hash")
    }
}
