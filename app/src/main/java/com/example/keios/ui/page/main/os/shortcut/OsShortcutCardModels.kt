package com.example.keios.ui.page.main

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
        intentMimeType = ""
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
        intentMimeType = config.intentMimeType.trim()
    )
}
