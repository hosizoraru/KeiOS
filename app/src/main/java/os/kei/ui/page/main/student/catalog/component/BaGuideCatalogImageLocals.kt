package os.kei.ui.page.main.student.catalog.component

import android.graphics.Bitmap
import androidx.compose.runtime.compositionLocalOf

internal val LocalBaGuideCatalogImageBitmaps =
    compositionLocalOf<Map<String, Bitmap>> { emptyMap() }

internal val LocalBaGuideCatalogImageRequester =
    compositionLocalOf<(List<String>) -> Unit> { {} }
