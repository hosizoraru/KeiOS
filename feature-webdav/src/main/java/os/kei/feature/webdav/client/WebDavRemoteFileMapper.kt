package os.kei.feature.webdav.client

import at.bitfire.dav4jvm.ktor.MultiStatusItem
import at.bitfire.dav4jvm.ktor.Response
import at.bitfire.dav4jvm.property.webdav.GetContentLength
import at.bitfire.dav4jvm.property.webdav.GetETag
import at.bitfire.dav4jvm.property.webdav.GetLastModified
import at.bitfire.dav4jvm.property.webdav.ResourceType
import at.bitfire.dav4jvm.property.webdav.WebDAV
import os.kei.feature.webdav.model.WebDavRemoteFile

private fun Response.isCollection(): Boolean =
    this[ResourceType::class.java]
        ?.types
        ?.contains(WebDAV.Collection) == true

internal fun MultiStatusItem.toWebDavRemoteFileOrNull(): WebDavRemoteFile? {
    val responseItem = this as? MultiStatusItem.Response ?: return null
    val response = responseItem.response
    if (responseItem.relation != Response.HrefRelation.MEMBER || response.isCollection()) return null

    return WebDavRemoteFile(
        href = response.href.toString(),
        displayName = response.hrefName(),
        lastModified = response[GetLastModified::class.java]?.lastModified?.toString(),
        contentLength = response[GetContentLength::class.java]?.contentLength ?: 0L,
        etag = response[GetETag::class.java]?.rawETag,
    )
}
