package os.kei.ui.page.main.widget.support

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import os.kei.R
import os.kei.core.prefs.UiPrefs

internal val LocalTextCopyExpandedOverride = compositionLocalOf<Boolean?> { null }

internal fun buildTextCopyPayload(key: String, value: String): String {
    val title = key.trim().ifBlank { "Info" }
    val content = value.trim().ifBlank { "-" }
    return "$title: $content"
}

@Composable
internal fun rememberTextCopyExpandedEnabled(): Boolean {
    LocalTextCopyExpandedOverride.current?.let { return it }
    val copyFlow = remember { UiPrefs.observeTextCopyCapabilityExpanded() }
    val enabled by copyFlow.collectAsState(initial = UiPrefs.isTextCopyCapabilityExpanded())
    return enabled
}

@Composable
internal fun rememberLightTextCopyAction(copyPayload: String): (() -> Unit)? {
    if (copyPayload.isBlank()) return null
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val copiedToast = stringResource(R.string.guide_toast_item_copied)
    return remember(clipboard, context, copiedToast, copyPayload) {
        {
            clipboard.setText(AnnotatedString(copyPayload))
            Toast.makeText(context, copiedToast, Toast.LENGTH_SHORT).show()
        }
    }
}

internal fun Modifier.copyModeAwareRow(
    copyPayload: String,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
): Modifier = composed {
    val expandedCopyMode = rememberTextCopyExpandedEnabled()
    val copyLabel = stringResource(R.string.common_copy)
    if (expandedCopyMode) {
        if (onClick != null) {
            this.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
        } else {
            this
        }
    } else {
        val quickCopyAction = rememberLightTextCopyAction(copyPayload)
        val resolvedLongClick = onLongClick ?: quickCopyAction
        if (onClick != null || resolvedLongClick != null) {
            this.combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = { onClick?.invoke() },
                onLongClickLabel = if (resolvedLongClick != null) copyLabel else null,
                onLongClick = { resolvedLongClick?.invoke() }
            )
        } else {
            this
        }
    }
}

@Composable
internal fun CopyModeSelectionContainer(
    content: @Composable () -> Unit
) {
    if (rememberTextCopyExpandedEnabled()) {
        SelectionContainer {
            content()
        }
    } else {
        content()
    }
}
