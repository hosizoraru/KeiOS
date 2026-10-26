package os.kei.feature.webdav.client

sealed interface WebDavTestConnectionResult {
    data class Success(val dirCreated: Boolean) : WebDavTestConnectionResult
    data object AuthFailed : WebDavTestConnectionResult
    data object PermissionDenied : WebDavTestConnectionResult
    data class NetworkError(val message: String) : WebDavTestConnectionResult
    data class InvalidUrl(val message: String) : WebDavTestConnectionResult
    data class Error(val message: String) : WebDavTestConnectionResult
}

sealed interface WebDavUploadResult {
    data class Success(val etag: String?) : WebDavUploadResult
    data object Conflict : WebDavUploadResult
    data class Error(val error: WebDavError) : WebDavUploadResult
}

sealed interface WebDavDownloadResult {
    data class Success(val content: String, val etag: String?) : WebDavDownloadResult
    data object Empty : WebDavDownloadResult
    data class Error(val error: WebDavError) : WebDavDownloadResult
}

sealed interface WebDavError {
    data object NetworkUnreachable : WebDavError
    data object AuthFailed : WebDavError
    data object PermissionDenied : WebDavError
    data class Unknown(val code: Int, val message: String) : WebDavError
}

interface WebDavRemoteClient {
    suspend fun testConnection(): WebDavTestConnectionResult
    suspend fun upload(fileName: String, content: String, etag: String? = null): WebDavUploadResult
    suspend fun uploadIfAbsent(fileName: String, content: String): WebDavUploadResult
    suspend fun download(fileName: String): WebDavDownloadResult
    fun close() = Unit
}
