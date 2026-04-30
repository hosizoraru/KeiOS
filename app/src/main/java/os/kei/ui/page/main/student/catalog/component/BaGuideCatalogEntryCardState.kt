package os.kei.ui.page.main.student.catalog.component

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import os.kei.R
import os.kei.ui.page.main.student.catalog.BaGuideCatalogEntry
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Immutable
internal data class BaGuideCatalogEntryCardUiState(
    val copyPayload: String,
    val favoriteActionColor: Color,
    val borderColor: Color,
    val containerColor: Color,
    val favoriteContentDescription: String
)

@Composable
internal fun rememberBaGuideCatalogEntryCardUiState(
    entry: BaGuideCatalogEntry,
    isFavorite: Boolean
): BaGuideCatalogEntryCardUiState {
    val favoriteContentDescription = if (isFavorite) {
        stringResource(R.string.ba_catalog_cd_unfavorite_student)
    } else {
        stringResource(R.string.ba_catalog_cd_favorite_student)
    }
    val unknownStudent = stringResource(R.string.ba_catalog_bgm_student_unknown)
    val copyPayload = remember(entry, unknownStudent) {
        buildString {
            append(entry.name.ifBlank { unknownStudent })
            append(" | ID ")
            append(entry.contentId)
            if (entry.aliasDisplay.isNotBlank()) {
                append('\n')
                append(entry.aliasDisplay)
            }
            if (entry.detailUrl.isNotBlank()) {
                append('\n')
                append(entry.detailUrl)
            }
        }
    }
    val favoriteActionColor = if (isFavorite) {
        Color(0xFFEC4899)
    } else {
        Color(0xFF3B82F6)
    }
    val borderColor = if (isFavorite) {
        Color(0x99EC4899)
    } else {
        MiuixTheme.colorScheme.primary.copy(alpha = 0.24f)
    }
    val containerColor = if (isFavorite) {
        Color(0x33EC4899)
    } else {
        MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
    }
    return remember(
        copyPayload,
        favoriteActionColor,
        borderColor,
        containerColor,
        favoriteContentDescription
    ) {
        BaGuideCatalogEntryCardUiState(
            copyPayload = copyPayload,
            favoriteActionColor = favoriteActionColor,
            borderColor = borderColor,
            containerColor = containerColor,
            favoriteContentDescription = favoriteContentDescription
        )
    }
}

@Composable
internal fun rememberBaGuideCatalogEntryCopyAction(
    copyPayload: String
): () -> Unit {
    val context = LocalContext.current
    val clipboard: ClipboardManager = LocalClipboardManager.current
    val copiedToast = stringResource(R.string.guide_toast_item_copied)
    return remember(context, clipboard, copiedToast, copyPayload) {
        {
            clipboard.setText(AnnotatedString(copyPayload))
            Toast.makeText(context, copiedToast, Toast.LENGTH_SHORT).show()
        }
    }
}
