package os.kei.ui.page.main.os.state

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import os.kei.R
import os.kei.ui.page.main.os.OsGoogleSystemServiceConfig
import os.kei.ui.page.main.os.shell.OsShellCommandCard
import os.kei.ui.page.main.os.shell.OsShellCommandCardStore
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCard
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCardStore
import os.kei.ui.page.main.os.transfer.OsActivityCardImportPayload
import os.kei.ui.page.main.os.transfer.OsCardImportFileKind
import os.kei.ui.page.main.os.transfer.OsCardImportPreview
import os.kei.ui.page.main.os.transfer.OsShellCardImportPayload
import os.kei.ui.page.main.os.transfer.OsUnknownCardImportPayload
import os.kei.ui.page.main.os.transfer.parseOsCardImportRoot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class OsPageCardTransferState(
    val exportLauncher: ActivityResultLauncher<String>,
    val importLauncher: ActivityResultLauncher<Array<String>>,
    val confirmImport: () -> Unit
)

@Composable
internal fun rememberOsPageCardTransferState(
    context: Context,
    scope: CoroutineScope,
    overlayState: OsPageOverlayState,
    activityShortcutCards: List<OsActivityShortcutCard>,
    onActivityShortcutCardsChange: (List<OsActivityShortcutCard>) -> Unit,
    activityCardExpanded: SnapshotStateMap<String, Boolean>,
    shellCommandCards: List<OsShellCommandCard>,
    onShellCommandCardsChange: (List<OsShellCommandCard>) -> Unit,
    shellCommandCardExpanded: SnapshotStateMap<String, Boolean>,
    googleSystemServiceDefaults: OsGoogleSystemServiceConfig,
    googleSettingsBuiltInSampleDefaults: OsGoogleSystemServiceConfig,
    cardImportFailedWithReason: String,
    exportSuccessText: String
): OsPageCardTransferState {
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val content = overlayState.pendingExportContent
        if (uri == null || content.isNullOrBlank()) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter().use { writer ->
                writer?.write(content)
            }
        }.onSuccess {
            Toast.makeText(context, exportSuccessText, Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(
                context,
                context.getString(R.string.common_export_failed_with_reason, it.javaClass.simpleName),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun applyActivityImport(payload: OsActivityCardImportPayload) {
        val result = OsActivityShortcutCardStore.applyImportedCards(
            payload = payload,
            existingCards = activityShortcutCards,
            defaults = googleSystemServiceDefaults,
            builtInSampleDefaults = googleSettingsBuiltInSampleDefaults
        )
        onActivityShortcutCardsChange(result.cards)
        val validIds = result.cards.mapTo(mutableSetOf()) { it.id }
        activityCardExpanded.keys.retainAll(validIds)
        if (!validIds.contains(overlayState.editingActivityShortcutCardId.orEmpty())) {
            overlayState.onShowActivityShortcutEditorChange(false)
            overlayState.onShowActivityCardDeleteConfirmChange(false)
            overlayState.onEditingActivityShortcutCardIdChange(null)
        }
        Toast.makeText(
            context,
            context.getString(
                R.string.os_activity_card_toast_imported_summary,
                result.addedCount,
                result.updatedCount,
                result.unchangedCount
            ),
            Toast.LENGTH_SHORT
        ).show()
    }

    fun applyShellImport(payload: OsShellCardImportPayload) {
        val result = OsShellCommandCardStore.applyImportedCards(
            payload = payload,
            existingCards = shellCommandCards
        )
        onShellCommandCardsChange(result.cards)
        val validIds = result.cards.mapTo(mutableSetOf()) { it.id }
        shellCommandCardExpanded.keys.retainAll(validIds)
        if (!validIds.contains(overlayState.editingShellCommandCardId.orEmpty())) {
            overlayState.onShowShellCommandCardEditorChange(false)
            overlayState.onShowShellCardDeleteConfirmChange(false)
            overlayState.onEditingShellCommandCardIdChange(null)
        }
        Toast.makeText(
            context,
            context.getString(
                R.string.os_shell_card_toast_imported_summary,
                result.addedCount,
                result.updatedCount,
                result.unchangedCount
            ),
            Toast.LENGTH_SHORT
        ).show()
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val target = overlayState.pendingImportTarget
        overlayState.onPendingImportTargetChange(null)
        if (uri == null || target == null) {
            overlayState.onCardTransferInProgressChange(false)
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            runCatching {
                val raw = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader().use { reader ->
                        reader?.readText().orEmpty()
                    }
                }
                buildImportPreview(
                    raw = raw,
                    target = target,
                    activityShortcutCards = activityShortcutCards,
                    shellCommandCards = shellCommandCards,
                    googleSystemServiceDefaults = googleSystemServiceDefaults,
                    googleSettingsBuiltInSampleDefaults = googleSettingsBuiltInSampleDefaults
                )
            }.onSuccess { preview ->
                overlayState.onPendingCardImportPreviewChange(preview)
            }.onFailure { error ->
                Toast.makeText(
                    context,
                    String.format(
                        cardImportFailedWithReason,
                        error.message ?: error.javaClass.simpleName
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
            overlayState.onCardTransferInProgressChange(false)
        }
    }

    val confirmPendingImport: () -> Unit = confirmPendingImport@{
        val preview = overlayState.pendingCardImportPreview ?: return@confirmPendingImport
        if (!preview.canImport || overlayState.cardTransferInProgress) {
            overlayState.onPendingCardImportPreviewChange(null)
            return@confirmPendingImport
        }
        scope.launch {
            overlayState.onCardTransferInProgressChange(true)
            runCatching {
                when (val payload = preview.payload) {
                    is OsActivityCardImportPayload -> applyActivityImport(payload)
                    is OsShellCardImportPayload -> applyShellImport(payload)
                    is OsUnknownCardImportPayload -> error("当前文件没有可导入的数据")
                }
            }.onFailure { error ->
                Toast.makeText(
                    context,
                    String.format(
                        cardImportFailedWithReason,
                        error.message ?: error.javaClass.simpleName
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
            overlayState.onPendingCardImportPreviewChange(null)
            overlayState.onCardTransferInProgressChange(false)
        }
    }

    return OsPageCardTransferState(
        exportLauncher = exportLauncher,
        importLauncher = importLauncher,
        confirmImport = confirmPendingImport
    )
}

private fun buildImportPreview(
    raw: String,
    target: OsCardImportTarget,
    activityShortcutCards: List<OsActivityShortcutCard>,
    shellCommandCards: List<OsShellCommandCard>,
    googleSystemServiceDefaults: OsGoogleSystemServiceConfig,
    googleSettingsBuiltInSampleDefaults: OsGoogleSystemServiceConfig
): OsCardImportPreview {
    val root = parseOsCardImportRoot(raw)
    return when (root.fileKind) {
        OsCardImportFileKind.Activity -> {
            val payload = OsActivityShortcutCardStore.parseCardsImport(
                root = root,
                defaults = googleSystemServiceDefaults,
                builtInSampleDefaults = googleSettingsBuiltInSampleDefaults
            )
            val result = if (target == OsCardImportTarget.Activity) {
                OsActivityShortcutCardStore.previewImportedCards(
                    payload = payload,
                    existingCards = activityShortcutCards,
                    defaults = googleSystemServiceDefaults,
                    builtInSampleDefaults = googleSettingsBuiltInSampleDefaults
                )
            } else {
                null
            }
            OsCardImportPreview(
                target = target,
                payload = payload,
                fileItemCount = payload.sourceCount,
                validCount = payload.cards.size,
                duplicateCount = payload.duplicateCount,
                invalidCount = payload.invalidCount,
                newCount = result?.addedCount ?: 0,
                updatedCount = result?.updatedCount ?: 0,
                unchangedCount = result?.unchangedCount ?: 0,
                mergedCount = result?.cards?.size ?: 0
            )
        }

        OsCardImportFileKind.Shell -> {
            val payload = OsShellCommandCardStore.parseCardsImport(root)
            val result = if (target == OsCardImportTarget.Shell) {
                OsShellCommandCardStore.previewImportedCards(
                    payload = payload,
                    existingCards = shellCommandCards
                )
            } else {
                null
            }
            OsCardImportPreview(
                target = target,
                payload = payload,
                fileItemCount = payload.sourceCount,
                validCount = payload.cards.size,
                duplicateCount = payload.duplicateCount,
                invalidCount = payload.invalidCount,
                newCount = result?.addedCount ?: 0,
                updatedCount = result?.updatedCount ?: 0,
                unchangedCount = result?.unchangedCount ?: 0,
                mergedCount = result?.cards?.size ?: 0
            )
        }

        OsCardImportFileKind.Unknown -> {
            val payload = OsUnknownCardImportPayload(
                sourceCount = root.sourceCount,
                invalidCount = root.sourceCount,
                duplicateCount = 0,
                fileKind = root.fileKind,
                isLegacyFormat = root.isLegacyFormat
            )
            OsCardImportPreview(
                target = target,
                payload = payload,
                fileItemCount = payload.sourceCount,
                validCount = 0,
                duplicateCount = payload.duplicateCount,
                invalidCount = payload.invalidCount,
                newCount = 0,
                updatedCount = 0,
                unchangedCount = 0,
                mergedCount = 0
            )
        }
    }
}
