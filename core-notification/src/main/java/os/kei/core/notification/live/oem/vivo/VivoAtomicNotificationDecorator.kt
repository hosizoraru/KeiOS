package os.kei.core.notification.live.oem.vivo

import android.app.Notification
import android.os.Bundle

/** Decorates an already built standard notification; never posts or creates a second notification. */
object VivoAtomicNotificationDecorator {
    fun atomic(notification: Notification, action: VivoAtomicAction.PublishAtomic): Notification {
        require(notification.category != Notification.CATEGORY_TRANSPORT &&
            !notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
        ) { "MediaSession notifications must bypass the atomic adapter" }
        return notification.clone().apply {
            extras = VivoAtomicPayloadMapper.ordinaryExtras(notification.extras).apply {
                putAll(Bundle(action.extras))
            }
            flags = flags and Notification.FLAG_AUTO_CANCEL.inv()
        }
    }

    fun ordinary(notification: Notification): Notification = notification.clone().apply {
        extras = VivoAtomicPayloadMapper.ordinaryExtras(notification.extras)
    }
}
