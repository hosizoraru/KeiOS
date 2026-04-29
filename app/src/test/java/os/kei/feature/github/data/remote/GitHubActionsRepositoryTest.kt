package os.kei.feature.github.data.remote

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import os.kei.feature.github.model.GitHubActionsLookupStrategyOption
import os.kei.feature.github.model.GitHubApiAuthMode
import os.kei.feature.github.model.GitHubActionsArtifact
import java.time.Instant
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
            val requestPaths = List(3) { server.takeRequest().path }
            assertEquals("/repos/demo/app/actions/workflows/42/runs?per_page=2", requestPaths.first())
            assertEquals(
                setOf(
                    "/repos/demo/app/actions/runs/101/artifacts?per_page=100",
                    "/repos/demo/app/actions/runs/100/artifacts?per_page=100"
                ),
                requestPaths.drop(1).toSet()
            )
        }
    }

    @Test
    fun `workflow artifact snapshot can preload only recent run artifacts`() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(sampleWorkflowRunsJson()))
            server.enqueue(MockResponse().setResponseCode(200).setBody(sampleArtifactsJson(runId = 101)))
            val repository = GitHubActionsRepository(
                apiToken = "",
                apiBaseUrl = server.url("/").toString()
            )

            val snapshot = repository.fetchWorkflowArtifactSnapshot(
                owner = "demo",
                repo = "app",
                workflowId = "42",
                runLimit = 2,
                artifactRunLimit = 1
            ).result.getOrThrow()

            assertEquals(2, snapshot.runs.size)
            assertEquals(listOf(101L, 100L), snapshot.runs.map { it.run.id })
            assertEquals(2, snapshot.runs.first().artifacts.size)
            assertTrue(snapshot.runs.last().artifacts.isEmpty())
            assertEquals(2, snapshot.artifacts.size)
            assertEquals(
                listOf(
                    "/repos/demo/app/actions/workflows/42/runs?per_page=2",
                    "/repos/demo/app/actions/runs/101/artifacts?per_page=100"
                ),
                List(2) { server.takeRequest().path }
            )
        }
    }

    @Test
    fun `api run artifact request reuses short cache`() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(sampleArtifactsJson(runId = 101)))
            val repository = GitHubActionsRepository(
                apiToken = "",
                apiBaseUrl = server.url("/").toString()
            )

            val first = repository.fetchRunArtifacts(
                owner = "demo",
                repo = "app",
                runId = 101
            ).result.getOrThrow()
            val second = repository.fetchRunArtifacts(
                owner = "demo",
                repo = "app",
                runId = 101
            ).result.getOrThrow()

            assertEquals(first.map { it.id }, second.map { it.id })
            assertEquals("/repos/demo/app/actions/runs/101/artifacts?per_page=100", server.takeRequest().path)
            assertEquals(1, server.requestCount)
        }
    }

    @Test
    fun `workflow run status snapshot fetches artifacts after completion`() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(sampleWorkflowRunJson(status = "completed")))
            server.enqueue(MockResponse().setResponseCode(200).setBody(sampleArtifactsJson(runId = 101)))
            val repository = GitHubActionsRepository(
                apiToken = "",
                apiBaseUrl = server.url("/").toString()
            )

            val snapshot = repository.fetchRunStatusSnapshot(
                owner = "demo",
                repo = "app",
                runId = 101
            ).result.getOrThrow()

            assertEquals("completed", snapshot.run.status)
            assertEquals(2, snapshot.artifacts.size)
            assertEquals(
                listOf(
                    "/repos/demo/app/actions/runs/101",
                    "/repos/demo/app/actions/runs/101/artifacts?per_page=100"
                ),
                List(2) { server.takeRequest().path }
            )
        }
    }

    @Test
    fun `workflow run status snapshot skips artifacts while running`() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(sampleWorkflowRunJson(status = "in_progress")))
            val repository = GitHubActionsRepository(
                apiToken = "",
                apiBaseUrl = server.url("/").toString()
            )

            val snapshot = repository.fetchRunStatusSnapshot(
                owner = "demo",
                repo = "app",
                runId = 101
            ).result.getOrThrow()

            assertEquals("in_progress", snapshot.run.status)
            assertTrue(snapshot.artifacts.isEmpty())
            assertEquals("/repos/demo/app/actions/runs/101", server.takeRequest().path)
            assertEquals(1, server.requestCount)
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
    fun `nightly link strategy discovers workflow files from public github page`() {
        MockWebServer().use { github ->
            github.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                            <a href="/demo/app/blob/abc/.github/workflows/android.yml">android.yml</a>
                            <a href="/demo/app/blob/abc/.github/workflows/codeql.yaml">codeql.yaml</a>
                        """.trimIndent()
                    )
            )
            val repository = GitHubActionsRepository(
                apiToken = "",
                actionsStrategy = GitHubActionsLookupStrategyOption.NightlyLink,
                githubHtmlBaseUrl = github.url("/").toString()
            )

            val workflows = repository.fetchWorkflows(owner = "demo", repo = "app").result.getOrThrow()

            assertEquals(listOf("android.yml", "codeql.yaml"), workflows.map { it.path.substringAfterLast('/') })
            assertEquals("Android", workflows.first().name)
            assertEquals("/demo/app/tree/HEAD/.github/workflows", github.takeRequest().path)
            assertEquals(1, github.requestCount)
        }
    }

    @Test
    fun `nightly link strategy reads latest successful artifacts from nightly link`() {
        MockWebServer().use { nightly ->
            val githubBaseUrl = nightly.url("/github/").toString()
            val nightlyBaseUrl = nightly.url("/nightly/").toString()
            val base = nightlyBaseUrl.trimEnd('/')
            nightly.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                            <a href="$base/demo/app/workflows/android/master/Foss.zip">Foss.zip</a>
                            <a href="$base/demo/app/workflows/android/master/Market.zip">Market.zip</a>
                        """.trimIndent()
                    )
            )
            nightly.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                            <a href="$base/demo/app/actions/runs/250/Foss.zip">run link</a>
                            <a href="https://github.com/demo/app/suites/999/artifacts/777">artifact link</a>
                        """.trimIndent()
                    )
            )
            nightly.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(samplePublicGitHubRunHtml())
            )
            val repository = GitHubActionsRepository(
                apiToken = "",
                actionsStrategy = GitHubActionsLookupStrategyOption.NightlyLink,
                githubHtmlBaseUrl = githubBaseUrl,
                nightlyLinkBaseUrl = nightlyBaseUrl
            )

            val snapshot = repository.fetchWorkflowArtifactSnapshot(
                owner = "demo",
                repo = "app",
                workflowId = ".github/workflows/android.yml",
                branch = "master"
            ).result.getOrThrow()

            assertEquals(1, snapshot.runs.size)
            assertEquals(250L, snapshot.runs.first().run.id)
            assertEquals("Build v1.2.3 arm64 package", snapshot.runs.first().run.displayTitle)
            assertEquals("master", snapshot.runs.first().run.headBranch)
            assertEquals("def4567", snapshot.runs.first().run.headSha)
            assertEquals(Instant.parse("2026-04-29T01:19:28Z").toEpochMilli(), snapshot.runs.first().run.updatedAtMillis)
            assertEquals(listOf("Foss", "Market"), snapshot.artifacts.map { it.name })
            assertEquals(777L, snapshot.artifacts.first().id)
            assertEquals(3_848_274L, snapshot.artifacts.first().sizeBytes)
            assertEquals("sha256:demo", snapshot.artifacts.first().digest)
            assertEquals("def4567", snapshot.artifacts.first().workflowRunHeadSha)
            assertEquals(Instant.parse("2026-04-29T01:19:28Z").toEpochMilli(), snapshot.artifacts.first().updatedAtMillis)
            assertEquals("$base/demo/app/workflows/android/master/Foss.zip", snapshot.artifacts.first().archiveDownloadUrl)
            assertEquals(
                listOf(
                    "/nightly/demo/app/workflows/android/master?preview",
                    "/nightly/demo/app/workflows/android/master/Foss",
                    "/github/demo/app/actions/runs/250"
                ),
                List(3) { nightly.takeRequest().path }
            )
        }
    }

    @Test
    fun `nightly link strategy falls back to public api workflow id when file preview fails`() {
        MockWebServer().use { server ->
            val nightlyBaseUrl = server.url("/nightly/").toString()
            val base = nightlyBaseUrl.trimEnd('/')
            server.enqueue(MockResponse().setResponseCode(500).setBody("workflow preview failed"))
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                            {
                              "total_count": 1,
                              "workflows": [
                                {
                                  "id": 97592034,
                                  "node_id": "W_97592034",
                                  "name": "Build Example App",
                                  "path": ".github/workflows/Action CI.yml",
                                  "state": "active",
                                  "html_url": "https://github.com/demo/app/blob/main/.github/workflows/Action%20CI.yml",
                                  "badge_url": ""
                                }
                              ]
                            }
                        """.trimIndent()
                    )
            )
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                            {
                              "total_count": 1,
                              "workflow_runs": [
                                {
                                  "id": 251,
                                  "name": "Build Example App",
                                  "display_title": "app: add android 37.0 to androidList",
                                  "workflow_id": 97592034,
                                  "workflow_name": "Build Example App",
                                  "run_number": 661,
                                  "run_attempt": 1,
                                  "event": "push",
                                  "status": "completed",
                                  "conclusion": "success",
                                  "head_branch": "main",
                                  "head_sha": "def456",
                                  "html_url": "https://github.com/demo/app/actions/runs/251",
                                  "artifacts_url": "https://api.github.com/repos/demo/app/actions/runs/251/artifacts",
                                  "repository": {"full_name":"demo/app"},
                                  "head_repository": {"full_name":"demo/app", "fork": false},
                                  "pull_requests": [],
                                  "created_at": "2026-04-29T09:55:12Z",
                                  "run_started_at": "2026-04-29T09:55:12Z",
                                  "updated_at": "2026-04-29T10:09:49Z"
                                }
                              ]
                            }
                        """.trimIndent()
                    )
            )
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                            {
                              "total_count": 2,
                              "artifacts": [
                                {
                                  "id": 6704426665,
                                  "node_id": "A_6704426665",
                                  "name": "Updater-android-universal-apk.apk",
                                  "size_in_bytes": 1933410,
                                  "expired": false,
                                  "digest": "sha256:android",
                                  "archive_download_url": "https://api.github.com/repos/demo/app/actions/artifacts/6704426665/zip",
                                  "created_at": "2026-04-29T10:01:32Z",
                                  "updated_at": "2026-04-29T10:01:32Z",
                                  "expires_at": "2026-07-28T09:55:12Z",
                                  "workflow_run": {"id": 251, "head_branch": "main", "head_sha": "def456"}
                                },
                                {
                                  "id": 6704426443,
                                  "node_id": "A_6704426443",
                                  "name": "Updater-linux-x64-bin",
                                  "size_in_bytes": 67171008,
                                  "expired": false,
                                  "digest": "sha256:linux",
                                  "archive_download_url": "https://api.github.com/repos/demo/app/actions/artifacts/6704426443/zip",
                                  "created_at": "2026-04-29T10:01:31Z",
                                  "updated_at": "2026-04-29T10:01:31Z",
                                  "expires_at": "2026-07-28T09:55:12Z",
                                  "workflow_run": {"id": 251, "head_branch": "main", "head_sha": "def456"}
                                }
                              ]
                            }
                        """.trimIndent()
                    )
            )
            val repository = GitHubActionsRepository(
                apiToken = "",
                actionsStrategy = GitHubActionsLookupStrategyOption.NightlyLink,
                apiBaseUrl = server.url("/api/").toString(),
                nightlyLinkBaseUrl = nightlyBaseUrl
            )

            val snapshot = repository.fetchWorkflowArtifactSnapshot(
                owner = "demo",
                repo = "app",
                workflowId = ".github/workflows/Action CI.yml",
                branch = "main",
                resolveNightlyRunDetail = false
            ).result.getOrThrow()

            assertEquals("97592034", snapshot.workflowId)
            assertEquals(251L, snapshot.runs.single().run.id)
            assertEquals(
                listOf("Updater-android-universal-apk.apk", "Updater-linux-x64-bin"),
                snapshot.artifacts.map { it.name }
            )
            assertEquals(
                "$base/demo/app/actions/runs/251/Updater-android-universal-apk.apk.zip",
                snapshot.artifacts.first().archiveDownloadUrl
            )
            assertEquals(
                listOf(
                    "/nightly/demo/app/workflows/Action%20CI/main?preview",
                    "/api/repos/demo/app/actions/workflows?per_page=50",
                    "/api/repos/demo/app/actions/workflows/97592034/runs?per_page=20&branch=main&status=success",
                    "/api/repos/demo/app/actions/runs/251/artifacts?per_page=100"
                ),
                List(4) { server.takeRequest().path }
            )
        }
    }

    @Test
    fun `nightly link strategy can skip run detail for background signals`() {
        MockWebServer().use { nightly ->
            val base = nightly.url("/").toString().trimEnd('/')
            nightly.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                            <a href="$base/demo/app/workflows/android/master/Foss.zip">Foss.zip</a>
                            <a href="$base/demo/app/workflows/android/master/Market.zip">Market.zip</a>
                        """.trimIndent()
                    )
            )
            val repository = GitHubActionsRepository(
                apiToken = "",
                actionsStrategy = GitHubActionsLookupStrategyOption.NightlyLink,
                nightlyLinkBaseUrl = nightly.url("/").toString()
            )

            val snapshot = repository.fetchWorkflowArtifactSnapshot(
                owner = "demo",
                repo = "app",
                workflowId = ".github/workflows/android.yml",
                branch = "master",
                resolveNightlyRunDetail = false
            ).result.getOrThrow()

            assertEquals(1, snapshot.runs.size)
            assertEquals("master", snapshot.runs.first().run.headBranch)
            assertEquals(listOf("Foss", "Market"), snapshot.artifacts.map { it.name })
            assertEquals(
                "https://github.com/demo/app/actions?query=event%3Apush%20is%3Asuccess%20branch%3Amaster",
                snapshot.runs.first().run.htmlUrl
            )
            assertEquals(listOf("/demo/app/workflows/android/master?preview"), List(1) { nightly.takeRequest().path })
            assertEquals(1, nightly.requestCount)
        }
    }

    @Test
    fun `nightly link selected workflow explains empty artifact page`() {
        MockWebServer().use { nightly ->
            nightly.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("<p>Choose one of the artifacts:</p><table></table>")
            )
            val repository = GitHubActionsRepository(
                apiToken = "",
                actionsStrategy = GitHubActionsLookupStrategyOption.NightlyLink,
                nightlyLinkBaseUrl = nightly.url("/").toString()
            )

            val errorMessage = repository.fetchWorkflowArtifactSnapshot(
                owner = "demo",
                repo = "app",
                workflowId = ".github/workflows/android.yml",
                branch = "master"
            ).result.exceptionOrNull()?.message.orEmpty()

            assertTrue(errorMessage.contains("nightly.link 没有读取到 demo/app"))
            assertTrue(errorMessage.contains("android.yml"))
            assertTrue(errorMessage.contains("master"))
            assertTrue(errorMessage.contains("GitHub API Token"))
            assertEquals("/demo/app/workflows/android/master?preview", nightly.takeRequest().path)
        }
    }

    @Test
    fun `nightly link public page failure recommends token`() {
        MockWebServer().use { nightly ->
            nightly.enqueue(MockResponse().setResponseCode(404).setBody("not found"))
            val repository = GitHubActionsRepository(
                apiToken = "",
                actionsStrategy = GitHubActionsLookupStrategyOption.NightlyLink,
                nightlyLinkBaseUrl = nightly.url("/").toString()
            )

            val errorMessage = repository.fetchWorkflowArtifactSnapshot(
                owner = "demo",
                repo = "app",
                workflowId = ".github/workflows/android.yml",
                branch = "master"
            ).result.exceptionOrNull()?.message.orEmpty()

            assertTrue(errorMessage.contains("nightly.link"))
            assertTrue(errorMessage.contains("Actions 资源"))
            assertTrue(errorMessage.contains("GitHub API Token"))
        }
    }

    @Test
    fun `nightly link preview page reuses short cache`() {
        MockWebServer().use { nightly ->
            val base = nightly.url("/").toString().trimEnd('/')
            nightly.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                            <a href="$base/demo/app/workflows/android/master/Foss.zip">Foss.zip</a>
                            <a href="$base/demo/app/workflows/android/master/Market.zip">Market.zip</a>
                        """.trimIndent()
                    )
            )
            val repository = GitHubActionsRepository(
                apiToken = "",
                actionsStrategy = GitHubActionsLookupStrategyOption.NightlyLink,
                nightlyLinkBaseUrl = nightly.url("/").toString()
            )

            val first = repository.fetchWorkflowArtifactSnapshot(
                owner = "demo",
                repo = "app",
                workflowId = ".github/workflows/android.yml",
                branch = "master",
                resolveNightlyRunDetail = false
            ).result.getOrThrow()
            val second = repository.fetchWorkflowArtifactSnapshot(
                owner = "demo",
                repo = "app",
                workflowId = ".github/workflows/android.yml",
                branch = "master",
                resolveNightlyRunDetail = false
            ).result.getOrThrow()

            assertEquals(first.artifacts.map { it.name }, second.artifacts.map { it.name })
            assertEquals(listOf("/demo/app/workflows/android/master?preview"), List(1) { nightly.takeRequest().path })
            assertEquals(1, nightly.requestCount)
        }
    }

    @Test
    fun `nightly link run artifacts reuse public github run metadata`() {
        MockWebServer().use { nightly ->
            val githubBaseUrl = nightly.url("/github/").toString()
            val nightlyBaseUrl = nightly.url("/nightly/").toString()
            val base = nightlyBaseUrl.trimEnd('/')
            nightly.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(samplePublicGitHubRunHtml())
            )
            nightly.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                            <a href="$base/demo/app/actions/runs/250/Foss.zip">Foss.zip</a>
                            <a href="$base/demo/app/actions/runs/250/Market.zip">Market.zip</a>
                        """.trimIndent()
                    )
            )
            val repository = GitHubActionsRepository(
                apiToken = "",
                actionsStrategy = GitHubActionsLookupStrategyOption.NightlyLink,
                githubHtmlBaseUrl = githubBaseUrl,
                nightlyLinkBaseUrl = nightlyBaseUrl
            )

            val artifacts = repository.fetchRunArtifacts(
                owner = "demo",
                repo = "app",
                runId = 250
            ).result.getOrThrow()

            assertEquals(listOf("Foss", "Market"), artifacts.map { it.name })
            assertEquals(777L, artifacts.first().id)
            assertEquals(3_848_274L, artifacts.first().sizeBytes)
            assertEquals("sha256:demo", artifacts.first().digest)
            assertEquals("master", artifacts.first().workflowRunHeadBranch)
            assertEquals("def4567", artifacts.first().workflowRunHeadSha)
            assertEquals(
                listOf(
                    "/github/demo/app/actions/runs/250",
                    "/nightly/demo/app/actions/runs/250"
                ),
                List(2) { nightly.takeRequest().path }
            )
        }
    }

    @Test
    fun `nightly link download resolver returns shareable link without token`() {
        MockWebServer().use { nightly ->
            val url = nightly.url("/demo/app/workflows/android/master/Foss.zip").toString()
            val repository = GitHubActionsRepository(
                apiToken = "",
                actionsStrategy = GitHubActionsLookupStrategyOption.NightlyLink,
                nightlyLinkBaseUrl = nightly.url("/").toString()
            )

            val result = repository.resolveArtifactDownloadUrl(
                artifact = GitHubActionsArtifact(
                    id = 10L,
                    name = "Foss",
                    archiveDownloadUrl = url
                ),
                owner = "demo",
                repo = "app"
            ).getOrThrow()

            assertEquals(10L, result.artifactId)
            assertEquals(url, result.downloadUrl)
            assertEquals(0, nightly.requestCount)
        }
    }

    @Test
    fun `nightly link download resolver uses api artifact redirect when token exists`() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(302)
                    .addHeader("Location", "https://pipelines.actions.githubusercontent.com/demo/artifact.zip")
            )
            val repository = GitHubActionsRepository(
                apiToken = "test-token-123",
                actionsStrategy = GitHubActionsLookupStrategyOption.NightlyLink,
                apiBaseUrl = server.url("/api/").toString(),
                nightlyLinkBaseUrl = server.url("/nightly/").toString()
            )

            val result = repository.resolveArtifactDownloadUrl(
                artifact = GitHubActionsArtifact(
                    id = 6704426665,
                    name = "Updater-android-universal-apk.apk",
                    archiveDownloadUrl = "https://nightly.link/demo/app/actions/runs/251/Updater-android-universal-apk.apk.zip"
                ),
                owner = "demo",
                repo = "app",
                preferApiTokenRedirect = true
            ).getOrThrow()

            assertEquals(6704426665L, result.artifactId)
            assertEquals("https://pipelines.actions.githubusercontent.com/demo/artifact.zip", result.downloadUrl)
            val request = server.takeRequest()
            assertEquals("/api/repos/demo/app/actions/artifacts/6704426665/zip", request.path)
            assertEquals("Bearer test-token-123", request.getHeader("Authorization"))
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

    private fun sampleWorkflowRunJson(status: String): String {
        val conclusion = if (status == "completed") "success" else ""
        return """
            {
              "id": 101,
              "name": "Android CI",
              "display_title": "Build arm64 artifact",
              "workflow_id": 1,
              "workflow_name": "Android CI",
              "run_number": 10,
              "run_attempt": 1,
              "event": "push",
              "status": "$status",
              "conclusion": "$conclusion",
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

    private fun samplePublicGitHubRunHtml(): String {
        return """
            <html>
              <head>
                <title>Build v1.2.3 arm64 package · demo/app@def4567 · GitHub</title>
              </head>
              <body>
                <div role="region" aria-label="Workflow run summary">
                  <span>Triggered via push
                    <relative-time datetime="2026-04-29T01:19:28Z">April 29, 2026 01:19</relative-time>
                  </span>
                  <a class="branch-name css-truncate-target" title="master" href="/demo/app/tree/refs/heads/master">master</a>
                  <a href="/demo/app/commit/def4567">def4567</a>
                  <span>Status</span>
                  <span>Success</span>
                  <a href="/demo/app/actions/runs/250/workflow">android.yml</a>
                </div>
                <table>
                  <tbody>
                    <tr role="row" data-artifact-id="777">
                      <td><span class="text-bold color-fg-default">Foss</span></td>
                      <td>3.67 MB</td>
                      <td><code>sha256:demo</code></td>
                    </tr>
                    <tr role="row" data-artifact-id="778">
                      <td><span class="text-bold color-fg-default">Market</span></td>
                      <td>4 KB</td>
                      <td><code>sha256:market</code></td>
                    </tr>
                  </tbody>
                </table>
              </body>
            </html>
        """.trimIndent()
    }
}
