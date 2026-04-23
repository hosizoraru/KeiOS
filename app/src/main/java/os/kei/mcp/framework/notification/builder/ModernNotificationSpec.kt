package os.kei.mcp.framework.notification.builder

import androidx.core.app.NotificationCompat
import os.kei.R
import os.kei.mcp.notification.McpNotificationPayload
import kotlin.math.roundToInt

internal enum class ModernNotificationKind {
    DEFAULT,
    BA_AP,
    BA_CAFE_VISIT,
    BA_ARENA_REFRESH
}

internal enum class ModernShortCriticalMode {
    NONE,
    SHORT_TEXT,
    ONLINE_TEXT
}

internal data class ModernNotificationSpec(
    val kind: ModernNotificationKind,
    val iconResId: Int,
    val progressPercent: Int,
    val progressColor: Int,
    val category: String,
    val shortCriticalMode: ModernShortCriticalMode,
    val ongoing: Boolean,
    val requestPromotedOngoing: Boolean
)

internal object ModernNotificationSpecResolver {
    private const val PROGRESS_ACTIVE_COLOR = 0xFF2E7D32.toInt()
    private const val PROGRESS_IDLE_COLOR = 0xFF64748B.toInt()
    private const val ICON_DEFAULT = R.drawable.ic_kei_logo_live_update
    private const val ICON_AP = R.drawable.ba_ap_icon
    private const val ICON_BA_CAFE_VISIT = R.drawable.ic_ba_schale
    private const val ICON_BA_ARENA_REFRESH = R.drawable.ic_ba_schale

    fun resolve(state: McpNotificationPayload): ModernNotificationSpec {
        val kind = resolveKind(state.serverName)
        val isRunning = state.running
        return ModernNotificationSpec(
            kind = kind,
            iconResId = resolveIcon(kind),
            progressPercent = resolveProgressPercent(state = state, kind = kind),
            progressColor = if (isRunning) PROGRESS_ACTIVE_COLOR else PROGRESS_IDLE_COLOR,
            category = if (isRunning) {
                NotificationCompat.CATEGORY_PROGRESS
            } else {
                NotificationCompat.CATEGORY_STATUS
            },
            shortCriticalMode = if (isRunning) resolveShortCriticalMode(kind) else ModernShortCriticalMode.NONE,
            ongoing = isRunning || state.ongoing,
            requestPromotedOngoing = isRunning || state.ongoing
        )
    }

    private fun resolveKind(serverName: String): ModernNotificationKind {
        return when {
            McpNotificationPayload.isBaApServerName(serverName) -> ModernNotificationKind.BA_AP
            McpNotificationPayload.isBaCafeVisitServerName(serverName) -> ModernNotificationKind.BA_CAFE_VISIT
            McpNotificationPayload.isBaArenaRefreshServerName(serverName) -> ModernNotificationKind.BA_ARENA_REFRESH
            else -> ModernNotificationKind.DEFAULT
        }
    }

    private fun resolveIcon(kind: ModernNotificationKind): Int {
        return when (kind) {
            ModernNotificationKind.DEFAULT -> ICON_DEFAULT
            ModernNotificationKind.BA_AP -> ICON_AP
            ModernNotificationKind.BA_CAFE_VISIT -> ICON_BA_CAFE_VISIT
            ModernNotificationKind.BA_ARENA_REFRESH -> ICON_BA_ARENA_REFRESH
        }
    }

    private fun resolveShortCriticalMode(kind: ModernNotificationKind): ModernShortCriticalMode {
        return when (kind) {
            ModernNotificationKind.BA_CAFE_VISIT,
            ModernNotificationKind.BA_ARENA_REFRESH -> ModernShortCriticalMode.ONLINE_TEXT

            ModernNotificationKind.DEFAULT,
            ModernNotificationKind.BA_AP -> ModernShortCriticalMode.SHORT_TEXT
        }
    }

    private fun resolveProgressPercent(
        state: McpNotificationPayload,
        kind: ModernNotificationKind
    ): Int {
        if (!state.running) return 0
        return when (kind) {
            ModernNotificationKind.BA_CAFE_VISIT,
            ModernNotificationKind.BA_ARENA_REFRESH -> 100

            ModernNotificationKind.BA_AP -> {
                val apLimit = state.clients.coerceAtLeast(1)
                val apCurrent = state.port.coerceAtLeast(0).coerceAtMost(apLimit)
                ((apCurrent.toFloat() / apLimit.toFloat()) * 100f)
                    .roundToInt()
                    .coerceIn(0, 100)
            }

            ModernNotificationKind.DEFAULT -> {
                (state.clients.coerceAtLeast(0) * 24)
                    .coerceIn(8, 100)
            }
        }
    }
}
