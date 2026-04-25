package os.kei.ui.page.main.host.pager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import os.kei.feature.home.data.HomeOverviewRepository
import os.kei.feature.home.model.HomeBaOverview
import os.kei.feature.home.model.HomeGitHubOverview
import os.kei.feature.home.model.HomeMcpOverview
import os.kei.feature.home.model.HomeOverviewCard
import os.kei.feature.home.model.HomeOverviewSnapshot
import os.kei.feature.home.model.defaultHomeOverviewCards
import os.kei.mcp.server.McpServerManager

internal data class MainPagerHomeOverviewState(
    val homeMcpOverview: HomeMcpOverview,
    val homeGitHubOverview: HomeGitHubOverview,
    val homeBaOverview: HomeBaOverview,
    val visibleOverviewCards: Set<HomeOverviewCard>,
    val onOverviewCardVisibilityChange: (HomeOverviewCard, Boolean) -> Unit
)

internal class MainPagerHomeOverviewViewModel(
    private val repository: HomeOverviewRepository
) : ViewModel() {
    val uiState: StateFlow<HomeOverviewSnapshot> = repository.observeOverview()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = HomeOverviewSnapshot()
        )

    fun refresh(reason: String) {
        repository.requestRefresh(reason)
    }

    fun setOverviewCardVisible(card: HomeOverviewCard, visible: Boolean) {
        viewModelScope.launch {
            repository.setOverviewCardVisible(card, visible)
        }
    }

    companion object {
        fun factory(mcpServerManager: McpServerManager): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MainPagerHomeOverviewViewModel::class.java)) {
                        return MainPagerHomeOverviewViewModel(
                            repository = HomeOverviewRepository(mcpServerManager.uiState)
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class ${modelClass.name}")
                }

                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras
                ): T {
                    return create(modelClass)
                }
            }
        }
    }
}

@Composable
internal fun rememberMainPagerHomeOverviewState(
    mcpServerManager: McpServerManager,
    settingsReturnToken: Int
): MainPagerHomeOverviewState {
    val homeOverviewViewModel: MainPagerHomeOverviewViewModel = viewModel(
        key = "main_pager_home_overview",
        factory = remember(mcpServerManager) {
            MainPagerHomeOverviewViewModel.factory(mcpServerManager)
        }
    )
    val uiState by homeOverviewViewModel.uiState.collectAsState()
    LaunchedEffect(settingsReturnToken) {
        if (settingsReturnToken <= 0) return@LaunchedEffect
        homeOverviewViewModel.refresh("settings_return_$settingsReturnToken")
    }
    val onOverviewCardVisibilityChange = remember(homeOverviewViewModel) {
        { card: HomeOverviewCard, visible: Boolean ->
            homeOverviewViewModel.setOverviewCardVisible(card, visible)
        }
    }
    return remember(uiState, onOverviewCardVisibilityChange) {
        MainPagerHomeOverviewState(
            homeMcpOverview = uiState.mcpOverview,
            homeGitHubOverview = uiState.githubOverview,
            homeBaOverview = uiState.baOverview,
            visibleOverviewCards = uiState.visibleOverviewCards.ifEmpty { defaultHomeOverviewCards() },
            onOverviewCardVisibilityChange = onOverviewCardVisibilityChange
        )
    }
}
