package os.kei.feature.webdav.client

import io.ktor.http.Url
import java.net.URI
import os.kei.feature.webdav.model.WebDavConfig

internal class WebDavEndpoint(config: WebDavConfig) {
    val base: Url
    val directories: List<Url>
    val collection: Url

    init {
        val uri = URI(config.serverUrl.trim())
        require(uri.scheme in setOf("https", "http") && !uri.host.isNullOrBlank()) {
            "A WebDAV server must be an HTTP or HTTPS URL"
        }
        require(uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null) {
            "Use the account fields for credentials and a server URL without query or fragment"
        }
        val path = uri.rawPath.orEmpty().trimEnd('/') + "/"
        base = Url("${uri.scheme}://${uri.rawAuthority}$path")
        val segments = config.remoteDir.trim().trim('/').split('/').filter(String::isNotEmpty)
        require(segments.none { it == "." || it == ".." || '\\' in it || it.any(Char::isISOControl) }) {
            "Invalid WebDAV directory"
        }
        var prefix = base.toString()
        directories = segments.map { segment ->
            prefix += encodeSegment(segment) + "/"
            Url(prefix)
        }
        collection = directories.lastOrNull() ?: base
    }

    fun file(name: String): Url {
        require(name.isNotBlank() && name != "." && name != ".." &&
            name.none { it == '/' || it == '\\' || it.isISOControl() }) {
            "A backup file must have a single file name"
        }
        return Url(collection.toString() + encodeSegment(name))
    }

    fun redirect(from: Url, location: String): Url {
        val target = Url(URI(from.toString()).resolve(location).toASCIIString())
        require(sameOrigin(base, target)) { "WebDAV redirected outside the configured server origin" }
        return target
    }

    private fun encodeSegment(value: String): String =
        URI(null, null, "/$value", null).toASCIIString().removePrefix("/")
            .replace("+", "%2B")
}

internal fun sameOrigin(first: Url, second: Url): Boolean =
    first.protocol == second.protocol && first.host.equals(second.host, ignoreCase = true) &&
        first.port == second.port
