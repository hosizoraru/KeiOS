package os.kei.ui.page.main.mcp.skill.page

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import os.kei.core.ui.effect.getMiuixAppBarColor
import os.kei.core.ui.effect.rememberMiuixBlurBackdrop
import os.kei.mcp.server.McpServerManager
import os.kei.ui.page.main.mcp.skill.McpSkillPageContentRequest
import os.kei.ui.page.main.mcp.skill.McpSkillPageViewModel
import os.kei.ui.page.main.mcp.skill.component.McpSkillContentList
import os.kei.ui.page.main.mcp.skill.state.rememberMcpSkillPageTextBundle
import os.kei.ui.page.main.widget.chrome.AppPageScaffold
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun McpSkillPage(
    mcpServerManager: McpServerManager,
    onBack: () -> Unit
) {
    val textBundle = rememberMcpSkillPageTextBundle()
    val listState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    val titleColor = MiuixTheme.colorScheme.onBackground
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant
    val accentColor = MiuixTheme.colorScheme.primary
    val codeColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.10f)
    val topBarMaterialBackdrop = rememberMiuixBlurBackdrop(enableBlur = true)
    val viewModel: McpSkillPageViewModel = viewModel()
    val contentRequest = McpSkillPageContentRequest(
        emptyMarkdown = textBundle.emptyMarkdown,
        defaultRootTitle = textBundle.defaultRootTitle,
        defaultOverviewTitle = textBundle.defaultOverviewTitle,
        defaultContentTitle = textBundle.defaultContentTitle,
        emptyContentText = textBundle.emptyContentText
    )
    LaunchedEffect(mcpServerManager, contentRequest) {
        viewModel.loadContent(
            manager = mcpServerManager,
            request = contentRequest
        )
    }
    val contentState by viewModel.contentState.collectAsState()

    AppPageScaffold(
        title = textBundle.pageTitle,
        modifier = Modifier.fillMaxSize(),
        scrollBehavior = scrollBehavior,
        topBarColor = topBarMaterialBackdrop.getMiuixAppBarColor(),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = MiuixIcons.Regular.Back,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurface
                )
            }
        }
    ) { innerPadding ->
        McpSkillContentList(
            innerPadding = innerPadding,
            listState = listState,
            nestedScrollConnection = scrollBehavior.nestedScrollConnection,
            contentState = contentState,
            clawCardTitle = textBundle.clawCardTitle,
            clawCardSummary = textBundle.clawCardSummary,
            clawPrompt = textBundle.clawPrompt,
            copyClawPromptText = textBundle.copyClawPromptText,
            clawPromptCopiedToast = textBundle.clawPromptCopiedToast,
            emptyItemText = textBundle.emptyItemText,
            titleColor = titleColor,
            subtitleColor = subtitleColor,
            accentColor = accentColor,
            codeColor = codeColor,
            modifier = Modifier.fillMaxSize()
        )
    }
}
