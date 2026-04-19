package com.example.keios.ui.page.main

import com.example.keios.R
import java.util.Locale
import java.util.UUID

internal const val LEGACY_GOOGLE_SYSTEM_SERVICE_CARD_ID = "legacy-google-system-service"

internal enum class OsActivityCardEditMode {
    Add,
    Edit
}

internal data class OsActivityShortcutCard(
    val id: String,
    val visible: Boolean = true,
    val config: OsGoogleSystemServiceConfig
)

internal enum class ShortcutIntentExtraType(
    val rawValue: String,
    val labelResId: Int
) {
    String("string", R.string.os_google_system_service_intent_extra_type_string),
    Boolean("boolean", R.string.os_google_system_service_intent_extra_type_boolean),
    Int("int", R.string.os_google_system_service_intent_extra_type_int),
    Long("long", R.string.os_google_system_service_intent_extra_type_long),
    Float("float", R.string.os_google_system_service_intent_extra_type_float),
    Double("double", R.string.os_google_system_service_intent_extra_type_double),
    Uri("uri", R.string.os_google_system_service_intent_extra_type_uri);

    companion object {
        fun fromRaw(raw: String): ShortcutIntentExtraType {
            val normalized = raw.trim().lowercase(Locale.ROOT)
            return entries.firstOrNull { it.rawValue == normalized } ?: String
        }
    }
}

internal data class ShortcutIntentExtra(
    val key: String = "",
    val type: ShortcutIntentExtraType = ShortcutIntentExtraType.String,
    val value: String = ""
)

internal fun newOsActivityShortcutCardId(): String {
    val compactUuid = UUID.randomUUID().toString().replace("-", "").take(12)
    return "activity-${compactUuid.lowercase(Locale.ROOT)}"
}

internal fun createDefaultActivityShortcutDraft(
    defaults: OsGoogleSystemServiceConfig
): OsGoogleSystemServiceConfig {
    return defaults.copy(
        title = "",
        subtitle = "",
        appName = "",
        packageName = "",
        className = "",
        intentAction = "",
        intentCategory = "",
        intentFlags = defaults.intentFlags,
        intentUriData = "",
        intentMimeType = "",
        intentExtras = listOf(ShortcutIntentExtra())
    )
}

internal fun normalizeActivityShortcutConfig(
    config: OsGoogleSystemServiceConfig,
    defaults: OsGoogleSystemServiceConfig
): OsGoogleSystemServiceConfig {
    val trimmedPackageName = config.packageName.trim()
    val trimmedAppName = config.appName.trim()
    val resolvedAppName = trimmedAppName.ifBlank {
        if (trimmedPackageName.isNotBlank()) trimmedPackageName else defaults.appName
    }
    val normalizedExtras = normalizeShortcutIntentExtras(config.intentExtras)
    return config.copy(
        title = config.title.trim().ifBlank {
            if (resolvedAppName.isNotBlank()) resolvedAppName else defaults.title
        },
        subtitle = config.subtitle.trim(),
        appName = resolvedAppName,
        packageName = trimmedPackageName,
        className = config.className.trim(),
        intentAction = config.intentAction.trim(),
        intentCategory = config.intentCategory.trim(),
        intentFlags = config.intentFlags.trim().ifBlank { defaults.intentFlags },
        intentUriData = config.intentUriData.trim(),
        intentMimeType = config.intentMimeType.trim(),
        intentExtras = normalizedExtras
    )
}

internal fun normalizeShortcutIntentExtras(
    extras: List<ShortcutIntentExtra>
): List<ShortcutIntentExtra> {
    return extras.map { extra ->
        extra.copy(
            key = extra.key.trim(),
            value = extra.value.trim()
        )
    }.filter { it.key.isNotBlank() }
}

internal fun ensureEditorShortcutIntentExtras(
    extras: List<ShortcutIntentExtra>
): List<ShortcutIntentExtra> {
    return if (extras.isEmpty()) {
        listOf(ShortcutIntentExtra())
    } else {
        extras
    }
}

internal fun ensureEditorActivityShortcutDraft(
    config: OsGoogleSystemServiceConfig
): OsGoogleSystemServiceConfig {
    return config.copy(
        intentExtras = ensureEditorShortcutIntentExtras(config.intentExtras)
    )
}
