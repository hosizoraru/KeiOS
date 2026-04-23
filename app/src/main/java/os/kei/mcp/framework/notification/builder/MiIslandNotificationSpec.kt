package os.kei.mcp.framework.notification.builder

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import os.kei.R
import os.kei.core.notification.focus.MiFocusNotificationAction
import os.kei.core.notification.focus.MiFocusNotificationCompactMode
import os.kei.core.notification.focus.MiFocusNotificationDisplayIconStyle
import os.kei.core.notification.focus.MiFocusNotificationExpandedMode
import os.kei.core.notification.focus.MiFocusNotificationSpec
import os.kei.mcp.notification.McpNotificationPayload
import kotlin.math.roundToInt

internal data class MiIslandNotificationAction(
    val title: String,
    val pendingIntent: PendingIntent
)

internal data class MiIslandNotificationSpec(
    val smallIconResId: Int,
    val category: String,
    val ongoing: Boolean,
    val requestPromotedOngoing: Boolean,
    val foregroundServiceImmediate: Boolean,
    val shortCriticalText: String?,
    val progressPercent: Int,
    val showExpandedProgress: Boolean,
    val regularActions: List<MiIslandNotificationAction>,
    val focusSpec: MiFocusNotificationSpec
)

internal enum class MiIslandNotificationVariant {
    DEFAULT_RUNNING,
    DEFAULT_STOPPED,
    BA_AP_RUNNING,
    BA_CAFE_VISIT_RUNNING,
    BA_ARENA_REFRESH_RUNNING
}

internal data class MiIslandNotificationBehavior(
    val variant: MiIslandNotificationVariant,
    val compactMode: MiFocusNotificationCompactMode,
    val expandedMode: MiFocusNotificationExpandedMode,
    val showNotification: Boolean,
    val updatable: Boolean,
    val embedTextButtons: Boolean,
    val allowFloat: Boolean
)

internal object MiIslandNotificationSpecResolver {
    private const val BA_AP_PROGRESS_COLOR = "#4DA3FF"
    private const val BA_AP_PROGRESS_TRACK_COLOR = "#374151"
    private const val ISLAND_ICON_RES_ID_DEFAULT = R.drawable.ic_kei_logo_island
    private const val ISLAND_ICON_RES_ID_AP = R.drawable.ic_ba_ap_island_notification
    private const val ISLAND_ICON_RES_ID_BA_CAFE_VISIT = R.drawable.ic_ba_schale_island
    private const val ISLAND_ICON_RES_ID_BA_ARENA_REFRESH = R.drawable.ic_ba_schale_island

