package os.kei.feature.webdav.model

/**
 * WebDAV server configuration.
 */
data class WebDavConfig(
    val serverUrl: String,
    val username: String,
    val appPassword: String,
    val remoteDir: String = "keios/",
)

/**
 * Metadata for a remote file discovered via PROPFIND.
 */
data class WebDavRemoteFile(
    val href: String,
    val displayName: String,
    val lastModified: String?,
    val contentLength: Long,
    val etag: String?,
)

/** Password rotation keeps a baseline; moving the account, server or directory does not. */
fun WebDavConfig.workspaceIdentity(): String {
    val endpoint = os.kei.feature.webdav.client.WebDavEndpoint(this)
    return os.kei.feature.webdav.sync.contentHash("${endpoint.collection}\n${username.trim()}")
}
