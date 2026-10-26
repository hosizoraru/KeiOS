package os.kei.feature.webdav.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import os.kei.feature.webdav.model.WebDavConfig
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class WebDavAuthTest {
    @Test
    fun `digest challenge does not poison the next preemptive request`() = runBlocking {
        val authorizationHeaders = mutableListOf<List<String>>()
        var requestIndex = 0
        val client = webDavAuthTestClient(
            MockEngine { request ->
                authorizationHeaders += request.headers.getAll(HttpHeaders.Authorization).orEmpty()
                requestIndex += 1
                when (requestIndex) {
                    1 -> respond(
                        content = "",
                        status = HttpStatusCode.Unauthorized,
                        headers = headersOf(
                            HttpHeaders.WWWAuthenticate,
                            "Digest realm=\"KeiOS\", nonce=\"first\", algorithm=MD5",
                        ),
                    )
                    else -> respond("ok", HttpStatusCode.OK)
                }
            },
        )

        client.use {
            assertEquals(HttpStatusCode.OK, it.get("https://dav.example.test/KeiOS/first").status)
            assertEquals(HttpStatusCode.OK, it.get("https://dav.example.test/KeiOS/second").status)
        }

        assertTrue(authorizationHeaders[0].any { it.startsWith("Basic ") })
        assertTrue(authorizationHeaders[1].any { it.startsWith("Digest ") })
        assertTrue(authorizationHeaders[2].any { it.startsWith("Basic ") })
        assertTrue(authorizationHeaders[2].none { it.startsWith("Digest ") })
    }

    @Test
    fun `basic credentials stay on the configured host`() = runBlocking {
        val authorizationHeaders = mutableListOf<String?>()
        val client = webDavAuthTestClient(
            MockEngine { request ->
                authorizationHeaders += request.headers[HttpHeaders.Authorization]
                respond("ok", HttpStatusCode.OK)
            },
        )

        client.use {
            it.get("https://dav.example.test/KeiOS/data")
            it.get("https://redirect.example.test/KeiOS/data")
        }

        assertTrue(authorizationHeaders[0]?.startsWith("Basic ") == true)
        assertNull(authorizationHeaders[1])
    }

    @Test
    fun `credentials never follow a scheme downgrade or a different port`() = runBlocking {
        val headers = mutableListOf<String?>()
        val client = webDavAuthTestClient(MockEngine { request ->
            headers += request.headers[HttpHeaders.Authorization]
            respond("ok")
        })
        client.use {
            it.get("http://dav.example.test/KeiOS/data")
            it.get("https://dav.example.test:8443/KeiOS/data")
        }
        assertEquals(listOf<String?>(null, null), headers)
    }

    private fun webDavAuthTestClient(engine: MockEngine): HttpClient =
        HttpClient(engine) {
            install(Auth) {
                configureWebDavAuth(TEST_CONFIG, "dav.example.test")
            }
        }

    private companion object {
        val TEST_CONFIG = WebDavConfig(
            serverUrl = "https://dav.example.test/dav/",
            username = "user@example.test",
            appPassword = "app-password",
            remoteDir = "KeiOS/",
        )
    }
}
