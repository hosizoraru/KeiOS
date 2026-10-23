package os.kei.ui.document

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

/** Single JSON import through the system document picker, including generic provider MIME types. */
internal class OpenJsonDocument : ActivityResultContract<Unit, Uri?>() {
    private val delegate = OpenReadableDocument()

    override fun createIntent(context: Context, input: Unit): Intent =
        delegate.createIntent(
            context,
            arrayOf("application/json", "text/*", "application/octet-stream"),
        )

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        delegate.parseResult(resultCode, intent)
}
