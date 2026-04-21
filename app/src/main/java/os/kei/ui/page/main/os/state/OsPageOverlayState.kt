package os.kei.ui.page.main.os.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import os.kei.ui.page.main.os.OsGoogleSystemServiceConfig
import os.kei.ui.page.main.os.OsSectionCard
import os.kei.ui.page.main.os.shell.OsShellCommandCard
import os.kei.ui.page.main.os.shell.createDefaultShellCommandCardDraft
import os.kei.ui.page.main.os.shortcut.OsActivityCardEditMode
import os.kei.ui.page.main.os.shortcut.ShortcutActivityClassOption
import os.kei.ui.page.main.os.shortcut.ShortcutInstalledAppOption
import os.kei.ui.page.main.os.shortcut.ShortcutSuggestionField
import os.kei.ui.page.main.os.shortcut.createDefaultActivityShortcutDraft
import os.kei.ui.page.main.os.transfer.OsCardImportPreview

internal enum class OsCardImportTarget {
    Activity,
    Shell
}

internal data class OsPageOverlayState(
    val activityShortcutDraft: OsGoogleSystemServiceConfig,
    val onActivityShortcutDraftChange: (OsGoogleSystemServiceConfig) -> Unit,
    val showActivityShortcutEditor: Boolean,
    val onShowActivityShortcutEditorChange: (Boolean) -> Unit,
    val showActivitySuggestionSheet: Boolean,
    val onShowActivitySuggestionSheetChange: (Boolean) -> Unit,
    val activityCardEditMode: OsActivityCardEditMode,
    val onActivityCardEditModeChange: (OsActivityCardEditMode) -> Unit,
    val editingActivityShortcutCardId: String?,
    val onEditingActivityShortcutCardIdChange: (String?) -> Unit,
    val editingActivityShortcutBuiltIn: Boolean,
    val onEditingActivityShortcutBuiltInChange: (Boolean) -> Unit,
    val googleSystemServiceSuggestionTarget: ShortcutSuggestionField,
    val onGoogleSystemServiceSuggestionTargetChange: (ShortcutSuggestionField) -> Unit,
    val googleSystemServicePackageSuggestions: List<ShortcutInstalledAppOption>,
    val onGoogleSystemServicePackageSuggestionsChange: (List<ShortcutInstalledAppOption>) -> Unit,
    val googleSystemServicePackageSuggestionsLoading: Boolean,
    val onGoogleSystemServicePackageSuggestionsLoadingChange: (Boolean) -> Unit,
    val googleSystemServicePackageSuggestionQuery: String,
    val onGoogleSystemServicePackageSuggestionQueryChange: (String) -> Unit,
    val googleSystemServiceClassSuggestions: List<ShortcutActivityClassOption>,
    val onGoogleSystemServiceClassSuggestionsChange: (List<ShortcutActivityClassOption>) -> Unit,
    val googleSystemServiceClassSuggestionsLoading: Boolean,
    val onGoogleSystemServiceClassSuggestionsLoadingChange: (Boolean) -> Unit,
    val googleSystemServiceClassSuggestionQuery: String,
    val onGoogleSystemServiceClassSuggestionQueryChange: (String) -> Unit,
    val showCardManager: Boolean,
    val onShowCardManagerChange: (Boolean) -> Unit,
    val showActivityVisibilityManager: Boolean,
    val onShowActivityVisibilityManagerChange: (Boolean) -> Unit,
    val showShellCardVisibilityManager: Boolean,
    val onShowShellCardVisibilityManagerChange: (Boolean) -> Unit,
    val showShellCommandCardEditor: Boolean,
    val onShowShellCommandCardEditorChange: (Boolean) -> Unit,
    val editingShellCommandCardId: String?,
    val onEditingShellCommandCardIdChange: (String?) -> Unit,
    val shellCommandCardDraft: OsShellCommandCard,
    val onShellCommandCardDraftChange: (OsShellCommandCard) -> Unit,
    val showShellCardDeleteConfirm: Boolean,
    val onShowShellCardDeleteConfirmChange: (Boolean) -> Unit,
    val showActivityCardDeleteConfirm: Boolean,
    val onShowActivityCardDeleteConfirmChange: (Boolean) -> Unit,
    val pendingExportContent: String?,
    val onPendingExportContentChange: (String?) -> Unit,
    val pendingImportTarget: OsCardImportTarget?,
    val onPendingImportTargetChange: (OsCardImportTarget?) -> Unit,
    val pendingCardImportPreview: OsCardImportPreview?,
    val onPendingCardImportPreviewChange: (OsCardImportPreview?) -> Unit,
    val cardTransferInProgress: Boolean,
    val onCardTransferInProgressChange: (Boolean) -> Unit,
    val exportingCard: OsSectionCard?,
    val onExportingCardChange: (OsSectionCard?) -> Unit,
)

