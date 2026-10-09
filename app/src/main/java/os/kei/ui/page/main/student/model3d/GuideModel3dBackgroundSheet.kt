@file:Suppress("FunctionName")
package os.kei.ui.page.main.student.model3d

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import os.kei.R
import os.kei.ui.page.main.widget.glass.AppStandaloneLiquidTextButton
import os.kei.ui.page.main.widget.glass.AppSwitch
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.sheet.*
import top.yukonga.miuix.kmp.basic.ColorPalette
import top.yukonga.miuix.kmp.basic.ColorPicker
import top.yukonga.miuix.kmp.basic.TextField

@Composable
internal fun GuideModel3dBackgroundSheet(show: Boolean, customArgb: Int?, effectiveColor: Color,
    onColorChanged: (Int?) -> Unit, onDismiss: () -> Unit,
) {
    SnapshotWindowBottomSheet(show = show, title = stringResource(R.string.guide_model_3d_background), onDismissRequest = onDismiss) {
        GuideModel3dBackgroundContent(customArgb, effectiveColor, onColorChanged)
    }
}

@Composable
internal fun GuideModel3dBackgroundContent(customArgb: Int?, effectiveColor: Color,
    onColorChanged: (Int?) -> Unit, scrollable: Boolean = true, transparent: Boolean = false,
) {
    var fine by rememberSaveable { mutableStateOf(false) }
    val color = customArgb?.let(::Color) ?: effectiveColor
    val hex = color.toArgb().toUInt().toString(16).padStart(8, '0').uppercase()
    var input by remember { mutableStateOf("#$hex") }
    var inputFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(hex, inputFocused) { if (!inputFocused) input = "#$hex" }
    val followLabel = stringResource(R.string.guide_model_3d_background_follow)
    val fineLabel = stringResource(R.string.guide_model_3d_background_fine)
    val paletteLabel = stringResource(R.string.guide_model_3d_background_palette)
    val surface = if (transparent) Color.White.copy(alpha = 0.10f) else null
        SheetContentColumn(Modifier.fillMaxWidth().padding(horizontal = if (transparent) 0.dp else 16.dp),
            scrollable = scrollable, verticalSpacing = 12.dp) {
            SheetSurfaceCard(containerColor = surface) {
                SheetControlRow(label = followLabel) {
                    AppSwitch(customArgb == null, { onColorChanged(if (it) null else effectiveColor.toArgb()) },
                        modifier = Modifier.testTag("model3d_background_follow").semantics { contentDescription = followLabel })
                }
                SheetSectionHeader(stringResource(R.string.guide_model_3d_background_hint))
            }
            SheetSurfaceCard(containerColor = surface) {
                SheetControlRow(label = fineLabel) {
                    AppSwitch(fine, { fine = it }, modifier = Modifier.testTag("model3d_background_fine")
                        .semantics { contentDescription = fineLabel })
                }
                if (fine) ColorPicker(color = color, onColorChanged = { onColorChanged(it.toArgb()) },
                    modifier = Modifier.fillMaxWidth().testTag("model3d_color_picker").semantics { contentDescription = fineLabel })
                else ColorPalette(color = color, onColorChanged = { onColorChanged(it.toArgb()) }, rows = 6, hueColumns = 8,
                    modifier = Modifier.fillMaxWidth().testTag("model3d_color_palette").semantics { contentDescription = paletteLabel })
                Spacer(Modifier.height(12.dp))
                // A text alternative also makes exact colors accessible without relying on a dense canvas grid.
                TextField(value = input, onValueChange = { raw ->
                    input = raw
                    parseModel3dColor(raw)?.let(onColorChanged)
                }, label = stringResource(R.string.guide_model_3d_background_hex), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii,
                        capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); keyboard?.hide() }),
                    modifier = Modifier.fillMaxWidth().testTag("model3d_background_hex").onFocusChanged { inputFocused = it.isFocused })
            }
            AppStandaloneLiquidTextButton(text = stringResource(R.string.guide_model_3d_background_reset),
                onClick = { onColorChanged(null) }, variant = GlassVariant.SheetAction, modifier = Modifier.fillMaxWidth())
        }
}