    fun resolve(
        context: Context,
        payload: NotificationPayload
    ): MiIslandNotificationSpec {
        val state = payload.state
        val isBlueArchiveAp = McpNotificationPayload.isBaApServerName(state.serverName)
        val isBlueArchiveCafeVisit = McpNotificationPayload.isBaCafeVisitServerName(state.serverName)
        val isBlueArchiveArenaRefresh = McpNotificationPayload.isBaArenaRefreshServerName(state.serverName)
        val isBlueArchiveNotification =
            isBlueArchiveAp || isBlueArchiveCafeVisit || isBlueArchiveArenaRefresh
        val behavior = resolveBehavior(state)
        val smallIconResId = when {
            isBlueArchiveAp -> ISLAND_ICON_RES_ID_AP
            isBlueArchiveCafeVisit -> ISLAND_ICON_RES_ID_BA_CAFE_VISIT
            isBlueArchiveArenaRefresh -> ISLAND_ICON_RES_ID_BA_ARENA_REFRESH
            else -> ISLAND_ICON_RES_ID_DEFAULT
        }
        val embeddedActions = listOf(
            MiFocusNotificationAction(
                key = "mcp_action_open",
                title = context.getString(R.string.common_open),
                pendingIntent = payload.state.openPendingIntent,
                isHighlighted = true
            ),
            MiFocusNotificationAction(
                key = "mcp_action_secondary",
                title = state.stopActionTitle(context),
                pendingIntent = state.stopPendingIntent
            )
        )
        return when (behavior.variant) {
            MiIslandNotificationVariant.BA_AP_RUNNING -> resolveBaAp(
                payload = payload,
                context = context,
                behavior = behavior,
                smallIconResId = smallIconResId,
                actions = embeddedActions
            )

            MiIslandNotificationVariant.BA_CAFE_VISIT_RUNNING -> resolveBaEvent(
                payload = payload,
                context = context,
                behavior = behavior,
                smallIconResId = smallIconResId,
                actions = embeddedActions,
                compactTitle = context.getString(R.string.ba_cafe_visit_notification_island_text)
            )

            MiIslandNotificationVariant.BA_ARENA_REFRESH_RUNNING -> resolveBaEvent(
                payload = payload,
                context = context,
                behavior = behavior,
                smallIconResId = smallIconResId,
                actions = embeddedActions,
                compactTitle = context.getString(R.string.ba_arena_refresh_notification_island_text)
            )

            MiIslandNotificationVariant.DEFAULT_RUNNING -> resolveDefaultRunning(
                payload = payload,
                context = context,
                behavior = behavior,
                smallIconResId = smallIconResId,
                isBlueArchiveNotification = isBlueArchiveNotification
            )

            MiIslandNotificationVariant.DEFAULT_STOPPED -> resolveStopped(
                payload = payload,
                context = context,
                behavior = behavior,
                smallIconResId = smallIconResId,
                actions = embeddedActions,
                isBlueArchiveNotification = isBlueArchiveNotification
            )
        }
    }

    internal fun resolveBehavior(state: McpNotificationPayload): MiIslandNotificationBehavior {
        return when {
            McpNotificationPayload.isBaApServerName(state.serverName) && state.running -> {
                MiIslandNotificationBehavior(
                    variant = MiIslandNotificationVariant.BA_AP_RUNNING,
                    compactMode = MiFocusNotificationCompactMode.PROGRESS,
                    expandedMode = MiFocusNotificationExpandedMode.BASE_INFO,
                    showNotification = true,
                    updatable = true,
                    embedTextButtons = true,
                    allowFloat = false
                )
            }

            McpNotificationPayload.isBaCafeVisitServerName(state.serverName) && state.running -> {
                MiIslandNotificationBehavior(
                    variant = MiIslandNotificationVariant.BA_CAFE_VISIT_RUNNING,
                    compactMode = MiFocusNotificationCompactMode.TEXT,
                    expandedMode = MiFocusNotificationExpandedMode.ICON_TEXT,
                    showNotification = true,
                    updatable = false,
                    embedTextButtons = true,
                    allowFloat = true
                )
            }

            McpNotificationPayload.isBaArenaRefreshServerName(state.serverName) && state.running -> {
                MiIslandNotificationBehavior(
                    variant = MiIslandNotificationVariant.BA_ARENA_REFRESH_RUNNING,
                    compactMode = MiFocusNotificationCompactMode.TEXT,
                    expandedMode = MiFocusNotificationExpandedMode.ICON_TEXT,
                    showNotification = true,
                    updatable = false,
                    embedTextButtons = true,
                    allowFloat = true
                )
            }

            state.running -> {
                MiIslandNotificationBehavior(
                    variant = MiIslandNotificationVariant.DEFAULT_RUNNING,
                    compactMode = MiFocusNotificationCompactMode.TEXT,
                    expandedMode = MiFocusNotificationExpandedMode.BASE_INFO,
                    showNotification = false,
                    updatable = true,
                    embedTextButtons = false,
                    allowFloat = false
                )
            }

            else -> {
                MiIslandNotificationBehavior(
                    variant = MiIslandNotificationVariant.DEFAULT_STOPPED,
                    compactMode = MiFocusNotificationCompactMode.TEXT,
                    expandedMode = MiFocusNotificationExpandedMode.ICON_TEXT,
                    showNotification = true,
                    updatable = false,
                    embedTextButtons = true,
                    allowFloat = true
                )
            }
        }
    }

