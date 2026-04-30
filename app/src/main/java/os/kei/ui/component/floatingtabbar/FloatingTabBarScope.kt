/*
 * Adapted from compose-floating-tab-bar.
 * Copyright 2025 Elyes Mansour.
 * SPDX-License-Identifier: Apache-2.0
 */

package os.kei.ui.component.floatingtabbar

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.runtime.Composable

interface FloatingTabBarScope {
    /**
     * Adds a regular tab to the floating tab bar.
     *
     * @param key Unique identifier for the tab
     * @param title Composable content for the tab title
     * @param icon Composable content for the tab icon
     * @param onClick Callback invoked when the tab is clicked
     * @param indication Optional indication provider for touch feedback, defaults to LocalIndication.current
     */
    fun tab(
        key: Any,
        title: @Composable () -> Unit,
        icon: @Composable () -> Unit,
        onClick: () -> Unit,
        indication: (@Composable () -> Indication)? = { LocalIndication.current }
    )

    /**
     * Adds a standalone tab to the floating tab bar.
     *
     * Calling this method repeatedly replaces the previous standalone tab value.
     *
     * @param key Unique identifier for the standalone tab
     * @param icon Composable content for the tab icon
     * @param onClick Callback invoked when the tab is clicked
     * @param indication Optional indication provider for touch feedback, defaults to LocalIndication.current
     */
    fun standaloneTab(
        key: Any,
        icon: @Composable () -> Unit,
        onClick: () -> Unit,
        indication: (@Composable () -> Indication)? = { LocalIndication.current }
    )
}

internal class FloatingTabBarScopeImpl : FloatingTabBarScope {
    val tabs = mutableListOf<FloatingTabBarTab>()
    var standaloneTab: FloatingTabBarTab? = null
        private set
    private var inlineTabKey: Any? = null

    fun update(content: FloatingTabBarScope.() -> Unit) {
        tabs.clear()
        standaloneTab = null
        content()
    }

    fun getInlineTab(selectedTabKey: Any?): FloatingTabBarTab? {
        val selectedTab = tabs.find { it.key == selectedTabKey }
        return if (selectedTab != null) {
            inlineTabKey = selectedTab.key
            selectedTab
        } else {
            tabs.find { it.key == inlineTabKey }
                ?: tabs.firstOrNull()?.also { inlineTabKey = it.key }
        }
    }

    override fun tab(
        key: Any,
        title: @Composable () -> Unit,
        icon: @Composable () -> Unit,
        onClick: () -> Unit,
        indication: (@Composable () -> Indication)?
    ) {
        tabs.add(
            FloatingTabBarTab(
                key = key,
                title = title,
                icon = icon,
                onClick = onClick,
                indication = indication
            )
        )
    }

    override fun standaloneTab(
        key: Any,
        icon: @Composable () -> Unit,
        onClick: () -> Unit,
        indication: (@Composable () -> Indication)?
    ) {
        standaloneTab = FloatingTabBarTab(
            key = key,
            title = {},
            icon = icon,
            onClick = onClick,
            indication = indication
        )
    }
}

internal data class FloatingTabBarTab(
    val key: Any,
    val title: @Composable () -> Unit,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit,
    val indication: (@Composable () -> Indication)?
)
