package com.example.keios.ui.page.main.host.pager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.keios.core.log.AppLogger
import com.example.keios.mcp.server.McpServerManager
import com.example.keios.ui.page.main.home.model.HomeBaOverview
import com.example.keios.ui.page.main.home.model.HomeGitHubOverview
import com.example.keios.ui.page.main.home.model.HomeMcpOverview
import com.example.keios.ui.page.main.home.model.loadHomeBaOverview
import com.example.keios.ui.page.main.home.model.loadHomeGitHubOverview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class MainPagerHomeOverviewState(
    val homeMcpOverview: HomeMcpOverview,
    val homeGitHubOverview: HomeGitHubOverview,
    val homeBaOverview: HomeBaOverview
)

private fun buildHomeTokenPreview(token: String): String {
    val trimmed = token.trim()
    if (trimmed.isBlank()) return ""
    if (trimmed.length <= 4) return trimmed
    return "${trimmed.take(2)}…${trimmed.takeLast(2)}"
}

@Composable
internal fun rememberMainPagerHomeOverviewState(
    mcpServerManager: McpServerManager,
    settingsReturnToken: Int
): MainPagerHomeOverviewState {
    val mcpUiState by mcpServerManager.uiState.collectAsState()
    var homeGitHubOverview by remember { mutableStateOf(HomeGitHubOverview()) }
    var homeBaOverview by remember { mutableStateOf(HomeBaOverview()) }
    val homeMcpOverview = remember(mcpUiState) {
        HomeMcpOverview(
            running = mcpUiState.running,
            runningSinceEpochMs = mcpUiState.runningSinceEpochMs,
            port = mcpUiState.port,
            endpointPath = mcpUiState.endpointPath,
            serverName = mcpUiState.serverName,
            authTokenConfigured = mcpUiState.authToken.isNotBlank(),
            authTokenPreview = buildHomeTokenPreview(mcpUiState.authToken),
            connectedClients = mcpUiState.connectedClients,
            allowExternal = mcpUiState.allowExternal
        )
    }

    suspend fun refreshHomeOverviewState(reason: String) {
        val (baOverview, githubOverview) = withContext(Dispatchers.IO) {
            val loadedBaOverview = runCatching { loadHomeBaOverview() }
                .onFailure { error ->
                    AppLogger.w(
                        "MainScreen",
                        "loadHomeBaOverview failed (reason=$reason)",
                        error
                    )
                }
                .getOrElse { HomeBaOverview(loaded = true) }
            val loadedGitHubOverview = runCatching { loadHomeGitHubOverview() }
                .onFailure { error ->
                    AppLogger.w(
                        "MainScreen",
                        "loadHomeGitHubOverview failed (reason=$reason)",
                        error
                    )
                }
                .getOrElse { HomeGitHubOverview(loaded = true) }
            loadedBaOverview to loadedGitHubOverview
        }
        homeBaOverview = baOverview
        homeGitHubOverview = githubOverview
    }

    LaunchedEffect(Unit) {
        if (homeBaOverview.loaded && homeGitHubOverview.loaded) return@LaunchedEffect
        refreshHomeOverviewState(reason = "initial")
    }
    LaunchedEffect(settingsReturnToken) {
        if (settingsReturnToken <= 0) return@LaunchedEffect
        refreshHomeOverviewState(reason = "settings_return_$settingsReturnToken")
    }

    return remember(homeMcpOverview, homeGitHubOverview, homeBaOverview) {
        MainPagerHomeOverviewState(
            homeMcpOverview = homeMcpOverview,
            homeGitHubOverview = homeGitHubOverview,
            homeBaOverview = homeBaOverview
        )
    }
}
