package com.example.keios.core.shortcut

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.keios.MainActivity
import com.example.keios.R

internal object AppShortcuts {
    private const val SHORTCUT_ID_BA_AP_ISLAND = "keios.ba.ap_island"
    private const val SHORTCUT_ID_MCP_TOGGLE = "keios.mcp.toggle"
    private const val SHORTCUT_ID_GITHUB_REFRESH = "keios.github.refresh_tracked"

    fun sync(context: Context) {
        val shortcuts = listOf(
            buildBaApIslandShortcut(context),
            buildMcpToggleShortcut(context),
            buildGitHubRefreshShortcut(context)
        )
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }

    private fun buildBaApIslandShortcut(context: Context): ShortcutInfoCompat {
        return ShortcutInfoCompat.Builder(context, SHORTCUT_ID_BA_AP_ISLAND)
            .setShortLabel(context.getString(R.string.shortcut_label_ap_island_short))
            .setLongLabel(context.getString(R.string.shortcut_label_ap_island_long))
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_ba_ap_island_shift))
            .setIntent(
                Intent(context, ShortcutDispatchActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra(MainActivity.EXTRA_SHORTCUT_ACTION, MainActivity.SHORTCUT_ACTION_BA_AP_ISLAND)
                }
            )
            .setRank(0)
            .build()
    }

    private fun buildMcpToggleShortcut(context: Context): ShortcutInfoCompat {
        return ShortcutInfoCompat.Builder(context, SHORTCUT_ID_MCP_TOGGLE)
            .setShortLabel(context.getString(R.string.shortcut_label_mcp_toggle_short))
            .setLongLabel(context.getString(R.string.shortcut_label_mcp_toggle_long))
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_mcp_lobehub))
            .setIntent(
                Intent(context, ShortcutDispatchActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra(MainActivity.EXTRA_SHORTCUT_ACTION, MainActivity.SHORTCUT_ACTION_MCP_TOGGLE)
                }
            )
            .setRank(1)
            .build()
    }

    private fun buildGitHubRefreshShortcut(context: Context): ShortcutInfoCompat {
        return ShortcutInfoCompat.Builder(context, SHORTCUT_ID_GITHUB_REFRESH)
            .setShortLabel(context.getString(R.string.shortcut_label_github_refresh_short))
            .setLongLabel(context.getString(R.string.shortcut_label_github_refresh_long))
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_github_invertocat))
            .setIntent(
                Intent(context, ShortcutDispatchActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra(
                        MainActivity.EXTRA_SHORTCUT_ACTION,
                        MainActivity.SHORTCUT_ACTION_GITHUB_REFRESH_TRACKED
                    )
                }
            )
            .setRank(2)
            .build()
    }
}
