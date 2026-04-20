package com.example.keios.ui.page.main.os.components

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.example.keios.R
import com.example.keios.ui.page.main.os.OsGoogleSystemServiceConfig
import com.example.keios.ui.page.main.os.shortcut.ShortcutActivityClassOption
import com.example.keios.ui.page.main.os.shortcut.ShortcutInstalledAppOption
import com.example.keios.ui.page.main.os.shortcut.ShortcutSuggestionField
import com.example.keios.ui.page.main.os.shortcut.ShortcutSuggestionItem
import com.example.keios.ui.page.main.os.shortcut.currentGoogleSystemServiceSuggestionFieldValue
import com.example.keios.ui.page.main.os.shortcut.parseIntentTokenText
import java.util.Locale

internal data class OsGoogleSystemServiceSuggestionUiState(
    val sheetTitle: String,
    val showPackageSearch: Boolean,
    val showClassSearch: Boolean,
    val showActionRecommendations: Boolean,
    val showCategoryRecommendations: Boolean,
    val explicitActionRecommendationSelected: Boolean,
    val implicitActionRecommendationSelected: Boolean,
    val explicitCategoryRecommendationSelected: Boolean,
    val implicitCategoryRecommendationSelected: Boolean,
    val showPackageLoading: Boolean,
    val showClassLoading: Boolean,
    val showPackageNoResult: Boolean,
    val showClassNoResult: Boolean,
    val orderedSuggestions: List<ShortcutSuggestionItem>,
    val isCurrentSuggestionSelected: (ShortcutSuggestionItem) -> Boolean
)

