package com.example.keios.feature.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_MARK_READ) return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, Int.MIN_VALUE)
        if (notificationId == Int.MIN_VALUE) return
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    companion object {
        const val ACTION_MARK_READ = "com.example.keios.notification.action.MARK_READ"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}
