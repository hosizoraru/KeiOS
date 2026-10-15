package os.kei.core.notification.live.builder

import androidx.core.app.NotificationCompat
import os.kei.core.notification.R
import os.kei.core.notification.live.LiveNotificationPayload
import kotlin.math.roundToInt

enum class ModernNotificationKind {
    DEFAULT,
    BA_AP,
    BA_CAFE_AP,
    BA_CAFE_VISIT,
    BA_ARENA_REFRESH,
    BA_CALENDAR_POOL,
    BA_DAILY_DONE,
    GITHUB_SHARE_IMPORT,
    WEBDAV_SYNC
}

enum class ModernShortCriticalMode {
    NONE,
    SHORT_TEXT,
    ONLINE_TEXT
}

data class ModernNotificationSpec(
    val kind: ModernNotificationKind,
    val iconResId: Int,
    val expandedIconResId: Int?,
    val trackerIconResId: Int?,
    val progressPercent: Int,
    val progressColor: Int,
    val category: String,
    val shortCriticalMode: ModernShortCriticalMode,
    val ongoing: Boolean,
    val requestPromotedOngoing: Boolean,
    val showProgressStyle: Boolean
)

object ModernNotificationSpecResolver {
    private const val PROGRESS_ACTIVE_COLOR = 0xFF2E7D32.toInt()
    private const val PROGRESS_IDLE_COLOR = 0xFF64748B.toInt()
    private const val PROGRESS_GITHUB_SHARE_IMPORT_COLOR = 0xFF2563EB.toInt()
    private const val PROGRESS_WEBDAV_COLOR = 0xFF2563EB.toInt()
    private val ICON_DEFAULT = R.drawable.ic_kei_notification_small
    private val ICON_BA_AP = R.drawable.ic_ba_ap_island_notification
    private val ICON_BA_CAFE_VISIT = R.drawable.ic_ba_tea_party_island
    private val ICON_BA_ARENA_REFRESH = R.drawable.ic_ba_arena_coin_island
    private val ICON_BA_CALENDAR_POOL = R.drawable.ic_ba_calendar_live_update
    private val ICON_BA_DAILY_DONE = R.drawable.ic_ba_daily_done_island
    private val ICON_GITHUB_SHARE_IMPORT = R.drawable.ic_github_invertocat_island_blue
    private val CONTENT_ICON_AP = R.drawable.ic_ba_ap_live_update
    private val CONTENT_ICON_BA_CAFE_VISIT = R.drawable.ic_ba_tea_party_live_update
    private val CONTENT_ICON_BA_ARENA_REFRESH = R.drawable.ic_ba_arena_coin_live_update
    private val CONTENT_ICON_BA_CALENDAR_POOL = R.drawable.ic_ba_calendar_live_update
    private val CONTENT_ICON_BA_DAILY_DONE = R.drawable.ic_ba_daily_done_live_update
    private val CONTENT_ICON_GITHUB_SHARE_IMPORT = R.drawable.ic_github_invertocat_island_blue

    fun resolve(
        state: LiveNotificationPayload,
        @androidx.annotation.DrawableRes defaultAppIconResId: Int = ICON_DEFAULT,
    ): ModernNotificationSpec {
        val kind = resolveKind(state.serverName)
        val isRunning = state.running
        val isCalendarPoolTerminal =
            kind == ModernNotificationKind.BA_CALENDAR_POOL && state.deadlineAtMs == null
        // Daily-done belongs here for the same reason cafe visit and arena refresh do: it reports an
        // event that has already happened. Without it the trailing branches below made a finished run an
        // ongoing PROMOTED_ONGOING ProgressStyle notification with a progress bar — verified on the AVD,
        // where it also arrived un-dismissible because ONGOING_EVENT was set. `requestPromotedOngoing`
        // tracks `ongoing` here, so getting `ongoing` right is what stops the promoted request too.
        val isOneShotBaEvent =
            kind == ModernNotificationKind.BA_CAFE_VISIT ||
                kind == ModernNotificationKind.BA_ARENA_REFRESH ||
                kind == ModernNotificationKind.BA_DAILY_DONE
        val ongoing = if (isCalendarPoolTerminal || isOneShotBaEvent) {
            state.ongoing
        } else {
            isRunning || state.ongoing
        }
        val showProgressStyle = when {
            isCalendarPoolTerminal || isOneShotBaEvent -> false
            kind == ModernNotificationKind.WEBDAV_SYNC -> isRunning
            kind == ModernNotificationKind.GITHUB_SHARE_IMPORT ->
                isRunning && state.clients > 0 && state.overrideProgressPercent != null
            else -> true
        }
        return ModernNotificationSpec(
            kind = kind,
            iconResId = resolveIcon(kind, defaultAppIconResId),
            expandedIconResId = resolveExpandedIcon(kind),
            trackerIconResId = resolveTrackerIcon(kind),
            progressPercent = resolveProgressPercent(state = state, kind = kind),
            progressColor = resolveProgressColor(
                state = state,
                kind = kind,
                isRunning = isRunning,
            ),
            category = if (isRunning && !isCalendarPoolTerminal && !isOneShotBaEvent) {
                NotificationCompat.CATEGORY_PROGRESS
            } else {
                NotificationCompat.CATEGORY_STATUS
            },
            shortCriticalMode = if (isRunning) resolveShortCriticalMode(kind) else ModernShortCriticalMode.NONE,
            ongoing = ongoing,
            requestPromotedOngoing = ongoing,
            showProgressStyle = showProgressStyle
        )
    }

