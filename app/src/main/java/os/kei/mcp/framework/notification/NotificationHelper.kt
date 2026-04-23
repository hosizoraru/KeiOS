package os.kei.mcp.framework.notification

import android.content.Context
import android.os.Build
import com.xzakota.hyper.notification.focus.util.FocusUtils
import os.kei.core.system.findPropString
import os.kei.mcp.notification.McpNotificationHelper
import os.kei.mcp.framework.notification.builder.NotificationRenderStyle

class NotificationHelper(
    val context: Context
) {

    enum class Channel(val value: String) {
        KeepAlive(McpNotificationHelper.CHANNEL_ID),
        LiveUpdate(McpNotificationHelper.LIVE_CHANNEL_ID)
    }

    val isHyperOS: Boolean by lazy {
        findPropString("ro.mi.os.version.name").startsWith("OS")
    }

    val isSupportMiIsland: Boolean by lazy {
        runCatching {
            FocusUtils.getFocusProtocolVersion(context) == 3
        }.getOrDefault(false)
    }

    val hasMiIslandPermission: Boolean by lazy {
        runCatching {
            FocusUtils.hasFocusPermission(context)
        }.getOrDefault(false)
    }

    val isMiIslandAvailable: Boolean
        get() = isHyperOS && isSupportMiIsland && hasMiIslandPermission

    val isModernLiveUpdateEligible: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA

    fun resolveChannel(style: NotificationRenderStyle): String {
        return when (style) {
            NotificationRenderStyle.MI_ISLAND -> Channel.KeepAlive.value
            NotificationRenderStyle.LIVE_UPDATE -> Channel.LiveUpdate.value
            NotificationRenderStyle.LEGACY -> Channel.KeepAlive.value
        }
    }
}
