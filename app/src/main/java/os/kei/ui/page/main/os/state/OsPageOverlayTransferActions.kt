package os.kei.ui.page.main.os.state

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import os.kei.ui.page.main.os.OsGoogleSystemServiceConfig
import os.kei.ui.page.main.os.OsPageViewModel

internal data class OsPageOverlayTransferActions(
    val onExportAllActivityCards: () -> Unit,
    val onImportAllActivityCards: () -> Unit,
    val onExportAllShellCards: () -> Unit,
    val onImportAllShellCards: () -> Unit
)

@Composable
internal fun rememberOsPageOverlayTransferActions(
    context: Context,
    osPageViewModel: OsPageViewModel,
    overlayState: OsPageOverlayState,
    cardTransferState: OsPageCardTransferState,
    googleSystemServiceDefaults: OsGoogleSystemServiceConfig
): OsPageOverlayTransferActions {
    return remember(
        context,
        osPageViewModel,
        overlayState,
        cardTransferState,
        googleSystemServiceDefaults
    ) {
        OsPageOverlayTransferActions(
            onExportAllActivityCards = {
                overlayState.onCardTransferInProgressChange(true)
                osPageViewModel.prepareActivityCardsExport(
                    defaults = googleSystemServiceDefaults,
                )
            },
            onImportAllActivityCards = {
                overlayState.onPendingCardImportPreviewChange(null)
                overlayState.onPendingImportTargetChange(OsCardImportTarget.Activity)
                overlayState.onCardTransferInProgressChange(true)
                cardTransferState.importLauncher.launch(Unit)
            },
            onExportAllShellCards = {
                overlayState.onCardTransferInProgressChange(true)
                osPageViewModel.prepareShellCardsExport()
            },
            onImportAllShellCards = {
                overlayState.onPendingCardImportPreviewChange(null)
                overlayState.onPendingImportTargetChange(OsCardImportTarget.Shell)
                overlayState.onCardTransferInProgressChange(true)
                cardTransferState.importLauncher.launch(Unit)
            }
        )
    }
}
