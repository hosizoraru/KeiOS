package os.kei.ui.page.main.os.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LiquidDialogActionButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
internal fun OsDeleteConfirmDialog(
    show: Boolean,
    title: String,
    summary: String,
    onDismissRequest: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    WindowDialog(
        show = show,
        title = title,
        summary = summary,
        onDismissRequest = onDismissRequest
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiquidDialogActionButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.common_cancel),
                    onClick = onDismissRequest
                )
                LiquidDialogActionButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.common_delete),
                    containerColor = MiuixTheme.colorScheme.error,
                    textColor = MiuixTheme.colorScheme.onError,
                    variant = GlassVariant.SheetDangerAction,
                    onClick = onConfirmDelete
                )
            }
        }
    }
}
