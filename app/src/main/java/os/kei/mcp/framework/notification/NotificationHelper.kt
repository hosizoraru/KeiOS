package os.kei.mcp.framework.notification

import android.content.Context
import android.os.Build
import com.xzakota.hyper.notification.focus.util.FocusUtils
import os.kei.core.platform.AndroidPlatformVersions
import os.kei.core.system.findPropString
import os.kei.mcp.notification.McpNotificationHelper
import os.kei.mcp.framework.notification.builder.NotificationRenderStyle
import java.util.Locale

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

    val preferOemLiveIconLayout: Boolean by lazy {
        isHyperOS || isColorOsFamily()
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
        get() = AndroidPlatformVersions.isAtLeastAndroid16

    fun resolveChannel(style: NotificationRenderStyle): String {
        return when (style) {
            NotificationRenderStyle.MI_ISLAND -> Channel.KeepAlive.value
            NotificationRenderStyle.LIVE_UPDATE -> Channel.LiveUpdate.value
            NotificationRenderStyle.LEGACY -> Channel.KeepAlive.value
        }
    }

    private fun isColorOsFamily(): Boolean {
        val buildFields = listOf(
            Build.BRAND,
            Build.MANUFACTURER,
            Build.DISPLAY
        ).joinToString(separator = " ").lowercase(Locale.ROOT)
        if (
            listOf("oppo", "oneplus", "realme", "coloros", "oplus")
                .any(buildFields::contains)
        ) {
            return true
        }
        if (listOf(
            "ro.build.version.opporom",
            "ro.build.version.oplusrom",
            "ro.build.version.realmeui",
            "ro.oplus.version"
        ).any { key -> findPropString(key).isNotBlank() }) {
            return true
        }
        val romVersion = findPropString("ro.rom.version").lowercase(Locale.ROOT)
        return listOf("oppo", "oneplus", "realme", "coloros", "oplus")
            .any(romVersion::contains)
    }
}
