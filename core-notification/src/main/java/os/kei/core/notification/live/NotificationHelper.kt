package os.kei.core.notification.live

import android.content.Context
import os.kei.core.notification.focus.MiFocusCapabilityAdapter
import os.kei.core.notification.live.builder.NotificationRenderStyle
import os.kei.core.platform.AndroidPlatformVersions
import os.kei.core.system.findPropString

class NotificationHelper(
    val context: Context,
    private val channels: LiveNotificationChannels = LiveNotificationChannels.DefaultMcp
) {
    val isHyperOS: Boolean by lazy {
        findPropString("ro.mi.os.version.name").startsWith("OS")
    }

    val miIslandFocusProtocolVersion: Int by lazy {
        runCatching {
            MiFocusCapabilityAdapter.getFocusProtocolVersion(context)
        }.getOrDefault(0)
    }

    val isSupportMiIslandFeature: Boolean by lazy {
        runCatching {
            MiFocusCapabilityAdapter.isSupportIsland()
        }.getOrDefault(false)
    }

    val isSupportMiIsland: Boolean
        get() = miIslandCapability.canBuildMiIslandTemplate &&
            miIslandCapability.hasRuntimeIslandSignal

    val hasMiIslandPermission: Boolean by lazy {
        runCatching {
            MiFocusCapabilityAdapter.hasFocusPermission(context)
        }.getOrDefault(false)
    }

    val miIslandCapability: MiIslandCapability
        get() = MiIslandCapability(
            isHyperOS = isHyperOS,
            focusProtocolVersion = miIslandFocusProtocolVersion,
            supportsIslandFeature = isSupportMiIslandFeature,
            hasFocusPermission = hasMiIslandPermission,
        )

    val isMiIslandAvailable: Boolean
        get() = isHyperOS && isSupportMiIsland && hasMiIslandPermission

    val isModernLiveUpdateEligible: Boolean
        get() = AndroidPlatformVersions.isAtLeastAndroid16

    fun resolveChannel(style: NotificationRenderStyle): String {
        return when (style) {
            NotificationRenderStyle.MI_ISLAND -> channels.islandChannelId
            NotificationRenderStyle.LIVE_UPDATE -> channels.liveUpdateChannelId
            NotificationRenderStyle.LEGACY -> channels.legacyChannelId
        }
    }

    fun resolveRenderDecision(
        preferSuperIsland: Boolean,
        bypassRestriction: Boolean,
    ): MiIslandRenderDecision =
        MiIslandRenderPolicy.resolve(
            preferSuperIsland = preferSuperIsland,
            bypassRestriction = bypassRestriction,
            capability = miIslandCapability,
        )
}

data class LiveNotificationChannels(
    val islandChannelId: String,
    val liveUpdateChannelId: String,
    val legacyChannelId: String = islandChannelId
) {
    companion object {
        val DefaultMcp = LiveNotificationChannels(
            islandChannelId = "mcp_keepalive_channel_v2",
            liveUpdateChannelId = "mcp_live_update_channel_v1"
        )
    }
}
