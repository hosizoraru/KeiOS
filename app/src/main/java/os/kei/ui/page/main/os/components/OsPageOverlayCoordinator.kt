package os.kei.ui.page.main.os.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import os.kei.R
import os.kei.ui.page.main.os.OsSectionCard
import os.kei.ui.page.main.os.shell.OsShellCommandCard
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCard
import os.kei.ui.page.main.os.state.OsPageActionState
import os.kei.ui.page.main.os.state.OsPageCardTransferState
import os.kei.ui.page.main.os.state.OsPageOverlayState
import os.kei.ui.page.main.os.state.OsPageOverlayTransferActions
import os.kei.ui.page.main.os.state.OsPageTextBundle
import com.kyant.backdrop.backdrops.LayerBackdrop
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun OsPageOverlayCoordinator(
    context: Context,
    scope: CoroutineScope,
    sheetBackdrop: LayerBackdrop,
    overlayState: OsPageOverlayState,
    textBundle: OsPageTextBundle,
    visibleCards: Set<OsSectionCard>,
    activityShortcutCards: List<OsActivityShortcutCard>,
    shellCommandCards: List<OsShellCommandCard>,
    actionState: OsPageActionState,
    overlayTransferActions: OsPageOverlayTransferActions,
    cardTransferState: OsPageCardTransferState,
    onShellCommandCardsChange: (List<OsShellCommandCard>) -> Unit,
    onRemoveShellCommandCardExpanded: (String) -> Unit,
    onActivityShortcutCardsChange: (List<OsActivityShortcutCard>) -> Unit,
    onRemoveActivityCardExpanded: (String) -> Unit
) {
    OsPageOverlayHost(
        context = context,
        scope = scope,
        sheetBackdrop = sheetBackdrop,
        overlayState = overlayState,
        visibleCardsTitle = textBundle.visibleCardsTitle,
        visibleCardsHint = stringResource(R.string.os_sheet_visible_cards_desc),
        visibleCards = visibleCards,
        applyCardVisibility = actionState.applyCardVisibility,
        visibleActivitiesTitle = textBundle.visibleActivitiesTitle,
        visibleActivitiesDesc = stringResource(R.string.os_sheet_visible_activities_desc),
        activityShortcutCards = activityShortcutCards,
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
        onExportAllShellCards = overlayTransferActions.onExportAllShellCards,
        onImportAllShellCards = overlayTransferActions.onImportAllShellCards,
        applyShellCommandCardVisibility = actionState.applyShellCommandCardVisibility,
        editShellCommandCardTitle = textBundle.editShellCommandCardTitle,
        onShellCommandCardsChange = onShellCommandCardsChange,
        onRemoveShellCommandCardExpanded = onRemoveShellCommandCardExpanded,
        shellCardCommandRequiredToast = textBundle.shellCardCommandRequiredToast,
        shellCardSavedToast = textBundle.shellCardSavedToast,
        shellCardDeletedToast = textBundle.shellCardDeletedToast,
        shellCardDeleteDialogTitle = textBundle.shellCardDeleteDialogTitle,
        addActivityCardTitle = textBundle.addActivityCardTitle,
        editActivityCardTitle = textBundle.editActivityCardTitle,
        noMatchedResultsText = textBundle.noMatchedResultsText,
        onActivityShortcutCardsChange = onActivityShortcutCardsChange,
        onRemoveActivityCardExpanded = onRemoveActivityCardExpanded,
        googleSystemServiceDefaults = textBundle.googleSystemServiceDefaults,
        googleSystemServiceDefaultTitle = textBundle.googleSystemServiceDefaultTitle,
        googleSystemServiceDefaultIntentFlags = textBundle.googleSystemServiceDefaultIntentFlags,
        activityCardDeletedToast = textBundle.activityCardDeletedToast,
        activityCardDeleteDialogTitle = textBundle.activityCardDeleteDialogTitle
    )
}
