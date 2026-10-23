package os.kei.core.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.annotation.WorkerThread
import java.util.UUID
import os.kei.core.background.BackgroundAsyncReceiverRunner
import os.kei.core.notification.live.oem.vivo.VivoAtomicCapabilityProbe
import os.kei.core.notification.live.oem.vivo.VivoAtomicCapability
import os.kei.core.notification.live.oem.vivo.VivoAtomicDispatcher
import os.kei.core.notification.live.oem.vivo.VivoAtomicDispatchResult
import os.kei.core.notification.live.oem.vivo.VivoAtomicNotificationTransport
import os.kei.core.notification.live.oem.vivo.VivoAtomicRecord
import os.kei.core.notification.live.oem.vivo.VivoAtomicRecordCodec
import os.kei.core.notification.live.oem.vivo.VivoAtomicRecordStore
import os.kei.core.notification.live.oem.vivo.VivoAtomicRequest
import os.kei.core.notification.live.oem.vivo.VivoAtomicSceneBinding
import os.kei.core.notification.live.oem.vivo.VivoAtomicSignal
import os.kei.core.prefs.KeiMmkv

/**
 * Explicit entry point for a configured business journey and an existing framework-built notification.
 * There is no implicit BA/MCP/download scene assignment and Media3 never enters this path.
 */
internal class VivoAtomicNotificationRuntime(context: Context) {
    private val context = context.applicationContext
    private val manager = this.context.getSystemService(NotificationManager::class.java)
    private val probe = VivoAtomicCapabilityProbe(this.context)
    private val dispatcher = VivoAtomicRuntimeOwner.dispatcher(this.context)
    private val bootId = runCatching {
        Settings.Global.getInt(this.context.contentResolver, Settings.Global.BOOT_COUNT).toString()
    }.getOrElse { VivoAtomicRuntimeOwner.processBootIdentity }

    @WorkerThread
    fun publish(
        ordinary: Notification,
        request: VivoAtomicRequest,
        binding: VivoAtomicSceneBinding?,
    ): VivoAtomicDispatchResult {
        require(request.bootId == bootId) { "Use this runtime's request clock and boot identity" }
        require(ordinary.category != Notification.CATEGORY_TRANSPORT &&
            !ordinary.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
        ) { "MediaSession notifications must bypass this runtime" }
        val channelExists = manager.getNotificationChannel(ordinary.channelId) != null
        val capability = if (binding != null && binding.business == request.business) {
            probe.inspect(context.packageName, binding.scene, channelExists)
        } else {
            VivoAtomicCapability(VivoAtomicSignal.DISABLED, channelExists = channelExists)
        }
        val wrapped = ordinary.clone().apply {
            deleteIntent = PendingIntent.getBroadcast(
                context, request.notificationId,
                Intent(context, VivoAtomicDismissReceiver::class.java).apply {
                    data = Uri.Builder().scheme("keios-internal").authority("vivo-atomic-dismiss")
                        .appendPath(request.activityId).build()
                    putExtra("activityId", request.activityId)
                    putExtra("originalDelete", ordinary.deleteIntent)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        return dispatcher.dispatch(wrapped, request, binding, capability)
    }

    fun request(
        activityId: String,
        notificationId: Int,
        business: String,
        content: os.kei.core.notification.live.oem.vivo.VivoAtomicContent,
        ended: Boolean = false,
    ) = VivoAtomicRequest(
        activityId, notificationId, business, bootId, SystemClock.elapsedRealtime(), content, ended,
    )
}

private object VivoAtomicRuntimeOwner {
    val processBootIdentity = "process:${UUID.randomUUID()}"
    private var sharedDispatcher: VivoAtomicDispatcher? = null

    @Synchronized
    fun dispatcher(context: Context): VivoAtomicDispatcher = sharedDispatcher ?: run {
        val manager = context.applicationContext.getSystemService(NotificationManager::class.java)
        VivoAtomicDispatcher(object : VivoAtomicNotificationTransport {
            override fun notify(notificationId: Int, notification: Notification) =
                manager.notify(notificationId, notification)
            override fun cancel(tag: String?, notificationId: Int) {
                if (tag == null) manager.cancel(notificationId) else manager.cancel(tag, notificationId)
            }
        }, VivoAtomicRuntimeRecords).also { sharedDispatcher = it }
    }
}

internal object VivoAtomicRuntimeRecords : VivoAtomicRecordStore {
    private val store by lazy { KeiMmkv.byId("vivo_atomic_lifecycle") }
    override fun load(activityId: String): VivoAtomicRecord? =
        store.decodeString(activityId)?.let(VivoAtomicRecordCodec::decode)
    override fun save(record: VivoAtomicRecord) {
        check(store.encode(record.activityId, VivoAtomicRecordCodec.encode(record)))
        store.sync()
    }
}

class VivoAtomicDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val activityId = intent.getStringExtra("activityId") ?: return
        val original = intent.getParcelableExtra("originalDelete", PendingIntent::class.java)
        BackgroundAsyncReceiverRunner.launch(this, context, "VivoAtomicDismiss", timeoutMs = 5_000) { app ->
            try {
                VivoAtomicRuntimeOwner.dispatcher(app).onUserRemoved(activityId)
            } finally {
                runCatching { original?.send() }
            }
        }
    }
}