    private fun resolveBaAp(
        payload: NotificationPayload,
        context: Context,
        behavior: MiIslandNotificationBehavior,
        smallIconResId: Int,
        actions: List<MiFocusNotificationAction>
    ): MiIslandNotificationSpec {
        val state = payload.state
        val progressPercent = resolveApProgressPercent(state)
        return MiIslandNotificationSpec(
            smallIconResId = smallIconResId,
            category = NotificationCompat.CATEGORY_PROGRESS,
            ongoing = true,
            requestPromotedOngoing = false,
            foregroundServiceImmediate = false,
            shortCriticalText = state.shortText.takeIf { it.isNotBlank() },
            progressPercent = progressPercent,
            showExpandedProgress = true,
            regularActions = emptyList(),
            focusSpec = MiFocusNotificationSpec(
                title = state.title(context),
                content = state.expandedContent(context),
                compactTitle = state.shortText,
                displayIconResId = smallIconResId,
                tickerIconResId = smallIconResId,
                compactMode = behavior.compactMode,
                expandedMode = behavior.expandedMode,
                displayIconStyle = MiFocusNotificationDisplayIconStyle.ORIGINAL,
                allowFloat = behavior.allowFloat,
                outerGlow = payload.settings.miIslandOuterGlow,
                updatable = behavior.updatable,
                showNotification = behavior.showNotification,
                progressPercent = progressPercent,
                progressColor = BA_AP_PROGRESS_COLOR,
                progressTrackColor = BA_AP_PROGRESS_TRACK_COLOR,
                showExpandedProgress = true,
                embedTextButtons = behavior.embedTextButtons,
                actions = actions
            )
        )
    }

    private fun resolveBaEvent(
        payload: NotificationPayload,
        context: Context,
        behavior: MiIslandNotificationBehavior,
        smallIconResId: Int,
        actions: List<MiFocusNotificationAction>,
        compactTitle: String
    ): MiIslandNotificationSpec {
        val state = payload.state
        return MiIslandNotificationSpec(
            smallIconResId = smallIconResId,
            category = NotificationCompat.CATEGORY_STATUS,
            ongoing = false,
            requestPromotedOngoing = false,
            foregroundServiceImmediate = false,
            shortCriticalText = state.onlineText(context).takeIf { it.isNotBlank() },
            progressPercent = 0,
            showExpandedProgress = false,
            regularActions = emptyList(),
            focusSpec = MiFocusNotificationSpec(
                title = state.title(context),
                content = state.expandedContent(context),
                compactTitle = compactTitle,
                compactContent = resolveBaEventDetail(context, payload),
                displayIconResId = smallIconResId,
                tickerIconResId = smallIconResId,
                compactMode = behavior.compactMode,
                expandedMode = behavior.expandedMode,
                displayIconStyle = MiFocusNotificationDisplayIconStyle.ORIGINAL,
                allowFloat = behavior.allowFloat,
                outerGlow = payload.settings.miIslandOuterGlow,
                updatable = behavior.updatable,
                showNotification = behavior.showNotification,
                embedTextButtons = behavior.embedTextButtons,
                actions = actions
            )
        )
    }