    private fun resolveProgressColor(
        state: LiveNotificationPayload,
        kind: ModernNotificationKind,
        isRunning: Boolean
    ): Int {
        state.overrideAccentColor?.toNotificationColorOrNull()?.let { return it }
        if (!isRunning) return PROGRESS_IDLE_COLOR
        return when (kind) {
            ModernNotificationKind.GITHUB_SHARE_IMPORT -> PROGRESS_GITHUB_SHARE_IMPORT_COLOR
            ModernNotificationKind.WEBDAV_SYNC -> PROGRESS_WEBDAV_COLOR
            else -> PROGRESS_ACTIVE_COLOR
        }
    }

    private fun resolveKind(serverName: String): ModernNotificationKind {
        return when {
            LiveNotificationPayload.isBaApServerName(serverName) -> ModernNotificationKind.BA_AP
            LiveNotificationPayload.isBaCafeApServerName(serverName) -> ModernNotificationKind.BA_CAFE_AP
            LiveNotificationPayload.isBaCafeVisitServerName(serverName) -> ModernNotificationKind.BA_CAFE_VISIT
            LiveNotificationPayload.isBaArenaRefreshServerName(serverName) -> ModernNotificationKind.BA_ARENA_REFRESH
            LiveNotificationPayload.isBaCalendarPoolServerName(serverName) -> ModernNotificationKind.BA_CALENDAR_POOL
            LiveNotificationPayload.isBaDailyDoneServerName(serverName) -> ModernNotificationKind.BA_DAILY_DONE
            LiveNotificationPayload.isGitHubShareImportServerName(serverName) -> ModernNotificationKind.GITHUB_SHARE_IMPORT
            LiveNotificationPayload.isWebDavSyncServerName(serverName) -> ModernNotificationKind.WEBDAV_SYNC
            else -> ModernNotificationKind.DEFAULT
        }
    }

    private fun resolveIcon(
        kind: ModernNotificationKind,
        defaultAppIconResId: Int,
    ): Int {
        return when (kind) {
            ModernNotificationKind.DEFAULT -> defaultAppIconResId

            ModernNotificationKind.BA_AP,
            ModernNotificationKind.BA_CAFE_AP,
            ModernNotificationKind.BA_CAFE_VISIT,
            ModernNotificationKind.BA_ARENA_REFRESH,
            ModernNotificationKind.BA_CALENDAR_POOL,
            ModernNotificationKind.BA_DAILY_DONE,
            ModernNotificationKind.GITHUB_SHARE_IMPORT,
            ModernNotificationKind.WEBDAV_SYNC ->
                resolveSemanticCompactIcon(kind, defaultAppIconResId)
        }
    }

    private fun resolveSemanticCompactIcon(
        kind: ModernNotificationKind,
        defaultAppIconResId: Int = ICON_DEFAULT,
    ): Int {
        return when (kind) {
            ModernNotificationKind.BA_AP -> ICON_BA_AP
            ModernNotificationKind.BA_CAFE_AP -> ICON_BA_AP
            ModernNotificationKind.BA_CAFE_VISIT -> ICON_BA_CAFE_VISIT
            ModernNotificationKind.BA_ARENA_REFRESH -> ICON_BA_ARENA_REFRESH
            ModernNotificationKind.BA_CALENDAR_POOL -> ICON_BA_CALENDAR_POOL
            ModernNotificationKind.BA_DAILY_DONE -> ICON_BA_DAILY_DONE
            ModernNotificationKind.GITHUB_SHARE_IMPORT -> ICON_GITHUB_SHARE_IMPORT
            ModernNotificationKind.WEBDAV_SYNC -> defaultAppIconResId
            ModernNotificationKind.DEFAULT -> defaultAppIconResId
        }
    }

