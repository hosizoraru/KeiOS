package os.kei.ui.page.main.os.transfer

import android.content.Context
import androidx.annotation.StringRes
import org.json.JSONArray
import org.json.JSONObject
import os.kei.R
import os.kei.ui.page.main.os.state.OsCardImportTarget
import os.kei.ui.page.main.os.shell.OsShellCommandCard
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCard

internal const val OS_ACTIVITY_CARD_EXPORT_SCHEMA = "keios.os.activity.cards.v1"
internal const val OS_SHELL_CARD_EXPORT_SCHEMA = "keios.os.shell.cards.v1"

internal enum class OsCardImportFileKind {
    Activity,
    Shell,
    Unknown
}

internal enum class OsCardImportError(@param:StringRes val messageRes: Int, val code: String) {
    EmptyFile(R.string.os_import_error_empty_file, "OS_CARD_IMPORT_EMPTY_FILE"),
    MissingData(R.string.os_import_error_missing_data, "OS_CARD_IMPORT_MISSING_DATA"),
    NoImportableData(R.string.os_import_error_no_importable_data, "OS_CARD_IMPORT_NO_IMPORTABLE_DATA"),
    NoValidActivityCards(R.string.os_import_error_no_valid_activity_cards, "OS_CARD_IMPORT_NO_VALID_ACTIVITY_CARDS"),
    NoValidShellCards(R.string.os_import_error_no_valid_shell_cards, "OS_CARD_IMPORT_NO_VALID_SHELL_CARDS")
}

internal class OsCardImportException(
    val importError: OsCardImportError
) : IllegalArgumentException(importError.code)

internal fun Throwable.localizedOsCardImportMessage(context: Context): String {
    val error = this as? OsCardImportException
    return if (error != null) {
        context.getString(error.importError.messageRes)
    } else {
        message ?: javaClass.simpleName
    }
}

internal data class OsCardImportRoot(
    val items: JSONArray,
    val sourceCount: Int,
    val fileKind: OsCardImportFileKind,
    val isLegacyFormat: Boolean
)

internal sealed interface OsCardImportPayload {
    val sourceCount: Int
    val invalidCount: Int
    val duplicateCount: Int
    val fileKind: OsCardImportFileKind
    val isLegacyFormat: Boolean
}

internal data class OsActivityCardImportPayload(
    val cards: List<OsActivityShortcutCard>,
    override val sourceCount: Int,
    override val invalidCount: Int,
    override val duplicateCount: Int,
    override val fileKind: OsCardImportFileKind,
    override val isLegacyFormat: Boolean
) : OsCardImportPayload

internal data class OsShellCardImportPayload(
    val cards: List<OsShellCommandCard>,
    override val sourceCount: Int,
    override val invalidCount: Int,
    override val duplicateCount: Int,
    override val fileKind: OsCardImportFileKind,
    override val isLegacyFormat: Boolean
) : OsCardImportPayload

internal data class OsUnknownCardImportPayload(
    override val sourceCount: Int,
    override val invalidCount: Int,
    override val duplicateCount: Int,
    override val fileKind: OsCardImportFileKind = OsCardImportFileKind.Unknown,
    override val isLegacyFormat: Boolean = false
) : OsCardImportPayload

internal data class OsCardImportPreview(
    val target: OsCardImportTarget,
    val payload: OsCardImportPayload,
    val fileItemCount: Int,
    val validCount: Int,
    val duplicateCount: Int,
    val invalidCount: Int,
    val newCount: Int,
    val updatedCount: Int,
    val unchangedCount: Int,
    val mergedCount: Int
) {
    val canImport: Boolean
        get() = validCount > 0 && when (target) {
            OsCardImportTarget.Activity -> payload is OsActivityCardImportPayload
            OsCardImportTarget.Shell -> payload is OsShellCardImportPayload
        }

    val isWrongTarget: Boolean
        get() = validCount > 0 && !canImport && payload.fileKind != OsCardImportFileKind.Unknown
}

internal fun OsCardImportTarget.expectedFileKind(): OsCardImportFileKind {
    return when (this) {
        OsCardImportTarget.Activity -> OsCardImportFileKind.Activity
        OsCardImportTarget.Shell -> OsCardImportFileKind.Shell
    }
}

internal fun parseOsCardImportRoot(raw: String): OsCardImportRoot {
    val normalizedRaw = raw.trim()
    if (normalizedRaw.isBlank()) {
        throw OsCardImportException(OsCardImportError.EmptyFile)
    }
    if (normalizedRaw.startsWith("[")) {
        val items = JSONArray(normalizedRaw)
        return OsCardImportRoot(
            items = items,
            sourceCount = items.length(),
            fileKind = detectCardImportFileKind(items),
            isLegacyFormat = true
        )
    }
    val root = JSONObject(normalizedRaw)
    val items = root.optJSONArray("items")
        ?: throw OsCardImportException(OsCardImportError.MissingData)
    val declaredSchema = root.optString("schema").trim().ifBlank {
        root.optString("format").trim()
    }
    val declaredKind = when (declaredSchema) {
        OS_ACTIVITY_CARD_EXPORT_SCHEMA -> OsCardImportFileKind.Activity
        OS_SHELL_CARD_EXPORT_SCHEMA -> OsCardImportFileKind.Shell
        else -> OsCardImportFileKind.Unknown
    }
    return OsCardImportRoot(
        items = items,
        sourceCount = if (root.has("itemCount")) {
            root.optInt("itemCount", items.length()).coerceAtLeast(0)
        } else {
            items.length()
        },
        fileKind = if (declaredKind != OsCardImportFileKind.Unknown) {
            declaredKind
        } else {
            detectCardImportFileKind(items)
        },
        isLegacyFormat = declaredSchema.isBlank()
    )
}

private fun detectCardImportFileKind(items: JSONArray): OsCardImportFileKind {
    var activityScore = 0
    var shellScore = 0
    val inspectedCount = minOf(items.length(), 12)
    repeat(inspectedCount) { index ->
        val item = items.optJSONObject(index) ?: return@repeat
        ACTIVITY_SIGNATURE_KEYS.forEach { key ->
            if (item.has(key)) activityScore += 1
        }
        SHELL_SIGNATURE_KEYS.forEach { key ->
            if (item.has(key)) shellScore += 1
        }
    }
    return when {
        activityScore == 0 && shellScore == 0 -> OsCardImportFileKind.Unknown
        activityScore >= shellScore -> OsCardImportFileKind.Activity
        else -> OsCardImportFileKind.Shell
    }
}

private val ACTIVITY_SIGNATURE_KEYS = setOf(
    "appName",
    "packageName",
    "className",
    "intentAction",
    "intentCategory",
    "intentFlags",
    "intentUriData",
    "intentMimeType",
    "intentExtras"
)

private val SHELL_SIGNATURE_KEYS = setOf(
    "command",
    "runOutput",
    "lastRunAtMillis",
    "createdAtMillis",
    "updatedAtMillis"
)