    private fun resolveDefaultRunning(
        payload: NotificationPayload,
        context: Context,
        behavior: MiIslandNotificationBehavior,
        smallIconResId: Int,
        isBlueArchiveNotification: Boolean
    ): MiIslandNotificationSpec {
        val state = payload.state
        val regularActions = buildList {
            add(
                MiIslandNotificationAction(
                    title = context.getString(R.string.common_open),
                    pendingIntent = state.openPendingIntent
                )
            )
            add(
                MiIslandNotificationAction(
                    title = state.stopActionTitle(context),
                    pendingIntent = state.stopPendingIntent
                )
            )
        }
        return MiIslandNotificationSpec(
            smallIconResId = smallIconResId,
            category = if (isBlueArchiveNotification) {
                NotificationCompat.CATEGORY_PROGRESS
            } else {
                NotificationCompat.CATEGORY_SERVICE
            },
            ongoing = state.ongoing,
            requestPromotedOngoing = state.ongoing && !isBlueArchiveNotification,
            foregroundServiceImmediate = !isBlueArchiveNotification,
            shortCriticalText = state.onlineText(context).takeIf { it.isNotBlank() },
            progressPercent = 0,
            showExpandedProgress = false,
            regularActions = regularActions,
            focusSpec = MiFocusNotificationSpec(
                title = state.title(context),
                content = state.expandedContent(context),
                compactTitle = state.onlineText(context),
                compactContent = resolveDefaultEndpointSummary(state),
                displayIconResId = smallIconResId,
                tickerIconResId = smallIconResId,
                compactMode = behavior.compactMode,
                expandedMode = behavior.expandedMode,
                displayIconStyle = MiFocusNotificationDisplayIconStyle.MONO_TINTED,
                allowFloat = behavior.allowFloat,
                outerGlow = payload.settings.miIslandOuterGlow,
                updatable = behavior.updatable,
                showNotification = behavior.showNotification
            )
        )
    }

    private fun resolveStopped(
        payload: NotificationPayload,
        context: Context,
        behavior: MiIslandNotificationBehavior,
        smallIconResId: Int,
        actions: List<MiFocusNotificationAction>,
        isBlueArchiveNotification: Boolean
    ): MiIslandNotificationSpec {
        val state = payload.state
        return MiIslandNotificationSpec(
            smallIconResId = smallIconResId,
            category = NotificationCompat.CATEGORY_STATUS,
            ongoing = state.ongoing,
            requestPromotedOngoing = false,
            foregroundServiceImmediate = !isBlueArchiveNotification && state.ongoing,
            shortCriticalText = state.statusText(context).takeIf { it.isNotBlank() },
            progressPercent = 0,
            showExpandedProgress = false,
            regularActions = emptyList(),
            focusSpec = MiFocusNotificationSpec(
                title = state.title(context),
                content = state.expandedContent(context),
                compactTitle = state.statusText(context),
                displayIconResId = smallIconResId,
                tickerIconResId = smallIconResId,
                compactMode = behavior.compactMode,
                expandedMode = behavior.expandedMode,
                displayIconStyle = if (isBlueArchiveNotification) {
                    MiFocusNotificationDisplayIconStyle.ORIGINAL
                } else {
                    MiFocusNotificationDisplayIconStyle.MONO_TINTED
                },
                allowFloat = behavior.allowFloat,
                outerGlow = payload.settings.miIslandOuterGlow,
                updatable = behavior.updatable,
                showNotification = behavior.showNotification,
                embedTextButtons = behavior.embedTextButtons,
                actions = actions
            )
        )
    }

    private fun resolveDefaultEndpointSummary(state: McpNotificationPayload): String? {
        if (!state.running) return null
        val path = state.path.trim().ifBlank { "/mcp" }
        val port = state.port.coerceAtLeast(0)
        return if (path.length <= 12) {
            "$port $path"
        } else {
            port.toString()
        }
    }

    private fun resolveApProgressPercent(state: McpNotificationPayload): Int {
        if (!state.running) return 0
        val limit = state.clients.coerceAtLeast(1)
        val current = state.port.coerceAtLeast(0).coerceAtMost(limit)
        return ((current.toFloat() / limit.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
    }

    private fun resolveBaEventDetail(
        context: Context,
        payload: NotificationPayload
    ): String? {
        return payload.state.expandedContent(context)
            .trim()
            .takeIf { it.isNotEmpty() && it != payload.state.title(context) }
    }
}