@Composable
internal fun rememberOsGoogleSystemServiceSuggestionUiState(
    target: ShortcutSuggestionField,
    draft: OsGoogleSystemServiceConfig,
    packageSuggestions: List<ShortcutInstalledAppOption>,
    packageSuggestionsLoading: Boolean,
    packageSuggestionQuery: String,
    classSuggestions: List<ShortcutActivityClassOption>,
    classSuggestionsLoading: Boolean,
    classSuggestionQuery: String
): OsGoogleSystemServiceSuggestionUiState {
    val sheetTitle = when (target) {
        ShortcutSuggestionField.PackageName ->
            stringResource(R.string.os_google_system_service_field_package_name)
        ShortcutSuggestionField.ClassName ->
            stringResource(R.string.os_google_system_service_field_class_name)
        ShortcutSuggestionField.IntentAction ->
            stringResource(R.string.os_google_system_service_field_intent_action)
        ShortcutSuggestionField.IntentCategory ->
            stringResource(R.string.os_google_system_service_field_intent_category)
        ShortcutSuggestionField.IntentFlags ->
            stringResource(R.string.os_google_system_service_field_intent_flags)
        ShortcutSuggestionField.IntentUriData ->
            stringResource(R.string.os_google_system_service_field_intent_data)
        ShortcutSuggestionField.IntentMimeType ->
            stringResource(R.string.os_google_system_service_field_intent_mime_type)
    }
    val packageQuery = packageSuggestionQuery.trim()
    val currentPackageName = draft.packageName.trim()
    val filteredInstalledApps = remember(
        packageSuggestions,
        packageQuery,
        currentPackageName
    ) {
        val filtered = if (packageQuery.isBlank()) {
            packageSuggestions
        } else {
            packageSuggestions.filter { app ->
                app.appName.contains(packageQuery, ignoreCase = true) ||
                    app.packageName.contains(packageQuery, ignoreCase = true)
            }
        }
        if (currentPackageName.isBlank()) {
            filtered
        } else {
            val selected = filtered.filter {
                it.packageName.equals(currentPackageName, ignoreCase = true)
            }
            val others = filtered.filterNot {
                it.packageName.equals(currentPackageName, ignoreCase = true)
            }
            selected + others
        }
    }
    val classQuery = classSuggestionQuery.trim()
    val currentClassName = draft.className.trim()
    val filteredActivityClasses = remember(
        classSuggestions,
        classQuery,
        currentClassName
    ) {
        val filtered = if (classQuery.isBlank()) {
            classSuggestions
        } else {
            classSuggestions.filter { option ->
                option.className.contains(classQuery, ignoreCase = true) ||
                    option.activityName.contains(classQuery, ignoreCase = true)
            }
        }
        if (currentClassName.isBlank()) {
            filtered
        } else {
            val selected = filtered.filter { option ->
                option.className.equals(currentClassName, ignoreCase = true)
            }
            val others = filtered.filterNot { option ->
                option.className.equals(currentClassName, ignoreCase = true)
            }
            selected + others
        }
    }
    val currentIntentAction = draft.intentAction.trim()
    val currentCategoryTokensUpper = remember(draft.intentCategory) {
        parseIntentTokenText(draft.intentCategory)
            .map { it.uppercase(Locale.ROOT) }
            .toSet()
    }
    val explicitActionRecommendationSelected = currentClassName.isNotBlank() &&
        currentIntentAction == Intent.ACTION_VIEW
    val implicitActionRecommendationSelected = currentClassName.isBlank() &&
        currentIntentAction == Intent.ACTION_MAIN
    val explicitCategoryRecommendationSelected = currentClassName.isNotBlank() &&
        currentCategoryTokensUpper.isEmpty()
    val implicitCategoryRecommendationSelected = currentClassName.isBlank() &&
        currentCategoryTokensUpper == setOf(Intent.CATEGORY_LAUNCHER.uppercase(Locale.ROOT))
    val suggestions = rememberOsGoogleSystemServiceSuggestions(
        target = target,
        filteredInstalledApps = filteredInstalledApps,
        filteredActivityClasses = filteredActivityClasses
    )
    val currentValue = currentGoogleSystemServiceSuggestionFieldValue(draft, target)
    val isCurrentSuggestionSelected: (ShortcutSuggestionItem) -> Boolean = remember(target, currentValue) {
        { suggestion ->
            isCurrentGoogleSystemServiceSuggestionSelected(
                target = target,
                currentValue = currentValue,
                suggestion = suggestion
            )
        }
    }
    val orderedSuggestions = remember(target, suggestions, isCurrentSuggestionSelected) {
        when (target) {
            ShortcutSuggestionField.ClassName -> suggestions
            else -> {
                val selected = suggestions.filter(isCurrentSuggestionSelected)
                val others = suggestions.filterNot(isCurrentSuggestionSelected)
                selected + others
            }
        }
    }

    return remember(
        sheetTitle,
        target,
        explicitActionRecommendationSelected,
        implicitActionRecommendationSelected,
        explicitCategoryRecommendationSelected,
        implicitCategoryRecommendationSelected,
        packageSuggestionsLoading,
        classSuggestionsLoading,
        orderedSuggestions,
        isCurrentSuggestionSelected
    ) {
        OsGoogleSystemServiceSuggestionUiState(
            sheetTitle = sheetTitle,
            showPackageSearch = target == ShortcutSuggestionField.PackageName,
            showClassSearch = target == ShortcutSuggestionField.ClassName,
            showActionRecommendations = target == ShortcutSuggestionField.IntentAction,
            showCategoryRecommendations = target == ShortcutSuggestionField.IntentCategory,
            explicitActionRecommendationSelected = explicitActionRecommendationSelected,
            implicitActionRecommendationSelected = implicitActionRecommendationSelected,
            explicitCategoryRecommendationSelected = explicitCategoryRecommendationSelected,
            implicitCategoryRecommendationSelected = implicitCategoryRecommendationSelected,
            showPackageLoading = target == ShortcutSuggestionField.PackageName && packageSuggestionsLoading,
            showClassLoading = target == ShortcutSuggestionField.ClassName && classSuggestionsLoading,
            showPackageNoResult = target == ShortcutSuggestionField.PackageName &&
                !packageSuggestionsLoading &&
                suggestions.isEmpty(),
            showClassNoResult = target == ShortcutSuggestionField.ClassName &&
                !classSuggestionsLoading &&
                suggestions.size <= 1,
            orderedSuggestions = orderedSuggestions,
            isCurrentSuggestionSelected = isCurrentSuggestionSelected
        )
    }
}

