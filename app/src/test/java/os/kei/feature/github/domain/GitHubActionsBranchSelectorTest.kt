package os.kei.feature.github.domain

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import os.kei.feature.github.model.GitHubActionsArtifact
import os.kei.feature.github.model.GitHubActionsRunArtifacts
import os.kei.feature.github.model.GitHubActionsWorkflow
import os.kei.feature.github.model.GitHubActionsWorkflowArtifactsSnapshot
import os.kei.feature.github.model.GitHubActionsWorkflowRun

class GitHubActionsBranchSelectorTest {
    @Test
    fun `starts from default branch before activity evidence loads`() {
        val workflow = workflow()

        val branch = GitHubActionsBranchSelector.recommendBranch(
            defaultBranch = "main",
            workflow = workflow
        )
        val options = GitHubActionsBranchSelector.buildOptions(
            defaultBranch = "main",
            workflow = workflow
        )

        assertEquals("main", branch)
        assertEquals("main", options.first().name)
        assertTrue(options.first().defaultBranch)
    }

    @Test
    fun `installerx nightly workflow recommends dev when default branch has no artifacts`() {
        val workflow = workflow(
            name = "Dev Branch Build & Artifact",
            path = ".github/workflows/auto-preview-dev.yml"
        )
        val snapshot = snapshot(
            workflow = workflow,
            runs = listOf(
                runArtifacts(
                    workflow = workflow,
                    runId = 25115668266,
                    branch = "dev",
                    artifacts = listOf(
                        "app-offline-Unstable-release.apk",
                        "app-online-Unstable-release.apk"
                    )
                )
            )
        )
        val signal = GitHubActionsWorkflowSelector.buildArtifactSignal(
            workflow = workflow,
            runs = snapshot.runs,
            defaultBranch = "main"
        )

        val branch = GitHubActionsBranchSelector.recommendBranch(
            defaultBranch = "main",
            workflow = workflow,
            signal = signal,
            snapshot = snapshot
        )
        val options = GitHubActionsBranchSelector.buildOptions(
            defaultBranch = "main",
            workflow = workflow,
            signal = signal,
            snapshot = snapshot
        )

        assertEquals("dev", branch)
        assertEquals("dev", options.first().name)
        assertTrue(options.first().recommended)
        assertEquals(2, options.first().artifactCount)
    }

    @Test
    fun `default branch keeps priority when it has artifacts`() {
        val workflow = workflow()
        val snapshot = snapshot(
            workflow = workflow,
            runs = listOf(
                runArtifacts(
                    workflow = workflow,
                    runId = 100,
                    branch = "main",
                    artifacts = listOf("app-arm64-v8a-release.apk")
                ),
                runArtifacts(
                    workflow = workflow,
                    runId = 101,
                    branch = "dev",
                    artifacts = listOf("app-arm64-v8a-debug.apk")
                )
            )
        )
        val signal = GitHubActionsWorkflowSelector.buildArtifactSignal(
            workflow = workflow,
            runs = snapshot.runs,
            defaultBranch = "main"
        )

        val branch = GitHubActionsBranchSelector.recommendBranch(
            defaultBranch = "main",
            workflow = workflow,
            signal = signal,
            snapshot = snapshot
        )

        assertEquals("main", branch)
    }

    @Test
    fun `active branch can replace inactive default branch without artifacts`() {
        val workflow = workflow()
        val snapshot = snapshot(
            workflow = workflow,
            runs = listOf(
                runArtifacts(workflow = workflow, runId = 200, branch = "develop", artifacts = emptyList()),
                runArtifacts(workflow = workflow, runId = 201, branch = "develop", artifacts = emptyList())
            )
        )
        val signal = GitHubActionsWorkflowSelector.buildArtifactSignal(
            workflow = workflow,
            runs = snapshot.runs,
            defaultBranch = "main"
        )

        val branch = GitHubActionsBranchSelector.recommendBranch(
            defaultBranch = "main",
            workflow = workflow,
            signal = signal,
            snapshot = snapshot
        )

        assertEquals("develop", branch)
    }

    private fun workflow(
        id: Long = 172991813,
        name: String = "Android CI",
        path: String = ".github/workflows/android.yml"
    ): GitHubActionsWorkflow {
        return GitHubActionsWorkflow(
            id = id,
            name = name,
            path = path,
            state = "active"
        )
    }

    private fun snapshot(
        workflow: GitHubActionsWorkflow,
        runs: List<GitHubActionsRunArtifacts>
    ): GitHubActionsWorkflowArtifactsSnapshot {
        return GitHubActionsWorkflowArtifactsSnapshot(
            owner = "demo",
            repo = "app",
            workflowId = workflow.id.toString(),
            runs = runs
        )
    }

    private fun runArtifacts(
        workflow: GitHubActionsWorkflow,
        runId: Long,
        branch: String,
        artifacts: List<String>
    ): GitHubActionsRunArtifacts {
        return GitHubActionsRunArtifacts(
            run = GitHubActionsWorkflowRun(
                id = runId,
                workflowId = workflow.id,
                workflowName = workflow.name,
                event = "push",
                status = "completed",
                conclusion = "success",
                headBranch = branch,
                repositoryFullName = "demo/app",
                headRepositoryFullName = "demo/app"
            ),
            artifacts = artifacts.mapIndexed { index, name ->
                GitHubActionsArtifact(
                    id = runId * 100 + index,
                    name = name,
                    workflowRunId = runId,
                    expired = false
                )
            }
        )
    }
}
