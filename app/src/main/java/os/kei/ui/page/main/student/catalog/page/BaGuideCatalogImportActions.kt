package os.kei.ui.page.main.student.catalog.page

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import os.kei.ui.document.OpenJsonDocument

internal data class BaGuideCatalogImportActions(
    val importStudentFavoritesLauncher: ActivityResultLauncher<Unit>,
    val importBgmFavoritesLauncher: ActivityResultLauncher<Unit>,
    val importAllFavoritesLauncher: ActivityResultLauncher<Unit>,
    val confirmFavoritesImport: (BaGuideCatalogImportPreviewState) -> Unit,
)

@Composable
internal fun rememberBaGuideCatalogImportActions(
    onRequestImportPreview: (Uri?, BaGuideCatalogImportKind) -> Unit,
    onConfirmFavoritesImport: (BaGuideCatalogImportPreviewState) -> Unit,
): BaGuideCatalogImportActions {
    val importStudentFavoritesLauncher =
        rememberLauncherForActivityResult(
            contract = OpenJsonDocument(),
        ) { uri ->
            onRequestImportPreview(
                uri,
                BaGuideCatalogImportKind.Student,
            )
        }
    val importBgmFavoritesLauncher =
        rememberLauncherForActivityResult(
            contract = OpenJsonDocument(),
        ) { uri ->
            onRequestImportPreview(
                uri,
                BaGuideCatalogImportKind.Bgm,
            )
        }
    val importAllFavoritesLauncher =
        rememberLauncherForActivityResult(
            contract = OpenJsonDocument(),
        ) { uri ->
            onRequestImportPreview(
                uri,
                BaGuideCatalogImportKind.All,
            )
        }
    return BaGuideCatalogImportActions(
        importStudentFavoritesLauncher = importStudentFavoritesLauncher,
        importBgmFavoritesLauncher = importBgmFavoritesLauncher,
        importAllFavoritesLauncher = importAllFavoritesLauncher,
        confirmFavoritesImport = onConfirmFavoritesImport,
    )
}