@Composable
internal fun rememberOsPageOverlayState(
    googleSystemServiceDefaults: OsGoogleSystemServiceConfig
): OsPageOverlayState {
    var activityShortcutDraft by remember {
        mutableStateOf(createDefaultActivityShortcutDraft(googleSystemServiceDefaults))
    }
    var showActivityShortcutEditor by rememberSaveable { mutableStateOf(false) }
    var showActivitySuggestionSheet by rememberSaveable { mutableStateOf(false) }
    var activityCardEditMode by rememberSaveable { mutableStateOf(OsActivityCardEditMode.Edit) }
    var editingActivityShortcutCardId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingActivityShortcutBuiltIn by rememberSaveable { mutableStateOf(false) }
    var googleSystemServiceSuggestionTarget by remember {
        mutableStateOf(ShortcutSuggestionField.IntentAction)
    }
    var googleSystemServicePackageSuggestions by remember {
        mutableStateOf<List<ShortcutInstalledAppOption>>(emptyList())
    }
    var googleSystemServicePackageSuggestionsLoading by remember { mutableStateOf(false) }
    var googleSystemServicePackageSuggestionQuery by rememberSaveable { mutableStateOf("") }
    var googleSystemServiceClassSuggestions by remember {
        mutableStateOf<List<ShortcutActivityClassOption>>(emptyList())
    }
    var googleSystemServiceClassSuggestionsLoading by remember { mutableStateOf(false) }
    var googleSystemServiceClassSuggestionQuery by rememberSaveable { mutableStateOf("") }
    var showCardManager by rememberSaveable { mutableStateOf(false) }
    var showActivityVisibilityManager by rememberSaveable { mutableStateOf(false) }
    var showShellCardVisibilityManager by rememberSaveable { mutableStateOf(false) }
    var showShellCommandCardEditor by rememberSaveable { mutableStateOf(false) }
    var editingShellCommandCardId by rememberSaveable { mutableStateOf<String?>(null) }
    var shellCommandCardDraft by remember { mutableStateOf(createDefaultShellCommandCardDraft()) }
    var showShellCardDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showActivityCardDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var pendingExportContent by remember { mutableStateOf<String?>(null) }
    var pendingImportTarget by remember { mutableStateOf<OsCardImportTarget?>(null) }
    var pendingCardImportPreview by remember { mutableStateOf<OsCardImportPreview?>(null) }
    var cardTransferInProgress by remember { mutableStateOf(false) }
    var exportingCard by remember { mutableStateOf<OsSectionCard?>(null) }

    return remember(
        activityShortcutDraft,
        showActivityShortcutEditor,
        showActivitySuggestionSheet,
        activityCardEditMode,
        editingActivityShortcutCardId,
        editingActivityShortcutBuiltIn,
        googleSystemServiceSuggestionTarget,
        googleSystemServicePackageSuggestions,
        googleSystemServicePackageSuggestionsLoading,
        googleSystemServicePackageSuggestionQuery,
        googleSystemServiceClassSuggestions,
        googleSystemServiceClassSuggestionsLoading,
        googleSystemServiceClassSuggestionQuery,
        showCardManager,
        showActivityVisibilityManager,
        showShellCardVisibilityManager,
        showShellCommandCardEditor,
        editingShellCommandCardId,
        shellCommandCardDraft,
        showShellCardDeleteConfirm,
        showActivityCardDeleteConfirm,
        pendingExportContent,
        pendingImportTarget,
        pendingCardImportPreview,
        cardTransferInProgress,
        exportingCard,
    ) {
        OsPageOverlayState(
            activityShortcutDraft = activityShortcutDraft,
            onActivityShortcutDraftChange = { activityShortcutDraft = it },
            showActivityShortcutEditor = showActivityShortcutEditor,
            onShowActivityShortcutEditorChange = { showActivityShortcutEditor = it },
            showActivitySuggestionSheet = showActivitySuggestionSheet,
            onShowActivitySuggestionSheetChange = { showActivitySuggestionSheet = it },
            activityCardEditMode = activityCardEditMode,
            onActivityCardEditModeChange = { activityCardEditMode = it },
            editingActivityShortcutCardId = editingActivityShortcutCardId,
            onEditingActivityShortcutCardIdChange = { editingActivityShortcutCardId = it },
            editingActivityShortcutBuiltIn = editingActivityShortcutBuiltIn,
            onEditingActivityShortcutBuiltInChange = { editingActivityShortcutBuiltIn = it },
            googleSystemServiceSuggestionTarget = googleSystemServiceSuggestionTarget,
            onGoogleSystemServiceSuggestionTargetChange = { googleSystemServiceSuggestionTarget = it },
            googleSystemServicePackageSuggestions = googleSystemServicePackageSuggestions,
            onGoogleSystemServicePackageSuggestionsChange = { googleSystemServicePackageSuggestions = it },
            googleSystemServicePackageSuggestionsLoading = googleSystemServicePackageSuggestionsLoading,
            onGoogleSystemServicePackageSuggestionsLoadingChange = {
                googleSystemServicePackageSuggestionsLoading = it
            },
            googleSystemServicePackageSuggestionQuery = googleSystemServicePackageSuggestionQuery,
            onGoogleSystemServicePackageSuggestionQueryChange = {
                googleSystemServicePackageSuggestionQuery = it
            },
            googleSystemServiceClassSuggestions = googleSystemServiceClassSuggestions,
            onGoogleSystemServiceClassSuggestionsChange = { googleSystemServiceClassSuggestions = it },
            googleSystemServiceClassSuggestionsLoading = googleSystemServiceClassSuggestionsLoading,
            onGoogleSystemServiceClassSuggestionsLoadingChange = {
                googleSystemServiceClassSuggestionsLoading = it
            },
            googleSystemServiceClassSuggestionQuery = googleSystemServiceClassSuggestionQuery,
            onGoogleSystemServiceClassSuggestionQueryChange = {
                googleSystemServiceClassSuggestionQuery = it
            },
            showCardManager = showCardManager,
            onShowCardManagerChange = { showCardManager = it },
            showActivityVisibilityManager = showActivityVisibilityManager,
            onShowActivityVisibilityManagerChange = { showActivityVisibilityManager = it },
            showShellCardVisibilityManager = showShellCardVisibilityManager,
            onShowShellCardVisibilityManagerChange = { showShellCardVisibilityManager = it },
            showShellCommandCardEditor = showShellCommandCardEditor,
            onShowShellCommandCardEditorChange = { showShellCommandCardEditor = it },
            editingShellCommandCardId = editingShellCommandCardId,
            onEditingShellCommandCardIdChange = { editingShellCommandCardId = it },
            shellCommandCardDraft = shellCommandCardDraft,
            onShellCommandCardDraftChange = { shellCommandCardDraft = it },
            showShellCardDeleteConfirm = showShellCardDeleteConfirm,
            onShowShellCardDeleteConfirmChange = { showShellCardDeleteConfirm = it },
            showActivityCardDeleteConfirm = showActivityCardDeleteConfirm,
            onShowActivityCardDeleteConfirmChange = { showActivityCardDeleteConfirm = it },
            pendingExportContent = pendingExportContent,
            onPendingExportContentChange = { pendingExportContent = it },
            pendingImportTarget = pendingImportTarget,
            onPendingImportTargetChange = { pendingImportTarget = it },
            pendingCardImportPreview = pendingCardImportPreview,
            onPendingCardImportPreviewChange = { pendingCardImportPreview = it },
            cardTransferInProgress = cardTransferInProgress,
            onCardTransferInProgressChange = { cardTransferInProgress = it },
            exportingCard = exportingCard,
            onExportingCardChange = { exportingCard = it },
        )
    }
}
