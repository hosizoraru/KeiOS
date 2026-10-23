@file:Suppress("FunctionName")

package os.kei.ui.page.main.os.components

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kyant.backdrop.Backdrop
import os.kei.R
import os.kei.ui.page.main.os.OsActivitySuggestionChromeState
import os.kei.ui.page.main.os.OsActivitySuggestionUiState
import os.kei.ui.page.main.os.OsPageViewModel
import os.kei.ui.page.main.os.OsSectionCard
import os.kei.ui.page.main.os.shell.OsShellCommandCard
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCard
import os.kei.ui.page.main.os.state.OsPageActionState
import os.kei.ui.page.main.os.state.OsPageCardTransferState
import os.kei.ui.page.main.os.state.OsPageOverlayState
import os.kei.ui.page.main.os.state.OsPageOverlayTransferActions
import os.kei.ui.page.main.os.state.OsPageTextBundle

@Composable
internal fun OsPageOverlayCoordinator(
    context: Context,
    sheetBackdrop: Backdrop,
    overlayState: OsPageOverlayState,
    editorDataReady: Boolean,
    textBundle: OsPageTextBundle,
    visibleCards: Set<OsSectionCard>,
    activityShortcutCards: List<OsActivityShortcutCard>,
    activityIconBitmaps: Map<String, Bitmap>,
    packageIconBitmaps: Map<String, Bitmap>,
    shellCommandCards: List<OsShellCommandCard>,
    activitySuggestionState: OsActivitySuggestionUiState,
    activitySuggestionChromeState: OsActivitySuggestionChromeState,
    actionState: OsPageActionState,
    overlayTransferActions: OsPageOverlayTransferActions,
    cardTransferState: OsPageCardTransferState,
    builtInShellCommandCards: List<OsShellCommandCard>,
    osPageViewModel: OsPageViewModel,
) {
    OsPageOverlayHost(
        context = context,
        sheetBackdrop = sheetBackdrop,
        overlayState = overlayState,
        editorDataReady = editorDataReady,
        visibleCardsTitle = textBundle.visibleCardsTitle,
        visibleCardsHint = stringResource(R.string.os_sheet_visible_cards_desc),
        visibleCards = visibleCards,
        applyCardVisibility = actionState.applyCardVisibility,
        visibleActivitiesTitle = textBundle.visibleActivitiesTitle,
        visibleActivitiesDesc = stringResource(R.string.os_sheet_visible_activities_desc),
        activityShortcutCards = activityShortcutCards,
        activityIconBitmaps = activityIconBitmaps,
        packageIconBitmaps = packageIconBitmaps,
        defaultActivityCardTitle = textBundle.googleSystemServiceDefaultTitle,
        cardTransferInProgress = overlayState.cardTransferInProgress,
        cardTransferState = cardTransferState,
        onExportAllActivityCards = overlayTransferActions.onExportAllActivityCards,
        onImportAllActivityCards = overlayTransferActions.onImportAllActivityCards,
        applyActivityCardVisibility = actionState.applyActivityCardVisibility,
        visibleShellCardsTitle = textBundle.visibleShellCardsTitle,
        visibleShellCardsDesc = textBundle.visibleShellCardsDesc,
        shellRunnerVisible = visibleCards.contains(OsSectionCard.SHELL_RUNNER),
        shellCommandCards = shellCommandCards,
        builtInShellCommandCards = builtInShellCommandCards,
        onExportAllShellCards = overlayTransferActions.onExportAllShellCards,
        onImportAllShellCards = overlayTransferActions.onImportAllShellCards,
        applyShellCommandCardVisibility = actionState.applyShellCommandCardVisibility,
        editShellCommandCardTitle = textBundle.editShellCommandCardTitle,
        shellCardCommandRequiredToast = textBundle.shellCardCommandRequiredToast,
        shellCardDeleteDialogTitle = textBundle.shellCardDeleteDialogTitle,
        addActivityCardTitle = textBundle.addActivityCardTitle,
        editActivityCardTitle = textBundle.editActivityCardTitle,
        noMatchedResultsText = textBundle.noMatchedResultsText,
        activitySuggestionState = activitySuggestionState,
        activitySuggestionChromeState = activitySuggestionChromeState,
        googleSystemServiceDefaults = textBundle.googleSystemServiceDefaults,
        googleSystemServiceDefaultTitle = textBundle.googleSystemServiceDefaultTitle,
        googleSystemServiceDefaultIntentFlags = textBundle.googleSystemServiceDefaultIntentFlags,
        activityCardDeleteDialogTitle = textBundle.activityCardDeleteDialogTitle,
        osPageViewModel = osPageViewModel,
    )
}
