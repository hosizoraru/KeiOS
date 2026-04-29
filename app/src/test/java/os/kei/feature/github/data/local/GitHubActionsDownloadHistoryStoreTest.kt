package os.kei.feature.github.data.local

import org.junit.Test
import os.kei.feature.github.model.GitHubActionsDownloadRecord
import kotlin.test.assertEquals

class GitHubActionsDownloadHistoryStoreTest {
    @Test
    fun `download history record round trips through json`() {
        val record = GitHubActionsDownloadRecord(
            owner = "demo",
            repo = "app",
            workflowId = 10L,
            workflowName = "Build",
            workflowPath = ".github/workflows/build.yml",
            runId = 20L,
            runNumber = 7L,
            runAttempt = 1,
            runDisplayName = "Build main",
            headBranch = "main",
            headSha = "abc123",
            event = "push",
            status = "completed",
            conclusion = "success",
            artifactId = 30L,
            artifactName = "app-arm64-v8a-release.apk",
            artifactDigest = "sha256:demo",
            artifactSizeBytes = 24_000_000L,
            sourceTrackId = "demo/app|pkg",
            packageName = "demo.app",
            downloadedAtMillis = 1000L
        )

        val decoded = GitHubActionsDownloadHistoryStore.decodeRecord(
            GitHubActionsDownloadHistoryStore.encodeRecord(record).toString()
        )

        assertEquals(record, decoded)
    }
}