    private fun resolveExpandedIcon(kind: ModernNotificationKind): Int? {
        return resolveContentIcon(kind)
    }

    private fun resolveTrackerIcon(kind: ModernNotificationKind): Int? {
        return resolveContentIcon(kind)
    }

    private fun resolveContentIcon(kind: ModernNotificationKind): Int? {
        return when (kind) {
            ModernNotificationKind.DEFAULT -> null
            ModernNotificationKind.BA_AP -> CONTENT_ICON_AP
            ModernNotificationKind.BA_CAFE_AP -> CONTENT_ICON_AP
            ModernNotificationKind.BA_CAFE_VISIT -> CONTENT_ICON_BA_CAFE_VISIT
            ModernNotificationKind.BA_ARENA_REFRESH -> CONTENT_ICON_BA_ARENA_REFRESH
            ModernNotificationKind.BA_CALENDAR_POOL -> CONTENT_ICON_BA_CALENDAR_POOL
            ModernNotificationKind.BA_DAILY_DONE -> CONTENT_ICON_BA_DAILY_DONE
            ModernNotificationKind.GITHUB_SHARE_IMPORT -> CONTENT_ICON_GITHUB_SHARE_IMPORT
            ModernNotificationKind.WEBDAV_SYNC -> null
        }
    }

    private fun resolveShortCriticalMode(kind: ModernNotificationKind): ModernShortCriticalMode {
        return when (kind) {
            ModernNotificationKind.BA_CAFE_VISIT,
            ModernNotificationKind.BA_ARENA_REFRESH -> ModernShortCriticalMode.ONLINE_TEXT

            // The dispatcher supplies a terminal short word in `overrideShortText`; ONLINE_TEXT would
            // reach for the server-status wording instead, which is the same mistake the island had.
            ModernNotificationKind.BA_DAILY_DONE -> ModernShortCriticalMode.SHORT_TEXT

            ModernNotificationKind.DEFAULT,
            ModernNotificationKind.BA_AP,
            ModernNotificationKind.BA_CAFE_AP,
            ModernNotificationKind.BA_CALENDAR_POOL,
            ModernNotificationKind.GITHUB_SHARE_IMPORT,
            ModernNotificationKind.WEBDAV_SYNC -> ModernShortCriticalMode.SHORT_TEXT
        }
    }

    private fun resolveProgressPercent(
        state: LiveNotificationPayload,
        kind: ModernNotificationKind
    ): Int {
        if (!state.running) return 0
        return when (kind) {
            ModernNotificationKind.BA_CAFE_VISIT,
            ModernNotificationKind.BA_ARENA_REFRESH -> 100

            // Full, not zero. `showProgressStyle` is false for this kind so nothing draws a bar, but the
            // value still reaches the tracker; reporting a finished run as 0% would render it as work
            // that never started if a surface ever chooses to show it.
            ModernNotificationKind.BA_DAILY_DONE -> 100

            ModernNotificationKind.BA_CALENDAR_POOL -> {
                state.overrideProgressPercent?.coerceIn(0, 100) ?: 100
            }

            ModernNotificationKind.GITHUB_SHARE_IMPORT -> {
                state.overrideProgressPercent
                    ?.coerceIn(0, 100)
                    ?: state.port.coerceIn(0, 100)
            }

            ModernNotificationKind.WEBDAV_SYNC -> {
                state.overrideProgressPercent
                    ?.coerceIn(0, 100)
                    ?: state.port.coerceIn(0, 100)
            }

            ModernNotificationKind.BA_AP,
            ModernNotificationKind.BA_CAFE_AP -> {
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

    private fun String.toNotificationColorOrNull(): Int? {
        val hex = trim()
            .removePrefix("#")
            .takeIf { it.length == 6 || it.length == 8 }
            ?: return null
        val raw = hex.toLongOrNull(radix = 16) ?: return null
        return if (hex.length == 6) {
            (0xFF000000L or raw).toInt()
        } else {
            raw.toInt()
        }
    }
}
