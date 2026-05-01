package os.kei.ui.page.main.os.shell

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import os.kei.ui.page.main.widget.glass.GlassSearchField
import os.kei.ui.page.main.widget.glass.GlassVariant

@Composable
internal fun ShellCommandInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    minHeight: Dp = 136.dp,
    focusRequestToken: Int = 0,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(focusRequestToken) {
        if (focusRequestToken > 0) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    GlassSearchField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        backdrop = null,
        singleLine = false,
        minHeight = minHeight,
        variant = GlassVariant.SheetInput,
        focusRequester = focusRequester,
        modifier = modifier
    )
}
