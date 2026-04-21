package os.kei.ui.page.main.os.state

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import os.kei.R
import os.kei.ui.page.main.os.OsGoogleSystemServiceConfig
import os.kei.ui.page.main.os.shell.OsShellCommandCard
import os.kei.ui.page.main.os.shell.OsShellCommandCardStore
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCard
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCardStore

internal data class OsPageOverlayTransferActions(
    val onExportAllActivityCards: () -> Unit,
    val onImportAllActivityCards: () -> Unit,
    val onExportAllShellCards: () -> Unit,
    val onImportAllShellCards: () -> Unit
)

@Composable
internal fun rememberOsPageOverlayTransferActions(
    context: Context,
    overlayState: OsPageOverlayState,
    cardTransferState: OsPageCardTransferState,
    activityShortcutCards: List<OsActivityShortcutCard>,
    shellCommandCards: List<OsShellCommandCard>,
    googleSystemServiceDefaults: OsGoogleSystemServiceConfig
): OsPageOverlayTransferActions {
    return remember(
        context,
        overlayState,
        cardTransferState,
        activityShortcutCards,
        shellCommandCards,
        googleSystemServiceDefaults
    ) {
        OsPageOverlayTransferActions(
            onExportAllActivityCards = {
                runCatching {
                    val payload = OsActivityShortcutCardStore.buildCardsExportJson(
                        cards = activityShortcutCards,
                        defaults = googleSystemServiceDefaults
                    )
                    overlayState.onPendingExportContentChange(payload)
                    cardTransferState.exportLauncher.launch("keios-os-activity-cards.json")
                }.onFailure { error ->
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.common_export_failed_with_reason,
                            error.message ?: error.javaClass.simpleName
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onImportAllActivityCards = {
                overlayState.onPendingCardImportPreviewChange(null)
                overlayState.onPendingImportTargetChange(OsCardImportTarget.Activity)
                overlayState.onCardTransferInProgressChange(true)
                cardTransferState.importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            onExportAllShellCards = {
                runCatching {
                    val payload = OsShellCommandCardStore.buildCardsExportJson(shellCommandCards)
                    overlayState.onPendingExportContentChange(payload)
                    cardTransferState.exportLauncher.launch("keios-os-shell-cards.json")
                }.onFailure { error ->
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.common_export_failed_with_reason,
                            error.message ?: error.javaClass.simpleName
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onImportAllShellCards = {
                overlayState.onPendingCardImportPreviewChange(null)
                overlayState.onPendingImportTargetChange(OsCardImportTarget.Shell)
                overlayState.onCardTransferInProgressChange(true)
                cardTransferState.importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            }
        )
    }
}
