package com.example.keios.ui.page.main.home.model

import androidx.compose.runtime.Immutable

@Immutable
data class HomeMcpOverview(
    val running: Boolean = false,
    val runningSinceEpochMs: Long = 0L,
    val port: Int = 0,
    val endpointPath: String = "",
    val serverName: String = "",
    val authTokenConfigured: Boolean = false,
    val authTokenPreview: String = "",
    val connectedClients: Int = 0,
    val allowExternal: Boolean = false,
)
