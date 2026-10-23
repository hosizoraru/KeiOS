package os.kei.ui.page.main.os.state

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import os.kei.ui.page.main.os.OsGoogleSystemServiceConfig
import os.kei.ui.page.main.os.OsPageViewModel
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCard
import os.kei.ui.document.OpenJsonDocument

internal data class OsPageCardTransferState(
    val exportLauncher: ActivityResultLauncher<String>,
    val importLauncher: ActivityResultLauncher<Unit>,
    val confirmImport: () -> Unit,
)

@Composable
internal fun rememberOsPageCardTransferState(
    context: Context,
    osPageViewModel: OsPageViewModel,
    overlayState: OsPageOverlayState,
    googleSystemServiceDefaults: OsGoogleSystemServiceConfig,
    googleSettingsBuiltInSampleDefaults: OsGoogleSystemServiceConfig,
    builtInActivityShortcutCards: List<OsActivityShortcutCard>,
): OsPageCardTransferState {
    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json"),
        ) { uri: Uri? ->
            val content = overlayState.pendingExportContent
            if (uri == null || content.isNullOrBlank()) return@rememberLauncherForActivityResult
            osPageViewModel.writeCardExportContent(
                contentResolver = context.contentResolver,
                uri = uri,
                content = content,
            )
        }

    val importLauncher =
        rememberLauncherForActivityResult(
            contract = OpenJsonDocument(),
        ) { uri: Uri? ->
            val target = overlayState.pendingImportTarget
            overlayState.onPendingImportTargetChange(null)
            if (uri == null || target == null) {
                overlayState.onCardTransferInProgressChange(false)
                return@rememberLauncherForActivityResult
            }
            osPageViewModel.requestCardImportPreview(
                contentResolver = context.contentResolver,
                uri = uri,
                target = target,
                googleSystemServiceDefaults = googleSystemServiceDefaults,
                googleSettingsBuiltInSampleDefaults = googleSettingsBuiltInSampleDefaults,
                builtInActivityShortcutCards = builtInActivityShortcutCards,
            )
        }

    val confirmPendingImport: () -> Unit = confirmPendingImport@{
        val preview = overlayState.pendingCardImportPreview ?: return@confirmPendingImport
        if (!preview.canImport || overlayState.cardTransferInProgress) {
            overlayState.onPendingCardImportPreviewChange(null)
            return@confirmPendingImport
        }
        overlayState.onCardTransferInProgressChange(true)
        osPageViewModel.confirmCardImport(
            preview = preview,
            googleSystemServiceDefaults = googleSystemServiceDefaults,
            googleSettingsBuiltInSampleDefaults = googleSettingsBuiltInSampleDefaults,
            builtInActivityShortcutCards = builtInActivityShortcutCards,
        )
    }

    return OsPageCardTransferState(
        exportLauncher = exportLauncher,
        importLauncher = importLauncher,
        confirmImport = confirmPendingImport,
    )
}
