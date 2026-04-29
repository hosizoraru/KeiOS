package os.kei.feature.github.domain

import org.junit.Test
import os.kei.feature.github.model.GitHubActionsArtifact
import os.kei.feature.github.model.GitHubActionsArtifactSelectionOptions
import os.kei.feature.github.model.GitHubActionsRunArtifacts
import os.kei.feature.github.model.GitHubActionsRunBranchTrust
import os.kei.feature.github.model.GitHubActionsRunSelectionOptions
import os.kei.feature.github.model.GitHubActionsWorkflow
import os.kei.feature.github.model.GitHubActionsWorkflowRun
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitHubActionsRunSelectorTest {
    @Test
    fun `piliplus sample prefers default branch android run over pull request run`() {
        val runs = listOf(
            runArtifacts(
                run = run(
                    id = 4899,
                    event = "workflow_dispatch",
                    branch = "main",
                    title = "Build PiliPlus 2.0.5",
                    pullRequestCount = 1
                ),
                artifactNames = listOf(
                    "PiliPlus_android_2.0.5-88c01ffca+4899_x86_64.apk",
                    "PiliPlus_android_2.0.5-88c01ffca+4899_armeabi-v7a.apk",
                    "PiliPlus_android_2.0.5-88c01ffca+4899_arm64-v8a.apk",
                    "PiliPlus_windows_2.0.5-88c01ffca+4899_x64.zip"
                )
            ),
            runArtifacts(
                run = run(
                    id = 4900,
                    event = "pull_request",
                    branch = "dev",
                    title = "PR build",
                    pullRequestCount = 1
                ),
                artifactNames = listOf("PiliPlus_android__arm64-v8a.apk")
            )
        )

        val matches = GitHubActionsRunSelector.selectRuns(
            runs = runs,
            options = GitHubActionsRunSelectionOptions(
                defaultBranch = "main",
                artifactOptions = GitHubActionsArtifactSelectionOptions(
                    preferredAbis = listOf("arm64-v8a")
                )
            )
        )

        assertEquals(4899, matches.single().runArtifacts.run.id)
        assertEquals(GitHubActionsRunBranchTrust.DefaultBranch, matches.single().traits.branchTrust)
        assertEquals(
            "PiliPlus_android_2.0.5-88c01ffca+4899_arm64-v8a.apk",
            matches.single().artifactMatches.first().artifact.name
        )
        assertTrue(matches.single().traits.safeForRecommendation)
    }

    @Test
    fun `animeko build workflow prefers main push run inside mixed workflow`() {
        val workflow = workflow("Build", ".github/workflows/build.yml")
        val workflowTraits = GitHubActionsWorkflowSelector.inspectWorkflow(workflow)
        val runs = listOf(
            runArtifacts(
                run = run(
                    id = 1001,
                    event = "pull_request",
                    branch = "feature/refactor",
                    title = "PR debug build",
                    pullRequestCount = 1
                ),
                artifactNames = listOf(
                    "ani-android-arm64-v8a-debug",
                    "ani-desktop-linux-x64"
                )
            ),
            runArtifacts(
                run = run(
                    id = 1002,
                    event = "push",
                    branch = "main",
                    title = "Build main"
                ),
                artifactNames = listOf(
                    "ani-android-universal-release",
                    "ani-android-arm64-v8a-release",
                    "ani-android-x86_64-debug"
                )
            )
        )

        val matches = GitHubActionsRunSelector.selectRuns(
            runs = runs,
            workflowTraits = workflowTraits,
            options = GitHubActionsRunSelectionOptions(
                defaultBranch = "main",
                artifactOptions = GitHubActionsArtifactSelectionOptions(
                    preferredAbis = listOf("arm64-v8a")
                )
            )
        )

        assertEquals(1002, matches.single().runArtifacts.run.id)
        assertTrue(matches.single().reasons.contains("default-branch"))
        assertEquals("ani-android-arm64-v8a-release", matches.single().artifactMatches.first().artifact.name)
    }

    @Test
    fun `animeko release workflow accepts version tag branch as trusted release run`() {
        val workflow = workflow("Release", ".github/workflows/release.yml")
        val workflowTraits = GitHubActionsWorkflowSelector.inspectWorkflow(workflow)
        val runs = listOf(
            runArtifacts(
                run = run(
                    id = 5403,
                    event = "workflow_dispatch",
                    branch = "v5.4.3",
                    title = "Release v5.4.3"
                ),
                artifactNames = listOf(
                    "ani-android-universal-release",
                    "ani-android-arm64-v8a-release",
                    "ani-android-arm64-v8a-debug"
                )
            ),
            runArtifacts(
                run = run(
                    id = 5502,
                    event = "pull_request",
                    branch = "feature/fix-player",
                    title = "PR build",
                    pullRequestCount = 1
                ),
                artifactNames = listOf("ani-android-arm64-v8a-debug")
            )
        )

        val matches = GitHubActionsRunSelector.selectRuns(
            runs = runs,
            workflowTraits = workflowTraits,
            options = GitHubActionsRunSelectionOptions(defaultBranch = "main")
        )

        assertEquals(5403, matches.single().runArtifacts.run.id)
        assertEquals(GitHubActionsRunBranchTrust.ReleaseTag, matches.single().traits.branchTrust)
        assertTrue(matches.single().traits.safeForRecommendation)
        assertTrue(matches.single().reasons.contains("release-tag"))
    }

    private fun workflow(
        name: String,
        path: String
    ): GitHubActionsWorkflow {
        return GitHubActionsWorkflow(
            id = name.hashCode().toLong(),
            name = name,
            path = path,
            state = "active"
        )
    }

    private fun run(
        id: Long,
        event: String,
        branch: String,
        title: String,
        pullRequestCount: Int = 0
    ): GitHubActionsWorkflowRun {
        return GitHubActionsWorkflowRun(
            id = id,
            workflowId = 1,
            workflowName = "Build",
            displayTitle = title,
            event = event,
            status = "completed",
            conclusion = "success",
            headBranch = branch,
            repositoryFullName = "demo/app",
            headRepositoryFullName = "demo/app",
            pullRequestCount = pullRequestCount,
            createdAtMillis = id
        )
    }

    private fun runArtifacts(
        run: GitHubActionsWorkflowRun,
        artifactNames: List<String>
    ): GitHubActionsRunArtifacts {
        return GitHubActionsRunArtifacts(
            run = run,
            artifacts = artifactNames.mapIndexed { index, name ->
                GitHubActionsArtifact(
                    id = run.id * 100 + index,
                    name = name,
                    sizeBytes = 20_000_000L,
                    workflowRunId = run.id
                )
            }
        )
    }
}
