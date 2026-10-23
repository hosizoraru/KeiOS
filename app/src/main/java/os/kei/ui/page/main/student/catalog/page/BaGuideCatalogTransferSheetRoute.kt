@file:Suppress("FunctionName")

package os.kei.ui.page.main.student.catalog.page

import androidx.compose.runtime.Composable
import os.kei.ui.page.main.student.catalog.component.BaGuideBgmPlaybackCoordinator

@Composable
internal fun BaGuideCatalogTransferSheetRoute(
    pageState: BaGuideCatalogPageStateHolder,
    pageActions: BaGuideCatalogPageActions,
    playbackCoordinator: BaGuideBgmPlaybackCoordinator,
    transferExportAction: BaGuideCatalogJsonExportAction,
    importActions: BaGuideCatalogImportActions,
    bgmCacheState: BaGuideCatalogBgmCacheState,
    nativeBgmMediaNotificationEnabled: Boolean,
    notificationPermissionGranted: Boolean,
    allExportSuccessText: String,
    studentExportSuccessText: String,
    bgmExportSuccessText: String,
    onRequestNotificationPermission: () -> Unit,
) {
    BaGuideCatalogTransferSheet(
        show = pageState.showTransferSheet,
        onDismissRequest = pageState::closeTransferSheet,
        mediaSaveCustomEnabled = transferExportAction.saveLocationState.mediaSaveCustomEnabled,
        mediaSaveFixedTreeUri = transferExportAction.saveLocationState.mediaSaveFixedTreeUri,
        playbackSettingsState =
            BaGuideCatalogPlaybackSettingsState(
                nativeBgmMediaNotificationEnabled = nativeBgmMediaNotificationEnabled,
                notificationPermissionGranted = notificationPermissionGranted,
                onNativeBgmMediaNotificationChange = { enabled ->
                    if (enabled && !notificationPermissionGranted) {
                        onRequestNotificationPermission()
                    }
                    pageActions.onSetNativeBgmMediaNotificationEnabled(enabled)
                    playbackCoordinator.updateNativeMediaNotificationEnabled(enabled)
                },
            ),
        onMediaSaveCustomEnabledChange = transferExportAction.saveLocationState.onMediaSaveCustomEnabledChange,
        onPickMediaSaveLocation = transferExportAction.saveLocationState.onPickMediaSaveLocation,
        onExportAllFavorites = {
            pageState.closeTransferSheet()
            transferExportAction.exportJsonFrom(
                pageActions.buildCatalogAllFavoritesExportJson,
                "keios-ba-favorites.json",
                allExportSuccessText,
            )
        },
        onImportAllFavorites = {
            pageState.closeTransferSheet()
            importActions.importAllFavoritesLauncher.launch(Unit)
        },
        onExportStudentFavorites = {
            pageState.closeTransferSheet()
            transferExportAction.exportJsonFrom(
                pageActions.buildCatalogFavoritesExportJson,
                "keios-ba-student-favorites.json",
                studentExportSuccessText,
            )
        },
        onImportStudentFavorites = {
            pageState.closeTransferSheet()
            importActions.importStudentFavoritesLauncher.launch(Unit)
        },
        onExportBgmFavorites = {
            pageState.closeTransferSheet()
            transferExportAction.exportJsonFrom(
                pageActions.buildBgmFavoritesExportJson,
                "keios-ba-bgm-favorites.json",
                bgmExportSuccessText,
            )
        },
        onImportBgmFavorites = {
            pageState.closeTransferSheet()
            importActions.importBgmFavoritesLauncher.launch(Unit)
        },
        bgmCacheSummary = bgmCacheState.summary,
        onCacheAllBgm = {
            pageState.closeTransferSheet()
            bgmCacheState.onCacheAllBgm()
        },
        onCleanInvalidBgmCache = {
            pageState.closeTransferSheet()
            bgmCacheState.onCleanInvalidBgmCache()
        },
    )
}
