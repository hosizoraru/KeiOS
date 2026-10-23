package os.kei.ui.document

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts

/** These consumers use openInputStream rather than typed virtual-document conversion. */
internal class OpenReadableDocument : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent =
        super.createIntent(context, input).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        }
}
