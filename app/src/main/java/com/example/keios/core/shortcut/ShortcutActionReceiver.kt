package com.example.keios.core.shortcut

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.keios.MainActivity
import com.example.keios.core.background.AppForegroundInfoHandler
import com.example.keios.mcp.server.McpServerRuntime
import com.example.keios.ui.page.main.ba.BaApNotificationDispatcher
import com.example.keios.ui.page.main.ba.support.BASettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ShortcutActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_EXECUTE_SHORTCUT) return
        val shortcutAction = intent.getStringExtra(MainActivity.EXTRA_SHORTCUT_ACTION)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        scope.launch {
            try {
                handleShortcutAction(appContext, shortcutAction)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleShortcutAction(
        context: Context,
        shortcutAction: String
    ) {
        when (shortcutAction) {
            MainActivity.SHORTCUT_ACTION_BA_AP_ISLAND -> {
                val snapshot = BASettingsStore.loadSnapshot()
                BaApNotificationDispatcher.send(
                    context = context,
                    currentDisplay = snapshot.apCurrent.coerceAtLeast(0.0).toInt(),
                    limitDisplay = snapshot.apLimit.coerceAtLeast(0),
                    thresholdDisplay = snapshot.apNotifyThreshold.coerceAtLeast(0)
                )
            }

            MainActivity.SHORTCUT_ACTION_MCP_TOGGLE -> {
                val manager = McpServerRuntime.getOrCreate(context)
                val state = manager.uiState.value
                if (state.running) {
                    manager.stop()
                } else {
                    manager.start(
                        port = state.port,
                        allowExternal = state.allowExternal
                    )
                }
            }

            MainActivity.SHORTCUT_ACTION_GITHUB_REFRESH_TRACKED -> {
                AppForegroundInfoHandler.handleGitHubShortcutRefresh(context)
            }
        }
    }

    companion object {
        const val ACTION_EXECUTE_SHORTCUT = "com.example.keios.shortcut.action.EXECUTE"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
