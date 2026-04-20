package com.example.keios.ui.page.main.settings.state

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.keios.R
import com.example.keios.core.log.AppLogStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Stable
internal data class SettingsLogController(
    val logStats: AppLogStore.Stats,
    val exportingLogZip: Boolean,
    val clearingLogs: Boolean,
    val exportZip: () -> Unit,
    val clearLogs: () -> Unit
)

@Composable
internal fun rememberSettingsLogController(
    logDebugEnabled: Boolean,
    pageUiState: SettingsPageUiState
): SettingsLogController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var exportingLogZip by remember { mutableStateOf(false) }
    var clearingLogs by remember { mutableStateOf(false) }

    val logStats by produceState(
        initialValue = AppLogStore.Stats.Empty,
        pageUiState.logReloadSignal,
        logDebugEnabled
    ) {
        do {
            value = withContext(Dispatchers.IO) { AppLogStore.stats(context) }
            if (!logDebugEnabled) break
            delay(1200)
        } while (true)
    }

    val logExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri == null) {
            exportingLogZip = false
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val result = withContext(Dispatchers.IO) { AppLogStore.exportZipToUri(context, uri) }
            exportingLogZip = false
            if (result.isSuccess) {
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_log_toast_exported),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val reason = result.exceptionOrNull()?.javaClass?.simpleName
                    ?: context.getString(R.string.common_unknown)
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_log_toast_export_failed, reason),
                    Toast.LENGTH_SHORT
                ).show()
            }
            pageUiState.requestLogReload()
        }
    }

    return remember(logStats, exportingLogZip, clearingLogs, logExportLauncher) {
        SettingsLogController(
            logStats = logStats,
            exportingLogZip = exportingLogZip,
            clearingLogs = clearingLogs,
            exportZip = {
                if (!exportingLogZip && !clearingLogs) {
                    exportingLogZip = true
                    val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
                    logExportLauncher.launch("keios-logs-$stamp.zip")
                }
            },
            clearLogs = {
                if (!exportingLogZip && !clearingLogs) scope.launch {
                    clearingLogs = true
                    val result = withContext(Dispatchers.IO) { runCatching { AppLogStore.clear(context) } }
                    clearingLogs = false
                    if (result.isSuccess) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.settings_log_toast_cleared),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val reason = result.exceptionOrNull()?.javaClass?.simpleName
                            ?: context.getString(R.string.common_unknown)
                        Toast.makeText(
                            context,
                            context.getString(R.string.settings_log_toast_clear_failed, reason),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    pageUiState.requestLogReload()
                }
            }
        )
    }
}
