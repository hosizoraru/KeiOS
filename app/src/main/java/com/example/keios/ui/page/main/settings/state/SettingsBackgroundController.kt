package com.example.keios.ui.page.main.settings.state

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.example.keios.R
import com.example.keios.ui.page.main.settings.support.createNonHomeBackgroundCropOutputUri
import com.example.keios.ui.page.main.settings.support.deleteManagedNonHomeBackgroundFile
import com.example.keios.ui.page.main.settings.support.resolveNonHomeBackgroundAspectRatio
import com.example.keios.ui.page.main.settings.support.resolveNonHomeBackgroundCropSize
import com.yalantis.ucrop.UCrop

@Stable
internal data class SettingsBackgroundController(
    val backgroundPickerLauncher: ActivityResultLauncher<Array<String>>,
    val clearBackground: () -> Unit
)

@Composable
internal fun rememberSettingsBackgroundController(
    nonHomeBackgroundEnabled: Boolean,
    onNonHomeBackgroundEnabledChanged: (Boolean) -> Unit,
    nonHomeBackgroundUri: String,
    onNonHomeBackgroundUriChanged: (String) -> Unit
): SettingsBackgroundController {
    val context = LocalContext.current
    val latestBackgroundEnabled by rememberUpdatedState(nonHomeBackgroundEnabled)
    val latestBackgroundUri by rememberUpdatedState(nonHomeBackgroundUri)
    val latestOnBackgroundEnabledChange by rememberUpdatedState(onNonHomeBackgroundEnabledChanged)
    val latestOnBackgroundUriChange by rememberUpdatedState(onNonHomeBackgroundUriChanged)

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val cropError = data?.let { UCrop.getError(it) }
        if (result.resultCode != Activity.RESULT_OK) {
            if (cropError != null) {
                val reason = cropError.javaClass.simpleName.ifBlank {
                    context.getString(R.string.common_unknown)
                }
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_non_home_background_toast_crop_failed, reason),
                    Toast.LENGTH_SHORT
                ).show()
            }
            return@rememberLauncherForActivityResult
        }

        val outputUri = data?.let { UCrop.getOutput(it) } ?: run {
            Toast.makeText(
                context,
                context.getString(
                    R.string.settings_non_home_background_toast_crop_failed,
                    context.getString(R.string.common_unknown)
                ),
                Toast.LENGTH_SHORT
            ).show()
            return@rememberLauncherForActivityResult
        }

        deleteManagedNonHomeBackgroundFile(context, latestBackgroundUri)
        latestOnBackgroundUriChange(outputUri.toString())
        if (!latestBackgroundEnabled) {
            latestOnBackgroundEnabledChange(true)
        }
        Toast.makeText(
            context,
            context.getString(R.string.settings_non_home_background_toast_selected),
            Toast.LENGTH_SHORT
        ).show()
    }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        val outputUri = createNonHomeBackgroundCropOutputUri(context)
        val (aspectRatioX, aspectRatioY) = resolveNonHomeBackgroundAspectRatio(context)
        val (maxResultWidth, maxResultHeight) = resolveNonHomeBackgroundCropSize(context)
        val cropOptions = UCrop.Options().apply {
            setToolbarTitle(context.getString(R.string.settings_non_home_background_crop_title))
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(92)
            setFreeStyleCropEnabled(false)
            setHideBottomControls(false)
            setShowCropFrame(true)
            setShowCropGrid(true)
        }

        val cropIntent = runCatching {
            UCrop.of(uri, outputUri)
                .withAspectRatio(aspectRatioX, aspectRatioY)
                .withMaxResultSize(maxResultWidth, maxResultHeight)
                .withOptions(cropOptions)
                .getIntent(context)
        }.getOrElse { error ->
            val reason = error.javaClass.simpleName.ifBlank {
                context.getString(R.string.common_unknown)
            }
            Toast.makeText(
                context,
                context.getString(R.string.settings_non_home_background_toast_crop_failed, reason),
                Toast.LENGTH_SHORT
            ).show()
            return@rememberLauncherForActivityResult
        }

        cropIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        cropLauncher.launch(cropIntent)
    }

    return remember(pickerLauncher) {
        SettingsBackgroundController(
            backgroundPickerLauncher = pickerLauncher,
            clearBackground = {
                deleteManagedNonHomeBackgroundFile(context, latestBackgroundUri)
                latestOnBackgroundUriChange("")
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_non_home_background_toast_cleared),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}