@Composable
private fun rememberOsGoogleSystemServiceSuggestions(
    target: ShortcutSuggestionField,
    filteredInstalledApps: List<ShortcutInstalledAppOption>,
    filteredActivityClasses: List<ShortcutActivityClassOption>
): List<ShortcutSuggestionItem> {
    return when (target) {
        ShortcutSuggestionField.PackageName -> filteredInstalledApps.map { app ->
            ShortcutSuggestionItem(
                label = app.appName,
                value = app.packageName,
                summary = app.packageName,
                relatedAppName = app.appName
            )
        }

        ShortcutSuggestionField.ClassName -> {
            val implicitItem = ShortcutSuggestionItem(
                label = stringResource(R.string.os_google_system_service_class_suggestion_implicit_title),
                value = "",
                summary = stringResource(R.string.os_google_system_service_class_suggestion_implicit_summary)
            )
            listOf(implicitItem) + filteredActivityClasses.map { option ->
                ShortcutSuggestionItem(
                    label = option.activityName,
                    value = option.className,
                    summary = option.className,
                    classItemExported = option.isExported
                )
            }
        }

        ShortcutSuggestionField.IntentAction -> listOf(
            ShortcutSuggestionItem(
                label = stringResource(R.string.os_google_system_service_suggestion_clear),
                value = "",
                summary = stringResource(R.string.os_google_system_service_suggestion_action_auto_summary),
                append = false
            ),
            ShortcutSuggestionItem(
                label = Intent.ACTION_MAIN,
                value = Intent.ACTION_MAIN,
                summary = stringResource(R.string.os_google_system_service_suggestion_action_main_summary)
            ),
            ShortcutSuggestionItem(
                label = Intent.ACTION_VIEW,
                value = Intent.ACTION_VIEW,
                summary = stringResource(R.string.os_google_system_service_suggestion_action_view_summary)
            ),
            ShortcutSuggestionItem(
                label = Intent.ACTION_SEND,
                value = Intent.ACTION_SEND,
                summary = stringResource(R.string.os_google_system_service_suggestion_action_send_summary)
            ),
            ShortcutSuggestionItem(
                label = Intent.ACTION_SEND_MULTIPLE,
                value = Intent.ACTION_SEND_MULTIPLE,
                summary = stringResource(R.string.os_google_system_service_suggestion_action_send_multiple_summary)
            ),
            ShortcutSuggestionItem(
                label = Intent.ACTION_EDIT,
                value = Intent.ACTION_EDIT,
                summary = stringResource(R.string.os_google_system_service_suggestion_action_edit_summary)
            ),
            ShortcutSuggestionItem(
                label = Intent.ACTION_PICK,
                value = Intent.ACTION_PICK,
                summary = stringResource(R.string.os_google_system_service_suggestion_action_pick_summary)
            )
        )

        ShortcutSuggestionField.IntentCategory -> listOf(
            ShortcutSuggestionItem(
                label = stringResource(R.string.os_google_system_service_suggestion_clear),
                value = "",
                summary = stringResource(R.string.os_google_system_service_suggestion_category_clear_summary),
                append = false
            ),
            ShortcutSuggestionItem(
                label = Intent.CATEGORY_DEFAULT,
                value = Intent.CATEGORY_DEFAULT,
                summary = stringResource(R.string.os_google_system_service_suggestion_category_default_summary),
                append = true
            ),
            ShortcutSuggestionItem(
                label = Intent.CATEGORY_BROWSABLE,
                value = Intent.CATEGORY_BROWSABLE,
                summary = stringResource(R.string.os_google_system_service_suggestion_category_browsable_summary),
                append = true
            ),
            ShortcutSuggestionItem(
                label = Intent.CATEGORY_LAUNCHER,
                value = Intent.CATEGORY_LAUNCHER,
                summary = stringResource(R.string.os_google_system_service_suggestion_category_launcher_summary),
                append = true
            ),
            ShortcutSuggestionItem(
                label = Intent.CATEGORY_INFO,
                value = Intent.CATEGORY_INFO,
                summary = stringResource(R.string.os_google_system_service_suggestion_category_info_summary),
                append = true
            )
        )

        ShortcutSuggestionField.IntentFlags -> listOf(
            ShortcutSuggestionItem(
                label = stringResource(R.string.os_google_system_service_suggestion_clear),
                value = "",
                summary = stringResource(R.string.os_google_system_service_suggestion_flags_auto_summary),
                append = false
            ),
            ShortcutSuggestionItem(
                label = "FLAG_ACTIVITY_NEW_TASK",
                value = "FLAG_ACTIVITY_NEW_TASK",
                summary = stringResource(R.string.os_google_system_service_suggestion_flags_new_task_summary),
                append = true
            ),
            ShortcutSuggestionItem(
                label = "FLAG_ACTIVITY_CLEAR_TOP",
                value = "FLAG_ACTIVITY_CLEAR_TOP",
                summary = stringResource(R.string.os_google_system_service_suggestion_flags_clear_top_summary),
                append = true
            ),
            ShortcutSuggestionItem(
                label = "FLAG_ACTIVITY_SINGLE_TOP",
                value = "FLAG_ACTIVITY_SINGLE_TOP",
                summary = stringResource(R.string.os_google_system_service_suggestion_flags_single_top_summary),
                append = true
            ),
            ShortcutSuggestionItem(
                label = "FLAG_ACTIVITY_NEW_TASK, FLAG_ACTIVITY_CLEAR_TOP",
                value = "FLAG_ACTIVITY_NEW_TASK, FLAG_ACTIVITY_CLEAR_TOP",
                summary = stringResource(R.string.os_google_system_service_suggestion_flags_combo_summary),
                append = true
            )
        )

        ShortcutSuggestionField.IntentUriData -> listOf(
            ShortcutSuggestionItem(
                label = stringResource(R.string.os_google_system_service_suggestion_clear),
                value = "",
                summary = stringResource(R.string.os_google_system_service_suggestion_uri_clear_summary),
                append = false
            ),
            ShortcutSuggestionItem(
                label = "market://details?id=com.android.vending",
                value = "market://details?id=com.android.vending",
                summary = stringResource(R.string.os_google_system_service_suggestion_uri_market_summary)
            ),
            ShortcutSuggestionItem(
                label = "https://play.google.com/store/apps/details?id=com.android.vending",
                value = "https://play.google.com/store/apps/details?id=com.android.vending",
                summary = stringResource(R.string.os_google_system_service_suggestion_uri_https_summary)
            ),
            ShortcutSuggestionItem(
                label = "package:com.android.vending",
                value = "package:com.android.vending",
                summary = stringResource(R.string.os_google_system_service_suggestion_uri_package_summary)
            )
        )

        ShortcutSuggestionField.IntentMimeType -> listOf(
            ShortcutSuggestionItem(
                label = stringResource(R.string.os_google_system_service_suggestion_clear),
                value = "",
                summary = stringResource(R.string.os_google_system_service_suggestion_mime_clear_summary),
                append = false
            ),
            ShortcutSuggestionItem(
                label = "text/plain",
                value = "text/plain",
                summary = stringResource(R.string.os_google_system_service_suggestion_mime_text_summary)
            ),
            ShortcutSuggestionItem(
                label = "application/vnd.android.package-archive",
                value = "application/vnd.android.package-archive",
                summary = stringResource(R.string.os_google_system_service_suggestion_mime_apk_summary)
            ),
            ShortcutSuggestionItem(
                label = "*/*",
                value = "*/*",
                summary = stringResource(R.string.os_google_system_service_suggestion_mime_any_summary)
            )
        )
    }
}

private fun isCurrentGoogleSystemServiceSuggestionSelected(
    target: ShortcutSuggestionField,
    currentValue: String,
    suggestion: ShortcutSuggestionItem
): Boolean {
    return when (target) {
        ShortcutSuggestionField.PackageName,
        ShortcutSuggestionField.ClassName -> {
            currentValue.trim().equals(suggestion.value.trim(), ignoreCase = true)
        }

        ShortcutSuggestionField.IntentAction,
        ShortcutSuggestionField.IntentUriData,
        ShortcutSuggestionField.IntentMimeType -> {
            currentValue.trim() == suggestion.value.trim()
        }

        ShortcutSuggestionField.IntentCategory,
        ShortcutSuggestionField.IntentFlags -> {
            val currentTokens = parseIntentTokenText(currentValue)
                .map { it.uppercase(Locale.ROOT) }
                .toSet()
            val suggestionTokens = parseIntentTokenText(suggestion.value)
                .map { it.uppercase(Locale.ROOT) }
                .toSet()
            if (suggestionTokens.isEmpty()) {
                currentTokens.isEmpty()
            } else {
                currentTokens.containsAll(suggestionTokens)
            }
        }
    }
}
