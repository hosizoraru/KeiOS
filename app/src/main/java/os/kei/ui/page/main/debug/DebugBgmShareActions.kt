package os.kei.ui.page.main.debug

import android.content.Context
import android.content.Intent

internal fun Context.launchDebugBgmTrackShare(
    chooserTitle: String,
    shareText: String
) {
    val shareIntent = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, shareText)
    startActivity(Intent.createChooser(shareIntent, chooserTitle))
}
