package os.kei.mcp.server

import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.util.Locale

internal const val DEFAULT_TOPINFO_LIMIT = 120
internal const val MAX_TOPINFO_LIMIT = 300
internal const val DEFAULT_LOG_LIMIT = 80
internal const val MAX_LOG_LIMIT = 200
internal const val DEFAULT_TRACK_LIMIT = 80
internal const val MAX_TRACK_LIMIT = 400
internal const val DEFAULT_ENTRY_LIMIT = 12
internal const val MAX_ENTRY_LIMIT = 200

internal const val MIME_MARKDOWN = "text/markdown"
internal const val MIME_TEXT = "text/plain"
internal const val MIME_JSON = "application/json"

internal const val SKILL_RESOURCE_URI = "keios://skill/keios-mcp.md"
internal const val SKILL_OVERVIEW_URI = "keios://skill/overview.txt"
internal const val SKILL_TOOL_TEMPLATE_URI = "keios://skill/tool/{tool}"
internal const val CONFIG_RESOURCE_URI = "keios://mcp/config/default.json"
internal const val CONFIG_TEMPLATE_URI = "keios://mcp/config/{mode}.json"
internal const val BOOTSTRAP_PROMPT = "keios.mcp.bootstrap"
internal const val DEFAULT_ENDPOINT = "http://127.0.0.1:38888/mcp"

internal fun callText(text: String): CallToolResult {
    return CallToolResult(content = listOf(TextContent(text)))
}

internal fun callResource(uri: String, mimeType: String, text: String): ReadResourceResult {
    return ReadResourceResult(
        contents = listOf(
            TextResourceContents(
                uri = uri,
                mimeType = mimeType,
                text = text
            )
        )
    )
}

internal fun jsonEscape(raw: String): String {
    return raw
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
}

internal fun argString(value: Any?): String {
    return (value as? JsonPrimitive)?.contentOrNull.orEmpty()
}

internal fun argInt(value: Any?, defaultValue: Int): Int {
    return argString(value).trim().toIntOrNull() ?: defaultValue
}

internal fun argIntOrNull(value: Any?): Int? {
    return argString(value).trim().toIntOrNull()
}

internal fun argBoolean(value: Any?, defaultValue: Boolean): Boolean {
    val raw = argString(value).trim().lowercase(Locale.ROOT)
    return when (raw) {
        "1", "true", "yes", "y", "on" -> true
        "0", "false", "no", "n", "off" -> false
        else -> defaultValue
    }
}

internal fun normalizeMcpConfigMode(raw: String): String {
    return when (raw.trim().lowercase(Locale.ROOT)) {
        "local" -> "local"
        "lan" -> "lan"
        else -> "auto"
    }
}
