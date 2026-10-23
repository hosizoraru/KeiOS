package os.kei.core.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import os.kei.core.log.AppLogger
import os.kei.core.icon.LauncherIconController

class AppBackgroundSystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action.orEmpty()
        if (!shouldRescheduleForAction(action)) return
        BackgroundAsyncReceiverRunner.launch(
            receiver = this,
            context = context,
            tag = TAG,
            timeoutMs = RESCHEDULE_TIMEOUT_MS,
        ) { appContext ->
            val startedElapsedRealtimeMs = SystemClock.elapsedRealtime()
            AppLogger.i(TAG, "reschedule background alarms after $action")
            try {
                if (action == Intent.ACTION_MY_PACKAGE_REPLACED) {
                    LauncherIconController.recoverAfterUpdate(appContext)
                }
                AppBackgroundScheduler.scheduleAll(appContext)
                AppBackgroundRecoveryStore.recordSucceeded(
                    action = action,
                    startedElapsedRealtimeMs = startedElapsedRealtimeMs,
                )
            } catch (error: Throwable) {
                AppBackgroundRecoveryStore.recordFailed(
                    action = action,
                    startedElapsedRealtimeMs = startedElapsedRealtimeMs,
                    error = error,
                )
                throw error
            }
        }
    }

    companion object {
        private const val TAG = "AppBackgroundSystemEvent"
        private const val RESCHEDULE_TIMEOUT_MS = 12_000L

        internal fun shouldRescheduleForAction(action: String): Boolean =
            action == Intent.ACTION_BOOT_COMPLETED ||
                action == Intent.ACTION_MY_PACKAGE_REPLACED ||
                action == Intent.ACTION_TIME_CHANGED ||
                action == Intent.ACTION_TIMEZONE_CHANGED
    }
}
