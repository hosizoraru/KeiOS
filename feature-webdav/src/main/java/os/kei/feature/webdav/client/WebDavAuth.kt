package os.kei.feature.webdav.client

import io.ktor.client.plugins.auth.AuthConfig
import io.ktor.client.plugins.auth.AuthProvider
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.BasicAuthProvider
import io.ktor.client.plugins.auth.providers.DigestAuthCredentials
import io.ktor.client.plugins.auth.providers.DigestAuthProvider
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.http.Url
import os.kei.feature.webdav.model.WebDavConfig

internal fun AuthConfig.configureWebDavAuth(
    config: WebDavConfig,
    credentialHost: String,
) {
    providers += HostScopedAuthProvider(
        credentialHost = credentialHost,
        origin = Url(config.serverUrl),
        delegate = BasicAuthProvider(
            credentials = {
                BasicAuthCredentials(
                    username = config.username,
                    password = config.appPassword,
                )
            },
            sendWithoutRequestCallback = { true },
        ),
    )
    providers += HostScopedAuthProvider(
        credentialHost = credentialHost,
        origin = Url(config.serverUrl),
        delegate = DigestAuthProvider(
            credentials = {
                DigestAuthCredentials(
                    username = config.username,
                    password = config.appPassword,
                )
            },
        ),
    )
}

private class HostScopedAuthProvider(
    private val credentialHost: String,
    private val origin: Url,
    private val delegate: AuthProvider,
) : AuthProvider {
    @Suppress("OVERRIDE_DEPRECATION")
    @Deprecated("Use sendWithoutRequest(request)", level = DeprecationLevel.ERROR)
    override val sendWithoutRequest: Boolean
        get() = false

    override fun sendWithoutRequest(request: HttpRequestBuilder): Boolean =
        matches(request) && delegate.sendWithoutRequest(request)

    override fun isApplicable(auth: HttpAuthHeader): Boolean = delegate.isApplicable(auth)

    override suspend fun addRequestHeaders(
        request: HttpRequestBuilder,
        authHeader: HttpAuthHeader?,
    ) {
        if (matches(request)) {
            delegate.addRequestHeaders(request, authHeader)
        }
    }

    override suspend fun refreshToken(response: HttpResponse): Boolean = delegate.refreshToken(response)

    override fun clearToken() {
        delegate.clearToken()
    }

    private fun matches(request: HttpRequestBuilder): Boolean =
        request.url.host.equals(credentialHost, ignoreCase = true) &&
            request.url.protocol == origin.protocol && request.url.build().port == origin.port
}
