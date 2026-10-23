package os.kei.ui.page.main.settings.state

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.yalantis.ucrop.UCrop
import os.kei.R
import os.kei.core.ui.resource.resolveString
import os.kei.ui.document.OpenReadableDocument

@Stable
internal data class SettingsBackgroundController(
    val backgroundPickerLauncher: ActivityResultLauncher<Array<String>>,
    val clearBackground: () -> Unit,
)

@Composable
internal fun rememberSettingsBackgroundController(
    settingsPageViewModel: SettingsPageViewModel,
    nonHomeBackgroundEnabled: Boolean,
    onNonHomeBackgroundEnabledChanged: (Boolean) -> Unit,
    nonHomeBackgroundUri: String,
    onNonHomeBackgroundUriChanged: (String) -> Unit,
): SettingsBackgroundController {
    val context = LocalContext.current
    val latestBackgroundEnabled by rememberUpdatedState(nonHomeBackgroundEnabled)
    val latestBackgroundUri by rememberUpdatedState(nonHomeBackgroundUri)
    val latestOnBackgroundEnabledChange by rememberUpdatedState(onNonHomeBackgroundEnabledChanged)
    val latestOnBackgroundUriChange by rememberUpdatedState(onNonHomeBackgroundUriChanged)

    val cropLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            val data = result.data
            val cropError = data?.let { UCrop.getError(it) }
            if (result.resultCode != Activity.RESULT_OK) {
                if (cropError != null) {
                    val reason =
                        cropError.javaClass.simpleName.ifBlank {
                            context.resolveString(R.string.common_unknown)
                        }
                    settingsPageViewModel.notifyNonHomeBackgroundCropFailed(reason)
                }
                settingsPageViewModel.trimManagedNonHomeBackgroundFiles(context, latestBackgroundUri)
                return@rememberLauncherForActivityResult
            }

            val outputUri =
                data?.let { UCrop.getOutput(it) } ?: run {
                    settingsPageViewModel.notifyNonHomeBackgroundCropFailed(
                        context.resolveString(R.string.common_unknown),
                    )
                    return@rememberLauncherForActivityResult
                }

            latestOnBackgroundUriChange(outputUri.toString())
            if (!latestBackgroundEnabled) {
                latestOnBackgroundEnabledChange(true)
            }
            settingsPageViewModel.trimManagedNonHomeBackgroundFiles(context, outputUri.toString())
            settingsPageViewModel.notifyNonHomeBackgroundSelected()
        }

    val pickerLauncher =
        rememberLauncherForActivityResult(
            contract = OpenReadableDocument(),
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            settingsPageViewModel.prepareNonHomeBackgroundCrop(context, uri)
        }

    LaunchedEffect(settingsPageViewModel, cropLauncher) {
        settingsPageViewModel.backgroundEvents.collect { event ->
            when (event) {
                is SettingsBackgroundEvent.LaunchNonHomeBackgroundCrop -> {
                    runCatching {
                        cropLauncher.launch(event.intent)
                    }.onFailure { error ->
                        settingsPageViewModel.trimManagedNonHomeBackgroundFiles(
                            context = context,
                            keepUriText = latestBackgroundUri,
                        )
                        val reason =
                            error.javaClass.simpleName.ifBlank {
                                context.resolveString(R.string.common_unknown)
                            }
                        settingsPageViewModel.notifyNonHomeBackgroundCropFailed(reason)
                    }
                }
            }
        }
    }

    return remember(pickerLauncher, settingsPageViewModel) {
        SettingsBackgroundController(
            backgroundPickerLauncher = pickerLauncher,
            clearBackground = {
                settingsPageViewModel.trimManagedNonHomeBackgroundFiles(context, keepUriText = "")
                latestOnBackgroundUriChange("")
                settingsPageViewModel.notifyNonHomeBackgroundCleared()
            },
        )
    }
}
