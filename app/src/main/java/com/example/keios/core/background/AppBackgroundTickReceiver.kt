package com.example.keios.core.background

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppBackgroundTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != ACTION_GITHUB_TICK && action != ACTION_BA_AP_TICK) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        scope.launch {
            try {
                when (action) {
                    ACTION_GITHUB_TICK -> AppForegroundInfoHandler.handleGitHubTick(appContext)
                    ACTION_BA_AP_TICK -> AppForegroundInfoHandler.handleBaApTick(appContext)
                }
            } finally {
                AppBackgroundScheduler.onTickHandled(appContext, action)
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_GITHUB_TICK = "com.example.keios.background.action.GITHUB_TICK"
        const val ACTION_BA_AP_TICK = "com.example.keios.background.action.BA_AP_TICK"
        private const val REQUEST_CODE_GITHUB_TICK = 42001
        private const val REQUEST_CODE_BA_AP_TICK = 42002

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun githubTickPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, AppBackgroundTickReceiver::class.java).apply {
                action = ACTION_GITHUB_TICK
            }
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_GITHUB_TICK,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun baApTickPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, AppBackgroundTickReceiver::class.java).apply {
                action = ACTION_BA_AP_TICK
            }
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_BA_AP_TICK,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
