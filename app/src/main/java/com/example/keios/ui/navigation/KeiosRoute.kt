package com.example.keios.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation keys for KeiOS.
 */
sealed interface KeiosRoute : NavKey {
    @Serializable
    data object Main : KeiosRoute

    @Serializable
    data object Settings : KeiosRoute

    @Serializable
    data object McpSkill : KeiosRoute
}
