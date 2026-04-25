package os.kei.ui.page.main.mcp.dialog

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
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
internal fun McpResetConfigDialog(
    show: Boolean,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    WindowDialog(
        show = show,
        title = stringResource(R.string.mcp_action_reset_service_config),
        summary = stringResource(R.string.mcp_reset_service_config_confirm_summary),
        onDismissRequest = onDismissRequest
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.common_cancel),
                    onClick = onDismissRequest
                )
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.common_reset),
                    colors = ButtonDefaults.textButtonColors(
                        color = MiuixTheme.colorScheme.error,
                        textColor = MiuixTheme.colorScheme.onError
                    ),
                    onClick = onConfirm
                )
            }
        }
    }
}

@Composable
internal fun McpResetTokenDialog(
    show: Boolean,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    WindowDialog(
        show = show,
        title = stringResource(R.string.mcp_action_reset_token),
        summary = stringResource(R.string.mcp_reset_token_confirm_summary),
        onDismissRequest = onDismissRequest
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.common_cancel),
                    onClick = onDismissRequest
                )
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.common_reset),
                    colors = ButtonDefaults.textButtonColors(
                        color = MiuixTheme.colorScheme.error,
                        textColor = MiuixTheme.colorScheme.onError
                    ),
                    onClick = onConfirm
                )
            }
        }
    }
}
