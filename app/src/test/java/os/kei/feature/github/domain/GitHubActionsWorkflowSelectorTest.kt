package os.kei.feature.github.domain

import org.junit.Test
import os.kei.feature.github.model.GitHubActionsArtifact
import os.kei.feature.github.model.GitHubActionsRunArtifacts
import os.kei.feature.github.model.GitHubActionsWorkflow
import os.kei.feature.github.model.GitHubActionsWorkflowKind
import os.kei.feature.github.model.GitHubActionsWorkflowRun
import os.kei.feature.github.model.GitHubActionsWorkflowSelectionOptions
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitHubActionsWorkflowSelectorTest {
    @Test
    fun `libchecker sample recommends android ci over maintenance workflows`() {
        val workflows = listOf(
            workflow(4599452, "Android CI", ".github/workflows/android.yml"),
            workflow(34409074, "Crowdin Action", ".github/workflows/translation.yml"),
            workflow(182581809, "Copilot coding agent", "dynamic/copilot-swe-agent/copilot"),
            workflow(79583659, "CodeQL", ".github/workflows/codeql.yml", state = "disabled_manually")
        )
        val signals = mapOf(
            4599452L to signal(
                workflows[0],
                artifacts = listOf("mappings", "Market", "Foss")
            )
        )

        val matches = GitHubActionsWorkflowSelector.selectWorkflows(
            workflows = workflows,
            artifactSignals = signals,
            options = GitHubActionsWorkflowSelectionOptions(
                preferredWorkflowPaths = setOf("android.yml")
            )
        )

        assertEquals("Android CI", matches.first().workflow.name)
        assertEquals(GitHubActionsWorkflowKind.AndroidBuild, matches.first().traits.kind)
        assertTrue(matches.first().reasons.contains("android-artifacts"))
        assertTrue(matches.none { it.workflow.name == "CodeQL" })
    }

    @Test
    fun `updater sample recommends build example app and filters dynamic maintenance`() {
        val workflows = listOf(
            workflow(97592034, "Build Example App", ".github/workflows/Action CI.yml"),
            workflow(108077146, "Dependabot Updates", "dynamic/dependabot/dependabot-updates"),
            workflow(165541003, "Automatic Dependency Submission", "dynamic/dependency-graph/auto-submission"),
            workflow(184068432, "Copilot", "dynamic/copilot-swe-agent/copilot")
        )
        val signals = mapOf(
            97592034L to signal(
                workflows[0],
                artifacts = listOf(
                    "Updater-darwin-arm64-dmg.dmg",
                    "Updater-android-universal-apk.apk",
                    "Updater-linux-x64-bin",
                    "Updater-windows-x64-exe",
                    "Updater-linux-arm64-bin"
                )
            ),
            165541003L to signal(
                workflows[2],
                artifacts = listOf("dependency-graph_dynamicdependency-graphauto-submission-submit-gradle.json")
            )
        )

        val matches = GitHubActionsWorkflowSelector.selectWorkflows(
            workflows = workflows,
            artifactSignals = signals,
            options = GitHubActionsWorkflowSelectionOptions(
                preferredWorkflowPaths = setOf("Action CI.yml")
            )
        )

        assertEquals("Build Example App", matches.first().workflow.name)
        assertTrue(matches.first().signal?.androidArtifactCount == 1)
        assertTrue(matches.first().score > matches.last().score)
    }

    @Test
    fun `installerx sample prefers dev artifact workflow over release workflow without artifacts`() {
        val workflows = listOf(
            workflow(172991813, "Dev Branch Build & Artifact", ".github/workflows/auto-preview-dev.yml"),
            workflow(168518891, "Automatic Alpha Pre-Release (Latest Only)", ".github/workflows/auto-preview-release.yml"),
            workflow(209362137, "CodeQL", ".github/workflows/codeql.yml"),
            workflow(218998575, "Deploy Documentation", ".github/workflows/deploy-docs.yml"),
            workflow(168538301, "Manual Stable Release", ".github/workflows/manual-stable-release.yml"),
            workflow(209350691, "PR Build and Test Check", ".github/workflows/pr-check.yml")
        )
        val signals = mapOf(
            172991813L to signal(
                workflows[0],
                artifacts = listOf(
                    "app-offline-Unstable-release.apk",
                    "app-online-Unstable-release.apk"
                )
            ),
            209350691L to signal(
                workflows[5],
                artifacts = listOf(
                    "app-offline-Preview-debug.apk",
                    "app-online-Preview-debug.apk"
                )
            )
        )

        val matches = GitHubActionsWorkflowSelector.selectWorkflows(
            workflows = workflows,
            artifactSignals = signals,
            options = GitHubActionsWorkflowSelectionOptions(
                preferredWorkflowPaths = setOf("auto-preview-dev.yml")
            )
        )

        assertEquals("Dev Branch Build & Artifact", matches.first().workflow.name)
        assertTrue(matches.first().reasons.contains("preferred"))
        assertTrue(matches.first().signal?.androidArtifactCount == 2)
        assertTrue(
            matches.first().score >
                matches.first { it.workflow.name == "Manual Stable Release" }.score
        )
    }

    @Test
    fun `require artifacts removes release workflows that publish elsewhere`() {
        val workflows = listOf(
            workflow(172991813, "Dev Branch Build & Artifact", ".github/workflows/auto-preview-dev.yml"),
            workflow(168518891, "Automatic Alpha Pre-Release (Latest Only)", ".github/workflows/auto-preview-release.yml")
        )
        val signals = mapOf(
            172991813L to signal(
                workflows[0],
                artifacts = listOf("app-online-Unstable-release.apk")
            ),
            168518891L to signal(workflows[1], artifacts = emptyList())
        )

        val matches = GitHubActionsWorkflowSelector.selectWorkflows(
            workflows = workflows,
            artifactSignals = signals,
            options = GitHubActionsWorkflowSelectionOptions(requireArtifacts = true)
        )

        assertEquals(listOf("Dev Branch Build & Artifact"), matches.map { it.workflow.name })
    }

    @Test
    fun `feature branch artifact run stays selectable without becoming safe recommendation`() {
        val workflow = workflow(172991813, "Dev Branch Build & Artifact", ".github/workflows/auto-preview-dev.yml")
        val signal = GitHubActionsWorkflowSelector.buildArtifactSignal(
            workflow = workflow,
            defaultBranch = "main",
            runs = listOf(
                runArtifacts(
                    workflow,
                    runId = 1365,
                    branch = "feat/Home",
                    event = "push",
                    artifactNames = listOf(
                        "app-offline-Unstable-release.apk",
                        "app-online-Unstable-release.apk"
                    )
                )
            )
        )

        assertEquals(2, signal.androidArtifactCount)
        assertEquals(0, signal.trustedRunCount)
        assertEquals(null, signal.recommendedRunId)
    }

    @Test
    fun `animeko sample hides disabled workflows and weights trusted runs`() {
        val workflows = listOf(
            workflow(1, "Build", ".github/workflows/build-main.yml", state = "disabled_manually"),
            workflow(2, "Build", ".github/workflows/build-pr.yml", state = "disabled_manually"),
            workflow(3, "Build", ".github/workflows/build.yml"),
            workflow(4, "Release", ".github/workflows/release.yml"),
            workflow(5, "Copilot", "dynamic/copilot-swe-agent/copilot")
        )
        val signals = mapOf(
            3L to GitHubActionsWorkflowSelector.buildArtifactSignal(
                workflow = workflows[2],
                defaultBranch = "main",
                runs = listOf(
                    runArtifacts(
                        workflows[2],
                        runId = 31,
                        branch = "feature/player",
                        event = "pull_request",
                        pullRequestCount = 1,
                        artifactNames = listOf("ani-android-arm64-v8a-debug")
                    ),
                    runArtifacts(
                        workflows[2],
                        runId = 32,
                        branch = "main",
                        event = "push",
                        artifactNames = listOf("ani-android-arm64-v8a-release")
                    )
                )
            ),
            4L to GitHubActionsWorkflowSelector.buildArtifactSignal(
                workflow = workflows[3],
                defaultBranch = "main",
                runs = listOf(
                    runArtifacts(
                        workflows[3],
                        runId = 41,
                        branch = "v5.4.3",
                        event = "workflow_dispatch",
                        artifactNames = listOf("ani-android-universal-release")
                    )
                )
            )
        )

        val matches = GitHubActionsWorkflowSelector.selectWorkflows(
            workflows = workflows,
            artifactSignals = signals
        )

        assertTrue(matches.none { it.workflow.id == 1L || it.workflow.id == 2L })
        val build = matches.first { it.workflow.id == 3L }
        val release = matches.first { it.workflow.id == 4L }
        assertTrue(build.reasons.contains("recommended-run"))
        assertEquals(1, build.signal?.defaultBranchRunCount)
        assertEquals(1, build.signal?.pullRequestRunCount)
        assertTrue(release.reasons.contains("recommended-run"))
        assertEquals(1, release.signal?.releaseTagRunCount)
    }

    private fun workflow(
        id: Long,
        name: String,
        path: String,
        state: String = "active"
    ): GitHubActionsWorkflow {
        return GitHubActionsWorkflow(
            id = id,
            name = name,
            path = path,
            state = state
        )
    }

    private fun signal(
        workflow: GitHubActionsWorkflow,
        artifacts: List<String>
    ) = GitHubActionsWorkflowSelector.buildArtifactSignal(
        workflow = workflow,
        runs = listOf(
            GitHubActionsRunArtifacts(
                run = GitHubActionsWorkflowRun(
                    id = workflow.id * 10,
                    workflowId = workflow.id,
                    status = "completed",
                    conclusion = "success"
                ),
                artifacts = artifacts.mapIndexed { index, name ->
                    GitHubActionsArtifact(
                        id = workflow.id * 100 + index,
                        name = name,
                        expired = false
                    )
                }
            )
        )
    )

    private fun runArtifacts(
        workflow: GitHubActionsWorkflow,
        runId: Long,
        branch: String,
        event: String,
        artifactNames: List<String>,
        pullRequestCount: Int = 0
    ): GitHubActionsRunArtifacts {
        return GitHubActionsRunArtifacts(
            run = GitHubActionsWorkflowRun(
                id = runId,
                workflowId = workflow.id,
                workflowName = workflow.name,
                event = event,
                status = "completed",
                conclusion = "success",
                headBranch = branch,
                repositoryFullName = "demo/app",
                headRepositoryFullName = "demo/app",
                pullRequestCount = pullRequestCount,
                createdAtMillis = runId
            ),
            artifacts = artifactNames.mapIndexed { index, name ->
                GitHubActionsArtifact(
                    id = runId * 100 + index,
                    name = name,
                    sizeBytes = 20_000_000L,
                    workflowRunId = runId
                )
            }
        )
    }
}
