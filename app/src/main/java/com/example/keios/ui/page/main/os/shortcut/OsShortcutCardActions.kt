package com.example.keios.ui.page.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.keios.R

internal fun buildGoogleSystemServiceRows(
    context: Context,
    config: OsGoogleSystemServiceConfig,
    defaults: OsGoogleSystemServiceConfig
): List<InfoRow> {
    val normalized = normalizeActivityShortcutConfig(config, defaults)
    val emptyDataValue = context.getString(R.string.os_google_system_service_value_data_empty)
    val actionValue = normalized.intentAction.ifBlank {
        context.getString(R.string.os_google_system_service_value_action_auto_default)
    }
    val flagsValue = normalized.intentFlags.ifBlank {
        context.getString(R.string.os_google_system_service_value_flags_auto_default)
    }
    return listOf(
        InfoRow(
            key = context.getString(R.string.os_google_system_service_label_app_name),
            value = normalized.appName
        ),
        InfoRow(
            key = context.getString(R.string.os_google_system_service_label_package_name),
            value = normalized.packageName
        ),
        InfoRow(
            key = context.getString(R.string.os_google_system_service_label_class_name),
            value = normalized.className
        ),
        InfoRow(
            key = context.getString(R.string.os_google_system_service_label_intent_action),
            value = actionValue
        ),
        InfoRow(
            key = context.getString(R.string.os_google_system_service_label_intent_category),
            value = normalized.intentCategory.ifBlank { emptyDataValue }
        ),
        InfoRow(
            key = context.getString(R.string.os_google_system_service_label_intent_flags),
            value = flagsValue
        ),
        InfoRow(
            key = context.getString(R.string.os_google_system_service_label_intent_data),
            value = normalized.intentUriData.ifBlank { emptyDataValue }
        ),
        InfoRow(
            key = context.getString(R.string.os_google_system_service_label_intent_mime_type),
            value = normalized.intentMimeType.ifBlank { emptyDataValue }
        )
    )
}

internal fun launchGoogleSystemServiceActivity(
    context: Context,
    config: OsGoogleSystemServiceConfig,
    defaults: OsGoogleSystemServiceConfig
) {
    val normalized = normalizeActivityShortcutConfig(config, defaults)
    val packageName = normalized.packageName.trim()
    val className = normalized.className.trim()
    val action = normalized.intentAction.trim()
    require(packageName.isNotBlank())

    val intent = if (className.isBlank()) {
        context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
            if (action.isNotBlank()) {
                this.action = action
            }
        } ?: if (action.isBlank()) {
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(packageName)
            }
        } else {
            Intent(action).apply { setPackage(packageName) }
        }
    } else {
        Intent(action.ifBlank { Intent.ACTION_VIEW }).apply {
            setClassName(packageName, className)
        }
    }

    intent.apply {
        val dataText = normalized.intentUriData.trim()
        val mimeType = normalized.intentMimeType.trim()
        if (dataText.isNotBlank() && mimeType.isNotBlank()) {
            setDataAndType(Uri.parse(dataText), mimeType)
        } else if (dataText.isNotBlank()) {
            data = Uri.parse(dataText)
        } else if (mimeType.isNotBlank()) {
            type = mimeType
        }
        parseIntentCategories(normalized.intentCategory).forEach { category ->
            addCategory(category)
        }
        val parsedFlags = parseIntentFlags(normalized.intentFlags)
        addFlags(parsedFlags.ifBlankUse(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    context.startActivity(intent)
}

internal fun currentGoogleSystemServiceSuggestionFieldValue(
    draft: OsGoogleSystemServiceConfig,
    target: ShortcutSuggestionField
): String {
    return when (target) {
        ShortcutSuggestionField.PackageName -> draft.packageName
        ShortcutSuggestionField.ClassName -> draft.className
        ShortcutSuggestionField.IntentAction -> draft.intentAction
        ShortcutSuggestionField.IntentCategory -> draft.intentCategory
        ShortcutSuggestionField.IntentFlags -> draft.intentFlags
        ShortcutSuggestionField.IntentUriData -> draft.intentUriData
        ShortcutSuggestionField.IntentMimeType -> draft.intentMimeType
    }
}

internal fun applyShortcutExplicitDefaults(
    draft: OsGoogleSystemServiceConfig,
    className: String,
    defaultIntentFlags: String
): OsGoogleSystemServiceConfig {
    return draft.copy(
        className = className.trim(),
        intentAction = Intent.ACTION_VIEW,
        intentCategory = "",
        intentFlags = draft.intentFlags.ifBlank { defaultIntentFlags }
    )
}

internal fun applyShortcutImplicitDefaults(
    draft: OsGoogleSystemServiceConfig,
    defaultIntentFlags: String
): OsGoogleSystemServiceConfig {
    return draft.copy(
        className = "",
        intentAction = Intent.ACTION_MAIN,
        intentCategory = Intent.CATEGORY_LAUNCHER,
        intentFlags = draft.intentFlags.ifBlank { defaultIntentFlags }
    )
}

internal fun applyGoogleSystemServiceSuggestion(
    draft: OsGoogleSystemServiceConfig,
    target: ShortcutSuggestionField,
    item: ShortcutSuggestionItem,
    defaultIntentFlags: String
): OsGoogleSystemServiceConfig {
    return when (target) {
        ShortcutSuggestionField.PackageName -> {
            val nextPackageName = item.value.trim()
            val nextAppName = item.relatedAppName.trim()
            draft.copy(
                packageName = nextPackageName,
                appName = if (nextAppName.isNotBlank()) nextAppName else draft.appName
            )
        }

        ShortcutSuggestionField.ClassName -> {
            val nextClassName = item.value.trim()
            if (nextClassName.isBlank()) {
                applyShortcutImplicitDefaults(draft, defaultIntentFlags)
            } else {
                applyShortcutExplicitDefaults(
                    draft = draft,
                    className = nextClassName,
                    defaultIntentFlags = defaultIntentFlags
                )
            }
        }

        ShortcutSuggestionField.IntentAction -> {
            draft.copy(intentAction = item.value)
        }

        ShortcutSuggestionField.IntentCategory -> {
            val next = mergeIntentTokenText(
                current = draft.intentCategory,
                incoming = item.value,
                append = item.append
            )
            draft.copy(intentCategory = next)
        }

        ShortcutSuggestionField.IntentFlags -> {
            val next = mergeIntentTokenText(
                current = draft.intentFlags,
                incoming = item.value,
                append = item.append
            )
            draft.copy(intentFlags = next)
        }

        ShortcutSuggestionField.IntentUriData -> {
            draft.copy(intentUriData = item.value)
        }

        ShortcutSuggestionField.IntentMimeType -> {
            draft.copy(intentMimeType = item.value)
        }
    }
}
