package os.kei.ui.page.main.github.share

import org.junit.Test
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubShareImportFlowMode
import kotlin.test.assertEquals

class GitHubShareImportActivityLaunchPolicyTest {
    @Test
    fun `incoming share display state follows share text and flow mode`() {
        data class Case(
            val name: String,
            val sharedText: String,
            val flowMode: GitHubShareImportFlowMode,
            val expected: GitHubShareImportActivityDisplayState,
        )
        val releasesUrl = "https://github.com/open-ani/animeko/releases"
        listOf(
            Case(
                name = "notification first incoming share starts hidden",
                sharedText = releasesUrl,
                flowMode = GitHubShareImportFlowMode.NotificationFirst,
                expected = GitHubShareImportActivityDisplayState.Hidden,
            ),
            Case(
                name = "sheet assisted incoming share starts sheet",
                sharedText = releasesUrl,
                flowMode = GitHubShareImportFlowMode.SheetAssisted,
                expected = GitHubShareImportActivityDisplayState.Sheet,
            ),
            Case(
                name = "invalid incoming share finishes activity",
                sharedText = "hello",
                flowMode = GitHubShareImportFlowMode.NotificationFirst,
                expected = GitHubShareImportActivityDisplayState.Finish,
            ),
        ).forEach { case ->
            assertEquals(
                case.expected,
                GitHubShareImportActivityLaunchPolicy.forIncomingShare(
                    sharedText = case.sharedText,
                    lookupConfig = GitHubLookupConfig(
                        shareImportFlowMode = case.flowMode
                    )
                ),
                case.name
            )
        }
    }
}
