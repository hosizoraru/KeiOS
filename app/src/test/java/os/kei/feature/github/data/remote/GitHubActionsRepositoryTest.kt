package os.kei.feature.github.data.remote

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import os.kei.feature.github.model.GitHubApiAuthMode
import os.kei.feature.github.model.GitHubActionsArtifact
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GitHubActionsRepositoryTest {
    @Test
    fun `guest metadata request lists workflows without authorization header`() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(sampleWorkflowsJson())
            )
            val repository = GitHubActionsRepository(
                apiToken = "",
                apiBaseUrl = server.url("/").toString()
            )

            val trace = repository.fetchWorkflows(owner = "demo", repo = "app")

            assertTrue(trace.result.isSuccess)
            assertEquals(GitHubApiAuthMode.Guest, trace.authMode)
            assertEquals("Android CI", trace.result.getOrThrow().first().name)
            val request = server.takeRequest()
            assertEquals("/repos/demo/app/actions/workflows?per_page=50", request.path)
            assertNull(request.getHeader("Authorization"))
        }
    }

    @Test
    fun `token metadata request sends bearer authorization`() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(sampleWorkflowRunsJson())
            )
            val repository = GitHubActionsRepository(
                apiToken = "test-token-123",
                apiBaseUrl = server.url("/").toString()
            )

            val trace = repository.fetchWorkflowRuns(
                owner = "demo",
                repo = "app",
                workflowId = "android.yml",
                branch = "main",
                status = "completed",
                headSha = "def456",
                excludePullRequests = true
            )

            assertTrue(trace.result.isSuccess)
            assertEquals(GitHubApiAuthMode.Token, trace.authMode)
            val request = server.takeRequest()
            assertEquals(
                "/repos/demo/app/actions/workflows/android.yml/runs?per_page=20&branch=main&status=completed&head_sha=def456&exclude_pull_requests=true",
                request.path
            )
            assertEquals("Bearer test-token-123", request.getHeader("Authorization"))
        }
    }

    @Test
    fun `repository info parser keeps default branch`() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(sampleRepositoryJson())
            )
            val repository = GitHubActionsRepository(
                apiToken = "",
                apiBaseUrl = server.url("/").toString()
            )

            val info = repository.fetchRepositoryInfo(owner = "demo", repo = "app").result.getOrThrow()

            assertEquals("demo/app", info.fullName)
            assertEquals("main", info.defaultBranch)
            assertEquals("/repos/demo/app", server.takeRequest().path)
        }
    }

    @Test
    fun `workflow artifact snapshot fetches runs then run artifacts`() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(sampleWorkflowRunsJson()))
            server.enqueue(MockResponse().setResponseCode(200).setBody(sampleArtifactsJson(runId = 101)))
            server.enqueue(MockResponse().setResponseCode(200).setBody(sampleArtifactsJson(runId = 100)))
            val repository = GitHubActionsRepository(
                apiToken = "",
                apiBaseUrl = server.url("/").toString()
            )

            val snapshot = repository.fetchWorkflowArtifactSnapshot(
                owner = "demo",
                repo = "app",
                workflowId = "42",
                runLimit = 2
            ).result.getOrThrow()

            assertEquals("demo", snapshot.owner)
            assertEquals("42", snapshot.workflowId)
            assertEquals(2, snapshot.runs.size)
            assertEquals(listOf(101L, 100L), snapshot.runs.map { it.run.id })
            assertEquals(4, snapshot.artifacts.size)
            assertEquals(
                listOf(
                    "/repos/demo/app/actions/workflows/42/runs?per_page=2",
                    "/repos/demo/app/actions/runs/101/artifacts?per_page=100",
                    "/repos/demo/app/actions/runs/100/artifacts?per_page=100"
                ),
                List(3) { server.takeRequest().path }
            )
        }
    }

    @Test
    fun `parsers keep core workflow run and artifact fields`() {
        val repository = GitHubActionsRepository(apiToken = "")

        val workflows = repository.parseWorkflows(sampleWorkflowsJson())
        val runs = repository.parseWorkflowRuns(sampleWorkflowRunsJson())
        val artifacts = repository.parseArtifacts(sampleArtifactsJson(runId = 101), fallbackWorkflowRunId = 101)

        assertEquals(2, workflows.size)
        assertEquals(1L, workflows.first().id)
        assertEquals("active", workflows.first().state)
        assertEquals(101L, runs.first().id)
        assertEquals("Build arm64 artifact", runs.first().displayTitle)
        assertEquals("completed", runs.first().status)
        assertEquals("success", runs.first().conclusion)
        assertEquals("demo/app", runs.first().repositoryFullName)
        assertEquals("demo/app", runs.first().headRepositoryFullName)
        assertEquals(123456L, runs.first().checkSuiteId)
        assertEquals(0, runs.first().pullRequestCount)
        assertEquals(2, artifacts.size)
        assertEquals("app-arm64-v8a-release.apk", artifacts.first().name)
        assertEquals(101L, artifacts.first().workflowRunId)
        assertEquals("main", artifacts.first().workflowRunHeadBranch)
        assertEquals("sha101", artifacts.first().workflowRunHeadSha)
        assertEquals("sha256:demo", artifacts.first().digest)
        assertFalse(artifacts.first().expired)
    }

    @Test
    fun `download resolver requires token`() {
        val repository = GitHubActionsRepository(apiToken = "")
        val result = repository.resolveArtifactDownloadUrl(
            artifact = GitHubActionsArtifact(
                id = 10L,
                name = "app-release.apk",
                archiveDownloadUrl = "https://api.github.test/artifact.zip"
            )
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("需要填写 token"))
    }

    @Test
    fun `download resolver returns redirect location with token`() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(302)
                    .addHeader("Location", "https://pipelines.actions.githubusercontent.com/demo/artifact.zip")
            )
            val repository = GitHubActionsRepository(
                apiToken = "test-token-123",
                apiBaseUrl = server.url("/").toString()
            )

            val result = repository.resolveArtifactDownloadUrl(
                artifact = GitHubActionsArtifact(
                    id = 10L,
                    name = "app-release.apk",
                    archiveDownloadUrl = server.url("/repos/demo/app/actions/artifacts/10/zip").toString()
                )
            ).getOrThrow()

            assertEquals(10L, result.artifactId)
            assertEquals("https://pipelines.actions.githubusercontent.com/demo/artifact.zip", result.downloadUrl)
            assertEquals("Bearer test-token-123", server.takeRequest().getHeader("Authorization"))
        }
    }

    @Test
    fun `guest rate limit error asks for token`() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(403)
                    .addHeader("X-RateLimit-Remaining", "0")
                    .setBody("""{"message":"API rate limit exceeded"}""")
            )
            val repository = GitHubActionsRepository(
                apiToken = "",
                apiBaseUrl = server.url("/").toString()
            )

            val message = repository
                .fetchWorkflows(owner = "demo", repo = "app")
                .result
                .exceptionOrNull()
                ?.message
                .orEmpty()

            assertTrue(message.contains("游客 API 已限流"))
            assertTrue(message.contains("填写 token"))
        }
    }

    private fun sampleWorkflowsJson(): String {
        return """
            {
              "total_count": 2,
              "workflows": [
                {
                  "id": 2,
                  "node_id": "W_2",
                  "name": "Nightly",
                  "path": ".github/workflows/nightly.yml",
                  "state": "disabled_manually",
                  "html_url": "https://github.com/demo/app/actions/workflows/nightly.yml",
                  "badge_url": "https://github.com/demo/app/actions/workflows/nightly.yml/badge.svg",
                  "created_at": "2026-04-20T00:00:00Z",
                  "updated_at": "2026-04-22T00:00:00Z"
                },
                {
                  "id": 1,
                  "node_id": "W_1",
                  "name": "Android CI",
                  "path": ".github/workflows/android.yml",
                  "state": "active",
                  "html_url": "https://github.com/demo/app/actions/workflows/android.yml",
                  "badge_url": "https://github.com/demo/app/actions/workflows/android.yml/badge.svg",
                  "created_at": "2026-04-19T00:00:00Z",
                  "updated_at": "2026-04-23T00:00:00Z"
                }
              ]
            }
        """.trimIndent()
    }

    private fun sampleRepositoryJson(): String {
        return """
            {
              "name": "app",
              "full_name": "demo/app",
              "default_branch": "main",
              "owner": {"login": "demo"}
            }
        """.trimIndent()
    }

    private fun sampleWorkflowRunsJson(): String {
        return """
            {
              "total_count": 2,
              "workflow_runs": [
                {
                  "id": 100,
                  "name": "Android CI",
                  "display_title": "Older build",
                  "workflow_id": 1,
                  "workflow_name": "Android CI",
                  "run_number": 9,
                  "run_attempt": 1,
                  "event": "push",
                  "status": "completed",
                  "conclusion": "success",
                  "head_branch": "main",
                  "head_sha": "abc123",
                  "html_url": "https://github.com/demo/app/actions/runs/100",
                  "artifacts_url": "https://api.github.com/repos/demo/app/actions/runs/100/artifacts",
                  "check_suite_id": 123455,
                  "actor": {"login":"octocat"},
                  "triggering_actor": {"login":"octocat"},
                  "repository": {"full_name":"demo/app"},
                  "head_repository": {"full_name":"demo/app", "fork": false},
                  "pull_requests": [],
                  "created_at": "2026-04-20T00:00:00Z",
                  "run_started_at": "2026-04-20T00:00:30Z",
                  "updated_at": "2026-04-20T00:03:00Z"
                },
                {
                  "id": 101,
                  "name": "Android CI",
                  "display_title": "Build arm64 artifact",
                  "workflow_id": 1,
                  "workflow_name": "Android CI",
                  "run_number": 10,
                  "run_attempt": 1,
                  "event": "push",
                  "status": "completed",
                  "conclusion": "success",
                  "head_branch": "main",
                  "head_sha": "def456",
                  "html_url": "https://github.com/demo/app/actions/runs/101",
                  "artifacts_url": "https://api.github.com/repos/demo/app/actions/runs/101/artifacts",
                  "check_suite_id": 123456,
                  "actor": {"login":"octocat"},
                  "triggering_actor": {"login":"octocat"},
                  "repository": {"full_name":"demo/app"},
                  "head_repository": {"full_name":"demo/app", "fork": false},
                  "pull_requests": [],
                  "created_at": "2026-04-21T00:00:00Z",
                  "run_started_at": "2026-04-21T00:00:30Z",
                  "updated_at": "2026-04-21T00:03:00Z"
                }
              ]
            }
        """.trimIndent()
    }

    private fun sampleArtifactsJson(runId: Long): String {
        return """
            {
              "total_count": 2,
              "artifacts": [
                {
                  "id": ${runId}1,
                  "node_id": "A_${runId}1",
                  "name": "mapping.txt",
                  "size_in_bytes": 512,
                  "expired": true,
                  "digest": "sha256:mapping",
                  "archive_download_url": "https://api.github.com/repos/demo/app/actions/artifacts/${runId}1/zip",
                  "created_at": "2026-04-21T00:02:00Z",
                  "updated_at": "2026-04-21T00:02:00Z",
                  "expires_at": "2026-05-21T00:02:00Z",
                  "workflow_run": {"id": $runId, "head_branch": "main", "head_sha": "sha$runId"}
                },
                {
                  "id": ${runId}0,
                  "node_id": "A_${runId}0",
                  "name": "app-arm64-v8a-release.apk",
                  "size_in_bytes": 10485760,
                  "expired": false,
                  "digest": "sha256:demo",
                  "archive_download_url": "https://api.github.com/repos/demo/app/actions/artifacts/${runId}0/zip",
                  "created_at": "2026-04-21T00:01:00Z",
                  "updated_at": "2026-04-21T00:01:00Z",
                  "expires_at": "2026-05-21T00:01:00Z",
                  "workflow_run": {"id": $runId, "head_branch": "main", "head_sha": "sha$runId"}
                }
              ]
            }
        """.trimIndent()
    }
}
